package edu.byu.ece.rapidSmith.examples2;

import java.io.*;

import edu.byu.ece.rapidSmith.RapidSmithEnv;
import edu.byu.ece.rapidSmith.design.*;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;

public class CreateDesignExample {

/**
 * A simple class to illustrate creating designs in RapidSmith2.
 * @author Brent Nelson
 */

	public static void main(String[] args) throws IOException{

		// Load the cell library from the directory indicated by the part name 
		// (directory should be $RAPIDSMITH_PATH/devices/artix7)
		CellLibrary libCells = new CellLibrary(RapidSmithEnv.getDefaultEnv()
				.getPartFolderPath("xc7a100tcsg324")
				.resolve("cellLibrary.xml"));
		System.out.println("Cell library loaded: cellLibrary.xml");

		// Load the device file from the directory indicated by the part name
		Device device = RapidSmithEnv.getDefaultEnv().getDevice("xc7a100tcsg324");
		System.out.println("Device loaded: xc7a100tcsg324");
		
		// Create a new empty CellDesign for the designated FPGA part
		CellDesign design = new CellDesign("HelloWorld2", "xc7a100tcsg324");

		// Create a new cell and add it to the current design.  It is a LUT1 cell.
		// Then, set the INIT property for the LUT cell (program the LUT) 
		Cell invcell = design.addCell(new Cell("lutcell", libCells.get("LUT1")));
		invcell.updateProperty("INIT", PropertyType.DESIGN, "2'h1");

		// Create a flip flop cell and set its properties 
		Cell ffcell = design.addCell(new Cell("ffcell", libCells.get("FDRE")));   
		ffcell.updateProperty("INIT", PropertyType.DESIGN, "INIT0");
		ffcell.updateProperty("SR", PropertyType.DESIGN, "SRLOW");
		
		// Create IOB's for the circuit's q output and for its clk input
		Cell qbufcell = design.addCell(new Cell("qbuf", libCells.get("OBUF")));
		Cell clkbufcell = design.addCell(new Cell("clkbuf", libCells.get("IBUF")));
		
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

		// Now, prettyprint what we have created
		for (Cell c : design.getCells()) {
			System.out.println("Cell: " + c.getName() + " " + 
					c.getLibCell().getName());
			for (CellPin cp : c.getPins()) {
				System.out.println("  Pin: " + cp.getName() + " " + 
						cp.getDirection() + " " + 
						(cp.getNet()!=null?cp.getNet().getName():"<unconnected>"));
			}
			for (Property p : c.getProperties()) {
				System.out.println("  Property: " + p.getStringKey() + " = " + 
						p.getStringValue());
			}
		}
	}
}
	