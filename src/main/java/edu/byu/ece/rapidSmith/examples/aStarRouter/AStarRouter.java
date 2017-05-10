package edu.byu.ece.rapidSmith.examples.aStarRouter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Stream;

import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.Connection;
import edu.byu.ece.rapidSmith.device.SitePin;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.Wire;

/**
 * Implements a very simple A* routing algorithm capable of routing a single {@link CellNet}
 * in a design. This code demonstrates how a physical route can be created using
 * RapidSmith data structures if you choose to use the {@link RouteTree} class. This class 
 * requires the extended device information to be loaded with the function call 
 * {@link ExtendedDeviceInfo.loadExtendedInfo(Device) }.
 */
public class AStarRouter {
	
	private final Comparator<RouteTree> routeTreeComparator;
	private PriorityQueue<RouteTree> priorityQueue;
	private Map<RouteTree, Set<Wire>> usedConnectionMap;
	private Tile targetTile;
	private Tile startTile;
	 
	/**
	 * Constructor. Initializes a new A* router object
	 */
	public AStarRouter() {		
		
		// Cost function for comparing RouteTree objects
		routeTreeComparator = new Comparator<RouteTree>() {
			@Override
			public int compare(RouteTree one, RouteTree two) {
				// cost = route tree cost (# of wires traversed) + distance to the target + distance from the source
				Integer costOne = one.getCost() + manhattenDistance(one, targetTile) + manhattenDistance(one, startTile);
				Integer costTwo = two.getCost() + manhattenDistance(two, targetTile) + manhattenDistance(two, startTile);
				
				return costOne.compareTo(costTwo);
			}
		};
		
		usedConnectionMap = new HashMap<>();
	}
	
	/**
	 * Routes the specified {@link CellNet} using an A* routing algorithm.
	 * 
	 * @param net {@link CellNet} to route
	 * @return The routed net in a {@link RouteTree} data structure
	 */
	public RouteTree routeNet(CellNet net) {
		
		// Initialize the route
		RouteTree start = initializeRoute(net);
		Set<RouteTree> terminals = new HashSet<>();
		
		// Find the pins that need to be routed for the net
		Iterator<SitePin> sinksToRoute = getSinksToRoute(net).iterator();
		assert sinksToRoute.hasNext() : "CellNet object should have at least one sink Site Pin in order to route it"; 
			
		// Iterate over each sink SitePin in the net, and find a valid route to it. 
		while(sinksToRoute.hasNext()) {
			
			// initialize the target wire, and priority queue
			SitePin sink = sinksToRoute.next(); 
			Wire targetWire = getTargetSinkWire(sink);
			targetTile = targetWire.getTile();
			resortPriorityQueue(start);
			
			boolean routeFound = false;
			// This loop actually builds the routing data structure
			while (!routeFound) {
				
				// Grab the lowest cost route from the queue
				RouteTree current = priorityQueue.poll();
				
				// Get a set of sink wires from the current RouteTree that already exist in the queue
				// we don't need to add them again
				Set<Wire> existingBranches = usedConnectionMap.getOrDefault(current, new HashSet<Wire>());
				
				// Search all connections for the wire of the current RouteTree
				for (Connection connection : current.getWire().getWireConnections()) {
					
					Wire sinkWire = connection.getSinkWire();
					
					// Solution has been found
					if (sinkWire.equals(targetWire)) {
						RouteTree sinkTree = current.addConnection(connection); 
						sinkTree = finializeRoute(sinkTree);
						terminals.add(sinkTree);
						routeFound = true;
						break;
					}
					
					// Only create and add a new RouteTree object if it doesn't already exist in the queue
					if (!existingBranches.contains(sinkWire)) {
						RouteTree sinkTree = current.addConnection(connection);
						sinkTree.setCost(current.getCost() + 1);
						priorityQueue.add(sinkTree);
						existingBranches.add(sinkWire);
					} 
				}
				
				usedConnectionMap.put(current, existingBranches);
			}
			
			// prune RouteTree objects not used in the final solution. This is not very efficient... 
			start.prune(terminals);
		}
		
		return start;
	}

