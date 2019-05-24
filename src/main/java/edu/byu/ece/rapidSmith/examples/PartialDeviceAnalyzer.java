package edu.byu.ece.rapidSmith.examples;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.*;

import java.util.Collection;

public class PartialDeviceAnalyzer {
	private static Device device;
	private static FamilyType familyType;

	public static void main(String[] args) {
		// TODO: Add an option to create a new partial device and then analyze it.
		if (args.length != 1) {
			System.out.println("USAGE: PartialDeviceAnalyzer [partialDeviceName]");
			return;
		}

		System.out.println("Starting PartialDeviceAnalyzer...\n");

		// Load the partial device file
		device = RSEnvironment.defaultEnv().getDevice(args[0]);
		familyType = device.getFamily();
		printPartialDevice();
		printOOCTile();
	}

	/**
	 * Prints info on the OOC Tile. In a partial device, an extra column in added to the device.
	 *  The purpose of this column is to represent out-of-context wires for the partial device.
	 *  These wires may start <b>within</b> the partial device and leave the partial device boundaries, or
	 *  they may start <b>outside</b> the partial device boundaries, but enter the partial device.
	 *  These OOC wires are all tile wires that are added to the "OOC_WIRE" tile. All other tiles in
	 *  the rightmost column are set to be of type NULL so they take up as little memory as possible.
	 */
	private static void printOOCTile(){
		Tile oocTile = device.getTile("OOC_WIRE_X0Y0");
		System.out.println("*Tiles Outside the Partial Device:*");
		printTileWires(oocTile);
	}

	/**
	 * Prints out information about the partial device - size, etc.
	 */
	private static void printPartialDevice() {
		System.out.println("Partial Device: " +  device.getPartName());
		System.out.println("  Family: " + familyType);
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
		System.out.println("*Tiles:*");

		for (Tile tile : device.getTiles()) {
			if (tile.getRow() == 0) {
				// The tile is in the added column for partial devices
				continue;
			}

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
			System.out.println("  *Sites (" + tile.getSites().length + ")*");

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
			System.out.println("  *Sites (0)*");
		}
	}

	/**
	 * Prints info on the bels of a site, including pins/wires
	 * @param site
	 */
	private static void printBels(Site site) {
		System.out.println("    *Bels (" + site.getBels().size() + ")*");

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
		if (bel.getSources().size() != 0) {
			System.out.println("        *Source Pins (" + bel.getSources().size() + ")*");
			for (BelPin belPin : bel.getSources()) {
				System.out.println("          " + belPin.getName() + " (Site Wire: " + belPin.getWire().getName() + ")");
			}
		}

		if (bel.getSinks().size() != 0) {
			System.out.println("        *Sink Pins (" + bel.getSinks().size() + ")*");
			for (BelPin belPin : bel.getSinks()) {
				System.out.println("          " + belPin.getName() + " (Site Wire: " + belPin.getWire().getName() +")");
			}
		}
	}

	/**
	 * Prints info on site pins, including external and internal wires
	 * @param site
	 */
	private static void printSitePins(Site site) {
		System.out.println("    *Source Pins (" + site.getSourcePins().size() + ")*");
		for (SitePin sitePin : site.getSourcePins()) {
			System.out.println("      " + sitePin.getName());
			System.out.println("        External Wire: " + sitePin.getExternalWire().getFullName());
			System.out.println("        Internal Wire: " + sitePin.getInternalWire().getFullName());
		}

		System.out.println("    *Sink Pins (" + site.getSinkPins().size() + ")*");
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
		System.out.println("    *Site Wires (" + site.getWires().size() + ")*");
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
		System.out.println("        *Connections " + "(" + wire.getWireConnections().size() + ")*");

		for (Connection c : wire.getWireConnections()) {
			String s;
				s = c.getSinkWire().getFullName();
			if (c.isRouteThrough())
				System.out.println("        [Routethrough] " + s);
			else if (c.isPip()) {
				System.out.println("        [PIP] " + s);
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

		if (t.getWireHashMap() != null) {
			// Build each wire and print its statistics
			Collection<Wire> wires = t.getWires();
			System.out.println("  *Tile Wires (" + wires.size() + ")*");
			for (Wire tw : wires) {
				printTileWire(tw);
			}
		}
		else {
			System.out.println("  *Tile Wires (0)*");
		}
	}

	/**
	 * Prints info about a tile wire, including connections.
	 * Print type of connection and if the other end is a different tile, the offset
	 * (unless either tile is outside the partial device)
	 * @param w
	 */
	private static void printTileWire(Wire w) {
		Tile t = w.getTile();
		TileType tileType = t.getType();
		TileType oocTileType = TileType.valueOf(familyType, "OOC_WIRE");
		String wireName = (tileType == TileType.valueOf(familyType, "OOC_WIRE")) ? w.getName() : w.getFullName();
		System.out.println("      " + wireName);
		System.out.println("        *Connections" + " (" + w.getWireConnections().size() + ")*");

		for (Connection c : w.getWireConnections()) {
			Tile sinkTile = c.getSinkWire().getTile();
			String s;

			if (sinkTile != t && sinkTile.getType() != oocTileType && tileType != oocTileType) {
				int xoff = sinkTile.getColumn() - t.getColumn() ;
				int yoff = sinkTile.getRow() - t.getRow() ;
				s = sinkTile.toString() + "/" + c.getSinkWire().getName() + " [" + yoff + "," + xoff + "]";
			}
			else if (sinkTile != t) {
				s = sinkTile.toString() + "/" + c.getSinkWire().getName();
			}
			else	 
				s = c.getSinkWire().getName();
			if (c.isRouteThrough())
				System.out.println("          [Routethrough] " + s);
			else if (c.isPip()) {
				System.out.println("          [PIP] " + s);
			}
			else
				System.out.println("          [nonPIP] " + s);
		}
	}	
	
}
