package edu.byu.ece.rapidSmith.examples;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.*;

import java.util.Collection;

public class PartialDeviceAnalyzer {
	private static Device device;

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("USAGE: PartialDeviceAnalyzer [partialDeviceName]");
			return;
		}

		System.out.println("Starting PartialDeviceAnalyzer...\n");

		// Load the partial device file
		device = RSEnvironment.defaultEnv().getDevice(args[0]);
		printPartialDevice();
		printOOCTile();
	}

	private static void printOOCTile(){
		Tile oocTile = device.getTile("OOC_WIRE_X0Y0");
		System.out.println("Wires entering device:");

		

	}

	/**
	 * Prints out information about the partial device - size, etc.
	 */
	private static void printPartialDevice() {
		System.out.println("Partial Device: " +  device.getPartName());
		System.out.println("  Family: " + device.getFamily());
		System.out.println("  Rows: " + device.getRows());
		System.out.println("  Columns: " + device.getColumns());
		System.out.println("  Tiles: " + device.getTiles().size());
		System.out.println("  Sites: " + device.getSites().size());

		printTiles();
	}

	/**
	 * Prints out info about the tiles, including tile wires and sites
	 */
	private static void printTiles() {
		System.out.println();
		System.out.println("==Tiles:==");

		for (Tile tile : device.getTiles()) {
			System.out.println("Tile: " + tile.getName());
			System.out.println("  Type: " + tile.getType());
			System.out.println("  Row: " + tile.getRow());
			System.out.println("  Col: " + tile.getColumn());
			System.out.println("  Tile X Coordinate: " + tile.getTileXCoordinate());
			System.out.println("  Tile Y Coordinate: " + tile.getTileYCoordinate());

			printTileWires(tile);
			printSites(tile);

		}
	}

	/**
	 * Prints info about the sites in a tile, including type, pins, wires, and bels
	 * @param tile
	 */
	private static void printSites(Tile tile) {
		if (tile.getSites() != null) {
			System.out.println("  ==Sites:==");
			System.out.println("    Total #: " + tile.getSites().length);

			for (Site site : tile.getSites()) {
				System.out.println("    " + site.getName());
				System.out.println("    Site X Coordinate: " + site.getInstanceX());
				System.out.println("    Site Y Coordinate: " + site.getInstanceY());
				printSiteTypes(site);
				printSitePins(site);
				printSiteWires(site);
				printBels(site);
			}
		}
		else {
			System.out.println("# Sites: 0");
		}
	}

	/**
	 * Prints info on the bels of a site, including pins/wires
	 * @param site
	 */
	private static void printBels(Site site) {
		System.out.println("    ==Bels:==");
		System.out.println("      Total #: " + site.getBels().size());

		for (Bel bel : site.getBels()) {
			System.out.println("      Bel:" + bel.getName());
			System.out.println("        Type: " + bel.getType());
			printBelPins(bel);
		}
	}

	/**
	 * Prints source and sink pins of a bel, including corresponding site wires
	 * @param bel
	 */
	private static void printBelPins(Bel bel) {
		System.out.println("        Source Pins:");
		System.out.println("          Total Number: " + bel.getSources().size());
		for (BelPin belPin : bel.getSources()) {
			System.out.println("          " + belPin.getName() + " (Site Wire: " + belPin.getWire().getName() +")");
		}

		System.out.println("        Sink Pins:");
		System.out.println("          Total Number: " + bel.getSinks().size());
		for (BelPin belPin : bel.getSinks()) {
			System.out.println("          " + belPin.getName() + " (Site Wire: " + belPin.getWire().getName() +")");
		}

	}


	/**
	 * Prints info on site pins, including external and internal wires
	 * @param site
	 */
	private static void printSitePins(Site site) {
		System.out.println("    Source Pins:");
		for (SitePin sitePin : site.getSourcePins()) {
			System.out.println("      " + sitePin.getName());
			System.out.println("        External Wire: " + sitePin.getExternalWire().getFullName());
			System.out.println("        Internal Wire: " + sitePin.getInternalWire().getFullName());
		}

		System.out.println("    Sink Pins:");
		for (SitePin sitePin : site.getSinkPins()) {
			System.out.println("    " + sitePin.getName());
			System.out.println("      External Wire: " + sitePin.getExternalWire().getFullName());
			System.out.println("      Internal Wire: " + sitePin.getInternalWire().getFullName());
		}
	}


	/**
	 * Prints default and possible types of a site
	 * @param site
	 */
	private static void printSiteTypes(Site site) {
		System.out.println("    Default Type: " + site.getDefaultType());
		System.out.print("    Possible Types:");
		for (SiteType siteType : site.getPossibleTypes()) {
			System.out.print(" " + siteType.toString());
		}
		System.out.println();
		System.out.print("    Compatible Types:");
		if (site.getCompatibleTypes() != null) {
			for (SiteType siteType : site.getCompatibleTypes()) {
				System.out.print(" " + siteType.toString());
			}
		}

		System.out.println();
	}

	/**
	 * Prints info on wires within a site
	 * @param site
	 */
	private static void printSiteWires(Site site) {
		System.out.println("    Site Wires:");
		System.out.println("      Total #: " + site.getWires().size());
		for (Wire wire : site.getWires()) {
			printSiteWire(wire);
		}
	}

	/**
	 * Prints info about a site wire, including connections.
	 * Print the type of connection.
	 * @param wire
	 */
	private static void printSiteWire(Wire wire) {
		System.out.println("      " + wire.getFullName());
		System.out.println("        # Connections: " + wire.getWireConnections().size());

		for (Connection c : wire.getWireConnections()) {
			String s;
				s = c.getSinkWire().getFullName();
			if (c.isPip())
				System.out.println("        [PIP] " + s);
			else if (c.isRouteThrough()) {
				System.out.println("        [Routethrough] " + s);
			}
			else
				System.out.println("        [nonPIP] " + s);
		}

	}
	
	/**
	 * Prints out the wires and their connections within a tile
	 * @param t A handle to the tile of interest.
	 */
	private static void printTileWires(Tile t) {
		System.out.println("  ==Tile Wires:==");
		if (t.getWireHashMap() != null) {
			// Build each wire and print its statistics
			Collection<Wire> wires = t.getWires();
			System.out.println("    Total #: " + wires.size());
			for (Wire tw : wires) {
				printTileWire(tw);
			}
		}
		else {
			System.out.println("    Total #: 0");
		}
	}

	/**
	 * Prints info about a tile wire, including connections.
	 * Print type of connection and if the other end is a different tile, the offset.
	 * @param w
	 */
	private static void printTileWire(Wire w) {
		Tile t = w.getTile();
		System.out.println("      " + w.getFullName());
		System.out.println("        Connections: ");
		System.out.println("          Total #: " + w.getWireConnections().size());

		for (Connection c : w.getWireConnections()) {
			String s;
			if (c.getSinkWire().getTile() != t) {	 
				int xoff = c.getSinkWire().getTile().getColumn() - t.getColumn() ;	 
				int yoff = c.getSinkWire().getTile().getRow() - t.getRow() ;	 
				s = c.getSinkWire().getTile().toString() + "/" + c.getSinkWire().getName() + " [" + yoff + "," + xoff + "]";
			}	
			else	 
				s = c.getSinkWire().getName();
			if (c.isPip())
				System.out.println("          [PIP] " + s);
			else if (c.isRouteThrough()) {
				System.out.println("          [Routethrough] " + s);
			}
			else
				System.out.println("          [nonPIP] " + s);
		}
	}	
	
}
