package edu.byu.ece.rapidSmith.cad.packer.AAPack.rules;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.PackStatus;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.PrimitiveSite;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class Mixing5And6LutsRuleFactory implements PackRuleFactory {

	@Override
	public PackRule createRule(Cluster<?, ?> cluster) {
		return new Mixing5And5LutsRule(cluster);
	}

	public static class Mixing5And5LutsRule implements PackRule {
		private Cluster<?, ?> cluster;

		public Mixing5And5LutsRule(Cluster<?, ?> cluster) {
			this.cluster = cluster;
		}

		@Override
		public PackStatus validate(Collection<PackCell> changed) {
			PackStatus status = PackStatus.VALID;

			if (!cluster.getType().toString().contains("CLB"))
				return status;

			for (Cell cell : cluster.getCells()) {
				Bel placement = ((PackCell) cell).getLocationInCluster();
				if (placement.getName().contains("LUT")) {
					if (!isCompatible(placement))
						return PackStatus.INFEASIBLE;
				}
			}

			return status;
		}

		private boolean isCompatible(Bel bel) {
			PrimitiveSite site = bel.getSite();
			char leName = bel.getName().charAt(0);
			Bel lut6 = site.getBel(leName + "6LUT");
			Bel lut5 = site.getBel(leName + "5LUT");

			if (cluster.isBelOccupied(lut6) && cluster.isBelOccupied(lut5)) {
				Cell cellAtLut6 = cluster.getCellAtBel(lut6);
				if (cellAtLut6.getLibCell().getName().contains("6"))
					return false;
			}

			return true;
		}

		@Override
		public Map<PackCell, Set<Bel>> getConditionals() {
			return Collections.emptyMap();
		}

		@Override
		public void revert() { }
	}
}
