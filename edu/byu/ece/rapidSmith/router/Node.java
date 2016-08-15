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

import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.SinkPin;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.WireConnection;


/**
 * This class represents the basic routing element, a node or wire.  A node is described as a 
 * wire with a particular name in a particular tile.  When routing, it keeps track of the source node
 * by setting the parent variable.
 * @author Chris Lavin
 *
 */
public class Node implements Comparable<Node>{
	
	/** This is the tile where the node/wire resides */
	protected Tile tile;
	/** This is the enumerated int that represents the name of the wire specified */
	protected int wire;
	/** This is the routing cost of using this node in the current route */
	protected int cost;
	/** This is the pointer to a parent node in the route it is a part of */
	protected Node parent;
	/** This is the number of hops from the original source of the route this node is */
	protected int level;
	/** This is the combined cost of a node when it is used, and used multiple times */
	protected int history;
	/** Determines if this node connected to the node after it creates a PIP */
	protected boolean isPIP;
	/** Keeps track of the wires that this node connects to */
	protected WireConnection[] wires;
	
	/**
	 * Empty constructor, sets tile and wires to null. Sets wire and cost to -1.
	 * level and history are set to 0 and isPIP is set to false.
	 */
	public Node(){
		tile = null;
		wire = -1;
		wires = null;
		cost = -1;
		level = 0;
		history = 0;
		isPIP = false;
	}
	
	/**
	 * A quick population constructor, parent is set to null, and the level is 0.
	 * @param tile The tile of the new node.
	 * @param wire The wire of the new node.
	 */
	public Node(Tile tile, int wire){
		setTile(tile);
		setWire(wire);
		setParent(null);
		setLevel(0);
	}
	
	/**
	 * A quick population constructor.
	 * @param tile The tile of the new node.
	 * @param wire The wire of the new node.
	 * @param parent The parent of the new node, or null if none.
	 * @param level The number of nodes between this node and the source node.
	 */
	public Node(Tile tile, int wire, Node parent, int level){
		setTile(tile);
		setWire(wire);
		setParent(parent);
		setLevel(level);
	}
	
	/**
	 * A quick population constructor.
	 * @param tile The tile of the new node.
	 * @param wire The wire of the new node.
	 * @param parent The parent of the new node, or null if none.
	 * @param level The number of nodes between this node and the source node.
	 * @param isPIP A flag indicating that this node and its parent form a PIP.
	 */
	public Node(Tile tile, int wire, Node parent, int level, boolean isPIP){
		setTile(tile);
		setWire(wire);
		setParent(parent);
		setLevel(level);
		setPIP(isPIP);
	}
	
	/**
	 * A quick setter method for the tile and wire.
	 * @param tile The new tile of the node.
	 * @param wire The new wire of the node.
	 */
	public void setTileAndWire(Tile tile, int wire){
		setTile(tile);
		setWire(wire);
	}
	
	/**
	 * Gets all the possible connections to leaving this node
	 * @return The list of all possible connections leaving this node 
	 */
	public WireConnection[] getConnections(){
		return wires;
	}
	
	/**
	 * Returns the current cost of this node
	 * @return The cost of this node
	 */
	public int getCost(){
		return this.cost;
	}
	
	/**
	 * @return the number of hops from the source this node is
	 */
	public int getLevel(){
		return level;
	}

	/**
	 * @param level the number of hops from the source to this node
	 */
	public void setLevel(int level){
		this.level = level;
	}

	/**
	 * @return the tile
	 */
	public Tile getTile(){
		return tile;
	}

	/**
	 * @param tile the tile to set
	 */
	public void setTile(Tile tile){
		this.tile = tile;
	}
	
	public void setCost(int cost){
		this.cost = cost;
	}

	/**
	 * @return the parent
	 */
	public Node getParent(){
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(Node parent){
		this.parent = parent;
	}

	/**
	 * @return the wire
	 */
	public int getWire(){
		return wire;
	}

	/**
	 * @param wire the wire to set
	 */
	public void setWire(int wire){
		this.wire = wire;
		this.wires = tile.getWireConnections(wire);
	}

	/**
	 * @return the history
	 */
	public int getHistory() {
		return history;
	}

	/**
	 * @param history the history to set
	 */
	public void setHistory(int history) {
		this.history = history;
	}

	/**
	 * @return the isPIP
	 */
	public boolean isPIP() {
		return isPIP;
	}

	/**
	 * @param isPIP the isPIP to set
	 */
	public void setPIP(boolean isPIP) {
		this.isPIP = isPIP;
	}

	public SinkPin getSinkPin(){
		return tile.getSinkPin(wire);
	}
	
	public Node getSwitchBoxSink(Device dev){
		SinkPin sp = tile.getSinks().get(wire);
		int y = sp.switchMatrixTileOffset;
		int x = y >> 16;
		y = (y << 16) >> 16; 
		Node n = new Node(dev.getTile(tile.getRow()+y, tile.getColumn()+x),sp.switchMatrixSinkWire,null,0);
		return n;
	}
	
	public int getManhattanDistance(Node snk){
		return tile.getManhattanDistance(snk.getTile());
	}
	
	/**
	 * The priority queue will use strictly the cost to evaluate priority
	 */
	public int compareTo(Node node){
		return this.cost - node.cost;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tile == null) ? 0 : tile.hashCode());
		result = prime * result + wire;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj){
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (wire != other.wire)
			return false;
		if (tile == null){
			if (other.tile != null)
				return false;
		} else if (!tile.equals(other.tile))
			return false;
		return true;
	}

	public String toString(){
		return tile + " " +
				(wire == -1 ? "-1" : tile.getDevice().getWireEnumerator().getWireName(wire)) +
				" " + cost + " " + level;
	}
}
