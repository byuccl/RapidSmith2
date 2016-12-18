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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A wire inside a tile but outside a site.  This is part of the general
 * routing circuitry.  TileWires are composed of the tile the wire exists in
 * and the enumeration identifying the individual wire.
 */
public class TileWire implements Wire, Serializable {

	private static final long serialVersionUID = 5844788118958981887L;
	private Tile tile;
	private int wire;
	
	public TileWire(Tile tile, int wire) {
		assert tile != null;

		this.tile = tile;
		this.wire = wire;
	}

	@Override
	public Tile getTile() {
		return tile;
	}

	@Override
	public String getWireName() {
		return tile.getDevice().getWireEnumerator().getWireName(wire);
	}

	@Override
	public String getFullWireName() {
		return getTile().getName() + "/" + getWireName();
	}
	
	/**
	 * Always returns null.
	 */
	@Override
	public Site getSite() {
		return null;
	}

	@Override
	public int getWireEnum() {
		return wire;
	}

	/**
	 * Returns all sink connections within and between tiles.
	 */
	@Override
	public Collection<Connection> getWireConnections() {	
		WireConnection[] wireConnections = tile.getWireConnections(wire);
		if (wireConnections == null)
			return Collections.emptyList();
		
		return Arrays.stream(wireConnections)
				.map(wc -> Connection.getTileWireConnection(this, wc))
				.collect(Collectors.toList());
	}

	/**
	 * Returns all connections into primitive sites this wire drives.
	 */
	@Override
	public Collection<Connection> getPinConnections() {
		SitePin sitePin = tile.getSitePinOfWire(this.wire);
		if (sitePin != null && sitePin.isInput()) {
			return Collections.singletonList(Connection.getTileToSiteConnection(sitePin));
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Always return an empty list.
	 */
	@Override
	public Collection<Connection> getTerminals() {
		return Collections.emptyList();
	}

	/**
	 * Returns a stream containing wire and pin connections.
	 */
	@Override
	public Stream<Connection> getAllConnections() {
		return Stream.concat(getWireConnections().stream(), getPinConnections().stream());
	}

	/**
	 * Returns connection to all connections within or between tiles which drive
	 * this wire.
	 */
	@Override
	public Collection<Connection> getReverseWireConnections() {
		WireConnection[] wireConnections = tile.getReverseConnections(wire);
		if (wireConnections == null)
			return Collections.emptyList();

		return Arrays.stream(wireConnections)
				.map(wc -> Connection.getReveserTileWireConnection(this, wc))
				.collect(Collectors.toList());
	}

	/**
	 * Returns all site pin connections driving this wire.
	 */
	@Override
	public Collection<Connection> getReversePinConnections() {
		SitePin sitePin = tile.getSitePinOfWire(this.wire);
		if (sitePin != null && sitePin.isOutput()) {
			return Collections.singletonList(Connection.getTileToSiteConnection(sitePin));
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Always returns an empty list.
	 */
	@Override
	public Collection<Connection> getSources() {
		return Collections.emptyList();
	}

	/**
	 * Returns a stream of reverse wire and pin connections.
	 */
	@Override
	public Stream<Connection> getAllReverseConnections() {
		return Stream.concat(getReverseWireConnections().stream(),
				getReversePinConnections().stream());
	}

	/**
	 * Tests if the object is equal to this wire.  Wires are equal if they share
	 * the same tile and wire enumeration.
	 * @return true if <i>obj</i> is the same wire as this wire
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		TileWire other = (TileWire) obj;
		return Objects.equals(this.tile, other.tile)
				&& this.wire == other.wire;
	}

	@Override
	public int hashCode() {
		return wire * 31 + tile.hashCode();
	}

	@Override
	public String toString() {
		return tile.getName() + " " + tile.getDevice().getWireEnumerator().getWireName(wire);
	}
}
