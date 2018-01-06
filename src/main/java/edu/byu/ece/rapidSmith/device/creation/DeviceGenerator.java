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

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.xdl.XdlAttribute;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.device.xdlrc.XDLRCParseProgressListener;
import edu.byu.ece.rapidSmith.device.xdlrc.XDLRCParser;
import edu.byu.ece.rapidSmith.device.xdlrc.XDLRCParserListener;
import edu.byu.ece.rapidSmith.device.xdlrc.XDLRCSource;
import edu.byu.ece.rapidSmith.primitiveDefs.*;
import edu.byu.ece.rapidSmith.util.Exceptions;
import edu.byu.ece.rapidSmith.util.HashPool;
import edu.byu.ece.rapidSmith.util.PartNameTools;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static edu.byu.ece.rapidSmith.util.Exceptions.EnvironmentException;
import static edu.byu.ece.rapidSmith.util.Exceptions.FileFormatException;

/**
 * Generates a new device through parsing the device's XDLRC representation.
 * <p>
 * Steps
 * 1) First parse
 * a) WireEnumeratorListener
 * i) Extract wire names, types, and directions from XDLRC
 * ii) Enumerate each wire name in device
 * b) TileAndPrimitiveSiteListener
 * i) Extract part name from XDLRC
 * ii) Extract number of rows and columns from XDLRC
 * iii) Construct device tile array
 * iv) Extract tile names and types
 * v) Create list of sites in each tile
 * vi) Extract name and type of each primitive site
 * vii) Define alternative types for each primitive site
 * c) PrimitiveDefsListener
 * i) Construct primitive defs structure
 * 2) Construct dependent resources
 * a) Build tile and sites name map
 * 2) Second parse
 * a) Build wire connection for each tile.  Preserve all connections that
 * are either sources or sinks of a site or a PIP
 */
public final class DeviceGenerator {
	private Device device;
	private WireEnumerator we;
	private Document familyInfo;

	private static final int PIP_CAPACITY = 40000;
	private final Set<String> pipSources = new HashSet<>(PIP_CAPACITY);
	private final Set<String> pipSinks = new HashSet<>(PIP_CAPACITY);

	/** Keeps track of each unique Wire object in the device */
	private HashPool<WireConnection> wirePool;
	/** Keeps track of each unique Wire[] object in the device */
	private HashPool<WireArray> wireArrayPool;
	/** Keeps track of all PIPRouteThrough objects */
	private HashPool<PIPRouteThrough> routeThroughPool;
	/** Keeps Track of all unique Wire Lists that exist in Tiles */
	private HashPool<WireHashMap> tileWiresPool;

	private HashPool<Map<String, Integer>> externalWiresPool;
	private HashPool<Map<SiteType, Map<String, Integer>>> externalWiresMapPool;
	private HashPool<AlternativeTypes> alternativeTypesPool;
	private Set<Integer> siteWireSourceSet;
	private Set<Integer> siteWireSinkSet;

	/**
	 * Generates and returns the Device created from the XDLRC at the specified
	 * source.
	 *
	 * @param xdlrcSource the XDLRC source containing the device description
	 * @return the generated Device representation
	 */
	public Device generate(XDLRCSource xdlrcSource) throws IOException {
		System.out.println("Generating device for file " + xdlrcSource.getFilePath());

		this.device = new Device();
		this.we = new WireEnumerator();
		this.device.setWireEnumerator(we);

		this.wirePool = new HashPool<>();
		this.wireArrayPool = new HashPool<>();
		this.routeThroughPool = new HashPool<>();
		this.tileWiresPool = new HashPool<>();
		this.externalWiresPool = new HashPool<>();
		this.externalWiresMapPool = new HashPool<>();
		this.alternativeTypesPool = new HashPool<>();

		// Requires a two part iteration, the first to obtain the tiles and sites,
		// and the second to gather the wires.  Two parses are required since the
		// wires need to know the source and sink tiles.
		System.out.println("Starting first pass");
		xdlrcSource.registerListener(new FamilyTypeListener());
		xdlrcSource.registerListener(new WireEnumeratorListener());
		xdlrcSource.registerListener(new TileAndSiteGeneratorListener());
		xdlrcSource.registerListener(new PrimitiveDefsListener());
		xdlrcSource.registerListener(new XDLRCParseProgressListener());
		try {
			xdlrcSource.parse();
		} catch (IOException e) {
			throw new IOException("Error handling file " + xdlrcSource.getFilePath(), e);
		}
		xdlrcSource.clearListeners();

		device.constructTileMap();
		PrimitiveDefsCorrector.makeCorrections(device.getPrimitiveDefs(), familyInfo);
		device.setSiteTemplates(createSiteTemplates());

		System.out.println("Starting second pass");
		xdlrcSource.registerListener(new WireConnectionGeneratorListener());
		xdlrcSource.registerListener(new SourceAndSinkListener());
		xdlrcSource.registerListener(new XDLRCParseProgressListener());
		try {
			xdlrcSource.parse();
		} catch (IOException e) {
			throw new IOException("Error handling file " + xdlrcSource.getFilePath(), e);
		}

		Map<Tile, Map<Integer, Set<WireConnection>>> wcsToAdd = getWCsToAdd();
		Map<Tile, Map<Integer, Set<WireConnection>>> wcsToRemove = getWCsToRemove();

		// These take up a lot of memory and we're going to regenerate each of these in the
		// next step.  Clearing these will allow for better garbage collection
		wirePool = new HashPool<>();
		wireArrayPool = new HashPool<>();
		tileWiresPool = new HashPool<>();

		System.out.println("Parsing Device Info file");
		if (parseDeviceInfo(device) == false) {
			System.err.println("[Warning]: The device info file for the part " + device.getPartName() + " cannot be found.");
		}
				
		makeWireCorrections(wcsToAdd, wcsToRemove);
	
		device.constructDependentResources();
		
		// free unneeded pools for garbage collection when done with
		routeThroughPool = null;

		System.out.println("Finishing device creation process");

		return device;
	}

