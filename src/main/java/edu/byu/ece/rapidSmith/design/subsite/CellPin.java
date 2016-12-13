package edu.byu.ece.rapidSmith.design.subsite;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.PinDirection;

/**
 *  A pin on a Cell. CellPins connect cells to nets and map to BelPins.
 *  CellPins are obtained through calls to {@link Cell#getPin(String)}.
 *  
 *  @author Travis Haroldsen
 *  @author Thomas Townsend 
 *
 */
public abstract class CellPin implements Serializable {
 
	/** Unique Serial number for this class */
	private static final long serialVersionUID = 2612839140455524421L;
	/** The cell this pin resides on */
	private Cell cell;
	/** The net this pin is a member of */
	private CellNet net;
	/** Set of BelPin objects that this pin maps to*/
	private Set<BelPin> belPinMappingSet;

	/**
	 * Protected Constructor to create a new CellPin
	 * 
	 * @param cell Parent cell of the CellPin. Can be <code>null</code>
	 * 				if the cell is an unattached pseudo pin 
	 */
	protected CellPin(Cell cell) {
		this.cell = cell;
	}
	
	/**
	 * Gets and returns the cell where this pin resides.
	 *
	 * @return The cell where the pin resides.
	 */
	public Cell getCell() {
		return cell;
	}

	/**
	 * Sets the {@link Cell} that this pin is attached to. This is package 
	 * private, and should not be called by regular users.
	 * 
	 * @param inst {@link Cell} to mark as the parent of this pin
	 */
	void setCell(Cell inst) {
		this.cell = inst;
	}

	/**
	 * Unattaches this pin from the {@link Cell} is was attached to. This
	 * is package private, and should not be called by regular users
	 */
	void clearCell() {
		this.cell = null;
	}

	/**
	 * @return <code>true</code> is this CellPin is attached to a net
	 */
	public boolean isConnectedToNet() {
		return net != null;
	}

	/**
	 * @return the net attached to this pin
	 */
	public CellNet getNet() {
		return net;
	}

	/**
	 * Sets the {@link CellNet} object that this CellPin is
	 * connected to. This call is package private and should
	 * not be used by regular users.
	 * 
	 * @param net {@link CellNet} to connect the CellPin to.
	 */
	void setNet(CellNet net) {
		this.net = net;
	}

	/**
	 * Disconnects this pin from the net is was connected to. This
	 * is package private and should not be called by regular users.
	 */
	void clearNet() {
		this.net = null;
	}
	
	/**
	 * @return <code>true</code> if the pin is an input (either INPUT or INOUT)
	 * to the cell. <code>false</code> otherwise.
	 */
	public boolean isInpin() {
		switch (getDirection()) {
		case IN:
		case INOUT:
			return true;
		default:
			return false;
		}
	}

	/**
	 * @return <code>true</code> if the pin is an output (either OUTPUT or INOUT)
	 * to the cell. <code>false</code> otherwise.
	 */
	public boolean isOutpin(){
		switch (getDirection()) {
		case OUT:
		case INOUT:
			return true;
		default:
			return false;
		}
	}
	
	/**
	 * Maps the CellPin to the specified BelPin
	 * 
	 * @param pin BelPin to map this CellPin to 
	 * @return <code>true</code> if the CellPin is not already mapped to the BelPin 
	 */
	public boolean mapToBelPin(BelPin pin) {
		
		if (belPinMappingSet == null) {
			belPinMappingSet = new HashSet<>();
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
		return belPinMappingSet != null && belPinMappingSet.contains(pin);

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
		return (belPinMappingSet == null) ? Collections.emptySet() : Collections.unmodifiableSet(belPinMappingSet); 
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
	 * Prints the CellPin object in the form: 
	 * "CellPin{ parentCellName.CellPinName }"
	 */
	@Override
	public String toString() {
		return "CellPin{" + getFullName() + "}";
	}
	
	/* **************************************************************
	 *  List of Abstract Functions to be implemented by sub-classes 
	 * **************************************************************/
	
	/**
	 * @return the name of this pin
	 */
	public abstract String getName();
	
	/**
	 * @return the full name of the CellPin in the form "cellName/pinName"
	 */
	public abstract String getFullName();

	/**
	 * @return the direction of this pin from the cell's perspective
	 */
	public abstract PinDirection getDirection();  
	
	/** 
	 * @return <code>true</code> if the pin is a pseudo pin. <code>false</code> otherwise.
	 */
	public abstract boolean isPseudoPin(); 
	
	/**
	 * Returns the BelPins that this pin can potentially be mapped onto. The BEL that this pin's parent
	 * cell is placed on is used to determine the potential mappings. This function should 
	 * NOT be called on pseudo CellPin objects because pseudo pins are not backed
	 * by an associated {@link LibraryPin}.
	 * 
	 * @return list of possible BelPins (which might be empty). If the caller is a pseudo pin, 
	 * 			<code>null</code> is returned.
	 */
	public abstract List<BelPin> getPossibleBelPins(); 

	/**
	 * Returns the BelPins that this pin can potentially be mapped to on the specified {@code bel}. 
	 * This function should NOT be called on pseudo CellPin objects because pseudo pins are not backed
	 * by an associated {@link LibraryPin}.
	 * 
	 * @param bel The BEL to get the possible pin maps for this pin
	 * @return list of possible BelPins (which could be empty). If the caller is a pseudo pin, 
	 * 			<code>null</code> is returned
	 */
	public abstract List<BelPin> getPossibleBelPins(Bel bel); 
	
	/**
	 * Returns the names of the BelPins that this pin can potentially be mapped onto. This function
	 * should NOT be called if the CellPin is a pseudo CellPin because pseudo pins are not backed
	 * by an associated {@link LibraryPin}. 
	 * 
	 * @return list of names of possible BelPins (which could be empty). If the caller is a pseudo pin, 
	 * 			<code>null</code> is returned
	 */
	public abstract List<String> getPossibleBelPinNames();  
	
	/**
	 * Returns the names of the BelPins that this pin can potentially be mapped onto.
	 * This function should NOT be called if the CellPin is a pseudo CellPin because 
	 * pseudo pins are not backed by an associated {@link LibraryPin}.
	 * 
	 * @return list of names of possible BelPins (which could be empty). If the caller is a pseudo pin, 
	 * 			<code>null</code> is returned
	 */
	public abstract List<String> getPossibleBelPinNames(BelId belId);  
	
	/**
	 * @return the backing {@link LibraryPin} of this CellPin. If the caller is a 
	 * 			pseudo cell pin, <code>null</code> is returned because it has no
	 * 			backing {@link LibraryPin}.
	 */
	public abstract LibraryPin getLibraryPin();  
	
	/**
	 * @return The {@link CellPinType} of this pin. 
	 */
	public abstract CellPinType getType();
}
