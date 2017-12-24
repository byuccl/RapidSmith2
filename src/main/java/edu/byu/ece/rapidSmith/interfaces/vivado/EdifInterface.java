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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.byu.ece.edif.core.BooleanTypedValue;
import edu.byu.ece.edif.core.EdifCell;
import edu.byu.ece.edif.core.EdifCellInstance;
import edu.byu.ece.edif.core.EdifCellInterface;
import edu.byu.ece.edif.core.EdifDesign;
import edu.byu.ece.edif.core.EdifEnvironment;
import edu.byu.ece.edif.core.EdifLibrary;
import edu.byu.ece.edif.core.EdifLibraryManager;
import edu.byu.ece.edif.core.EdifNameConflictException;
import edu.byu.ece.edif.core.EdifNameable;
import edu.byu.ece.edif.core.EdifNet;
import edu.byu.ece.edif.core.EdifPort;
import edu.byu.ece.edif.core.EdifPortRef;
import edu.byu.ece.edif.core.EdifPrintWriter;
import edu.byu.ece.edif.core.EdifSingleBitPort;
import edu.byu.ece.edif.core.EdifTypedValue;
import edu.byu.ece.edif.core.IntegerTypedValue;
import edu.byu.ece.edif.core.InvalidEdifNameException;
import edu.byu.ece.edif.core.PropertyList;
import edu.byu.ece.edif.core.RenamedObject;
import edu.byu.ece.edif.core.StringTypedValue;
import edu.byu.ece.edif.util.parse.EdifParser;
import edu.byu.ece.edif.util.parse.ParseException;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.LibraryCell;
import edu.byu.ece.rapidSmith.design.subsite.LibraryMacro;
import edu.byu.ece.rapidSmith.design.subsite.LibraryPin;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.design.subsite.PropertyType;
import edu.byu.ece.rapidSmith.device.PinDirection;
import edu.byu.ece.rapidSmith.device.PortDirection;
import edu.byu.ece.rapidSmith.util.Exceptions;

/**
 * This class is used to interface RapidSmith with EDIF files generated from Vivado.
 * It is capable of: <br>  
 * <p>
 * <li> Parsing an EDIF file <b>from Vivado</b> into a RapidSmith2 {@link CellDesign} </li> 
 * <li> Creating an Vivado-compatible EDIF file from a {@link CellDesign}. </li>
 * <p> 
 * Currently, the EdifInterace class only supports EDIF files created from fully flattened Vivado designs.
 * All BYU EDIF exceptions are wrapped into RapidSmith2 exceptions before being thrown. 
 */
public final class EdifInterface {

	private static boolean suppressWarnings = false;
	
	/**
	 * Suppress non-critical warnings while parsing an EDIF file. 
	 */
	public static void suppressWarnings(boolean suppress) {
		suppressWarnings = suppress;
	}
	
	/* ********************
	 * 	 Import Section
	 *********************/
	private static Pattern busNamePattern; 
	
	static 
	{
		busNamePattern = Pattern.compile("(.*)\\[.+:(.+)\\]");
	}
	
	/**
	 * Parses the Edif netlist into a RapidSmith2 CellDesign data structure
	 * 
	 * @param edifFile Input EDIF file
	 * @param libCells A Cell library for a specific Xilinx part
	 * 
	 * @return The RapidSmith2 representation of the EDIF netlist
	 * @throws FileNotFoundException
	 */
	public static CellDesign parseEdif(String edifFile, CellLibrary libCells) {
		
		List<CellNet> vccNets = new ArrayList<>();
		List<CellNet> gndNets = new ArrayList<>();
		Map<EdifPort, Integer> portOffsetMap = new HashMap<EdifPort, Integer>();

		try {		
			// parse edif into the BYU edif tools data structures
			EdifEnvironment top = EdifParser.translate(edifFile);
			EdifCell topLevelCell = top.getTopCell();
			
			// create RS2 cell design
			String partName = ((StringTypedValue)top.getTopDesign().getProperty("part").getValue()).getStringValue();
			CellDesign design= new CellDesign(top.getTopDesign().getName(), partName);	
			design.getProperties().updateAll(createCellProperties(topLevelCell.getPropertyList()));
			
			// add all the cells and nets to the design
			processTopLevelEdifPorts(design, topLevelCell.getInterface(), libCells, portOffsetMap);
			processEdifCells(design, topLevelCell.getCellInstanceList(), libCells, vccNets, gndNets);
			processEdifNets(design, topLevelCell.getNetList(), vccNets, gndNets, portOffsetMap);
					
			collapseStaticNets(design, libCells, vccNets, gndNets);
			return design;
		}
		catch (FileNotFoundException | ParseException e) {
			throw new Exceptions.ParseException(e);
		}
	}
	
