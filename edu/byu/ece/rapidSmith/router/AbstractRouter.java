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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.design.Net;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.design.Pin;
import edu.byu.ece.rapidSmith.device.*;


public abstract class AbstractRouter{

	/** The XDL object that holds the input design to route */
	protected Design design;
	/** This is the device database */
	protected Device dev;
	/** This is the wire enumerator database */
	protected WireEnumerator we;
	/** This keeps track of all the used nodes in the chip during routing */
	protected HashSet<Node> usedNodes;
	/** Keeps track for each used node by which net it is used by */
	protected HashMap<Node,LinkedList<Net>> usedNodesMap; // TODO - Does this really need to have multiple values, resources can't be used by multiple nets
	/** This keeps track of all the visited nodes in the chip during routing */
	protected HashSet<Node> visitedNodes;
	/** The current working net list */
	public ArrayList<Net> netList;
	/** A Priority Queue for nodes to be processed */
	protected PriorityQueue<Node> queue;
	/** Some nodes are reserved for particular routes to minimize routing conflicts later */
	protected HashMap<Net,ArrayList<Node>> reservedNodes;

	/** PIPs that are part of the most recently routed connection */
	protected ArrayList<PIP> pipList;
	
	/** Keeps track of all current sources for a given net (to avoid the RUG CREATION PROBLEM) */
	protected HashSet<Node> currSources;
	/** Current sink node to be routed */
	protected Node currSink;
	/** Current net to be routed */
	protected Net currNet;
	/** Current sink pin to be routed */ 
	protected Pin currSinkPin;
	/** PIPs of the current net being routed */
	protected ArrayList<PIP> netPIPs;

	protected Node tempNode;
	
	/** A flag indicating if the current connection was routed successfully */
	protected boolean successfulRoute;
	/** A flag which determines if the current sink is a clock wire */
	protected boolean isCurrSinkAClkWire;
	
	// Statistic variables
	/** Total number of connections in design */
	protected int totalConnections;
	/** Counts the total number of nodes that were examined in routing */
	protected int totalNodesProcessed;
	/** Counts number of nodes processed during a route */
	protected int nodesProcessed;
	/** Counts the number of times the router failed to route a connection */
	protected int failedConnections;
	NodeFactory<? extends Node> factory;
	
	public AbstractRouter() {
		this(new DefaultNodeFactory());
	}
	
	public AbstractRouter(NodeFactory<? extends Node> n) {
		factory = n;
		// Initialize variables
		tempNode = factory.newNode();
		usedNodes = new HashSet<Node>();
		usedNodesMap = new HashMap<Node, LinkedList<Net>>();
		reservedNodes = new HashMap<Net, ArrayList<Node>>();
		// Create a compare function based on node's cost
		queue = new PriorityQueue<Node>(16, new Comparator<Node>() {
			public int compare(Node i, Node j) {return i.cost - j.cost;}});

		totalConnections = 0;
		totalNodesProcessed = 0;
		nodesProcessed = 0;
		failedConnections = 0;
		currSink = factory.newNode();
	}
	
	public Design getDesign(){
		return design;
	}
	
	/**
	 * Sets a node (combined tile and wire) as used and maps 
	 * the usage to the given net.
	 * @param t The tile specifier for the node to be marked as used.
	 * @param wire The wire specifier for the node to be marked as used.
	 * @param net The net using the node.
	 * @return The node that was set as used.
	 */
	protected Node setWireAsUsed(Tile t, int wire, Net net){
		Node n = factory.newNode(t, wire, null, 0);
		usedNodes.add(n);
		addUsedWireMapping(net, n);	
		return n;
	}
	
	/**
	 * Sets a node (combined tile and wire) as unused and unmaps 
	 * the usage to the given net.
	 * @param t The tile specifier for the node to be marked as unused.
	 * @param wire The wire specifier for the node to be marked as unused.
	 * @param net The net currently using the node.
	 * @return The node that was set as unused.
	 */
	protected Node setWireAsUnused(Tile t, int wire, Net net){
		Node n = factory.newNode(t, wire, null, 0);
		usedNodes.remove(n);
		removeUsedWireMapping(net, n);		
		return n;
	}
	
	/**
	 * This method allows a router to keep track of which nets use which
	 * nodes.
	 * @param net The net using node n.
	 * @param n The node used by the given net
	 */
	protected void addUsedWireMapping(Net net, Node n){
		LinkedList<Net> list = usedNodesMap.get(n);
		if(list == null){ 
			list = new LinkedList<Net>(); 
		}
		if(!list.contains(net)){ 
			list.add(net);
			usedNodesMap.put(n, list);
		}
	}
	
	/**
	 * This method removes a node usage mapping to a net when it is being
	 * marked as unused.
	 * @param net The net currently using the node.
	 * @param n The node to be removed.
	 */
	protected void removeUsedWireMapping(Net net, Node n){
		LinkedList<Net> list = usedNodesMap.get(n);
		if(list == null){ 
			return; 
		}
		if(list.remove(net)){
			if(list.isEmpty()){
				usedNodesMap.remove(n);
			}
		}
	}
	
