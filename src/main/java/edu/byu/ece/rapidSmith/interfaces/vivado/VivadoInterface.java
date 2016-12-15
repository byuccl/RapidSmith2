package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import edu.byu.ece.edif.core.EdifNameConflictException;
import edu.byu.ece.edif.core.InvalidEdifNameException;
import static edu.byu.ece.rapidSmith.util.Exceptions.ParseException;
import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.util.Exceptions;

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

	public static TincrCheckpoint loadTCP(String tcp) throws IOException {
		return loadTCP(tcp, false);
	}
	
	/**
	 * Parses a TINCR checkpoint, and creates an equivalent RapidSmith 2 design.
	 * 
	 * @param tcp Path to the TINCR checkpoint to import
	 * @throws InvalidEdifNameException 
	 * @throws EdifNameConflictException 
	 */
	public static TincrCheckpoint loadTCP (String tcp, boolean storeAdditionalInfo) throws IOException {
	
		if (tcp.endsWith("/") || tcp.endsWith("\\")) {
			tcp = tcp.substring(0, tcp.length()-1);
		}
		
		// check to make sure the specified directory is a TINCR checkpoint
		if (!tcp.endsWith(".tcp")) {
			throw new AssertionError("Specified directory is not a TINCR checkpoint.");
		}
			
		// setup the cell library and the device based on the part in the TCP file
		String partName = DesignInfoInterface.parseInfoFile(Paths.get(tcp, "design.info").toString());
		CellLibrary libCells = new CellLibrary(RSEnvironment.defaultEnv()
				.getPartFolderPath(partName)
				.resolve(CELL_LIBRARY_NAME));
		Device device = RSEnvironment.defaultEnv().getDevice(partName);
		
		// TODO: throw an exception here if the we can't find the device

		if (device == null) {
			throw new Exceptions.EnvironmentException("Device files for part: " + partName + " cannot be found.");
		}
		
		// create the RS2 netlist
		String edifFile = Paths.get(tcp, "netlist.edf").toString();
		CellDesign design;
		try {
			design = EdifInterface.parseEdif(edifFile, libCells);
		} catch (edu.byu.ece.edif.util.parse.ParseException e) {
			throw new ParseException(e);
		}
		
		// re-create the placement and routing information
		String placementFile = Paths.get(tcp, "placement.rsc").toString();
		XdcPlacementInterface placementInterface = new XdcPlacementInterface(design, device);
		placementInterface.parsePlacementXDC(placementFile);
 
		String routingFile = Paths.get(tcp, "routing.rsc").toString();
		XdcRoutingInterface routingInterface = new XdcRoutingInterface(design, device, placementInterface.getPinMap());
		routingInterface.parseRoutingXDC(routingFile);
		
		TincrCheckpoint tincrCheckpoint = new TincrCheckpoint(partName, design, device, libCells); 
		
		if (storeAdditionalInfo) {
			tincrCheckpoint.setRoutethroughBels(routingInterface.getRoutethroughsBels());
			tincrCheckpoint.setStaticSourceBels(routingInterface.getStaticSourceBels());
			tincrCheckpoint.setBelPinToCellPinMap(placementInterface.getPinMap());
		}
		
		return tincrCheckpoint;
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
	public static void writeTCP(String tcpDirectory, CellDesign design, Device device, CellLibrary libCells) throws IOException {
				
		new File(tcpDirectory).mkdir();
		
		// insert routethrough buffers
		LutRoutethroughInserter inserter = new LutRoutethroughInserter(design, libCells);
		inserter.execute();
		
		String placementOut = Paths.get(tcpDirectory, "placement.xdc").toString();	
		XdcPlacementInterface placementInterface = new XdcPlacementInterface(design, device);
		placementInterface.writePlacementXDC(placementOut);
		
		String routingOut = Paths.get(tcpDirectory, "routing.xdc").toString();
		XdcRoutingInterface routingInterface = new XdcRoutingInterface(design, device, null);
		routingInterface.writeRoutingXDC(routingOut, design);
		
		String edifOut = Paths.get(tcpDirectory, "netlist.edf").toString();
		
		try {
			EdifInterface.writeEdif(edifOut, design);
		} 
		catch (EdifNameConflictException | InvalidEdifNameException e) {
			throw new AssertionError(e); 
		}

		String partInfoOut = Paths.get(tcpDirectory, "design.info").toString();
		DesignInfoInterface.writeInfoFile(partInfoOut, device.getPartName());
				
	}	
} // END CLASS 
