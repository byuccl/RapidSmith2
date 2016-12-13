package edu.byu.ece.rapidSmith.examples2.placerDemo;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileType;

/**
 * Site cluster object used to represent SLICEL/M carry chains.
 * These sites must be placed together, and so this objects moves
 * the entire carry chain at the same time. It extends the basic 
 * site cluster object and overrides certain methods
 * TODO: Perhaps make SiteCluster an interface, and have this implement the interface? 
 * @author Thomas Townsend
 *
 */
public class CarryChainCluster extends SiteCluster {
	/** ordered list of carry chain elements */
	protected ArrayList<SiteCluster> dependent; 
	protected int carryChainHeight;
	
	private static final HashSet<TileType> clbTileTypes = new HashSet<>(
			Arrays.asList(
					TileType.CLBLL_L,
					TileType.CLBLL_R,
					TileType.CLBLM_L,
					TileType.CLBLM_R,
					TileType.CLBLL,
					TileType.CLBLM
			)
	);	
	
	public CarryChainCluster(Site start) {
		super(start);

		this.dependent = new ArrayList<>();
	}
	
	public ArrayList<SiteCluster> getDependentSiteClusters() {
		return dependent;
	}

	public void setDependentSiteClusters(ArrayList<SiteCluster> dependent) {
		this.dependent = dependent;
	}

	public void addDependentSite(SiteCluster next) {
		this.dependent.add(next);
	}

	public int getCarryChainHeight() {
		return carryChainHeight;
	}

	public void setCarryChainHeight(int carryChainHeight) {
		this.carryChainHeight = carryChainHeight;
	}
	
	@Override
	public void storeUniqueNets() {
		for(SiteCluster sc : this.dependent) 
			this.nets.addAll(sc.getNets());
	}
	
	@Override
	public void applyPlacement(CellDesign design) {
		super.applyPlacement(design);
		
		for(SiteCluster cluster: this.dependent)
			cluster.applyPlacement(design);
	}
	
	/**
	 * Checks to see if the carry chain can be placed starting at the given site. 
	 * Returns true if it can be placed, false otherwise
	 */
	@Override
	public boolean placeRandomly(Device device, Site site, HashMap<Site, SiteCluster> usedSites) {
	
		HashMap<Site, SiteCluster> tmp = new HashMap<>();
		
		if (usedSites.containsKey(site))
			return false;
	
		setCurrentSite(site);	
		int sliceIndex = site.getIndex();
		tmp.put(site, this);
		
		Tile currentTile = site.getTile();
		for(SiteCluster carryTmp : this.dependent) { //int i = 0; i < this.carryChainHeight-1; i++) {
			Tile aboveTile = device.getTile(currentTile.getRow() - 1, currentTile.getColumn());
			
			if (aboveTile == null || !aboveTile.getName().startsWith("CLB"))
				return false;
			
			Site aboveSite = aboveTile.getPrimitiveSite(sliceIndex);
			if(usedSites.containsKey(aboveSite)) 
				return false;
			
			carryTmp.setCurrentSite(aboveSite);
			tmp.put(aboveSite, this);
			
			currentTile = aboveTile;
		}
		
		usedSites.putAll(tmp);
		return true;
	}

	/**
	 * Accept the last move made with this carry-chain
	 */
	@Override
	public void acceptMove(HashMap<Site, SiteCluster> sitenameToCluster) {
		sitenameToCluster.remove(previousLOC);
		this.previousLOC = this.currentLOC;
		sitenameToCluster.put(currentLOC, this);
		this.hasMoved = false;
		
		for(SiteCluster sc: this.dependent) 
			sc.acceptMoveDependent(sitenameToCluster, this);	
		
		for(SiteCluster sc: this.swappedWith)
			sc.acceptSwap(sitenameToCluster);
	}
	
	/**
	 * Reject the last move made with this carry-chain
	 */
	@Override
	public void rejectMove( ) {						
		this.currentLOC = this.previousLOC;
		this.currentTile = currentLOC.getTile();
		this.hasMoved = false;
		
		for(SiteCluster sc: this.dependent) 
			sc.unmakeMove();
		
		for(SiteCluster sc : this.swappedWith)
			sc.unmakeMove();
	}
	
	@SuppressWarnings("unused")
	private boolean isPlacementValid(Site site, Device device) {
		Tile siteTile = site.getTile();	
		Tile endTile = device.getTile(siteTile.getRow() - this.carryChainHeight, siteTile.getColumn()); 
		
		//System.out.println(endTile);
		
		return endTile != null && isCLBTile(endTile); //endTile.getName().startsWith("CLB");
	}
	
	private boolean isCLBTile(Tile t) {
		return clbTileTypes.contains(t.getType());
	}
	
