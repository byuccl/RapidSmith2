package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
 * This class is used for parsing and writing placement XDC files in a TINCR checkpoint. <br>
 * Placement.xdc files are used to specify the physical location of cells in a Vivado netlist.
 * 
 * @author Thomas Townsend
 *
 */
public class XdcPlacementInterface {

	/**
	 * Applies the placement constraints from the TINCR checkpoint files
	 * to the RapidSmith2 Cell Design.
	 *  
	 * @param xdcFile Placement.xdc file
	 * @param design Design to apply placement
	 * @param device Device which the design is implemented on
	 * @throws IOException
	 */
	public static void parsePlacementXDC(String xdcFile, CellDesign design, Device device) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(xdcFile));
		String line = null;
		
		//parse the placement file
		while ((line = br.readLine()) != null) {
			String[] toks = line.split("\\s+");
			if (toks[0].equals("LOC")) {
				String cname = toks[1]; 
				String sname = toks[2];
				Cell c = design.getCell(cname);
				
				Objects.requireNonNull(c, "Null cell found in design! This should never happen.");
								
				Site ps = device.getPrimitiveSite(sname);
				
				if (ps == null) {
					br.close();
					throw new ParseException(String.format("Site: %s not found in the current device!", sname));
				}
				
				String stype = toks[3]; 
				ps.setType(SiteType.valueOf(stype));

				String bname = toks[4];
				Bel b = ps.getBel(bname);
				
				if (b == null) {
					br.close();
					throw new ParseException(String.format("Bel: %s not found in site: %s.", bname, sname));
				}
				
				design.placeCell(c, b);

				// Now, map all the cell pins to bel pins
				// TODO: Add a special case for mapping BRAM cell pins that can map to multiple bel pins
				//		it seems that this has something to do with 
				for (CellPin cp : c.getPins()) {
					List<BelPin> bpl = cp.getPossibleBelPins();
					
					//special case for the CIN pin ... TODO: update cell library instead of doing this
					if (b.getName().equals("CARRY4") && cp.getName().equals("CI") ) {
						cp.setBelPin("CIN");
					}
					//TODO: may have to update this with startwith FIFO as well as RAMB
					else if (bpl.size() == 1 || b.getName().startsWith("RAMB")) {
						cp.setBelPin(bpl.get(0));
					}
					else if (bpl.size() == 0) {
						br.close();
						throw new ParseException(String.format("Uknown cellpin to belpin mapping for cellpin: %s. Update CellLibrary.xml.", cp.getName()));
					}
				}				
			}
			//LOCK_PINS MAP1 MAP2 ... CELL
			else if (toks[0].equals("LOCK_PINS")) {
				String cellName = toks[toks.length-1];
				Cell cell = design.getCell(cellName); 
				
				if (cell == null) {
					br.close();
					throw new ParseException(String.format("Cell %s does not exist in the design!", cellName));
				}
				
				//extract the actual cell to bel pin mappings for LUTs
				for(int i = 1; i < toks.length-1; i++){
					String[] pins = toks[i].split(":");
					cell.getPin(pins[0]).setBelPin(pins[1]);
				}
			}
			// pins (ports)
			else if (toks[0].equals("PACKAGE_PIN")) {
				String siteName = toks[1];
				String cellName = toks[2];
				
				Bel bel = device.getPrimitiveSite(siteName).getBel("PAD");
				Cell cell = design.getCell(cellName); 
				design.placeCell(cell, bel);
				cell.getPin("PAD").setBelPin(bel.getBelPin("PAD"));
			}
		}
		br.close();
	}
	
	/**
	 * Creates a placement.xdc file from the cells of the given design <br>
	 * This file can be imported into Vivado to constrain the cells to a physical location
	 * 
	 * @param xdcOut Output placement.xdc file location
	 * @param design Design with cells to export
	 * @throws IOException
	 */
	public static void writePlacementXDC(String xdcOut, CellDesign design) throws IOException {
		
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
			if(cell.getLibCell().isLut()) { //.getName().startsWith("LUT")) {
				fileout.write("set_property LOCK_PINS { ");
				for(CellPin cp: cell.getInputPins()) 
					fileout.write(String.format("%s:%s ", cp.getName(), cp.getBelPin().getName()));
				
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
	private static Collection<Cell> sortCellsForXdcExport(CellDesign design) {
		
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
