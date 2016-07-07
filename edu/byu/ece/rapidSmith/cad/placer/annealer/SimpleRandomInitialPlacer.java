/*
 * Copyright (c) 2010-2011 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.cad.placer.annealer;

import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;

import java.util.*;

/**
 * A Random initial placer. This is a quick and dirty initial placer that will attempt
 * to place a design randomly using a vary naive approach. It will place each instance
 * one at a time at a random location. It will place every group that has a "Free"
 * anchor site. For congested situations, this initial placer may not find a
 * free spot. In this case, it will leave these groups unplaced.
 * 
 * It will not try to place instances for which no free site exists.
 *
 */
public class SimpleRandomInitialPlacer<CTYPE extends ClusterType, CSITE extends ClusterSite>
		implements InitialPlacer<CTYPE, CSITE> {
	/** The random number generator used to determine random placements. */
	protected Random rng;

	/**
	 * Constructor with a Random number generator.
	 */
	public SimpleRandomInitialPlacer(Random rand) {
		this.rng = rand;
	}
	
	/**
	 * Perform an initial placement on the given design. The design may be partially placed
	 * to begin with. It will not move any placed groups and it 
	 * 
	 * Returns 'true' if the initial placement is complete (all groups placed). 
	 * It returns 'false' if one or more groups could not be placed.
	 */
	public boolean initialPlace(PlacerState<CTYPE, CSITE> ps) {
		
		Set<PlacementGroup<CTYPE, CSITE>> groupsToPlace = ps.getGroupsToPlace();

		// See if the design can be placed
		if (groupsToPlace == null)
			return false;
		//if (!ps.canBePlaced()) {
		//	return false;
		//}
		
		// See if placement is necessary
		if (groupsToPlace.size() == 0) {
			if (ps.isMinimumDebug())
				System.out.println(" Design already placed. No initial placement necessary");
			return true;
		}

		// Create a list of groups that need to be placed		
		List<PlacementGroup<CTYPE, CSITE>> sortedGroupsToPlace =
				new ArrayList<>(groupsToPlace);

//		// Remove those groups from the list that have already been placed
		Set<PlacementGroup<CTYPE, CSITE>> groupsNotNeedingPlacement = new HashSet<>();
		for (PlacementGroup<CTYPE, CSITE> pg : sortedGroupsToPlace) {
			if (ps.getGroupAnchorSite(pg) != null)
				groupsNotNeedingPlacement.add(pg);
		}
		// Remove the groups that have already been placed.
		sortedGroupsToPlace.removeAll(groupsNotNeedingPlacement);

		// See if placement is necessary
		if (sortedGroupsToPlace.size() == 0) {
			if (ps.isMinimumDebug())
				System.out.println(" Design already placed. No initial placement necessary");
			return true;
		}

		// Randomize the groups
		Collections.shuffle(sortedGroupsToPlace, this.rng);

		// Flag for indicating that all the groups were successfully placed
		boolean allGroupsPlaced = true;
		
		for (PlacementGroup<CTYPE, CSITE> group : sortedGroupsToPlace) {

			if (group.fixedPlacement()) {
				if (ps.isMediumDebug())
					System.out.println(" Group "+group+" has a fixed placement");
				continue;
			}
			
			if (ps.isMediumDebug())
				System.out.print(group+" "+group.getGroupType()+" ");

			// Determine the number of possible placement anchors and thus the placement probability
			AreaPlacementConstraint<CTYPE, CSITE> constraint = ps.getGroupConstraint(group);
			Set<CSITE> possibleAnchorSites = constraint.getValidAnchorSites(group);
			if (possibleAnchorSites == null || possibleAnchorSites.size() == 0) {
				if (ps.isMediumDebug())
					System.out.println(" - no possible placement site found");
				System.out.println("Warning: no placeable sites for group "+group);
				System.out.println("Constraint:"+constraint.toString());
				allGroupsPlaced = false;
				continue;
			} else {
				if (ps.isMediumDebug())
					System.out.print(" "+possibleAnchorSites.size()+" possible sites");
			}

			// Iterate until a free size has been selected or there are no
			// free sites.
			List<CSITE> orderedAnchorSites = new ArrayList<>(possibleAnchorSites);
			CSITE selectedSite;
			do {
				int selectedSiteNumber = this.rng.nextInt(orderedAnchorSites.size());
				selectedSite = orderedAnchorSites.get(selectedSiteNumber);
				// See if selected site is occupied
				if (ps.isGroupOverlapping(group, selectedSite)) {
					orderedAnchorSites.remove(selectedSite);
					selectedSite = null;
				}
			} while (selectedSite == null && orderedAnchorSites.size() > 0);
			
			// If no site was selected, move on
			if (selectedSite == null) {
				allGroupsPlaced = false;
				if (ps.isMediumDebug())
					System.out.println(" - No Site Selected");
				continue;
			}
			
			if (ps.isMediumDebug())
				System.out.println(" @ "+selectedSite);

			
			// A free site was found. Make the move.
			Set<PlacementGroupMoveDescription<CTYPE, CSITE>> movesSet = new HashSet<>();
			movesSet.add(new PlacementGroupMoveDescription<>(group, null, selectedSite));
			PlacerMove move = new PlacerMove<>(ps, movesSet);
			move.makeMove();				

		}		

		return allGroupsPlaced;		
	}

	

}

