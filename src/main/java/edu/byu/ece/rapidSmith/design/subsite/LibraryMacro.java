/*
 * Copyright (c) 2016 Brigham Young University
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

package edu.byu.ece.rapidSmith.design.subsite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.util.Exceptions;

/**
 * Represents a primitive MACRO cell from Vivado cell library. A macro library 
 * cell is a wrapper that contains one or more internal leaf library
 * cells that are interconnected. For example, a LUTRAM can be implemented using a macro 
 * (6 internal cells in total are used).
 */
public class LibraryMacro extends LibraryCell {
	private static final long serialVersionUID = 282290704449047358L;

	private Map<LibraryPin, List<InternalPin>> pinMap;
	private Map<String, String> internalToExternalPinMap;
	private Map<String, InternalCell> internalCells;
	private List<InternalNet> internalNets;
	private Map<String, Integer> pinOffsetMap;
	private Map<SiteType, RPM> rpmsMap;
	private static Pattern pinNamePattern;
	private static Pattern lutramPattern;
	
	static 
	{
		pinNamePattern = Pattern.compile("(.+)/([^/]+)$");
		lutramPattern = Pattern.compile("RAM[^B].*");
	}
	
	
	/**
	 * Creates a new library macro
	 * 
	 * @param name String name of the Macro cell.
	 */
	public LibraryMacro(String name) {
		super(name);
		pinMap = new HashMap<>();
		internalToExternalPinMap = new HashMap<>();
		internalCells = new HashMap<>();
		rpmsMap = new HashMap<>();
	}

	@Override
	public boolean isMacro() {
		return true;
	}

	@Override
	public boolean isLut() {
		return false;
	}

	@Override
	public boolean isFlipFlop() {
		return false;
	}

	@Override
	public boolean isLatch() {
		return false;
	}

	/**
	 * Returns {@code true} if the cell is a LUT RAM macro cell, {@code false} otherwise.
	 */
	@Override
	public boolean isLutRamMacro() {
		Matcher m = lutramPattern.matcher(this.getName());
		return m.matches();
	}

	@Override
	public boolean isVccSource() {
		return false;
	}

	@Override
	public boolean isGndSource() {
		return false;
	}

	@Override
	public boolean isPort() {
		return false;
	}

	@Override
	public Integer getNumLutInputs() {
		return null;
	}
	
	@Override
	public List<BelId> getPossibleAnchors() {
		return null;
	}

	@Override
	public Map<String, SiteProperty> getSharedSiteProperties(BelId anchor) {
		return null;
	}
	
	/**
	 * Creates the external-to-internal pin Map of the macro cell.
	 * 
	 * @param libraryPin external macro library pin
	 * @param pinNames List of internal pin names in the form "cellname/pinname" 
	 */
	void addInternalPinConnections(LibraryPin libraryPin, List<String> pinNames) {
		
		if (pinNames.size() == 0) {
			System.err.println("[Warning]: External macro pin " + libraryPin.getName() + " is not connected to any internal pins.");
			return;
		}
		
		assert (pinNames.size() > 0) : "Macro pin " + libraryPin.getName() + " has no internal pin connections";
		
		List<InternalPin> internalPins = new ArrayList<>(pinNames.size());
		
		for(String pinName : pinNames) {
			Matcher m = pinNamePattern.matcher(pinName);
			if (!m.matches()) {
				throw new AssertionError("Invalid pin name in macro XML file: " + pinName);
			}
			assert m.groupCount() == 2;
			
			String cellName = m.group(1);
			String refPinName = m.group(2);
			internalPins.add(new InternalPin(cellName, refPinName));
			this.internalToExternalPinMap.put(cellName + "/" + refPinName, libraryPin.getName());
		}
		
		this.pinMap.put(libraryPin, internalPins);
	}
	
	/**
	 * Adds a new {@link InternalCell} to the library macro. 
	 *  
	 * @param name Relative name of the internal cell  
	 * @param libCell Internal cell type 
	 */
	void addInternalCell(String name, SimpleLibraryCell libCell) {
		this.internalCells.put(name, new InternalCell(name, libCell));
	}

	/**
	 * Adds an entry for an internal cell to the RPM map.
	 * @param siteType the site type of the particular RPM
	 * @param internalCellName name of the internal cell
	 * @param belId the belID the internal cell can be placed on for the RPM
	 * @param rloc the RLOC of the internal cell for the RPM
	 */
	void addRpmCellEntry(SiteType siteType, String internalCellName, BelId belId, String rloc) {
		RPM rpm = rpmsMap.computeIfAbsent(siteType, s -> new RPM(siteType));
		InternalCell internalCell = internalCells.get(internalCellName);
		if (internalCell == null) {
			throw new Exceptions.ParseException("Internal Cell referenced in macro RPM xml \"" + internalCellName +
					"\" not found \"");
		}

		rpm.addCellEntry(internalCell, belId, rloc);
	}
	
