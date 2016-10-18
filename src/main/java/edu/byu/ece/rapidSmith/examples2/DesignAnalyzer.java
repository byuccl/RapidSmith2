package edu.byu.ece.rapidSmith.examples2;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import edu.byu.ece.rapidSmith.interfaces.vivado.TincrCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.edif.util.parse.ParseException;
import edu.byu.ece.rapidSmith.RapidSmithEnv;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.design.subsite.PropertyType;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

public class DesignAnalyzer {
	
	    // part name and cell library  
	public static final String PART_NAME = "xc7a100tcsg324";
	public static final String CANONICAL_PART_NAME = "xc7a100tcsg324";
	public static final String CELL_LIBRARY = "cellLibrary.xml";
	
	private static CellLibrary libCells;
	private static Device device;
	
	public static void classSetup() throws IOException {
		libCells = new CellLibrary(RapidSmithEnv.getDefaultEnv()
				.getPartFolderPath(PART_NAME)
				.resolve(CELL_LIBRARY));
		device = RapidSmithEnv.getDefaultEnv().getDevice(CANONICAL_PART_NAME);
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
		for (CellNet n : design.getNets()) {
			System.out.println("Net: " + n.getName());
			List<CellPin> pins = (List<CellPin>) n.getPins();
			// Print the net's pins
			for (CellPin cp : pins) {
				if (cp == n.getSourcePin())
					System.out.println("  Pin*: " + cp.getCell().getName() + "." + cp.getName());
				else
					System.out.println("  Pin:  " + cp.getCell().getName() + "." + cp.getName());
			}
			// Print out the net's route trees if it has any
			if (n.getIntersiteRouteTreeList() != null) {
				Collection<RouteTree> rts = n.getIntersiteRouteTreeList();
				if (rts.isEmpty())
					System.out.println("  Route Trees: none");
				else
					System.out.println("  Route Trees:");
				for (RouteTree rt: rts) {
					System.out.println("RT.toString() = " + rt);
					String s = formatRouteTree(rt.getFirstSource());
					System.out.println("[ " + s + " ]");
				}
			}
			
		}
	}


	public static String formatRouteTree(RouteTree rt) {
		String s = "";
//		s = rt.getWire().toString() + " ";
		s = rt.toString();
		return s;
//		RouteTree[] trt = (RouteTree[])rt.getSinkTrees().toArray();
//		if (trt.length == 0)
//			return s;
//		if (trt.length == 1)
//			return s + formatRouteTree(trt[0]);
//		for (int i=0;i<trt.length;i++)
//			s = s + "[ " + formatRouteTree(trt[i]) + " ]";
//		return s;
	}


	public static void summarizeDesign(CellDesign design) {

		
	}
	
}

