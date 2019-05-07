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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;

import static edu.byu.ece.rapidSmith.util.Exceptions.*;

/**
 * This class is used for parsing and writing placement XDC files in a TINCR checkpoint.
 * Placement.xdc files are used to specify the physical location of cells in a Vivado netlist.
 * 
 * @author Thomas Townsend
 *
 */
public class XdcPlacementInterface {

	private final CellDesign design;
	private final Device device;
	private final CellLibrary libCells;
	private static final String BUFFER_INIT_STRING = "2'h2";
	private int currentLineNumber;
	private String currentFile;
	private final Map<BelPin, CellPin> belPinToCellPinMap;
	private Map<String, String> oocPortMap; // Map from port name to the associated partition pin's node
	private Collection<CellNet> multiPortSinkNets; // nets with more than one port as a sink


	public XdcPlacementInterface(CellDesign design, Device device) {
		this.design = design;
		this.device = device;
		libCells = null;
		belPinToCellPinMap = new HashMap<>();
	}

	public XdcPlacementInterface(CellDesign design, Device device, CellLibrary libCells) {
		this.design = design;
		this.device = device;
		this.libCells = libCells;
		belPinToCellPinMap = new HashMap<>();
	}

	// Get all nets with more than one port as a sink
	private Collection<CellNet> getMultiPortSinkNets() {
		Collection<CellNet> nets = new ArrayList<>();
		for (CellNet net : design.getNets()) {
			if (net.getPins().stream().filter(cellPin -> cellPin.getDirection().equals(PinDirection.IN) && cellPin.getCell().isPort()).count() > 1)
				nets.add(net);
		}
		return nets;
	}

	/**
	 * @return the oocPortMap
	 */
	public Map<String, String> getOocPortMap() {
		return oocPortMap;
	}

	/**
	 * @param oocPortMap the oocPortMap to set
	 */
	public void setOocPortMap(Map<String, String> oocPortMap) {
		this.oocPortMap = oocPortMap;
	}

	/**
	 * Applies the placement constraints from the TINCR checkpoint files
	 * to the RapidSmith2 Cell Design.
	 *  
	 * @param xdcFile Placement.xdc file
	 * @throws IOException
	 */
	public void parsePlacementXDC(String xdcFile) throws IOException {
		
		currentFile = xdcFile;
		LineNumberReader br = new LineNumberReader(new BufferedReader(new FileReader(xdcFile)));
		String line;
		// Regex used to split lines via whitespace
		Pattern whitespacePattern = Pattern.compile("\\s+");
		
		while ((line = br.readLine()) != null) {
			currentLineNumber = br.getLineNumber();
			
			String[] toks = whitespacePattern.split(line);
			
			switch (toks[0]) {
				case "LOC" : applyCellPlacement(toks);
					break;
				case "PINMAP" : applyCellPinMappings(toks);
					break;
				case "PACKAGE_PIN" : applyPortPlacement(toks) ;
					break;
				case "IPROP" : applyInternalCellProperty(toks) ; 
					break;
				case "PART_PIN" : processPartPin(toks);
					break;
				case "VCC_PART_PINS":
					processStaticPartPins(toks, true);
					break;
				case "GND_PART_PINS":
					processStaticPartPins(toks, false);
					break;
				default :
					throw new ParseException(String.format("Unrecognized Token: %s \nOn %d of %s", toks[0], currentLineNumber, currentFile));
			}
		}

		br.close();
	}

