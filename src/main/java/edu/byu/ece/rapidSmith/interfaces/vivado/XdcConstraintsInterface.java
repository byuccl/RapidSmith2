/*
 * Copyright (c) 2018 Brigham Young University
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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used for parsing XDC constraint files and adding them into a RS2 design.
 * 
 * @author Dallon Glick, Dr. Jeffrey Goeders
 *
 */
public class XdcConstraintsInterface {
	private final CellDesign design;
	private final Device device;

	public XdcConstraintsInterface(CellDesign design, Device device) {
		this.design = design;
		this.device = device;
	}

    /**
     * Splits an XDC constraint that includes a dictionary from a single command to multiple properties
     * into individual XDC constraints that include only one command and one property.
     * Example 1: set_property -dict "name1 value1 ... nameN valueN" [get_ports {portName}]
     * Example 2: set_property -dict {name1 value1 ... nameN valueN} [get_ports {portName}]
     * @param line the constraint line to split
     * @return a list of the individual constraint strings
     */
    private ArrayList<String> splitDictConstraints(String line) {

        int prefixEndIndex = line.lastIndexOf("-dict") + 5;

        // TODO: Make this work with constraints other than get_ports.
        int suffixBeginIndex = line.lastIndexOf("[get_ports");
        String suffix = line.substring(suffixBeginIndex, line.length());
        String prefix = line.substring(0, line.lastIndexOf("-dict"));

        // Split up the individual properties
        String properties = line.substring(prefixEndIndex, suffixBeginIndex);
        properties = properties.replaceAll("[{}]", "");
        properties = properties.trim();

        ArrayList<String> splitConstraints = new ArrayList<>();
        String[] splitProps = properties.split("\\s+");

        for (int i = 0; i < splitProps.length - 1; i+=2)
        {
            splitConstraints.add(prefix + splitProps[i] + " " + splitProps[i+1] + " " + suffix);
        }

        return splitConstraints;
    }

    /**
     * Parses a single line from constraints.xdc, makes an XdcConstraint, and adds it to the design.
     * @param line the constraint line to parse
     */
	private void parseConstraintsLine(String line) {
        // Remove same line comments
        int commentIndex = line.indexOf("#");
        line = (commentIndex != -1) ? line.substring(0, commentIndex) : line;
        line = line.trim();
        line = line.replaceAll("[;]", "");


        int getPortsIndex = line.lastIndexOf("[get_ports");
        if (getPortsIndex != -1) {
            // Remove curly braces from [get_ports ... ] suffix
            String suffix = line.substring(getPortsIndex, line.length());
            suffix = suffix.replaceAll("[{}]", "");
            line = line.substring(0, line.lastIndexOf("[get_ports") - 1) + " " + suffix;

            if (line.matches("(.*)(-dict)(.*)")) {
                // Split up dict constraints into individual constraints for ease of use
                ArrayList<String> splitLines = splitDictConstraints(line);

                for (String splitLine : splitLines) {
                    int index = splitLine.indexOf(" ");
                    String command = splitLine.substring(0, index);
                    String options = splitLine.substring(index + 1);
                    design.addVivadoConstraint(new XdcConstraint(command, options));
                }
            }
            else {
                // assuming a space after the command TODO: make sure this assumption is correct
                int index = line.indexOf(" ");
                String command = line.substring(0, index);
                String options = line.substring(index + 1);
                design.addVivadoConstraint(new XdcConstraint(command, options));
            }
        }
        else {
            // Not a constraint that ends in "[get_ports ... ]
            int index = line.indexOf(" ");
            String command = line.substring(0, index);
            String options = line.substring(index + 1);
            design.addVivadoConstraint(new XdcConstraint(command, options));
        }
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
			if (line.equals("") || line.matches("\\s*#.*"))
				continue;

			parseConstraintsLine(trimmed);
		}

		br.close();
	}

}
