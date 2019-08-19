/*
 * Copyright (c) 2016 Brigham Young University
 *
 * This file is part of the BYU RapidSmith Tools.
 *
 * BYU RapidSmith Tools is free software: you may redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * BYU RapidSmith Tools is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * A copy of the GNU General Public License is included with the BYU
 * RapidSmith Tools. It can be found at doc/LICENSE.GPL3.TXT. You may
 * also get a copy of the license at <http://www.gnu.org/licenses/>.
 */

package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.interfaces.StaticResourcesInterface;
import edu.byu.ece.rapidSmith.util.Exceptions;

/**
 * This class is used to interface Vivado and RapidSmith2. 
 * It parses RSCP checkpoints and creates equivalent RapidSmith {@link CellDesign}s.
 * It can also create TINCR checkpoints from existing RapidSmith {@link CellDesign}s.
 */
public final class VivadoInterface {

	private static final String CELL_LIBRARY_NAME = "cellLibrary.xml";

	/* Design Import */

	public static VivadoCheckpoint loadRSCP(String rscp) throws IOException {
		return loadRSCP(rscp, false);
	}

	/**
	 * Creates a route tree, starting from an input site pin and ending at a BelPin.
	 * Used for pesudo VCC pins.
	 * @param net
	 * @param sinkPin
	 */
	private static void createSinkRouteTree(CellNet net, BelPin sinkPin) {
		// The BelPin must be a sink Lut BEL Pin
		// TODO: Fix getWiresInNode for site wires.
		// TODO: Add some safety checks.
		Wire lutPinWire = sinkPin.getWire().getReverseWireConnections().iterator().next().getSinkWire();
		SitePin sitePin = lutPinWire.getReverseConnectedPin();

		RouteTree rt = new RouteTree(sitePin.getInternalWire());
		rt.connect(lutPinWire.getWireConnections().iterator().next());

		net.addSinkRouteTree(sitePin, rt);
		net.addSinkRouteTree(sinkPin, rt);
	}

