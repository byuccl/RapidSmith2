package edu.byu.ece.rapidSmith.cad.clusters.tileCluster;

import edu.byu.ece.rapidSmith.cad.clusters.DirectConnection;
import edu.byu.ece.rapidSmith.util.WireTraverser;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;

import java.util.*;

/**
 *
 */
public class DirectSourceAndSinkFinder {
	public List<DirectConnection> sources = new ArrayList<>();
	public List<DirectConnection> sinks = new ArrayList<>();

	private Map<Tile, Integer> tileIndexes;
	private Map<Integer, TilePinPair> newTiles;
	private Map<Integer, Map<String, Integer>> belPinMap = new HashMap<>();
	private int nextUnusedIndex;

	public DirectSourceAndSinkFinder() {
		nextUnusedIndex = 0;
		belPinMap.values().forEach(v -> {
			int max = v.values().stream().mapToInt(Integer::valueOf).max().getAsInt();
			if (nextUnusedIndex <= max)
				nextUnusedIndex = max + 1;
		});
	}

	private static final class TilePinPair {
		public Tile tile;
		public BelPin sourcePin;
		public List<DirectConnection> dc = new ArrayList<>();

		public TilePinPair(Tile tile, BelPin sourcePin) {
			this.tile = tile;
			this.sourcePin = sourcePin;
		}
	}

	public DirectSourceAndSinkFinder findSourcesAndSinks(
			List<Tile> instance, Map<Tile, Tile> tileMap
	) {
		tileIndexes = new HashMap<>();
		newTiles = new HashMap<>();

		Traverser sinksTraverser = new Traverser(tileMap, sinks, true);
		Traverser sourceTraverser = new Traverser(tileMap, sources, false);

		for (Tile tile : instance) {
			for (PrimitiveSite site : tile.getPrimitiveSites()) {
				for (Bel bel : site.getBels()) {
					for (BelPin sourcePin : bel.getSources()) {
						sinksTraverser.run(sourcePin);
					}
					for (BelPin sinkPin : bel.getSinks()) {
						sourceTraverser.run(sinkPin);
					}
				}
			}
		}
		return this;
	}

	private class Traverser extends WireTraverser<ExitWireWrapper> {
		public List<DirectConnection> dcs;
		public Map<Tile, Tile> tileMap;
		private BelPin untranslatedSourcePin;
		private BelPin translatedSourcePin;
		private Wire sourceWire;

		public Traverser(
				Map<Tile, Tile> tileMap, List<DirectConnection> dcs, boolean forward
		) {
			super((k1, k2) -> 0);
			this.tileMap = tileMap;
			this.dcs = dcs;
			this.forward = forward;
		}

		public void run(BelPin sourcePin) {
			untranslatedSourcePin = sourcePin;
			translatedSourcePin = translateBelPin(sourcePin, tileMap);
			sourceWire = sourcePin.getWire();
			ExitWireWrapper sourceWrapper = new ExitWireWrapper(sourceWire);
			traverse(Collections.singletonList(sourceWrapper), forward);
		}

		@Override
		public void handleWireConnection(ExitWireWrapper source, Connection c) {
			Wire sink = c.getSinkWire();
			if (TileClusterGenerator.SWITCH_MATRIX_TILES.contains(sink.getTile().getType()))
				return;
			Wire exitWire = source.clusterExit;
			if (exitWire == null && !tileMap.containsKey(sink.getTile()))
				exitWire = translateWire(source.wire(), tileMap);
			queueWire(new ExitWireWrapper(sink, source.exitedSite, exitWire));
		}

		@Override
		public void handlePinConnection(ExitWireWrapper source, Connection c) {
			PrimitiveSite sinkSite = c.getSitePin().getSite();
			Tile sinkTile = sinkSite.getTile();
			if (!source.exitedSite) {
				ExitWireWrapper wrapper = new ExitWireWrapper(c.getSinkWire(), true, null);
				assert source.clusterExit == null;
				queueWire(wrapper);
			} else if (source.clusterExit != null) {
				PrimitiveType defaultType = sinkSite.getType();
				for (PrimitiveType type : sinkSite.getPossibleTypes()) {
					sinkSite.setType(type);
					SitePin typedSitePin = sinkTile.getSitePinOfWire(source.wire().getWireEnum());
					if (typedSitePin == null)
						continue;
					Wire siteWire = typedSitePin.getInternalWire();
					ExitWireWrapper wrapper = new ExitWireWrapper(
							siteWire, true, source.clusterExit);
					SinkSiteTraverser sinkSiteTraverser = new SinkSiteTraverser(
							dcs, untranslatedSourcePin, translatedSourcePin);
					sinkSiteTraverser.traverse(
							Collections.singletonList(wrapper), forward);
				}
				sinkSite.setType(defaultType);
			}
		}

