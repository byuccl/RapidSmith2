package edu.byu.ece.rapidSmith.examples.aStarRouter;
import java.io.IOException;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.creation.ExtendedDeviceInfo;
import edu.byu.ece.rapidSmith.interfaces.vivado.TincrCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.rapidSmith.util.RapidSmithDebug;

/**
 * Demonstrates the sample {@link AStarRouter} on a single net in a CORDIC design. 
 * To see the results of the router, open up the equivalent cordic.dcp file in Vivado,
 * and run the hightlight TCL command that is produced after running this program. This
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
				.resolve("src")
				.resolve("test")
				.resolve("resources")
				.resolve("ImportTests")
				.resolve("TCP")
				.resolve("cordic.tcp").toString();
		
		System.out.println("Loading Device and Design...");
		TincrCheckpoint tcp = VivadoInterface.loadTCP(checkpoint);
		CellDesign design = tcp.getDesign();
		Device device = tcp.getDevice();
		
		// loading reverse wire connections
		ExtendedDeviceInfo.loadExtendedInfo(device);
		
		// Routing net
		System.out.println("Routing Net...");
		AStarRouter router = new AStarRouter();
		RouteTree test = router.routeNet(design.getNet("u2/gen_pipe[8].Pipe/Zo_reg_n_0_[9]"));
		
		// Displaying results
		System.out.println("\nCommand to highlight chosen wires in Vivado: ");
		System.out.println(RapidSmithDebug.createHighlightWiresTclCommand(test));
		
		System.out.println("\nRouteTree data structure in RapidSmith:");
		RapidSmithDebug.printRouteTree(test);
	}
}
