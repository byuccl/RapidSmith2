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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import edu.byu.ece.edif.core.EdifNameConflictException;
import edu.byu.ece.edif.core.InvalidEdifNameException;
import static edu.byu.ece.rapidSmith.util.Exceptions.ParseException;
import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.util.Exceptions;

/**
 * This class is used to interface Vivado and RapidSmith. <br>
 * It parses TINCR checkpoints and creates equivalent RapidSmith designs. <br>
 * It can also create TINCR checkpoints from existing RapidSmith designs.
 * 
 * @author Thomas Townsend
 *
 */
public final class VivadoInterface {

	private static final String CELL_LIBRARY_NAME = "cellLibrary.xml";

	public static TincrCheckpoint loadTCP(String tcp) throws IOException {
		return loadTCP(tcp, false);
	}
	
	/**
	 * Parses a TINCR checkpoint, and creates an equivalent RapidSmith 2 design.
	 * 
	 * @param tcp Path to the TINCR checkpoint to import
	 * @throws InvalidEdifNameException 
	 * @throws EdifNameConflictException 
	 */
	public static TincrCheckpoint loadTCP (String tcp, boolean storeAdditionalInfo) throws IOException {
	
		if (tcp.endsWith("/") || tcp.endsWith("\\")) {
			tcp = tcp.substring(0, tcp.length()-1);
		}
		
		// check to make sure the specified directory is a TINCR checkpoint
		if (!tcp.endsWith(".tcp")) {
			throw new AssertionError("Specified directory is not a TINCR checkpoint.");
		}
			
		// load the device
		String partName = DesignInfoInterface.parseInfoFile(Paths.get(tcp, "design.info").toString());
		
		Device device = RSEnvironment.defaultEnv().getDevice(partName);
		
		if (device == null) {
			throw new Exceptions.EnvironmentException("Device files for part: " + partName + " cannot be found.");
		}

		// load the cell library
		CellLibrary libCells = new CellLibrary(RSEnvironment.defaultEnv()
				.getPartFolderPath(partName)
				.resolve(CELL_LIBRARY_NAME));
				
		// create the RS2 netlist
		String edifFile = Paths.get(tcp, "netlist.edf").toString();
		CellDesign design;
		try {
			design = EdifInterface.parseEdif(edifFile, libCells);
		} catch (edu.byu.ece.edif.util.parse.ParseException e) {
			throw new ParseException(e);
		}
		
		// parse the constraints into RapidSmith
		parseConstraintsXDC(design, Paths.get(tcp, "constraints.rsc").toString());
		
		// re-create the placement and routing information
		String placementFile = Paths.get(tcp, "placement.rsc").toString();
		XdcPlacementInterface placementInterface = new XdcPlacementInterface(design, device);
		placementInterface.parsePlacementXDC(placementFile);
 
		String routingFile = Paths.get(tcp, "routing.rsc").toString();
		XdcRoutingInterface routingInterface = new XdcRoutingInterface(design, device, placementInterface.getPinMap());
		routingInterface.parseRoutingXDC(routingFile);
		
		TincrCheckpoint tincrCheckpoint = new TincrCheckpoint(partName, design, device, libCells); 
		
		if (storeAdditionalInfo) {
			tincrCheckpoint.setRoutethroughBels(routingInterface.getRoutethroughsBels());
			tincrCheckpoint.setStaticSourceBels(routingInterface.getStaticSourceBels());
			tincrCheckpoint.setBelPinToCellPinMap(placementInterface.getPinMap());
		}
		
		return tincrCheckpoint;
	}
	
