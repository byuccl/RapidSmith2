package edu.byu.ece.rapidSmith.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
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
	 * Debug method that will print a RouteTree in the following format: <br>
	 * Wire Name --> Depth <br>
	 * <br>
	 * Wire1 --> 0 <br>
	 * Wire2 --> 1 <br>
	 * Wire3 --> 2 <br>
	 *   
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
	}
	
	/**
	 * Creates a TCL command that can be run in Vivado to highlight all wires 
	 * in the specified {@link CellNet} object. This command can be used to 
	 * visualize a net in the Vivado GUI.
	 * 
	 * @param net CellNet
	 * @return A formatted TCL command that can be run directly in Vivado.
	 */
	public static String createHighlighWiresTclCommand(CellNet net) {

		return createHighlightWiresTclCommand(net.getIntersiteRouteTreeList());
	}
	
	/**
	 * Creates a TCL command that can be run in Vivado to highlight all wires 
	 * in the specified {@link RouteTree} object. This command can be used to 
	 * visualize a RouteString structure in the Vivado GUI.
	 *  
	 * @param routeTree RouteTree source (i.e. the top of the tree)
	 * @return A formatted TCL command that can be run directly in Vivado.
	 */
	public static String createHighlightWiresTclCommand(RouteTree routeTree) {
		return createHighlightWiresTclCommand(Collections.singletonList(routeTree));
	}
	
	/**
	 * Creates a TCL command that can be run in Vivado to highlight all wires 
	 * in the specified collection of {@link RouteTree} objects. This command can be used to 
	 * visualize a collection RouteString structure in the Vivado GUI.
	 *  
	 * @param routeTrees A collection of RouteTree objects
	 * @return A formatted TCL command that can be run directly in Vivado.
	 */
	public static String createHighlightWiresTclCommand(Collection<RouteTree> routeTrees) {
		String cmd = "select [get_wires {";
		
		for(RouteTree rt : routeTrees) {
			Iterator<RouteTree> it = rt.getFirstSource().iterator();
			
			while (it.hasNext()) {
				Wire w = it.next().getWire();
				cmd += w.getTile().getName() + "/" + w.getWireName() + " "; 
			}
		}
		cmd += "}]";
		
		return cmd;
	}
	
	/**
	 * Prints the full CellPin name and BelPin mappings for each pseudo cell found attached to Cells in the
	 * collection {@code cells}. Each pseudo pin is printed in the following format:   
	 * <br>
	 * "Pseudo Pin: cellName.pseudoPinName -> BelPin1 BelPin2 ... BelPinN" <br>
	 * <br>
	 * The total number of pseudo cell pins in the collection is also printed. 
	 * To print all pseudo pins in a design, pass in the collections returned from 
	 * {@link CellDesign#getCells}.
	 *  
	 * @param cells Collections of Cell objects
	 */
	public static void printPseudoPins(Collection<Cell> cells) {
		int pseudoPinCount = 0;
		for (Cell cell : cells) {
			pseudoPinCount += cell.getPseudoPinCount();
			for (CellPin pin : cell.getPseudoPins()) {
				System.out.print("Pseudo Pin: " + pin.getFullName() + " -> ");
				pin.getMappedBelPins().forEach(System.out::print);
				System.out.println();
			}
		}
		
		System.out.println("Pseudo Pin Count: " + pseudoPinCount);
	}
	
	public static void printNetInfo(CellNet net) {
	
		System.out.println("Net:" + net.getName() );
		System.out.println("Type: " + net.getType());
		System.out.println("Pin Count: " + net.getPins().size());
		System.out.println("Number of routed sinks: " + net.getRoutedSinks().size());
		System.out.println("Route Status: " + net.getRouteStatus() + (net.isIntrasite() ? ", INTRASITE" : "" ));
	}
}
