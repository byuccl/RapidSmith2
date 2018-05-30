package edu.byu.ece.rapidSmith.examples.aStarRouter;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.rapidSmith.util.DotFilePrinter;
import edu.byu.ece.rapidSmith.util.RapidSmithDebug;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates the sample {@link AStarRouter} on a single net in a CORDIC design. 
 * To see the results of the router, open up the equivalent cordic.dcp file in Vivado,
 * and run the highlight TCL command that is produced after running this program. This
 * file also demonstrates how to load the extended device information that is not
 * included by default (reverse wire connections for example).
 * 
 * View the {@link AStarRouter} source code as an example of how to creating
 * routing algorithms in RapidSmith.
 */
public class AStarTest {

	public static void main(String[] args) throws IOException {

		if (args.length < 1) {
			System.err.println("Usage: ImportPlaceRouteExport rscpCheckpointDirectoryName");
			System.exit(1);
		}

		String checkpointIn = args[0];
		System.out.println("Loading Device and Design...");
		VivadoCheckpoint vcp = VivadoInterface.loadRSCP(checkpointIn, true);
		CellDesign design = vcp.getDesign();

		// Routing net
		System.out.println("Routing Net...");
		Map<Wire, PFCost> wireUsage = new HashMap<>();
		AStarRouter router = new AStarRouter(wireUsage);
		CellNet net = design.getNet("ld_IBUF");
		//RouteTree test = router.routeNet(net);
		//net.addIntersiteRouteTree(test);
		
		// Displaying results
		System.out.println("\nCommand to highlight chosen wires in Vivado: ");
		//System.out.println(RapidSmithDebug.createHighlightWiresTclCommand(test));
		
		System.out.println("\nRouteTree data structure in RapidSmith:");
		//RapidSmithDebug.printRouteTree(test);

		String checkpointOut = checkpointIn.substring(0, checkpointIn.length() - 5) + "_astarred" + ".tcp";
		design = vcp.getDesign();
		Device device = vcp.getDevice();
		CellLibrary libCells = vcp.getLibCells();
		VivadoInterface.writeTCP(checkpointOut, design, device, libCells, ImplementationMode.REGULAR);

		//System.out.println("\nRouteTree data structure in DOT file format");
		//System.out.println("\n" + DotFilePrinter.getRouteTreeDotString(net));
	}
}
