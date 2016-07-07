package edu.byu.ece.rapidSmith.cad.clusters.router;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.device.Device;

/**
 *
 */
public class PathFinderRoutabilityCheckerFactory implements RoutabilityCheckerFactory {
	@Override
	public RoutabilityChecker create(Cluster<?, ?> cluster, Device device) {
		return new PathFinderRoutabilityChecker(cluster, device);
	}
}
