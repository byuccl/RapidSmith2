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
	private final String name;

	public LibraryCell(String name) {
		Objects.nonNull(name);
		this.name = name;
	}

	public final String getName() {
		return name;
	}

	abstract public boolean isMacro();
	abstract public boolean isVccSource();
	abstract public boolean isGndSource();
	abstract public boolean isLut();
	abstract public boolean isPort();
	abstract public Integer getNumLutInputs();
	abstract public List<LibraryPin> getLibraryPins();
	abstract public List<BelId> getPossibleAnchors();
	abstract public List<Bel> getRequiredBels(Bel anchor);
	abstract public Map<String, SiteProperty> getSharedSiteProperties(BelId anchor);

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
}
