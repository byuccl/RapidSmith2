package edu.byu.ece.rapidSmith.examples.aStarRouter;
import java.util.*;
import java.util.stream.Stream;

import edu.byu.ece.partialreconfig.router.NetInfo;
import edu.byu.ece.partialreconfig.router.RouteInfo;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.PartitionPin;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.*;

/**
 * Implements a very simple A* routing algorithm capable of routing a single {@link CellNet}
 * in a design. This code demonstrates how a physical route can be created using
 * RapidSmith data structures if you choose to use the {@link RouteTree} class.
 */
public class AStarRouter {
	
	private final Comparator<RouteTreeWithCost> routeTreeComparator;
	private PriorityQueue<RouteTreeWithCost> priorityQueue;
	private Map<RouteTree, Set<Wire>> usedConnectionMap;
	private Tile targetTile;
	private Tile startTile;
	private Map<Wire, PFCost> wireUsage;
	private Map<Wire, CellPin> wirePartitionPinMap;

	/**
	 * Constructor. Initializes a new A* router object
	 * @param wireUsage
	 */
	public AStarRouter(Map<Wire, PFCost> wireUsage) {
		
		// Cost function for comparing RouteTree objects
		// used whenever something is added to the priority queue
		routeTreeComparator = (one, two) -> {
				// cost = route tree cost (# of wires traversed) + distance to the target + distance from the source
				Integer costOne = one.getCost() + manhattanDistance(one, targetTile); //+ manhattanDistance(one, startTile);
				Integer costTwo = two.getCost() + manhattanDistance(two, targetTile); //+ manhattanDistance(two, startTile);
				
				return costOne.compareTo(costTwo);
		};

		this.wireUsage = wireUsage;

		usedConnectionMap = new HashMap<>();
		wirePartitionPinMap = new HashMap<>();
	}
	
	/**
	 * Routes the specified {@link CellNet} using an A* routing algorithm.
	 * 
	 * @return The routed net in a {@link RouteTree} data structure
	 */
	public RouteTreeWithCost routeNet(RouteInfo routeInfo) {

		// Initialize the route
		RouteTreeWithCost start = routeInfo.getStartTree();
		startTile = routeInfo.getStartWire().getTile();

		Set<RouteTree> terminals = new HashSet<>();
		
		// Find the pins that need to be routed for the net
		Collection<Wire> sinksToRoute = routeInfo.getSinkWires();

		// TODO: Pass a map from sink wires to cell pins...

		assert !sinksToRoute.isEmpty() : "CellNet object should have at least one sink Site Pin in order to route it";
			
		// Iterate over each sink SitePin in the net, and find a valid route to it. 
		for (Wire targetWire : sinksToRoute) {
			
			// initialize the target wire, and priority queue
			//SitePin sink = sinksToRoute.next();
			//Wire targetWire = getTargetSinkWire(sink);

			System.out.println("Target wire is " + targetWire.getFullName());

			if (targetWire.getFullName().equals("INT_L_X26Y94/IMUX_L39"))
				System.out.println("DEBUG");


			targetTile = targetWire.getTile();
			resortPriorityQueue(start);



			boolean routeFound = false;
			// This loop actually builds the routing data structure
			while (!routeFound) {

				// Grab the lowest cost route from the queue

				if (priorityQueue.size() == 0) {
					System.out.println("WARNING. RAN OUT OF STUFF");
				}

				RouteTreeWithCost current = priorityQueue.poll();



				// Get a set of sink wires from the current RouteTree that already exist in the queue
				// we don't need to add them again
				//Set<Wire> existingBranches = usedConnectionMap.getOrDefault(current, new HashSet<Wire>());
				Set<Wire> existingBranches = new HashSet<Wire>();

				// Search all connections for the wire of the current RouteTree
				Wire currWire = current.getWire();
				Collection<Connection> currConnections;
				if (currWire == null) {
					System.out.println("WHY NULL");
					currConnections = current.getWire().getWireConnections();
				}
				else
					currConnections = current.getWire().getWireConnections();
				for (Connection connection : currConnections) {

					//if (!connection.isPip())
					//	continue;

					if (connection.isRouteThrough()) {
						// skip site routethroughs
						//if (!existingBranches.contains(connection.getSinkWire()))
						//{
						//	System.out.println("Skip " + connection.toString());
						//	existingBranches.add(connection.getSinkWire());
						//}

						continue;
					}

					Wire sinkWire = connection.getSinkWire();

					// Solution has been found
					if (sinkWire.equals(targetWire)) {
						PFCost pfCost = wireUsage.computeIfAbsent(targetWire, k -> new PFCost());
						RouteTreeWithCost sinkTree = current.connect(connection);

						// If this sink is a partition pin, don't "finalize" the route
						if (!wirePartitionPinMap.containsKey(sinkWire)) {
							sinkTree = finalizeRoute(sinkTree);
						}

						terminals.add(sinkTree);
						routeFound = true;
						break;
					}

					// Only create and add a new RouteTree object if it doesn't already exist in the queue
					if (!existingBranches.contains(sinkWire)) {
						RouteTreeWithCost sinkTree = current.connect(connection);
						PFCost pfCost = wireUsage.computeIfAbsent(sinkWire, k -> new PFCost());
						//pfCost.setOccupancy(pfCost.getOccupancy() + 1);
						// only increment the historical for shared wires
					//	if (pfCost.getOccupancy() > 1)
						//	pfCost.setHistory(pfCost.getHistory() + 1);
						//int sinkWireCost = pfCost.getPFCost();

						// sinkTree.setCost(current.getCost() + 1);
						sinkTree.setCost(current.getCost() + 1);
						priorityQueue.add(sinkTree);
						existingBranches.add(sinkWire);
					}
				}

				
				usedConnectionMap.put(current, existingBranches);
			}
			
			// prune RouteTree objects not used in the final solution. This is not very efficient...
			// TODO: Just prune after all routes are created.
			start.prune(terminals);

			//System.out.println("I routed a sink " + sink.getName());
		}
		
		return start;
	}

