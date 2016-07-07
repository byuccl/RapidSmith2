package edu.byu.ece.rapidSmith.cad.packer.AAPack;

import edu.byu.ece.rapidSmith.cad.clusters.*;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.rules.PackRule;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.rules.PackRuleFactory;
import edu.byu.ece.rapidSmith.cad.packer.Packer;
import edu.byu.ece.rapidSmith.design.ClusterDesign;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.cad.clusters.PackingCompletionUtils;
import edu.byu.ece.rapidSmith.device.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 */
public final class AAPack<T extends ClusterType, S extends ClusterSite>
		implements Packer<T, S>
{
//	public static final Logger logger = Logger.getLogger("PACKER");

	public final ClusterDevice<T> clusterDevice;
	public final CellLibrary cellLibrary;
	public final List<Prepacker> prepackers;
	public final SeedSelector seedSelector;
	public final CellSelector cellSelector;
	public final BelSelector belSelector;
	public final ClusterCostCalculator<T, S> clusterCostCalculator;
	public final ClusterFactory<T, S> clusterFactory;

	public final List<PackRuleFactory> packRuleFactories = new ArrayList<>();
	public final PackingCompletionUtils<T, S> utils;

	Set<PackCell> unclusteredCells = new HashSet<>();
	Collection<PackCell> unpackableCells;
	ClusterDesign<T, S> clustersDesign;

	public List<PackRule> packRules;

	public AAPack(
			ClusterDevice<T> clusterDevice,
			CellLibrary cellLibrary,
			ClusterFactory<T, S> clusterFactory,
			PackingCompletionUtils<T, S> utils,
			List<Prepacker> prepackers,
			ClusterCostCalculator<T, S> clusterCostCalculator,
			SeedSelector seedSelector,
			CellSelector cellSelector,
			BelSelector belSelector) {
		this.clusterDevice = clusterDevice;
		this.cellLibrary = cellLibrary;
		this.clusterFactory = clusterFactory;
		this.utils = utils;
		this.prepackers = prepackers;
		this.clusterCostCalculator = clusterCostCalculator;
		this.seedSelector = seedSelector;
		this.cellSelector = cellSelector;
		this.belSelector = belSelector;
	}

	public void registerPackRule(PackRuleFactory ruleFactory) {
		packRuleFactories.add(ruleFactory);
	}

	public ClusterDesign<T, S> pack(CellDesign netlist) {
//		logger.log(Level.INFO, "PACKER_START", System.nanoTime());
		init(netlist);
		packNetlist();
		cleanupClusters(clustersDesign);
		handleUnpackableCells();
//		logger.log(Level.INFO, "PACKER_END", System.nanoTime());

		return clustersDesign;
	}

	private void packNetlist() {
		// do until all molecules have been packed
		while (!unclusteredCells.isEmpty()) {
			System.out.println("Cells remaining to pack " + unclusteredCells.size());
			PackCell seedCell = seedSelector.nextSeed();
			assert seedCell != null;
			assert seedCell.isValid();
			assert seedCell.getCluster() == null;

//			logger.log(Level.FINE, "SEED_START",
//					Arrays.asList(seedCell.getName(), System.nanoTime()).toArray());

			Cluster<T, S> best = null;
			for (T type : clusterFactory.getAvailableClusterTypes()) {
//				logger.log(Level.FINE, "CLUSTER_START",
//						Arrays.asList(type, System.nanoTime()).toArray());

				if (!clusterOfTypeAvailable(type)) {
//					logger.log(Level.FINE, "CLUSTER_END",
//							Arrays.asList("NO RESOURCES AVAILABLE", System.nanoTime()
//							).toArray()
//					);
					continue;
				}
				StatusClusterPair result = tryPackClusterType(seedCell, type);

				// Compute the final cluster cost and update best if necessary
				if (result.status == PackStatus.VALID) {
					Cluster<T, S> cluster = result.cluster;
					if (best == null || cluster.getCost() < best.getCost())
						best = cluster;
//					logger.log(Level.FINE, "CLUSTER_END",
//							Arrays.asList("VALID", printClusterContents(cluster),
//									cluster.getCost(), System.nanoTime()
//							).toArray()
//					);
				} else {
//					logger.log(Level.FINE, "CLUSTER_END",
//							Arrays.asList("INVALID", System.nanoTime()).toArray());
				}
			}

			if (best == null) {
//				logger.log(Level.FINE, "SEED_END",
//						Arrays.asList("Unpackable", System.nanoTime()));
				throw new AssertionError("No valid cluster for seed.");
			}
			commitCluster(best); // accept it and update all of the stats
//			logger.log(Level.FINE, "SEED_END",
//					Arrays.asList(best.getType(), System.nanoTime()));
		}
	}

	private String printClusterContents(Cluster<?, ?> cluster) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Map.Entry<Bel, PackCell> e : cluster.getPlacementMap().entrySet()) {
			if (!first) {
				sb.append(System.lineSeparator());
			} else {
				first = false;
			}
			sb.append(e.getValue().getName() + " @ " + e.getKey().getFullName());
		}
		return sb.toString();
	}

	private StatusClusterPair tryPackClusterType(
			PackCell seedCell, T type
	) {
		Cluster<T, S> cluster =
				clusterFactory.createNewCluster(seedCell.getName(), type);
		PackStatus result = tryPackCluster(cluster, seedCell);

		return new StatusClusterPair(result, cluster);
	}

	private void handleUnpackableCells() {
		for (PackCell cell : unpackableCells) {
			Cluster<T, S> cluster = new UnpackedCellCluster<>(cell);
			cell.setCluster(cluster);
			clustersDesign.addCluster(cluster);
		}
	}

	protected void init(CellDesign netlist) {
		initClusterDesign(netlist);

		prepack(clustersDesign);
		clearCellPackingInformation();
		removeUnpackableCells();
		new CarryChainFinder().findCarryChains(clusterDevice, clustersDesign.getNets());
		packRuleFactories.forEach(pr -> pr.init(clustersDesign));

		seedSelector.init(clusterDevice, unclusteredCells);
		cellSelector.init(unclusteredCells);
		clusterFactory.init();
		clusterCostCalculator.init(clusterFactory);
	}

	private void initClusterDesign(CellDesign netlist) {
		clustersDesign = new ClusterDesign<>(netlist.getName(), netlist.getPartName());
		netlist.getCells().forEach(c -> clustersDesign.addCell(c.deepCopy()));
		netlist.getNets().forEach(net ->{
			CellNet newNet = net.deepCopy();
			clustersDesign.addNet(newNet);
			net.getPins().forEach(pin -> {
				Cell newCell = clustersDesign.getCell(pin.getCell().getName());
				CellPin newPin = newCell.getPin(pin.getName());
				newNet.connectToPin(newPin);
			});
		});
	}

	private void prepack(ClusterDesign<?, ?> netlist) {
		Set<Cell> allCells = new HashSet<>(netlist.getCells());
		for (Prepacker prepacker : prepackers) {
			prepacker.prepack(allCells);
		}
		unclusteredCells = netlist.getCells().stream()
				.map(c -> (PackCell) c)
				.collect(Collectors.toSet());
	}

	private void clearCellPackingInformation() {
		unclusteredCells.forEach(c -> {
			c.setCluster(null);
			c.setValid(true);
			c.setLocationInCluster(null);
			c.setGain(Double.MAX_VALUE);
		});
	}

	private void removeUnpackableCells() {
		unpackableCells = new ArrayList<>();
		Iterator<PackCell> it = unclusteredCells.iterator();
		while (it.hasNext()) {
			PackCell cell = it.next();
			if (!cell.isPackable()) {
				unpackableCells.add(cell);
				cell.setValid(false);
				it.remove();
			}
		}
	}

	protected boolean clusterOfTypeAvailable(T clusterType) {
		return clusterFactory.getNumRemainingOfType(clusterType) > 0;
	}

	protected void commitCluster(Cluster<T, S> cluster) {
		// Mark all molecules in the cluster as invalid and set their cluster.
		// Commit the cluster to the seed selector and cluster generator.
		// Create the pin mapping and intrasite routing information for the cluster.
		clustersDesign.addCluster(cluster);
		for (Cell cell : cluster.getCells()) {
			PackCell packCell = (PackCell) cell;
			packCell.setCluster(cluster);
			packCell.setValid(false);
			packCell.setLocationInCluster(cluster.getCellPlacement(packCell));
			unclusteredCells.remove(packCell);
		}
		packRuleFactories.forEach(r -> r.commitCluster(cluster));
		seedSelector.commitCluster(cluster);
		clusterFactory.commitCluster(cluster);
	}

	PackStatus tryPackCluster(Cluster<T, S> cluster, PackCell seed) {
		assert cluster.getCells().isEmpty();
		assert seed.isValid();
		assert seed.isPackable();
		assert seed.getCluster() == null;

		cellSelector.initCluster(cluster);
		belSelector.initCluster(cluster);
		packRules = packRuleFactories.stream()
				.map(f -> f.createRule(cluster))
				.collect(Collectors.toList());

		AAPackState state = new AAPackState();
		state.cell = seed;
		belSelector.initCell(seed, null);

		fillCluster(cluster, state);

		PackStatus clusterStatus = state.status;
		if (clusterStatus == PackStatus.VALID) {
			double cost = clusterCostCalculator.calculateCost(cluster);
			cluster.setCost(cost);
		}

		packRules.forEach(PackRule::cleanup);
		cellSelector.cleanupCluster();
		belSelector.cleanupCluster();
		unbindCluster(state);

		for (Cell cell : cluster.getCells()) {
			PackCell packCell = (PackCell) cell;
			assert packCell.isValid();
			assert packCell.getCluster() == null;
		}

		return clusterStatus;
	}

	void unbindCluster(AAPackState state) {
		while(!state.isSeedState()) {
			state.packedCells.keySet().forEach(this::unbindCell);
			state.invalidatedCells.forEach(c -> c.setValid(true));
			state.rollback();
		}
		state.packedCells.keySet().forEach(this::unbindCell);
		state.invalidatedCells.forEach(c -> c.setValid(true));
	}

	void fillCluster(Cluster<T, S> cluster, AAPackState state) {
		fillClusterEntryCheck(state);

		// Roll back until we found a valid final cluster, or determined that
		// none exists.  Keeps us ending with a conditional cluster
		do {
			boolean breakFromLoop = false;
			// roll back loop
			do {
				tryPackCellsUntilSuccess(cluster, state);

				switch (state.status) {
					case INFEASIBLE:
						assert state.cell == null;
						// No rolling back seed
						if (!state.isSeedState()) {
							// rollback one and try cell on next BEL
//							logger.log(Level.FINER, "REVERT");
							rollBackLastCommit(cluster, state);
						}
						breakFromLoop = true;
						break;
					case CONDITIONAL:
						assert state.nextConditionals != null;
						if (packMore(cluster)) {
							commitCellBelPair(cluster, state, state.nextConditionals.keySet());
							nextCell(state);
						} else {
							breakFromLoop = true;
						}
						break;
					case VALID:
						if (packMore(cluster)) {
							commitCellBelPair(cluster, state, null);
							nextCell(state);
						} else {
							breakFromLoop = true;
						}
						break;
				}
			} while (!breakFromLoop);

			if (state.status == PackStatus.CONDITIONAL) {
				state.status = PackStatus.INFEASIBLE;
				revertBelChoice(cluster, state);
			} else if (state.status == PackStatus.INFEASIBLE) {
				assert state.isSeedState();
				break;
			} else {
				assert state.status == PackStatus.VALID;
				break;
			}
		} while (true);

		assert state.status == PackStatus.VALID || state.status == PackStatus.INFEASIBLE;
		assert state.status == PackStatus.VALID || state.packedCells.isEmpty();
	}

	private void fillClusterEntryCheck(AAPackState state) {
		assert state.isSeedState();
		assert state.status == PackStatus.INFEASIBLE;
		assert state.cell != null;
		assert state.invalidatedCells.isEmpty();
		assert state.prevConditionals == null;
		assert state.nextConditionals == null;
	}

	boolean packMore(Cluster<T, S> cluster) {
		return !cluster.isFull();
	}

	void nextCell(AAPackState state) {
		assert state.cell == null;
		assert state.status == PackStatus.INFEASIBLE;

		PackCell nextCell = cellSelector.nextCell();
		assert nextCell == null || nextCell.isValid();
		assert nextCell == null || nextCell.getCluster() == null;
		assert nextCell == null || state.prevConditionals == null ||
				state.prevConditionals.containsKey(nextCell);

		state.cell = nextCell;

		if (nextCell != null) {
			Collection<Bel> conditionals = state.prevConditionals != null ?
					state.prevConditionals.get(nextCell) :
					null;
			belSelector.initCell(nextCell, conditionals);
//			logger.log(Level.FINER, "PACK_CELL", nextCell.getName());
		}
	}

	void revertBelChoice(
			Cluster<?, ?> cluster, AAPackState state
	) {
		PackCell initialCell = state.cell;
		Collection<PackCell> packedCells =
				new ArrayList<>(state.packedCells.keySet());
		Collection<PackCell> invalidatedCells = state.invalidatedCells;

		revertBelChoiceEntryCheck(state, packedCells);
		revertState(cluster, state);
		revertBelChoiceExitCheck(cluster, state, initialCell, packedCells, invalidatedCells);
	}

	private void revertBelChoiceEntryCheck(
			AAPackState state, Collection<PackCell> packedCells
	) {
		assert state.cell != null;
		assert state.status == PackStatus.INFEASIBLE;
		assert !packedCells.stream()
				.map(PackCell::isValid)
				.reduce(false, Boolean::logicalOr);
	}

	private void revertBelChoiceExitCheck(
			Cluster<?, ?> cluster, AAPackState state, PackCell initialCell,
			Collection<PackCell> packedCells, Collection<PackCell> invalidatedCells
	) {
		assert state.cell == initialCell;
		assert initialCell.isValid();
		assert initialCell.getCluster() == null;
		assert !cluster.hasCell(state.cell);

		assert packedCells.stream()
				.map(PackCell::isValid)
				.reduce(true, Boolean::logicalAnd);
		assert state.invalidatedCells == invalidatedCells;
		assert state.packedCells.isEmpty();
		assert state.nextConditionals == null;
		assert state.status == PackStatus.INFEASIBLE;
	}

	AAPackState commitCellBelPair(
			Cluster<?, ?> cluster, AAPackState state, Set<PackCell> conditionals
	) {
		commitCellBelPairEntryCheck(cluster, state);

		cellSelector.commitCells(
				state.packedCells.keySet(),
				conditionals
		);
		belSelector.commitBels(state.packedCells.values());
		state.commit();

		commitCellBelPairExitCheck(state);

		return state;
	}

	private void commitCellBelPairEntryCheck(Cluster<?, ?> cluster, AAPackState state) {
		assert state.status != PackStatus.INFEASIBLE;
		assert state.cell != null;
		assert state.packedCells != null;
		assert state.packedCells.containsKey(state.cell);

		Collection<PackCell> packedCells = state.packedCells.keySet();
		assert !packedCells.stream()
				.map(PackCell::isValid)
				.reduce(false, Boolean::logicalOr);
		assert packedCells.stream()
				.map(c -> c.getCluster() == cluster)
				.reduce(true, Boolean::logicalAnd);
		assert packedCells.stream()
				.map(cluster::hasCell)
				.reduce(true, Boolean::logicalAnd);

		assert !state.invalidatedCells.stream()
				.map(PackCell::isValid)
				.reduce(false, Boolean::logicalOr);
	}

	private void commitCellBelPairExitCheck(AAPackState state) {
		assert state.cell == null;
		assert state.status == PackStatus.INFEASIBLE;
		assert state.packedCells.isEmpty();
		assert state.invalidatedCells.isEmpty();
		assert state.prevConditionals == state.stack.peek().nextConditionals;
		assert state.nextConditionals == null;
	}

	AAPackState revertToLastCommit(Cluster<?, ?> cluster, AAPackState state) {
		PackCell initialCell = state.cell;

		revertEntryCheck(cluster, state);

		belSelector.revertToLastCommit();

		revertState(cluster, state);
		state.cell.setValid(false);
		state.invalidatedCells.add(state.cell);
		state.cell = null;

		revertExitCheck(state, initialCell);

		return state;
	}

	private void revertEntryCheck(Cluster<?, ?> cluster, AAPackState state) {
		assert state.cell != null;
		assert state.cell.getCluster() == null;
		assert !cluster.hasCell(state.cell);
		assert state.status == PackStatus.INFEASIBLE;
		assert state.packedCells == null || state.packedCells.isEmpty();
		assert !state.invalidatedCells.contains(state.cell);
	}

	private void revertExitCheck(
			AAPackState state, PackCell initialCell
	) {
		assert state.cell == null;
		assert state.status == PackStatus.INFEASIBLE;
		assert state.packedCells.isEmpty();
		assert state.invalidatedCells.contains(initialCell);

		assert !state.isSeedState() || state.prevConditionals == null;
	}

	AAPackState rollBackLastCommit(Cluster<T, S> cluster, AAPackState state) {
		Collection<PackCell> invalidatedCells =
				new ArrayList<>(state.invalidatedCells);

		rollBackEntryCheck(state);

		belSelector.rollBackLastCommit();
		cellSelector.rollBackLastCommit();
		state.invalidatedCells.forEach(c -> c.setValid(true));
		state.rollback();

		rollBackExitCheck(cluster, state, invalidatedCells);

		return state;
	}

	private void rollBackEntryCheck(
			AAPackState state
	) {
		assert state.cell == null;
		assert state.packedCells == null || state.packedCells.isEmpty();
		assert state.status == PackStatus.INFEASIBLE;
	}

	private void rollBackExitCheck(
			Cluster<T, S> cluster, AAPackState state,
			Collection<PackCell> invalidatedCells
	) {
		assert invalidatedCells.stream()
				.map(PackCell::isValid)
				.reduce(true, Boolean::logicalAnd);
		assert !invalidatedCells.stream()
				.map(c -> c.getCluster() == cluster)
				.reduce(false, Boolean::logicalOr);
		assert !invalidatedCells.stream()
				.map(cluster::hasCell)
				.reduce(false, Boolean::logicalOr);

		assert state.cell != null;
		assert state.status != PackStatus.INFEASIBLE;
		assert state.packedCells.containsKey(state.cell);

		assert !state.packedCells.keySet().stream()
				.map(PackCell::isValid)
				.reduce(false, Boolean::logicalOr);
		assert state.packedCells.keySet().stream()
				.map(c -> c.getCluster() == cluster)
				.reduce(true, Boolean::logicalAnd);
		assert state.packedCells.keySet().stream()
				.map(cluster::hasCell)
				.reduce(true, Boolean::logicalAnd);
	}

	void tryPackCellsUntilSuccess(
			Cluster<T, S> cluster, AAPackState state
	) {
		assert state.status ==  PackStatus.INFEASIBLE;
		assert state.packedCells.isEmpty();
		assert state.invalidatedCells != null;

		while(state.cell != null && state.status == PackStatus.INFEASIBLE) {
			assert state.cell.isValid();
			assert state.cell.getCluster() == null;

			tryPackCell(cluster, state);

			switch (state.status) {
				case INFEASIBLE:
//					logger.log(Level.FINER, "REVERT");
					revertToLastCommit(cluster, state);
					// Don't want to choose a different seed cell
					if (state.isSeedState())
						return;
					nextCell(state);
					break;
				case CONDITIONAL:
					assert state.nextConditionals != null;
			}
		}
	}

	void tryPackCell(Cluster<T, S> cluster, AAPackState state) {
		tryPackCellEntryCheck(state);

		PackStatus status;
		PackCell cell = state.cell;

//		logger.log(Level.FINER, "PACK_CELL", new Object[] { cell, System.nanoTime() });

		Bel anchor;
		long start;
		do {
			start = System.nanoTime();
			anchor = belSelector.nextBel();
			if (anchor == null)
				break;

			// Work on a clean slate to easily roll back if necessary.
			status = addCellToCluster(cluster, cell, anchor, state.packedCells);
			assert status == PackStatus.INFEASIBLE || state.packedCells.keySet().contains(cell);
			assert status == PackStatus.INFEASIBLE || state.packedCells.get(cell).equals(anchor);

//			if (status == PackStatus.INFEASIBLE)
//				logger.log(Level.FINER, "CELL/BEL", new Object[] { "FAILED", "PLACEMENT", System.nanoTime() - start} );

			if (status != PackStatus.INFEASIBLE) {
				status = expandForcedPacking(cluster, state.packedCells);
//				if (status == PackStatus.INFEASIBLE)
//					logger.log(Level.FINER, "CELL/BEL", new Object[] { "FAILED", "EXPANSION", System.nanoTime() - start} );
			}

			state.status = status;
			state.nextConditionals = new HashMap<>();
			validateRules(start, state);

			if (state.status == PackStatus.INFEASIBLE) {
				revertBelChoice(cluster, state);
			}
		} while (state.status == PackStatus.INFEASIBLE);

		switch (state.status) {
			case CONDITIONAL:
				assert !state.nextConditionals.isEmpty();
//				logger.log(Level.FINER, "CELL/BEL", new Object[] { cell, anchor, "CONDITIONAL", start, System.nanoTime()} );
//				logger.log(Level.FINER, "PACK_CELL_RESULT", new Object[] { "CONDITIONAL", System.nanoTime()});
				break;
			case VALID:
//				logger.log(Level.FINER, "CELL/BEL", new Object[] { cell, anchor, "VALID", start, System.nanoTime()} );
//				logger.log(Level.FINER, "PACK_CELL_RESULT", new Object[] { "VALID", System.nanoTime()});
				state.nextConditionals = null;
				break;
			case INFEASIBLE:
				state.nextConditionals = null;
//				logger.log(Level.FINER, "PACK_CELL_RESULT", new Object[] { "INFEASIBLE", System.nanoTime()});
		}

		tryPackCellExitChecks(cluster, state);
	}

	private String printConditionals(Map<PackCell, Set<Bel>> conditionals) {
		StringBuilder sb = new StringBuilder();

		for (Map.Entry<PackCell, Set<Bel>> e : conditionals.entrySet()) {
			sb.append(e.getKey().toString());
			sb.append(": {");
			for (Bel bel : e.getValue()) {
				sb.append(bel.getFullName()).append(", ");
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append("}").append(System.lineSeparator());
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	private void tryPackCellEntryCheck(AAPackState state) {
		assert state.cell != null;
		assert state.cell.isValid();
		assert state.nextConditionals == null;
		assert state.packedCells.isEmpty();
	}

	private void tryPackCellExitChecks(Cluster<T, S> cluster, AAPackState state) {
		assert state.status == PackStatus.INFEASIBLE || state.packedCells.containsKey(state.cell);
		assert state.status == PackStatus.INFEASIBLE || !state.cell.isValid();
		assert state.status == PackStatus.INFEASIBLE || state.cell.getCluster() == cluster;
		assert state.status == PackStatus.INFEASIBLE || cluster.hasCell(state.cell);

		assert state.status != PackStatus.INFEASIBLE || state.packedCells.isEmpty();
		assert state.status != PackStatus.INFEASIBLE || state.cell.isValid();
		assert state.status != PackStatus.INFEASIBLE || state.cell.getCluster() == null;
		assert state.status != PackStatus.INFEASIBLE || !cluster.hasCell(state.cell);

		assert state.status != PackStatus.CONDITIONAL || state.nextConditionals != null;
		assert state.status != PackStatus.VALID || state.nextConditionals == null;
	}

	private void validateRules(long start, AAPackState state) {
		Iterator<PackRule> rulesIterator = packRules.iterator();
		while (rulesIterator.hasNext() && state.status != PackStatus.INFEASIBLE) {
			PackRule rule = rulesIterator.next();
			PackStatus result = rule.validate(state.packedCells.keySet());

			state.status = PackStatus.meet(state.status, result);
			if (result == PackStatus.CONDITIONAL) {
				if (rule.getConditionals().isEmpty())
					state.status = PackStatus.INFEASIBLE;
				else
					mergeConditionals(state.nextConditionals, rule.getConditionals());
			}
			state.checkedRules.add(rule);
//			if (result == PackStatus.INFEASIBLE)
//				logger.log(Level.FINER, "CELL/BEL", new Object[] { "FAILED", "RULE" + rule.getClass().getSimpleName(), System.nanoTime() - start} );
		}
	}

	void mergeConditionals(Map<PackCell, Set<Bel>> conditionals, Map<PackCell, Set<Bel>> toAdd) {
		for (Map.Entry<PackCell, Set<Bel>> e : toAdd.entrySet()) {
			assert !e.getValue().contains(null);
			if (!conditionals.containsKey(e.getKey())) {
				conditionals.put(e.getKey(), new HashSet<>(e.getValue()));
			} else {
				conditionals.get(e.getKey()).addAll(e.getValue());
			}
		}
	}

	private boolean cellCanBePlacedAt(Cluster<T, S> cluster, PackCell cell, Bel anchor) {
		return !cell.getRequiredBels(anchor).stream()
				.anyMatch(cluster::isBelOccupied);
	}

	PackStatus addCellToCluster(
			Cluster<T, S> cluster, PackCell cell,
			Bel anchor, Map<PackCell, Bel> packedCells
	) {
		boolean successfullyAdded = cellCanBePlacedAt(cluster, cell, anchor);
		if (successfullyAdded) {
			cluster.addCell(anchor, cell);
			cell.setCluster(cluster);
			cell.setLocationInCluster(anchor);

			// mark the cell as packed
			cell.setValid(false);
			packedCells.put(cell, anchor);
//			logger.log(Level.FINER, "ADD_CELL", new Object[] { cell.getName(), anchor.getFullName() });
			return PackStatus.VALID;
		} else {
			return PackStatus.INFEASIBLE;
		}
	}

	void removeCellFromCluster(
			Cluster<?, ?> cluster, PackCell cell
	) {
		assert cell.getCluster() == cluster;
		assert !cell.isValid();
		assert cluster.hasCell(cell);

		cell.setCluster(null);
		cell.setValid(true);
		cell.setLocationInCluster(null);
		cluster.removeCell(cell);
	}

	void unbindCell(PackCell cell) {
		cell.setValid(true);
		cell.setCluster(null);
		cell.setLocationInCluster(null);
	}

	/**
	 * Adds and places all cells within one hop from a cell in the cluster
	 * candidate whose location is determined by the netlist arrangement and
	 * available remaining BELs and routing resources in the cluster.
	 *
	 * @param candidate the cluster to add the cells to
	 * @return VALID if no routing conflicts occurred, else FAILED_ROUTE
	 */
	PackStatus expandForcedPacking(
			Cluster<T, S> candidate, Map<PackCell, Bel> packedCells
	) {
		int prevClusterSize = 0;

		while(candidate.getCells().size() != prevClusterSize) {
			prevClusterSize = candidate.getCells().size();

			PackStatus packStatus = expandRequiredSinks(candidate, packedCells);
			if (packStatus != PackStatus.VALID)
				return packStatus;

			packStatus = expandRequiredSources(candidate, packedCells);
			if (packStatus != PackStatus.VALID)
				return packStatus;
		}

		return PackStatus.VALID;
	}

	PackStatus expandRequiredSinks(
			Cluster<T, S> candidate, Map<PackCell, Bel> packedCells
	) {
		// TODO The pin needs to map to the relocated BEL, not the anchor BEL

		for (Cell sourceCell : new ArrayList<>(candidate.getCells())) {
			for (CellPin sourceCellPin : sourceCell.getOutputPins()) {
				if (!sourceCellPin.isConnectedToNet())
					continue;

				for (CellPin sinkCellPin : sourceCellPin.getNet().getPins()) {
					if (sinkCellPin == sourceCellPin)
						continue;
					PackCell sinkCell = (PackCell) sinkCellPin.getCell();
					Cluster<T, S> cellCluster = sinkCell.getCluster();
					if (cellCluster != null)
						continue;

					Set<Bel> sinkBels = getPossibleSinkBels(sourceCellPin, sinkCellPin, candidate);
					if (sinkBels == null) // means pins can both reachable from general fabric
						continue;

					// 0 bels could indicate a direct connection outside the cluster.
					if (sinkBels.size() == 0) {
						return PackStatus.INFEASIBLE;
					} else
					if (sinkBels.size() == 1) {
						Bel sinkBel = sinkBels.iterator().next();
						PackStatus packStatus = PackStatus.INFEASIBLE;
						if (sinkCell.isValid()) {
							packStatus = addCellToCluster(
									candidate, sinkCell, sinkBel, packedCells);
						}
						if (packStatus != PackStatus.VALID)
							return PackStatus.INFEASIBLE;
					}
				}
			}
		}

		return PackStatus.VALID;
	}

	// Null indicate the connections are possible through general fabric
	// Empty set indicates an invalid configuration
	// One element indicates a forced packing
	// More than one element indicates multiple possible packings
	Set<Bel> getPossibleSinkBels(
			CellPin sourceCellPin, CellPin sinkCellPin, Cluster<T, S> candidate
	) {
		ClusterTemplate<T> template = candidate.getTemplate();
		PackCell sourceCell = (PackCell) sourceCellPin.getCell();
		Bel sourceBel = sourceCell.getLocationInCluster();
		assert sourceCellPin.getPossibleBelPinNames(sourceBel.getId()).size() == 1;
		String belPinName = sourceCellPin
				.getPossibleBelPinNames(sourceBel.getId()).iterator().next();
		BelPin sourceBelPin = sourceBel.getBelPin(belPinName);
		assert sourceBelPin != null;

		// Check for direct connections
		for (DirectConnection dc : template.getDirectSinksOfCluster()) {
			if (dc.clusterPin.equals(sourceBelPin)) {
				List<String> possNames = sinkCellPin.getPossibleBelPinNames(dc.endPin.getId());
				if (possNames.contains(dc.endPin.getName()))
					return null;
			}
		}

		// Check if this connection is possible between sites
		boolean sourceDrivesFabric = sourceBelPin.drivesGeneralFabric();
		boolean sinkDrivenByFabric = cellPinDrivenByFabric(sinkCellPin);
		if (sourceDrivesFabric && sinkDrivenByFabric)
			return null;

		Set<Bel> sinkBels = new HashSet<>();
		Collection<ClusterConnection> sinkBelPins = template.getSinksOfSource(sourceBelPin);
		for (ClusterConnection sinkConnection : sinkBelPins) {
			BelPin sinkBelPin = sinkConnection.getPin();
			Bel sinkBel = sinkBelPin.getBel();
			if (candidate.isBelOccupied(sinkBel))
				continue;

			List<String> possibleSinkCellPins = sinkCellPin.getPossibleBelPinNames(sinkBel.getId());
			if (possibleSinkCellPins != null && possibleSinkCellPins.contains(sinkBelPin.getName())) {
				sinkBels.add(sinkBel);
			}
		}
		return sinkBels;
	}

	boolean cellPinDrivenByFabric(CellPin cellPin) {
		Cell cell = cellPin.getCell();
		for (BelId belId : cell.getLibCell().getPossibleAnchors()) {
			BelTemplate belTemplate = clusterDevice.getSiteTemplate(belId.getPrimitiveType())
					.getBelTemplates().get(belId.getName());
			List<String> possibleSourcePins = cellPin.getPossibleBelPinNames(belId);
			for (String sourcePin : possibleSourcePins) {
				boolean pinDrivesFabric = belTemplate.getPinTemplate(sourcePin).isDrivenByGeneralFabric();
				if (pinDrivesFabric)
					return true;
			}
		}
		return false;
	}

	PackStatus expandRequiredSources(
			Cluster<T, S> candidate, Map<PackCell, Bel> packedMolecules
	) {
		// TODO The pin needs to map to the relocated BEL, not the anchor BEL

		for (Cell sinkCell : new ArrayList<>(candidate.getCells())) {
			for (CellPin sinkCellPin : sinkCell.getInputPins()) {
				if (!sinkCellPin.isConnectedToNet())
					continue;

				CellPin sourceCellPin = sinkCellPin.getNet().getSourcePin();
				if (sourceCellPin == null)
					continue;
				PackCell sourceCell = (PackCell) sourceCellPin.getCell();
				if (sourceCell.getCluster() != null)
					continue;

				Set<Bel> sourceBels = getPossibleSourceBels(sinkCellPin, sourceCellPin, candidate);
				if (sourceBels == null) // means pins can both reached from general fabric
					continue;

				// Zero bels could be a direct connection.  I'd like to figure out if a pin
				// can be a direct connection or not but I currently don't have this info yet.
				if (sourceBels.size() == 0) {
					return PackStatus.INFEASIBLE;
				} else if (sourceBels.size() == 1) {
					Bel sourceBel = sourceBels.iterator().next();
					PackStatus packStatus = PackStatus.INFEASIBLE;
					if (sourceCell.isValid()) {
						packStatus = addCellToCluster(candidate, sourceCell, sourceBel,
								packedMolecules);
					}
					if (packStatus != PackStatus.VALID)
						return PackStatus.INFEASIBLE;
				}
			}
		}

		return PackStatus.VALID;
	}

	// Null indicate the connections are possible through general fabric
	// Empty set indicates an invalid configuration
	// One element indicates a forced packing
	// More than one element indicates multiple possible packings
	Set<Bel> getPossibleSourceBels(
			CellPin sinkCellPin, CellPin sourceCellPin, Cluster<T, S> candidate) {

		ClusterTemplate<T> template = candidate.getTemplate();
		Cell sourceCell = sourceCellPin.getCell();
		PackCell sinkCell = (PackCell) sinkCellPin.getCell();
		Bel sinkBel = sinkCell.getLocationInCluster();
		Collection<String> belPinNames = sinkCellPin.getPossibleBelPinNames(sinkBel.getId());

		Set<BelPin> sinkBelPins = belPinNames.stream()
				.map(sinkBel::getBelPin)
				.collect(Collectors.toSet());

		// Check for direct connections
		for (DirectConnection dc : template.getDirectSourcesOfCluster()) {
			for (BelPin sinkBelPin : sinkBelPins) {
				if (dc.clusterPin.equals(sinkBelPin)) {
					List<String> possNames = sourceCellPin.getPossibleBelPinNames(dc.endPin.getId());
					if (possNames.contains(dc.endPin.getName()))
						return null;
				}
			}
		}

		// test if the connection is possible between sites
		boolean sourceDrivesFabric = cellPinDrivesFabric(sourceCellPin);
		boolean sinkDrivenByFabric = false;
		for (BelPin sinkBelPin : sinkBelPins)
			sinkDrivenByFabric |= sinkBelPin.drivenByGeneralFabric();
		if (sourceDrivesFabric && sinkDrivenByFabric)
			return null;

		Collection<BelId> compatibleSourceBels = sourceCell.getLibCell().getPossibleAnchors();
		Set<Bel> sourceBels = new HashSet<>();
		for (BelPin sinkBelPin : sinkBelPins) {
			for (ClusterConnection sourceConn : template.getSourcesOfSink(sinkBelPin)) {
				BelPin sourceBelPin = sourceConn.getPin();
				Bel sourceBel = sourceBelPin.getBel();
				if (candidate.isBelOccupied(sourceBel))
					continue;

				if (compatibleSourceBels.contains(sourceBel.getId()))
					sourceBels.add(sourceBel);
			}
		}
		return sourceBels;
	}

	boolean cellPinDrivesFabric(CellPin cellPin) {
		Cell cell = cellPin.getCell();
		for (BelId belId : cell.getLibCell().getPossibleAnchors()) {
			BelTemplate belTemplate = clusterDevice.getSiteTemplate(belId.getPrimitiveType())
					.getBelTemplates().get(belId.getName());
			List<String> possibleSourcePins = cellPin.getPossibleBelPinNames(belId);
			assert possibleSourcePins.size() == 1;
			String sourcePin = possibleSourcePins.iterator().next();
			boolean pinDrivesFabric = belTemplate.getPinTemplate(sourcePin).drivesGeneralFabric();

			if (pinDrivesFabric)
				return true;
		}
		return false;
	}

	AAPackState revertState(Cluster<?, ?> cluster, AAPackState state) {
		assert state.status == PackStatus.INFEASIBLE;

		state.checkedRules.forEach(PackRule::revert);
		state.checkedRules.clear();
		state.packedCells.keySet().forEach(c -> removeCellFromCluster(cluster, c));
		state.packedCells.clear();
		state.nextConditionals = null;
		return state;
	}

	void cleanupClusters(ClusterDesign<T, S> clusterDesign) {
		utils.finishClusterConstruction(clusterDesign, clusterDevice);
	}

	/**
	 *
	 */
	class StatusClusterPair {
		public PackStatus status;
		public Cluster<T, S> cluster;

		public StatusClusterPair(PackStatus packStatus, Cluster<T, S> cluster) {
			this.status = packStatus;
			this.cluster = cluster;
		}
	}

	private static class AAPackState {
		Deque<State> stack = new ArrayDeque<>();
		PackCell cell;
		Map<PackCell, Bel> packedCells = new HashMap<>();
		PackStatus status = PackStatus.INFEASIBLE;
		List<PackCell> invalidatedCells = new ArrayList<>();
		Map<PackCell, Set<Bel>> prevConditionals;
		Map<PackCell, Set<Bel>> nextConditionals;
		List<PackRule> checkedRules = new ArrayList<>();

		public AAPackState() { }

		void commit() {
			stack.push(new State(this));
			cell = null;
			packedCells = new HashMap<>();
			status = PackStatus.INFEASIBLE;
			invalidatedCells = new ArrayList<>();
			prevConditionals = nextConditionals;
			nextConditionals = null;
			checkedRules = new ArrayList<>();
		}

		void rollback() {
			State state = stack.pop();
			cell = state.cell;
			packedCells = state.packedCells;
			status = state.status;
			invalidatedCells = state.invalidatedCells;
			prevConditionals = state.prevConditionals;
			nextConditionals = state.nextConditionals;
			checkedRules = state.checkedRules;
		}

		public boolean isSeedState() {
			return stack.isEmpty();
		}

		private static class State {
			PackCell cell;
			Map<PackCell, Bel> packedCells;
			PackStatus status;
			List<PackCell> invalidatedCells;
			Map<PackCell, Set<Bel>> prevConditionals;
			Map<PackCell, Set<Bel>> nextConditionals;
			List<PackRule> checkedRules;

			State(AAPackState curState) {
				cell = curState.cell;
				packedCells = curState.packedCells;
				status = curState.status;
				invalidatedCells = curState.invalidatedCells;
				prevConditionals = curState.prevConditionals;
				nextConditionals = curState.nextConditionals;
				checkedRules = curState.checkedRules;
			}
		}
	}

//	public static class FillClusterStats {
//		int cellAttempts = 0;
//		int belAttempts = 0;
//		int SUCCESS = 0;
//		int CONDITIONAL = 0;
//		int FAILED_PLACEMENT = 0;
//		int FAILED_EXPANSION = 0;
//		Map<Class<?>, Integer> failedRule = new HashMap<>();
//
//	}


}
