package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.device.PrimitiveSite;
import edu.byu.ece.rapidSmith.device.WireEnumerator;

import java.util.Objects;

/**
 * A PIP in a site.
 */
public final class SitePip extends PIP {
	private PrimitiveSite site;

	public SitePip(PrimitiveSite site, int startWire, int endWire) {
		super(site.getTile(), startWire, endWire);
		this.site = site;
	}

	public PrimitiveSite getSite() {
		return site;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getEndWire(), getStartWire(), getSite());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SitePip other = (SitePip) obj;

		if (!Objects.equals(getSite(), other.getSite()))
			return false;

		return (getEndWire() == other.getEndWire() && getStartWire() == other.getStartWire());
	}

	@Override
	public String toString() {
		WireEnumerator we = site.getTile().getDevice().getWireEnumerator();
		return "pip " + site.getName() + " " + we.getWireName(getStartWire()) +
				" -> " + we.getWireName(getEndWire());
	}
}
