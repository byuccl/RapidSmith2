package edu.byu.ece.rapidSmith.examples2;

import java.io.IOException;

import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.edif.core.EdifNameConflictException;
import edu.byu.ece.edif.core.InvalidEdifNameException;
import edu.byu.ece.edif.util.parse.ParseException;
import edu.byu.ece.rapidSmith.RapidSmithEnv;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

public class ImportExportExample {
	
	    // part name and cell library  
	public static final String PART_NAME = "xc7a100tcsg324";
	public static final String CANONICAL_PART_NAME = "xc7a100tcsg324";
	public static final String CELL_LIBRARY = "cellLibrary.xml";
	
	private static CellLibrary libCells;
	private static Device device;
	
	public static void classSetup() throws IOException {
		libCells = new CellLibrary(RapidSmithEnv.getDefaultEnv()
				.getPartFolderPath(PART_NAME)
				.resolve(CELL_LIBRARY));
		device = RapidSmithEnv.getDefaultEnv().getDevice(CANONICAL_PART_NAME);
	}
	
	
	public static void main(String[] args) throws IOException, ParseException, EdifNameConflictException, InvalidEdifNameException {
		// Load device file
		System.out.println("Loading Device...");
		classSetup();
		
		// Loading in a TINCR checkpoint
		System.out.println("Loading Design...");
		String checkpointIn = "count16.tcp";
		String checkpointOut = "count16_modified.tcp";
		CellDesign design = VivadoInterface.loadTCP(checkpointIn);
		
		// Call to CAD tool - do some manipulations
		System.out.println("Modifying Design...");
		// Do something here
		
		// Write out TINCR Checkpoint
		System.out.println("Exporting Modified Design...");
		VivadoInterface.writeTCP(checkpointOut, design);
	}
	
}