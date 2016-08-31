/*
 * Copyright (c) 2010 Brigham Young University
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

import java.util.ArrayList;
import java.util.HashMap;

public final class TileType {
	private final String name;
	private final int ordinal;

	private TileType(String name, int ordinal) {
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
	public String toString() {
		return "TileType." + name;
	}

	private static final HashMap<String, TileType> types = new HashMap<>();
	private static int count = 0;

	public static synchronized TileType get(String name) {
		// synchronized to allow parallel accesses.
		// TileTypes should be case independent
		String upper = name.toUpperCase();
		TileType type = types.get(upper);

		// if it doesn't already exist, create it and store it
		if (type == null) {
			type = new TileType(upper, count++);
			types.put(name, type);
		}

		return type;
	}

	public static synchronized TileType valueOf(String name) {
		return types.get(name.toUpperCase());
	}

	public static synchronized ArrayList<TileType> values() {
		return new ArrayList<>(types.values());
	}
}


