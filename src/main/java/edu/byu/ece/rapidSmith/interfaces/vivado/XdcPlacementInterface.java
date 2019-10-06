/*
 * Copyright (c) 2019 Brigham Young University
 *
 * This file is part of the BYU RapidSmith Tools.
 *
 * BYU RapidSmith Tools is free software: you may redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * BYU RapidSmith Tools is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * A copy of the GNU General Public License is included with the BYU
 * RapidSmith Tools. It can be found at doc/LICENSE.GPL3.TXT. You may
 * also get a copy of the license at <http://www.gnu.org/licenses/>.
 */

package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.interfaces.AbstractXdcInterface;

import static edu.byu.ece.rapidSmith.util.Exceptions.*;

/**
 * This class is used for parsing and writing placement XDC files in a TINCR checkpoint.
 * Placement.xdc files are used to specify the physical location of cells in a Vivado netlist.
 * 
 * @author Thomas Townsend
 *
 */
public class XdcPlacementInterface extends AbstractXdcInterface {
	private final CellDesign design;
	private int currentLineNumber;
	private String currentFile;
	private final Map<BelPin, CellPin> belPinToCellPinMap;

	public XdcPlacementInterface(CellDesign design, Device device) {
		super(device, design);
		this.design = design;
		belPinToCellPinMap = new HashMap<>();
	}
	
