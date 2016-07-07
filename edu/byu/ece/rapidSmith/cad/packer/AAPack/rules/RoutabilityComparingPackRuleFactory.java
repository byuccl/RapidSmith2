package edu.byu.ece.rapidSmith.cad.packer.AAPack.rules;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.clusters.router.RoutabilityChecker;
import edu.byu.ece.rapidSmith.cad.clusters.router.RoutabilityCheckerFactory;
import edu.byu.ece.rapidSmith.cad.clusters.router.RoutabilityResult;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.PackStatus;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.Routability;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.Device;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class RoutabilityComparingPackRuleFactory implements PackRuleFactory {
	RoutabilityCheckerFactory factory1;
	RoutabilityCheckerFactory factory2;
	Device device;

	public RoutabilityComparingPackRuleFactory(
			RoutabilityCheckerFactory factory1,
			RoutabilityCheckerFactory factory2,
			Device device
	) {
		this.factory1 = factory1;
		this.factory2 = factory2;
		this.device = device;
	}

	@Override
	public PackRule createRule(Cluster<?, ?> cluster) {
		return new RoutabilityComparingPackRule(cluster);
	}

	private class RoutabilityComparingPackRule implements PackRule {
		private RoutabilityChecker checker1;
		private RoutabilityChecker checker2;

		public RoutabilityComparingPackRule(Cluster<?, ?> cluster) {
			this.checker1 = factory1.create(cluster, device);
			this.checker2 = factory2.create(cluster, device);
		}

		@Override
		public PackStatus validate(Collection<PackCell> changedCells) {
			checker1.checkpoint();
			checker2.checkpoint();

			RoutabilityResult result1 = checker1.check(changedCells);
			RoutabilityResult result2 = checker2.check(changedCells);

			Routability r1 = result1.routability;
			Routability r2 = result2.routability;
//			if (r1 != r2)
//				AAPack.logger.log(Level.ALL, "Mismatched checks",
//						Arrays.asList(r1, r2).toArray());

			return PackStatus.VALID;
		}

		@Override
		public void revert() {
			checker1.rollback();
			checker2.rollback();
		}

		@Override
		public Map<PackCell, Set<Bel>> getConditionals() {
			return null;
		}
	}
}
