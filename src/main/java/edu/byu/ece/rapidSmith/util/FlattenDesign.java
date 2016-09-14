/*
 * Copyright (c) 2010-2011 Brigham Young University
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
package edu.byu.ece.rapidSmith.util;

import edu.byu.ece.rapidSmith.design.xdl.Design;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLWriter;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLReader;

import java.io.IOException;
import java.nio.file.Paths;

public class FlattenDesign {
	public static void main(String[] args) throws IOException {
		if(args.length != 2){
			MessageGenerator.briefMessageAndExit("USAGE: <inputDesign.xdl> <flattenedDesign.xdl>");
		}
		Design d = new XDLReader().readDesign(Paths.get(args[0]));
		d.flattenDesign();
		XDLWriter writer = new XDLWriter();
		writer.writeXDL(d, Paths.get(args[1]));
	}
}
