package edu.byu.ece.rapidSmith.interfaces;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
	private Map<Bel, BelRoutethrough> belRoutethroughMap;
	/** PIPs used by the static design */
	private Collection<PIP> staticPips;

	/**
	 * Creates a new XdcRoutingInterface object.
	 *
	 * @param design {@link CellDesign} to add routing information to
	 * @param device {@link Device} of the specified design
	 */
	public StaticResourcesInterface(CellDesign design, Device device, Map<Bel, BelRoutethrough> belRoutethroughMap) {
		super(device, design);
		this.design = design;
		this.rmStaticNetMap = new HashMap<>();
		this.staticRouteStringMap = new HashMap<>();
		this.belRoutethroughMap = belRoutethroughMap;
		staticPips = new ArrayList<>();
	}

	public StaticResourcesInterface(CellDesign design, Device device) {
		super(device, design);
		this.design = design;
		this.rmStaticNetMap = new HashMap<>();
		this.staticRouteStringMap = new HashMap<>();
		this.belRoutethroughMap = new HashMap<>();
		staticPips = new ArrayList<>();
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
					case "RESERVED_PIPS" :
						processReservedPips(toks);
						break;
					case "STATIC_RT" :
						processStaticRoutes(toks);
						break;
					case "SITE_RT":
						processSiteRoutethrough(toks);
						break;
					case "SITE_PIPS":
						processSitePips(toks);
						break;
					case "SITE_LUTS":
						processSiteLuts(toks);
						break;
					default :
						throw new ParseException("Unrecognized Token: " + toks[0]);
				}
			}
		}
	}

	/**
	 * Processes a single site being used as a route-through.
	 * @param toks a String array of the form: <br>
	 * {@code SITE_RT site routethroughPip} </br>
	 */
	private void processSiteRoutethrough(String[] toks) {
		assert toks.length == 3;
		String siteName = toks[1];
		Site site = tryGetSite(siteName);

		// Reserve the site
		design.addReservedSite(site);

		// Currently don't do anything with the site route-through PIP in toks[2].
		// In the future, we could create a mapping from these to the
		// individual LUT route-throughs and site PIPs that make up
		// a site route-through PIP.
	}

	/**
	 * Processes the site PIPs used in a site route-through.
	 * If a mapping from site route-throughs to PIPs and LUT route-throughs is added, this method can be removed.
	 * @param toks a String array of the form: <br>
	 * {@code SITE_PIPS site pip1 pip2 ...} </br>
	 */
	private void processSitePips(String[] toks) {
		Site site = tryGetSite(toks[1]);
		HashSet<Integer> usedSitePips = new HashSet<>();
		HashMap<String, String> pipToInputVal = new HashMap<>();
		String namePrefix = "intrasite:" + site.getType().name() + "/";

		for(int i = 2; i < toks.length; i++) {
			String pipWireName = (namePrefix + toks[i].replace(":", "."));
			Integer wireEnum = tryGetWireEnum(pipWireName);
			SiteWire sw = new SiteWire(site, wireEnum);
			Collection<Connection> connList = sw.getWireConnections();
			assert (connList.size() == 1 || connList.size() == 0) : "Site Pip wires should have one or no connections " + sw.getName() + " " + connList.size() ;

			if (connList.size() == 1) {
				Connection conn = connList.iterator().next();
				assert (conn.isPip()) : "Site Pip connection should be a PIP connection!";
				usedSitePips.add(wireEnum);
				usedSitePips.add(conn.getSinkWire().getWireEnum());
			}

			// If the created wire has no connections, it is a polarity selector that has been removed from the site.
			// We still need the input value in order to correctly import intra-site routing changes back into Vivado.
			String[] vals = toks[i].split(":");
			assert vals.length == 2;
			pipToInputVal.put(vals[0], vals[1]);
		}

		design.setUsedSitePipsAtSite(site, usedSitePips);
		design.addPIPInputValsAtSite(site, pipToInputVal);
	}

	/**
	 * Processes the LUT route-throughs used in a site route-through.
	 * If a mapping from site route-throughs to PIPs and LUT route-throughs is added, this method can be removed.
	 * @param toks a String array of the form: <br>
	 * {@code SITE_PIPS site lutRoutethrough1 lutRoutethrough2 ...} </br>
	 */
	private void processSiteLuts(String[] toks) {
		Site site = tryGetSite(toks[1]);
		for (int i = 2; i < toks.length; i++) {
			String[] routethroughToks = toks[i].split("/");
			Bel bel = tryGetBel(site, routethroughToks[0]);
			BelPin inputPin = tryGetBelPin(bel, routethroughToks[1]);
			BelPin outputPin = tryGetBelPin(bel, routethroughToks[2]);
			belRoutethroughMap.put(bel, new BelRoutethrough(inputPin, outputPin));
		}
	}

	/**
	 * Processes a string array of reserved pip tokens.
	 *
	 * @param toks a String array of used pip tokens in the form: <br>
	 * {@code RESERVED_WIRES pip0 pip1 ...} </br>
	 */
	private void processReservedPips(String[] toks) {
		for(int i = 1; i < toks.length; i++) {
			Matcher m = pipNamePattern.matcher(toks[i]);

			if (m.matches()) {
				String tileName = m.group(1);
				String direction = m.group(3);
				// No use for the direction in m.group(3)
				String source = m.group(2);
				String sink = m.group(4);

				// If it's bi-directional, we must figure out what direction is being used.
				if (direction.equals("<<->>")) {

				}


					Tile tile = tryGetTile(tileName);
				Wire sourceWire = new TileWire(tile, tryGetWireEnum(source));
				Wire sinkWire = new TileWire(tile, tryGetWireEnum(sink));
				PIP pip = new PIP(sourceWire, sinkWire);

				// Add to a list of static PIPs so it is easy to include this info in a FASM
				staticPips.add(pip);

				// Mark all wires in the node as reserved. These wires are used by the static design for other nets not
				// in the RM; i.e., they just route through the RM.
				design.addReservedNode(sourceWire);
				design.addReservedNode(sinkWire);
			}
			else {
				throw new ParseException("Invalid Pip String configuration: " + toks[i]);
			}
		}
	}

	/**
	 * Creates a route string tree starting at branchRoot.
	 * @param branchRoot The start wire
	 * @param index index into the tokens
	 * @param toks tokens containing wires in the form tile0/wire0 tile1/wire1 ... tileN/wireN
	 * @return index into the tokens
	 */
	private int createIntersiteRouteStringTree(CellNet net, RouteStringTree branchRoot, int index, String[] toks) {
		RouteStringTree current = branchRoot;

		while (index < toks.length) {

			// new branch
			if (toks[index].equals("{") ) {
				index++;
				index = createIntersiteRouteStringTree(net, current, index, toks);
			}
			//end of a branch
			else if (toks[index].equals("}") ) {
				return index + 1;
			}
			else {
				reserveWire(net, toks[index]);
				current = current.addChild(toks[index]);
				index++;
			}
		}
		return index;
	}

	/**
	 * Reserves the given wire if it can be found within the device
	 * @param wireName
	 */
	private void reserveWire(CellNet net, String wireName) {
		String[] vals = wireName.split("/");
		assert vals.length == 2;

		Tile tile = device.getTile(vals[0]);
		if (tile != null) {
			Wire wire = tile.getWire(vals[1]);
			if (wire != null) {
				design.addReservedNode(wire, net);
			}
		}
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

		// Add all rm net names to the rm -> static net name map.
		// Also add aliases (within the context of the full-device design) for the net.
		CellNet rmNet = null;
		for (CellNet portNet : portNets) {
			rmNet = (rmNet == null) ? portNet : rmNet;
			rmStaticNetMap.put(portNet.getName(), staticNetName);
			Set<CellNet> aliases = portNets.stream().filter(cellNet -> cellNet != portNet).collect(Collectors.toSet());
			portNet.setAliases(aliases);
		}

		// Create the Route String Tree and reserve used wires
		i++;
		RouteStringTree root = new RouteStringTree(toks[i]);
		createIntersiteRouteStringTree(rmNet, root, i+1, toks);
		staticRouteStringMap.put(staticNetName, root);
	}

	public Map<String, RouteStringTree> getStaticRouteStringMap() {
		return staticRouteStringMap;
	}

	public Map<String, String> getRmStaticNetMap() {
		return rmStaticNetMap;
	}

	public Collection<PIP> getStaticPips() {
		return staticPips;
	}

	public Map<Bel, BelRoutethrough> getBelRoutethroughMap() {
		return belRoutethroughMap;
	}
}