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
package edu.byu.ece.rapidSmith.device.creation;

import edu.byu.ece.rapidSmith.util.PartialDeviceInstaller;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.device.families.FamilyInfo;
import edu.byu.ece.rapidSmith.device.families.FamilyInfos;
import edu.byu.ece.rapidSmith.util.Exceptions;

import java.util.*;
import java.util.Map.Entry;

/**
 * This class can be used to create small device files that represent
 * a sub-region of a larger Xilinx FPGA part. An example for a 3X3
 * partial device is shown below: <br>
 * 	
 * <pre>
 * ------------------------------------------- <br>
 * |         |       |        ||             | <br>
 * |  INT_R  |  CLB  |  VBRK  ||  OOC_WIRE  | <br>
 * |         |       |        ||             | <br>
 * |------------------------------------------ <br>
 * |         |       |        ||             | <br>
 * |  INT_R  |  CLB  |  VBRK  ||    NULL     | <br>
 * |         |       |        ||             | <br>
 * |------------------------------------------ <br>
 * |         |       |        ||             | <br>
 * |  INT_R  |  CLB  |  VBRK  ||    NULL     | <br>
 * |         |       |        ||             | <br>
 * |------------------------------------------ <br>
 *                                   |         <br>
 *                     Extra Column for OOC_WIRE tile <br>
 * </pre>
 * As can be seen, when creating a 3X3 device file, an extra column is added to the device.
 * The purpose of this column is to represent out-of-context wires for the partial device.
 * These are either IWIREs or OWIREs. OWIREs are wires that start <b>within</b> the partial
 * device, but leave the partial device boundaries. IWIREs are wires that start <b>outside</b>
 * the partial device boundaries, but enter the partial device. These OOC wires are all tile
 * wires that are added to the "OOC_WIRE" tile. All other tiles in the rightmost column are
 * set to be of type NULL so they take up as little memory as possible.
 * 
 * <p>
 * Run the Java class {@link PartialDeviceInstaller} to create a new partial device and
 * install it to the corresponding family of the RapidSmith2 "device" folder.
 *
 */
public class PartialDeviceGenerator {

	/**
	 * Creates a <b>rectangular</b> partial device from a larger Xilinx FPGA part
	 * 
	 * @param newDeviceName The name of the new device. This name can be used to load the device once a RS2 device file has been created.
	 * @param oldDevice The original larger device to create a subsection from
	 * @param tileAName The name of the first corner tile of the rectangular region
	 * @param tileBName The name of the second corner tile of the rectangular region
	 * 
	 * @return a {@link Device} object which represents a sub-section of the original part
	 */
	public Device generatePartialDevice(String newDeviceName, Device oldDevice, String tileAName, String tileBName) {

		// Verify that the specified tiles are valid locations in the given device
		Tile tileA = oldDevice.getTile(tileAName);
		Tile tileB = oldDevice.getTile(tileBName);

		if (tileA == null || tileB == null) {
			throw new IllegalArgumentException("Invalid tile locations specified " + tileAName + " " + tileBName);
		}

		// Get the top left and bottom right tiles
		int topLeftRow = (tileA.getRow() < tileB.getRow()) ? tileA.getRow() : tileB.getRow();
		int topLeftCol = (tileA.getColumn() < tileB.getColumn()) ? tileA.getColumn() : tileB.getColumn();
		int bottRightRow = (tileA.getRow() > tileB.getRow()) ? tileA.getRow() : tileB.getRow();
		int bottRightCol = (tileA.getColumn() > tileB.getColumn()) ? tileA.getColumn() : tileB.getColumn();

		Tile topLeft = oldDevice.getTile(topLeftRow, topLeftCol);
		Tile bottomRight = oldDevice.getTile(bottRightRow, bottRightCol);
		
		// Step One: Create a new device and copy the properties of the old device to the new device
		Device newDevice = new Device();		
		newDevice.setPartName(newDeviceName);
		copyDeviceProperties(oldDevice, newDevice);
		
		// Step Two: Create a tile array for the new device, and record the used tiles in the partial device
		createTileArray(newDevice, topLeft, bottomRight);
		Set<Tile> tileSet = generateTileSet(oldDevice, newDevice, topLeft);

		// Step Three: Copy the tile information from the main device file to the partial device.
		// The bounded wire connections are generated for each tile, and the unbounded wires and names
		// are recorded in the "toOutputWires" and "toInputWires"
		Map<String, List<TileWire>> toOutputWires = new LinkedHashMap<>();
		Map<String, List<TileWire>> toInputWires = new LinkedHashMap<>();
		for (int i = 0; i < newDevice.getRows(); i++) {
			for (int j = 0; j < newDevice.getColumns()-1; j++) {
				Tile newTile = newDevice.getTile(i, j);
				Tile oldTile = oldDevice.getTile(topLeft.getRow() + i, topLeft.getColumn() + j);
				copyTile(oldTile, newTile, tileSet, toOutputWires, toInputWires);
			}
		}

		// Step Four: create an OOC_WIRE tile in the upper-right corner of the device, and add the OOC wires
		createOocWireTile(newDevice, toOutputWires, toInputWires);
		
		// Step Five: Set the types of the unused tiles in the last column to NULL;
		generateNullTiles(newDevice);
		
		// Set Six: Construct the tile and site maps in the device
		newDevice.constructTileMap();
		
		return newDevice;
	}
	
