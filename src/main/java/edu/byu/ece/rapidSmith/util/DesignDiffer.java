/*
 * Copyright (c) 2010 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.util;

import edu.byu.ece.rapidSmith.design.*;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLReader;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This is a class contains methods for comparing designs.  Differences can be
 * either a change when design1 and design2 differ, an addition when design1 does
 * not contain an element found in design2 or a subtraction when design2 is missing
 * an element contained in design1.  Each difference contains a short description
 * of the element that is different, such as the name of the element, and the
 * values of the elements.
 * <p>
 * The primary method for this class is the diffDesigns method but support is
 * added for diffing instances and nets as well.  The main method prints the
 * differences between the two designs on the command line.
 */
public class DesignDiffer {
	public enum MatchInstanceMethod {
		BY_NAME, BY_LOCATION
	}

	public enum MatchNetMethod {
		BY_NAME, BY_SOURCE_PIN
	}

	private MatchInstanceMethod matchInstanceMethod;
	private MatchNetMethod matchNetMethod;
	private boolean ignoreBelProps;
	private boolean ignoreInstProps;

	public DesignDiffer() {
		matchInstanceMethod = MatchInstanceMethod.BY_NAME;
		matchNetMethod = MatchNetMethod.BY_NAME;
		ignoreBelProps = false;
		ignoreInstProps = false;
	}

	public void setMatchInstanceMethod(MatchInstanceMethod matchInstanceMethod) {
		this.matchInstanceMethod = matchInstanceMethod;
	}

	public void setMatchNetMethod(MatchNetMethod matchNetMethod) {
		this.matchNetMethod = matchNetMethod;
	}

	public void setIgnoreBelProps(boolean ignoreBelProp) {
		this.ignoreBelProps = ignoreBelProp;
	}

	public void setIgnoreInstProps(boolean ignoreInstProp) {
		this.ignoreInstProps = ignoreInstProp;
	}

