package edu.byu.ece.rapidSmith.cad.placer.annealer;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;

/**
 * Represents a potential move of a single group. Contains the new and
 * old locations of the group.
 * <p>
 * Note that the new site may be null. This indicates that the group should be "unplaced"
 * after the move. If the old site is null, it indicates that the group is currently
 * unplaced. Either the old site or the new site should be non-null indicating a change
 * in the placement status of the group.
 */
public class PlacementGroupMoveDescription<CTYPE extends ClusterType, CSITE extends ClusterSite> {

	/**
	 * Create a new placement move for a given group.
	 */
	public PlacementGroupMoveDescription(
			PlacementGroup<CTYPE, CSITE> group, CSITE oldSite, CSITE newSite
	) {
		this.group = group;
		this.newSite = newSite;
		this.oldSite = oldSite;
	}

	public static <CTYPE extends ClusterType, CSITE extends ClusterSite>
	PlacementGroupMoveDescription<CTYPE, CSITE> createUnplaceMove(
			PlacementGroup<CTYPE, CSITE> group, CSITE oldSite
	) {
		return new PlacementGroupMoveDescription<>(group, oldSite, null);
	}

	public static <CTYPE extends ClusterType, CSITE extends ClusterSite>
	PlacementGroupMoveDescription createInitialPlaceMove(
			PlacementGroup<CTYPE, CSITE> group, CSITE newSite
	) {
		return new PlacementGroupMoveDescription<>(group, null, newSite);
	}

	public PlacementGroup<CTYPE, CSITE> getGroup() {
		return group;
	}

	public CSITE getNewSite() {
		return newSite;
	}

	public CSITE getOldSite() {
		return oldSite;
	}

	public String toString() {
		return "Moving Group " + group + " from " + oldSite + " to " + newSite;
	}

	/** Group represented by the move. */
	protected PlacementGroup<CTYPE, CSITE> group;

	/**
	 * The proposed new site location of the group. This can be null which would
	 * indicate that the proposed move is to "unplace" the object.
	 */
	protected CSITE newSite;

	/**
	 * The current location of the group. This can be null which would
	 * indicate that the group has not yet been placed.
	 */
	protected CSITE oldSite;

}
