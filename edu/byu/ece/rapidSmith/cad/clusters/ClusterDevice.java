package edu.byu.ece.rapidSmith.cad.clusters;

import edu.byu.ece.rapidSmith.device.Device;

import java.util.Collection;

/**
 *
 */
public abstract class ClusterDevice<
		T extends ClusterType>
		extends Device
{
	public abstract Collection<T> getAvailableClusterTypes();

	public abstract ClusterTemplate<T> getCluster(T type);

	public abstract Collection<ClusterTemplate<T>> getTemplates();
}
