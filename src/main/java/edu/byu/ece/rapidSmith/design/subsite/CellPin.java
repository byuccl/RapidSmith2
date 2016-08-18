package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.PinDirection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
	/** The BelPin this pin is placed on */
	private BelPin belPin;

	CellPin(Cell cell, LibraryPin libraryPin) {
		assert cell != null;
		assert libraryPin != null;

		this.cell = cell;
		this.libraryPin = libraryPin;
	}

	/**
	 * @return the name of this pin
	 */
	public String getName() {
		return libraryPin.getName();
	}

	public String getFullName() {
		return getCell().getName() + "." + getName();
	}

	/**
	 * @return the direction of this pin from the cell's perspective
	 */
	public PinDirection getDirection() {
		return libraryPin.getDirection();
	}

	/**
	 * @return <code>true</code> if the pin is an input (either INPUT or INOUT)
	 * to the cell
	 */
	public boolean isInpin() {
		switch (libraryPin.getDirection()) {
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
		switch (libraryPin.getDirection()) {
			case OUT:
			case INOUT:
				return true;
			default:
				return false;
		}
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
	 * @return the BelPin this pin is placed on
	 */
	public BelPin getBelPin() {
		return belPin;
	}

	/**
	 * Sets the BelPin this pin is placed on to the pin specified
	 * by the given name.
	 * @param pinName name of the BelPin this pin is placed
	 */
	public void setBelPin(String pinName) {
		setBelPin(getCell().getAnchor().getBelPin(pinName));
	}

	public void setBelPin(BelPin belPin) {
		assert belPin != null;

		this.belPin = belPin;
	}

	public void clearBelPin() {
		if (getPossibleBelPins().size() > 1)
			this.belPin = null;
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
		if (belPinNames == null)
			return null;
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
		if (!cell.isPlaced())
			return null;
		BelId belId = cell.getAnchor().getId();
		return libraryPin.getPossibleBelPins().get(belId);
	}

	/**
	 * Returns the names of the BelPins that this pin can potentially be placed.
	 * @return list of names of possible BelPins
	 */
	public List<String> getPossibleBelPinNames(BelId belId) {
		return libraryPin.getPossibleBelPins().getOrDefault(belId, Collections.emptyList());
	}

	@Override
	public String toString() {
		return "CellPin{" + getFullName() + "}";
	}

	public LibraryPin getLibraryPin() {
		return libraryPin;
	}
}
