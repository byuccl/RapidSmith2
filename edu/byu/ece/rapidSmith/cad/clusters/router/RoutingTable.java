package edu.byu.ece.rapidSmith.cad.clusters.router;

import edu.byu.ece.rapidSmith.cad.clusters.ClusterDevice;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterTemplate;
import edu.byu.ece.rapidSmith.cad.clusters.PinGroup;
import edu.byu.ece.rapidSmith.design.subsite.Connection;
import edu.byu.ece.rapidSmith.design.subsite.Wire;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.PinDirection;

import java.util.*;
import java.util.function.BiFunction;

/**
 *
 */
public class RoutingTable {
	public ArrayList<Row> rows;

	public RoutingTable(List<Row> rows) {
		this.rows = new ArrayList<>(rows);
	}

	public static class Row {
		public Map<BelPin, SourcePinEntry> sourcePins = new HashMap<>();
		public Map<BelPin, SinkPinEntry> sinkPins = new HashMap<>();
	}

	public static class SourcePinEntry {
		public boolean drivesGeneralFabric = false;
		public List<BelPin> drivenSinks = new ArrayList<>();
		public List<Wire> drivenClusterPins = new ArrayList<>();
	}

	public static class SinkPinEntry {
		public boolean drivenByGeneralFabric;
		public Wire sourceClusterPin;
		public BelPin sourcePin;
	}

	public static void constructRoutingTables(ClusterDevice<?> device) {
		for (ClusterTemplate<?> template : device.getTemplates()) {
			for (PinGroup pg : template.getPinGroups()) {
				pg.routingTable = buildRouteTable(template, pg);
			}
		}
	}

	public static RoutingTable buildRouteTable(ClusterTemplate<?> template, PinGroup pg) {
		Map<Wire, Set<Wire>> muxes = findMuxes(template, pg);

		ArrayList<Row> tableRows = new ArrayList<>();
		for (Map<Wire, Wire> muxConfiguration : getMuxConfigurations(muxes)) {
			tableRows.add(buildTableRow(pg, muxConfiguration));
		}
		return new RoutingTable(tableRows);
	}

	private static Row buildTableRow(PinGroup pg, Map<Wire, Wire> muxes) {
		Row tableRow = new Row();

		traverseFromSourcePins(pg, muxes, tableRow);
		traverseFromClusterSources(pg, muxes, tableRow);
		traverseFromCarryChainSources(pg, muxes, tableRow);
		reduceTableRowMemory(tableRow);

		return tableRow;
	}

	private static void reduceTableRowMemory(Row tableRow) {
		for (SourcePinEntry entry : tableRow.sourcePins.values()) {
			if (entry.drivenSinks.isEmpty())
				entry.drivenSinks = Collections.emptyList();
			else
				((ArrayList) entry.drivenSinks).trimToSize();

			if (entry.drivenClusterPins.isEmpty())
				entry.drivenClusterPins = Collections.emptyList();
			else
				((ArrayList) entry.drivenClusterPins).trimToSize();
		}
	}

	private static void traverseFromClusterSources(
			PinGroup pg, Map<Wire, Wire> muxes, Row tableRow
	) {
		for (Wire sourceWire : pg.getSourceWires()) {
			Queue<Wire> wireQueue = new LinkedList<>();
			Set<Wire> visitedWires = new HashSet<>();
			wireQueue.add(sourceWire);

			while (!wireQueue.isEmpty()) {
				Wire wire = wireQueue.poll();
				if (!visitedWires.add(wire))
					continue;

				for (Connection c : wire.getWireConnections()) {
					if (c.isRouteThrough())
						continue;

					Wire sinkWire = c.getSinkWire();
					if (pg.getSinkWires().contains(sinkWire) ||
							pg.getCarryChainSinks().contains(sinkWire)) {
						throw new AssertionError("Did not expect any straight entry to exit routes");
					} else if (!muxes.containsKey(sinkWire) || muxes.get(sinkWire).equals(wire)) {
						wireQueue.add(sinkWire);
					}
				}
				for (Connection c : wire.getPinConnections()) {
					wireQueue.add(c.getSinkWire());
				}
				for (Connection c : wire.getTerminals()) {
					BelPin sink = c.getBelPin();
					SinkPinEntry sinkEntry = tableRow.sinkPins.computeIfAbsent(
							sink, k -> new SinkPinEntry());
					assert sinkEntry.sourceClusterPin == null;
					assert sinkEntry.sourcePin == null;
					sinkEntry.sourceClusterPin = sourceWire;
					sinkEntry.drivenByGeneralFabric = true;
				}
			}
		}
	}

