/*
 * Copyright (c) 2010-2011 Brigham Young University
 *
 * This file is part of the BYU RapidSmith Tools.
 *
 * BYU RapidSmith Tools is free software: you may redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2 of
 * the License, or (at your option) any later version.
 *
 * BYU RapidSmith Tools is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * A copy of the GNU General Public License is included with the BYU
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 *
 */

package edu.byu.ece.rapidSmith.device;

import java.io.Serializable;
import java.util.Objects;

/**
 *  Identifier for a unique BEL name (SLICEL/AFF, SLICEM/F7MUX, etc) in the device.
 *
 *  The identifier consists of a site type and the name of the BEL within the
 *  site.  BelId objects are immutable.
 */
public final class BelId implements Serializable {
	private static final long serialVersionUID = -4845391283243751324L;
	private final SiteType siteType;
	private final String name;

	/**
	 * Constructs a new BelId object.
	 *
	 * @param siteType the site type of the BEL id
	 * @param name the name of the BEL within the site
	 */
	public BelId(SiteType siteType, String name) {
		this.siteType = siteType;
		this.name = name;
	}

	/**
	 * Returns the site type portion of this id.
	 *
	 * @return the site type
	 */
	public SiteType getSiteType() {
		return siteType;
	}

	/**
	 * Returns the name portion of this id.
	 *
	 * @return the BEL name
	 */
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return siteType.hashCode() * 31 + name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final BelId other = (BelId) obj;
		return Objects.equals(this.siteType, other.siteType) && Objects.equals(this.name, other.name);
	}

	@Override
	public String toString() {
		return siteType + "/" + name ;
	}
}
