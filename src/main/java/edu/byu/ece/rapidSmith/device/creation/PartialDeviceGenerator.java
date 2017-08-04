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
package edu.byu.ece.rapidSmith.device.creation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.BelPinTemplate;
import edu.byu.ece.rapidSmith.device.BelTemplate;
import edu.byu.ece.rapidSmith.device.BondedType;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.PinDirection;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.SitePinTemplate;
import edu.byu.ece.rapidSmith.device.SiteTemplate;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileType;
import edu.byu.ece.rapidSmith.device.TileWire;
import edu.byu.ece.rapidSmith.device.WireConnection;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import edu.byu.ece.rapidSmith.device.WireHashMap;
import edu.byu.ece.rapidSmith.util.Exceptions;

/**
 * This class can be used to create small device files that represent
 * a sub-region of a larger Xilinx FPGA part. An example for a 3X3
 * partial device is shown below: <br>
 * 	
 * <pre>
 * ------------------------------------------- <br>
 * |         |       |        ||             | <br>
 * |  INT_R  |  CLB  |  VBRK  ||  HIER_PORT  | <br>
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
 *                     Extra Column for hierarchical ports <br>
 * </pre>
 * As can be seen, when creating a 3X3 device file,
 * an extra column is added to the device. The purpose of this column is to
 * represent hierarchical ports for the device. Hierarchical ports are for
 * wires which start <b>within</b> the partial device, but are routed
 * <b>outside</b> the small device boundaries. Each wire that fits this  
 * criteria is instead connected to an individual output port in the "HEIR_PORT" 
 * tile in the upper right corner of the device. Similarly, for wires
 * that start <b>outside</b> of the partial boundaries and are routed
 * <b> within</b> the small device, an input port is created and
 * which is connected to the sink wire in the device. For each port,
 * either an "OPORT" or "IPORT" site is created  within the "HEIR_PORT"
 * tile, with a single PAD BEL. All other tiles in the rightmost column are
 * set to be of type NULL so they take up as little memory as possible. 
 * 
 * <p>
 * Run the Java class {@link PartialDeviceInstaller} to create a new partial device and
 * install it to the corresponding family of the RapidSmith2 "device" folder.
 * 
 * <p>
 * Possible Improvements:
 * <ul>
 * <li> Add an option to NOT include reverse wire connections. Reverse wire connections are currently
 * 		being generated and so leaving them out will help reduce the size of the final device file
 * <li> Currently, PORT sites have the name format of "IPORT_0" or "OPORT_1". It would be
 *      better to have a naming convention of something like "OPORT:INT_R_X27Y134/SR1BEG1" 
 *      where the source or sink wire outside of the partial device is directly included in the
 *      port name. This can give users information about where the hierarchical port goes
 *      (or comes from) in the original device.  
 * <li> More error checking. Currently this class only checks that the specified corner tiles
 * 		are valid tiles in the large device and form a rectangle. Its unclear if we want to allow
 *      IOB tiles in a small device, so we may need to check for this.
 * <li> Add support for more generate functions with different options as parameters. For example
 * 		the function generate(String newDeviceName, Tile topLeft, int width, int height) is another
 * 		potentially useful way to create a partial device.
 * <li> Remove unused wires from the WireEnumerator so that it makes the data structure slightly smaller.
 * 		This also applies to the device site templates that aren't used.
 * <li> Add an area limit for the partial device. I don't think it makes sense to make a partial device
 *      more than 20X20, so this may be the limit
 * <li> Add support for general shapes and not just rectangles? I'm not sure how useful this would be.
 * <li> Integrate this function with the device browser. 
 * </ul>
 */
public class PartialDeviceGenerator {