	/**
	 * Adds a new {@link InternalNet} to the library macro.
	 * 
	 * @param name Relative name of the internal net
	 * @param fullPinNames A List of pin names that are attached to the internal net
	 */
	void addInternalNet(String name, String type, List<String> fullPinNames) {
		
		// lazily initialize the internal net list (only some macros have this list)
		if (this.internalNets == null) {
			this.internalNets = new ArrayList<>();
		}
		
		this.internalNets.add(new InternalNet(name, type, fullPinNames));
	}
	
	/**
	 * Get the external macro pin that is connected to the specified internal macro pin.
	 * 
	 * @param macroCell top-level macro cell
	 * @param internalPin cell pin attached to an internal cell of the macro cell.
	 * @return The corresponding external {@link CellPin}
	 */
	CellPin getExternalPin(Cell macroCell, CellPin internalPin) {
		assert (macroCell.getLibCell() == this) : "Cell of incorrect type";
		assert (macroCell.isMacro()) : "Input cell to this function must be a macro";
		assert (internalPin.isInternal()) : "Input cell pin to this function must be internal" ;
		
		String pinName = internalPin.getFullName();
		String relativePinName = pinName.substring(pinName.indexOf("/") + 1);
		
		return macroCell.getPin(internalToExternalPinMap.get(relativePinName));		
	}

	/**
	 * Adds RPM EDIF properties to an internal cell. Currently only works for LUTRAM internal cells.
	 * @param internalCell the internal cell to add the RPM properties to.
	 * @param cell the cell instance of the internal cell
	 */
	private void addInternalCellRPMProperties(InternalCell internalCell, Cell cell) {
		// Get site type to RPM map
		// There is only one site type a LUT RAM can be placed on, so grab the first one
		RPM rpm = rpmsMap.values().iterator().next();
		BelIdRlocPair belIdRlocPair = rpm.getCellToBelRlocMap().get(internalCell);

		// Add a U_SET property for the internal cell
		cell.getProperties().update("U_SET", PropertyType.EDIF, cell.getParent().getName());

		// Add the BEL constraint. Internal cells BEL constraints cannot change and need to be in place
		// to ensure Vivado can place the macro (at least in the case of LUT RAMs).
		cell.getProperties().update("BEL", PropertyType.EDIF, belIdRlocPair.getBelId().getName());

		// Set the RLOC property
		cell.getProperties().update("RLOC", PropertyType.EDIF, belIdRlocPair.getRloc());
	}
	
	/**
	 * Creates the internal cells of a macro cell instance. This function is package private
	 * and is not for general use.  
	 * 
	 * @param parent The parent macro cell
	 * @return A map containing the constructed cells
	 */
	Map<String, Cell> constructInternalCells(Cell parent) {
		Map<String, Cell> internalCellMap = new HashMap<>();
		String parentName = parent.getName();
		
		for (InternalCell internalCell : internalCells.values()) {
			String fullCellName = parentName + "/" + internalCell.getName();
			Cell cell = new Cell(fullCellName, internalCell.getLeafCell());
			cell.setParent(parent);
			internalCellMap.put(fullCellName, cell);

			// For now, only add an RPM for LUTRAM macros. LUTRAM macros can only be placed in one exact way
			// (on a SLICEM), whereas other macros have more than one possible type of placement
			// (see the IOBUF macro for an example). Additionally, only LUTRAM macros seem to need to have RPMs in
			// place for Vivado to create the correct placer macros when it reads the EDIF.
			if (parent.getLibCell().isLutRamMacro()) {
				addInternalCellRPMProperties(internalCell, cell);
			}
		}
		
		return internalCellMap;	
	}
		
