package edu.byu.ece.rapidSmith.cad.packer.AAPack.rules;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.PackStatus;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.PrimitiveSite;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class LutMemberConsistencyRuleFactory implements PackRuleFactory {
	@Override
	public PackRule createRule(Cluster<?, ?> cluster) {
		return new LutMemberConsistencyRule(cluster);
	}

	private static class LutMemberConsistencyRule implements PackRule {
		private Cluster<?, ?> cluster;

		public LutMemberConsistencyRule(Cluster<?, ?> cluster) {
			this.cluster = cluster;
		}

		@Override
		public PackStatus validate(Collection<PackCell> changedCells) {
			for (PackCell changed : changedCells) {
				Bel location = changed.getLocationInCluster();
				if (location.getName().matches("[A-D][5-6]LUT")) {
					if (!isCompatible(location))
						return PackStatus.INFEASIBLE;
				}
			}
			return PackStatus.VALID;
		}

		private boolean isCompatible(Bel bel) {
			PrimitiveSite site = bel.getSite();
			char leName = bel.getName().charAt(0);
			Bel lut6 = site.getBel(leName + "6LUT");
			Bel lut5 = site.getBel(leName + "5LUT");

			if (cluster.isBelOccupied(lut6) && cluster.isBelOccupied(lut5)) {
				PackCell cellAtLut6 = cluster.getCellAtBel(lut6);
				PackCell cellAtLut5 = cluster.getCellAtBel(lut5);

				if (isStaticSource(cellAtLut5) || isStaticSource(cellAtLut6))
					return true;
				return cellAtLut5.getLibCell().equals(cellAtLut6.getLibCell());
			}

			return true;
		}

		private boolean isStaticSource(PackCell cell) {
			return cell.isVccSource() || cell.isGndSource();
		}

		@Override
		public void revert() {
			// no state, do nothing
		}

		@Override
		public Map<PackCell, Set<Bel>> getConditionals() {
			// Result is never conditional
			return null;
		}
	}
}
