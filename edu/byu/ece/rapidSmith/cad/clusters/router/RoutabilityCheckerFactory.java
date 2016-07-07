package edu.byu.ece.rapidSmith.cad.clusters.router;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.device.Device;

/**
 *
 */
public interface RoutabilityCheckerFactory {
	RoutabilityChecker create(Cluster<?, ?> cluster, Device device);
}