	/**
	 * Creates the templates for the primitive sites with information from the
	 * primitive defs and device information file.
	 */
	private Map<SiteType, SiteTemplate> createSiteTemplates() {
		Map<SiteType, SiteTemplate> siteTemplates = new HashMap<>();
		FamilyType family = device.getFamily();

		// Create a template for each primitive type
		for (PrimitiveDef def : device.getPrimitiveDefs()) {
			Element ptEl = getSiteTypeEl(def.getType());
			
			SiteTemplate template = new SiteTemplate();
			template.setType(def.getType());
			template.setBelTemplates(createBelTemplates(def, ptEl));
			createAndSetIntrasiteRouting(def, template, ptEl);
			createAndSetSitePins(def, template);

			Element compatTypesEl = ptEl.getChild("compatible_types");
			if (compatTypesEl != null) {
				List<SiteType> compatibleTypes = compatTypesEl.getChildren("compatible_type").stream()
						.map(compatTypeEl -> SiteType.valueOf(family, compatTypeEl.getText()))
						.collect(Collectors.toList());
				template.setCompatibleTypes(compatibleTypes.toArray(
						new SiteType[compatibleTypes.size()]));
			}

			siteTemplates.put(def.getType(), template);
		}

		return siteTemplates;
	}

	/**
	 * Creates the templates for each BEL in the primitive site
	 *
	 * @param def       The primitive def to process
	 * @param ptElement XML element detailing the primitive type
	 * @return The templates for each BEL in the primitive type
	 */
	private Map<String, BelTemplate> createBelTemplates(PrimitiveDef def, Element ptElement) {
		Map<String, BelTemplate> templates = new HashMap<>();

		// for each BEL element
		for (PrimitiveElement el : def.getElements()) {
			if (!el.isBel())
				continue;
			
			BelId id = new BelId(def.getType(), el.getName());
			// Set the BEL type as defined in the deviceinfo file
			String belType = getTypeOfBel(el.getName(), ptElement);

			BelTemplate template = new BelTemplate(id, belType);

			// Create the BEL pin templates
			Map<String, BelPinTemplate> sinks = new HashMap<>();
			Map<String, BelPinTemplate> sources = new HashMap<>();
			for (PrimitiveDefPin pin : el.getPins()) {
				BelPinTemplate belPin = new BelPinTemplate(id, pin.getInternalName());
				belPin.setDirection(pin.getDirection());
				String wireName = getIntrasiteWireName(def.getType(), el.getName(), belPin.getName());
				belPin.setWire(we.getWireEnum(wireName));
				if (pin.getDirection() == PinDirection.IN || pin.getDirection() == PinDirection.INOUT)
					sinks.put(belPin.getName(), belPin);
				if (pin.getDirection() == PinDirection.OUT || pin.getDirection() == PinDirection.INOUT)
					sources.put(belPin.getName(), belPin);
			}
			template.setSources(sources);
			template.setSinks(sinks);
			templates.put(el.getName(), template);
		}

		// Find the site pins that connect to each BEL pin by traversing the routing.
		// This info is useful for directing which site pin should be targeted while
		// routing to reach the correct BEL pin.
		for (PrimitiveDefPin pin : def.getPins()) {
			PrimitiveElement el = def.getElement(pin.getInternalName());
			boolean forward = !pin.isOutput(); // traverse forward or backward?
			findAndSetSitePins(templates, def, forward, pin.getExternalName(), el);
		}

		return templates;
	}

	/**
	 * Recursively traverses through the elements to find all BEL pins reachable from the site pin.
	 *
	 * @param templates the BEL templates in the primitive type
	 * @param def       the primitive def for the current type
	 * @param forward   traverse forward or backward (forward for site sinks and
	 *                  backward for site sources)
	 * @param sitePin   Site pin we're searching from
	 * @param element   The current element we're looking at
	 */
	private void findAndSetSitePins(Map<String, BelTemplate> templates, PrimitiveDef def,
	                                boolean forward, String sitePin, PrimitiveElement element) {

		// follow each connection from the element
		for (PrimitiveConnection c : element.getConnections()) {
			PrimitiveElement destElement;

			// This connection goes the opposite of the way we want to search
			if (forward != c.isForwardConnection())
				continue;

			destElement = def.getElement(c.getElement1());

			if (destElement.isMux()) {
				// This is a routing mux.  Follow it.
				findAndSetSitePins(templates, def, forward, sitePin, destElement);
			}
		}
	}

	/**
	 * Find the XML element specifying the type for the desired BEL
	 *
	 * @param belName   name of the BEL to find the type for
	 * @param ptElement XML element detailing the primitive type
	 * @return the BEL type
	 */
	private String getTypeOfBel(String belName, Element ptElement) {
		for (Element belEl : ptElement.getChild("bels").getChildren("bel")) {
			if (belEl.getChildText("name").equals(belName))
				return belEl.getChildText("type");
		}
		assert false : "No type found for the specified BEL " + belName + " " + ptElement.getChildText("name");
		return null;
	}

