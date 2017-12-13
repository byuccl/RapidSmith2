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

import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.BondedType;
import edu.byu.ece.rapidSmith.device.PinDirection;
import edu.byu.ece.rapidSmith.device.PortDirection;
import edu.byu.ece.rapidSmith.device.Site;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 *  Cells represent a primitive logical element in a cell design which can map
 *  onto a single BEL on the device.  Once created, the name and type of the cell
 *  are immutable.  Pins on the cell are initialized upon cell creation based upon
 *  the cell's type.
 */
public class Cell {
	/** Unique name of this instance */
	private String name;
	/** The CellDesign this cell exists in */
	private CellDesign design;
	/** Type of the cell (LUT6, FF, DSP48, ...) */
	private final LibraryCell libCell;
	/** IO Bondedness for this pad cells.  Use internal for non-IO pad cells. */
	private BondedType bonded;
	/** BEL in the device this site is placed on */
	private Bel bel;
	/** Properties of the cell */		
	private final PropertyList properties;
	/** Mapping of pin names to CellPin objects of this cell */
	private final Map<String, CellPin> pinMap;
	/**	Set of pseudo pins attached to the cell */
	private Set<CellPin> pseudoPins;
	
	// Macro specific cell categories.
	
	/** Parent of this cell (for internal cells only)*/
	private Cell parent;
	/** Mapping of cell name to internal cell*/
	private Map<String, Cell> internalCells;
	/** Mapping of net name to internal net*/
	private Map<String, CellNet> internalNets;
	
	/**
	 * Creates a new cell with specified name and type.
	 *
	 * @param name name of the new cell
	 * @param libCell the library cell to base this cell on
	 */
	public Cell(String name, LibraryCell libCell) {
		Objects.requireNonNull(name);
		Objects.requireNonNull(libCell);

		this.name = name;
		this.libCell = libCell;
		this.bonded = BondedType.INTERNAL;

		this.design = null;
		this.bel = null;

		this.properties = new PropertyList(libCell.getDefaultPropertyMap());
		
		this.pinMap = new HashMap<>();
		for (LibraryPin pin : libCell.getLibraryPins()) {
			this.pinMap.put(pin.getName(), new BackedCellPin(this, pin));
		}

		// for port cells, set the direction property
		if(libCell.isPort()) {
			this.properties.update(new Property("Dir", PropertyType.USER, PortDirection.getPortDirectionForImport(this)));
		}
		
		// additional initialization for macro cells
		if (libCell.isMacro()) {
			LibraryMacro macroCell = (LibraryMacro) libCell;
			this.internalCells = macroCell.constructInternalCells(this);
			this.internalNets = macroCell.constructInternalNets(this.name, this.internalCells);
		}
	}

	/**
	 * Returns the name of this cell.
	 */
	public final String getName() {
		return this.name;
	}

	void setName(String name) {
		assert name != null;

		this.name = name;
	}
	
	void setParent(Cell parent) {
		this.parent = parent;
	}
	
	/**
	 * Returns the parent {@link Cell} of the cell if it is an internal cell (inside of a macro).
	 * If the cell is not inside of a macro, null will be returned. 
	 */
	public Cell getParent() {
		return this.parent;
	}
	
	/**
	 * Returns {@code true} if the cell is an internal cell of a macro.
	 */
	public final boolean isInternal() {
		return this.parent != null;
	}
	
	/**
	 * Returns true if this cell is part of a design.
	 */
	public final boolean isInDesign() {
		return design != null;
	}

	/**
	 * Returns the design this cell exists in.
	 */
	public final CellDesign getDesign() {
		return design;
	}

	void setDesign(CellDesign design) {
		assert design != null;

		this.design = design;
	}

	void clearDesign() {
		this.design = null;
	}

	/**
	 * Returns the library cell this cell is backed by.
	 */
	public final LibraryCell getLibCell() {
		return libCell;
	}
	
	/**
	 * Returns the type of cell this is (i.e LUT6)
	 */
	public final String getType() {
		return libCell.getName();
	}

	/**
	 * Returns true if this cell acts as a VCC source.
	 */
	public boolean isVccSource() {
		return getLibCell().isVccSource();
	}

