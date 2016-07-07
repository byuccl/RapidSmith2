package edu.byu.ece.rapidSmith.cad.clusters.router;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterDevice;
import edu.byu.ece.rapidSmith.device.Device;

/**
 *
 */
public class TableBasedRoutabilityCheckerFactory implements RoutabilityCheckerFactory {
	public TableBasedRoutabilityCheckerFactory(ClusterDevice<?> clusterDevice) {
		RoutingTable.constructRoutingTables(clusterDevice);
	}

	@Override
	public RoutabilityChecker create(Cluster<?, ?> cluster, Device device) {
		return new TableBasedRoutabilityChecker(cluster, device);
	}
}
