package edu.byu.ece.rapidSmith.examples2;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import edu.byu.ece.rapidSmith.interfaces.vivado.TincrCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.edif.util.parse.ParseException;
import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.SitePin;

public class DesignAnalyzer {
	
	    // part name and cell library  
	public static final String PART_NAME = "xc7a100tcsg324";
	public static final String CANONICAL_PART_NAME = "xc7a100tcsg324";
	public static final String CELL_LIBRARY = "cellLibrary.xml";
	
	private static CellLibrary libCells;
	private static Device device;
	
	public static void classSetup() throws IOException {
		libCells = new CellLibrary(RSEnvironment.defaultEnv()
				.getPartFolderPath(PART_NAME)
				.resolve(CELL_LIBRARY));
		device = RSEnvironment.defaultEnv().getDevice(CANONICAL_PART_NAME);
	}
	
	
	public static void main(String[] args) throws IOException, ParseException {
		
		if (args.length < 1) {
			System.err.println("Usage: DesignAnalyzer tincrCheckpointName");
			System.exit(1);
		}
		
		String checkpointbase = args[0];

		// Load device file
		System.out.println("Loading Device...");
		classSetup();
		
		// Loading in a TINCR checkpoint
		System.out.println("Loading Design...");
		TincrCheckpoint tcp = VivadoInterface.loadTCP(checkpointbase+".tcp");
		CellDesign design = tcp.getDesign();
		
//        DesignAnalyzer da = new DesignAnalyzer();
        
		prettyPrintDesign(design);
		summarizeDesign(design);        

		System.out.println("Done...");
	}


	public static void prettyPrintDesign(CellDesign design) {
		// Print the cells
		for (Cell c : design.getCells()) {
			System.out.println("Cell: " + c.getName() + " " + 
					c.getLibCell().getName());
			if (c.isPlaced())
				// Print out its placement
				System.out.println("  <<<Placed on: " + c.getAnchor() + ">>>");
			else System.out.println("  <<<Unplaced>>>");
			// Print out the pins
			for (CellPin cp : c.getPins()) {
				System.out.println("  Pin: " + cp.getName() + " " + 
						cp.getDirection() + " " + 
						(cp.getNet()!=null?cp.getNet().getName():"<unconnected>"));
			}
			// Print the properties for a given cell if there are any
			for (Property p : c.getProperties()) {
				String s = null;
//				System.out.print("  Property: " + p.getStringKey() + " = ");

//				if (p.getValue() instanceof Integer)
//					s = p.getValue().toString() + " <int>";
//				else if (p.getValue() instanceof Boolean)
//					s = p.getValue().toString() + " <bool>";
//				else if (p.getValue() instanceof String)
//					s = p.getValue().toString() + " <string>";
//				else MessageGenerator.briefErrorAndExit("[ERROR] Unknown type for property: " + p.toString());
	
				s = "  Property: " + p.toString();
				System.out.println(s);
			}
		}

		// Print the nets
		System.out.println();
		for (CellNet n : design.getNets()) {
//			if (!n.getName().equals("D2_1946") && !n.getName().equals("nextstate_TMR_1"))
//				continue;
			System.out.println("Net: " + n.getName());
			HashSet<CellPin> pins = (HashSet<CellPin>) n.getPins();
			// Print the net's pins
			for (CellPin cp : pins) {
				if (cp == n.getSourcePin())
					System.out.println("  Pin*: " + cp.getCell().getName() + "." + cp.getName());
				else
					System.out.println("  Pin:  " + cp.getCell().getName() + "." + cp.getName());
			}
			
			// Print the net's route tree(s) if they exist
			
			// First, do the beginning (source pin intra-site routing)
			// Iterate through the source RouteTree however you want
			String s = "{ " + createRoutingString(n.getSourceRouteTree(), true) + " }"; 
			System.out.println("Source intrasite routing: " + s);

			s = "{ " + createRoutingString(n.getIntersiteRouteTree(), true) + " }";
			System.out.println("Intersite Routing: " + s);
		}
	}		
		
	private static String getSourceIntraSiteRoutingString(RouteTree rt) {
		String s = "";
		if (rt == null) return s;

		// If rt is a leaf cell then one and exactly one of these is true:
		//   (a) We have hit a site pin or
		//   (b) We have hit a bel pin
		if(rt.isLeaf())	 {
			assert (rt.getConnectingBelPin()!=null || rt.getConnectingSitePin()!=null);
			assert (!(rt.getConnectingBelPin()!=null && rt.getConnectingSitePin()!=null));
			SitePin conn = rt.getConnectingSitePin();
			if (conn != null)
				return s + " SlicePin{" + conn + "}";
			else {
				BelPin bp = rt.getConnectingBelPin();
				if (bp != null)
					return s + " " + bp;
			}
		}
		// If rt is not a leaf cell then there must be at least one sink tree
		else {
			
			s += " {" + rt.getWire() + "}";
		}
		return s;
	}


	public static String getVivadoIntersiteRoutingString(CellNet n) {
			
		Collection<RouteTree> rts = n.getIntersiteRouteTreeList();
			
		// A WIRE type (normal signal) should have only one route tree if it is routed.
		if (n.getType().equals(NetType.WIRE)) {
			assert (rts.size() <= 1);
			if (rts.size() == 1) {
				RouteTree rt = n.getIntersiteRouteTreeList().iterator().next();
				return "{ " + createVivadoRoutingString(rt.getFirstSource(), true) + " }"; 
			}
		}
		// Otherwise, must be a VCC/GND net, which may have multiple route trees, each with their own VCC or GND source.
		else if (rts.size() > 0) {
			String routeString = "{ ";
			for (RouteTree rt : rts) {
				routeString += "( " + createVivadoRoutingString(rt.getFirstSource(), true) + " ) ";
			}
			routeString += "}";
			return routeString;
		}
		return "";
	}