	/**
	 * Returns true if this cell acts as a ground source.
	 */
	public boolean isGndSource() {
		return getLibCell().isGndSource();
	}

	/**
	 * Returns true if this cell is a top-level port of the design
	 */
	public boolean isPort() {
		return getLibCell().isPort();
	}
	
	/**
	 * Returns {@code true} if the current cell is a macro cell, {@code false} otherwise.
	 */
	public boolean isMacro() {
		return libCell.isMacro();
	}
	
	/**
	 * Returns true if the cell is a LUT cell with 1-6 inputs
	 */
	public boolean isLut() {
		return libCell.isLut();
	}
	
	/**
	 * Returns the bondedness of this cell.  IO are either BONDED or UNBONDED,
	 * all others INTERNAL.
	 */
	public BondedType getBonded() {
		return bonded;
	}

	/**
	 * Sets the bonded parameter for this cell.
	 */
	public void setBonded(BondedType bonded) {
		Objects.requireNonNull(bonded);

		this.bonded = bonded;
	}

	/**
	 * Returns true if this cell is placed on a BEL.
	 */
	public final boolean isPlaced() {
		return bel != null;
	}

	/**
	 * Returns the BEL this cell is placed at in the design.
	 *
	 * @return the BEL this cell is placed at in the design
	 */
	public final Bel getBel() {
		return bel;
	}

	public final List<BelId> getPossibleAnchors() {
		return getLibCell().getPossibleAnchors();
	}

	public final List<Bel> getRequiredBels(Bel anchor) {
		return getLibCell().getRequiredBels(anchor);
	}

	/**
	 * Returns the site this cell resides at.
	 *
	 * @return the site this cell resides
	 */
	public final Site getSite() {
		return bel == null ? null : bel.getSite();
	}

	void place(Bel anchor) {
		assert anchor != null;

		this.bel = anchor;
	}

	void unplace() {
		this.bel = null;
	}
	
	/**
	 * Creates a new pseudo pin, and attaches it to the cell. 
	 * 
	 * @param pinName Name of the pin to attach
	 * @param dir Direction of the pseudo pin
	 * 
	 * @throws IllegalArgumentException If a pin with {@code pinName} already exists on this cell
	 * 							an assertion error is thrown.
	 * 
	 * @return The newly created pseudo CellPin. If a pin by {@code pinName}
	 * 			already exists, then null is returned.  
	 */
	public CellPin attachPseudoPin(String pinName, PinDirection dir) {
		
		if ( pinMap.containsKey(pinName) ) {
			throw new IllegalArgumentException("Pin \"" + pinName + "\" already attached to cell  \"" 
									+ getName() + "\". Cannot attach it again");
		}
		
		if ( pseudoPins == null ) {
			pseudoPins = new HashSet<>(5);
		}
		
		CellPin pseudoPin = new PseudoCellPin(pinName, dir);
		pseudoPin.setCell(this);
		
		this.pinMap.put(pinName, pseudoPin);
		this.pseudoPins.add(pseudoPin);
		return pseudoPin;
	}
	
	/**
	 * Attaches an existing pseudo pin to this cell. The pin will be
	 * updated to point to this cell as its new parent. Any BEL pin mappings
	 * that were previously on the pin are now invalid and should be invalidated
	 * before this function is called.
	 * 
	 * @param pin Pseudo pin to attach to the cell
	 * 
	 * @throws IllegalArgumentException If {@code pin} already exists on this cell or 
	 * 			is not a pseudo pin, an exception is thrown.
	 * 
	 * @return <code>true</code> if the pin was successfully attached
	 * 			to the cell. <code>false</code> otherwise 
	 */
	public boolean attachPseudoPin(CellPin pin) {
		if (!pin.isPseudoPin()) {
			throw new IllegalArgumentException("Expected argument \"pin\" to be a pseudo cell pin.\n"
												+ "Cell: " + getName() + " Pin: " + pin.getName()); 
		}
		
		if (pinMap.containsKey(pin.getName())) {
			throw new IllegalArgumentException("Pin \"" + pin.getName() + "\" already attached to cell  \"" 
									+ getName() + "\". Cannot attach it again");
		}
		
		pin.setCell(this);
		this.pinMap.put(pin.getName(), pin);
		this.pseudoPins.add(pin);
		return true;
	}
	
