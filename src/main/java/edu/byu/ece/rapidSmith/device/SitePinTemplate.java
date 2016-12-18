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
 *
 */
public final class SitePinTemplate implements Serializable {
	private static final long serialVersionUID = 4547857938761572358L;
	private final String name;
	private final SiteType siteType;
	private PinDirection direction;
	private int internalWire;

	public SitePinTemplate(String name, SiteType siteType) {
		this.name = name;
		this.siteType = siteType;
	}

	public String getName() {
		return name;
	}

	public SiteType getSiteType() {
		return siteType;
	}

	public int getInternalWire() {
		return internalWire;
	}

	public void setInternalWire(int internalWire) {
		this.internalWire = internalWire;
	}

	public PinDirection getDirection() {
		return direction;
	}

	public void setDirection(PinDirection direction) {
		this.direction = direction;
	}

	public boolean isInput() {
		return direction == PinDirection.IN || direction == PinDirection.INOUT;
	}

	public boolean isOutput() {
		return direction == PinDirection.OUT || direction == PinDirection.INOUT;
	}

	@Override
	public String toString() {
		return "SitePinTemplate{" +
				"name='" + name + '\'' +
				", siteType=" + siteType +
				'}';
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, siteType, direction, internalWire);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SitePinTemplate other = (SitePinTemplate) obj;
		return Objects.equals(this.name, other.name) &&
				Objects.equals(this.siteType, other.siteType) &&
				Objects.equals(this.direction, other.direction) &&
				Objects.equals(this.internalWire, other.internalWire);
	}
}
