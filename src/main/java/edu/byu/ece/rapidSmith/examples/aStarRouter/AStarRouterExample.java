package edu.byu.ece.rapidSmith.examples.aStarRouter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.rapidSmith.util.DotFilePrinter;
import edu.byu.ece.rapidSmith.util.RapidSmithDebug;

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
public class AStarRouterExample {

	public static void main(String[] args) throws IOException {
		// load the device and design
		String checkpoint = RSEnvironment.defaultEnv().getEnvironmentPath()
				.resolve("exampleVivadoDesigns")
				.resolve("addPlaced.rscp").toString();
		
		System.out.println("Loading Device and Design...");
		VivadoCheckpoint vcp = VivadoInterface.loadRSCP(checkpoint);
		CellDesign design = vcp.getDesign();

		// Routing net
		System.out.println("Routing Net...");
		Map<Wire, PFCost> wireUsage = new HashMap<>();

		AStarRouter router = new AStarRouter(wireUsage);
		CellNet net = design.getNet("b_IBUF");
		//RouteTree test = router.routeNet(net);
		//net.addIntersiteRouteTree(test);
		
		// Displaying results
		System.out.println("\nCommand to highlight chosen wires in Vivado: ");
		//System.out.println(RapidSmithDebug.createHighlightWiresTclCommand(test));
		
		System.out.println("\nRouteTree data structure in RapidSmith:");
		//RapidSmithDebug.printRouteTree(test);
		
		System.out.println("\nRouteTree data structure in DOT file format");
		System.out.println("\n" + DotFilePrinter.getRouteTreeDotString(net));
	}
}