	/**
	 * Removes a pseudo pin from the cell
	 * 
	 * @param pin CellPin to remove
	 * @return <code>true</code> if the pin was attached to the cell 
	 * 			and was successfully removed. <code>false</code> is returned if either
	 * 			{@code pin} is not a pseudo pin, or is not attached to the cell 
	 */
	public boolean removePseudoPin(CellPin pin) {
		
		if (pseudoPins == null || !pseudoPins.contains(pin)) {
			return false; 
		}
		
		pinMap.remove(pin.getName());
		pseudoPins.remove(pin);
		return true;
	}
	
	/**
	 * Removes a pseudo pin from the cell. If you want to remove the pin from the design
	 * completely, you will need to disconnect it from all nets as well.
	 * 
	 * @param pinName Name of the pin to remove
	 * @return The CellPin object removed from the cell. If no matching cell pin is found
	 * 			or the cell pin is not a pseudo pin, null is returned.
	 */ 
	public CellPin removePseudoPin(String pinName) {
		
		CellPin pin = pinMap.get(pinName);
		
		if (pin == null || !pin.isPseudoPin()) {
			return null;
		}
		
		pinMap.remove(pinName);
		pseudoPins.remove(pin);
		return pin;
	}
	
	public boolean removePseudoPins() {
		List<String> pseudoPins = getPseudoPins().stream()
			.map(CellPin::getName)
			.collect(Collectors.toList());

		pseudoPins.forEach(pinMap::remove);
		this.pseudoPins = null;
		return pseudoPins.size() > 0;
	}

	/**
	 * Gets all pseudo pins currently attached to this cell
	 * 
	 * @return A Set of attached pseudo pins
	 */
	public Set<CellPin> getPseudoPins() {
		
		if (pseudoPins == null) {
			return Collections.emptySet();
		}
		
		// TODO: think about creating this unmodifiable during class creation instead of on demand
		return Collections.unmodifiableSet(pseudoPins);
	}
	
	/**
	 * Gets the number of pseudo (fake) pins
	 * currently attached to this cell.
	 * 
	 * @return The number of pseudo pins attached to this cell.
	 */
	public int getPseudoPinCount() {
		return pseudoPins == null ? 0 : pseudoPins.size();
	}
		
	/**
	 * Returns the properties of this cell in a {@link PropertyList}.  The
	 * properties describe the configuration of this cell and can be used
	 * to store user attributes for this cell.
	 *
	 * @return a {@code PropertyList} containing the properties of this cell
	 */
	public final PropertyList getProperties() {
		return properties;
	}

	public Map<SiteProperty, Object> getSharedSiteProperties() {
		return getSharedSiteProperties(bel.getId());
	}

	public Map<SiteProperty, Object> getSharedSiteProperties(BelId belId) {
		Map<SiteProperty, Object> returnMap = new HashMap<>();

		Map<String, SiteProperty> referenceMap =
				getLibCell().getSharedSiteProperties(belId);
		for (Map.Entry<String, SiteProperty> e : referenceMap.entrySet()) {
			if (properties.has(e.getKey()) && properties.get(e.getKey()).getType() == PropertyType.DESIGN) {
				returnMap.put(e.getValue(), properties.getValue(e.getKey()));
			}
		}
		return returnMap;
	}

	/**
	 * Returns the nets that connect to the pins of this cell.
	 */
	public final Collection<CellNet> getNetList() {
		return pinMap.values().stream()
				.filter(pin -> pin.getNet() != null)
				.map(CellPin::getNet)
				.collect(Collectors.toSet());
	}

	/**
	 * Returns the pin on this cell with the specified name.
	 */
	public final CellPin getPin(String pinName) {
		return pinMap.get(pinName);
	}

	/**
	 * Returns all of the pins on this net.  The returned collection should not
	 * be modified by the user.
	 *
	 * @return A collection of unique pins being used on this cell.
	 */
	public final Collection<CellPin> getPins() {
		return pinMap.values();
	}

	/**
	 * Returns all of the output pins on this net.
	 */
	public final Collection<CellPin> getOutputPins() {
		return pinMap.values().stream()
				.filter(CellPin::isOutpin)
				.collect(Collectors.toList());
	}

