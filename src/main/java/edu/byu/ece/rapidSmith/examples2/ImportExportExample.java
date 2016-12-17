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
