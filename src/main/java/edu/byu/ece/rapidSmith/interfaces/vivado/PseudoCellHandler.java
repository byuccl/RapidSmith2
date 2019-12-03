package edu.byu.ece.rapidSmith.interfaces.vivado;

import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;

import java.util.*;
import java.util.stream.Collectors;

public class PseudoCellHandler {
	private final CellDesign design;
	private final Device device;
	private final CellLibrary libCells;

	public PseudoCellHandler(CellDesign design, Device device, CellLibrary libCells) {
		this.design = design;
		this.device = device;
		this.libCells = libCells;
	}


	/**
	 * Creates a route tree, starting from an input site pin and ending at a BelPin.
	 * Used for pseudo VCC pins.
	 * @param net the vcc net
	 * @param sinkPin
	 */
	private void createPseudoVccSinkTree(CellNet net, BelPin sinkPin) {
		// The BelPin must be a sink Lut BEL Pin
		Wire lutPinWire = sinkPin.getWire().getReverseWireConnections().iterator().next().getSinkWire();
		SitePin sitePin = lutPinWire.getReverseConnectedPin();
		RouteTree rt = new RouteTree(sitePin.getInternalWire());
		rt.connect(lutPinWire.getWireConnections().iterator().next());
		net.addSinkRouteTree(sitePin, rt);
		net.addSinkRouteTree(sinkPin, rt);
	}

	/**
	 * Removes pseudo lut cells from the design. Pseudo cells are only used when pseudo pins are needed for
	 * physical routing.
	 * @param design the design
	 */
	void removePseudoLuts() {
		for (Cell staticSource : design.getCells().stream()
				.filter(Cell::isPseudo)
				.collect(Collectors.toList())) {
			design.removeCell(staticSource);
		}
	}

	/**
	 * Searches the design and adds pseudo cell pins for physical-only vcc routes that need to be made.
	 */
	void addPseudoVccPins() {
		//CellDesign design = vivadoCheckpoint.getDesign();
		CellNet vccNet = design.getVccNet();

		// If a 5LUT Bel and a 6LUT Bel are both used, we must tie A6 to VCC
		// Get ALL used LUT bels (including bels with no logical counterpart)
		Collection<Bel> usedLut6Bels = design.getUsedBels().stream()
				.filter(bel -> bel.getName().matches("[A-D]6LUT")).collect(Collectors.toList());
		Collection<Bel> usedLut5Bels = design.getUsedBels().stream()
				.filter(bel -> bel.getName().matches("[A-D]5LUT")).collect(Collectors.toList());

		for (Bel bel : usedLut6Bels) {
			Cell cell = design.getCellAtBel(bel);
			assert (cell != null);

			switch (cell.getType()) {
				case "SRLC32E":
					CellPin pin = cell.attachPseudoPin("pseudoA1", PinDirection.IN);
					BelPin belPin = bel.getBelPin("A1");
					pin.mapToBelPin(belPin);
					vccNet.connectToPin(pin);
					createPseudoVccSinkTree(vccNet, belPin);
					break;
				case "SRLC16E":
				case "SRL16E":
					CellPin a1pin = cell.attachPseudoPin("pseudoA1", PinDirection.IN);
					belPin = bel.getBelPin("A1");
					a1pin.mapToBelPin(belPin);
					vccNet.connectToPin(a1pin);
					createPseudoVccSinkTree(vccNet, belPin);

					CellPin a6pin = cell.attachPseudoPin("pseudoA6", PinDirection.IN);
					belPin = bel.getBelPin("A6");
					a6pin.mapToBelPin(belPin);
					vccNet.connectToPin(a6pin);
					createPseudoVccSinkTree(vccNet, belPin);
					break;
				case "RAMS32":
				case "RAMD32":
					CellPin wa6pin = cell.attachPseudoPin("pseudoWA6", PinDirection.IN);
					belPin = bel.getBelPin("WA6");
					wa6pin.mapToBelPin(belPin);
					vccNet.connectToPin(wa6pin);
					createPseudoVccSinkTree(vccNet, belPin);

					a6pin = cell.attachPseudoPin("pseudoA6", PinDirection.IN);
					belPin = bel.getBelPin("A6");
					a6pin.mapToBelPin(belPin);
					vccNet.connectToPin(a6pin);
					createPseudoVccSinkTree(vccNet, belPin);
					break;
				default:
					break;
			}
		}

		for (Bel bel : usedLut5Bels) {
			Cell cell = design.getCellAtBel(bel);
			assert (cell != null);

			// Check to see if both the LUT6 and LUT5 BEL are used
			Bel lut6Bel = bel.getSite().getBel(bel.getName().charAt(0) + "6LUT");
			Cell lut6Cell = design.getCellAtBel(lut6Bel);
			if (usedLut6Bels.contains(lut6Bel)) {
				BelPin belPin = lut6Bel.getBelPin("A6");

				boolean macroPseudoPin = false;
				// Pseudo pins may have already been created for macro cells
				// TODO: Get rid of this duplication of efforts.
				for (CellPin pseudoPin : lut6Cell.getPseudoPins()) {
					if (pseudoPin.getMappedBelPin().equals(belPin))
						macroPseudoPin = true;
				}

				if (!macroPseudoPin) {
					CellPin pin = lut6Cell.attachPseudoPin("pseudoA6", PinDirection.IN);

					// Assume that vcc can be routed to this pin.
					pin.mapToBelPin(belPin);
					vccNet.connectToPin(pin);
					createPseudoVccSinkTree(vccNet, belPin);
				}
			}

			if (cell.getType().equals("SRLC16E") || cell.getType().equals("SRL16E")) {
				CellPin pin = cell.attachPseudoPin("pseudoA1", PinDirection.IN);
				BelPin belPin = bel.getBelPin("A1");
				pin.mapToBelPin(belPin);
				vccNet.connectToPin(pin);
				createPseudoVccSinkTree(vccNet, belPin);
			}

		}

		// We have added pins, so we need to recalculate the route status
		vccNet.computeRouteStatus();
	}

