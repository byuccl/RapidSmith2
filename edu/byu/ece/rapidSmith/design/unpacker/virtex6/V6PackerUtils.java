package edu.byu.ece.rapidSmith.design.unpacker.virtex6;

import edu.byu.ece.rapidSmith.design.*;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.LibraryCell;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.design.unpacker.PackerUtils;
import edu.byu.ece.rapidSmith.device.*;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class V6PackerUtils implements PackerUtils {

	@Override
	public void prepare(CellDesign design) {
		for (Cell cell : design.getCells()) {
			LibraryCell libCell = cell.getLibCell();
			if (libCell.getName().equals("FF_INIT") && cell.getAnchor().getName().matches("[A-D]FF")) {
				Property property = cell.getProperty(libCell.getName());
				assert property != null;
				property.setValue("#FF");
			}
		}
	}

	@Override
	public void finish(Design design) {
		Device device = design.getDevice();

		handleRouteThroughs(design, device);
		handleStatics(design);
		createIONets(design);
		mergeDMuxNetsIntoCoutNets(design);
	}

	private void createIONets(Design design) {
		for (Instance inst : design.getInstances()) {
			if (design.getDevice().getIOBTypes().contains(inst.getType())) {
				String padName = inst.getAttribute("PAD").getLogicalName();
				Net net = new Net(inst.getName(), NetType.WIRE);
				// TODO should this be the instance name or the pad name?
				// logical_name definitely uses the inst name
				// value doesn't seem to matter, maybe its pcf related?
				net.addAttribute("_BELSIG", "PAD,PAD," + inst.getName(), inst.getName());
				design.addNet(net);
			}
		}
	}

	private void handleStatics(Design design) {
		for (Instance inst : design.getInstances()) {
			switch (inst.getType()) {
				case SLICEL :
				case SLICEM :
					for (char ch = 'A'; ch <= 'D'; ch++) {
						String lutName = ch + "6LUT";
						String usedMuxName = ch + "USED";
						if (lutIsStaticSource(inst, ch) && appropriateStaticRouting(inst, ch)) {
							boolean isVcc = inst.getAttributeValue(lutName).endsWith("1");
							inst.removeAttribute(lutName);
							inst.removeAttribute(usedMuxName);

							inst.addAttribute((isVcc ? "_VCC" : "_GND") + "_SOURCE", "", "" + ch);
						}
					}
					boolean noUserLogic = true;
					for (Attribute attr : inst.getAttributes()) {
						if (!attr.getPhysicalName().matches("_(VCC|GND)_SOURCE")) {
							noUserLogic = false;
							break;
						}
					}
					if (noUserLogic)
						inst.addAttribute("_NO_USER_LOGIC", "", "");
					break;
				case TIEOFF :
					inst.addAttribute("_NO_USER_LOGIC", "", "");
					if (inst.hasAttribute("HARD0GND")) {
						inst.removeAttribute("HARD0GND");
						inst.addAttribute("_GND_SOURCE", "", "HARD0");
					}

					if (inst.hasAttribute("HARD1VCC")) {
						inst.removeAttribute("HARD1VCC");
						inst.addAttribute("_VCC_SOURCE", "", "HARD1");
					}
					break;
			}
		}
	}

	private boolean lutIsStaticSource(Instance inst, char le) {
		String lut5Name = le + "5LUT";
		String lut6Name = le + "6LUT";
		String staticEqnPattern = "#LUT:O6=[01]";
		boolean lut5Unused = !inst.hasAttribute(lut5Name) ||
				inst.getAttributeValue(lut5Name).equals("#OFF");
		boolean lut6IsStatic = inst.hasAttribute(lut6Name) &&
				inst.getAttributeValue(lut6Name).matches(staticEqnPattern);

		return lut5Unused && lut6IsStatic;
	}

	private boolean appropriateStaticRouting(Instance inst, char le) {
		String oUsedMuxName = le + "USED";
		String f7MuxName = "F7" + (le == 'A' || le == 'B' ? "A" : "B") + "MUX";
		String outMuxName = le + "OUTMUX";
		String ffMuxName = le + "FFMUX";

		boolean oUsedMuxUsed = inst.hasAttribute(oUsedMuxName) && inst.getAttributeValue(oUsedMuxName).equals("0");
		boolean f7MuxUsed = inst.hasAttribute(f7MuxName) && !inst.getAttributeValue(f7MuxName).equals("#OFF");
		boolean outMuxUsed = inst.hasAttribute(outMuxName) && inst.getAttributeValue(outMuxName).equals("O6");
		boolean ffMuxUsed = inst.hasAttribute(ffMuxName) && inst.getAttributeValue(ffMuxName).equals("O6");

		return oUsedMuxUsed && !f7MuxUsed && !outMuxUsed && !ffMuxUsed;
	}

	private void handleRouteThroughs(Design design, Device device) {
		for (Net net : design.getNets()) {
			for (PIP pip : net.getPIPs()) {
				RouteThrough rt = getRouteThrough(device, pip);
				if (rt != null) {
					Site site = rt.site;
					Instance inst = design.getInstanceAtPrimitiveSite(site);
					if (inst == null) {
						inst = new Instance("XDL_DUMMY_" + site.getTile().getName() +
								"_" + site.getName(), rt.rt.getType());
						design.addInstance(inst);
						inst.place(site);
						inst.addAttribute("_NO_USER_LOGIC", "", "");
					}
					String inPin = rt.rt.getInPin();
					if (inPin.equals("DDLY")) {
						System.out.println(rt.rt.getType());
					}
					inst.addAttribute("_ROUTETHROUGH", inPin, rt.rt.getOutPin());
				}
			}
		}
	}

	private RouteThrough getRouteThrough(Device device, PIP pip) {
		if (device.isRouteThrough(pip)) {
			PIPRouteThrough rt = device.getRouteThrough(pip);
			for (Site site : pip.getTile().getPrimitiveSites()) {
				for (SitePin sitePin : site.getSourcePins()) {
					if (sitePin.getExternalWire().equals(pip.getEndWire())) {
						return new RouteThrough(site, rt);
					}
				}
			}
		}
		return null;
	}

	private void mergeDMuxNetsIntoCoutNets(Design design) {
		for (Instance inst : design.getInstances()) {
			if (sharesCarryChainWithDMUX(inst)) {
				Pin coutPin = inst.getPin("COUT");
				Pin dmuxPin = inst.getPin("DMUX");
				Attribute dmuxout = inst.getAttribute("DOUTMUX");
				// Get the sink pins on the DMUX net
				List<Pin> toMove = new ArrayList<>();
				for (Pin pin : dmuxPin.getNet().getPins()) {
					if (pin != dmuxPin) {
						toMove.add(pin);
					}
				}

				// move all of the DMUX pins into the COUT net
				for (Pin pin : toMove) {
					dmuxPin.getNet().removePin(pin);
					coutPin.getNet().addPin(pin);
				}

				design.removeNet(dmuxPin.getNet());
				dmuxPin.detachInstance();
				dmuxout.setValue("#OFF");
			}
		}
	}

	private boolean sharesCarryChainWithDMUX(Instance inst) {
		if (inst.getType() == SiteType.SLICEL || inst.getType() == SiteType.SLICEM) {
			Pin coutPin = inst.getPin("COUT");
			if (coutPin != null && coutPin.getNet() != null) {
				Pin dmuxPin = inst.getPin("DMUX");
				if (dmuxPin != null && dmuxPin.getNet() != null) {
					Attribute doutmux = inst.getAttribute("DOUTMUX");
					return doutmux != null && doutmux.getValue().equals("CY");
				}
			}
		}

		return false;
	}

	private static class RouteThrough {
		public Site site;
		public PIPRouteThrough rt;

		public RouteThrough(Site site, PIPRouteThrough rt) {
			this.site = site;
			this.rt = rt;
		}
	}
}