	/**
	 * @return the reserved Nodes Map
	 */
	public HashMap<Net, ArrayList<Node>> getReservedNodes() {
		return reservedNodes;
	}

	/**
	 * Gets are returns a list of reserved nodes for the provide net.
	 * @param net The net to get reserved nodes for.
	 * @return A list of reserved nodes for the net, or null if no 
	 * nodes are reserved.
	 */
	public ArrayList<Node> getReservedNodesForNet(Net net){
		return reservedNodes.get(net);
	}
	
	public boolean isNodeUsed(Tile tile, int wire){
		tempNode.setTileAndWire(tile, wire);
		return usedNodes.contains(tempNode);
	}
	
	public boolean isNodeUsed(Node node){
		return usedNodes.contains(node);
	}
	
	/**
	 * Examines the pips in the list and marks all of the resources
	 * as used.
	 * @param pips The PIPs to mark as used.
	 */
	public void markPIPsAsUsed(ArrayList<PIP> pips){
		for (PIP pip : pips){
			setWireAsUsed(pip.getTile(), pip.getStartWire(), currNet);
			setWireAsUsed(pip.getTile(), pip.getEndWire(), currNet);
			markIntermediateNodesAsUsed(pip, currNet);
		}
	}
	
	/**
     * Creates sources from a list of PIPs
	 * @param pips The pips of the net to examine.
	 * @return The list of sources gathered from the pips list.
	 */
	public ArrayList<Node> getSourcesFromPIPs(ArrayList<PIP> pips){
		ArrayList<Node> sources = new ArrayList<Node>(pips.size()*2);
		for(PIP pip : pips){
			sources.add(factory.newNode(pip.getTile(), pip.getStartWire(), null, 0));
			sources.add(factory.newNode(pip.getTile(), pip.getEndWire(), null, 0));
		}
		return sources;
	}
	
	/**
	 * Checks each node in a PIP to see if there are other nodes that should be
	 * marked as used. These are wires external to a tile such as
	 * doubles/pents/hexes/longlines.
	 * @param pip The pip to check intermediate used nodes for
	 * @param currentNet The net to associate with the intermediate nodes, null if 
	 * the usedNodesMap should not be updated
	 */
	protected void markIntermediateNodesAsUsed(PIP pip, Net currentNet){
		WireConnection[] wires = pip.getTile().getWireConnections(pip.getEndWire());
		if(wires != null && wires.length > 1){
			for(WireConnection w : wires){
				if(w.getRowOffset() != 0 || w.getColumnOffset() != 0){
					Node tmp = setWireAsUsed(w.getTile(pip.getTile()), w.getWire(), currentNet);
					if(currentNet != null) addUsedWireMapping(currentNet, tmp);
				}
			}
		}
		if(we.getWireType(pip.getStartWire()).equals(WireType.LONG) && we.getWireType(pip.getEndWire()).equals(WireType.LONG)){
			wires = pip.getTile().getWireConnections(pip.getStartWire());
			if(wires != null && wires.length > 1){
				for(WireConnection w : wires){
					if(w.getRowOffset() != 0 || w.getColumnOffset() != 0){
						Node tmp = setWireAsUsed(w.getTile(pip.getTile()), w.getWire(), currentNet);
						if(currentNet != null) addUsedWireMapping(currentNet, tmp);
					}
				}
			}
		}
	}
	
	protected void markIntermediateNodesAsUnused(PIP pip, Net currentNet){
		WireConnection[] wires = pip.getTile().getWireConnections(pip.getEndWire());
		if(wires != null && wires.length > 1){
			for(WireConnection w : wires){
				if(w.getRowOffset() != 0 || w.getColumnOffset() != 0){
					Node tmp = setWireAsUnused(w.getTile(pip.getTile()), w.getWire(), currentNet);
					if(currentNet != null) removeUsedWireMapping(currentNet, tmp);
				}
			}
		}
		if(we.getWireType(pip.getStartWire()).equals(WireType.LONG) && we.getWireType(pip.getEndWire()).equals(WireType.LONG)){
			wires = pip.getTile().getWireConnections(pip.getStartWire());
			if(wires != null && wires.length > 1){
				for(WireConnection w : wires){
					if(w.getRowOffset() != 0 || w.getColumnOffset() != 0){
						Node tmp = setWireAsUnused(w.getTile(pip.getTile()), w.getWire(), currentNet);
						if(currentNet != null) removeUsedWireMapping(currentNet, tmp);
					}
				}
			}
		}
	}	
}

class DefaultNodeFactory implements NodeFactory<Node> {

	public Node newNode() {
		return new Node();
	}

	public Node newNode(Tile t, int i, Node parent, int depth) {
		return new Node(t,i,parent,depth);
	}
	
}
