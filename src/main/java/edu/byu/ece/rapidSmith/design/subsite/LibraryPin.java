package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.PinDirection;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *  Pin on a LibraryCell.
 */
public final class LibraryPin implements Serializable {
	
	/** Unique serialization version UID for this class*/
	private static final long serialVersionUID = 428090750543375520L;
	/** Name of the pin */
	private String name;
	/** The LibraryCell this pin is for */
	private LibraryCell libraryCell;
	/** Direction of the pin */
	private PinDirection direction;
	/** Names of the BelPins that this pin can map to for each BelIdentifier */
	private Map<BelId, List<String>> possibleBelPins;

	/** Type of the LibraryPin. See {@link CellPinType} for the possible types.*/ 
	private CellPinType pinType;
	
	public LibraryPin() {
		possibleBelPins = new HashMap<>();
	}

	public LibraryPin(String name) {
		this.name = name;
		possibleBelPins = new HashMap<>();
	}

	public LibraryPin(String name, LibraryCell libCell, PinDirection direction) {
		this.name = name;
		this.libraryCell = libCell;
		this.direction = direction;
		possibleBelPins = new HashMap<>();
	}

	/**
	 * @return name of the pin (ie I1, I2, O...)
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name name of the pin
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the <code>LibraryCell</code> this pin resides on
	 */
	public LibraryCell getLibraryCell() {
		return libraryCell;
	}

	/**
	 * @param libraryCell the <code>LibraryCell</code> this pin resides on
	 */
	public void setLibraryCell(LibraryCell libraryCell) {
		this.libraryCell = libraryCell;
	}

	/**
	 * @return the direction of the pin relative to its <code>LibraryCell</code>
	 */
	public PinDirection getDirection() {
		return direction;
	}

	/**
	 * @param direction the direction of the pin relative to its <code>LibraryCell</code>
	 */
	public void setDirection(PinDirection direction) {
		this.direction = direction;
	}

	/**
	 * Sets the type  of LibraryPin. See {@link CellPinType} for the possible
	 * pin types. 
	 * 
	 * @param type CellPinType
	 */
	public void setPinType(CellPinType type) {
		pinType = type;
	}
	
	/**
	 * @return The {@link CellPinType} of this LibraryPin
	 */
	public CellPinType getPinType() {
		return pinType;
	}
	
	/**
	 * Returns a <code>Map</code> of <code>BelIdentifiers</code> the <code>LibraryCell</code>
	 * can be placed on to a <code>List</code> of names of <code>BelPins</code> on such BELs
	 * that this pin can be placed onto.
	 * @return possible <code>BelPins</code> this library pin can map to for each possible
	 * <code>BelIdentifier</code>
	 */
	public Map<BelId, List<String>> getPossibleBelPins() {
		return possibleBelPins;
	}

	/**
	 * Returns a <code>List</code> of names of <code>BelPins</code> on BELs of type <code>belName</code>
	 * that this pin can be placed onto.
	 * @param belId <code>BelName</code> to get possible pins for
	 * @return list of possible BelPins on <code>belName</code>
	 */
	public List<String> getPossibleBelPins(BelId belId) {
		return possibleBelPins.get(belId);
	}

	/**
	 * Sets the possibleBelPins.  See {@link #getPossibleBelPins()}.
	 * @param possibleBelPins the possibleBelPins map
	 */
	public void setPossibleBelPins(Map<BelId, List<String>> possibleBelPins) {
		this.possibleBelPins = possibleBelPins;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LibraryPin that = (LibraryPin) o;
		return Objects.equals(name, that.name) &&
				Objects.equals(libraryCell, that.libraryCell);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, libraryCell);
	}
}
