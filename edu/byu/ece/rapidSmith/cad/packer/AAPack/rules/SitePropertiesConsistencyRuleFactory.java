package edu.byu.ece.rapidSmith.cad.packer.AAPack.rules;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.PackStatus;
import edu.byu.ece.rapidSmith.design.subsite.SiteProperty;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.PrimitiveSite;
import edu.byu.ece.rapidSmith.util.StackedHashMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
public class SitePropertiesConsistencyRuleFactory implements PackRuleFactory {
	@Override
	public PackRule createRule(Cluster<?, ?> cluster) {
		return new SitePropertiesConsistencyRule(cluster);
	}

	private static class SitePropertiesConsistencyRule implements PackRule {
		private Map<PrimitiveSite, StackedHashMap<SiteProperty, Object>> siteProperties;

		public SitePropertiesConsistencyRule(Cluster<?, ?> cluster) {
			siteProperties = new HashMap<>();
			Set<PrimitiveSite> clusterSites = cluster.getTemplate().getBels().stream()
					.map(Bel::getSite)
					.collect(Collectors.toSet());
			clusterSites.forEach(k -> siteProperties.put(k, new StackedHashMap<>()));
		}

		@Override
		public PackStatus validate(Collection<PackCell> changedCells) {
			checkpoint();

			for (PackCell cell : changedCells) {
				Bel location = cell.getLocationInCluster();
				StackedHashMap<SiteProperty, Object> existing =
						siteProperties.get(location.getSite());
				Map<SiteProperty, Object> props =
						cell.getSharedSiteProperties(location.getId());

				for (Map.Entry<SiteProperty, Object> e : props.entrySet()) {
					if (existing.containsKey(e.getKey())) {
						if (!existing.get(e.getKey()).equals(e.getValue()))
							return PackStatus.INFEASIBLE;
					} else {
						existing.put(e.getKey(), e.getValue());
					}
				}
			}
			return PackStatus.VALID;
		}

		private void checkpoint() {
			for (StackedHashMap<?, ?> siteMap : siteProperties.values())
				siteMap.checkPoint();
		}

		@Override
		public void revert() {
			for (StackedHashMap<?, ?> siteMap : siteProperties.values())
				siteMap.rollBack();
		}

		@Override
		public Map<PackCell, Set<Bel>> getConditionals() {
			return null;
		}
	}
}
