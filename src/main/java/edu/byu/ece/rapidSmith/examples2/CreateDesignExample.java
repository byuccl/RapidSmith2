package edu.byu.ece.rapidSmith.examples2;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.device.families.Artix7;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple class to illustrate creating designs in RapidSmith2.
 * @author Brent Nelson
 */
public class CreateDesignExample {
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException{

		System.out.println("Starting CreateDesignExample...\n");

		// Load the device file from the directory indicated by the part name
		Device device = RSEnvironment.defaultEnv().getDevice("xc7a100tcsg324");
		System.out.println("Device loaded: " + device.getPartName());
		
		// Load the cell library from the directory indicated by the part name 
		// (directory should be $RAPIDSMITH_PATH/devices/artix7)
		CellLibrary libCells = new CellLibrary(RSEnvironment.defaultEnv()
				.getPartFolderPath("xc7a100tcsg324")
				.resolve("cellLibrary.xml"));
		System.out.println("Cell library loaded: cellLibrary.xml");
		
		// Create a new empty CellDesign for the designated FPGA part
		CellDesign design = new CellDesign("HelloWorld", "xc7a100tcsg324");

		// Create a new cell and add it to the current design.  It is a LUT1 cell.
		Cell invcell = design.addCell(new Cell("lutcell", libCells.get("LUT1")));
		// Set the INIT property for the LUT cell (program the LUT) 
		invcell.updateProperty("INIT", PropertyType.DESIGN, "2'h1");

		// Create a flip flop cell and set its properties 
		Cell ffcell = design.addCell(new Cell("ffcell", libCells.get("FDRE")));   
		ffcell.updateProperty("INIT", PropertyType.DESIGN, "INIT0");
		ffcell.updateProperty("SR", PropertyType.DESIGN, "SRLOW");
		
		// Create IOB's for the circuit's q output and for its clk input
		Cell qbufcell = design.addCell(new Cell("qbuf", libCells.get("OBUF")));
		Cell clkbufcell = design.addCell(new Cell("clkbuf", libCells.get("IBUF")));
		
		// Create top-level ports for these IOB's
		Cell qport= design.addCell(new Cell("qport", libCells.get("OPORT")));
		Cell clkport= design.addCell(new Cell("clkport", libCells.get("IPORT")));
		
		// Create the Q wire and connect it up 
		CellNet qnet = design.addNet(new CellNet("qnet", NetType.WIRE));
		qnet.connectToPin(ffcell.getPin("Q"));
		qnet.connectToPin(invcell.getPin("I0"));
		qnet.connectToPin(qbufcell.getPin("I"));

		// Create and connect the wire between the LUT' output to the flip flop's D input
		CellNet dnet = design.addNet(new CellNet("dnet", NetType.WIRE));
		dnet.connectToPin(ffcell.getPin("D"));
		dnet.connectToPin(invcell.getPin("O"));

		// Create and connect the clock wire between the IOB output and the flip flop's input
		CellNet clknet = design.addNet(new CellNet("clknet", NetType.WIRE));
		clknet.connectToPin(clkbufcell.getPin("O"));
		clknet.connectToPin(ffcell.getPin("C"));
		
		// Create and connect the wires between the ports and their IOB's
		CellNet qportnet= design.addNet(new CellNet("qportnet", NetType.WIRE));
		qportnet.connectToPin(qport.getPin("PAD"));
		qportnet.connectToPin(qbufcell.getPin("O"));
		CellNet clkportnet= design.addNet(new CellNet("clkportnet", NetType.WIRE));
		clkportnet.connectToPin(clkport.getPin("PAD"));
		clkportnet.connectToPin(clkbufcell.getPin("I"));
		
		System.out.println();

		////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Place some of the cells into a slice
		// There are 2 ways to do this.  
		// The first (and simpler) way is when you know exactly where you want to place it.
		// Get the first SLICEL in the device's SLICEL sites
		Site slice = device.getAllSitesOfType(Artix7.SiteTypes.SLICEL).get(0);
		// Place the cell onto the A6LUT of that site
		design.placeCell(invcell, slice.getBel("A6LUT"));
		// Now, let's un-place the cell since we are next going to re-place it using the 2nd method
		design.unplaceCell(invcell);
		
		// The more complex way is a multi-step process:
		// 1. Find a suitable site of the right type (you find the right type by querying the Cell)
		// 2. Find a BEL within that site to place the cell onto (you find the right BEL types for this by querying the Cell)
		//    Get a set of BelId objects which describe the site type/belname pairs where this cell could be placed.
		//    This will consist of pairs like: SLICEL/A6LUT or SLICEM/D6LUT
		List<BelId> anchors = invcell.getPossibleAnchors();
		//    Pull the actual site types out of these BelId objects and collect them into a sorted list without duplicates 
		//    (the resulting list should contain just SLICEL and SLICEM)
		List<SiteType> anchorsitetypes = anchors.stream()
				.map(BelId::getSiteType)
				.distinct()
				.sorted()
				.collect(Collectors.toList());
		// Grab the first primitive site type in the list (should be SLICEL since the list is sorted)
		SiteType sitetype = anchorsitetypes.get(0);
		// Get the first SLICEL in the device's SLICEL sites
		slice = device.getAllSitesOfType(sitetype).get(0);
		// Place the invcell on a suitable LUT (the first one found that is suitable)
		// Get a list of the ones which have the primitive site type matching above (which will be SLICEL or SLICEM)
		anchors = invcell.getPossibleAnchors().stream()
				.filter(t -> t.getSiteType() == sitetype)
				.collect(Collectors.toList());
		// Place the cell on the bel of the first one
		design.placeCell(invcell, slice.getBel(anchors.get(0).getName()));

		// Now, place the ffcell on the the first suitable flip flop found in the site used above (the more complex way)  
		// NOTE: in general it would be good to ensure the LUT and FF in are in corresponding BELS (an ALUT with an AFF, a BLUT with a BFF and so on) 
		//       to ensure good packing and routability.
		// But, in this case it doesn't really matter, the design will be routable.
		anchors = ffcell.getPossibleAnchors().stream()
				.filter(t -> t.getSiteType() == sitetype)
				.collect(Collectors.toList());
		design.placeCell(ffcell, slice.getBel(anchors.get(0).getName()));
		/////////////////////////////////////////////////////////////////////////////////////////////////////////

		// Now, prettyprint what we have created
		System.out.println("\nContents of design:");
		DesignAnalyzer.prettyPrintDesign(design);
		
		System.out.println("\nDone...");
		
	}
}
	
