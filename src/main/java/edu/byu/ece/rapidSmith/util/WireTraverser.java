package edu.byu.ece.rapidSmith.util;

import edu.byu.ece.rapidSmith.design.subsite.Connection;
import edu.byu.ece.rapidSmith.device.Wire;

import java.util.*;

/**
 *
 */
public class WireTraverser<WC extends WireTraverser.WireObject> {
	private Comparator<WC> comparator;
	private PriorityQueue<WC> pq;
	protected Set<Wire> searchedWires;
	protected boolean forward;

	public WireTraverser(Comparator<WC> comparator) {
		this.comparator = comparator;
	}

	public final void traverse(Collection<WC> sources, boolean forward) {
		this.forward = forward;
		pq = new PriorityQueue<>(comparator);
		searchedWires = new HashSet<>();
		for (WC source : sources) {
			pq.add(source);
		}

		while (!pq.isEmpty()) {
			WC obj = pq.poll();
			Wire wire = obj.wire();

			if (searchedWires.contains(wire))
				continue;
			searchedWires.add(wire);

			for (Connection c : getWireConnections(wire, forward)) {
				handleWireConnection(obj, c);
			}

			for (Connection c : getPinConnections(wire, forward)) {
				handlePinConnection(obj, c);
			}

			for (Connection c : getTerminals(wire, forward)) {
				handleTerminals(obj, c);
			}
		}
	}

	public final Collection<Connection> getWireConnections(Wire wire, boolean forward) {
		if (forward) {
			return wire.getWireConnections();
		} else {
			return wire.getReverseWireConnections();
		}
	}

	public final Collection<Connection> getPinConnections(Wire wire, boolean forward) {
		if (forward) {
			return wire.getPinConnections();
		} else {
			return wire.getReversePinConnections();
		}
	}

	public final Collection<Connection> getTerminals(Wire wire, boolean forward) {
		if (forward) {
			return wire.getTerminals();
		} else {
			return wire.getSources();
		}
	}

	public void handleWireConnection(WC source, Connection c) {
		// Default is do nothing
	}
	public void handlePinConnection(WC source, Connection c) {
		// Default is do nothing
	}
	public void handleTerminals(WC source, Connection c) {
		// Default is do nothing
	}

	public final void queueWire(WC wireObject) {
		if (!searchedWires.contains(wireObject.wire())) {
			pq.add(wireObject);
		}
	}

	public interface WireObject {
		Wire wire();
	}

	public static class WireWrapper implements WireTraverser.WireObject {
		private Wire wire;

		public WireWrapper(Wire wire) {
			this.wire = wire;
		}

		public Wire wire() {
			return wire;
		}
	}
}