	/*
	 * Converts EDIF top level ports to equivalent RapidSmith port cells and adds them to the design
	 */
	private static void processTopLevelEdifPorts (CellDesign design, EdifCellInterface topInterface, CellLibrary libCells, Map<EdifPort, Integer> portOffsetMap) {
		
		for ( EdifPort port : topInterface.getPortList() ) {
			
			String libraryPortType;
			
			if (port.isInOut()) {
				libraryPortType = "IOPORT";
			}
			else if (port.isInput()) {
				libraryPortType = "IPORT";
			}
			else {
				libraryPortType = "OPORT";
			}

			int offset = 0;
			
			// find the port suffix and offset
			String portSuffix = port.getOldName();
			if (port.isBus()) {
				Matcher matcher = busNamePattern.matcher(port.getOldName());
				if (matcher.matches()) {
					portSuffix = matcher.group(1);
					offset = Integer.parseInt(matcher.group(2));
					portOffsetMap.put(port, offset);
				}
				else {
					throw new AssertionError("Vivado Naming pattern for bus does not match expected pattern");
				}
			}
			
			// Create a new RapidSmith cell for each port in the EDIF
			for (EdifSingleBitPort busMember : port.getSingleBitPortList() ) {
				
				LibraryCell libCell = libCells.get(libraryPortType);
				
				String portName = port.isBus() ?
									String.format("%s[%d]", portSuffix, reverseBusIndex(port.getWidth(), busMember.bitPosition(), offset)) :
									portSuffix;
				Cell portCell = new Cell(portName, libCell);
				//portCell.getProperties().update(new Property("Dir", PropertyType.USER, PortDirection.getPortDirectionForImport(portCell)));
				design.addCell(portCell);
			}
		}
	}
		
	/*
	 * Converts EDIF cell instances to equivalent RapidSmith cells and adds them to the design
	 */
	private static void processEdifCells(CellDesign design, Collection<EdifCellInstance> edifCellInstances, CellLibrary libCells, List<CellNet> vccNets, List<CellNet> gndNets) {
		// TODO: think about throwing an error or warning here
		if (edifCellInstances == null || edifCellInstances.size() == 0) {
			if (!suppressWarnings) {
				System.err.println("[Warning] No cells found in the edif netlist");
			}
			return;
		}
		
		// create equivalent RS2 cells from the edif cell instances
		for(EdifCellInstance eci : edifCellInstances) {
			// create the corresponding RS2 cell
			LibraryCell lcType = libCells.get(eci.getType());
			if (lcType == null) {
				throw new Exceptions.ParseException("Unable to find library cell of type: " + eci.getType());
			}
			
			// Check for naming conflicts and rename cells as required...this should not be necessary for designs
			// synthesized and implemented in Vivado, but if the netlist is manipulated by an external tool,
			// this can happen
			if (design.hasCell(eci.getOldName())) {
				handleNamingConflict(design, design.getCell(eci.getOldName()));
			}
			
			Cell newcell = design.addCell(new Cell(eci.getOldName(), lcType));
			
			// Add properties to the cell 
			newcell.getProperties().updateAll(createCellProperties(eci.getPropertyList()));
			
			// look for internal macro nets
			if (newcell.isMacro()) {
				for (CellNet net : newcell.getInternalNets()) {
					if (net.isVCCNet()) {
						vccNets.add(net);
					}
					else if (net.isGNDNet()) {
						gndNets.add(net);
					}
				}
			}
		}
	}
	
