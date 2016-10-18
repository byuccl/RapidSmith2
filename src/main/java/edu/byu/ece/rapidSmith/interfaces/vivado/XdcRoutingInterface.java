package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.Connection;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.design.subsite.CellNet.RouteStatus;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.SitePin;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileWire;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import static edu.byu.ece.rapidSmith.util.Exceptions.ParseException;

/**
 * This class is used for parsing and writing routing XDC files in a TINCR checkpoint. <br>
 * Routing.xdc files are used to specify the physical wires that a net in Vivado uses.
 * 
 * TODO: Almost done with long lines, need to figure out how to handle long line bounces...
 * TODO: add some sort of debug method that makes it easy to debug
 * @author Thomas Townsend
 *
 */
public class XdcRoutingInterface {

	private Device device;
	private CellDesign design;
	private WireEnumerator wireEnumerator;
	private HashMap<Wire, RouteTree> wiresInNet = new HashMap<Wire, RouteTree>();
	private HashSet<RouteTree> terminals = new HashSet<RouteTree>();
	private HashMap<SitePin, IntrasiteRoute> sitePinToRouteMap;
	private Map<BelPin, CellPin> belPinToCellPinMap;
	private CellNet currentNet;
	private Map<BelPin, Wire> usedRoutethroughMap;
	private Map<SiteType, Set<String>> staticSourceMap;
	private Set<Bel> routethroughBels;
	private Set<Bel> staticSourceBels;
	private int currentLineNumber;
	private String currentFile;
	
	public XdcRoutingInterface(CellDesign design, Device device, Map<BelPin, CellPin> pinMap) {
		
		this.device = device;
		this.wireEnumerator = device.getWireEnumerator();
		this.design = design;
		this.sitePinToRouteMap = new HashMap<SitePin, IntrasiteRoute>();
		this.usedRoutethroughMap = new HashMap<BelPin, Wire>();
		this.staticSourceMap = new HashMap<SiteType, Set<String>>();
		this.belPinToCellPinMap = pinMap;
		this.currentLineNumber = 0;
	}
	
	// TODO: add these to the TincrCheckpoint class
	/**
	 * Returns a set of BELs that are being used as a routethrough
	 * @return
	 */
	public Set<Bel> getRoutethroughsBels() {
		
		return (routethroughBels == null) ? 
				Collections.emptySet() :
				routethroughBels;
	}
	
	/**
	 * Returns a set of BELs that are being used as a static source (VCC or GND)
	 * @return
	 */
	public Set<Bel> getStaticSourceBels() {
		
		return (staticSourceBels == null) ? 
				Collections.emptySet() :
				staticSourceBels;
	}
		
	/**
	 * Parses the specified routing.xdc file, and applies the physical wire information to the nets of the design
	 * 
	 * @param xdcFile routing.xdc file
	 * @param design Design to apply routing
	 * @param deviceA Device which the design is implemented on
	 * @throws IOException
	 */
	public void parseRoutingXDC(String xdcFile) throws IOException {

		currentFile = xdcFile;
		LineNumberReader br = new LineNumberReader(new BufferedReader(new FileReader(xdcFile)));
		String line = null;

		while ((line = br.readLine()) != null) {
			this.currentLineNumber = br.getLineNumber();
			String[] toks = line.split("\\s+");

			// TODO: I know the order these things appear in the file, so I probably don't need a big switch statement
			// SITE_PIPS -> STATIC_SOURCES -> LUT_RTS -> INTRASITE/INTERSITE/ROUTE
			// Update this if there is a performance issue, but it should be fine
			switch (toks[0]) {
			
				case "SITE_PIPS" : processSitePips(toks);
					break;
				case "INTERSITE" : processIntersitePins(toks);
					break;
				case "INTRASITE" : processIntrasitePins(toks);
					break;
				case "ROUTE" : processIntersiteRoute(toks); 
					break;
				case "LUT_RTS" : processLutRoutethroughs(toks); 
					break;
				case "STATIC_SOURCES" : processStaticSources(toks);
					break;
				default : 
					throw new ParseException("Unrecognized Token: " + toks[0]);
			}
		}
		
		br.close();
	}
	
	/**
	 * 
	 * @param toks
	 */
	private void processSitePips (String[] toks) {
		
		Site site = tryGetSite(toks[1]);
		readUsedSitePips(site, toks);
		createStaticSubsiteRouteTrees(site);
	}
	
	/**
	 * 
	 * @param toks
	 */
	private void processIntersitePins(String[] toks) {
		
		CellNet net = tryGetCellNet(toks[1]);
		// System.out.println(net.getName());
		for (int index = 2 ; index < toks.length; index++) {
			
			String[] sitePinToks = toks[index].split("/");
			
			assert (sitePinToks.length == 2);
			
			Site site = tryGetSite(sitePinToks[0]);
			SitePin pin = tryGetSitePin(site, sitePinToks[1]);
			
			if (pin.isInput()) {
				createIntrasiteRoute(pin, net, design.getUsedSitePipsAtSite(site));
			}
			else { // pin is an output of the site
				BelPin source = tryGetNetSource(net).getBelPin();
				createIntrasiteRoute(source, false, false, design.getUsedSitePipsAtSite(site));
			}
		}
	}
	
