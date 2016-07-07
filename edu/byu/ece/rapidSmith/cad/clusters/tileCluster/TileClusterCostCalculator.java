package edu.byu.ece.rapidSmith.cad.clusters.tileCluster;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterFactory;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.ClusterCostCalculator;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.device.Bel;

/**
 *
 */
public class TileClusterCostCalculator implements ClusterCostCalculator<TileClusterType, TileClusterSite> {
	private static final double BEL_UTILIZATION_FACTOR = 0.5;
	private static final double PIN_UTILIZATION_FACTOR = 0.2;
	private static final double REMAINING_TYPES_FACTOR = 0.3;

	private ClusterFactory<TileClusterType, TileClusterSite> generator;
	private int maxNumOfTypes;

	@Override
	public void init(ClusterFactory<TileClusterType, TileClusterSite> generator) {
		this.generator = generator;
		maxNumOfTypes = -1;
		for (TileClusterType type : generator.getAvailableClusterTypes()) {
			int numTypes = generator.getNumRemainingOfType(type);
			if (numTypes > maxNumOfTypes)
				maxNumOfTypes = numTypes;
		}
	}

	@Override
	public double calculateCost(Cluster<TileClusterType, TileClusterSite> cluster) {
		double belUtilization = calculateBelUtilization(cluster);
		double pinUtilization = calcPinUtilization(cluster);
		double availability = calcAvailability(cluster);

		return 1.0 / (belUtilization * BEL_UTILIZATION_FACTOR +
				pinUtilization * PIN_UTILIZATION_FACTOR +
				availability * REMAINING_TYPES_FACTOR);
	}

	private double calculateBelUtilization(Cluster cluster) {
		int numCells = cluster.getCells().size();
		int numBels = cluster.getTemplate().getBels().size();
		return ((double) numCells) / numBels;
	}

	private double calcPinUtilization(Cluster<TileClusterType, TileClusterSite> cluster) {
		int cellPins = 0;
		int belPins = 0;

		for (Bel bel : cluster.getTemplate().getBels()) {
			belPins += bel.getSources().size();
			belPins += bel.getSinks().size();
		}

		for (Cell cell : cluster.getCells()) {
			for (CellPin pin : cell.getPins()) {
				if (pin.isConnectedToNet())
					cellPins += 1;
			}
		}

		return ((double) cellPins) / belPins;
	}

	private double calcAvailability(Cluster<TileClusterType, TileClusterSite> cluster) {
		return generator.getNumRemainingOfType(cluster.getType()) / ((double) maxNumOfTypes);
	}
}
