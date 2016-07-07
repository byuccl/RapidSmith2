package edu.byu.ece.rapidSmith.cad.packer.AAPack.rules;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;

/**
 *
 */
public interface PackRuleFactory {
	default void init(CellDesign design) {
		// do nothing
	}

	default void commitCluster(Cluster<?, ?> cluster) {
		// do nothing
	}

	PackRule createRule(Cluster<?, ?> cluster);
}
