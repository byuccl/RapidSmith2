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

import java.util.List;

import edu.byu.ece.rapidSmith.device.*;

/**
 * This class represents a PseudoCellPin: a pin that can be created dynamically
 * and attached to a cell in the design. It is used to represent a CellPin that is
 * not logically represented, but is physically present. For example, when a SRL16E
 * cell is placed onto a LUT6 of a SLICEM, the A1 pin of the LUT6 needs to be routed
 * to VCC. However, this is not represented in the logical netlist. In this case, 
 * a PseudoCellPin can be created and attached to the SRL16E cell, and mapped onto the
 * A1 pin get a more accurate design representation. These CellPins behave exactly
 * the same way as other pins as in they can attach to nets and map to BelPins. 
 * 
 * @author Thomas Townsend
 */
public class PseudoCellPin extends CellPin {

	
	/** Unqiue Serial Version for this class */
	private static final long serialVersionUID = -4765068478025538798L;
	/** Name of the pseudo pin */
	private final String name;
	/** Direction of the pseudo pin relative to the cell*/
	private final PinDirection direction;
	/** {@link CellPinType} of the pin*/
	private final CellPinType pinType;
		
	/**
	 * PseudoCellPin Constructor. Creates a floating pseudo cell pin that does not
	 * have a parent cell. To attach it to a cell, call {@link Cell#attachPseudoPin(CellPin)} 
	 * with this object as the argument. If you want to create and attach a pseudo pin to 
	 * a cell in a single function call, use {@link Cell#attachPseudoPin(String, PinDirection)}
	 * instead of this constructor.  
	 * 
	 * @param pinName Name of the pin
	 * @param pinDir Direction of the pin relative to cell it's attached to
	 */
	public PseudoCellPin(String pinName, PinDirection pinDir) {
		// Pass null to the super constructor to represent that this pin has not been attached to a cell 
		super(null);
		this.name = pinName;
		this.direction = pinDir;
		this.pinType = CellPinType.PSEUDO;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getFullName() {
		Cell cell = getCell();
		return (cell == null) ? name : cell.getName() + "/" + name ;
	}

	@Override
	public PinDirection getDirection() {
		return direction;
	}

	@Override
	public boolean isPseudoPin() {
		return true;
	}

	@Override
	public boolean isPartitionPin() {
		return false;
	}

	@Override
	public Wire getPartPinWire() {
		return null;
	}

	@Override
	public List<BelPin> getPossibleBelPins() {
		return null;
	}

	@Override
	public List<BelPin> getPossibleBelPins(Bel bel) {
		return null;
	}

	@Override
	public List<String> getPossibleBelPinNames() {
		return null;
	}

	@Override
	public List<String> getPossibleBelPinNames(BelId belId) {
		return null;
	}

	@Override
	public LibraryPin getLibraryPin() {
		return null;
	}

	@Override
	public CellPinType getType() {
		return pinType;
	}

	@Override
	public CellPin getExternalPin() {
		return null;
	}

	@Override
	public boolean isPartitionPin() {
		return false;
	}

	@Override
	public Wire getPartPinWire() {
		return null;
	}
}