	/**
	 * Some EDIF netlists from Vivado can have identical port and cell names. This function renames the ports
	 * so that there is no naming conflict in RapidSmith. 
	 * 
	 * @param design CellDesign
	 * @param cell Cell to rename (should be a port cell)
	 */
	private static void handleNamingConflict (CellDesign design, Cell cell) {
		assert cell.isPort() : "Conflicting cell names should only happen with Port cells: " + cell.getName();
		
		// print a warning to the user
		if (!suppressWarnings) {
			System.err.println("[Warning] A top-level port and another cell in the netlist have identical names: " + cell.getName() 
			              + ". The port cell will be renamed to " + cell.getName() + "_rsport");
		}
		
		// update the name of the cell 
		design.removeCell(cell);
		Cell newPortCell = new Cell(cell.getName() + "_rsport", cell.getLibCell());
		design.addCell(newPortCell);
	}
	
	/*
	 * Converts EDIF nets to equivalent RapidSmith nets and adds them to the design
	 */
	private static void processEdifNets(CellDesign design, Collection<EdifNet> edifNets, List<CellNet> vccNets, List<CellNet> gndNets, Map<EdifPort, Integer> portOffsetMap) {
		 
		if (edifNets == null || edifNets.size() == 0) {
			if (!suppressWarnings) {
				System.err.println("[Warning] No nets found in the edif netlist");
			}
			return;
		}
		
		// Go through the cell's nets and hook up inputs and outputs 
		for(EdifNet net : edifNets) {
			
			//create a new net
			CellNet cn = new CellNet(net.getOldName(), NetType.WIRE); 
			
			// Add all the source and sink connections to the net
			//Collection<EdifPortRef> sources = net.getSourcePortRefs(false, true);
			
			// process all net connections
			processNetConnections(net.getPortRefList(), design, cn, portOffsetMap);
			
			//report a warning if no sources on a net are found
			if (cn.getAllSourcePins().size() == 0) {
				if (!suppressWarnings) {
					System.err.println("[Warning] No source for net " + net.getOldName());
				}
			}
						
			// Add the net to the design if is is NOT a static net.
			// Otherwise, store it for later use (will collapse later)
			if (cn.isVCCNet()) {
				vccNets.add(cn);
			}
			else if (cn.isGNDNet()) {
				gndNets.add(cn);
			}
			else {
				design.addNet(cn);
			}
			
			cn.getProperties().updateAll(createCellProperties(net.getPropertyList()));
		}
	}
	
	/*
	 * Builds the connections of CellNet based on the specified EDIF port references. Returns true
	 * if the given net is attached to a top-level port, false otherwise
	 * 
	 * TODO: update this once top-level ports are added
	 */
	private static void processNetConnections(Collection<EdifPortRef> portRefs, CellDesign design, CellNet net, Map<EdifPort, Integer> portOffsetMap) {
			
		for (EdifPortRef portRef: portRefs) {  
			
			EdifPort port = portRef.getPort(); 
			
			// Connects to a top-level port
			if (portRef.isTopLevelPortRef()) {
								
				String portname = portRef.isSingleBitPortRef() ? port.getOldName() : 
				 				  String.format("%s[%d]", getPortNameSuffix(port.getName()), reverseBusIndex(port.getWidth(), portRef.getBusMember(), portOffsetMap.get(port)));
				
				Cell portCell = design.getCell(portname);
				
				if (portCell == null) {
					throw new Exceptions.ParseException("Port Cell " + portname + " does not exist in the design!");
				}
				else if (!portCell.isPort()) {
					portCell = design.getCell(portname + "_rsport");
				}
				
				net.connectToPin(portCell.getPin("PAD"));
								
				continue; 
			}
			
			Cell node = design.getCell(portRef.getCellInstance().getOldName()); 
			if (node == null) {
				throw new Exceptions.ParseException("Cell: " + portRef.getCellInstance().getOldName()  + " does not exist in the design!");
			}
			
			int busOffset = 0;
			if (node.isMacro()) {
				LibraryMacro macro = (LibraryMacro)node.getLibCell(); 
				busOffset = macro.getPinOffset(port.getName());
			}
			
			// Connects to a cell pin
			// TODO: take a closer look at this...I am using the edif name of a cell pin name which should be ok, but be aware
			String pinname = portRef.isSingleBitPortRef() ? port.getName() 
					 : String.format("%s[%d]", port.getName(), reverseBusIndex(port.getWidth(), portRef.getBusMember(), busOffset));
			
			// Mark GND and VCC nets 
			if (node.isVccSource()) {
				net.setType(NetType.VCC);
			}
			else if (node.isGndSource()) {
				net.setType(NetType.GND);
			}
			
			//if (net.getName().equals("vga_o[76]")) {
			//	System.out.println(" --> " + pinname  + " " + node.getPin(pinname) + " " + node.isMacro());
			//}
			
			net.connectToPin(node.getPin(pinname));						
		}		
	}
	
