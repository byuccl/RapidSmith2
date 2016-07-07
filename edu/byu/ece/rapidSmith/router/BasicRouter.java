/*
 * Copyright (c) 2010 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.router;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;

import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.design.Net;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.design.Pin;
import edu.byu.ece.rapidSmith.device.WireConnection;
import edu.byu.ece.rapidSmith.device.WireDirection;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * This router is a brute force maze router.  It does not have any 
 * mechanism to address routing conflicts.  It is simply provided as
 * an illustration of how to build a router in RapidSmith and use the given
 * framework.  It WILL fail to route some nets on several designs.
 * @author Chris Lavin
 */
public class BasicRouter extends AbstractRouter{

	/**
	 * Constructor to initialize router
	 */
	public BasicRouter(){
		super();
		MessageGenerator.printHeader(this.getClass().getCanonicalName());
	}
	
	/**
	 * Cost function, used to set each node's cost to be prioritized by the queue 
	 * @param node The node to calculate and set its cost based on currSink.
	 * @param isRouteThrough True if the node is a route through
	 */
	public void setCost(Node node, boolean isRouteThrough){
		// Calculate Manhattan distance between node and sink
		int x = currSink.getTile().getTileXCoordinate() - node.tile.getTileXCoordinate();
		int y = currSink.getTile().getTileYCoordinate() - node.tile.getTileYCoordinate();
		
		// ABS
		if(x < 0) x = -x;
		if(y < 0) y = -y;

		// Favor clock wires when routing the clock tree
		if(isCurrSinkAClkWire && we.getWireDirection(node.wire).equals(WireDirection.CLK) && !isRouteThrough){
			node.cost = ((x + y + node.level) * 2) - 1000 + node.history;
		}
		else{
			node.cost = (x + y + node.level) * 2 + node.history;
		}
	}
	
	/**
	 * Prepares the class variables for the route() method. Sets everything up
	 * for each connection to be made. This method is called for each connection
	 * in a net by routeNet(). It calls route() once the variables are ready
	 * for routing.
	 * 
	 * @param sources The candidate sources to attempt to route from.
	 */
	protected void routeConnection(ArrayList<Node> sources){
		// Reset Variable for a new route
		pipList = new ArrayList<PIP>();
		visitedNodes = new HashSet<Node>();
		queue.clear();
		nodesProcessed = 0;
		successfulRoute = false;
		// Setup the source nodes for starting the routing process
		for(Node src : sources){
			// Add the source nodes to the queue
			if(src.getConnections() != null){
				// Set the cost of the source
				setCost(src, false);
				this.queue.add(src);
			}
		}
		// Do the actual routing
		route();
		totalNodesProcessed += nodesProcessed;
	}
	
	/**
	 * The heart of the router, it does the actual routing by consuming nodes on
	 * the priority queue and determining how to proceed to the sink. It is
	 * called by routeConnection().
	 */
	protected void route(){	
		// Iterate through all of the nodes in the queue, adding potential candidate nodes 
		// as we go along. We are finished when we find the sink node.
		while(!queue.isEmpty()){
			if(nodesProcessed > 1000000){
				// If we haven't found a route by now, we probably never will
				return;
			}
			Node currNode = queue.remove();
			nodesProcessed++;
			
			for(WireConnection w : currNode.getConnections()){
				if(w.getWire() == this.currSink.wire && w.getTile(currNode.tile).equals(currSink.tile)){
					
					// We've found the sink, lets retrace our steps
					Node currPathNode = new Node(w.getTile(currNode.tile), w.getWire(), currNode, currNode.level+1);

					// Add this connection as a PIP, and follow it back to the source
					while(currPathNode.parent != null){
						
						for(WireConnection w1 : currPathNode.parent.tile.getWireConnections(currPathNode.parent.wire)){
							if(w1.getWire() == currPathNode.wire){
								if(w1.isPIP() && currPathNode.parent.tile.equals(currPathNode.tile)){
									pipList.add(new PIP(currPathNode.tile, currPathNode.parent.wire, currPathNode.wire));
									break;
								}
							}
						}
						// Update the current node to the parent
						// this way we can traverse backwards to the source
						currPathNode = currPathNode.parent;
					}
					// We are now done with the routing of this connection
					successfulRoute = true;
					return;
				} 
				else{						
					// This is not the sink, but is this wire one we should look at in the future?
					Node tmp = new Node(w.getTile(currNode.tile), w.getWire(), currNode, currNode.level+1);
					
					// Check if this node has already been visited, if so don't add it
					if(!(visitedNodes.contains(tmp))){
						if(tmp.getConnections() != null && !usedNodes.contains(tmp)){
							// Make sure we haven't used this node already
							if(tmp.getConnections() != null){
								// This looks like a possible candidate for our next node, we'll add it
								setCost(tmp, dev.isRouteThrough(w));
								visitedNodes.add(tmp);
								queue.add(tmp);
								if(currSources.contains(tmp)){
									tmp.parent = null;
								}
							}
						}
					} 
				}
			}
		}
	}
	
