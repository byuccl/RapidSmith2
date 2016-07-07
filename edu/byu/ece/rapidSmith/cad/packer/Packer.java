package edu.byu.ece.rapidSmith.cad.packer;

import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;
import edu.byu.ece.rapidSmith.design.ClusterDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;

/**
 *
 */
public interface Packer<CTYPE extends ClusterType, CSITE extends ClusterSite> {
	ClusterDesign<CTYPE, CSITE> pack(CellDesign design);
}
