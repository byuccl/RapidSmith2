/*
 * Copyright (c) 2019 Brigham Young University
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

package edu.byu.ece.rapidSmith.interfaces.yosys;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.interfaces.StaticResourcesInterface;
import edu.byu.ece.rapidSmith.interfaces.vivado.*;
import edu.byu.ece.rapidSmith.util.Exceptions;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class is used to interface Yosys and RapidSmith2.
 * It parses RSCP checkpoints containing Yosys-produced EDIFs and creates equivalent RapidSmith {@link CellDesign}s.
 */
public final class YosysInterface {
	private static final String CELL_LIBRARY_NAME = "cellLibrary.xml";


	public static VivadoCheckpoint loadRSCP(String rscp) throws IOException {
		return loadRSCP(rscp, false);
	}

	/**
	 * Parses a RSCP containing a Yosys EDIF, and creates an equivalent RapidSmith2 design.
	 * @param rscp Path to the RSCP to import. Expected to contain a YOSYS-produced EDIF and a macros.xml file.
	 * @param transformCells Whether or not to transform cells. Only transforms MUXCY, XORCY cells to CARRY4 cells right now.
	 * @throws IOException
	 */
	public static VivadoCheckpoint loadRSCP(String rscp, boolean transformCells) throws IOException {
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
		CellDesign design;
		YosysEdifInterface yosysEdifInterface = new YosysEdifInterface();
		design = yosysEdifInterface.parseEdif(edifFile, libCells, partName, transformCells);
		design.setImplementationMode(mode);

		// parse the constraints into RapidSmith2
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

		// If importing a reconfigurable module, process all of the static resources
		if (mode == ImplementationMode.RECONFIG_MODULE) {
			// Process other static resources (reserved sites, PIPs, partition pin routes)
			String resourcesFile = rscpPath.resolve("static.rsc").toString();
			StaticResourcesInterface staticInterface = new StaticResourcesInterface(design, device);
			staticInterface.parseResourcesRSC(resourcesFile);
			design.setRmStaticNetMap(staticInterface.getRmStaticNetMap());
			design.setStaticRouteStringMap(staticInterface.getStaticRouteStringMap());
			vivadoCheckpoint.setStaticPips(staticInterface.getStaticPips());
			vivadoCheckpoint.setRoutethroughBels(staticInterface.getBelRoutethroughMap());
		}

		for (CellNet net : design.getNets()) {
			if (!net.isIntrasite()) {
				net.removeRoutedSinks();
				if (!net.getPins().isEmpty())
					net.computeRouteStatus();
			}
		}


		return vivadoCheckpoint;
	}

}
