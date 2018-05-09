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
package edu.byu.ece.rapidSmith.device;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class represents the programmable-interconnect-points (PIPs) as found
 * in XDL designs.  The wire names are stored as integers using the WireEnumerator
 * found in the edu.byu.ece.rapidSmith.device class.
 *
 * @author Chris Lavin
 *         Created on: Jun 22, 2010
 */
public final class PIP implements Serializable {
	private static final long serialVersionUID = 122367735864726588L;
	private Wire startWire;
	private Wire endWire;
	//private Boolean used; // used by the static design
	//private Boolean unavailable; // Unavailable as another PIP is using the same start or end wire.

	public PIP(PIP other) {
		this.startWire = other.startWire;
		this.endWire = other.endWire;
	}

	/**
	 * Constructs a new PIP.
	 *
	 * @param startWire the start wire of this PIP
	 * @param endWire the end wire of this PIP
	 * @throws NullPointerException if startWire or endWire is null
	 * @throws IllegalArgumentException if startWire and endWire are in different tiles
	 */
	public PIP(Wire startWire, Wire endWire) {
		Objects.requireNonNull(startWire);
		Objects.requireNonNull(endWire);

		if (startWire.getTile() != endWire.getTile())
			throw new IllegalArgumentException("startWire and endWire in different tiles");

		this.startWire = startWire;
		this.endWire = endWire;
	}

	/**
	 * Returns the enumeration of the start wire of this PIP.
	 *
	 * @return the start wire enumeration
	 */
	public Wire getStartWire() {
		return startWire;
	}

	/**
	 * Returns the enumeration of the end wire of this PIP.
	 *
	 * @return the end wire of this PIP
	 */
	public Wire getEndWire() {
		return endWire;
	}

	/**
	 * @return the tile the PIP is in
	 */
	public Tile getTile() {
		return startWire.getTile();
	}

	@Override
	public int hashCode() {
		return endWire.hashCode() + startWire.hashCode();
	}

	/**
	 * PIPs are equal if they have the same tile, start wire, and end wire.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PIP other = (PIP) obj;

		return (endWire.equals(other.endWire) && startWire.equals(other.startWire)) ||
				(endWire.equals(other.startWire) && startWire.equals(other.endWire));
	}

	public String getName() {
		StringBuilder name = new StringBuilder();
		name.append(startWire.getTile().getName()+"/"+startWire.getTile().getType().getName()+".");
		name.append(startWire.getName());
		if(startWire.getTile().getType().getName().contains("CLB")){
			name.append("->");
		}
		else{
			name.append("->>");
		}
		name.append(endWire.getName());
		return name.toString();
	}
	
	/**
	 * Creates a string representation of this PIP using the WireEnumerator
	 * class.
	 *
	 * @return An XDL-compatible string of this PIP
	 */
	public String toString() {
		return "pip " + startWire.getTile().getName() + " " + startWire.getName() +
				" -> " + endWire.getName();
	}
}
