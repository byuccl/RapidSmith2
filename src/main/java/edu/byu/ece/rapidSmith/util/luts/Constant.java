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

package edu.byu.ece.rapidSmith.util.luts;

import java.util.Map;
import java.util.Set;

/**
 * A LutEquation constant value.  Only options are ZERO and ONE.
 */
public final class Constant extends LutEquation {
    public static final Constant ONE = new Constant("1");
	public static final Constant ZERO = new Constant("0");
	private final String strValue;


	private Constant(String strValue) {
		this.strValue = strValue;
	}

	@Override
	public String toString() {
		return strValue;
	}

	@Override
	public LutEquation deepCopy() {
		return this;
	}

	@Override
	protected void getUsedInputs(Set<Integer> usedInputs) {
		// nothing to add
	}

	@Override
	public void remapPins(Map<Integer, Integer> mapping) {
		// nothing to do here
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		return this == ONE ? 1 : 0;
	}
}
