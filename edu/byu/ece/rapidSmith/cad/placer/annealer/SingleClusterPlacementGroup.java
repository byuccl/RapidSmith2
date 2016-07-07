package edu.byu.ece.rapidSmith.cad.placer.annealer;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;

import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A placement group that consists of a single instance.
 */
public class SingleClusterPlacementGroup<CTYPE extends ClusterType, CSITE extends ClusterSite>
		extends PlacementGroup<CTYPE, CSITE> {

	/** Primary constructor */
	public SingleClusterPlacementGroup(Cluster<CTYPE, CSITE> i) {
		super();
		clusters = Collections.singleton(i);
		singleCluster = i;
	}

	public Set<Cluster<CTYPE, CSITE>> getClusters() {
		return clusters;
	}

	public int size() {
		return 1;
	}

	public Cluster<CTYPE, CSITE> getCluster() {
		return singleCluster;
	}


	/**
	 * This shouldn't be called. If so, create a new alignment block
	 */
	public PlacementAlignment<CSITE> getAlignment() {
		return PlacementAlignment.getDefaultAlignment();
	}

	public Point getDimension() {
		return DEFAULT_DIMENSION;
	}

	public CTYPE getGroupType() {
		return singleCluster.getType();
	}

	public Cluster<CTYPE, CSITE> getAnchor() {
		return singleCluster;
	}

	public Point getClusterOffset(Cluster<CTYPE, CSITE> i) {
		return DEFAULT_POINT;
	}

	public CSITE getClusterSite(
			Cluster<CTYPE, CSITE> i, CSITE anchor,
			AreaPlacementConstraint<CTYPE, CSITE> coord
	) {
		if (i == singleCluster)
			return anchor;
		return null;
	}

	public Set<CSITE> getGroupSites(
			CSITE anchor, AreaPlacementConstraint<CTYPE, CSITE> coord
	) {
		Set<CSITE> s = new HashSet<>(1);
		s.add(anchor);
		return s;
	}

	public String toString() {
		return singleCluster.getName();
	}

	protected Set<Cluster<CTYPE, CSITE>> clusters = null;
	protected Cluster<CTYPE, CSITE> singleCluster = null;
	protected static final Point DEFAULT_POINT = new Point(0, 0);
	protected static final Point DEFAULT_DIMENSION = new Point(1, 1);
}
