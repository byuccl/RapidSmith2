/*
 * Copyright (c) 2016 Brigham Young University
 *
 * This file is part of the BYU RapidSmith Tools.
 *
 * BYU RapidSmith Tools is free software: you may redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * BYU RapidSmith Tools is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * A copy of the GNU General Public License is included with the BYU
 * RapidSmith Tools. It can be found at doc/LICENSE.GPL3.TXT. You may
 * also get a copy of the license at <http://www.gnu.org/licenses/>.
 */
package edu.byu.ece.rapidSmith.constraints;

public class AreaGroupRange {
	private AreaGroupCoordinate lowerLeftCoordinate;
	private AreaGroupCoordinate upperRightCoordinate;
	private String rangeType;
	private String areaGroupName;
	
	public AreaGroupRange(String rangeType, int ll_x, int ll_y, int ur_x, int ur_y) {
		this.rangeType = rangeType;
		setLLCoordinate(ll_x, ll_y);
		setURCoordinate(ur_x, ur_y);
	}
	
	public AreaGroupRange(String areaGroupName, String rangeType, int ll_x, int ll_y, int ur_x, int ur_y) {
		this.areaGroupName = areaGroupName;
		this.rangeType = rangeType;
		setLLCoordinate(ll_x, ll_y);
		setURCoordinate(ur_x, ur_y);
	}
	
	//TODO: are the x/y boundaries for area_groups inclusive, or exclusive?  Guessing inclusive for now; might cause problems later
	public boolean containsPoint(int x, int y) {
		return x >= lowerLeftCoordinate.getX() && x <= upperRightCoordinate.getX() &&
				y >= lowerLeftCoordinate.getY() && y <= upperRightCoordinate.getY();
	}
	
	public void setAreaGroupName(String areaGroupName) {
		this.areaGroupName = areaGroupName;
	}
	
	public String getAreaGroupName() {
		return areaGroupName;
	}
	
	public void setLLCoordinate(int x, int y) {
		lowerLeftCoordinate = new AreaGroupCoordinate(x, y);
	}
	
	public void setURCoordinate(int x, int y) {
		upperRightCoordinate = new AreaGroupCoordinate(x, y);
	}
	
	public void setRangeType(String rangeType) {
		this.rangeType = rangeType;
	}
	
	public String getRangeType() {
		return rangeType;
	}
	
	public String toString() {
		return areaGroupName + " " + rangeType + "_" +
				lowerLeftCoordinate.toString() + ":" +
				upperRightCoordinate.toString();
	}

	protected class AreaGroupCoordinate {
		private int x;
		private int y;
		
		public AreaGroupCoordinate(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		public String toString() {
			return "X" + x + "Y" + y;
		}
		
		public int getX() {
			return x;
		}
		
		public int getY() {
			return y;
		}
		
	}
}
