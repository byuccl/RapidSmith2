package edu.byu.ece.rapidSmith.examples2;

import java.io.IOException;
import java.util.Collection;

import org.jdom2.JDOMException;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.Connection;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Tile;

import edu.byu.ece.rapidSmith.device.TileWire;


import java.util.HashSet;

import edu.byu.ece.rapidSmith.device.Wire;

public class DeviceAnalyzer {
	
	static Device device;

	public static void main(String[] args) throws IOException, JDOMException {

		msg("Starting DeviceAnalyzer...\n");

		// Load the device file
		device = RSEnvironment.defaultEnv().getDevice("xc7a100tcsg324");
		
		// Grab a few tiles and print out their wires
		printTileWires(device.getTile("CLBLL_R_X17Y181"));
		printTileWires(device.getTile("INT_R_X17Y181"));
	}
	
	/**
	 * Prints out the wires and their connections within a tile
	 * @param t A handle to the tile of interest.
	 */
	private static void printTileWires(Tile t) {

		msg("\n===========================\nSelected tile " + t.toString());
		msg("Its row and column numbers are: [" + t.getRow() + ", " + t.getColumn() + "]");
		
		/*
		// Build each wire and print its statistics
		Collection<Wire> wires = t.getWires();
		msg("There are " + wires.size() + " wires in this tile...");
		for (Wire tw : wires) {
			printWire(tw);
		}
		*/
		msg("Done...");
	}
	
	private static void printWire(Wire w) {
		Tile t = w.getTile();
		msg("Wire " + w.getFullWireName() + " has " + w.getWireConnections().size() + " connections."); 

		/*
		 * A wire has a number of connections to other wires. 
		 * 
		 * These are essentially of two types. The first is a programmable 
		 * connection, also known as a PIP. The other is a non-programmable
		 * connection and essentially is the name of the other end of the
		 * wire (that is, each end of a wire typically has a different
		 * name).
		 * 
		 * For many wires, the other end of the connection is in the same
		 * tile. For others, it is in a different tile.
		 * 
		 * The following code will print out the various wire connections,
		 * marking whether they are PIPs or not. Additionally, if the other
		 * end of the connection is in a different tile, it will print the
		 * offset as well.
		 *  
		 */ 
		for (Connection c : w.getWireConnections()) {	 
			String s;
			if (c.getSinkWire().getTile() != t) {	 
				int xoff = c.getSinkWire().getTile().getColumn() - t.getColumn() ;	 
				int yoff = c.getSinkWire().getTile().getRow() - t.getRow() ;	 
				s = c.getSinkWire().getTile().toString() + "/" + c.getSinkWire().getWireName() + " [" + yoff + "," + xoff + "]";	 
			}	
			else	 
				s = c.getSinkWire().getWireName();	 
			if (c.isPip())	
				msg("  [PIP] " + s);	 
			else if (c.isWireConnection())	 
				msg("  [nonPIP] " + s);	
			else	
				msg("  [???] " + s); 
		}
	}	
	
	private static void msg(String s) {
		System.out.println(s);
	}

}
