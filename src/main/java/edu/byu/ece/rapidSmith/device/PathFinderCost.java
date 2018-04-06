package edu.byu.ece.rapidSmith.device;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.byu.ece.rapidSmith.device.Connection.ReverseTileWireConnection;

public class PathFinderCost{
		private int delay;
		private int history;
		private int usedPrev;
		private int pipFactor;
		private int tmpCost;
		private int pfCost = 0;
		String wire;
		
		public static final int multiplyFactorHistory = 1;
		public static final int bounceFactor = 2;
		public static final int increaseCost = 50;
		
		private static Map<Tile, Map<Wire, PathFinderCost>> pfcMap = new HashMap<Tile, Map<Wire, PathFinderCost>>();
		private static Set<Wire> containedWires = new HashSet<Wire>();
		
		public PathFinderCost(int d, Wire wire){
			delay = d;
			history = 0;
			usedPrev = 1;
			this.wire = wire.getFullName();
			tmpCost = 1;
			this.pipFactor = 0;
			if(this.wire.contains("LV")){
				this.pipFactor = 1;
			}
			boolean isPIP = findIsPip(wire);
			setIsPip(isPIP);
			addWire(wire, this);
		}
		
		public PathFinderCost(int d, Wire wire, boolean isPip){
			delay = d;
			history = 0;
			usedPrev = 1;
			this.wire = wire.getFullName();
			tmpCost = 1;
			this.pipFactor = 0;
			if(this.wire.contains("LV")){
				this.pipFactor = 1;
			}
			setIsPip(isPip);
			addWire(wire, this);
		}
		
		private static void addWire(Wire wire, PathFinderCost pfc){
			assert !containedWires.contains(wire);
			containedWires.add(wire);
			Tile tileOfWire = wire.getTile();
			Map<Wire, PathFinderCost> wireToPFC = pfcMap.get(tileOfWire);
			if(wireToPFC == null){
				wireToPFC = new HashMap<Wire, PathFinderCost>();
				pfcMap.put(tileOfWire, wireToPFC);
			}
			wireToPFC.put(wire, pfc);
		}
		
		public static PathFinderCost getPFCOfWire(Wire wire){
			Tile tileOfWire = wire.getTile();
			Map<Wire, PathFinderCost> wireToPFC = pfcMap.get(tileOfWire);
			if(wireToPFC != null){
				return wireToPFC.get(wire);
			}
			return null;
		}
		
		public static PathFinderCost createOrGetPFCOfWire(Wire wire, int d, boolean isPip){
			PathFinderCost alreadyExists = getPFCOfWire(wire);
			if(alreadyExists == null){
				alreadyExists = new PathFinderCost(d, wire, isPip);
			}
			return alreadyExists;
		}
		
		public static PathFinderCost createOrGetPFCOfWire(Wire wire, int d){
			PathFinderCost alreadyExists = getPFCOfWire(wire);
			if(alreadyExists == null){
				alreadyExists = new PathFinderCost(d, wire);
			}
			return alreadyExists;
		}
		
		public static boolean findIsPip(Wire wire){
			boolean isPIP = false;
			WireConnection[] wCons = wire.getReverseWireConnectionsArray();
			if(wCons == null){
				wCons = new WireConnection[0];
			}
			for(WireConnection wCon : wCons){
				Connection con = new ReverseTileWireConnection((TileWire) wire, wCon);
				if(con.isPip()){
					isPIP = true;
					break;
				}
			}
			return isPIP;
		}
		
		private void setIsPip(boolean isPip){
			if(isPip){
				this.pipFactor=1;
			}
			calculatePFCost();
		}
		
		public int getWireCost(){
			return pipFactor;
			//return delay * pipFactor;
		}
		
		public int getCost(){
			//use pipFactor to account for nonPip connections that have to be used
			return pfCost;
		}
		
		private void calculatePFCost(){
			pfCost = (history + pipFactor) * usedPrev;

			if (pfCost <= -1) {
				System.out.println("history: " + history);
				System.out.println("pipFactor: " + pipFactor);
				System.out.println("usedPrev: " + usedPrev);
				System.out.println("pfCost: " + pfCost);
			}

			assert pfCost > -1;
		}
		
		public void incrementHistory(){
			history+=multiplyFactorHistory;
			calculatePFCost();
		}
		
		public void incrementHistory(int h){
			history+=(h*multiplyFactorHistory);
			calculatePFCost();
		}
		
		public void incrementUsage(int iteration){
			usedPrev += (increaseCost * (iteration));
			calculatePFCost();
		}
		
		public void incrementUsage(int u, int iteration){
			usedPrev+=(u*increaseCost*(iteration));
			calculatePFCost();
		}
		
		public void resetUsage(){
			usedPrev = 1;
			calculatePFCost();
		}
		
		public void decrementUsage(){
			usedPrev--;
			assert usedPrev > 0;
			calculatePFCost();
		}
		
		public int getHistory(){
			return history;
		}
		
		public void setTmpCost(int i){
			tmpCost = i;
		}
		
		public int getDelay(){
			return delay;
		}
		
		public int getUsage(){
			return usedPrev;
		}
		
		public int getPipFactor(){
			return pipFactor;
		}
		
		public int getTmpCost(){
			return tmpCost;
		}
	}