	/**
	 * Processes the "VCC_PART_PINS" or "GND_PART_PINS" token in the static_resources.rsc of a RSCP. These are partition pins that need to be driven with VCC/GND and do not have a normal partition pin.
	 * Expected Format: VCC_PART_PINS partPinName partPinName ...
	 * @param toks An array of space separated string values parsed from the placement.rsc
	 */
	private void processStaticPartPins(String[] toks, boolean isVcc) {
		for (int i = 1; i < toks.length; i++) {

			if (oocPortMap == null) {
				oocPortMap = new HashMap<>();
			}
			String oocPortVal = isVcc? "VCC" : "GND";

			oocPortMap.put(toks[i], oocPortVal);
			String portName = toks[i];
			Cell portCell = design.getCell(portName);

			// Remove the port cell's pin from the design (they are outside of the partial device)
			assert (portCell.getPins().size() == 1);
			CellPin cellPin = portCell.getPins().iterator().next();
			CellNet net = cellPin.getNet();
			List<CellPin> pins = new ArrayList<>();

			// TODO: In what situations will the net not be null?
			// An RM synthesized and placed by Vivado has a null net.
			// Detach the port's pin from its current net
			//assert (net != null);
			if (net != null) {
				net.disconnectFromPin(cellPin);
				pins.addAll(net.getPins());
				net.disconnectFromPins(pins);
				design.removeNet(net);
			}

			// Re-assign the net's pins to either VCC or GND
			CellNet staticNet = isVcc? design.getVccNet() : design.getGndNet();
			staticNet.connectToPins(pins);

			CellPin partPin = new PartitionPin(portName, null, PinDirection.IN);
			portCell.attachPartitionPin(partPin);

			// Make the partition pin point to the appropriate global static net
			partPin.setPinToGlobalNet(staticNet);
		}
	}

	/**
	 * Processes the "PART_PIN" token in the placement.rsc of a RSCP. Specifically,
	 * this function adds the OOC port and corresponding port wire to the oocPortMap
	 * data structure for later processing.
	 *
	 * Expected Format: PART_PIN PortName Tile/Wire Direction
	 * @param toks An array of space separated string values parsed from the placement.rsc
	 */
	private void processPartPin(String[] toks) {

		assert (toks.length == 4) : String.format("Token error on line %d: Expected format is \"PART_PIN\" PortName Tile/Wire Direction", this.currentLineNumber);

		if (this.oocPortMap == null) {
			this.oocPortMap = new HashMap<>();
		}

		oocPortMap.put(toks[1], toks[2]);

		String portName = toks[1];
		Cell portCell = tryGetCell(portName);

		String[] wireToks = toks[2].split("/");
		Tile tile = tryGetTile(wireToks[0]);
		int wireEnum;
		if (tile.getType() == TileType.valueOf(device.getFamily(), "OOC_WIRE")) {
			wireEnum = tryGetWireEnum(wireToks[0] + "/" + wireToks[1]);
		}

		else
			wireEnum = tryGetWireEnum(wireToks[1]);
		TileWire partPinWire = new TileWire(tile, wireEnum);

		String direction = toks[3];
		PinDirection partPinDirection;
		switch (direction) {
			case "IN" : partPinDirection = PinDirection.IN; break;
			case "OUT" : partPinDirection = PinDirection.OUT; break;
			case "INOUT" :
			default: throw new AssertionError("Invalid direction");
		}

		// Create and add the partition pin to the design
		addPartitionPin(portCell, partPinWire, partPinDirection);

		// Given the partition pin wire, mark all wires in the node as reserved
		for (Wire wire : partPinWire.getWiresInNode()) {
			CellPin partPin = portCell.getPin(portName);
			design.addReservedWire(wire, partPin.getNet());
		}
	}