	/**
	 * 
	 * @param toks
	 */
	private void processIntrasitePins(String[] toks) {
		
		CellNet net = tryGetCellNet(toks[1]);		
		BelPin belPin = tryGetNetSource(net).getBelPin();
		
		if (belPin == null) {
			throw new AssertionError(net + " ");
		}
		
		Site site = belPin.getBel().getSite();
		createIntrasiteRoute(belPin, true, false, design.getUsedSitePipsAtSite(site));
		net.setIsIntrasite(true);
	}
	
	/**
	 * 
	 * @param toks
	 */
	private void processIntersiteRoute(String[] toks) {
		
		CellNet net = tryGetCellNet(toks[1]);
		this.currentNet = net;
		
		if (net.isVCCNet()) {
			net.setIntersiteRouteTrees(createRouteTreeListForPowerNet(net, toks));
		}
		else if (net.isGNDNet()) {
			net.setIntersiteRouteTrees(createRouteTreeListForPowerNet(net, toks));
		}
		else { // otherwise it's a general net 		
			RouteTree start = initializeRouteTree(net, toks[3]);
			createRouteTreeForNet(start, 4, toks, isBufgClkNet(net));
			net.addIntersiteRouteTree(start);
		}
		
		/*
	// 	CellNet net = tryGetCellNet(toks[1]);
		this.currentNet = net;
		
		if (net.isVCCNet()) {
			if (vccRouteTrees != null) {
				net.setIntersiteRouteTrees(vccRouteTrees);
			}
			else {
				vccRouteTrees = createRouteTreeListForPowerNet(net, toks);
			}
		}
		else if (net.isGNDNet()) {
			if (gndRouteTrees != null) {
				net.setIntersiteRouteTrees(gndRouteTrees);
			}
			else {
				gndRouteTrees = createRouteTreeListForPowerNet(net, toks);
			}
		}
		else { // otherwise, its a general wire
			
			RouteTree start = initializeRouteTree(net, toks[3]);
			createRouteTreeForNet(start, 4, toks, isBufgClkNet(net));
			net.addIntersiteRouteTree(start);
		}
		*/
	}
	
	/**
	 * Creates a map from BelPin to sink wire for all used routethroughs found in the routing.rsc file.
	 * TODO: create a list of BELs that are used as a routethrough so the user can access these?
	 * 
	 * @param toks List of routethrough tokens
	 */
	private void processLutRoutethroughs(String[] toks) {

		if (toks.length > 1) {
			this.routethroughBels = new HashSet<Bel>();
		}
		
		for (int i = 1; i < toks.length; i++) {
			String[] routethroughToks = toks[i].split("/");
			
			assert(routethroughToks.length == 3);
			
			Site site = tryGetSite(routethroughToks[0]);
			Bel bel = tryGetBel(site, routethroughToks[1]);
			BelPin belPin = tryGetBelPin(bel, routethroughToks[2]);
			
			Wire outputWire = bel.getSources().iterator().next().getWire();
			
			routethroughBels.add(bel);
			usedRoutethroughMap.put(belPin, outputWire);
		}
	}
	
	/**
	 * Create intrasite nets for static (VCC/GND) sources.
	 * 
	 * @param toks
	 */
	private void processStaticSources(String[] toks) {
		
		if (toks.length > 1) {
			this.staticSourceBels = new HashSet<Bel>();
		}
		
		for (int i = 1; i < toks.length; i++) {
			String[] staticToks = toks[i].split("/");
			
			assert(staticToks.length == 3);
			
			Site site = tryGetSite(staticToks[0]);
			Bel bel = tryGetBel(site, staticToks[1]);
			BelPin sourcePin = tryGetBelPin(bel, staticToks[2]);
			boolean routeFound = tryCreateStaticIntrasiteRoute(sourcePin, design.getUsedSitePipsAtSite(site));
			assert (routeFound == true) : site.getName() + "/" + bel.getName() + "/" + sourcePin.getName();
			staticSourceBels.add(bel);
		}
	}
					
	/*
	 * Read the used site pips for the given primitive site, and store the information in a HashSet
	 */
	private void readUsedSitePips(Site site, String[] toks) {
		
		HashSet<Integer> usedSitePips = new HashSet<Integer>();
		
		String namePrefix = "intrasite:" + site.getType() + "/";
		
		//list of site pip wires that are used...
		for(int i = 2; i <toks.length; i++) {
			String pipWireName = (namePrefix + toks[i].replace(":", "."));
			Integer wireEnum = tryGetWireEnum(pipWireName); 
			
			//add the input and output pip wires (there are two of these in RS2)
			usedSitePips.add(wireEnum); 	
			usedSitePips.add(tryGetWireEnum(pipWireName.split("\\.")[0] + ".OUT"));
		}
		
		design.setUsedSitePipsAtSite(site, usedSitePips);
	}
		
	/*
	 * Creates the initial RouteTree from a ROUTE string
	 */
	private RouteTree initializeRouteTree(CellNet net, String firstWireName) {
			
		// create the start of the intersite route tree
		CellPin source = tryGetNetSource(net);
		Tile sourceTile = source.getCell().getAnchorSite().getTile(); 
		Wire firstWire = new TileWire(sourceTile, wireEnumerator.getWireEnum(firstWireName));
		return new RouteTree(firstWire);
	}
	
	/*
	 * Creates the initial RouteTree for a GND or VCC ROUTE string
	 */
	private RouteTree initializeGlobalLogicRouteTree(String firstWireName) {
		
		String[] toks = firstWireName.split("/");
		assert(toks.length == 2); 
		
		Tile tile = device.getTile(toks[0]);
		Wire firstWire = new TileWire(tile, wireEnumerator.getWireEnum(toks[1]));
		
		return new RouteTree(firstWire);
	}
	
