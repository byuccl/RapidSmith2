package edu.byu.ece.rapidSmith.examples2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.device.Connection;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.SitePin;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileWire;
import edu.byu.ece.rapidSmith.device.Wire;

public class DeviceAnalyzer {
	
	static Device device;

	public static void main(String[] args) throws IOException{

		msg("Starting DeviceAnalyzer...\n");

		// Load the cell library
		CellLibrary libCells = new CellLibrary(RSEnvironment.defaultEnv()
				.getPartFolderPath("xc7a100tcsg324")
				.resolve("cellLibrary.xml"));

		// Load the device file
		device = RSEnvironment.defaultEnv().getDevice("xc7a100tcsg324");
		printTileWires(device.getTile("CLBLL_R_X17Y181"));
		printTileWires(device.getTile("INT_R_X17Y181"));
	}
	
	private static void printTileWires(Tile t) {

		msg("\n===========================\nSelected tile " + t.toString());
		msg("Its row and column numbers are: [" + t.getRow() + ", " + t.getColumn() + "]");
		
		// Build each wire and print its statistics
		HashSet<Integer> wires = (HashSet<Integer>) t.getWires();
		msg("There are " + wires.size() + " wires in this tile...");
		for (Integer i : wires) {
			TileWire tw = new TileWire(t, i);
			Collection<Connection> conns = tw.getWireConnections();
			msg("Wire " + tw.getWireName() + " has " + conns.size() + " connections.");
			for (Connection c : conns) {
				String s;
				if (c.getSinkWire().getTile() != t) {
					int xoff = c.getSinkWire().getTile().getColumn() - t.getColumn() ;
					int yoff = c.getSinkWire().getTile().getRow() - t.getRow() ;
					s = c.getSinkWire().getTile().toString() + "/" + c.getSinkWire().getWireName() + " [" + yoff + "," + xoff + "]";
				}
				else
					s = c.getSinkWire().getWireName();
				if (c.isPip())
					msg("  [pip] " + s);
				else if (c.isRouteThrough())
					msg("  [routethrough] " + s);
				else if (c.isWireConnection())
					msg("  [wireconn] " + s);
				else
					msg("  " + s);
			}
		}
		msg("Done...");
	}
	
	private static void msg(String s) {
		System.out.println(s);
	}

}
