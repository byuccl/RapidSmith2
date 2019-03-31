package edu.byu.ece.rapidSmith.interfaces.yosys;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.byu.ece.edif.core.*;
import edu.byu.ece.edif.util.parse.EdifParser;
import edu.byu.ece.edif.util.parse.ParseException;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.interfaces.AbstractEdifInterface;
import edu.byu.ece.rapidSmith.util.Exceptions;

public class YosysEdifInterface extends AbstractEdifInterface {

	/* ********************
	 * 	 Import Section
	 *********************/

	/**
	 * Gets the XORCY cell connected to a MUXCY cell, if one exists.
	 * There will be no corresponding XORCY cell if the corresponding O ouput on the CARRY4 doesn't need to be used.
	 * @param muxCell
	 * @return the corresponding XORCY cell
	 */
	private Cell getXorCell(Cell muxCell) {
		// The XORCY and MUXCY both share an input pin (MUXCY: S, XORCY: LI)
		CellNet muxSelectNet = muxCell.getPin("S").getNet();

		Optional<CellPin> xorLiPin = muxSelectNet.getSinkPins().stream()
				.filter(cellPin -> cellPin.getCell().getType().equals("XORCY"))
				.filter(cellPin -> cellPin.getName().equals("LI"))
				.findFirst();

		return xorLiPin.map(CellPin::getCell).orElse(null);
	}

	/**
	 * Adds the XORCY cell to the CARRY4 cell.
	 * @param xorCell
	 * @param carryCell
	 * @param carryIndex
	 * @param chainStart
	 */
	private void addXorCellToCarryCell(Cell xorCell, Cell carryCell, int carryIndex, boolean chainStart) {
		// Connect the input pins
		for (CellPin cellPin : xorCell.getInputPins()) {
			CellNet net = cellPin.getNet();

			switch (cellPin.getName()) {
				case "CI":
					if (chainStart && carryIndex == 0) {
						// Disconnect from the XORCY pin
						net.disconnectFromPin(cellPin);

						// Connect to CARRY4 pin
						net.connectToPin(carryCell.getPin("CYINIT"));
					}
					else if (carryIndex == 0) {
						// Disconnect from the XORCY pin
						net.disconnectFromPin(cellPin);

						// Connect to CARRY4 pin
						net.connectToPin(carryCell.getPin("CI"));
					}
					break;
				case "LI":
					// Disconnect from the XORCY pin
					net.disconnectFromPin(cellPin);

					// Connect to CARRY4 pin
					net.connectToPin(carryCell.getPin("S" + "[" + carryIndex + "]"));
					break;
				default:
					throw new Exceptions.ParseException("XORCY cell " + xorCell.getName() + " has unexpected input pin " + cellPin.getFullName());
			}
		}

		// Connect the output pins
		for (CellPin cellPin : xorCell.getOutputPins()) {
			CellNet net = cellPin.getNet();

			if (cellPin.getName().equals("O")) {
				// Disconnect form the XORCY pin
				net.disconnectFromPin(cellPin);

				// Connect to CARRY4 pin
				net.connectToPin(carryCell.getPin("O" + "[" + carryIndex + "]"));
			}
			else {
				throw new Exceptions.ParseException("XORCY cell " + xorCell.getName() + " has unexpected input pin " + cellPin.getFullName());
			}
		}
	}

