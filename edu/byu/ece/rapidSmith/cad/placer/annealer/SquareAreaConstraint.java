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

import java.awt.*;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * An area constraint that involves a single square area. The area may be
 * smaller than the total allowed area. PrimitiveSite objects within this
 * square may be null.
 */
public class SquareAreaConstraint<CTYPE extends ClusterType, CSITE extends ClusterSite>
		extends AreaPlacementConstraint<CTYPE, CSITE> {

	/**
	 * Constructor with a specific rectangular area constraint. The Rectangle
	 * specifies a subset region within the TypeSiteCoordinates.
	 */
	public SquareAreaConstraint(TypeSiteCoordinates<CTYPE, CSITE> coord, Rectangle rect) {
		super(coord);
		rectangleConstraint = new Rectangle(rect);
	}

	/**
	 * Constructor without a specific rectangular area constraint.
	 * Size of type coordinates will be used.
	 */
	public SquareAreaConstraint(TypeSiteCoordinates<CTYPE, CSITE> coord) {
		this(coord, coord.getSiteRectangle());
	}

	/**
	 * Return the set of valid sites within the constraint.
	 */
	public Set<CSITE> getValidSites() {
		if (validSites == null) {
			validSites = new HashSet<>(this.area());
			for (int x = rectangleConstraint.x; x < rectangleConstraint.x + rectangleConstraint.width; x++) {
				for (int y = rectangleConstraint.y; y < rectangleConstraint.y + rectangleConstraint.height; y++) {
					CSITE s = getSite(x, y);
					if (s != null)
						validSites.add(s);
				}
			}
		}
		return validSites;
	}

	/**
	 * Return a set of PrimitiveSite objects that are valid anchors for
	 * the given group.
	 * <p>
	 * TODO: There could be a big savings in memory and computation time
	 * if we cached the "unplaceable" valid sites within the
	 * area group for each placement group. The single instance
	 * placement groups won't have any issues here. Only the multi-
	 * instance placement groups will have this infomration and the
	 * places should not be that large.
	 */
	public Set<CSITE> getValidAnchorSites(PlacementGroup<CTYPE, CSITE> group) {
		Set<CSITE> validSites = new HashSet<>();
		for (CSITE ps : getValidSites())
			if (isValidAnchorPlacementSite(group, ps))
				validSites.add(ps);
		return validSites;
	}


	public int area() {
		return rectangleConstraint.height * rectangleConstraint.width;
	}

	/**
	 * Determines whether a given placement group can be placed at the given
	 * site.
	 */
	public boolean isValidAnchorPlacementSite(
			PlacementGroup<CTYPE, CSITE> group, CSITE anchorSite
	) {
		if (anchorSite == null) {
			System.out.println("Warning: requesting a check on a null site");
			return false;
		}

		// 1. See if the site is a valid anchor site based on the group alignment
		PlacementAlignment<CSITE> align = group.getAlignment();
		if (align != null && !align.isValidPlacement(anchorSite))
			return false;

		// 2. Check each instance site and see if it is valid
		for (Cluster<CTYPE, CSITE> i : group.getClusters()) {
			Point p = group.getClusterOffset(i);
			CSITE destSite = getSiteOffset(anchorSite, p.x, p.y);
			if (destSite == null)
				return false;
		}

		return true;
	}

	public CSITE getSite(int column, int row) {
		if (!rectangleConstraint.contains(column, row))
			return null;
		return coordinates.getSite(column, row);
	}

	public CSITE getSiteOffset(CSITE anchor, int column, int row) {
		CSITE site = coordinates.getSiteOffset(anchor, column, row);
		if (site == null)
			return null;
		Point newPoint = coordinates.getSiteCoordinates(site);
		if (!rectangleConstraint.contains(newPoint.x, newPoint.y))
			return null;
		return site;
	}

	public CSITE getValidRandomSite(Random rand) {
		CSITE s;
		int maxIteration = 10000;
		int iteration = 0;
		do {
			float f = rand.nextFloat();
			int siteNum = (int) (area() * f);
			int siteColumnOffset = siteNum % rectangleConstraint.width + rectangleConstraint.x;
			int siteRowOffset = siteNum / rectangleConstraint.width + rectangleConstraint.y;
			s = getSite(siteColumnOffset, siteRowOffset);
			iteration++;
		} while (s == null && iteration < maxIteration);
		if (s == null) {
			System.out.println(" Can;t finda random valid site");
			System.exit(1);
		}
		return s;
	}

	public CSITE getValidRandomSiteWithinRange(CSITE center, int range, Random rand) {
		CSITE newSite;
		Point p = coordinates.getSiteCoordinates(center);

		do {
			int xOffset = getRandomOffset(range, rand);
			int yOffset = getRandomOffset(range, rand);
			int xNew = p.x + xOffset;
			int yNew = p.y + yOffset;

			// the new coordinates fit. Now get the site.
			// Note that if the site returned is null, the loop
			// will continue.
			newSite = getSite(xNew, yNew);
		} while (newSite == null);
		return newSite;
	}

//	public String toString() {
//		return super.coordinates.type + " at " + PlacerState.rectString(rectangleConstraint);
//	}

	/**
	 * Represents the bounds of this square constraint. It is used for quick checking
	 * of bounds of the constraint. It uses the same coordinate system as the
	 * corresponding type coordinate system.
	 */
	protected Rectangle rectangleConstraint;

	/**
	 * A cache of the valid sites of this constraint. This is populate by
	 * the method getValidSites
	 */
	protected Set<CSITE> validSites;

	public String toString() {
		return super.coordinates.getType().toString();
	}
}