	private void addPartitionPin(Cell portCell, TileWire partPinNode, PinDirection partPinDirection) {
		assert (portCell.getPins().size() == 1);

		if (multiPortSinkNets == null)
			multiPortSinkNets = getMultiPortSinkNets();

		CellPin portCellPin = portCell.getPins().iterator().next();
		CellNet net = portCellPin.getNet();

		switch (partPinDirection) {
			case OUT: // If the partition pin is a driver from the RM's perspective
				// Create the partition pin
				CellPin partPin = new PartitionPin(portCell.getName(), partPinNode, partPinDirection);
				portCell.attachPartitionPin(partPin);

				// If the EDIF came from Vivado, there may be no net driving this partition pin
				// This depends on the implementation steps that have been completed.
				if (net == null) {
					net = new CellNet(partPin.getPortName(), NetType.WIRE);
					design.addNet(net);
				}
				else {
					// Detach the port cell pin from the net
					net.disconnectFromPin(portCellPin);
				}

				// Attach the partition pin to the net
				// Case 1: The static side is driving the partition pin, but the RM has no active loads for it.
				if (net.getFanOut() == 0) {
					// Create a LUT1 buffer. The partition pin will drive this LUT, but the LUT's output will go nowhere.
					Cell lutCell = new Cell("IN_BUF_Inserted_" + partPin.getPortName(), libCells.get("LUT1"));
					design.addCell(lutCell);
					lutCell.getProperties().update(new Property("INIT", PropertyType.EDIF, BUFFER_INIT_STRING));

					// Aattach the net to the partition pin and LUT1 buffer.
					net.connectToPin(partPin);
					net.connectToPin(lutCell.getPin("I0"));

				}
				else {
					// Normal case
					net.connectToPin(partPin);
				}
				break;
			case IN: // If the partition pin is a sink from the RM's perspective

				// Create the partition pin and attach it to the port.
				partPin = new PartitionPin(portCell.getName(), partPinNode, partPinDirection);
				portCell.attachPartitionPin(partPin);

				// If the EDIF came from Vivado, there may be no net driving this partition pin
				// This depends on the implementation steps that have been completed.
				if (net == null) {
					net = design.getGndNet();
				}
				else {
					// Detach the port cell pin from the net
					net.disconnectFromPin(portCellPin);
				}

				// Attach the partition pin to the net

				// Case 2: The RM is driving the partition pin with VCC or GND.
				// Case 3: The RM is driving additional partition pins with the same net.
				// Case 4: The RM is driving this partition pin with another partition pin / port
				// Whether it is considered a port or partition pin depends only on whether or not the source net was
				// processed first.
				if (net.isStaticNet() || multiPortSinkNets.contains(net) || net.getSourcePin().isPartitionPin() || net.getSourcePin().getCell().isPort()) {
					// Make a LUT1 buffer and drive it with the net.
					String bufferName = net.getName() + "_InsertedInst_" + partPin.getPortName();
					Cell lutCell = new Cell(bufferName, libCells.get("LUT1"));
					lutCell.getProperties().update(new Property("INIT", PropertyType.EDIF, BUFFER_INIT_STRING));
					design.addCell(lutCell);
					net.connectToPin(lutCell.getPin("I0"));

					// Make another net and connect it to the output of the LUT1 buffer and the partition pin.
					CellNet partPinDriverNet = new CellNet(net.getName() + "_InsertedNet_" + partPin.getPortName(), NetType.WIRE);
					design.addNet(partPinDriverNet);
					partPinDriverNet.connectToPin(lutCell.getPin("O"));
					partPinDriverNet.connectToPin(partPin);
				}
				else {
					// Normal case
					net.connectToPin(partPin);
				}
				break;
			case INOUT:
			default:
				throw new AssertionError("Invalid direction");

		}
	}


	// TODO: Don't duplicate
	/**
	 * Tries to retrieve the integer enumeration of a wire name in the currently loaded device <br>
	 * If the wire does not exist, a ParseException is thrown <br>
	 */
	private int tryGetWireEnum(String wireName) {

		Integer wireEnum = device.getWireEnumerator().getWireEnum(wireName);

		if (wireEnum == null) {
			throw new ParseException(String.format("Wire: \"%s\" does not exist in the current device. \n"
					+ "On line %d of %s", wireName, currentLineNumber, currentFile));
		}

		return wireEnum;
	}

	// TODO: Don't have this function in both Placement and Routing interfaces.
	/**
	 * Tries to retrieve the Tile object with the given name from the currently
	 * loaded device. If no such tile exists, a {@link ParseException} is thrown.
	 *
	 * @param tileName Name of the tile to get a handle of
	 * @return {@link Tile} object
	 */
	private Tile tryGetTile(String tileName) {
		Tile tile = device.getTile(tileName);

		// TODO: Check that the node is exactly the right one. ie make sure the true tile name matches as well.
		if (tile == null && design.getImplementationMode() == ImplementationMode.RECONFIG_MODULE) {
			// Assume the tile is outside the partial device boundaries.
			tile = device.getTile("OOC_WIRE_X0Y0");
		}

		if (tile == null) {
			throw new ParseException("Tile \"" + tileName + "\" not found in device " + device.getPartName() + ". \n"
					+ "On line " + this.currentLineNumber + " of " + currentFile);
		}
		return tile;
	}
	
	private void applyCellPlacement(String[] toks) {
		
		Cell cell = tryGetCell(toks[1]);
		Site site = tryGetSite(toks[2]);
		
		String siteType = toks[3];
		site.setType(SiteType.valueOf(device.getFamily(), siteType));
		
		Bel bel = tryGetBel(site, toks[4]);
		
		design.placeCell(cell, bel);
	}
	
