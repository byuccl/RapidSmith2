package edu.byu.ece.rapidSmith.cad.clusters.tileCluster;

import edu.byu.ece.rapidSmith.cad.clusters.*;
import edu.byu.ece.rapidSmith.design.ClusterDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.device.Bel;

import java.util.*;

/**
 *
 */
public class TileClusterPackingCompletionUtils
		extends PackingCompletionUtils<TileClusterType, TileClusterSite>
{
	private TileClusterType CLBLM;
	private TileClusterType CLBLL;
	private TileClusterFactory factory;

	public TileClusterPackingCompletionUtils(
			CellLibrary cellLibrary, TileClusterFactory factory) {
		super(cellLibrary);
		this.factory = factory;
		for (TileClusterType type : factory.getAvailableClusterTypes()) {
			if (type.toString().contains("CLBLM"))
				CLBLM = type;
			else if (type.toString().contains("CLBLL"))
				CLBLL = type;
		}
		assert CLBLM != null;
		assert CLBLL != null;
	}

	@Override
	protected void unifyCarryChains(
			ClusterDesign<TileClusterType, TileClusterSite> design
	) {
		buildCarryChains(design);
		Set<ClusterChain<TileClusterType, TileClusterSite>> ccs = getClusterChains(design);
		List<TileCluster> toPromote = getClustersToPromote(ccs);
		promoteClusters(design, toPromote);
	}

	private void copyCluster(TileCluster oldCluster, TileCluster newCluster) {
		for (PackCell cell : oldCluster.getCells()) {
			Bel oldBel = cell.getLocationInCluster();
			int oldIndex = oldBel.getSite().getIndex();
			Bel newBel = null;

			for (Bel bel : newCluster.getTemplate().getBels()) {
				if (bel.getSite().getIndex() == oldIndex && bel.getName().equals(oldBel.getName())) {
					newBel = bel;
					break;
				}
			}
			assert newBel != null;
			newCluster.addCell(newBel, cell);
			cell.setCluster(newCluster);
			cell.setLocationInCluster(newBel);
		}
	}

	private void buildCarryChains(ClusterDesign<TileClusterType, TileClusterSite> design) {
		for (Cluster<TileClusterType, TileClusterSite> cluster : design.getClusters()) {
			for (PackCell cell : cluster.getCells()) {
				for (CarryChainConnection ccc : cell.getSourceCarryChainConnections()) {
					ClusterChain.merge(cluster, ccc.getEndCell().getCluster());
				}
			}
		}
	}

	private Set<ClusterChain<TileClusterType, TileClusterSite>> getClusterChains(
			ClusterDesign<TileClusterType, TileClusterSite> design
	) {
		Set<ClusterChain<TileClusterType, TileClusterSite>> ccs = new HashSet<>();
		for (Cluster<TileClusterType, TileClusterSite> cluster : design.getClusters()) {
			ClusterChain<TileClusterType, TileClusterSite> chain = cluster.getChain();
			if (chain != null)
				ccs.add(chain);
		}
		return ccs;
	}

	private ArrayList<TileCluster> getClustersToPromote(
			Set<ClusterChain<TileClusterType, TileClusterSite>> ccs
	) {
		ArrayList<TileCluster> toPromote = new ArrayList<>();
		for (ClusterChain<TileClusterType, TileClusterSite> cc : ccs) {
			boolean promote = false;
			for (Cluster<TileClusterType, TileClusterSite> cluster: cc.getClusters()) {
				if (cluster.getType() == CLBLM) {
					promote = true;
					break;
				}
			}

			if (promote) {
				for (Cluster<TileClusterType, TileClusterSite> cluster : cc.getClusters()) {
					if (cluster.getType() == CLBLL) {
						toPromote.add((TileCluster) cluster);
					}
				}
			}
		}
		return toPromote;
	}

	private void promoteClusters(ClusterDesign<TileClusterType, TileClusterSite> design, List<TileCluster> toPromote) {
		for (TileCluster oldCluster : toPromote) {
			TileCluster newCluster = factory.createNewCluster(oldCluster.getName(), CLBLM);
			design.removeCluster(oldCluster);
			copyCluster(oldCluster, newCluster);
			design.addCluster(newCluster);
		}
	}
}