	/**
	 * This method routes all the connections within a net.  
	 * @param i The number of the net (in sequence from the beginning)
	 */
	public void routeNet(int i){
		Pin currSource = currNet.getSource();
		ArrayList<Node> sources = new ArrayList<Node>();
		currSources = new HashSet<Node>();
		boolean firstConnection = true;
		
		// Route each pin by itself
		for(Pin currSinkPin : currNet.getPins()){
			// Ignore the source pin
			if (currSinkPin.isOutPin()) continue; 

			// This will print out until the Virtex 5 patch is complete
			if(dev.getPrimitiveExternalPin(currSinkPin) == null){
				MessageGenerator.printHeader("Pin Missing from V5 Patch: " + currNet.getName() + " " + currSinkPin.getName() 
						+ " " +currSinkPin.getInstance().getTile() + " " + currSinkPin.getInstance().getType());
				continue;
			}
			
			// Populate the current sink node
			currSink.tile = currSinkPin.getInstance().getTile();
			currSink.wire = dev.getPrimitiveExternalPin(currSinkPin).getWireEnum();

			// Is this source from a buffer (likely a clock net)?
			boolean currNetOutputFromBUF = currSource.getInstance().getType().toString().contains("BUF");
			
			isCurrSinkAClkWire = (we.getWireDirection(currSink.wire).equals(WireDirection.CLK) ||
								  currSinkPin.getName().contains("CLK") ||
								  currSinkPin.getName().equals("C")) &&
								 (currNetOutputFromBUF || 
								  currSinkPin.getInstance().getType().toString().contains("BUF")
								 );

			// Add additional sources if this is not the first sink of the net being routed
			if(firstConnection){
				// Error checking
				if(dev.getPrimitiveExternalPin(currSource) == null){
					MessageGenerator.briefErrorAndExit("ERROR: Could not find valid external source pin name: " +
							currSource + " " + currSource.getInstance().getType());
				}
				
				// just add the original source
				Node n = new Node(currSource.getInstance().getTile(), 
						dev.getPrimitiveExternalPin(currSource).getWireEnum(), null, 0);
				sources.add(n);
				currSources.add(n);
			}
			else{
				// Add starting point sources taken from previous routings to begin the route 
				sources = getSourcesFromPIPs(pipList);
			}

			// Route the current sink node
			totalConnections++;
			routeConnection(sources);

			// Check if it was a successful routing
			if(successfulRoute){
				// Add these PIPs to the rest used in the net
				netPIPs.addAll(pipList);
			} 
			else{
				failedConnections++;
				MessageGenerator.briefError("\tFAILED TO ROUTE: net: " + currNet.getName() + " inpin: " + currSinkPin.getName() +
                   " (" + we.getWireName(currSink.wire) + ") on instance: " + currSinkPin.getInstanceName());
			}
			firstConnection = false;
		}
	}
	
