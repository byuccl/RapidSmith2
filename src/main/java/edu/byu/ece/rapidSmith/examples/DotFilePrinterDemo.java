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
import java.nio.file.Paths;
import java.util.MissingResourceException;


/**
 * This program is used to the test the DotFilePrinter by displaying 
 * a dot file of a RapidSmith2 netlist. If successful a graph of a RapidSMith2 netlist
 * should automatically display in a separate Java window. It is suggested to run this 
 * with the "cordic.rscp" example in the "exampleVivadoDesigns" directory. 
 * <b>To run this program ZGRViewer is required. Follow the instructions below to install ZGRViewer:</b> 
 * 
 * <p>
 * ZGRViewer is a very nice way to view .dot files that have been rendered into an image. 
 * It supports zooming, text-search, path-following, and many other features that make 
 * viewing a graph much easier. It also can easily be called directly from Java. For these reasons,
 * I suggest downloading and using ZGRViewer for dot file visualization in RapidSmith. <br>
 * 
 * <p>
 * How to install and use ZGRViewer from Java:<br>
 * -------------------------------------------
 * <ul>
 * <li> Download ZGRViewer v.0.10.0 at https://sourceforge.net/projects/zvtm/files/zgrviewer/
 * <li> Extract the program to whatever location you wish
 * <li> Create a new system environment variable called "ZGRV" and set is value to your installation directory
 * <li> Restart Eclipse to apply the new environment variables
 * </ul>
 * 
 * <p>
 * After these steps are complete, FPL test programs that visualize DOT files can be run 
 * 
 * <p>
 * To run ZGRViewer from the command line, use the following command: <br> 
 * java -jar "%ZGRV%\target\zgrviewer-0.10.0.jar" -P dot -f netlist.dot -ogl
 * 
 * <p>
 * -ogl: helps for faster rendering, but makes the mouse disappear <br>
 * -P: specify which graphviz program to render the .dot file (dot, neato, circo, or twopi) <br>
 * -f: dot file to display <br> 
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
	
		System.out.println("Printing DOT file...");
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

		// display the dot file
		// "dot" gives the best results, but the GraphViz program used to render can be changed
		System.out.println("Rendering DOT file for display. This may take a little time...");
		try {
			displayDotFile(fileout, "dot");
		} catch (IOException e) {
			System.err.println("Could not display output");
			e.printStackTrace();
		}

		System.out.println("\nDone...");
	}
	
	/**
	 * Function to execute ZGRViewer to visualize the dot file
	 */
	public static void displayDotFile(String dotFile, String program) throws IOException  {
		
		String zgrvInstallionDirectory = System.getenv("ZGRV");
		
		// throw an exception to the user if we can't find the environment variable
		if (zgrvInstallionDirectory == null) {
			throw new MissingResourceException("Path Variable \"ZGRV\" not set","","");
		}

		String jarPath = Paths.get(zgrvInstallionDirectory, "target", "zgrviewer-0.10.0.jar").toString();
		
		// You can optionally omit the "-ogl" option. 
		// It seems to have faster render times, but the mouse disappears which is kind of annoying
		String cmd = String.format("java -jar \"%s\" -P %s -f %s -ogl", jarPath, program, dotFile);
		Runtime.getRuntime().exec(cmd);
	}
	
	/**
	 * Optional function that can visualize DOT files for MAC users.
	 */
	public static void displayDotFileMac(String dotFile) throws IOException  {
		
		// Change this command to whatever command you want executed to view the dot file
		String cmd = String.format("/Applications/Graphviz.app/Contents/MacOS/Graphviz %s", dotFile);
		System.out.println("Executing: " + cmd);
		Runtime.getRuntime().exec(cmd);
	}
}
