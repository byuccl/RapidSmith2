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
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * This class is used for parsing and writing routing XDC files in a TINCR checkpoint. <br>
 * Routing.xdc files are used to specify the physical wires that a net in Vivado uses.
 * 
 * @author Thomas Townsend
 *
 */
public class XdcRoutingInterface {

	private static Device device;
	private static WireEnumerator wireEnumerator;
	private static HashMap<Wire, RouteTree> wiresInNet = new HashMap<Wire, RouteTree>();
	private static HashSet<RouteTree> terminals = new HashSet<RouteTree>();
	private static Wire clockSinkWireToSkip = null; 
		
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
					MessageGenerator.briefError("[Warning] Unable to find Primitive Site \"" + toks[1] + "\" in current device. Cannot create intrasite routing structure.\n"
							+ "        Line " + br.getLineNumber() + " of " + xdcFile);
					continue;
				}
								
				design.setUsedSitePipsAtSite(ps, readUsedSitePips(ps, toks, br, xdcFile));
			}
			// handle routes between sites
			else if (toks[0].equals("ROUTE")) {
				String netname = toks[1]; 
				CellNet net = design.getNet(netname); //net to add the route tree to
				
				// make sure the net is in the design
				if (net == null) {
					MessageGenerator.briefErrorAndExit("[ERROR] Unable to find net \"" + netname + "\" in cell design\n"
							+ "\tLine " + br.getLineNumber() + " of " + xdcFile);
				}
				
				// Vivado nets always have to have a source pin to be valid...skip nets with no source
				// Should I throw an error here?
				if(net.getSourcePin() == null) {
					MessageGenerator.briefError("[Warning] Net " + netname + " is not sourced. Cannot apply routing information.\n"
							+ "\tLine " + br.getLineNumber() + " of " + xdcFile);
					continue;
				}
				
				// create a RouteTree object containing the wires in the net
				if(net.getType().equals(NetType.WIRE)) {
					RouteTree start = initializeRouteTree(net, toks[3]);
					createRouteTreeForNet(start, 4, toks, net.isClkNet());
					net.addRouteTree(start.getFirstSource());
				}					
				else { // if its not a wire, then its a GND or VCC net, which can have multiple RouteTrees and sources
					
					// TODO: check if a VCC net has already been parsed, if it has, then there is no need to re-parse it. 
					int index = 4; 
					
					while(index < toks.length) {
						
						RouteTree start = initializeGlobalLogicRouteTree(toks[index++]);
						index = createRouteTreeForNet(start, index, toks, false);
						net.addRouteTree(start.getFirstSource());
						index += 3; // get to the start of the next route string
					}	
				}
			}
		}
		
		br.close();
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
			
			if(wireEnum == null) {
				MessageGenerator.briefError("[Warning] Unknown PIP wire \"" + pipWireName + "\" in current device.\n"
						+ "        Line " + br.getLineNumber() + " of " + fname);
				continue;
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
				RouteTree test = checkForPreBranchWires(current, toks[index]);
				
				boolean shouldSourceParent = false;
				if (!current.equals(test)) {
					current = test;
					shouldSourceParent = true;
				}
					
				index = createIntersiteRouteTree(current, index, toks, isClkNet);
				
				if(shouldSourceParent) {
					current = current.getSourceTree();
				}
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
	 */
	private static RouteTree findNextNode(RouteTree rt, String nodeName, HashSet<Wire> wireSet) {

		Queue<RouteTree> routeQueue = new LinkedList<RouteTree>();
								
		routeQueue.add(rt);
		wireSet.add(rt.getWire());
		int debugCount = 0;
		
		while (!routeQueue.isEmpty()) {
			
			// just in case we get lost in our search...
			if (debugCount++ > 10000) { 
				break;
			}
			
			RouteTree nextRoute = routeQueue.poll();
						
			for (Connection c : nextRoute.getWire().getWireConnections()) {
			
				Wire sinkWire = c.getSinkWire();
				
				if (sinkWire.getWireName().equals(nodeName) ) {
					// There should always be a new route tree created here...if not then may need to debug
					RouteTree test = addRouteTreeConnection(nextRoute, c);
					return test;
				}
				
				if (!wireSet.contains(sinkWire)) {	
					wireSet.add(sinkWire);
					routeQueue.add(addRouteTreeConnection(nextRoute, c));
				}
			}
		}
		
		throw new AssertionError("Unable to find the next node after looking at 10,000 wires!");
	}
		
	private static RouteTree findNextClockNode(RouteTree rt, String nodeName) {
		
		if (nodeName.startsWith("<")) { // format <12>HCLK_NodeName
			
			int endIndex = nodeName.indexOf('>');
			int offset = Integer.parseInt(nodeName.substring(1, endIndex));
			String clkWireName = nodeName.substring(endIndex+1);
			TileDirection dir = clkWireName.charAt(0) == 'H' ? TileDirection.LEFT : TileDirection.BELOW;

			return GetNextClockWire(rt, rt.getWire().getTile(), clkWireName, offset, dir);
		}
		
		// special case for clocks in series 7 devices...I don't think there is another way to do this
		// Right after we exit the bufg, we need to know which direction to go, up or down...
		// since the relative wire names in both directions are identical, we have to use this special 
		// wire to determine which direction to choose
		if (nodeName.startsWith("CLK_BUFG_REBUF")) {
			
			String suffix = nodeName.endsWith("TOP") ? "BOT" : "TOP";
			
			assert(rt.getWire().getWireConnections().size() == 2);
			boolean foundWire = false;
			for (Connection c : rt.getWire().getWireConnections()) {
				
				Wire sinkWire = c.getSinkWire();
				String wireName = sinkWire.getWireName();
				
				if (wireName.endsWith(suffix)) {
					rt = addRouteTreeConnection(rt, c);
					foundWire = true;
				}
				else {
					clockSinkWireToSkip = sinkWire;
				}
			}
			
			assert(foundWire == true);
			return rt;
		}
		
		if (rt.getWire().getWireName().startsWith("CLK_BUFG_REBUF")) {
			assert (clockSinkWireToSkip != null);
			
			// help guide the search in the right direction
			HashSet<Wire> wireSet = new HashSet<Wire>();
			wireSet.add(clockSinkWireToSkip);
			clockSinkWireToSkip = null; // for debugging
			return findNextNode(rt, nodeName, wireSet); 
		}
		
		return findNextNode(rt, nodeName); 
	}
	
	private static RouteTree addRouteTreeConnection(RouteTree parent, Connection c) {
		
		Wire sinkWire = c.getSinkWire();
		RouteTree childTree = wiresInNet.get(sinkWire);
		
		if (childTree == null) { // a route tree for this wire has not yet been created..
			childTree = parent.addConnection(c);
			wiresInNet.put(sinkWire, childTree);
		}
		
		return childTree;
	}
	
	private static RouteTree GetNextClockWire(RouteTree tree, Tile tile, String wirename, int offset, TileDirection searchDir)
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
		
		// TODO: need to do the following to be true to the rapidSmith representation of wires
		// 		 ask Travis if there is a more efficient way of doing this...
			
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
	 * Checks for wire-end segments that are not represented in the Vivado ROUTE string,
	 * but are represented in RapidSmith, so we need to add them.
	 * 
	 * TODO: Update this function to be cleaner
	 */
	private static RouteTree checkForPreBranchWires(RouteTree current, String branchWireName) {
		
		Collection<Connection> connTmp =  current.getWire().getWireConnections();
		Connection[] connections = new Connection[connTmp.size()];
		connTmp.toArray(connections);
		
		//assuming there is always at least one connection, and that a wire can't have both PIP and wire connections
		Connection tmp = connections[0];
		
		if(!tmp.isPip()) {
			if (connections.length == 1) {
				current = addRouteTreeConnection(current, tmp); // current.addConnection(tmp);
			}
			else { //search for the first wire in the branch, and add the missing wire connections if necessary 
				outerLoop : for(Connection c : connections) {
					for(Connection c2: c.getSinkWire().getWireConnections()){
						if(c2.getSinkWire().getWireName().equals(branchWireName)) {
						
							current = addRouteTreeConnection(current, c);
							break outerLoop;
						}
					}
				}
			}
		}
		return current;
	}
		
	/*
	 * On the final node of a Vivado ROUTE string branch we may have to march along the wires until we hit
	 * a site pin. This is the case for I/O nets in particular. 
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
		
		//write the routing information to the TCL script...assumes one final route tree has been created
		for(CellNet net : design.getNets()) {
			
			//only print to the XDC file if a net has a route attached to it. 
			Collection<RouteTree> routes = net.getRouteTrees();
			if (routes.size() > 0) {
				if (net.getType().equals(NetType.WIRE)) {
					//a signal net should only have one route tree object associated with it...up to the user to ensure this
					RouteTree route = routes.iterator().next(); 
					fileout.write("set_property ROUTE " + createVivadoRoutingString(route) + " [get_nets " + "{" + net.getName() + "}]\n" );
				}
				else { //net is a GND or VCC net
					String routeString = "";
					for(RouteTree rt: routes) {
						routeString += "( " + createVivadoRoutingString(rt.getFirstSource()) + ") ";
					}
					fileout.write("set_property ROUTE " + routeString + " [get_nets " + "{" + net.getName() + "}]\n" );
				}
			}
		}
		
		fileout.close();
	}
	
	/**
	 * Creates the Vivado equivalent route string for the RouteTree object of the specified net.
	 * Can be used to incrementally update the ROUTE property of a net in Vivado. 
	 * @param net CellNet to create a Vivado ROUTE string for
	 * @return
	 */
	public static String getVivadoRouteString(CellNet net) {
		//only print to the XDC file if a net has a route attached to it. 
		Collection<RouteTree> routes = net.getRouteTrees();
		String routeString = null;
		if (routes.size() > 0) {
			if (net.getType().equals(NetType.WIRE)) {
				// a signal net should only have one route tree object associated with it...up to the user to ensure this
				RouteTree route = routes.iterator().next(); 
				routeString = createVivadoRoutingString(route.getFirstSource());
			}
			else { // net is a GND or VCC net
				routeString = "{ ";
				for(RouteTree rt: routes)
					routeString += "( " + createVivadoRoutingString(rt.getFirstSource()) + ") ";
				routeString += " }";
			}
		}
		return routeString;
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
