package edu.byu.ece.rapidSmith.cad.clusters;

import edu.byu.ece.rapidSmith.cad.clusters.tileCluster.TileClusterGenerator;
import edu.byu.ece.rapidSmith.util.WireTraverser;
import edu.byu.ece.rapidSmith.design.subsite.Connection;
import edu.byu.ece.rapidSmith.design.subsite.SiteWire;
import edu.byu.ece.rapidSmith.design.subsite.TileWire;
import edu.byu.ece.rapidSmith.design.subsite.Wire;
import edu.byu.ece.rapidSmith.device.*;

import java.util.*;

/**
 *
 */
public class ClusterRoutingBuilder {
	public Map<Tile, WireHashMap> forward;
	public Map<Tile, WireHashMap> reverse;

	private Map<Wire, Set<WireConnection>> wireConnectionsMap = new HashMap<>();
	private Map<Wire, Set<WireConnection>> revWireConnectionsMap = new HashMap<>();
	private Set<Wire> wiresDrivingSwitchBox = new HashSet<>();
	private Set<Wire> wiresDrivenBySwitchBox = new HashSet<>();

	private Map<Tile, Tile> tileMap;

	public ClusterRoutingBuilder traverse(List<Tile> instance, Map<Tile, Tile> tileMap) {
		this.tileMap = tileMap;

		CRTraverser sinkTraverser = new CRTraverser(
				true, wiresDrivingSwitchBox, wireConnectionsMap);
		CRTraverser sourceTraverser = new CRTraverser(
				false, wiresDrivenBySwitchBox, revWireConnectionsMap);

		for (Tile tile : instance) {
			for (PrimitiveSite site : tile.getPrimitiveSites()) {
				for (Bel bel : site.getBels()) {
					for (BelPin sourcePin : bel.getSources()) {
						sinkTraverser.run(sourcePin);
					}
					for (BelPin sinkPin : bel.getSinks()) {
						sourceTraverser.run(sinkPin);
					}
				}
			}
		}

		return this;
	}

	public void finish() {
		addSwitchBoxConns(wireConnectionsMap, wiresDrivingSwitchBox, wiresDrivenBySwitchBox);
		addSwitchBoxConns(revWireConnectionsMap, wiresDrivenBySwitchBox, wiresDrivingSwitchBox);

		forward = buildWireHashMap(wireConnectionsMap);
		reverse = buildWireHashMap(revWireConnectionsMap);
	}

	private void addSwitchBoxConns(
			Map<Wire, Set<WireConnection>> connsMap,
			Set<Wire> wiresDriving, Set<Wire> wiresDriven
	) {
		for (Wire driving : wiresDriving) {
			Set<WireConnection> conns = connsMap.computeIfAbsent(driving, k -> new HashSet<>());
			for (Wire driven : wiresDriven) {
				conns.add(getTileWireConnection(driving, driven));
			}
		}
	}

	private Map<Tile, WireHashMap> buildWireHashMap(Map<Wire, Set<WireConnection>> conns) {
		Map<Tile, WireHashMap> whm = new HashMap<>();

		for (Map.Entry<Wire, Set<WireConnection>> e : conns.entrySet()) {
			Set<WireConnection> set = e.getValue();
			WireConnection[] c = new WireConnection[set.size()];
			Iterator<WireConnection> it = set.iterator();
			for (int i = 0; i < set.size(); i++) {
				c[i] = it.next();
			}
			assert !it.hasNext();
			Wire sourceWire = e.getKey();
			whm.computeIfAbsent(sourceWire.getTile(), k -> new WireHashMap())
					.put(sourceWire.getWireEnum(), c);
		}
		return whm;
	}

	private class CRTraverser extends WireTraverser<WireTraverser.WireWrapper> {
		private Set<Wire> exitWires;
		private Map<Wire, Set<WireConnection>> wireConnectionsMap;

		public CRTraverser(
				boolean forward, Set<Wire> exitWires,
				Map<Wire, Set<WireConnection>> wireConnectionsMap
		) {
			super((o1, o2) -> 0);
			this.forward = forward;
			this.exitWires = exitWires;
			this.wireConnectionsMap = wireConnectionsMap;
		}

		public void run(BelPin sourcePin) {
			Wire sourceWire = sourcePin.getWire();
			WireWrapper sourceWrapper = new WireWrapper(sourceWire);
			traverse(Collections.singletonList(sourceWrapper), forward);
		}

		@Override
		public void handleWireConnection(WireWrapper source, Connection c) {
			Wire sinkWire = c.getSinkWire();
			if (tileMap.containsKey(sinkWire.getTile())) {
				Wire translatedSource = translateWire(source.wire(), tileMap);
				if (!isSwitchMatrixWire(source.wire())) {
					if (isSwitchMatrixWire(sinkWire)) {
						exitWires.add(translatedSource);
					} else {
						Wire translatedSink = translateWire(sinkWire, tileMap);
						if (sinkWire instanceof TileWire) {
							wireConnectionsMap
									.computeIfAbsent(translatedSource, (k) -> new HashSet<>())
									.add(getTileWireConnection(translatedSource, translatedSink));
						}
					}
				}

				queueWire(new WireWrapper(sinkWire));
			}
		}

		@Override
		public void handlePinConnection(WireWrapper source, Connection c) {
			queueWire(new WireWrapper(c.getSinkWire()));
		}
	}

	private boolean isSwitchMatrixWire(Wire wire) {
		return TileClusterGenerator.SWITCH_MATRIX_TILES.contains(wire.getTile().getType());
	}

	private WireConnection getTileWireConnection(Wire sourceWire, Wire sinkWire) {
		Tile sourceTile = sourceWire.getTile();
		Tile sinkTile = sinkWire.getTile();

		int yOff = sourceTile.getRow() - sinkTile.getRow();
		int xOff = sourceTile.getColumn() - sinkTile.getColumn();
		return new WireConnection(sinkWire.getWireEnum(), yOff, xOff, true);
	}

	private Wire translateWire(Wire origWire, Map<Tile, Tile> tileMap) {
		Tile translatedTile = tileMap.get(origWire.getTile());
		assert translatedTile != null;

		if (origWire instanceof TileWire) {
			return new TileWire(translatedTile, origWire.getWireEnum());
		} else {
			assert origWire instanceof SiteWire;
			int siteIndex = origWire.getSite().getIndex();
			PrimitiveSite translatedSite = translatedTile.getPrimitiveSite(siteIndex);
			return new SiteWire(translatedSite, origWire.getWireEnum());
		}
	}
}