	/**
	 * Adds the MUXCY cell to the CARRY4 cell.
	 * @param muxCell
	 * @param carryCell
	 * @param carryIndex
	 * @param chainStart
	 */
	private void addMuxCellToCarryCell(Cell muxCell, Cell carryCell, int carryIndex, boolean chainStart) {
		// Connect the input pins
		for (CellPin cellPin : muxCell.getInputPins()) {
			CellNet net = cellPin.getNet();
			if (net != null) {
				switch (cellPin.getName()) {
					case "CI":
						// Disconnect from the MUXCY pin
						net.disconnectFromPin(cellPin);

						// Connect to the CARRY4 pin
						// If this isn't the first MUXCY in a CARRY4, no connection needs to be added
						if (chainStart && !net.getPins().contains(carryCell.getPin("CYINIT"))) {
							net.connectToPin(carryCell.getPin("CYINIT"));
						}
						else if (carryIndex == 0 && !net.getPins().contains(carryCell.getPin("CI"))) {
							net.connectToPin(carryCell.getPin("CI"));
						}
						break;
					case "DI":
						// Disconnect from the MUXCY pin
						net.disconnectFromPin(cellPin);

						// Connect to the CARRY4 pin
						net.connectToPin(carryCell.getPin("DI[" + carryIndex + "]"));
						break;
					case "S":
						// Disconnect from the MUXCY pin
						net.disconnectFromPin(cellPin);

						// Connect to the CARRY4 pin
						CellPin carryPin = carryCell.getPin("S[" + carryIndex + "]");

						// The net may already be connected to the pin (from adding an XORCY cell)
						if (!net.getPins().contains(carryPin))
							net.connectToPin(carryPin);
						break;
					default:
						throw new Exceptions.ParseException("MUXCY cell " + muxCell.getName() + " has unexpected input pin " + cellPin.getFullName());
				}
			}
		}

		// Connect the output pin
		CellPin muxOutPin = muxCell.getPin("O");
		CellNet muxOutNet = muxOutPin.getNet();

		for (CellPin pin : muxOutNet.getSinkPins()) {
			Cell sinkCell = pin.getCell();

			if ("CI".equals(pin.getName())) {
				if (sinkCell.getType().equals("MUXCY") || sinkCell.getType().equals("XORCY")) {
					// If it is the last MUXCY in a CARRY4, we need to disconnect the net from the MUXCY source
					// and connect it to the CARRY4 source.
					// Since MUXCY and XORCY are driven by this net, we only need to do this once.
					if (carryIndex == 3 && !muxOutNet.getSourcePin().equals(carryCell.getPin("CO[3]"))) {
						muxOutNet.disconnectFromPin(muxCell.getPin("O"));
						muxOutNet.connectToPin(carryCell.getPin("CO[3]"));
					}
					else if (carryIndex != 3) {
						// Disconnect the sink and the MUXCY pin
						muxOutNet.disconnectFromPin(pin);
					}
				}
				else {
					throw new Exceptions.ParseException("MUXCY cell " + muxCell.getName() + " has unexpected sink pin " + pin.getFullName());
				}
			}
			// Assume the sink is a LUT pin, etc.
			else {

				CellNet cyNet = pin.getNet();

				// disconnect from source
				cyNet.disconnectFromPin(muxOutNet.getSourcePin());

				// Connect to the CARRY4 CO pin.
				cyNet.connectToPin(carryCell.getPin("CO" + "[" + carryIndex + "]"));
			}
		}
	}


	/**
	 * Builds up a carry chain, creating CARRY4 cells, given a starting MUXCY cell.
	 * @param muxCell the starting XORCY cell of a CARRY4 chain
	 */
	private void buildCarryChain(CellDesign design, CellLibrary libCells, Cell muxCell) {
		// Get the corresponding XORCY cell if it exists
		Cell xorCell = getXorCell(muxCell);

		// Mark the start of the carry chain
		boolean chainStart = true;
		Cell carryCell = new Cell(muxCell.getName() + "_CARRY4", libCells.get("CARRY4"));
		int carryIndex = 0;

		// Replace muxCell
		while (muxCell != null || xorCell != null) {

			// Add the XORCY cell (if it exists) and the MUXCY cell to the CARRY4 cell.
			if (xorCell != null) {
				addXorCellToCarryCell(xorCell, carryCell, carryIndex, chainStart);
				design.removeCell(xorCell);
				xorCell = null;
			}

			if (muxCell != null) {
				// Get a map to the MUXCY/XORCY cells this MUXCY drives
				Map<String, Cell> sinkCells = muxCell.getPin("O").getNet().getSinkPins().stream()
						.filter(p -> p.getName().equals("CI"))
						.collect(Collectors.toMap(p -> p.getCell().getType(), CellPin::getCell));

				addMuxCellToCarryCell(muxCell, carryCell, carryIndex, chainStart);
				design.removeCell(muxCell);

				muxCell = sinkCells.get("MUXCY");
				xorCell = sinkCells.get("XORCY");
			}

			chainStart = false;

			// If the carry chain has ended
			if (muxCell == null && xorCell == null) {
				design.addCell(carryCell);
			}
			// If we need to make a new CARRY4 cell to continue the chain
			else if (carryIndex == 3) {
				design.addCell(carryCell);
				String carryCellNamePrefix = muxCell != null ? muxCell.getName() : xorCell.getName();
				carryCell = new Cell(carryCellNamePrefix + "_CARRY4", libCells.get("CARRY4"));
				carryIndex = 0;
			}
			else
				carryIndex++;
			}

		}

