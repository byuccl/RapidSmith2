package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

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
import edu.byu.ece.rapidSmith.design.subsite.LibraryPin;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.design.subsite.PropertyType;
import edu.byu.ece.rapidSmith.device.PinDirection;
import edu.byu.ece.rapidSmith.device.PortDirection;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * This class is used to interface RapidSmith and EDIF files generated from Vivado. <br>
 * This includes both parsing an EDIF file into a RapidSmith CellDesign, and creating an EDIF <br>
 * file from a RapidSmith CellDesign. Currently, it only supports edif created from <br>
 * fully flattened Vivado designs. <br>
 * 
 * @author Thomas Townsend
 *
 */
public final class EdifInterface {

	/* ********************
	 * 	 Import Section
	 *********************/

	/**
	 * Parses the Edif netlist into a RapidSmith2 CellDesign data structure
	 * 
	 * @param edifFile Input EDIF file
	 * @param libCells A Cell library for a specific Xilinx part
	 * @return
	 * @throws FileNotFoundException
	 * @throws ParseException
	 */
	public static CellDesign parseEdif(String edifFile, CellLibrary libCells) throws FileNotFoundException, ParseException {
		
		// parse edif into the BYU edif tools data structures
		EdifEnvironment top = EdifParser.translate(edifFile);
		EdifCell topLevelCell = top.getTopCell();
		
		// create RS2 cell design
		String partName = ((StringTypedValue)top.getTopDesign().getProperty("part").getValue()).getStringValue();
		CellDesign design= new CellDesign(top.getTopDesign().getName(), partName);	
		design.updateProperties(createCellProperties(topLevelCell.getPropertyList()));
		
		// add all the cells and nets to the design
		processTopLevelEdifPorts(design, topLevelCell.getInterface(), libCells);
		processEdifCells(design, topLevelCell.getCellInstanceList(), libCells);
		processEdifNets(design, topLevelCell.getNetList());
		
		return design;
	}
	
	/*
	 * Converts EDIF top level ports to equivalent RapidSmith port cells and adds them to the design
	 */
	private static void processTopLevelEdifPorts (CellDesign design, EdifCellInterface topInterface, CellLibrary libCells) {
		
		for ( EdifPort port : topInterface.getPortList() ) {
			
			String libraryPortType = port.isInput() ? "IPORT" : "OPORT";
		
			// TODO: See if inout ports are supported in RS2, and if not what needs to be updated to support them 
			if (port.isInOut()) {
				throw new UnsupportedOperationException("Inout port functionality not yet implemented! Cannot load EDIF");
			}
			
			for (EdifSingleBitPort busMember : port.getSingleBitPortList() ) {
				
				LibraryCell libCell = libCells.get(libraryPortType);
				Objects.requireNonNull(libCell, "Library cell: " + libraryPortType + " not found in the cell library!");
				
				Cell portCell = new Cell(renameEdifPort(busMember), libCell);
				portCell.updateProperty(new Property("Dir", PropertyType.USER, PortDirection.getPortDirectionForImport(portCell)));
				design.addCell(portCell);
			}
		}
	}
	
	/*
	 * Renames an edifPort to be consistent with RapidSmith bus naming conventions.
	 * Specifically, caret characters ('<','>') are replaced with brackets ('[',']')
	 * Example: testPort<2> --> testPort[2]
	 */
	private static String renameEdifPort(EdifSingleBitPort edifPort) {
		
		return edifPort.getPortName().replace('<', '[').replace('>', ']');
	}
	
	/*
	 * Converts EDIF cell instances to equivalent RapidSmith cells and adds them to the design
	 */
	private static void processEdifCells(CellDesign design, Collection<EdifCellInstance> edifCellInstances, CellLibrary libCells) {
		// TODO: think about throwing an error or warning here
		if (edifCellInstances == null || edifCellInstances.size() == 0) {
			MessageGenerator.generalError("[Warning] No cells found in the edif netlist");
			return;
		}
		
		// create equivalent RS2 cells from the edif cell instances
		for(EdifCellInstance eci : edifCellInstances) {
			// create the corresponding RS2 cell
			LibraryCell lcType = libCells.get(eci.getType());
			if (lcType == null) { 
				MessageGenerator.briefErrorAndExit("[ERROR] Cannot find library cell of type: " + eci.getType() + ", exiting...");
			}
			Cell newcell = design.addCell(new Cell(eci.getOldName(), lcType));
			
			// Add properties to the cell 
			// TODO: when macros are added, we will need to update this
			newcell.updateProperties(createCellProperties(eci.getPropertyList()));
		}
	}
	
