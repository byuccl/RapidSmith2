package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import edu.byu.ece.edif.core.EdifNameConflictException;
import edu.byu.ece.edif.core.InvalidEdifNameException;
import edu.byu.ece.edif.util.parse.ParseException;
import edu.byu.ece.rapidSmith.RapidSmithEnv;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * This class is used to interface Vivado and RapidSmith. <br>
 * It parses TINCR checkpoints and creates equivalent RapidSmith designs. <br>
 * It can also create TINCR checkpoints from existing RapidSmith designs.
 * 
 * @author Thomas Townsend
 *
 */
public final class VivadoInterface {

	private static final String CELL_LIBRARY_NAME = "cellLibrary.xml";

	private static String partname;
	private static CellLibrary libCells;
	private static Device device;
		
	/**
	 * Parses a TINCR checkpoint, and creates an equivalent RapidSmith 2 design.
	 * 
	 * @param tcp Path to the TINCR checkpoint to import
	 * @throws InvalidEdifNameException 
	 * @throws EdifNameConflictException 
	 */
	public static CellDesign loadTCP (String tcp) throws IOException, ParseException {
	
		if (tcp.endsWith("/") || tcp.endsWith("\\")) {
			tcp = tcp.substring(0, tcp.length()-1);
		}
		
		// check to make sure the specified directory is a TINCR checkpoint
		if (!tcp.endsWith(".tcp")) {
			throw new AssertionError("Specified directory is not a TINCR checkpoint.");
		}
			
		// setup the cell library and the device based on the part in the TCP file
		partname = parseInfoFile(tcp);
		initializeDevice(partname);
		
		// create the RS2 netlist 
		String edifFile = Paths.get(tcp, "netlist.edf").toString();
		CellDesign design = EdifInterface.parseEdif(edifFile, libCells);
		
		// re-create the placement and routing information
		String placementFile = Paths.get(tcp, "placement.txt").toString();
		XdcPlacementInterface.parsePlacementXDC(placementFile, design, device);

		String routingFile = Paths.get(tcp, "routing.txt").toString();
		XdcRoutingInterface.parseRoutingXDC(routingFile, design, device);
							
		return design;
	}
		
	/*
	 * Load the cell library and device files...
	 */
	private static void initializeDevice(String partname) throws IOException {
		
		libCells = new CellLibrary(RapidSmithEnv.getDefaultEnv()
				.getPartFolderPath(partname)
				.resolve(CELL_LIBRARY_NAME));
		device = RapidSmithEnv.getDefaultEnv().getDevice(partname);	
	}
	
	/*
	 * Parses the Vivado part name from the "design.info" file of a TINCR checkpoint 
	 */
	private static String parseInfoFile (String tcp) throws IOException {
		
		BufferedReader br = null;
		String part = "";
		
		try {
			br = new BufferedReader(new FileReader(tcp + File.separator + "design.info"));
			String line = br.readLine();
			part = line.split("=")[1];
		}
		catch (IndexOutOfBoundsException e) {
			MessageGenerator.briefErrorAndExit("[ERROR]: No part name found in the design.info file.");
		}
		finally {
			if (br != null)
				br.close();
		}
		
		return part;
	}
	
	/**
	 * Export the RapidSmith2 design into an existing TINCR checkpoint file. 
	 *   
	 * @param tcpDirectory TINCR checkpoint directory to write XDC files to
	 * @param design CellDesign to convert to a TINCR checkpoint
	 * @throws IOException
	 * @throws InvalidEdifNameException 
	 * @throws EdifNameConflictException 
	 */
	public static void writeTCP(String tcpDirectory, CellDesign design) throws IOException, EdifNameConflictException, InvalidEdifNameException {
				
		new File(tcpDirectory).mkdir();
		
		String placementOut = Paths.get(tcpDirectory, "placement.xdc").toString();	
		XdcPlacementInterface.writePlacementXDC(placementOut, design);
		
		String routingOut = Paths.get(tcpDirectory, "routing.xdc").toString();
		XdcRoutingInterface.writeRoutingXDC(routingOut, design);
		
		String edifOut = Paths.get(tcpDirectory, "netlist.edf").toString();
		EdifInterface.writeEdif(edifOut, design);
	}
} // END CLASS 
