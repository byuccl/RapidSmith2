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

package edu.byu.ece.rapidSmith.device.xdlrc;

import edu.byu.ece.rapidSmith.device.xdlrc.XDLRCParserListener.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static edu.byu.ece.rapidSmith.util.Exceptions.ParseException;

/**
 *  Parses an XDLRC file and calls the added listener methods for each encountered
 *  parse element.  This parser is very brittle and expects the file to be
 *  formatted very similar to the structure produced by calling "xdl -report"
 *  including closing parentheses on their own line when appropriate.
 */
public class XDLRCParser {
	// List of listeners to call when a parser element is detected
	private final List<XDLRCParserListener> listeners;

	// XDLRC input stream reader
	private BufferedReader in;
	// Tokens detected on the line
	private List<String> tokens;

	private pl_Conn pl_conn = new pl_Conn();
	private pl_Pip pl_pip = new pl_Pip();
	private pl_Wire pl_wire = new pl_Wire();
	private pl_PinWire pl_pinwire = new pl_PinWire();
	private pl_Routethrough rtTokens = new pl_Routethrough();

	/**
	 * Creates a new XDLRC parser.
	 */
	public XDLRCParser() {
		this.listeners = new ArrayList<>();
	}

	/**
	 * Parses the file specified by the given path.
	 * @param xdlrcFilePath path to the XDLRC file to parse
	 * @throws IOException if an error occurs while opening or reading the file
	 */
	public void parse(Path xdlrcFilePath) throws IOException {
		try (BufferedReader in = Files.newBufferedReader(xdlrcFilePath, Charset.defaultCharset())) {
			this.in = in;
			// (xdl_resource_report <version> <part> <family>
			findMatch("(xdl_resource_report");
			pl_XdlResourceReport xdlReportTokens = new pl_XdlResourceReport();
			xdlReportTokens.version = tokens.get(1);
			xdlReportTokens.part = tokens.get(2);
			xdlReportTokens.family = tokens.get(3);
			listeners.forEach(listener -> listener.enterXdlResourceReport(xdlReportTokens));
			parseXdlResourceReport();
			listeners.forEach(listener -> listener.exitXdlResourceReport(xdlReportTokens));
		}
	}

	/**
	 * Register a new listener with this parser.
	 * @param listener listener to register with this parser
	 */
	public void registerListener(XDLRCParserListener listener) {
		listeners.add(listener);
	}

	/**
	 * Clears all listeners currently associated with this parser.
	 */
	public void clearListeners() {
		listeners.clear();
	}

	private void parseXdlResourceReport() throws IOException {
		// (tiles <rows> <columns>
		findMatch("(tiles");
		parseTiles();

		while (readLine()) {
			switch (tokens.get(0)) {
				// (primitive_defs <count>
				case "(primitive_defs" :
					parsePrimitiveDefs();
					findMatch("(summary");
				// (summary x=y ...
				case "(summary" :
					pl_Summary summaryTokens = new pl_Summary();
					// remove the start and trailing parens
					summaryTokens.stats = tokens.subList(1, tokens.size() - 1);
					listeners.forEach(listener -> listener.enterSummary(summaryTokens));
					listeners.forEach(listener -> listener.exitSummary(summaryTokens));

					findMatch(")");
					return;
			}
		}
		throw new ParseException();
	}

	private void parseTiles() throws IOException {
		pl_Tiles tilesTokens = new pl_Tiles();
		tilesTokens.rows = Integer.parseInt(tokens.get(1));
		tilesTokens.columns = Integer.parseInt(tokens.get(2));
		listeners.forEach(listener -> listener.enterTiles(tilesTokens));

		while (readLine()) {
			switch (tokens.get(0)) {
				// (tile <row> <column> <name> <type> <site_count>
				case "(tile" :
					parseTile();
					break;
				case ")" :
					listeners.forEach(listener -> listener.exitTiles(tilesTokens));
					return;
			}
		}
		throw new ParseException();
	}

