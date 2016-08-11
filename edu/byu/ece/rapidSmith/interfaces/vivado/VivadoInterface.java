package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import edu.byu.ece.edif.core.EdifNameConflictException;
import edu.byu.ece.edif.core.InvalidEdifNameException;
import edu.byu.ece.edif.util.parse.ParseException;
import edu.byu.ece.rapidSmith.RapidSmithEnv;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.Connection;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.TileWire;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.WireConnection;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * This class is used to interface Vivado and RapidSmith. <br>
 * It parses TINCR checkpoints and creates equivalent RapidSmith designs. <br>
 * It can also create TINCR checkpoints from existing RapidSmith designs.
 * 
 * TODO: Change the name of the class?
 * @author Thomas Townsend
 *
 */
public final class VivadoInterface {

	private static final String CELL_LIBRARY_NAME = "cellLibrary.xml";

	private static String partname;
	private static CellLibrary libCells;
	private static Device device;
	
	// list which holds the cells in the order they were read into Vivado
	// TODO: create a sorting algorithm to sort the cells of a design for Vivado import order
	private static ArrayList<Cell> cellsInOrder = new ArrayList<Cell>();
	
	/**
	 * Parses a TINCR checkpoint, and creates an equivalent RapidSmith 2 design.
	 * 
	 * @param tcp Path to the TINCR checkpoint to import
	 * @throws InvalidEdifNameException 
	 * @throws EdifNameConflictException 
	 */
	public static CellDesign loadTCP (String tcp) throws IOException, ParseException {
	
		if (tcp.endsWith("/") || tcp.endsWith("\\")) {
			tcp = tcp.substring(0, tcp.length()-1);
		}
		
		// check to make sure the specified directory is a TINCR checkpoint
		if (!tcp.endsWith(".tcp")) {
			MessageGenerator.briefErrorAndExit("[ERROR] Specified directory is not a TINCR checkpoint. "
											+ "Expecting a directory name ending in .tcp");
		}
		
		// TODO: remove this
		cellsInOrder.clear();
		
		// setup the cell library and the device based on the part in the TCP file
		partname = parseInfoFile(tcp);
		initializeDevice(partname);
		
		// create the RS2 netlist 
		String edifFile = Paths.get(tcp, "netlist.edf").toString();
		CellDesign design = EdifInterface.parseEdif(edifFile, libCells);
			
		// re-create the placement and routing information
		String placementXdc = Paths.get(tcp, "placement.xdc").toString();
		applyPlacement(design, placementXdc);

		String routingXdc = Paths.get(tcp, "routing.xdc").toString();
		applyRouting(design, routingXdc);
			
		return design;
	}
		
	/*
	 * Load the cell library and device files
	 */
	private static void initializeDevice(String partname) throws IOException {
		libCells = new CellLibrary(RapidSmithEnv.getDefaultEnv()
				.getPartFolderPath(partname)
				.resolve(CELL_LIBRARY_NAME));
		device = RapidSmithEnv.getDefaultEnv().getDevice(partname);	
	}
	
	/*
	 * Parses the Vivado part name from the "design.info" file of a TINCR checkpoint 
	 */
	private static String parseInfoFile (String tcp) throws IOException {
		BufferedReader br = null;
		String part = "";
		
		try {
			br = new BufferedReader(new FileReader(tcp + File.separator + "design.info"));
			String line = br.readLine();
			part = line.split("=")[1];
		}
		catch (IndexOutOfBoundsException e) {
			MessageGenerator.briefErrorAndExit("[ERROR]: No part name found in the design.info file.");
		}
		finally {
			if (br != null)
				br.close();
		}
		
		return part;
	}
	
