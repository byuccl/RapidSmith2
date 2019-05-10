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
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.util.Exceptions;
import org.apache.logging.log4j.core.appender.routing.Route;

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


/*
	public RouteStringTree(RouteTree routeTree) {
		this.wireName = routeTree.getWire().getFullName();
		for (RouteTree rt : routeTree.getChildren()) {
			RouteTree trueChild = rt;
			Connection c = rt.getConnection();

			while (c.isDirectConnection()) {
				assert (rou)
			}

			// Only add programmable connections
			if (c.isPip() || c.isRouteThrough()) {
				this.addChild(new RouteStringTree(rt));
			}

		}
	}
	*/


//	/**
//	 * Construct a Route String Tree given a Route Tree.
//	 * @param routeTree
//	 */
//	public RouteStringTree(RouteTree routeTree) {
//		this.wireName = routeTree.getWire().getFullName();
//		for (RouteTree rt : routeTree.getChildren()) {
//			this.addChild(new RouteStringTree(rt));
//		}
	//}

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

	@Override
	public String toString() {
		return wireName;
	}

	/**
	 * This method assumes that the route string tree is made up of only valid nodes for a route string
	 * @return
	 */
	public String toRouteString() {
		RouteStringTree currentRoute = this;
		String routeString = "{ ";

		while ( true ) {
			routeString = routeString.concat(currentRoute.getWireName() + " ");

			ArrayList<RouteStringTree> sinkTrees = (ArrayList<RouteStringTree>) currentRoute.getSinkTrees();

			if (sinkTrees.size() == 0)
				break;

			for(int i = 0; i < sinkTrees.size() - 1; i++)
				routeString = routeString.concat(sinkTrees.get(i).toRouteString());

			currentRoute = sinkTrees.get(sinkTrees.size() - 1) ;
		}

		return routeString + "} ";
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
	 * Find the equivalent route string tree that is in this tree's children
	 * @param toFind
	 * @return
	 */
	public RouteStringTree find(RouteStringTree toFind) {
		for (RouteStringTree tree : this) {

			if (tree.getWireName().equals(toFind.getWireName())) {
				System.out.println(tree.getWireName());
			}

			if (tree.equals(toFind))
				return tree;
		}
		return null;
	}

	/**
	 * Find the equivalent route string tree that is in this tree's children
	 * @param wireName name of the wire in the tree to find
	 * @return
	 */
	public RouteStringTree find(String wireName) {
		for (RouteStringTree tree : this) {
			if (tree.getWireName().equals(wireName))
				return tree;
		}
		return null;
	}

	/**
	 * Hash is based on the wire of this node.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(wireName);
	}

	//@Override
	//public boolean equals(RouteStringTree o) {
	///	return (this.getWireName().equals(o.getWireName()));
	//}

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

	public void addChildren(Collection<RouteStringTree> toAdd) {
		this.sinkTrees.addAll(toAdd);
	}

	public RouteStringTree addChild(String wireName) {
		RouteStringTree sink = new RouteStringTree(wireName);

		sinkTrees.add(sink);
		sink.setSourceTree(this);
		//sink.setConnection(c);
		return sink;
	}

	/**
	 * @return a deep copy of this tree beginning at this node
	 */
	public RouteStringTree deepCopy() {
		RouteStringTree copy = new RouteStringTree(this.wireName);
		sinkTrees.forEach(rt -> copy.sinkTrees.add(rt.deepCopy()));
		copy.sinkTrees.forEach(rt -> rt.sourceTree = this);
		return copy;
	}

	/**
	 * Adds
	 * @param toAdd the tree whose part pin node's children are to be added
	 */
	public void mergePartPinTree(RouteStringTree toAdd, String partPin) {
		// Let's say the part pin is an input to the RM
		// So I call this method on the static tree

		// Find the partition pin tree node of this tree
		RouteStringTree partPinNode = this.find(partPin);

		if (partPinNode == null) {
			// TODO: Throw an error or a warning stating the node wasn't found
			return;
		}

		// We just need to add the RM tree's children now
		assert (toAdd.getWireName().equals(partPin));
		partPinNode.addChildren(toAdd.getSinkTrees());


		// If the part pin acts as an input to the RM
			// Find partPin in static tree
			// Add RM's partPin tree's children to the static tree's


		// If the part pin acts as an output from the RM
			// Find partPin in RM tree
			// Add static partPin tree's children to the RM tree

	}

	public static RouteStringTree merge(RouteStringTree a, RouteStringTree b) {
		for (RouteStringTree node : b) { // for every node in breadth-first traversal order

			// Skip the root
			if (node.getSourceTree() == null)
				continue;

			System.out.println("Find " + node.getSourceTree().getWireName());
			RouteStringTree found = a.find(node.getSourceTree());  // find parent in tree1

			// If there's no parent, it's the root.
			if (found == null)  {
				continue;
			}

			// If there is no node from tree 2 in tree 1
			if (!found.getSinkTrees().contains(node))
				found.addChild(node);
		}
		return a;

		/*
		List<RouteStringTree> aSinkTrees = new ArrayList<>(a.getSinkTrees());
		List<RouteStringTree> bSinkTrees = new ArrayList<>(b.getSinkTrees());
		List<RouteStringTree> toMerge = new ArrayList<>(aSinkTrees);
		toMerge.retainAll(b.getSinkTrees());
		List<RouteStringTree> toAdd = new ArrayList<>(bSinkTrees);
		toAdd.removeAll(a.getSinkTrees());

		for(RouteStringTree n : toMerge) {
			merge(n, bSinkTrees.get(bSinkTrees.indexOf(n)));
		}

		for(RouteStringTree n : toAdd)
			a.addChild(n.deepCopy());


		*/
	}
}
