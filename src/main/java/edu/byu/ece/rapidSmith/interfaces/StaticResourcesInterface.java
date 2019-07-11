package edu.byu.ece.rapidSmith.interfaces;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.*;
import java.util.regex.Pattern;

import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;
import static edu.byu.ece.rapidSmith.util.Exceptions.ParseException;

/**
 * This class is used for parsing the static_resources.rsc file in a RSCP checkpoint.
 * This file is used to specify which resources in a reconfigurable area that the static
 * design uses.
 */
public class StaticResourcesInterface extends AbstractXdcInterface {
	private final CellDesign design;
	/** Map from RM port name(s) to the static net's name */
	private Map<String, String> rmStaticNetMap;
	/** Map from the static net name to the route string tree */
	private Map<String, RouteStringTree> staticRouteStringMap;

	/**
	 * Creates a new XdcRoutingInterface object.
	 *
	 * @param design {@link CellDesign} to add routing information to
	 * @param device {@link Device} of the specified design
	 */
	public StaticResourcesInterface(CellDesign design, Device device) {
		super(device, design);
		this.design = design;
		this.rmStaticNetMap = new HashMap<>();
		this.staticRouteStringMap = new HashMap<>();
	}

	/**
	 * Parses the specified static_resources.rsc file.
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
					case "RESERVED_WIRES" :
						processReservedWires(toks);
						break;
					case "STATIC_RT" :
						processStaticRoutes(toks);
						break;
					case "SITE_RTS":
						processSiteRoutethroughs(toks);
						break;
					default :
						throw new ParseException("Unrecognized Token: " + toks[0]);
				}
			}
		}
	}

	/**
	 * Processes a string array of sites being uses as route-throughs by the static design.
	 * @param toks a String array of sites in the form: <br>
	 * {@code SITE_RTS tile0/site0 tile1/site1 ... tileN/siteN} </br>
	 */
	private void processSiteRoutethroughs(String[] toks) {
		for(int i = 1; i < toks.length; i++) {
			String[] vals = toks[i].split("/");
			assert vals.length == 2;
			String siteName = vals[1];
			Site site = tryGetSite(siteName);

			// Mark the site as reserved
			design.addReservedSite(site);
		}
	}

	/**
	 * Processes a string array of reserved wire tokens.
	 *
	 * @param toks a String array of used wire tokens in the form: <br>
	 * {@code RESERVED_WIRES tile0/wire1 tile1/wire2 ...} </br>
	 */
	private void processReservedWires(String[] toks) {
		for(int i = 1; i < toks.length; i++) {
			String[] vals = toks[i].split("/");
			assert vals.length == 2;
			String tileName = vals[0];
			String wireName = vals[1];

			Tile tile = tryGetTile(tileName);
			Wire reservedWire = new TileWire(tile, tryGetWireEnum(wireName));

			// Mark all wires in the node as reserved. These wires are used by the static design for other nets not
			// in the RM; i.e., they just route through the RM.
			design.addReservedNode(reservedWire);
		}
	}

	/**
	 * Creates a route string tree starting at branchRoot.
	 * @param branchRoot The start wire
	 * @param index index into the tokens
	 * @param toks tokens containing wires in the form tile0/wire0 tile1/wire1 ... tileN/wireN
	 * @return index into the tokens
	 */
	private int createIntersiteRouteStringTree(RouteStringTree branchRoot, int index, String[] toks) {
		RouteStringTree current = branchRoot;

		while ( index < toks.length ) {

			// new branch
			if (toks[index].equals("{") ) {
				index++;
				index = createIntersiteRouteStringTree(current, index, toks);
			}
			//end of a branch
			else if (toks[index].equals("}") ) {
				return index + 1;
			}
			else {
				current = current.addChild(toks[index]);
				index++;
			}
		}
		return index;
	}

	/**
	 * Processes the static portion of partition pin routes, creating route string trees.
	 *
	 * @param toks A string array of the form: <br>
	 * <br>
	 * {@code STATIC_RT staticNetName rmNetName0 rmNetName1 ... rmNetNameN {Tile0/Wire0 Tile1/Wire1 ... TileN/WireN}
	 * The {@code tile/wire} elements are the wires that make up the static portion of a partition pin route.
	 */
	private void processStaticRoutes(String[] toks) {
		assert(toks.length > 3);
		Set<CellNet> portNets = new HashSet<>();

		// First token (after STATIC_RT) is name of the static-net
		String staticNetName = toks[1];

		// Next tokens are the names of the associated ports
		int i = 2;
		while (!toks[i].equals("{")) {
			// Get the partition pin net. Note that it may be named differently than just the ooc port name.
			Cell oocPort = design.getCell(toks[i]);
			CellPin partPin = oocPort.getPin(toks[i]);
			CellNet partPinNet = partPin.getNet();

			assert (partPinNet != null);
			portNets.add(partPinNet);
			i++;
		}

		// Add all rm net names to the rm -> static net name map
		for (CellNet portNet : portNets) {
			rmStaticNetMap.put(portNet.getName(), staticNetName);
		}

		// Create the Route String Tree
		i++;
		RouteStringTree root = new RouteStringTree(toks[i]);
		createIntersiteRouteStringTree(root, i+1, toks);
		staticRouteStringMap.put(staticNetName, root);

	}

	public Map<String, RouteStringTree> getStaticRouteStringMap() {
		return staticRouteStringMap;
	}

	public Map<String, String> getRmStaticNetMap() {
		return rmStaticNetMap;
	}
}