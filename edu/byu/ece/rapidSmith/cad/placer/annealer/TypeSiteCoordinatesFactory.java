package edu.byu.ece.rapidSmith.cad.placer.annealer;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;

/**
 *
 */
public interface TypeSiteCoordinatesFactory<CTYPE extends ClusterType, CSITE extends ClusterSite> {
	TypeSiteCoordinates<CTYPE, CSITE> make(CTYPE t);
}
