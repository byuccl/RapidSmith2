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

package edu.byu.ece.rapidSmith.examples2;

import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.interfaces.vivado.TincrCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import org.jdom2.JDOMException;

import java.io.IOException;

/**
 * A simple class to illustrate importing and exporting Tincr checkpoints.
 * @author Brent Nelson
 */

/**
 * @author nelson
 *
 */
public class ImportExportExample {
	
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException, JDOMException {
		if (args.length < 1) {
			System.err.println("Usage: ImportExportExample tincrCheckpointName");
			System.exit(1);
		}
		
		String checkpointIn = args[0];
		String checkpointOut = args[0] + ".modified";

		System.out.println("Starting ImportExportExample...\n");

		// Load in in a TINCR checkpoint
		System.out.println("Loading Design...");
		TincrCheckpoint tcp = VivadoInterface.loadTCP(checkpointIn);
		
		// Get the pieces out of the checkpoint for use in manipulating it
		CellDesign design = tcp.getDesign();
		Device device= tcp.getDevice();
		CellLibrary libCells = tcp.getLibCells();
		String partName = tcp.getPartName();
		
		// Do some manipulations, in this case just print out the design
		System.out.println("\nPrinting out the design...");
		DesignAnalyzer.prettyPrintDesign(design);
		
		// Write out TINCR Checkpoint
		System.out.println("Exporting Modified Design...");
		VivadoInterface.writeTCP(checkpointOut, design, device, libCells);
		
		System.out.println("\nDone...");
	}
}