	private static void traverseFromSourcePins(
			PinGroup pg, Map<Wire, Wire> muxes, Row tableRow
	) {
		for (BelPin sourcePin : pg.getSourceBelPins()) {
			SourcePinEntry entry = new SourcePinEntry();
			tableRow.sourcePins.put(sourcePin, entry);

			Wire sourceWire = sourcePin.getWire();
			Queue<Wire> wireQueue = new LinkedList<>();
			Set<Wire> visitedWires = new HashSet<>();
			wireQueue.add(sourceWire);

			while (!wireQueue.isEmpty()) {
				Wire wire = wireQueue.poll();
				if (!visitedWires.add(wire))
					continue;

				for (Connection c : wire.getWireConnections()) {
					if (c.isRouteThrough())
						continue;

					Wire sinkWire = c.getSinkWire();
					if (pg.getSinkWires().contains(sinkWire)) {
						entry.drivenClusterPins.add(sinkWire);
						entry.drivesGeneralFabric = true;
					} else if (pg.getCarryChainSinks().contains(sinkWire)) {
						entry.drivenClusterPins.add(sinkWire);
					} else if (!muxes.containsKey(sinkWire) || muxes.get(sinkWire).equals(wire)) {
						wireQueue.add(sinkWire);
					}
				}
				for (Connection c : wire.getPinConnections()) {
					wireQueue.add(c.getSinkWire());
				}
				for (Connection c : wire.getTerminals()) {
					BelPin sink = c.getBelPin();
					entry.drivenSinks.add(sink);
					SinkPinEntry sinkEntry = tableRow.sinkPins.computeIfAbsent(
							sink, k -> new SinkPinEntry());
					assert sinkEntry.sourceClusterPin == null;

					// Correctly handle inout pins.  Out only pins take precedence over input pins
					if (sinkEntry.sourcePin == null) {
						sinkEntry.sourcePin = sourcePin;
					} else {
						if (sinkEntry.sourcePin.getDirection() == PinDirection.OUT) {
							assert sourcePin.getDirection() != PinDirection.OUT;
						} else {
							sinkEntry.sourcePin = sourcePin;
						}
					}
				}
			}
		}
	}

	private static void traverseFromCarryChainSources(
			PinGroup pg, Map<Wire, Wire> muxes, Row tableRow
	) {
		for (Wire sourceWire : pg.getCarryChainSources()) {
			Queue<Wire> wireQueue = new LinkedList<>();
			Set<Wire> visitedWires = new HashSet<>();
			wireQueue.add(sourceWire);

			while (!wireQueue.isEmpty()) {
				Wire wire = wireQueue.poll();
				if (!visitedWires.add(wire))
					continue;

				for (Connection c : wire.getWireConnections()) {
					if (c.isRouteThrough())
						continue;

					Wire sinkWire = c.getSinkWire();
					if (pg.getSinkWires().contains(sinkWire) ||
							pg.getCarryChainSinks().contains(sinkWire)) {
						throw new AssertionError("Did not expect any straight entry to exit routes");
					} else if (!muxes.containsKey(sinkWire) || muxes.get(sinkWire).equals(wire)) {
						wireQueue.add(sinkWire);
					}
				}
				for (Connection c : wire.getPinConnections()) {
					wireQueue.add(c.getSinkWire());
				}
				for (Connection c : wire.getTerminals()) {
					BelPin sink = c.getBelPin();
					SinkPinEntry sinkEntry = tableRow.sinkPins.computeIfAbsent(
							sink, k -> new SinkPinEntry());
					assert sinkEntry.sourceClusterPin == null;
					assert sinkEntry.sourcePin == null;
					sinkEntry.sourceClusterPin = sourceWire;
				}
			}
		}
	}

