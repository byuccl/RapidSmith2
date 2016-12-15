package edu.byu.ece.rapidSmith.interfaces.ise;

import edu.byu.ece.rapidSmith.design.*;
import edu.byu.ece.rapidSmith.design.xdl.*;
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

	private XdlDesign design;
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

	public void writeXDL(XdlDesign design, Path file) throws IOException {
		try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			writeXDL(design, writer);
		}
	}

	public void writeXDL(XdlDesign design, Writer out) throws IOException {
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

			List<XdlModule> modules = new ArrayList<>(design.getModules());
			modules.sort(Comparator.comparing(XdlModule::getName));
			for (XdlModule module : modules) {
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

			List<XdlModuleInstance> moduleInstanceNames = new ArrayList<>(design.getModuleInstances());
			moduleInstanceNames.sort(Comparator.comparing(XdlModuleInstance::getName));
			for (XdlModuleInstance mi : moduleInstanceNames) {
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

		List<XdlNet> nets = new ArrayList<>(design.getNets());
		nets.sort(Comparator.comparing(XdlNet::getName));
		for (XdlNet net : nets) {
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
			for (XdlInstance instance : design.getInstances()) {
				SiteType type = instance.getType();
				if (XdlDesign.sliceTypes.contains(type)) {
					sliceCount++;
				} else if (XdlDesign.dspTypes.contains(type)) {
					dspCount++;
				} else if (XdlDesign.bramTypes.contains(type)) {
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
			XdlModule mod = design.getHardMacro();
			out.append("# =======================================================" + nl);
			out.append("# MACRO SUMMARY" + nl);
			out.append("# Number of Module Insts: " + Integer.toString(mod.getInstances().size()) + nl);
			Map<SiteType, Integer> instTypeCount = new HashMap<>();
			for (XdlInstance inst : mod.getInstances()) {
				Integer count = instTypeCount.get(inst.getType());
				if (count == null) {
					instTypeCount.put(inst.getType(), 1);
				} else {
					count++;
					instTypeCount.put(inst.getType(), count);
				}
			}

			List<SiteType> types = new ArrayList<>(instTypeCount.keySet());
			types.sort(Comparator.comparing(SiteType::name));
			for (SiteType type : types) {
				out.append("#   Number of " + type.toString() + "s: " + Integer.toString(instTypeCount.get(type)) + nl);
			}
			out.append("# Number of Module Ports: " + mod.getPorts().size() + nl);
			out.append("# Number of Module Nets: " + mod.getNets().size() + nl);
			out.append("# =======================================================" + nl + nl + nl);
		}
	}

	private void writeAttributes(Collection<XdlAttribute> attrs, String sep) throws IOException {
		List<XdlAttribute> sorted = new ArrayList<>(attrs);
		sorted.sort(new AttributeComparator());

		for (XdlAttribute attr : sorted) {
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

	private void writePorts(Collection<XdlPort> ports) throws IOException {
		List<XdlPort> sorted = new ArrayList<>(ports);
		sorted.sort(Comparator.comparing(XdlPort::getName));
		for (XdlPort port : sorted) {
			out.append("  port \"" + port.getName() + "\" \"" + port.getInstanceName() + "\" \"" + port.getPinName() + "\";" + nl);
		}

	}

	private void writeInstances(Collection<XdlInstance> instances, String ind) throws IOException {
		List<XdlInstance> sorted = new ArrayList<>(instances);
		sorted.sort(Comparator.comparing(XdlInstance::getName));
		for (XdlInstance inst : sorted) {
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

	private void writeNets(Collection<XdlNet> nets, String ind) throws IOException {
		List<XdlNet> sorted = new ArrayList<>(nets);
		sorted.sort(Comparator.comparing(XdlNet::getName));
		for (XdlNet net : sorted) {
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

	private void writePins(Collection<XdlPin> pins, String ind) throws IOException {
		List<XdlPin> sorted = new ArrayList<>(pins);
		sorted.sort(Comparator.comparing(XdlPin::getName));
		for (XdlPin pin : sorted)
			out.append(ind + pin.getPinType().name().toLowerCase() + " \"" +
					pin.getInstanceName() + "\" " + pin.getName() + " ," + nl);
	}

	private void writePips(Collection<PIP> pips, String ind) throws IOException {
		List<PIP> netPips = new ArrayList<>(pips);
		netPips.sort(new PIPComparator());
		for (PIP pip : netPips)
			out.append(ind + "pip " + pip.getTile().getName() + " " +
					pip.getStartWire().getWireName() + " -> " + pip.getEndWire().getWireName() + " ," + nl);
	}

	// Comparators for XDL ordering
	private static class AttributeComparator implements Comparator<XdlAttribute> {
		@Override
		public int compare(XdlAttribute t1, XdlAttribute t2) {
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
			cmp = t1.getStartWire().getWireName().compareTo(t2.getStartWire().getWireName());
			if (cmp != 0) return cmp;
			return t1.getEndWire().getWireName().compareTo(t2.getEndWire().getWireName());
		}
	}
}
