package edu.byu.ece.rapidSmith.examples.handRouter;
import java.io.IOException;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.PIP;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileWire;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import edu.byu.ece.rapidSmith.util.RapidSmithDebug;

/**
 * Runs the {@link HandRouter} 
 *
 */
public class HandRouterExample {

	public static final String PART_NAME = "xc7a100tcsg324";
	
	public static void main(String[] args) throws IOException {
		//load the device file in to RS2
		System.out.println("Loading device " + PART_NAME + " into RapidSmith2...");
		Device device = RSEnvironment.defaultEnv().getDevice(PART_NAME);

		// Load the wire enumerator
		WireEnumerator we = device.getWireEnumerator();
				
		// Create a new hand router object
		HandRouter hr = new HandRouter();
			
		// Create a new TileWire object to be the starting wire of the route
		Wire startWire = new TileWire(device.getTile("CLBLL_L_X16Y152"), we.getWireEnum("CLBLL_L_C"));
		
		Wire sinkWire0 = new TileWire(device.getTile("CLBLL_L_X16Y152"), we.getWireEnum("CLBLL_LOGIC_OUTS10"));
		Wire sinkWire1 = new TileWire(device.getTile("CLBLL_L_X16Y152"), we.getWireEnum("CLBLL_L_CMUX"));

		// CLBLL_L_X16Y152/CLBLL_LOGIC_OUTS10 is used
		// CLBLL_L_X16Y152/CLBLL_L_CMUX is unavailable
		PIP usedPip = new PIP(startWire, sinkWire0);
		Tile tile = device.getTile("CLBLL_L_X16Y152");
		if (tile.hasPIP(usedPip))
		{
			System.out.println("Has PIP!");
			// Now mark the PIP as used.
			tile.setUsedPIP(usedPip);
			
		}
		
		PIP unavailablePip = new PIP(startWire, sinkWire1);
		if (tile.hasPIP(unavailablePip))
		{
			System.out.println("Has PIP!");
			// Now mark the PIP as unavailable.
			tile.setUnavailablePIP(unavailablePip);
		}

		
//		Integer startWire = we.getWireEnum(tokens.get(2));
//		device.getTile("CLBLL_L_X16Y152").removeConnection(src, dest);
		
		// Run the hand router, which returns a RapidSmith RouteTree object
		RouteTree route = hr.route(startWire);
		
		// Print the RouteTree to the console
		System.out.println("Printing chosen route:");
		RapidSmithDebug.printRouteTree(route);
	}
}
