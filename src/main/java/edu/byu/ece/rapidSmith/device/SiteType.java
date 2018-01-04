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
public final class SiteType implements Comparable<SiteType>, Serializable {
	private static final long serialVersionUID = -2823098655445630138L;
	private static int nextOrdinal = 0;
	private static final Map<FamilyType, Map<String, SiteType>> types = new HashMap<>();
	private final FamilyType family;
	private final String name;
	private final int ordinal;

	private SiteType(FamilyType family, String name, int ordinal) {
		this.family = family;
		this.name = name;
		this.ordinal = ordinal;
	}

	public String name() {
		return name;
	}

	public FamilyType family() {
		return family;
	}

	public int ordinal() {
		return ordinal;
	}

	@Override
	public int compareTo(SiteType other) {
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
	public static SiteType valueOf(FamilyType family, String name) {
		Objects.requireNonNull(family);
		Objects.requireNonNull(name);

		name = name.toUpperCase();
		synchronized (types) {
			Map<String, SiteType> familyTypes = types.computeIfAbsent(family, k -> new HashMap<>());
			return familyTypes.computeIfAbsent(name, k -> new SiteType(family, k, nextOrdinal++));
		}
	}
	
	public static Map<String, SiteType> getSiteTypes(FamilyType family) {
		return types.get(family);
	}

	private static class SiteTypeReplace implements Serializable {
		private static final long serialVersionUID = 4803134026521902169L;
		private FamilyType familyType;
		private String name;

		@SuppressWarnings("unused")
		SiteTypeReplace() { }

		private SiteTypeReplace(SiteType type) {
			this.familyType = type.family;
			this.name = type.name;
		}

		@SuppressWarnings("unused")
		private SiteType readResolve() {
			return SiteType.valueOf(familyType, name);
		}
	}

	@SuppressWarnings("unused")
	private SiteTypeReplace writeReplace() {
		return new SiteTypeReplace(this);
	}
}

