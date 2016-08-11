package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.WireEnumerator;

import java.util.Objects;

/**
 * A PIP in a site.
 */
public final class SitePip extends PIP {
	public SitePip(Wire startWire, Wire endWire) {
		super(startWire, endWire);
	}

	public Site getSite() {
		return getStartWire().getSite();
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
		return "pip " + getSite().getName() + " " + getStartWireName() +
				" -> " + getEndWireName();
	}
}
