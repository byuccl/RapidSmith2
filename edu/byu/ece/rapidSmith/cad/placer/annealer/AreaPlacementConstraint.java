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

import java.util.Random;
import java.util.Set;


/**
 * Represents an area constraint for a particular coordinate system.
 * An area constraint is a region (of any size or shape) within the FPGA
 * for a specific type (i.e., using the coordinate system of
 * the type) where PlacementGroup objects of that type can be placed.
 */
public abstract class AreaPlacementConstraint<CTYPE extends ClusterType, CSITE extends ClusterSite> {

	/** Default constructor. All constraints are based on a single type and
	 * the type must be specified upon construction (immutable).
	 */
	public AreaPlacementConstraint(TypeSiteCoordinates<CTYPE, CSITE> c) {
		this.coordinates = c;
	}

	public CTYPE getType() {
		return coordinates.getType();
	}
	
	/**
	 * Determines a random offset based on a range. The offset will
	 * be between -range <= offset <= +range. This is used to determine
	 * a random distance from a current location.
	 */
	public static int getRandomOffset(int range, Random rand) {
		float r = rand.nextFloat();
		float offset = r * (2*range+1) - range;
		int iOffset;
		if (offset < 0) {
			iOffset = Math.round(-offset);
			iOffset = -iOffset;
		} else {
			iOffset = Math.round(offset);
		}
		//Check to see if offset is hitting max and min
		//if(iOffset == range || iOffset == -range){
			//System.out.println("**********************************");
			//System.out.println("Range: " + range + "    Offset: " + iOffset);
			//System.out.println("**********************************");
		//}
		return iOffset;
	}
	
	/**
	 * Determines if the given site is a valid location within the area constraint
	 * for the anchor of the given group.
	 */
	public abstract boolean isValidAnchorPlacementSite(
			PlacementGroup<CTYPE, CSITE> group, CSITE anchorSite);

	/**
	 * Return all of the valid sites in this area constraint. These sites may not be valid
	 * anchor sites for all groups but they are valid sites that can host an cluster
	 * of this type.
	 */
	public abstract Set<CSITE> getValidSites();

	//public abstract Set<PrimitiveSite> getValidSitesWithinRange(PrimitiveSite site, int dist);
	
	/**
	 * Return a set of PrimitiveSite objects that are valid anchor sites for the given group.
	 * This may not be the same as the getValidSites set as some groups may not be placed
	 * at all sites due to size and shape constraints.
	 */
	public abstract Set<CSITE> getValidAnchorSites(PlacementGroup<CTYPE, CSITE> group);
	
	/**
	 * Return a valid random site for the given constraint.
	 */
	public abstract CSITE getValidRandomSite(Random rand);
	
	public abstract CSITE getValidRandomSiteWithinRange(CSITE site, int dist, Random rand);
	
//	public abstract String toString();
	
	/**
	* Return the given site from the constraint.
	**/
	public CSITE getSite(int column, int row) {
		return coordinates.getSite(column, row);
	}
	public CSITE getSiteOffset(CSITE anchor, int offsetX, int offsetY) {
		return coordinates.getSiteOffset(anchor, offsetX, offsetY);
	}

	/**
	 * Determine the area of the placement constraint.
	 */
	public abstract int area();

	/** Coordinate system for constraint. */
	TypeSiteCoordinates<CTYPE, CSITE> coordinates;
}
