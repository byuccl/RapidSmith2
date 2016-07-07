package edu.byu.ece.rapidSmith.cad.packer.AAPack;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.device.Device;

import java.util.Collection;

/**
 * Finds a seed molecule for the clustering.  If no suitable molecule is
 * found, returns null.
 */
public interface SeedSelector {
	void init(Device device, Collection<PackCell> molecules);
	PackCell nextSeed();
	void commitCluster(Cluster<?, ?> cluster);
}
