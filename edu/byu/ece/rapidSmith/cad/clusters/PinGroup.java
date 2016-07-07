package edu.byu.ece.rapidSmith.cad.clusters;

import edu.byu.ece.rapidSmith.cad.clusters.router.RoutingTable;
import edu.byu.ece.rapidSmith.design.subsite.Connection;
import edu.byu.ece.rapidSmith.design.subsite.SiteWire;
import edu.byu.ece.rapidSmith.design.subsite.Wire;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.PrimitiveType;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class PinGroup implements Serializable {
	private List<Wire> sourceWires = new ArrayList<>();
	private List<Wire> sinkWires = new ArrayList<>();
	private List<BelPin> sourceBelPins = new ArrayList<>();
	private List<BelPin> sinkBelPins = new ArrayList<>();
	private List<Wire> carryChainSinks = new ArrayList<>();
	private List<Wire> carryChainSources = new ArrayList<>();
	public RoutingTable routingTable;

	private PinGroup() {}

	private void addSourcePin(BelPin sinkBelPin) {
		if (!sourceBelPins.contains(sinkBelPin))
			sourceBelPins.add(sinkBelPin);
	}

	private void addSourceWire(Wire sourceWire) {
		if (!sourceWires.contains(sourceWire))
			sourceWires.add(sourceWire);
	}

	public List<Wire> getSourceWires() {
		return sourceWires;
	}

	public List<BelPin> getSourceBelPins() {
		return sourceBelPins;
	}

	public List<Wire> getSinkWires() {
		return sinkWires;
	}

	public void addSinkWire(Wire sinkWire) {
		if (!sinkWires.contains(sinkWire))
			sinkWires.add(sinkWire);
	}

	public List<BelPin> getSinkPins() {
		return sinkBelPins;
	}

	public void addSinkPin(BelPin sinkBelPin) {
		if (!sinkBelPins.contains(sinkBelPin))
			sinkBelPins.add(sinkBelPin);
	}

	public List<Wire> getCarryChainSinks() {
		return carryChainSinks;
	}

	public void addCarryChainSink(Wire sinkPin) {
		carryChainSinks.add(sinkPin);
	}

	public List<Wire> getCarryChainSources() {
		return carryChainSources;
	}

	public void addCarryChainSource(Wire sourcePin) {
		carryChainSources.add(sourcePin);
	}

	public static Map<BelPin, PinGroup> buildClusterPinGroups(
			ClusterTemplate<?> cluster
	) {
		return new Builder(cluster).build();
	}

	private static class Builder {
		private ClusterTemplate<?> template;
		private Map<BelPin, PinGroup> groups = new HashMap<>();
		private Map<Wire, PinGroup> wireGroups = new HashMap<>();

		public Builder(ClusterTemplate<?> template) {
			this.template = template;
		}

		private Map<BelPin, PinGroup> build() {
			for (Bel bel : template.getBels()) {
				// Kind of a special case.  Some inputs to DSPs are driven by
				// VCC and GND coming from a TIEOFF in the tile (not the switch
				// box one).  I'll check if the input comes from the TIEOFF and remove
				// them.  If a tile has a legitimate use for a TIEOFF other than alternative
				// to using the TIEOFF in the switchbox, then this code will fail.
				if (bel.getSite().getType() == PrimitiveType.TIEOFF)
					continue;

				for (Direction direction : Direction.values()) {
					Collection<BelPin> belPins = direction == Direction.FORWARD ?
							bel.getSources() :
							bel.getSinks();
					Set<Wire> edgeWires = direction == Direction.FORWARD ?
							new HashSet<>(template.getOutputs()) :
							new HashSet<>(template.getInputs());
					for (BelPin sourcePin : belPins) {
						PinGroup pg = new PinGroup();
						if (direction == Direction.FORWARD)
							pg.addSourcePin(sourcePin);
						else
							pg.addSinkPin(sourcePin);
						groups.putIfAbsent(sourcePin, pg);
						SiteWire sourceWire = sourcePin.getWire();
						pg = traverseToSinks(sourceWire, pg, edgeWires, direction);
						traverseToDirectConnections(sourcePin, pg, direction);
					}
				}
			}

			return groups;
		}

		private PinGroup traverseToSinks(
				Wire sourceWire, PinGroup pg, Set<Wire> edgeWires, Direction direction
		) {
			Queue<Wire> queue = new LinkedList<>();
			Set<Wire> queued = new HashSet<>();
			queue.offer(sourceWire);
			queued.add(sourceWire);

			while (!queue.isEmpty()) {
				Wire wire = queue.poll();

				List<Connection> conns = direction == Direction.FORWARD ?
						wire.getAllConnections().collect(Collectors.toList()) :
						wire.getAllReverseConnections().collect(Collectors.toList());

				for (Connection c : conns) {
					if (c.isTerminal()) {
						BelPin sinkPin = c.getBelPin();

						// Kind of a special case.  Some inputs to DSPs are driven by
						// VCC and GND coming from a TIEOFF in the tile (not the switch
						// box one).  I'll check if the input comes from the TIEOFF and remove
						// them.
						if (sinkPin.getBel().getSite().getType() == PrimitiveType.TIEOFF)
							continue;

						if (direction == Direction.FORWARD)
							pg.addSinkPin(sinkPin);
						else
							pg.addSourcePin(sinkPin);

						PinGroup old = groups.get(sinkPin);
						if (old == null) {
							groups.put(sinkPin, pg);
						} else {
							pg = merge(pg, old);
						}
					} else {
						Wire sinkWire = c.getSinkWire();
						if (edgeWires.contains(sinkWire)) {
							if (direction == Direction.FORWARD)
								pg.addSinkWire(sinkWire);
							else
								pg.addSourceWire(sinkWire);

							PinGroup old = wireGroups.get(sinkWire);
							if (old == null) {
								wireGroups.put(sinkWire, pg);
							} else {
								pg = merge(pg, old);
							}
						}

						if (!edgeWires.contains(sinkWire) && !queued.contains(sinkWire)) {
							queue.offer(sinkWire);
							queued.add(sinkWire);
						}
					}
				}
			}
			return pg;
		}


		private PinGroup merge(
				PinGroup first, PinGroup second
		) {
			PinGroup newGroup = new PinGroup();
			
			Set<Wire> sourceWires = new HashSet<>(
					first.sourceWires.size() + second.sourceWires.size());
			sourceWires.addAll(first.sourceWires);
			sourceWires.addAll(second.sourceWires);
			newGroup.sourceWires = new ArrayList<>(sourceWires);

			Set<Wire> sinkWires = new HashSet<>(
					first.sinkWires.size() + second.sinkWires.size());
			sinkWires.addAll(first.sinkWires);
			sinkWires.addAll(second.sinkWires);
			newGroup.sinkWires = new ArrayList<>(sinkWires);

			Set<BelPin> sourceBelPins = new HashSet<>(
					first.sourceBelPins.size() + second.sourceBelPins.size());
			sourceBelPins.addAll(first.sourceBelPins);
			sourceBelPins.addAll(second.sourceBelPins);
			newGroup.sourceBelPins = new ArrayList<>(sourceBelPins);

			Set<BelPin> sinkBelPins = new HashSet<>(
					first.sinkBelPins.size() + second.sinkBelPins.size());
			sinkBelPins.addAll(first.sinkBelPins);
			sinkBelPins.addAll(second.sinkBelPins);
			newGroup.sinkBelPins = new ArrayList<>(sinkBelPins);

			Set<Wire> sourceCCs = new HashSet<>(
					first.carryChainSources.size() + second.carryChainSources.size());
			sourceCCs.addAll(first.carryChainSources);
			sourceCCs.addAll(second.carryChainSources);
			newGroup.carryChainSources = new ArrayList<>(sourceCCs);

			Set<Wire> sinkCCs = new HashSet<>(
					first.carryChainSinks.size() + second.carryChainSinks.size());
			sinkCCs.addAll(first.carryChainSinks);
			sinkCCs.addAll(second.carryChainSinks);
			newGroup.carryChainSinks = new ArrayList<>(sinkCCs);

			for (BelPin p : newGroup.sinkBelPins)
				groups.put(p, newGroup);
			for (BelPin p : newGroup.sourceBelPins)
				groups.put(p, newGroup);
			for (Wire w : newGroup.sinkWires)
				wireGroups.put(w, newGroup);
			for (Wire w : newGroup.sourceWires)
				wireGroups.put(w, newGroup);

			return newGroup;
		}

		private void traverseToDirectConnections(
				BelPin sourcePin, PinGroup pg, Direction direction
		) {
			List<DirectConnection> dcs = direction == Direction.FORWARD ?
					template.getDirectSinksOfCluster() :
					template.getDirectSourcesOfCluster();

			PinGroup newPinGroup = new PinGroup();
			for (DirectConnection dc : dcs) {
				if (dc.clusterPin.equals(sourcePin)) {
					if (direction == Direction.FORWARD)
						newPinGroup.addCarryChainSink(dc.clusterExit);
					else
						newPinGroup.addCarryChainSource(dc.clusterExit);
				}
			}

			merge(newPinGroup, pg);
		}

		private enum Direction {
			FORWARD, BACKWARD
		}
	}
}