	private int createRouteTreeForNet(RouteTree current, int index, String[] toks, boolean isClkNet) {
		
		wiresInNet.clear();
		terminals.clear();
		int i = createIntersiteRouteTree(current, index, toks, isClkNet);
		current.getFirstSource().prune(terminals);
		return i;
	}
	
	private List<RouteTree> createRouteTreeListForPowerNet(CellNet net, String[] toks) {
		
		tryGetNetSource(net); // checking to see that the net has a source
		List<RouteTree> routeTrees = new ArrayList<RouteTree>();
		
		int index = 4; 	
		while(index < toks.length) {
			
			RouteTree start = initializeGlobalLogicRouteTree(toks[index++]);
			index = createRouteTreeForNet(start, index, toks, false);
			routeTrees.add(start);
			net.addIntersiteRouteTree(start);
			index += 3; // get to the start of the next route string
		}
		
		return routeTrees;
	}
	
	/*
	 * Converts a Vivado ROUTE string to a RS2 RouteTree data structure
	 */
	private int createIntersiteRouteTree(RouteTree current, int index, String[] toks, boolean isClkNet) {
				
		while ( index < toks.length ) {
			
			// new branch
			if (toks[index].equals("{") ) {
				index = createIntersiteRouteTree(current, ++index, toks, isClkNet);
			}
			//end of a branch
			else if (toks[index].equals("}") ) {
				completeFinalBranchNode(current);
				return index + 1; 
			}
			else {
				
				current = (isClkNet) ? 
							findNextClockNode(current, toks[index]) : 
							findNextNode(current, toks[index]);
				index++;
			}
		}
		
		return index;
	}
	
	private RouteTree findNextNode(RouteTree rt, String nodeName) {
		
		return findNextNode(rt, nodeName, new HashSet<Wire>());
	}
	
	/*
	 * Searches for the next node of the Vivado ROUTE string in RS2. Performs a bounded DFS 
	 * looking for the node of interest.  
	 * 
	 * TODO: update to use integer wire enums instead of string node names...I only have to parse the
	 * 		 nodename once when I first read in the nodename, but I don't need to do a string comparison 
	 * 		 after that because I can use the enum values instead. May have to create a new data structure
	 * 		 that builds on a RouteTree object to speed things up 
	 */
	private RouteTree findNextNode(RouteTree rt, String nodeName, HashSet<Wire> wireSet) {

		Queue<RouteTree> routeQueue = new LinkedList<RouteTree>();
		
		routeQueue.add(rt);
		wireSet.add(rt.getWire());
		
		// for bidirectional wires, prevent us from searching where we just came from.
		if (rt.getSourceTree() != null) {
			wireSet.add(rt.getSourceTree().getWire());
		}
		
		int debugCount = 0;
		
		while (!routeQueue.isEmpty()) {
			
			// just in case we get lost in our search...
			if (debugCount++ > 10000) { 
				break;
			}
			
			RouteTree nextRoute = routeQueue.poll();
						
			for (Connection c : nextRoute.getWire().getWireConnections()) {
			
				Wire sinkWire = c.getSinkWire();
				
				// skip long line sink connections in the same tile and wires we have already looked at
				if (isSinkLongLineBounce(nextRoute, sinkWire) || wireSet.contains(sinkWire)) {
					continue;
				}
				
				// found the wire we were looking for
				if (c.isPip() && sinkWire.getWireName().equals(nodeName)) { 
					return nextRoute.addConnection(c); 
				}
				
				wireSet.add(sinkWire);
				routeQueue.add(addRouteTreeConnection(nextRoute, c));
			}
		}
		
		// for debugging
		System.out.println(nodeName + " " + rt.getWire().getTile() + "/" + rt.getWire().getWireName());
		throw new AssertionError("Unable to find the next node after looking at 10,000 wires!");
	}
	
	private boolean isSinkLongLineBounce(RouteTree source, Wire sinkWire) {
		
		if (source.getSourceTree() == null) {
			return false;
		}
		
		Tile currentWireTile = source.getWire().getTile();
		Tile prevWireTile = source.getSourceTree().getWire().getTile();
		Tile sinkWireTile = sinkWire.getTile();
			
		return isLongLineWire(source.getWire()) &&
			   currentWireTile.equals(prevWireTile) &&
			   currentWireTile.equals(sinkWireTile);
	}
		
	private boolean isLongLineWire(Wire w) {
		
		String wireName = w.getWireName();
		return wireName.startsWith("LV") || wireName.startsWith("LH"); 
	}
	
	private RouteTree findNextClockNode(RouteTree rt, String nodeName) {
		
		// Special case for horizontal and vertical clk wires
		// In the Route string, they have names like "<2>HCLK..."
		if ( isDedicatedClockWire(nodeName) ) { 
			
			int endIndex = nodeName.indexOf(">");
			int offset = 1;
			String clkWireName = nodeName;
			
			if (endIndex > 0) {
				offset = Integer.parseInt(nodeName.substring(1, endIndex));
				clkWireName = nodeName.substring(endIndex+1);
			}
			
			TileDirection dir = getClockWireDirection(rt.getWire().getWireName(), clkWireName);	
			return getNextClockWire(rt, rt.getWire().getTile(), clkWireName, offset, dir);
		}
					
		return findNextNode(rt, nodeName); 
	}
	
	private boolean isDedicatedClockWire(String nodeName) {
		
		return nodeName.startsWith("<") || nodeName.startsWith("HCLK") || nodeName.startsWith("GCLK");
	}
	
