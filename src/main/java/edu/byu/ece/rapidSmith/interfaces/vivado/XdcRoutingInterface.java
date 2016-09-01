package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.Connection;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
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

	private static Device device;
	private static WireEnumerator wireEnumerator;
	private static HashMap<Wire, RouteTree> wiresInNet = new HashMap<Wire, RouteTree>();
	private static HashSet<RouteTree> terminals = new HashSet<RouteTree>();
	private static List<RouteTree> vccRouteTrees = null;
	private static List<RouteTree> gndRouteTrees = null;
	
	/**
	 * Parses the specified routing.xdc file, and applies the physical wire information to the nets of the design
	 * 
	 * @param xdcFile routing.xdc file
	 * @param design Design to apply routing
	 * @param deviceA Device which the design is implemented on
	 * @throws IOException
	 */
	public static void parseRoutingXDC(String xdcFile, CellDesign design, Device deviceA) throws IOException {
		
		device = deviceA;
		wireEnumerator = device.getWireEnumerator();
		
		LineNumberReader br = new LineNumberReader(new BufferedReader(new FileReader(xdcFile)));
		String line = null;

		while ((line = br.readLine()) != null) {
			String[] toks = line.split("\\s+");
			
			//handle intrasite routing and site pip configuration
			if (toks[0].equals("SITE_PIPS")) {			
				Site ps = device.getPrimitiveSite(toks[1]);
				
				if (ps == null) {
					throw new ParseException("Primitive site: " + toks[1] + " does not exist in the current device!");
				}
								
				design.setUsedSitePipsAtSite(ps, readUsedSitePips(ps, toks, br, xdcFile));
			}
			// handle routes between sites
			else if (toks[0].equals("ROUTE")) {
				String netname = toks[1]; 
				CellNet net = design.getNet(netname); //net to add the route tree to
				
				// make sure the net is in the design
				if (net == null) {
					throw new ParseException(String.format("Net: %s does not exist in the current design. Validate Edif is correct.", netname));
				}
								
				// Vivado nets always have to have a source pin to be valid
				if (net.getSourcePin() == null) {
					throw new ParseException(String.format("Net: %s is not sourced. It should not have routing information", netname));
				}
				
				if (net.isVCCNet()) {
					if (vccRouteTrees != null) {
						net.setRouteTrees(vccRouteTrees);
						continue;
					}
					vccRouteTrees = createRouteTreeListForPowerNet(net, toks);
				}
				else if (net.isGNDNet()) {
					if (gndRouteTrees != null) {
						net.setRouteTrees(gndRouteTrees);
						continue;
					}
					gndRouteTrees = createRouteTreeListForPowerNet(net, toks);
				}
				else { // otherwise, its a general wire
					
					RouteTree start = initializeRouteTree(net, toks[3]);
					createRouteTreeForNet(start, 4, toks, isBufgClkNet(net));
					net.addRouteTree(start.getFirstSource());
				}
			}
		}
		
		br.close();
	}
	
	private static List<RouteTree> createRouteTreeListForPowerNet(CellNet net, String[] toks) {
		
		List<RouteTree> routeTrees = new ArrayList<RouteTree>();
		
		int index = 4; 	
		while(index < toks.length) {
			
			RouteTree start = initializeGlobalLogicRouteTree(toks[index++]);
			index = createRouteTreeForNet(start, index, toks, false);
			routeTrees.add(start);
			net.addRouteTree(start);
			index += 3; // get to the start of the next route string
		}
		
		return routeTrees;
	}
		
	/*
	 * Read the used site pips for the given primitive site, and store the information in a HashSet
	 */
	private static HashSet<Integer> readUsedSitePips(Site ps, String[] toks, LineNumberReader br, String fname) {
		
		HashSet<Integer> usedSitePips = new HashSet<Integer>();
		
		String namePrefix = "intrasite:" + ps.getType() + "/";
		
		//list of site pip wires that are used...
		for(int i = 2; i <toks.length; i++) {
			String pipWireName = (namePrefix + toks[i].replace(":", "."));
			Integer wireEnum = wireEnumerator.getWireEnum(pipWireName);
			
			if (wireEnum == null) {
				throw new ParseException(String.format("Unknown PIP wire \"%s\" in current device. Line number: %d", pipWireName, br.getLineNumber()));
			}
			//add the input and output pip wires (there are two of these in RS2)
			usedSitePips.add(wireEnum); 	
			usedSitePips.add(wireEnumerator.getWireEnum(pipWireName.split("\\.")[0] + ".OUT"));
		}
		return usedSitePips;
	}
		
	/*
	 * Creates the initial RouteTree from a ROUTE string
	 */
	private static RouteTree initializeRouteTree(CellNet net, String firstWireName) {
		
		Tile sourceTile = net.getSourcePin().getCell().getAnchorSite().getTile(); 
		Wire firstWire = new TileWire(sourceTile, wireEnumerator.getWireEnum(firstWireName));
		return new RouteTree(firstWire);
	}
	
	/*
	 * Creates the initial RouteTree for a GND or VCC ROUTE string
	 */
	private static RouteTree initializeGlobalLogicRouteTree(String firstWireName) {
		
		String[] toks = firstWireName.split("/");
		assert(toks.length == 2); 
		
		Tile tile = device.getTile(toks[0]);
		Wire firstWire = new TileWire(tile, wireEnumerator.getWireEnum(toks[1]));
		
		return new RouteTree(firstWire);
	}
	
	private static int createRouteTreeForNet(RouteTree current, int index, String[] toks, boolean isClkNet) {
		
		wiresInNet.clear();
		terminals.clear();
		int i = createIntersiteRouteTree(current, index, toks, isClkNet);
		current.getFirstSource().prune(terminals);
		return i;
	}
	
	/*
	 * Converts a Vivado ROUTE string to a RS2 RouteTree data structure
	 */
	private static int createIntersiteRouteTree(RouteTree current, int index, String[] toks, boolean isClkNet) {
				
		while ( index < toks.length ) {
			//we hit a branch in the route...TODO: I don't think we need to check for pre-branch wires anymore
			if (toks[index].equals("{") ) {
								
				index++;
				index = createIntersiteRouteTree(current, index, toks, isClkNet);
			}
			//end of a branch
			else if (toks[index].equals("}") ) {
				
				RouteTree terminal = completeFinalBranchNode(current);
				terminals.add(terminal);
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
	
	private static RouteTree findNextNode(RouteTree rt, String nodeName) {
		
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
	private static RouteTree findNextNode(RouteTree rt, String nodeName, HashSet<Wire> wireSet) {

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
					// There should always be a new route tree created here...if not then may need to debug
					return addRouteTreeConnection(nextRoute, c);
				}
				
				wireSet.add(sinkWire);
				routeQueue.add(addRouteTreeConnection(nextRoute, c));
			}
		}
		
		// for debugging
		System.out.println(nodeName + " " + rt.getWire().getTile() + "/" + rt.getWire().getWireName());
		throw new AssertionError("Unable to find the next node after looking at 10,000 wires!");
	}
	
	private static boolean isSinkLongLineBounce(RouteTree source, Wire sinkWire) {
		
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
		
	private static boolean isLongLineWire(Wire w) {
		
		String wireName = w.getWireName();
		return wireName.startsWith("LV") || wireName.startsWith("LH"); 
	}
	
	private static RouteTree findNextClockNode(RouteTree rt, String nodeName) {
		
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
	
	private static boolean isDedicatedClockWire(String nodeName) {
		
		return nodeName.startsWith("<") || nodeName.startsWith("HCLK") || nodeName.startsWith("GCLK");
	}
	
	// This seems a little hackish, and is not guaranteed to work for ultrascale...
	// TODO: think of a better way to do this			
	private static TileDirection getClockWireDirection(String prevNodeName, String clockWireName) {
		if (clockWireName.charAt(0) == 'H') {
			String[]toks = prevNodeName.split("_");
			return toks[toks.length-1].contains("L") ? TileDirection.LEFT : TileDirection.RIGHT;					
		}
		else {
			return prevNodeName.contains("BOT") ? TileDirection.BELOW : TileDirection.ABOVE;
		}
	}
		
	private static RouteTree getNextClockWire(RouteTree tree, Tile tile, String wirename, int offset, TileDirection searchDir)
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
	 * For routing import, we need to handle nets that use the dedicated clock logic differently
	 * Returns true if the source pin of the net is connected to a BUFG cell
	 */
	private static boolean isBufgClkNet(CellNet net) {
		
		return net.getSourcePin().getCell().getLibCell().getName().equals("BUFG");
	}
	
	private static RouteTree addRouteTreeConnection(RouteTree parent, Connection c) {
		
		Wire sinkWire = c.getSinkWire();
		RouteTree childTree = wiresInNet.get(sinkWire);
		
		// a route tree for this wire has not yet been created..
		if (childTree == null) { 
			childTree = parent.addConnection(c);
			wiresInNet.put(sinkWire, childTree);
		}
		
		return childTree;
	}
	
	private static Tile getAdjacentTile(Tile tile, TileDirection direction) {
		return getAdjacentTile(tile, direction, 1);
	}
	
	private static Tile getAdjacentTile(Tile tile, TileDirection direction, int offset) {
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
	
	private enum TileDirection
	{
		ABOVE,
		BELOW, 
		LEFT,
		RIGHT
	}
			
	/*
	 * On the final node of a Vivado ROUTE string branch we may have to march along the wires until we hit
	 * a site pin. This is the case for I/O nets in particular. 
	 * 
	 * TODO: Look at this code and see what is going on
	 */
	private static RouteTree completeFinalBranchNode(RouteTree rt) {
		
		/* Alternative way to perform this functionality...may be slightly more robust 
		// if we have to traverse more than one wire to reach our destination 
		Collection<Connection> pinConnections = rt.getWire().getPinConnections(); 
		
		while (pinConnections.size() == 0) {
			Collection<Connection> wireConnections = rt.getWire().getWireConnections();
			
			assert(wireConnections.size() == 1); 
			
			rt.addConnection(wireConnections.stream().collect(Collectors.toList()).get(0));
			
			pinConnections = rt.getWire().getPinConnections();
		}
		*/
		
		//assuming that our final wire will be at most one wire away from 
		//from the wire that we end on...not sure if this assumption is correct.
		Collection<Connection> tmp = rt.getWire().getWireConnections();
		// assert(tmp.size() == 1); 
		if (tmp.size() == 1) {
			Connection c = tmp.iterator().next();
			if (!c.isPip()) { 
				rt = rt.addConnection(c);
			}
		}
		
		return rt; 
	}
		
	/**
	 * Creates a routing.xdc file from the nets of the given design. <br>
	 * This file can be imported into Vivado to constrain the physical location of nets. 
	 * 
	 * @param xdcOut Location to write the routing.xdc file
	 * @param design Design with nets to export
	 * @throws IOException
	 */
	public static void writeRoutingXDC(String xdcOut, CellDesign design) throws IOException {
		
		BufferedWriter fileout = new BufferedWriter (new FileWriter(xdcOut));
		
		//write the routing information to the TCL script
		for(CellNet net : design.getNets()) {
			
			// only print nets that have routing information
			if (net.isRouted()) {
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
			RouteTree route = net.getRouteTrees().iterator().next();
			return createVivadoRoutingString(route.getFirstSource());
		}
		
		// otherwise we assume its a VCC or GND net, which has a special Route string
		String routeString = "{ ";
		for (RouteTree rt : net.getRouteTrees()) {
			routeString += "( " + createVivadoRoutingString(rt.getFirstSource()) + ") ";
		}
		return routeString + " }"; 		
	}
	
	/*
	 * Creates and formats the route tree into a string that Vivado understands and can be applied to a Vivado net
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
}
