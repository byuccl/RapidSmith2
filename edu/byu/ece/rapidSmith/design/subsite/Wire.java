package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.PrimitiveSite;
import edu.byu.ece.rapidSmith.device.Tile;

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
	default String getWireName() {
		return getTile().getDevice().getWireEnumerator().getWireName(getWireEnum());
	}
	Tile getTile();
	PrimitiveSite getSite();

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
