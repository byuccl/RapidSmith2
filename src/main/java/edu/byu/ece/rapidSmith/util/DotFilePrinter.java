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

package edu.byu.ece.rapidSmith.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.families.FamilyInfo;
import edu.byu.ece.rapidSmith.device.families.FamilyInfos;

/**
 * This class is used to print Dot files to visualize netlists in RapidSmith. <br>
 * I have gotten best results by using GraphViz's dot command to render <br>
 * the dot files created from this class. 
 * 
 * You can learn more about the format of DOT files at the following link: <br>
 * <a href="http://www.graphviz.org/Documentation/dotguide.pdf">Dot Guide</a>
 * 
 * TODO: add max fanout to constructor
 * TODO: add assertions throughout the code
 * 
 * @author Thomas Townsend
 *
 */
public class DotFilePrinter {

	private HashMap<String, Integer> nodeIds;
	private StringBuilder dotBuilder;
	private BufferedWriter outputStream; 
	private final Map<String,String> dotProperties; 
	private final CellDesign design;
	private static final int MAX_FANOUT = 10;
	
	/**
	 * Creates a new DotFilePrinter for the specified CellDesign
	 * 
	 * @param design CellDesign
	 */
	public DotFilePrinter(CellDesign design) {
		this.design = design;

		// initialize dot configuration with default options
		dotProperties = new HashMap<>();
		dotProperties.put("rankdir", "LR");
		dotProperties.put("concentrate", "true");
	}
	
	/**
	 * Creates a new DotFilePrinter for the specified CellDesign and top-level graph <br>
	 * properties. Only use this function if you are familiar with DOT file formats
	 * 
	 * @param design CellDesign
	 * @param dotProperties Map of top-level graph properties for the DOT file
	 */
	public DotFilePrinter(CellDesign design, Map<String,String> dotProperties) {
		this.design = design;
		this.dotProperties = dotProperties;
	}
		
	/**
	 * Prints a DOT file of the design netlist with the configured dot properties. <br>
	 * 
	 * @param outputFile Output .dot location (C:/example.dot)
	 * @throws IOException
	 */
	public void printNetlistDotFile(String outputFile) throws IOException {
		
		outputStream = new BufferedWriter(new FileWriter(outputFile));
		outputStream.write(getNetlistDotString());
		outputStream.close();
	}
		
	/**
	 * Create a DOT string of the design netlist with the configured DOT properties. <br>
	 * 
	 * @return a DOT string
	 */
	public String getNetlistDotString() {
	
		nodeIds = new HashMap<>();
		dotBuilder = new StringBuilder();
			
		// add the dot file header
		dotBuilder.append("digraph " + design.getName() + "{\n");
		formatGraphProperties();
		
		// add cells to the dot file
		for (Cell cell: design.getCells()) {
			formatCell(cell, dotBuilder, "  ");			
		}
		
		// add nets to the dot file
		for (CellNet net : design.getNets()) {
			
			// only print small fanout nets for now to produce cleaner results
			// TODO: add option for this?
			if (net.getFanOut() == 0 || net.getFanOut() > 10) {
				continue;
			}
			
			formatNet(net);
		}
		
		dotBuilder.append("}");
		
		return dotBuilder.toString();
	}
		
	/**
	 * Prints a DOT file that represents a placed netlist of the design with <br>
	 * the specified DOT properties. Cells placed in the same site are clustered <br>
	 * together.
	 * 
	 * @param outputFile Output .dot location (C:/example.dot)
	 * @throws IOException
	 */
	public void printPlacementDotFile(String outputFile) throws IOException {
		
		outputStream = new BufferedWriter(new FileWriter(outputFile));
		outputStream.write(getPlacementDotString());
		outputStream.close();
	}
	
	/**
	 * Creates a DOT string that represents a placed netlist of the design. <br>
	 * Cells placed in the same site are clustered together.
	 * 
	 * @return
	 */
	public String getPlacementDotString() {
		
		nodeIds = new HashMap<>();
		dotBuilder = new StringBuilder();
			
		// add the dot file header
		dotBuilder.append("digraph " + design.getName() + "{\n");
		formatGraphProperties();
		
		for (Site site : design.getUsedSites()) {
			formatSiteCluster(site);
		}
		
		// add nets to the dot file
		for (CellNet net : design.getNets()) {
			
			// only print small fanout nets for now to produce cleaner results
			// TODO: add option for this?
			if (net.getFanOut() == 0 || net.getFanOut() > 10) {
				continue;
			}
			
			formatNet(net);
		}
		
		dotBuilder.append("}");
		
		return dotBuilder.toString();
	}
	
