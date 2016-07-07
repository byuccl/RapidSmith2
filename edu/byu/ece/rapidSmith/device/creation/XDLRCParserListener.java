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

package edu.byu.ece.rapidSmith.device.creation;

import java.util.List;

/**
 *  Base class for XDLRC Parser listeners.  When the parser enters or exists an
 *  XDLRC parse element, the parser will call the associated listener method of
 *  each registered listener.
 */
@SuppressWarnings("UnusedParameters")
public abstract class XDLRCParserListener {
	protected void enterXdlResourceReport(List<String> tokens) { }
	protected void exitXdlResourceReport(List<String> tokens) { }
	protected void enterTiles(List<String> tokens) { }
	protected void exitTiles(List<String> tokens) { }
	protected void enterTile(List<String> tokens) { }
	protected void exitTile(List<String> tokens) { }
	protected void enterPrimitiveSite(List<String> tokens) { }
	protected void exitPrimitiveSite(List<String> tokens) { }
	protected void enterPinWire(List<String> tokens) { }
	protected void exitPinWire(List<String> tokens) { }
	protected void enterWire(List<String> tokens) { }
	protected void exitWire(List<String> tokens) { }
	protected void enterConn(List<String> tokens) { }
	protected void exitConn(List<String> tokens) { }
	protected void enterTileSummary(List<String> tokens) { }
	protected void exitTileSummary(List<String> tokens) { }
	protected void enterPip(List<String> tokens) { }
	protected void exitPip(List<String> tokens) { }
	protected void enterPrimitiveDefs(List<String> tokens) { }
	protected void exitPrimitiveDefs(List<String> tokens) { }
	protected void enterPrimitiveDef(List<String> tokens) { }
	protected void exitPrimitiveDef(List<String> tokens) { }
	protected void enterPin(List<String> tokens) { }
	protected void exitPin(List<String> tokens) { }
	protected void enterElement(List<String> tokens) { }
	protected void exitElement(List<String> tokens) { }
	protected void enterElementPin(List<String> tokens) { }
	protected void exitElementPin(List<String> tokens) { }
	protected void enterElementConn(List<String> tokens) { }
	protected void exitElementConn(List<String> tokens) { }
	protected void enterElementCfg(List<String> tokens) { }
	protected void exitElementCfg(List<String> tokens) { }
	protected void enterSummary(List<String> tokens) { }
	protected void exitSummary(List<String> tokens) { }


	public static String stripTrailingParenthesis(String string) {
		if (string.endsWith(")")) {
			return string.substring(0, string.length()-1);
		}
		return string;
	}
}
