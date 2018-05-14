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

package edu.byu.ece.rapidSmith.design.subsite;

/**
 * Each cell pin in Vivado has an associated type. {@code CellPinType)
 * represents the possible cell pin types available in Vivado. This is useful, for example,
 * to determine if a pin is a clock pin. When changing versions of Vivado, 
 * run the command {@code report_property -class pin} in an open Vivado TCL
 * command prompt, and look at the IS_* properties. NOTE: not all of these properties 
 * will correspond to a pin type.  
 * 
 * @author Thomas Townsend
 *
 */
public enum CellPinType {
	CLEAR,
	CLOCK,
	ENABLE,
	PRESET,
	RESET,
	REUSED,
	SET,
	SETRESET,
	WRITE_ENABLE,
	DATA, 
	PSEUDO,
	MACRO,
	PARTITION,
	PARTITION_CLK
}