	// This seems a little hackish, and is not guaranteed to work for ultrascale...
	// TODO: think of a better way to do this			
	private TileDirection getClockWireDirection(String prevNodeName, String clockWireName) {
		if (clockWireName.charAt(0) == 'H') {
			String[]toks = prevNodeName.split("_");
			return toks[toks.length-1].contains("L") ? TileDirection.LEFT : TileDirection.RIGHT;					
		}
		else {
			return prevNodeName.contains("BOT") ? TileDirection.BELOW : TileDirection.ABOVE;
		}
	}
			
	private RouteTree getNextClockWire(RouteTree tree, Tile tile, String wirename, int offset, TileDirection searchDir)
	{
		Tile currentTile = tile;
		int wire = wireEnumerator.getWireEnum(wirename);	
		
		assert(currentTile.getWires().contains(wire) == false);
		
		// find the correct tile the clock wire branches from based on the offset
		while (offset > 0) {
			
			currentTile = getAdjacentTile(currentTile, searchDir);
			
			if (currentTile.getWires().contains(wire)) {
				offset--;
			}
		}
		
		// TODO: ask Travis if there is a more efficient way of doing this...
		// find the wire connection that connects us to the correct tile
		boolean foundTile = false;
		for (Connection c : tree.getWire().getWireConnections()) {
			Tile sinkTile = c.getSinkWire().getTile();
			// TODO: test to make sure this works!
			if (sinkTile == currentTile) {
				tree = addRouteTreeConnection(tree, c);
				foundTile = true;
				break;
			}
		}
		assert (foundTile == true);
		
		// find the correct wire connection
		boolean foundWire = false;
		for (Connection c : tree.getWire().getWireConnections()) {
			String sinkWireName = c.getSinkWire().getWireName();
			
			if (sinkWireName.equals(wirename)) {
				tree = addRouteTreeConnection(tree, c);
				foundWire = true;
				break;
			}
		}
		assert (foundWire == true);
		
		return tree;
	}
	
	/*
	 * On the final node of a Vivado ROUTE string branch we may have to march along the wires until we hit
	 * a site pin. This is the case for I/O nets in particular. 
	 * 
	 * TODO: Test this feature with other designs...
	 */
	private RouteTree completeFinalBranchNode(RouteTree rt) {
		
		Collection<Connection> pinConnections = rt.getWire().getPinConnections(); 
		
		// march along our current connection until:
		// (1) We hit a pin connection (site pin routed) or
		// (2) We hit a multiple wire connection junction (site pin not routed)
		if (pinConnections.isEmpty()) {
			Collection<Connection> wireConnections = rt.getWire().getWireConnections();
			
			// TODO: remove this, only true for a fully routed design
			assert(wireConnections.size() == 1) : rt.getWire().getTile() + " " + rt.getWire().getWireName();
			
			while (wireConnections.size() == 1) {
				rt = rt.addConnection(wireConnections.iterator().next());
				
				// we reached a pin connection
				pinConnections = rt.getWire().getPinConnections();
				if (!pinConnections.isEmpty()) {
					// TODO: remove this once we have tested it
					System.out.println("HEY! : " + currentNet.getName());
					break;
				}
				
				wireConnections = rt.getWire().getWireConnections();
				
				// TODO: remove this, only true for a fully routed design
				assert(wireConnections.size() == 1) : rt.getWire().getTile() + " " + rt.getWire().getWireName();
			}
		}
				
		if (!pinConnections.isEmpty()) {
		
			// update the net with the routed cell pins
			SitePin sitePin = pinConnections.iterator().next().getSitePin();
			IntrasiteRoute internalRoute = sitePinToRouteMap.get(sitePin);
			
			if (internalRoute != null) { 
				internalRoute.setSinksAsRouted();
			}
			else { 
				// implicit intrasite net. An Example is a GND/VCC net going to the A6 pin of a LUT.
				// I does not actually show the bel pin as used, but it is being used.
				// TODO: see if there are any other examples of this besides A6 lut pin with VCC net
				assert(currentNet.isStaticNet()) : "Only static nets should not have site pin information" ;
				createStaticNetImplicitSinks(sitePin, currentNet);
			}
		}
		
		// Add the sink RouteTree to the list of terminals for pruning
		terminals.add(rt);
		return rt; 
	}
	
	private void createStaticNetImplicitSinks(SitePin sitePin, CellNet net) {
		
		IntrasiteRoute staticRoute = new IntrasiteRouteSitePinSource(sitePin, net, true);
		buildIntrasiteRoute(staticRoute, design.getUsedSitePipsAtSite(sitePin.getSite()));
		
		if (!staticRoute.isValid()){
			throw new AssertionError("Static net does not finish...");
		}
		staticRoute.applyRouting();
	}
	
	/*
	 * For routing import, we need to handle nets that use the dedicated clock logic differently
	 * Returns true if the source pin of the net is connected to a BUFG cell
	 */
	private boolean isBufgClkNet(CellNet net) {
		
		// TODO: may have to add other cells that can be placed on BUFGCTRL sites
		//		 as well for this to work correctly
		CellPin source = tryGetNetSource(net);
		return source.getCell().getLibCell().getName().equals("BUFG");
	}
	
