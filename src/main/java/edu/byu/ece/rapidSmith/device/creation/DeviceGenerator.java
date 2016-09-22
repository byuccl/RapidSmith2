/*
 * Copyright (c) 2010 Brigham Young University
 *
 * This file is part of the BYU RapidSmith Tools.
 *
 * BYU RapidSmith Tools is free software: you may redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2 of
 * the License, or (at your option) any later version.
 *
 * BYU RapidSmith Tools is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * A copy of the GNU General Public License is included with the BYU
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 *
 */

package edu.byu.ece.rapidSmith.device.creation;

import edu.byu.ece.rapidSmith.design.xdl.XdlAttribute;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.device.helper.*;
import edu.byu.ece.rapidSmith.primitiveDefs.*;
import edu.byu.ece.rapidSmith.util.MessageGenerator;
import edu.byu.ece.rapidSmith.util.PartNameTools;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

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
	private Set<String> pipSources = new HashSet<>(PIP_CAPACITY);
	private Set<String> pipSinks = new HashSet<>(PIP_CAPACITY);

	/** Keeps track of each unique Wire object in the device */
	private HashPool<WireConnection> wirePool;
	/** Keeps track of each unique Wire[] object in the device */
	private HashPool<WireArray> wireArrayPool;
	/** Keeps track of all PIPRouteThrough objects */
	private HashPool<PIPRouteThrough> routeThroughPool;
	/** Keeps Track of all unique Sinks that exist in Tiles */
	private HashPool<HashMap<Integer, SinkPin>> tileSinkMapPool;
	/** Keeps track of all unique sinks that exist in tiles */
	private HashPool<SinkPin> sinksPool;
	/** Keeps Track of all unique Sources Lists that exist in Tiles */
	private HashPool<TileSources> tileSourcesPool;
	/** Keeps Track of all unique Wire Lists that exist in Tiles */
	private HashPool<WireHashMap> tileWiresPool;
	/** The number of source wires each wires */
	private Map<Tile, Map<Integer, Integer>> wireSourcesCount;

	private HashPool<Map<String, Integer>> externalWiresPool;
	private HashPool<Map<SiteType, Map<String, Integer>>> externalWiresMapPool;
	private HashPool<AlternativeTypes> alternativeTypesPool;

	/**
	 * Generates and returns the Device created from the XDLRC at the specified
	 * path.
	 *
	 * @param xdlrcPath path to the XDLRC file for the device
	 * @param familyInfo XML containing extended family info
	 * @return the generated Device representation
	 */
	public Device generate(Path xdlrcPath, Document familyInfo) {
		MessageGenerator.briefMessage("Generating device for file " + xdlrcPath.getFileName());

		this.familyInfo = familyInfo;
		this.device = new Device();
		this.we = new WireEnumerator();
		this.device.setWireEnumerator(we);

		this.wirePool = new HashPool<>();
		this.wireArrayPool = new HashPool<>();
		this.routeThroughPool = new HashPool<>();
		this.tileSinkMapPool = new HashPool<>();
		this.sinksPool = new HashPool<>();
		this.tileSourcesPool = new HashPool<>();
		this.tileWiresPool = new HashPool<>();
		this.externalWiresPool = new HashPool<>();
		this.externalWiresMapPool = new HashPool<>();
		this.alternativeTypesPool = new HashPool<>();

		// Requires a two part iteration, the first to obtain the tiles and sites,
		// and the second to gather the wires.  Two parses are required since the
		// wires need to know the source and sink tiles.
		XDLRCParser parser = new XDLRCParser();
		MessageGenerator.briefMessage("Starting first pass");
		parser.registerListener(new WireEnumeratorListener());
		parser.registerListener(new TileAndSiteGeneratorListener());
		parser.registerListener(new PrimitiveDefsListener());
		parser.registerListener(new XDLRCParseProgressListener());
		try {
			parser.parse(xdlrcPath);
		} catch (IOException e) {
			MessageGenerator.briefErrorAndExit("Error handling file " + xdlrcPath);
		}
		parser.clearListeners();

		device.constructTileMap();
		PrimitiveDefsCorrector.makeCorrections(device.getPrimitiveDefs(), familyInfo);
		device.setSiteTemplates(createSiteTemplates());
		setDistinguishedTypes(device, familyInfo);

		MessageGenerator.briefMessage("Starting second pass");
		parser.registerListener(new WireConnectionGeneratorListener());
		parser.registerListener(new SourceAndSinkListener());
		parser.registerListener(new XDLRCParseProgressListener());
		try {
			parser.parse(xdlrcPath);
		} catch (IOException e) {
			MessageGenerator.briefErrorAndExit("Error handling file " + xdlrcPath);
		}

		Map<Tile, Map<Integer, Set<WireConnection>>> wcsToAdd = getWCsToAdd();
		Map<Tile, Map<Integer, Set<WireConnection>>> wcsToRemove = getWCsToRemove();

		// These take up a lot of memory and we're going to regenerate each of these in the
		// next step.  Clearing these will allow for better garbage collection
		wirePool = new HashPool<>();
		wireArrayPool = new HashPool<>();
		tileWiresPool = new HashPool<>();

		makeWireCorrections(wcsToAdd, wcsToRemove);
	
		device.constructDependentResources();

		// free unneeded pools for garbage collection when done with
		routeThroughPool = null;
		tileSourcesPool = null;

		populateSinkPins();
		sinksPool = null;
		tileSinkMapPool = null;

		MessageGenerator.briefMessage("Finishing device creation process");

		return device;
	}

	private static void setDistinguishedTypes(Device device, Document familyInfo) {
		HashSet<TileType> smTypes = new HashSet<>();
		Element rootElement = familyInfo.getRootElement();
		Element smTypesEl = rootElement.getChild("switch_matrix_types");
		for (Element smTypeEl : smTypesEl.getChildren("type")) {
			TileType type = TileType.valueOf(smTypeEl.getText());
			smTypes.add(type);
		}
		device.setSwitchMatrixTypes(smTypes);

		device.setSliceTypes(new HashSet<>(4));
		device.setBramTypes(new HashSet<>(4));
		device.setFifoTypes(new HashSet<>(4));
		device.setDspTypes(new HashSet<>(4));
		device.setIOBTypes(new HashSet<>(4));

		for (Element ptEl : rootElement.getChild("primitive_types").getChildren("primitive_type")) {
			SiteType type = SiteType.valueOf(ptEl.getChildText("name"));
			if (ptEl.getChild("is_slice") != null)
				device.getSliceTypes().add(type);
			if (ptEl.getChild("is_bram") != null)
				device.getBramTypes().add(type);
			if (ptEl.getChild("is_fifo") != null)
				device.getFifoTypes().add(type);
			if (ptEl.getChild("is_dsp") != null)
				device.getDspTypes().add(type);
			if (ptEl.getChild("is_iob") != null)
				device.getIOBTypes().add(type);
		}
	}
	
	/**
	 * Creates the templates for the primitive sites with information from the
	 * primitive defs and device information file.
	 */
	private Map<SiteType, SiteTemplate> createSiteTemplates() {
		Map<SiteType, SiteTemplate> siteTemplates = new HashMap<>();

		// Create a template for each primitive type
		for (PrimitiveDef def : device.getPrimitiveDefs()) {
			Element ptEl = getPrimitiveTypeEl(def.getType());

			SiteTemplate template = new SiteTemplate();
			template.setType(def.getType());
			template.setBelTemplates(createBelTemplates(def, ptEl));
			createAndSetIntrasiteRouting(def, template, ptEl);
			createAndSetSitePins(def, template);

			Element compatTypesEl = ptEl.getChild("compatible_types");
			if (compatTypesEl != null) {
				List<SiteType> compatibleTypes = compatTypesEl.getChildren("compatible_type").stream()
						.map(compatTypeEl -> SiteType.valueOf(compatTypeEl.getText()))
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
			String destPin;

			// This connection goes the opposite of the way we want to search
			if (forward != c.isForwardConnection())
				continue;

			destElement = def.getElement(c.getElement1());
			destPin = c.getPin1();

			if (destElement.isBel()) {
				// We've reached a BEL.  Add the site pin to the BEL pin.
				BelPinTemplate pin = templates.get(destElement.getName()).getPinTemplate(destPin);
				pin.addSitePin(sitePin);
			} else if (destElement.isMux()) {
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
		assert false : "No type found for the specified BEL " + belName + ptElement.getChildText("name");
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
		
		Map <Integer, Set<Integer>> belRoutethroughMap = new HashMap<Integer, Set<Integer>>();
		
		for (Element belEl : siteElement.getChild("bels").getChildren("bel")) {
			String belType = belEl.getChildText("type");
			
			Element routethroughs = belEl.getChild("routethroughs");
			
			// bel has routethroughs
			if (routethroughs != null) {
				System.out.println(template.getType());
				for(Element routethrough : routethroughs.getChildren("routethrough")) {
				
					String inputPin = routethrough.getChildText("input");
					String outputPin = routethrough.getChildText("output");
					
					Integer startEnum = we.getWireEnum(getIntrasiteWireName(template.getType(), belType, inputPin)); 
					Integer endEnum = we.getWireEnum(getIntrasiteWireName(template.getType(), belType, outputPin));
										
					// check that the wire names actually exist
					assert (startEnum != null && endEnum != null) : "Intrasite wirename not found";
					
					// add the routethrough to the routethrough map; 
					Set<Integer> sinkWires = belRoutethroughMap.get(startEnum);					
					if (sinkWires == null) {
						sinkWires = new HashSet<Integer>();
						belRoutethroughMap.put(startEnum, sinkWires);
					}
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
			Map<Integer, XdlAttribute> sinkMap = muxes.get(srcWire);
			if (sinkMap == null) {
				sinkMap = new HashMap<>();
				muxes.put(srcWire, sinkMap);
			}
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
			List<WireConnection> wcs = wcsMap.get(source);
			if (wcs == null) {
				wcs = new ArrayList<>();
				wcsMap.put(source, wcs);
			}
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
	private Element getPrimitiveTypeEl(SiteType type) {
		Element primitiveTypesEl = familyInfo.getRootElement().getChild("primitive_types");
		for (Element primitiveTypeEl : primitiveTypesEl.getChildren("primitive_type")) {
			if (primitiveTypeEl.getChild("name").getText().equals(type.name()))
				return primitiveTypeEl;
		}
		assert false;
		return null;
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
			for (Integer wire : tile.getWires()) {
				Set<WireConnection> wcToAdd = new HashSet<>();
				Set<WireConnection> checkedConnections = new HashSet<>();
				Queue<WireConnection> connectionsToFollow = new LinkedList<>();

				// Add the wire to prevent building a connection back to itself
				checkedConnections.add(new WireConnection(wire, 0, 0, false));
				for (WireConnection wc : tile.getWireConnections(wire)) {
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
					tileWCsToAdd.put(wire, wcToAdd);
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
			Set<Integer> wires = new HashSet<>(tile.getWires());

			for (Integer wire : wires) {
				Set<WireConnection> wcToRemove = new HashSet<>();
				for (WireConnection wc : tile.getWireConnections(wire)) {
					// never remove PIPs.  We only are searching for different names
					// of the same wire.  A PIP connect unique wires.
					if (wc.isPIP())
						continue;
					if (!sourceWires.contains(wire) ||
							!wireIsSink(wc.getTile(tile), wc.getWire())) {
						wcToRemove.add(wc);
					}
				}
				tileWCsToRemove.put(wire, wcToRemove);
			}
			wcsToRemove.put(tile, tileWCsToRemove);
		}
		return wcsToRemove;
	}

	private Set<Integer> getSourceWiresOfTile(Tile tile) {
		Set<Integer> sourceWires = new HashSet<>();
		for (Integer wire : tile.getWires()) {
			if (we.getWireType(wire) == WireType.SITE_SOURCE)
				sourceWires.add(wire);
			for (WireConnection wc : tile.getWireConnections(wire)) {
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
		if (we.getWireType(wire) == WireType.SITE_SINK)
			return true;
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

			for (Integer wire : tile.getWires()) {
				Set<WireConnection> wcs =
						new HashSet<>(Arrays.asList(tile.getWireConnections(wire)));
				if (wcsToRemove.containsKey(tile) && wcsToRemove.get(tile).containsKey(wire))
					wcs.removeAll(wcsToRemove.get(tile).get(wire));
				if (wcsToAdd.containsKey(tile) && wcsToAdd.get(tile).containsKey(wire))
					wcs.addAll(wcsToAdd.get(tile).get(wire));

				if (wcs.size() > 0) {
					WireConnection[] arrView = wcs.toArray(new WireConnection[wcs.size()]);
					wireHashMap.put(wire, wireArrayPool.add(new WireArray(arrView)).array);
				}
			}

			// Update the tile with the new wire map.
			tile.setWireHashMap(tileWiresPool.add(wireHashMap));
		}
	}

	/**
	 * Finds the switch matrix tile which leads to the sink pin.  This information
	 * is useful during routing to determine what switch matrix we really want to
	 * be targeting.
	 */
	private void populateSinkPins() {
		buildWireSourcesCountMap();

		Stack<Wire> stack = new Stack<>();
		for (Tile tile : device.getTileMap().values()) {
			for (Integer wireEnum : wireSourcesCount.get(tile).keySet()) {
				// if 0, then any sink of this wire is unconnected to
				// the device unless it is a source
				// if 1, then this wire has a source that would itself be the source
				// of any sinks on the wire.
				// Tile sources, however, are always evaluated.
				Wire wire = new TileWire(tile, wireEnum);
				int numSources = wireSourcesCount.get(tile).get(wireEnum);
				if (numSources <= 1 && !arrayContains(tile.getSources(), wire.getWireEnum()))
					continue;

				addSinkWiresToTraverse(stack, wire);
				findAndAddSinkPins(stack, wire);
			}
		}

		// clean up the sources
		for (Tile tile : device.getTileMap().values())
			tile.setSinks(tileSinkMapPool.add(tile.getSinks()));
	}

	// Pathetic that this function doesn't exist in Java
	private static boolean arrayContains(int[] array, int value) {
		boolean isSource = false;
		for (int source : array)
			isSource |= (source == value);
		return isSource;
	}

	private void findAndAddSinkPins(Stack<Wire> stack, Wire srcWire) {
		// this wire might be a multi-sourced sinkPin
		checkAndAddSinkPin(srcWire, srcWire);

		while (!stack.isEmpty()) {
			Wire curWire = stack.pop();

			// Check if we've found a sink and add its pin
			checkAndAddSinkPin(srcWire, curWire);

			addSinkWiresToTraverse(stack, curWire);
		}
	}

	private void checkAndAddSinkPin(Wire srcWire, Wire curWire) {
		Tile curTile = curWire.getTile();
		Tile srcTile = srcWire.getTile();
		if (curTile.getSinks().containsKey(curWire.getWireEnum())) {
			int xOffset = (srcTile.getColumn() - curTile.getColumn());
			int yOffset = (srcTile.getRow() - curTile.getRow());

			SinkPin sinkPin = new SinkPin(srcWire.getWireEnum(), xOffset, yOffset);
			curTile.getSinks().put(curWire.getWireEnum(), sinksPool.add(sinkPin));
		}
	}

	private void addSinkWiresToTraverse(Stack<Wire> stack, Wire curWire) {
		Tile curTile = curWire.getTile();
		WireConnection[] wcs = curTile.getWireConnections(curWire.getWireEnum());
		if (wcs == null) return;
		for (WireConnection wc : wcs) {
			final Tile destTile = wc.getTile(curTile);
			final int destWire = wc.getWire();

			// Don't search through route throughs
			if (device.isRouteThrough(wc))
				continue;

			if (wireSourcesCount.get(destTile).get(destWire) == 1) {
				stack.push(new TileWire(destTile, destWire));
			}
		}
	}

	/**
	 * Creates a map storing the number of sources for each wire in the device.
	 */
	private void buildWireSourcesCountMap() {
		wireSourcesCount = new HashMap<>(device.getTileMap().size());
		for (Tile tile : device.getTileMap().values()) {
			wireSourcesCount.put(tile, new HashMap<>());
			for (Integer i : tile.getWires())
				wireSourcesCount.get(tile).put(i, 0);
		}
		for (Tile srcTile : device.getTileMap().values()) {
			if (srcTile.getWireHashMap() == null)
				continue;
			for (Integer wire : srcTile.getWires()) {
				for (WireConnection wc : srcTile.getWireConnections(wire)) {
					Tile sinkTile = wc.getTile(srcTile);
					Integer sinkWire = wc.getWire();

					// Don't include routethroughs in count
					if (device.isRouteThrough(wc))
						continue;
					int count = 0;
					if (wireSourcesCount.get(sinkTile).containsKey(sinkWire))
						count = wireSourcesCount.get(sinkTile).get(sinkWire);
					wireSourcesCount.get(sinkTile).put(sinkWire, count + 1);
				}
			}
		}
	}

	/**
	 * Remove duplicate wire resources in the tile.
	 */
	private void removeDuplicateTileResources(Tile tile) {
		WireHashMap origTileWires = tile.getWireHashMap();
		for (Integer wire : tile.getWires()) {
			WireArray unique = wireArrayPool.add(new WireArray(origTileWires.get(wire)));
			tile.getWireHashMap().put(wire, unique.array);
		}

		WireHashMap retrievedTileWires = tileWiresPool.add(origTileWires);
		tile.setWireHashMap(retrievedTileWires);
	}

	private static String getIntrasiteWireName(
			SiteType type, String element, String pinName) {
		return "intrasite:" + type.name() + "/" + element + "." + pinName;
	}

	private final class WireEnumeratorListener extends XDLRCParserListener {
		private static final int PIN_SET_CAPACITY = 10000;

		private Set<String> wireSet = new TreeSet<>();
		private Set<String> inpinSet = new HashSet<>(PIN_SET_CAPACITY);
		private Set<String> outpinSet = new HashSet<>(PIN_SET_CAPACITY);

		private SiteType currType;
		private String currElement;

		@Override
		/**
		 * Tracks special site pin wires.
		 */
		protected void enterPinWire(List<String> tokens) {
			String externalName = stripTrailingParenthesis(tokens.get(3));

			if (tokens.get(2).equals("input")) {
				inpinSet.add(externalName);
			} else {
				outpinSet.add(externalName);
			}
		}

		@Override
		protected void enterWire(List<String> tokens) {
			String wireName = stripTrailingParenthesis(tokens.get(1));
			wireSet.add(wireName);
		}

		@Override
		protected void enterPip(List<String> tokens) {
			pipSources.add(tokens.get(2));

			String wireName = stripTrailingParenthesis(tokens.get(4));
			pipSinks.add(wireName);
		}

		@Override
		protected void enterPrimitiveDef(List<String> tokens) {
			currType = SiteType.valueOf(tokens.get(1));
		}

		@Override
		protected void enterElement(List<String> tokens) {
			currElement = tokens.get(1);
		}

		@Override
		protected void enterElementPin(List<String> tokens) {
			String wireName = getIntrasiteWireName(currType, currElement, tokens.get(1));
			wireSet.add(wireName);
		}

		@Override
		protected void exitXdlResourceReport(List<String> tokens) {
			WireExpressions wireExp = new WireExpressions();

			Map<String, Integer> wireMap = new HashMap<>((int) (wireSet.size() / 0.75 + 1));
			String[] wires = new String[wireSet.size()];
			WireType[] wireTypes = new WireType[wireSet.size()];
			WireDirection[] wireDirections = new WireDirection[wireSet.size()];

			int i = 0;
			for (String wire : wireSet) {
				wires[i] = wire;
				wireMap.put(wire, i);

				if (inpinSet.contains(wire)) {
					wireTypes[i] = WireType.SITE_SINK;
					wireDirections[i] = WireDirection.EXTERNAL;
				} else if (outpinSet.contains(wire)) {
					wireTypes[i] = WireType.SITE_SOURCE;
					wireDirections[i] = WireDirection.EXTERNAL;
				} else {
					wireTypes[i] = wireExp.getWireType(wire);
					wireDirections[i] = wireExp.getWireDirection(wire);
				}
				i++;
			}

			we.setWireMap(wireMap);
			we.setWires(wires);
			we.setWireTypes(wireTypes);
			we.setWireDirections(wireDirections);
		}
	}

	private final class TileAndSiteGeneratorListener extends XDLRCParserListener {
		private ArrayList<Site> tileSites;

		private Tile currTile;

		@Override
		protected void enterXdlResourceReport(List<String> tokens) {
			device.setPartName(PartNameTools.removeSpeedGrade(tokens.get(2)));
		}

		@Override
		protected void enterTiles(List<String> tokens) {
			int rows = Integer.parseInt(tokens.get(1));
			int columns = Integer.parseInt(tokens.get(2));

			device.createTileArray(rows, columns);
		}

		@Override
		protected void enterTile(List<String> tokens) {
			int row = Integer.parseInt(tokens.get(1));
			int col = Integer.parseInt(tokens.get(2));
			currTile = device.getTile(row, col);
			currTile.setName(tokens.get(3));
			currTile.setType(TileType.valueOf(tokens.get(4)));
			currTile.setSinks(new HashMap<>());

			tileSites = new ArrayList<>();
		}

		@Override
		protected void enterPrimitiveSite(List<String> tokens) {
			Site site = new Site();
			site.setTile(currTile);
			site.setName(tokens.get(1));
			site.setIndex(tileSites.size());
			site.setBondedType(BondedType.valueOf(tokens.get(3).toUpperCase()));

			List<SiteType> alternatives = new ArrayList<>();
			SiteType type = SiteType.valueOf(tokens.get(2));
			alternatives.add(type);

			Element ptEl = getPrimitiveTypeEl(type);
			Element alternativesEl = ptEl.getChild("alternatives");
			if (alternativesEl != null) {
				alternatives.addAll(alternativesEl.getChildren("alternative").stream()
						.map(alternativeEl -> SiteType.valueOf(alternativeEl.getChildText("name")))
						.collect(Collectors.toList()));
			}

			SiteType[] arr = alternatives.toArray(new SiteType[alternatives.size()]);
			arr = alternativeTypesPool.add(new AlternativeTypes(arr)).types;
			site.setPossibleTypes(arr);

			tileSites.add(site);
		}

		@Override
		protected void exitTile(List<String> tokens) {
			// Create an array of primitive sites (more compact than ArrayList)
			if (tileSites.size() > 0) {
				currTile.setPrimitiveSites(tileSites.toArray(
						new Site[tileSites.size()]));
			} else {
				currTile.setPrimitiveSites(null);
			}

			currTile = null;
			tileSites = null;
		}
	}

	private final class WireConnectionGeneratorListener extends XDLRCParserListener {
		private Tile currTile;
		private int currTileWire;
		private boolean currTileWireIsSource;

		@Override
		protected void enterXdlResourceReport(List<String> tokens) {
		}

		@Override
		protected void enterTile(List<String> tokens) {
			int row = Integer.parseInt(tokens.get(1));
			int col = Integer.parseInt(tokens.get(2));
			currTile = device.getTile(row, col);
			currTile.setWireHashMap(new WireHashMap());
		}

		@Override
		protected void exitTile(List<String> tokens) {
			removeDuplicateTileResources(currTile);
		}

		@Override
		protected void enterWire(List<String> tokens) {
			String wireName = tokens.get(1);
			currTileWire = we.getWireEnum(wireName);
			currTileWireIsSource =
					we.getWireType(currTileWire) == WireType.SITE_SOURCE ||
							pipSinks.contains(wireName);
		}

		@Override
		protected void exitWire(List<String> tokens) {
			currTileWire = -1;
		}

		@Override
		protected void enterConn(List<String> tokens) {
			String currWireName = tokens.get(2).substring(0, tokens.get(2).length() - 1);
			int currWire = we.getWireEnum(currWireName);
			boolean currWireIsSiteSink = we.getWireType(currWire) == WireType.SITE_SINK;
			boolean currWireIsPIPSource = pipSources.contains(currWireName);
			boolean currWireIsSink = currWireIsSiteSink || currWireIsPIPSource;
			if (currTileWireIsSource || currWireIsSink) {
				Tile t = device.getTile(tokens.get(1));
				WireConnection wc = new WireConnection(currWire,
						currTile.getRow() - t.getRow(),
						currTile.getColumn() - t.getColumn(),
						false);
				currTile.addConnection(currTileWire, wirePool.add(wc));
			}
		}

		@Override
		protected void enterPip(List<String> tokens) {
			String endWireName;
			WireConnection wc;

			Integer startWire = we.getWireEnum(tokens.get(2));
			if (tokens.get(4).endsWith(")")) { // regular pip
				endWireName = tokens.get(4).substring(0, tokens.get(4).length() - 1);
				wc = wirePool.add(new WireConnection(we.getWireEnum(endWireName), 0, 0, true));
			} else { // route-through PIP
				endWireName = tokens.get(4);
				Integer endWireEnum = we.getWireEnum(endWireName);
				wc = wirePool.add(new WireConnection(endWireEnum, 0, 0, true));
				SiteType type = SiteType.valueOf(tokens.get(6).substring(0, tokens.get(6).length() - 2));

				String[] parts = tokens.get(5).split("-");
				String inPin = parts[1];
				String outPin = parts[2];

				PIPRouteThrough currRouteThrough = new PIPRouteThrough(type, inPin, outPin);
				currRouteThrough = routeThroughPool.add(currRouteThrough);
				device.addRouteThrough(startWire, endWireEnum, currRouteThrough);
			}
			currTile.addConnection(startWire, wc);
		}
	}

	private final class SourceAndSinkListener extends XDLRCParserListener {
		private Tile currTile;
		private Site currSite;
		private Set<Integer> tileSources;
		private Map<String, Integer> externalPinWires;

		@Override
		protected void enterTile(List<String> tokens) {
			int row = Integer.parseInt(tokens.get(1));
			int col = Integer.parseInt(tokens.get(2));
			currTile = device.getTile(row, col);

			tileSources = new TreeSet<>();
		}

		@Override
		protected void exitTile(List<String> tokens) {
			// We're converting the set of Integer objects to an int array
			// so we need to do this the long way
			int[] sourcesArray = new int[tileSources.size()];
			Iterator<Integer> it = tileSources.iterator();
			int i = 0;
			while (it.hasNext()) {
				sourcesArray[i++] = it.next();
			}

			// Remove duplicates
			TileSources tileSources = new TileSources(sourcesArray);
			currTile.setSources(tileSourcesPool.add(tileSources).sources);

			currTile = null;
		}

		@Override
		protected void enterPrimitiveSite(List<String> tokens) {
			currSite = device.getPrimitiveSite(tokens.get(1));
			externalPinWires = new HashMap<>();
		}

		@Override
		protected void enterPinWire(List<String> tokens) {
			String name = tokens.get(1);
			PinDirection direction =
					tokens.get(2).equals("input") ? PinDirection.IN : PinDirection.OUT;
			String externalWireName = stripTrailingParenthesis(tokens.get(3));
			externalPinWires.put(name, we.getWireEnum(externalWireName));

			if (direction == PinDirection.IN) {
				currTile.addSink(we.getWireEnum(externalWireName));
			} else {
				tileSources.add(we.getWireEnum(externalWireName));
			}
		}

		@Override
		protected void exitPrimitiveSite(List<String> tokens) {
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
			Element ptEl = getPrimitiveTypeEl(currSite.getPossibleTypes()[0]);
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
			for (Element pinTmpEl : pinmapsEl.getChildren("pin")) {
				if (pinTmpEl.getChildText("name").equals(sitePin)) {
					pinEl = pinTmpEl;
					break;
				}
			}
			return pinEl;
		}
	}

	private class PrimitiveDefsListener extends XDLRCParserListener {
		private PrimitiveDefList defs;
		private PrimitiveDef currDef;
		private PrimitiveElement currElement;
		private ArrayList<PrimitiveDefPin> pins;
		private ArrayList<PrimitiveElement> elements;

		public PrimitiveDefsListener() {
			defs = new PrimitiveDefList();
			device.setPrimitiveDefs(defs);
		}

		@Override
		protected void enterPrimitiveDef(List<String> tokens) {
			currDef = new PrimitiveDef();
			String name = tokens.get(1).toUpperCase();
			currDef.setType(SiteType.valueOf(name));

			pins = new ArrayList<>(Integer.parseInt(tokens.get(2)));
			elements = new ArrayList<>(Integer.parseInt(tokens.get(3)));

			currDef.setPins(pins);
			currDef.setElements(elements);
		}

		@Override
		protected void exitPrimitiveDef(List<String> tokens) {
			currDef.setPins(pins);
			currDef.setElements(elements);
			defs.add(currDef);
		}

		@Override
		protected void enterPin(List<String> tokens) {
			PrimitiveDefPin p = new PrimitiveDefPin();
			p.setExternalName(tokens.get(1));
			p.setInternalName(tokens.get(2));
			p.setDirection(tokens.get(3).startsWith("output") ? PinDirection.OUT : PinDirection.IN);
			pins.add(p);
		}

		@Override
		protected void enterElement(List<String> tokens) {
			currElement = new PrimitiveElement();
			currElement.setName(tokens.get(1));
			currElement.setBel(tokens.size() >= 5 && tokens.get(3).equals("#") && tokens.get(4).equals("BEL"));
		}

		@Override
		protected void exitElement(List<String> tokens) {
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
		protected void enterElementPin(List<String> tokens) {
			PrimitiveDefPin elementPin = new PrimitiveDefPin();
			elementPin.setExternalName(tokens.get(1));
			elementPin.setInternalName(tokens.get(1));
			elementPin.setDirection(tokens.get(2).startsWith("output") ? PinDirection.OUT : PinDirection.IN);
			currElement.addPin(elementPin);
		}

		@Override
		protected void enterElementCfg(List<String> tokens) {
			for(int k = 1; k < tokens.size(); k++){
				currElement.addCfgOption(tokens.get(k).replace(")", ""));
			}
		}

		@Override
		protected void enterElementConn(List<String> tokens) {
			PrimitiveConnection c = new PrimitiveConnection();
			c.setElement0(tokens.get(1));
			c.setPin0(tokens.get(2));
			c.setForwardConnection(tokens.get(3).equals("==>"));
			c.setElement1(tokens.get(4));
			c.setPin1(tokens.get(5).substring(0, tokens.get(5).length() - 1));
			currElement.addConnection(c);
		}
	}
}
