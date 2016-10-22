package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.PinDirection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *  A pin on a Cell.  CellPins connect cells to nets and map to BelPins.
 *  CellPins are obtained through calls to {@link Cell#getPin(String)}.
 */
public class CellPin implements Serializable {
	/** LibraryPin forming the basis of this pin */
	private LibraryPin libraryPin;
	/** The cell this pin resides on */
	private Cell cell;
	/** The net this pin is a member of */
	private CellNet net;
	/** Set of BelPin objects that this pin maps to*/
	private Set<BelPin> belPinMappingSet;
	/** Flag that marks this cellPin as a PseudoPin */
	private boolean isPseudo;
	/** Name of the pin if it is pseudo pin*/
	private String pseudoPinName;
	/** Direction of pin if it is a pseudo pin*/
	private PinDirection pseudoPinDirection;
	 
	
	CellPin(Cell cell, LibraryPin libraryPin) {
		assert cell != null;
		assert libraryPin != null;

		this.cell = cell;
		this.libraryPin = libraryPin;
		this.isPseudo = false;
	}
	
	/**
	 * Private constructor used to create pseudo cell pins
	 */
	private CellPin(Cell cell, String pinName, PinDirection dir) {
		this.cell = cell;
		this.isPseudo = true;
		this.pseudoPinName = pinName;
		this.pseudoPinDirection = dir;
	}
	
	/**
	 * Creates a new pseudo CellPin object. Pseudo cell pins
	 * are not backed by a LibraryCellPin, but they are still
	 * associated with a cell.
	 *  
	 * @param parent The parent cell of this pin.
	 * @return A CellPin object
	 */
	public static CellPin createPseudoPin(Cell parent, String pinName, PinDirection dir) {
		return new CellPin(parent, pinName, dir); 
	}

	/**
	 * @return the name of this pin
	 */
	public String getName() {
		
		return isPseudo ? pseudoPinName : libraryPin.getName();
	}

	public String getFullName() {
		return getCell().getName() + "." + getName();
	}

	/**
	 * @return the direction of this pin from the cell's perspective
	 */
	public PinDirection getDirection() {
		return isPseudo ? pseudoPinDirection : libraryPin.getDirection();
	}

	/**
	 * @return <code>true</code> if the pin is an input (either INPUT or INOUT)
	 * to the cell
	 */
	public boolean isInpin() {
		
		PinDirection pinDirection = (isPseudo) ? pseudoPinDirection : libraryPin.getDirection();
		
		switch (pinDirection) {
			case IN:
			case INOUT:
				return true;
			default:
				return false;
		}
	}

	/**
	 * @return <code>true</code> if the pin is an output (either OUTPUT or INOUT)
	 * to the cell
	 */
	public boolean isOutpin() {
		
		PinDirection pinDirection = (isPseudo) ? pseudoPinDirection : libraryPin.getDirection();
		
		switch (pinDirection) {
			case OUT:
			case INOUT:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Checks if the CellPin is a pseudo pin
	 * 
	 * @return <code>true</code> if the pin is a pseudo pin. <code>false</code> otherwise
	 */
	public boolean isPseudoPin(){
		return isPseudo;
	}
	
	/**
	 * Gets and returns the cell where this pin resides.
	 *
	 * @return The cell where the pin resides.
	 */
	public Cell getCell() {
		return cell;
	}

	void setCell(Cell inst) {
		this.cell = inst;
	}

	void clearCell() {
		this.cell = null;
	}

	public boolean isConnectedToNet() {
		return net != null;
	}

	/**
	 * @return the net attached to this pin
	 */
	public CellNet getNet() {
		return net;
	}

	void setNet(CellNet net) {
		this.net = net;
	}

	void clearNet() {
		this.net = null;
	}
	
	/**
	 * Maps the CellPin to the specified BelPin
	 * 
	 * @param pin BelPin to map this CellPin to 
	 * @return <code>true</code> if the CellPin is not already mapped to the BelPin 
	 */
	public boolean mapToBelPin(BelPin pin) {
		
		if (belPinMappingSet == null) {
			belPinMappingSet = new HashSet<BelPin>();
		}
		
		return belPinMappingSet.add(pin);
	}
	
	/**
	 * Maps the CellPin to all BelPins in the specified collection
	 *  
	 * @param pins Collection of BelPins to map BelPins to. 
	 * @return <code>true</code> if all BelPins in "pins" have not already been mapped to the CellPin
	 */
	public boolean mapToBelPins(Collection<BelPin> pins) {
		
		boolean allPinsMapped = true;
		
		for (BelPin pin : pins) {
			allPinsMapped &= mapToBelPin(pin);
		}
		return allPinsMapped;
	}
	
	/**
	 * Tests if the mapping from this pin to the specified BelPin exists
	 * 
	 * @param pin BelPin to test mapping
	 * @return <code>true</code> if the mapping {@code this} -> {@code pin} exists
	 */
	public boolean isMappedTo(BelPin pin) {
		
		if (belPinMappingSet == null) {
			return false;
		}
		
		return belPinMappingSet.contains(pin);
	}
	
	/**
	 * Tests to see if the pin has been mapped to a BelPin
	 * 
	 * @return <code>true</code> if this pin is mapped to at least one BelPin.
	 */
	public boolean isMapped() {
		return belPinMappingSet != null && !belPinMappingSet.isEmpty();
	}
	
	/**
	 * Returns a set of BelPins that this CellPin is currently mapped to.
	 * If the CellPin is not mapped to any BelPins, and empty set is returned. 
	 */
	public Set<BelPin> getMappedBelPins() {
		return (belPinMappingSet == null) ? Collections.emptySet() : belPinMappingSet; 
	}
	
	/**
	 * Get the BelPin that this CellPin currently maps to. Use this function
	 * if you know that the CellPin is currently mapped to only one BelPin, otherwise
	 * use {@link #getMappedBelPins()} instead.
	 * 
	 * @return The BelPin that this CellPin is mapped to. If the CellPin
	 * is not mapped, <code>null</code> is returned. 
	 */
	public BelPin getMappedBelPin() {
		if (belPinMappingSet == null || belPinMappingSet.isEmpty()) {
			return null;
		}
		else {
			return belPinMappingSet.iterator().next();
		}
	}
	
	/**
	 * Get the number of BelPins that this pin is mapped to. In most cases,
	 * CellPins are mapped to only one BelPin, but it is possible that they can
	 * be mapped to 0 or more than 1 BelPins. 
	 * 
	 *  @return The number of mapped BelPins
	 */
	public int getMappedBelPinCount() {
		return belPinMappingSet == null ? 0 : belPinMappingSet.size();
	}
	
	/**
	 * Removes all CellPin -> BelPin mappings for this pin (i.e. this
	 * pin will no longer map to any BelPins). 
	 */
	public void clearPinMappings() {
		if (getPossibleBelPins().size() > 1) {
			this.belPinMappingSet = null;
		}
	}
	
	/**
	 * Removes the pin mapping to the specified BelPin
	 * 
	 * @param belPin BelPin to un-map
	 */
	public void clearPinMapping(BelPin belPin) {
		if (belPinMappingSet != null && belPinMappingSet.contains(belPin)) {
			belPinMappingSet.remove(belPin);
		}
	}
	
	/**
	 * Returns the BelPins that this pin can potentially be placed.
	 * @return list of possible BelPins
	 */
	public List<BelPin> getPossibleBelPins() {
		return getPossibleBelPins(cell.getAnchor());
	}

	public List<BelPin> getPossibleBelPins(Bel bel) {
		List<String> belPinNames = getPossibleBelPinNames(bel.getId());
		List<BelPin> belPins = new ArrayList<>(belPinNames.size());
		
		for (String pinName : belPinNames) {
			belPins.add(bel.getBelPin(pinName));
		}
		return belPins;
	}

	/**
	 * Returns the names of the BelPins that this pin can potentially be placed.
	 * @return list of names of possible BelPins
	 */
	public List<String> getPossibleBelPinNames() {
		if (!cell.isPlaced() || this.isPseudo) {
			return Collections.emptyList();
		}
		BelId belId = cell.getAnchor().getId();
		return libraryPin.getPossibleBelPins().get(belId);
	}

	/**
	 * Returns the names of the BelPins that this pin can potentially be placed.
	 * @return list of names of possible BelPins
	 */
	public List<String> getPossibleBelPinNames(BelId belId) {

		// pseudo pins do not have a backing library pin, so return an empty list
		if (isPseudo) {
			return Collections.emptyList();
		}
		
		List<String> namesTmp = libraryPin.getPossibleBelPins().getOrDefault(belId, Collections.emptyList());
		return (namesTmp == null) ? Collections.emptyList() : namesTmp;
	}

	@Override
	public String toString() {
		return "CellPin{" + getFullName() + "}";
	}

	public LibraryPin getLibraryPin() {
		return libraryPin;
	}
}
