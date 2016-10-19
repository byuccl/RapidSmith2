package edu.byu.ece.rapidSmith.examples2;

import java.io.IOException;

import edu.byu.ece.rapidSmith.interfaces.vivado.TincrCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.edif.core.EdifNameConflictException;
import edu.byu.ece.edif.core.InvalidEdifNameException;
import edu.byu.ece.edif.util.parse.ParseException;
import edu.byu.ece.rapidSmith.RapidSmithEnv;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

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
	 * @throws ParseException
	 * @throws EdifNameConflictException
	 * @throws InvalidEdifNameException
	 */
	public static void main(String[] args) throws IOException, ParseException, EdifNameConflictException, InvalidEdifNameException {
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
		
		// Do some manipulations
		System.out.println("Modifying the design...");
		// Do something here
		
		// Write out TINCR Checkpoint
		System.out.println("Exporting Modified Design...");
		VivadoInterface.writeTCP(checkpointOut, design, device, libCells);
		
		System.out.println("\nDone...");
	}
}