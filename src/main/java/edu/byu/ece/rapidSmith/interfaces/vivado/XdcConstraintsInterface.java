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

package edu.byu.ece.rapidSmith.interfaces.vivado;

import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.util.Exceptions;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static edu.byu.ece.rapidSmith.util.Exceptions.ImplementationException;
import static edu.byu.ece.rapidSmith.util.Exceptions.ParseException;

/**
 * This class is used for parsing XDC constraint files and adding them into a RS2 design.
 * 
 * @author Dallon Glick, Dr. Jeffrey Goeders
 *
 */
public class XdcConstraintsInterface {
	private final CellDesign design;

	public XdcConstraintsInterface(CellDesign design, Device device) {
		this.design = design;
	}

	private void parseConstraintsLine(String line) {
		// assuming a space after the command TODO: make sure this assumption is correct
		int index = line.indexOf(" ");
		String command = line.substring(0, index);
		String options = line.substring(index + 1);
		design.addVivadoConstraint(new XdcConstraint(command, options));
	}

	/**
	 * Loads Vivado constraints into the specified {@link CellDesign}. For now, these constraints are
	 * loaded as two strings, a command and a list of arguments. There is not much of an attempt right now to
	 * intelligently handle these constraints, and they are included so the user has access to them.
	 * TODO: Update how we handle constraints files to make them easier to move
	 * @param xdcFile constraints.xdc file
	 * @throws IOException
	 */
	public void parseConstraintsXDC(String xdcFile) throws IOException {
		LineNumberReader br = new LineNumberReader(new BufferedReader(new FileReader(xdcFile)));
		String line;

		while ((line = br.readLine()) != null) {
			String trimmed = line.trim();

			// Skip empty and commented lines
			// TODO: Is line.equals("") really needed?
			if (line.equals("") || line.matches("\\s*#.*"))
				continue;

			parseConstraintsLine(trimmed);
		}

		br.close();
	}

}
