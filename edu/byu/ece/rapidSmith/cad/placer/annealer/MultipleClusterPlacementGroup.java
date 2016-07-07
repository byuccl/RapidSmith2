package edu.byu.ece.rapidSmith.cad.placer.annealer;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This group represents a collection of instances whose placement
 * are relative to each other. All of the instances are of the
 * same type.
 */
public class MultipleClusterPlacementGroup<CTYPE extends ClusterType, CSITE extends ClusterSite>
		extends PlacementGroup<CTYPE, CSITE> {

	public MultipleClusterPlacementGroup() {
		super();
		clusterOffsetMap = new HashMap<>();
	}

	@Override
	public Set<Cluster<CTYPE, CSITE>> getClusters() {
		return clusterOffsetMap.keySet();
	}

	@Override
	public int size() {
		return clusterOffsetMap.size();
	}

	@Override
	public Cluster<CTYPE, CSITE> getAnchor() {
		return anchor;
	}

	@Override
	public Point getDimension() {
		return groupDimension;
	}

	@Override
	public Point getClusterOffset(Cluster<CTYPE, CSITE> i) {
		return clusterOffsetMap.get(i);
	}

	@Override
	public CTYPE getGroupType() {
		return groupType;
	}


	@Override
	public Set<CSITE> getGroupSites(
			CSITE anchorSite, AreaPlacementConstraint<CTYPE, CSITE> coord
	) {
		HashSet<CSITE> sites = new HashSet<>(getClusters().size());
		for (Cluster<CTYPE, CSITE> i : getClusters()) {
			CSITE s = getClusterSite(i, anchorSite, coord);
			//if (s == null)
			//	System.out.println("Warning: a null site for group "+this+", instance "+i.getName()+"" +
			//			" at site "+anchorSite);
			sites.add(s);
		}
		return sites;
	}

	@Override
	public CSITE getClusterSite(
			Cluster<CTYPE, CSITE> i, CSITE anchorSite,
			AreaPlacementConstraint<CTYPE, CSITE> constraint
	) {
		if (anchorSite == null) {
			System.out.println("Warning: requesting an instance site with a null anchor site");
			return null;
		}
		if (anchor == null) {
			System.out.println("Warning: null anchor");
			return null;
		}

		Cluster<CTYPE, CSITE> anchor = getAnchor();
		if (anchor == i)
			return anchorSite;
		Point instanceOffset = getClusterOffset(i);
		return constraint.getSiteOffset(anchorSite, instanceOffset.x, instanceOffset.y);
	}

	@Override
	public PlacementAlignment<CSITE> getAlignment() {
		return alignment;
	}

	@Override
	public String toString() {
		return this.anchor.getName() + " (" + clusterOffsetMap.size() + ")";
	}

	/**
	 * The placement alignment object associated with this group.
	 */
	public PlacementAlignment<CSITE> alignment;

	/**
	 * A map between an instance of the group and its offset relative
	 * to the group anchor.
	 */
	public Map<Cluster<CTYPE, CSITE>, Point> clusterOffsetMap;

	/**
	 * The anchor instance of the group. This should have an offset of 0,0.
	 */
	public Cluster<CTYPE, CSITE> anchor;

	/**
	 * Indicates the dimensions of the group in both the x and y locations.
	 */
	public Point groupDimension;

	public CTYPE groupType;
}
