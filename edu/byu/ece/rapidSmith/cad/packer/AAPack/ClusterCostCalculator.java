package edu.byu.ece.rapidSmith.cad.packer.AAPack;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterFactory;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;

/**
 *
 */
public interface ClusterCostCalculator<CTYPE extends ClusterType, CSITE extends ClusterSite> {
	void init(ClusterFactory<CTYPE, CSITE> generator);

	double calculateCost(Cluster<CTYPE, CSITE> cluster);
}
