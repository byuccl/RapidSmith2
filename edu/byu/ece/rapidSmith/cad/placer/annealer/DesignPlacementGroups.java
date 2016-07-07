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

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;
import edu.byu.ece.rapidSmith.design.ClusterDesign;
import edu.byu.ece.rapidSmith.device.PrimitiveType;

import java.awt.*;
import java.util.*;

/**
 * Creates all of the "static" information necessary for a Design to be placed on a particular
 * Device. This static information can be created from the Design and Device and does NOT need
 * to be serialized. This static information includes:
 * 
 * - PlacementGroup objects of a design. The PlacementGroup objects are the atomic units of
 *   placement within a design and need to be identified before placement can begin.
 * - PlacmentGroup objects that CANNOT be placed by this placer. This is based on the type
 *   of the cluster.
 * - Placement alignment information for each group
 *
 * This class also manages a Map between clusters in the design and the PlacementGroup
 * object that they belong to. This
 * facilitates the identification of PlacementGroups during placement.
 * 
 * @author Mike Wirthlin
 * Created on: May 30, 2012
 */
public class DesignPlacementGroups<CTYPE extends ClusterType, CSITE extends ClusterSite> {

	public DesignPlacementGroups(
			ClusterDesign<CTYPE, CSITE> d,
			TypeSiteCoordinatesFactory<CTYPE, CSITE> tscFactory
	) {
		alignmentValidSiteMap = new HashMap<>();
		design = d;
		this.tscFactory = tscFactory;
		identifyPlaceableClusters();
		createTypeCoordinates();
		createPlacementShapeGroups();
	}
	
	/** Return the design associated with these placement groups. */
	public ClusterDesign<CTYPE, CSITE> getDesign() {
		return design;
	}

	/**
	 * Return the placement groups that can be placed by the placer
	 */
	public Set<PlacementGroup<CTYPE, CSITE>> getPlacementGroups() {
		return groups;
	}

	public TypeSiteCoordinates<CTYPE, CSITE> getCoordinates(CTYPE type) {
		return typeCoordinateSiteMap.get(type);
	}
	
	public Set<Cluster<CTYPE, CSITE>> getClustersToPlace() {
		return clustersToPlace;
	}
		
	public Set<Cluster<CTYPE, CSITE>> getClustersNotToPlace() {
		return clustersThatCannotBePlaced;
	}

	public PlacementGroup<CTYPE, CSITE> getGroup(Cluster<CTYPE, CSITE> i) {
		return clusterGroupMap.get(i);
	}

	public Set<Cluster<CTYPE, CSITE>> getAllClusters() {
		return clusterGroupMap.keySet();
	}

	public Set<PlacementGroup<CTYPE, CSITE>> getMultiClusterPlacementGroups() {
		HashSet<PlacementGroup<CTYPE, CSITE>> multiGroups = new HashSet<>(groups);
		HashSet<PlacementGroup<CTYPE, CSITE>> groupsToRemove = new HashSet<>(groups.size());
		for (PlacementGroup<CTYPE, CSITE> group : multiGroups) {
			if (group.getClusters().size()== 1)
				groupsToRemove.add(group);
		}
		multiGroups.removeAll(groupsToRemove);
		return multiGroups;
	}

	public Comparator<PlacementGroup<CTYPE, CSITE>> getPlacementGroupSizeComparator() {
		return new PlacementGroupSizeComparator<>();
	}
	
	/**
	 * Identify all of the atomic placement groups and create the
	 * data structure for each group.
	 */
	protected void createPlacementShapeGroups() {
		Collection<Cluster<CTYPE, CSITE>> remainingClusters = new HashSet<>(design.getClusters());

		// remove special cases from consideration
		remainingClusters.removeAll(clustersThatCannotBePlaced);
		groups = new HashSet<>();

		// Step 1: Go through the remaining clusters and see if they match any of the known
		// patterns.
		Set<PlacementGroup<CTYPE, CSITE>> newgroups =
				new PlacementGroupFinder<CTYPE, CSITE>().findPlacementGroups(design);
		if (newgroups != null) {
			groups.addAll(newgroups);
			for (PlacementGroup<CTYPE, CSITE> ngroup : newgroups) {
				remainingClusters.removeAll(ngroup.getClusters());
			}
		}

		// Step 2: take care of single cluster groups
		for (Cluster<CTYPE, CSITE> i : remainingClusters) {
			PlacementGroup<CTYPE, CSITE> newGroup = new SingleClusterPlacementGroup<>(i);
			groups.add(newGroup);
		}

		// Step 3: Create the map between clusters and groups
		clusterGroupMap = new HashMap<>(design.getClusters().size());
		for (PlacementGroup<CTYPE, CSITE> group : groups) {
			for (Cluster<CTYPE, CSITE> i : group.getClusters()) {
				clusterGroupMap.put(i, group);
			}
		}
		
	}

