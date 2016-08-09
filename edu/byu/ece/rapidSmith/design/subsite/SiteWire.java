package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class SiteWire implements Wire, Serializable {
	private Site site;
	private int wire;

	public SiteWire(Site site, int wire) {
		this.site = site;
		this.wire = wire;
	}

	@Override
	public Site getSite() {
		return site;
	}

	@Override
	public Tile getTile() {
		return site.getTile();
	}

	@Override
	public int getWireEnum() {
		return wire;
	}

	@Override
	public Collection<Connection> getWireConnections() {
		WireConnection[] wireConnections = site.getWireConnections(wire);
		if (wireConnections == null)
			return Collections.emptyList();

		return Arrays.asList(wireConnections).stream()
				.map(wc -> Connection.getSiteWireConnection(this, wc))
				.collect(Collectors.toList());
	}

	@Override
	public Collection<Connection> getPinConnections() {
		SitePin sitePin = site.getSitePinOfInternalWire(this.wire);
		if (sitePin != null && sitePin.isOutput()) {
			return Collections.singletonList(Connection.getSiteToTileConnection(sitePin));
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public Collection<Connection> getTerminals() {
		BelPin belPin = site.getBelPinOfWire(wire);
		if (belPin != null && belPin.isInput()) {
			return Collections.singletonList(Connection.getTerminalConnection(belPin));
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public Collection<Connection> getReverseWireConnections() {
		WireConnection[] wireConnections = site.getReverseConnections(wire);
		if (wireConnections == null)
			return Collections.emptyList();

		return Arrays.asList(wireConnections).stream()
				.map(wc -> Connection.getSiteWireConnection(this, wc))
				.collect(Collectors.toList());
	}

	@Override
	public Collection<Connection> getReversePinConnections() {
		SitePin sitePin = site.getSitePinOfInternalWire(this.wire);
		if (sitePin != null && sitePin.isInput()) {
			return Collections.singletonList(Connection.getSiteToTileConnection(sitePin)); // TODO reversed Site2TileConn?
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public Collection<Connection> getSources() {
		BelPin belPin = site.getBelPinOfWire(wire);
		if (belPin != null && belPin.isOutput()) {
			return Collections.singletonList(Connection.getTerminalConnection(belPin));
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public Stream<Connection> getAllConnections() {
		Stream<Connection> stream = Stream.concat(
				getWireConnections().stream(), getPinConnections().stream());
		stream = Stream.concat(stream, getTerminals().stream());
		return stream;
	}

	@Override
	public Stream<Connection> getAllReverseConnections() {
		Stream<Connection> stream = Stream.concat(
				getReverseWireConnections().stream(), getReversePinConnections().stream());
		stream = Stream.concat(stream, getSources().stream());
		return stream;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final SiteWire other = (SiteWire) obj;
		return Objects.equals(this.site, other.site)
				&& Objects.equals(this.wire, other.wire);
	}

	@Override
	public int hashCode() {
		return wire * 31 + site.hashCode();
	}

	@Override
	public String toString() {
		return site.getName() + " " + site.getTile().getDevice().getWireEnumerator().getWireName(wire);
	}
}
