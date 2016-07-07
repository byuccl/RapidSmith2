package edu.byu.ece.rapidSmith.cad.clusters.router;

import edu.byu.ece.rapidSmith.cad.clusters.PackCell;

import java.util.Collection;

/**
 *
 */
public interface RoutabilityChecker {
	RoutabilityResult check(Collection<PackCell> changed);
	void checkpoint();
	void rollback();
}