	/**
	 * Compares two design and returns the differences.
	 *
	 * @param design1 First design to compare.
	 * @param design2 Second design to compare.
	 */
	public DifferenceTree diffDesigns(Design design1, Design design2) {
		DifferenceTree diffs = new DifferenceTree(design1.getName(), "design");

		// Compare Design elements
		if (!design1.getName().equals(design2.getName()))
			diffs.add(Difference.change("name", design1.getName(), design2.getName()));
		if (!design1.getPartName().equals(design2.getPartName()))
			diffs.add(Difference.change("part", design1.getPartName(), design2.getPartName()));
		if (!design1.getNCDVersion().equals(design2.getNCDVersion()))
			diffs.add(Difference.change("NCD version", design1.getNCDVersion(), design2.getNCDVersion()));
		if (design1.isHardMacro() != design2.isHardMacro()) {
			diffs.add(Difference.change("is_hard_macro", Boolean.toString(design1.isHardMacro()),
					Boolean.toString(design2.isHardMacro())));
		}
		if (design1.getHardMacro() != null) {
			if (design2.getHardMacro() != null) {
				if (!design1.getHardMacro().getName().equals(design2.getHardMacro().getName()))
					diffs.add(Difference.change("hard_macro_name", design1.getHardMacro().getName(),
							design2.getHardMacro().getName()));
			} else {
				diffs.add(Difference.subtraction("hard macro", design1.getHardMacro().getName()));
			}
		} else if (design2.getHardMacro() != null) {
			diffs.add(Difference.addition("hard_macro", design2.getHardMacro().getName()));
		}

		// compare design attributes
		Map<String, Set<Attribute>> attrs2 = new HashMap<>();
		for (Attribute attr : design2.getAttributes()) {
			String physicalName = attr.getPhysicalName();
			if (!attrs2.containsKey(physicalName))
				attrs2.put(physicalName, new HashSet<>());
			Set<Attribute> attrs = attrs2.get(physicalName);
			attrs.add(attr);
		}
		for (Attribute attr1 : design1.getAttributes()) {
			if (attr1.getPhysicalName().startsWith("_")) {
				// test if it exists and remove it simultaneously
				Set<Attribute> attrSet2 = attrs2.get(attr1.getPhysicalName());
				if (attrSet2 == null) {
					diffs.add(Difference.subtraction("attribute", attr1.getPhysicalName()));
					continue;
				}
				boolean found = false;
				for (Iterator<Attribute> iterator = attrSet2.iterator(); iterator.hasNext(); ) {
					Attribute attr2 = iterator.next();
					if (attr2.getValue().equals(attr1.getValue())) {
						iterator.remove();
						found = true;
						break;
					}
				}
				if (!found)
					diffs.add(Difference.addition("attribute", attr1.getPhysicalName()));
			} else {
				// test if it exists and remove it simultaneously
				Set<Attribute> attrSet2 = attrs2.remove(attr1.getPhysicalName());
				if (attrSet2 == null) {
					diffs.add(Difference.subtraction("attribute", attr1.getPhysicalName()));
					continue;
				}
				Attribute attr2 = attrSet2.iterator().next();
				attrSet2.clear();
				DifferenceTree attrTree = diffAttributes(attr1, attr2);
				if (!attrTree.isEmpty())
					diffs.addChild(attrTree);
			}
		}
		for (Set<Attribute> attrSet2 : attrs2.values()) {
			for (Attribute attr2 : attrSet2) {
				diffs.add(Difference.addition("attribute", attr2.getPhysicalName()));
			}
		}

		// Compare instances
		for (Instance inst1 : design1.getInstances()) {
			Instance inst2 = null;
			switch (matchInstanceMethod) {
				case BY_NAME :
					inst2 = design2.getInstance(inst1.getName());
					break;
				case BY_LOCATION :
					inst2 = design2.getInstanceAtPrimitiveSite(inst1.getPrimitiveSite());
					break;
			}
			if (inst2 == null) {
				diffs.add(Difference.subtraction("instance", inst1.getName()));
				continue;
			}
			DifferenceTree instTree = diffInstances(inst1, inst2);
			diffs.addChild(instTree);
		}
		for (Instance inst2 : design2.getInstances()) {
			switch (matchInstanceMethod) {
				case BY_NAME :
					if (design1.getInstance(inst2.getName()) == null)
						diffs.add(Difference.addition("instance", inst2.getName()));
					break;
				case BY_LOCATION :
					if (design1.getInstanceAtPrimitiveSite(inst2.getPrimitiveSite()) == null)
						diffs.add(Difference.addition("instance", inst2.getName()));
					break;
			}
		}

		// compare nets

		Map<PinId, Net> netSourcePinMap = null;
		if (matchNetMethod == MatchNetMethod.BY_SOURCE_PIN) {
			netSourcePinMap = new HashMap<>();
			for (Net net : design2.getNets()) {
				if (net.getSource() != null)
					netSourcePinMap.put(new PinId(net.getSource()), net);
			}
		}

		for (Net net1 : design1.getNets()) {
			Net net2 = null;
			switch(matchNetMethod) {
				case BY_NAME :
					net2 = design2.getNet(net1.getName());
					break;
				case BY_SOURCE_PIN :
					if (net1.getSource() == null)
						continue;
					PinId pinId1 = new PinId(net1.getSource());
					//noinspection ConstantConditions
					net2 = netSourcePinMap.remove(pinId1);
					break;
			}
			if (net2 == null) {
				diffs.add(Difference.subtraction("net", net1.getName()));
				continue;
			}
			DifferenceTree netTree = diffNets(net1, net2);
			if (!netTree.isEmpty())
				diffs.addChild(netTree);
		}
		switch (matchNetMethod) {
			case BY_NAME:
				for (Net net2 : design2.getNets()) {
					if (design1.getNet(net2.getName()) == null)
						diffs.add(Difference.addition("net", net2.getName()));
					break;
				}
			case BY_SOURCE_PIN:
				//noinspection ConstantConditions
				for (Net net2 : netSourcePinMap.values()) {
					diffs.add(Difference.addition("net", net2.getName()));
				}
		}

		// compare modules
		for (Module module1 : design1.getModules()) {
			Module module2 = design2.getModule(module1.getName());
			if (module2 == null) {
				diffs.add(Difference.subtraction("module", module1.getName()));
				continue;
			}
			DifferenceTree modTree = diffModules(module1, module2);
			if (!modTree.isEmpty())
				diffs.addChild(modTree);
		}
		for (Module module2 : design2.getModules()) {
			if (design1.getModuleInstance(module2.getName()) == null)
				diffs.add(Difference.addition("module", module2.getName()));
		}

		// compare module instances
		for (ModuleInstance mi1 : design1.getModuleInstances()) {
			ModuleInstance mi2 = design2.getModuleInstance(mi1.getName());
			if (mi2 == null) {
				diffs.add(Difference.subtraction("module_instance", mi1.getName()));
				continue;
			}
			DifferenceTree miTree = diffModuleInstances(mi1, mi2);
			if (!miTree.isEmpty())
				diffs.addChild(miTree);
		}
		for (ModuleInstance mi2 : design2.getModuleInstances()) {
			if (design1.getModuleInstance(mi2.getName()) == null)
				diffs.add(Difference.addition("module_instance", mi2.getName()));
		}

		return diffs;
	}

