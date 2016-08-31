package edu.byu.ece.rapidSmith.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringJoiner;

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

	private HashMap<String, Integer> nodeIds;
	private StringBuilder dotBuilder;
	private BufferedWriter outputStream; 
	private final Map<String,String> dotProperties; 
	private CellDesign design;
	
	/**
	 * Creates a new DotFilePrinter for the specified CellDesign
	 * 
	 * @param design CellDesign
	 */
	public DotFilePrinter(CellDesign design) {
		this.design = design;
		
		// initialize dot configuration with default options
		dotProperties = new HashMap<String,String>();
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
	
		nodeIds = new HashMap<String, Integer>();
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
	 * @param design CellDesign 
	 * @param dotProperties A map of top level graph properties for the dot file
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
	 * @param design CellDesign
	 * @param dotProperties A map of top level graph properties for the dot file
	 * @return
	 */
	public String getPlacementDotString() {
		
		nodeIds = new HashMap<String, Integer>();
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
		
		nodeIds = new HashMap<String, Integer>();
		dotBuilder = new StringBuilder();
				
		// add the dot file header
		dotBuilder.append("digraph " + design.getName() + "{\n");
		formatGraphProperties();
		
		// create blank nodes
		for (Cell cell: design.getCellsAtSite(site)) {
			for (CellNet net : cell.getNetList()) {
				CellPin source = net.getSourcePin();
				
				if (source.getCell().isVccSource() || source.getCell().isGndSource() || net.getSinkPins().size() == 0 || net.getSinkPins().size() > 10) {
					continue;
				}
				
				if (!isCellPinInSite(source, site) && !nodeIds.containsKey(source.getFullName())) {
					int id = nodeIds.size();
					nodeIds.put(source.getFullName(), id);
					dotBuilder.append("  " + id + " [style=invis];\n");
				}
				
				for (CellPin cellPin : net.getSinkPins()) {
					if (!isCellPinInSite(cellPin, site) && !nodeIds.containsKey(cellPin.getFullName())) {
						int id = nodeIds.size();
						nodeIds.put(cellPin.getFullName(), id);
						dotBuilder.append("  " + id + " [style=invis];\n");
					}
				}
			}
		}
				
		formatSiteCluster(site);

		HashSet<CellNet> processedNets = new HashSet<CellNet>();
		
		for (Cell cell: design.getCellsAtSite(site)) {
			
			if (cell.isVccSource() || cell.isGndSource()) {
				continue;
			}
			
			for (CellNet net: cell.getNetList()) {
				
				boolean sourceInSite = false;
				
				if (processedNets.contains(net) || net.getSinkPins().size() == 0 || net.getSinkPins().size() > 10) {
					continue;
				}
								
				CellPin source = net.getSourcePin();
				
				if (source == null || source.getCell().isVccSource() || source.getCell().isGndSource()) {
					continue;
				}
				
				if (!isCellPinInSite(source, site)) {
					dotBuilder.append(String.format("  %d:e->", nodeIds.get(source.getFullName())));
				}
				else {
					sourceInSite = true; 
					dotBuilder.append(String.format("  %d:%d:e->", nodeIds.get(source.getCell().getName()), 
															   nodeIds.get(source.getFullName())));
				}
				
				StringJoiner joiner = new StringJoiner(",");
				for (CellPin sinkPin : net.getSinkPins()) {
					if (sinkPin.getCell().getAnchorSite().equals(site)) {
						joiner.add(String.format("%d:%d", nodeIds.get(sinkPin.getCell().getName()), 
														  nodeIds.get(sinkPin.getFullName())));
					}
					else if (sourceInSite) {
						joiner.add(nodeIds.get(sinkPin.getFullName()).toString() + ":w");
					}
				}
				
				dotBuilder.append(joiner.toString() + ";\n");
				processedNets.add(net);
			}
		}
		
		dotBuilder.append("}");
		
		return dotBuilder.toString();
	}
	
	private boolean isCellPinInSite(CellPin pin, Site site) { 
		return (pin == null) ? false : pin.getCell().getAnchorSite().equals(site);
	}
	
	/*
	 * Formats the top-level graph properties
	 * Example: rankDir=LR;
	 */
	private void formatGraphProperties() {
		
		for(String key : dotProperties.keySet()) {
			String value = dotProperties.get(key);
			dotBuilder.append(String.format("  %s=%s\n", key, value));
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
		
		// add cell info
		String lutString = (c.getLibCell().isLut() ? "\\n" + c.getProperty("INIT").getValue() : "");
		builder.append(String.format(" { <%d>%s%s\\n%s\\n(%s) } ", cellId, c.getName(), lutString, c.getAnchor().getName(), c.getLibCell().getName()));
		
		// add output pin info
		if (c.getOutputPins().size() > 0) {
			formatCellPins(c.getOutputPins(), builder, "| { ", "}");
		}
		
		builder.append("}\"];\n");
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
	
		// format a slice to make it look good
		if (site.getType() == SiteType.get("SLICEL") || site.getType() == SiteType.get("SLICEM") ) {
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
