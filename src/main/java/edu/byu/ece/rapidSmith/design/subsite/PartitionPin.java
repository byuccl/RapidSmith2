/*
 * Copyright (c) 2019 Brigham Young University
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

import edu.byu.ece.rapidSmith.device.*;

import java.util.List;

/**
 * This class represents a Partition Pin.
 *
 * @author Dallon Glick
 */
public class PartitionPin extends CellPin {

	/** Unqiue Serial Version for this class */
	private static final long serialVersionUID = -4765068478025538798L;
	/** Name of the pseudo pin */
	private final String name;
	/** Direction of the pseudo pin relative to the cell*/
	private final PinDirection direction;
	/** {@link CellPinType} of the pin*/
	private final CellPinType pinType;
	/** The partition pin's corresponding wire **/
	private final Wire wire;

	/**
	 * PartitionPin Constructor. Creates a partition pin that does not
	 * have a parent cell.
	 *
	 * @param portCell the OOC port
	 * @param pinDir Direction of the pin relative to cell it's attached to
	 */
	public PartitionPin(Cell portCell, Wire wire, PinDirection pinDir) {
		super(portCell);
		this.name = portCell.getName();
		this.direction = pinDir;
		this.wire = wire;

		// If the partition pin wire is an HCLK row, it is a special clock partition pin
		// Is there a better way to identify this other than the name of the wire?
		this.pinType = (wire != null && wire.getName().contains("CLK_HROW")) ? CellPinType.PARTITION_CLK : CellPinType.PARTITION;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getFullName() {
		return name;
	}

	@Override
	public PinDirection getDirection() {
		return direction;
	}

	@Override
	public boolean isPseudoPin() {
		return false;
	}

	@Override
	public boolean isPartitionPin() {
		return true;
	}

	@Override
	public Wire getPartPinWire() {
		return wire;
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

}
