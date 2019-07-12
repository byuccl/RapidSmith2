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

import static edu.byu.ece.rapidSmith.util.Exceptions.DesignAssemblyException;

/**
 *
 */
public abstract class Connection implements Serializable {
	private static final long serialVersionUID = 3236104431137672033L;

	public final static class TileWireConnection extends Connection {
		private static final long serialVersionUID = 7549238102833227662L;
		private final TileWire sourceWire;
		private final WireConnection wc;

		public TileWireConnection(TileWire sourceWire, WireConnection wc) {
			this.sourceWire = sourceWire;
			this.wc = wc;
		}

		@Override
		public TileWire getSourceWire() {
			return sourceWire;
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
			return device.isRouteThrough(sourceWire.getWireEnum(), wc.getWire());
		}

		@Override
		public boolean isDirectConnection() {
			return !isPip() && !isRouteThrough();
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

		@Override
		public String toString() {
			return sourceWire + " -> " + getSinkWire();
		}
	}

	public final static class ReverseTileWireConnection extends Connection {
		private static final long serialVersionUID = -3585646572632532927L;
		private final TileWire sourceWire;
		private final WireConnection wc;

		public ReverseTileWireConnection(TileWire sourceWire, WireConnection wc) {
			this.sourceWire = sourceWire;
			this.wc = wc;
		}

		@Override
		public TileWire getSourceWire() {
			return sourceWire;
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
		public boolean isDirectConnection() {
			return !isPip() && !isRouteThrough();
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

		@Override
		public String toString() {
			return sourceWire + " <- " + getSinkWire();
		}
	}

	public final static class SiteWireConnection extends Connection {
		private static final long serialVersionUID = -6889841775729826036L;
		private final SiteWire sourceWire;
		private final WireConnection wc;

		public SiteWireConnection(SiteWire sourceWire, WireConnection wc) {
			this.sourceWire = sourceWire;
			this.wc = wc;
		}

		@Override
		public SiteWire getSourceWire() {
			return sourceWire;
		}

		@Override
		public SiteWire getSinkWire() {
			Site site = sourceWire.getSite();
			SiteType siteType = sourceWire.getSiteType();
			return new SiteWire(site, siteType, wc.getWire());
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
			SiteType siteType = sourceWire.getSiteType();
			int sourceEnum = sourceWire.getWireEnum();
			return sourceWire.getSite().isRoutethrough(siteType, sourceEnum, wc.getWire());
		}

		@Override
		public boolean isDirectConnection() {
			return !isPip() && !isRouteThrough();
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

		@Override
		public String toString() {
			return sourceWire + " -> " + getSinkWire();
		}
	}

	public final static class ReverseSiteWireConnection extends Connection {
		private static final long serialVersionUID = -6889841775729826036L;
		private final SiteWire sourceWire;
		private final WireConnection wc;

		public ReverseSiteWireConnection(SiteWire sourceWire, WireConnection wc) {
			this.sourceWire = sourceWire;
			this.wc = wc;
		}

		@Override
		public SiteWire getSourceWire() {
			return sourceWire;
		}

		@Override
		public SiteWire getSinkWire() {
			Site site = sourceWire.getSite();
			SiteType siteType = sourceWire.getSiteType();
			return new SiteWire(site, siteType, wc.getWire());
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
			SiteType siteType = sourceWire.getSiteType();
			int sourceEnum = sourceWire.getWireEnum();
			return sourceWire.getSite().isRoutethrough(siteType, wc.getWire(), sourceEnum);
		}

		@Override
		public boolean isDirectConnection() {
			return !isPip() && !isRouteThrough();
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

		@Override
		public String toString() {
			return sourceWire + " <- " + getSinkWire();
		}
	}

	@Deprecated
	public final static class TileToSiteConnection extends Connection {
		private static final long serialVersionUID = -5352375975919207638L;
		private final SitePin pin;

		public TileToSiteConnection(SitePin pin) {
			this.pin = pin;
		}

		@Override
		public Wire getSourceWire() {
			return pin.getExternalWire();
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
		public boolean isDirectConnection() {
			return true;
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

	@Deprecated
	public final static class SiteToTileConnection extends Connection {
		private static final long serialVersionUID = 6090945282983450142L;
		private final SitePin pin;

		public SiteToTileConnection(SitePin pin) {
			this.pin = pin;
		}

		@Override
		public Wire getSourceWire() {
			return pin.getInternalWire();
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
		public boolean isDirectConnection() {
			return true;
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

	@Deprecated
	public final static class Terminal extends Connection {
		private static final long serialVersionUID = 5789458862592782873L;
		private final BelPin belPin;

		public Terminal(BelPin belPin) {
			this.belPin = belPin;
		}

		@Override
		public Wire getSourceWire() {
			return belPin.getWire();
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
		public boolean isDirectConnection() {
			return true;
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

	public abstract Wire getSourceWire();

	public abstract Wire getSinkWire();

	@Deprecated
	public abstract boolean isWireConnection();

	public abstract boolean isPip();

	public abstract boolean isRouteThrough();

	public abstract boolean isDirectConnection();

	@Deprecated
	public abstract boolean isPinConnection();

	@Deprecated
	public abstract SitePin getSitePin();

	public abstract PIP getPip();

	@Deprecated
	public abstract boolean isTerminal();

	@Deprecated
	public abstract BelPin getBelPin();
}
