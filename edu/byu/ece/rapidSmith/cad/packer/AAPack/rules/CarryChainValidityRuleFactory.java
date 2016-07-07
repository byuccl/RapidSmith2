package edu.byu.ece.rapidSmith.cad.packer.AAPack.rules;

import edu.byu.ece.rapidSmith.cad.clusters.*;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.PackStatus;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.util.StackedHashMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class CarryChainValidityRuleFactory implements PackRuleFactory {
	Map<PackCell, Set<PackCell>> mergedCells;

	@Override
	public void init(CellDesign design) {
		mergedCells = new HashMap<>();
	}

	@Override
	public void commitCluster(Cluster<?, ?> cluster) {
		List<PackCell> ccCells = new ArrayList<>();
		for (PackCell cell : cluster.getCells()) {
			if (cell.getCarryChain() != null)
				ccCells .add(cell);
		}

		if (ccCells.isEmpty())
			return;

		List<CarryChain> ccs = new ArrayList<>();
		for (PackCell ccCell : ccCells) {
			ccs.add(ccCell.getCarryChain());
		}
		ccs.forEach(CarryChain::incrementNumPackedCells);

		CarryChainGroup group = null;
		Integer index = null;
		for (PackCell ccCell : ccCells) {
			if (ccCell.getCarryGroup() != null) {
				group = ccCell.getCarryGroup();
				index = ccCell.getCarryIndex();
				break;
			}
		}
		if (group == null) {
			group = new CarryChainGroup();
			index = group.getUniqueIndex();
		}

		cluster.setCarryGroup(group);
		cluster.setCarryIndex(index);
		for (PackCell ccCell : ccCells) {
			if (ccCell.getCarryGroup() == null) {
				group.addCell(ccCell, index);
			}
		}
		mergeSharedCarryChains(cluster, group, index);
	}

	@Override
	public PackRule createRule(Cluster<?, ?> cluster) {
		return new CarryChainValidityRule(cluster);
	}

	private class CarryChainValidityRule implements PackRule {
		private Cluster<?, ?> cluster;
		private Deque<Map<PackCell, Set<Bel>>> conditionalsStack = new ArrayDeque<>();
		private Deque<List<CarryChain>> incrementedCarryChains = new ArrayDeque<>();
		StackedHashMap<Integer, ClusterWrapper> claimedClusters = new StackedHashMap<>();

		public CarryChainValidityRule(Cluster<?, ?> cluster) {
			this.cluster = cluster;
		}

		@Override
		public PackStatus validate(Collection<PackCell> changedCells) {
			List<PackCell> ccCells = getCarryChainCells(changedCells);

			Map<PackCell, Set<Bel>> conditionals = new HashMap<>();
			conditionalsStack.push(conditionals);
			claimedClusters.checkPoint();

			boolean infeasible = !checkForNonconsecutiveCarryChains(ccCells);

			if (!infeasible) {
				infeasible = !checkMergable(ccCells);
			}

			if (!infeasible) {
				infeasible = !checkForMergedCarryChains(changedCells, ccCells, conditionals);
			} else {
				incrementedCarryChains.push(Collections.emptyList());
			}

			if (!infeasible) {
				infeasible = !checkIndex(ccCells, cluster);
			}

			if (infeasible)
				return PackStatus.INFEASIBLE;
			return conditionals.isEmpty() ? PackStatus.VALID : PackStatus.CONDITIONAL;
		}

		private boolean checkIndex(List<PackCell> ccCells, Cluster<?, ?> cluster) {
			for (PackCell clusterCell : cluster.getCells()) {
				if (clusterCell.getCarryGroup() != null) {
					for (PackCell ccCell : ccCells) {
						if (clusterCell != ccCell) {
							if (!areCompatibleIndices(clusterCell, ccCell))
								return false;
						}
					}
				}
			}
			return true;
		}

		private boolean areCompatibleIndices(PackCell ccCell, PackCell compareTo) {
			if (ccCell.getCarryGroup() == null || compareTo.getCarryGroup() == null)
				return true;
			if (ccCell.getCarryGroup() == compareTo.getCarryGroup()) {
				int ccIndex = ccCell.getCarryIndex();
				int compareToIndex = compareTo.getCarryIndex();
				if (ccIndex != compareToIndex)
					return false;
			}
			return true;
		}

		private boolean checkMergable(List<PackCell> ccCells) {
			boolean infeasible = true;
			Map<Integer, Set<PackCell>> toMerge = new HashMap<>();
			buildSharedCarryChains(ccCells, cluster.getTemplate(), toMerge);
			for (Map.Entry<Integer, Set<PackCell>> e : toMerge.entrySet()) {
				for (PackCell cell : e.getValue()) {
					Cluster<?, ?> newCluster = cell.getCluster();
					if (claimedClusters.containsKey(e.getKey())) {
						Cluster<?, ?> existing = claimedClusters.get(e.getKey()).cluster;
						if (existing != newCluster) {
							infeasible = false;
							break;
						}
					} else {
						claimedClusters.put(e.getKey(), new ClusterWrapper(newCluster));
					}
				}
				if (!infeasible)
					break;
			}
			return infeasible;
		}

		private boolean checkForNonconsecutiveCarryChains(List<PackCell> ccCells) {
			for (PackCell ccCell : ccCells) {
				CarryChain cc = ccCell.getCarryChain();
				if (cc.isPartiallyPlaced()) {
					for (CarryChainConnection ccc : ccCell.getSinkCarryChainConnections()) {
						if (!cellCanBePlaced(ccc.getEndCell(), SearchDirection.SOURCE2SINK))
							return false;
					}

					for (CarryChainConnection ccc : ccCell.getSourceCarryChainConnections()) {
						if (!cellCanBePlaced(ccc.getEndCell(), SearchDirection.SINK2SOURCE))
							return false;
					}
				}
			}
			return true;
		}

		private boolean checkForMergedCarryChains(
				Collection<PackCell> changedCells, List<PackCell> ccCells,
				Map<PackCell, Set<Bel>> conditionals
		) {
			Set<PackCell> conditionalCells = new HashSet<>();
			conditionalCells.addAll(conditionalsStack.peek().keySet());
			for (PackCell changedCell : changedCells) {
				conditionalCells.addAll(mergedCells.getOrDefault(
						changedCell, Collections.emptySet()));
			}

			for (PackCell cell : conditionalCells) {
				if (cell.isValid()) {
					Set<Bel> bels = cell.getPossibleAnchors(cluster.getTemplate()).stream()
							.filter(b -> !cluster.isBelOccupied(b))
							.collect(Collectors.toSet());
					if (bels.isEmpty())
						return false;
					conditionals.put(cell, bels);
				}
			}

			List<CarryChain> ccs = ccCells.stream()
					.map(PackCell::getCarryChain)
					.collect(Collectors.toList());
			ccs.forEach(CarryChain::incrementNumPackedCells);
			incrementedCarryChains.push(ccs);
			return true;
		}

		private boolean cellCanBePlaced(PackCell endCell, SearchDirection direction) {
			// Can only pack partially placed carry chains
			Cluster<?, ?> cluster = endCell.getCluster();
			if (cluster != null)
				return true;

			Queue<CarryChainConnection> queue = new ArrayDeque<>();
			Set<CarryChainConnection> set = new HashSet<>();
			Collection<CarryChainConnection> cccs = getCCCs(endCell, direction);
			set.addAll(cccs);
			queue.addAll(cccs);
			while (!queue.isEmpty()) {
				PackCell end = queue.poll().getEndCell();
				if (end.getCluster() != null)
					return false;

				Set<CarryChainConnection> cccSet = new HashSet<>(getCCCs(end, direction));
				cccSet.removeAll(set);
				set.addAll(cccSet);
				queue.addAll(cccSet);
			}
			return true;
		}

		private Collection<CarryChainConnection> getCCCs(
				PackCell endCell, SearchDirection direction
		) {
			Collection<CarryChainConnection> cccs;
			switch (direction) {
				case SINK2SOURCE:
					cccs = endCell.getSourceCarryChainConnections();
					break;
				case SOURCE2SINK:
					cccs = endCell.getSinkCarryChainConnections();
					break;
				default:
					throw new AssertionError("Illegal enum value");
			}
			return cccs;
		}

		@Override
		public void revert() {
			conditionalsStack.pop();
			List<CarryChain> ccs = incrementedCarryChains.pop();
			ccs.forEach(CarryChain::decrementNumPackedCells);
			claimedClusters.rollBack();
		}

		@Override
		public Map<PackCell, Set<Bel>> getConditionals() {
			return conditionalsStack.peek();
		}

		@Override
		public void cleanup() {
			while (!incrementedCarryChains.isEmpty()) {
				incrementedCarryChains.pop().forEach(CarryChain::decrementNumPackedCells);
			}
		}
	}

	private static List<PackCell> getCarryChainCells(Collection<PackCell> changedCells) {
		return changedCells.stream()
				.filter(c -> c.getCarryChain() != null)
				.collect(Collectors.toList());
	}

	private void mergeCells(Collection<PackCell> cells) {
		for (PackCell cell : cells) {
			if (cell.getCluster() == null) {
				mergedCells.computeIfAbsent(cell, k -> new HashSet<>()).addAll(cells);
			}
		}
	}

	private void mergeSharedCarryChains(
			Cluster<?, ?> cluster, CarryChainGroup group, Integer index
	) {
		List<PackCell> ccCells = cluster.getCells().stream()
				.filter(c -> c.getCarryChain() != null)
				.collect(Collectors.toList());

		HashMap<Integer, Set<PackCell>> carryChainEnds = new HashMap<>();
		buildSharedCarryChains(ccCells, cluster.getTemplate(), carryChainEnds);

		Map<CarryChainGroup, Map<Integer, Integer>> groupIndexUpdateMap =
				getGroupIndexUpdateMap(cluster, carryChainEnds);
		for (Map.Entry<CarryChainGroup, Map<Integer, Integer>> e : groupIndexUpdateMap.entrySet())
			group.absorbGroup(e.getKey(), e.getValue());

		for (Set<PackCell> set : carryChainEnds.values()) {
			Integer newIndex = null;
			for (PackCell cell : set) {
				if (cell.getCarryGroup() != null) {
					assert cell.getCarryGroup() == group;
					assert newIndex == null || newIndex.equals(cell.getCarryIndex());
					newIndex = cell.getCarryIndex();
				}
			}

			if (newIndex == null)
				newIndex = group.getUniqueIndex();
			for (PackCell cell : set) {
				if (cell.getCarryGroup() == null) {
					group.addCell(cell, newIndex);
				}
			}
		}


		for (Map.Entry<Integer, Set<PackCell>> e : carryChainEnds.entrySet()) {
			Set<PackCell> m = e.getValue();
			if (m.size() > 1) {
				assert m.stream().map(PackCell::getCluster).distinct().count() == 1;
				mergeCells(m);
			}
		}
	}

	private Map<CarryChainGroup, Map<Integer, Integer>> getGroupIndexUpdateMap(
			Cluster<?, ?> cluster, HashMap<Integer, Set<PackCell>> carryChainEnds
	) {
		Map<CarryChainGroup, Map<Integer, Integer>> groupIndexUpdateMap = new HashMap<>();
		CarryChainGroup group = cluster.getCarryGroup();
		for (PackCell cell : cluster.getCells()) {
			if (cell.getCarryGroup() != null && cell.getCarryGroup() != group) {
				groupIndexUpdateMap.computeIfAbsent(cell.getCarryGroup(), k -> new HashMap<>())
						.put(cell.getCarryIndex(), cluster.getCarryIndex());
			}
		}

		for (Set<PackCell> set : carryChainEnds.values()) {
			Integer curWithGroup = null;
			Map<CarryChainGroup, Integer> outsideGroup = new HashMap<>();
			for (PackCell c : set) {
				if (c.getCarryGroup() == group) {
					assert curWithGroup == null || Objects.equals(c.getCarryIndex(), curWithGroup);
					curWithGroup = c.getCarryIndex();
				} else if (c.getCarryGroup() != null) {
					Integer curOutsideGroup = outsideGroup.get(c.getCarryGroup());
					assert curOutsideGroup == null || Objects.equals(curOutsideGroup, c.getCarryIndex());
					outsideGroup.put(c.getCarryGroup(), c.getCarryIndex());
				}
			}

			if (curWithGroup == null && !outsideGroup.isEmpty())
				curWithGroup = group.getUniqueIndex();
			for (Map.Entry<CarryChainGroup, Integer> e : outsideGroup.entrySet()) {
				groupIndexUpdateMap.computeIfAbsent(e.getKey(), k -> new HashMap<>())
						.put(e.getValue(), curWithGroup);
			}
		}
		return groupIndexUpdateMap;
	}

	private void buildSharedCarryChains(
			Collection<PackCell> ccCells, ClusterTemplate<?> template,
			Map<Integer, Set<PackCell>> toMerge
	) {
		for (PackCell cell : ccCells) {
			if (cell.getCarryChain() == null)
				continue;

			for (CarryChainConnection ccc : cell.getSinkCarryChainConnections()) {
				CellPin sourceCellPin = ccc.getClusterPin();
				BelPin sourceBelPin = getBelPinOfCellPin(sourceCellPin);
				for (DirectConnection dc : template.getDirectSinksOfCluster()) {
					if (isCompatibleConnection(dc, sourceBelPin, ccc.getEndPin())) {
						Integer sinkTileIndex = dc.endTileIndex;
						if (sinkTileIndex != null) {
							toMerge.computeIfAbsent(sinkTileIndex, k -> new HashSet<>())
									.add(ccc.getEndCell());
							break;
						}
					}
				}
			}

			for (CarryChainConnection ccc : cell.getSourceCarryChainConnections()) {
				CellPin ssinkCellPin = ccc.getClusterPin();
				BelPin sinkBelPin = getBelPinOfCellPin(ssinkCellPin);
				for (DirectConnection dc : template.getDirectSourcesOfCluster()) {
					if (isCompatibleConnection(dc, sinkBelPin, ccc.getEndPin())) {
						Integer sinkTileIndex = dc.endTileIndex;
						if (sinkTileIndex != null) {
							toMerge.computeIfAbsent(sinkTileIndex, k -> new HashSet<>())
									.add(ccc.getEndCell());
							break;
						}
					}
				}
			}
		}
	}

	private boolean isCompatibleConnection(
			DirectConnection dc, BelPin clusterPin, CellPin endPin
	) {
		if (!dc.clusterPin.equals(clusterPin))
			return false;

		PackCell endCell = (PackCell) endPin.getCell();
		if (endCell.getCluster() == null) {
			List<BelId> anchors = endCell.getPossibleAnchors();
			BelId dcEndPinId = dc.endPin.getId();
			if (!anchors.contains(dcEndPinId))
				return false;
			List<String> endBelPins = endPin.getPossibleBelPinNames(dcEndPinId);
			assert endBelPins.size() == 1;
			String endBelPin = endBelPins.get(0);
			return dc.endPin.getName().equals(endBelPin);
		} else {
			Bel endBel = endCell.getLocationInCluster();
			List<BelPin> endBelPins = endPin.getPossibleBelPins(endBel);
			assert endBelPins.size() == 1;
			BelPin endBelPin = endBelPins.get(0);
			return dc.endPin.equals(endBelPin.getTemplate()) &&
					dc.endSiteIndex == endBelPin.getBel().getSite().getIndex();
		}
	}

	private BelPin getBelPinOfCellPin(CellPin cellPin) {
		PackCell cell = (PackCell) cellPin.getCell();
		Bel bel = cell.getLocationInCluster();
		Objects.requireNonNull(bel);
		List<BelPin> pinList = cellPin.getPossibleBelPins(bel);
		assert pinList.size() == 1;
		return pinList.get(0);
	}

	private enum SearchDirection {
		SOURCE2SINK, SINK2SOURCE
	}

	private static class ClusterWrapper {
		Cluster<?, ?> cluster;

		public ClusterWrapper(Cluster<?, ?> cluster) {
			this.cluster = cluster;
		}
	}
}