	/**
	 * This the central method for routing the design in this class.  This prepares
	 * the nets for routing.
	 * @return The final routed design.
	 */
	public Design routeDesign(){
		netList = new ArrayList<Net>();
		netList.addAll(design.getNets());
		
		// Deal with static nets (vcc/gnd)
		StaticSourceHandler ssHandler = new StaticSourceHandler(this);
		ssHandler.separateStaticSourceNets();
		
		// Start Routing
		for (int i = 0; i < netList.size(); i++){
			currNet = netList.get(i);
			
			// We need to ignore some empty/informational nets
			if ((currNet.hasAttributes() && currNet.getModuleTemplateNet() == null) || currNet.getPIPs().size() > 0) continue;
		
			if(currNet.getSource() == null){
				MessageGenerator.briefError("ERROR: " + currNet.getName() + " does not have a source pins associated with it.");
				continue;
			}
			
			// release some reservedNodes
			ArrayList<Node> rNodes = reservedNodes.get(currNet);
			
			if(rNodes != null){
				usedNodes.removeAll(rNodes);
			}
			
			// netPIPs are the pips that belong to a particular net, however, 
			// because GND/VCC nets can use pips of other nets, we need a usedPIPs
			// variable to keep everything straight.
			netPIPs = new ArrayList<PIP>();
			routeNet(i);
			
			// Mark these used PIPs as used in the data structures
			for (PIP pip : netPIPs){
				setWireAsUsed(pip.getTile(), pip.getStartWire(), currNet);
				setWireAsUsed(pip.getTile(), pip.getEndWire(), currNet);
				markIntermediateNodesAsUsed(pip, currNet);
			}
			// Let's add these PIPs to the actual net, to be included in the design
			currNet.setPIPs(netPIPs);
		}
		design.setNets(netList);
		return design;
	}	
	
	protected static void printTimeHelper(String timedOperation, long start) {
		System.out.printf("%s %8.3fs\n", timedOperation,
				(System.nanoTime() - start) / 1000000000.0);
	}
	
	public static void main(String[] args){
		long[] runtimes = new long[4];
		String nl = System.getProperty("line.separator");
		runtimes[0] = runtimes[1] = System.nanoTime();
		if (args.length != 2){
			System.out.println("USAGE: Router <input.xdl> <output.xdl>");
			System.exit(0);
		}
		
		// Initialize router and load design and device
		BasicRouter router = new BasicRouter();
		router.design = new Design();
		router.design.loadXDLFile(Paths.get(args[0]));
		router.dev = router.design.getDevice();

		runtimes[1] = System.nanoTime() - runtimes[1];
		runtimes[2] = System.nanoTime();
		
		// Route the design
		router.routeDesign();
		
		runtimes[2] = System.nanoTime() - runtimes[2];
		runtimes[3] = System.nanoTime();
		
		// Save routed design to XDL file
		router.design.saveXDLFile(Paths.get(args[1]), true);
		
		runtimes[3] = System.nanoTime() - runtimes[3];
		runtimes[0] = System.nanoTime() - runtimes[0];
		
		// Print out runtime summary
		System.out.println();
		System.out.println("----------------- SUMMARY --------------------");
		System.out.println("         Total Nodes Processed : " + router.totalNodesProcessed);
		System.out.println("             Total Connections : " + router.totalConnections);
		System.out.println("      Total Failed Connections : " + router.failedConnections);
		System.out.println("----------------------------------------------");
		System.out.printf("    Loading Design/Device Time : %8.3fs %s", runtimes[1]/1000000000.0, nl);
		System.out.printf("                  Routing Time : %8.3fs %s", runtimes[2]/1000000000.0, nl);
		System.out.printf("            Saving Design Time : %8.3fs %s", runtimes[3]/1000000000.0, nl);
	    System.out.println("----------------------------------------------");
		System.out.printf("                 Total Runtime : %8.3fs %s", runtimes[0]/1000000000.0, nl);
	}
}
