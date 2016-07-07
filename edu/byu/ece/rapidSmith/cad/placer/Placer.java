package edu.byu.ece.rapidSmith.cad.placer;

import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;
import edu.byu.ece.rapidSmith.design.ClusterDesign;
import edu.byu.ece.rapidSmith.device.Device;

/**
 *
 */
public interface Placer<CTYPE extends ClusterType, CSITE extends ClusterSite>
{
	boolean place(ClusterDesign<CTYPE, CSITE> design, Device device);
}
