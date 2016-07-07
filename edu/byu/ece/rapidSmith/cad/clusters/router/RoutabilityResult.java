package edu.byu.ece.rapidSmith.cad.clusters.router;

import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.Routability;
import edu.byu.ece.rapidSmith.device.Bel;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class RoutabilityResult {
	public Routability routability;
	public Map<PackCell, Set<Bel>> conditionals;

	public RoutabilityResult(Routability routability) {
		this.routability = routability;
	}

	public RoutabilityResult(
			Routability routability, Map<PackCell, Set<Bel>> conditionals
	) {
		this.routability = routability;
		this.conditionals = conditionals;
	}
}
