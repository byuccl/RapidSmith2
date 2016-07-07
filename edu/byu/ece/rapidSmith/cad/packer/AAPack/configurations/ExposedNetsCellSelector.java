package edu.byu.ece.rapidSmith.cad.packer.AAPack.configurations;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.CellSelector;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class ExposedNetsCellSelector implements CellSelector {
	private static final int HIGH_FANOUT_LIMIT = 200;

	private double FULLY_EXPOSED_NET_PENALTY = -100.0;
	private double WEAKLY_EXPOSED_NET_PENALTY = -75.0;
	private double ABSORBED_NET_BONUS  = 25.0;
	private int PIN_ON_NET_PENALTY_CUTOFF = 8;
	private double EXPOSED_PIN_PENALTY =
			(FULLY_EXPOSED_NET_PENALTY - WEAKLY_EXPOSED_NET_PENALTY) / PIN_ON_NET_PENALTY_CUTOFF;

	private double SHARED_PIN_BONUS = 0.1;
	private double DANGLING_PIN_PENALTY = -0.1;
	private int DANGLING_PINS_CUTOFF = 8;

	private double PERCENT_SHARED_PINS_MAX = 1.0;
	private double PERCENT_SHARED_PINS_MIN = -1.0;

	// TODO right now the gain function is biased towards molecules with circular nets.

//	// Penalty for creating a new net that is exposed
//	private static final double NEW_EXPOSED_NET_PENALTY = 0.25;
//	// Value of completely absorbing a net
//	private static final double ABSORBED_NET_BONUS = 0.5;
//
//	private static final double EXPOSED_PINS_MODIFIER = 2.0;
//
//	// Weights towards more pins being more influential
//	private static final double PIN_MULTIPLIER = 1.01;
//	private static final double MAX_PIN_MULTIPLIER = Math.pow(PIN_MULTIPLIER, 8);
//	// Slight penalty for adding a new net to the molecule
//	private static final double NEW_NET_MULTIPLIER = 0.95;

	private Cluster<?, ?> cluster;

	private PriorityQueue<PackCell> feasibleCells;
	private Set<PackCell> markedCells;

	private Stack<List<PackCell>> committedStack;
	private Stack<Set<PackCell>> markedCellsStack;
	private Stack<Set<PackCell>> feasibleCellsStack;

	private Map<PackCell, Map<CellNet, Integer>> numPinsInCellMap;
	private Map<CellNet, Integer> numPinsInClusterMap;

	@Override
	public void init(Collection<PackCell> cells) {
		numPinsInCellMap = new HashMap<>();
		cells.forEach(m -> m.setInitialGain(computeInitialMoleculeGain(m)));
		cells.forEach(m -> m.setGain(null));
	}

	private double computeInitialMoleculeGain(PackCell cell) {
		Map<CellNet, Integer> discoveredNets = new HashMap<>();

		double gain = 0.0;
		for (CellPin pin : cell.getPins()) {
			if (!pin.isConnectedToNet())
				continue;

			CellNet net = pin.getNet();
			if (isFilteredNet(net))
				continue;

			gain += computeInitialExposedNetCost(discoveredNets, pin);
			gain += computeDanglingPinPenalty(discoveredNets, pin);
			gain += computePercentDanglingPinPenalty(discoveredNets, pin);

			discoveredNets.compute(net, (k, v) -> v == null ? 1 : v + 1);
		}
		numPinsInCellMap.put(cell, discoveredNets);
		return gain;
	}

	private double computeInitialExposedNetCost(Map<CellNet, Integer> discoveredNets, CellPin pin) {
		CellNet net = pin.getNet();
		double gain = 0.0;

		if (!discoveredNets.containsKey(net)) {
			gain += WEAKLY_EXPOSED_NET_PENALTY;
			int numPinsOnNet = net.getPins().size();
			// subtract 2: 1 for the pin on the molecule and 1 so an unabsorbed net will always
			// have a final cost of WEAKLY_EXPOSED_NET_PENALTY
			int pinsToPenalize = Integer.min((numPinsOnNet - 2), PIN_ON_NET_PENALTY_CUTOFF);
			if (pinsToPenalize < 0) {
				// this net neither comes from nor goes to anywhere, a dangling net
				// TODO maybe this should be handled differently
				gain -= WEAKLY_EXPOSED_NET_PENALTY;
				gain += ABSORBED_NET_BONUS;
			} else {
				gain += pinsToPenalize * EXPOSED_PIN_PENALTY;
			}
		} else {
			gain -= EXPOSED_PIN_PENALTY;
			if (discoveredNets.get(net) == net.getPins().size()) {
				gain -= WEAKLY_EXPOSED_NET_PENALTY;
				gain += ABSORBED_NET_BONUS;
			}
		}
		return gain;
	}

	private double computeDanglingPinPenalty(Map<CellNet, Integer> discoveredNets, CellPin pin) {
		CellNet net = pin.getNet();
		double gain = 0.0;

		int numPinsOnNet = net.getPins().size();
		if (!discoveredNets.containsKey(net)) {
			// subtract this pin from the number of nets
			int pinsToPenalize = Integer.min((numPinsOnNet - 1), DANGLING_PINS_CUTOFF);
			gain += pinsToPenalize * DANGLING_PIN_PENALTY;
		} else {
			int moleculePinsOnNet = discoveredNets.get(net) + 1;
			assert moleculePinsOnNet <= numPinsOnNet;

			if (numPinsOnNet - moleculePinsOnNet < DANGLING_PINS_CUTOFF) {
				gain -= DANGLING_PIN_PENALTY;
				gain += SHARED_PIN_BONUS;
			}
		}
		return gain;
	}

	private double computePercentDanglingPinPenalty(
			Map<CellNet, Integer> discoveredNets, CellPin pin
	) {
		CellNet net = pin.getNet();
		double gain = 0.0;

		int numPinsOnNet = net.getPins().size();
		double distance = PERCENT_SHARED_PINS_MAX - PERCENT_SHARED_PINS_MIN;
		double spacing = distance / (numPinsOnNet - 1);

		if (!discoveredNets.containsKey(net)) {
			gain += PERCENT_SHARED_PINS_MIN;
		} else {
			gain += spacing;
		}
		return gain;
	}

	private boolean isFilteredNet(CellNet net) {
		return net.isClkNet() || net.isStaticNet() ||
				net.getPins().size() > HIGH_FANOUT_LIMIT;
	}

	private double calculateGainFromAbsorbingPin(
			CellPin clusterPin, Map<CellNet, Integer> pinsInCell
	) {
		CellNet net = clusterPin.getNet();
		assert net != null;

		int numPinsInCluster = numPinsInClusterMap.get(net);
		int numPinsInMolecule = pinsInCell.get(net);
		int totalPinsAbsorbed = numPinsInCluster + numPinsInMolecule;

		double gain = 0.0;
		gain += calculateExposedNetGainFromAbsorbingPin(clusterPin, totalPinsAbsorbed);
		gain += calculateDanglingPinsGainFromAbsorbingPin(clusterPin, totalPinsAbsorbed);
		gain += calculatePercentDanglingPinsGainFromAbsorbingPin(clusterPin);

		return gain;
	}

	private double calculateExposedNetGainFromAbsorbingPin(CellPin pin, int totalPinsAbsorbed) {
		CellNet net = pin.getNet();
		int numPinsOnNet = net.getPins().size();
		assert numPinsOnNet >= totalPinsAbsorbed;

		double gain = 0.0;
		if (totalPinsAbsorbed == numPinsOnNet) {
			gain -= WEAKLY_EXPOSED_NET_PENALTY;
			gain += ABSORBED_NET_BONUS;
		} else {
			gain -= EXPOSED_PIN_PENALTY;
		}
		return gain;
	}

	private double calculateDanglingPinsGainFromAbsorbingPin(CellPin pin, int totalPinsAbsorbed) {
		CellNet net = pin.getNet();
		int numPinsOnNet = net.getPins().size();
		assert numPinsOnNet >= totalPinsAbsorbed;

		double gain = 0.0;
		if (numPinsOnNet - totalPinsAbsorbed < DANGLING_PINS_CUTOFF) {
			gain -= DANGLING_PIN_PENALTY;
			gain += SHARED_PIN_BONUS;
		}
		return gain;
	}

	private double calculatePercentDanglingPinsGainFromAbsorbingPin(CellPin pin) {
		CellNet net = pin.getNet();

		int numPinsOnNet = net.getPins().size();
		double distance = PERCENT_SHARED_PINS_MAX - PERCENT_SHARED_PINS_MIN;
		return distance / (numPinsOnNet - 1);
	}

	@Override
	public void initCluster(Cluster<?, ?> cluster) {
		assert this.cluster == null;

		this.cluster = cluster;

		feasibleCells = null;
		markedCells = new HashSet<>();

		feasibleCellsStack = new Stack<>();
		markedCellsStack = new Stack<>();
		markedCellsStack.push(markedCells);
		committedStack = new Stack<>();
		committedStack.push(new ArrayList<>());

		numPinsInClusterMap = new HashMap<>();
	}

	@Override
	public PackCell nextCell() {
		assert !cluster.getCells().isEmpty();

		PackCell cell = feasibleCells.poll();
		if (cell != null && !cellIsMarked(cell))
			markedCells.add(cell); // need to add if it on a large conditional
		return cell;
	}

	private boolean cellIsMarked(PackCell toCheck) {
		for (Set<PackCell> packCells : markedCellsStack) {
			if (packCells.contains(toCheck))
				return true;
		}
		return false;
	}

	private PriorityQueue<PackCell> makeFeasibleMoleculesQueue() {
		return new PriorityQueue<>(
				Comparator.comparing(PackCell::getGain).reversed());
	}

	private Set<PackCell> getValidMolecules(Collection<PackCell> conditionals) {
		if (conditionals != null) {
			return conditionals.stream()
					.filter(PackCell::isValid)
					.collect(Collectors.toSet());
		} else {
			HashSet<PackCell> molecules = new HashSet<>();
			for (Set<PackCell> markedMolecules : markedCellsStack) {
				markedMolecules.stream()
						.filter(PackCell::isValid)
						.forEach(molecules::add);
			}
			return molecules;
		}
	}

	@Override
	public void commitCells(Collection<PackCell> cells, Collection<PackCell> conditionals) {
		for (PackCell cell : cells) {
			assert !cell.isValid();
			assert cell.getCluster() == cluster;
		}

		checkpoint();

		for (PackCell cell : cells) {
			updateCosts(cell);
		}
		committedStack.peek().addAll(cells);

		feasibleCells = makeFeasibleMoleculesQueue();
		feasibleCells.addAll(getValidMolecules(conditionals));
	}

	private void updateCosts(PackCell cell) {
		for (CellPin pin : cell.getPins()) {
			if (!pin.isConnectedToNet())
				continue;

			CellNet net = pin.getNet();
			if (isFilteredNet(net))
				continue;

			numPinsInClusterMap.compute(net, (k, v) -> v == null ? 1 : v + 1);

			for (CellPin sinkPin : net.getPins()) {
				if (sinkPin == pin)
					continue;

				PackCell sinkCell = (PackCell) sinkPin.getCell();
				if (cell == sinkCell || !sinkCell.isValid())
					continue;

				Map<CellNet, Integer> pinsInCell = numPinsInCellMap.get(sinkCell);

				double gain;
				if (sinkCell.getGain() == null) {
					gain = sinkCell.getInitialGain();
					markedCells.add(sinkCell);
				} else {
					gain = sinkCell.getGain();
				}

				gain += calculateGainFromAbsorbingPin(pin, pinsInCell);
				sinkCell.setGain(gain);
			}
		}
	}

	@Override
	public void cleanupCluster() {
		for (Set<PackCell> mc : markedCellsStack) {
			for (PackCell cell : mc) {
				cell.setGain(null);
			}
		}
		markedCellsStack = null;
		markedCells = null;
		cluster = null;
	}

	public void checkpoint() {
		Set<PackCell> oldFeasible = null;
		if (feasibleCells != null)
			oldFeasible = new HashSet<>(feasibleCells);
		feasibleCellsStack.push(oldFeasible);
		markedCells = new HashSet<>();
		markedCellsStack.push(markedCells);
		committedStack.push(new ArrayList<>());
	}

	@Override
	public void rollBackLastCommit() {
		// rollback gains
		for (PackCell moleculeToRemove : committedStack.pop()) {
			uncommitMolecule(moleculeToRemove);
		}
		markedCellsStack.pop().forEach(m -> m.setGain(null));
		markedCells = markedCellsStack.peek();

		Set<PackCell> updated = feasibleCellsStack.pop();
		if (updated != null) {
			feasibleCells = makeFeasibleMoleculesQueue();
			feasibleCells.addAll(updated);
		} else {
			feasibleCells = null;
		}
	}

	private void uncommitMolecule(PackCell cell) {
		for (CellPin pin : cell.getPins()) {
			if (!pin.isConnectedToNet())
				continue;

			CellNet net = pin.getNet();
			if (isFilteredNet(net))
				continue;

			for (CellPin sinkPin : net.getPins()) {
				if (sinkPin == pin)
					continue;

				PackCell sinkCell = (PackCell) sinkPin.getCell();
				if (sinkCell == cell || !sinkCell.isValid())
					continue;
				Map<CellNet, Integer> pinsInCell = numPinsInCellMap.get(sinkCell);

				double gain = sinkCell.getGain();
				gain += calculateGainFromRemovingPin(pin, pinsInCell);
				sinkCell.setGain(gain);
			}

			numPinsInClusterMap.compute(net, (k, v) -> v - 1);
		}
	}

	private double calculateGainFromRemovingPin(
			CellPin clusterPin, Map<CellNet, Integer> pinsInCell
	) {
		CellNet net = clusterPin.getNet();
		assert net != null;

		int numPinsInCluster = numPinsInClusterMap.get(net);
		int numPinsInMolecule = pinsInCell.get(net);
		int totalPinsAbsorbed = numPinsInCluster + numPinsInMolecule;

		double gain = 0.0;
		gain += calculateExposedNetGainFromRemovingPin(clusterPin, totalPinsAbsorbed);
		gain += calculateDanglingPinsGainFromRemovingPin(clusterPin, totalPinsAbsorbed);
		gain += calculatePercentDanglingPinsGainFromRemovingPin(clusterPin);

		return gain;
	}

	private double calculateExposedNetGainFromRemovingPin(CellPin pin, int totalPinsAbsorbed) {
		CellNet net = pin.getNet();
		int numPinsOnNet = net.getPins().size();
		assert numPinsOnNet >= totalPinsAbsorbed;

		double gain = 0.0;
		if (totalPinsAbsorbed == numPinsOnNet) {
			gain += WEAKLY_EXPOSED_NET_PENALTY;
			gain -= ABSORBED_NET_BONUS;
		} else {
			gain += EXPOSED_PIN_PENALTY;
		}
		return gain;
	}

	private double calculateDanglingPinsGainFromRemovingPin(CellPin pin, int totalPinsAbsorbed) {
		CellNet net = pin.getNet();
		int numPinsOnNet = net.getPins().size();
		assert numPinsOnNet >= totalPinsAbsorbed;

		double gain = 0.0;
		if (numPinsOnNet - totalPinsAbsorbed < DANGLING_PINS_CUTOFF) {
			gain += DANGLING_PIN_PENALTY;
			gain -= SHARED_PIN_BONUS;
		}
		return gain;
	}

	private double calculatePercentDanglingPinsGainFromRemovingPin(CellPin pin) {
		CellNet net = pin.getNet();

		int numPinsOnNet = net.getPins().size();
		double distance = PERCENT_SHARED_PINS_MIN - PERCENT_SHARED_PINS_MAX;
		return distance / (numPinsOnNet - 1);
	}
}
