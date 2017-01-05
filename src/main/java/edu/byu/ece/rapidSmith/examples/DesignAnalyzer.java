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

package edu.byu.ece.rapidSmith.examples;

import java.io.IOException;
import java.util.*;

import edu.byu.ece.rapidSmith.interfaces.vivado.TincrCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.SitePin;
import org.jdom2.JDOMException;

public class DesignAnalyzer {
	
	    // part name and cell library  
	public static final String PART_NAME = "xc7a100tcsg324";
	public static final String CANONICAL_PART_NAME = "xc7a100tcsg324";
	public static final String CELL_LIBRARY = "cellLibrary.xml";
	
	public static void main(String[] args) throws IOException, JDOMException {
		
		if (args.length < 1) {
			System.err.println("Usage: DesignAnalyzer tincrCheckpointName");
			System.exit(1);
		}
		
		// Load a TINCR checkpoint
		System.out.println("Loading Design...");
		TincrCheckpoint tcp = VivadoInterface.loadTCP(args[0] + ".tcp");
		CellDesign design = tcp.getDesign();
		
		// Print out a representation of the design 
		prettyPrintDesign(design);
		
		System.out.println();
		
		// Print out some summary statistics onthe design
		summarizeDesign(design);      
		
		printCellBelMappings(design);

		System.out.println("Done...");
	}