	/**
	 * Creates an initial {@link RouteTree} object for the specified {@link CellNet}.
	 * This is the beginning of the physical route. 
	 */
	private RouteTree initializeRoute(CellNet net) {
		Wire startWire = net.getSourceSitePin().getExternalWire();
		RouteTree start = new RouteTree(startWire);
		startTile = startWire.getTile();
		usedConnectionMap.clear();
		return start;
	}
	
	/**
	 * Update the costs of the RouteTrees in the priority queue for the new target wire
	 */
	private void resortPriorityQueue (RouteTree start) {
		
		// if the queue has not been created, create it, otherwise create a new queue double the size
		priorityQueue = (priorityQueue == null) ? 
				new PriorityQueue<RouteTree>(routeTreeComparator) :
				new PriorityQueue<RouteTree>(priorityQueue.size()*2, routeTreeComparator);
		
		// add the RouteTree objects to the new queue so costs will be updated
		start.iterator().forEachRemaining(rt -> priorityQueue.add(rt));
	}
	
	/**
	 * Returns a {@link Stream} of {@link SitePin} objects that need to 
	 * be routed for the specified net.
	 * 
	 * @param net {@link CellNet} to route
	 */
	private Stream<SitePin> getSinksToRoute(CellNet net) {
		return net.getSitePins().stream().filter(SitePin::isInput);
	}
	
	/**
	 * Calculates the Manhattan distance between the specified {@link RouteTree} and {@link Tile} objects. 
	 * The Tile of the wire within {@code tree} is used for the comparison. The Manhattan distance from 
	 * a {@link RouteTree} to the final destination tile is used for "H" in the A* Router.
	 * 
	 * @param tree {@link RouteTree}
	 * @param compareTile {@link Tile} 
	 * @return The Manhattan distance between {@code tree} and {@code compareTile}
	 */
	private int manhattenDistance(RouteTree tree, Tile compareTile) {
		Tile currentTile = tree.getWire().getTile();
		return Math.abs(currentTile.getColumn() - compareTile.getColumn() ) + Math.abs(currentTile.getRow() - compareTile.getRow()); 
	}
	
	/**
	 * Completes the route for a the current {@link SitePin}. It does this by following connections 
	 * from the target wire until it reaches a {@link SitePin}, adding {@link RouteTree} 
	 * objects along the way.
	 * 
	 * @param route {@link RouteTree} representing the target wire that has been routed to
	 * @return the final {@link RouteTree}, which connects to a {@link SitePin}
	 */
	private RouteTree finializeRoute(RouteTree route) {
		
		while (route.getWire().getConnectedPin() == null) {
			assert (route.getWire().getWireConnections().size() == 1);
			route = route.addConnection(route.getWire().getWireConnections().iterator().next());
		}
		return route;
	}
	
	/**
	 * Returns the sink wire that needs to be routed to in order to reach the specified {@link SitePin}.
	 * Typically, this is a switchbox wire which is easier to route to than a site pin wire. 
	 * The target wire is found be traversing reverse wire connections until a backwards branch is found. 
	 *  
	 * @param pin input {@link SitePin} of a {@link Site}
	 */
	private Wire getTargetSinkWire(SitePin pin) {
		Wire sinkWire = pin.getExternalWire();
		assert (pin.isInput()) : "Can only find sink wires for input site pins..";
		assert (sinkWire.getReverseWireConnections() != null) : "Reverse wire connections not loaded!";
		
		// search the reverse wire connections until we reach a wire that has more than one connection backwards.
		while (sinkWire.getReverseWireConnections().size() == 1) {
			
			Wire previous = sinkWire.getReverseWireConnections().iterator().next().getSinkWire();
			
			if (previous.getWireConnections().size() > 1) {
				break;
			}
			sinkWire = previous;
		}
		
		return sinkWire;
	}
}