	/**
	 * 
	 * @param parent
	 * @param c
	 * @return
	 */
	private RouteTree addRouteTreeConnection(RouteTree parent, Connection c) {
		
		Wire sinkWire = c.getSinkWire();
		RouteTree childTree = wiresInNet.get(sinkWire);
		
		// a route tree for this wire has not yet been created..
		if (childTree == null) { 
			childTree = parent.addConnection(c);
			wiresInNet.put(sinkWire, childTree);
		}
		
		return childTree;
	}
		
	private Tile getAdjacentTile(Tile tile, TileDirection direction) {
		switch(direction) 
		{
			case ABOVE:
				return device.getTile(tile.getRow() - 1, tile.getColumn());
			case BELOW:
				return device.getTile(tile.getRow() + 1, tile.getColumn());
			case LEFT:
				return device.getTile(tile.getRow(), tile.getColumn() - 1);
			case RIGHT:
				return device.getTile(tile.getRow(), tile.getColumn() + 1);
			default: 
				throw new AssertionError("Invalid Tile Direction"); 
		}
	}
	
	/**
	 * Searches through
	 * @param site
	 */
	private void createStaticSubsiteRouteTrees(Site site) {
				
		Iterator<BelPin> staticSourceIt = getPowerBelSourcesToSearch(site); 
		while (staticSourceIt.hasNext()) {
			
			BelPin pin = staticSourceIt.next();
			tryCreateStaticIntrasiteRoute(pin, design.getUsedSitePipsAtSite(site));
		}
	}
		
	/*
	 * Create an iterator for VCC and GND output bel pins in the site
	 */
	private Iterator<BelPin> getPowerBelSourcesToSearch(Site site) {
	
		Set<String> staticSourcesInSite =  staticSourceMap.get(site);
		
		if (staticSourcesInSite == null) {
			
			staticSourcesInSite = site.getBels().stream()
									.filter(bel -> bel.getName().contains("VCC") || bel.getName().contains("GND"))
									.map(bel -> bel.getName())
									.collect(Collectors.toSet());
		}
		
		return staticSourcesInSite.stream()
							.map(belName -> site.getBel(belName))
							.flatMap(bel -> bel.getSources().stream())
							.iterator();
	}
	
	/**
	 * Creates a route starting at the specified site pin object. <br>
	 * The search is guided by the specified used site pips of the site. <br>
	 * If no valid route is found, an exception is thrown becaues this function <br>.
	 * expects a route to be found 
	 * 
	 * @param pin Site Pin to start the route
	 * @param net Net attached to that site pin
	 * @param usedSiteWires Set of used pips within the site
	 */
	private void createIntrasiteRoute(SitePin pin, CellNet net, Set<Integer> usedSiteWires) {
	
		IntrasiteRoute route = new IntrasiteRouteSitePinSource(pin, net, false);
		buildIntrasiteRoute(route, usedSiteWires);
		
		if (!route.isValid()) {			
			throw new AssertionError("Valid intrasite route not found from site pin : " + pin);
		}
		
		route.applyRouting();
		sitePinToRouteMap.put(pin, route);
	}
	
	/**
	 * Creates a route starting at the specified bel pin object. <br>
	 * The search is guided by the specified used site pips of the site. <br>
	 * If no valid route is found, an exception is thrown because this function <br>
	 * expects a route to be found 
	 * 
	 * @param pin Bel Pin to start the route
	 * @param isContained True if all sinks of the net are in the same site as the source
	 * @param isStatic True if the source of the net is a VCC or GND bel
	 * @param usedSiteWires Set of used pips within the site
	 */
	private void createIntrasiteRoute(BelPin pin, boolean isContained, boolean isStatic, Set<Integer> usedSiteWires) {
	
		IntrasiteRoute route = new IntrasiteRouteBelPinSource(pin, isContained, isStatic);
		buildIntrasiteRoute(route, usedSiteWires);
		
		if (!route.isValid()) {
			throw new AssertionError("Valid intrasite route not found from bel pin : " + pin);
		}
		
		route.applyRouting();
		route.setSinksAsRouted();
	}
	
	/**
	 * Tries to create a route starting at a site GND or VCC bel. <br>
	 * Because no route is guaranteed to exist, routing is only applied <br>
	 * if a valid route is found. Otherwise, nothing happens.
	 * 
	 * @param pin BelPin to start the search
	 * @param usedSiteWires Used site pips in the site
	 * @return True if a route is found
	 */
	private boolean tryCreateStaticIntrasiteRoute(BelPin pin, Set<Integer> usedSiteWires) {
		
		IntrasiteRoute route = new IntrasiteRouteBelPinSource(pin, false, true);
		buildIntrasiteRoute(route, usedSiteWires);
		
		if (route.isValid()) {
			route.applyRouting();
			route.setSinksAsRouted();
			return true;
		}
		return false;
	}
	
