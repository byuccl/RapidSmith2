package edu.byu.ece.rapidSmith.cad.packer.AAPack;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.device.Bel;

import java.util.Collection;

/**
 *
 *
 * Expected order of calls:
 *  1: initCluster
 *  2: initCell
 *  3: nextBel
 *  4: choose 3, 5, 7, 9, 11
 *  5: commitBels
 *  6: goto 2, 11
 *  7: revertToLastCommit
 *  8: goto 2, 11
 *  9: rollBackLastCommit
 * 10: goto 2, 11
 * 11: cleanupCluster
 * 12: goto 1
 */
public interface BelSelector {
	// Called when a cluster is created to allow the cluster.  The cluster should be empty.
	// Should not change the state of the cluster or netlist.
	void initCluster(Cluster<?, ?> cluster);

	// Called before a new molecule is packed into the cluster.  The molecule should be
	// valid.  Should follow either initCluster or commitBels.
	void initCell(PackCell cell, Collection<Bel> forcedAnchors);

	// Called to obtain the next bel in the molecule.  Should not be called
	// until after initCell.  Bel should be unused in the cluster.  If reqBels is not
	// null, then choose from on of the BELs in the collection.  Otherwise, choose any
	// unused BEL in the cluster.  BELs in reqBels may already be occupied in the cluster.
	Bel nextBel();

	/**
	 * Stores the previous state prior to committing the BEL, then updates the costs
	 * of each BEL and cleans the priority queue.
 	 */

	void commitBels(Collection<Bel> bel);
	void revertToLastCommit();
	void rollBackLastCommit();
	void cleanupCluster();
}
