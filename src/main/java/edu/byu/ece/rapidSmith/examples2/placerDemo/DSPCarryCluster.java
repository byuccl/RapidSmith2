package edu.byu.ece.rapidSmith.examples2.placerDemo;


import java.util.HashMap;

import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileType;


/**
 * Implements a DSP carry-connected site cluster for my placer.
 * Since most of the methods are shared with the basic carry-chain cluster
 * this object inherits from the carry-chain cluster object
 * @author Thomas Townsend
 *
 */
public class DSPCarryCluster extends CarryChainCluster {

	public DSPCarryCluster(Site start) {
		super(start);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Checks to see if the DSP carry cluster can be placed starting at the given site. 
	 * Returns true if it can be placed, false otherwise
	 */
	@Override
	public boolean placeRandomly(Device device, Site site, HashMap<Site, SiteCluster> usedSites) {
	
		//System.out.println("Hello!");
		HashMap<Site, SiteCluster> tmp = new HashMap<>();
		
		if (usedSites.containsKey(site))
			return false;
	
		setCurrentSite(site);	
		tmp.put(site, this);
		
		Tile nextTile = site.getTile();
		int dspIndex = site.getIndex();
		for(SiteCluster dspTmp : this.dependent) { 
			Site aboveSite;
			
			dspIndex++;
			if(dspIndex > 1) {
				nextTile = device.getTile(nextTile.getRow() - 1, nextTile.getColumn());
				
				//block between clock regions...skip to the next tile
				if (nextTile.getType().equals(TileType.BRKH_DSP_R) || nextTile.getType().equals(TileType.BRKH_DSP_L) )
					nextTile = device.getTile(nextTile.getRow() - 1, nextTile.getColumn());
				
				//invalid placement... TODO: update this without using string comparisons...they are too slow!!
				if (nextTile == null || nextTile.getType().equals(TileType.NULL) )
					return false;
				
				dspIndex = 0;
			}
			
			//System.out.println(swapTile.getName() + " " + swapTile.getType());
			aboveSite = nextTile.getPrimitiveSite(dspIndex);
				
			if(usedSites.containsKey(aboveSite)) 
				return false;
			
			dspTmp.setCurrentSite(aboveSite);
			tmp.put(aboveSite, this);
		}
		
		//System.out.println(site.getName());
		
		usedSites.putAll(tmp);
		return true;
	}
	
	/**
	 * Moves the carry-chain cluster to the given site. Swaps any site clusters 
	 * that now collide with any of the DSP cells in the carry chain.
	 */
	@Override
	public boolean makeMove(Site site, HashMap<Site, SiteCluster> sitenameToCluster, Device device) {		
		
		assert(currentLOC.getName().equals(previousLOC.getName()));
		this.swappedWith.clear();
		
		if(currentLOC == site)
			return false; 
			
		Tile swapTile = site.getTile();
		Site swapSite;
		SiteCluster swapSiteCluster; 
		Site swapID = site;
		
		if (sitenameToCluster.containsKey(swapID)) { 					
			swapSiteCluster = sitenameToCluster.get(swapID);
				
			if (!swapSiteCluster.swap(currentLOC))
				return false;
			
			this.swappedWith.add(swapSiteCluster);
			
		}
		
		//make the move
		this.previousLOC = this.currentLOC;
		this.currentLOC = site;
		this.currentTile = currentLOC.getTile();
		this.hasMoved = true;
		
		
		int siteIndex = site.getIndex();
		
		for(SiteCluster sc: this.dependent) {
			//get the next site to swap with
			if(++siteIndex > 1) {
				swapTile = device.getTile(swapTile.getRow() - 1, swapTile.getColumn());
				
				//block between clock regions...skip to the next tile
				if (swapTile.getType().equals(TileType.BRKH_DSP_R) || swapTile.getType().equals(TileType.BRKH_DSP_L) )
					swapTile = device.getTile(swapTile.getRow() - 1, swapTile.getColumn());
				
				//invalid placement... TODO: update this without using string comparisons...they are too slow!!
				if (swapTile == null || swapTile.getType().equals(TileType.NULL) )
					return false;
				
				siteIndex = 0;
			}
			
			//System.out.println(swapTile.getName() + " " + swapTile.getType());
			swapSite = swapTile.getPrimitiveSite(siteIndex);
			
			//check to see if there is currently anything at that location..if there is than perform a swap
			swapID = swapSite;
			if (sitenameToCluster.containsKey(swapID)) {				
				swapSiteCluster = sitenameToCluster.get(swapID);
				
				if (!swapSiteCluster.swap(sc.getPrimitiveSite()))
					return false; 
				
				this.swappedWith.add(swapSiteCluster);
			
			}
			
			sc.swap(swapSite); 
			
		}
		
		return true;
	}

	@SuppressWarnings("unused")
	private boolean isDSPTile(Tile tile){
		TileType type = tile.getType();
		return type.equals(TileType.DSP_L) || type.equals(TileType.DSP_R);
	}
}