	private void parseTile() throws IOException {
		pl_Tile tileTokens = new pl_Tile();
		tileTokens.row = Integer.parseInt(tokens.get(1));
		tileTokens.column = Integer.parseInt(tokens.get(2));
		tileTokens.name = tokens.get(3);
		tileTokens.type = tokens.get(4);
		tileTokens.site_count = Integer.parseInt(tokens.get(5));
		listeners.forEach(listener -> listener.enterTile(tileTokens));

		while (readLine()) {
			switch (tokens.get(0)) {
				// (primitive_site <name> <type> <bonded> <pinwire_count>
				case "(primitive_site" :
					parsePrimitiveSite();
					break;
				// (wire <name> <connection_count>
				case "(wire" :
					parseWire();
					break;
				// (pip <tile> <start_wire> <direction> <end_wire> <rt_name> <rt_site>
				case "(pip" :
					pl_Pip pipTokens = pl_pip;
					pipTokens.tile = tokens.get(1);
					pipTokens.start_wire = tokens.get(2);
					pipTokens.type = tokens.get(3);
					pipTokens.end_wire = tokens.get(4);
					listeners.forEach(listener -> listener.enterPip(pipTokens));

					if (tokens.size() > 6) {
						String lastValue = tokens.get(6);
						tokens.set(6, lastValue.substring(0, lastValue.length()-1));
						tokens.add(")");
						rtTokens.pins = tokens.get(5);
						rtTokens.site_type = tokens.get(6);
						listeners.forEach(listener -> listener.enterRoutethrough(rtTokens));
						listeners.forEach(listener -> listener.exitRoutethrough(rtTokens));
					}

					listeners.forEach(listener -> listener.exitPip(pipTokens));
					break;
				// (tile_summary <name> <type> <pin_count> <wire_count> <pip_count>
				case "(tile_summary" :
					pl_TileSummary tsTokens = new pl_TileSummary();
					tsTokens.name = tokens.get(1);
					tsTokens.type = tokens.get(2);
					tsTokens.pin_count = Integer.parseInt(tokens.get(3));
					tsTokens.wire_count = Integer.parseInt(tokens.get(4));
					tsTokens.pip_count = Integer.parseInt(tokens.get(5));
					listeners.forEach(listener -> listener.enterTileSummary(tsTokens));
					listeners.forEach(listener -> listener.exitTileSummary(tsTokens));
					break;
				case ")" :
					listeners.forEach(listener -> listener.exitTile(tileTokens));
					return;
			}
		}
		throw new ParseException();
	}

	private void parsePrimitiveSite() throws IOException {
		pl_PrimitiveSite siteTokens = new pl_PrimitiveSite();
		siteTokens.name = tokens.get(1);
		siteTokens.type = tokens.get(2);
		siteTokens.bonded = tokens.get(3);
		siteTokens.pinwire_count = Integer.parseInt(tokens.get(4));
		listeners.forEach(listener -> listener.enterPrimitiveSite(siteTokens));

		while(readLine()) {
			switch (tokens.get(0)) {
				// (pinwire <name> <direction> <external_wire>
				case "(pinwire" :
					pl_PinWire pwTokens = pl_pinwire;
					pwTokens.name = tokens.get(1);
					pwTokens.direction = tokens.get(2);
					pwTokens.external_wire = tokens.get(3);
					listeners.forEach(listener -> listener.enterPinWire(pwTokens));
					listeners.forEach(listener -> listener.exitPinWire(pwTokens));
					break;
				case ")" :
					listeners.forEach(listener -> listener.exitPrimitiveSite(siteTokens));
					return;
			}
		}
		throw new ParseException();
	}

	private void parseWire() throws IOException {
		pl_Wire wireTokens = pl_wire;
		pl_wire.name = tokens.get(1);
		pl_wire.connections_count = Integer.parseInt(tokens.get(2));
		listeners.forEach(listener -> listener.enterWire(wireTokens));

		if (tokens.get(tokens.size()-1).equals(")")) {
			listeners.forEach(listener -> listener.exitWire(wireTokens));
			return;
		}

		while (readLine()) {
			switch (tokens.get(0)) {
				// (conn <tile> <name>
				case "(conn" :
					pl_Conn connTokens = pl_conn;
					connTokens.tile = tokens.get(1);
					connTokens.wire = tokens.get(2);
					listeners.forEach(listener -> listener.enterConn(connTokens));
					listeners.forEach(listener -> listener.exitConn(connTokens));
					break;
				case ")" :
					listeners.forEach(listener -> listener.exitWire(wireTokens));
					return;
			}
		}
		throw new ParseException();
	}

	private void parsePrimitiveDefs() throws IOException {
		pl_PrimitiveDefs pdTokens = new pl_PrimitiveDefs();
		pdTokens.num_defs = Integer.parseInt(tokens.get(1));
		listeners.forEach(listener -> listener.enterPrimitiveDefs(pdTokens));

		while (readLine()) {
			switch (tokens.get(0)) {
				case "(primitive_def" :
					parsePrimitiveDef();
					break;
				case ")" :
					listeners.forEach(listener -> listener.exitPrimitiveDefs(pdTokens));
					return;
			}
		}
	}