	private DifferenceTree diffModules(Module mod1, Module mod2) {
		DifferenceTree diffs = new DifferenceTree(mod1.getName(), "module");

		// compare module attributes
		Map<String, Attribute> attrs2 = mod2.getAttributes().stream()
				.collect(Collectors.toMap(Attribute::getPhysicalName, Function.identity()));
		for (Attribute attr1 : mod1.getAttributes()) {
			// test if it exists and remove it simultaneously
			Attribute attr2 = attrs2.remove(attr1.getPhysicalName());
			if (attr2 == null) {
				diffs.add(Difference.subtraction("attribute", attr1.getPhysicalName()));
				continue;
			}
			DifferenceTree attrTree = diffAttributes(attr1, attr2);
			if (!attrTree.isEmpty())
				diffs.addChild(attrTree);
		}
		for (Attribute attr2 : attrs2.values()) {
			diffs.add(Difference.addition("attribute", attr2.getPhysicalName()));
		}

		// compare anchor
		if (mod1.getAnchor() != null) {
			if (mod2.getAnchor() != null) {
				if (!mod1.getAnchor().getName().equals(mod2.getAnchor().getName()))
					diffs.add(Difference.change("site", mod1.getAnchor().getName(), mod2.getAnchor().getName()));
			} else {
				diffs.add(Difference.change("site", mod1.getAnchor().getName(), "unplaced"));
			}
		} else if (mod2.getAnchor() != null) {
			diffs.add(Difference.change("site", "unplaced", mod2.getAnchor().getName()));
		}

		// compare ports
		for (Port port1 : mod1.getPorts()) {
			Port port2 = mod2.getPort(port1.getName());
			if (port2 == null) {
				diffs.add(Difference.subtraction("port", port1.getName()));
				continue;
			}
			DifferenceTree portTree = diffPorts(port1, port2);
			if (!portTree.isEmpty())
				diffs.addChild(portTree);
		}
		for (Port port2 : mod2.getPorts()) {
			if (mod1.getPort(port2.getName()) == null)
				diffs.add(Difference.addition("port", port2.getName()));
		}

		// Compare instances
		for (Instance inst1 : mod1.getInstances()) {
			Instance inst2 = mod2.getInstance(inst1.getName());
			if (inst2 == null) {
				diffs.add(Difference.subtraction("instance", inst1.getName()));
				continue;
			}
			DifferenceTree instTree = diffInstances(inst1, inst2);
			diffs.addChild(instTree);
		}
		for (Instance inst2 : mod2.getInstances()) {
			if (mod1.getInstance(inst2.getName()) == null)
				diffs.add(Difference.addition("instance", inst2.getName()));
		}

		// compare nets
		for (Net net1 : mod1.getNets()) {
			Net net2 = mod2.getNet(net1.getName());
			if (net2 == null) {
				diffs.add(Difference.subtraction("net", net1.getName()));
				continue;
			}
			DifferenceTree netTree = diffNets(net1, net2);
			if (!netTree.isEmpty())
				diffs.addChild(netTree);
		}
		for (Net net2 : mod2.getNets()) {
			if (mod1.getNet(net2.getName()) == null)
				diffs.add(Difference.addition("net", net2.getName()));
		}

		return diffs;
	}

