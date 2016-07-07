package edu.byu.ece.rapidSmith.cad.clusters.tileCluster;

import edu.byu.ece.rapidSmith.cad.clusters.ClusterConnection;
import edu.byu.ece.rapidSmith.util.WireTraverser;
import edu.byu.ece.rapidSmith.design.subsite.Connection;
import edu.byu.ece.rapidSmith.design.subsite.Wire;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.PrimitiveSite;
import edu.byu.ece.rapidSmith.device.Tile;

import java.util.*;

/**
 *
 */
public class ClusterConnectionsBuilder {
	public Map<BelPin, List<ClusterConnection>> sourcesOfSinks = new HashMap<>();
	public Map<BelPin, List<ClusterConnection>> sinksOfSources = new HashMap<>();

	public ClusterConnectionsBuilder findSourcesAndSinks(
			List<Tile> instance, Map<Tile, Tile> tileMap
	) {
		for (Tile tile : instance) {
			for (PrimitiveSite site : tile.getPrimitiveSites()) {
				for (Bel bel : site.getBels()) {
					for (BelPin sourcePin : bel.getSources()) {
						BelPin translatedPin = translatePin(sourcePin, tileMap);
						ArrayList<ClusterConnection> ccs = new Traverser(tileMap, true).run(sourcePin);
						ccs.trimToSize();
						sinksOfSources.put(translatedPin, ccs);
					}
					for (BelPin sinkPin : bel.getSinks()) {
						BelPin translatedPin = translatePin(sinkPin, tileMap);
						ArrayList<ClusterConnection> ccs = new Traverser(tileMap, false).run(sinkPin);
						ccs.trimToSize();
						sourcesOfSinks.put(translatedPin, ccs);
					}
				}
			}
		}
		return this;
	}


	private static BelPin translatePin(BelPin sourcePin, Map<Tile, Tile> tileMap) {
		Bel sourceBel = sourcePin.getBel();
		PrimitiveSite sourceSite = sourceBel.getSite();
		Tile sourceTile = sourceSite.getTile();

		Tile translatedTile = tileMap.get(sourceTile);
		PrimitiveSite translatedSite = translatedTile.getPrimitiveSite(sourceSite.getIndex());
		Bel translatedBel = translatedSite.getBel(sourceBel.getId());
		return translatedBel.getBelPin(sourcePin.getName());
	}

	private static class Traverser extends WireTraverser<CCTWire> {
		private ArrayList<ClusterConnection> connections;
		private Map<Tile, Tile> tileMap;
		private static final Comparator<CCTWire> comparator =
				Comparator.comparing(CCTWire::leavesSite)
				.thenComparing(CCTWire::getDistance);

		public Traverser(Map<Tile, Tile> tileMap, boolean forward) {
			super(comparator);
			this.tileMap = tileMap;
			this.forward = forward;
		}

		public ArrayList<ClusterConnection> run(BelPin sourcePin) {
			connections = new ArrayList<>();
			Wire sourceWire = sourcePin.getWire();
			CCTWire wrapper = new CCTWire(sourceWire);
			traverse(Collections.singletonList(wrapper), forward);
			return connections;
		}

		@Override
		public void handleWireConnection(CCTWire source, Connection c) {
			Wire sinkWire = c.getSinkWire();
			if (tileMap.containsKey(sinkWire.getTile())) {
				int distance = source.distance;
				if (c.isPip())
					distance += 1;
				CCTWire wrapper = new CCTWire(sinkWire, source.leavesSite, distance);
				queueWire(wrapper);
			}
		}

		@Override
		public void handlePinConnection(CCTWire source, Connection c) {
			queueWire(new CCTWire(c.getSinkWire(), true, source.distance));
		}

		@Override
		public void handleTerminals(CCTWire source, Connection c) {
			BelPin translatedPin = translatePin(c.getBelPin(), tileMap);
			connections.add(new ClusterConnection(
					translatedPin, !source.leavesSite, source.distance));
		}
	}

	private static class CCTWire extends WireTraverser.WireWrapper {
		public boolean leavesSite;
		public int distance;

		public CCTWire(Wire wire) {
			super(wire);
			leavesSite = false;
			distance = 0;
		}

		public CCTWire(Wire sinkWire, boolean leavesSite, int distance) {
			super(sinkWire);
			this.leavesSite = leavesSite;
			this.distance = distance;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CCTWire cctWire = (CCTWire) o;
			return Objects.equals(wire(), cctWire.wire());
		}

		@Override
		public int hashCode() {
			return Objects.hash(wire());
		}

		public boolean leavesSite() {
			return leavesSite;
		}

		public int getDistance() {
			return distance;
		}
	}
}
