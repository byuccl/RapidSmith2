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
 * to place a design randomly using a vary naive approach.
 */
public class DisplacementRandomInitialPlacer<CTYPE extends ClusterType, CSITE extends ClusterSite>
		extends SimpleRandomInitialPlacer<CTYPE, CSITE>
{

	public DisplacementRandomInitialPlacer(Random rand) {
		super(rand);
	}
	
	public boolean initialPlace(PlacerState<CTYPE, CSITE> s) {

		// Attempt a simple place first. If it is successful, don't continue.
		boolean initialPlace = super.initialPlace(s);
		if (initialPlace)
			return true;

		// See if design can be placed
		if (!s.canBePlaced)
			return false;
		
		// Must displace some instances to make room for other instances
		System.out.println("Displacement initial placement required ("+
				s.getUnplacedPlacementGroups().size()+" of " + s.getPlacementGroups().size() +
				" groups displaced)");
		
		if (s.isMediumDebug()) {
			System.out.println(" Placed groups:");
			for (PlacementGroup<CTYPE, CSITE> g : s.getPlacedPlacementGroups())
				System.out.println("\t"+g);
			System.out.println(" Unplaced groups:");
			for (PlacementGroup<CTYPE, CSITE> g : s.getUnplacedPlacementGroups())
				System.out.println("\t"+g);
			System.out.println(" Placement Sites:");
			for (CSITE site : s.getSitesUsed())
				System.out.println("\t"+site+" "+s.getPlacedGroup(site));
		}
		
		// Create a priority queue of placement groups that need to be placed.
		// Initialize the queue 
		PriorityQueue<PlacementGroup<CTYPE, CSITE>> unplacedGroupQueue =
			new PriorityQueue<>(s.getUnplacedPlacementGroups().size(),
					new PlacementGroupPlacementComparator<>(s));

		// Determine the unplaced groups and add to priority queue
		for (PlacementGroup<CTYPE, CSITE> g : s.getUnplacedPlacementGroups())
			unplacedGroupQueue.add(g);
		
		// Create an object representing the cost of sites
		// TODO does this need to be generic?
		SiteCost siteCost = new SiteCost(s);
		
		/////////////////////////////////////////////////////////////////////////////////////////
		// Iterate over all elements in the queue of Instances to place until the queue is empty
		/////////////////////////////////////////////////////////////////////////////////////////
		//int oldDebug = s.DEBUG;
		//s.DEBUG = s.DEBUG_MEDIUM;
		
		int iteration = 0;
		int maxIterations = unplacedGroupQueue.size() * 1000;
		Map<PlacementGroup<CTYPE, CSITE>, Integer> groupReplacementCountMap = new HashMap<>();
		do {
			
			// Get the highest probability group in the list
			PlacementGroup<CTYPE, CSITE> groupToPlace = unplacedGroupQueue.remove();
			Integer groupReplaceCount = groupReplacementCountMap.get(groupToPlace);
			if (groupReplaceCount == null) {
				groupReplacementCountMap.put(groupToPlace,1);
				groupReplaceCount = 1;
			} else {
				groupReplacementCountMap.put(groupToPlace,groupReplaceCount+1);				
			}
			if (s.isMediumDebug()) {
				System.out.println("Placing "+groupToPlace+" (times placed ="+groupReplaceCount+") Iteration="+iteration);
			}

			// Determine all of the valid placement sites for this group
			AreaPlacementConstraint<CTYPE, CSITE> constraint = s.getGroupConstraint(groupToPlace);
			Set<CSITE> validAnchorSites = constraint.getValidAnchorSites(groupToPlace);

			// Determine "sites to consider" for placement. First look for all unoccupied sites
			// and consider these first. If there are no free sites, consider all valid sites.
			List<CSITE> unoccupiedValidAnchorSites = new ArrayList<>();
			boolean displacementRequired = false;
			for (CSITE possibleAnchorSite : validAnchorSites) {
				if (!s.isGroupOverlapping(groupToPlace, possibleAnchorSite)) {
					unoccupiedValidAnchorSites.add(possibleAnchorSite);
				}
			}
			
			// If there are not any free sites, create a map of all valid sites
			List<CSITE> sitesToConsiderForPlacement = new ArrayList<>();
			if (unoccupiedValidAnchorSites.size() == 0) {
				// No Free site! Must displace
				if (s.isMediumDebug()) {
					System.out.println(" No Free sites exist - consider all valid placement sites for placement");
				}
				sitesToConsiderForPlacement.addAll(validAnchorSites);
				displacementRequired = true;
			} else {
				// Free sites are available
				sitesToConsiderForPlacement.addAll(unoccupiedValidAnchorSites);
				if (s.isMediumDebug()) {
					System.out.println(" Free sites:"+sitesToConsiderForPlacement.size());
				}				
			}

			// Determine the probabilities of each site considered for placement. This probability
			//  map is used to influence the displacement placer to chose those that are more likely
			//  going to lead to a successful placement.
			float totalSiteProbability = 0.0f;
			Map<CSITE, Float> anchorSiteCostMap = new HashMap<>();
			for (CSITE possibleAnchorSite : sitesToConsiderForPlacement) {
				// Find the cost of placing this group at this site
				float anchorSiteCost = 0.0f;
				for (CSITE possibleUsedSite : s.getGroupSites(groupToPlace, possibleAnchorSite)) {
					anchorSiteCost += siteCost.getSiteCost(possibleUsedSite);
				}
				if (anchorSiteCost == 0)
					anchorSiteCost = 1;
				anchorSiteCostMap.put(possibleAnchorSite,anchorSiteCost);
				// Determine the site probability (1/cost). Those with higher cost have lower probability
				float siteProbability = 1 / anchorSiteCost;
				if (s.isMaximumDebug())
					System.out.println("  "+possibleAnchorSite+" cost="+anchorSiteCost+" prob:"+siteProbability+" ("+totalSiteProbability+"-"+
							(totalSiteProbability+siteProbability+")"));
				totalSiteProbability += siteProbability;
			}
			
			// Select a site randomly using this probability map.
			CSITE selectedSite = null;
			if (s.isMediumDebug())
				System.out.println(" "+sitesToConsiderForPlacement.size()+
						" sites. Probabilty sum="+totalSiteProbability);
			// Choose a random number between zero and the total site probability
			float randomNum = this.rng.nextFloat() * totalSiteProbability;
			float curVal = 0.0f;
			// Iterate through all of the sites to consider and stop when it matches the random number
			for (CSITE siteToConsider : sitesToConsiderForPlacement) {
				float siteVal = anchorSiteCostMap.get(siteToConsider);
				float siteProb = 1 / siteVal;
				if (randomNum >= curVal && randomNum < curVal + siteProb) {
					selectedSite = siteToConsider;
					break;
				}
				curVal += siteProb;
			}
			if (s.isMediumDebug())
				System.out.println("  Selected site:"+selectedSite+" random="+randomNum);
			
			// Perform the move
			Set<PlacementGroupMoveDescription<CTYPE, CSITE>> movesSet = new HashSet<>();
			movesSet.add(new PlacementGroupMoveDescription<>(groupToPlace, null, selectedSite));
			// add moves for displacement
			if (displacementRequired) {	
				Set<CSITE> targetSites = groupToPlace.getGroupSites(selectedSite,s.getGroupConstraint(groupToPlace));
				Set<PlacementGroup<CTYPE, CSITE>> movedGroups = new HashSet<>();
				for (CSITE targetSite : targetSites) {
					PlacementGroup<CTYPE, CSITE> displacedGroup = s.getPlacedGroup(targetSite);
					if (displacedGroup != null) {
						// Overlapping group
						
						// is group itself (i.e., is it moving on top of its old position?). If so, don't
						// add an "unplace" move
						if (displacedGroup == groupToPlace)
							continue;
						
						
						CSITE oldSite = s.getGroupAnchorSite(displacedGroup);
						if (s.isMediumDebug()) {
							System.out.println("   At Target site: "+targetSite+" Displacing "+displacedGroup+" from initial site "+oldSite);
						}
						// See if group has already been moved
						if (movedGroups.contains(displacedGroup)) {
							if (s.isMediumDebug())
								System.out.println("    Already moved group "+displacedGroup);							
						} else {
							if (s.isMediumDebug())
								System.out.println("    Group needs moving");							
							movedGroups.add(displacedGroup);
							PlacementGroupMoveDescription<CTYPE, CSITE> gs =
									new PlacementGroupMoveDescription<>(displacedGroup,oldSite,null);
							movesSet.add(gs);
							if (s.isMediumDebug()) {
								System.out.println("     "+gs);
							}							
						}
						// add groups to queue
						unplacedGroupQueue.add(displacedGroup);
					}
				}
			}
			PlacerMove<CTYPE, CSITE> move = new PlacerMove<>(s, movesSet);
			move.makeMove();				
			if (s.isMediumDebug()) {
				System.out.println("Move:");
				System.out.println(move);
			}


			// update cost of sites used
			Set<CSITE> currentSites = s.getGroupSites(groupToPlace);
			if (s.isMediumDebug()) {
				System.out.println("Anchor Site="+s.getGroupAnchorSite(groupToPlace));
			}
			if (currentSites == null) {
				System.out.println("ERROR: Current sites of group are null. Group = "+groupToPlace+
						" selected site = " + selectedSite + " current site "+s.getGroupAnchorSite(groupToPlace));
				System.out.println("  Valid site ="+
						constraint.isValidAnchorPlacementSite(groupToPlace, selectedSite));
				Set<CSITE> possibleSites = groupToPlace.getGroupSites(selectedSite, constraint);
				if (possibleSites == null)
					System.out.println("  No possible sites");
				else {
					System.out.println("  Possible sites");
					for (CSITE site :  possibleSites)
						System.out.println(" "+site);
				}
				System.exit(1);	
			}
			for (CSITE site : currentSites) {
				siteCost.incrementSiteCost(site);
			}
			
			// Increment iteration count
			iteration++;
		} while (!unplacedGroupQueue.isEmpty() && iteration < maxIterations);

		//s.DEBUG = oldDebug;

		if (s.isMediumDebug()) {
			System.out.println("Initial Placement");
			for (PlacementGroup<CTYPE, CSITE> pg : s.getPlacementGroups()) {
				System.out.println(" "+pg+":"+s.getGroupAnchorSite(pg));
			}
		}


		if (unplacedGroupQueue.isEmpty()) {
			// everything is placed
			System.out.println(" Displacement iterations = "+iteration);
			return true;
		}
		else {
			System.out.println(" Failed to place after "+iteration+" iterations");
			return false;
		}
	}

	public class SiteCost {
		
		public SiteCost(PlacerState<CTYPE, CSITE> ps) {
			for (CSITE p : ps.getSitesUsed())
				siteUseMap.put(p, 1);
		}

		public void incrementSiteCost(CSITE s) {
			incrementSiteCost(s, 1);
		}
		
		public void incrementSiteCost(CSITE s, int amnt) {
			Integer i = siteUseMap.get(s);
			if (i==null)
				i = 1;
			else {
				i = i + 1;
			}
			siteUseMap.put(s,i);
		}
		
		public int getSiteCost(CSITE s) {
			Integer i = siteUseMap.get(s);
			if (i == null)
				return 0;
			return i;
		}
		
		Map<CSITE, Integer> siteUseMap = new HashMap<>();
	}
	
	
	
	
}

