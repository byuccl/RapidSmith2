package edu.byu.ece.rapidSmith.cad.clusters;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class ClusterChain<T extends ClusterType, S extends ClusterSite> {
	private Set<Cluster<T, S>> clusters = new HashSet<>();

	public static <T extends ClusterType, S extends ClusterSite> ClusterChain<T, S> merge(
			Cluster<T, S> source, Cluster<T, S> sink
	) {
		ClusterChain<T, S> cc, o;

		if (source.getChain() != null) {
			cc = source.getChain();
			o = sink.getChain();
			cc.addCluster(sink);
		} else if (sink.getChain() != null) {
			cc = sink.getChain();
			o = null;
			cc.addCluster(source);
		} else {
			cc = new ClusterChain<>();
			o = null;
			cc.addCluster(source);
			cc.addCluster(sink);
		}

		if (cc != o && o != null) {
			o.clusters.forEach(cc::addCluster);
		}

		return cc;
	}

	private ClusterChain() {}

	private void addCluster(Cluster<T, S> cluster) {
		clusters.add(cluster);
		cluster.setChain(this);
	}

	public Set<Cluster<T, S>> getClusters() {
		return clusters;
	}
}
