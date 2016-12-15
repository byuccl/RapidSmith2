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

package edu.byu.ece.rapidSmith.examples2.placerDemo;


import java.util.HashMap;

import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.Tile;

/**
 * Implements a BRAM Site Cluster for my placer. This is needed,
 * because there are special cases for placing BRAM's that I need to consider.
 * Only overrides the needed methods from the base SiteCluster class.
 * @author Thomas Townsend
 *
 */

public class BramCluster extends SiteCluster {
	private final boolean isRAMB36;
	private boolean isEmptySwap;
	
	public BramCluster(Site start) {
		super(start);		
		this.isRAMB36 = start.getType().toString().contains("36");
	}
	
	@Override
	public void acceptMove(HashMap<Site, SiteCluster> sitenameToCluster) {
		sitenameToCluster.remove(previousLOC);
		this.previousLOC = this.currentLOC;
		sitenameToCluster.put(currentLOC, this);
		this.hasMoved = false;
		
		//System.out.println(swappedWith.size());
		
		for(SiteCluster sc : this.swappedWith)
			((BramCluster)sc).acceptBramSwap(sitenameToCluster, isEmptySwap);
	}
	
	/**
	 * Custom function to accept a BRAM swap
	 */
	public void acceptBramSwap(HashMap<Site, SiteCluster> sitenameToCluster, boolean emptySwap) {
		if (emptySwap)
			sitenameToCluster.remove(previousLOC);
		
		this.previousLOC = this.currentLOC;
		sitenameToCluster.put(currentLOC, this);
		this.hasMoved = false;
	}
	
	/**
	 * Moves the BRAM cluster to the given site. Swaps any BRAM clusters 
	 * that are in the tile that I am swapping to (even if they are different type of BRAM)
	 */
	@Override
	public boolean makeMove(Site site, HashMap<Site, SiteCluster> sitenameToCluster, Device device) {
		//System.out.println("BRAM MOVE");
		//assert(currentLOC.getName().equals(previousLOC.getName()));
		this.swappedWith.clear();

		//don't swap a BRAM with itself
		if(currentLOC == site)
			return false; 
		

		if (sitenameToCluster.containsKey(site)) {
			SiteCluster swapSite = sitenameToCluster.get(site);
			swapSite.swap(currentLOC); 			
			this.swappedWith.add(swapSite);
			isEmptySwap = false ;
		} 
		else { // check to see if the rest of the tile is occupied...if it is, then we need to swap those sites
			if(isRAMB36) {	
				Tile swapTile = site.getTile();
				
				Site bram18one = swapTile.getPrimitiveSite(0);		
				if (sitenameToCluster.containsKey(bram18one)) {
					SiteCluster swapSite = sitenameToCluster.get(bram18one);
					swapSite.swap(this.currentTile.getPrimitiveSite(0)); 
					this.swappedWith.add(swapSite);
				}
				
				Site bram18two = swapTile.getPrimitiveSite(1);
				if (sitenameToCluster.containsKey(bram18two)) {
					SiteCluster swapSite = sitenameToCluster.get(bram18two);
					swapSite.swap(this.currentTile.getPrimitiveSite(1)); 
					this.swappedWith.add(swapSite);
				}
			}
			else {	
				Tile swapTile = site.getTile();
				
				Site bram36 = swapTile.getPrimitiveSite(2);		
				if (sitenameToCluster.containsKey(bram36)) {
					SiteCluster swapSite = sitenameToCluster.get(bram36);
					swapSite.swap(this.currentTile.getPrimitiveSite(2)); 
					this.swappedWith.add(swapSite);
					
					//Also need to swap the original RAMB18 partner site if applicable
					Site bram18 = currentTile.getPrimitiveSite(1 - currentLOC.getIndex());
					if (sitenameToCluster.containsKey(bram18)) {
						swapSite = sitenameToCluster.get(bram18);
						swapSite.swap(swapTile.getPrimitiveSite(1 - site.getIndex()));
						this.swappedWith.add(swapSite);
					}
				}				
			}
			isEmptySwap = true;
		}
					
		//make the move
		this.previousLOC = this.currentLOC;
		this.currentLOC = site;
		this.currentTile = currentLOC.getTile();
		this.hasMoved = true;
		
		return true;
	}
	
	@Override
	public boolean placeRandomly(Device device, Site site, HashMap<Site, SiteCluster> usedSites) {
		if (usedSites.containsKey(site))
			return false;
		
		if(isRAMB36) {
			Tile selectedTile = site.getTile();
			Site bram18one = selectedTile.getPrimitiveSite(0);
			Site bram18two = selectedTile.getPrimitiveSite(1);
			
			if(usedSites.containsKey(bram18one) || usedSites.containsKey(bram18two))
				return false;
		}
		else {
			Tile selectedTile = site.getTile();
			Site bram36 = selectedTile.getPrimitiveSite(2);
			
			if(usedSites.containsKey(bram36))
				return false;
		}
				
		this.currentLOC = site;
		this.previousLOC = site;
		this.currentTile = site.getTile();
		usedSites.put(site, this);
		return true;	
	}
}