	/**
	 * 
	 * @param outputFile
	 * @param site
	 * @throws IOException
	 */
	public void printSiteDotString(String outputFile, Site site) throws IOException {
		outputStream = new BufferedWriter(new FileWriter(outputFile));
		outputStream.write(getSiteDotString(site));
		outputStream.close();
	}
	
	/**
	 * TODO: clean this up, but committing it to get it to Dr. Nelson for FPL demo
	 * @param site Site to print dot string for
	 * @return
	 */
	public String getSiteDotString(Site site) {
		
		nodeIds = new HashMap<>();
		dotBuilder = new StringBuilder();
				
		// add the dot file header
		dotBuilder.append("digraph " + design.getName() + "{\n");
		
		formatGraphProperties();
		formatBlankNodes(site);
		formatSiteCluster(site);
		formatSiteNets(site);
		
		dotBuilder.append("}");
		
		return dotBuilder.toString();
	}
	
	/**
	 * Creates a DOT string of the intersite route of the specified {@link CellNet}.
	 * This function assumes that a RouteTree has been assigned to the net.
	 * 
	 * @param net {@link CellNet}
	 */
	public static String getRouteTreeDotString(CellNet net) {
		
		// initialize function
		Queue<RouteTree> rtQueue = new LinkedList<RouteTree>();
		Map<RouteTree, Integer> nodeIds = new HashMap<>(); 
		StringBuilder builder = new StringBuilder();
		RouteTree route = net.getIntersiteRouteTree();
		
		if(route == null) {
			throw new Exceptions.DesignAssemblyException("Net needs to have a RouteTree to generate dot string");
		}
		
		// append the header
		builder.append("digraph \"" + net.getName() + "\"{\n");
				
		// create the unique ID list for RouteTrees
		route.iterator().forEachRemaining(rt -> nodeIds.put(rt, nodeIds.size()));
		
		// print the node and edge information for the RouteTree
		rtQueue.add(route);

		while (!rtQueue.isEmpty()) {
			RouteTree tmp = rtQueue.poll();
			
			builder.append(String.format(" %d [label=\"%s\"]\n", nodeIds.get(tmp), tmp.getWire().getFullWireName()));
			
			// only print edges if the route tree has any
			if (!tmp.isLeaf()) {
				for (RouteTree sink : tmp.getSinkTrees()) {
					String edgeColor = sink.getConnection().isPip() ? "red" : "black";
					builder.append(String.format(" %d->%d [color=\"%s\"]\n", nodeIds.get(tmp), nodeIds.get(sink), edgeColor));
					rtQueue.add(sink);
				}
			} 
			else {
				// TODO: add a node for site pins?
			}
		}
		
		builder.append("}");
		return builder.toString();
	}
	
	private boolean isCellPinInSite(CellPin pin, Site site) { 
		return pin != null && pin.getCell().getSite().equals(site);
	}
	
	/*
	 * Formats the top-level graph properties
	 * Example: rankDir=LR;
	 */
	private void formatGraphProperties() {
		
		for(String key : dotProperties.keySet()) {
			String value = dotProperties.get(key);
			dotBuilder.append(String.format("  %s=%s;\n", key, value));
		}
	}
	
	/*
	 * Formats the cell into a DOT record node
	 */
	private void formatCell(Cell c, StringBuilder builder, String startSpace) {

		int cellId = nodeIds.size();
		builder.append(startSpace + cellId + " [shape=record, label=\"{");
		
		// update the nodeID map with the cell and cell id's 
		nodeIds.put(c.getName(), cellId);
		
		// add input pin info		
		if (c.getInputPins().size() > 0) {
			formatCellPins(c.getInputPins(), builder, "{ ", "} |");
		}
		
		// add cell info .. TODO: update this to print all properties of a cell
		String lutString = (c.getLibCell().isLut() ? "\\n" + c.getProperties().getValue("INIT") : "");
		builder.append(String.format(" { <%d>%s%s\\n%s\\n(%s) } ", cellId, getAbbreviatedCellName(c), lutString, c.getBel().getName(), c.getLibCell().getName()));
		
		// add output pin info
		if (c.getOutputPins().size() > 0) {
			formatCellPins(c.getOutputPins(), builder, "| { ", "}");
		}
		
		builder.append("}\"];\n");
	}
	
