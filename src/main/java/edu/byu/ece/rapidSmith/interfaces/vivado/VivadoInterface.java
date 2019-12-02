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

		PseudoCellHandler pseudoCellHandler = new PseudoCellHandler(design, device, libCells);
		if (storeAdditionalInfo) {
			Set<Bel> vccBels = routingInterface.getVccSourceBels();
			Set<Bel> gndBels = routingInterface.getGndSourceBels();
			vivadoCheckpoint.setRoutethroughBels(routingInterface.getRoutethroughsBels());
			vivadoCheckpoint.setVccSourceBels(vccBels);
			vivadoCheckpoint.setGndSourceBels(gndBels);
			vivadoCheckpoint.setBelPinToCellPinMap(placementInterface.getPinMap());
			pseudoCellHandler.addPseudoCells(vccBels, gndBels, vivadoCheckpoint.getRoutethroughObjects());
		}

		if (addPseudoVccPins) {
			// Detect and add pseudo VCC pins based off of the placement.
			pseudoCellHandler.addPseudoVccPins();
		}

		// Mark the used static resources
		if (mode == ImplementationMode.RECONFIG_MODULE) {
			String resourcesFile = rscpPath.resolve("static.rsc").toString();
			StaticResourcesInterface staticInterface = new StaticResourcesInterface(design, device, routingInterface.getRoutethroughsBels());
			staticInterface.parseResourcesRSC(resourcesFile);
			design.setRmStaticNetMap(staticInterface.getRmStaticNetMap());
			design.setStaticRouteStringMap(staticInterface.getStaticRouteStringMap());
			vivadoCheckpoint.setStaticPips(staticInterface.getStaticPips());
			vivadoCheckpoint.setRoutethroughBels(staticInterface.getBelRoutethroughMap());
		}


		return vivadoCheckpoint;
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

		PseudoCellHandler pseudoCellHandler = new PseudoCellHandler(design, device, libCells);
		pseudoCellHandler.removePseudoLuts();

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
