package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.SiteType;

import static edu.byu.ece.rapidSmith.util.Exceptions.ParseException;

/**
 * This class is used for parsing and writing placement XDC files in a TINCR checkpoint.
 * Placement.xdc files are used to specify the physical location of cells in a Vivado netlist.
 * 
 * @author Thomas Townsend
 *
 */
public class XdcPlacementInterface {

	private CellDesign design;
	private Device device;
	private int currentLineNumber;
	private String currentFile;
	private Map<BelPin, CellPin> belPinToCellPinMap; 
	
	public XdcPlacementInterface(CellDesign design, Device device) {
		this.design = design;
		this.device = device;
		belPinToCellPinMap = new HashMap<BelPin, CellPin>();
	}
	
	/**
	 * Applies the placement constraints from the TINCR checkpoint files
	 * to the RapidSmith2 Cell Design.
	 *  
	 * @param xdcFile Placement.xdc file
	 * @param design Design to apply placement
	 * @param device Device which the design is implemented on
	 * @throws IOException
	 */
	public void parsePlacementXDC(String xdcFile) throws IOException {
		
		currentFile = xdcFile;
		LineNumberReader br = new LineNumberReader(new BufferedReader(new FileReader(xdcFile)));
		String line = null;
		
		while ((line = br.readLine()) != null) {
			currentLineNumber = br.getLineNumber();
			
			String[] toks = line.split("\\s+");
			
			switch (toks[0]) {
				case "LOC" : applyCellPlacement(toks);
					break;
				case "PINMAP" : applyCellPinMappings(toks);
					break;
				case "PACKAGE_PIN" : applyPortPlacement(toks) ;
					break;
				default :
					throw new ParseException(String.format("Unrecognized Token: %s \nOn %d of %s", toks[0], currentLineNumber, currentFile));
			}
		}
	
		br.close();
	}
	
	private void applyCellPlacement(String[] toks) {
		
		Cell cell = tryGetCell(toks[1]);
		Site site = tryGetSite(toks[2]);
		
		// TODO: add a check to see that the sitetype is a valid option for the site
		String siteType = toks[3]; 
		site.setType(SiteType.valueOf(siteType));
		
		Bel bel = tryGetBel(site, toks[4]);
		
		design.placeCell(cell, bel);
	}
	
	private void applyCellPinMappings(String[] toks) {
		
		Cell cell = tryGetPlacedCell(toks[1]);
		Bel bel = cell.getAnchor();
		
		for (int i = 2; i < toks.length; i++) {
			String[] pinmap = toks[i].split(":");

			// If pinmap.length = 1, this mean the cell pin has no belPinMapping.
			if (pinmap.length > 1) {
				CellPin cellPin = tryGetCellPin(cell, pinmap[0]);
				
				for (int j = 1; j < pinmap.length; j++) {
					BelPin belPin = tryGetBelPin(bel, pinmap[j]);
					cellPin.mapToBelPin(belPin);
					belPinToCellPinMap.put(belPin, cellPin);
				}
			}
		}
	}
	
	private void applyPortPlacement(String[] toks) {
		
		Site site = tryGetSite(toks[1]);
		Cell cell = tryGetCell(toks[2]);
				
		// TODO: update this so PAD is not hardcoded in this location. Not sure if there is a way to do this...
		Bel bel = tryGetBel(site, "PAD"); 
		design.placeCell(cell, bel);
		
		BelPin belPin = tryGetBelPin(bel, "PAD");
		CellPin cellPin = tryGetCellPin(cell, "PAD");
		
		cellPin.mapToBelPin(belPin);
		belPinToCellPinMap.put(belPin, cellPin);
	}

	/**
	 * Returns the map of BelPin->CellPin mapping after the placement xdc
	 * has been applied to the design. Should be called after parsePlacementXDC
	 * is called.
	 * 
	 * @return Map from BelPin to the CellPin that is placed on it
	 */
	public Map<BelPin, CellPin> getPinMap() {
		return belPinToCellPinMap;
	}
	
	/**
	 * Tries to retrieve the Cell object with the given name
	 * from the currently loaded design. If the cell does not exist,
	 * a ParseException is thrown
	 * 
	 * @param cellName Name of the cell to retrieve
	 */
	private Cell tryGetCell(String cellName) {
		
		Cell cell = design.getCell(cellName);
		
		if (cell == null) {
			throw new ParseException("Cell \"" + cellName + "\" not found in the current device. \n" 
									+ "On line " + this.currentLineNumber + " of " + currentFile);
		}
		
		return cell;
	}
	
	private Cell tryGetPlacedCell(String cellName) {
		Cell cell = tryGetCell(cellName);
		
		if (!cell.isPlaced()) {
			throw new ParseException("Cell \"" + cellName + "\" not placed. Cannot apply a pin mapping.\n"
									+ "On line " + this.currentLineNumber + " of " + currentFile);
		}
		
		return cell;
	}
	
	/**
	 * Tries to retrieve a CellPin object on the specified Cell parameter.
	 * If the pin does not exist, a ParseException is thrown.
	 * 
	 * @param Cell Cell which the pin is attached
	 * @param pinName Name of the pin
	 * @return CellPin
	 */
	private CellPin tryGetCellPin(Cell cell, String pinName) {
		
		CellPin pin = cell.getPin(pinName);
		
		if (pin == null) {
			throw new ParseException(String.format("CellPin: \"%s/%s\" does not exist in the current device"
												 + "On line %d of %s", cell.getName(), pinName, currentLineNumber, currentFile));
		}
		
		return pin;
	}
	