	private void applyCellPinMappings(String[] toks) {
		
		Cell cell = tryGetPlacedCell(toks[1]);
		Bel bel = cell.getBel();
		
		for (int i = 2; i < toks.length; i++) {
			String[] pinmap = toks[i].split(":");

			// If pinmap.length = 1, this mean the cell pin has no belPinMapping.
			if (pinmap.length > 1) {
				CellPin cellPin = tryGetCellPin(cell, pinmap[0]);
				
				for (int j = 1; j < pinmap.length; j++) {
					BelPin belPin = tryGetBelPin(bel, pinmap[j]);
					cellPin.mapToBelPin(belPin);
					belPinToCellPinMap.put(belPin, cellPin);
				}
			}
		}
	}
	
	private void applyPortPlacement(String[] toks) {
		
		if (toks.length != 4) {
			throw new ParseException("PACKAGE_PIN declaration should be followed by 3 tokens: cell site bel.\n"
					+ "On line: " + currentLineNumber + " of " + currentFile);
		}
		
		Cell cell = tryGetCell(toks[1]);
		
		if (!cell.isPort()) {
			cell = tryGetCell(toks[1] + "_rsport");
		}
		
		Site site = tryGetSite(toks[2]);
		Bel bel = tryGetBel(site, toks[3]);
				
		design.placeCell(cell, bel);
		
		assert (cell.getPins().size() == 1) : "PAD cell should only have one pin";
		assert (bel.getBelPins().count() == 1) : "PAD BEL " + site.getName() + "/" + bel.getName() + " should only have one pin. but has " + bel.getBelPins().count();
		
		BelPin belPin = bel.getBelPins().findFirst().get();
		CellPin cellPin = cell.getPins().iterator().next();
		
		cellPin.mapToBelPin(belPin);
		belPinToCellPinMap.put(belPin, cellPin);
	}

	/*
	 * Applies a property to an internal cell based on the tokens read from the placement.rsc file
	 * Expected format of toks : "IPROP cellName propertyName propertyValue"
	 */
	private void applyInternalCellProperty(String[] toks) {
		
		// throw an exception if the number of tokens on the line is not correct
		if (toks.length != 4) {
			throw new ParseException("Expected 3 parameters after token IPROP, found " + toks.length + " instead\n" 
					+ "On line " + this.currentLineNumber + " of " + currentFile);
		}
		
		// add the property to the cell
		Cell cell = tryGetCell(toks[1]);
		cell.getProperties().update(new Property(toks[2], PropertyType.EDIF, toks[3]));
	}

	/**
	 * Returns the map of BelPin->CellPin mapping after the placement xdc
	 * has been applied to the design. Should be called after parsePlacementXDC
	 * is called.
	 * 
	 * @return Map from BelPin to the CellPin that is placed on it
	 */
	public Map<BelPin, CellPin> getPinMap() {
		return belPinToCellPinMap;
	}
	
	/**
	 * Tries to retrieve the Cell object with the given name
	 * from the currently loaded design. If the cell does not exist,
	 * a ParseException is thrown
	 * 
	 * @param cellName Name of the cell to retrieve
	 */
	private Cell tryGetCell(String cellName) {
		
		Cell cell = design.getCell(cellName);
		
		if (cell == null) {
			throw new ParseException("Cell \"" + cellName + "\" not found in the current design. \n" 
									+ "On line " + this.currentLineNumber + " of " + currentFile);
		}
		
		return cell;
	}
	
	private Cell tryGetPlacedCell(String cellName) {
		Cell cell = tryGetCell(cellName);
		
		if (!cell.isPlaced()) {
			throw new ParseException("Cell \"" + cellName + "\" not placed. Cannot apply a pin mapping.\n"
									+ "On line " + this.currentLineNumber + " of " + currentFile);
		}
		
		return cell;
	}
	
	/**
	 * Tries to retrieve a CellPin object on the specified Cell parameter.
	 * If the pin does not exist, a ParseException is thrown.
	 * 
	 * @param cell Cell which the pin is attached
	 * @param pinName Name of the pin
	 * @return CellPin
	 */
	private CellPin tryGetCellPin(Cell cell, String pinName) {
		
		CellPin pin = cell.getPin(pinName);
		
		if (pin == null) {
			throw new ParseException(String.format("CellPin: \"%s/%s\" does not exist in the current device"
												 + "On line %d of %s", cell.getName(), pinName, currentLineNumber, currentFile));
		}
		
		return pin;
	}
	