	/**
	 * Creates the wire connections connecting the BELs and muxes in the primitive type.
	 *
	 * @param def      the primitive def for the current type
	 * @param template the template for the current type
	 */
	private void createAndSetIntrasiteRouting(PrimitiveDef def, SiteTemplate template, Element siteElement) {
		WireHashMap wireMap = new WireHashMap();
		// Stores the attributes associated with the subsite PIPs for converting
		// back to XDL
		Map<Integer, Map<Integer, XdlAttribute>> muxes = new HashMap<>();

		/*
		    We build the routing structure by find all of the wire sources and
		    creating a wire connection between it and its sinks.  For muxes, we
		    additionally create a wire connection from each input of the mux to
		    the output.
		 */
		for (PrimitiveElement el : def.getElements()) {
			String elName = el.getName();
			if (el.isPin() && !def.getPin(elName).isOutput()) { // input site pin
				addWireConnectionsForElement(def, el, wireMap);
			} else if (el.isBel()) {
				addWireConnectionsForElement(def, el, wireMap);
			} else if (el.isMux()) {
				addWireConnectionsForElement(def, el, wireMap);
				createAndAddMuxPips(def, el, wireMap, muxes);
			}
		}
		
		Map<Integer, Set<Integer>> belRoutethroughMap = createBelRoutethroughs(template, siteElement, wireMap); 
		
		template.setBelRoutethroughs(belRoutethroughMap); 
		template.setRouting(wireMap);
		template.setPipAttributes(muxes);
	}

	/**
	 * Creates a BEL routethrough map for the site template.  
	 * @param template Site Template to generate routethroughs for
	 * @param siteElement XML document element of the site in the familyinfo.xml file
	 * @param wireMap WireHashMap of the site template
	 * @return A Map of BEL routethroughs
	 */
	private Map <Integer, Set<Integer>> createBelRoutethroughs(SiteTemplate template, Element siteElement, WireHashMap wireMap) {
		
		Map <Integer, Set<Integer>> belRoutethroughMap = new HashMap<>();
		
		for (Element belEl : siteElement.getChild("bels").getChildren("bel")) {
			String belName = belEl.getChildText("name");
			
			Element routethroughs = belEl.getChild("routethroughs");
			
			// bel has routethroughs
			if (routethroughs != null) {
				
				for(Element routethrough : routethroughs.getChildren("routethrough")) {
				
					String inputPin = routethrough.getChildText("input");
					String outputPin = routethrough.getChildText("output");
					
					String inputWireName = getIntrasiteWireName(template.getType(), belName, inputPin);
					String outputWireName = getIntrasiteWireName(template.getType(), belName, outputPin);
							
					Integer startEnum = we.getWireEnum(inputWireName); 
					Integer endEnum = we.getWireEnum(outputWireName);
										
					// If the wire names for the routethrough do not exist, throw a parse exception telling the user 
					if (startEnum == null) {
						throw new Exceptions.ParseException(String.format("Cannot find intrasite wire \"%s\" for bel routethrough \"%s:%s:%s\". "
								+ "Check the familyInfo.xml file for this routethrough and make sure the connections are correct.", 
								inputWireName, template.getType(), inputPin, outputPin));
					} else if (endEnum == null) {
						throw new Exceptions.ParseException(String.format("Cannot find intrasite wire \"%s\" for bel routethrough \"%s:%s:%s\". "
								+ "Check the familyInfo.xml file for this routethrough and make sure the connections are correct.", 
								outputWireName, template.getType(), inputPin, outputPin));
					}
					
					// add the routethrough to the routethrough map; 
					Set<Integer> sinkWires = belRoutethroughMap.computeIfAbsent(startEnum, k -> new HashSet<>());
					sinkWires.add(endEnum);
				}
			}
		}
		
		// create a new wire connection for each routethrough and adds them to the wire map
		for (Integer startWire : belRoutethroughMap.keySet()) {
			
			Set<Integer> sinkWires = belRoutethroughMap.get(startWire);				
			WireConnection[] wireConnections = new WireConnection[sinkWires.size()];
			
			int index = 0; 
			for (Integer sink : sinkWires) {
				// routethroughs will be considered as pips in rapidSmith
				wireConnections[index] =  new WireConnection(sink, 0, 0, true);
				index++;
			}
			
			wireMap.put(startWire, wireConnections);
		}
		
		// return null if the belRoutethroughMap is empty
		return belRoutethroughMap.isEmpty() ? null : belRoutethroughMap;
	}
	
	/**
	 * Creates a PIP wire connection from each input of the mux to the output.
	 * Additionally creates the attribute that would represent this connection
	 * in XDL and adds it to the muxes structure.
	 *
	 * @param def     the primitive def for the current type
	 * @param el      the mux element from the primitive def
	 * @param wireMap the map of wire connections for the site template
	 * @param muxes   the connection to attribute map for the site template
	 */
	private void createAndAddMuxPips(PrimitiveDef def, PrimitiveElement el,
	                                 WireHashMap wireMap, Map<Integer, Map<Integer, XdlAttribute>> muxes) {
		String elName = el.getName();
		String sinkName = getIntrasiteWireName(def.getType(), elName, getOutputPin(el));
		Integer sinkWire = we.getWireEnum(sinkName);
		WireConnection[] wcs = {new WireConnection(sinkWire, 0, 0, true)};
		for (PrimitiveDefPin pin : el.getPins()) {
			if (pin.isOutput())
				continue;
			String srcName = getIntrasiteWireName(def.getType(), elName, pin.getInternalName());
			int srcWire = we.getWireEnum(srcName);
			wireMap.put(srcWire, wcs);

			XdlAttribute attr = new XdlAttribute(elName, "", pin.getInternalName());
			Map<Integer, XdlAttribute> sinkMap = muxes.computeIfAbsent(srcWire, k -> new HashMap<>());
			sinkMap.put(sinkWire, attr);
		}
	}