	private DifferenceTree diffModuleInstances(ModuleInstance mi1, ModuleInstance mi2) {
		DifferenceTree diffs = new DifferenceTree(mi1.getName(), "module instance");
		if (!mi1.getModule().getName().equals(mi2.getName()))
			diffs.add(Difference.change("module", mi1.getModule().getName(), mi2.getModule().getName()));

		Map<String, Instance> instances1 = mi1.getInstances().stream()
				.collect(Collectors.toMap(i -> i.getModuleTemplateInstance().getName(), Function.identity()));
		Map<String, Instance> instances2 = mi2.getInstances().stream()
				.collect(Collectors.toMap(i -> i.getModuleTemplateInstance().getName(), Function.identity()));
		for (String templateInstName : instances1.keySet()) {
			Instance miInst1 = instances1.get(templateInstName);
			Instance miInst2 = instances2.get(templateInstName);

			if (!miInst1.getName().equals(miInst2.getName())) {
				diffs.add(Difference.change(templateInstName + " instantiation", miInst1.getName(), miInst2.getName()));
			}
		}

		Map<String, Net> nets1 = mi1.getNets().stream()
				.collect(Collectors.toMap(i -> i.getModuleTemplateNet().getName(), Function.identity()));
		Map<String, Net> nets2 = mi2.getNets().stream()
				.collect(Collectors.toMap(i -> i.getModuleTemplateNet().getName(), Function.identity()));
		for (String templateNetName : nets1.keySet()) {
			Net miNet1 = nets1.get(templateNetName);
			Net miNet2 = nets2.get(templateNetName);

			if (!miNet1.getName().equals(miNet2.getName())) {
				diffs.add(Difference.change(templateNetName + " instantiation", miNet1.getName(), miNet2.getName()));
			}
		}

		return diffs;
	}

	private DifferenceTree diffAttributes(Attribute attr1, Attribute attr2) {
		// TODO handle multi value attributes to remove ordering requirement
		// TODO compare LUTS based on function instead of string representation

		DifferenceTree diffs = new DifferenceTree(attr1.getPhysicalName(), "attribute");

		if (!attr1.getLogicalName().equals(attr2.getLogicalName()))
			diffs.add(Difference.change("logical_name", attr1.getLogicalName(), attr2.getLogicalName()));
		if (!attr1.getValue().equals(attr2.getValue()))
			diffs.add(Difference.change("value", attr1.getValue(), attr2.getValue()));
		return diffs;
	}

