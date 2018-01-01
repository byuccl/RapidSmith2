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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.byu.ece.rapidSmith.design.subsite.BelRoutethrough;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.ImplementationMode;
import edu.byu.ece.rapidSmith.device.Connection;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.SitePin;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.device.SiteWire;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.PIP;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileWire;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.WireEnumerator;

import static edu.byu.ece.rapidSmith.util.Exceptions.ParseException;

/**
 * This class is used for parsing the static_resources.rsc file in a RSCP checkpoint.
 * This file is used to specify which resources in a reconfigurable area that the static
 * design uses.
 */
public class UsedStaticResources {

	private final Device device;
	private final CellDesign design;
	private final WireEnumerator wireEnumerator;
//	private final HashMap<SitePin, IntrasiteRoute> sitePinToRouteMap;
//	private final Map<BelPin, CellPin> belPinToCellPinMap;
//	private final Map<SiteType, Set<String>> staticSourceMap;
	private Set<Bel> staticSourceBels;
	private int currentLineNumber;
	private String currentFile;
	private Map<Bel, BelRoutethrough> belRoutethroughMap;
	private Pattern pipNamePattern;
//	private Map<String, String> oocPortMap;
//	private ImplementationMode implementationMode;
//	private boolean pipUsedInRoute = false;
	
	/**
	 * Creates a new XdcRoutingInterface object.
	 * 
	 * @param design {@link CellDesign} to add routing information to
	 * @param device {@link Device} of the specified design
	 */
	public UsedStaticResources(CellDesign design, Device device) {
		this.device = device;
		this.wireEnumerator = device.getWireEnumerator();
		this.design = design;
//		this.sitePinToRouteMap = new HashMap<>();
//		this.staticSourceMap = new HashMap<>();
//		this.belPinToCellPinMap = pinMap;
		this.currentLineNumber = 0;
		this.pipNamePattern = Pattern.compile("(.*)/.*\\.([^<]*)((?:<<)?->>?)(.*)"); 
//		this.implementationMode = mode;
	}
	
	/**
	 * Returns a set of BELs that are being used as a routethrough.
	 * This is only valid after {@link XdcRoutingInterface::parseRoutingXDC} has been called.
	 * @return A map from a {@link Bel}s to {@link BelRoutethrough}s
	 */
	public Map<Bel, BelRoutethrough> getRoutethroughsBels() {
		
		return (this.belRoutethroughMap == null) ? 
				Collections.emptyMap() :
					belRoutethroughMap;
	}
	
	/**
	 * Returns a set of BELs that are being used as a static source (VCC or GND).
	 * This is only valid after {@link XdcRoutingInterface::parseRoutingXDC} has been called.
	 */
	public Set<Bel> getStaticSourceBels() {
		
		return (staticSourceBels == null) ? 
				Collections.emptySet() :
				staticSourceBels;
	}
		
	/**
	 * Parses the specified static_resources.rsc file, and marks used resources in the design as used.
	 * 
	 * @param resourcesFile static_resources.rsc file
	 * @throws IOException
	 */
	public void parseResourcesRSC(String resourcesFile) throws IOException {
		
		currentFile = resourcesFile;
		// Regex used to split lines via whitespace
		Pattern whitespacePattern = Pattern.compile("\\s+");
		
		// try-with-resources to guarantee no resource leakage
		try (LineNumberReader br = new LineNumberReader(new BufferedReader(new FileReader(resourcesFile)))) {
		
			String line;
			while ((line = br.readLine()) != null) {
				this.currentLineNumber = br.getLineNumber();
				String[] toks = whitespacePattern.split(line);
	
				// TODO: I know the order these things appear in the file, so I probably don't need a big switch statement
				// SITE_PIPS -> STATIC_SOURCES -> LUT_RTS -> INTRASITE/INTERSITE/ROUTE
				// Update this if there is a performance issue, but it should be fine
				switch (toks[0]) {
					case "USED_PIPS" : 
						processUsedPips(toks);
						break;
					case "VCC_SOURCES" : 
						// processStaticSources(toks, true);
						break;
					case "GND_SOURCES" : 
						// processStaticSources(toks, false);
						break;
					case "LUT_RTS" : 
						// processLutRoutethroughs(toks); 
						break;
					case "SITE_RTS": 
						// processSiteRoutethroughs
						break; 
					default : 
						throw new ParseException("Unrecognized Token: " + toks[0]);
				}
			}
		}
	}
	
	
	/**
	 * 
	 * 
	 * @param toks List of used PIP tokens in the form: <br>
	 * {@code LUT_RTS tile0.tileType/sourceWire0->>sinkWire0 tile1.tileType/sourceWire1->sinkWire1 ...}
	 */
	private void processUsedPips(String[] toks) {
		System.out.println("processUsedPips");
		
		WireEnumerator we = device.getWireEnumerator();

		
		// Build a map from PIP names (tokens) to the names of the source and sink wires
		
		// 1. Mark the corresponding wire connections as used
		
		for (int i = 1; i < toks.length; i++ ) {			
			Matcher m = pipNamePattern.matcher(toks[i]);
			
			if (m.matches()) {
				String tileName = m.group(1);
//				System.out.println("Tile: " + tile);
				String source = m.group(1) + "/" + m.group(2);
//				System.out.println("huh: " + source);

				String sink = m.group(1) + "/" + m.group(4);

				Tile tile = device.getTile(tileName);
				Wire startWire = new TileWire(tile, we.getWireEnum("CLBLL_L_C"));
				Wire sinkWire = new TileWire(tile, we.getWireEnum("CLBLL_LOGIC_OUTS10"));
				PIP pip = new PIP(startWire, sinkWire);
				
//				if (tile.hasPIP(pip)) {
				System.out.println("Has PIP!");
				// Now mark the PIP as used and the other PIPs using the same start wire to unavailable
				tile.setUsedPIP(pip);
//				}
					
				// 2. Set connections using the same PIP junctions to unavailable
				
					
					
			}
			else {
				throw new ParseException("Invalid Pip String configuration: " + toks[i]);
			}
		}
		
		//Map<String, Set<String>> pipMap = buildPipMap(toks, 1);
		
		


//		this.belRoutethroughMap = new HashMap<>();
//		
//		for (int i = 1; i < toks.length; i++) {
//			String[] routethroughToks = toks[i].split("/");			
//			checkTokenLength(routethroughToks.length, 4);
//			
//			// TODO: Check that the input pin is an input pin and the output pin is an output pin?
//			Site site = tryGetSite(routethroughToks[0]);
//			Bel bel = tryGetBel(site, routethroughToks[1]);
//			BelPin inputPin = tryGetBelPin(bel, routethroughToks[2]);
//			BelPin outputPin = tryGetBelPin(bel, routethroughToks[3]);
//		
//			belRoutethroughMap.put(bel, new BelRoutethrough(inputPin, outputPin));
//		}
	}
	
	
	private Map<String, Set<String>> buildPipMap(String[] toks, int startIndex) {
		Map<String, Set<String>> pipMap = new HashMap<String, Set<String>>();
		
		// build the pip map for connections
		for (int i = startIndex; i < toks.length; i++ ) {			
			Matcher m = pipNamePattern.matcher(toks[i]);
			
			if (m.matches()) {
				String source = m.group(1) + "/" + m.group(2);
				String sink = m.group(1) + "/" + m.group(4);
				pipMap.computeIfAbsent(source, k -> new HashSet<String>()).add(sink);
				
				// if the PIP is a bi-directional pip, add both directions to the map...
				// the correct pip direction will be determined later in the routing import.
				if (m.group(3).equals("<<->>")) {
					pipMap.computeIfAbsent(sink, k -> new HashSet<String>()).add(source);
				}
			}
			else {
				throw new ParseException("Invalid Pip String configuration: " + toks[i]);
			}
		}
		return pipMap;
	}


