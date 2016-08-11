/*
 * Copyright (c) 2010 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.design;

import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.WireConnection;
import edu.byu.ece.rapidSmith.device.WireEnumerator;

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
public class PIP implements Comparable<PIP>, Serializable {

	private static final long serialVersionUID = 122367735864726588L;
	private Wire startWire;
	private Wire endWire;

	/**
	 * Constructs an empty PIP.
	 */
	public PIP() { }

	public PIP(PIP other) {
		this.startWire = other.startWire;
		this.endWire = other.endWire;
	}

	/**
	 * Constructs a new PIP.
	 *
	 * @param startWire the start wire of this PIP
	 * @param endWire the end wire of this PIP
	 */
	public PIP(Wire startWire, Wire endWire) {
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
	 * Returns the name of the start wire of this PIP.
	 *
	 * @return the name of the start wire of this PIP
	 */
	public String getStartWireName() {
		return startWire.getWireName();
	}

	/**
	 * Sets the start wire of this PIP.
	 * @param wire the enumeration of the start wire of this PIP
	 */
	public void setStartWire(Wire wire) {
		this.startWire = wire;
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
	 * Returns the name of the end wire of this PIP.
	 *
	 * @return the name of the end wire of this PIP
	 */
	public String getEndWireName() {
		return endWire.getWireName();
	}

	/**
	 * Sets the end wire of this PIP.
	 * @param wire the enumeration of the end wire of this PIP
	 */
	public void setEndWire(Wire wire) {
		this.endWire = wire;
	}

	/**
	 * Returns the tile of this PIP.
	 *
	 * @return the tile where this PIP resides
	 */
	public Tile getTile() {
		return startWire.getTile();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getEndWire(), getStartWire());
	}

	@Override
	/**
	 * PIPs are equal if they have the same tile, start wire, and end wire.
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PIP other = (PIP) obj;

		if (!Objects.equals(getTile(), other.getTile()))
			return false;

		return (getEndWire() == other.getEndWire() && getStartWire() == other.getStartWire()) ||
				(getEndWire() == other.getStartWire() && getStartWire() == other.getEndWire());
	}

	@Deprecated
	public int compareTo(PIP pip) throws ClassCastException {
		return ("" + this.getStartWire() + this.getEndWire())
				.compareTo("" + pip.getStartWire() + pip.getEndWire());
	}

	/**
	 * Creates a string representation of this PIP using the WireEnumerator
	 * class.
	 *
	 * @return An XDL-compatible string of this PIP
	 */
	public String toString() {
		return "pip " + getTile().getName() + " " + getStartWireName() +
				" -> " + getEndWireName();
	}
}