	/*
	 * Vivado ports that are buses are named portName[15:0]
	 * This function will return the "portName" portion of the bus name
	 */
	private static String getPortNameSuffix(String portName) {
		
		int bracketIndex = portName.indexOf("[");
		return bracketIndex == -1 ? portName : portName.substring(0, bracketIndex);
	}
		
	/*
	 * Creates a list of RapidSmith cell properties from an EDIF property list
	 */
	private static List<Property> createCellProperties(PropertyList edifPropertyList) {
		List<Property> cellProperties = new ArrayList<>();
		
		if (edifPropertyList != null) {
			for (String keyName : edifPropertyList.keySet()) {
				edu.byu.ece.edif.core.Property property = edifPropertyList.getProperty(keyName);
				Property prop = new Property(property.getName(), PropertyType.EDIF, getValueFromEdifType(property.getValue()));
				cellProperties.add(prop);
			}
		}
		
		return cellProperties;
	}
	
	/*
	 * Converts an EdifTypedValue to the corresponding native Java type
	 */
	private static Object getValueFromEdifType(EdifTypedValue typedValue) {
		
		Object value;
		if (typedValue instanceof IntegerTypedValue) {
			value = ((IntegerTypedValue)typedValue).getIntegerValue();
		}
		else if (typedValue instanceof BooleanTypedValue) {
			value = ((BooleanTypedValue)typedValue).getBooleanValue();
		}
		else  { // default is string type
			value = ((StringTypedValue)typedValue).getStringValue();
		}
		return value;
	}
	
	/*
	 * Because EDIF files reverse the index of bus members, this function
	 * is used to get the original index of a port into a bus.  
	 */
	private static int reverseBusIndex(int width, int busMember, int offset) {
		return width - 1 - busMember + offset;
	}
	
	private static void collapseStaticNets(CellDesign design, CellLibrary libCells, List<CellNet> vccNets, List<CellNet> gndNets) {
		
		// Create new global VCC/GND cells and nets
		Cell globalVCC = new Cell("RapidSmithGlobalVCC", libCells.getVccSource());
		Cell globalGND = new Cell("RapidSmithGlobalGND", libCells.getGndSource());		
		CellNet globalVCCNet = new CellNet("RapidSmithGlobalVCCNet", NetType.VCC);
		CellNet globalGNDNet = new CellNet("RapidSmithGlobalGNDNet", NetType.GND);
		
		// Connect the global sources to the global nets
		globalVCCNet.connectToPin(globalVCC.getOutputPins().iterator().next());
		globalGNDNet.connectToPin(globalGND.getOutputPins().iterator().next());
		
		// Add all VCC/GND sink pins to the global nets
		for(CellNet net : vccNets) {
			transferSinkPins(net, globalVCCNet);
		}
		
		for(CellNet net : gndNets) {
			transferSinkPins(net, globalGNDNet);
		}
			
		// Remove the old VCC/GND cells from the list
		List<Cell> cellsToRemove = new ArrayList<>();
		for (Cell cell : design.getCells()) {
			if (cell.isVccSource() || cell.isGndSource()) {
				cellsToRemove.add(cell);
			}
		}
		cellsToRemove.forEach(design::removeCell);
				
		// Add the new master cells/nets to the design
		design.addCell(globalVCC);
		design.addNet(globalVCCNet);
		design.addCell(globalGND);
		design.addNet(globalGNDNet);
		
		// For macro pins tied to power or ground, make them point to the appropriate global static net
		for (Cell c : design.getCells()) {
			for (CellPin cp : c.getPins()) {
				if (cp.getNet()!= null) {
					if (cp.getNet() != globalGNDNet && cp.getNet().getType() == NetType.GND) 
						cp.setMacroPinToGlobalNet(globalGNDNet);
					else if (cp.getNet() != globalVCCNet && cp.getNet().getType() == NetType.VCC) 
						cp.setMacroPinToGlobalNet(globalVCCNet);
				}
			}
		}

	}
	
