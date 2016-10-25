package edu.byu.ece.rapidSmith.examples2;

import java.io.*;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.*;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;

public class CreateDesignExample {

/**
 * A simple class to illustrate creating designs in RapidSmith2.
 * @author Brent Nelson
 */

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException{

		System.out.println("Starting CreateDesignExample...\n");

		// Load the cell library from the directory indicated by the part name 
		// (directory should be $RAPIDSMITH_PATH/devices/artix7)
		CellLibrary libCells = new CellLibrary(RSEnvironment.defaultEnv()
				.getPartFolderPath("xc7a100tcsg324")
				.resolve("cellLibrary.xml"));
		System.out.println("Cell library loaded: cellLibrary.xml");

		// Load the device file from the directory indicated by the part name
		Device device = RSEnvironment.defaultEnv().getDevice("xc7a100tcsg324");
		System.out.println("Device loaded: xc7a100tcsg324");
		
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
		
		// Place some of the cells into a slice
		// Get the first SLICEL in the device
		Site slice = device.getAllSitesOfType(SiteType.SLICEL)[0];
		
		// Place the invcell on the A5LUT
		design.placeCell(invcell, slice.getBel("A5LUT"));

		// Place the ffcell on the AFF
		design.placeCell(ffcell, slice.getBel("AFF"));

		// Now, prettyprint what we have created
		DesignAnalyzer.prettyPrintDesign(design);
		
		System.out.println("\nDone...");
		
	}
}
	
