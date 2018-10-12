package edu.byu.ece.rapidSmith.interfaces.yosys;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.byu.ece.edif.core.*;
import edu.byu.ece.edif.core.PropertyList;
import edu.byu.ece.edif.util.parse.EdifParser;
import edu.byu.ece.edif.util.parse.ParseException;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.util.Exceptions;

public final class YosysEdifInterface {

	private static boolean suppressWarnings = false;

	/**
	 * Suppress non-critical warnings while parsing an EDIF file.
	 */
	public static void suppressWarnings(boolean suppress) {
		suppressWarnings = suppress;
	}

	/* ********************
	 * 	 Import Section
	 *********************/
	private static Pattern busNamePattern;

	static
	{
		busNamePattern = Pattern.compile("(.*)\\[.+:(.+)\\]");
	}


	/*
	 * Converts EDIF top level ports to equivalent RapidSmith port cells and adds them to the design
	 */
	private static void processTopLevelEdifPorts (CellDesign design, EdifCellInterface topInterface, CellLibrary libCells, Map<EdifPort, Integer> portOffsetMap) {

		for ( EdifPort port : topInterface.getPortList() ) {

			String libraryPortType;

			if (port.isInOut()) {
				libraryPortType = "IOPORT";
			}
			else if (port.isInput()) {
				libraryPortType = "IPORT";
			}
			else {
				libraryPortType = "OPORT";
			}

			int offset = 0;

			// find the port suffix and offset
			String portSuffix = port.getOldName();
			if (port.isBus()) {
				//Matcher matcher = busNamePattern.matcher(port.getOldName());
			//	if (matcher.matches()) {
				//	portSuffix = matcher.group(1);
			//		offset = Integer.parseInt(matcher.group(2));
				// Assume offset is 0
				// TODO: Figure out if Yosys always does offsets of 0.
				//offset = 0;
				portOffsetMap.put(port, offset);
				//}
			//	else {
			//		throw new AssertionError("Vivado Naming pattern for bus does not match expected pattern");
			//	}
			}

			// Create a new RapidSmith cell for each port in the EDIF
			for (EdifSingleBitPort busMember : port.getSingleBitPortList() ) {

				LibraryCell libCell = libCells.get(libraryPortType);

				String portName = port.isBus() ?
						String.format("%s[%d]", portSuffix, reverseBusIndex(port.getWidth(), busMember.bitPosition(), offset)) :
						portSuffix;
				Cell portCell = new Cell(portName, libCell);
				//portCell.getProperties().update(new Property("Dir", PropertyType.USER, PortDirection.getPortDirectionForImport(portCell)));
				design.addCell(portCell);
			}
		}
	}

	/*
	 * Converts EDIF cell instances to equivalent RapidSmith cells and adds them to the design
	 */
	private static void processEdifCells(CellDesign design, Collection<EdifCellInstance> edifCellInstances, CellLibrary libCells, List<CellNet> vccNets, List<CellNet> gndNets) {
		// TODO: think about throwing an error or warning here
		if (edifCellInstances == null || edifCellInstances.size() == 0) {
			if (!suppressWarnings) {
				System.err.println("[Warning] No cells found in the edif netlist");
			}
			return;
		}

		// create equivalent RS2 cells from the edif cell instances
		for(EdifCellInstance eci : edifCellInstances) {
			// create the corresponding RS2 cell
			LibraryCell lcType = libCells.get(eci.getType());
			if (lcType == null) {
				throw new Exceptions.ParseException("Unable to find library cell of type: " + eci.getType());
			}

			// Check for naming conflicts and rename cells as required...this should not be necessary for designs
			// synthesized and implemented in Vivado, but if the netlist is manipulated by an external tool,
			// this can happen
			if (design.hasCell(eci.getOldName())) {
				handleNamingConflict(design, design.getCell(eci.getOldName()));
			}

			Cell newcell = design.addCell(new Cell(eci.getOldName(), lcType));

			// Add properties to the cell
			newcell.getProperties().updateAll(createCellProperties(eci.getPropertyList()));

			// look for internal macro nets
			if (newcell.isMacro()) {
				for (CellNet net : newcell.getInternalNets()) {
					if (net.isVCCNet()) {
						vccNets.add(net);
					}
					else if (net.isGNDNet()) {
						gndNets.add(net);
					}
				}
			}
		}
	}

