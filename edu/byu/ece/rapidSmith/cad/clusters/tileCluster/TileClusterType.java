package edu.byu.ece.rapidSmith.cad.clusters.tileCluster;

import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;
import edu.byu.ece.rapidSmith.device.PrimitiveType;
import edu.byu.ece.rapidSmith.device.TileType;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public class TileClusterType extends ClusterType implements Serializable {
	private List<TileType> tileTypes;
	private List<List<PrimitiveType>> mode;

	public TileClusterType(List<TileType> tileTypes, List<List<PrimitiveType>> mode) {
		this.tileTypes = tileTypes;
		this.mode = mode;
	}

	public List<TileType> getTileTypes() {
		return tileTypes;
	}

	public List<List<PrimitiveType>> getMode() {
		return mode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TileClusterType that = (TileClusterType) o;
		return getTileTypes().equals(that.getTileTypes()) &&
				getMode().equals(that.getMode());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getTileTypes()) * 31 + Objects.hash(getMode());
	}

	@Override
	public String toString() {
		return tileTypes.get(0).toString() +  mode;
	}
}