	public void buildIntrasiteRoute(IntrasiteRoute intrasiteRoute, Set<Integer> usedSiteWires) {
		
		Set<Wire> visitedWires = new HashSet<Wire>(); // used to prevent cycles
		Queue<RouteTree> routeQueue = new LinkedList<RouteTree>();
		
		RouteTree startRoute = intrasiteRoute.getStartRoute();
		Wire startWire = startRoute.getWire();
		routeQueue.add(startRoute);
		visitedWires.add(startWire);
				
		while (!routeQueue.isEmpty()) {

			RouteTree currentRoute = routeQueue.poll();
			Wire currentWire = currentRoute.getWire();
			
			// reached a used bel pin that is not the source
			if (intrasiteRoute.isValidBelPinSink(currentWire) && !currentWire.equals(startWire)) {
				
				BelPin bp = currentWire.getTerminals().iterator().next().getBelPin();
				intrasiteRoute.addBelPinSink(bp, currentRoute);
			}
			// reached a site pin
			else if (connectsToSitePin(currentWire)) {
				SitePin sinkPin = currentWire.getPinConnections().iterator().next().getSitePin();	
				intrasiteRoute.addSitePinSink(sinkPin, currentRoute);
			}
			else {
				for (Connection conn : currentWire.getWireConnections()) {
				
					// skip wires we already visited
					if (visitedWires.contains(conn.getSinkWire())) {
						continue;
					}
					
					// only add valid search connections to the queue
					if (isQualifiedConnection(conn, currentWire, usedSiteWires)) {
						RouteTree next = currentRoute.addConnection(conn);
						routeQueue.add(next);
						visitedWires.add(next.getWire());
					}
				}
			}
		}
		
		// prune the route tree 
		intrasiteRoute.pruneRoute();
	}
	
	/*
	 * 
	 */
	private boolean connectsToSitePin(Wire currentWire) {
		return !currentWire.getPinConnections().isEmpty();
	}
	
	/*
	 * 
	 */
	private boolean isQualifiedConnection(Connection conn, Wire sourceWire, Set<Integer> usedSiteWires) {
		return !conn.isPip() || // the connection is a regular wire connection
				isUsedRoutethrough(conn, sourceWire) || // or, the connection is a used lut routethrough 
				usedSiteWires.contains(sourceWire.getWireEnum()); // or the connection is a used site pip
	}
	
	/**
	 * Returns true if the given connections is an used BEL routethrough. <br>
	 * A connection satisfies this condition if: <br>
	 * (1) The connection is a routethrough <br>
	 * (2) The BelPin is a key in the usedRoutethroughMap <br>
	 * (3) The sink wire of the connection matched the value in the usedRoutethroughMap <br>
	 * 
	 * @param conn Connection to test 
	 * @return True if the Connection is an available routethrough. False otherwise.
	 */
	private boolean isUsedRoutethrough(Connection conn, Wire sourceWire) {
		
		if (!conn.isRouteThrough()) {
			return false;
		}
		
		// a bel routethrough must also be connected to a BEL pin 
		assert (!sourceWire.getTerminals().isEmpty()) : "Wire: " + sourceWire + " should connect to BelPin!";
		BelPin source = sourceWire.getTerminals().iterator().next().getBelPin();
		
		Wire sinkWire = usedRoutethroughMap.get(source);
		return sinkWire != null && sinkWire.equals(conn.getSinkWire());
	}
	
	/*
	 * Returns true if a bel pin is being used in the design
	 */
	private boolean isBelPinUsed(BelPin pin) {
		return belPinToCellPinMap.containsKey(pin);
	}
	
	/**
	 * Tries to retrieve the Site object with the given site name <br>
	 * from the currently loaded device. If the site does not exist <br>
	 * a ParseException is thrown <br>
	 * 
	 * @param siteName Name of the site to retrieve
	 */
	private Site tryGetSite(String siteName) {
		
		Site site = device.getPrimitiveSite(siteName);
		
		if (site == null) {
			throw new ParseException("Site \"" + siteName + "\" not found in the current device. \n" 
									+ "On line " + this.currentLineNumber + " of " + currentFile);
		}
		
		return site;
	}
	
	/**
	 * Tries to retrieve the CellNet object with the given name <br>
	 * from the currently loaded design. If the net does not exist <br>
	 * a ParseException is thrown.
	 * 
	 * @param netName Name of net to retrieve
	 * @return CellNet
	 */
	private CellNet tryGetCellNet(String netName) {
		
		CellNet net = null; 
		if (netName.equals("VCC")) {
			net = design.getVccNet();
		}
		else if (netName.equals("GND")) {
			net = design.getGndNet();
		}
		else {
			net = design.getNet(netName);
		}
		
		if (net == null) {
			throw new ParseException("Net \"" + netName + "\" does not exist in the current design. Validate Edif is correct.\n" 
									+ "On line " + this.currentLineNumber + " of " + currentFile);
		}
				
		return net;
	}
	
	/**
	 * Tries to retrieve the SitePin object specified by the site <br>
	 * and pin name. If the pin does not exist, a ParseException is thrown. <br>
	 *  
	 * @param site Site object
	 * @param pinName Name of the pin on the site
	 * @return SitePin
	 */
	private SitePin tryGetSitePin(Site site, String pinName) {
		
		SitePin pin = site.getSitePin(pinName);
		
		if (pin == null) {
			throw new ParseException(String.format("SitePin: \"%s/%s\" does not exist in the current device\n"
												   + "On line %d of %s", site.getName(), pinName, currentLineNumber, currentFile));
		}
		
		return pin;
	}
	
	/**
	 * Tries to retrieve the source of a CellNet. If the net does not have a source, <br>
	 * a ParseException is thrown because all nets with routing information are expected <br>
	 * to have a source cell pin.
	 * 
	 * @param net CellNet to get the source of
	 * @return CellPin
	 */
	private CellPin tryGetNetSource(CellNet net) {
		
		CellPin source = net.getSourcePin();
		
		if (source == null) {
			throw new ParseException("Net \"" + net.getName() +"\" does not have a source pin!\n" 
									+ "On line " + this.currentLineNumber + " of " + currentFile);
		}
		
		return source;
	}
	
