package edu.byu.ece.rapidSmith.cad.placer.annealer;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;
import edu.byu.ece.rapidSmith.design.ClusterDesign;

import java.awt.*;
import java.util.*;

/**
 * 
 * This class manages the dynamic placement state of all placement
 * groups during the placement process. The placement state changes frequently
 * during placement and this state is managed locally. The actually placement
 * state is not set in the XDL file until after placement and by calling the 
 * "finalizePlacement" method.
 * 
 * The state of the placement (i.e., the locations where the groups are placed) 
 * is managed by two objects that must be consistent. The first is the groupAnchorMap:
 * 	
 *   protected Map<PlacementGroup, PrimitiveSite> groupAnchorMap;
 * 
 * This stores the anchor location of each group in the design. A group is
 * considered "unplaced" when it does not have an entry in this map. This object
 * also stores a map between sites and instances:
 * 
 *   protected Map<PrimitiveSite, Instance> siteInstanceMap;
 *
 * This provides the ability to go from groups to sites or from
 * sites to groups(instances). This object must be consistent with the 
 * groupAnchorMap.
 * 
 * This object does NOT own the placement groups involved with placement. This
 * information is obtained in the placementGroups (DesignPlacementGroups) object
 * 
 * 
 * 
 * The placement of groups is made using a set of "Moves". A move is an atomic
 * unit of groups and locations in which they should be placed. These moves are
 * created to insure that a valid placement is always maintained.
 * 
 * This class also manages the area constraints used by each placement group.
 * 
 * This class also holds a debug flag that can be used by any classes related to
 * placement. This consolidates the debug infrastructure.
 * 
 * - Methods for determining which groups overlap other groups
 * - TODO: describe how "unplaceable" instances are handled. Their state is currently
 *   not managed here.
 */
public class PlacerState<CTYPE extends ClusterType, CSITE extends ClusterSite> {
	/**
	 * A Map between each group and its anchor site
	 */
	protected Map<PlacementGroup<CTYPE, CSITE>, CSITE> groupAnchorMap;

	/**
	 * A map between a primitive site and the group that occupies the site.
	 */
	protected Map<CSITE, Cluster<CTYPE, CSITE>> siteInstanceMap;

	/**
	 * Manages all of the PlacementGroups of a design.
	 */
	protected DesignPlacementGroups<CTYPE, CSITE> placementGroups;

	/**
	 * A map between each group and the placement constraint used during placement
	 * for the group.
	 */
	protected Map<PlacementGroup<CTYPE, CSITE>,
			AreaPlacementConstraint<CTYPE, CSITE>> groupAreaConstraintMap;

	/**
	 * Primary constructor
	 * 
	 * @param d The design to place
	 * @param typeRectangleMap A map between a type and a Rectangle area constraint.
	 * @param preserveInitialPlacement If true, this flat instructs the constructor to populate the state
	 *  with the placement found in the original XDL design. If false, it ignores the current placement and
	 *  assume the design is unplaced.
	 */
	public PlacerState(
			ClusterDesign<CTYPE, CSITE> d, Map<CTYPE, Rectangle> typeRectangleMap,
			TypeSiteCoordinatesFactory<CTYPE, CSITE> tscFactory, boolean preserveInitialPlacement
	) {
		placementGroups = new DesignPlacementGroups<>(d, tscFactory);
		siteInstanceMap = new HashMap<>();
		groupAnchorMap = new HashMap<>();
		createAreaConstraintsFromRectangleMap(typeRectangleMap);
		if (preserveInitialPlacement)
			copyPlacementFromDesign();
	}

	public PlacerState(
			ClusterDesign<CTYPE, CSITE> d, Map<CTYPE, Rectangle> typeRectangleMap,
			TypeSiteCoordinatesFactory<CTYPE, CSITE> tscFactory
	) {
		this(d,typeRectangleMap, tscFactory, false);
	}
	