	/** Create the type coordiantes for each type used in the design. */
	protected void createTypeCoordinates() {
		typeCoordinateSiteMap =	new HashMap<>();

		// Iterate over all of the clusters and find all the types in the design.
		// For each type:
		//  - Determine all of the compatible sites
		//  - Determine the dimensions of the sites
		for (Cluster<CTYPE, CSITE> i : clustersToPlace) {
			CTYPE t = i.getType();
			// see if this type has been processed yet
			if (typeCoordinateSiteMap.get(t) == null) {
				TypeSiteCoordinates<CTYPE, CSITE> s = tscFactory.make(t);
				typeCoordinateSiteMap.put(t, s);
			}
		}
	}

	/**
	 * Iterates through all of the clusters in the design and determines which clusters
	 * are to be placed through conventional placement and which are to be saved
	 * for special consideration.
	 */
	private void identifyPlaceableClusters() {
		clustersThatCannotBePlaced = new HashSet<>();
		clustersToPlace = new HashSet<>();

		for (Cluster<CTYPE, CSITE> i : design.getClusters()) {
			if (i.isPlaceable()) {
				clustersToPlace.add(i);
			} else {
				clustersThatCannotBePlaced.add(i);
			}
		}
		System.out.println("The following clusters are not going to be placed:");
		for (Cluster t : clustersThatCannotBePlaced) {
			System.out.println("\t" + t);
		}
	}

	public static String rectString(Rectangle rect) {
		return "[" + rect.x+","+rect.y+","+rect.width+","+rect.height+"]";
	}

	/**
	 * A map between each cluster in the design and the group that it
	 * belongs to. This map is created at initialization and should not
	 * change during the placement process.
	 */
	protected Map<Cluster<CTYPE, CSITE>, PlacementGroup<CTYPE, CSITE>> clusterGroupMap;
	
	/**
	 * The placement groups that need to be placed.
	 */
	protected Set<PlacementGroup<CTYPE, CSITE>> groups;

	/**
	 * A Map between each primitive type used in the design and the
	 * type coordinate system used by this type. This is cached so
	 * that this information does not need to be regenerated on the fly.
	 */
	protected Map<CTYPE, TypeSiteCoordinates<CTYPE, CSITE>> typeCoordinateSiteMap;

	/**
	 * The design that is being placed.
	 */
	protected ClusterDesign<CTYPE, CSITE> design;

	/**
	 * A map between each unique placement alignment scheme and the sites
	 * that this alignment can be placed at. This cached map is used to reduce
	 * the memory requirements of the placer - many groups use the same placement
	 * alignment and it is unnecessary to store the primitive valid sites for
	 * each group when they are mostly the same.
	 */
	protected Map<PlacementAlignment<CSITE>, Set<CSITE>> alignmentValidSiteMap;

	/**
	 * The clusters in the design that cannot be placed.
	 */
	protected Set<Cluster<CTYPE, CSITE>> clustersThatCannotBePlaced;
	
	/**
	 * The clusters in the design that must be placed.
	 */
	protected Set<Cluster<CTYPE, CSITE>> clustersToPlace;

	private TypeSiteCoordinatesFactory<CTYPE, CSITE> tscFactory;

	/**
	 * The types of primitives that will be placed with the conventional placer.
	 * All other cluster types will NOT be placed by this placer.
	 * 
	 * This is fixed for V4.
	 */
	// TODO replace with a getter
	public static Set<PrimitiveType> typesToPlace;
	static {
		typesToPlace = new HashSet<>();
		typesToPlace.add(PrimitiveType.SLICE);
		typesToPlace.add(PrimitiveType.SLICEL);
		typesToPlace.add(PrimitiveType.SLICEM);
		typesToPlace.add(PrimitiveType.SLICEX);
		typesToPlace.add(PrimitiveType.RAMB16);
		typesToPlace.add(PrimitiveType.DSP48);
	}

}

class PlacementGroupSizeComparator<CTYPE extends ClusterType, CSITE extends ClusterSite>
		implements Comparator<PlacementGroup<CTYPE, CSITE>>
{
	public PlacementGroupSizeComparator() {
	}
	
	public int compare(PlacementGroup a, PlacementGroup b) {
		if (a.getClusters().size() < b.getClusters().size())
			return 1;
		if (a.getClusters().size() > b.getClusters().size())
			return -1;
		return 0;
	}
	
}


