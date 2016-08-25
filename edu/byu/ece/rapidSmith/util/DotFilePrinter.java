package edu.byu.ece.rapidSmith.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.SiteType;

/**
 * This class is used to print Dot files to visualize netlists in RapidSmith. <br>
 * I have gotten best results by using GraphViz's dot command to render <br>
 * the dot files created from this class. 
 * 
 * You can learn more about the format of DOT files at the following link: <br>
 * <a href="http://www.graphviz.org/Documentation/dotguide.pdf">Dot Guide</a>
 * 
 * TODO: add assertions throughout the code
 * 
 * @author Thomas Townsend
 *
 */
public class DotFilePrinter {

	private static HashMap<String, Integer> nodeIds;
	private static StringBuilder dotBuilder;
	private static BufferedWriter outputStream; 
	private static final Map<String,String> defaultDotProperties; 
	
	// initialize default dot properties
	static 
	{
		defaultDotProperties = new HashMap<String,String>();
		defaultDotProperties.put("rankdir", "LR");
		defaultDotProperties.put("concentrate", "true");
	}
	
	/**
	 * Prints a DOT file of the design netlist with some default dot settings. <br> 
	 * In order to view the generated dot file, an external tool such as GraphViz <br>
	 * will need to be installed.
	 * 
	 * @param design CellDesign 
	 * @param outputFile Output .dot location (C:/example.dot)
	 * @throws IOException
	 */
	public static void printNetlistDotFile(CellDesign design, String outputFile) throws IOException {
		
		printNetlistDotFile(design, defaultDotProperties, outputFile);
	}
	
	/**
	 * Prints a DOT file of the design netlist with the specified dot properties. <br>
	 * Only use this function if you are familiar with DOT file formats.
	 * 
	 * @param design CellDesign 
	 * @param dotProperties A map of top level graph properties for the dot file
	 * @param outputFile Output .dot location (C:/example.dot)
	 * @throws IOException
	 */
	public static void printNetlistDotFile(CellDesign design, Map<String,String> dotProperties, String outputFile) throws IOException {
		
		outputStream = new BufferedWriter(new FileWriter(outputFile));
		outputStream.write(getNetlistDotString(design, dotProperties));
		outputStream.close();
	}
	
	/**
	 * Create a DOT string of the design netlist with some default dot settings. <br>
	 * 
	 * @param design CellDesign
	 * @return a DOT string
	 */
	public static String getNetlistDotString(CellDesign design) {
		
		return getNetlistDotString(design, defaultDotProperties);
	}
	
