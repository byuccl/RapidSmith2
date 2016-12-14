package edu.byu.ece.rapidSmith.device;

import edu.byu.ece.rapidSmith.design.PIP;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import static edu.byu.ece.rapidSmith.util.Exceptions.*;

/**
 *
 */
public abstract class Connection implements Serializable {
	public static Connection getTileWireConnection(
			TileWire sourceWire, WireConnection wc
	) {
		return new TileWireConnection(sourceWire, wc);
	}

	public static Connection getReveserTileWireConnection(
			TileWire sourceWire, WireConnection wc
	) {
		return new ReverseTileWireConnection(sourceWire, wc);
	}

	public static Connection getSiteWireConnection(SiteWire sourceWire, WireConnection wc) {
		return new SiteWireConnection(sourceWire, wc);
	}

	public static Connection getTileToSiteConnection(SitePin pin) {
		return new TileToSiteConnection(pin);
	}

	public static Connection getSiteToTileConnection(SitePin pin) {
		return new SiteToTileConnection(pin);
	}

	public static Connection getTerminalConnection(BelPin belPin) {
		return new Terminal(belPin);
	}

	public abstract boolean isWireConnection();

	// TODO: Update this to have a cache of created wires?
	private final static class TileWireConnection extends Connection {
		private TileWire sourceWire;
		private WireConnection wc;
		private TileWire sinkWire;
		
		public TileWireConnection(TileWire sourceWire, WireConnection wc) {
			this.sourceWire = sourceWire;
			this.wc = wc;
			this.sinkWire = null;
		}

		@Override
		public TileWire getSinkWire() {
		
			if (sinkWire == null) {				
				sinkWire = new TileWire(wc.getTile(sourceWire.getTile()), wc.getWire());
			}
			
			return sinkWire;
		}

		@Override
		public boolean isWireConnection() {
			return true;
		}

		@Override
		public boolean isPip() {
			return wc.isPIP();
		}

		@Override
		public boolean isRouteThrough() {
			Device device = sourceWire.getTile().getDevice();
			return device.isRouteThrough(sourceWire.getWireEnum(), wc.getWire());
		}

		@Override
		public boolean isPinConnection() {
			return false;
		}

		@Override
		public SitePin getSitePin() {
			return null;
		}

		@Override
		public PIP getPip() {
			return new PIP(sourceWire, getSinkWire());
		}

		@Override
		public boolean isTerminal() {
			return false;
		}

		@Override
		public BelPin getBelPin() {
			return null;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TileWireConnection that = (TileWireConnection) o;
			return Objects.equals(sourceWire, that.sourceWire) &&
					Objects.equals(wc, that.wc);
		}

		@Override
		public int hashCode() {
			return Objects.hash(sourceWire, wc);
		}
	}

	private final static class ReverseTileWireConnection extends Connection {
		private TileWire sourceWire;
		private WireConnection wc;

		public ReverseTileWireConnection(TileWire sourceWire, WireConnection wc) {
			this.sourceWire = sourceWire;
			this.wc = wc;
		}

		@Override
		public TileWire getSinkWire() {
			return new TileWire(wc.getTile(sourceWire.getTile()), wc.getWire());
		}

		@Override
		public boolean isWireConnection() {
			return true;
		}

		@Override
		public boolean isPip() {
			return wc.isPIP();
		}

		@Override
		public boolean isRouteThrough() {
			Device device = sourceWire.getTile().getDevice();
			return device.isRouteThrough(wc.getWire(), sourceWire.getWireEnum());
		}

		@Override
		public boolean isPinConnection() {
			return false;
		}

		@Override
		public SitePin getSitePin() {
			return null;
		}

		@Override
		public PIP getPip() {
			return new PIP(sourceWire, getSinkWire());
		}

		@Override
		public boolean isTerminal() {
			return false;
		}

		@Override
		public BelPin getBelPin() {
			return null;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TileWireConnection that = (TileWireConnection) o;
			return Objects.equals(sourceWire, that.sourceWire) &&
					Objects.equals(wc, that.wc);
		}

		@Override
		public int hashCode() {
			return Objects.hash(sourceWire, wc);
		}
	}

	private final static class SiteWireConnection extends Connection {
		private SiteWire sourceWire;
		private WireConnection wc;

		public SiteWireConnection(SiteWire sourceWire, WireConnection wc) {
			this.sourceWire = sourceWire;
			this.wc = wc;
		}

		@Override
		public Wire getSinkWire() {
			return new SiteWire(sourceWire.getSite(), wc.getWire());
		}

		@Override
		public boolean isWireConnection() {
			return true;
		}

