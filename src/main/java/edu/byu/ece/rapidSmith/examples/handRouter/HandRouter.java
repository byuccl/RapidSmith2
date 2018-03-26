package edu.byu.ece.rapidSmith.examples.handRouter;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.Connection;

/**
 * Class used to hand route a net in the design. The source code demonstrates how to search
 * through the wires of a device and build up a RouteTree data structure for a net.
 * The program can also be used to search through the device for debugging purposes. 
 */
public class HandRouter {

	private int restoreCount;
	private boolean haveSaved;
	private Scanner in;

	/**
	 * Constructor
	 */
	HandRouter() {
		this.haveSaved = false;
		this.restoreCount = 0;
	}

	/**
	 * Runs the HandRouter starting at the specified {@link Wire} object.
	 *
	 * @param wire Starting {@link Wire} in the route
	 * @return A {@link RouteTree} representing the chosen hand route
	 */
	public RouteTree route( Wire wire ) {

		Objects.requireNonNull(wire);

		in = new Scanner(System.in);
		RouteTree start = new RouteTree(wire);
		handRoute(start);
		in.close();

		return start;
	}

	/**
	 * Internal routing function
	 */
	private boolean handRoute(RouteTree route) {

		boolean done = false;

		while(!done) {

			printDownhillConnections(route.getWire());
			System.out.print("  >>>");

			if (in.hasNextInt()) {
				route = followConnection(route, in.nextInt());
				continue;
			}

			switch(in.nextLine()) {
				case "back":
				case "b": route = goBackwards(route);
					break;
				case "done" :
				case "d" : done = true;
					break;
				case "save":
				case "s": saveCheckpoint();
					break;
				case "restore":
				case "r": route = restoreCheckpoint(route);
					break;
				case "usage":
				case "u": printCommands();
					break;
				default:
					System.out.println("Invalid Option! Enter back(b), done(d), save(s), restore(r), usage(u), or 1-" + route.getWire().getWireConnections().size());
					break;
			}
		}

		return true;
	}

	/**
	 * Returns to the previous {@link RouteTree} connection. If the current RouteTree object is the
	 * source of the route, nothing happens. 
	 * 
	 * @param current the current {@code RouteTree} node.
	 * @return {@code current}
	 */
	private RouteTree goBackwards(RouteTree current) {

		// only go backwards if the RouteTree has a source
		if (current.isSourced()) {
			if (haveSaved) {
				restoreCount--;
			}
			
			RouteTree source = current.getParent();
			source.disconnect(current);
			return source; 
		} 
		
		System.out.println("Can't go back any further. At the source wire");
		return current;
	}

	/**
	 * Saves the current route as a checkpoint
	 */
	private void saveCheckpoint() {
		restoreCount = 0;
		haveSaved = true;
		System.out.println("Route Saved. Type \"restore\" to return to this point");
	}

	/**
	 * Restores a users saved checkpoint
	 * @param current the current route tree node
	 * @return {@code current}
	 */
	private RouteTree restoreCheckpoint(RouteTree current) {

		System.out.println("Restoring Saved Route.");

		while (restoreCount > 0) {
			RouteTree parent = current.getParent();
			parent.disconnect(current);
			current = parent;
			restoreCount--;
		}

		return current;
	}

	/**
	 * Adds the specified connection to the current {@link RouteTree} object,
	 * and returns the new {@link RouteTree} object after adding the connection.
	 *
	 * @param current Latest part of the hand route
	 * @param connectionNum The connection number to follow
	 * @return The {@link RouteTree} created after following the connection
	 */
	private RouteTree followConnection(RouteTree current, int connectionNum) {

		List<Connection> connections = (List<Connection>)current.getWire().getWireConnections();

		if (connectionNum < 1 || connectionNum > connections.size() ) {
			return current;
		}

		// else, follow the chosen connection, and return the new route tree
		if (haveSaved) {
			restoreCount++;
		}

		return current.connect(connections.get(connectionNum - 1));
	}

	/**
	 * Prints all available connections to the user in the form
	 * {@code 1.) CLBLL_L_X16Y152/CLBLL_LOGIC_OUTS18 (PIP) }, where "PIP" is
	 * only displayed if the wire connection is a PIP connection
	 * @param wire {@link Wire} object to print the connections for
	 */
	private void printDownhillConnections ( Wire wire ) {
		System.out.println("All connections for wire: " + wire.getFullName());
		int i = 1;

		for (Connection conn : wire.getWireConnections()) {
			Wire sinkWire = conn.getSinkWire();
			System.out.println(String.format("  %d.) %s %s", i, sinkWire.getFullName(), conn.isPip() ?"(PIP)" : ""));
			i++;
		}
	}

	/**
	 * Prints the available commands to the user with appropriate descriptions. 
	 */
	private void printCommands() {
		System.out.println("Hand Router Commands: \n"
				+ " - back(b): Move backwards in the RouteTree from the last taken connection. This will remove the connection from the route.\n"
				+ " - done(d): Finalize the route and exit the HandRouter.\n"
				+ " - save(s): Save the current route as a checkpoint. Type \"restore\" to return to this checkpoint. Nested checkpoints are not supported.\n"
				+ " - restore(r): Return to a saved checkpoint.\n");
	}
}
