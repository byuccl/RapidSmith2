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

package edu.byu.ece.rapidSmith.examples;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoDesignCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.rapidSmith.util.DotFilePrinter;

import java.io.IOException;


/**
 * This program is used to the test the DotFilePrinter in RapidSmith <br>
 * by displaying a dot file after it has been created <br>
 * 
 * If successful a dot file should automatically display in a separate Java window.
 * 
 * @author Thomas Townsend
 *
 */
public class DotFilePrinterDemo {
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) {

		if (args.length != 1) {
			System.err.println("Usage: java DotFilePrinterDemo checkpointName");
			System.exit(1);
		}

		System.out.println("Starting DotFilePrinterDemo...\n");
		
		
		System.out.println("Loading Device and Design...");		
		// replace with your file location
		String checkpoint = args[0];
		VivadoDesignCheckpoint tcp = null;
		try {
			tcp = VivadoInterface.loadRSCP(checkpoint);
		} catch (IOException e) {
			System.err.println("Failed loading TCP");
			e.printStackTrace();
		}
		CellDesign design = tcp.getDesign();
	
		System.out.println("Printing DOT file...");
		// testing dot string stuff	
		DotFilePrinter dotFilePrinter = new DotFilePrinter(design);
		// replace with your file location
		String fileout = "netlist.dot";
		try {
			dotFilePrinter.printPlacementDotFile(fileout);
		} catch (IOException e) {
			System.err.println("Failed writing dot file out");
			e.printStackTrace();
		}

		// dot gives the best results, but the GraphViz program used to render can be changed
		try {
			displayDotFile(fileout);
		} catch (IOException e) {
			System.err.println("Could not display output");
			e.printStackTrace();
		}

		System.out.println("\nDone...");
	}
	
	/**
	 * @param dotFile
	 * @throws IOException
	 */
	public static void displayDotFile(String dotFile) throws IOException  {
		
		// Change this command to whatever command you want executed to view the dot file
		String cmd = String.format("/Applications/Graphviz.app/Contents/MacOS/Graphviz %s", dotFile);
		System.out.println("Executing: " + cmd);
		Runtime.getRuntime().exec(cmd);
	}
}
