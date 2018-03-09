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

import edu.byu.ece.rapidSmith.device.*;

import java.util.*;

/**
 * A tree describing a route in a design.  Each RouteTree object represents
 * a node in the tree (with its associated wire in the device) and contains the
 * sinks and connections to the sinks.
 */
public class RouteTree extends AbstractRouteTree<RouteTree> {
	private RouteTree parent;
	private Connection connection;
	private int cost; // for routers

	/**
	 * Creates a new unsourced route tree.
	 * @param wire the wire for the starting node in the new tree
	 */
	public RouteTree(Wire wire) {
		super(wire);
	}

	@Deprecated
	public int getCost() {
		return cost;
	}

	@Deprecated
	public void setCost(int cost) {
		if(cost < 0){
			cost = Integer.MAX_VALUE;
		}
		this.cost = cost;
	}

	@Override
	protected RouteTree newInstance(Wire wire) {
		return new RouteTree(wire);
	}

	@Override
	protected void connectToSource(Connection c, AbstractRouteTree<RouteTree> parent) {
		if (getParent() != null)
			throw new IllegalStateException("RouteTree already sourced: wire=" + getWire() +
				", currentParent=" + getParent() + ", newParent=" + parent);

		super.connectToSource(c, parent);
		connection = c;
		this.parent = (RouteTree) parent;
	}

	@Override
	protected void disconnectFromSource() {
		super.disconnectFromSource();
		parent = null;
		connection = null;
	}

	/**
	 * @return the {@link Connection} connecting this node to its parent or null if this
	 * node is a root
	 */
	public final Connection getConnection() {
		return connection;
	}

	/**
	 * @param <T> the expected type of the parent
	 * @return the parent of this tree or null if this node is a root
	 */
	@SuppressWarnings("unchecked")
	public final <T extends RouteTree> T getParent() {
		return (T) parent;
	}

	/**
	 * @param <T> the expected type of the tree
	 * @return the root of this tree
	 */
	@SuppressWarnings("unchecked")
	public final <T extends RouteTree> T getRoot() {
		RouteTree parent = this;
		while (parent.isSourced())
			parent = parent.getParent();
		return (T) parent;
	}

	/**
	 * @deprecated use {@link #getParent()}
	 */
	@Deprecated
	public final RouteTree getSourceTree() {
		return getParent();
	}

	/**
	 * @deprecated use {@link #getRoot()}
	 */
	@Deprecated
	public final RouteTree getFirstSource() {
		return getRoot();
	}

	/**
	 * @deprecated use {@link #connect(Connection)}
	 */
	@Deprecated
	public final RouteTree addConnection(Connection c) {
		return connect(c);
	}

	/**
	 * @deprecated use {@link #connect(Connection, AbstractRouteTree)}
	 */
	@Deprecated
	public final RouteTree addConnection(Connection c, RouteTree sink) {
		return connect(c, sink);
	}

	/**
	 * @deprecated use {@link #getConnectedSitePin()}
	 */
	@Deprecated
	public final SitePin getConnectingSitePin() {
		return getConnectedSitePin();
	}

	/**
	 * @deprecated use {@link #getConnectedBelPin()}
	 */
	@Deprecated
	public final BelPin getConnectingBelPin() {
		return getConnectedBelPin();
	}

	/**
	 * @deprecated use {@link #disconnect(Connection)}
	 */
	@Deprecated
	public final void removeConnection(Connection c) {
		disconnect(c);
	}

	/**
	 * @deprecated use {@link #getChildren()}
	 */
	@Deprecated
	public final Collection<RouteTree> getSinkTrees() {
		return getChildren();
	}

	/**
	 * @return true if the tree has a parent
	 */
	public final boolean isSourced() {
		return parent != null;
	}

	/**
	 * @return all of the PIPs used in this tree
	 */
	public final List<PIP> getAllPips() {
		return getRoot().getAllPips(new ArrayList<>());
	}

	private List<PIP> getAllPips(List<PIP> pips) {
		for (RouteTree rt : getChildren()) {
			if (rt.getConnection().isPip())
				pips.add(rt.getConnection().getPip());
			rt.getAllPips(pips);
		}
		return pips;
	}

	/**
	 * Provides a type safe iterator for subclasses of RouteTree.
	 */
	@SuppressWarnings("unchecked")
	public <T extends RouteTree> Iterable<T> typedIterator() {
		// this cast scares the crap out of me.  Is it safe?
		return (Iterable<T>) this;
	}

	/**
	 * @return a deep copy of this tree starting from this node
	 */
	@SuppressWarnings("unchecked")
	public <S extends RouteTree> S deepCopy() {
		Queue<CopyPair> q = new ArrayDeque<>();
		RouteTree copy = newInstance(getWire());
		q.add(new CopyPair(this, copy));

		while (!q.isEmpty()) {
			CopyPair pair = q.poll();
			for (RouteTree origChild : pair.orig.getChildren()) {
				RouteTree copyChild = origChild.newInstance(origChild.getWire());
				pair.copy.connect(origChild.getConnection(), copyChild);
				q.add(new CopyPair(origChild, copyChild));
			}
		}
		return (S) copy;
	}

	private static class CopyPair {
		RouteTree orig;
		RouteTree copy;

		CopyPair(RouteTree orig, RouteTree copy) {
			this.orig = orig;
			this.copy = copy;
		}
	}

	public String toRouteString(){
		StringBuilder toReturn = new StringBuilder();
		toReturn.append(this.getWire().getFullName()).append("\n");
		for(RouteTree sink : this.getChildren()){
			toReturn.append(sink.toRouteString());
		}
		return toReturn.toString();
	}
}
