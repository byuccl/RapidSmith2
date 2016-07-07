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

import java.awt.*;
import java.util.List;

/**
 * Represents a set of PrimitiveSites for a given PrimitiveType on a
 * specific Device. Each PrimitiveType has its own coordinate system
 * and this class provides an programatic interface to this coordinate
 * system.
 * <p>
 * The valid sites for a type are available
 * using the device.getAllCompatibleSites(PrimitiveType) method.
 * This array of sites, however, is awkward to access. This method
 * will take these sites and organize them as a two dimensional array
 * (see coordinates). This makes it easy to find sites using the
 * method:
 * <p>
 * PrimitiveSite getSite(column,row)
 * <p>
 * Note that not all sites in this two dimensional array are valid -
 * some of them are null and represent unplaceable positions. Users
 * of this class will need to check and make sure that the site is valid.
 * <p>
 * This class also maintains a HashSet of the PrimitiveSite objects
 * for the given PrimitiveType. This HashSet is used to quickly determine
 * whether a given site is within the coordinate system (without having
 * to iterate through the entire 2D array).
 */
public abstract class TypeSiteCoordinates<CTYPE extends ClusterType, CSITE extends ClusterSite> {
	/**
	 * The Type that corresponds to this coordinate system.
	 */
	protected CTYPE type;

	/**
	 * Indicates the dimensions of the 2 dimensional coordinate system.
	 * This is length of each dimension of the array. Although this information
	 * is available from the array, this member avoids having to query the
	 * coordinates array.
	 * <p>
	 * TODO: Remove this. Instead, create a method that provides the
	 * dimensions from the array and returns a Point.
	 */
	protected Point coordinateDimensions;

	public TypeSiteCoordinates(CTYPE t) {
		type = t;
	}

	/**
	 * Return the set of valid sites associated with this coordinate system.
	 */
	public abstract List<CSITE> getValidSites();

	/** Return the type of the coordinate system */
	public CTYPE getType() {
		return type;
	}

	/**
	 * Returns the rectangle definition of the type coordinates.
	 */
	public Rectangle getSiteRectangle() {
		return new Rectangle(0, 0, this.coordinateDimensions.x, this.coordinateDimensions.y);
	}

	/**
	 * Returns the site based on a column and row.
	 */
	public abstract CSITE getSite(int column, int row);

	/**
	 * Returns a PrimitiveSite within the coordinate system that is offset by an integer
	 * amount. If the resulting offset is outside of the coordinates of the type, this
	 * method returns a null. If the offset falls within the coordinates, it may still
	 * return a null if the site is not a valid site.
	 */
	public abstract CSITE getSiteOffset(CSITE site, int xOffset, int yOffset);

	/**
	 * Determines the offset between two PrimitiveSite objects. These sites should be of
	 * the same PrimitiveType.
	 */
	public abstract Point getSiteOffset(CSITE aSite, CSITE bSite);

	public abstract Point getSiteCoordinates(CSITE site);
}
