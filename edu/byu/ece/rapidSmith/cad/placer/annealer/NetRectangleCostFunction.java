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

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.design.ClusterDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;

import java.awt.*;
import java.util.*;

/**
 * A cost function that determines system cost based on nets and their
 * distances. Based on the VPR cost function.
 */
public class NetRectangleCostFunction<CTYPE extends ClusterType, CSITE extends ClusterSite>
		implements PlacerCostFunction<CTYPE, CSITE>
{

	/**
	 * This is the q(i) term used in the VPR cost function (see p.308 of Reconfigurable Computing
	 * and place.c of VPR source)
	 */
	protected static float cross_count[] = {   /* [0..49] */
			1.0f, 1.0f, 1.0f, 1.0828f, 1.1536f, 1.2206f, 1.2823f, 1.3385f, 1.3991f, 1.4493f,
			1.4974f, 1.5455f, 1.5937f, 1.6418f, 1.6899f, 1.7304f, 1.7709f, 1.8114f, 1.8519f, 1.8924f,
			1.9288f, 1.9652f, 2.0015f, 2.0379f, 2.0743f, 2.1061f, 2.1379f, 2.1698f, 2.2016f, 2.2334f,
			2.2646f, 2.2958f, 2.3271f, 2.3583f, 2.3895f, 2.4187f, 2.4479f, 2.4772f, 2.5064f, 2.5356f,
			2.5610f, 2.5864f, 2.6117f, 2.6371f, 2.6625f, 2.6887f, 2.7148f, 2.7410f, 2.7671f, 2.7933f};

	public static Set<CellNet> getRealNets(ClusterDesign<?, ?> d) {
		Set<CellNet> realNets = new HashSet<>();
		for (CellNet n : d.getNets()) {
			if (n.isClkNet() || n.isStaticNet() || n.getFanOut() == 0) {
				continue;
			}
			realNets.add(n);
		}
		return realNets;
	}

	public NetRectangleCostFunction(
			ClusterDesign<CTYPE, CSITE> d,
			PlacerState<CTYPE, CSITE> state
	) {
		design = d;
		this.state = state;
		// prepare the nets for cost function
		netsForCostFunction = getRealNets(design);
	}

	public float getCurrentCost() {
		return netCost;
	}


	public float calcSystemCost() {
		netCost = getTotalNetCost();
		if (DEBUG) System.out.println("Total net cost=" + netCost);
		return netCost;
	}

	public float calcIncrementalCost(PlacerMove<CTYPE, CSITE> move) {

		//for which nets do we need to recalculate the costs?
		Set<CellNet> affectedNets = new HashSet<>();

		for (Cluster<CTYPE, CSITE> i : move.getCluster()) {
			affectedNets.addAll(i.getExternalNets());
		}

		//update the cost of affected nets
		if (DEBUG)
			System.out.println("Orig cost=" + (netCost));

		float diffCost = 0;
		for (CellNet n : affectedNets) {
			if (!netsForCostFunction.contains(n)) continue;
			float oldNetCost = netToCostMap.get(n);
			diffCost -= oldNetCost;
			float netCost = getNetCost(n);

			diffCost += netCost;
			netToCostMap.put(n, netCost);
		}
		if (DEBUG)
			System.out.println("Diff cost=" + diffCost + " new cost=" + (diffCost + netCost));
		netCost += diffCost;
		return netCost;
	}


	/**
	 * Determine the cost based on all nets in the design.
	 */
	protected float getTotalNetCost() {
		float allNetsCost = 0;
		for (CellNet n : design.getNets()) {
			float netCost = getNetCost(n);
			allNetsCost += netCost;
			netToCostMap.put(n, netCost);
		}
		return allNetsCost;
	}

	protected static float getCrossing(CellNet n) {
		float crossing;
		if (n.getFanOut() > 49) {
			crossing = 2.7933f + 0.02616f * (n.getFanOut() - 49);
		} else {
			crossing = cross_count[n.getFanOut()];
		}
		return crossing;
	}

	/**
	 * Determine the bounding box of a net.
	 */
	protected PlacerBoundingRect getNetBounds(CellNet n) {
		PlacerBoundingRect bRect = new PlacerBoundingRect();
		Set<Cluster<CTYPE, CSITE>> attachedClusters = netToClustersMap
				.computeIfAbsent(n, k -> getClustersAttachedToNet(n));
		for (Cluster<CTYPE, CSITE> i : attachedClusters) {
			CSITE iSite = state.getCurrentClusterSite(i);
			//first main
			if (iSite != null) {
				//bRect.addNewPoint(iSite);
				Point iSiteLoc = iSite.getLocation();
				bRect.addNewPoint(iSiteLoc.x, iSiteLoc.y);
			} else {
				// Check and see if the instance is a special case
				if (state.getClustersNotToPlace().contains(i))
					continue;
				System.out.println("WARNING: incomplete placement. Cost will be inaccurate.");
				System.out.println("Instance: " + i.getName());
			}
		}
		return bRect;
	}

	private Set<Cluster<CTYPE, CSITE>> getClustersAttachedToNet(CellNet n) {
		Collection<CellPin> pins = n.getPins();
		Set<Cluster<CTYPE, CSITE>> attachedClusters = new HashSet<>(pins.size() * 3);
		for (CellPin p : pins) {
			PackCell cell = (PackCell) p.getCell();
			Cluster<CTYPE, CSITE> i = (Cluster<CTYPE, CSITE>) cell.getCluster();
			attachedClusters.add(i);
		}
		return attachedClusters;
	}

	/**
	 * Determine the cost of a single net.
	 */
	protected float getNetCost(CellNet n) {
		if (!netsForCostFunction.contains(n))
			return 0;

		PlacerBoundingRect bRect = getNetBounds(n);
		float crossing = getCrossing(n);

		float netCost = crossing * ((bRect.getMaxX() - bRect.getMinX() + 1) + (bRect.getMaxY() - bRect.getMinY() + 1));
		if (netCost < 0) System.out.println("Net cost: " + netCost);
		return netCost;
	}

	/**
	 * Caches the current cost of each net. This cache is used to speed up the time to compute
	 * the cost of the placement when only a few nets are chanced.
	 */

	protected HashMap<CellNet, Float> netToCostMap = new HashMap<>();
	protected HashMap<CellNet, Set<Cluster<CTYPE, CSITE>>> netToClustersMap = new HashMap<>();

	protected ClusterDesign<CTYPE, CSITE> design;

	/** The current state of the placer (used to determine where things are) */
	protected PlacerState<CTYPE, CSITE> state;

	protected float netCost;

	/**
	 * Debug flag for printing messages during cost computation.
	 */
	protected boolean DEBUG = false;

	/**
	 * Used to identify those nets that are used within the cost function
	 * (some nets are not used in computing cost, i.e., clock)
	 */
	protected Set<CellNet> netsForCostFunction;


}