	/**
	 * Returns all of the input pins on this net.
	 */
	public final Collection<CellPin> getInputPins() {
		return pinMap.values().stream()
				.filter(CellPin::isInpin)
				.collect(Collectors.toList());
	}

	/**
	 * Clears the cell pin -> bel pin mappings of all pins on this cell.
	 */
	public void clearPinMappings() {
		getPins().forEach(CellPin::clearPinMappings);
	}

	/**
	 * Returns a collections of the internal cells within a macro cell.
	 * If this function is used on a leaf cell, then null will be returned.
	 */
	public Collection<Cell> getInternalCells() {
		return isMacro() ? this.internalCells.values() : null;
	}
	
	/**
	 * Returns a list of internal cell pins that are connected to the
	 * specified external cell pin for the macro. This is package private,
	 * and should not be used by users of RapidSmith. 
	 * 
	 * @param pin External macro pin
	 */
	List<CellPin> mapToInternalPins(CellPin pin) {
		assert (libCell.isMacro()) : "Only macro cells can call this function!";
		return ((LibraryMacro)libCell).getInternalPins(this.name, pin.getLibraryPin(), this.internalCells); 
	}
	
	/**
	 * Returns a collections of the internal nets within a macro cell.
	 * If this function is used on a leaf cell, then null will be returned.
	 */	
	public Collection<CellNet> getInternalNets() {
		return isMacro() ? this.internalNets.values() : null;
	}
	
	/**
	 * Returns the external macro pin of an internal {@link CellPin}. This
	 * is package private and so should not be used by users.
	 *  
	 * @param internalPin Internal {@link CellPin}
	 */
	CellPin getExternalPin(CellPin internalPin) {
		
		return ((LibraryMacro)libCell).getExternalPin(this, internalPin);
	}
	
	/**
	 * Returns a deep copy of this cell.  The deep copy does not have any design
	 * or cluster information.
	 * @return a deep copy of this cell
	 */
	public Cell deepCopy() {
		return deepCopy(Collections.emptyMap());
	}

	/**
	 * Returns a deep copy of this cell except with changes specified in the
	 * changes described in the <i>changes</i> argument map.  The valid changes
	 * described in the map that are accepted are <br>
	 * <ul>
	 *     <li>name -> String</li>
	 *     <li>type -> LibraryCell</li>
	 * </ul><br>
	 * Other values in the map are ignored.
	 *
	 * @param changes map containing the changes to be made to the cell copy
	 * @return a deep copy of this cell with specified changes
	 */
	public Cell deepCopy(Map<String, Object> changes) {
		return deepCopy(Cell::new, changes);
	}

	protected Cell deepCopy(BiFunction<String, LibraryCell, Cell> cellFactory) {
		return deepCopy(cellFactory, Collections.emptyMap());
	}

	protected Cell deepCopy(
			BiFunction<String, LibraryCell, Cell> cellFactory,
			Map<String, Object> changes
	) {
		String name;
		LibraryCell libCell;

		if (changes.containsKey("name"))
			name = (String) changes.get("name");
		else
			name = getName();

		if (changes.containsKey("type"))
			libCell = (LibraryCell) changes.get("type");
		else
			libCell = getLibCell();

		// Call the constructor to get a new cell
		Cell cellCopy = cellFactory.apply(name, libCell);
		
		// Make copies of and attach existing pseudo-pins
		for (CellPin pcp : this.getPseudoPins()) {
			cellCopy.attachPseudoPin(pcp.getName(), pcp.getDirection());
		}
		
		cellCopy.setBonded(getBonded());
		getProperties().forEach(p ->
				cellCopy.properties.update(copyAttribute(getLibCell(), libCell, p))
		);
		return cellCopy;
	}

	private Property copyAttribute(LibraryCell oldType, LibraryCell newType, Property orig) {
		if (!oldType.equals(newType) && orig.getKey().equals(oldType.getName())) {
			return new Property(newType.getName(), orig.getType(), orig.getValue());
		} else {
			return orig.deepCopy();
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return "Cell{" + getName() + " " + (isPlaced() ? "@" + getBel().getFullName() : "") + "}";
	}
}