	/**
	 * Transform XORCY and MUXCY cells into CARRY4 cells.
	 * @param design
	 * @param libCells
	 */
	private void transformCarryCells(CellDesign design, CellLibrary libCells) {

		if (!libCells.contains("CARRY4")) {
			if (!suppressWarnings) {
				System.err.println("[Warning] Cell library does not contain CARRY4 cells. MUXCY and XORCY cells will not be transformed into CARRY cells.");
			}
			return;
		}

		// Find the beginning of the carry chains in the design.
		// The carry chains will start with a MUXCY cell whose CI/CYINIT pin is driven by VCC/GND.
		Collection<Cell> muxInitCells = design.getCells().stream()
				.filter(cell -> cell.getType().equals("MUXCY"))
				.filter(cell -> cell.getPin("CI").getNet().isStaticNet())
				.collect(Collectors.toList());

		for (Cell muxCell : muxInitCells) {
			buildCarryChain(design, libCells, muxCell);
		}
	}

	@Override
	public CellDesign parseEdif(String edifFile, CellLibrary libCells, String partName) {
		return parseEdif(edifFile, libCells, partName, false);
	}

	/**
	 * Parses the Edif netlist into a RapidSmith2 CellDesign data structure
	 *
	 * @param edifFile Input EDIF file
	 * @param libCells A Cell library for a specific Xilinx part
	 * @param partName Name of the part for the design. Needs to be set manually for partial devices when using a netlist
	 *                 synthesized by Vivado, since the netlist contains the name of the full part.
	 * @param transformCells Whether or not to transform cells. Only transforms MUXCY, XORCY cells to CARRY4 cells right now.
	 * @return The RapidSmith2 representation of the EDIF netlist
	 */
	public CellDesign parseEdif(String edifFile, CellLibrary libCells, String partName, boolean transformCells) {
		List<CellNet> vccNets = new ArrayList<>();
		List<CellNet> gndNets = new ArrayList<>();
		Map<EdifPort, Integer> portOffsetMap = new HashMap<>();

		try {
			// parse edif into the BYU edif tools data structures
			EdifEnvironment top = EdifParser.translate(edifFile);
			EdifCell topLevelCell = top.getTopCell();

			// create RS2 cell design
			CellDesign design = new CellDesign(top.getTopDesign().getName(), partName);
			design.getProperties().updateAll(createCellProperties(topLevelCell.getPropertyList()));

			// add all the cells and nets to the design
			processTopLevelEdifPorts(design, topLevelCell.getInterface(), libCells, portOffsetMap);
			processEdifCells(design, topLevelCell.getCellInstanceList(), libCells, vccNets, gndNets);
			processEdifNets(design, topLevelCell.getNetList(), vccNets, gndNets, portOffsetMap);

			// Transform MUXCY, XORCY cells to CARRY4 cells
			if (transformCells)
				transformCarryCells(design, libCells);

			collapseStaticNets(design, libCells, vccNets, gndNets);

			return design;
		}
		catch (FileNotFoundException | ParseException e) {
			throw new Exceptions.ParseException(e);
		}
	}


	/* *********************
	 *    Export Section
	 ***********************/

	@Override
	public void writeEdif(String edifOutputFile, CellDesign design) {
	}

}
	
