package edu.byu.ece.rapidSmith.cad.placer.annealer;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;

import java.awt.*;
import java.util.Set;

/**
 * Represents the atomic unit for placement. Because of placement constraints
 * between instances (i.e., instances of a carry chain), multiple instances
 * may be part of an atomic placement group. In other words, all instances
 * of a given placement group have a specific relative placement constraints
 * and when one of the instances are placed, all of the instances in the group
 * are correspondingly placed.
 * <p>
 * This class does not maintain ANY placement information. All of the
 * placement information is stored in the PlacerShapeState.
 */
public abstract class PlacementGroup<CTYPE extends ClusterType, CSITE extends ClusterSite> {
	/** Default constructor */
	public PlacementGroup() {
	}

	/**
	 * Get all instances associated with the group
	 */
	public abstract Set<Cluster<CTYPE, CSITE>> getClusters();

	/**
	 * Return the anchor instance of the group;
	 */
	public abstract Cluster<CTYPE, CSITE> getAnchor();

	/**
	 * Return the dimensions of the group (size in x and y coordinates)
	 */
	public abstract Point getDimension();

	/**
	 * Return the number of instances elements in the group
	 */
	public abstract int size();

	/**
	 * Return the offset of instance i within the group.
	 */
	public abstract Point getClusterOffset(Cluster<CTYPE, CSITE> i);

	/**
	 * This method will return the primitive site for an instance if the entire
	 * placement group is placed at the location specified by the parameter.
	 */
	public abstract CSITE getClusterSite(
			Cluster<CTYPE, CSITE> i, CSITE anchor, AreaPlacementConstraint<CTYPE, CSITE> constraint);

	/**
	 * This method will return a set of sites that the group will occupy if placed
	 * at the given anchor.
	 */
	public abstract Set<CSITE> getGroupSites(
			CSITE anchor, AreaPlacementConstraint<CTYPE, CSITE> constraint);

	/** Determine the PrimitiveType of the group. */
	public abstract CTYPE getGroupType();

	/**
	 * Return the alignment object associated with this placement group.
	 */
	public abstract PlacementAlignment<CSITE> getAlignment();

	/**
	 * Indicates that the group's placement is fixed and cannot be changed.
	 */
	public boolean fixedPlacement() {
		return false;
	}

}