	/**
	 * Some EDIF netlists from Vivado can have identical port and cell names. This function renames the ports
	 * so that there is no naming conflict in RapidSmith.
	 *
	 * @param design CellDesign
	 * @param cell Cell to rename (should be a port cell)
	 */
	private static void handleNamingConflict (CellDesign design, Cell cell) {
		assert cell.isPort() : "Conflicting cell names should only happen with Port cells: " + cell.getName();

		// print a warning to the user
		if (!suppressWarnings) {
			System.err.println("[Warning] A top-level port and another cell in the netlist have identical names: " + cell.getName()
					+ ". The port cell will be renamed to " + cell.getName() + "_rsport");
		}

		// update the name of the cell
		design.removeCell(cell);
		Cell newPortCell = new Cell(cell.getName() + "_rsport", cell.getLibCell());
		design.addCell(newPortCell);
	}

	/*
	 * Converts EDIF nets to equivalent RapidSmith nets and adds them to the design
	 */
	private static void processEdifNets(CellDesign design, Collection<EdifNet> edifNets, List<CellNet> vccNets, List<CellNet> gndNets, Map<EdifPort, Integer> portOffsetMap) {

		if (edifNets == null || edifNets.size() == 0) {
			if (!suppressWarnings) {
				System.err.println("[Warning] No nets found in the edif netlist");
			}
			return;
		}

		// Go through the cell's nets and hook up inputs and outputs
		for(EdifNet net : edifNets) {

			//create a new net
			CellNet cn = new CellNet(net.getOldName(), NetType.WIRE);

			// Add all the source and sink connections to the net
			//Collection<EdifPortRef> sources = net.getSourcePortRefs(false, true);

			// process all net connections
			processNetConnections(net.getPortRefList(), design, cn, portOffsetMap);

			//report a warning if no sources on a net are found
			if (cn.getAllSourcePins().size() == 0) {
				if (!suppressWarnings) {
					System.err.println("[Warning] No source for net " + net.getOldName());
				}
			}

			// Add the net to the design if is is NOT a static net.
			// Otherwise, store it for later use (will collapse later)
			if (cn.isVCCNet()) {
				vccNets.add(cn);
			}
			else if (cn.isGNDNet()) {
				gndNets.add(cn);
			}
			else {
				design.addNet(cn);
			}

			cn.getProperties().updateAll(createCellProperties(net.getPropertyList()));
		}
	}

	/*
	 * Builds the connections of CellNet based on the specified EDIF port references. Returns true
	 * if the given net is attached to a top-level port, false otherwise
	 *
	 * TODO: update this once top-level ports are added
	 */
	private static void processNetConnections(Collection<EdifPortRef> portRefs, CellDesign design, CellNet net, Map<EdifPort, Integer> portOffsetMap) {

		for (EdifPortRef portRef: portRefs) {

			EdifPort port = portRef.getPort();

			// Connects to a top-level port
			if (portRef.isTopLevelPortRef()) {

				String portname = portRef.isSingleBitPortRef() ? port.getOldName() :
						String.format("%s[%d]", getPortNameSuffix(port.getName()), reverseBusIndex(port.getWidth(), portRef.getBusMember(), portOffsetMap.get(port)));

				Cell portCell = design.getCell(portname);

				if (portCell == null) {
					throw new Exceptions.ParseException("Port Cell " + portname + " does not exist in the design!");
				}
				else if (!portCell.isPort()) {
					portCell = design.getCell(portname + "_rsport");
				}

				net.connectToPin(portCell.getPin("PAD"));

				continue;
			}

			Cell node = design.getCell(portRef.getCellInstance().getOldName());
			if (node == null) {
				throw new Exceptions.ParseException("Cell: " + portRef.getCellInstance().getOldName()  + " does not exist in the design!");
			}

			int busOffset = 0;
			if (node.isMacro()) {
				LibraryMacro macro = (LibraryMacro)node.getLibCell();
				busOffset = macro.getPinOffset(port.getName());
			}

			// Connects to a cell pin
			// TODO: take a closer look at this...I am using the edif name of a cell pin name which should be ok, but be aware
			String pinname = portRef.isSingleBitPortRef() ? port.getName()
					: String.format("%s[%d]", port.getName(), reverseBusIndex(port.getWidth(), portRef.getBusMember(), busOffset));

			// Mark GND and VCC nets
			if (node.isVccSource()) {
				net.setType(NetType.VCC);
			}
			else if (node.isGndSource()) {
				net.setType(NetType.GND);
			}

			//if (net.getName().equals("vga_o[76]")) {
			//	System.out.println(" --> " + pinname  + " " + node.getPin(pinname) + " " + node.isMacro());
			//}

			net.connectToPin(node.getPin(pinname));
		}
	}