	private static void addPseudoVccPins(VivadoCheckpoint vivadoCheckpoint) {
		CellDesign design = vivadoCheckpoint.getDesign();
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
					createSinkRouteTree(vccNet, belPin);
					break;
				case "SRLC16E":
				case "SRL16E":
					CellPin a1pin = cell.attachPseudoPin("pseudoA1", PinDirection.IN);
					belPin = bel.getBelPin("A1");
					a1pin.mapToBelPin(belPin);
					vccNet.connectToPin(a1pin);
					createSinkRouteTree(vccNet, belPin);
					CellPin a6pin = cell.attachPseudoPin("pseudoA6", PinDirection.IN);
					belPin = bel.getBelPin("A6");
					a6pin.mapToBelPin(belPin);
					vccNet.connectToPin(a6pin);
					createSinkRouteTree(vccNet, belPin);
					break;
				case "RAMS32":
				case "RAMD32":
					CellPin wa6pin = cell.attachPseudoPin("pseudoWA6", PinDirection.IN);
					belPin = bel.getBelPin("WA6");
					wa6pin.mapToBelPin(belPin);
					vccNet.connectToPin(wa6pin);
					createSinkRouteTree(vccNet, belPin);
					a6pin = cell.attachPseudoPin("pseudoA6", PinDirection.IN);
					belPin = bel.getBelPin("A6");
					a6pin.mapToBelPin(belPin);
					vccNet.connectToPin(a6pin);
					createSinkRouteTree(vccNet, belPin);
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
					createSinkRouteTree(vccNet, belPin);
				}
			}

			if (cell.getType().equals("SRLC16E") || cell.getType().equals("SRL16E")) {
				CellPin pin = cell.attachPseudoPin("pseudoA1", PinDirection.IN);
				BelPin belPin = bel.getBelPin("A1");
				pin.mapToBelPin(belPin);
				vccNet.connectToPin(pin);
				createSinkRouteTree(vccNet, belPin);
			}

		}

		// We have added pins, so we need to recalculate the route status
        vccNet.computeRouteStatus();
	}

	public static VivadoCheckpoint loadRSCP (String rscp, boolean storeAdditionalInfo) throws IOException {
		return loadRSCP(rscp, storeAdditionalInfo, false);
	}

	/**
	 * Parses a RSCP generated from Tincr, and creates an equivalent RapidSmith2 design.
	 * @param rscp Path to the RSCP to import
	 * @param storeAdditionalInfo
	 * @param addPseudoVccPins Whether to detect and add pseudo VCC pins based off of the placement.
	 * @return
	 * @throws IOException
	 */
	public static VivadoCheckpoint loadRSCP (String rscp, boolean storeAdditionalInfo, boolean addPseudoVccPins) throws IOException {
		Path rscpPath = Paths.get(rscp);
		
		if (!rscpPath.getFileName().toString().endsWith(".rscp")) {
			throw new AssertionError("Specified directory is not a RSCP. The directory should end in \".rscp\"");
		}
					
		// load the device
		DesignInfoInterface designInfo = new DesignInfoInterface();
		designInfo.parse(rscpPath);
		String partName = designInfo.getPart();
		ImplementationMode mode = designInfo.getMode();
		if (partName == null) {
			throw new Exceptions.ParseException("Part name for the design not found in the design.info file!");
		}
		
		Device device = RSEnvironment.defaultEnv().getDevice(partName);
		
		if (device == null) {
			throw new Exceptions.EnvironmentException("Device files for part: " + partName + " cannot be found.");
		}

		// load the cell library
		CellLibrary libCells = new CellLibrary(RSEnvironment.defaultEnv()
				.getPartFolderPath(partName)
				.resolve(CELL_LIBRARY_NAME));
		
		// add additional macro cell specifications to the cell library before parsing the EDIF netlist
		libCells.loadMacroXML(rscpPath.resolve("macros.xml"));
		
		// create the RS2 netlist
		String edifFile = rscpPath.resolve("netlist.edf").toString();
		VivadoEdifInterface vivadoEdifInterface = new VivadoEdifInterface();
		CellDesign design = vivadoEdifInterface.parseEdif(edifFile, libCells, partName);
		design.setImplementationMode(mode);
		
		// parse the constraints into RapidSmith
		String constraintsFile = rscpPath.resolve("constraints.xdc").toString();
		XdcConstraintsInterface constraintsInterface = new XdcConstraintsInterface(design, device);
		constraintsInterface.parseConstraintsXDC(constraintsFile);

		// re-create the placement and routing information
		String placementFile = rscpPath.resolve("placement.rsc").toString();
		XdcPlacementInterface placementInterface = new XdcPlacementInterface(design, device);
		placementInterface.parsePlacementXDC(placementFile);

		String routingFile = rscpPath.resolve("routing.rsc").toString();
		XdcRoutingInterface routingInterface = new XdcRoutingInterface(design, device, placementInterface.getPinMap(), libCells);
		routingInterface.parseRoutingXDC(routingFile);
		design.setPartPinMap(routingInterface.getPartPinMap());

		VivadoCheckpoint vivadoCheckpoint = new VivadoCheckpoint(partName, design, device, libCells); 
		
		if (storeAdditionalInfo) {
			vivadoCheckpoint.setRoutethroughBels(routingInterface.getRoutethroughsBels());
			vivadoCheckpoint.setVccSourceBels(routingInterface.getVccSourceBels());
			vivadoCheckpoint.setGndSourceBels(routingInterface.getGndSourceBels());
			vivadoCheckpoint.setBelPinToCellPinMap(placementInterface.getPinMap());
			addPseudoCells(vivadoCheckpoint, device, design, libCells);
		}

		if (addPseudoVccPins) {
			// Detect and add pseudo VCC pins based off of the placement.
			addPseudoVccPins(vivadoCheckpoint);
		}

		// Mark the used static resources
		if (mode == ImplementationMode.RECONFIG_MODULE) {
			String resourcesFile = rscpPath.resolve("static.rsc").toString();
			StaticResourcesInterface staticInterface = new StaticResourcesInterface(design, device);
			staticInterface.parseResourcesRSC(resourcesFile);
			design.setRmStaticNetMap(staticInterface.getRmStaticNetMap());
			design.setStaticRouteStringMap(staticInterface.getStaticRouteStringMap());
		}
		
		return vivadoCheckpoint;
	}

	private static void addPseudoCells(VivadoCheckpoint vivadoCheckpoint, Device device, CellDesign design, CellLibrary libCells) {
		// Create pseudo cells for the routethroughs and static source BELs
		//for (Bel bel : vivadoCheckpoint.getBelRoutethroughs()) {

		//}

		for (Bel bel : vivadoCheckpoint.getVccSourceBels()) {
			// assuming LUT Bel
			Cell cell = new Cell("Pseudo_" + bel.getSite().getName() + "_" + bel.getName(), libCells.get("LUT1"), true);
			design.addCell(cell);
			design.placeCell(cell, bel);
		}

		for (Bel bel : vivadoCheckpoint.getGndSourceBels()) {
			// assuming LUT Bel
			Cell cell = new Cell("Pseudo_" + bel.getSite().getName() + "_" + bel.getName(), libCells.get("LUT1"), true);
			design.addCell(cell);
			design.placeCell(cell, bel);
		}

		List<String> ffBels = new ArrayList<>(Arrays.asList("D5FF", "DFF", "C5FF", "CFF", "B5FF", "BFF", "A5FF", "AFF"));

		// We must create pseudo cells for inferred latches, but not lut routethroughs since they are handled
		// differently at the moment
		for (BelRoutethrough belRoutethrough : vivadoCheckpoint.getRoutethroughObjects()) {
			Bel bel = belRoutethrough.getBel();
			Site site = bel.getSite();

			if (ffBels.contains(bel.getName())) {
				Cell cell = new Cell("Pseudo_" + site.getName() + "_" + bel.getName(), libCells.get("FDRE"), true);
				design.addCell(cell);

				// Don't assign anything to the D, Q, or CE pins since they should be handled within the site.
				// The CLK will come into the site at the clock pin. For routers to know to route to this pin, a
				// pseudo cell pin must be added to the cell.

				// Note: I cannot check the CLK site PIP to see if it is used and which nets are involved.
				// Additionally, the RSCP does not report that the clk pin is used. So, am I forced to resort
				// to assume VCC is coming into the CLK pin, and is then inverted at the SITE PIP, brining GND
				// to the FF BELs.
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

	/* Design Export */

	/**
	 * Removes all static source LUTs from the design. These LUTs are implied and should not be included
	 * in the EDIF netlist or be placed.
	 */
	private static void removeStaticSourceLUTs(CellDesign design) {
		for (Cell staticSource : design.getCells().stream()
				.filter(Cell::isLut)
				.filter(c -> c.getPin("O").getNet() != null && c.getPin("O").getNet().isStaticNet())
				.collect(Collectors.toList())) {
			design.removeCell(staticSource);
		}
	}

	private static void removePseudoLuts(CellDesign design) {
		for (Cell staticSource : design.getCells().stream()
				.filter(Cell::isPseudo)
				.collect(Collectors.toList())) {
			design.removeCell(staticSource);
		}
	}

	/**
	 * Export the RapidSmith2 design into an existing TINCR checkpoint file.
	 *
	 * @param tcpDirectory TINCR checkpoint directory to write XDC files to
	 * @param design CellDesign to convert to a TINCR checkpoint
	 * @throws IOException
	 */
	public static void writeTCP(String tcpDirectory, CellDesign design, Device device, CellLibrary libCells) throws IOException {
		writeTCP(tcpDirectory, design, device, libCells, false);
	}

	/**
	 * Export the RapidSmith2 design into an existing TINCR checkpoint file. 
	 *   
	 * @param tcpDirectory TINCR checkpoint directory to write XDC files to
	 * @param design CellDesign to convert to a TINCR checkpoint
	 * @param intrasiteRouting Whether to include commands to manually set intrasite routing in Vivado
	 * @throws IOException
	 */
	public static void writeTCP(String tcpDirectory, CellDesign design, Device device, CellLibrary libCells, boolean intrasiteRouting) throws IOException {
		new File(tcpDirectory).mkdir();

		removePseudoLuts(design);

		// Remove static-source LUTs
		removeStaticSourceLUTs(design);
		
		// insert routethrough buffers
		LutRoutethroughInserter inserter = new LutRoutethroughInserter(design, libCells);
		inserter.execute();
		
		// Write placement.xdc
		String placementOut = Paths.get(tcpDirectory, "placement.xdc").toString();	
		XdcPlacementInterface placementInterface = new XdcPlacementInterface(design, device);
		placementInterface.writePlacementXDC(placementOut);
		
		// Write routing.xdc
		String routingOut = Paths.get(tcpDirectory, "routing.xdc").toString();
		XdcRoutingInterface routingInterface = new XdcRoutingInterface(design, device);
		if (design.getImplementationMode().equals(ImplementationMode.RECONFIG_MODULE)) {
			String partpinRoutingOut = Paths.get(tcpDirectory, "partpin_routing.xdc").toString();
			routingInterface.writeRoutingXDC(routingOut, partpinRoutingOut, design, intrasiteRouting);
		} else {
			routingInterface.writeRoutingXDC(routingOut, design, intrasiteRouting);
		}

		// Write EDIF netlist
		String edifOut = Paths.get(tcpDirectory, "netlist.edf").toString();
		VivadoEdifInterface vivadoEdifInterface = new VivadoEdifInterface();
		vivadoEdifInterface.writeEdif(edifOut, design);

		// write constraints.xdc
		String constraintsOut = Paths.get(tcpDirectory, "constraints.xdc").toString();
		XdcConstraintsInterface constraintsInterface = new XdcConstraintsInterface(design, device);
		constraintsInterface.writeConstraintsXdc(constraintsOut);

		// write design.info
		String partInfoOut = Paths.get(tcpDirectory, "design.info").toString();
		DesignInfoInterface.writeInfoFile(partInfoOut, design.getPartName());
	}
} // END CLASS 