	/**
	 * Tries to retrieve a BEL object from the currently loaded device. <br>
	 * If the BEL does not exist, a ParseException is thrown. <br>
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
	 * Tries to retrieve a BelPin object from the currently loaded device <br>
	 * If the pin does not exist, a ParseException is thrown. <br>
	 * 
	 * @param bel Bel which the pin is attached
	 * @param pinName Name of the bel pin
	 * @return BelPin
	 */
	private BelPin tryGetBelPin(Bel bel, String pinName) {
		
		BelPin pin = bel.getBelPin(pinName);
		
		if (pin == null) {
			throw new ParseException(String.format("BelPin: \"%s/%s\" does not exist in the current device"
												 + "On line %d of %s", bel.getName(), pinName, currentLineNumber, currentFile));
		}
		
		return pin;
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
			throw new ParseException(String.format("Wire: \"%s\" does not exist in the current device"
												 + "On line %d of %s", wireName, currentLineNumber, currentFile));
		}
		
		return wireEnum;
	}
	
	/**
	 * Creates a routing.xdc file from the nets of the given design. <br>
	 * This file can be imported into Vivado to constrain the physical location of nets. 
	 * 
	 * @param xdcOut Location to write the routing.xdc file
	 * @param design Design with nets to export
	 * @throws IOException
	 */
	public void writeRoutingXDC(String xdcOut, CellDesign design) throws IOException {
		
		BufferedWriter fileout = new BufferedWriter (new FileWriter(xdcOut));
		
		//write the routing information to the TCL script
		for(CellNet net : design.getNets()) {

			// only print nets that have routing information. Grab the first RouteTree of the net and use this as the final route
			if ( net.getIntersiteRouteTree() != null ) {
				fileout.write(String.format("set_property ROUTE %s [get_nets {%s}]\n", getVivadoRouteString(net), net.getName()));
			}
		}
		
		fileout.close();
	}
	
	/**
	 * Creates the Vivado equivalent route string of the specified net. <br>
	 * If the net is a generic net (i.e. not VCC or GND), the first RouteTree <br>
	 * in the net's list of RouteTrees is assumed to be the route to print. <br>
	 * For GND and VCC nets, all RouteTrees in the net's list of RouteTrees <br>
	 * are printed. This function can be used to incrementally update the <br>
	 * ROUTE property of a net in Vivado.
	 *  
	 * @param net CellNet to create a Vivado ROUTE string for
	 * @return Vivado ROUTE string
	 */
	public static String getVivadoRouteString(CellNet net) {
		
		if (net.getType().equals(NetType.WIRE)) {
			// assuming the first RouteTree is the actual route
			RouteTree route = net.getIntersiteRouteTree();// .getRouteTrees().iterator().next();
			return createVivadoRoutingString(route.getFirstSource());
		}
		
		// otherwise we assume its a VCC or GND net, which has a special Route string
		String routeString = "{ ";
		for (RouteTree rt : net.getIntersiteRouteTreeList()) {
			routeString += "( " + createVivadoRoutingString(rt.getFirstSource()) + ") ";
		}
		return routeString + " }"; 		
	}
	
	/*
	 * Creates and formats the route tree into a string that Vivado understands and can be applied to a Vivado net
	 * TODO: refactor...this code is confusing to read
	 */
	private static String createVivadoRoutingString (RouteTree rt) {
		
		RouteTree currentRoute = rt; 
		String routeString = "{ ";
			
		while ( true ) {
			Tile t = currentRoute.getWire().getTile();
			routeString = routeString.concat(t.getName() + "/" + currentRoute.getWire().getWireName() + " ");
						
			ArrayList<RouteTree> children = (ArrayList<RouteTree>) currentRoute.getSinkTrees();
			
			if (children.size() == 0)
				break;
			
			ArrayList<RouteTree> trueChildren = new ArrayList<RouteTree>();
			for(RouteTree child: children) {
				Connection c = child.getConnection();
				if (c.isPip() || c.isRouteThrough()) {
					trueChildren.add(child);
				}
				else { // if its a regular wire connection and we don't want to add this to the route tree					
					trueChildren.addAll(child.getSinkTrees());
				}
			}
			
			if (trueChildren.size() == 0)
				break;
			
			for(int i = 0; i < trueChildren.size() - 1; i++) 
				routeString = routeString.concat(createVivadoRoutingString(trueChildren.get(i)));
			
			currentRoute = trueChildren.get(trueChildren.size() - 1) ; 
		}
		
		return routeString + "} ";
	}
	
	/* **************
	 * 	Nested Types
	 * **************/
	
	/**
	 * Enumeration to represent relative tile directions
	 * TODO: May be good to rename above and below to something else
	 * 
	 * @author Thomas Townsend
	 */
	private enum TileDirection
	{
		ABOVE,
		BELOW, 
		LEFT,
		RIGHT
	}
	
	/**
	 * Interface used to create intrasite route sourced from either a bel pin or site pin
	 * 
	 * @author Thomas Townsend
	 */
	public interface IntrasiteRoute {
		
		public boolean addBelPinSink(BelPin belPin, RouteTree terminal);
		public boolean addSitePinSink(SitePin sitePin, RouteTree terminal);
		public void pruneRoute();
		public void applyRouting();
		public boolean isValid();
		public RouteTree getStartRoute();
		public void setSinksAsRouted();
		public boolean isValidBelPinSink(Wire currentWire);
	}
	
	public final class IntrasiteRouteSitePinSource implements IntrasiteRoute {
		
		private final SitePin source;
		private final CellNet net; 
		private final Set<BelPin> belPinSinks;
		private final Set<RouteTree> terminals;
		private final RouteTree route;
		private final boolean allowUnusedBelPins; 
		
		public IntrasiteRouteSitePinSource (SitePin source, CellNet net, boolean allowUnusedBelPinSinks) {
			this.source = source;
			this.net = net;
			this.belPinSinks = new HashSet<BelPin>();
			this.terminals = new HashSet<RouteTree>();
			this.route = new RouteTree(source.getInternalWire());
			this.allowUnusedBelPins = allowUnusedBelPinSinks;
		}
				
		@Override
		public RouteTree getStartRoute() {
			return route;
		}
	
		@Override
		public boolean addBelPinSink(BelPin belPin, RouteTree terminal) {
			
			CellPin sinkCellPin = belPinToCellPinMap.get(belPin);
			
			if (allowUnusedBelPins) {
				if (sinkCellPin != null) {
					throw new AssertionError("Only unused bel pin sinks expected");
				}
			}
			else {
				if (sinkCellPin == null || !sinkCellPin.getNet().equals(this.net)) {
					return false;
				}
			}
						
			terminals.add(terminal);
			belPinSinks.add(belPin);
			return true;
		}

		@Override
		public boolean addSitePinSink(SitePin sitePin, RouteTree terminal) {
			throw new AssertionError("Intrasite Route starting at input site pin should not reach output site pin " + source);
		}

		@Override
		public void pruneRoute() {
			route.prune(terminals);
		}

		@Override
		public void applyRouting() {

			net.addSinkRouteTree(source, route);
			
			for (BelPin pin : this.belPinSinks) {
				net.addSinkRouteTree(pin, route);	
			}
		}

		@Override
		public boolean isValid() {
			return belPinSinks.size() > 0;
		}
		
		@Override
		public void setSinksAsRouted() {
			// TODO: only do this for used bel pins
			belPinSinks.stream()
						.map(belPin -> belPinToCellPinMap.get(belPin))
						.forEach(cellPin -> net.addRoutedSink(cellPin));
		}

		@Override
		public boolean isValidBelPinSink(Wire currentWire) {
			
			Collection<Connection> terminals = currentWire.getTerminals(); 
						
			// BEL pin sink is valid if the wire connects to
			// a bel pin and either:
			// (1) The net is a static net (GND or VCC)
			// (2) The BelPin is being used (i.e. a cell pin has been mapped to it)
			return !terminals.isEmpty() &&
					(allowUnusedBelPins || isBelPinUsed(terminals.iterator().next().getBelPin()));
		}
	}
	
	public final class IntrasiteRouteBelPinSource implements IntrasiteRoute {
		
		private final BelPin source;
		private final Set<BelPin> belPinSinks;
		private final Set<SitePin> sitePinSinks;
		private final RouteTree route;
		private final Set<RouteTree> terminals;
		private final boolean isContained;
		private final boolean isStatic;
		
		public IntrasiteRouteBelPinSource (BelPin source, boolean isContained, boolean isStatic) {
			this.source = source;
			this.isContained = isContained;
			this.isStatic = isStatic;
			this.belPinSinks = new HashSet<BelPin>();
			this.sitePinSinks = new HashSet<SitePin>();
			this.terminals = new HashSet<RouteTree>();
			this.route = new RouteTree(source.getWire());
		}

		@Override
		public boolean addBelPinSink(BelPin belPin, RouteTree terminal) {

			this.belPinSinks.add(belPin);
			this.terminals.add(terminal);
			
			return true;
		}

		@Override
		public boolean addSitePinSink(SitePin sitePin, RouteTree terminal) {
			
			if (isContained || isStatic) {
				throw new AssertionError("Contained instrasite route should not reach site pin");
			}
			
			sitePinSinks.add(sitePin);
			terminals.add(terminal);
			return true;
		}

		@Override
		public void pruneRoute() {
			route.prune(terminals);
		}

		@Override
		public void applyRouting() {
					
			// statically sourced nets (VCC and GND) don't have a net attached to the bel pin
			if (!isStatic) {
				
				CellNet net = belPinToCellPinMap.get(source).getNet();
				net.setSourceRouteTree(route);
				
				for (SitePin sp : sitePinSinks) {
					net.addSinkRouteTree(sp, route);
				}
			}
			
			for (BelPin bp: belPinSinks) {
				CellNet net = belPinToCellPinMap.get(bp).getNet();
				net.addSinkRouteTree(bp, route);
			}
		}

		@Override
		public boolean isValid() {
			
			// completely intrasite routes should not hit a site pin
			if (isContained || isStatic) {
				return belPinSinks.size() > 0 && sitePinSinks.size() == 0;
			}
			
			return belPinSinks.size() > 0 || sitePinSinks.size() > 0;
		}

		@Override
		public RouteTree getStartRoute() {
			return route;
		}
		
		@Override
		public void setSinksAsRouted() {

			// by definition, if the source of a intrasite is a bel pin,
			// then all bel pin sinks are routed
			for (BelPin belPin : belPinSinks) {
				CellPin cellPin = belPinToCellPinMap.get(belPin);
				cellPin.getNet().addRoutedSink(cellPin);
			}
		}

		@Override
		public boolean isValidBelPinSink(Wire currentWire) {
			Collection<Connection> terminals = currentWire.getTerminals(); 
			
			return !terminals.isEmpty() &&
					isBelPinUsed(terminals.iterator().next().getBelPin());
		}
	}
}
