package edu.byu.ece.rapidSmith.cad.packer.AAPack.rules;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.PackStatus;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.util.StackedHashMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class MergeFFsWithSourceRuleFactory implements PackRuleFactory {
	private Set<LibraryCell> libCellsToMerge;
	private Set<LibraryCell> ffLibCells;
	private Map<Cell, PackCell> mergedCells;

	public MergeFFsWithSourceRuleFactory(CellLibrary cellLibrary) {
		libCellsToMerge = new HashSet<>();
		libCellsToMerge.add(cellLibrary.get("CARRY4"));
		libCellsToMerge.add(cellLibrary.get("LUT5"));
		libCellsToMerge.add(cellLibrary.get("LUT6"));
		libCellsToMerge.add(cellLibrary.get("SRL16"));
		libCellsToMerge.add(cellLibrary.get("SRL32"));
		libCellsToMerge.add(cellLibrary.get("SPRAM32"));
		libCellsToMerge.add(cellLibrary.get("SPRAM64"));
		libCellsToMerge.add(cellLibrary.get("DPRAM32"));
		libCellsToMerge.add(cellLibrary.get("DPRAM64"));
		libCellsToMerge.add(cellLibrary.get("F7MUX"));
		libCellsToMerge.add(cellLibrary.get("F8MUX"));

		ffLibCells = new HashSet<>();
		ffLibCells.add(cellLibrary.get("FF_INIT"));
		ffLibCells.add(cellLibrary.get("REG_INIT"));
	}

	@Override
	public void init(CellDesign design) {
		mergedCells = new HashMap<>();

		for (Cell cell : design.getCells()) {
			LibraryCell libcell = cell.getLibCell();
			if (libCellsToMerge.contains(libcell)) {
				PackCell packCell = (PackCell) cell;
				for (CellPin sourcePin : cell.getPins()) {
					if (sourcePin.isConnectedToNet()) {
						CellNet net = sourcePin.getNet();
						Collection<CellPin> sinkPins = net.getSinkPins();
						if (sinkPins.size() == 1) {
							CellPin sinkPin = sinkPins.iterator().next();
							if (isFlipflopDInput(sinkPin))
								mergedCells.put(sinkPin.getCell(), packCell);
						}
					}
				}
			}
		}
	}

	private boolean isFlipflopDInput(CellPin sinkPin) {
		return ffLibCells.contains(sinkPin.getCell().getLibCell()) &&
				sinkPin.getName().equals("D");
	}

	@Override
	public PackRule createRule(Cluster<?, ?> cluster) {
		return new MergeFFsWithSourceRule(cluster);
	}

	private class MergeFFsWithSourceRule implements PackRule {
		private Cluster<?, ?> cluster;
		private StackedHashMap<Cell, PackCell> cellsToCheck = new StackedHashMap<>();

		public MergeFFsWithSourceRule(Cluster<?, ?> cluster) {
			this.cluster = cluster;
		}

		@Override
		public PackStatus validate(Collection<PackCell> changedCells) {
			cellsToCheck.checkPoint();
			List<PackCell> newMergedCells = changedCells.stream()
					.filter(c ->mergedCells.containsKey(c))
					.collect(Collectors.toList());
			for (PackCell newMergedCell : newMergedCells) {
				cellsToCheck.put(newMergedCell, mergedCells.get(newMergedCell));
			}

			PackStatus status = PackStatus.VALID;
			Iterator<Map.Entry<Cell, PackCell>> it = cellsToCheck.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Cell, PackCell> e = it.next();
				PackCell sourceCell = e.getValue();
				if (sourceCell.getCluster() != null) {
					it.remove();
				} else {
					status = PackStatus.CONDITIONAL;
					List<Bel> possibles = getAvailableBels(sourceCell);
					if (possibles.isEmpty()) {
						status = PackStatus.INFEASIBLE;
						break;
					}
				}
			}
			return status;
		}

		@Override
		public void revert() {
			cellsToCheck.rollBack();
		}

		@Override
		public Map<PackCell, Set<Bel>> getConditionals() {
			HashMap<PackCell, Set<Bel>> conditionals = new HashMap<>();
			for (PackCell sourceCell : cellsToCheck.values()) {
				Set<Bel> bels = new HashSet<>(getAvailableBels(sourceCell));
				assert !bels.isEmpty();
				conditionals.put(sourceCell, bels);
			}
			return conditionals;
		}

		private List<Bel> getAvailableBels(PackCell cell) {
			return cell.getPossibleAnchors(cluster.getTemplate()).stream()
					.filter(b -> !cluster.isBelOccupied(b))
					.collect(Collectors.toList());
		}
	}
}