	/**
	 * Applies the placement constraints from the TINCR checkpoint files
	 * to the RapidSmith2 Cell Design.
	 *  
	 * @param xdcFile Placement.xdc file
	 * @throws IOException
	 */
	public void parsePlacementXDC(String xdcFile) throws IOException {
		
		currentFile = xdcFile;
		LineNumberReader br = new LineNumberReader(new BufferedReader(new FileReader(xdcFile)));
		String line;
		// Regex used to split lines via whitespace
		Pattern whitespacePattern = Pattern.compile("\\s+");
		
		while ((line = br.readLine()) != null) {
			currentLineNumber = br.getLineNumber();
			
			String[] toks = whitespacePattern.split(line);
			
			switch (toks[0]) {
				case "LOC" : applyCellPlacement(toks);
					break;
				case "PINMAP" : applyCellPinMappings(toks);
					break;
				case "PACKAGE_PIN" : applyPortPlacement(toks) ;
					break;
				case "IPROP" : applyInternalCellProperty(toks) ; 
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
		
		String siteType = toks[3];
		site.setType(SiteType.valueOf(device.getFamily(), siteType));
		
		Bel bel = tryGetBel(site, toks[4]);
		
		design.placeCell(cell, bel);
	}
	
	private void applyCellPinMappings(String[] toks) {
		
		Cell cell = tryGetPlacedCell(toks[1]);
		Bel bel = cell.getBel();
		
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
		
		if (toks.length != 4) {
			throw new ParseException("PACKAGE_PIN declaration should be followed by 3 tokens: cell site bel.\n"
					+ "On line: " + currentLineNumber + " of " + currentFile);
		}
		
		Cell cell = tryGetCell(toks[1]);
		
		if (!cell.isPort()) {
			cell = tryGetCell(toks[1] + "_rsport");
		}
		
		Site site = tryGetSite(toks[2]);
		Bel bel = tryGetBel(site, toks[3]);
				
		design.placeCell(cell, bel);
		
		assert (cell.getPins().size() == 1) : "PAD cell should only have one pin";
		assert (bel.getBelPins().count() == 1) : "PAD BEL " + site.getName() + "/" + bel.getName() + " should only have one pin. but has " + bel.getBelPins().count();
		
		BelPin belPin = bel.getBelPins().findFirst().get();
		CellPin cellPin = cell.getPins().iterator().next();
		
		cellPin.mapToBelPin(belPin);
		belPinToCellPinMap.put(belPin, cellPin);
	}

	/*
	 * Applies a property to an internal cell based on the tokens read from the placement.rsc file
	 * Expected format of toks : "IPROP cellName propertyName propertyValue"
	 */
	private void applyInternalCellProperty(String[] toks) {
		
		// throw an exception if the number of tokens on the line is not correct
		if (toks.length != 4) {
			throw new ParseException("Expected 3 parameters after token IPROP, found " + toks.length + " instead\n" 
					+ "On line " + this.currentLineNumber + " of " + currentFile);
		}
		
		// add the property to the cell
		Cell cell = tryGetCell(toks[1]);
		cell.getProperties().update(new Property(toks[2], PropertyType.EDIF, toks[3]));
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
	 * @param cell Cell which the pin is attached
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
	 * Creates a placement.xdc file from the cells of the given design 
	 * This file can be imported into Vivado to constrain the cells to a physical location
	 * 
	 * @param xdcOut Output placement.xdc file location
	 * @throws IOException
	 */
	public void writePlacementXDC(String xdcOut) throws IOException {
		
		try (BufferedWriter fileout = new BufferedWriter (new FileWriter(xdcOut)) ) {

			Iterator<Cell> cellIt = sortCellsForXdcExport(design).iterator();
			
			// All cells are assumed placed in this while loop
			while (cellIt.hasNext()) {
				Cell cell = cellIt.next();
				Site site = cell.getSite();
				Bel bel = cell.getBel();

				String cellname = cell.getName();
				
				// ports need a package pin reference, and aren't placed in Vivado
				if (cell.isPort()) {
					PackagePin packagePin = device.getPackagePin(bel);
					// if the port is not mapped to a valid package pin, thrown an exception
					if (packagePin == null) {
						if (device.getPackagePins().isEmpty()) {
							throw new ImplementationException("Device " + device.getPartName() + " is missing package pin information: cannot generate TCP without it.\n"
									+ "To generate the package pin information and add it to your device follow these three steps: \n"
									+ "1.) Run the Tincr command \"tincr::create_xml_device_info\" for your part.\n"
									+ "2.) Store the generated XML file to the devices/family directory which corresponds to your part.\n"
									+ "3.) Run the DeviceInfoInstaller in the util package to add the package pins to the device");
						}
						
						throw new ImplementationException("Cannot export placement information for port cell " + cellname + ".\n"
								+ "Package Pin for BEL " + bel.getFullName() + " cannot be found.");
					}
					fileout.write(String.format("set_property PACKAGE_PIN %s [get_ports {%s}]\n", packagePin.getName(), cellname));
				}
				else {
					fileout.write(String.format("set_property BEL %s.%s [get_cells {%s}]\n", site.getType().name(), bel.getName(), cellname));
					fileout.write(String.format("set_property LOC %s [get_cells {%s}]\n", site.getName(), cellname));
										
					//TODO: Update this function when more cells with LOCK_PINS are discovered
					if (cell.isLut()) { 
						fileout.write("set_property LOCK_PINS { ");
						for(CellPin cp: cell.getInputPins()) {
							if (!cp.isPseudoPin() && cp.getMappedBelPin() != null) {
								fileout.write(String.format("%s:%s ", cp.getName(), cp.getMappedBelPin().getName()));
							}
						}
						
						fileout.write("} [get_cells {" + cellname + "}]\n");
					}
				}
			}
			// If OOC mode, write the partition pin location properties.
			// This tells Vivado the tile where the Partition Pin should be placed (but not the exact wire).
			// Note that there is a “bug” where Vivado won't let you set the ROUTE string for nets where the source is
			// a port (or partition pin). So, Vivado has to route these nets ultimately.
			if (design.getImplementationMode() == ImplementationMode.OUT_OF_CONTEXT)
			{
				// Iterate through the ooc port map to construct the properties
				Map<String, String> oocPortMap = design.getPartPinMap();

				if (oocPortMap != null) {
					for (Map.Entry<String, String> entry : oocPortMap.entrySet()) {
						fileout.write("set_property HD.PARTPIN_LOCS {");

						// Write the tile the partition pin is located in
						String[] toks = entry.getValue().split("/");
						String tileName = toks[0];
						fileout.write(tileName);

						// Write the corresponding port name
						fileout.write("} [get_ports ");
						fileout.write(entry.getKey() + "]\n");
					}
				}
			}
		}
	}

	/*
	 * Sorts the cells of the design in the order required for TINCR export.
	 * Cells that are unplaced are not included in the sorted list. 
	 * Uses a bin sorting algorithm to have a complexity of O(n). 
	 * 
	 * TODO: Add <is_lut>, <is_carry>, and <is_ff> tags to cell library
	 */
	private Stream<Cell> sortCellsForXdcExport(CellDesign design) {
		design.getDevice().getAllSitesOfType(SiteType.valueOf(design.getFamily(), "SLICEL"));

		// cell bins
		ArrayList<Cell> sorted = new ArrayList<>(design.getCells().size());		
		ArrayList<Cell> lutCellsH5 = new ArrayList<>();
		ArrayList<Cell> lutCellsD5 = new ArrayList<>();
		ArrayList<Cell> lutCellsABC5 = new ArrayList<>();
		ArrayList<Cell> lutCellsH6 = new ArrayList<>();
		ArrayList<Cell> lutCellsD6 = new ArrayList<>();
		ArrayList<Cell> lutCellsABC6 = new ArrayList<>();
		ArrayList<Cell> carryCells = new ArrayList<>();
		ArrayList<Cell> ffCells = new ArrayList<>();
		ArrayList<Cell> ff5Cells = new ArrayList<>();
		ArrayList<Cell> muxCells = new ArrayList<>();

		// traverse the cells and drop them in the correct bin
		Iterator<Cell> cellIt = design.getLeafCells().iterator();
		
		while (cellIt.hasNext()) {
			Cell cell = cellIt.next();
			
			// only add cells that are placed to the list
			if (!cell.isPlaced()) {
				continue;
			}
			
			String libCellName = cell.getLibCell().getName();
			String belName = cell.getBel().getName();
			
			if (belName.endsWith("6LUT")) {
				if (belName.contains("H")) {
					lutCellsH6.add(cell);
				}
				else if (belName.contains("D")) {
					lutCellsD6.add(cell);
				}
				else {
					lutCellsABC6.add(cell);
				}
			}
			else if (belName.endsWith("5LUT")) {
				if (belName.contains("H")) {
					lutCellsH5.add(cell);
				}
				else if (belName.contains("D")) {
					lutCellsD5.add(cell);
				}
				else {
					lutCellsABC5.add(cell);
				}
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
			else if(belName.endsWith("MUX")) {
				muxCells.add(cell);
			}
			else {
				sorted.add(cell);
			}
		}

		// append all other cells in the correct order
		return Stream.of(sorted.stream(),
				lutCellsH5.stream(),
				lutCellsD5.stream(), 
				lutCellsABC5.stream(), 
				lutCellsH6.stream(),
				lutCellsD6.stream(), 
				lutCellsABC6.stream(), 
				ffCells.stream(), 
				carryCells.stream(),
				muxCells.stream(),
				ff5Cells.stream())
				.flatMap(Function.identity());
	}
}
