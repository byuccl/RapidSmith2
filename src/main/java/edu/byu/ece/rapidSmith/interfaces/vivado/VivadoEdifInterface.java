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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import edu.byu.ece.edif.core.InvalidEdifNameException;
import edu.byu.ece.edif.core.PropertyList;
import edu.byu.ece.edif.core.RenamedObject;
import edu.byu.ece.edif.core.StringTypedValue;
import edu.byu.ece.edif.util.parse.EdifParser;
import edu.byu.ece.edif.util.parse.ParseException;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.LibraryCell;
import edu.byu.ece.rapidSmith.design.subsite.LibraryPin;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.design.subsite.PropertyType;
import edu.byu.ece.rapidSmith.device.PinDirection;
import edu.byu.ece.rapidSmith.device.PortDirection;
import edu.byu.ece.rapidSmith.interfaces.AbstractEdifInterface;
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
public final class VivadoEdifInterface extends AbstractEdifInterface {

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
	 */
	public CellDesign parseEdif(String edifFile, CellLibrary libCells, String partName) {
		
		List<CellNet> vccNets = new ArrayList<>();
		List<CellNet> gndNets = new ArrayList<>();
		Map<EdifPort, Integer> portOffsetMap = new HashMap<>();

		try {		
			// parse edif into the BYU edif tools data structures
			EdifEnvironment top = EdifParser.translate(edifFile);
			EdifCell topLevelCell = top.getTopCell();
			
			// create RS2 cell design
			String edifPartName = ((StringTypedValue)top.getTopDesign().getProperty("part").getValue()).getStringValue();

			if (!suppressWarnings && !partName.equals(edifPartName)) {
				System.err.println("[Warning] Part name in EDIF, " + edifPartName + ", does not match part name in design.info, " + partName);
			}

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
	private void processTopLevelEdifPorts (CellDesign design, EdifCellInterface topInterface, CellLibrary libCells, Map<EdifPort, Integer> portOffsetMap) {
		
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
			
			// find the port prefix and offset
			String portPrefix = port.getOldName();
			if (port.isBus()) {
				Matcher matcher = busNamePattern.matcher(port.getOldName());
				if (matcher.matches()) {
					portPrefix = matcher.group(1);
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
									String.format("%s[%d]", portPrefix, reverseBusIndex(port.getWidth(), busMember.bitPosition(), offset)) :
									portPrefix;
				Cell portCell = new Cell(portName, libCell);
				design.addCell(portCell);
			}
		}
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
	@Override
	public void writeEdif(String edifOutputFile, CellDesign design) throws IOException {
		
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
	private HashSet<LibraryCell> getUniqueLibraryCellsInDesign(CellDesign design) {
		HashSet<LibraryCell> uniqueLibraryCells = new HashSet<>();

		for (Cell cell : design.getCells()) {
			if (!cell.isPort()) {
				if (cell.isMacro() && cell.isPlaced()) {
					for (Cell internalCell : cell.getInternalCells()) {
						uniqueLibraryCells.add(internalCell.getLibCell());
					}
				} else {
					// If the macro cell has not been placed, include the full macro in the EDIF.
					// If the full macro for unplaced LUTRAMs is not included in the EDIF, Vivado will
					// be unable to place them.
					uniqueLibraryCells.add(cell.getLibCell());
				}
			}
		}

		return uniqueLibraryCells;
	}

	/*
	 * Creates the top level EDIF cell that contains the design
	 */
	private EdifCell createEdifTopLevelCell(CellDesign design, EdifLibrary library, HashMap<LibraryCell, EdifCell> cellMap) throws EdifNameConflictException, InvalidEdifNameException {
		
		EdifCell topLevelCell = new EdifCell(library, createEdifNameable(design.getName()));
		
		// TODO: add the interface
		Map<Cell, PortInformation> portInfoMap = new HashMap<>();
		EdifCellInterface cellInterface = createTopLevelInterface(design, topLevelCell, portInfoMap);
		topLevelCell.setInterface(cellInterface);
		
		// create the cell instances
		for (Cell cell : design.getCells()) {

			if (cell.isPort())
				continue;

			if (cell.isMacro() && cell.isPlaced()) {
				if (!suppressInfoMessages)
					System.out.println("[Info] Macro cell " + cell.getName() + " is placed and will be flattened.");

				for (Cell internalCell : cell.getInternalCells()) {
					EdifCell edifLibCell = cellMap.get(internalCell.getLibCell());
					topLevelCell.addSubCell(createEdifCellInstance(internalCell, topLevelCell, edifLibCell));
				}
			} else {
				if (cell.isMacro() && !suppressInfoMessages)
					System.out.println("[Info] Macro cell " + cell.getName() + " is unplaced and will NOT be flattened.");

				EdifCell edifLibCell = cellMap.get(cell.getLibCell());
				topLevelCell.addSubCell(createEdifCellInstance(cell, topLevelCell, edifLibCell));
			}
		}
		
		// create the net instances
		for (CellNet net : getEdifExportNets(design)) {
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
	private EdifCellInterface createTopLevelInterface(CellDesign design, EdifCell topLevelEdifCell, Map<Cell, PortInformation> portInfoMap) throws EdifNameConflictException, InvalidEdifNameException {
		
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
					portMap.putIfAbsent(portName, portInfo);
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
			
			EdifNameable edifPortName;
			
			if (portInfo.isSingleBitPort()) {
				// some single-bit ports can be names like port[0]...which matches the bus pattern for port names...
				// this code returns the port to the correct name in this scenario
				edifPortName = portInfo.createdAsBus() ? 
						createEdifNameable(String.format("%s[%d]", portName, portInfo.getFirstIndex())) :
						createEdifNameable(portName);
			}
			else {
				// Some bus ports have bad names like port[0:0][7:0].
				// Because of this, just always make sure the new name is Edif Nameable.
				// Doing so checks that all port names are valid EDIF names, likely slightly increasing EDIF export time.
				edifPortName = new RenamedObject(createEdifNameable(portName), String.format("%s[%d:%d]", portName, portInfo.getMax(), portInfo.getMin()));
			}
						
			cellInterface.addPort(edifPortName, portInfo.getWidth(), portInfo.getDirection());
		}
				
		return cellInterface;
	}
	
	/*
	 * Creates an EDIF cell instance from a corresponding RapidSmith cell
	 */
	private EdifCellInstance createEdifCellInstance(Cell cell, EdifCell parent, EdifCell edifLibCell) {
		
		Objects.requireNonNull(edifLibCell);
		EdifCellInstance cellInstance = new EdifCellInstance(createEdifNameable(cell.getName()), parent, edifLibCell);
	
		// create an equivalent edif property for each RS2 property
		cellInstance.addPropertyList(createEdifPropertyList(cell.getProperties()));
				
		return cellInstance;
	}
	
	/*
	 * Creates an EDIF net from a corresponding RapidSmith net
	 */
	private EdifNet createEdifNet(CellNet cellNet, EdifCell edifParentCell, Map<Cell, PortInformation> portInfoMap) {
		EdifNet edifNet = new EdifNet(createEdifNameable(cellNet.getName()), edifParentCell);

		// create the port references for the edif net
		for (CellPin cellPin : getEdifExportNetPins(cellNet)) {

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
	private EdifPortRef createEdifPortRefFromPort(Cell port, EdifCell edifParent, EdifNet edifNet, int portOffset) {
		// Split on the last '['
		String[] toks = port.getName().split("\\[(?!.*\\[)");
		
		assert(toks.length == 1 || toks.length == 2);

		EdifPort edifPort = edifParent.getPort(toks[0]);
		
		if (edifPort == null) {
			edifPort = edifParent.getPort(getEdifName(toks[0]));
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
	private EdifPortRef createEdifPortRefFromCellPin(CellPin cellPin, EdifCell edifParent, EdifNet edifNet) {
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
	private PropertyList createEdifPropertyList(edu.byu.ece.rapidSmith.design.subsite.PropertyList properties) {
		PropertyList edifProperties = new PropertyList();
		
		for (Property prop : properties) {
			// The key and value of the property need sensible toString() methods when exporting to EDIF
			// this function is for creating properties for printing only!
			// TODO: make sure to inform the user of this
			// Only get PropertyType EDIF when creating EDIF propertyList
			if (prop.getType().equals(PropertyType.EDIF)){
				edu.byu.ece.edif.core.Property edifProperty;

				Object value = prop.getValue();

				if (value instanceof Boolean) {
					edifProperty = new edu.byu.ece.edif.core.Property(prop.getKey(), (Boolean) value);
				} else if (value instanceof Integer) {
					edifProperty = new edu.byu.ece.edif.core.Property(prop.getKey(), (Integer) value);
				} else {
					edifProperty = new edu.byu.ece.edif.core.Property(prop.getKey(), prop.getValue().toString());
				}

				edifProperties.addProperty(edifProperty);
			}
		}
		return edifProperties;
	}
	
	/*
	 * Creates an EDIF cell that corresponds to a RapidSmith library cell
	 */
	private EdifCell createEdifLibraryCell(LibraryCell cell, EdifLibrary library) throws EdifNameConflictException, InvalidEdifNameException {
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
	private EdifNameable createEdifNameable(String name) {
		
		return RenamedObject.createValidEdifNameable(name);
	}
	
	/*
	 * Returns the valid EDIF name for the specified input
	 */
	private String getEdifName(String originalName) {
		return RenamedObject.createValidEdifString(originalName);
	}
	
	/*
	 * Converts a RapidSmith pin direction to an EDIF pin direction
	 */
	private int getEdifPinDirection(PinDirection direction) {
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
	private int getEdifPinDirection(Cell portCell) {
		if (PortDirection.isInoutPort(portCell)) {
			return EdifPort.INOUT;
		}
		
		return PortDirection.isInputPort(portCell) ? EdifPort.IN : EdifPort.OUT; 
	}


	/**
	 * Returns the cell pins of a net, returning macro cell pins in place of
	 * internal cell pins if the corresponding internal cell has not been placed.
	 * @return
	 */
	private Collection<CellPin> getEdifExportNetPins(CellNet net) {
		return net.getPins().stream().map(p -> {
			if (p.isInternal() && !p.getCell().isPlaced())
				return p.getExternalPin();
			return p;
		}).collect(Collectors.toSet());
	}

	/**
	 * Returns a collection of external {@link CellNet}s and internal nets for placed macro cells
	 * that are currently in the design.
	 */
	private Collection<CellNet> getEdifExportNets(CellDesign design) {
		// Only include internal nets for macro cells that are placed.
		Collection<CellNet> nets = design.getNets().stream()
				.filter(n -> !n.isInternal()).collect(Collectors.toList());

		Iterator<Cell> cellIt = design.getMacros().iterator();

		while (cellIt.hasNext()) {
			Cell macroCell = cellIt.next();

			if (macroCell.isPlaced()) {
				nets.addAll(macroCell.getInternalNets());
			}
		}
		return nets;
	}

}