	/*
	 * Vivado ports that are buses are named portName[15:0]
	 * This function will return the "portName" portion of the bus name
	 */
	private static String getPortNameSuffix(String portName) {

		int bracketIndex = portName.indexOf("[");
		return bracketIndex == -1 ? portName : portName.substring(0, bracketIndex);
	}

	/*
	 * Creates a list of RapidSmith cell properties from an EDIF property list
	 */
	private static List<edu.byu.ece.rapidSmith.design.subsite.Property> createCellProperties(PropertyList edifPropertyList) {
		List<edu.byu.ece.rapidSmith.design.subsite.Property> cellProperties = new ArrayList<>();

		if (edifPropertyList != null) {
			for (String keyName : edifPropertyList.keySet()) {
				edu.byu.ece.edif.core.Property property = edifPropertyList.getProperty(keyName);
				edu.byu.ece.rapidSmith.design.subsite.Property prop = new edu.byu.ece.rapidSmith.design.subsite.Property(property.getName(), PropertyType.EDIF, getValueFromEdifType(property.getValue()));
				cellProperties.add(prop);
			}
		}

		return cellProperties;
	}

	/*
	 * Converts an EdifTypedValue to the corresponding native Java type
	 */
	private static Object getValueFromEdifType(EdifTypedValue typedValue) {

		Object value;
		if (typedValue instanceof IntegerTypedValue) {
			value = ((IntegerTypedValue)typedValue).getIntegerValue();
		}
		else if (typedValue instanceof BooleanTypedValue) {
			value = ((BooleanTypedValue)typedValue).getBooleanValue();
		}
		else  { // default is string type
			value = ((StringTypedValue)typedValue).getStringValue();
		}
		return value;
	}

	/*
	 * Because EDIF files reverse the index of bus members, this function
	 * is used to get the original index of a port into a bus.
	 */
	private static int reverseBusIndex(int width, int busMember, int offset) {
		return width - 1 - busMember + offset;
	}

	private static void collapseStaticNets(CellDesign design, CellLibrary libCells, List<CellNet> vccNets, List<CellNet> gndNets) {

		// Create new global VCC/GND cells and nets
		Cell globalVCC = new Cell("RapidSmithGlobalVCC", libCells.getVccSource());
		Cell globalGND = new Cell("RapidSmithGlobalGND", libCells.getGndSource());
		CellNet globalVCCNet = new CellNet("RapidSmithGlobalVCCNet", NetType.VCC);
		CellNet globalGNDNet = new CellNet("RapidSmithGlobalGNDNet", NetType.GND);

		// Connect the global sources to the global nets
		globalVCCNet.connectToPin(globalVCC.getOutputPins().iterator().next());
		globalGNDNet.connectToPin(globalGND.getOutputPins().iterator().next());

		// Add all VCC/GND sink pins to the global nets
		for(CellNet net : vccNets) {
			transferSinkPins(net, globalVCCNet);
		}

		for(CellNet net : gndNets) {
			transferSinkPins(net, globalGNDNet);
		}

		// Remove the old VCC/GND cells from the list
		List<Cell> cellsToRemove = new ArrayList<>();
		for (Cell cell : design.getCells()) {
			if (cell.isVccSource() || cell.isGndSource()) {
				cellsToRemove.add(cell);
			}
		}
		cellsToRemove.forEach(design::removeCell);

		// Add the new master cells/nets to the design
		design.addCell(globalVCC);
		design.addNet(globalVCCNet);
		design.addCell(globalGND);
		design.addNet(globalGNDNet);

		// For macro pins tied to power or ground, make them point to the appropriate global static net
		for (Cell c : design.getCells()) {
			for (CellPin cp : c.getPins()) {
				if (cp.getNet()!= null) {
					if (cp.getNet() != globalGNDNet && cp.getNet().getType() == NetType.GND)
						cp.setMacroPinToGlobalNet(globalGNDNet);
					else if (cp.getNet() != globalVCCNet && cp.getNet().getType() == NetType.VCC)
						cp.setMacroPinToGlobalNet(globalVCCNet);
				}
			}
		}
	}