	/*
	 * Shortens a cell name if necessary to improve readability of the graph
	 */
	private String getAbbreviatedCellName(Cell cell) {
		
		String cellName = cell.getName();
		return cellName.length() > 20 ? cellName.substring(0, 19) + "..." : cellName;
	}
	
	/*
	 * Formats a list of cell pins for the corresponding cell record node
	 */
	private void formatCellPins(Collection<CellPin> cellPins, StringBuilder builder, String start, String finish) {
		
		builder.append(start);
		
		int index = 0;
		for (CellPin cellPin : cellPins) {
			
			int pinId = nodeIds.size();
			nodeIds.put(cellPin.getFullName(), pinId);
			
			boolean lastIteration = (index == cellPins.size() - 1);
			builder.append(String.format("<%d>%s %s", pinId, cellPin.getName(), lastIteration ? finish : "| "));
			
			index++;
		}
	}
	
	/*
	 * Formats a net into an edge list for the dot file
	 */
	private void formatNet(CellNet net) {
		
		CellPin source = net.getSourcePin(); 		
		dotBuilder.append(String.format("  %d:%d:e->", nodeIds.get(source.getCell().getName()), nodeIds.get(source.getFullName())));
		
		int index = 0;
		for (CellPin sink : net.getSinkPins()) {
			boolean isLastIteration = index == net.getSinkPins().size() - 1; 
			
			dotBuilder.append(String.format("%d:%d%s", nodeIds.get(sink.getCell().getName()), 
													   nodeIds.get(sink.getFullName()), 
													   isLastIteration ? ";\n" : ","));
			index++;
		}
	}
	
	/*
	 * Formats the site into a dot subgraph with all the cells placed at that site
	 */
	private void formatSiteCluster(Site site) {
		
		dotBuilder.append("  subgraph cluster" + site.getName() + "{\n");
		dotBuilder.append("    label=" + site.getName() + ";\n");
		FamilyInfo familyInfo = FamilyInfos.get(design.getFamily());

		// format a slice to make it look good
		if (familyInfo.sliceSites().contains(site.getType())) {
			formatSliceCluster(site, design.getCellsAtSite(site));
		}
		else {
			// create a cluster for the site 
			for (Cell cell : design.getCellsAtSite(site)) {			
				formatCell(cell, dotBuilder, "    ");
			}
		}
			
		// end of subgraph
		dotBuilder.append("  }\n");
	}
	
	private void formatSliceCluster(Site site, Collection<Cell> cellsAtSite) {
		
		StringBuilder lutBuilder = new StringBuilder();
		StringBuilder cellBuilder = new StringBuilder();
		StringBuilder ffBuilder = new StringBuilder();
		 
		lutBuilder.append("    subgraph clusterLUT {\n" );
		lutBuilder.append("      style=invis\n");
		
		cellBuilder.append("    subgraph clusterMid {\n" );
		cellBuilder.append("      style=invis\n");
		
		ffBuilder.append("    subgraph clusterFF {\n" );
		ffBuilder.append("      style=invis\n");
				
		Cell lutCell = null;
		Cell middleCell = null;
		Cell ffCell = null;
		
		for (Cell cell : cellsAtSite) {
			if (cell.getLibCell().isLut()) {
				formatCell(cell, lutBuilder, "      ");
				lutCell = cell;
			}
			else if (cell.getBel().getName().contains("FF")) {
				formatCell(cell, ffBuilder, "      ");
				ffCell = cell;
			}
			else { // basic cell
				formatCell(cell, cellBuilder, "      ");
				middleCell = cell;
			}
		}
		
		lutBuilder.append("    }\n");
		cellBuilder.append("    }\n");		
		ffBuilder.append("    }\n");

		dotBuilder.append(lutBuilder.toString());
		dotBuilder.append(cellBuilder.toString());
		dotBuilder.append(ffBuilder.toString());
		
		formatSliceLayout(lutCell, middleCell, ffCell);
	}
	
