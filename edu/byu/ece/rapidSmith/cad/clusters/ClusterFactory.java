package edu.byu.ece.rapidSmith.cad.clusters;

import java.util.Collection;
import java.util.List;

/**
 *
 */
public interface ClusterFactory<T extends ClusterType, S extends ClusterSite> {
	void init();
	ClusterTemplate<T> getTemplate(T clusterType);
	Collection<T> getAvailableClusterTypes();
	int getNumRemainingOfType(T clusterType);
	Cluster<T, S> createNewCluster(String clusterName, T type);
	void commitCluster(Cluster<T, S> cluster);
	List<S> getLocations(T type);
}
