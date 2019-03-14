package edu.byu.ece.rapidSmith.interfaces;

import edu.byu.ece.edif.core.*;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.util.Exceptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractEdifInterface {

	protected static boolean suppressWarnings = false;
	protected static boolean suppressInfoMessages = false;

	/**
	 * Suppress non-critical warnings while parsing an EDIF file.
	 */
	public static void suppressWarnings(boolean suppress) {
		suppressWarnings = suppress;
	}

	/**
	 * Suppress info messages while parsing an EDIF file.
	 */
	public static void suppressInfoMessages(boolean suppress) {
		suppressInfoMessages = suppress;
	}

	/* ********************
	 * 	 Import Section
	 *********************/

	public abstract CellDesign parseEdif(String edifFile, CellLibrary libCells, String partName);

	/**
	 * Converts EDIF cell instances to equivalent RapidSmith cells and adds them to the design
	 * @param design
	 * @param edifCellInstances
	 * @param libCells
	 * @param vccNets
	 * @param gndNets
	 */
		protected void processEdifCells(CellDesign design, Collection<EdifCellInstance> edifCellInstances, CellLibrary libCells, List<CellNet> vccNets, List<CellNet> gndNets) {
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
	private void handleNamingConflict (CellDesign design, Cell cell) {
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

	/**
	 * Converts EDIF nets to equivalent RapidSmith nets and adds them to the design
	 * @param design
	 * @param edifNets
	 * @param vccNets
	 * @param gndNets
	 * @param portOffsetMap
	 */
	protected void processEdifNets(CellDesign design, Collection<EdifNet> edifNets, List<CellNet> vccNets, List<CellNet> gndNets, Map<EdifPort, Integer> portOffsetMap) {

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

	/**
	 * Builds the connections of CellNet based on the specified EDIF port references. Returns true
	 * if the given net is attached to a top-level port, false otherwise
	 *
	 * TODO: update this once top-level ports are added
	 *
	 * @param portRefs
	 * @param design
	 * @param net
	 * @param portOffsetMap
	 */
	private void processNetConnections(Collection<EdifPortRef> portRefs, CellDesign design, CellNet net, Map<EdifPort, Integer> portOffsetMap) {

		for (EdifPortRef portRef: portRefs) {

			EdifPort port = portRef.getPort();

			// Connects to a top-level port
			if (portRef.isTopLevelPortRef()) {

				String portname = portRef.isSingleBitPortRef() ? port.getOldName() :
						String.format("%s[%d]", getPortNamePrefix(port.getOldName()), reverseBusIndex(port.getWidth(), portRef.getBusMember(), portOffsetMap.get(port)));

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

			net.connectToPin(node.getPin(pinname));
		}
	}

	/**
	 * Vivado ports that are buses are named portName[15:0]
	 * This function will return the "portName" portion of the bus name
	 *
	 * @param portName
	 * @return
	 */
	private String getPortNamePrefix(String portName) {

		int bracketIndex = portName.lastIndexOf("[");
		return bracketIndex == -1 ? portName : portName.substring(0, bracketIndex);
	}

	/**
	 * Creates a list of RapidSmith cell properties from an EDIF property list
	 * @param edifPropertyList
	 * @return
	 */
	protected List<Property> createCellProperties(edu.byu.ece.edif.core.PropertyList edifPropertyList) {
		List<Property> cellProperties = new ArrayList<>();

		if (edifPropertyList != null) {
			for (String keyName : edifPropertyList.keySet()) {
				edu.byu.ece.edif.core.Property property = edifPropertyList.getProperty(keyName);
				Property prop = new Property(property.getName(), PropertyType.EDIF, getValueFromEdifType(property.getValue()));
				cellProperties.add(prop);
			}
		}

		return cellProperties;
	}

	/**
	 * Converts an EdifTypedValue to the corresponding native Java type
	 * @param typedValue
	 * @return
	 */
	private Object getValueFromEdifType(EdifTypedValue typedValue) {

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

	/**
	 * Because EDIF files reverse the index of bus members, this function
	 * is used to get the original index of a port into a bus.
	 *
	 * @param width
	 * @param busMember
	 * @param offset
	 * @return
	 */
	protected static int reverseBusIndex(int width, int busMember, int offset) {
		return width - 1 - busMember + offset;
	}

	/**
	 *
	 * @param design
	 * @param libCells
	 * @param vccNets
	 * @param gndNets
	 */
	protected void collapseStaticNets(CellDesign design, CellLibrary libCells, List<CellNet> vccNets, List<CellNet> gndNets) {
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

	/**
	 *
	 * @param oldNet
	 * @param newNet
	 */
	private static void transferSinkPins(CellNet oldNet, CellNet newNet) {
		Collection<CellPin> sinkPins = oldNet.getSinkPins();
		oldNet.detachNet();
		oldNet.unrouteFull();
		newNet.connectToPins(sinkPins);
	}

	/* *********************
	 *    Export Section
	 ***********************/

	/**
	 *
	 * @param edifOutputFile
	 * @param design
	 * @throws IOException
	 */
	public abstract void writeEdif(String edifOutputFile, CellDesign design) throws IOException;

}
