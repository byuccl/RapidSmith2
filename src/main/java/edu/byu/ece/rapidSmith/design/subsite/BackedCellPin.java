package edu.byu.ece.rapidSmith.design.subsite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.PinDirection;

/**
 *  A CellPin that is "backed" by a {@link LibraryPin}. This means they represent 
 *  an instance of a {@link LibraryPin} on a {@link LibraryCell}. 
 *  
 * @author Travis Haroldsen
 * @author Thomas Townsend
 *
 */
public class BackedCellPin extends CellPin {

	/** TODO: Not sure what to put here...*/
	private static final long serialVersionUID = 3866278951715319836L;
	/** LibraryPin forming the basis of this pin */
	private LibraryPin libraryPin;
	
	/**
	 * Package Private Constructor for BackedCellPin objects. This
	 * constructor should only be called by {@link Cell} while creating
	 * the pins of the cell.
	 * 
	 * @param cell Parent cell of this pin
	 * @param libraryPin Backed {@link LibraryPin} of this pin
	 */
	BackedCellPin(Cell cell, LibraryPin libraryPin) {
		super(cell);
		
		assert cell != null;
		assert libraryPin != null;
		
		this.libraryPin = libraryPin;
	}
	
	@Override
	public String getName() {
		return libraryPin.getName();
	}

	@Override
	public String getFullName() {
		return getCell().getName() + "." + getName();
	}

	@Override
	public PinDirection getDirection() {
		return libraryPin.getDirection();
	}

	@Override
	public boolean isPseudoPin() {
		return false;
	}

	@Override
	public List<BelPin> getPossibleBelPins() {
		return getPossibleBelPins(getCell().getAnchor());
	}

	@Override
	public List<BelPin> getPossibleBelPins(Bel bel) {
		List<String> belPinNames = getPossibleBelPinNames(bel.getId());
		List<BelPin> belPins = new ArrayList<>(belPinNames.size());
		
		for (String pinName : belPinNames) {
			belPins.add(bel.getBelPin(pinName));
		}
		return belPins;
	}

	@Override
	public List<String> getPossibleBelPinNames() {
		Cell cell = getCell();
		return (cell.isPlaced()) ? getPossibleBelPinNames(cell.getAnchor().getId()) 
								: Collections.emptyList(); 
	}

	@Override
	public List<String> getPossibleBelPinNames(BelId belId) {
		return libraryPin.getPossibleBelPins().getOrDefault(belId, Collections.emptyList());
	}

	@Override
	public LibraryPin getLibraryPin() {
		return libraryPin;
	}
}
