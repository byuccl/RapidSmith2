package edu.byu.ece.rapidSmith.cad.placer.annealer;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;
import edu.byu.ece.rapidSmith.cad.clusters.UnpackedCellCluster;
import edu.byu.ece.rapidSmith.cad.placer.Placer;
import edu.byu.ece.rapidSmith.design.ClusterDesign;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.PrimitiveSite;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * A basic placer. Contains a random number generator, a design, and placer state.
 * 
 * Contains a number of methods for managing the movement and placement
 * of placement groups. Other placement algorithms can be developed that use the
 * methods in this class. The important methods within this class are:
 * 
 *   
 */
public class BasicPlacer<CTYPE extends ClusterType, CSITE extends ClusterSite>
		implements Placer<CTYPE, CSITE>
{
	/**
	 * Indicates how many moves to make before printing a progress message.
	 */
	public static final int MOVES_PER_MESSAGE = 10000;

	public static final int DEBUG_NONE = 0;
	public static final int DEBUG_HIGH = 200;
	public static final int DEBUG_MEDIUM = 100;
	public static final int DEBUG_LOW = 50;
	public static int DEBUG = DEBUG_NONE;

	/**
	 * Random number generator used in placement
	 */
	public Random rng;

	/**
	 * The design being placed.
	 */
	protected ClusterDesign<CTYPE, CSITE> design;
	protected Device device;

	/** The state of the placement **/
	protected PlacerState<CTYPE, CSITE> state;

	protected Map<CTYPE, Rectangle> typeRectangleMap;
	protected TypeSiteCoordinatesFactory<CTYPE, CSITE> tscFactory;
	protected InitialPlacer<CTYPE, CSITE> initPlacer;
	protected PlacerCostFunction<CTYPE, CSITE> cost;

	public BasicPlacer(
			TypeSiteCoordinatesFactory<CTYPE, CSITE> tscFactory,
			long seed, Map<CTYPE, Rectangle> typeRectangleMap
	) {
		this.tscFactory = tscFactory;
		this.rng = new Random(seed);
		this.typeRectangleMap = typeRectangleMap;
		this.initPlacer = new SimpleRandomInitialPlacer<>(rng);
	}

	public BasicPlacer(
			TypeSiteCoordinatesFactory<CTYPE, CSITE> tscFactory,
			Map<CTYPE, Rectangle> typeRectangleMap
	) {
		this(tscFactory, System.currentTimeMillis(), typeRectangleMap);
	}
	
	public ClusterDesign<CTYPE, CSITE> getDesign() {
		return design;
	}
	
	public PlacerState<CTYPE, CSITE> getState() {
		return state;
	}
	
	/**
	 * A method for creating the state of the placer. Super classes of this class
	 * will override this method to create different PlacerState objects.
	 */
	protected PlacerState<CTYPE, CSITE> createPlacerState(
			ClusterDesign<CTYPE, CSITE> design, Map<CTYPE, Rectangle> typeRectangleMap) {
		return new PlacerState<>(design, typeRectangleMap, tscFactory);
	}
	
	public boolean place(ClusterDesign<CTYPE, CSITE> design, Device device) {
		this.device = device;
		state = createPlacerState(design, typeRectangleMap);
		initPlacer.initialPlace(state);
		cost = new NetRectangleCostFunction<>(design, state);
		
		float currCost = cost.calcSystemCost();
		System.out.println("Initial placement cost: " + currCost);
		int numMoves = 0;
		ArrayList<PlacementGroup<CTYPE, CSITE>> allGroups = new ArrayList<>(state.getPlacementGroups());
		int MAX_MOVES = 100000;
		while(numMoves < MAX_MOVES) {
			if(numMoves % MOVES_PER_MESSAGE == 0) System.out.println("Move: " + numMoves + " Cost: " + currCost);
			int toSwapIdx = rng.nextInt(allGroups.size());
			PlacementGroup<CTYPE, CSITE> toSwap = allGroups.get(toSwapIdx);
			PlacerMove<CTYPE, CSITE> move = proposeMove(toSwap);
			//if there's no way to swap the group, move on...
			if(move == null) continue;

			move.makeMove();
			float newCost = cost.calcIncrementalCost(move);
			//System.out.println("New cost="+newCost);
	        if(newCost >= currCost) {
	        	move.undoMove();
	        	float backCost = cost.calcIncrementalCost(move);
	        	if(Math.abs(backCost-currCost) > 1) {
	        		move.debugMove();
	        		System.out.println("Curr: " + currCost + " Back: " + backCost);
	        		System.exit(-1);
	        	}
	        }
	        else {
	        	currCost = newCost;
	        }
	        numMoves++;
		}
		System.out.println("Final system cost: " + currCost);
		state.finalizePlacement(design);
		return true;
	}

	/**
	 * Propose a new location for a given PlacementGroup. A range constraint can be used
	 * to limit the move within a pre-specified distance from its initial location.
	 * 
	 * This method will continually identify a random locations until it finds a valid
	 * location for the group. Once a valid location is found, it calls createSwapMove to
	 * create a move involving a swap. Note that while a valid location for the group may be
	 * found, its corresponding swap move may not be legal and the move is not created.
	 * 
	 * Note that this method does not actually perform the move - it simply 
	 */
	public PlacerMove<CTYPE, CSITE> proposeMove(
			PlacementGroup<CTYPE, CSITE> groupToPlace,
			boolean checkRangeLimit, int rangeLimit
	) {
		int searchLimit = 10000;

		if (DEBUG >= DEBUG_MEDIUM) {
			System.out.print(" Propose move for group "+groupToPlace);
			if (checkRangeLimit)
				System.out.println(" with range limit of "+rangeLimit);
			else
				System.out.println(" with no range limit");
		}
		
		// Find a new site
		CSITE oldSite = state.getGroupAnchorSite(groupToPlace);
		CSITE newSite;
		int iteration = 0;
		do {
			if (checkRangeLimit) {
				newSite =
					state.getGroupConstraint(groupToPlace).getValidRandomSiteWithinRange(oldSite, rangeLimit, rng);
			} else {
				newSite =
					state.getGroupConstraint(groupToPlace).getValidRandomSite(rng);						
			}
			if (DEBUG >= DEBUG_MEDIUM)
				System.out.print("  Proposed site:"+newSite);
			if (!state.isValidAnchorPlacementSiteSite(groupToPlace, newSite)) {
				newSite = null;
				if (DEBUG >= DEBUG_MEDIUM)
					System.out.println(" - invalid");
			} else {
				if (DEBUG >= DEBUG_MEDIUM)
					System.out.println(" - valid");				
			}
			iteration++;
		} while (newSite == null && iteration < searchLimit);

		if (iteration == searchLimit) {
			System.out.println("Can't find a new site");
			System.exit(1);
		}
		
		// Create a move for this group at this site
		return createSwapMove(groupToPlace, newSite);
	}
	
	public PlacerMove<CTYPE, CSITE> proposeMove(
			PlacementGroup<CTYPE, CSITE> groupToPlace
	) {
		return proposeMove(groupToPlace, false, 0);
	}

	/**
	 * See if the proposed move is valid
	 */
	public PlacerMove<CTYPE, CSITE> createValidPotentialMove(
			PlacementGroup<CTYPE, CSITE> groupToPlace, CSITE proposedSite
	) {

		// If true, this will allow multiple overlapping groups to be swapped with the groupToPlace.		
		boolean allowMultiOverlappingGroupReplacement = false;

		// Make sure the proposed site is a valid site
		//if (!groupToPlace.isValidSite(proposedSite))
		if (!state.isValidAnchorPlacementSiteSite(groupToPlace, proposedSite))
			return null;
				
		// Store the 'current' site of the group. It will be vacated if all goes well.
		CSITE currentGroupSite = state.getGroupAnchorSite(groupToPlace);
		Set<PlacementGroup<CTYPE, CSITE>> overlappingGroups = state.getOverlappingPlacementGroups(groupToPlace, proposedSite);
		// The set of moves that must occur for this placement to be valid. Primitive moves will be
		// added to this set as the move is constructed.
		Set<PlacementGroupMoveDescription<CTYPE, CSITE>> groupMoves = new HashSet<>();

		// Process the group based on the number of overlapping groups
		PlacementGroupMoveDescription<CTYPE, CSITE> move;
		switch(overlappingGroups.size()) {
			case 0:
				// No overlaping groups. Don't do anything. The groupToMove can be moved safely to a 
				// new spot.
				break;
			case 1:
				// Only one group overlaps. See if the group will fit in the location of the groupToPlace
				PlacementGroup<CTYPE, CSITE> overlappingGroup = overlappingGroups.iterator().next();
				move = validGroupPlacement(groupToPlace,overlappingGroup,currentGroupSite);
				if (move == null)					
					return null;
				groupMoves.add(move);
			default:
				// There are multiple groups that overlap. See if the set of groups that overlap can be moved
				// to the group being placed.
				return null;
		}
		// Add a move for the group being placed
		move = new PlacementGroupMoveDescription<>(groupToPlace, currentGroupSite, proposedSite);
		groupMoves.add(move);
    	if (state.validMoveSet(groupMoves)) 
    		return new PlacerMove<>(state, groupMoves);
    	// Shouldn't get here
    	return null;
		
	}
	
	/**
	 * Creates a PlacerMove object based on the movement of the initial group to a new group site.
	 * This class will see what objects overlap this new site and create a set of moves to swap
	 * if it is legal. 
	 * 
	 * Note that the initialGroup MUST have an initial placement. This cannot be used for
	 * unplaced modules.
	 * 
	 * @param initialGroup 
	 * 	Group that is being moved and causing a possible swap
	 * @param initialGroupNewSite 
	 *  The new intended site for the group. Note that this should NOT be null.
	 */	
	public PlacerMove<CTYPE, CSITE> createSwapMove(
			PlacementGroup<CTYPE, CSITE> initialGroup, CSITE initialGroupNewSite) {
				
		if (DEBUG >= DEBUG_HIGH) {
			System.out.print("  Creating Move:");
			System.out.print(" ("+initialGroup.getClusters().size()+")");
		}
		
		// Make sure that the suggested site is a valid site for the group. Don't waste
		// any time on checking conflicts if the site is invalid.
		// TODO: remove this check from the method that calls this method
		//if (!initialGroup.isValidSite(initialGroupNewSite)) {
		if (!state.isValidAnchorPlacementSiteSite(initialGroup, initialGroupNewSite)) {
			if (DEBUG >= DEBUG_HIGH) {
				System.out.println("\tInvalid target site for for group");
				//System.out.println("\t*** Invalid Move ***");
			}
			return null;
		}
		
		// Determine original location of group
		CSITE initialGroupOriginalSite;
		initialGroupOriginalSite = state.getGroupAnchorSite(initialGroup);			
		Point igLoc = initialGroupOriginalSite.getLocation();

		// Determine the set of sites that the initial group will occupy if the move is
		// to take place		
		// TODO: this method involves the creation of
		// a new container. Need to create a method that allows you to pass in an already allocated
		// container to avoid the repeated garbage collection.
		//Set<PrimitiveSite> newInitialPlacementSites = initialGroup.getGroupSites(initialGroupNewSite);
		Set<CSITE> newInitialPlacementSites = state.getGroupSites(initialGroup, initialGroupNewSite);

		// These data structures hold the set of moves that must be made in order for the initialGroupMove
		// to take place. 
		// TODO: cache this member so that it doesn't have to be continually created
		// and destroyed.
		//Map<PlacementGroup, ShadowPlacementGroupMove> displacedShadowGroups = 
		//	new HashMap<PlacementGroup, ShadowPlacementGroupMove>();
		Map<PlacementGroup<CTYPE, CSITE>, PlacementGroupMoveDescription<CTYPE, CSITE>> displacedMainGroups =
				new HashMap<>();
		Map<PlacementGroup<CTYPE, CSITE>, Set<CSITE>> newMainPlacementSitesMap =
				new HashMap<>();
		//Map<PlacementGroup, Set<PrimitiveSite>> newShadowPlacementSitesMap = 
		//	new HashMap<PlacementGroup, Set<PrimitiveSite>>();
		
		// Iterate over all of the instances of the initial group to move. Find the site that the instance
		// will occupy and see if there are any groups (shadow or main) that overlap. Figure
		// out which groups need to be moved and create the appropriate move.
		//
		// The movement of the initial group may displace more than one group. This method will try
		// to move all displaced groups together and keep their relative positions with each other.
		// This way, displaced groups will not overlap each other. The placement location of displaced groups
		// is as follows:
		// - Determine the offset of the displaced group from the anchor location of the
		//   new site for the initial group
		// - Identify the site that has the same offset from the original anchor location
		//   of the old site for the initial group
		//
		// For example, if the anchor of a displaced group is (47,13) and the new anchor of the initial
		// group is (45,12), the displaced group has an offset of (+2,+1) from the 
		// anchor location of the new initial group site. If the old anchor of the initial group
		// is (17,32), the new anchor of the displaced group will be offset by (+2,+1) or at
		// (19,31).
		for (Cluster<CTYPE, CSITE> groupCluster : initialGroup.getClusters()) {
			// target site of this instance. This is where we want the instance to go
			//PrimitiveSite instanceTargetSite = initialGroup.getClusterSite(groupCluster, initialGroupNewSite);
			CSITE instanceTargetSite = state.getClusterSite(groupCluster, initialGroup, initialGroupNewSite);
			
			// Now find all of the conflicts of the new site for this instance. 
			Cluster<CTYPE, CSITE> overlappingInstance = state.getPlacedCluster(instanceTargetSite);
			if (overlappingInstance != null) {
				PlacementGroup<CTYPE, CSITE> overlappingMainGroup = state.getGroup(overlappingInstance);
				
				// If the group is already in the list, a move has been created. No
				// need to create a second move for the group
				if (displacedMainGroups.keySet().contains(overlappingMainGroup))
					continue;

				// Determine location of this displaced group.
				CSITE displacedGroupAnchorSite = state.getGroupAnchorSite(overlappingMainGroup);
				int xOffsetFromInitialGroupNewAnchorSite = displacedGroupAnchorSite.getLocation().x - initialGroupNewSite.getLocation().x;
				int yOffsetFromInitialGroupNewAnchorSite = displacedGroupAnchorSite.getLocation().y - initialGroupNewSite.getLocation().y;
				int newGroupXlocation = igLoc.x + xOffsetFromInitialGroupNewAnchorSite;
				int newGroupYlocation = igLoc.y + yOffsetFromInitialGroupNewAnchorSite;
				CSITE newMainAnchorSite = state.getGroupConstraint(overlappingMainGroup).getSite(newGroupXlocation, newGroupYlocation);
				if (DEBUG  >= DEBUG_HIGH) {
					System.out.println("  Overlaping instance at "+instanceTargetSite+". Group of "+
							overlappingMainGroup.getClusters().size()+" with anchor at "+state.getGroupAnchorSite(overlappingMainGroup));
					if (newMainAnchorSite != null)
						System.out.println("   must be moved to "+newMainAnchorSite);
					//else
					//	System.out.println("   invalid target site");
				}
				
				// Check to see if the proposed site for the main group is valid. If not, this move cannot happen.
				//if (newMainAnchorSite == null || !overlappingMainGroup.isValidSite(newMainAnchorSite)) {
				if (newMainAnchorSite == null || !state.isValidAnchorPlacementSiteSite(overlappingMainGroup, newMainAnchorSite)) {
					if (DEBUG >= DEBUG_HIGH) {
						System.out.println("\t\tInvalid site for displaced main group");
						//System.out.println("\t*** Invalid Move ***");
					}
					return null;
				}
				
				// Determine the new primitive sites of this main move. See if it conflicts with the initial move
				// or any of the moves in the set.
				//Set<PrimitiveSite> newMainSites = overlappingMainGroup.getGroupSites(newMainAnchorSite);
				Set<CSITE> newMainSites = state.getGroupSites(overlappingMainGroup, newMainAnchorSite);
				if (overlappingSites(newInitialPlacementSites, newMainSites)) {
					if (DEBUG >= DEBUG_HIGH) {
						System.out.println("\t\tSite for displaced main group will confict with new location of initial group");
						//System.out.println("\t*** Invalid Move ***");
					}
					return null;
				}
				
				// Create the move and save the information for later checking
				PlacementGroupMoveDescription<CTYPE, CSITE> m =
						new PlacementGroupMoveDescription<>(
								overlappingMainGroup, displacedGroupAnchorSite, newMainAnchorSite);
				displacedMainGroups.put(overlappingMainGroup, m);			
				newMainPlacementSitesMap.put(overlappingMainGroup, newMainSites);

			}

		}

		// At this point, we know that the initial group move is valid (the target site is valid and
		// we moved everything out of its way). The proposed moves for the displaced groups
		// all have valid locations (from a possible placement standpoint). Now we need to verify that
		// there are no conflicts between these proposed moves and the existing placement of the circuit.
		//
		// The following checks will investigate the target sites for all of the displaced groups
		// and see if they conflict with any groups NOT associated with this move. We do not need to check
		// if they conflict with the groups involved with this move because 1) we already checked to see
		// if it conflicts with the initial group move and 2) groups involved with the move won't conflict with 
		// each other because they are all placed the same way relative to each other (we assume they
		// were placed without conflict previously).
						
		// Check the main moves against the existing placement
		for (PlacementGroup<CTYPE, CSITE> mainGroup : displacedMainGroups.keySet()) {
			// Iterate over the new primitive sites of this group
			Set<CSITE> newMainSites = newMainPlacementSitesMap.get(mainGroup);
			for (CSITE newMainSite : newMainSites) {
				Cluster<CTYPE, CSITE> overlappingMainInstance = state.getPlacedCluster(newMainSite);
				PlacementGroup<CTYPE, CSITE> overlappingMainGroup = state.getGroup(overlappingMainInstance);
				// If the new shadow location overlaps with a main group that is involved with
				// this set of moves, we can ignore it. If it is not a part of the move,
				// we have a conflict and the move is invalid.
				if (!displacedMainGroups.keySet().contains(overlappingMainGroup) && overlappingMainGroup!=initialGroup) {
					if (DEBUG >= DEBUG_HIGH) {
						System.out.println("\tA main group displacement move conflicts with an existing main placement "+newMainSite);
						//System.out.println("\t*** Invalid Move ***");
					}				
					return null;
				}
				// Check to see if the new main location overlaps with a shadow group that is
				// not associated with this move.
				
			}
		}
		
		// If we make it to this point, the move is considered valid and can be made. Create the composite move
		// and return.
		Set<PlacementGroupMoveDescription<CTYPE, CSITE>> groupMoves = new HashSet<>();
		
		// Create initial group move
		PlacementGroupMoveDescription<CTYPE, CSITE> initialMove =
				new PlacementGroupMoveDescription<>(initialGroup, initialGroupOriginalSite,
						initialGroupNewSite);
		groupMoves.add(initialMove);

		// Add the moves associated with the shadow and main moves
		//groupMoves.addAll(displacedShadowGroups.values());
		groupMoves.addAll(displacedMainGroups.values());
		
		// Create composite move and return
		PlacerMove<CTYPE, CSITE> tmp = new PlacerMove<>(state,groupMoves);
		if (DEBUG >= DEBUG_HIGH) {
			//System.out.println("\t*** Valid Move ***");
			tmp.debugMove();
		}
		return tmp;
	}

	/**
	 * Create a move for a an unplaced group.
	 * 
	 * @param unplaceDisplacedGroups
	 *   true: if the location for the group displaces any groups, create moves to unplace the displaced groups.
	 *   false: if the location for the group displaces any groups, do not create a move (i.e., a null move)
	 */
	public PlacerMove<CTYPE, CSITE> createMoveForUnplacedGroup(
			PlacementGroup<CTYPE, CSITE> initialGroup, CSITE initialGroupNewSite,
			boolean unplaceDisplacedGroups) {
		
		if (DEBUG >= DEBUG_HIGH) {
			System.out.print("Main Move:");
			System.out.print(" ("+initialGroup.getClusters().size()+")");
		}
		
		// Make sure that the suggested site is a valid site for the group. Don't waste
		// any time on checking conflicts if the site is invalid.
		if (!state.isValidAnchorPlacementSiteSite(initialGroup, initialGroupNewSite)) {
			if (DEBUG >= DEBUG_HIGH) {
				System.out.println("\tInvalid site for for group");
				System.out.println("\t*** Invalid Move ***");
			}
			return null;
		}
		
		// Determine the set of sites that the initial group will occupy if the move is
		// to take place		
		//Set<PrimitiveSite> newInitialPlacementSites = state.getGroupSites(initialGroup, initialGroupNewSite);
		if (DEBUG >= DEBUG_HIGH) {
			System.out.print(" unplaced ->"+initialGroupNewSite);
		}	
		
		// These data structures hold the set of moves that must be made in order for the initialGroupMove
		// to take place. 
		Map<PlacementGroup<CTYPE, CSITE>, PlacementGroupMoveDescription<CTYPE, CSITE>> displacedMainGroups =
				new HashMap<>();
		//Map<PlacementGroup, Set<PrimitiveSite>> newMainPlacementSitesMap = 
		//	new HashMap<PlacementGroup, Set<PrimitiveSite>>();
		
		// Iterate over all of the instances of the initial group to move. Find the site that the instance
		// will occupy and see if there are any groups (shadow or main) that overlap. Figure
		// out which groups need to be moved and create the appropriate move.
		//
		// The movement of the initial group may displace more than one group. This method will try
		// to move all displaced groups together and keep their relative positions with each other.
		// This way, displaced groups will not overlap each other. The placement location of displaced groups
		// is as follows:
		// - Determine the offset of the displaced group from the anchor location of the
		//   new site for the initial group
		// - Identify the site that has the same offset from the original anchor location
		//   of the old site for the initial group
		//
		// For example, if the anchor of a displaced group is (47,13) and the new anchor of the initial
		// group is (45,12), the displaced group has an offset of (+2,+1) from the 
		// anchor location of the new initial group site. If the old anchor of the initial group
		// is (17,32), the new anchor of the displaced group will be offset by (+2,+1) or at
		// (19,31).
		for (Cluster<CTYPE, CSITE> groupCluster : initialGroup.getClusters()) {
			// target site of this instance. This is where we want the instance to go
			CSITE instanceTargetSite = state.getClusterSite(groupCluster, initialGroup, initialGroupNewSite);
			
			// Now find all of the conflicts of the new site for this instance. 
			Cluster<CTYPE, CSITE> overlappingInstance = state.getPlacedCluster(instanceTargetSite);
			if (overlappingInstance != null) {
				
				// There is a displacement conflict. If we are not going to unplace the displaced groups,
				// return a null move. Otherwise, continue by creating an unplace move.
				if (!unplaceDisplacedGroups) {
					if (DEBUG >= DEBUG_HIGH) {
						System.out.println("\t\tConflict for placement");
						System.out.println("\t*** Invalid Move ***");
					}
					return null;
				}	
				
				PlacementGroup<CTYPE, CSITE> overlappingGroup = state.getGroup(overlappingInstance);
				
				// If the group is already in the list, a move has been created. No
				// need to create a second move for the group
				if (displacedMainGroups.keySet().contains(overlappingGroup))
					continue;

				// Create the move and save the information for later checking
				CSITE displacedGroupAnchorSite = state.getGroupAnchorSite(overlappingGroup);
				PlacementGroupMoveDescription<CTYPE, CSITE> m =
						PlacementGroupMoveDescription.createUnplaceMove(overlappingGroup, displacedGroupAnchorSite);
				displacedMainGroups.put(overlappingGroup, m);
			}

		}

		
		// If we make it to this point, the move is considered valid and can be made. Create the composite move
		// and return.
		Set<PlacementGroupMoveDescription<CTYPE, CSITE>> groupMoves = new HashSet<>();
		
		// Create initial group move
		PlacementGroupMoveDescription<CTYPE, CSITE> initialMove =
				PlacementGroupMoveDescription.createInitialPlaceMove(initialGroup, initialGroupNewSite);
		groupMoves.add(initialMove);

		// Add the moves associated with the shadow and main moves
		groupMoves.addAll(displacedMainGroups.values());
		
		// Create composite move and return
		PlacerMove<CTYPE, CSITE> tmp = new PlacerMove<>(state,groupMoves);
		if (DEBUG >= DEBUG_HIGH) {
			System.out.println("\t*** Valid Move ***");
			tmp.debugMove();
		}
		return tmp;
	}

	public boolean overlappingSites(Set<CSITE> set1, Set<CSITE> set2) {
		for (CSITE s : set1) {
			if(set2.contains(s))
				return true;
		}
		return false;
	}

	/**
	 *  See if the possibleReplacement of the groupBeingMoved can be placed at the possibleReplacementDestination.
	 *  There should be no placement collisions except for collisions of groupBeingMoved.
	 *  
	 *  If a valid move is possible, the move is created. If not, a null is returned.
	 */
	public PlacementGroupMoveDescription<CTYPE, CSITE> validGroupPlacement(
			PlacementGroup<CTYPE, CSITE> groupBeingMoved,
			PlacementGroup<CTYPE, CSITE> possibleReplacement,
			CSITE possibleReplacementDestination) {
		// Step 1. See if the anchor of the group is a valid placement.
		//if(!possibleReplacement.isValidSite(possibleReplacementDestination))
		if(!state.isValidAnchorPlacementSiteSite(possibleReplacement, possibleReplacementDestination))
			return null;
		// Step 2. See how many placement groups the overlapping group will displace. It may displace the
		// group to move. However, if it displaces any other groups, then it is not allowed.
		Set<PlacementGroup<CTYPE, CSITE>> destOverlappingGroups =
    		state.getOverlappingPlacementGroups(possibleReplacement, possibleReplacementDestination);
		if (destOverlappingGroups.size() > 1)
			return null;
		// There should be only one group and it should be the main group
		if (destOverlappingGroups.size() == 1 && destOverlappingGroups.iterator().next() != groupBeingMoved)
			return null;
		// This represents a valid move. Create it.
		return new PlacementGroupMoveDescription<>(possibleReplacement,
				state.getGroupAnchorSite(possibleReplacement),
				possibleReplacementDestination);
		
	}
	
	/** Main function */
	public static void main(String args[]) {
		if (args.length < 1) {
			System.err.println("filename");
			System.exit(1);
		}
		
		long startTime = System.currentTimeMillis();
//		Design design = new Design(args[0]+".xdl");
//		BasicPlacer placer = new BasicPlacer(design);
//		Design placed = placer.iterativePlace();
//		placed.saveXDLFile(args[0]+"_placed.xdl");
//
//		long endTime = System.currentTimeMillis();
//	    long elapsedTime = endTime - startTime;
//	    double seconds = elapsedTime / 1.0E03;
//	    System.out.println ("Elapsed Time = " + seconds + " seconds");
//
//		System.out.println("=========================================");
	}
	
}
