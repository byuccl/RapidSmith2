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
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import org.jdom2.JDOMException;

import java.io.IOException;

/**
 * A simple class to illustrate importing and exporting RapidSmith checkpoints.
 * @author Brent Nelson
 */
public class ImportExportExample {
	private static String checkpointIn = null;
	private static String checkpointOut = null;
	private static CellDesign design = null;
	private static Device device = null;
	private static CellLibrary libCells = null;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException, JDOMException {
		if (args.length < 1) {
			System.err.println("Usage: ImportExportExample rscpCheckpointDirectoryName");
			System.exit(1);
		}
		String checkpointIn = args[0];
		String checkpointOut = checkpointIn.substring(0, checkpointIn.length() - 4) + "tcp";

		System.out.println("Starting ImportExportExample...\n");
		ImportExportExample ex = new ImportExportExample(checkpointIn, checkpointOut);	
		ex.importExportDesign();

		System.out.println("\nDone...");
	}

	/**
	 * Constructor for ImportExportExample
	 * @param checkpointIn path to the RSCP checkpoint
	 * @param checkpointOut path to the TCP checkpoint
	 */
	public ImportExportExample(String checkpointIn, String checkpointOut)
	{
		ImportExportExample.checkpointIn = checkpointIn;
		ImportExportExample.checkpointOut = checkpointOut;
	}

	/**
	 * Imports, manipulates, and exports the design.
	 * @throws IOException
	 */
	public void importExportDesign() throws IOException
	{
		importDesign();
		manipulateDesign();
		exportDesign();
	}

	/**
	 * Loads in a TINCR checkpoint and gets pieces out to use in manipulating the design.
	 * @throws IOException
	 */
	private void importDesign() throws IOException
	{
		// Load in in a TINCR checkpoint
		System.out.println("Loading Design...");
		VivadoCheckpoint vcp = VivadoInterface.loadRSCP(checkpointIn);

		// Get the pieces out of the checkpoint for use in manipulating it
		ImportExportExample.design = vcp.getDesign();
		ImportExportExample.device= vcp.getDevice();
		ImportExportExample.libCells = vcp.getLibCells();
	}

	/**
	 * Do some manipulations to the design. In this case, just prints out the design.
	 */
	private void manipulateDesign()
	{
		System.out.println("\nPrinting out the design...");
	//	DesignAnalyzer.prettyPrintDesign(design);
	}

	/**
	 * Writes out the TINCR checkpoint.
	 * @throws IOException
	 */
	private void exportDesign() throws IOException
	{
		System.out.println("Exporting Modified Design...");
		VivadoInterface.writeTCP(checkpointOut, design, device, libCells);
	}

}
