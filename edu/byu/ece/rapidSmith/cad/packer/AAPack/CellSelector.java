package edu.byu.ece.rapidSmith.cad.packer.AAPack;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;

import java.util.Collection;

/**
 *
 */
public interface CellSelector {
	void init(Collection<PackCell> cells);
	void initCluster(Cluster<?, ?> cluster);
	PackCell nextCell();
	void commitCells(Collection<PackCell> cell, Collection<PackCell> conditionals);
	void cleanupCluster();

	void checkpoint();
	void rollBackLastCommit();
}
