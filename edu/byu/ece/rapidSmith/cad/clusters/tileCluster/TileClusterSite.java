package edu.byu.ece.rapidSmith.cad.clusters.tileCluster;

import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.device.Tile;

import java.awt.*;
import java.util.Objects;

/**
 *
 */
public class TileClusterSite implements ClusterSite {
	private Tile tile;
	private Point location;

	public TileClusterSite(Tile tile) {
		this.tile = tile;
		this.location = new Point(tile.getTileXCoordinate(), tile.getTileYCoordinate());
	}

	public Tile getTile() {
		return tile;
	}

	@Override
	public Point getLocation() {
		return location;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TileClusterSite that = (TileClusterSite) o;
		return Objects.equals(tile, that.tile);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tile);
	}

	@Override
	public String toString() {
		return "TileClusterSite{" +
				"tile=" + tile +
				'}';
	}
}