	private static void transferSinkPins(CellNet oldNet, CellNet newNet) {
		Collection<CellPin> sinkPins = oldNet.getSinkPins();
		oldNet.detachNet();
		oldNet.unrouteFull();
		newNet.connectToPins(sinkPins);
	}
	
	/* *********************
	 *    Export Section
	 ***********************/
	
	/**
	 * Creates an EDIF netlist file from a RapidSmith CellDesign netlist.
	 * 
	 * @param edifOutputFile Output EDIF file path
	 * @param design RapidSmith design to convert to EDIF
	 * @throws IOException
	 */
	public static void writeEdif(String edifOutputFile, CellDesign design) throws IOException {
		
		try {
			// TODO: copy old edif environment properties into new edif environment properties
			EdifEnvironment edifEnvironment = new EdifEnvironment(createEdifNameable(design.getName()));
			edifEnvironment.setAuthor("BYU CCL");
			edifEnvironment.setProgram("RapidSmith");
			edifEnvironment.setVersion("2.0.0");
			edifEnvironment.setDateWithCurrentTime();
			EdifLibraryManager libManager = edifEnvironment.getLibraryManager();
					
			// create the edif cell library
			EdifLibrary edifPrimitiveLibrary = new EdifLibrary(libManager, "hdi_primitives");
			
			HashMap<LibraryCell, EdifCell> cellMap = new HashMap<>();
			
			for (LibraryCell libCell : getUniqueLibraryCellsInDesign(design)) {
				EdifCell edifCell = createEdifLibraryCell(libCell, edifPrimitiveLibrary); 
				edifPrimitiveLibrary.addCell(edifCell);
				cellMap.put(libCell, edifCell);
			}
			
			edifEnvironment.addLibrary(edifPrimitiveLibrary);
			
			//create top level library
			//TODO: do not assume the default name...is the default name an issue? 
			EdifLibrary topCellLibrary = new EdifLibrary(libManager, "work");
			EdifCell topCell = createEdifTopLevelCell(design, topCellLibrary, cellMap);
			topCellLibrary.addCell(topCell);
			edifEnvironment.addLibrary(topCellLibrary);
			
			// create the top level edif design
			EdifDesign topDesign = new EdifDesign(edifEnvironment.getEdifNameable());
			EdifCellInstance eci = new EdifCellInstance(edifEnvironment.getEdifNameable(), topCell, topCell);
			topDesign.setTopCellInstance(eci);
			topDesign.addProperty(new edu.byu.ece.edif.core.Property("part", design.getPartName()));
			edifEnvironment.setTopDesign(topDesign);
			
			// write edif
			EdifPrintWriter edifWriter = new EdifPrintWriter(edifOutputFile);
			edifEnvironment.toEdif(edifWriter);
		}
		catch (EdifNameConflictException | InvalidEdifNameException e ) {
			throw new AssertionError(e);
		}
	}
	
