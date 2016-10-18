package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.SitePin;
import edu.byu.ece.rapidSmith.device.Wire;

import java.util.*;

/**
 *
 */
public final class RouteTree implements
		Comparable<RouteTree>, Iterable<RouteTree> {
	private RouteTree sourceTree; // Do I want bidirectional checks?
	private Wire wire;
	private Connection connection;
	private int cost; // for routers
	private Collection<RouteTree> sinkTrees = new ArrayList<>(1);

	public RouteTree(Wire wire) {
		this.wire = wire;
	}

	RouteTree(Wire wire, Connection c) {
		this.wire = wire;
		this.connection = c;
	}

	public Wire getWire() {
		return wire;
	}

	public int getCost() {
		return cost;
	}

	public void setCost(int cost) {
		this.cost = cost;
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public RouteTree getSourceTree() {
		return sourceTree;
	}

	public RouteTree getFirstSource() {
		RouteTree parent = this;
		while (parent.isSourced())
			parent = parent.getSourceTree();
		return parent;
	}

	public boolean isSourced() {
		return sourceTree != null;
	}

	public void setSourceTree(RouteTree sourceTree) {
		this.sourceTree = sourceTree;
	}

	public Collection<RouteTree> getSinkTrees() {
		return sinkTrees;
	}
	
	public boolean isLeaf() {
		return sinkTrees.size() == 0;
	}
	
	public SitePin getConnectingSitePin() {
		Collection<Connection> pinConnections = wire.getPinConnections();
		assert(!pinConnections.isEmpty()) : "RouteTree does not connect to SitePin";
		return pinConnections.iterator().next().getSitePin();
	}
	
	public BelPin getConnectingBelPin() {
		Collection<Connection> terminalConnections = wire.getTerminals();;
		assert(!terminalConnections.isEmpty()) : "RouteTree does not connect to BelPin";
		return terminalConnections.iterator().next().getBelPin();
	}

	public RouteTree addConnection(Connection c) {
		RouteTree endTree = new RouteTree(c.getSinkWire(), c);
		endTree.setSourceTree(this);
		sinkTrees.add(endTree);
		return endTree;
	}

	public RouteTree addConnection(Connection c, RouteTree sink) {
		if (sink.getSourceTree() != null)
			throw new DesignAssemblyException("Sink tree already sourced");
		if (!c.getSinkWire().equals(sink.getWire()))
			throw new DesignAssemblyException("Connection does not match sink tree");

		sinkTrees.add(sink);
		sink.setSourceTree(this);
		sink.setConnection(c);
		return sink;
	}

	public void removeConnection(Connection c) {
		for (Iterator<RouteTree> it = sinkTrees.iterator(); it.hasNext(); ) {
			RouteTree sink = it.next();
			if (sink.getConnection().equals(c)) {
				sink.setSourceTree(null);
				it.remove();
			}
		}
	}

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

	public boolean prune(RouteTree terminal) {
		Set<RouteTree> toPrune = new HashSet<RouteTree>();
		toPrune.add(terminal);
		return prune(toPrune);
	}
	
	public boolean prune(Set<RouteTree> terminals) {
		return pruneChildren(terminals);
	}

	private boolean pruneChildren(Set<RouteTree> terminals) {
		for (Iterator<RouteTree> iterator = sinkTrees.iterator(); iterator.hasNext(); ) {
			RouteTree rt = iterator.next();
			if (!rt.pruneChildren(terminals))
				iterator.remove();
		}

		return !sinkTrees.isEmpty() || terminals.contains(this);
	}
	
	@Override
	public Iterator<RouteTree> iterator() {
		return prefixIterator();
	}

	public Iterator<RouteTree> prefixIterator() {
		return new PrefixIterator();
	}

	private class PrefixIterator implements Iterator<RouteTree> {
		private Stack<RouteTree> stack;

		public PrefixIterator() {
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

	@Override
	public int hashCode() {
		return Objects.hash(connection);
	}
}
