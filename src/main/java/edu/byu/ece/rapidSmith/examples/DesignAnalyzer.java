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

import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoCheckpoint;
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
		VivadoCheckpoint vcp = VivadoInterface.loadRSCP(args[0]);
		CellDesign design = vcp.getDesign();
		
		// Print out a representation of the design 
		prettyPrintDesign(design, true);
		
		System.out.println();
		
		// Print out some summary statistics on the design
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
			if (c.isMacro()) { 
				System.out.println("    Cell is macro");
				continue;
			}
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
		prettyPrintDesign(design, false);
	}
	
	/**
	 * Print out a formatted representation of a design to help visualize it.  Another way of visualizing designs is illustrated
	 * in the DotFilePrinterDemo program in the examples2 directory.  
	 * @param design The design to be pretty printed.
	 * @param Flag to control printing of detailed cellBelPinMappings 
	 */
	public static void prettyPrintDesign(CellDesign design, boolean cellBelPinMappings) {
		// Print the cells
		for (Cell c : design.getCells()) {
			if (c.isMacro())
			{				
				// Mark the start of a macro cell section
				System.out.println("\n====================================================");
				
				prettyPrintCell(c, cellBelPinMappings);
				
				// Print out names of internal nets
				Collection<CellNet> internalNets = c.getInternalNets();
	
				if (internalNets != null && !internalNets.isEmpty())
				{
					for (Iterator<CellNet> it = internalNets.iterator(); it.hasNext(); )
					{
						CellNet internalNet = it.next();
						System.out.println("  Internal Net: " + internalNet.getName());
					}
				}
				
				// Print out the internal cells
				Collection<Cell> internalCells = c.getInternalCells();
				
				if (internalCells != null && !internalCells.isEmpty())
				{
					for (Iterator<Cell> it = internalCells.iterator(); it.hasNext(); )
					{
						Cell internalCell = it.next();
						prettyPrintCell(internalCell, cellBelPinMappings);
					}
					// Mark the end of a macro cell section
					System.out.println("====================================================");
				}
			}
			else prettyPrintCell(c, cellBelPinMappings);
		}
		
		// Print the nets
		for (CellNet n : design.getNets()) {
			System.out.println("\nNet: " + n.getName());
			
			// Print if it is an internal net
			if (n.isInternal())
				System.out.println("  *Internal Net*");

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
				if (n.getIntersiteRouteTreeList().size() > 1)
					System.out.println("  Note: net \"" + n.getName() + "\" has " + n.getIntersiteRouteTreeList().size() + " intersite route trees.");
				String s = createRoutingString("   ", n, n.getSourceRouteTree(), true, true);
				if (Objects.equals(s, ""))
					System.out.println("  <<<Unrouted>>>");
				else
					System.out.println("  Physical routing: \n   (" + createRoutingString("   ", n, n.getSourceRouteTree(), true, true) + "\n   )"); 
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
	public static String createRoutingString(String indnt, CellNet n, RouteTree rt, boolean head, boolean inside) {
		String s="";

		if (rt == null)  return s;

		// A RouteTree object contains a collection of RouteTree objects which represent the downstream segments making up the route.
		// If this collection has more than element, it represents that the physical wire branches at this point.
		Collection<RouteTree> sinkTrees = rt.getSinkTrees();
		
		// Always print first wire at the head of a net's RouteTree. The format is "tileName/wireName".
		if (head)
			s = "<head>" + rt.getWire().getFullName();
		

		// The connection member of the RouteTree object describes the connection between this RouteTree and its predecessor.
		// The connection may be a programmable connection (PIP or route-through) or it may be a non-programmable connection.  
		// Look upstream and, if it was a programmable connection, include it.
		else if (rt.getConnection().isPip() || rt.getConnection().isRouteThrough())
			s = " " + rt.getWire().getFullName();
		// It is a non-programmable connection - append it with marker.
		else  
			s += "=" + rt.getWire().getName();

		// Now, let's look downstream and see where to go and what to print.
		// If it is a leaf cell, it either: 
		//     (1) has a site pin attached, 
		//     (2) has a BEL pin attached, or 
		//     (3) simply ends. Wires that end like this are called "used stubs" in Vivado's GUI.  They don't go anywhere.
		if (rt.isLeaf()) {
			SitePin sp = rt.getConnectedSitePin();
			BelPin bp = rt.getConnectedBelPin();
			if (sp != null) {
				// If we are at a site pin then what we do differs depending on whether we are inside the site (and leaving) or outside the site (and entering). 
				if (inside) {  
					// Inside site, so look for correct intersite route tree to leave on
					for (RouteTree rt1 : n.getIntersiteRouteTreeList()) 
						if (sp.getExternalWire().equals(rt1.getWire())) 
							return s + " SitePin{" + sp + "} <<entering general routing fabric>> " + createRoutingString(indnt, n, rt1, true, !inside);
					////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
					// An explanation on the above code is in order.
					// This explanation only applies to regular nets (VCC and GND nets have their own special rules).
					// When tracing from inside a Site out into the intersite routing, there are multiple cases to consider:
					// Case 1. A net connects to a single SitePin, the net has a single intersite route tree
					//    + Straightforward - n.getIntersiteRouteTree() will give the first (and only) RouteTree to follow.
					// Case 2. A net connects to multiple SitePin's, the net has multiple intersite route trees
					//    + In this case, all we have ever seen is that the corresponding route trees reconverge immediately
					//      through a site PIP (reconvergent fanout).
					//    + Also in this case, we have only observed this happening when a signal leaves both the COUT and DMUX site pins.
					//    + In this case you can follow either both RouteTrees (and get redundant paths printed out) or just one.
					// Case 3. A net connects to multiple SitePin's, but the net has only a single intersite route tree
					//    + One of the SitePin's connects to the wire at head of an intersite route tree and the other doesn't.
					//    + In this case as in #1 above, just follow the single intersite route tree
					// Case 4. We have not yet observed this last case: net connects to single SitePin, net has multiple intersite route trees.
					//    + This doesn't make sense.
					//
					// The above code will handle case 1 and 2 just fine by searching - it will find the corresponding RouteTree for each SitePin hit.  
					// For case 3 it will find a RouteTree for one of the SitePin's but not the other (and fall out the bottom of the for-loop).	
					// This last case (3b) is handled below.
					////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
					
					// Case 3b: If we get here, net connects to a SitePin but there is no corresponding RouteTree... 
					return s + " SitePin{" + sp + "} <<<<Connects to no corresponding RouteTree outside site>>>> ";
				}
				else 
					// Outside site, so just follow the route from the general routing fabric and into a site
					return s + " SitePin{" + sp + "} <<Leaving general routing fabric, entering site>> " + createRoutingString(indnt, n, n.getSinkRouteTree(sp), true, inside);
			}
			// If not a site pin, see if it is a BEL pin  
			else if (bp != null) {
				// Print the attached BEL pin.
				return s + " " + bp;
			}
			else {
				// It must be a "used stub".
				return s + " <stub> ";
			}
		}  // End of rt.isLeaf() block

		else {
			// Otherwise, if it is not a leaf route tree, then iterate across its sink trees and add them
			for (Iterator<RouteTree> it = sinkTrees.iterator(); it.hasNext(); ) {
				RouteTree sink = it.next();

				// If there is only one sink tree then this is just the next wire segment in the route (not a branch).  
				// Don't enclose this in ()'s, just list it as the next wire segment. 
				if (sinkTrees.size() == 1) 
					s += createRoutingString(indnt, n, sink, false, inside);
				// Otherwise, this is a branch of the wire, so enclose it in ( )'s to mark that it represents a branch in the wire.
				else {
					s += "\n" + indnt + "   (" + createRoutingString(indnt + "   ", n, sink, false, inside) + "\n" + indnt + "   )";
				}
			}
			return s;
		}
	}

	public static void summarizeDesign(CellDesign design) {

		System.out.println("Design Summary:");
		int numplaced = 0;
		for (Cell c : design.getCells())
			if (c.getBel() != null)
				numplaced++;
		
		System.out.println("The design has: " + design.getCells().size() + " cells, " + numplaced + " of them are placed.");
		
		int numrouted= 0;
		for (CellNet n: design.getNets())
			if (n.getIntersiteRouteTreeList()!= null)
				numrouted++;
		System.out.println("The design has: " + design.getNets().size() + " nets, "  + numrouted + " of them are routed.");
		
	}
	
	/**
	 * Print out a formatted representation of a cell. Placement is not printed for macro cells.
	 * @param c The internal cell to be pretty printed.
	 * @param cellBelPinMappings Controls whether cell pin to bel pin mappings are printed
	 */
	public static void prettyPrintCell(Cell c, boolean cellBelPinMappings)
	{
		if (c.isMacro()) {
			System.out.println("*Macro (Parent) Cell*");
			System.out.println("Cell: " + c.getName() + " " + 
					c.getLibCell().getName());
		}
		else {
			if (c.isInternal()) {
				System.out.println("\n*Internal Cell*");
				System.out.println("Cell: " + c.getName() + " " + 
						c.getLibCell().getName());
			}
			else {
				System.out.println("\nCell: " + c.getName() + " " + 
						c.getLibCell().getName());
			}
			if (c.isPlaced()) 
				// Print out its placement
				System.out.println("  <<<Placed on: " + c.getBel() + ">>>");
			else System.out.println("  <<<Unplaced>>>");
		}

		// Print out the external pins
		for (CellPin cp : c.getPins()) {
			System.out.println("  Pin: " + cp.getName() + " " + 
					cp.getDirection() + " " + 
					(cp.getNet()!=null?cp.getNet().getName():"<unconnected>"));
			if (!c.isMacro()) {
				if (c.isPlaced()) {
					if (cellBelPinMappings) {
						List<BelPin> bps = cp.getPossibleBelPins();
						if (bps != null) {
							for (BelPin bp2 : bps) {
								System.out.println("    Candidate BelPin Mapping = " + bp2);
							}
						}

						for (BelPin bp1 : cp.getMappedBelPins()) {
							System.out.println("      Actual BelPinMapping = " + bp1);
						}
					}
				}
			}
		}
		// Print the properties for a given cell if there are any
		// For now, properties are strings. 
		for (Property p : c.getProperties()) {
			String s = "  Property: " + p.toString();
			System.out.println(s);
		}
	}
	
}

// Other ideas:
// - Get all connected nets from a cell
// - How to handle pseudo cell pins?
//   + They don't have a backing library cell pin
// - Example of attaching a pseudo pin

