package edu.byu.ece.rapidSmith.cad.placer.annealer;

import edu.byu.ece.rapidSmith.device.PrimitiveSite;

/**
 * Helper class to help more easily determine the bounding box for a given Net
 *
 * @author whowes
 */
public class PlacerBoundingRect {
	private int maxX;
	private int maxY;
	private int minX;
	private int minY;

	public PlacerBoundingRect() {
		this.maxX = Integer.MIN_VALUE;
		this.minX = Integer.MAX_VALUE;
		this.maxY = Integer.MIN_VALUE;
		this.minY = Integer.MAX_VALUE;
	}

	public void addNewPoint(PrimitiveSite iSite) {
		addNewPoint(iSite.getTile().getColumn(), iSite.getTile().getRow());
	}

	public void addNewPoint(int x, int y) {
		if (x > maxX) maxX = x;
		if (x < minX) minX = x;
		if (y > maxY) maxY = y;
		if (y < minY) minY = y;
	}

	public int getMaxX() {
		return maxX;
	}

	public int getMaxY() {
		return maxY;
	}

	public int getMinX() {
		return minX;
	}

	public int getMinY() {
		return minY;
	}

	public String toString() {
		return "(" + minX + ", " + minY + ") to (" + maxX + ", " + maxY + ")";
	}

	public int getHalfPerimiter() {
		return maxX - minX + 1 + maxY - minY + 1;
	}

	public boolean equals(Object other) {
		if (!(other instanceof PlacerBoundingRect)) return false;
		PlacerBoundingRect or = (PlacerBoundingRect) other;
		return (maxX == or.maxX && maxY == or.maxY && minX == or.minX && minY == or.minY);
	}


}
