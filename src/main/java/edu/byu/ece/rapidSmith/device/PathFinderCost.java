package edu.byu.ece.rapidSmith.device;

import edu.byu.ece.rapidSmith.device.Connection.ReverseTileWireConnection;

import java.util.*;

public class PathFinderCost {
	private int history;
	private int usedPrev; // congestion
	private int pfCost = 0;
	private static int wireCost; // pipFactor
	private static final int historyFactor = 25; // default = 1
	private static final int congestionFactor = 100; // default = 1.


	private static Map<Tile, Map<Wire, PathFinderCost>> pfcMap = new HashMap<>();
	private static Set<Wire> containedWires = new HashSet<>();

	public PathFinderCost(Wire wire) {
		history = 0;
		usedPrev = 1;
		wireCost = 1;

		//assert (!wire.isUsed());

		if (findIsPip(wire)) {
			setIsPip(wire);
		}
		else {
			Collection<Connection> reverseConns = wire.getReverseWireConnections();
			if (reverseConns.iterator().hasNext()) {

				Wire startWire = null;
				Tile sinkTile = wire.getTile();
				int biggestDist = 0;

				for (Connection conn : reverseConns) {
					int dist = sinkTile.getIndexManhattanDistance(conn.getSinkWire().getTile());

					if (startWire == null || dist > biggestDist) {
						startWire = conn.getSinkWire();
						biggestDist = dist;
					}
				}

				assert (startWire != null);
				addDirectCost(startWire, wire);
			}
		}

		addWire(wire, this);
	}



	private static void addWire(Wire wire, PathFinderCost pfc) {
		assert !containedWires.contains(wire);
		containedWires.add(wire);
		Tile tileOfWire = wire.getTile();
		Map<Wire, PathFinderCost> wireToPFC = pfcMap.get(tileOfWire);
		if (wireToPFC == null) {
			wireToPFC = new HashMap<Wire, PathFinderCost>();
			pfcMap.put(tileOfWire, wireToPFC);
		}
		wireToPFC.put(wire, pfc);
	}

	public static PathFinderCost getPFCOfWire(Wire wire) {
		Tile tileOfWire = wire.getTile();
		Map<Wire, PathFinderCost> wireToPFC = pfcMap.get(tileOfWire);
		if (wireToPFC != null) {
			return wireToPFC.get(wire);
		}
		return null;
	}

	public static PathFinderCost createOrGetPFCOfWire(Wire wire) {
		PathFinderCost alreadyExists = getPFCOfWire(wire);
		if (alreadyExists == null) {
			alreadyExists = new PathFinderCost(wire);
		}
		return alreadyExists;
	}

	public int getWireCost() {
		return 1; // all wires are equal
	}

	public int getCost() {
		//use pipFactor to account for nonPip connections that have to be used
		return pfCost;
	}

	private void calculatePFCost() {
		pfCost = (history + wireCost) * usedPrev;
		assert pfCost > -1;
	}


	public void incrementHistory(int h) {
		history += historyFactor;
		calculatePFCost();
	}

	public void incrementUsage() {
		usedPrev += congestionFactor;
		calculatePFCost();
	}

	public void resetUsage() {
		usedPrev = 1;
		calculatePFCost();
	}

	public int getUsage() {
		return usedPrev;
	}

	public boolean findIsPip(Wire wire){
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


	private void addDirectCost(Wire startWire, Wire endWire) {
		// figure out how much a direct connection should cost
		// (what tile does it take us to?)
		// figure out how much this wire costs
		// If the PIP leads to a wire that connects to a non-programmable connection

		Tile startTile = startWire.getTile();
		Tile endTile = endWire.getTile();

		// figure out what tile conn ends in (should be a different tile)
		int distance = endTile.getIndexManhattanDistance(startTile);

		wireCost = distance;
		calculatePFCost();
	}

	private void setIsPip(Wire wire){
		/*
		   Types of PIPs:
		   CLBLL_L/CLBLL_R/CLBLM_L/CLBLM_R:
		   A "tall" switchbox connects the CLBLL with an interconnect tile next to it. These PIP junctions only have
		   one PIP each. There are also 2 PIPs from the sites' cout pins. Either to a cin of another site (in an above
		   tile) or to the DMUX wire.

           LIOI3/LIOI3_TBYTESRC/LIOI3_TBYTETERM:
           Contains a "tall" switchbox. These PIP junctions have one PIP each.

           Also some non-swithbox PIPs. Maybe these should not cost extra (should not count as PIPs?)

           INT_L/INT_R:
           Contains a large switchbox with 3,737 PIPs (363 PIP junctions). PIP lengths differ. Currently no easy way
           to know how long a PIP "wire" is. Just count each as length 1 for now.

           IO_INT_INTERFACE_L/IO_INT_INTERFACE_R:
           Small set of PIPs. Not sure if they should cost any extra, but probably doesn't matter.


           CLK_HROW_BOT_R:
           Contain some "tall" switchboxes. PIP junctions have several PIPs. They never lead to a connection
           that leaves the tile, which itself is quite long.

		   Don't worry about the following for now:
		   BRAM, DSP, PCIE, GTP

		 */


		FamilyType familyType = wire.getTile().getDevice().getFamily();
		TileType tileType = wire.getTile().getType();

		// Don't count these as PIPs.
		// For example of why, see count16's clk_IBUF net.
		if (tileType.equals(TileType.valueOf(familyType, "LIOI3")) || tileType.equals(TileType.valueOf(familyType, "LIOI3_TBYTESRC"))
				|| tileType.equals(TileType.valueOf(familyType, "LIOI3_TBYTETERM"))) {
			wireCost = 1;

			calculatePFCost();
			return;

		}

		// All PIPs (even ones that just lead to a bounce PIP in the same tile) cost at least 2
		// Non-Pips cost 1
		// We give all PIPs at least +1 more to account for the wire distance of travelling through the switchbox
		// This is still an estimate.
		wireCost = 2;

		calculatePFCost();
	}

}