	public DifferenceTree diffInstances(Instance inst1, Instance inst2) {
		DifferenceTree diffs = new DifferenceTree(inst1.getName(), "instance");
		// compare names
		if (!inst1.getName().equals(inst2.getName()))
			diffs.add(Difference.change("name", inst1.getName(), inst2.getName()));

		// compare types
		if (inst1.getType() != inst2.getType())
			diffs.add(Difference.change("type", inst1.getType().toString(), inst2.getType().toString()));

		// compare bondedness
		if (inst1.getBonded() != inst2.getBonded())
			diffs.add(Difference.change("bonded", inst1.getBonded().toString(), inst2.getBonded().toString()));

		// compare site
		if (inst1.isPlaced()) {
			if (inst2.isPlaced()) {
				if (!inst1.getPrimitiveSite().getName().equals(inst2.getPrimitiveSite().getName()))
					diffs.add(Difference.change("site", inst1.getPrimitiveSite().getName(), inst2.getPrimitiveSite().getName()));
			} else {
				diffs.add(Difference.change("site", inst1.getPrimitiveSite().getName(), "unplaced"));
			}
		} else if (inst2.isPlaced()) {
			diffs.add(Difference.change("site", "unplaced", inst2.getPrimitiveSite().getName()));
		}

		// compare pins
		for (Pin pin1 : inst1.getPins()) {
			Pin pin2 = inst2.getPin(pin1.getName());
			if (pin2 == null) {
				diffs.add(Difference.subtraction("pin", pin1.getName()));
				continue;
			}
			DifferenceTree pinTree = diffPins(pin1, pin2);
			if (!pinTree.isEmpty())
				diffs.addChild(pinTree);
		}
		for (Pin pin2 : inst2.getPins()) {
			if (inst1.getPin(pin2.getName()) == null)
				diffs.add(Difference.addition("pin", pin2.getName()));
		}

		// compare attributes
		for (Attribute attr1 : inst1.getAttributes()) {
			if (ignoreBelProps && attr1.getPhysicalName().equals("_BEL_PROP"))
				continue;
			if (ignoreInstProps && attr1.getPhysicalName().equals("_INST_PROP"))
				continue;
			Attribute attr2 = inst2.getAttribute(attr1.getPhysicalName());
			if (attr2 == null) {
				if (!attr1.getValue().equals("#OFF"))
					diffs.add(Difference.subtraction("attribute", attr1.getPhysicalName()));
				continue;
			}
			DifferenceTree attrTree = diffAttributes(attr1, attr2);
			if (!attrTree.isEmpty())
				diffs.addChild(attrTree);
		}
		for (Attribute attr2 : inst2.getAttributes()) {
			if (ignoreBelProps && attr2.getPhysicalName().equals("_BEL_PROP"))
				continue;
			if (ignoreInstProps && attr2.getPhysicalName().equals("_INST_PROP"))
				continue;
			if (inst1.getAttribute(attr2.getPhysicalName()) == null) {
				if (!attr2.getValue().equals("#OFF"))
					diffs.add(Difference.addition("attribute", attr2.getPhysicalName()));
			}
		}
		return diffs;
	}

	public DifferenceTree diffNets(Net net1, Net net2) {
		DifferenceTree diffs = new DifferenceTree(net1.getName(), "net");
		if (!net1.getName().equals(net2.getName()))
			diffs.add(Difference.change("name", net1.getName(), net2.getName()));

		if (net1.getType() != net2.getType())
			diffs.add(Difference.change("type", net1.getType().toString(), net2.getType().toString()));

		if (net1.getSource() != null) {
			if (net2.getSource() == null) {
				diffs.add(Difference.change("source", net1.getSource().getName(), "unsourced"));
			} else if (!net1.getSource().getName().equals(net2.getSource().getName())) {
				diffs.add(Difference.change("source", net1.getSource().getName(), net2.getSource().getName()));
			}
		} else if (net2.getSource() != null) {
			diffs.add(Difference.change("source", "unsourced", net2.getSource().getName()));
		}

		// compare attributes
		if (net2.getAttributes() != null) {
			Map<String, Attribute> attrs2 = net2.getAttributes().stream()
					.collect(Collectors.toMap(Attribute::getPhysicalName, Function.identity()));
			for (Attribute attr1 : net1.getAttributes()) {
				Attribute attr2 = attrs2.get(attr1.getPhysicalName());
				if (attr2 == null) {
					diffs.add(Difference.subtraction("attribute", attr1.getPhysicalName()));
					continue;
				}
				DifferenceTree attrTree = diffAttributes(attr1, attr2);
				if (!attrTree.isEmpty())
					diffs.addChild(attrTree);
			}
			for (Attribute attr2 : attrs2.values()) {
				diffs.add(Difference.addition("attribute", attr2.getPhysicalName()));
			}
		} else {
			if (net1.getAttributes() != null) {
				for (Attribute attr1 : net1.getAttributes()) {
					diffs.add(Difference.addition("attribute", attr1.getPhysicalName()));
				}
			}
		}

		// compare pins
		Map<PinId, Pin> pins2 = net2.getPins().stream()
				.collect(Collectors.toMap(PinId::new, Function.identity()));
		for (Pin pin1 : net1.getPins()) {
			// Check for existence and remove it simultaneously
			Pin pin2 = pins2.remove(new PinId(pin1));
			if (pin2 == null) {
				diffs.add(Difference.subtraction("pin", new PinId(pin1).toString()));
			}
		}
		for (Pin pin2 : pins2.values()) {
			diffs.add(Difference.addition("pin", new PinId(pin2).toString()));
		}

		// compare PIPS
		Set<PIP> pips2 = new HashSet<>(net2.getPIPs());
		for (PIP pip1 : net1.getPIPs()) {
			// Check for existence and remove it simultaneously
			if (!pips2.remove(pip1)) {
				diffs.add(Difference.subtraction("PIP", pip1.toString()));
			}
		}
		for (PIP pip2 : pips2) {
			diffs.add(Difference.addition("PIP", pip2.toString()));
		}

		return diffs;
	}