	/*
	 * Adds invisible edges so that the LUTs are displayed to the left, the FF are displayed
	 * to the right, and everything else is displayed in the middle for a slice.\
	 * TODO: may have to add an invisible node between the two
	 */
	private void formatSliceLayout(Cell lutCell, Cell middleCell, Cell ffCell) {
		// add invisible edges to create desired layout
		if (lutCell != null) {		
			if (middleCell != null) {
				dotBuilder.append(String.format("    %d->%d [lhead=clusterLUT, ltail=clusterMid, style=invis];\n", 
												nodeIds.get(lutCell.getName()), nodeIds.get(middleCell.getName())));
			}
			else if (ffCell != null) {
				dotBuilder.append(String.format("    %d->%d [lhead=clusterLUT, ltail=clusterFF style=invis];\n", 
												nodeIds.get(lutCell.getName()), nodeIds.get(ffCell.getName())));
			}
		}
		else if (ffCell != null && middleCell != null) {
			dotBuilder.append(String.format("    %d->%d [lhead=clusterMid, ltail=clusterFF, style=invis];\n", 
											nodeIds.get(middleCell.getName()), nodeIds.get(ffCell.getName())));
		}
	}
	
	/*
	 * 
	 */
	private void formatBlankNodes(Site site) {
		
		for (CellNet net : getUniqueNetsAtSite(site)) {		
			
			if(!isNetQualifiedForSiteView(net)){
				continue;
			}

			CellPin source = net.getSourcePin();
			if (!isCellPinInSite(source, site)) {
				createBlankNode(source.getFullName());
			}
			
			for (CellPin cellPin : net.getSinkPins()) {
				if (!isCellPinInSite(cellPin, site)) {
					createBlankNode(cellPin.getFullName());
				}
			}
		}
	}
	
	private Set<CellNet> getUniqueNetsAtSite(Site site) {
		return design.getCellsAtSite(site).stream()
				.flatMap(x->x.getNetList().stream())
				.collect(Collectors.toSet());
	}
	
	private boolean isNetQualifiedForSiteView(CellNet net) {
		
		CellPin source = net.getSourcePin(); 
		return source != null &&
				!source.getCell().isGndSource() &&
				!source.getCell().isVccSource() &&
				net.getSinkPins().size() > 0 &&
				net.getSinkPins().size() < MAX_FANOUT; 
	}
	
	private boolean createBlankNode(String nodeName) {
		
		if(nodeIds.containsKey(nodeName)) {
			return false;
		}
	
		int id = nodeIds.size();
		nodeIds.put(nodeName, id);
		dotBuilder.append("  " + id + " [style=invis];\n");
		
		return true;
	}
	
	/*
	 * This function will only print the nets attached to the specified site
	 */
	private void formatSiteNets(Site site) {
		
		for (CellNet net : getUniqueNetsAtSite(site)) {
		
			if (!isNetQualifiedForSiteView(net)) {
				continue;
			}
			
			// format source
			boolean sourceInSite = false;
			CellPin source = net.getSourcePin();
			if (isCellPinInSite(source, site)) {
				sourceInSite = true; 
				dotBuilder.append(String.format("  %d:%d:e->", nodeIds.get(source.getCell().getName()), 
															   nodeIds.get(source.getFullName())));
			}
			else {
				dotBuilder.append(String.format("  %d:e->", nodeIds.get(source.getFullName())));
			}
			
			// format sinks
			StringJoiner joiner = new StringJoiner(",");
			for (CellPin sinkPin : net.getSinkPins()) {
				if (isCellPinInSite(sinkPin, site)) {
					joiner.add(String.format("%d:%d", nodeIds.get(sinkPin.getCell().getName()), 
													  nodeIds.get(sinkPin.getFullName())));
				}
				else if (sourceInSite) {
					joiner.add(nodeIds.get(sinkPin.getFullName()).toString() + ":w");
				}
			}
			
			dotBuilder.append(joiner.toString() + ";\n");
		}
	}
 }
