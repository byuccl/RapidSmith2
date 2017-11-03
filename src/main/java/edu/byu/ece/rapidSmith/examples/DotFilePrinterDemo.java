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
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.rapidSmith.util.DotFilePrinter;

import java.io.IOException;



/**
 * This program is used to the test the DotFilePrinter by creating
 * a dot file of a RapidSmith2 netlist.  
 * <p>
 * ZGRViewer is a very nice way to view .dot files that have been rendered into an image. 
 * It supports zooming, text-search, path-following, and many other features that make 
 * viewing a graph much easier. It also can easily be called directly from Java. For these reasons,
 * I suggest downloading and using ZGRViewer for dot file visualization. <br>
 * 
 */
public class DotFilePrinterDemo {
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) {

		if (args.length != 2) {
			System.err.println("Usage: java DotFilePrinterDemo checkpointName outfileName");
			System.exit(1);
		}

		System.out.println("Starting DotFilePrinterDemo...\n");
		
		
		System.out.println("Loading Device and Design...");		
		// replace with your file location
		String checkpoint = args[0];
		VivadoCheckpoint vcp = null;
		try {
			vcp = VivadoInterface.loadRSCP(checkpoint);
		} catch (IOException e) {
			System.err.println("Failed loading RSCP");
			e.printStackTrace();
		}
		CellDesign design = vcp.getDesign();
		
		DesignAnalyzer.prettyPrintDesign(design);
	
		System.out.println("\nPrinting DOT file...");
		// testing dot string stuff	
		DotFilePrinter dotFilePrinter = new DotFilePrinter(design);
		// replace with your file location
		String fileout = args[1];
		
		// generate the dot file onto your local disk
		try {
			dotFilePrinter.printPlacementDotFile(fileout);
		} catch (IOException e) {
			System.err.println("Failed writing dot file out");
			e.printStackTrace();
		}

		System.out.println("\nDone...");
	}
	
}