	private class PinId {
		private Pin pin;

		private PinId(Pin pin) {
			this.pin = pin;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			PinId pinId = (PinId) o;

			if (!pin.getName().equals(pinId.pin.getName())) return false;
			if (matchInstanceMethod == MatchInstanceMethod.BY_NAME)
				if (!pin.getInstanceName().equals(pinId.pin.getInstanceName()))
					return false;
			else if (matchInstanceMethod == MatchInstanceMethod.BY_LOCATION)
				if (!pin.getInstance().getPrimitiveSite().equals(
						pinId.pin.getInstance().getPrimitiveSite()))
					return false;
			else
				throw new AssertionError("Unknown Match Instance Method");

			return true;
		}

		@Override
		public int hashCode() {
			int result = pin.getName().hashCode();
			if (matchInstanceMethod == MatchInstanceMethod.BY_NAME)
				result = 31 * result + pin.getInstanceName().hashCode();
			else if (matchInstanceMethod == MatchInstanceMethod.BY_LOCATION)
				result = 31 * result + pin.getInstance().getPrimitiveSite().hashCode();
			else
				throw new AssertionError("Unknown Match Instance Method");

			return result;
		}

		@Override
		public String toString() {
			String result = "";
			if (matchInstanceMethod == MatchInstanceMethod.BY_NAME)
				result += pin.getInstanceName();
			else if (matchInstanceMethod == MatchInstanceMethod.BY_LOCATION)
				result += pin.getInstance().getPrimitiveSite().getName();

			return result + "/" + pin.getName();
		}
	}

	private DifferenceTree diffPins(Pin pin1, Pin pin2) {
		DifferenceTree diffs = new DifferenceTree(pin1.getName(), "pin");
		if (pin1.getPinType() != pin2.getPinType())
			diffs.add(Difference.change("type", pin1.getPinType().toString(), pin2.getPinType().toString()));
		return diffs;
	}

	private DifferenceTree diffPorts(Port port1, Port port2) {
		DifferenceTree diffs = new DifferenceTree(port1.getName(), "port");
		if (!port1.getPinName().equals(port2.getPinName()))
			diffs.add(Difference.change("pin", port1.getPinName(), port2.getPinName()));
		return diffs;
	}

	public static class Difference {
		private DifferenceType type;
		private String property;
		private String expected;
		private String actual;

		private Difference(DifferenceType type, String property, String expected, String actual) {
			this.type = type;
			this.property = property;
			this.expected = expected;
			this.actual = actual;
		}

		public static Difference change(String property, String expected, String actual) {
			return new Difference(DifferenceType.CHANGE, property, expected, actual);
		}

		public static Difference addition(String property, String expected) {
			return new Difference(DifferenceType.ADDITION, property, expected, null);
		}

		public static Difference subtraction(String property, String actual) {
			return new Difference(DifferenceType.SUBTRACTION, property, null, actual);
		}

		public DifferenceType getType() {
			return type;
		}

		public String getProperty() {
			return property;
		}

		public String getExpected() {
			return expected;
		}

		public String getActual() {
			return actual;
		}

		public String toString() {
			if (type == DifferenceType.CHANGE)
				return "* (" + property + ") " + expected + " -- " + actual;
			else if (type == DifferenceType.ADDITION)
				return "+ (" + property + ") " + expected;
			else if (type == DifferenceType.SUBTRACTION)
				return "- (" + property + ") " + actual;

			throw new RuntimeException("Should never reach");
		}
	}

	public enum DifferenceType {
		CHANGE, ADDITION, SUBTRACTION
	}

	public static class DifferenceTree {
		private static final String nl = System.lineSeparator();
		private String levelName;
		private String levelType;
		private List<Difference> diffs = new ArrayList<>();
		private List<DifferenceTree> children = new ArrayList<>();

