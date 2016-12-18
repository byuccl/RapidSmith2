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

/**
 * Binary operations for a LutEquation.
 */
public enum OpType {
	AND('*'), OR('+'), XOR('@');

	/** The XDL equation symbol for the operation. */
	public final char xdlSymbol;

	OpType(char xdlSymbol) {
		this.xdlSymbol = xdlSymbol;
	}

	/**
	 * @param symbol the XDL symbol
	 * @return the operation correlating to the symbol {@code symbol} in an XDL equation
	 */
	public static OpType fromXdlSymbol(char symbol) {
		switch (symbol) {
			case '*': return AND;
			case '+': return OR;
			case '@': return XOR;
			default: throw new IllegalArgumentException("unsupported symbol");
		}
	}

	public String toString() {
		switch (this) {
			case AND: return "*";
			case OR: return "+";
			case XOR: return "@";
		}
		throw new AssertionError("Unknown operation");
	}
}