	/**
	 * Gets the wire connections for this element and adds them to the wire map
	 *
	 * @param def     the primitive def for the current type
	 * @param el      the current element from the primitive def
	 * @param wireMap the map of wire connections for the site template
	 */
	private void addWireConnectionsForElement(
			PrimitiveDef def, PrimitiveElement el, WireHashMap wireMap) {
		Map<Integer, List<WireConnection>> wcsMap;
		wcsMap = getWireConnectionsForElement(def, el);
		for (Map.Entry<Integer, List<WireConnection>> entry : wcsMap.entrySet()) {
			List<WireConnection> wcsList = entry.getValue();
			WireConnection[] wcs = wcsList.toArray(new WireConnection[wcsList.size()]);
			wireMap.put(entry.getKey(), wcs);
		}
	}

	/**
	 * Returns all of the wire connections coming from the element
	 *
	 * @param def the primitive def for the current type
	 * @param el  the current element from the primitive def
	 * @return all of the wire connection coming from the element
	 */
	private Map<Integer, List<WireConnection>> getWireConnectionsForElement(
			PrimitiveDef def, PrimitiveElement el) {
		Map<Integer, List<WireConnection>> wcsMap = new HashMap<>();
		for (PrimitiveConnection conn : el.getConnections()) {
			// Only handle connections this element sources
			if (!conn.isForwardConnection())
				continue;

			Integer source = getPinSource(def, conn);
			Integer sink = getPinSink(def, conn);
			List<WireConnection> wcs = wcsMap.computeIfAbsent(source, k -> new ArrayList<>());
			wcs.add(new WireConnection(sink, 0, 0, false));
		}
		return wcsMap;
	}

	private static String getOutputPin(PrimitiveElement el) {
		for (PrimitiveDefPin pin : el.getPins()) {
			if (pin.isOutput())
				return pin.getInternalName();
		}
		return null;
	}

	private Integer getPinSource(PrimitiveDef def, PrimitiveConnection conn) {
		String element = conn.getElement0();
		String pin = conn.getPin0();
		String wireName = getIntrasiteWireName(def.getType(), element, pin);
		return we.getWireEnum(wireName);
	}

	private Integer getPinSink(PrimitiveDef def, PrimitiveConnection conn) {
		String element = conn.getElement1();
		String pin = conn.getPin1();
		String wireName = getIntrasiteWireName(def.getType(), element, pin);
		return we.getWireEnum(wireName);
	}

	/**
	 * Creates the site pin templates and adds them to the site template.
	 */
	private void createAndSetSitePins(PrimitiveDef def, SiteTemplate siteTemplate) {
		Map<String, SitePinTemplate> sources = new HashMap<>();
		Map<String, SitePinTemplate> sinks = new HashMap<>();

		for (PrimitiveDefPin pin : def.getPins()) {
			String name = pin.getInternalName();
			SitePinTemplate template = new SitePinTemplate(name, def.getType());
			template.setDirection(pin.getDirection());
			String wireName = getIntrasiteWireName(def.getType(), name, name);
			template.setInternalWire(we.getWireEnum(wireName));
			if (pin.getDirection() == PinDirection.IN)
				sinks.put(name, template);
			else
				sources.put(name, template);
		}

		siteTemplate.setSources(sources);
		siteTemplate.setSinks(sinks);
	}

	/**
	 * Searches the device info file for the primitive type element of the
	 * specified type.
	 *
	 * @param type the type of the element to retrieve
	 * @return the JDOM element for the requested primitive type
	 */
	private Element getSiteTypeEl(SiteType type) {
		Element siteTypesEl = familyInfo.getRootElement().getChild("site_types");
		for (Element siteTypeEl : siteTypesEl.getChildren("site_type")) {
			if (siteTypeEl.getChild("name").getText().equals(type.name()))
				return siteTypeEl;
		}
		throw new FileFormatException("no site type " + type.name() + " in familyInfo.xml");
	}

	private Map<Tile, Map<Integer, Set<WireConnection>>> getWCsToAdd() {
		Map<Tile, Map<Integer, Set<WireConnection>>> wcsToAdd = new HashMap<>();

		for (Tile tile : device.getTileMap().values()) {
			if (tile.getWireHashMap() == null)
				continue;

			Map<Integer, Set<WireConnection>> tileWCsToAdd = new HashMap<>();
			// Traverse all non-PIP wire connections starting at this source wire.  If any
			// such wire connections lead to a sink wire that is not already a connection of
			// the source wire, mark it to be added as a connection
			for (Wire wire : tile.getWires()) {
				int wireEnum = wire.getWireEnum();
				Set<WireConnection> wcToAdd = new HashSet<>();
				Set<WireConnection> checkedConnections = new HashSet<>();
				Queue<WireConnection> connectionsToFollow = new LinkedList<>();

				// Add the wire to prevent building a connection back to itself
				checkedConnections.add(new WireConnection(wireEnum, 0, 0, false));
				for (WireConnection wc : tile.getWireConnections(wireEnum)) {
					if (!wc.isPIP()) {
						checkedConnections.add(wc);
						connectionsToFollow.add(wc);
					}
				}

				while (!connectionsToFollow.isEmpty()) {
					WireConnection midwc = connectionsToFollow.remove();
					Tile midTile = midwc.getTile(tile);
					Integer midWire = midwc.getWire();

					// Dead end checks
					if (midTile.getWireHashMap() == null || midTile.getWireConnections(midWire) == null)
						continue;

					for (WireConnection sinkwc : midTile.getWireConnections(midWire)) {
						if (sinkwc.isPIP()) continue;

						Integer sinkWire = sinkwc.getWire();
						Tile sinkTile = sinkwc.getTile(midTile);
						int colOffset = midwc.getColumnOffset() + sinkwc.getColumnOffset();
						int rowOffset = midwc.getRowOffset() + sinkwc.getRowOffset();

						// This represents the wire connection from the original source to the sink wire
						WireConnection source2sink = new WireConnection(sinkWire, rowOffset, colOffset, false);
						boolean wirePreviouslyChecked = !checkedConnections.add(source2sink);

						// Check if we've already processed this guy and process him if we haven't
						if (wirePreviouslyChecked)
							continue;
						connectionsToFollow.add(source2sink);

						// Only add the connection if the wire is a sink.  Other connections are
						// useless for wire traversing.
						if (wireIsSink(sinkTile, sinkWire))
							wcToAdd.add(wirePool.add(source2sink));
					}
				}

				// If there are wires to add, add them here by creating a new WireConnection array
				// combining the old and new wires.
				if (!wcToAdd.isEmpty()) {
					tileWCsToAdd.put(wireEnum, wcToAdd);
				}
			}
			if (!tileWCsToAdd.isEmpty())
				wcsToAdd.put(tile, tileWCsToAdd);
		}
		return wcsToAdd;
	}