	private static Map<Wire, Set<Wire>> findMuxes(ClusterTemplate<?> template, PinGroup pg) {
		Map<Wire, Set<Wire>> muxes = new HashMap<>();

		Queue<Wire> queue = new LinkedList<>();
		for (BelPin sinkPin : pg.getSinkPins()) {
			Wire sinkWire = sinkPin.getWire();
			queue.add(sinkWire);
		}
		for (Wire sinkWire : pg.getSinkWires())
			queue.add(sinkWire);
		// TODO still need carry chain sinks, how do I represent them?
//		for (Wire sinkWire : pg.getCarryChainSinks()) {
//
//		}
		// TODO carry chain sinks, but first i think sinks and sources are reversed

		Set<Wire> visited = new HashSet<>();
		while (!queue.isEmpty()) {
			Wire wire = queue.poll();
			if (!visited.add(wire))
				continue;

			for (Connection c : wire.getReverseWireConnections()) {
				if (c.isRouteThrough())
					continue;
				Wire source = c.getSinkWire();
				if (template.getInputs().contains(source))
					continue;
				if (c.isPip()) {
					muxes.computeIfAbsent(wire, k -> new HashSet<>()).add(source);
				}
				queue.add(source);
			}

			for (Connection c : wire.getReversePinConnections()) {
				queue.add(c.getSinkWire());
			}
		}

		removeIf(muxes, (k, v) -> v.size() <= 1);
		return muxes;
	}

	private static <K,V> void removeIf(Map<K, V> map, BiFunction<K, V, Boolean> condition) {
		Iterator<Map.Entry<K, V>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<K, V> e = it.next();
			if (condition.apply(e.getKey(), e.getValue()))
				it.remove();
		}
	}

	private static Iterable<Map<Wire, Wire>> getMuxConfigurations(
			Map<Wire, Set<Wire>> muxes
	) {
		return new MuxConfigurationIterable(muxes);
	}

	private static class MuxConfigurationIterable implements Iterable<Map<Wire, Wire>> {
		private Map<Wire, Set<Wire>> muxes;

		public MuxConfigurationIterable(Map<Wire, Set<Wire>> muxes) {
			this.muxes = muxes;
		}

		@Override
		public Iterator<Map<Wire, Wire>> iterator() {
			return new MuxConfigurationIterator(muxes);
		}
	}

	private static class MuxConfigurationIterator implements Iterator<Map<Wire, Wire>> {
		private List<Integer> status;
		private List<List<Wire>> sources;
		private List<Wire> sinks;
		private boolean exhausted = false;

		public MuxConfigurationIterator(Map<Wire, Set<Wire>> muxes) {
			status = new ArrayList<>();
			sources = new ArrayList<>();
			sinks = new ArrayList<>();

			for (Map.Entry<Wire, Set<Wire>> e : muxes.entrySet()) {
				status.add(0);
				sources.add(new ArrayList<>(e.getValue()));
				sinks.add(e.getKey());
			}
		}

		@Override
		public boolean hasNext() {
			return !exhausted;
		}

		@Override
		public Map<Wire, Wire> next() {
			if (!hasNext())
				throw new NoSuchElementException();

			Map<Wire, Wire> next = buildNext();
			updateStatus();

			return next;
		}

		private void updateStatus() {
			for (int i = status.size()-1; i >= 0; i--) {
				int nextStatus = status.get(i) + 1;
				if (nextStatus >= sources.get(i).size()) {
					status.set(i, 0);
				} else {
					status.set(i, nextStatus);
					return;
				}
			}
			exhausted = true;
		}

		private Map<Wire, Wire> buildNext() {
			Map<Wire, Wire> next = new HashMap<>();
			for (int i = 0; i < status.size(); i++) {
				next.put(sinks.get(i), sources.get(i).get(status.get(i)));
			}
			return next;
		}
	}
}
