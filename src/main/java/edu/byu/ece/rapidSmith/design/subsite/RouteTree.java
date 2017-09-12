/*
 * Copyright (c) 2016 Brigham Young University
 *
 * This file is part of the BYU RapidSmith Tools.
 *
 * BYU RapidSmith Tools is free software: you may redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * BYU RapidSmith Tools is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * A copy of the GNU General Public License is included with the BYU
 * RapidSmith Tools. It can be found at doc/LICENSE.GPL3.TXT. You may
 * also get a copy of the license at <http://www.gnu.org/licenses/>.
 */

package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.PIP;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Connection;
import edu.byu.ece.rapidSmith.device.SitePin;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.util.Exceptions;

import java.util.*;

/**
 * A tree describing a route in a design.  Each RouteTree object represents
 * a node in the tree (with its associated wire in the device) and contains the
 * sinks and connections to the sinks.
 */
public final class RouteTree implements
		Comparable<RouteTree>, Iterable<RouteTree> {
	private RouteTree sourceTree; // Do I want bidirectional checks?
	private final Wire wire;
	private Connection connection;
	private int cost; // for routers
	private final Collection<RouteTree> sinkTrees = new ArrayList<>(1);

	/**
	 * Creates a new unsourced route tree.
	 * @param wire the wire for the starting node in the new tree
	 */
	public RouteTree(Wire wire) {
		this.wire = wire;
	}

	RouteTree(Wire wire, Connection c) {
		this.wire = wire;
		this.connection = c;
	}

	/**
	 * @return the wire connected to this node
	 */
	public Wire getWire() {
		return wire;
	}

	@Deprecated
	public int getCost() {
		return cost;
	}

	@Deprecated
	public void setCost(int cost) {
		this.cost = cost;
	}

	/**
	 * @return the connection connecting this node to its source
	 */
	public Connection getConnection() {
		return connection;
	}

	private void setConnection(Connection connection) {
		this.connection = connection;
	}

	/**
	 * @return the node sourcing this node in the tree
	 */
	public RouteTree getSourceTree() {
		return sourceTree;
	}

	/**
	 * @return the root node of the entire route tree
	 */
	public RouteTree getFirstSource() {
		RouteTree parent = this;
		while (parent.isSourced())
			parent = parent.getSourceTree();
		return parent;
	}

	/**
	 * @return true if this node is sourced, else false
	 */
	public boolean isSourced() {
		return sourceTree != null;
	}

	private void setSourceTree(RouteTree sourceTree) {
		this.sourceTree = sourceTree;
	}

	/**
	 * @return all route trees sourced by this node
	 */
	public Collection<RouteTree> getSinkTrees() {
		return sinkTrees;
	}
	
	/**
	 * Returns true if this node is a leaf (i.e. it has no children).
	 * For a fully routed net, a leaf tree should connect to either a SitePin
	 * or BelPin.
	 */
	public boolean isLeaf() {
		return sinkTrees.size() == 0;
	}
	
	/**
	 * Returns the SitePin connected to the wire of this node. If no SitePin
	 * object is connected, null is returned.
	 */
	public SitePin getConnectingSitePin() {
		return wire.getConnectedPin();
	}
	
	/**
	 * Returns the BelPin connected to the wire of the RouteTree. If no BelPin
	 * object is connected, null is returned.
	 */
	public BelPin getConnectingBelPin() {
		return wire.getTerminal();
	}

	/**
	 * Creates a new route tree for the sink wire of c and adds a connection between
	 * this tree and the new tree.
	 * @param c the connection to the new sink
	 * @return the newly created tree based on the sinkwire of c
	 */
	public RouteTree addConnection(Connection c) {
		RouteTree endTree = new RouteTree(c.getSinkWire(), c);
		endTree.setSourceTree(this);
		sinkTrees.add(endTree);
		return endTree;
	}

	/**
	 * Connects this route tree to tree sink through connection c.
	 * @param c the connection connecting these two trees
	 * @param sink the sink route tree to connect to
	 * @return sink
	 */
	public RouteTree addConnection(Connection c, RouteTree sink) {
		if (sink.getSourceTree() != null)
			throw new Exceptions.DesignAssemblyException("Sink tree already sourced");
		if (!c.getSinkWire().equals(sink.getWire()))
			throw new Exceptions.DesignAssemblyException("Connection does not match sink tree");

		sinkTrees.add(sink);
		sink.setSourceTree(this);
		sink.setConnection(c);
		return sink;
	}

	/**
	 * Removes the route tree connected to this tree through connect c.
	 * @param c connection to a sink to remove
	 */
	public void removeConnection(Connection c) {
		for (Iterator<RouteTree> it = sinkTrees.iterator(); it.hasNext(); ) {
			RouteTree sink = it.next();
			if (sink.getConnection().equals(c)) {
				sink.setSourceTree(null);
				it.remove();
			}
		}
	}

	/**
	 * @return a list of all PIPs used in this route tree
	 */
	public List<PIP> getAllPips() {
		return getFirstSource().getAllPips(new ArrayList<>());
	}

	private List<PIP> getAllPips(List<PIP> pips) {
		for (RouteTree rt : sinkTrees) {
			if (rt.getConnection().isPip())
				pips.add(rt.getConnection().getPip());
			rt.getAllPips(pips);
		}
		return pips;
	}

	/**
	 * @return a deep copy of this tree beginning at this node
	 */
	public RouteTree deepCopy() {
		RouteTree copy = new RouteTree(wire, connection);
		sinkTrees.forEach(rt -> copy.sinkTrees.add(rt.deepCopy()));
		copy.sinkTrees.forEach(rt -> rt.sourceTree = this);
		return copy;
	}

	@Override
	public int compareTo(RouteTree o) {
		return Integer.compare(cost, o.cost);
	}

	/**
	 * Prunes all nodes in the tree that are neither in the set of terminals nor
	 * source a branch of the tree that ends in one of the terminals.
	 * @param terminals the terminal nodes to keep
	 * @return true if the node is either in terminals are sources a node in terminal,
	 *   else false
	 */
	public boolean prune(Set<RouteTree> terminals) {
		return pruneChildren(terminals);
	}

	private boolean pruneChildren(Set<RouteTree> terminals) {
		sinkTrees.removeIf(rt -> !rt.pruneChildren(terminals));
		return !sinkTrees.isEmpty() || terminals.contains(this);
	}

	/**
	 * Iterates over all trees in this route tree starting from this node.  Nodes
	 * in this tree before this node are not traversed.  This iterator provides no
	 * guarantee on the order of traversal, only that all nodes will be visited.
	 */
	@Override
	public Iterator<RouteTree> iterator() {
		return prefixIterator();
	}

	/**
	 * Iterates over all trees in this route tree starting from this node in a
	 * prefix order, ie. parent nodes are guaranteed to be visited prior to the
	 * children.  Nodes in the tree prior to this node are not traversed.
	 */
	public Iterator<RouteTree> prefixIterator() {
		return new PrefixIterator();
	}

	private class PrefixIterator implements Iterator<RouteTree> {
		private final Stack<RouteTree> stack;

		PrefixIterator() {
			this.stack = new Stack<>();
			this.stack.push(RouteTree.this);
		}

		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		@Override
		public RouteTree next() {
			if (!hasNext())
				throw new NoSuchElementException();
			RouteTree tree = stack.pop();
			stack.addAll(tree.getSinkTrees());
			return tree;
		}
	}

	// Uses identity equals

	/**
	 * Hash is based on the wire of this node.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(wire);
	}
}
