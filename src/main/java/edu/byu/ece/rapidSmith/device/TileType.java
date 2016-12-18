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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class represents the types of tiles in a device (eg. CLBLM, RAMB36E1).  Values
 * are obtained through calls to valueOf.
 */
public final class TileType implements Comparable<TileType>, Serializable {
	private static final long serialVersionUID = -4299672560706873783L;
	private static int nextOrdinal = 0;
	private static final Map<FamilyType, Map<String, TileType>> types = new HashMap<>();
	private final FamilyType family;
	private final String name;
	private final int ordinal;

	private TileType(FamilyType family, String name, int ordinal) {
		assert family != null;
		assert name != null;

		this.family = family;
		this.name = name;
		this.ordinal = ordinal;
	}

	/**
	 * @return the name of this TileType.
	 */
	public String name() {
		return name;
	}

	/**
	 * @return the family of this TileType
	 */
	public FamilyType family() {
		return family;
	}

	/**
	 * Unique integer value for this type.  This value is not consistent between executions.
	 * @return a unique integer value for the type
	 */
	public int ordinal() {
		return ordinal;
	}

	@Override
	public int compareTo(TileType other) {
		Objects.requireNonNull(other);
		int nameCompare = name.compareTo(other.name);
		if (nameCompare != 0)
			return nameCompare;
		return family.compareTo(other.family);
	}

	@Override
	public String toString() {
		return family.name() + "." + name;
	}

	/**
	 * Returns the constant of this type with the specified name. The string must match
	 * exactly an identifier used to declare an enum constant in this type. (Extraneous
	 * whitespace characters are not permitted.)
	 * @return the constant with the specified name
	 */
	public static TileType valueOf(FamilyType family, String name) {
		Objects.requireNonNull(family);
		Objects.requireNonNull(name);

		name = name.toUpperCase();
		synchronized (types) {
			Map<String, TileType> familyTypes = types.computeIfAbsent(family, k -> new HashMap<>());
			return familyTypes.computeIfAbsent(name, k -> new TileType(family, k, nextOrdinal++));
		}
	}

	private static class TileTypeReplace implements Serializable {
		private static final long serialVersionUID = 8701190440202786455L;
		private FamilyType familyType;
		private String name;

		TileTypeReplace() { }

		TileTypeReplace(TileType type) {
			this.familyType = type.family;
			this.name = type.name;
		}

		@SuppressWarnings("unused")
		private TileType readResolve() {
			return TileType.valueOf(familyType, name);
		}
	}

	@SuppressWarnings("unused")
	private TileTypeReplace writeReplace() {
		return new TileTypeReplace(this);
	}
}