	/**
	 * Constructs the internal nets of a macro cell instance. This functions is package private
	 * and is not for general use. It should be called after {@link LibraryMacro::constructInternalCells}
	 * has been called.
	 * @param parentName The string name of the parent macro cell. All nets that are created
	 * 					from this function have the prefix "parentName/"
	 * @param internalCellMap Map of cell name to cell instance of the parent macro
	 * @return A map containing the constructed internal {@link CellNet}s for the macro
	 */
	Map<String, CellNet> constructInternalNets(String parentName, Map<String, Cell> internalCellMap) {

		// If there are no internal nets, then return an empty map
		if (internalNets == null || internalNets.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<String, CellNet> internalNetMap = new HashMap<>();
		
		for (InternalNet net: internalNets) {
			String netName = parentName + "/" + net.getName();
			CellNet cellNet = new CellNet(netName, net.getType());
			cellNet.setIsInternal(true);
			
			for (InternalPin pin : net.getInternalPins()) {
				Cell cell = internalCellMap.get(parentName + "/" + pin.getCellName());
				CellPin cellPin = cell.getPin(pin.getPinName());
				
				assert (cellPin != null) : "Unable to find cell pin for macro cell: " +  cell.getName() + "/" + pin.getPinName();
				
				cellNet.connectToPin(cellPin);
			}
			internalNetMap.put(netName, cellNet);
		}
		
		return internalNetMap;
	}
	
	/**
	 * Returns the internal macro pins connected to the specified {@link LibraryPin}.
	 * 
	 * @param parentName Name of the parent macro cell
	 * @param libPin LibraryPin of the peripheral macro pin
	 * @param internalCellMap internal cell map of the macro cell
	 */
	List<CellPin> getInternalPins(String parentName, LibraryPin libPin, Map<String, Cell> internalCellMap) {
		
		List<CellPin> internalCellPins = new ArrayList<CellPin>();
		
		for(InternalPin ipin : this.pinMap.get(libPin)) {
			Cell cell = internalCellMap.get(parentName + "/" + ipin.getCellName());
			internalCellPins.add(cell.getPin(ipin.getPinName()));
		}
		
		return internalCellPins;
	}
	
	void addPinOffset(String pinname, int busMember) {
		
		if (this.pinOffsetMap == null) {
			this.pinOffsetMap = new HashMap<>();
		}
		
		int offset = this.pinOffsetMap.getOrDefault(pinname, Integer.MAX_VALUE);
		
		if (busMember < offset) {
			this.pinOffsetMap.put(pinname, busMember);
		}
	}
	
	public int getPinOffset(String pinname) {
		if (this.pinOffsetMap == null) {
			return 0;
		}
		return this.pinOffsetMap.getOrDefault(pinname, 0);
	}
	
	/* *********************
	/*   Nested Classes
	 * *********************/
	/**
	 * Holds all necessary information to reconstruct an internal cell of a macro  
	 */
	private class InternalCell {
		private final String name;
		private final SimpleLibraryCell leafCell;
		
		public InternalCell(String name, SimpleLibraryCell cell) {
			this.name = name;
			this.leafCell = cell;
		}
		
		public String getName(){
			return name;
		}
		
		public LibraryCell getLeafCell() {
			return leafCell;
		}
	}
	
	/**
	 * Holds all necessary information to reconstruct the internal nets of a macro
	 * after the internal cells have been created. 
	 */
	private class InternalNet {
		private final String name;
		private final NetType type;
		private final List<InternalPin> internalPins;
		
		public InternalNet(String name, String type, List<String> fullPinNames) {
			this.name = name;
			
			if (fullPinNames.size() <= 1) {
				System.err.println("[Warning]: Internal net " + name + " has " + fullPinNames.size() + " connections. This is not enough for a complete net");
			}
			
			// TODO: re-enable this assertion
			//assert (fullPinNames.size() > 1) : "Need at least two pins for a complete net: " + name + " " + type + " " + fullPinNames.size();
			
			this.internalPins = new ArrayList<>();
			
			this.type = NetType.valueOf(type); 
			
			for (String fullPinName : fullPinNames) {
				Matcher m = pinNamePattern.matcher(fullPinName);
				
				if (!m.matches()) {
					throw new AssertionError("Invalid pin name in macro XML file: " + fullPinName);
				}
				assert m.groupCount() == 2;
				
				internalPins.add(new InternalPin(m.group(1), m.group(2)));
			}
		}
		
		public String getName() {
			return name;
		}
		
		public NetType getType() {
			return type;
		}
		
		public List<InternalPin> getInternalPins() {
			return internalPins;
		}
	}
	
	/**
	 *	Represents a pin on an internal macro cell.  
	 */
	private class InternalPin {
		private final String cellName;
		private final String pinName;
		
		public InternalPin(String cellName, String pinName) {
			this.cellName = cellName;
			this.pinName = pinName;
		}
		
		public String getCellName() {
			return cellName;
		}
		
		public String getPinName() {
			return pinName;
		}
	}

	/**
	 * A Bel ID and RLOC pair.
	 */
	private class BelIdRlocPair {
		private final BelId belId;
		private final String rloc;

		public BelIdRlocPair(BelId belId, String rloc) {
			this.belId = belId;
			this.rloc = rloc;
		}

		public BelId getBelId() {
			return belId;
		}

		public String getRloc() {
			return rloc;
		}

	}

	/**
	 * An RPM for a library macro. There may be multiple RPMs for a given library macro.
	 */
	private class RPM {
		/** The site type the whole macro is placed on for this RPM. Used as an identifier **/
		private SiteType siteType;

		public Map<InternalCell, BelIdRlocPair> getCellToBelRlocMap() {
			return cellToBelRlocMap;
		}

		private Map<InternalCell, BelIdRlocPair> cellToBelRlocMap;

		public RPM(SiteType siteType) {
			this.siteType = siteType;
			this.cellToBelRlocMap = new HashMap<>();
		}

		public void addCellEntry(InternalCell internalCell, BelId belId, String rloc) {
			cellToBelRlocMap.put(internalCell, new BelIdRlocPair(belId, rloc));
		}

	}

}