	/*
	 * Returns a hashset of all unique library cells in a given design
	 */
	private static HashSet<LibraryCell> getUniqueLibraryCellsInDesign(CellDesign design) {
		
		HashSet<LibraryCell> uniqueLibraryCells = new HashSet<>();
		
		Iterator<Cell> cellIt = design.getLeafCells().iterator();
		
		// for (Cell c : design.getCells()) {
		while (cellIt.hasNext()) {
			Cell c = cellIt.next();
			
			if (!c.isPort())
				uniqueLibraryCells.add(c.getLibCell());
		}
		
		return uniqueLibraryCells;
	}
	
	/*
	 * Creates the top level EDIF cell that contains the design
	 */
	private static EdifCell createEdifTopLevelCell(CellDesign design, EdifLibrary library, HashMap<LibraryCell, EdifCell> cellMap) throws EdifNameConflictException, InvalidEdifNameException {	
		
		EdifCell topLevelCell = new EdifCell(library, createEdifNameable(design.getName()));
		
		// TODO: add the interface
		Map<Cell, PortInformation> portInfoMap = new HashMap<>();
		EdifCellInterface cellInterface = createTopLevelInterface(design, topLevelCell, portInfoMap);
		topLevelCell.setInterface(cellInterface);
		
		// create the cell instances
		Iterator<Cell> cellIt = design.getLeafCells().iterator();
		//for (Cell cell : design.getCells) {
		while (cellIt.hasNext()) {
			Cell cell = cellIt.next();
			
			if (cell.isPort())
				continue;
			
			EdifCell edifLibCell = cellMap.get(cell.getLibCell());
			topLevelCell.addSubCell( createEdifCellInstance(cell, topLevelCell, edifLibCell) );
		}
		
		// create the net instances
		for (CellNet net : design.getNets()) {
		 	topLevelCell.addNet(createEdifNet(net, topLevelCell, portInfoMap));
		}
			
		topLevelCell.addPropertyList(createEdifPropertyList(design.getProperties()));
		return topLevelCell; 
	}
	
	/*
	 * Creates the EDIF interface for the top level cell
	 * 
	 * TODO: This is only guaranteed to work with netlists imported from Vivado. 
	 */
	private static EdifCellInterface createTopLevelInterface(CellDesign design, EdifCell topLevelEdifCell, Map<Cell, PortInformation> portInfoMap) throws EdifNameConflictException, InvalidEdifNameException {
		
		EdifCellInterface cellInterface = new EdifCellInterface(topLevelEdifCell);
		Pattern portNamePattern = Pattern.compile("(.*)\\[(.*)\\]");
		
		Map<String, PortInformation> portMap = new HashMap<>();
		
		for (Cell cell : design.getCells()) {
			
			if (cell.isPort()) {
				Matcher m = portNamePattern.matcher(cell.getName());
				int direction = getEdifPinDirection(cell);
				if (m.matches()) {
					String portName = m.group(1);
					int busMember = Integer.parseInt(m.group(2));
					PortInformation portInfo = portMap.getOrDefault(portName, new PortInformation(portName, direction, false, busMember));
					portInfo.addPort(busMember);
					portMap.computeIfAbsent(portName, k -> portInfo);
					portInfoMap.put(cell, portInfo);
				} 
				else {
					PortInformation portInfo = new PortInformation(cell.getName(), direction, true, 0);
					portMap.put(cell.getName(), portInfo);
					portInfoMap.put(cell, portInfo);
				}
			}
		}
		
		for (Map.Entry<String,PortInformation> entry : portMap.entrySet()) {
			String portName = entry.getKey();
			PortInformation portInfo = entry.getValue();
			
			EdifNameable edifPortName = null;
			
			if (portInfo.isSingleBitPort()) {
				// some single-bit ports can be names like port[0]...which matches the bus pattern for port names...
				// this code returns the port to the correct name in this scenario
				edifPortName = portInfo.createdAsBus() ? 
						createEdifNameable(String.format("%s[%d]", portName, portInfo.getFirstIndex())) :
						createEdifNameable(portName);
			}
			else {
				edifPortName = new RenamedObject(portName, String.format("%s[%d:%d]", portName, portInfo.getMax(), portInfo.getMin()));
			}
						
			cellInterface.addPort(edifPortName, portInfo.getWidth(), portInfo.getDirection());
		}
				
		return cellInterface;
	}
	
