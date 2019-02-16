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

import edu.byu.ece.rapidSmith.device.BelId;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a Vivado leaf primitive library cell. Examples of leaf cells
 * include LUTs (LUT1, LUT2, ..., LUT6), Flip Flops (FDRE), BRAMs (RAMB36E1), etc. 
 */
public class SimpleLibraryCell extends LibraryCell {
	private static final long serialVersionUID = 6378678352365270213L;
	/** List of types of BELs cells of this type can be placed on */
	private List<BelId> compatibleBels;
	/** Stores the properties of a cell that are part of a control set*/
	private Map<BelId, Map<String, SiteProperty>> sharedSiteProperties;

	private boolean isVccSource;
	private boolean isGndSource;
	private Integer numLutInputs = null;
	private boolean isPort;

	private final List<String> FLIP_FLOP_CELLS = Arrays.asList("FDCE", "FDPE", "FDRE", "FDSE");
	private final List<String> LATCH_CELLS = Arrays.asList("LDCE", "LDPE");

	/**
	 * Creates a new simple library cell with the specified name.
	 * @param name Name of the library cell (i.e. LUT6)
	 */
	public SimpleLibraryCell(String name) {
		super(name);
	}

	@Override
	public boolean isMacro() {
		return false;
	}

	@Override
	public boolean isPort() {
		return isPort;
	}
	
	@Override
	public boolean isVccSource() {
		return isVccSource;
	}
	
	/**
	 * Marks this library cell as a VCC source.
	 * 
	 * @param isVccSource {@code true} to mark cell as a VCC source, {@code false} otherwise
	 */
	public void setVccSource(boolean isVccSource) {
		this.isVccSource = isVccSource;
	}

	@Override
	public boolean isGndSource() {
		return isGndSource;
	}

	/**
	 * Marks this library cell as a top-level design port.
	 * 
	 * @param isPort {@code true} to mark cell as top-level port, {@code false} otherwise
	 */
	public void setIsPort(boolean isPort) {
		this.isPort = isPort;
	}
	
	/**
	 * Marks this library cell as a GND source.
	 * 
	 * @param isGndSource {@code true} to mark cell as a GND source, {@code false} otherwise
	 */
	public void setGndSource(boolean isGndSource) {
		this.isGndSource = isGndSource;
	}

	/**
	 * Set the number of LUT inputs on the cell
	 * 
	 * @param numInputs Integer number of LUT inputs
	 */
	public void setNumLutInputs(Integer numInputs) {
		this.numLutInputs = numInputs;
	}

	/**
	 * Returns {@code true} if the library cell is a flip-flop (FDRE, FDSE, etc.), {@code false} otherwise.
	 */
	@Override
	public boolean isFlipFlop() {
		return FLIP_FLOP_CELLS.contains(this.getName());
	}

	/**
	 * Returns {@code true} if the library cell is a latch (LDCE, LDPE, etc.), {@code false} otherwise.
	 */
	@Override
	public boolean isLatch() {
		return LATCH_CELLS.contains(this.getName());
	}

	/**
	 * Returns {@code true} if the cell is a LUT RAM macro cell, {@code false} otherwise.
	 */
	@Override
	public boolean isLutRamMacro() {
		return false;
	}

	/**
	 * Returns {@code true} if the library cell is a LUT (LUT1, LUT2, etc.), {@code false} otherwise.
	 */
	@Override
	public boolean isLut() {
		return numLutInputs != null;
	}

	@Override
	public Integer getNumLutInputs() {
		return numLutInputs;
	}

	/**
	 * @return list of the {@link BelId}s of BELs that cells of this type can be
	 * placed on
	 */
	@Override
	public List<BelId> getPossibleAnchors() {
		return compatibleBels;
	}

	/**
	 * List containing the {@link BelId}s of BELs that cells of this type can be
	 * placed on.
	 */
	public void setPossibleBels(List<BelId> possibleBels) {
		this.compatibleBels = possibleBels;
	}

	@Override
	public Map<String, SiteProperty> getSharedSiteProperties(BelId anchor) {
		return sharedSiteProperties.get(anchor);
	}
	
	/**
	 * Sets the shared site properties for the cell
	 * @param sharedSiteProperties
	 */
	public void setSharedSiteProperties(
			Map<BelId, Map<String, SiteProperty>> sharedSiteProperties
	) {
		this.sharedSiteProperties = sharedSiteProperties;
	}

	/**
	 * Library cells with the same name are considered equal.
	 */
	@Override
	public boolean equals(Object o) {
		// I'd prefer this to be identity equals but I think that would break
		// code somewhere.
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LibraryCell that = (LibraryCell) o;
		return Objects.equals(getName(), that.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName());
	}

	@Override
	public String toString() {
		return "LibraryCell{" +
				"name='" + getName() + '\'' +
				'}';
	}
}