	/**
	 * Tries to retrieve the Site object with the given site name
	 * from the currently loaded device. If the site does not exist
	 * a ParseException is thrown
	 * 
	 * @param siteName Name of the site to retrieve
	 */
	private Site tryGetSite(String siteName) {
		
		Site site = device.getPrimitiveSite(siteName);
		
		if (site == null) {
			throw new ParseException("Site \"" + siteName + "\" not found in the current device. \n" 
									+ "On line " + this.currentLineNumber + " of " + currentFile);
		}
		
		return site;
	}
		
	/**
	 * Tries to retrieve a BEL object from the currently loaded device. 
	 * If the BEL does not exist, a ParseException is thrown. 
	 * 
	 * @param site Site where the BEL resides
	 * @param belName Name of the BEL within the site
	 * @return Bel
	 */
	private Bel tryGetBel(Site site, String belName) {
		
		Bel bel = site.getBel(belName);
		
		if (bel == null) {
			throw new ParseException(String.format("Bel: \"%s/%s\" does not exist in the current device"
												 + "On line %d of %s", site.getName(), belName, currentLineNumber, currentFile));
		}
		
		return bel;
	}
	
	/**
	 * Tries to retrieve a BelPin object from the currently loaded device
	 * If the pin does not exist, a ParseException is thrown.
	 * 
	 * @param bel Bel which the pin is attached
	 * @param pinName Name of the bel pin
	 * @return BelPin
	 */
	private BelPin tryGetBelPin(Bel bel, String pinName) {
		
		BelPin pin = bel.getBelPin(pinName);
		
		if (pin == null) {
			throw new ParseException(String.format("BelPin: \"%s/%s\" does not exist in the current device.\n"
												 + "On line %d of %s", bel.getName(), pinName, currentLineNumber, currentFile));
		}
		
		return pin;
	}
		
	/**
	 * Creates a placement.xdc file from the cells of the given design <br>
	 * This file can be imported into Vivado to constrain the cells to a physical location
	 * 
	 * @param xdcOut Output placement.xdc file location
	 * @param design Design with cells to export
	 * @throws IOException
	 */
	public void writePlacementXDC(String xdcOut) throws IOException {
		
		BufferedWriter fileout = new BufferedWriter (new FileWriter(xdcOut));
		
		//TODO: Assuming that the logical design has not been modified...can no longer assume this with insertion/deletion
		for (Cell cell : sortCellsForXdcExport(design)) {
						
			Site ps = cell.getAnchorSite();
			Bel b = cell.getAnchor();
			String cellname = cell.getName();
			
			// ports need a package pin reference, and aren't placed in Vivado
			if (cell.isPort()) {
				fileout.write(String.format("set_property PACKAGE_PIN %s [get_ports {%s}]\n", ps.getName(), cellname));
				continue;
			}
			
			fileout.write(String.format("set_property BEL %s.%s [get_cells {%s}]\n", ps.getType().toString(), b.getName(), cellname));
			fileout.write(String.format("set_property LOC %s [get_cells {%s}]\n", ps.getName(), cellname));
								
			//TODO: Update this function when more cells with LOCK_PINS are discovered
			if(cell.getLibCell().isLut()) { 
				fileout.write("set_property LOCK_PINS { ");
				for(CellPin cp: cell.getInputPins()) 
					fileout.write(String.format("%s:%s ", cp.getName(), cp.getMappedBelPin().getName()));
				
				fileout.write("} [get_cells {" + cellname + "}]\n");
			}
		}
		
		fileout.close();
	}
	
	/*
	 * Sorts the cells of the design in the order required for TINCR export. 
	 * Uses a bin sorting algorithm to have a complexity of O(n). 
	 * 
	 * TODO: Add <is_lut>, <is_carry>, and <is_ff> tags to cell library
	 */
	private Collection<Cell> sortCellsForXdcExport(CellDesign design) {
		
		// cell bins
		ArrayList<Cell> sorted = new ArrayList<Cell>(design.getCells().size());
		ArrayList<Cell> lutCells = new ArrayList<Cell>();
		ArrayList<Cell> carryCells = new ArrayList<Cell>();
		ArrayList<Cell> ffCells = new ArrayList<Cell>();
		ArrayList<Cell> ff5Cells = new ArrayList<Cell>();
		
		// traverse the cells and drop them in the correct bin
		for (Cell cell : design.getCells()) {
			
			// only add cells that are placed to the list
			if( !cell.isPlaced() ) {
				continue;
			}
			
			String libCellName = cell.getLibCell().getName();
			String belName = cell.getAnchor().getName();
			
			if (belName.endsWith("LUT")) {
				lutCells.add(cell); 
			}
			else if (libCellName.startsWith("CARRY")) {
				carryCells.add(cell);
			}
			else if (belName.endsWith("5FF")) {
				ff5Cells.add(cell);
			}
			else if (belName.endsWith("FF")) {
				ffCells.add(cell);
			}
			else {
				sorted.add(cell);
			}
		}
				
		// append all other cells in the correct order
		sorted.addAll(lutCells);
		sorted.addAll(ffCells);
		sorted.addAll(carryCells);		
		sorted.addAll(ff5Cells);
	
		return sorted;
	}
}