		public DifferenceTree(String levelName, String levelType) {
			this.levelName = levelName;
			this.levelType = levelType;
		}

		public void add(Difference diff) {
			diffs.add(diff);
		}

		public void addChild(DifferenceTree child) {
			children.add(child);
		}

		public boolean isEmpty() {
			return diffs.isEmpty() && children.isEmpty();
		}

		public void removeDifference(String... property) {
			Objects.requireNonNull(property);
			if (property.length == 0)
				throw new IllegalArgumentException();
			removeDifference(property, 0);
		}

		private void removeDifference(String[] property, int index) {
			if (property.length == index + 1) {
				for (Iterator<Difference> iterator = diffs.iterator(); iterator.hasNext(); ) {
					Difference diff = iterator.next();
					if (diff.getProperty().equals(property[index]))
						iterator.remove();
				}
			} else {
				for (Iterator<DifferenceTree> iterator = children.iterator(); iterator.hasNext(); ) {
					DifferenceTree child = iterator.next();
					if (child.levelType.equals(property[index])) {
						child.removeDifference(property, index + 1);
						if (child.children.isEmpty() && child.diffs.isEmpty()) {
							iterator.remove();
						}
					}
				}
			}
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			toString("", sb);
			return sb.toString();
		}

		public void toString(String indent, StringBuilder sb) {
			sb.append(indent + levelName + " (" + levelType + ")" + nl);
			for (Difference diff : diffs) {
				sb.append(indent + "  " + diff + nl);
			}
			for (DifferenceTree child : children) {
				child.toString(indent + "  ", sb);
			}
		}
	}

	public static void main(String[] args) throws IOException {
		OptionParser parser = new OptionParser();
		parser.acceptsAll(Arrays.asList("output", "o"), "Output file, stdout if not specified").withRequiredArg();
		parser.accepts("match_sites", "Match instances based on site, assumes placed circuit");
		parser.accepts("match_sources", "Match nets based on source pin, unsourced nets ignored");
		parser.accepts("ignore", "Ignores specified differences, '/' used to distinguish hierarchy").withRequiredArg();
		parser.accepts("ignore_bel_props");
		parser.accepts("ignore_inst_props");
		parser.nonOptions("<path to design 1> <path to design 2>");

		OptionSet options = null;
		try {
			options = parser.parse(args);
		} catch (OptionException e) {
			try {
				parser.printHelpOn(System.err);
			} catch (IOException ignored) {
			}
			System.exit(-1);
		}

		if (options.nonOptionArguments().size() < 2) {
			try {
				parser.printHelpOn(System.err);
			} catch (IOException ignored) {
			}
			System.exit(-1);
		}

		Path path1 = Paths.get((String) options.nonOptionArguments().get(0));
		Path path2 = Paths.get((String) options.nonOptionArguments().get(1));
		Design design1 = new XDLReader().readDesign(path1);
		Design design2 = new XDLReader().readDesign(path2);

		DesignDiffer designDiffer = new DesignDiffer();
		if (options.has("match_sites"))
			designDiffer.setMatchInstanceMethod(MatchInstanceMethod.BY_LOCATION);
		if (options.has("match_sources"))
			designDiffer.setMatchNetMethod(MatchNetMethod.BY_SOURCE_PIN);
		designDiffer.setIgnoreBelProps(options.has("ignore_bel_props"));
		designDiffer.setIgnoreInstProps(options.has("ignore_inst_props"));
		DifferenceTree tree = designDiffer.diffDesigns(design1, design2);

		for (Object removeOpsObj : options.valuesOf("ignore")) {
			String removeOps = (String) removeOpsObj;
			String[] propertyTree = removeOps.split("/");
			tree.removeDifference(propertyTree);
		}

		PrintStream ps = System.out;
		if (options.hasArgument("output")) {
			try {
				ps = new PrintStream(Files.newOutputStream(Paths.get((String) options.valueOf("output"))));
			} catch (IOException e) {
				System.err.println("Error opening file for output");
				ps.close();
				System.exit(-1);
			}
		}

		ps.print(tree);
	}
}
