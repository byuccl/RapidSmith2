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
public class Carry4RequiredDI0LutSourcePackRuleFactory implements PackRuleFactory {
	private Map<Cell, PackCell> combinedLutCarry4Map;
	private LibraryCell carry4;

	public Carry4RequiredDI0LutSourcePackRuleFactory(CellLibrary cellLibrary) {
		carry4 = cellLibrary.get("CARRY4");
	}

	@Override
	public void init(CellDesign design) {
		combinedLutCarry4Map = new HashMap<>();

		for (Cell cell : design.getCells()) {
			PackCell packCell = (PackCell) cell;
			if (packCell.getLibCell().equals(carry4)) {
				if (requiresExternalCYInitPin(packCell)) {
					CellPin DI0Pin = packCell.getPin("DI0");
					assert DI0Pin.isConnectedToNet();
					CellNet net = DI0Pin.getNet();
					if (!net.isStaticNet()) {
						Cell sourceCell = net.getSourcePin().getCell();
						combinedLutCarry4Map.put(sourceCell, packCell);
					}
				}
			}
		}
	}

	private static boolean requiresExternalCYInitPin(PackCell carry4Cell) {
		CellPin cyinitPin = carry4Cell.getPin("CYINIT");
		if (cyinitPin.isConnectedToNet()) {
			CellNet cyinitNet = cyinitPin.getNet();
			if (cyinitNet.isStaticNet())
				return false;

			CellPin di0Pin = carry4Cell.getPin("DI0");
			if (!di0Pin.isConnectedToNet())
				return false;

			CellNet DI0Net = di0Pin.getNet();
			return cyinitNet != DI0Net;
		}

		return false;
	}

	@Override
	public PackRule createRule(Cluster<?, ?> cluster) {
		return new Carry4RequiredDI0LutSourcePackRule(cluster);
	}

	private class Carry4RequiredDI0LutSourcePackRule implements PackRule {
		private Cluster<?, ?> cluster;

		// Using this as a set, null value means its been removed
		private StackedHashMap<PackCell, PackCell> cellsToCheck = new StackedHashMap<>();

		public Carry4RequiredDI0LutSourcePackRule(Cluster<?, ?> cluster) {
			this.cluster = cluster;
		}

		@Override
		public PackStatus validate(Collection<PackCell> changedCells) {
			cellsToCheck.checkPoint();

			for (PackCell cell : changedCells) {
				if (combinedLutCarry4Map.containsKey(cell)) {
					cellsToCheck.put(cell, combinedLutCarry4Map.get(cell));
				}
			}

			PackStatus status = PackStatus.VALID;
			for (PackCell carryCell : cellsToCheck.values()) {
				if (carryCell.getCluster() == null) {
					status = PackStatus.CONDITIONAL;
					List<Bel> possibles = getAvailableBels(carryCell);
					if (possibles.isEmpty()) {
						status = PackStatus.INFEASIBLE;
						break;
					}
				} else {
					assert carryCell.getCluster() == cluster;
				}
			}

			return status;
		}

		private List<Bel> getAvailableBels(PackCell carryCell) {
			return carryCell.getPossibleAnchors(cluster.getTemplate()).stream()
					.filter(b -> !cluster.isBelOccupied(b))
					.collect(Collectors.toList());
		}

		@Override
		public void revert() {
			cellsToCheck.rollBack();
		}

		@Override
		public Map<PackCell, Set<Bel>> getConditionals() {
			HashMap<PackCell, Set<Bel>> conditionals = new HashMap<>();
			for (PackCell carryCell : cellsToCheck.values()) {
				if (carryCell.getCluster() != null) {
					Set<Bel> bels = new HashSet<>(getAvailableBels(carryCell));
					assert !bels.isEmpty();
					conditionals.put(carryCell, bels);
				}
			}
			return conditionals;
		}
	}
}