	private Map<Tile, Map<Integer, Set<WireConnection>>> getWCsToRemove() {
		Map<Tile, Map<Integer, Set<WireConnection>>> wcsToRemove = new HashMap<>();

		// Traverse the entire device and find which wires to remove first
		for (Tile tile : device.getTileMap().values()) {
			if (tile.getWireHashMap() == null)
				continue;

			Map<Integer, Set<WireConnection>> tileWCsToRemove = new HashMap<>();

			// Create a set of wires that can be driven by other wires within the tile
			// We need this to do a fast look up later on
			Set<Integer> sourceWires = getSourceWiresOfTile(tile);

			// Identify any wire connections that are not a "source" wire to "sink" wire
			// connection.
			Set<Wire> wires = new HashSet<>(tile.getWires());

			for (Wire wire : wires) {
				int wireEnum = wire.getWireEnum();
				Set<WireConnection> wcToRemove = new HashSet<>();
				for (WireConnection wc : tile.getWireConnections(wireEnum)) {
					// never remove PIPs.  We only are searching for different names
					// of the same wire.  A PIP connect unique wires.
					if (wc.isPIP())
						continue;
					if (!sourceWires.contains(wireEnum) ||
							!wireIsSink(wc.getTile(tile), wc.getWire())) {
						wcToRemove.add(wc);
					}
				}
				tileWCsToRemove.put(wireEnum, wcToRemove);
			}
			wcsToRemove.put(tile, tileWCsToRemove);
		}
		return wcsToRemove;
	}

	private Set<Integer> getSourceWiresOfTile(Tile tile) {
		Set<Integer> sourceWires = new HashSet<>();
		for (Wire wire : tile.getWires()) {
			int wireEnum = wire.getWireEnum();
			if (siteWireSourceSet.contains(wireEnum)) {
				sourceWires.add(wireEnum);
			}
			for (WireConnection wc : tile.getWireConnections(wireEnum)) {
				if (wc.isPIP()) {
					sourceWires.add(wc.getWire());
				}
			}
		}
		return sourceWires;
	}

	// A wire is a sink if it is a site source (really should check in the tile sinks but
	// the wire type check is easier and should be sufficient or the wire is the source of
	// a PIP.
	private boolean wireIsSink(Tile tile, Integer wire) {
		if (siteWireSinkSet.contains(wire)) {
			return true;
		}
		if (tile.getWireHashMap() == null || tile.getWireConnections(wire) == null)
			return false;
		for (WireConnection wc : tile.getWireConnections(wire)) {
			if (wc.isPIP())
				return true;
		}
		return false;
	}

	/**
	 * Add the missing wire connection and remove the unnecssary wires in a single
	 * pass.  It's easier to just recreate the wire hash maps with the corrections.
	 */
	private void makeWireCorrections(
			Map<Tile, Map<Integer, Set<WireConnection>>> wcsToAdd,
			Map<Tile, Map<Integer, Set<WireConnection>>> wcsToRemove) {

		HashPool<WireHashMap> tileWiresPool = new HashPool<>();
		HashPool<WireArray> wireArrayPool = new HashPool<>();

		for (Tile tile : device.getTileMap().values()) {
			if (tile.getWireHashMap() == null)
				continue;

			// create a safe wire map to modify
			WireHashMap wireHashMap = new WireHashMap();

			for (Wire wire : tile.getWires()) {
				int wireEnum = wire.getWireEnum();
				Set<WireConnection> wcs =
						new HashSet<>(Arrays.asList(tile.getWireConnections(wireEnum)));
				if (wcsToRemove.containsKey(tile) && wcsToRemove.get(tile).containsKey(wireEnum))
					wcs.removeAll(wcsToRemove.get(tile).get(wireEnum));
				if (wcsToAdd.containsKey(tile) && wcsToAdd.get(tile).containsKey(wireEnum))
					wcs.addAll(wcsToAdd.get(tile).get(wireEnum));

				if (wcs.size() > 0) {
					WireConnection[] arrView = wcs.toArray(new WireConnection[wcs.size()]);
					wireHashMap.put(wireEnum, wireArrayPool.add(new WireArray(arrView)).array);
				}
			}

			// Update the tile with the new wire map.
			tile.setWireHashMap(tileWiresPool.add(wireHashMap));
		}
	}

	/**
	 * Remove duplicate wire resources in the tile.
	 */
	private void removeDuplicateTileResources(Tile tile) {
		WireHashMap origTileWires = tile.getWireHashMap();
		for (Wire wire : tile.getWires()) {
			int wireEnum = wire.getWireEnum();
			WireArray unique = wireArrayPool.add(new WireArray(origTileWires.get(wireEnum)));
			tile.getWireHashMap().put(wireEnum, unique.array);
		}

		WireHashMap retrievedTileWires = tileWiresPool.add(origTileWires);
		tile.setWireHashMap(retrievedTileWires);
	}