	/**
	 * Adds pseudo cells to the design for route-through LUTs, implied latches, and static source LUTs.
	 */
	void addPseudoCells(Set<Bel> vccBels, Set<Bel> gndBels, Collection<BelRoutethrough> routethroughs) {
		// Create pseudo cells for the route-throughs and static source BELs
		for (Bel bel : vccBels) {
			// assuming LUT Bel
			Cell cell = new Cell("Pseudo_" + bel.getSite().getName() + "_" + bel.getName(), libCells.get("LUT1"), true);
			design.addCell(cell);
			design.placeCell(cell, bel);
		}

		for (Bel bel : gndBels) {
			// assuming LUT Bel
			Cell cell = new Cell("Pseudo_" + bel.getSite().getName() + "_" + bel.getName(), libCells.get("LUT1"), true);
			design.addCell(cell);
			design.placeCell(cell, bel);
		}

		List<String> ffBels = new ArrayList<>(Arrays.asList("D5FF", "DFF", "C5FF", "CFF", "B5FF", "BFF", "A5FF", "AFF"));

		for (BelRoutethrough belRoutethrough : routethroughs) {
			Bel bel = belRoutethrough.getBel();
			Site site = bel.getSite();

			if (ffBels.contains(bel.getName())) {
				Cell cell = new Cell("Pseudo_" + site.getName() + "_" + bel.getName(), libCells.get("FDRE"), true);
				design.addCell(cell);

				// Don't assign anything to the D, Q, or CE pins since they should be handled within the site.
				// The CLK will come into the site at the clock pin. For routers to know to route to this pin, a
				// pseudo cell pin must be added to the cell. Note: I cannot check the CLK site PIP to see if it is
				// used and which nets are involved. Additionally, the RSCP does not report that the clk pin is used.
				// So, am I forced to resort to assume VCC is coming into the CLK pin, and is then inverted at the
				// SITE PIP, bringing GND to the FF BELs.
				PseudoCellPin pseudoCK = new PseudoCellPin("pseudoCK", PinDirection.IN);
				cell.attachPseudoPin(pseudoCK);
				design.placeCell(cell, bel);
				BelPin belPin = bel.getBelPin("CK");
				pseudoCK.mapToBelPin(bel.getBelPin("CK"));
				design.getVccNet().connectToPin(pseudoCK);

				// Add a stand-in route-tree connecting the cell-pin sink and the sitepin.
				String namePrefix = "intrasite:" + site.getType().name() + "/";
				RouteTree routeTree = new RouteTree(site.getWire(namePrefix + "CLK.CLK"));
				design.getVccNet().addSinkRouteTree(belPin, routeTree);

			} else {
				// assuming LUT Bel
				Cell cell = new Cell("Pseudo_" + bel.getSite().getName() + "_" + bel.getName(), libCells.get("LUT1"), true);
				design.addCell(cell);
				design.placeCell(cell, bel);
			}
		}
	}
}
