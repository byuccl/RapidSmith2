package edu.byu.ece.rapidSmith.examples2.placerDemo;


import edu.byu.ece.rapidSmith.device.Tile;

/**
 * Class used to represent a net that connects site clusters together. 
 * @author Thomas Townsend
 *
 */
public class VirtualNet {

	private SiteCluster source;
	//private ArrayList<SiteCluster> sinks; 
	private int cost;
	private String name;
	private boolean upToDate = false; 
	
	private SiteCluster[] sinks;
	private int numSinks = 0;
	private int scaleFactor = 0;
	//current bounding box of the net
	private int top;
	private int bottom;
	private int right;
	private int left;
	
	//ID to uniquely identify a net
	private static int nextID = 0;
	private final int uniqueID;
	
	public VirtualNet() {
		//sinks = new ArrayList<SiteCluster>();
		sinks = new SiteCluster[10];
		this.uniqueID = nextID++;
	}
	
	//	Compute the half-perimeter of the bounding box of the net. This helps the placer
	//	determine if it should make a move
	public int calculateCost() {
		Tile sourceTile = source.getCurrentTile();
		
		top = sourceTile.getRow();
		bottom = top;
		right = sourceTile.getColumn();
		left = right;
			
		for (int i = 0; i < this.numSinks; i++) {
			Tile sinkTile = sinks[i].getCurrentTile();
			
			int row = sinkTile.getRow();
			if (row > top) {
				top = row;
			}
			else if (row < bottom) {
				bottom = row;
			}
			
			int column = sinkTile.getColumn();
			if(column > right) {
				right = column;
			}
			else if(column < left) {
				left = column;
			}
		}
		this.cost = ((top - bottom) + (right - left)) * scaleFactor; //* scaleFactor;
		return cost;
	}
	
	/**
	 * Update the cost of the net...but only update the bounding box for the sites that have moved
	 * @return
	 */
	public int updateCost() {
		
		if (source.hasMoved()) 
			updateBoundingBox(source.getSite().getTile());
		
		for (int i = 0; i < this.numSinks; i++) {
			SiteCluster sc = sinks[i];
			
			if (sc.hasMoved()) 
				updateBoundingBox(sc.getSite().getTile());
		}
		
		this.cost =  ((top - bottom) + (right - left)); // * scaleFactor 
		return cost; 
	}
	
	/*
	 *  Function to incrementally update the bounding box
	 */
	private void updateBoundingBox(Tile t){
		int row = t.getRow();
		
		if (row > top) {
			top = row;
		}
		else if (row < bottom) {
			bottom = row;
		}
		
		int column = t.getColumn();
		if(column > right) {
			right = column;
		}
		else if(column < left){
			left = column;
		}
	}
	
	public String getName() {
		return this.name; 
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setCost(int newCost) {
		this.cost = newCost; 
	}
	
	public int getCost() {
		return this.cost;
	}
	
	public void setSource(SiteCluster source) {
		this.source = source; 
	}
	
	public void setSinks(SiteCluster[] sinks){
		this.sinks = sinks;
		this.numSinks = sinks.length;
		this.scaleFactor = numSinks;
	}
	//old sink data types using array list
	//public void addSink(SiteCluster sink) {
	//	this.sinks.add(sink);
	//}
	
	//public void setSinks(ArrayList<SiteCluster> sinks) {
	//	this.sinks = sinks; 
	//}
	
	public boolean isUpToDate() {
		return this.upToDate;
	}
	public void setUpToDate(boolean upToDate) { 
		this.upToDate = upToDate;
	}
	public int getUniqueID(){
		return this.uniqueID;
	}
	
	//each virtual net has a unique ID, so we simply use the ID as the hash 
	//code because it is guaranteed to be unique across objects
	@Override
	public int hashCode() {
		return this.uniqueID;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VirtualNet that = (VirtualNet) o;
		return uniqueID == that.uniqueID;
	}
}