	/**
	 * Create a DOT string of the design netlist with the specified DOT properties. <br>
	 * Only use this function if you are familiar with DOT file formats.
	 * 
	 * @param design CellDesign
	 * @param dotProperties A map of top level graph properties for the dot file
	 * @return a DOT string
	 */
	public static String getNetlistDotString(CellDesign design, Map<String,String> dotProperties) {
	
		nodeIds = new HashMap<String, Integer>();
		dotBuilder = new StringBuilder();
			
		// add the dot file header
		dotBuilder.append("digraph " + design.getName() + "{\n");
		formatGraphProperties(dotProperties);
		
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
	 * Prints a DOT file that represents a placed netlist of the design. <br>
	 * Cells placed in the same site are clustered together, and a set of <br>
	 * default DOT properties are used.
	 * 
	 * @param design CellDesign 
	 * @param outputFile Output .dot location (C:/example.dot)
	 * @throws IOException
	 */
	public static void printPlacementDotFile(CellDesign design, String outputFile) throws IOException {
		
		printPlacementDotFile(design, defaultDotProperties, outputFile);
	}
	
	/**
	 * Prints a DOT file that represents a placed netlist of the design with <br>
	 * the specified DOT properties. Cells placed in the same site are clustered <br>
	 * together. Only use this function if you are familiar with DOT file formats.
	 * 
	 * @param design CellDesign 
	 * @param dotProperties A map of top level graph properties for the dot file
	 * @param outputFile Output .dot location (C:/example.dot)
	 * @throws IOException
	 */
	public static void printPlacementDotFile(CellDesign design, Map<String,String> dotProperties, String outputFile) throws IOException {
		
		outputStream = new BufferedWriter(new FileWriter(outputFile));
		outputStream.write(getPlacementDotString(design, dotProperties));
		outputStream.close();
	}
	
	/**
	 * Creates a DOT string that represents a placed netlist of the design. <br>
	 * Cells placed in the same site are clustered together. A set of default <br>
	 * dot properties are used while creating the dot String
	 * 
	 * @param design CellDesign
	 * @return a DOT string
	 */
	public static String getPlacementDotString(CellDesign design) {
		
		return getPlacementDotString(design, defaultDotProperties);
	}
	
	/**
	 * Creates a DOT string that represents a placed netlist of the design. <br>
	 * Cells placed in the same site are clustered together.
	 * 
	 * @param design CellDesign
	 * @param dotProperties A map of top level graph properties for the dot file
	 * @return
	 */
	public static String getPlacementDotString(CellDesign design, Map<String,String> dotProperties) {
		
		nodeIds = new HashMap<String, Integer>();
		dotBuilder = new StringBuilder();
			
		// add the dot file header
		dotBuilder.append("digraph " + design.getName() + "{\n");
		formatGraphProperties(dotProperties);
		
		for (Site site : design.getUsedSites()) {
			formatSiteCluster(design, site);
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
	
	/*
	 * Formats the top-level graph properties
	 * Example: rankDir=LR;
	 */
	private static void formatGraphProperties(Map<String,String> dotProperties) {
		
		for(String key : dotProperties.keySet()) {
			String value = dotProperties.get(key);
			dotBuilder.append(String.format("  %s=%s\n", key, value));
		}
	}
	
	/*
	 * Formats the cell into a DOT record node
	 */
	private static void formatCell(Cell c, StringBuilder builder, String startSpace) {

		int cellId = nodeIds.size();
		builder.append(startSpace + cellId + " [shape=record, label=\"{");
		
		// update the nodeID map with the cell and cell id's 
		nodeIds.put(c.getName(), cellId);
		
		// add input pin info		
		if (c.getInputPins().size() > 0) {
			formatCellPins(c.getInputPins(), builder, "{ ", "} |");
		}
		
		// add cell info
		String lutString = (c.getLibCell().isLut() ? "\\n" + c.getProperty("INIT").getValue() : "");
		builder.append(String.format(" { <%d>%s%s\\n(%s) } ", cellId, c.getName(), lutString, c.getLibCell().getName()));
		
		// add output pin info
		if (c.getOutputPins().size() > 0) {
			formatCellPins(c.getOutputPins(), builder, "| { ", "}");
		}
		
		builder.append("}\"];\n");
	}
	
	/*
	 * Formats a list of cell pins for the corresponding cell record node
	 */
	private static void formatCellPins(Collection<CellPin> cellPins, StringBuilder builder, String start, String finish) {
		
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
	private static void formatNet(CellNet net) {
		
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
	private static void formatSiteCluster(CellDesign design, Site site) {
		
		dotBuilder.append("  subgraph cluster" + site.getName() + "{\n");
		dotBuilder.append("    label=" + site.getName() + ";\n");
	
		// format a slice to make it look good
		if (site.getType() == SiteType.SLICEL || site.getType() == SiteType.SLICEM ) {
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
	
	private static void formatSliceCluster(Site site, Collection<Cell> cellsAtSite) {
		
		StringBuilder lutBuilder = new StringBuilder();
		StringBuilder cellBuilder = new StringBuilder();
		StringBuilder ffBuilder = new StringBuilder();
		
		
		// TODO: test to see if I need to add a unique number to each of the clusters 
		lutBuilder.append("    subgraph clusterLUT {\n" );
		lutBuilder.append("      style=invis\n");
				
		ffBuilder.append("    subgraph clusterFF {\n" );
		ffBuilder.append("      style=invis\n");
		
		for (Cell cell : cellsAtSite) {
			if (cell.getLibCell().isLut()) {
				formatCell(cell, lutBuilder, "      ");
			}
			else if (cell.getAnchor().getName().contains("FF")) {
				formatCell(cell, ffBuilder, "      ");
			}
			else { // basic cell
				formatCell(cell, cellBuilder, "    ");
			}
		}
		
		lutBuilder.append("    }\n");
		ffBuilder.append("    }\n");

		dotBuilder.append(lutBuilder.toString());
		dotBuilder.append(cellBuilder.toString());
		dotBuilder.append(ffBuilder.toString());
	}
}