package edu.byu.ece.rapidSmith.examples2.placerDemo;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.device.Tile;

/**
 * Represents a cluster of cells on a single site. These are the objects that are moved around
 * while the simulated annealing placer is running.
 * 
 * @author Thomas Townsend
 */
public class SiteCluster {

	protected SiteType sitetype;
	//the current location of this Site Cluster
	protected Site currentLOC;
	
	protected Tile currentTile; 
	
	//the possible new location of the site cluster if it gets switched
	protected Site previousLOC;
	
	//parallel arrays
	protected ArrayList<Cell> cells;
	protected ArrayList<String> bels; 
		
	//list of virtual nets that are connected to this cluster
	protected HashSet<VirtualNet> nets;
	
	protected ArrayList<SiteCluster> swappedWith;
	public boolean bramTest = false;
	protected boolean hasMoved;
	
	/*
	 * Constructor
	 */
	public SiteCluster(Site start) {
		this.currentLOC = start;
		this.previousLOC = start;
		this.currentTile = start.getTile();
		this.sitetype = start.getType();
		this.cells = new ArrayList<>();
		this.bels = new ArrayList<>();
		this.nets = new HashSet<>();
		this.swappedWith = new ArrayList<>();
		this.hasMoved = false;
	}
	
	/*Data Accessors*/
	public SiteType getType(){
		return this.sitetype;
	}
	
	public HashSet<VirtualNet> getNets () {
		return this.nets;
	}
	
	public boolean hasMoved(){
		return this.hasMoved;
	}
	
	public ArrayList<SiteCluster> getAllAffectedSiteClusters(){
		return this.swappedWith;
	}
	
	public HashSet<VirtualNet> getAllAffectedNets() {
		//return this.nets;
		
		if (this.swappedWith.size() == 0) {
			return this.nets;
		}
		else {
			HashSet<VirtualNet> affectedNets = new HashSet<>(this.nets);
	
			for(SiteCluster sc : this.swappedWith) 
				affectedNets.addAll(sc.getNets());
		
			return affectedNets;
		}
	}
		
	public void storeUniqueNets() {}
	
	public void addnet(VirtualNet net) {
		this.nets.add(net);
	}
	
	public Tile getCurrentTile(){
		return this.currentTile;
	}
	
	public Site getPrimitiveSite() {
		return this.currentLOC;
	}
	
	public void setCurrentSite(Site current) {
		//this.previousLOC = this.currentLOC;
		this.currentLOC = current;
		this.previousLOC = current;
		this.currentTile = current.getTile();
		//this.sitetype = current.getType();
	}
	
	public Site getPreviousSite() {
		return this.previousLOC;
	}
	
	public void addCell(Cell c) {
		this.cells.add(c);
		this.bels.add(c.getAnchor().getName());
	}
	
	
	//Applies the final placement to a site cluster by placing each cell
	//onto the corresponding bel in the parallel arrays
	public void applyPlacement(CellDesign design) {
				
		if (!currentLOC.getType().toString().startsWith("SLICE"))
			this.currentLOC.setType(sitetype);
		
		//System.out.println(this.sitetype + " " + currentLOC.getType());
		int i = 0;
		for(Cell c : this.cells) {
			Bel b = currentLOC.getBel(bels.get(i));
								
			design.placeCell(c, b);
			i++;
		}
	}
	
	/*	
	 * Place the site at a random location
	 * Return true if the location is currently empty.
	*/
	public boolean placeRandomly(Device device, Site site, HashMap<Site, SiteCluster> usedSites) {
		if (usedSites.containsKey(site))
			return false;
				
		this.currentLOC = site;
		this.previousLOC = site;
		this.currentTile = site.getTile();
		usedSites.put(site, this);
		return true;	
	}
	
	/*
	 * Reset the current location to the previous location.
	 * Also do this for all sites that this object swapped with
	 */
	public void rejectMove( ) {
		this.currentLOC = this.previousLOC;
		this.currentTile = currentLOC.getTile();
		this.hasMoved = false;
		
		for(SiteCluster sc : this.swappedWith)
			sc.unmakeMove();
	}
	
	public void unmakeMove() {
		this.currentLOC = this.previousLOC;
		this.currentTile = currentLOC.getTile();
		this.hasMoved = false;
	}
	
	/*
	 * Move the current site cluster to the given site.
	 * If there is a site there, perform a swap
	 */
	public boolean makeMove(Site site, HashMap<Site, SiteCluster> sitenameToCluster, Device device) {
		
		this.swappedWith.clear();
		
		//don't swap a site with itself...I'm wondering if doing this check every time is worth it...
		if(currentLOC == site)
			return false; 
		
		//TODO: possibly speed this up...
		if (sitenameToCluster.containsKey(site)) { //swapSite != null) {			
			SiteCluster swapSite = sitenameToCluster.get(site);
			
			//don't swap with the middle of carry chains...this takes too long and is not worth it
			//if (!swapSite.matchesSite(site))
			//	return false;
				
			//Tile t1 = swapSite.getPrimitiveSite().getTile();
			//Tile t2 = site.getTile();
						
			if (!swapSite.swap(currentLOC))
				return false;
			
			this.swappedWith.add(swapSite);
		}
				
		//make the move
		this.previousLOC = this.currentLOC;
		this.currentLOC = site;
		this.currentTile = currentLOC.getTile();
		this.hasMoved = true;
		
		return true;
	}
	
	public boolean swap(Site site) {	
		//System.out.println("SWAP!");
		this.previousLOC = this.currentLOC;
		this.currentLOC = site;
		this.currentTile = currentLOC.getTile();
		this.hasMoved = true;
		
		return true;
	}

	/*
	 * Functions used to accept a swap / move
	 */
	public void acceptSwap(HashMap<Site, SiteCluster> sitenameToCluster) {
		this.previousLOC = this.currentLOC;
		sitenameToCluster.put(currentLOC, this);
		this.hasMoved = false;
	}
	
	public void acceptMove(HashMap<Site, SiteCluster> sitenameToCluster) {
		sitenameToCluster.remove(previousLOC);
		//I'm not sure if I have to do this
		this.previousLOC = this.currentLOC;
		sitenameToCluster.put(currentLOC, this);
		this.hasMoved = false;
		
		for(SiteCluster sc : this.swappedWith)
			sc.acceptSwap(sitenameToCluster);
	}
	
	protected void acceptMoveDependent(HashMap<Site, SiteCluster> sitenameToCluster, SiteCluster parent) {
		sitenameToCluster.remove(previousLOC);
		this.previousLOC = this.currentLOC;
		sitenameToCluster.put(currentLOC, parent);
		this.hasMoved = false;
	}
	
	public boolean matchesSite(Site ps) {
		return currentLOC == ps;
	}
	
	@Override
	public int hashCode() {
		return currentLOC.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SiteCluster that = (SiteCluster) o;
		return Objects.equals(currentLOC, that.currentLOC);
	}
}
