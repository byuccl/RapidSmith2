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

/**
 *  This class represents a pin on a site and provides information necessary
 *  to switch between intersite and intrasite routing networks.  Site pins
 *  are created on demand through different getPin methods in the
 *  Site class.
 *
 *  @see edu.byu.ece.rapidSmith.device.Site
 */
public final class SitePin implements Serializable {
	private static final long serialVersionUID = -7522129253921515308L;
	// The site this pin resides on
	private final Site site;
	// The template that describes this pin
	private final SitePinTemplate template;
	// the tile wire that connects to this pin
	private final int externalWire;

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
	 * Gets the SiteType of the site this pin was created for.
	 * This may be different than the current type of the site as the
	 * site's type may have been updated since this pin was created.
	 * @return the SiteType of the site this pin was created for
	 */
	public SiteType getSiteType() {
		return template.getSiteType();
	}

	/**
	 * Provides access to the internal template backing this object.  This method
	 * is not needed for normal use.
	 * @return the template backing this object
	 */
	public SitePinTemplate getTemplate() {
		return template;
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
		return new SiteWire(getSite(), getSiteType(), template.getInternalWire());
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
		return Objects.equals(this.site, other.site) &&
			Objects.equals(this.template, other.template);
	}

	@Override
	public String toString() {
		return getSite().getName() + "/" + template.getName();
	}
}