		@Override
		public boolean isPip() {
			return wc.isPIP();
		}

		@Override
		public boolean isRouteThrough() {
			// bel routethrough
			return sourceWire.getSite().isRoutethrough(sourceWire.getWireEnum(), wc.getWire());
		}

		@Override
		public boolean isPinConnection() {
			return false;
		}

		@Override
		public SitePin getSitePin() {
			return null;
		}

		@Override
		public PIP getPip() {
			if (!wc.isPIP())
				throw new DesignAssemblyException("Attempting to create PIP " +
						"of non-PIP connection");
			return new PIP(sourceWire, getSinkWire());
		}

		@Override
		public boolean isTerminal() {
			return false;
		}

		@Override
		public BelPin getBelPin() {
			return null;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SiteWireConnection that = (SiteWireConnection) o;
			return Objects.equals(sourceWire, that.sourceWire) &&
					Objects.equals(wc, that.wc);
		}

		@Override
		public int hashCode() {
			return Objects.hash(sourceWire, wc);
		}
	}

	private final static class TileToSiteConnection extends Connection {
		private SitePin pin;

		public TileToSiteConnection(SitePin pin) {
			this.pin = pin;
		}

		@Override
		public Wire getSinkWire() {
			return pin.getInternalWire();
		}

		@Override
		public boolean isWireConnection() {
			return false;
		}

		@Override
		public boolean isPip() {
			return false;
		}

		@Override
		public boolean isRouteThrough() {
			return false;
		}

		@Override
		public boolean isPinConnection() {
			return true;
		}

		@Override
		public SitePin getSitePin() {
			return pin;
		}

		@Override
		public PIP getPip() {
			return null;
		}

		@Override
		public boolean isTerminal() {
			return false;
		}

		@Override
		public BelPin getBelPin() {
			return null;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TileToSiteConnection that = (TileToSiteConnection) o;
			return Objects.equals(pin, that.pin);
		}

		@Override
		public int hashCode() {
			return Objects.hash(pin);
		}
	}

	private final static class SiteToTileConnection extends Connection {
		private SitePin pin;

		public SiteToTileConnection(SitePin pin) {
			this.pin = pin;
		}

		@Override
		public Wire getSinkWire() {
			return pin.getExternalWire();
		}

		@Override
		public boolean isWireConnection() {
			return false;
		}

		@Override
		public boolean isPip() {
			return false;
		}

		@Override
		public boolean isRouteThrough() {
			return false;
		}

		@Override
		public boolean isPinConnection() {
			return true;
		}

		@Override
		public SitePin getSitePin() {
			return pin;
		}

		@Override
		public PIP getPip() {
			return null;
		}

		@Override
		public boolean isTerminal() {
			return false;
		}

		@Override
		public BelPin getBelPin() {
			return null;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SiteToTileConnection that = (SiteToTileConnection) o;
			return Objects.equals(pin, that.pin);
		}

		@Override
		public int hashCode() {
			return Objects.hash(pin);
		}
	}

	private final static class Terminal extends Connection {
		private BelPin belPin;

		public Terminal(BelPin belPin) {
			this.belPin = belPin;
		}

		@Override
		public Wire getSinkWire() {
			return null;
		}

		@Override
		public boolean isWireConnection() {
			return false;
		}

		@Override
		public boolean isPip() {
			return false;
		}

		@Override
		public boolean isRouteThrough() {
			return false;
		}

		@Override
		public boolean isPinConnection() {
			return false;
		}

		@Override
		public SitePin getSitePin() {
			return null;
		}

		@Override
		public PIP getPip() {
			return null;
		}

		@Override
		public boolean isTerminal() {
			return true;
		}

		@Override
		public BelPin getBelPin() {
			return belPin;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Terminal terminal = (Terminal) o;
			return Objects.equals(belPin, terminal.belPin);
		}

		@Override
		public int hashCode() {
			return Objects.hash(belPin);
		}
	}

	public abstract Wire getSinkWire();

	public abstract boolean isPip();

	public abstract boolean isRouteThrough();

	public abstract boolean isPinConnection();

	public abstract SitePin getSitePin();

	public abstract PIP getPip();

	public abstract boolean isTerminal();

	public abstract BelPin getBelPin();

	public Collection<Connection> getWireConnections() {
		return getSinkWire().getWireConnections();
	}

	public Collection<Connection> getPinConnections() {
		return getSinkWire().getPinConnections();
	}

	public Collection<Connection> getTerminals() {
		return getSinkWire().getTerminals();
	}

	public Stream<Connection> getAllConnections() {
		return getSinkWire().getAllConnections();
	}

}
