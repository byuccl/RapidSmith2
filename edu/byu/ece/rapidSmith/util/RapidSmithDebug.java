package edu.byu.ece.rapidSmith.util;

import java.util.Iterator;

import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.Wire;

/**
 * This class is for general purpose debug methods that users <br>
 * of RapidSmith might find helpful. <br> 
 * 
 * @author Thomas Townsend
 *
 */
public final class RapidSmithDebug {
		
	/**
	 * Debug method that will print a RouteTree in the following format
	 * 
	 * Wire 1 --> 0
	 * Wire 2 --> 1
	 * Wire 3 --> 2
	 *  
	 * TODO: print using tabs for levels? 
	 * @param rt RouteTree to print
	 */
	public static void printRouteTree(RouteTree rt) {
		printRouteTree(rt, 0);
	}
	
	/*
	 * Recursive method to print a RouteTree data structure
	 */
	private static void printRouteTree(RouteTree rt, int level) {
		Wire w = rt.getWire();
		System.out.println(w.getTile() + "/" + w.getWireName() + "--> " + level);
			
		level++;
		for(RouteTree r: rt.getSinkTrees()) {
			printRouteTree(r, level);
		}
		level--; 
		return;
	}
		
	/**
	 * Debug method that creates a TCL command that can be run in Vivado
	 * to highlight all of the wires in a RouteTree. Used to visually
	 * verify the RouteTree data structure for a specific net.
	 * @param net
	 */
	public static void createHighlighWiresTclCommand(CellNet net) {

		String cmd = "select [get_wires {";
		
		for(RouteTree rt : net.getRouteTrees()) {
			Iterator<RouteTree> it = rt.getFirstSource().iterator();
			// System.out.println(rt.getFirstSource().getWire());
			while (it.hasNext()) {
				Wire w = it.next().getWire();
				cmd += w.getTile().getName() + "/" + w.getWireName() + " "; 
			}
		}
		cmd += "}]";
		System.out.println(cmd);
	}
}