	/*
	 * Creates an EDIF cell instance from a corresponding RapidSmith cell
	 */
	private static EdifCellInstance createEdifCellInstance(Cell cell, EdifCell parent, EdifCell edifLibCell) {
		
		Objects.requireNonNull(edifLibCell);
		EdifCellInstance cellInstance = new EdifCellInstance(createEdifNameable(cell.getName()), parent, edifLibCell);
	
		// create an equivalent edif property for each RS2 property
		cellInstance.addPropertyList(createEdifPropertyList(cell.getProperties()));
				
		return cellInstance;
	}
	
	/*
	 * Creates an EDIF net from a corresponding RapidSmith net
	 */
	private static EdifNet createEdifNet(CellNet cellNet, EdifCell edifParentCell, Map<Cell, PortInformation> portInfoMap) {
		EdifNet edifNet = new EdifNet(createEdifNameable(cellNet.getName()), edifParentCell);
				
		// create the port references for the edif net
		for (CellPin cellPin : cellNet.getPins()) {
			
			if (cellPin.isPseudoPin()) {
				continue;
			}
						
			Cell parentCell = cellPin.getCell();
			
			EdifPortRef portRef = parentCell.isPort() ?
									createEdifPortRefFromPort(parentCell, edifParentCell, edifNet, portInfoMap.get(parentCell).getMin()) : 
									createEdifPortRefFromCellPin(cellPin, edifParentCell, edifNet) ;
			
			edifNet.addPortConnection(portRef);
		}
					
		return edifNet;
	}
	
	/*
	 * Creates a port reference (connection) for an EDIF net from a top-level port cell.
	 */
	private static EdifPortRef createEdifPortRefFromPort(Cell port, EdifCell edifParent, EdifNet edifNet, int portOffset) {
		
		String[] toks = port.getName().split("\\[");
		
		assert(toks.length == 1 || toks.length == 2);
		
		EdifPort edifPort = edifParent.getPort(toks[0]);
		
		if (edifPort == null) {
			edifPort = edifParent.getPort(createEdifNameable(port.getName()).getName());
		}
		
		int portIndex = 1;
		if (toks.length == 2 && edifPort.getWidth() > 1) {
			portIndex = reverseBusIndex(edifPort.getWidth(), Integer.parseInt(toks[1].substring(0, toks[1].length()-1)), portOffset);
		}
		EdifSingleBitPort singlePort = new EdifSingleBitPort(edifPort, portIndex);
		return new EdifPortRef(edifNet, singlePort, null);	
	}
	
	/*
	 * Creates a port reference (connection) for an EDIF net from a cell pin. 
	 * This is different than the port implementation because it needs to add a cell instance
	 * to the connection.
	 */
	private static EdifPortRef createEdifPortRefFromCellPin(CellPin cellPin, EdifCell edifParent, EdifNet edifNet) {
		
		EdifCellInstance cellInstance = edifParent.getCellInstance(getEdifName(cellPin.getCell().getName()));
		EdifCell libCell = cellInstance.getCellType();
		
		String[] toks = cellPin.getName().split("\\["); 
		
		assert(toks.length == 1 || toks.length == 2);
		
		String portName = toks[0];
		
		int busMember = 1;
		if (toks.length == 2) {
			busMember = Integer.parseInt(toks[1].substring(0, toks[1].length()-1));
		}
					
		EdifPort port = libCell.getPort(portName); 
		int reversedIndex = reverseBusIndex(port.getWidth(), busMember, 0);  
		EdifSingleBitPort singlePort = new EdifSingleBitPort(port, reversedIndex);
		return new EdifPortRef(edifNet, singlePort, cellInstance);
	}
	