	// Given a pointer to the head of a RouteTree, format up a string to represent it.
	// These are essentially the same as the directed routing strings Vivado uses to represent physical routes.
	// Comparing what this produces to the routing.txt files in a Tincr checkpoint, one will see the same structure.  
	// However, there are three differences:
	//   1. Vivado directed routing strings only list the head end of a wire (each end of a wire in Vivado typically has 
	//     a different name with a non-programmable connection between them).  Here we list both wire end names (the 2nd one is in parentheses).
	//   2. We list wire segment in the form: tileName/wireName.  This is legal for Vivado but it's representation doesn't usually include the tileName.
	//   3. When a wire branches, the various branches may appear in a different order between the two representations.  This doesn't change the structure.
	public static String createVivadoRoutingString(RouteTree rt, boolean head) {
		String s="";
		Collection<RouteTree> sinkTrees = rt.getSinkTrees();
		
		// Always print first wire at the head of a net's RouteTree
		if (head)
			s = rt.getWire().getTile().getName() + "/" + rt.getWire().getWireName();

		// The connection between this RouteTree and its upstream predecessor may be a PIP (programmable connection)
		//    or it may be a non-programmable connection.  
		// If it is a programmable connection - include it.
		else if (rt.getConnection().isPip() || rt.getConnection().isRouteThrough())
			s = " " + rt.getWire().getTile().getName() + "/" + rt.getWire().getWireName();

		// It is a non-programmable connection (a re-naming of the other end of the wire).
		// Append it to 
		else  
			s += "(" + rt.getWire().getWireName() + ")";

		// Iterate across the sink trees and print them
		for (Iterator<RouteTree> it = sinkTrees.iterator(); it.hasNext(); ) {
			RouteTree sink = it.next();

			// If there is only one sink tree then this is just the next wire segment in the route (not a branch).  
			// Don't enclose this in {}'s, just list it. 
			// Or, if this is the last leg of a multi-way branch, don't enclose this in {}'s (to match Vivado's style).
			if (sinkTrees.size() == 1 || !it.hasNext()) {
				s += createVivadoRoutingString(sink, false);
			}
			// Otherwise, this is a branch of the wire, so enclose it in { }'s to mark that the wire is branching.
			else {
				s += " {" + createVivadoRoutingString(sink, false) + " }";
			}
		}
		return s;
	}

	// Given a pointer to the head of a RouteTree, format up a string to represent it.
	// This works for either intra-site routes as well as inter-site routes
	public static String createRoutingString(RouteTree rt, boolean head) {
		if (rt == null)  return "";
		String s="";

		Collection<RouteTree> sinkTrees = rt.getSinkTrees();
		
		// Always print first wire at the head of a net's RouteTree
		if (head)
			s = rt.getWire().getTile().getName() + "/" + rt.getWire().getWireName();

		// The connection between this RouteTree and its upstream predecessor may be a PIP (programmable connection)
		//    or it may be a non-programmable connection.  
		// Look upstream and, if it is a programmable connection, include it.
		else if (rt.getConnection().isPip() || rt.getConnection().isRouteThrough())
			s = " " + rt.getWire().getTile().getName() + "/" + rt.getWire().getWireName();
		// It is not a programmable connection but a wire end renaming - append it in parens.
		else  
			s += "(" + rt.getWire().getWireName() + ")";

		// Now, let's look downstream and see what to do

		// If it is a leaf cell then let's print the site or bel pin attached
		if(rt.isLeaf())	 {
			assert (rt.getConnectingBelPin()!=null || rt.getConnectingSitePin()!=null);
			assert (!(rt.getConnectingBelPin()!=null && rt.getConnectingSitePin()!=null));
			SitePin conn = rt.getConnectingSitePin();
			if (conn != null)
				return s + " SlicePin{" + conn + "}";
			else {
				BelPin bp = rt.getConnectingBelPin();
				if (bp != null)
					return s + " " + bp;
			}
		}

		// Otherwise, iterate across the sink trees and print them
		for (Iterator<RouteTree> it = sinkTrees.iterator(); it.hasNext(); ) {
			RouteTree sink = it.next();

			// If there is only one sink tree then this is just the next wire segment in the route (not a branch).  
			// Don't enclose this in {}'s, just list it. 
			// Or, if this is the last leg of a multi-way branch, don't enclose this in {}'s (to match Vivado's style).
			if (sinkTrees.size() == 1 || !it.hasNext()) {
				s += createVivadoRoutingString(sink, false);
			}
			// Otherwise, this is a branch of the wire, so enclose it in { }'s to mark that the wire is branching.
			else {
				s += " {" + createVivadoRoutingString(sink, false) + " }";
			}
		}
		return s;
	}

	public static void summarizeDesign(CellDesign design) {
		int numcells = design.getCells().size();
		int numnets= design.getNets().size();

		int numplaced = 0;
		for (Cell c : design.getCells())
			if (c.getAnchor() != null)
				numplaced++;
		System.out.println("The design has: " + design.getCells().size() + " cells, " + numplaced + " of them are placed.");
		
		int numrouted= 0;
		for (CellNet n: design.getNets())
			if (n.getIntersiteRouteTreeList()!= null)
				numrouted++;
		System.out.println("The design has: " + design.getNets().size() + " nets, "  + numrouted + " of them are routed.");
		
	}
	
}

