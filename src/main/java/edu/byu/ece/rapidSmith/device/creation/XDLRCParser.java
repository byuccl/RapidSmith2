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

import edu.byu.ece.rapidSmith.device.creation.XDLRCParserListener.*;

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
			pl_XdlResourceReport xdlReportTokens = new pl_XdlResourceReport(tokens);
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
					pl_Summary summaryTokens = new pl_Summary(tokens);
					listeners.forEach(listener -> listener.enterSummary(summaryTokens));
					listeners.forEach(listener -> listener.exitSummary(summaryTokens));

					findMatch(")");
					return;
			}
		}
		throw new ParseException();
	}

	private void parseTiles() throws IOException {
		pl_Tiles tilesTokens = new pl_Tiles(tokens);
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
		pl_Tile tileTokens = new pl_Tile(tokens);
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
					pl_Pip pipTokens = new pl_Pip(tokens);
					listeners.forEach(listener -> listener.enterPip(pipTokens));

					if (tokens.size() > 5) {
						tokens.set(6, stripTrailingParen(tokens.get(6)));
						pl_Routethrough rtTokens = new pl_Routethrough(tokens);
						listeners.forEach(listener -> listener.enterRoutethrough(rtTokens));
						listeners.forEach(listener -> listener.exitRoutethrough(rtTokens));
					}

					listeners.forEach(listener -> listener.exitPip(pipTokens));
					break;
				// (tile_summary <name> <type> <pin_count> <wire_count> <pip_count>
				case "(tile_summary" :
					pl_TileSummary tsTokens = new pl_TileSummary(tokens);
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
		pl_PrimitiveSite siteTokens = new pl_PrimitiveSite(tokens);
		listeners.forEach(listener -> listener.enterPrimitiveSite(siteTokens));

		while(readLine()) {
			switch (tokens.get(0)) {
				// (pinwire <name> <direction> <external_wire>
				case "(pinwire" :
					pl_PinWire pwTokens = new pl_PinWire(tokens);
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
		pl_Wire wireTokens = new pl_Wire(tokens);
		listeners.forEach(listener -> listener.enterWire(wireTokens));

		if (tokens.get(tokens.size()-1).endsWith(")")) {
			listeners.forEach(listener -> listener.exitWire(wireTokens));
			return;
		}

		while (readLine()) {
			switch (tokens.get(0)) {
				// (conn <tile> <name>
				case "(conn" :
					pl_Conn connTokens = new pl_Conn(tokens);
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
		pl_PrimitiveDefs primitiveDefsTokens = new pl_PrimitiveDefs(tokens);
		listeners.forEach(listener -> listener.enterPrimitiveDefs(primitiveDefsTokens));

		while (readLine()) {
			switch (tokens.get(0)) {
				case "(primitive_def" :
					parsePrimitiveDef();
					break;
				case ")" :
					listeners.forEach(listener -> listener.exitPrimitiveDefs(primitiveDefsTokens));
					return;
			}
		}
	}

	private void parsePrimitiveDef() throws IOException {
		pl_PrimitiveDef primitiveDefTokens = new pl_PrimitiveDef(tokens);
		listeners.forEach(listener -> listener.enterPrimitiveDef(primitiveDefTokens));

		while (readLine()) {
			switch (tokens.get(0)) {
				case "(pin" :
					pl_Pin pinTokens = new pl_Pin(tokens);
					listeners.forEach(listener -> listener.enterPin(pinTokens));
					listeners.forEach(listener -> listener.exitPin(pinTokens));
					break;
				case "(element" :
					parseElement();
					break;
				case ")" :
					listeners.forEach(listener -> listener.exitPrimitiveDef(primitiveDefTokens));
					return;
			}
		}
		throw new ParseException();
	}

	private void parseElement() throws IOException {
		pl_Element elementTokens = new pl_Element(tokens);
		listeners.forEach(listener -> listener.enterElement(elementTokens));

		while (readLine()) {
			switch (tokens.get(0)) {
				case "(pin" :
					pl_ElementPin pinTokens = new pl_ElementPin(tokens);
					listeners.forEach(listener -> listener.enterElementPin(pinTokens));
					listeners.forEach(listener -> listener.exitElementPin(pinTokens));
					break;
				case "(cfg" :
					pl_ElementCfg cfgTokens = new pl_ElementCfg(tokens);
					listeners.forEach(listener -> listener.enterElementCfg(cfgTokens));
					listeners.forEach(listener -> listener.exitElementCfg(cfgTokens));
					break;
				case "(conn" :
					pl_ElementConn connTokens = new pl_ElementConn(tokens);
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
			// 1. wrod found ending at end of line -- add word to the list
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
		int lastIndex = parts.size() - 1;
		parts.set(lastIndex, stripTrailingParen(parts.get(lastIndex)));

		return parts;
	}

	private static String stripTrailingParen(String string) {
		if (string.endsWith(")")) {
			return string.substring(0, string.length()-1);
		}
		return string;
	}
}