	/**
	 * Copies key properties from the old, large device to the new partial device.
	 * These properties currently include:
	 * <ul>
	 * <li> {@link FamilyType} (i.e. Artix7}
	 * <li> {@link WireEnumerator} Wires all have the same enumerations in the partial device
	 * 		as they did for the old device.
	 * <li> The list of {@link SiteTemplate}s which detail the internals of a site.
	 * <li> Wire objects that have a corresponding PIPRouteThrough.
	 * </ul>
	 *
	 * 
	 * @param oldDevice Original device file
	 * @param newDevice Partial device file
	 */
	private void copyDeviceProperties(Device oldDevice, Device newDevice) {
		newDevice.setFamily(oldDevice.getFamily());
		newDevice.setWireEnumerator(oldDevice.getWireEnumerator());
		newDevice.setSiteTemplates(oldDevice.getSiteTemplates());
		newDevice.setRouteThroughMap(oldDevice.getRouteThroughMap());
	}
	
	/**
	 * Creates a set of {@link Tile}s in the original device that needs to be included
	 * in the new partial device. This set is used to test if a wire leaves the partial
	 * device boundaries.
	 * 
	 * @param oldDevice Original {@link Device} object
	 * @param newDevice Partial {@link Device} object
	 * @param topLeft Top-left tile in the rectangular partial device region
	 * @return A set of {@link Tile} objects used in the partial region
	 */
	private Set<Tile> generateTileSet(Device oldDevice, Device newDevice, Tile topLeft) {
		FamilyInfo familyInfo = FamilyInfos.get(oldDevice.getFamily());
		
		// first pass: make a set of all tiles in the rectangular map
		Set<Tile> tileSet = new HashSet<>();
		for (int i = 0; i < newDevice.getRows(); i++) {
			for (int j = 0; j < newDevice.getColumns()-1; j++) {
				Tile newTile = oldDevice.getTile(topLeft.getRow() + i, topLeft.getColumn() + j);
						
				if (familyInfo.ioTiles().contains(newTile.getType())){
					// Print a warning if the Tile Type is an IOB.
					// It's still unclear if we want to allow IOB tiles in a small device
					System.out.println("WARNING: " + newTile.getName() + "(" + (topLeft.getColumn() + j) + ", " + (topLeft.getRow() + i) + ") is an IOB tile!"); 
				}
				tileSet.add(newTile);
			}
		}
		
		return tileSet;
	}
	
	/**
	 * Checks that the specified topLeft and bottomRight Tiles form a valid rectangular
	 * region, and creates the tile array for the new partial device based on this
	 * region. An extra column is added for the "OOC_WIRE" tile which contains
	 * all of the out-of-context wires in the device.
	 *  
	 * @param topLeft Top-left Tile in the partial region
	 * @param bottomRight Bottom-right Tile in the partial region
	 */
	private void createTileArray(Device device, Tile topLeft, Tile bottomRight) {
		
		int rows = bottomRight.getRow() - topLeft.getRow();
		
		if (rows < 0) {
			throw new Exceptions.ImplementationException("The bottom right tile needs to be to the right of the top left tile");
		}
		
		int cols = bottomRight.getColumn() - topLeft.getColumn();
		
		if (cols < 0) {
			throw new Exceptions.ImplementationException("The bottom right tile needs to be to the below the top left tile");
		}
		
		// Add an extra column of tiles for OOC wires
		rows += 1;
		cols += 2;
		
		device.createTileArray(rows, cols);
	}

