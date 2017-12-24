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
 *  Template that backs BelPin objects.  A template exists for every
 *  (BEL id, pin) pair.
 *
 *  @see edu.byu.ece.rapidSmith.device.BelPin
 */
public final class BelPinTemplate implements Serializable {
	private static final long serialVersionUID = -1112760770804694136L;
	private String name;
	// BEL id for the pins this template backs
	private BelId id;
	private PinDirection direction;
	// Wire the BEL pin connects to
	private int wire;

	public BelPinTemplate(BelId id, String name) {
		this.name = name;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BelId getId() {
		return id;
	}

	public void setId(BelId id) {
		this.id = id;
	}

	public PinDirection getDirection() {
		return direction;
	}

	public void setDirection(PinDirection direction) {
		this.direction = direction;
	}

	public int getWire() {
		return wire;
	}

	public void setWire(int wire) {
		this.wire = wire;
	}

	@Override
	public String toString() {
		return "BelPinTemplate{" + id.toString() + "." + name + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BelPinTemplate that = (BelPinTemplate) o;
		return Objects.equals(name, that.name) &&
				Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode() * 31 + name.hashCode();
	}
}
