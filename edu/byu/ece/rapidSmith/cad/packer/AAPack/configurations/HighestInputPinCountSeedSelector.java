package edu.byu.ece.rapidSmith.cad.packer.AAPack.configurations;

import edu.byu.ece.rapidSmith.cad.clusters.CarryChainConnection;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.SeedSelector;
import edu.byu.ece.rapidSmith.device.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class HighestInputPinCountSeedSelector implements SeedSelector {
	/* different lists for clustering */
	private int maxCellInputs;
	private Map<Integer, List<PackCell>> unclusteredCellsMap;
	private Set<PackCell> carryChains = new HashSet<>();

	@Override
	public void init(Device device, Collection<PackCell> cells) {
		unclusteredCellsMap = new HashMap<>();

		// Add all the cells to the appropriate location
		maxCellInputs = 0;
		for (PackCell cell : cells) {
			if (!cell.isPackable())
				continue;
			int numInputPins = getNumExternalPinsOfCell(cell);

			unclusteredCellsMap.computeIfAbsent(
					numInputPins, k -> new LinkedList<>()).add(cell);
			if (numInputPins > maxCellInputs)
				maxCellInputs = numInputPins;
		}

		// Now sort them based on the gain
		for (List<PackCell> unclusteredCellsList : unclusteredCellsMap.values()) {
			unclusteredCellsList.sort(Comparator.comparingDouble(this::getCellBaseGain));
		}
	}

	private int getNumExternalPinsOfCell(PackCell cell) {
		int numInputPins = 0;
		for (CellPin pin : cell.getInputPins()) {
			if (!pinIsSourcedInternally(pin, cell))
				numInputPins++;
		}
		return numInputPins;
	}

	private boolean pinIsSourcedInternally(CellPin pin, PackCell cell) {
		if (!pin.isConnectedToNet())
			return false;

		CellPin sourcePin = pin.getNet().getSourcePin();
		if (sourcePin == null)
			return false;

		Cell sourceCell = sourcePin.getCell();
		return sourceCell == cell;
	}

	private double getCellBaseGain(PackCell cell) {
		return cell.getNumExposedPins();
	}

	@Override
	public PackCell nextSeed() {
		// Use existing carry chains first.  This avoids accidentally placing parts of a
		// single carry chain in incompatible locations.
		while (!carryChains.isEmpty()) {
			Iterator<PackCell> it = carryChains.iterator();
			PackCell next = it.next();
			it.remove();
			if (next.isPackable())
				return next;
		}

		/* Returns the cell with the largest number of used inputs that satisfies the
		 * clocking and number of inputs constraints. */
		for (int externalInputs = maxCellInputs; externalInputs >= 0; externalInputs--) {
			List<PackCell> possibleSeeds = unclusteredCellsMap.get(externalInputs);
			if (possibleSeeds == null)
				continue;
			PackCell cell = possibleSeeds.get(0);

			assert cell.isValid();
			return cell;
		}
		return null;
	}

	@Override
	public void commitCluster(Cluster<?, ?> cluster) {
		for (Cell cell : cluster.getCells()) {
			PackCell packCell = (PackCell) cell;
			int numExternalPins = getNumExternalPinsOfCell(packCell);
			unclusteredCellsMap.get(numExternalPins).remove(packCell);
			if (unclusteredCellsMap.get(numExternalPins).isEmpty())
				unclusteredCellsMap.remove(numExternalPins);
			carryChains.remove(packCell);
			getCarryChainCells(packCell);
		}
	}

	private void getCarryChainCells(PackCell cell) {
		carryChains.addAll(getCarryChainCells(cell.getSinkCarryChainConnections()));
		carryChains.addAll(getCarryChainCells(cell.getSourceCarryChainConnections()));
	}

	private Collection<PackCell> getCarryChainCells(
			Collection<CarryChainConnection> cccs
	) {
		return cccs.stream()
				.map(CarryChainConnection::getEndCell)
				.filter(m -> m.getCluster() == null)
				.collect(Collectors.toList());
	}
}
