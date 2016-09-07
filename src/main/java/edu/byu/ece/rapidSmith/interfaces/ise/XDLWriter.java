package edu.byu.ece.rapidSmith.interfaces.ise;

import edu.byu.ece.rapidSmith.design.*;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.util.FileTools;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class XDLWriter {
	private String nl = System.lineSeparator();
	private boolean addComments = true;
	private boolean addPips = true;

	private Design design;
	private Writer out;

	public XDLWriter setLineSeparator(String lineSeparator) {
		nl = lineSeparator;
		return this;
	}

	public XDLWriter addComments(boolean c) {
		this.addComments = c;
		return this;
	}

	public XDLWriter addPips(boolean p) {
		this.addPips = p;
		return this;
	}

	public void writeXDL(Design design, Path file) throws IOException {
		try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			writeXDL(design, writer);
		}
	}

	public void writeXDL(Design design, Writer out) throws IOException {
		this.design = design;
		this.out = out;
		writeHeader();

		if (!design.isHardMacro()) {
			out.append("design \"" + design.getName() + "\" " + design.getPartName() + " " + design.getNCDVersion() + " ," + nl);
			out.append("  cfg \"");
			writeAttributes(design.getAttributes(), nl + "\t");
			out.append("\";" + nl + nl + nl);
		} else {
			out.append("design \"" + design.getName() + "\" " + design.getPartName() + ";" + nl + nl);
		}

		writeModules();

		if (!design.isHardMacro()) {
			writeModuleInstances();
			writeDesignInstances();
			writeDesignNets();

			out.append(nl);

			writeDesignSummary();
		} else {
			writeHMSummary();
		}
	}

	private void writeHeader() throws IOException {
		if (addComments) {
			out.append(nl + "# =======================================================" + nl);
			out.append("# " + this.getClass().getCanonicalName() + " XDL Generation $Revision: 1.01$" + nl);
			out.append("# time: " + FileTools.getTimeString() + nl + nl);
			out.append("# =======================================================" + nl + nl + nl);

			out.append("# =======================================================" + nl);
			out.append("# The syntax for the design statement is:                " + nl);
			out.append("# design <design_name> <part> <ncd version>;             " + nl);
			out.append("# or                                                     " + nl);
			out.append("# design <design_name> <device> <package> <speed> <ncd_version>" + nl);
			out.append("# =======================================================" + nl);
		}
	}

	private void writeModules() throws IOException {
		if (design.getModules().size() > 0) {
			if (addComments) {
				out.append("# =======================================================" + nl);
				out.append("# The syntax for modules is:" + nl);
				out.append("#     module <name> <inst_name> ;" + nl);
				out.append("#     port <name> <inst_name> <inst_pin> ;" + nl);
				out.append("#     ." + nl);
				out.append("#     ." + nl);
				out.append("#     instance ... ;" + nl);
				out.append("#     ." + nl);
				out.append("#     ." + nl);
				out.append("#     net ... ;" + nl);
				out.append("#     ." + nl);
				out.append("#     ." + nl);
				out.append("#     endmodule <name> ;" + nl);
				out.append("# =======================================================" + nl + nl);
			}

			List<Module> modules = new ArrayList<>(design.getModules());
			Collections.sort(modules, (o1, o2) -> o1.getName().compareTo(o2.getName()));
			for (Module module : modules) {
				if (addComments) {
					out.append("# =======================================================" + nl);
					out.append("# MODULE of \"" + module.getName() + "\"" + nl);
					out.append("# =======================================================" + nl);
				}

				if (module.getAnchor() == null) {
					if (addComments) {
						out.append("# This module is a routing only block" + nl);
					}
					continue;
				}

				out.append("module " + "\"" + module.getName() + "\" \"" + module.getAnchor().getName() + "\" , cfg \"");

				writeAttributes(module.getAttributes(), " ");
				writePorts(module.getPorts());
				writeInstances(module.getInstances(), "  ");
				writeNets(module.getNets(), "  ");

				out.append("endmodule \"" + module.getName() + "\" ;" + nl + nl);
			}
		}
	}

	private void writeModuleInstances() throws IOException {
		if (design.getModuleInstances().size() > 0 && addComments) {
			out.append(nl);
			out.append("#  =======================================================" + nl);
			out.append("#  MODULE INSTANCES" + nl);
			out.append("#  =======================================================" + nl);

			List<ModuleInstance> moduleInstanceNames = new ArrayList<>(design.getModuleInstances());
			Collections.sort(moduleInstanceNames, (o1, o2) -> o1.getName().compareTo(o2.getName()));
			for (ModuleInstance mi : moduleInstanceNames) {
				out.append("# instance \"" + mi.getName() + "\" \"" + mi.getModule().getName() + "\" , ");
				if (mi.getAnchor() != null && mi.getAnchor().isPlaced()) {
					out.append("placed " + mi.getAnchor().getTile().getName() + " " +
							mi.getAnchor().getPrimitiveSiteName() + " ;" + nl);
				} else {
					out.append("unplaced  ;" + nl);
				}
			}
			out.append(nl);
		}
	}

	private void writeDesignInstances() throws IOException {
		if (addComments) {
			out.append("#  =======================================================" + nl);
			out.append("#  The syntax for instances is:" + nl);
			out.append("#      instance <name> <sitedef>, placed <tile> <site>, cfg <string> ;" + nl);
			out.append("#  or" + nl);
			out.append("#      instance <name> <sitedef>, unplaced, cfg <string> ;" + nl);
			out.append("# " + nl);
			out.append("#  For typing convenience you can abbreviate instance to inst." + nl);
			out.append("# " + nl);
			out.append("#  For IOs there are two special keywords: bonded and unbonded" + nl);
			out.append("#  that can be used to designate whether the PAD of an unplaced IO is" + nl);
			out.append("#  bonded out. If neither keyword is specified, bonded is assumed." + nl);
			out.append("# " + nl);
			out.append("#  The bonding of placed IOs is determined by the site they are placed in." + nl);
			out.append("# " + nl);
			out.append("#  If you specify bonded or unbonded for an instance that is not an" + nl);
			out.append("#  IOB it is ignored." + nl);
			out.append("# " + nl);
			out.append("#  Shown below are three examples for IOs. " + nl);
			out.append("#     instance IO1 IOB, unplaced ;          # This will be bonded" + nl);
			out.append("#     instance IO1 IOB, unplaced bonded ;   # This will be bonded" + nl);
			out.append("#     instance IO1 IOB, unplaced unbonded ; # This will be unbonded" + nl);
			out.append("#  =======================================================" + nl);
		}

		writeInstances(design.getInstances(), "");
		out.append(nl);
	}

	private void writeDesignNets() throws IOException {
		if (addComments) {
			out.append("#  ================================================" + nl);
			out.append("#  The syntax for nets is:" + nl);
			out.append("#     net <name> <type>," + nl);
			out.append("#       outpin <inst_name> <inst_pin>," + nl);
			out.append("#       ." + nl);
			out.append("#       ." + nl);
			out.append("#       inpin <inst_name> <inst_pin>," + nl);
			out.append("#       ." + nl);
			out.append("#       ." + nl);
			out.append("#       pip <tile> <wire0> <dir> <wire1> , # [<rt>]" + nl);
			out.append("#       ." + nl);
			out.append("#       ." + nl);
			out.append("#       ;" + nl);
			out.append("# " + nl);
			out.append("#  There are three available wire types: wire, power and ground." + nl);
			out.append("#  If no type is specified, wire is assumed." + nl);
			out.append("# " + nl);
			out.append("#  Wire indicates that this a normal wire." + nl);
			out.append("#  Power indicates that this net is tied to a DC power source." + nl);
			out.append("#  You can use \"power\", \"vcc\" or \"vdd\" to specify a power net." + nl);
			out.append("# " + nl);
			out.append("#  Ground indicates that this net is tied to ground." + nl);
			out.append("#  You can use \"ground\", or \"gnd\" to specify a ground net." + nl);
			out.append("# " + nl);
			out.append("#  The <dir> token will be one of the following:" + nl);
			out.append("# " + nl);
			out.append("#     Symbol Description" + nl);
			out.append("#     ====== ==========================================" + nl);
			out.append("#       ==   Bidirectional, unbuffered." + nl);
			out.append("#       =>   Bidirectional, buffered in one direction." + nl);
			out.append("#       =-   Bidirectional, buffered in both directions." + nl);
			out.append("#       ->   Directional, buffered." + nl);
			out.append("# " + nl);
			out.append("#  No pips exist for unrouted nets." + nl);
			out.append("#  ================================================" + nl);
		}

		List<Net> nets = new ArrayList<>(design.getNets());
		Collections.sort(nets, (o1, o2) -> o1.getName().compareTo(o2.getName()));
		for (Net net : nets) {
			out.append("  net \"" + net.getName() + "\" ");
			if (!net.getType().equals(NetType.WIRE))
				out.append(net.getType().name().toLowerCase());
			out.append(",");
			if (net.getAttributes() != null) {
				out.append(" cfg \"");
				writeAttributes(net.getAttributes(), " ");
				out.append("\",");
			}
			out.append(nl);

			writePins(net.getPins(), "  ");
			if (addPips) {
				writePips(net.getPIPs(), "  ");
			}
			out.append("  ;" + nl);
		}
	}

	private void writeDesignSummary() throws IOException {
		if (addComments) {
			int sliceCount = 0;
			int bramCount = 0;
			int dspCount = 0;
			for (Instance instance : design.getInstances()) {
				SiteType type = instance.getType();
				if (Design.sliceTypes.contains(type)) {
					sliceCount++;
				} else if (Design.dspTypes.contains(type)) {
					dspCount++;
				} else if (Design.bramTypes.contains(type)) {
					bramCount++;
				}
			}

			out.append("# =======================================================" + nl);
			out.append("# SUMMARY" + nl);
			out.append("# Number of Module Defs: " + design.getModules().size() + nl);
			out.append("# Number of Module Insts: " + design.getModuleInstances().size() + nl);
			out.append("# Number of Primitive Insts: " + design.getInstances().size() + nl);
			out.append("#     Number of SLICES: " + sliceCount + nl);
			out.append("#     Number of DSP48s: " + dspCount + nl);
			out.append("#     Number of BRAMs: " + bramCount + nl);
			out.append("# Number of Nets: " + design.getNets().size() + nl);
			out.append("# =======================================================" + nl + nl + nl);
		}
	}

	private void writeHMSummary() throws IOException {
		if (addComments) {
			Module mod = design.getHardMacro();
			out.append("# =======================================================" + nl);
			out.append("# MACRO SUMMARY" + nl);
			out.append("# Number of Module Insts: " + Integer.toString(mod.getInstances().size()) + nl);
			Map<SiteType, Integer> instTypeCount = new HashMap<>();
			for (Instance inst : mod.getInstances()) {
				Integer count = instTypeCount.get(inst.getType());
				if (count == null) {
					instTypeCount.put(inst.getType(), 1);
				} else {
					count++;
					instTypeCount.put(inst.getType(), count);
				}
			}

			List<SiteType> types = new ArrayList<>(instTypeCount.keySet());
			Collections.sort(types, (o1, o2) -> o1.name().compareTo(o2.name()));
			for (SiteType type : types) {
				out.append("#   Number of " + type.toString() + "s: " + Integer.toString(instTypeCount.get(type)) + nl);
			}
			out.append("# Number of Module Ports: " + mod.getPorts().size() + nl);
			out.append("# Number of Module Nets: " + mod.getNets().size() + nl);
			out.append("# =======================================================" + nl + nl + nl);
		}
	}

	private void writeAttributes(Collection<Attribute> attrs, String sep) throws IOException {
		List<Attribute> sorted = new ArrayList<>(attrs);
		Collections.sort(sorted, new AttributeComparator());

		for (Attribute attr : sorted) {
			String[] logicalNames = attr.getMultiValueLogicalNames();
			String[] values = attr.getMultiValueValues();
			for (int i = 0; i < values.length; i++) {
				out.append(sep);
				out.append(attr.getPhysicalName());
				out.append(":");
				out.append(logicalNames[i]);
				out.append(":");
				out.append(values[i]);
			}
		}
	}

	private void writePorts(Collection<Port> ports) throws IOException {
		List<Port> sorted = new ArrayList<>(ports);
		Collections.sort(sorted, (o1, o2) -> o1.getName().compareTo(o2.getName()));
		for (Port port : sorted) {
			out.append("  port \"" + port.getName() + "\" \"" + port.getInstanceName() + "\" \"" + port.getPinName() + "\";" + nl);
		}

	}

	private void writeInstances(Collection<Instance> instances, String ind) throws IOException {
		List<Instance> sorted = new ArrayList<>(instances);
		Collections.sort(sorted, (o1, o2) -> o1.getName().compareTo(o2.getName()));
		for (Instance inst : sorted) {
			out.append(ind + "inst \"" + inst.getName() + "\" \"" + inst.getType().name() + "\",");
			if (inst.isPlaced())
				out.append("placed " + inst.getTile().getName() +	" " + inst.getPrimitiveSiteName());
			else
				out.append("unplaced");
			out.append("  ,");
			if (inst.getModuleInstance() != null)
				out.append("module \"" + inst.getModuleInstanceName() + "\" \"" +
						inst.getModuleTemplate().getName() + "\" \"" +
						inst.getModuleTemplateInstance().getName() + "\" ,");
			out.append(nl);

			out.append(ind + "  cfg \"");
			writeAttributes(inst.getAttributes(), " ");
			out.append(" \"" + nl);
			out.append(ind + "  ;" + nl);
		}
	}

	private void writeNets(Collection<Net> nets, String ind) throws IOException {
		List<Net> sorted = new ArrayList<>(nets);
		Collections.sort(sorted, (o1, o2) -> o1.getName().compareTo(o2.getName()));
		for (Net net : sorted) {
			out.append(ind + "net \"" + net.getName() + "\" ,");
			if (net.getAttributes() != null) {
				out.append(" cfg \"");
				writeAttributes(net.getAttributes(), " ");
				out.append("\",");
			}
			out.append(nl);

			writePins(net.getPins(), ind + "  ");
			writePips(net.getPIPs(), ind + "  ");
			out.append(ind + "  ;" + nl);
		}
	}

	private void writePins(Collection<Pin> pins, String ind) throws IOException {
		List<Pin> sorted = new ArrayList<>(pins);
		Collections.sort(sorted, (o1, o2) -> o1.getName().compareTo(o2.getName()));
		for (Pin pin : sorted)
			out.append(ind + pin.getPinType().name().toLowerCase() + " \"" +
					pin.getInstanceName() + "\" " + pin.getName() + " ," + nl);
	}

	private void writePips(Collection<PIP> pips, String ind) throws IOException {
		List<PIP> netPips = new ArrayList<>(pips);
		Collections.sort(netPips, new PIPComparator());
		for (PIP pip : netPips)
			out.append(ind + "pip " + pip.getTile().getName() + " " +
					pip.getStartWireName() + " -> " + pip.getEndWireName() + " ," + nl);
	}

	// Comparators for XDL ordering
	private static class AttributeComparator implements Comparator<Attribute> {
		@Override
		public int compare(Attribute t1, Attribute t2) {
			int cmp = t1.getPhysicalName().compareTo(t2.getPhysicalName());
			if (cmp != 0) return cmp;
			cmp = t1.getLogicalName().compareTo(t2.getLogicalName());
			if (cmp != 0) return cmp;
			return t1.getValue().compareTo(t2.getValue());
		}
	}

	private static class PIPComparator implements Comparator<PIP> {
		@Override
		public int compare(PIP t1, PIP t2) {
			int cmp = t1.getTile().getName().compareTo(t2.getTile().getName());
			if (cmp != 0) return cmp;
			cmp = t1.getStartWireName().compareTo(t2.getStartWireName());
			if (cmp != 0) return cmp;
			return t1.getEndWireName().compareTo(t2.getEndWireName());
		}
	}
}