	// Print out the first few cells and the list of Bels they can be placed onto
	public static void printCellBelMappings(CellDesign design) {
		System.out.println("\nSome Cell/Bel Mappings:");
		int i=0;
		Set<String> cells = new HashSet<>();
		for (Cell c : design.getCells()) {
			if (cells.contains(c.getLibCell().getName()))
				continue;
			cells.add(c.getLibCell().getName());
			if (++i > 20)
				break;
			System.out.println("  Cell #" + i + " = " + c.toString());
			if (c.getPossibleAnchors().size() == 0)
				System.out.println("    This cell cannot be placed.");
			for (BelId b : c.getPossibleAnchors()) {
				System.out.println("    Can be placed onto sites of type " + b.getSiteType() + " on Bels of type " + b.getName());
			}
		}
	}
	
	
	/**
	 * Print out a formatted representation of a design to help visualize it.  Another way of visualizing designs is illustrated
	 * in the DotFilePrinterDemo program in the examples2 directory.  
	 * @param design The design to be pretty printed.
	 */
	public static void prettyPrintDesign(CellDesign design) {
		// Print the cells
		for (Cell c : design.getCells()) {
			System.out.println("\nCell: " + c.getName() + " " + 
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
			// For now, properties are strings. 
			for (Property p : c.getProperties()) {
				String s = "  Property: " + p.toString();
				System.out.println(s);
			}
		}
		
		// Print the nets
		for (CellNet n : design.getNets()) {
			System.out.println("\nNet: " + n.getName());

			// Print the net's pins
			// Source pin first
			System.out.println("  Source Pin: " + n.getSourcePin().getCell().getName() + "." + n.getSourcePin().getName());

			// Then the sink pins
			for (CellPin cp : n.getSinkPins()) {
				System.out.println("  Pin:  " + cp.getCell().getName() + "." + cp.getName());
			}
			
            // Print the net's route tree(s) if they exist
			// In the net definitions which get printed, the syntax is the following:
			//   1. Branching of nets is shown by enclosing side-branches within { and } characters."
			//   2. For a given wire segment name, a / character will separate the tile name from the wire name."
			//   3. Sometimes a wire will have 2 names (or more properly, each end will have a different name with a 
			//      non-programmable connection between them).  In these cases, the 2nd wire name is appended 
			//      to the first wire name inside ( )'s.   Otherwise, there are PIPs between successive wires.
			// There are actually 3 parts to a net's physical routing.  
			//  a) The first is from a BEL pin to the site pins its leaves the source site on.
			//  b) The second is the inter-site route for the net = that is the routing that is all done in switchboxes.
			//  c) Eventually the wire then re-enters sites where the sink BEL pins are.
			// The code below traverses all 3 sections and prints out what it finds along the way as a way of 
			// demonstrating how to trace out a net's physical route.
			
			// VCC and GND nets are different from regular nets in that regular nets have a single which is the root of the
			// route tree while VCC and GND nets are a forest of route trees.  For this demo only do signal nets.
			if (n.isVCCNet() || n.isGNDNet()) {
				System.out.println("Vcc or GND net, not printing out its route trees.");  
				System.out.println("Since VCC and GND drivers (tieoffs) are not placed anywhere, these have no source route trees, they just have intersite sink route trees.");
				System.out.println("Vcc and GND nets have multiple intersite route trees, each with a single source.");
			}
			else {
				// Regular nets should have only a single route tree
				assert(n.getIntersiteRouteTreeList().size() <= 1);
				String s = createRoutingString(n, n.getSourceRouteTree(), true, true);
				if (Objects.equals(s, ""))
					System.out.println("<<<Unrouted>>>");
				else
					System.out.println("Physical routing: { " + createRoutingString(n, n.getSourceRouteTree(), true, true) + " }"); 
			}
		}
	}		
		

	// 
	/**
	 * Given a pointer to the head of a RouteTree, format up a string to represent it.
	   This works for either intra-site routes as well as inter-site routes
	 * @param n The net being traversed
	 * @param rt The RouteTree object we are currently at in the physical route.
	 * @param head An indication if we are just starting a wire so we can be sure to print out that segment. 
	 * @param inside An indication of whether we are inside a site or outside.  Physical wires start inside sites and go until they hit site pins, 
	 * at which point they enter the global routing fabric.  They eventually hit site pins again at which point they re-enter sites.  They then
	 * continue until they hit BEL pins, which are the sink pins of the physical route.
	 * @return A string representing the physical route.  It is similar in many ways to XIlinx Directed Routing strings but have been enhanced 
	 * to show where the route enters and exits sites as well as a description of the sink pins where it terminates.
	 */
	public static String createRoutingString(CellNet n, RouteTree rt, boolean head, boolean inside) {
		String s="";

		if (rt == null)  return s;

		// A RouteTree object contains a collection of RouteTree objects which represent the downstream segments making up the route.
		// If this collection has more than element, it represents that the physical wire brances at this point.
		Collection<RouteTree> sinkTrees = rt.getSinkTrees();
		
		// Always print first wire at the head of a net's RouteTree. The format is "tileName/wireName".
		if (head)
			s = rt.getWire().getFullWireName();

		// The connection member of the RouteTree object describes the connection between this RouteTree and its predecessor.
		// The connection may be a programmable connection (PIP or route-through) or it may be a non-programmable connection.  
		// Look upstream and, if it was a programmable connection, include it.
		else if (rt.getConnection().isPip() || rt.getConnection().isRouteThrough())
			s = " " + rt.getWire().getFullWireName();
		// It is a non-programmable connection - append it in parens.
		else  
			s += "(" + rt.getWire().getWireName() + ")";

		// Now, let's look downstream and see where to go and what to print
		// If it is a leaf cell then let's print the site or bel pin attached.  In the case of a site pin, continue following it.
		if (rt.isLeaf())	 {
			SitePin sp = rt.getConnectingSitePin();
			if (sp != null) {
				if (inside) 
					// Follow the route out of the site into the general routing fabric 
					s += " SitePin{" + sp + "} <<entering general routing fabric>> " + createRoutingString(n, n.getIntersiteRouteTree(), true, !inside);
				else
					// Follow the route from the general routing fabric and into a site
					s += " SitePin{" + sp + "} <<leaving general routing fabric, entering site>> " + createRoutingString(n, n.getSinkRouteTree(sp), true, inside);
			}
			// If not a site pin, see if it is a BEL pin (it should be).  Print the BEL pin.
			else {
				BelPin bp = rt.getConnectingBelPin();
				assert (bp != null);
				return s + " " + bp;
			}
		}

		// Otherwise, if it is not a leaf route tree, then iterate across its sink trees and print them
		for (Iterator<RouteTree> it = sinkTrees.iterator(); it.hasNext(); ) {
			RouteTree sink = it.next();

			// If there is only one sink tree then this is just the next wire segment in the route (not a branch).  
			// Don't enclose this in {}'s, just list it as the next wire segment. 
			if (sinkTrees.size() == 1) 
				s += createRoutingString(n, sink, false, inside);
			// Otherwise, this is a branch of the wire, so enclose it in { }'s to mark that it represents a branch in the wire.
			else {
				s += " {" + createRoutingString(n, sink, false, inside) + " }";
			}
		}
		return s;
	}

	public static void summarizeDesign(CellDesign design) {

		System.out.println("Design Summary:");
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

// Other ideas:
// - Get all connected nets from a cell
// - How to handle pseudo cell pins?
//   + They don't have a backing library cell pin
// - Example of attaching a pseudo pin