	public PlacerState(ClusterDesign<CTYPE, CSITE> d,
	                   TypeSiteCoordinatesFactory<CTYPE, CSITE> tscFactory,
	                   boolean preserveInitialPlacement
	) {
		this(d, null, tscFactory, preserveInitialPlacement);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods for creating the initial placer state
	////////////////////////////////////////////////////////////////////////////////////////////////////

	protected void createAreaConstraintsFromRectangleMap(Map<CTYPE, Rectangle> typeRectangleMap) {
		
		// Set the area constraints of each group
		groupAreaConstraintMap = new HashMap<>();

		// A temporary map between each type and the constraint used for that type
		HashMap<CTYPE, AreaPlacementConstraint<CTYPE, CSITE>>	typeConstraintsMap = new HashMap<>();

		// A temporary map between each constraint and the groups that must occupy the constraint.
		//Map<AreaPlacementConstraint, Set<PlacementGroup>> constraintGroupMap = 
		//	new HashMap<AreaPlacementConstraint, Set<PlacementGroup>>();
		
		for (PlacementGroup<CTYPE, CSITE> group : placementGroups.getPlacementGroups()) {

			// 1. Figure out which type group coordinate system to use
			CTYPE groupType = group.getGroupType();
			
			// 2. Get the type coordinate system for the group
			TypeSiteCoordinates<CTYPE, CSITE> coord = getCoordinates(groupType);
			
			// 3. Set the area constraint of the site
			AreaPlacementConstraint<CTYPE, CSITE> typeConstraint = typeConstraintsMap.get(groupType);
			if (typeConstraint == null) {
				// No constraint found for type. Create one
				Rectangle rect = null;
				if (typeRectangleMap != null)
					rect = typeRectangleMap.get(groupType);
				if (rect != null) {
					typeConstraint = new SquareAreaConstraint<>(coord, rect);
					System.out.println("User constraint of "+rectString(rect)+
							" for type "+groupType);
					System.out.println("  # valid sites="+typeConstraint.getValidSites().size()+" # invalid sites="+
							(typeConstraint.area()-typeConstraint.getValidSites().size())+ " type size = "+
							rectString(coord.getSiteRectangle()));
					//for (Point p : typeConstraint.getInvalidSitePoints())
					//	System.out.println("   "+p);
				} else
					typeConstraint = new SquareAreaConstraint<>(coord);
				typeConstraintsMap.put(groupType, typeConstraint);
			}			
			groupAreaConstraintMap.put(group, typeConstraint);			
		}		

		// Check to see if the constraints are ok
		canBePlaced = true;
		for (AreaPlacementConstraint<CTYPE, CSITE> constraint : getAllConstraints()) {

			// Figure out how many instances are allocated to this constraint
			Set<PlacementGroup<CTYPE, CSITE>> groupsInConstraint = getGroupsUsingConstraint(constraint);
			int intCount = 0;
			for (PlacementGroup<CTYPE, CSITE> pg : groupsInConstraint) {
				intCount += pg.getClusters().size();
			}
			
			// Figure out how many valid spots there are
			int validSites = constraint.getValidSites().size();
			
			System.out.println("Constraint "+constraint+" has "+validSites+" sites for "+intCount+" instances");
			if (validSites >= intCount) {
				System.out.println("\t"+(float)intCount/validSites*100+"% utilization");
			} else {
				canBePlaced = false;
				System.out.println("\tWarning: over utilized - cannot place "+(float)intCount/validSites*100+"% utilization");
			}
		}
		
		Set<PlacementGroup<CTYPE, CSITE>> multipleInstanceGroups = placementGroups.getMultiClusterPlacementGroups();
		int numGroups = (multipleInstanceGroups == null ? 0 : multipleInstanceGroups.size());
		System.out.println("Multiple Instance Groups = "+numGroups);
	}

	/**
	 * Copy the placement found in the original design into the PlacerState.
	 */
	protected void copyPlacementFromDesign() {
		
		// Iterate through all of the instances and find those that are anchors
		for (Cluster<CTYPE, CSITE> i : placementGroups.getDesign().getClusters()) {
			
			// Take the instance placement location and update the group placement or
			// validate the group placement.
			PlacementGroup<CTYPE, CSITE> iGroup = placementGroups.getGroup(i);

			// If there is no group associated with this site, continue
			if (iGroup == null) {
				System.out.println("Cluster "+i.getName()+" does not have a group");
				continue;
			}

			Cluster<CTYPE, CSITE> groupInstanceAnchor = iGroup.getAnchor();
			if (i == groupInstanceAnchor) {
				// i is a known anchor
				
				CSITE iSite = i.getPlacement();
				
				// See if the anchor site has been placed
				if (iSite != null) {
					AreaPlacementConstraint<CTYPE, CSITE> constraint = getGroupConstraint(iGroup);
					if (!constraint.isValidAnchorPlacementSite(iGroup, iSite)) {
						System.out.println("Warning: group "+iGroup+" at anchor "+ iSite+" is invalid. Placement ignored");
						continue;
					}
					// Any overlap?
					Set<PlacementGroup<CTYPE, CSITE>> overlappingGroups = getOverlappingPlacementGroups(iGroup, iSite);
					if (overlappingGroups != null && overlappingGroups.size() > 0) {
						System.out.println("Warning: group "+iGroup+" at anchor "+ iSite+" overlaps with other groups. Placement ignored");
						continue;					
					}
					// Must be good. Perform placement	
					PlacerMove<CTYPE, CSITE> move = new PlacerMove<>(this,iGroup,iSite);
					move.makeMove();
					
				} else {
					System.out.println("Warning: anchor instance "+groupInstanceAnchor.getName()+" is not placed");
				}				
			}
		}

		// The anchors have been placed. See if the non-anchor instances are properly placed
		for (Cluster<CTYPE, CSITE> i : placementGroups.getDesign().getClusters()) {
			// Take the instance placement location and update the group placement or
			// validate the group placement.
			PlacementGroup<CTYPE, CSITE> iGroup = placementGroups.getGroup(i);

			// If there is no group associated with this site, continue
			if (iGroup == null) {
				continue;
			}
			
			// Get the anchor
			Cluster<CTYPE, CSITE> anchor = iGroup.getAnchor();
			CSITE groupAnchorSite = getGroupAnchorSite(iGroup);
			if (groupAnchorSite == null) {
				System.out.println("Instance "+i.getName()+" not placed (no anchor placement)");
			} else {
				CSITE site = i.getPlacement();
				CSITE expectedSite = getCurrentClusterSite(i);
				if (expectedSite != site) {
					System.out.println("Warning: Instance "+i.getName()+" placed at "+site+" is inconsistent with anchor "+
							anchor.getName()+" placed at site "+groupAnchorSite+". Should be placed at site "+
							getClusterSite(i, iGroup, groupAnchorSite));
				}
			}
			
		}
		
		
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods for querying the basic current state of placement. None of these methods change
	// the state. 
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Returns all instances that the state is aware of. This is different from Design.getCluster()
	 * because the placer only places a subset of the design's instances (at present).
	 * 
	 * @return clusters in this state
	 */
	public Set<Cluster<CTYPE, CSITE>> getAllClusters() {
		return placementGroups.getAllClusters();
	}
	
	/**
	 * Returns all primitive sites used in the placement
	 */
	public Set<CSITE> getSitesUsed() {
		return siteInstanceMap.keySet();
	}
	
	public Set<Cluster<CTYPE, CSITE>> getClustersToPlace() {
		return placementGroups.getClustersToPlace();
	}
		
	public Set<Cluster<CTYPE, CSITE>> getClustersNotToPlace() {
		return placementGroups.getClustersNotToPlace();
	}

	/**
	 * Determine the anchor site of a given placement group.
	 */
	public CSITE getGroupAnchorSite(PlacementGroup<CTYPE, CSITE> group) {
		return groupAnchorMap.get(group);
	}

	/**
	 * Return the placement groups that can be placed by the placer
	 */
	public Set<PlacementGroup<CTYPE, CSITE>> getPlacementGroups() {
		return placementGroups.getPlacementGroups();
	}
	
	/** 
	 * Return the placement groups that have been placed.
	 */
	public Set<PlacementGroup<CTYPE, CSITE>> getPlacedPlacementGroups() {
		return groupAnchorMap.keySet();
	}
	
	/** 
	 * Return the placement groups that have been placed.
	 */
	public Set<PlacementGroup<CTYPE, CSITE>> getUnplacedPlacementGroups() {
		Set<PlacementGroup<CTYPE, CSITE>> unplacedGroups = new HashSet<>(placementGroups.getPlacementGroups());
		unplacedGroups.removeAll(getPlacedPlacementGroups());
		return unplacedGroups;
	}

	/**
	 * Return the design associated with this placer state.
	 */
	public ClusterDesign<CTYPE, CSITE> getDesign() {
		return placementGroups.getDesign();
	}

	/**
	 * Return the groups that have more than one instance.
	 */
	public Set<PlacementGroup<CTYPE, CSITE>> getMultiInstancePlacementGroups() {
		return placementGroups.getMultiClusterPlacementGroups();
	}

	/**
	 * Return the PlacementGroup that owns this instance.
	 */
	public PlacementGroup<CTYPE, CSITE> getGroup(Cluster<CTYPE, CSITE> i) {
		return placementGroups.getGroup(i);
	}

	public TypeSiteCoordinates<CTYPE, CSITE> getCoordinates(CTYPE type) {
		return placementGroups.getCoordinates(type);
	}
	
	/** Get all of the AreaPlacementConstraint objects associated
	 * with this placement.
	 */
	public Set<AreaPlacementConstraint<CTYPE, CSITE>> getAllConstraints() {
		Set<AreaPlacementConstraint<CTYPE, CSITE>> constraints = new HashSet<>();
		constraints.addAll(groupAreaConstraintMap.values());
		return constraints;
	}
	
	/** Returns the area constraint for the given group. **/
	public AreaPlacementConstraint<CTYPE, CSITE> getGroupConstraint(
			PlacementGroup<CTYPE, CSITE> g
	) {
		return groupAreaConstraintMap.get(g);
	}

	public AreaPlacementConstraint<CTYPE, CSITE> getConstraint(CTYPE t) {
		for (AreaPlacementConstraint<CTYPE, CSITE> apc : getAllConstraints()) {
			if (apc.getType() == t)
				return apc;
		}
		return null;
	}
	
	/**
	 * Returns a Set of Set<AreaPlacementConstraint> objects where the 
	 * Set<AreaPlacementConstraint> objects are overlappping constraints
	 * (i.e., SLICEL and SLICEM).
	 */
	/*
	public Set<Set<AreaPlacementConstraint>> getAllNonOverlappingConstraintSets() {
		Set<AreaPlacementConstraint> allConstraints = getAllConstraints();
		Map<AreaPlacementConstraint,Set<AreaPlacementConstraint>> constraintSetMap =
			new HashMap<AreaPlacementConstraint, Set<AreaPlacementConstraint>>();
		Set<Set<AreaPlacementConstraint>> nonOverlappingConstraints = new HashSet<Set<AreaPlacementConstraint>>();

		// Look at every constraint in the system. See if it overlaps with any other constraint.
		for (AreaPlacementConstraint apc : allConstraints) {
			// see if an overlap has already been found
			if (constraintSetMap.get(apc) != null)
				continue;
			Set<AreaPlacementConstraint> overlappingConstraints = new HashSet<AreaPlacementConstraint>();
			for (AreaPlacementConstraint apc2 : allConstraints) {
				if (apc == apc2)
					continue;
				// if the coordinate systems overlap
				if (TypeSiteCoordinates.doCoordinatesOverlap(apc.coordinates, apc2.coordinates)) {
					overlappingConstraints.add(apc2);
				}				
			}
			// Now see if there was any overlap
			if (overlappingConstraints.size() > 0) {
				// There was overlap. Add the outer loop APC to the Set. This set indicates those
				// constraints that overlap. Update the map
				overlappingConstraints.add(apc);
				for (AreaPlacementConstraint apc3 : overlappingConstraints) {
					constraintSetMap.put(apc3, overlappingConstraints);
				}
				nonOverlappingConstraints.add(overlappingConstraints);
			} else {
				// No overlap. Create a single set
				Set<AreaPlacementConstraint> nonOverlappingConstraint = new HashSet<AreaPlacementConstraint>(1);
				nonOverlappingConstraint.add(apc);
				constraintSetMap.put(apc,nonOverlappingConstraint);
				nonOverlappingConstraints.add(nonOverlappingConstraint);
			}
		}
		return nonOverlappingConstraints;
	}
	*/

	public DesignPlacementGroups<CTYPE, CSITE> getDesignPlacementGroups() {
		return placementGroups;
	}

	/**
	 * Returns the instance that is placed at a given site.
	 */
	public Cluster<CTYPE, CSITE> getPlacedCluster(CSITE site) {
		return siteInstanceMap.get(site);
	}

	/** Return all groups that use the given constraint. */
	public Set<PlacementGroup<CTYPE, CSITE>> getGroupsUsingConstraint(
			AreaPlacementConstraint<CTYPE, CSITE> apc
	) {
		Set<PlacementGroup<CTYPE, CSITE>> groupsOfThisConstraint = new HashSet<>();
		for (PlacementGroup<CTYPE, CSITE> group : getPlacementGroups()) {
			if (getGroupConstraint(group) == apc)
				groupsOfThisConstraint.add(group);
		}
		return groupsOfThisConstraint;
	}

	
	public boolean isPlaced(PlacementGroup<CTYPE, CSITE> g) {
		return getGroupAnchorSite(g) != null;
	}
	
	public boolean canBePlaced() {
		return canBePlaced;
	}

	/**
	 * Determines the groups that need to be placed. Will return null if the design
	 * cannot be placed.
	 */
	public Set<PlacementGroup<CTYPE, CSITE>> getGroupsToPlace() {
		if (!canBePlaced()) {
			return null;
		}
		
		// Create a list of groups that need to be placed		
		Set<PlacementGroup<CTYPE, CSITE>> groupsToPlace =
			new HashSet<>(getPlacementGroups());

		// Remove those groups from the list that have already been placed
		Set<PlacementGroup<CTYPE, CSITE>> groupsNotNeedingPlacement = new HashSet<>();
		for (PlacementGroup<CTYPE, CSITE> pg : groupsToPlace) {
			if (getGroupAnchorSite(pg) != null)
				groupsNotNeedingPlacement.add(pg);
		}
		// Remove the groups that have already been placed.
		groupsToPlace.removeAll(groupsNotNeedingPlacement);
		return groupsToPlace;
	}
	
	/**
	 * Returns the group placed at this site.
	 */
	public PlacementGroup<CTYPE, CSITE> getPlacedGroup(CSITE site) {
		Cluster<CTYPE, CSITE> i = getPlacedCluster(site);
		if (i != null)
			return getGroup(i);
		return null;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods for querying about site locations. These use the current state and do NOT
	// change the state of this class.
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/** Return the site of the given instance if the group were placed at the given anchor. */
	public CSITE getClusterSite(
			Cluster<CTYPE, CSITE> i, PlacementGroup<CTYPE, CSITE> group, CSITE anchorSite
	) {
		AreaPlacementConstraint<CTYPE, CSITE> apc = groupAreaConstraintMap.get(group);
		if (apc != null) {
			return group.getClusterSite(i, anchorSite, apc);
		}
		return null;
	}
	
	/** Return the current site of the given instance. */
	public CSITE getCurrentClusterSite(Cluster<CTYPE, CSITE> i) {
		PlacementGroup<CTYPE, CSITE> g = getGroup(i);
		if (g != null)
			return g.getClusterSite(i, getGroupAnchorSite(g), groupAreaConstraintMap.get(g));
		return null;
	}
	
	
	/**
	 * Returns a set of groups that overlap with the group passed in as a parameter if the
	 * group is placed at the PrimitiveSite passed in as a parameter.
	 */
	public Set<PlacementGroup<CTYPE, CSITE>> getOverlappingPlacementGroups(
			PlacementGroup<CTYPE, CSITE> group, CSITE site) {
		Set<PlacementGroup<CTYPE, CSITE>> overlappingGroups = new HashSet<>();
		Set<CSITE> targetSites = group.getGroupSites(site,groupAreaConstraintMap.get(group));
		for (CSITE targetSite : targetSites) {
			PlacementGroup<CTYPE, CSITE> g = getPlacedGroup(targetSite);
			if (g != null) {
				overlappingGroups.add(g);
			}
		}
		return overlappingGroups;
	}

	/** Determines whether there is an overlapping group if the given group is
	 *  placed at the given anchor.
	 */
	public boolean isGroupOverlapping(PlacementGroup<CTYPE, CSITE> group, CSITE site) {
		Set<CSITE> targetSites = group.getGroupSites(site,groupAreaConstraintMap.get(group));
		for (CSITE targetSite : targetSites) {
			PlacementGroup g = getPlacedGroup(targetSite);
			if (g != null)
				return true;
		}
		return false;
	}

	/** Determines whether the given site is a valid placement site
	 * for the group anchor. */
	public boolean isValidAnchorPlacementSiteSite(PlacementGroup<CTYPE, CSITE> g, CSITE site) {
		AreaPlacementConstraint<CTYPE, CSITE> apc = groupAreaConstraintMap.get(g);
		if (apc != null) {
			return apc.isValidAnchorPlacementSite(g, site);
		}
		return false;
	}
	
	/** Determines the the sites occupied by a group if placed at the given anchor. */
	public Set<CSITE> getGroupSites(PlacementGroup<CTYPE, CSITE> g, CSITE anchorSite) {
		AreaPlacementConstraint<CTYPE, CSITE> apc = groupAreaConstraintMap.get(g);
		if (apc != null) {
			return g.getGroupSites(anchorSite, apc);
		} else
			System.out.println("Warning: no constraint for group "+g);
		return null;		
	}

	/** Determines the the sites currently occupied by a group. If the group has not yet
	 *  been placed, return null.  */
	public Set<CSITE> getGroupSites(PlacementGroup<CTYPE, CSITE> g) {
		AreaPlacementConstraint<CTYPE, CSITE> apc = groupAreaConstraintMap.get(g);
		CSITE anchor = getGroupAnchorSite(g);
		if (anchor == null)
			return null;
		if (apc != null) {
			return g.getGroupSites(anchor, apc);
		} else {
			System.out.println("Warning: no constraint for group "+g);
			return null;		
		}		
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods that select placement sites
	////////////////////////////////////////////////////////////////////////////////////////////////////
	public CSITE getValidRandomSiteWithinRange(
			PlacementGroup<CTYPE, CSITE> group, CSITE baseSite, int range, Random rng
	) {
		CSITE newSite;
		int iteration = 0;
		final int SEARCH_LIMIT= 10000;
		do {
			if (range > 0) {
				newSite =
					getGroupConstraint(group).getValidRandomSiteWithinRange(baseSite, range, rng);
			} else {
				newSite =
					getGroupConstraint(group).getValidRandomSite(rng);						
			}
			if (DEBUG >= DEBUG_MEDIUM)
				System.out.print("  Proposed site:"+newSite);
			if (!isValidAnchorPlacementSiteSite(group, newSite)) {
				newSite = null;
				if (DEBUG >= DEBUG_MEDIUM)
					System.out.println(" - invalid");
			} else {
				if (DEBUG >= DEBUG_MEDIUM)
					System.out.println(" - valid");				
			}
			iteration++;
		} while (newSite == null && iteration < SEARCH_LIMIT);
		return newSite;
	}
	
	public CSITE getClosestRandomSite(
			CSITE site, PlacementGroup<CTYPE, CSITE> group, Random rand, int max_range
	) {
		CSITE closestSite = null;
		for (int i = 1; i < max_range; i++) {
			closestSite = getValidRandomSiteWithinRange(group, site, i, rand);
			if (closestSite != null)
				return closestSite;
		}
		return null;
	}

	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods that change the state of the placer (after initialization)
	////////////////////////////////////////////////////////////////////////////////////////////////////
	public void placeGroupsOfMove(PlacerMove<CTYPE, CSITE> psMove, boolean isUndo) {
		// unplace all groups
		for(PlacementGroupMoveDescription<CTYPE, CSITE> move : psMove.getMoves()) {
			unplaceGroup(move.getGroup());
		}
		// place all groups
		for(PlacementGroupMoveDescription<CTYPE, CSITE> move : psMove.getMoves()) {
			CSITE placementSite;
			if(isUndo) {
				placementSite = move.getOldSite();
			}
			else {
				placementSite = move.getNewSite();
			}
			placeGroup(move.getGroup(), placementSite);
		}
	}

	/**
	 * Modify placement information of a group. This method assumes that the
	 * PrimitiveSite anchor used for placement is valid. If the new site is
	 * null, unplaceGroup will be called.
	 * 
	 * This method will update both placement state objects to keep them
	 * consistent: 
	 *  - groupAnchorMap
	 *  - siteInstanceMap.
	 */
	protected void placeGroup(PlacementGroup<CTYPE, CSITE> group, CSITE newSite) {
		if (newSite == null)
			unplaceGroup(group);
		else {
			// Update the new anchor location of the group
			groupAnchorMap.put(group,newSite);

			// Update the siteInstanceMap with the Instances of the group
			for (Cluster<CTYPE, CSITE> i : group.getClusters()) {
				CSITE s = group.getClusterSite(i, newSite, groupAreaConstraintMap.get(group));
				if (s == null) {
					System.out.println("Warning: attempting to place an instance to a null site");					
				}
				siteInstanceMap.put(s,i);
			}
		}
	}
	
	/**
	 * Remove the placement information for a given group. The group is
	 * considered "unplaced" with no location after calling this method.
	 * 
	 * Both placement state objects are updated in this method: the groupAnchorMap
	 * and the siteInstanceMap.
	 */
	public void unplaceGroup(PlacementGroup<CTYPE, CSITE> group) {

		// Remove primitive sites used in placement from siteInstanceMap
		Set<CSITE> oldGroupSites = getGroupSites(group);
		if (oldGroupSites != null)
			for (CSITE oldSite : oldGroupSites) {
				siteInstanceMap.remove(oldSite);
			}

		// Remove group anchor
		groupAnchorMap.remove(group);

	}

	public void unplaceAllGroups() {
		groupAnchorMap.clear();
		siteInstanceMap.clear();
		//for (PlacementGroup g : this.getPlacementGroups())
		//	unplaceGroup(g);
	}
	
	/**
	 * The placement of groups is "tentatively" stored in the objects
	 * contained within the state. When placement is done, this placement
	 * information needs to be transfered to the actual Instance elements
	 * themselves. This method will transfer this information. This method
	 * should be called AFTER completing the placement process.
	 */
	public void finalizePlacement(ClusterDesign<CTYPE, CSITE> design) {
		for (PlacementGroup<CTYPE, CSITE> group : placementGroups.getPlacementGroups()) {
			for (Cluster<CTYPE, CSITE> cluster : group.getClusters()) {
				CSITE groupAnchorSite = getGroupAnchorSite(group);
				System.out.println(cluster + " placed @" + group.getClusterSite(cluster,
						groupAnchorSite, groupAreaConstraintMap.get(group)));
			}
		}

		for (PlacementGroup<CTYPE, CSITE> group : placementGroups.getPlacementGroups()) {
			Set<Cluster<CTYPE, CSITE>> instances = group.getClusters();
			CSITE anchor = getGroupAnchorSite(group);
			for (Cluster<CTYPE, CSITE> i : instances) {
				CSITE site = group.getClusterSite(i, anchor, groupAreaConstraintMap.get(group));
				if (site != null)
					design.placeCluster(i, site);
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	// 
	//  Miscelleaneous methods
	////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Go through the set of moves and make sure that they result in no
	 * overlap.
	 */
	public boolean validMoveSet(Set<PlacementGroupMoveDescription<CTYPE, CSITE>> moves) {
		// TODO: why is this method necessary?
		return true;
	}

	public Comparator<PlacementGroup<CTYPE, CSITE>> getPlacementGroupProbabilityComparator() {
		return new PlacementGroupPlacementComparator<>(this);
	}
	
	public static String rectString(Rectangle rect) {
		return "[" + rect.x+","+rect.y+","+rect.width+","+rect.height+"]";
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	// 
	// 
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean isMediumDebug() {
		return DEBUG >= DEBUG_MEDIUM;
	}

	public boolean isMinimumDebug() {
		return DEBUG >= DEBUG_MIN;
	}
	
	public boolean isMaximumDebug() {
		return DEBUG >= DEBUG_FULL;
	}
	
	public void setDebug(int d) {
		DEBUG = d;
	}
	public void setNoDebug() { DEBUG = NO_DEBUG; }
	public void setMinDebug() { DEBUG = DEBUG_MIN; }
	public void setMediumDebug() { DEBUG = DEBUG_MEDIUM; }
	public void setMaxDebug() { DEBUG = DEBUG_FULL; }
	
	/** Debug flags */
	public final int NO_DEBUG = 0;	
	public final int DEBUG_MIN = 20;
	public final int DEBUG_MEDIUM = 50;
	public final int DEBUG_FULL = 100;

	/** Debug flag. Used for all placing. */
	protected int DEBUG = NO_DEBUG;

	
	/**
	 * Indicates whether this current placement state can be placed. This is true
	 * if the constraints and the design match up. If the design cannot fit
	 * in the constraints, it cannot be placed. This determination is made
	 * in this class to avoid it being made by the placers.
	 */
	boolean canBePlaced;


}

/**
 * This is a comparator in which each group has a probability associated with it.
 * The comparator keeps this probability state. The probability of a placement group
 * is 1/# of valid placement sites. Those groups with fewer possible sites have a higher
 * probability than those with more possible sites. This comparator is used to prioritize
 * placement - those groups with fewer possible placement locations should be given
 * priority in placement over those with more possible placement locations.
 */
class PlacementGroupPlacementComparator<CTYPE extends ClusterType, CSITE extends ClusterSite>
		implements Comparator<PlacementGroup<CTYPE, CSITE>>
{

	public PlacementGroupPlacementComparator(PlacerState<CTYPE, CSITE> s) {
		groupProbabilities = new HashMap<>(s.getPlacementGroups().size());
		for (PlacementGroup<CTYPE, CSITE> g : s.getPlacedPlacementGroups()) {
			AreaPlacementConstraint<CTYPE, CSITE> constraint = s.getGroupConstraint(g);
			Set<CSITE> possibleAnchorSites = constraint.getValidAnchorSites(g);
			float placementProbability;
			if (possibleAnchorSites == null || possibleAnchorSites.size() == 0) {
				System.out.println("Warning: no placement sites f or group "+g);
				placementProbability = Float.MIN_VALUE;
			} else {
				placementProbability = 1.0f / possibleAnchorSites.size();
			}
			groupProbabilities.put(g,placementProbability);				
		}
		
	}
	
	public int compare(PlacementGroup<CTYPE, CSITE> a, PlacementGroup<CTYPE, CSITE> b) {
		float aProbability = getGroupProbability(a);
		float bProbability = getGroupProbability(b);
		if (aProbability < bProbability) return 1;
		else if(aProbability > bProbability) return -1;
		else return 0;
	}
	
	public float getGroupProbability(PlacementGroup<CTYPE, CSITE> g) {
		Float f = groupProbabilities.get(g);
		if (f != null)
			return f;
		return Float.MIN_VALUE;

		/*
		AreaPlacementConstraint constraint = state.getGroupConstraint(g);
		Set<PrimitiveSite> possibleAnchorSites = constraint.getValidAnchorSites(g);
		float placementProbability = 0;
		if (possibleAnchorSites == null || possibleAnchorSites.size() == 0) {
			System.out.println("Warning: no placement sites f or group "+g);
			placementProbability = Float.MIN_VALUE;
		} else {
			placementProbability = 1.0f / possibleAnchorSites.size();
		}
		groupProbabilities.put(g,placementProbability);
		return placementProbability;
		*/
	}
	
	//PlacerState state;
	Map<PlacementGroup<CTYPE, CSITE>,Float> groupProbabilities;
}

