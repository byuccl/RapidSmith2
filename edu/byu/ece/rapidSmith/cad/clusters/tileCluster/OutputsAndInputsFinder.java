package edu.byu.ece.rapidSmith.cad.clusters.tileCluster;

import edu.byu.ece.rapidSmith.util.WireTraverser;
import edu.byu.ece.rapidSmith.design.subsite.Connection;
import edu.byu.ece.rapidSmith.design.subsite.TileWire;
import edu.byu.ece.rapidSmith.design.subsite.Wire;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.PrimitiveSite;
import edu.byu.ece.rapidSmith.device.Tile;

import java.util.*;

/**
 *
 */
public class OutputsAndInputsFinder {
	public Map<BelPin, List<Wire>> inputs = new HashMap<>();
	public Set<Wire> outputs = new HashSet<>();

	public void traverse(List<Tile> instance, Map<Tile, Tile> tileMap) {
		Traverser oTraverser = new Traverser(tileMap, true, outputs);

		for (Tile tile : instance) {
			for (PrimitiveSite site : tile.getPrimitiveSites()) {
				for (Bel bel : site.getBels()) {
					for (BelPin pin : bel.getSources()) {
						oTraverser.run(pin);
					}
					for (BelPin pin : bel.getSinks()) {
						Set<Wire> inputsForPin = new HashSet<>();
						new Traverser(tileMap, false, inputsForPin).run(pin);
						inputs.put(translatePin(pin, tileMap), new ArrayList<>(inputsForPin));
					}
				}
			}
		}
	}

	private static class Traverser extends WireTraverser<WireTraverser.WireWrapper> {
		private final Collection<Wire> outputs;
		private Map<Tile, Tile> tileMap;

		public Traverser(Map<Tile, Tile> tileMap, boolean forward, Collection<Wire> outputs) {
			super((k1, k2) -> 0);
			this.tileMap = tileMap;
			this.forward = forward;
			this.outputs = outputs;
		}

		public void run(BelPin sourcePin) {
			Wire sourceWire = sourcePin.getWire();
			WireWrapper wrapper = new WireWrapper(sourceWire);
			traverse(Collections.singletonList(wrapper), forward);
		}

		@Override
		public void handleWireConnection(WireWrapper source, Connection c) {
			Wire sinkWire = c.getSinkWire();
			Tile sinkTile = sinkWire.getTile();

			if (TileClusterGenerator.SWITCH_MATRIX_TILES.contains(sinkTile.getType())) {
				outputs.add(translateWire(source.wire(), tileMap));
			} else if (tileMap.containsKey(sinkTile)) {
				queueWire(new WireWrapper(sinkWire));
			}
		}

		@Override
		public void handlePinConnection(WireWrapper source, Connection c) {
			queueWire(new WireWrapper(c.getSinkWire()));
		}
	}

	private static Wire translateWire(Wire origWire, Map<Tile, Tile> tileMap) {
		Tile translatedTile = tileMap.get(origWire.getTile());
		assert translatedTile != null;

		assert origWire instanceof TileWire;
		return new TileWire(translatedTile, origWire.getWireEnum());
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
}