	/**
	 * Tries to retrieve the Site object with the given site name
	 * from the currently loaded device. If the site does not exist
	 * a ParseException is thrown
	 * 
	 * @param siteName Name of the site to retrieve
	 */
	private Site tryGetSite(String siteName) {
		
		Site site = device.getSite(siteName);
		
		if (site == null) {
			throw new ParseException("Site \"" + siteName + "\" not found in the current device. \n"
									+ "On line " + this.currentLineNumber + " of " + currentFile);
		}
		
		return site;
	}
		
	/**
	 * Tries to retrieve a BEL object from the currently loaded device. 
	 * If the BEL does not exist, a ParseException is thrown. 
	 * 
	 * @param site Site where the BEL resides
	 * @param belName Name of the BEL within the site
	 * @return Bel
	 */
	private Bel tryGetBel(Site site, String belName) {
		
		Bel bel = site.getBel(belName);
		
		if (bel == null) {
			throw new ParseException(String.format("Bel: \"%s/%s\" does not exist in the current device"
												 + "On line %d of %s", site.getName(), belName, currentLineNumber, currentFile));
		}
		
		return bel;
	}
	
	/**
	 * Tries to retrieve a BelPin object from the currently loaded device
	 * If the pin does not exist, a ParseException is thrown.
	 * 
	 * @param bel Bel which the pin is attached
	 * @param pinName Name of the bel pin
	 * @return BelPin
	 */
	private BelPin tryGetBelPin(Bel bel, String pinName) {
		
		BelPin pin = bel.getBelPin(pinName);
		
		if (pin == null) {
			throw new ParseException(String.format("BelPin: \"%s/%s\" does not exist in the current device.\n"
												 + "On line %d of %s", bel.getName(), pinName, currentLineNumber, currentFile));
		}
		
		return pin;
	}
		
	/**
	 * Creates a placement.xdc file from the cells of the given design 
	 * This file can be imported into Vivado to constrain the cells to a physical location
	 * 
	 * @param xdcOut Output placement.xdc file location
	 * @throws IOException
	 */
	public void writePlacementXDC(String xdcOut) throws IOException {
		
		try (BufferedWriter fileout = new BufferedWriter (new FileWriter(xdcOut)) ) {

			Iterator<Cell> cellIt = sortCellsForXdcExport(design).iterator();
			
			// All cells are assumed placed in this while loop
			while (cellIt.hasNext()) {
				Cell cell = cellIt.next();
				Site site = cell.getSite();
				Bel bel = cell.getBel();

				String cellname = cell.getName();
				
				// ports need a package pin reference, and aren't placed in Vivado
				if (cell.isPort()) {
					PackagePin packagePin = device.getPackagePin(bel);
					// if the port is not mapped to a valid package pin, thrown an exception
					if (packagePin == null) {
						if (device.getPackagePins().isEmpty()) {
							throw new ImplementationException("Device " + device.getPartName() + " is missing package pin information: cannot generate TCP without it.\n"
									+ "To generate the package pin information and add it to your device follow these three steps: \n"
									+ "1.) Run the Tincr command \"tincr::create_xml_device_info\" for your part.\n"
									+ "2.) Store the generated XML file to the devices/family directory which corresponds to your part.\n"
									+ "3.) Run the DeviceInfoInstaller in the util package to add the package pins to the device");
						}
						
						throw new ImplementationException("Cannot export placement information for port cell " + cellname + ".\n"
								+ "Package Pin for BEL " + bel.getFullName() + " cannot be found.");
					}
					fileout.write(String.format("set_property PACKAGE_PIN %s [get_ports {%s}]\n", packagePin.getName(), cellname));
				}
				else {
					fileout.write(String.format("set_property BEL %s.%s [get_cells {%s}]\n", site.getType().name(), bel.getName(), cellname));
					fileout.write(String.format("set_property LOC %s [get_cells {%s}]\n", site.getName(), cellname));
										
					//TODO: Update this function when more cells with LOCK_PINS are discovered
					if (cell.isLut()) { 
						fileout.write("set_property LOCK_PINS { ");
						for(CellPin cp: cell.getInputPins()) {
							if (!cp.isPseudoPin() && cp.getMappedBelPin() != null) {
								fileout.write(String.format("%s:%s ", cp.getName(), cp.getMappedBelPin().getName()));
							}
						}
						
						fileout.write("} [get_cells {" + cellname + "}]\n");
					}
				}
			}
		}
	}

