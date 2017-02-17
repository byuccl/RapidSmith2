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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *  Provides a template of possible cells for a design.
 */
public abstract class LibraryCell implements Serializable {
	private static final long serialVersionUID = -7850247997306342388L;
	/** Name of the Library cell*/
	private final String name;
	/** List of LibraryPins of this LibraryCell */
	private List<LibraryPin> libraryPins;

	/**
	 * Library Cell constructor
	 * @param name String name of the library cell (i.e. LUT6)
	 */
	public LibraryCell(String name) {
		Objects.nonNull(name);
		this.name = name;
	}

	/**
	 * Returns the name of the Library Cell (i.e. LUT6)
	 */
	public final String getName() {
		return name;
	}

	/**
	 * @return the templates of the pins that reside on cells of this type
	 */
	public List<LibraryPin> getLibraryPins() {
		return libraryPins;
	}

	/**
	 * List containing the templates of all this pins on this site
	 */
	public void setLibraryPins(List<LibraryPin> libraryPins) {
		this.libraryPins = libraryPins;
	}
	
	/**
	 * Returns the {@link LibraryPin} on this LibraryCell with the given name.<p>
	 * Operates in O{# of pins} time.
	 */
	public LibraryPin getLibraryPin(String pinName) {
		for (LibraryPin pin : getLibraryPins()) {
			if (pin.getName().equals(pinName))
				return pin;
		}
		return null;
	}
	
	// Abstract functions
	/**
	 * Returns {@code true} if the cell is a library cell is a library macro, 
	 * {@code false} otherwise.
	 */
	abstract public boolean isMacro();
	/**
	 * Returns {@code true} if the cell is a VCC source, {@code false} otherwise.
	 */
	abstract public boolean isVccSource();
	/**
	 * Returns {@code true} if the cell is a GND source, {@code false} otherwise.
	 */
	abstract public boolean isGndSource();
	/**
	 * Returns {@code true} if the cell is a LUT cell (LUT1, LUT2, etc.), {@code false} otherwise.
	 */
	abstract public boolean isLut();
	/**
	 * Returns {@code true} if the cell represents a top-level port cell.
	 */
	abstract public boolean isPort();
	/**
	 * If the cell is a LUT cell, this returns the number of LUT inputs
	 * on the cell.
	 */
	abstract public Integer getNumLutInputs();
	
	/**
	 * Returns a List of {@link BelId} objects that represent where the
	 * current cell can be placed. Since macro cells cannot be placed
	 * directly in RapidSmith, null is returned if this function is used 
	 * for a library macro
	 */
	abstract public List<BelId> getPossibleAnchors();
	/**
	 * For macro cells, returns a list of required bel object that are
	 * needed to place the macro. This functionality is currently unimplemented
	 * and should not be used.
	 * @param anchor Anchor {@link Bel} for the macro
	 */
	abstract public List<Bel> getRequiredBels(Bel anchor);
	
	/**
	 * Returns a list of site properties that are shared across a {@link Bel} Type.
	 * For example, all Flip Flop Bels in a Site, must all be either rising edge or
	 * falling edge. This property would be returned in this list. For Macro cells,
	 * {@code null} is returned.
	 *  
	 * @param anchor {@link Bel}
	 */
	abstract public Map<String, SiteProperty> getSharedSiteProperties(BelId anchor);
		
}