	//TODO: Look at this more!
	/**
	 * Parse the used PIPS within a given {@link Site}, and store that information in the current {@link CellDesign}
	 * data structure. These site pips are used to correctly import intrasite routing later in the parse process. 
	 * 
	 * @param site {@link Site} object
	 * @param toks An array of used site PIPS in the form: <br>
	 * {@code SITE_PIPS siteName pip0:input0 pip1:input1 ... pipN:inputN}
	 */
	private void readUsedSitePips(Site site, String[] toks) {
		
		HashSet<Integer> usedSitePips = new HashSet<>();
		
		String namePrefix = "intrasite:" + site.getType().name() + "/";

		//create hashmap that shows pip used to input val
		HashMap<String, String> pipToInputVal = new HashMap<String, String>();
		
		// Iterate over the list of used site pips, and store them in the site
		for(int i = 2; i < toks.length; i++) {
			String pipWireName = (namePrefix + toks[i].replace(":", "."));
			Integer wireEnum = tryGetWireEnum(pipWireName); 
			
			SiteWire sw = new SiteWire(site, wireEnum);
			Collection<Connection> connList = sw.getWireConnections();
			
			// If the created wire has no connections, it is a polarity selector
			// that has been removed from the site
			if (connList.size() == 0) {
				continue;
			}
			
			assert (connList.size() == 1) : "Site Pip wires should have exactly one connection " + sw.getName() + " " + connList.size() ;
			
			Connection conn = connList.iterator().next();
			
			assert (conn.isPip()) : "Site Pip connection should be a PIP connection!";
			
			//add the input and output pip wires (there are two of these in RS2)
			// TODO: Is it useful to add the output wires?...I don't think these are necessary
			usedSitePips.add(wireEnum); 	
			usedSitePips.add(conn.getSinkWire().getWireEnum());
			// tryGetWireEnum(pipWireName.split("\\.")[0] + ".OUT")
			String[] vals = toks[i].split(":");
			assert vals.length == 2;
			pipToInputVal.put(vals[0], vals[1]);
		}
		
		design.setUsedSitePipsAtSite(site, usedSitePips);
		design.addPIPInputValsAtSite(site, pipToInputVal);
		
	}
	
	/**
	 * Compares the actual token length of a line in the routing.rsc file against the 
	 * expected token length. If they do not agree, a {@link ParseException} is thrown.
	 * 
	 * @param tokenLength
	 * @param expectedLength
	 */
	private void checkTokenLength(int tokenLength, int expectedLength) {
		
		if (tokenLength != expectedLength) {
			throw new ParseException(String.format("Incorrect number of tokens on line %d of %s.\n"
												+ "Expected: %d Actual: %d", currentLineNumber, currentFile, tokenLength, expectedLength));
		}
	}
	
	/**
	 * Tries to retrieve the integer enumeration of a wire name in the currently loaded device <br>
	 * If the wire does not exist, a ParseException is thrown <br>
	 * @param wireName
	 * @return
	 */
	private int tryGetWireEnum(String wireName) {
		
		Integer wireEnum = wireEnumerator.getWireEnum(wireName);
		
		if (wireEnum == null) {
			throw new ParseException(String.format("Wire: \"%s\" does not exist in the current device. \n"
												 + "On line %d of %s", wireName, currentLineNumber, currentFile));
		}
		
		return wireEnum;
	}
	
	
}