	/**
	 * Adds a tile wire to a map of OOC wires, creating a new list if necessary
	 * @param wires map of wires to add to - either toInputWires or toOutputWires
	 * @param key the key (wire name) to use to look up in the map
	 * @param tileWire the tile wire to add to the map
	 */
	private void addToOocWireMap(Map<String, List<TileWire>> wires, String key, TileWire tileWire) {
		if (wires.get(key) == null) {
			List<TileWire> tileWires = new ArrayList<>();
			tileWires.add(tileWire);
			wires.put(key, tileWires);
		}
		else {
			wires.get(key).add(tileWire);
		}
	}

	/**
	 * Copies the properties of a {@link Tile} from the original device to the new partial device. 
	 * This includes:
	 * <ul>
	 * <li> The name of the tile for reference to the old device
	 * <li> {@link TileType}
	 * <li> Sites
	 * <li> WireSite map (see {@link Tile} source code)
	 * <li> The forward <b>and</b> reverse wire connections of the tile 
	 * </ul>
	 * 
	 * This function also records which wires leave the partial device boundaries
	 * so they can be included in the "OOC_WIRE" tile.
	 *
	 * @param oldTile Tile in the original device
	 * @param newTile Corresponding Tile in the partial device
	 * @param tileSet Set of tiles within the partial device region (used for determining if a wire leaves the region)
	 * @param toOutputWires Map of wire names to {@link TileWire}s that leave the partial device region. This is populated in this function.
	 * @param toInputWires Map of wire names to {@link TileWire}s that are sourced by wires outside the partial device region. This is populated in this function.
	 */
	private void copyTile(Tile oldTile, Tile newTile, Set<Tile> tileSet, Map<String, List<TileWire>> toOutputWires, Map<String, List<TileWire>> toInputWires) {
		newTile.setName(oldTile.getName());
		newTile.setType(oldTile.getType());
		newTile.setSites(oldTile.getSites());
		newTile.setWireSites(oldTile.getWireSites());
		
		// process the forward connections
		WireHashMap oldHashMap = oldTile.getWireHashMap();
		WireHashMap forwardMap = new WireHashMap();
		for (Integer sourceWire : oldHashMap.keySet() ) {
			
			ArrayList<WireConnection> bounded = new ArrayList<>();
			
			for (WireConnection conn : oldHashMap.get(sourceWire)) {
				Tile sinkTile = conn.getTile(oldTile);
				
				if (tileSet.contains(sinkTile)) {
					// the wire connection is within the small device
					WireConnection newConn = new WireConnection(conn.getWire(), conn.getRowOffset(), conn.getColumnOffset(), conn.isPIP());
					bounded.add(newConn);
				}
				else {
					// the wire connection leaves the small device region
					int destWire = conn.getWire();	
					String wireName = sinkTile.getName() + "/" + sinkTile.getDevice().getWireEnumerator().getWireName(destWire);
					TileWire tileWire = new TileWire(newTile, sourceWire);
					addToOocWireMap(toOutputWires, wireName, tileWire);
				}
			}
			
			// convert the array list to an array
			WireConnection[] connectionArray = new WireConnection[bounded.size()];
			for (int i = 0; i < bounded.size(); i++) {
				connectionArray[i] = bounded.get(i);
			}
			forwardMap.put(sourceWire, connectionArray);
		}
		// Add the forward wire connections to the tile
		newTile.setWireHashMap(forwardMap);
		
		// process the reverse connections
		WireHashMap oldReverseHashMap = oldTile.getReverseWireHashMap();
		WireHashMap reverseMap = new WireHashMap();
		for (Integer sourceWire : oldReverseHashMap.keySet()) {
			
			ArrayList<WireConnection> bounded = new ArrayList<>();
			
			for (WireConnection conn : oldReverseHashMap.get(sourceWire)) {
				Tile sinkTile = conn.getTile(oldTile);
				
				if (tileSet.contains(sinkTile)) {
					// the wire connection is within the small device
					WireConnection newConn = new WireConnection(conn.getWire(), conn.getRowOffset(), conn.getColumnOffset(), conn.isPIP());
					bounded.add(newConn);
				}
				else {
					// the wire is sourced from a wire outside the small device region
					int destWire = conn.getWire();
					String wireName = sinkTile.getName() + "/" + sinkTile.getDevice().getWireEnumerator().getWireName(destWire);
					TileWire tileWire = new TileWire(newTile, sourceWire);
					addToOocWireMap(toInputWires, wireName, tileWire);
				}
			}
			
			WireConnection[] connectionArray = new WireConnection[bounded.size()];
			for (int i = 0; i < bounded.size(); i++) {
				connectionArray[i] = bounded.get(i);
			}
			reverseMap.put(sourceWire, connectionArray);
		}
		// Add the reverse wire map to the tile
		newTile.setReverseWireConnections(reverseMap);		
	}
	
