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
package edu.byu.ece.rapidSmith.device.creation;

import java.util.HashSet;
import java.util.Set;

/**
 * A helper class to help remove duplicate objects and reduce memory usage and file
 * size of the Device class.
 *
 * @author Chris Lavin
 */
public class TileSources {
	/**
	 * Sources of the tile
	 */
	public final int[] sources;
	private final Set<Integer> set;

	private Integer hash = null;

	public TileSources(int[] sources) {
		if (sources == null)
			sources = new int[0];
		this.sources = sources;

		set = new HashSet<>();
		for (int src : sources)
			set.add(src);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if (hash == null) {
			hash = set.hashCode();
		}
		return hash;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;

		TileSources other = (TileSources) obj;
		return set.equals(other.set);
	}
}
