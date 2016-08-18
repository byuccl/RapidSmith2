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
import edu.byu.ece.rapidSmith.util.MessageGenerator;

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
				
				if (ps == null)
					MessageGenerator.briefError("[Warning] Site: " + sname + " not found, skipping placement of " + cname);
				else {
					String stype = toks[3]; 
					ps.setType(SiteType.valueOf(stype));

					String bname = toks[4];
					Bel b = ps.getBel(bname);
					
					if (b == null)
						MessageGenerator.briefError("[Warning] Bel: " + sname + "/" + bname + " not found, skipping placement of cell " + c.getName()); 
					else {
						design.placeCell(c, b);

						// Now, map all the cell pins to bel pins
						// TODO: Add a special case for mapping BRAM cell pins that can map to multiple bel pins
						//		it seems that this has something to do with 
						for (CellPin cp : c.getPins()) {
							List<BelPin> bpl = cp.getPossibleBelPins();
							
							//special case for the CIN pin
							if (b.getName().equals("CARRY4") && cp.getName().equals("CI") ) {
								cp.setBelPin("CIN");
							}
							//TODO: may have to update this with startwith FIFO as well as RAMB
							else if (bpl.size() == 1 || b.getName().startsWith("RAMB")) {
								if(bpl.get(0) != null)
									cp.setBelPin(bpl.get(0));
								else {
									MessageGenerator.briefErrorAndExit("Pin Error: " + c.getLibCell().getName() + " / " + cp.getName());
								}
							}
							else if (bpl.size() == 0) {
								MessageGenerator.briefError("[Warning]: Unknown cellpin to belpin mapping for cellpin: " + cp.getName());
							}
						}
					}
				}
			}
			//LOCK_PINS MAP1 MAP2 ... CELL
			else if (toks[0].equals("LOCK_PINS")) {
				Cell cell = design.getCell(toks[toks.length-1]); 
								
				//extract the actual cell to bel pin mappings for LUTs
				for(int i = 1; i < toks.length-1; i++){
					String[] pins = toks[i].split(":");
					cell.getPin(pins[0]).setBelPin(pins[1]);
				}
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
			
			fileout.write(String.format("set_property BEL %s.%s [get_cells {%s}]\n", ps.getType().toString(), b.getName(), cellname));
			fileout.write(String.format("set_property LOC %s [get_cells {%s}]\n", ps.getName(), cellname));
								
			//TODO: Update this function when more cells with LOCK_PINS are discovered
			if(cell.getLibCell().getName().startsWith("LUT")) {
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
