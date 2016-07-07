package edu.byu.ece.rapidSmith.cad.placer.annealer;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an atomic move between one or more PlacementGroup objects.
 * In some cases, this will be a single object that is being moved from its
 * current location to an empty, available location. In other cases, this
 * move will involve multiple groups (usually a swap between groups at
 * different locations). The set of moves should have already been
 * checked for validity (i.e., that the atomc set of moves are
 * mutually compatible).
 *
 * @author Mike Wirthlin
 */
public class PlacerMove<CTYPE extends ClusterType, CSITE extends ClusterSite> {

	/**
	 * Constructor for a set of moves.
	 */
	public PlacerMove(
			PlacerState<CTYPE, CSITE> state,
			Set<? extends PlacementGroupMoveDescription<CTYPE, CSITE>> moves) {
		moveMade = false;
		moveUndone = false;
		this.state = state;
		this.moves = moves;
	}

	/**
	 * Constructor for a single placement move.
	 */
	public PlacerMove(
			PlacerState<CTYPE, CSITE> state,
			PlacementGroup<CTYPE, CSITE> group, CSITE newSite
	) {
		CSITE oldSite = state.getGroupAnchorSite(group);
		PlacementGroupMoveDescription<CTYPE, CSITE> move =
				new PlacementGroupMoveDescription<>(group, oldSite, newSite);
		Set<PlacementGroupMoveDescription<CTYPE, CSITE>> movesSet = Collections.singleton(move);

		moveMade = false;
		moveUndone = false;
		this.state = state;
		this.moves = movesSet;
	}

	/**
	 * A method for debugging a move.
	 */
	public void debugMove() {
		System.out.println("   Valid Move Details:");
		for (PlacementGroupMoveDescription<CTYPE, CSITE> move : moves) {
			CSITE newGroupSite = move.getNewSite();
			CSITE oldGroupSite = move.getOldSite();
			PlacementGroup<CTYPE, CSITE> group = move.getGroup();
			System.out.println("    Group " + group.getAnchor().getName());
			System.out.println("      was placed at " + oldGroupSite + " will be placed at " + newGroupSite);
		}
	}

	/**
	 * Actually place all Clusters specified in this move at their new PrimitiveSites
	 */
	public void makeMove() {
		if (moveMade) {
			System.out.println("Move already made");
			return;
		}
		state.placeGroupsOfMove(this, false);
		moveMade = true;
		if (state.isMaximumDebug())
			System.out.println(" Move Made");
	}

	/**
	 * Place all Clusters of this move back at their previous PrimitiveSites
	 */
	public void undoMove() {
		if (!moveMade) {
			System.out.println("Move not made");
			return;
		}
		state.placeGroupsOfMove(this, true);
		moveMade = false;
		moveUndone = true;
		if (state.isMaximumDebug())
			System.out.println(" Move Undo");
	}

	public Set<? extends PlacementGroupMoveDescription<CTYPE, CSITE>> getMoves() {
		return moves;
	}

	/**
	 * Returns all of the Clusters that are part of this PlacerMove
	 */
	public Set<Cluster<CTYPE, CSITE>> getCluster() {
		Set<Cluster<CTYPE, CSITE>> moveClusters = new HashSet<>();
		for (PlacementGroupMoveDescription<CTYPE, CSITE> move : moves) {
			PlacementGroup<CTYPE, CSITE> group = move.getGroup();
			moveClusters.addAll(group.getClusters());
		}
		return moveClusters;
	}

	public Set<PlacementGroup<CTYPE, CSITE>> getGroups() {
		Set<PlacementGroup<CTYPE, CSITE>> moveGroups = new HashSet<>();
		for (PlacementGroupMoveDescription<CTYPE, CSITE> move : moves) {
			PlacementGroup<CTYPE, CSITE> group = move.getGroup();
			moveGroups.add(group);
		}
		return moveGroups;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Move Made=" + moveMade + " move Undone=" + moveUndone + "\n");
		for (PlacementGroupMoveDescription move : moves)
			sb.append(" " + move + "\n");
		return sb.toString();
	}

	/**
	 * A set of all individual group moves that make up this "swap". This
	 * may be a single group or multiple groups.
	 */
	protected Set<? extends PlacementGroupMoveDescription<CTYPE, CSITE>> moves;

	protected boolean moveMade;

	protected boolean moveUndone;

	protected PlacerState<CTYPE, CSITE> state;


}