	/*
	 * Converts EDIF nets to equivalent RapidSmith nets and adds them to the design
	 */
	private static void processEdifNets(CellDesign design, Collection<EdifNet> edifNets) {
		 
		if (edifNets == null || edifNets.size() == 0) {
			MessageGenerator.briefError("[Warning] No nets found in the edif netlist");
			return;
		}
		
		// Go through the cell's nets and hook up inputs and outputs 
		for(EdifNet net : edifNets) {
			
			//create a new net
			CellNet cn = new CellNet(net.getOldName(), NetType.WIRE); 
			
			// Add all the source and sink connections to the net
			Collection<EdifPortRef> sources = net.getSourcePortRefs(false, true);
			
			if (sources.size() == 0) {
				MessageGenerator.briefError("[Warning] No source for net " + net.getOldName());
				continue;
			}
			
			processNetConnections(sources, design, cn);
			processNetConnections(net.getSinkPortRefs(false, true), design, cn);
							
			design.addNet(cn);
			cn.updateProperties(createCellProperties(net.getPropertyList()));
		}
	}
	
	/*
	 * Builds the connections of CellNet based on the specified EDIF port references. Returns true
	 * if the given net is attached to a top-level port, false otherwise
	 * 
	 * TODO: update this once top-level ports are added
	 */
	private static void processNetConnections(Collection<EdifPortRef> ports, CellDesign design, CellNet net) {
			
		for (EdifPortRef port: ports) {  
				
			// Connects to a top-level port
			if (port.isTopLevelPortRef()) {
				
				String portname = port.isSingleBitPortRef() ? port.getPort().getName()
								  : String.format("%s[%d]", port.getPort().getName(), port.getBusMember());
				
				//TODO: add a check for null and throw a meaningful exception if this is the case
				Cell portCell = design.getCell(portname);
				net.connectToPin(portCell.getPin("PAD"));
								
				continue; 
			}
			
			// Connects to a cell pin
			String pinname = port.isSingleBitPortRef() ? port.getPort().getName() 
					 : port.getPort().getName() + "[" + reverseBusIndex(port.getPort().getWidth(), port.getBusMember()) + "]";
			
			Cell node = design.getCell(port.getCellInstance().getOldName()); 
			if (node == null) {
				MessageGenerator.briefErrorAndExit("[ERROR] Trying to connect net " + net.getName() + " to cell " 
								+ port.getCellInstance().getOldName() + ", but specified cell does not exist");
			}
			
			// Mark GND and VCC nets 
			if (node.isVccSource())
				net.setType(NetType.VCC);
			else if (node.isGndSource())
				net.setType(NetType.GND);
			
			net.connectToPin(node.getPin(pinname));						
		}		
	}
		
	/*
	 * Creates a list of RapidSmith cell properties from an EDIF property list
	 */
	private static List<Property> createCellProperties(PropertyList edifPropertyList) {
		List<Property> cellProperties = new ArrayList<Property>();
		
		if (edifPropertyList != null) {
			for (String keyName : edifPropertyList.keySet()) {
				edu.byu.ece.edif.core.Property property = edifPropertyList.getProperty(keyName);
				Property prop = new Property(property.getName(), PropertyType.USER, getValueFromEdifType(property.getValue()));
				cellProperties.add(prop);
			}
		}
		
		return cellProperties;
	}
	
