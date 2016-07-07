package edu.byu.ece.rapidSmith.cad.packer.AAPack.rules;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.router.RoutabilityChecker;
import edu.byu.ece.rapidSmith.cad.clusters.router.RoutabilityCheckerFactory;
import edu.byu.ece.rapidSmith.cad.clusters.router.RoutabilityResult;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.PackStatus;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.Device;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class RoutabilityCheckerPackRuleFactory implements PackRuleFactory {
	RoutabilityCheckerFactory factory;
	Device device;

	public RoutabilityCheckerPackRuleFactory(
			RoutabilityCheckerFactory factory, Device device
	) {
		this.factory = factory;
		this.device = device;
	}

	@Override
	public PackRule createRule(Cluster<?, ?> cluster) {
		return new RoutabilityCheckerPackRule(cluster);
	}

	private class RoutabilityCheckerPackRule implements PackRule {
		private RoutabilityChecker checker;
		private RoutabilityResult result;

		public RoutabilityCheckerPackRule(Cluster<?, ?> cluster) {
			this.checker = factory.create(cluster, device);
		}

		@Override
		public PackStatus validate(Collection<PackCell> changedCells) {
			checker.checkpoint();
			result = checker.check(changedCells);
			switch (result.routability) {
				case FEASIBLE:
					return PackStatus.VALID;
				case CONDITIONAL:
					return PackStatus.CONDITIONAL;
				case INFEASIBLE:
					return PackStatus.INFEASIBLE;
			}
			throw new AssertionError("Invalid result");
		}

		@Override
		public void revert() {
			checker.rollback();
			result = null;
		}

		@Override
		public Map<PackCell, Set<Bel>> getConditionals() {
			return result.conditionals;
		}
	}
}