	/*
	 * Converts a collection of RapidSmith properties to an EDIF property list
	 */
	private static PropertyList createEdifPropertyList (edu.byu.ece.rapidSmith.design.subsite.PropertyList properties) {
		PropertyList edifProperties = new PropertyList();
		
		for (Property prop : properties) {
			// The key and value of the property need sensible toString() methods when exporting to EDIF
			// this function is for creating properties for printing only!
			// TODO: make sure to inform the user of this 
			
			edu.byu.ece.edif.core.Property edifProperty;
			
			Object value = prop.getValue(); 
			
			if (value instanceof Boolean) {
				edifProperty = new edu.byu.ece.edif.core.Property(prop.getKey().toString(), (Boolean) value);
			}
			else if (value instanceof Integer) {
				edifProperty = new edu.byu.ece.edif.core.Property(prop.getKey().toString(), (Integer) value);
			}
			else {	
				edifProperty = new edu.byu.ece.edif.core.Property(prop.getKey().toString(), prop.getValue().toString());
			}
			
			edifProperties.addProperty(edifProperty);
		}
		
		return edifProperties;
	}
	
	/*
	 * Creates an EDIF cell that corresponds to a RapidSmith library cell
	 */
	private static EdifCell createEdifLibraryCell(LibraryCell cell, EdifLibrary library) throws EdifNameConflictException, InvalidEdifNameException {
		// this will print a cell with the "view PRIM" ... vivado prints them with "view netlist" this shouldn't be a problem
		EdifCell edifCell = new EdifCell(library, createEdifNameable(cell.getName()), true);
		
		EdifCellInterface edifInterface = new EdifCellInterface(edifCell);
		edifCell.setInterface(edifInterface);
		
		HashMap<String, Integer> pinWidthMap = new HashMap<>();
		HashMap<String, Integer> pinDirectionMap = new HashMap<>();
		
		// Assumption: Vivado pinnames are a series of alphanumberic characters, followed by an optional number within brackets (for busses)
		// Examples: pin1, pin[2], name
		for (LibraryPin pin : cell.getLibraryPins()) {
			String pinname = pin.getName().split("\\[")[0];
			Integer count = pinWidthMap.getOrDefault(pinname, 0);
			pinWidthMap.put(pinname, count + 1); 
			
			if (count == 0) {
				pinDirectionMap.put(pinname, getEdifPinDirection(pin.getDirection()));
			}
		}
		
		// add the library cell ports to the interface
		for (String pinname : pinWidthMap.keySet()) {
			int width = pinWidthMap.get(pinname);		
			EdifNameable edifPortName = (width == 1) ? 
									createEdifNameable(pinname) : 
									new RenamedObject(pinname, String.format("%s[%d:0]", pinname, width-1));
			edifInterface.addPort(edifPortName, width, pinDirectionMap.get(pinname));
		}
		
		return edifCell;
	}
	
	/*
	 * Creates a valid EDIF nameable for the specified input
	 */
	private static EdifNameable createEdifNameable(String name) {
		
		return RenamedObject.createValidEdifNameable(name);
	}
	
	/*
	 * Returns the valid EDIF name for the specified input
	 */
	private static String getEdifName(String originalName) {
		return RenamedObject.createValidEdifString(originalName);
	}
	
	/*
	 * Converts a RapidSmith pin direction to an EDIF pin direction
	 */
	private static int getEdifPinDirection(PinDirection direction) {	
		switch(direction) {
			case IN:
				return EdifPort.IN;
			case OUT:
				return EdifPort.OUT;
			case INOUT:
				return EdifPort.INOUT;
			default: // if we reach here, then thrown a new exception
				throw new AssertionError("Invalid Pin Direction!");
		}
	}
	
	/*
	 * Returns the corresponding EDIF port direction of a RapidSmith
	 * port cell.
	 */
	private static int getEdifPinDirection(Cell portCell) {		
		if (PortDirection.isInoutPort(portCell)) {
			return EdifPort.INOUT;
		}
		
		return PortDirection.isInputPort(portCell) ? EdifPort.IN : EdifPort.OUT; 
	}
}
