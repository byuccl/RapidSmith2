package edu.byu.ece.rapidSmith.design;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;
import edu.byu.ece.rapidSmith.cad.clusters.UnpackedCellCluster;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.clusters.tileCluster.TileClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.tileCluster.TileClusterType;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;

import java.util.*;

/**
 *
 */
public class ClusterDesign
		<CTYPE extends ClusterType, CSITE extends ClusterSite>
		extends CellDesign
{
	/** This is a list of all the cells in the design */
	private Map<String, Cluster<CTYPE, CSITE>> clusterMap = new HashMap<>();
	/** A map used to keep track of all used primitive sites used by the design */
	private Map<CSITE, Cluster<CTYPE, CSITE>> placementMap = new HashMap<>();
	private Device device;

	/**
	 * Constructor which initializes all member data structures. Sets name and
	 * partName to null.
	 */
	public ClusterDesign() {
		super();
	}

	/**
	 * Creates a new design and populates it with the given design name and
	 * part name.
	 *
	 * @param designName The name of the newly created design.
	 * @param partName   The target part name of the newly created design.
	 */
	public ClusterDesign(String designName, String partName) {
		super(designName, partName);
		this.device = Device.getInstance(partName);
	}

	/**
	 * Returns all of the clusters in this design.  The returned collection should not
	 * be modified.
	 *
	 * @return the clusters in this design
	 */
	public Collection<Cluster<CTYPE, CSITE>> getClusters() {
		return clusterMap.values();
	}

	/**
	 * Adds a cluster to this design.  The name of this added cluster should be unique
	 * to this design.  The cluster should not be part of another design and should
	 * not have any placement information.  Returns the added cluster for convenience.
	 *
	 * @param cluster the cluster to add
	 * @return the added cluster
	 */
	public Cluster<CTYPE, CSITE> addCluster(Cluster<CTYPE, CSITE> cluster) {
		Objects.requireNonNull(cluster);
		if (clusterMap.containsKey(cluster.getName()))
			throw new DesignAssemblyException("Cluster<CTYPE, CSITE> with name already exists in design.");

		clusterMap.put(cluster.getName(), cluster);
		return cluster;
	}

	public void removeCluster(Cluster<CTYPE, CSITE> cluster) {
		unplaceCluster(cluster);
		clusterMap.remove(cluster.getName(), cluster);
	}

	public Cluster<CTYPE, CSITE> getClusterAtLocation(CSITE site) {
		Objects.requireNonNull(site);
		return placementMap.get(site);
	}

	public boolean isLocationUsed(CSITE site) {
		Objects.requireNonNull(site);

		return placementMap.containsKey(site);
	}

	public Collection<CSITE> getUsedLocations() {
		return placementMap.keySet();
	}

	public void placeCluster(Cluster<CTYPE, CSITE> cluster, CSITE site) {
		Objects.requireNonNull(cluster);
		Objects.requireNonNull(site);
		if (cluster.isPlaced())
			throw new DesignAssemblyException("Cannot re-place cell.");
		if (placementMap.containsKey(site))
			throw new DesignAssemblyException("Cell already placed at location.");

		placementMap.put(site, cluster);
		cluster.place(site);
	}

	public boolean swap(Cluster<CTYPE, CSITE> cluster1, Cluster<CTYPE, CSITE> cluster2) {
		Objects.requireNonNull(cluster1);
		Objects.requireNonNull(cluster2);

		if (!cluster1.isPlaced())
			throw new DesignAssemblyException("Cell must be placed to swap.");

		if (!cluster2.isPlaced())
			throw new DesignAssemblyException("Cell must be placed to swap.");

		if (cluster1 == cluster2)
			return false; // Swapping with self does nothing
		return swap_impl(cluster1, cluster2.getPlacement());
	}

	public boolean swap(Cluster<CTYPE, CSITE> cluster1, CSITE location2) {
		Objects.requireNonNull(cluster1);
		Objects.requireNonNull(location2);

		if (!cluster1.isPlaced())
			throw new DesignAssemblyException("Cell must be placed to swap.");

		if (cluster1.getPlacement().equals(location2))
			return false;  // Attempting to swap with self
		return swap_impl(cluster1, location2);
	}

	private boolean swap_impl(Cluster<CTYPE, CSITE> cluster1, CSITE site2) {
		CSITE site1 = cluster1.getPlacement();
		Cluster<CTYPE, CSITE> cluster2 = getClusterAtLocation(site2);

		if (cluster2 != null) {
			placementMap.put(site1, cluster2);
			cluster2.place(site1);
		} else {
			placementMap.remove(site1);
		}
		placementMap.put(site2, cluster1);
		cluster1.place(site2);

		return true;
	}

	public void unplaceCluster(Cluster<CTYPE, CSITE> cluster) {
		Objects.requireNonNull(cluster);
		placementMap.remove(cluster.getPlacement());
		cluster.unplace();
	}

	public CellDesign flatten() {
		CellDesign cellDesign = new CellDesign(getName(), getPartName());

		for (Cluster<CTYPE, CSITE> cluster : getClusters()) {
			if (!cluster.isPlaced()) {
				assert cluster instanceof UnpackedCellCluster;
				Cell cell = ((UnpackedCellCluster) cluster).getCell();
				Cell newCell = cell.deepCopy();
				cellDesign.addCell(newCell);
			} else {
				cluster.commitPlacement();
				for (Map.Entry<Bel, PackCell> e : cluster.getPlacementMap().entrySet()) {
					Cell cell = e.getValue().deepCopy();
					cellDesign.addCell(cell);
					cellDesign.placeCell(cell, e.getKey());
				}
			}
		}

		for (CellNet net : getNets()) {
			CellNet copy = net.deepCopy();
			cellDesign.addNet(copy);
			for (CellPin pin : net.getPins()) {
				String cellName = pin.getCell().getName();
				CellPin copyPin = cellDesign.getCell(cellName).getPin(pin.getName());
				copy.connectToPin(copyPin);
			}
		}

		for (Cluster<?, ?> cluster : getClusters()) {
			if (cluster.getPinMap() == null) {
				for (Cell oldCell : cluster.getCells()) {
					Cell newCell = cellDesign.getCell(oldCell.getName());
					if (newCell.isPlaced()) {
						for (CellPin pin : newCell.getPins()) {
							if (pin.getNet() == null)
								continue;
							List<BelPin> belPins = pin.getPossibleBelPins();
							assert belPins.size() == 1;
							pin.setBelPin(belPins.get(0));
						}
					}
				}
			} else {
				for (Map.Entry<CellPin, BelPin> e : cluster.getPinMap().entrySet()) {
					CellPin oldPin = e.getKey();
					Cell newCell = cellDesign.getCell(oldPin.getCell().getName());
					CellPin newPin = newCell.getPin(oldPin.getName());
					newPin.setBelPin(e.getValue());
				}

				for (Map.Entry<CellNet, List<RouteTree>> e : cluster.getRouteTreeMap().entrySet()) {
					CellNet oldNet = e.getKey();
					CellNet newNet = cellDesign.getNet(oldNet.getName());
					for (RouteTree rt : e.getValue())
						newNet.addRouteTree(rt);
				}
			}
		}

		return cellDesign;
	}
}
