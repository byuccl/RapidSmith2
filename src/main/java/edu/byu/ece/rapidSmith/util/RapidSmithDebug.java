package edu.byu.ece.rapidSmith.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
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
	 * Debug method that creates a TCL command that can be run in Vivado <br>
	 * to highlight all of the wires in a RouteTree. Used to visually <br>
	 * verify the RouteTree data structure for a specific net.
	 * 
	 * @param net
	 */
	public static String createHighlighWiresTclCommand(CellNet net) {

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
		
		return cmd;
	}
	
	public static String getNetlistDotString(CellDesign design) {
	
		HashMap<String, Integer> nodeIds = new HashMap<String, Integer>();

		StringBuilder dotBuilder = new StringBuilder();
		
		// add the dot file header
		dotBuilder.append("digraph " + design.getName() + "{\n");
		dotBuilder.append("  rankdir=LR\n");
		
		// add cells to the dot file
		for (Cell cell: design.getCells()) {
			
			dotBuilder.append(createCellCluster(cell, nodeIds));
		}
		
		// add nets to the dot file
		// TODO: make this its own function
		for (CellNet net : design.getNets()) {
			
			if (net.getSinkPins().size() == 0) {
				System.out.println(net.getName());
				continue;
			}
			
			CellPin source = net.getSourcePin(); 
			dotBuilder.append("  " + nodeIds.get(source.getCell().getName()) + ":" + nodeIds.get(source.getFullName()) + "->");
			
			int index = 0;
			for (CellPin sink : net.getSinkPins()) {
				boolean isLastIteration = index == net.getSinkPins().size() - 1; 
				
				dotBuilder.append(String.format("%d:%d%s", nodeIds.get(sink.getCell().getName()), nodeIds.get(sink.getFullName()), 
												isLastIteration ? ";\n" : ","));
				index++;
			}
		}
			
		dotBuilder.append("}");
		return dotBuilder.toString();
	}
	
	private static String createCellCluster(Cell c, Map<String, Integer> nodeIds) {

		StringBuilder cellBuilder = new StringBuilder();
		
		int cellId = nodeIds.size();
		cellBuilder.append("  " + cellId + " [shape=record, label=\"{");
		
		// update the nodeID map with the cell and cell id's 
		nodeIds.put(c.getName(), cellId);
		
		// structff [shape=record,label="{q | ck | rst} | <ff> FDRE | {<o> Out}"];

		// add input pin info
		int index = 0;
		
		if (c.getInputPins().size() > 0) {
			cellBuilder.append("{ ");
			for (CellPin cellPin : c.getInputPins()) {
				
				int pinId = nodeIds.size();
				nodeIds.put(cellPin.getFullName(), pinId);
	
				boolean lastIteration = (index == c.getInputPins().size() - 1);
				
				cellBuilder.append(String.format("<%d>%s %s", pinId, cellPin.getName(), lastIteration ? "} |" : "| "));
				
				index++;
			}
		}
		
		// add cell info
		cellBuilder.append(String.format(" { <%d>%s } ", cellId, c.getLibCell().getName()));
		
		// add output pin info
		index = 0;
		if (c.getOutputPins().size() > 0) {
			cellBuilder.append("| { ");
			for (CellPin cellPin : c.getOutputPins()) {
	
				int pinId = nodeIds.size();
				nodeIds.put(cellPin.getFullName(), pinId);
				
				boolean lastIteration = (index == c.getOutputPins().size() - 1);
				
				cellBuilder.append(String.format("<%d>%s %s ", pinId, cellPin.getName(), lastIteration ? "}" : "|"));
				
				index++;
			}
		}
		
		cellBuilder.append("}\"];\n");

		return cellBuilder.toString();
	}
}