	private static String getIntrasiteWireName(
			SiteType type, String element, String pinName) {
		return "intrasite:" + type.name() + "/" + element + "." + pinName;
	}

	/**
	 * Parses the device info XML file for the specified device, and adds the information
	 * to the {@link Device} object that is being created. If no device info file can be found
	 * for the part, then a warning is printed to the console.
	 * 
	 * TODO: parse the clock pads and add them to the device file
	 * 
	 * @param device Device object created from the XDLRC parser
	 */
	public static boolean parseDeviceInfo(Device device) {
		Document deviceInfo = RSEnvironment.defaultEnv().loadDeviceInfo(device.getFamily(), device.getPartName());
		
		if (deviceInfo != null) {
			createPackagePins(device, deviceInfo);
			return true;
		}
		return false;
	}
	
	/**
	 * Creates a map from pad bel name -> corresponding package pin. This
	 * information is needed when generating Tincr Checkpoints from
	 * RS to be loaded into Vivado.
	 */
	private static void createPackagePins(Device device, Document deviceInfo) {
		Element pinMapRootEl = deviceInfo.getRootElement().getChild("package_pins");
		
		if (pinMapRootEl == null) {
			throw new Exceptions.ParseException("No package pin information found in device info file: " + deviceInfo.getBaseURI() + ".\n"
				+ "Either add the package pin mappings, or remove the device info file and regenerate.");
		}
		
		// Add the package pins to the device
		pinMapRootEl.getChildren("package_pin")
			.stream()
			.map(ppEl -> new PackagePin(ppEl.getChildText("name"), ppEl.getChildText("bel"), ppEl.getChild("is_clock") != null))
			.forEach(packagePin -> device.addPackagePin(packagePin));
			
		if (device.getPackagePins().isEmpty()) {
			throw new Exceptions.ParseException("No package pin information found in device info file: " + deviceInfo.getBaseURI() + ".\n"
					+ "Either add the package pin mappings, or remove the device info file and regenerate.");
		}
	}
	
	private final class FamilyTypeListener extends XDLRCParserListener {
		@Override
		protected void enterXdlResourceReport(pl_XdlResourceReport tokens) {
			FamilyType family = FamilyType.valueOf(tokens.family.toUpperCase());
			try {
				familyInfo = RSEnvironment.defaultEnv().loadFamilyInfo(family);
			} catch (IOException|JDOMException e) {
				throw new EnvironmentException("Failed to load family information file", e);
			}
			device.setFamily(family);
		}
	}

	private final class WireEnumeratorListener extends XDLRCParserListener {
		private static final int PIN_SET_CAPACITY = 10000;

		private final Set<String> wireSet = new TreeSet<>();
		private final Set<String> inpinSet = new HashSet<>(PIN_SET_CAPACITY);
		private final Set<String> outpinSet = new HashSet<>(PIN_SET_CAPACITY);

		private SiteType currType;
		private String currElement;

		/**
		 * Tracks special site pin wires.
		 */
		@Override
		protected void enterPinWire(pl_PinWire tokens) {
			String externalName = tokens.external_wire;
			
			if (tokens.direction.equals("input")) {
				inpinSet.add(externalName);
			} else {
				outpinSet.add(externalName);
			}
		}

		@Override
		protected void enterWire(pl_Wire tokens) {
			String wireName = tokens.name;
			wireSet.add(wireName);
		}

		@Override
		protected void enterPip(pl_Pip tokens) {
			pipSources.add(tokens.start_wire);

			String wireName = tokens.end_wire;
			pipSinks.add(wireName);
		}

		@Override
		protected void enterPrimitiveDef(pl_PrimitiveDef tokens) {
			currType = SiteType.valueOf(device.getFamily(), tokens.name);
		}

		@Override
		protected void enterElement(pl_Element tokens) {
			currElement = tokens.name;
		}

		@Override
		protected void enterElementPin(pl_ElementPin tokens) {
			String wireName = getIntrasiteWireName(currType, currElement, tokens.name);
			wireSet.add(wireName);
		}

		@Override
		protected void exitXdlResourceReport(pl_XdlResourceReport tokens) {
			Map<String, Integer> wireMap = new HashMap<>((int) (wireSet.size() / 0.75 + 1));
			String[] wires = new String[wireSet.size()];
			
			Set<Integer> sourceWireSetLocal = new HashSet<> (outpinSet.size());
			Set<Integer> sinkWireSetLocal = new HashSet<> (inpinSet.size());
						
			int i = 0;
			for (String wire : wireSet) {
				wires[i] = wire;
				wireMap.put(wire, i);

				if (inpinSet.contains(wire)) {
					sinkWireSetLocal.add(i);
				} 
				if (outpinSet.contains(wire)) {
					sourceWireSetLocal.add(i);
				} 

				i++;
			}

			we.setWireMap(wireMap);
			we.setWires(wires);
			
			// create the global source and sinks wire set
			siteWireSourceSet = sourceWireSetLocal;
			siteWireSinkSet = sinkWireSetLocal;
		}
	}

	private final class TileAndSiteGeneratorListener extends XDLRCParserListener {
		private ArrayList<Site> tileSites;

		private Tile currTile;

		@Override
		protected void enterXdlResourceReport(pl_XdlResourceReport tokens) {
			device.setPartName(PartNameTools.removeSpeedGrade(tokens.part));
		}

		@Override
		protected void enterTiles(pl_Tiles tokens) {
			int rows = tokens.rows;
			int columns = tokens.columns;

			device.createTileArray(rows, columns);
		}