	/*
	 * Applies the placement constraints from the TINCR checkpoint files
	 * to the RapidSmith2 Cell Design.  
	 * TODO: update this function once MACRO cells are supported
	 */
	private static void applyPlacement(CellDesign design, String fname) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		
		//parse the placement file
		while ((line = br.readLine()) != null) {
			String[] toks = line.split("\\s+");
			if (toks[0].equals("LOC")) {
				String cname = toks[1]; 
				String sname = toks[2];
				Cell c = design.getCell(cname);
				
				Objects.requireNonNull(c, "Null cell found in design! This should never happen.");
				
				cellsInOrder.add(c);
				
				Site ps = device.getPrimitiveSite(sname);
				
				if (ps == null)
					MessageGenerator.briefError("[Warning] Site: " + sname + " not found, skipping placement of " + cname);
				else {
					String stype = toks[3]; 
					ps.setType(SiteType.valueOf(stype));

					String bname = toks[4];
					Bel b = ps.getBel(bname);
					
					if (b == null)
						MessageGenerator.briefError("[Warning] Bel: " + sname + "/" + bname + " not found, skipping placement of cell " + c.getName()); 
					else {
						design.placeCell(c, b);

						// Now, map all the cell pins to bel pins
						// TODO: Add a special case for mapping BRAM cell pins that can map to multiple bel pins
						//		it seems that this has something to do with 
						for (CellPin cp : c.getPins()) {
							List<BelPin> bpl = cp.getPossibleBelPins();
							
							//special case for the CIN pin
							if (b.getName().equals("CARRY4") && cp.getName().equals("CI") ) {
								cp.setBelPin("CIN");
							}
							//TODO: may have to update this with startwith FIFO as well as RAMB
							else if (bpl.size() == 1 || b.getName().startsWith("RAMB")) {
								if(bpl.get(0) != null)
									cp.setBelPin(bpl.get(0));
								else {
									MessageGenerator.briefErrorAndExit("Pin Error: " + c.getLibCell().getName() + " / " + cp.getName());
								}
							}
							else if (bpl.size() == 0) {
								MessageGenerator.briefError("[Warning]: Unknown cellpin to belpin mapping for cellpin: " + cp.getName());
							}
						}
					}
				}
			}
			//LOCK_PINS MAP1 MAP2 ... CELL
			else if (toks[0].equals("LOCK_PINS")) {
				Cell cell = design.getCell(toks[toks.length-1]); 
								
				//extract the actual cell to bel pin mappings for LUTs
				for(int i = 1; i < toks.length-1; i++){
					String[] pins = toks[i].split(":");
					cell.getPin(pins[0]).setBelPin(pins[1]);
				}
			}
		}
		br.close();
	}
	
	/*
	 * Applies the placement constraints from the TINCR checkpoint files
	 * to the RapidSmith2 Cell Design. Currently only supports relative 
	 * node names for ROUTE strings (Vivado default). Also, clock nets
	 * are not yet supported.
	 * 
	 * TODO: Update once macro cells are supported
	 * TODO: Update to handle clock strings with the name <3>HCLK* and <21>GCLK*
	 * TODO: Update to handle relative node names and absolute node names
	 */
	private static void applyRouting(CellDesign design, String fname) throws IOException {
		
		LineNumberReader br = new LineNumberReader(new BufferedReader(new FileReader(fname)));
		String line = null;

		while ((line = br.readLine()) != null) {
			String[] toks = line.split("\\s+");
			
			//handle intrasite routing
			if(toks[0].equals("SITE_PIPS")) {			
				Site ps = device.getPrimitiveSite(toks[1]);
				
				if(ps == null) {
					MessageGenerator.briefError("[Warning] Unable to find Primitive Site \"" + toks[1] + "\" in current device. Cannot create intrasite routing structure.\n"
							+ "        Line " + br.getLineNumber() + " of " + fname);
					continue;
				}
				
				//ps.setUsedSitePips(readUsedSitePips(ps, toks, br, fname));
				
				design.setUsedSitePipsAtSite(ps, readUsedSitePips(ps, toks, br, fname));
				
				//createSiteRouteTrees(ps);
				//siteToRTs.put(ps.getName(), pinToRT);
			}
			// handle intersite (between sites) routing
			else if(toks[0].equals("ROUTE")) {
				String netname = toks[1]; 
				CellNet net = design.getNet(netname); //net to add the route tree to
				
				// make sure the net is in the design
				if (net == null) {
					MessageGenerator.briefErrorAndExit("[ERROR] Unable to find net \"" + netname + "\" in cell design\n"
							+ "\tLine " + br.getLineNumber() + " of " + fname);
				}
				
				// Vivado nets always have to have a source pin to be valid...skip nets with no source
				// Should I throw an error here?
				if(net.getSourcePin() == null) {
					MessageGenerator.briefError("[Warning] Net " + netname + " is not sourced. Cannot apply routing information.\n"
							+ "\tLine " + br.getLineNumber() + " of " + fname);
				}
				else {
					// TODO: test clock net and make sure it works
					if(net.getType().equals(NetType.WIRE)) {			
						RouteTree start = initializeRouteTree(net, toks[3]);
						createIntersiteRouteTree(start, 4, toks);
						net.addRouteTree(start.getFirstSource());
					}					
					else { // if its not a wire, then its a ground or VCC net
						int index = 4; 
						
						while(index < toks.length) {
							// TODO: Could have another RouteTree pointer point to start
							// so that I don't have to call getFirstSource for larger nets
							RouteTree start = initializeGlobalLogicRouteTree(toks[index++]);
							index = createIntersiteRouteTree(start, index, toks);
							net.addRouteTree(start.getFirstSource());
							index += 3; // get to the start of the next route string
						}	
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
			Integer wireEnum = device.getWireEnumerator().getWireEnum(pipWireName);
			
			if(wireEnum == null) {
				MessageGenerator.briefError("[Warning] Unknown PIP wire \"" + pipWireName + "\" in current device.\n"
						+ "        Line " + br.getLineNumber() + " of " + fname);
				continue;
			}
			//add the input and output pip wires (there are two of these in RS2)
			usedSitePips.add(wireEnum); 	
			usedSitePips.add(device.getWireEnumerator().getWireEnum(pipWireName.split("\\.")[0] + ".OUT"));
		}	
		return usedSitePips;
	}
		
	/*
	 * Creates the initial RouteTree from a ROUTE string
	 */
	private static RouteTree initializeRouteTree(CellNet net, String firstWireName) {
		
		Tile sourceTile = net.getSourcePin().getCell().getAnchorSite().getTile(); 
		Wire firstWire = new TileWire(sourceTile, device.getWireEnumerator().getWireEnum(firstWireName));
		return new RouteTree(firstWire);
	}
	
	/*
	 * Creates the initial RouteTree for a GND or VCC ROUTE string
	 */
	private static RouteTree initializeGlobalLogicRouteTree(String firstWireName) {
		
		String[] toks = firstWireName.split("/");
		assert(toks.length == 2); 
		
		Tile tile = device.getTile(toks[0]);
		Wire firstWire = new TileWire(tile, device.getWireEnumerator().getWireEnum(toks[1]));
		
		return new RouteTree(firstWire);
	}
		
	/*
	 * Converts a Vivado ROUTE string to a RS2 RouteTree data structure
	 */
	private static int createIntersiteRouteTree(RouteTree current, int index, String[] toks) {
		
		while ( index < toks.length ) {
			//we hit a branch in the route
			if (toks[index].equals("{") ) {
				index++;
				RouteTree test = checkForPreBranchWires(current, toks[index]);
				
				boolean shouldSourceParent = false;
				if (!current.equals(test)) {
					current = test;
					shouldSourceParent = true;
				}
					
				index = createIntersiteRouteTree(current, index, toks);
				
				if(shouldSourceParent) 
					current = current.getSourceTree();	
			}
			//end of a branch
			else if (toks[index].equals("}") ) {
				//if(current.getWire().getPinConnections().size() == 0) 
					completeFinalBranchNode(current);
				//else 
				//	connectSitePinRouteTree(current);
				
				return index + 1; 
			}
			else {
				current = findNextNode(current, toks[index]);
				index++;
			}
		}
		return index;
	}
	
	/*
	 * Searches for the next node of the Vivado ROUTE string in RS2. Since Vivado represents nodes by only the first
	 * wire in the node and RS2 represents the first and last wire, we have to search a maximum of two wires away to find the
	 * next node in the route (and add the missing wire in RS2 if necessary). 
	 */
	//TODO: make sure not to add the first c connection if the connection is already added
	private static RouteTree findNextNode(RouteTree rt, String nodeName) {
		//search for the next node in the device...it will be either one wire or two RS wires away
		//System.out.println(nodeName + " " + rt.getWire().getTile().getName() + "/" + rt.getWire().getWireName());
		
		// check for a clock route string...add to this as necessary
		if (nodeName.startsWith("<")) {
			Direction dir = nodeName.charAt(3) == 'H' ? Direction.LEFT : Direction.BELOW;
			return GetNextClockWire(rt, rt.getWire().getTile(), nodeName.substring(3), Character.getNumericValue(nodeName.charAt(1)) - 1, dir);
		}
		
		if (nodeName.startsWith("HCLK")) {
			return GetNextClockWire(rt, rt.getWire().getTile(), nodeName, 0, Direction.LEFT);
		}
		
		if (nodeName.startsWith("GCLK")) {
			return GetNextClockWire(rt, rt.getWire().getTile(), nodeName, 0, Direction.BELOW);
		}
		
		// normal string
		//first, search ALL wires that are one hop away
		for(Connection c: rt.getWire().getWireConnections()) {
			if(c.getSinkWire().getWireName().equals(nodeName) ) {
				rt = rt.addConnection(c);
				return rt;
			}
		}
		
		//if we don't find it, then search all wires that are two hops away
		for(Connection c: rt.getWire().getWireConnections()) {
			for(Connection c2: c.getSinkWire().getWireConnections()) {
				if(c2.getSinkWire().getWireName().equals(nodeName)) {

					rt = addConnectionToRouteTree(rt, c);		
					rt = rt.addConnection(c2);
					return rt;
				}
			}
		}
		
		//ASSUMPTION: we never actually reach here
		assert(false);
		return null;
	}
	
	private static RouteTree GetNextClockWire(RouteTree tree, Tile tile, String wirename, int offset, Direction searchDir)
	{
		Tile currentTile = tile;
		int wire = device.getWireEnumerator().getWireEnum(wirename);	
		
		while (offset > 0)
		{
			if (currentTile.getWires().contains(wire) )
			{
				offset--;
			}
			
			currentTile = getAdjacentTile(currentTile, searchDir); 
		}
		
		// I think we have to do this to prevent us from recreating a new connection object for the route tree? 
		/*
		for	(Connection c : tree.getWire().getWireConnections())
		{
			if(c.getSinkWire().getTile().equals(currentTile)){
				return tree.addConnection(c);
			}
		}
		*/
		
		// this is creating a new wire connection object...there is always one in memory, but is not efficient to access
		WireConnection wc = new WireConnection(wire, tile.getRow() - currentTile.getRow(), tile.getColumn() - currentTile.getColumn(), true);
		
		return tree.addConnection(Connection.getTileWireConnection((TileWire)tree.getWire() , wc));
	}
	
	private static Tile getAdjacentTile(Tile tile, Direction direction){
		return getAdjacentTile(tile, direction, 1);
	}
	
	private static Tile getAdjacentTile(Tile tile, Direction direction, int offset) {
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
				assert(false); // should never reach here
				return null;
		}
	}
	
	private enum Direction
	{
		ABOVE,
		BELOW, 
		LEFT,
		RIGHT
	}
	
	/*
	 * Adds a connection to a route tree. Before adding the connection,
	 * it first checks to make sure the connection has not been added before.
	 * TODO: Update this to be more efficient
	 */
	private static RouteTree addConnectionToRouteTree(RouteTree parent, Connection c) {
		Wire w = c.getSinkWire();
		RouteTree found = null;
		for(RouteTree child : parent.getSinkTrees()) {
			if(child.getWire().equals(w)) {
				found = child;
				break;
			}
		}
		
		return (found == null) ? parent.addConnection(c) : found;
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
				current = addConnectionToRouteTree(current, tmp); //current.addConnection(tmp);
			}
			else { //search for the first wire in the branch, and add the missing wire connections if necessary 
				outerLoop : for(Connection c : connections) {
					for(Connection c2: c.getSinkWire().getWireConnections()){
						if(c2.getSinkWire().getWireName().equals(branchWireName)) {
						
							current = addConnectionToRouteTree(current, c);
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
	private static void completeFinalBranchNode(RouteTree rt) {
		
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
		//assert(tmp.size() == 1); 
		if (tmp.size() == 1) {
			Connection c = tmp.iterator().next();
			if (!c.isPip()) 
				rt.addConnection(c);
		}
	}
	
	/* **************************************
   	 * 		   Exporter Code Start
   	 ***************************************/	
	/**
	 * Export the RapidSmith2 design into an existing TINCR checkpoint file. 
	 * Currently assumes the cells of the design has been unmodified so that
	 * it can write cells to the checkpoint file in the order that they were
	 * read in. 
	 * 
	 * TODO: add support for changed designs...writing the EDIF out and constraint file
	 * TODO: Create a java comparator class that can sort cells to be in the correct
	 * 		 order for Vivado
	 *  
	 * @param tcpDirectory TINCR checkpoint directory to write XDC files to
	 * @param design CellDesign to convert to a TINCR checkpoint
	 * @throws IOException
	 * @throws InvalidEdifNameException 
	 * @throws EdifNameConflictException 
	 */
	public static void writeTCP(String tcpDirectory, CellDesign design) throws IOException, EdifNameConflictException, InvalidEdifNameException {
		new File(tcpDirectory).mkdir();
		writePlacementXDC(tcpDirectory, design);
		writeRoutingXDC(tcpDirectory, design);
		String edifOut = Paths.get(tcpDirectory, "netlist.edf").toString();
		EdifInterface.writeEdif(edifOut, design);
	}
	
	/*
	 * Function to write the placement.xdc file of a TINCR checkpoint
	 */
	private static void writePlacementXDC(String tcpDirectory, CellDesign design) throws IOException {
		Path outPath = Paths.get(tcpDirectory, "placement.xdc");
		BufferedWriter fileout = new BufferedWriter (new FileWriter(outPath.toString()));
		
		//TODO: Assuming that the logical design has not been modified...can no longer assume this with insertion/deletion
		for (Cell cell : cellsInOrder) {
			if(cell.isPlaced()) {
				Site ps = cell.getAnchorSite();
				Bel b = cell.getAnchor();
				String cellname = cell.getName();
				
				fileout.write(String.format("set_property BEL %s.%s [get_cells {%s}]\n", ps.getType().toString(), b.getName(), cellname));
				fileout.write(String.format("set_property LOC %s [get_cells {%s}]\n", ps.getName(), cellname));
									
				//TODO: Update this function when more cells with LOCK_PINS are discovered
				if(cell.getLibCell().getName().startsWith("LUT")) {
					fileout.write("set_property LOCK_PINS { ");
					for(CellPin cp: cell.getInputPins()) 
						fileout.write(String.format("%s:%s ", cp.getName(), cp.getBelPin().getName()));
					
					fileout.write("} [get_cells {" + cellname + "}]\n");
				}
			}
			else {
				System.out.println(cell.getLibCell().getName());
			}
		}
		
		fileout.close();
	}
	
	/*
	 * Function to write the routing.xdc file of a TINCR checkpoint
	 */
	private static void writeRoutingXDC(String tcpDirectory, CellDesign design) throws IOException {
		Path outPath = Paths.get(tcpDirectory, "routing.xdc");
		BufferedWriter fileout = new BufferedWriter (new FileWriter(outPath.toString()));
		
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
	
} // END CLASS 

/* ***********
 * Possible future code/utility functions that are currently un-needed
 * ************/
//future code to create top level ports (if this functionality is added to RapidSmith
//probably have to first check for null before iterating through the portlist that is returned
/*
for (EdifPort ep: tcell.getInterface().getPortList()) {
	Port p;
	int portDirection = (ep.isInputOnly()) ? 0 : ( ep.isOutputOnly() ? 1 : 2);
	msg("Top Level Port: " + ep.getName());
	if (ep.isBus()){
		for (EdifSingleBitPort single: ep.getSingleBitPortList()) {
			//p = new Port(single.getPortName(), portDirection);
			msg("\tchild: " + single.getPortName());
			//des.addTopLevelPort(p);
		}
	}
	else {
		p = new Port(ep.getName(), portDirection);
	}
}
*/