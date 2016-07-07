package edu.byu.ece.rapidSmith.cad.clusters.tileCluster;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterFactory;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileType;

import java.util.*;

/**
 *
 */
public class TileClusterFactory implements ClusterFactory<TileClusterType, TileClusterSite> {
	private TileClusterDevice tcd;
	private Map<List<TileType>, List<TileClusterSite>> locations;

//	private Map<List<TileType>, Integer> numAvailableClusterTypes;
	private static Map<List<TileType>, List<List<TileType>>> sharedTypes;
	private static Map<List<TileType>, List<List<TileType>>> compatibleTypes;
	private Map<List<TileType>, Integer> numUsedClusterType;

	public TileClusterFactory(Device device, TileClusterDevice tcd) {
		this.tcd = tcd;
		locations = new HashMap<>();

		int maxWidth = 0;
		for (TileClusterType type : tcd.getAvailableClusterTypes()) {
			List<TileType> tileTypes = type.getTileTypes();
			maxWidth = Integer.max(maxWidth, tileTypes.size());
			locations.putIfAbsent(tileTypes, new ArrayList<>());
		}

		for (Tile[] tiles : device.getTiles()) {
			for (int i = 0; i < tiles.length; i++) {
				for (int j = 1; j <= maxWidth; j++) {
					List<TileType> tileList = new ArrayList<>();
					for (int k = 0; k < j; k++) {
						if (i + k >= tiles.length || tiles[i + k].getType() == TileType.NULL) {
							tileList = null;
							break;
						}
						Tile tile = tiles[i + k];
						tileList.add(tile.getType());
					}
					if (tileList != null) {
//						numAvailableClusterTypes.computeIfPresent(tileList, (k, v) -> v + 1);
						List<TileClusterSite> sites = locations.get(tileList);
						if (sites != null)
							sites.add(new TileClusterSite(tiles[i]));
					}
				}
			}
		}

		for (List<TileType> base : locations.keySet()) {
			for (List<TileType> descendant : locations.keySet()) {
				if (compatibleTypes.getOrDefault(base, Collections.emptyList()).contains(descendant))
					locations.get(base).addAll(locations.get(descendant));
			}
		}

	}

	@Override
	public void init() {
		numUsedClusterType = new HashMap<>();
		for (List<TileType> tileTypes : locations.keySet())
			numUsedClusterType.put(tileTypes, 0);
	}

	@Override
	public TileClusterTemplate getTemplate(TileClusterType type) {
		return tcd.getCluster(type);
	}

	@Override
	public Collection<TileClusterType> getAvailableClusterTypes() {
		return tcd.getAvailableClusterTypes();
	}

	@Override
	public int getNumRemainingOfType(TileClusterType clusterType) {
		List<TileType> tileType = clusterType.getTileTypes();
		return locations.get(tileType).size() -
				numUsedClusterType.get(tileType);
	}

	@Override
	public TileCluster createNewCluster(String clusterName, TileClusterType type) {
		return new TileCluster(clusterName, getTemplate(type));
	}

	@Override
	public void commitCluster(Cluster<TileClusterType, TileClusterSite> cluster) {
		TileClusterType clusterType = cluster.getType();
		List<TileType> type = clusterType.getTileTypes();
		numUsedClusterType.compute(type, (k, v) -> v + 1);
		assert getNumRemainingOfType(cluster.getType()) >= 0;

		for (List<TileType> sharedType : sharedTypes.getOrDefault(type, Collections.emptyList())) {
			numUsedClusterType.compute(sharedType, (k, v) -> v + 1);
			// These condition shouldn't even be possible to reach
			assert numUsedClusterType.get(type) <= numUsedClusterType.get(sharedType);
			assert locations.get(sharedType).size() >=
					numUsedClusterType.get(sharedType);
		}
	}

	@Override
	public List<TileClusterSite> getLocations(TileClusterType type) {
		return locations.get(type.getTileTypes());
	}

	static {
		sharedTypes = new HashMap<>();
		compatibleTypes = new HashMap<>();

		compatibleTypes.put(Collections.singletonList(TileType.CLBLL),
				Collections.singletonList(Collections.singletonList(TileType.CLBLM)));

		sharedTypes.put(Collections.singletonList(TileType.CLBLM),
				Collections.singletonList(Collections.singletonList(TileType.CLBLL)));
	}
}
