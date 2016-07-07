package edu.byu.ece.rapidSmith.cad.packer.AAPack.rules;

import edu.byu.ece.rapidSmith.cad.clusters.CarryChain;
import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.PackStatus;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.PrimitiveType;

import java.util.*;

/**
 *
 */
public class CarryChainLookAheadRuleFactory implements PackRuleFactory {
	private Map<CarryChain, Boolean> requiresSLICEM = new HashMap<>();
	private static final List<String> SPINS = Arrays.asList("S0", "S1", "S2", "S3");
	private static final List<String> RAMS = Arrays.asList(
			"SRL32", "SRL16", "SPRAM32", "SPRAM64", "DPRAM32", "DPRAM64");

	@Override
	public void init(CellDesign design) {
		Set<CarryChain> ccs = getCarryChains(design);
		for (CarryChain cc : ccs) {
			boolean requiresSLICEM = requiresSLICEM(cc);
			this.requiresSLICEM.put(cc, requiresSLICEM);
		}
	}

	private Set<CarryChain> getCarryChains(CellDesign design) {
		Set<CarryChain> ccs = new HashSet<>();
		for (Cell cell : design.getCells()) {
			PackCell packCell = (PackCell) cell;
			if (packCell.getCarryChain() != null)
				ccs.add(packCell.getCarryChain());
		}
		return ccs;
	}

	private boolean requiresSLICEM(CarryChain cc) {
		for (PackCell cell : cc.getCells()) {
			if (cell.getLibCell().getName().equals("CARRY4")) {
				if (doesSourceRequireSLICEM(cell))
					return true;
			}
		}
		return false;
	}

	private boolean doesSourceRequireSLICEM(PackCell cell) {
		for (String sPin : SPINS) {
			CellPin sinkPin = cell.getPin(sPin);
			CellNet net = sinkPin.getNet();
			if (net != null) {
				if (!net.isStaticNet()) {
					CellPin sourcePin = net.getSourcePin();
					Cell sourceCell = sourcePin.getCell();
					if (RAMS.contains(sourceCell.getLibCell().getName())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public PackRule createRule(Cluster<?, ?> cluster) {
		return new CarryChainLookAheadRule();
	}

	private class CarryChainLookAheadRule implements PackRule {

		@Override
		public PackStatus validate(Collection<PackCell> changedCells) {
			for (PackCell cell : changedCells) {
				CarryChain cc = cell.getCarryChain();
				if (cc != null) {
					if (requiresSLICEM.get(cc)) {
						if (cell.getLibCell().getName().equals("CARRY4")) {
							Bel bel = cell.getLocationInCluster();
							if (bel.getSite().getType() != PrimitiveType.SLICEM)
								return PackStatus.INFEASIBLE;
						}
					}
				}
			}
			return PackStatus.VALID;
		}

		@Override
		public void revert() {
			// No state, unused
		}

		@Override
		public Map<PackCell, Set<Bel>> getConditionals() {
			// Conditional is never a valid value
			return null;
		}
	}
}