		@Override
		protected void enterTile(pl_Tile tokens) {
			int row = tokens.row;
			int col = tokens.column;
			currTile = device.getTile(row, col);
			currTile.setName(tokens.name);
			currTile.setType(TileType.valueOf(device.getFamily(), tokens.type));

			tileSites = new ArrayList<>();
		}

		@Override
		protected void enterPrimitiveSite(pl_PrimitiveSite tokens) {
			Site site = new Site();
			site.setTile(currTile);
			site.setName(tokens.name);
			site.parseCoordinatesFromName(tokens.name);
			site.setIndex(tileSites.size());
			site.setBondedType(BondedType.valueOf(tokens.bonded.toUpperCase()));

			List<SiteType> alternatives = new ArrayList<>();
			SiteType type = SiteType.valueOf(device.getFamily(), tokens.type);
			alternatives.add(type);

			Element ptEl = getSiteTypeEl(type);
			Element alternativesEl = ptEl.getChild("alternatives");
			if (alternativesEl != null) {
				FamilyType family = device.getFamily();
				alternatives.addAll(alternativesEl.getChildren("alternative").stream()
						.map(alternativeEl -> SiteType.valueOf(family, alternativeEl.getChildText("name")))
						.collect(Collectors.toList()));
			}

			SiteType[] arr = alternatives.toArray(new SiteType[alternatives.size()]);
			arr = alternativeTypesPool.add(new AlternativeTypes(arr)).types;
			site.setPossibleTypes(arr);

			tileSites.add(site);
		}

		@Override
		protected void exitTile(pl_Tile tokens) {
			// Create an array of sites (more compact than ArrayList)
			if (tileSites.size() > 0) {
				currTile.setSites(tileSites.toArray(
						new Site[tileSites.size()]));
			} else {
				currTile.setSites(null);
			}

			currTile = null;
			tileSites = null;
		}
	}

	private final class WireConnectionGeneratorListener extends XDLRCParserListener {
		private Tile currTile;
		private Integer currTileWire;
		private boolean currTileWireIsSource;
		private Integer pipStartWire;
		private Integer pipEndWire;

		@Override
		protected void enterTile(pl_Tile tokens) {
			int row = tokens.row;
			int col = tokens.column;
			currTile = device.getTile(row, col);
			currTile.setWireHashMap(new WireHashMap());
		}

		@Override
		protected void exitTile(pl_Tile tokens) {
			removeDuplicateTileResources(currTile);
			currTile = null;
		}

		@Override
		protected void enterWire(pl_Wire tokens) {
			String wireName = tokens.name;
			currTileWire = we.getWireEnum(wireName);
			currTileWireIsSource = siteWireSourceSet.contains(currTileWire) || pipSinks.contains(wireName);
		}

		@Override
		protected void exitWire(pl_Wire tokens) {
			currTileWire = null;
		}

		@Override
		protected void enterConn(pl_Conn tokens) {
			String currWireName = tokens.wire;
			int currWire = we.getWireEnum(currWireName);
			boolean currWireIsSiteSink = siteWireSinkSet.contains(currWire);
			boolean currWireIsPIPSource = pipSources.contains(currWireName);
			boolean currWireIsSink = currWireIsSiteSink || currWireIsPIPSource;
			if (currTileWireIsSource || currWireIsSink) {
				Tile t = device.getTile(tokens.tile);
				WireConnection wc = new WireConnection(currWire,
						currTile.getRow() - t.getRow(),
						currTile.getColumn() - t.getColumn(),
						false);
				currTile.addConnection(currTileWire, wirePool.add(wc));
			}
		}

		@Override
		protected void enterPip(pl_Pip tokens) {
			Integer startWire = we.getWireEnum(tokens.start_wire);
			Integer endWire = we.getWireEnum(tokens.end_wire);
			WireConnection wc = wirePool.add(new WireConnection(endWire, 0, 0, true));
			currTile.addConnection(startWire, wc);

			pipStartWire = startWire;
			pipEndWire = endWire;
		}

		@Override
		protected void exitPip(pl_Pip tokens) {
			pipStartWire = null;
			pipEndWire = null;
		}

		@Override
		protected void enterRoutethrough(pl_Routethrough tokens) {
			SiteType type = SiteType.valueOf(device.getFamily(), tokens.site_type);

			String[] parts = tokens.pins.split("-");
			String inPin = parts[1];
			String outPin = parts[2];

			PIPRouteThrough currRouteThrough = new PIPRouteThrough(type, inPin, outPin);
			currRouteThrough = routeThroughPool.add(currRouteThrough);
			device.addRouteThrough(pipStartWire, pipEndWire, currRouteThrough);
		}
	}

	private final class SourceAndSinkListener extends XDLRCParserListener {
		private Site currSite;
		private Set<Integer> tileSources;
		private Set<Integer> tileSinks;
		private Map<String, Integer> externalPinWires;

		@Override
		protected void enterTile(pl_Tile tokens) {
			tileSources = new TreeSet<>();
			tileSinks = new TreeSet<>();
		}

		@Override
		protected void exitTile(pl_Tile tokens) {
			tileSources = null;
			tileSinks = null;
		}

		@Override
		protected void enterPrimitiveSite(pl_PrimitiveSite tokens) {
			currSite = device.getSite(tokens.name);
			externalPinWires = new HashMap<>();
		}

		@Override
		protected void enterPinWire(pl_PinWire tokens) {
			String name = tokens.name;
			PinDirection direction =
				tokens.direction.equals("input") ? PinDirection.IN : PinDirection.OUT;
			Integer externalWire = we.getWireEnum(tokens.external_wire);
			externalPinWires.put(name, externalWire);

			if (direction == PinDirection.IN) {
				tileSinks.add(externalWire);
			} else {
				tileSources.add(externalWire);
			}
		}