	/*
	 * Sorts the cells of the design in the order required for TINCR export.
	 * Cells that are unplaced are not included in the sorted list. 
	 * Uses a bin sorting algorithm to have a complexity of O(n). 
	 * 
	 * TODO: Add <is_lut>, <is_carry>, and <is_ff> tags to cell library
	 */
	private Stream<Cell> sortCellsForXdcExport(CellDesign design) {
		
		design.getDevice().getAllSitesOfType(SiteType.valueOf(design.getFamily(), "SLICEL"));
		
		
		// cell bins
		ArrayList<Cell> sorted = new ArrayList<>(design.getCells().size());		
		ArrayList<Cell> lutCellsH5 = new ArrayList<>();
		ArrayList<Cell> lutCellsD5 = new ArrayList<>();
		ArrayList<Cell> lutCellsABC5 = new ArrayList<>();
		ArrayList<Cell> lutCellsH6 = new ArrayList<>();
		ArrayList<Cell> lutCellsD6 = new ArrayList<>();
		ArrayList<Cell> lutCellsABC6 = new ArrayList<>();
		ArrayList<Cell> carryCells = new ArrayList<>();
		ArrayList<Cell> ffCellsA = new ArrayList<>();
		ArrayList<Cell> ffCellsB = new ArrayList<>();
		ArrayList<Cell> ffCellsC = new ArrayList<>();
		ArrayList<Cell> ffCellsD = new ArrayList<>();
		ArrayList<Cell> ff5Cells = new ArrayList<>();
		ArrayList<Cell> muxCells = new ArrayList<>();

		// traverse the cells and drop them in the correct bin
		Iterator<Cell> cellIt = design.getLeafCells().iterator();
		
		while (cellIt.hasNext()) {
			Cell cell = cellIt.next();
			
			// only add cells that are placed to the list
			if (!cell.isPlaced()) {
				continue;
			}
			
			String libCellName = cell.getLibCell().getName();
			String belName = cell.getBel().getName();
			
			if (belName.endsWith("6LUT")) {
				if (belName.contains("H")) {
					lutCellsH6.add(cell);
				}
				else if (belName.contains("D")) {
					lutCellsD6.add(cell);
				}
				else {
					lutCellsABC6.add(cell);
				}
			}
			else if (belName.endsWith("5LUT")) {
				if (belName.contains("H")) {
					lutCellsH5.add(cell);
				}
				else if (belName.contains("D")) {
					lutCellsD5.add(cell);
				}
				else {
					lutCellsABC5.add(cell);
				}
			}
			else if (libCellName.startsWith("CARRY")) {
				carryCells.add(cell);
			}
			else if (belName.endsWith("5FF")) {
				ff5Cells.add(cell);
			}
			else if (belName.endsWith("FF")) {
				if (belName.contains("A")) {
					ffCellsA.add(cell);
				}
				else if (belName.contains("B")) {
					ffCellsB.add(cell);
				}
				else if (belName.contains("C")) {
					ffCellsC.add(cell);
				}
				else {
					ffCellsD.add(cell);
				}
			}
			else if(belName.endsWith("MUX")) {
				muxCells.add(cell);
			}
			else {
				sorted.add(cell);
			}
		}

		// append all other cells in the correct order
		return Stream.of(sorted.stream(),
				lutCellsH5.stream(),
				lutCellsD5.stream(), 
				lutCellsABC5.stream(), 
				lutCellsH6.stream(),
				lutCellsD6.stream(), 
				lutCellsABC6.stream(),
				ffCellsD.stream(),
				ffCellsC.stream(),
				ffCellsB.stream(),
				ffCellsA.stream(),
				carryCells.stream(),
				muxCells.stream(),
				ff5Cells.stream())
				.flatMap(Function.identity());
	}
}
