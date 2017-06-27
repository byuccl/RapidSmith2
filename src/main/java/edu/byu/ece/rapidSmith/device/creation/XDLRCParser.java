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
			List<String> xdlReportTokens = tokens;
			for (XDLRCParserListener listener : listeners)
				listener.enterXdlResourceReport(tokens);
			parseXdlResourceReport();
			for (XDLRCParserListener listener : listeners)
				listener.exitXdlResourceReport(xdlReportTokens);
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

		// (primitive_defs <count>
		while (readLine()) {
			switch (tokens.get(0)) {
				// the primitive_defs section is optional
				case "(primitive_defs" :
					parsePrimitiveDefs();
					findMatch("(summary");
				case "(summary" :
					for (XDLRCParserListener listener : listeners)
						listener.enterSummary(tokens);
					for (XDLRCParserListener listener : listeners)
						listener.exitSummary(tokens);

					findMatch(")");
					return;
			}
		}
		throw new ParseException();
	}

	private void parseTiles() throws IOException {
		List<String> tilesTokens = tokens;
		for (XDLRCParserListener listener : listeners)
			listener.enterTiles(tilesTokens);

		while (readLine()) {
			switch (tokens.get(0)) {
				case "(tile" :
					parseTile();
					break;
				case ")" :
					for (XDLRCParserListener listener : listeners)
						listener.exitTiles(tilesTokens);
					return;
			}
		}
		throw new ParseException();
	}

	private void parseTile() throws IOException {
		List<String> tileTokens = tokens;
		for (XDLRCParserListener listener : listeners)
			listener.enterTile(tileTokens);

		while (readLine()) {
			switch (tokens.get(0)) {
				case "(primitive_site" :
					parsePrimitiveSite();
					break;
				case "(wire" :
					parseWire();
					break;
				case "(pip" :
					for (XDLRCParserListener listener : listeners)
						listener.enterPip(tokens);
					for (XDLRCParserListener listener : listeners)
						listener.exitPip(tokens);
					break;
				case "(tile_summary" :
					for (XDLRCParserListener listener : listeners)
						listener.enterTileSummary(tokens);
					for (XDLRCParserListener listener : listeners)
						listener.exitTileSummary(tokens);
					break;
				case ")" :
					for (XDLRCParserListener listener : listeners)
						listener.exitTile(tileTokens);
					return;
			}
		}
		throw new ParseException();
	}

	private void parsePrimitiveSite() throws IOException {
		List<String> siteTokens = this.tokens;
		for (XDLRCParserListener listener : listeners)
			listener.enterPrimitiveSite(siteTokens);

		while(readLine()) {
			switch (tokens.get(0)) {
				case "(pinwire" :
					for (XDLRCParserListener listener : listeners)
						listener.enterPinWire(tokens);
					for (XDLRCParserListener listener : listeners)
						listener.exitPinWire(tokens);
					break;
				case ")" :
					for (XDLRCParserListener listener : listeners)
						listener.exitPrimitiveSite(siteTokens);
					return;
			}
		}
		throw new ParseException();
	}

	private void parseWire() throws IOException {
		List<String> wireTokens = this.tokens;
		for (XDLRCParserListener listener : listeners)
			listener.enterWire(wireTokens);

		if (tokens.get(tokens.size()-1).endsWith(")")) {
			for (XDLRCParserListener listener : listeners)
				listener.exitWire(wireTokens);
			return;
		}

		while (readLine()) {
			switch (tokens.get(0)) {
				case "(conn" :
					for (XDLRCParserListener listener : listeners)
						listener.enterConn(tokens);
					for (XDLRCParserListener listener : listeners)
						listener.exitConn(tokens);
					break;
				case ")" :
					for (XDLRCParserListener listener : listeners)
						listener.exitWire(wireTokens);
					return;
			}
		}
		throw new ParseException();
	}

	private void parsePrimitiveDefs() throws IOException {
		List<String> primitiveDefsTokens = tokens;
		for (XDLRCParserListener listener : listeners)
			listener.enterPrimitiveDefs(primitiveDefsTokens);

		while (readLine()) {
			switch (tokens.get(0)) {
				case "(primitive_def" :
					parsePrimitiveDef();
					break;
				case ")" :
					for (XDLRCParserListener listener : listeners)
						listener.exitPrimitiveDefs(primitiveDefsTokens);
					return;
			}
		}
	}

	private void parsePrimitiveDef() throws IOException {
		List<String> primitiveDefTokens = tokens;
		for (XDLRCParserListener listener : listeners)
			listener.enterPrimitiveDef(primitiveDefTokens);

		while (readLine()) {
			switch (tokens.get(0)) {
				case "(pin" :
					for (XDLRCParserListener listener : listeners)
						listener.enterPin(tokens);
					for (XDLRCParserListener listener : listeners)
						listener.exitPin(tokens);
					break;
				case "(element" :
					parseElement();
					break;
				case ")" :
					for (XDLRCParserListener listener : listeners)
						listener.exitPrimitiveDef(primitiveDefTokens);
					return;
			}
		}
		throw new ParseException();
	}

	private void parseElement() throws IOException {
		List<String> elementTokens = tokens;
		for (XDLRCParserListener listener : listeners)
			listener.enterElement(elementTokens);

		while (readLine()) {
			switch (tokens.get(0)) {
				case "(pin" :
					for (XDLRCParserListener listener : listeners)
						listener.enterElementPin(tokens);
					for (XDLRCParserListener listener : listeners)
						listener.exitElementPin(tokens);
					break;
				case "(cfg" :
					for (XDLRCParserListener listener : listeners)
						listener.enterElementCfg(tokens);
					for (XDLRCParserListener listener : listeners)
						listener.exitElementCfg(tokens);
					break;
				case "(conn" :
					for (XDLRCParserListener listener : listeners)
						listener.enterElementConn(tokens);
					for (XDLRCParserListener listener : listeners)
						listener.exitElementConn(tokens);
					break;
				case ")" :
					for (XDLRCParserListener listener : listeners)
						listener.exitElement(elementTokens);
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

		while (line.length() > startIndex && line.charAt(startIndex) == '\t')
			startIndex++;

		while (line.length() > startIndex) {
			endIndex = line.indexOf(" ", startIndex);
			if (endIndex == -1) {
				parts.add(line.substring(startIndex));
				break;
			} else if (endIndex != startIndex) {
				parts.add(line.substring(startIndex, endIndex));
			}
			startIndex = endIndex + 1;
		}

		return parts;
	}
}
