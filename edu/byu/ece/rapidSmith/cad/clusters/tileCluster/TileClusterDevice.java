package edu.byu.ece.rapidSmith.cad.clusters.tileCluster;

import edu.byu.ece.rapidSmith.cad.clusters.ClusterDevice;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterTemplate;
import edu.byu.ece.rapidSmith.device.*;

import java.util.*;

/**
 *
 */
public class TileClusterDevice extends ClusterDevice<TileClusterType> {
	public static final String CURRENT_VERSION = "1.2";
	private String version = CURRENT_VERSION;

	/** A 2D array of all the tiles in the device */
	private Map<TileClusterType, TileClusterTemplate> tileClusters;

	@Override
	public Set<TileClusterType> getAvailableClusterTypes() {
		return tileClusters.keySet();
	}

	@Override
	public TileClusterTemplate getCluster(TileClusterType type) {
		return tileClusters.get(type);
	}

	@Override
	public Collection<ClusterTemplate<TileClusterType>> getTemplates() {
		@SuppressWarnings("unchecked")
		Collection<ClusterTemplate<TileClusterType>> cast =
				(Collection<ClusterTemplate<TileClusterType>>) (Collection<?>) tileClusters.values();
		return cast;
	}

	public void setTileClusters(Map<TileClusterType, TileClusterTemplate> tileClusters) {
		this.tileClusters = tileClusters;
	}

	/*
	   For Hessian compression.  Avoids writing duplicate data.
	 */
	protected static class TCDReplace extends DeviceReplace {
		private List<TileClusterTemplate> templates;
		private Map<PrimitiveType, WireHashMap> reverseSiteWireMaps;
		private Map<String, WireHashMap> reverseTileWireMaps;
		private String version;

		@SuppressWarnings("UnusedDeclaration")
		private TileClusterDevice readResolve() {
			TileClusterDevice tcd = new TileClusterDevice();
			super.readResolve(tcd);
			tcd.tileClusters = new HashMap<>();
			for (TileClusterTemplate tct : templates) {
				tcd.tileClusters.put(tct.getType(), tct);
			}
			tcd.version = version;
			reverseSiteWireMaps.forEach((t, m) ->
					tcd.getSiteTemplate(t).setReverseWireConnections(m)
			);
			reverseTileWireMaps.forEach((t, m) ->
					tcd.getTile(t).setReverseWireConnections(m));
			return tcd;
		}
	}

	@SuppressWarnings("unused")
	private TCDReplace writeReplace() {
		TCDReplace repl = new TCDReplace();
		super.writeReplace(repl);
		repl.templates = new ArrayList<>(tileClusters.values());
		repl.version = version;
		repl.reverseSiteWireMaps = new HashMap<>();
		for (SiteTemplate siteTemplate : getSiteTemplates().values()) {
			repl.reverseSiteWireMaps.put(siteTemplate.getType(),
					siteTemplate.getReversedWireHashMap());
		}
		repl.reverseTileWireMaps = new HashMap<>();
		for (Tile[] tiles : getTiles()) {
			for (Tile tile : tiles) {
				repl.reverseTileWireMaps.put(tile.getName(),
						tile.getReverseWireHashMap());
			}
		}

		return repl;
	}
}