	/**
	 * Creates the out-of-context wire tile.
	 * 
	 * @param newDevice Partial device data structure
	 * @param toOutputWires Map of wire names to {@link TileWire} objects that leave the partial device
	 * @param toInputWires Map of wire names to {@link TileWire} objects that enter the partial device
	 */
	private void createOocWireTile(Device newDevice, Map<String, List<TileWire>> toOutputWires, Map<String, List<TileWire>> toInputWires) {
		Tile oocWiresTile = newDevice.getTile(0,  newDevice.getColumns() - 1);
		oocWiresTile.setName("OOC_WIRE_X0Y0");
		oocWiresTile.setType(TileType.valueOf(newDevice.getFamily(), "OOC_WIRE"));

		// create ooc wires
		createOocWires(newDevice, toOutputWires, toInputWires);
		createOocWiresTileWireMaps(oocWiresTile, toOutputWires, toInputWires);
	}
	
	/**
	 * Creates new wires that are needed for the partial device file and adds them 
	 * to the {@link WireEnumerator}. These wires include all wires in the new "OOC_WIRE" tile.
	 *  
	 * @param device Partial device data structure
	 * @param toOutputWires Map of Wire Names to {@link TileWire}s that leave the partial device region.
	 * @param toInputWires Map of Wire Names to {@link TileWire}s that are sourced by wires outside the partial device region.
	 */
	private void createOocWires(Device device, Map<String, List<TileWire>> toOutputWires, Map<String, List<TileWire>> toInputWires) {
		WireEnumerator we = device.getWireEnumerator();
		List<String> wireNames = new ArrayList<> (Arrays.asList(we.getWires()));
		Map<String, Integer> enumMap = we.getWireMap();
		
		// create a new tile wire for each output wire leaving the partial device
		for (String wire : toOutputWires.keySet()) {
			String wireName = "OWIRE:" + wire;
			wireNames.add(wireName);
			enumMap.put(wireName, enumMap.size());
		}
		
		// create a new tile wire for each input wire entering the partial device
		for (String wire : toInputWires.keySet()) {
			String wireName = "IWIRE:" + wire;
			wireNames.add(wireName);
			enumMap.put(wireName, enumMap.size());
		}

		// update the wirename array and wire map of the WireEnumerator accordingly
		we.setWires(wireNames.toArray(new String[0]));
		we.setWireMap(enumMap);
		
		device.setWireEnumerator(we);
	}

