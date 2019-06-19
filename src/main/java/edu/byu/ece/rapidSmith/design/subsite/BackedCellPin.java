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
import java.util.List;

import edu.byu.ece.rapidSmith.device.*;

/**
 *  A CellPin that is "backed" by a {@link LibraryPin}. This means they represent 
 *  an instance of a {@link LibraryPin} on a {@link LibraryCell}. 
 *  
 * @author Travis Haroldsen
 * @author Thomas Townsend
 *
 */
public class BackedCellPin extends CellPin {

	/** Unique serial number for class */
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
		return getCell().getName() + "/" + getName();
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
	public boolean isPartitionPin() {
		return false;
	}

	@Override
	public List<BelPin> getPossibleBelPins() {
		return getPossibleBelPins(getCell().getBel());
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
		return (cell.isPlaced()) ? getPossibleBelPinNames(cell.getBel().getId())
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

	@Override
	public CellPinType getType() {
		return libraryPin.getPinType();
	}

	@Override
	public CellPin getExternalPin() {		
		return isInternal() ? getCell().getParent().getExternalPin(this) : null;
	}

	@Override
	public Wire getPartPinWire() {
		return null;
	}
}