		@Override
		protected void exitPrimitiveSite(pl_PrimitiveSite tokens) {
			Map<SiteType, Map<String, Integer>> externalPinWiresMap =
					new HashMap<>();
			externalPinWiresMap.put(currSite.getPossibleTypes()[0], externalWiresPool.add(externalPinWires));

			SiteType[] alternativeTypes = currSite.getPossibleTypes();
			for (int i = 1; i < alternativeTypes.length; i++) {
				Map<String, Integer> altExternalPinWires = new HashMap<>();
				SiteType altType = alternativeTypes[i];
				SiteTemplate site = device.getSiteTemplate(altType);
				for (String sitePin : site.getSources().keySet()) {
					Integer wire = getExternalWireForSitePin(altType, sitePin);
					altExternalPinWires.put(sitePin, wire);
				}
				for (String sitePin : site.getSinks().keySet()) {
					Integer wire = getExternalWireForSitePin(altType, sitePin);
					if (wire == null)
						System.out.println("There be an error here");
					altExternalPinWires.put(sitePin, wire);
				}

				externalPinWiresMap.put(altType, externalWiresPool.add(altExternalPinWires));
			}


			externalPinWiresMap = externalWiresMapPool.add(externalPinWiresMap);
			currSite.setExternalWires(externalPinWiresMap);

			externalPinWires = null;
			currSite = null;
		}

		private Integer getExternalWireForSitePin(SiteType altType, String sitePin) {
			Element pinEl = getPinmapElement(altType, sitePin);

			String connectedPin = sitePin;
			if (pinEl != null) {
				connectedPin = pinEl.getChildText("map");
			}

			return externalPinWires.get(connectedPin);
		}

		private Element getPinmapElement(SiteType altType, String sitePin) {
			Element ptEl = getSiteTypeEl(currSite.getPossibleTypes()[0]);
			Element alternativesEl = ptEl.getChild("alternatives");
			Element altEl = null;
			for (Element altTmpEl : alternativesEl.getChildren("alternative")) {
				if (altTmpEl.getChildText("name").equals(altType.name())) {
					altEl = altTmpEl;
					break;
				}
			}

			assert altEl != null;
			Element pinmapsEl = altEl.getChild("pinmaps");
			Element pinEl = null;
			if (pinmapsEl != null) {
				for (Element pinTmpEl : pinmapsEl.getChildren("pin")) {
					if (pinTmpEl.getChildText("name").equals(sitePin)) {
						pinEl = pinTmpEl;
						break;
					}
				}
			}
			return pinEl;
		}
	}

	private class PrimitiveDefsListener extends XDLRCParserListener {
		private final PrimitiveDefList defs;
		private PrimitiveDef currDef;
		private PrimitiveElement currElement;
		private ArrayList<PrimitiveDefPin> pins;
		private ArrayList<PrimitiveElement> elements;

		PrimitiveDefsListener() {
			defs = new PrimitiveDefList();
			device.setPrimitiveDefs(defs);
		}

		@Override
		protected void enterPrimitiveDef(pl_PrimitiveDef tokens) {
			currDef = new PrimitiveDef();
			String name = tokens.name.toUpperCase();
			currDef.setType(SiteType.valueOf(device.getFamily(), name));

			pins = new ArrayList<>(tokens.pin_count);
			elements = new ArrayList<>(tokens.element_count);

			currDef.setPins(pins);
			currDef.setElements(elements);
		}

		@Override
		protected void exitPrimitiveDef(pl_PrimitiveDef tokens) {
			currDef.setPins(pins);
			currDef.setElements(elements);
			defs.add(currDef);
		}

		@Override
		protected void enterPin(pl_Pin tokens) {
			PrimitiveDefPin p = new PrimitiveDefPin();
			p.setExternalName(tokens.external_name);
			p.setInternalName(tokens.internal_name);
			p.setDirection(tokens.direction.startsWith("output") ? PinDirection.OUT : PinDirection.IN);
			pins.add(p);
		}

		@Override
		protected void enterElement(pl_Element tokens) {
			currElement = new PrimitiveElement();
			currElement.setName(tokens.name);
			currElement.setBel(tokens.isBel);
		}

		@Override
		protected void exitElement(pl_Element tokens) {
			// Determine the element type
			if (!currElement.isBel()) {
				if (currElement.getCfgOptions() != null && !currElement.getPins().isEmpty())
					currElement.setMux(true);
				else if (currElement.getCfgOptions() != null) // && currElement.getPins() == null
					currElement.setConfiguration(true);
				else if (currElement.getName().startsWith("_ROUTETHROUGH"))
					currElement.setRouteThrough(true);
				else
					currElement.setPin(true);
			}
			elements.add(currElement);
		}

		@Override
		protected void enterElementPin(pl_ElementPin tokens) {
			PrimitiveDefPin elementPin = new PrimitiveDefPin();
			elementPin.setExternalName(tokens.name);
			elementPin.setInternalName(tokens.name);
			elementPin.setDirection(tokens.direction.startsWith("output") ? PinDirection.OUT : PinDirection.IN);
			currElement.addPin(elementPin);
		}

		@Override
		protected void enterElementCfg(pl_ElementCfg tokens) {
			for(String cfg : tokens.cfgs){
				currElement.addCfgOption(cfg);
			}
		}

		@Override
		protected void enterElementConn(pl_ElementConn tokens) {
			PrimitiveConnection c = new PrimitiveConnection();
			c.setElement0(tokens.element0);
			c.setPin0(tokens.pin0);
			c.setForwardConnection(tokens.direction.equals("==>"));
			c.setElement1(tokens.element1);
			c.setPin1(tokens.pin1);
			currElement.addConnection(c);
		}
	}
}