	/**
	 * Creates the forward and reverse wire connections for the "OOC_WIRE" tile.
	 *  
	 * @param oocWiresTile OOC_WIRE tile (top-right tile of the partial device)
	 * @param toOutput Map of wire names to {@link TileWire} objects that leave the partial device
	 * @param toInput Map of wire names to {@link TileWire} objects that enter the partial device
	 */
	private void createOocWiresTileWireMaps(Tile oocWiresTile, Map<String, List<TileWire>> toOutput, Map<String, List<TileWire>> toInput) {
		WireEnumerator we = oocWiresTile.getDevice().getWireEnumerator();
		
		// create the connections for the input OOC wires
		WireHashMap forwardMap = new WireHashMap();
		
		for (Entry<String, List<TileWire>> entry : toInput.entrySet()) {
			String wireName = "IWIRE:" + entry.getKey();
			WireConnection[] forwardConnections = new WireConnection[entry.getValue().size()];

			for (int i = 0; i < entry.getValue().size(); i++) {
				TileWire tileWire = entry.getValue().get(i);
				Tile pdTile = tileWire.getTile();
				int pdEnum = tileWire.getWireEnum();

				// create the forward connection from the OOC_WIRE tile -> small device tile
				int rowOffset = oocWiresTile.getRow() - pdTile.getRow();
				int colOffset = oocWiresTile.getColumn() - pdTile.getColumn();
				forwardConnections[i] = new WireConnection(pdEnum, rowOffset, colOffset, false);

				//create the reverse wire connection from the small device tile -> OOC_WIRE tile
				WireConnection pdTileConn = new WireConnection(we.getWireEnum(wireName), -rowOffset, -colOffset, false);
				WireHashMap pdReverseMap = pdTile.getReverseWireHashMap();
				pdReverseMap.put(pdEnum, addConnection(pdReverseMap.get(pdEnum), pdTileConn) );
			}

				// add the forward connections from the OOC_WIRE tile -> partial device tile
				forwardMap.put(we.getWireEnum(wireName), forwardConnections);
		}
		
		// create the connections for the output OOC wires
		WireHashMap reverseMap = new WireHashMap();
		for (Entry<String, List<TileWire>> entry : toOutput.entrySet()) {
			String wireName = "OWIRE:" + entry.getKey();
			WireConnection[] reverseConnections = new WireConnection[entry.getValue().size()];

			for (int i = 0; i < entry.getValue().size(); i++) {
				TileWire tileWire = entry.getValue().get(i);
				Tile pdTile = tileWire.getTile();
				int pdEnum = tileWire.getWireEnum();

				// create the reverse connection from the OOC_WIRE tile -> partial device tile
				int rowOffset = oocWiresTile.getRow() - pdTile.getRow();
				int colOffset = oocWiresTile.getColumn() - pdTile.getColumn();
				reverseConnections[i] = new WireConnection(pdEnum, rowOffset, colOffset, false);

				// create the forward connection from the partial device tile -> OOC_WIRE tile
				WireConnection pdTileConn = new WireConnection(we.getWireEnum(wireName), -rowOffset, -colOffset, false);
				WireHashMap pdWireMap = pdTile.getWireHashMap();
				pdWireMap.put(pdEnum, addConnection(pdWireMap.get(pdEnum), pdTileConn) );
			}

			// add the reverse connections from the OOC_WIRE tile -> partial device tile
			reverseMap.put(we.getWireEnum(wireName), reverseConnections);
		}
		
		// set the forward and reverse connections in the new tile
		oocWiresTile.setWireHashMap(forwardMap);
		oocWiresTile.setReverseWireConnections(reverseMap);
	}
	
	/**
	 * Adds a new {@link WireConnection} to an existing array of WireConnections.
	 * 
	 * @param connArray Old connection array
	 * @param newConn Wire connection to add
	 * 
	 * @return A new array of {@link WireConnection}s where the new connection is appended to the end.
	 * 		This returned array one size bigger than the original.
	 */
	private WireConnection[] addConnection(WireConnection[] connArray, WireConnection newConn) {
		WireConnection[] newArray = new WireConnection[connArray.length + 1];
		System.arraycopy(connArray, 0, newArray, 0, connArray.length);
		newArray[connArray.length] = newConn; 
		
		return newArray;
	}

	/**
	 * Sets the tile types of all tiles in the right-most column 
	 * to NULL (besides the OOC_WIRE tile)
	 *  
	 * @param device Partial device data structure
	 */
	private void generateNullTiles(Device device) {
		// Set the tile types of all unused tiles to NULL
		TileType nullType = TileType.valueOf(device.getFamily(), "NULL");
		for (int i = 1; i < device.getRows(); i++) {
			Tile nullTile =  device.getTile(i, device.getColumns()-1);
			nullTile.setType(nullType);
			nullTile.setName("NULL_X0Y" + (i-1));
		}
	}
}
