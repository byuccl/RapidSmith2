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
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.PIP;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileWire;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import javafx.util.Pair;
import org.apache.commons.lang3.tuple.MutablePair;

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
	private Pattern pipNamePattern;

    // Map from port name to a pair of the static net name and the static portion of the route string
	private Map<String, MutablePair<String, String>> staticRoutemap;

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
		this.pipNamePattern = Pattern.compile("(.*)/.*\\.([^<]*)((?:<<)?->>?)(.*)"); 
	}
	
	/**
	 * Parses the specified static_resources.rsc file, and marks used resources in the design as used.
	 * 
	 * @param resourcesFile static_resources.rsc file
	 * @throws IOException
	 */
	public void parseResourcesRSC(String resourcesFile) throws IOException {
		// Regex used to split lines via whitespace
		Pattern whitespacePattern = Pattern.compile("\\s+");
		
		// try-with-resources to guarantee no resource leakage
		try (LineNumberReader br = new LineNumberReader(new BufferedReader(new FileReader(resourcesFile)))) {
		
			String line;
			while ((line = br.readLine()) != null) {
				String[] toks = whitespacePattern.split(line);
	
				switch (toks[0]) {
					case "USED_PIPS" : 
						processUsedPips(toks);
						break;
					case "STATIC_RT" :
						// FIXME: Everything doesn't really need to be split by whitespace for processStaticRoutes.
						processStaticRoutes(toks);
						break;
					case "VCC_SOURCES" : 
					case "GND_SOURCES" : 
					case "LUT_RTS" : 
					case "SITE_RTS":
						// Used static sources, LUT Routethroughs, & Site Routethroughs
						// aren't expected to be found within a PR region.
						if (toks.length > 1)
							throw new ParseException("Unexpected Token Content: " + toks[0]);
						break;
					default : 
						throw new ParseException("Unrecognized Token: " + toks[0]);
				}
			}
		}
	}
	
	/**
	 * Processes an array of used PIP tokens and marks used PIP wire connections as used.
	 * 
	 * @param toks a String array of used PIP tokens in the form: <br>
	 * {@code LUT_RTS tile0.tileType/sourceWire0->>sinkWire0 tile1.tileType/sourceWire1->sinkWire1 ...}
	 */
	private void processUsedPips(String[] toks) {		
		
		for (int i = 1; i < toks.length; i++ ) {			
			Matcher m = pipNamePattern.matcher(toks[i]);
			
			if (m.matches()) {
				String tileName = m.group(1);
				String source = m.group(2);
				String sink = m.group(4);

				Tile tile = device.getTile(tileName);
				Wire startWire = new TileWire(tile, wireEnumerator.getWireEnum(source));
				Wire sinkWire = new TileWire(tile, wireEnumerator.getWireEnum(sink));
		
				PIP pip = new PIP(startWire, sinkWire);
				tile.setUsedPIP(pip, false); // Mark the PIP as used 
	 				
			}
			else {
				throw new ParseException("Invalid Pip String configuration: " + toks[i]);
			}
		}	
	}

	/**
	 *
	 * @param toks
	 */
	private void processStaticRoutes(String[] toks) {
		assert(toks.length > 3);
		String staticRouteString = "";
		ArrayList<String> portNames  = new ArrayList<>();

		// First token (after STATIC_RT) is name of the static-net
		String staticNetName = toks[1];

		// Next tokens are the names of the associated ports
		int i = 2;
		while (!toks[i].equals("{")) {
			portNames.add(toks[i]);
			i++;
		}

		// FIXME: This is pretty unnecessary
		for (; i < toks.length; i++) {
			staticRouteString += toks[i] + " ";
		}

		MutablePair<String, String> netRoute = new MutablePair<>(staticNetName, staticRouteString);

		if (this.staticRoutemap == null)
			this.staticRoutemap = new HashMap<>();

		for (String portName : portNames) {
			staticRoutemap.put(portName, netRoute);
		}

	}

    public Map<String, MutablePair<String, String>> getStaticRoutemap() {
        return staticRoutemap;
    }

}