	/*
	 * Converts an EdifTypedValue to the corresponding native Java type
	 */
	private static Object getValueFromEdifType(EdifTypedValue typedValue) {
		
		Object value = null; 
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
	private static int reverseBusIndex(int width, int busMember) {
		return width - 1 - busMember;
	}
	
	/* *********************
	 *    Export Section
	 ***********************/
	
	/**
	 * Creates an EDIF netlist file from a RapidSmith CellDesign netlist.
	 * 
	 * @param edifOutputFile Output EDIF file path
	 * @param design RapidSmith design to convert to EDIF
	 * @throws EdifNameConflictException
	 * @throws InvalidEdifNameException
	 * @throws IOException
	 */
	public static void writeEdif(String edifOutputFile, CellDesign design) throws EdifNameConflictException, InvalidEdifNameException, IOException {
		
		// TODO: copy old edif environment properties into new edif environment properties
		EdifEnvironment edifEnvironment = new EdifEnvironment(createEdifNameable(design.getName()));
		edifEnvironment.setAuthor("BYU CCL");
		edifEnvironment.setProgram("RapidSmith2");
		edifEnvironment.setVersion("1.0.0"); // TODO: replace this with the current RS2 version
		edifEnvironment.setDateWithCurrentTime();
		EdifLibraryManager libManager = edifEnvironment.getLibraryManager();
				
		// create the edif cell library
		EdifLibrary edifPrimitiveLibrary = new EdifLibrary(libManager, "hdi_primitives");
		
		HashMap<LibraryCell, EdifCell> cellMap = new HashMap<LibraryCell, EdifCell>();	
		
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
	
	/*
	 * Returns a hashset of all unique library cells in a given design
	 */
	private static HashSet<LibraryCell> getUniqueLibraryCellsInDesign(CellDesign design) {
		
		HashSet<LibraryCell> uniqueLibraryCells = new HashSet<LibraryCell>();
		
		for (Cell c : design.getCells()) {			
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
		EdifCellInterface cellInterface = createTopLevelInterface(design, topLevelCell);
		topLevelCell.setInterface(cellInterface);
		
		// create the cell instances
		for (Cell cell : design.getCells()) {
			
			if (cell.isPort())
				continue;
			
			EdifCell edifLibCell = cellMap.get(cell.getLibCell());	
			topLevelCell.addSubCell( createEdifCellInstance(cell, topLevelCell, edifLibCell) );
		}
		
		// create the net instances
		for (CellNet net : design.getNets()) {
		 	topLevelCell.addNet(createEdifNet(net, topLevelCell));
		}
			
		topLevelCell.addPropertyList(createEdifPropertyList(design.getProperties()));
		return topLevelCell; 
	}
	
	/*
	 * Creates the EDIF interface for the top level cell
	 * 
	 * TODO: This is only guaranteed to work with netlists imported from Vivado. 
	 * 		 Update once top level ports are added to RapidSmith
	 */
	private static EdifCellInterface createTopLevelInterface(CellDesign design, EdifCell topLevelEdifCell) throws EdifNameConflictException, InvalidEdifNameException {
		
		EdifCellInterface cellInterface = new EdifCellInterface(topLevelEdifCell);
		
		HashMap<String, Integer> portWidthMap = new HashMap<String, Integer>();
		HashMap<String, Integer> portDirectionMap = new HashMap<String, Integer>();
		
		for (Cell cell : design.getCells()) {
			
			if (cell.isPort()) {
				String portName = cell.getName().split("\\[")[0];
				Integer count = portWidthMap.getOrDefault(portName, 0);
				portWidthMap.put(portName, count + 1);
				
				if (count == 0) {
					portDirectionMap.put(portName, PortDirection.isInputPort(cell) ? EdifPort.IN : EdifPort.OUT);
				}
			}			
		}
		
		for (String portName : portWidthMap.keySet()) {
						
			int portWidth = portWidthMap.get(portName);		
			EdifNameable edifPortName = (portWidth == 1) ? 
									createEdifNameable(portName) : 
									new RenamedObject(portName, String.format("%s[%d:0]", portName, portWidth-1));
			cellInterface.addPort(edifPortName, portWidth, portDirectionMap.get(portName));
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
	private static EdifNet createEdifNet(CellNet cellNet, EdifCell edifParentCell) {
		EdifNet edifNet = new EdifNet(createEdifNameable(cellNet.getName()), edifParentCell);
		
		// create the port references for the edif net
		for (CellPin cellPin : cellNet.getPins()) {
			
			Cell parentCell = cellPin.getCell();
			
			EdifPortRef portRef = parentCell.isPort() ?
									createEdifPortRefFromPort(parentCell, edifParentCell, edifNet) : 
									createEdifPortRefFromCellPin(cellPin, edifParentCell, edifNet) ;
			
			edifNet.addPortConnection(portRef);
		}
					
		return edifNet;
	}
	
	/*
	 * Creates a port reference (connection) for an EDIF net from a top-level port cell.
	 */
	private static EdifPortRef createEdifPortRefFromPort(Cell port, EdifCell edifParent, EdifNet edifNet) {
		
		String[] toks = port.getName().split("\\[");
		
		assert(toks.length == 1 || toks.length == 2);
		
		EdifPort edifPort = edifParent.getPort(toks[0]);
		
		int portIndex = 1; 
		if (toks.length == 2) {
			portIndex = Integer.parseInt(toks[1].substring(0, toks[1].length()-1));
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
		int reversedIndex = reverseBusIndex(port.getWidth(), busMember);  
		EdifSingleBitPort singlePort = new EdifSingleBitPort(port, reversedIndex);
		return new EdifPortRef(edifNet, singlePort, cellInstance);
	}
	
	/*
	 * Converts a collection of RapidSmith properties to an EDIF property list
	 */
	private static PropertyList createEdifPropertyList (Collection<Property> properties) {
		PropertyList edifProperties = new PropertyList();
		
		for (Property prop : properties) {
			// The key and value of the property need sensible toString() methods when exporting to EDIF
			// this function is for creating properties for printing only!
			// TODO: make sure to inform the user of this 
			edu.byu.ece.edif.core.Property edifProperty = new edu.byu.ece.edif.core.Property(prop.getKey().toString(), prop.getValue().toString());
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
		
		HashMap<String, Integer> pinWidthMap = new HashMap<String, Integer>();
		HashMap<String, Integer> pinDirectionMap = new HashMap<String, Integer>();
		
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
				throw new UnsupportedOperationException("Invalid Pin Direction!");
		}
	}
}