	private static void transferSinkPins(CellNet oldNet, CellNet newNet) {
		Collection<CellPin> sinkPins = oldNet.getSinkPins();
		oldNet.detachNet();
		oldNet.unrouteFull();
		newNet.connectToPins(sinkPins);
	}


	// TODO: Refactor this. It's messy, confusing, inefficient, and there is probably a better solution.
	private static void transformCarryCells(CellDesign design, CellLibrary libCells) {
		// Find the beginning of carry chains
		Collection<Cell> xorInitCells = design.getCells().stream()
				.filter(cell -> cell.getType().equals("XORCY"))
				.filter(cell -> cell.getPin("CI").getNet().isStaticNet())
				.collect(Collectors.toList());
		for (Cell xorCell : xorInitCells) {
			
			// Get the corresponding MUXCY cell (if it exists)
			// The XORCY and MUXCY both share an input (the S input pin for MUXCY and the LI input pin for XORCY)
			CellNet initMuxSelectNet = xorCell.getPin("LI").getNet();
			Cell muxCell = null;
			Collection<CellPin> muxSinkPins = initMuxSelectNet.getSinkPins().stream()
					.filter(cellPin -> cellPin.getCell().getType().equals("MUXCY"))
					.collect(Collectors.toList());

			if (muxSinkPins.size() > 0) {
				assert (muxSinkPins.size() == 1);
				muxCell = muxSinkPins.iterator().next().getCell();
			}

			boolean chainStart = true;
			Cell carryCell = new Cell(xorCell.getName() + "_CARRY4", libCells.get("CARRY4"));
			int i = 0;

			Cell nextMuxCell = null;
			Cell nextXorCell = null;

			// Replace xorcell
			while (xorCell != null) {

				// XOR inputs
				for (CellPin cellPin : xorCell.getInputPins()) {
					CellNet net = cellPin.getNet();

					switch (cellPin.getName()) {
						case "CI":
							if (chainStart && i == 0) {
								net.connectToPin(carryCell.getPin("CYINIT"));
							} else if (i == 0)
								net.connectToPin(carryCell.getPin("CI"));
							break;
						case "LI":
							net.connectToPin(carryCell.getPin("S" + "[" + i + "]"));
							break;
						default:
							System.out.println("WARNING! UNEXPECTED XOR CELL PIN");
							break;
					}

					net.disconnectFromPin(cellPin);

				}

				// XOR outputs
				for (CellPin cellPin : xorCell.getOutputPins()) {
					CellNet net = cellPin.getNet();
					net.disconnectFromPin(cellPin);
					// O pin
					if (cellPin.getName().equals("O")) {
						net.connectToPin(carryCell.getPin("O" + "[" + i + "]"));
					}
				}

				// MUX inputs
				if (muxCell != null) {
					for (CellPin cellPin : muxCell.getInputPins()) {
						CellNet net = cellPin.getNet();
						if (net != null) {
							if (cellPin.getName().equals("CI")) {
								if (chainStart) {
									if (!net.getPins().contains(carryCell.getPin("CYINIT")))
										net.connectToPin(carryCell.getPin("CYINIT"));
								} else  if (i == 0) {
									if (!net.getPins().contains(carryCell.getPin("CI")))
										net.connectToPin(carryCell.getPin("CI"));
								} // else nothing needs to be done
							} else {
								// either DI or S
								CellPin carryPin = carryCell.getPin(cellPin.getName() + "[" + i + "]");
								if (!net.getPins().contains(carryPin))
									net.connectToPin(carryPin);

							}
							net.disconnectFromPin(cellPin);

						}
					}
					// MUX outputs
					CellPin muxOutPin = muxCell.getPin("O");
					CellNet muxOutNet =muxOutPin.getNet();

					// If connecting to a different carry cell
					if (muxOutNet.getSinkPins().size() > 0 && i == 3) {
						// The nets for each sink pin should be the same
						CellNet net = muxOutNet.getSinkPins().iterator().next().getNet();

						// disconnect from source
						net.disconnectFromPin(muxOutPin);

						// connect to carry4 source
						net.connectToPin(carryCell.getPin("CO[3]"));
					}

					for (CellPin pin : muxOutNet.getSinkPins()) {
						Cell sinkCell = pin.getCell();
						switch (sinkCell.getType()) {
							case "MUXCY":
								// Found another internal cell of a carry cell
								nextMuxCell = sinkCell;
								break;
							case "XORCY":
								nextXorCell = sinkCell;
								break;
							default:
								// CO sink
								CellNet coNet = pin.getNet();
								//coNet.disconnectFromPin(pin);

								// disconnect from source
								coNet.disconnectFromPin(muxOutNet.getSourcePin());
								coNet.connectToPin(carryCell.getPin("CO" + "[" + i + "]"));
								break;
						}
					}
				}


				if (muxCell != null)
					design.removeCell(muxCell);

				design.removeCell(xorCell);

				chainStart = false;
				muxCell = nextMuxCell;
				nextMuxCell = null;
				xorCell = nextXorCell;
				nextXorCell = null;

				if (xorCell == null)
					design.addCell(carryCell);
				else if (i == 3) {
					design.addCell(carryCell);
					carryCell = new Cell(xorCell.getName() + "_CARRY4", libCells.get("CARRY4"));
					i = 0;
				}
				else
					i++;
			}
		}

		// It's possible for there to just be MUXCY's chained together with no XORCY's as well...
		// so check for chains starting with MUXCY.
		Collection<Cell> muxInitCells = design.getCells().stream()
				.filter(cell -> cell.getType().equals("MUXCY"))
				.filter(cell -> cell.getPin("CI").getNet().isStaticNet())
				.collect(Collectors.toList());

		for (Cell muxCell : muxInitCells) {

			// Get the corresponding XORCY cell (if it exists)
			// The XORCY and MUXCY both share an input (the S input pin for MUXCY and the LI input pin for XORCY)
			CellNet initMuxSelectNet = muxCell.getPin("S").getNet();
			//Cell muxCell = null;
			Cell xorCell = null;
			Collection<CellPin> xorSinkPins = initMuxSelectNet.getSinkPins().stream()
					.filter(cellPin -> cellPin.getCell().getType().equals("XORCY"))
					.collect(Collectors.toList());

			if (xorSinkPins.size() > 0) {
				assert (xorSinkPins.size() == 1);
				xorCell = xorSinkPins.iterator().next().getCell();
			}

			boolean chainStart = true;
			Cell carryCell = new Cell(muxCell.getName() + "_CARRY4", libCells.get("CARRY4"));
			int i = 0;

			Cell nextMuxCell = null;
			Cell nextXorCell = null;

			// Replace muxCell
			while (muxCell != null) {

				// XOR inputs
				if (xorCell != null) {
					for (CellPin cellPin : xorCell.getInputPins()) {
						CellNet net = cellPin.getNet();

						switch (cellPin.getName()) {
							case "CI":
								if (chainStart && i == 0) {
									net.connectToPin(carryCell.getPin("CYINIT"));
								} else if (i == 0)
									net.connectToPin(carryCell.getPin("CI"));
								break;
							case "LI":
								net.connectToPin(carryCell.getPin("S" + "[" + i + "]"));
								break;
							default:
								System.out.println("WARNING! UNEXPECTED XOR CELL PIN");
								break;
						}

						net.disconnectFromPin(cellPin);

					}

					// XOR outputs
					for (CellPin cellPin : xorCell.getOutputPins()) {
						CellNet net = cellPin.getNet();
						net.disconnectFromPin(cellPin);
						// O pin
						if (cellPin.getName().equals("O")) {
							net.connectToPin(carryCell.getPin("O" + "[" + i + "]"));
						}


					}
				}

				// MUX inputs
				//if (muxCell != null) {
					for (CellPin cellPin : muxCell.getInputPins()) {
						CellNet net = cellPin.getNet();
						if (net != null) {
							if (cellPin.getName().equals("CI")) {
								if (chainStart) {
									if (!net.getPins().contains(carryCell.getPin("CYINIT")))
										net.connectToPin(carryCell.getPin("CYINIT"));
								} else  if (i == 0) {
									if (!net.getPins().contains(carryCell.getPin("CI")))
										net.connectToPin(carryCell.getPin("CI"));
								} // else nothing needs to be done
							} else {
								// either DI or S
								CellPin carryPin = carryCell.getPin(cellPin.getName() + "[" + i + "]");
								if (!net.getPins().contains(carryPin))
									net.connectToPin(carryPin);

							}
							net.disconnectFromPin(cellPin);

						}
					}
					// MUX outputs
					CellPin muxOutPin = muxCell.getPin("O");
					CellNet muxOutNet =muxOutPin.getNet();

					// If connecting to a different carry cell
					if (muxOutNet.getSinkPins().size() > 0 && i == 3) {
						// The nets for each sink pin should be the same
						CellNet net = muxOutNet.getSinkPins().iterator().next().getNet();

						// disconnect from source
						net.disconnectFromPin(muxOutPin);

						// connect to carry4 source
						net.connectToPin(carryCell.getPin("CO[3]"));
					}

					for (CellPin pin : muxOutNet.getSinkPins()) {
						Cell sinkCell = pin.getCell();
						switch (sinkCell.getType()) {
							case "MUXCY":
								// Found another internal cell of a carry cell
								nextMuxCell = sinkCell;
								break;
							case "XORCY":
								nextXorCell = sinkCell;
								break;
							default:
								// CO sink
								CellNet coNet = pin.getNet();

								// disconnect from source
								coNet.disconnectFromPin(muxOutNet.getSourcePin());
								//coNet.disconnectFromPin(pin);

								coNet.connectToPin(carryCell.getPin("CO" + "[" + i + "]"));
								break;
						}
					}
				//}


				//if (muxCell != null)
					design.removeCell(muxCell);

				if (xorCell != null)
					design.removeCell(xorCell);

				chainStart = false;
				muxCell = nextMuxCell;
				nextMuxCell = null;
				xorCell = nextXorCell;
				nextXorCell = null;

				if (muxCell == null)
					design.addCell(carryCell);
				else if (i == 3) {
					design.addCell(carryCell);
					carryCell = new Cell(muxCell.getName() + "_CARRY4", libCells.get("CARRY4"));
					i = 0;
				}
				else
					i++;
			}
		}

	}

