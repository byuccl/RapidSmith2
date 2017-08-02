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
 * The names of the architecture families.  FamilyType objects are created upon request
 * and cached through the value of method.
 * @author Chris Lavin
 */
public final class FamilyType implements Comparable<FamilyType>, Serializable {
	private static final long serialVersionUID = 2547068989112328515L;
	private static int nextOrdinal = 0;
	private static final Map<String, FamilyType> types = new HashMap<>();
	private final String name;
	private final int ordinal;

	private FamilyType(String name, int ordinal) {
		this.name = name;
		this.ordinal = ordinal;
	}

	public String name() {
		return name;
	}

	public int ordinal() {
		return ordinal;
	}

	@Override
	public int compareTo(FamilyType other) {
		Objects.requireNonNull(other);
		return name.compareTo(other.name);
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Returns the constant of this type with the specified name. The string must match
	 * exactly an identifier used to declare an enum constant in this type. (Extraneous
	 * whitespace characters are not permitted.)
	 * @return the constant with the specified name
	 */
	public static FamilyType valueOf(String name) {
		Objects.requireNonNull(name);

		name = name.toUpperCase();
		synchronized (types) {
			return types.computeIfAbsent(name, k -> new FamilyType(k, nextOrdinal++));
		}
	}

	private static class FamilyTypeReplace implements Serializable {
		private static final long serialVersionUID = -9010197157638586336L;
		private String name;

		@SuppressWarnings("unused")
		FamilyTypeReplace() {}

		FamilyTypeReplace(FamilyType type) {
			this.name = type.name;
		}

		@SuppressWarnings("unused")
		private FamilyType readResolve() {
			return FamilyType.valueOf(name);
		}
	}

	@SuppressWarnings("unused")
	private FamilyTypeReplace writeReplace() {
		return new FamilyTypeReplace(this);
	}

}
