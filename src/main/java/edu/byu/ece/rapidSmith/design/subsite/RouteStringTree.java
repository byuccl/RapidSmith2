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

import edu.byu.ece.rapidSmith.util.Exceptions;

import java.util.*;

/**
 * A tree describing a route in a design.  Each RouteStringTree object represents
 * a node in the tree (with its associated wire name in the device) and contains the
 * sinks.
 */
public final class RouteStringTree implements Comparable<RouteStringTree>, Iterable<RouteStringTree> {
    private final String wireName;
    private final Collection<RouteStringTree> sinkTrees = new ArrayList<>(1);
    private RouteStringTree sourceTree;

    /**
     * Creates a new unsourced route string tree.
     *
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
     * @return the wire of this node
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

    private void setSourceTree(RouteStringTree sourceTree) {
        this.sourceTree = sourceTree;
    }

    /**
     * @return the root node of the entire route string tree
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
     * Converts this tree (and its children) into a route string for Vivado.
     * Assumes the tree is made up of only valid nodes for a Vivado route string.
     *
     * @return the route string
     */
    public String toRouteString() {
        RouteStringTree currentRoute = this;
        String routeString = "{ ";

        while (true) {
            routeString = routeString.concat(currentRoute.getWireName() + " ");

            ArrayList<RouteStringTree> sinkTrees = (ArrayList<RouteStringTree>) currentRoute.getSinkTrees();

            if (sinkTrees.size() == 0)
                break;

            for (int i = 0; i < sinkTrees.size() - 1; i++)
                routeString = routeString.concat(sinkTrees.get(i).toRouteString());

            currentRoute = sinkTrees.get(sinkTrees.size() - 1);
        }

        return routeString + "} ";
    }

    /**
     * Prunes all nodes in the tree that are neither in the set of terminals nor
     * source a branch of the tree that ends in one of the terminals.
     *
     * @param terminals the terminal nodes to keep
     * @return true if the node is either in terminals are sources a node in terminal,
     * else false
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

    /**
     * Find the equivalent route string tree that is in this tree's children
     *
     * @param toFind the route string tree to find
     * @return found tree if successful, null otherwise
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
     *
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
     * Hash is based on the wire name of this tree node.
     */
    @Override
    public int hashCode() {
        return Objects.hash(wireName);
    }

    /**
     * Adds sink as a child of the tree.
     *
     * @param sink the sink route string tree to connect to
     * @return sink
     */
    public RouteStringTree addChild(RouteStringTree sink) {
        if (sink.getSourceTree() != null)
            throw new Exceptions.DesignAssemblyException("Sink tree already sourced");

        sinkTrees.add(sink);
        sink.setSourceTree(this);
        return sink;
    }

    /**
     * Adds the trees as children to this tree.
     *
     * @param toAdd trees to add
     */
    public void addChildren(Collection<RouteStringTree> toAdd) {
        this.sinkTrees.addAll(toAdd);
    }

    /**
     * Creates a new tree by the name of wireName and adds it as a child to this tree
     *
     * @param wireName name of new tree to add as a child.
     * @return sink
     */
    public RouteStringTree addChild(String wireName) {
        RouteStringTree sink = new RouteStringTree(wireName);
        sinkTrees.add(sink);
        sink.setSourceTree(this);
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
     * Adds all the children from another route string tree to the same node of this tree.
     *
     * @param toAdd the tree whose children to add
     */
    public void addChildrenFromTree(RouteStringTree toAdd) {
        // Find the wire node that is in this tree
        RouteStringTree wireNode = this.find(toAdd.getWireName());

        if (wireNode == null) {
            System.err.println("[Warning]: " + toAdd.getWireName() + " not found in route tree. No children will be added.");
            return;
        }

        // Add all of the wire's children to this tree
        wireNode.addChildren(toAdd.getSinkTrees());
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
}