	/**
	 * Parses the Edif netlist into a RapidSmith2 CellDesign data structure
	 *
	 * @param edifFile Input EDIF file
	 * @param libCells A Cell library for a specific Xilinx part
	 * @param partName Name of the part for the design. Needs to be set manually for partial devices when using a netlist
	 *                 synthesized by Vivado, since the netlist contains the name of the full part.
	 *
	 * @return The RapidSmith2 representation of the EDIF netlist
	 * @throws FileNotFoundException
	 */
	public static CellDesign parseEdif(String edifFile, CellLibrary libCells, String partName) {
		List<CellNet> vccNets = new ArrayList<>();
		List<CellNet> gndNets = new ArrayList<>();
		Map<EdifPort, Integer> portOffsetMap = new HashMap<>();

		try {
			// parse edif into the BYU edif tools data structures
			EdifEnvironment top = EdifParser.translate(edifFile);
			EdifCell topLevelCell = top.getTopCell();

			// create RS2 cell design
			CellDesign design= new CellDesign(top.getTopDesign().getName(), partName);
			design.getProperties().updateAll(createCellProperties(topLevelCell.getPropertyList()));

			// add all the cells and nets to the design
			processTopLevelEdifPorts(design, topLevelCell.getInterface(), libCells, portOffsetMap);
			processEdifCells(design, topLevelCell.getCellInstanceList(), libCells, vccNets, gndNets);
			processEdifNets(design, topLevelCell.getNetList(), vccNets, gndNets, portOffsetMap);

			transformCarryCells(design, libCells);
			collapseStaticNets(design, libCells, vccNets, gndNets);

			return design;
		}
		catch (FileNotFoundException | ParseException e) {
			throw new Exceptions.ParseException(e);
		}
	}

}
	
