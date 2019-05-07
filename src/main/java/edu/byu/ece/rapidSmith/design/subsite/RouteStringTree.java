/*
 * Copyright (c) 2019 Brigham Young University
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

import edu.byu.ece.rapidSmith.device.Connection;
import edu.byu.ece.rapidSmith.util.Exceptions;

import java.util.*;

/**
 * A tree describing a route in a design.  Each RouteStringTree object represents
 * a node in the tree (with its associated wire in the device) and contains the
 * sinks and connections to the sinks.
 */
public final class RouteStringTree implements Comparable<RouteStringTree>, Iterable<RouteStringTree> {
	private RouteStringTree sourceTree; // Do I want bidirectional checks?
	private final String wireName;
	//private Connection connection;
	//private int cost; // for routers
	private final Collection<RouteStringTree> sinkTrees = new ArrayList<>(1);

	/**
	 * Creates a new unsourced route tree.
	 * @param wireName the wire for the starting node in the new tree
	 */
	public RouteStringTree(String wireName) {
		this.wireName = wireName;
	}

	public RouteStringTree(RouteTree routeTree, RouteStringTree parent) {
		this.wireName = routeTree.getWire().getFullName();
		for (RouteTree rt : routeTree.getChildren()) {
			parent.addChild(new RouteStringTree(rt, parent));
		}

	}

	/**
	 * @return the wire connected to this node
	 */
	public String getWireName() {
		return wireName;
	}

	/**
	 * @return the node sourcing this node in the tree
	 */
	public RouteStringTree getSourceTree() {
		return sourceTree;
	}

	/**
	 * @return the root node of the entire route tree
	 */
	public RouteStringTree getFirstSource() {
		RouteStringTree parent = this;
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

	private void setSourceTree(RouteStringTree sourceTree) {
		this.sourceTree = sourceTree;
	}

	/**
	 * @return all route trees sourced by this node
	 */
	public Collection<RouteStringTree> getSinkTrees() {
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


	@Override
	public int compareTo(RouteStringTree o) {
		return this.wireName.compareTo(o.wireName);
	}

	/**
	 * Prunes all nodes in the tree that are neither in the set of terminals nor
	 * source a branch of the tree that ends in one of the terminals.
	 * @param terminals the terminal nodes to keep
	 * @return true if the node is either in terminals are sources a node in terminal,
	 *   else false
	 */
	public boolean prune(Set<RouteStringTree> terminals) {
		return pruneChildren(terminals);
	}

	private boolean pruneChildren(Set<RouteStringTree> terminals) {
		sinkTrees.removeIf(rt -> !rt.pruneChildren(terminals));
		return !sinkTrees.isEmpty() || terminals.contains(this);
	}

	/**
	 * Iterates over all trees in this route tree starting from this node.  Nodes
	 * in this tree before this node are not traversed.  This iterator provides no
	 * guarantee on the order of traversal, only that all nodes will be visited.
	 */
	@Override
	public Iterator<RouteStringTree> iterator() {
		return prefixIterator();
	}

	/**
	 * Iterates over all trees in this route tree starting from this node in a
	 * prefix order, ie. parent nodes are guaranteed to be visited prior to the
	 * children. Nodes in the tree prior to this node are not traversed.
	 */
	public Iterator<RouteStringTree> prefixIterator() {
		return new PrefixIterator();
	}

	private class PrefixIterator implements Iterator<RouteStringTree> {
		private final Stack<RouteStringTree> stack;

		PrefixIterator() {
			this.stack = new Stack<>();
			this.stack.push(RouteStringTree.this);
		}

		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		@Override
		public RouteStringTree next() {
			if (!hasNext())
				throw new NoSuchElementException();
			RouteStringTree tree = stack.pop();
			stack.addAll(tree.getSinkTrees());
			return tree;
		}
	}

	/**
	 * Hash is based on the wire of this node.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(wireName);
	}

	/**
	 * Connects this route tree to tree sink through connection c.
	 * @param sink the sink route tree to connect to
	 * @return sink
	 */
	public RouteStringTree addChild( RouteStringTree sink) {
		if (sink.getSourceTree() != null)
			throw new Exceptions.DesignAssemblyException("Sink tree already sourced");

		sinkTrees.add(sink);
		sink.setSourceTree(this);
		//sink.setConnection(c);
		return sink;
	}

	public RouteStringTree addChild(String wireName) {
		RouteStringTree sink = new RouteStringTree(wireName);

		sinkTrees.add(sink);
		sink.setSourceTree(this);
		//sink.setConnection(c);
		return sink;
	}

	public String toRouteString(){
		//StringBuilder toReturn = new StringBuilder();
		//toReturn.append(this.getWireName().getFullName()+"\n");
		//for(RouteStringTree sink : this.getSinkTrees()){
		//	toReturn.append(sink.toRouteString());
		//}
		//return toReturn.toString();
		return wireName;
	}
}
