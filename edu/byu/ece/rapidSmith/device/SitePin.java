package edu.byu.ece.rapidSmith.device;

import edu.byu.ece.rapidSmith.design.subsite.SiteWire;
import edu.byu.ece.rapidSmith.design.subsite.TileWire;

import java.io.Serializable;
import java.util.Objects;

/**
 *  This class represents a pin on a site and provides information necessary
 *  to switch between intersite and intrasite routing networks.  Site pins
 *  are created on demand through different getPin methods in the
 *  PrimitiveSite class.
 *
 *  @see edu.byu.ece.rapidSmith.device.Site
 */
public final class SitePin implements Serializable {
	// The site this pin resides on
	private Site site;
	// The template that describes this pin
	private SitePinTemplate template;
	// the tile wire that connects to this pin
	private int externalWire;

	SitePin(Site site, SitePinTemplate template, int externalWire) {
		this.site = site;
		this.template = template;
		this.externalWire = externalWire;
	}

	/**
	 * Returns the name of this pin.
	 * @return the name of this pin
	 */
	public String getName() {
		return template.getName();
	}

	/**
	 * Gets the PrimitiveType of the site this pin was created for.
	 * This may be different than the current type of the primitive site as the
	 * site's type may have been updated since this pin was created.
	 * @return the PrimitiveType of the site this pin was created for
	 */
	public SiteType getPrimitiveType() {
		return template.getPrimitiveType();
	}

	/**
	 * Returns the site this pin exists on.
	 * @return the site this pin exists on
	 */
	public Site getSite() {
		return site;
	}

	/**
	 * Returns the tile wire that connects to this pin
	 * @return the tile wire that connects to this pin
	 */
	public TileWire getExternalWire() {
		return new TileWire(getSite().getTile(), externalWire);
	}

	/**
	 * Returns the site wire that connects to this pin.
	 * @return the site wire that connects to this pin
	 */
	public SiteWire getInternalWire() {
		return new SiteWire(getSite(), template.getInternalWire());
	}

	/**
	 * Returns the direction of this pin from its site's perspective.
	 * @return the direction of this pin from its site's perspective
	 */
	public PinDirection getDirection() {
		return template.getDirection();
	}

	/**
	 * Tests if this pin is an input from the site's perspective..
	 * @return true if this pin is an input of its site
	 */
	public boolean isInput() {
		return template.isInput();
	}

	/**
	 * Tests if this pin is an output from the site's perspective.
	 * @return true if this pin is an output of its site
	 */
	public boolean isOutput() {
		return template.isOutput();
	}

	@Override
	public int hashCode() {
		return Objects.hash(site, template);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final SitePin other = (SitePin) obj;
		return Objects.equals(this.site, other.site) && Objects.equals(this.template, other.template);
	}

	@Override
	public String toString() {
		return getSite().getName() + "/" + template.getName();
	}

	public SitePinTemplate getTemplate() {
		return template;
	}

	public boolean drivesGeneralFabric() {
		return getTemplate().drivesGeneralFabric();
	}

	public boolean drivenByGeneralFabric() {
		return getTemplate().isDrivenByGeneralFabric();
	}
}