	/**
	 * Moves the carry-chain to the given site. Swaps any site clusters 
	 * that now collide with any of the slices in the carry chain.
	 */
	@Override
	public boolean makeMove(Site site, HashMap<Site, SiteCluster> sitenameToCluster, Device device) {		
		//System.out.println("CARRY CHAIN MOVE");
		
		assert(currentLOC.getName().equals(previousLOC.getName()));
		this.swappedWith.clear();
		
		//TODO: make this check in the top level placement algorithm instead...it would probably be quicker
		if(currentLOC == site)
			return false; 
		
		//swap the anchor carry chain
		Tile swapTile = site.getTile();
		Site swapSite = site;
		SiteCluster swapSiteCluster; 
		
		if (sitenameToCluster.containsKey(swapSite)) { //swapSite != null) {			
			swapSiteCluster = sitenameToCluster.get(swapSite);
				
			//don't swap if its the middle of a carry chain
			//if(!swapSite.matchesSite(site))
			//	return false;
				
			if (!swapSiteCluster.swap(currentLOC))
				return false;
			
			this.swappedWith.add(swapSiteCluster);
		}
		
		//make the move
		this.previousLOC = this.currentLOC;
		this.currentLOC = site;
		this.currentTile = currentLOC.getTile();
		//this.sliceIndex = currentLOC.getIndex();
		this.hasMoved = true;
		
		int siteIndex = site.getIndex();
		
		//swap all of the other carry chains
		for(SiteCluster sc: this.dependent) {
									
			swapTile = device.getTile(swapTile.getRow() - 1, swapTile.getColumn());
			
			if (swapTile.getType().equals(TileType.BRKH_CLB))
				swapTile = device.getTile(swapTile.getRow() - 1, swapTile.getColumn());
			
			//invalid placement (either a non-clb tile in the carry chain location, or we have gone off the edge of the chip)
			if (swapTile == null || !isCLBTile(swapTile)) //!currentTile.getName().startsWith("CL")) 
				return false;
						
			swapSite = swapTile.getPrimitiveSite(siteIndex);
						
			if (sitenameToCluster.containsKey(swapSite)) { //swapSite != null) { // && !this.swappedWith.contains(swapSite)) {	
				swapSiteCluster = sitenameToCluster.get(swapSite);
				
				if (!swapSiteCluster.swap(sc.getPrimitiveSite()))
					return false; 
				
				this.swappedWith.add(swapSiteCluster);
			}
			
			sc.swap(swapSite); //setCurrentSite(currentSite);
		}
		
		return true;
	}
	
	//don't allow carry chain swaps...they are too long and complicated
	@Override
	public boolean swap(Site site) {		
		return false;
	}
	
	//Old swap function...this can swap carry chains with other locations...but I think there is a bug in it
	/*
	@Override
	public boolean swap(Site site, int offset, Device device, HashMap<Site, SiteCluster> sitenameToCluster) {	
		
		return false;
		//System.out.println("Collision!");
		//remove carry chain pieces from used site map
		//sitenameToCluster.remove(this.currentLOC.getName());
		//for(SiteCluster sc: this.dependent) {
		//	sitenameToCluster.remove(sc.getPrimitiveSite().getName());	
		//}
		
		//Tile swapTile = device.getTile(site.getTile().getRow() + offset, site.getTile().getColumn()); 
		
		//if (swapTile == null || !swapTile.getName().startsWith("CLB"))
		//	return false;
	
		//old code start
		int siteIndex = site.getIndex();
		//Site newSite = swapTile.getPrimitiveSite(siteIndex);
		
		//if (sitenameToCluster.containsKey(newSite)) {
		//	return false;
		//}
		
		//make move
		this.previousLOC = this.currentLOC;
		this.currentLOC = site;
		//sitenameToCluster.put(currentLOC.getName(), this);
		Tile swapTile = site.getTile();
		for(SiteCluster sc: this.dependent) {
			swapTile  = device.getTile(swapTile.getRow() - 1, swapTile.getColumn());
			
			//check to make sure the tile above is suitable for a carry chain
			if (swapTile == null || !swapTile.getName().startsWith("CLB"))
				return false;
		
			Site swapSite = swapTile.getPrimitiveSite(siteIndex); 

			if (sitenameToCluster.containsKey(swapSite))
				return false;
			
			sc.setCurrentSite(swapSite);
			//sitenameToCluster.put(swapSite.getName(), this);
		}
		
		return true;
	}
	*/
	
	//Function used for debugging 
	public void printCarryChain() {	
		System.out.println("CarryChain:\n\t" + this.currentLOC.getName() + " " + this.cells.get(0));
		
		for(SiteCluster carry : this.dependent)
			System.out.println("\t" + carry.getPrimitiveSite().getName() + " " + carry.cells.get(0));
	}
	
	
}
