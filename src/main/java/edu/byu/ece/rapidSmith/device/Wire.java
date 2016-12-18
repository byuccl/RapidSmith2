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
import java.util.Collection;
import java.util.stream.Stream;

/**
 * Wires represent a piece of metal on a device.  Wires are composed of two
 * components, a location such as a tile or primitive site where the tile resides
 * and an enumeration identifying the unique wire inside the resource.  Wire
 * object are immutable.
 */
public interface Wire extends Serializable {
	int getWireEnum();
	String getWireName();
	String getFullWireName();
	Tile getTile();
	Site getSite();

	/**
	 * Returns a stream comprised of wire, pin and terminal connections.
	 */
	Stream<Connection> getAllConnections();

	/**
	 * Return connection linking this wire to other wires in the same hierarchy.
	 */
	Collection<Connection> getWireConnections();

	/**
	 * Returns connection linking this wire to another wire in a different
	 * hierarchical level through a pin.
	 */
	Collection<Connection> getPinConnections();

	/**
	 * Returns the terminals (BelPins) this wire drives.
	 */
	Collection<Connection> getTerminals();

	/**
	 * Returns a stream comprised of wire, pin and terminal connection in the
	 * reverse direction, ie sink to source.
	 * */
	Stream<Connection> getAllReverseConnections();

	/**
	 * Returns connection linking this wire to its drivers in the same hierarchy.
	 */
	Collection<Connection> getReverseWireConnections();

	/**
	 * Return connection linking this wire to its drivers in the different
	 * levels of hierarchy.
	 */
	Collection<Connection> getReversePinConnections();

	/**
	 * Returns the sources (BelPins) which drive this wire.
	 */
	Collection<Connection> getSources();
}