	/**
	 * Loads Vivado constraints into the specified {@link CellDesign}. For now, these constraints are
	 * loaded as two strings, a command and a list of arguments. There is no attempt right now to 
	 * intelligently handle these constraints, and they are included so the user has access to them.
	 * TODO: Update how we handle constraints files to make them easier to move
	 * 
	 * @param design {@link CellDesign}  
	 * @param constraintPath File location of the constraints file. Typically tcpDirectory/constraints.rsc is the constraints file.
	 */
	private static void parseConstraintsXDC(CellDesign design, String constraintPath) {
				
		try (BufferedReader br = new BufferedReader(new FileReader(constraintPath))) {
			
			String line = null;
			// add the design constraints to the design
			while ((line = br.readLine()) != null) {
				
				String trimmed = line.trim();
				
				// Skip commented lines
				if (trimmed.startsWith("#") || trimmed.length() < 1)
					continue;
				
				// assuming a space after the command TODO: make sure this assumption is correct
				int index = trimmed.indexOf(" ");
				String command = trimmed.substring(0, index);
				String options = trimmed.substring(index + 1);
				design.addVivadoConstraint(new XdcConstraint(command, options));
			}
			
		} catch (IOException e) {
			throw new Exceptions.ParseException(e);
		}
	}
		
	/**
	 * Export the RapidSmith2 design into an existing TINCR checkpoint file. 
	 *   
	 * @param tcpDirectory TINCR checkpoint directory to write XDC files to
	 * @param design CellDesign to convert to a TINCR checkpoint
	 * @throws IOException
	 * @throws InvalidEdifNameException 
	 * @throws EdifNameConflictException 
	 */
	public static void writeTCP(String tcpDirectory, CellDesign design, Device device, CellLibrary libCells) throws IOException {
				
		new File(tcpDirectory).mkdir();
		
		// insert routethrough buffers
		LutRoutethroughInserter inserter = new LutRoutethroughInserter(design, libCells);
		inserter.execute();
		
		// Write placement.xdc
		String placementOut = Paths.get(tcpDirectory, "placement.xdc").toString();	
		XdcPlacementInterface placementInterface = new XdcPlacementInterface(design, device);
		placementInterface.writePlacementXDC(placementOut);
		
		// Write routing.xdc
		String routingOut = Paths.get(tcpDirectory, "routing.xdc").toString();
		XdcRoutingInterface routingInterface = new XdcRoutingInterface(design, device, null);
		routingInterface.writeRoutingXDC(routingOut, design);
		
		// Write EDIF netlist
		String edifOut = Paths.get(tcpDirectory, "netlist.edf").toString();
		
		try {
			EdifInterface.writeEdif(edifOut, design);
		} 
		catch (EdifNameConflictException | InvalidEdifNameException e) {
			throw new AssertionError(e); 
		}

		// write constraints.xdc
		writeConstraintsXdc(design, Paths.get(tcpDirectory, "constraints.xdc").toString());
		
		// write design.info
		String partInfoOut = Paths.get(tcpDirectory, "design.info").toString();
		DesignInfoInterface.writeInfoFile(partInfoOut, device.getPartName());
	}
	
	/**
	 * Reads the Vivado constraints from the specified {@link CellDesign} and creates a 
	 * constraints.xdc file representing the constraints. The file is written to the newly
	 * created TINCR checkpoint.   
	 * 
	 * @param design {@link CellDesign}
	 * @param xdcOut Constraints.xdc file path 
	 * @throws IOException
	 */
	private static void writeConstraintsXdc(CellDesign design, String xdcOut) throws IOException {
		
		try (BufferedWriter fileout = new BufferedWriter (new FileWriter(xdcOut))) {
		
			LocalDateTime time = LocalDateTime.now();
			
			fileout.write(String.format("##############################################################\n"
					+ "# Generated by RapidSmith v.2.0 on %02d/%02d/%02d at %02d:%02d:%02d\n"
					+ "##############################################################\n\n", 
					time.getMonthValue(), time.getDayOfMonth(), time.getYear(), time.getHour(), time.getMinute(), time.getSecond()));
						
			for (XdcConstraint constraint : design.getVivadoConstraints()) {
				fileout.write(constraint + "\n");
			}
		}
	}
} // END CLASS 