	private void parsePrimitiveDef() throws IOException {
		pl_PrimitiveDef pdTokens = new pl_PrimitiveDef();
		pdTokens.name = tokens.get(1);
		pdTokens.pin_count = Integer.parseInt(tokens.get(2));
		pdTokens.element_count = Integer.parseInt(tokens.get(3));
		listeners.forEach(listener -> listener.enterPrimitiveDef(pdTokens));

		while (readLine()) {
			switch (tokens.get(0)) {
				case "(pin" :
					pl_Pin pinTokens = new pl_Pin();
					pinTokens.external_name = tokens.get(1);
					pinTokens.internal_name = tokens.get(2);
					pinTokens.direction = tokens.get(3);
					listeners.forEach(listener -> listener.enterPin(pinTokens));
					listeners.forEach(listener -> listener.exitPin(pinTokens));
					break;
				case "(element" :
					parseElement();
					break;
				case ")" :
					listeners.forEach(listener -> listener.exitPrimitiveDef(pdTokens));
					return;
			}
		}
		throw new ParseException();
	}

	private void parseElement() throws IOException {
		pl_Element elementTokens = new pl_Element();
		elementTokens.name = tokens.get(1);
		elementTokens.pin_count = Integer.parseInt(tokens.get(2));
		elementTokens.isBel = tokens.size() >= 5 && tokens.get(3).equals("#") && tokens.get(4).equals("BEL");

		listeners.forEach(listener -> listener.enterElement(elementTokens));

		while (readLine()) {
			switch (tokens.get(0)) {
				case "(pin" :
					pl_ElementPin pinTokens = new pl_ElementPin();
					pinTokens.name = tokens.get(1);
					pinTokens.direction = tokens.get(2);
					listeners.forEach(listener -> listener.enterElementPin(pinTokens));
					listeners.forEach(listener -> listener.exitElementPin(pinTokens));
					break;
				case "(cfg" :
					pl_ElementCfg cfgTokens = new pl_ElementCfg();
					// remove the start and trailing parens
					cfgTokens.cfgs = tokens.subList(1, tokens.size() - 1);
					listeners.forEach(listener -> listener.enterElementCfg(cfgTokens));
					listeners.forEach(listener -> listener.exitElementCfg(cfgTokens));
					break;
				case "(conn" :
					pl_ElementConn connTokens = new pl_ElementConn();
					connTokens.element0 = tokens.get(1);
					connTokens.pin0 = tokens.get(2);
					connTokens.direction = tokens.get(3);
					connTokens.element1 = tokens.get(4);
					connTokens.pin1 = tokens.get(5);
					listeners.forEach(listener -> listener.enterElementConn(connTokens));
					listeners.forEach(listener -> listener.exitElementConn(connTokens));
					break;
				case ")" :
					listeners.forEach(listener -> listener.exitElement(elementTokens));
					return;
			}
		}
		throw new ParseException();
	}

	/**
	 * Iterates through the lines in the file until a line is found that starts
	 * with the specified token.
	 */
	private void findMatch(String token) throws IOException {
		while (readLine()) {
			if (tokens.get(0).equals(token))
				return;
		}
		throw new ParseException();
	}

	/**
	 * Reads the next line from the file and parses it into tokens
	 */
	private boolean readLine() throws IOException {
		// filter out empty lines
		while (true) {
			// read next line
			String line = in.readLine();

			// check if end of file
			if (line == null) {
				tokens = null;
				return false;
			}

			tokens = split(line);
			if (!tokens.isEmpty())
				return true;
		}
	}

	private static List<String> split(String line) {
		List<String> parts = new ArrayList<>();
		int startIndex = 0, endIndex;

		// Strip any starting tabs and check for empty lines
		while (line.length() > startIndex && line.charAt(startIndex) == '\t')
			startIndex++;

		// Continue while there are more unread characters on the line
		while (line.length() > startIndex) {
			// find the next space character.  Three outcomes
			// 1. word found ending at end of line -- add word to the list
			// 2. word found ending in space -- add the word to the list
			// 3. next space is next character -- strip the space character
			endIndex = line.indexOf(" ", startIndex);

			if (endIndex == -1) {
				parts.add(line.substring(startIndex));
				break;
			} else if (endIndex != startIndex) {
				parts.add(line.substring(startIndex, endIndex));
			}
			startIndex = endIndex + 1;
		}

		// strip any trailing parenthesis
		stripTrailingParen(parts);

		return parts;
	}

	private static void stripTrailingParen(List<String> tokens) {
		int lastIndex = tokens.size() - 1;
		String lastValue = tokens.get(lastIndex);
		if (lastValue.length() > 1 && lastValue.endsWith(")")) {
			tokens.set(lastIndex, lastValue.substring(0, lastValue.length()-1));
			tokens.add(")");
		}
	}
}
