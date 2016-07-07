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
	/** The tile where this PIP is located */
	private Tile tile;

	private int startWire;
	private int endWire;

	/**
	 * Constructs an empty PIP.
	 */
	public PIP() {

	}

	public PIP(PIP other) {
		this.tile = other.tile;
		this.startWire = other.startWire;
		this.endWire = other.endWire;
	}

	/**
	 * Constructs a new PIP.
	 *
	 * @param tile the tile of this PIP
	 * @param startWire the start wire of this PIP
	 * @param endWire the end wire of this PIP
	 */
	public PIP(Tile tile, int startWire, int endWire) {
		this.startWire = startWire;
		this.endWire = endWire;
		this.tile = tile;
	}

	/**
	 * Returns the enumeration of the start wire of this PIP.
	 *
	 * @return the start wire enumeration
	 */
	public int getStartWire() {
		return startWire;
	}

	/**
	 * Returns the name of the start wire of this PIP.
	 *
	 * @return the name of the start wire of this PIP
	 */
	public String getStartWireName() {
		return tile.getDevice().getWireEnumerator().getWireName(startWire);
	}

	/**
	 * Sets the start wire of this PIP.
	 * @param wire the enumeration of the start wire of this PIP
	 */
	public void setStartWire(int wire) {
		this.startWire = wire;
	}

	/**
	 * Returns the enumeration of the end wire of this PIP.
	 *
	 * @return the end wire of this PIP
	 */
	public int getEndWire() {
		return endWire;
	}

	/**
	 * Returns the name of the end wire of this PIP.
	 *
	 * @return the name of the end wire of this PIP
	 */
	public String getEndWireName() {
		return tile.getDevice().getWireEnumerator().getWireName(endWire);
	}

	/**
	 * Sets the end wire of this PIP.
	 * @param wire the enumeration of the end wire of this PIP
	 */
	public void setEndWire(int wire) {
		this.endWire = wire;
	}

	/**
	 * Returns the tile of this PIP.
	 *
	 * @return the tile where this PIP resides
	 */
	public Tile getTile() {
		return tile;
	}

	/**
	 * Sets the tile of this PIP.
	 * @param tile the new tile for this PIP
	 */
	public void setTile(Tile tile) {
		this.tile = tile;
	}

	/**
	 * Returns an array of all possible wire connections that
	 * can be made from the start wire of this PIP.  Keep in mind that some
	 * of the wire connections that leave the tile are not PIPs.
	 *
	 * @return An array of all possible end wire connections from this PIP's
	 * start wire
	 */
	public WireConnection[] getAllPossibleEndWires() {
		return getTile().getWireConnections(getStartWire());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getEndWire() + getStartWire(), getTile());
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
		return (this.tile.getName() + this.getStartWire() + this.getEndWire())
				.compareTo(pip.tile.getName() + pip.getStartWire() + pip.getEndWire());
	}

	/**
	 * Creates a string representation of this PIP using the WireEnumerator
	 * class.
	 *
	 * @return An XDL-compatible string of this PIP
	 */
	public String toString() {
		WireEnumerator we = tile.getDevice().getWireEnumerator();
		return "pip " + tile.getName() + " " + we.getWireName(getStartWire()) +
				" -> " + we.getWireName(getEndWire());
	}
}