	/**
	 * Creates an initial {@link RouteTree} object for the specified {@link CellNet}.
	 * This is the beginning of the physical route. 
	 */
	private RouteTreeWithCost initializeRoute(CellNet net) {
		Wire startWire;

		// If the source pin is a partition pin
		if (net.getSourcePin().isPartitionPin())
			startWire = net.getSourcePin().getWire();
		else
			startWire = net.getSourceSitePin().getExternalWire();

		PFCost pfCost = wireUsage.computeIfAbsent(startWire, k -> new PFCost());
		//pfCost.setOccupancy(pfCost.getOccupancy() + 1);
		//wireUsage.computeIfAbsent(startWire, k -> new PFCost());

		System.out.println("route from: " + startWire.getFullName());

		RouteTreeWithCost start = new RouteTreeWithCost(startWire);
		startTile = startWire.getTile();
		usedConnectionMap.clear();
		return start;
	}
	
	/**
	 * Update the costs of the RouteTrees in the priority queue for the new target wire
	 */
	private void resortPriorityQueue (RouteTreeWithCost start) {
		
		// if the queue has not been created, create it, otherwise create a new queue double the size

		// Should it be possible for the size to be 0??
		priorityQueue = (priorityQueue == null || priorityQueue.size() == 0) ?
			new PriorityQueue<>(routeTreeComparator) :
			new PriorityQueue<>(priorityQueue.size()*2, routeTreeComparator);
		
		// add the RouteTree objects to the new queue so costs will be updated
		Iterable<RouteTreeWithCost> typed = start.typedIterator();
		typed.forEach(rt -> priorityQueue.add(rt));
	}
	
	/**
	 * Returns a {@link Stream} of {@link SitePin} objects that need to 
	 * be routed for the specified net.
	 * 
	 * @param net {@link CellNet} to route
	 */
	private Collection<Wire> getSinksToRoute(CellNet net) {
		// TODO: Optimize this code, remove repetition
		Collection<Wire> sinks = new ArrayList<>();

		for (CellPin sinkPin : net.getSinkPartitionPins()) {
			sinks.add(sinkPin.getWire());
			wirePartitionPinMap.put(sinkPin.getWire(), sinkPin);
		}

		for (SitePin sinkSitePin : net.getSinkSitePins()) {
			sinks.add(getTargetSinkWire(sinkSitePin));
		}

		return sinks;
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
	private int manhattanDistance(RouteTree tree, Tile compareTile) {
		Tile currentTile = tree.getWire().getTile();
		//return currentTile.getManhattanDistance(compareTile);
		return currentTile.getIndexManhattanDistance(compareTile);
	}
	
	/**
	 * Completes the route for a the current {@link SitePin}. It does this by following connections 
	 * from the target wire until it reaches a {@link SitePin}, adding {@link RouteTree} 
	 * objects along the way.
	 * 
	 * @param route {@link RouteTree} representing the target wire that has been routed to
	 * @return the final {@link RouteTree}, which connects to a {@link SitePin}
	 */
	private RouteTreeWithCost finalizeRoute(RouteTreeWithCost route) {
		
		while (route.getWire().getConnectedPin() == null) {
			//TODO: FIXME
			//assert (route.getWire().getWireConnections().size() == 1);
			route = route.connect(route.getWire().getWireConnections().iterator().next());
		}
		return route;
	}
	
	/**
	 * Returns the sink wire that needs to be routed to in order to reach the specified {@link SitePin}.
	 * Typically, this is a switchbox wire which is easier to route to than a site pin wire. 
	 * The target wire is found be traversing reverse wire connections until a backwards branch is found. 
	 *  
	 * @param pin input {@link SitePin} of a {@link edu.byu.ece.rapidSmith.device.Site}
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

	public static class RouteTreeWithCost extends RouteTree {
		private int cost = 0;

		public RouteTreeWithCost(Wire wire) {
			super(wire);
		}

		@Override
		protected RouteTree newInstance(Wire wire) {
			return new RouteTreeWithCost(wire);
		}

		public int getCost() {
			return cost;
		}

		public void setCost(int cost) {
			this.cost = cost;
		}
	}
}