	/**
	 * Creates a <b>rectangular</b> partial device from a larger Xilinx FPGA part
	 * 
	 * @param newDeviceName The name of the new device. This name can be used to load the device once a RS2 device file has been created.
	 * @param oldDevice The original larger device to create a subsection from
	 * @param topLeftTile The name of the top-left tile of the rectangular region
	 * @param bottomRightTile The name of the bottom-right tile of the rectangular region 
	 * 
	 * @return a {@link Device} object which represents a sub-section of the original part
	 */
	public Device generatePartialDevice(String newDeviceName, Device oldDevice, String topLeftTile, String bottomRightTile) {

		// Verify that the specified tiles are valid locations in the given device
		Tile topLeft = oldDevice.getTile(topLeftTile);
		Tile bottomRight = oldDevice.getTile(bottomRightTile);
		
		if (topLeft == null || bottomRight == null) {
			throw new IllegalArgumentException("Invalid tile locations specified " + topLeftTile + " " + bottomRightTile);
		}
		
		// Step one: Create a new device and copy the properties of the old device to the new device
		Device newDevice = new Device();		
		newDevice.setPartName(newDeviceName);
		copyDeviceProperties(oldDevice, newDevice);
		
		
		// Step two: Create a tile array for the new device, and record the used tiles in the partial device
		createTileArray(newDevice, topLeft, bottomRight);
		Set<Tile> tileSet = generateTileSet(oldDevice, newDevice, topLeft);
		
		
		// Step Three: Copy the tile information from the main device file to the partial device.
		// The bounded wire connections are generated for each tile, and the unbounded wires
		// are recorded in the "toOutputPorts" and "toInputPorts"
		List<TileWire> toOutputPorts = new ArrayList<>();
		List<TileWire> toInputPorts = new ArrayList<>();
		for (int i = 0; i < newDevice.getRows(); i++) {
			for (int j = 0; j < newDevice.getColumns()-1; j++) {
				Tile newTile = newDevice.getTile(i, j);
				Tile oldTile = oldDevice.getTile(topLeft.getRow() + i, topLeft.getColumn() + j);
				copyTile(oldTile, newTile, tileSet, toOutputPorts, toInputPorts);
			}
		}
				
		// Step Four: create a port Tile in the upper-right corner of the device, and generate 
		// the missing port connections from step 3  
		createPortTile(newDevice, toOutputPorts, toInputPorts); 
		
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
	 * <li> The list of {@link SiteTemplates} which detail the internals of a site
	 * </ul>
	 * 
	 * TODO: Make a copy of the WireEnumerator and SiteTemplates? If we are just creating 
	 *  a partial device and not using the older device in the same Java code run, we 
	 *  don't have to make a copy
	 * 
	 * @param oldDevice Original device file
	 * @param newDevice Partial device file
	 */
	private void copyDeviceProperties(Device oldDevice, Device newDevice) {
		newDevice.setFamily(oldDevice.getFamily());
		newDevice.setWireEnumerator(oldDevice.getWireEnumerator());
		newDevice.setSiteTemplates(oldDevice.getSiteTemplates());
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
		// first pass: make a set of all tiles in the rectangular map
		Set<Tile> tileSet = new HashSet<Tile>();
		for (int i = 0; i < newDevice.getRows(); i++) {
			for (int j = 0; j < newDevice.getColumns()-1; j++) {
				// TODO check if Tile type is an IOB and throw an error if it is, these should not be included in a small device file?
				tileSet.add(oldDevice.getTile(topLeft.getRow() + i, topLeft.getColumn() + j));
			}
		}
		
		return tileSet;
	}
	
	/**
	 * Checks that the specified topLeft and bottomRight Tiles form a valid rectangular
	 * region, and creates the tile array for the new partial device based on this
	 * region. An extra column is added for the "HEIR_PORT" tile which contains 
	 * all of the hierarchical ports in the device.
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
		
		// Add an extra column of tiles for ports
		rows +=1; 
		cols += 2;
		
		device.createTileArray(rows, cols);
	}
	
	/**
	 * Copies the properties of a {@link Tile} from the original device to the new partial device. 
	 * This includes:
	 * <ul>
	 * <li> The name of the tile for reference to the old device
	 * <li> {@link TileType}
	 * <li> Sites
	 * <li> Source and sink wires
	 * <li> WireSite map (see {@Tile} source code)
	 * <li> The forward <b>and</b> reverse wire connections of the tile 
	 * </ul>
	 * 
	 * This function also records which wires leave the partial device boundaries
	 * so they can be connected to ports in the "HEIR_PORT" tile. 
	 * 
	 * @param oldTile Tile in the original device
	 * @param newTile Corresponding Tile in the partial device
	 * @param tileSet Set of tiles within the partial device region (used for determining if a wire leaves the region)
	 * @param toOutputPorts Set of {@link TileWire}s that leave the partial device region. This is populated in this function.
	 * @param toInputPorts Set of {@link TileWire}s that are sourced by wires outside the partial device region. This is populated in this function.
	 */
	private void copyTile(Tile oldTile, Tile newTile, Set<Tile> tileSet, List<TileWire> toOutputPorts, List<TileWire> toInputPorts) {
		newTile.setName(oldTile.getName());
		newTile.setType(oldTile.getType());
		newTile.setSites(oldTile.getSites());
		newTile.setSinks(oldTile.getSinks().stream().map(w -> w.getWireEnum()).mapToInt(i -> i).toArray());
		newTile.setSources(oldTile.getSources().stream().map(w -> w.getWireEnum()).mapToInt(i -> i).toArray());
		newTile.setWireSites(oldTile.getWireSites());
		
		// process the forward connections
		WireHashMap oldHashMap = oldTile.getWireHashMap();
		WireHashMap forwardMap = new WireHashMap();
		for (Integer sourceWire : oldHashMap.keySet() ) {
			
			ArrayList<WireConnection> bounded = new ArrayList<WireConnection>();
			
			for (WireConnection conn : oldHashMap.get(sourceWire)) {
				Tile sinkTile = conn.getTile(oldTile);
				
				if (tileSet.contains(sinkTile)) {
					// the wire connection is within the small device
					WireConnection newConn = new WireConnection(conn.getWire(), conn.getRowOffset(), conn.getColumnOffset(), conn.isPIP());
					bounded.add(newConn);
				}
				else {
					// the wire connection leaves the small device region 
					toOutputPorts.add(new TileWire(newTile, sourceWire));
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
		for (Integer sourceWire : oldReverseHashMap.keySet() ) {
			
			ArrayList<WireConnection> bounded = new ArrayList<WireConnection>();
			
			for (WireConnection conn : oldReverseHashMap.get(sourceWire)) {
				Tile sinkTile = conn.getTile(oldTile);
				
				if (tileSet.contains(sinkTile)) {
					// the wire connection is within the small device
					WireConnection newConn = new WireConnection(conn.getWire(), conn.getRowOffset(), conn.getColumnOffset(), conn.isPIP());
					bounded.add(newConn);
				}
				else {
					// the wire is sourced from a wire outside the small device region
					toInputPorts.add(new TileWire(newTile, sourceWire));
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
	 * Creates the hierarchical port tile.
	 * 
	 * @param newDevice Partial device data structure
	 * @param toOutput Set of {@link TileWire} objects that need to be connected to an "OPORT"  
	 * @param fromInput Set of {@link TileWire} objects that need to be connected to an "IPORT"
	 */
	private void createPortTile(Device newDevice, List<TileWire> toOutputPorts, List<TileWire> toInputPorts) {
		Tile portTile = newDevice.getTile(0,  newDevice.getColumns() - 1);
		portTile.setName("HEIR_PORT_X0Y0");
		portTile.setType(TileType.valueOf(newDevice.getFamily(), "HIERARCHICAL_PORT"));
		
		int[] tileSources = new int[toInputPorts.size()];
		int[] tileSinks = new int[toOutputPorts.size()];
		
		// create 
		createPortWires(newDevice, toOutputPorts.size(), toInputPorts.size(), tileSources, tileSinks);
		portTile.setSources(tileSources);
		portTile.setSinks(tileSinks);
		
		createPortTileWireMaps(portTile, toOutputPorts, toInputPorts);
		createInputPortSiteTemplate(newDevice);
		createOutputPortSiteTemplate(newDevice);
		
		Site[] portSiteArray = new Site[toOutputPorts.size() + toInputPorts.size()];
		Map<Integer, Integer> wireSites = new HashMap<>();
		createPortSites("IPORT", portTile, portSiteArray, toInputPorts.size(), 0, wireSites);
		createPortSites("OPORT", portTile, portSiteArray, toOutputPorts.size(), toInputPorts.size(), wireSites);
		portTile.setSites(portSiteArray);
		portTile.setWireSites(wireSites);
	}
	
	/**
	 * Creates new wires that are needed for the partial device file and adds them 
	 * to the {@link WireEnumerator}. These wires include the intrasite wires for the 
	 * "IPORT" and "OPORT" sites, and all wires in the new "HEIR_PORT" tile.
	 *  
	 * @param device Partial device data structure
	 * @param outputPortCount Number of output ports to create
	 * @param inputPortCount Number of input ports to create
	 * @param tileSources Source wire array for the "HIER_PORT" tile. Populated in this method.
	 * @param tileSinks Sink wire array for the "HIER_PORT" tile. Populated in this method.
	 */
	private void createPortWires(Device device, int outputPortCount, int inputPortCount, int[] tileSources, int[] tileSinks) {
		WireEnumerator we = device.getWireEnumerator();

		// create the intrasite wires for the needed sites
		String iportPinWire = "intrasite:IPORT/O.O";
		String oportPinWire = "intrasite:OPORT/I.I";
		String iportBelWire = "intrasite:IPORT/IPAD.PAD";
		String oportBelWire = "intrasite:OPORT/OPAD.PAD";
		List<String> wireNames = new ArrayList<String> (Arrays.asList(we.getWires()));
		
		Map<String, Integer> enumMap = we.getWireMap();
		wireNames.add(iportPinWire);
		enumMap.put(iportPinWire, enumMap.size());
		wireNames.add(oportPinWire);
		enumMap.put(oportPinWire, enumMap.size());
		wireNames.add(iportBelWire);
		enumMap.put(iportBelWire, enumMap.size());
		wireNames.add(oportBelWire);
		enumMap.put(oportBelWire, enumMap.size());
		
		// create a new tile wire for each output port that needs to be created
		for (int i = 0; i < outputPortCount; i++) {
			String wireName = "oport_wire" + i;
			wireNames.add(wireName);
			tileSinks[i] = enumMap.size();
			enumMap.put(wireName, enumMap.size());
		}
		
		// create a new tile wire for each inpout port that needs to be created
		for (int i = 0; i < inputPortCount; i++) {
			String wireName = "iport_wire" + i;
			wireNames.add(wireName);
			tileSources[i] = enumMap.size();
			enumMap.put(wireName, enumMap.size());
		}

		// update the wirename array and wire map of the WireEnumerator accordingly
		we.setWires(wireNames.toArray(new String[wireNames.size()]));
		we.setWireMap(enumMap);
		
		device.setWireEnumerator(we);
	}
	
	/**
	 * Creates a {@link SiteTemplate} for an "IPORT" site and adds it to the partial device
	 * 
	 * @param device Partial Device data structure
	 */
	private void createInputPortSiteTemplate(Device device) {
		
		WireEnumerator we = device.getWireEnumerator();
		Integer sitePinWire = we.getWireEnum("intrasite:IPORT/O.O");
		Integer belPinWire = we.getWireEnum("intrasite:IPORT/IPAD.PAD");
		
		assert (sitePinWire != null && belPinWire != null) : "IPORT intrasite wires not found.";
		
		// create a new site template of type "IPORT"
		SiteTemplate siteTemplate = new SiteTemplate();
		SiteType siteType = SiteType.valueOf(device.getFamily(), "IPORT");
		siteTemplate.setType(siteType);
		
		// create the forward and reverse wire connections in the site
		WireHashMap wireMap = new WireHashMap();
		WireConnection[] wcs = {new WireConnection(sitePinWire, 0, 0, false)};
		wireMap.put(belPinWire, wcs);
		siteTemplate.setRouting(wireMap);
		
		WireHashMap reverseWireMap = new WireHashMap();
		WireConnection[] wcs2 = {new WireConnection(belPinWire, 0, 0, false)};
		reverseWireMap.put(sitePinWire, wcs2);
		siteTemplate.setReverseWireConnections(reverseWireMap);
		
		// create the site pins of the site
		SitePinTemplate sitePinTemplate = new SitePinTemplate("O", siteTemplate.getType());
		sitePinTemplate.setDirection(PinDirection.OUT);
		sitePinTemplate.setInternalWire(sitePinWire);
		Map<String, SitePinTemplate> sitePinMap = new HashMap<>();
		sitePinMap.put(sitePinTemplate.getName(), sitePinTemplate);
		siteTemplate.setSources(sitePinMap);
		siteTemplate.setSinks(Collections.emptyMap());
		
		Map<Integer, SitePinTemplate> sitePinWireMap = new HashMap<>();
		sitePinWireMap.put(sitePinTemplate.getInternalWire(), sitePinTemplate);
		siteTemplate.setInternalSiteWireMap(sitePinWireMap);
		
		// create the BELs and BEL pins of the site
		BelId ipadId = new BelId(siteTemplate.getType(), "IPAD");
		BelTemplate belTemplate = new BelTemplate(ipadId, "IPAD"); 
		BelPinTemplate belPinTemplate = new BelPinTemplate(ipadId, "PAD");
		belPinTemplate.setWire(belPinWire);
		belPinTemplate.setDirection(PinDirection.OUT);
		belPinTemplate.addSitePin(sitePinTemplate.getName());
		
		Map<String, BelPinTemplate> belPinMap = new HashMap<>();
		belPinMap.put(belPinTemplate.getName(), belPinTemplate);
		belTemplate.setSources(belPinMap);
		
		Map<String, BelTemplate> belTemplateMap = new HashMap<>();
		belTemplateMap.put(belTemplate.getId().getName(), belTemplate);
		siteTemplate.setBelTemplates(belTemplateMap);
		
		Map<Integer, BelPinTemplate> belPinWireMap = new HashMap<>();
		belPinWireMap.put(belPinTemplate.getWire(), belPinTemplate);
		siteTemplate.setBelPins(belPinWireMap);
	
		Map<SiteType, SiteTemplate> templateMap =  device.getSiteTemplates();
		templateMap.put(siteType, siteTemplate);
	}
	
	/**
	 * Creates a {@link SiteTemplate} for an "OPORT" site and adds it to the partial device
	 * 
	 * @param device Partial Device data structure
	 */
	private SiteTemplate createOutputPortSiteTemplate(Device device) {
		
		WireEnumerator we = device.getWireEnumerator();
		Integer sitePinWire = we.getWireEnum("intrasite:OPORT/I.I");
		Integer belPinWire = we.getWireEnum("intrasite:OPORT/OPAD.PAD");
		
		assert (sitePinWire != null && belPinWire != null) : "OPORT intrasite wires not found.";
		
		// create a new site template of type "OPORT"
		SiteTemplate siteTemplate = new SiteTemplate();
		SiteType siteType = SiteType.valueOf(device.getFamily(), "OPORT");
		siteTemplate.setType(siteType);
		
		// create the forward and reverse wire connections in the site
		WireHashMap wireMap = new WireHashMap();
		WireConnection[] wcs = {new WireConnection(belPinWire, 0, 0, false)};
		wireMap.put(sitePinWire, wcs);
		siteTemplate.setRouting(wireMap);
		
		WireHashMap reverseWireMap = new WireHashMap();
		WireConnection[] wcs2 = {new WireConnection(sitePinWire, 0, 0, false)};
		reverseWireMap.put(belPinWire, wcs2);
		siteTemplate.setReverseWireConnections(reverseWireMap);
		
		// create the site pins of the site
		SitePinTemplate sitePinTemplate = new SitePinTemplate("I", siteTemplate.getType());
		sitePinTemplate.setDirection(PinDirection.IN);
		sitePinTemplate.setInternalWire(sitePinWire);
		Map<String, SitePinTemplate> sitePinMap = new HashMap<>();
		sitePinMap.put(sitePinTemplate.getName(), sitePinTemplate);
		siteTemplate.setSinks(sitePinMap);
		siteTemplate.setSources(Collections.emptyMap());
		
		Map<Integer, SitePinTemplate> sitePinWireMap = new HashMap<>();
		sitePinWireMap.put(sitePinTemplate.getInternalWire(), sitePinTemplate);
		siteTemplate.setInternalSiteWireMap(sitePinWireMap);
		
		// create the BELs and BEL pins of the site
		BelId ipadId = new BelId(siteTemplate.getType(), "OPAD");
		BelTemplate belTemplate = new BelTemplate(ipadId, "OPAD"); 
		BelPinTemplate belPinTemplate = new BelPinTemplate(ipadId, "PAD");
		belPinTemplate.setWire(belPinWire);
		belPinTemplate.setDirection(PinDirection.IN);
		belPinTemplate.addSitePin(sitePinTemplate.getName());
		
		Map<String, BelPinTemplate> belPinMap = new HashMap<>();
		belPinMap.put(belPinTemplate.getName(), belPinTemplate);
		belTemplate.setSinks(belPinMap);
		
		Map<String, BelTemplate> belTemplateMap = new HashMap<>();
		belTemplateMap.put(belTemplate.getId().getName(), belTemplate);
		siteTemplate.setBelTemplates(belTemplateMap);
		
		Map<Integer, BelPinTemplate> belPinWireMap = new HashMap<>();
		belPinWireMap.put(belPinTemplate.getWire(), belPinTemplate);
		siteTemplate.setBelPins(belPinWireMap);
		
		Map<SiteType, SiteTemplate> templateMap =  device.getSiteTemplates();
		templateMap.put(siteType, siteTemplate);
	
		return siteTemplate;
	}
	
	/**
	 * Creates the forward and reverse wire connections for the "HIER_PORT" tile.
	 *  
	 * @param portTile Hierarchical port tile (top-right tile of the partial device)
	 * @param toOutput Set of {@link TileWire} objects that need to be connected to an "OPORT"  
	 * @param fromInput Set of {@link TileWire} objects that need to be connected to an "IPORT"
	 */
	private void createPortTileWireMaps(Tile portTile, List<TileWire> toOutput, List<TileWire> fromInput) {
		
		WireEnumerator we = portTile.getDevice().getWireEnumerator();
		
		// create the forward connections of the port tile (from the IPORTs to the small device tiles)
		WireHashMap forwardMap = new WireHashMap();
		int i = 0;
		for (TileWire tileWire : fromInput) {
			Tile sdTile = tileWire.getTile();
			int sdEnum = tileWire.getWireEnum();
					
			// create the forward connections from the IPORT -> small device tile
			int rowOffset = portTile.getRow() - sdTile.getRow();
			int colOffset = portTile.getColumn() - sdTile.getColumn();
			WireConnection[] portConn = {new WireConnection(sdEnum, rowOffset, colOffset, false)};
			forwardMap.put(we.getWireEnum("iport_wire" + i), portConn);
			
			//create the reverse wire connections from the small device tile -> IPORT tile			
			WireConnection sdTileConn = new WireConnection(we.getWireEnum("iport_wire" + i), -rowOffset, -colOffset, false);
			WireHashMap sdReverseMap = sdTile.getReverseWireHashMap();
			sdReverseMap.put(sdEnum, addConnection(sdReverseMap.get(sdEnum), sdTileConn) );
			i++;
		}
		
		// create the reverse connections of the port (from the OPORTs to the small device tiles)
		i = 0;
		WireHashMap reverseMap = new WireHashMap();
		for (TileWire tileWire : toOutput) {
			Tile sdTile = tileWire.getTile();
			int sdEnum = tileWire.getWireEnum();
			
			// create the reverse connection from the OPORT -> small device tile
			int rowOffset = portTile.getRow() - sdTile.getRow();
			int colOffset = portTile.getColumn() - sdTile.getColumn();
			WireConnection[] portConn = {new WireConnection(sdEnum, rowOffset, colOffset, false)};
			reverseMap.put(we.getWireEnum("oport_wire" + i), portConn);
			
			// create the forward connection from the small device tile -> OPORT tile
			WireConnection sdTileConn = new WireConnection(we.getWireEnum("oport_wire" + i), -rowOffset, -colOffset, false);
			WireHashMap sdWireMap = sdTile.getWireHashMap();
			sdWireMap.put(sdEnum, addConnection(sdWireMap.get(sdEnum), sdTileConn) );
			i++;
		}
		
		// set the forward and reverse connections in the new tile
		portTile.setWireHashMap(forwardMap);
		portTile.setReverseWireConnections(reverseMap);
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
		
		for(int i = 0; i < connArray.length; i++) {
			newArray[i] = connArray[i];
		}
		
		newArray[connArray.length] = newConn; 
		
		return newArray;
	}
	
	/**
	 * Creates the required IPORT and OPORT sites within the hierarchical port tile.
	 * 
	 * @param type Type of ports to create. Valid options are "IPORT" and "OPORT"
	 * @param portTile	Hierarchical port {@link Tile} in the partial device
	 * @param siteArray Site array for the port tile. Populated in this method
	 * @param numSites Number of port sites to create
	 * @param startIndex Starting number to index the ports
	 * @param wireSites Map of wire to site index for wires in the port tile
	 */
	private void createPortSites(String type, Tile portTile, Site[] siteArray, int numSites, int startIndex, Map<Integer, Integer> wireSites) {
		Device device = portTile.getDevice();
		WireEnumerator we = device.getWireEnumerator();
		String upperType = type.toUpperCase();
		String lowerType = type.toLowerCase();
		SiteType portType = SiteType.valueOf(device.getFamily(), upperType);
		String pinName = type.equals("IPORT") ? "O" : "I";
		int siteIndex = startIndex;
		
		for (int i = 0; i < numSites; i++, siteIndex++) {
			Site site = new Site();
			site.setTile(portTile);
			site.setType(portType);
			SiteType[] possibleTypes = {portType};
			site.setPossibleTypes( possibleTypes );
			site.setBondedType(BondedType.INTERNAL);
			site.setIndex(siteIndex);
			site.setName(upperType + "_" + i);
			
			Map<SiteType, Map<String, Integer>> externalWireMap = new HashMap<>();
			Map<String, Integer> pinWireMap = new HashMap<>();
			int wireEnum = we.getWireEnum(lowerType + "_wire" + i);
			pinWireMap.put(pinName, wireEnum);
			externalWireMap.put(portType, pinWireMap);
			site.setExternalWires(externalWireMap);
			
			Map<SiteType, Map<Integer, SitePinTemplate>> externalWireToPinNameMap = new HashMap<>();
			Map<Integer, SitePinTemplate> externalMap = new HashMap<>();
			externalMap.put(wireEnum, site.getSitePin(pinName).getTemplate());
			externalWireToPinNameMap.put(portType, externalMap);
			site.setExternalWireToPinNameMap(externalWireToPinNameMap);
			
			siteArray[siteIndex] = site;
			wireSites.put(wireEnum, siteIndex);
		}
	}
	
	/**
	 * Sets the tile types of all tiles in the right-most column 
	 * to NULL (besides the port tile)
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