		private BelPin translateBelPin(BelPin origPin, Map<Tile, Tile> tileMap) {
			Bel origBel = origPin.getBel();
			PrimitiveSite origSite = origBel.getSite();
			Tile origTile = origSite.getTile();

			Tile translatedTile = tileMap.get(origTile);
			PrimitiveSite translatedSite = translatedTile.getPrimitiveSite(origSite.getIndex());
			Bel translatedBel = translatedSite.getBel(origBel.getId());
			return translatedBel.getBelPin(origPin.getName());
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

	private class SinkSiteTraverser extends WireTraverser<ExitWireWrapper> {
		private List<DirectConnection> dcs;
		private BelPin untranslatedSourcePin;
		private BelPin translatedSourcePin;

		public SinkSiteTraverser(
				List<DirectConnection> dcs, BelPin untranslatedSourcePin, BelPin translatedSource
		) {
			super((k1, k2) -> 0);
			this.dcs = dcs;
			this.untranslatedSourcePin = untranslatedSourcePin;
			this.translatedSourcePin = translatedSource;
		}

		@Override
		public void handleWireConnection(ExitWireWrapper source, Connection c) {
			queueWire(new ExitWireWrapper(c.getSinkWire(), true, source.clusterExit));
		}

		@Override
		public void handlePinConnection(ExitWireWrapper source, Connection c) {
			assert false : "Should be no entrances to exits.";
		}

		@Override
		public void handleTerminals(ExitWireWrapper source, Connection c) {
			BelPin sinkPin = c.getBelPin();
			Integer sinkTileIndex = getSinkIndex(sinkPin);
			DirectConnection dc = new DirectConnection(
					sinkPin, translatedSourcePin, source.clusterExit, sinkTileIndex
			);
			dcs.add(dc);
			TilePinPair pair = newTiles.get(sinkTileIndex);
			if (pair != null) {
				pair.dc.add(dc);
			}
		}

		private Integer getSinkIndex(BelPin sinkPin) {
			PrimitiveSite sinkSite = sinkPin.getBel().getSite();
			Tile sinkTile = sinkSite.getTile();
			Integer sinkTileIndex = getBelPinMappedIndex(sinkPin);

			if (sinkTileIndex == null) {
				if (tileIndexes.containsKey(sinkTile)) {
					sinkTileIndex = tileIndexes.get(sinkTile);
				} else {
					sinkTileIndex = nextUnusedIndex++;
					tileIndexes.put(sinkTile, sinkTileIndex);
					TilePinPair pair = new TilePinPair(sinkTile, untranslatedSourcePin);
					newTiles.put(sinkTileIndex, pair);
				}
				setBelPinMappedIndex(sinkPin, sinkTileIndex);
			} else {
				Integer existingTileIndex = tileIndexes.get(sinkSite.getTile());
				if (existingTileIndex != null) {
					if (!existingTileIndex.equals(sinkTileIndex)) {
						if (newTiles.containsKey(existingTileIndex)) {
							TilePinPair tilePinPair = newTiles.get(existingTileIndex);
							BelPin oldSource = tilePinPair.sourcePin;
							if (untranslatedSourcePin.equals(oldSource)) {
								sinkTileIndex = null;
								updateIndices(tilePinPair, null);
								newTiles.remove(existingTileIndex);
								tilePinPair.sourcePin = untranslatedSourcePin;
							} else {
								updateIndices(tilePinPair, sinkTileIndex);
								newTiles.remove(existingTileIndex);
							}
						} else {
							assert false : "I can't do this";
						}
					}
				} else {
					tileIndexes.put(sinkTile, sinkTileIndex);
				}
			}
			return sinkTileIndex;
		}

		private void updateIndices(TilePinPair pair, Integer newIndex) {
			for (DirectConnection dc : pair.dc) {
				dc.endTileIndex = newIndex;
			}
			tileIndexes.put(pair.tile, newIndex);
		}

		private Integer getBelPinMappedIndex(BelPin sinkPin) {
			Integer sinkIndex = null;

			PrimitiveSite sinkSite = sinkPin.getBel().getSite();
			int sinkSiteIndex = sinkSite.getIndex();
			String sinkPinName = sinkPin.getName();

			if (belPinMap.containsKey(sinkSiteIndex)) {
				Map<String, Integer> map = belPinMap.get(sinkSiteIndex);
				if (map.containsKey(sinkPinName)) {
					sinkIndex = map.get(sinkPinName);
				}
			}
			return sinkIndex;
		}

		private void setBelPinMappedIndex(BelPin sinkPin, Integer index) {
			PrimitiveSite sinkSite = sinkPin.getBel().getSite();
			belPinMap.computeIfAbsent(sinkSite.getIndex(), k -> new HashMap<>())
					.put(sinkPin.getName(), index);
		}
	}

	public static class ExitWireWrapper extends WireTraverser.WireWrapper {
		public boolean exitedSite;
		public Wire clusterExit;

		public ExitWireWrapper(Wire wire) {
			super(wire);
		}

		public ExitWireWrapper(Wire wire, boolean exitedSite, Wire clusterExit) {
			super(wire);
			this.exitedSite = exitedSite;
			this.clusterExit = clusterExit;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ExitWireWrapper that = (ExitWireWrapper) o;
			return Objects.equals(wire(), that.wire());
		}

		@Override
		public int hashCode() {
			return wire().hashCode();
		}
	}

}
