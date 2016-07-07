package edu.byu.ece.rapidSmith.cad.packer.AAPack.rules;

import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.PackStatus;
import edu.byu.ece.rapidSmith.device.Bel;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public interface PackRule {
	PackStatus validate(Collection<PackCell> changedCells);
	void revert();
	Map<PackCell, Set<Bel>> getConditionals();
	default void cleanup() {
		// Do nothing
	}
}
