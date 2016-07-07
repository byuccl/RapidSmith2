package edu.byu.ece.rapidSmith.cad.packer.AAPack.rules;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterTemplate;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.PackStatus;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.PrimitiveSite;
import edu.byu.ece.rapidSmith.device.PrimitiveType;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class LUTRAMValidityChecksFactory implements PackRuleFactory {
	private final Set<LibraryCell> ramCellTypes = new HashSet<>();

	public LUTRAMValidityChecksFactory(CellLibrary cellLibrary) {
		ramCellTypes.add(cellLibrary.get("SPRAM32"));
		ramCellTypes.add(cellLibrary.get("SPRAM64"));
		ramCellTypes.add(cellLibrary.get("DPRAM32"));
		ramCellTypes.add(cellLibrary.get("DPRAM64"));
	}

	@Override
	public void init(CellDesign design) {
		Map<String, Ram> ramsMap = new HashMap<>();
		for (Cell cell : design.getCells()) {
			if (ramCellTypes.contains(cell.getLibCell())) {
				String ramGroup = (String) cell.getPropertyValue("$RAMGROUP");
				Ram ram = ramsMap.get(ramGroup);
				if (ram == null) {
					ram = new Ram();
					ramsMap.put(ramGroup, ram);
				}
				ram.cells.add((PackCell) cell);
				cell.updateProperty(Ram.class, PropertyType.USER, ram);
			}
		}
	}

	@Override
	public PackRule createRule(Cluster<?, ?> cluster) {
		return new LUTRAMValidityChecks(cluster);
	}

	public class LUTRAMValidityChecks implements PackRule {
		private Map<String, List<Bel>> lutRamsBels = new HashMap<>();
		private Cluster<?, ?> cluster;
		private State state;
		private Deque<State> stack = new ArrayDeque<>();

		public LUTRAMValidityChecks(Cluster<?, ?> cluster) {
			this.cluster = cluster;
			ClusterTemplate<?> template = cluster.getTemplate();
			for (Bel bel : template.getBels()) {
				if (bel.getSite().getType() == PrimitiveType.SLICEM) {
					if (bel.getName().matches("[A-D][5-6]LUT"))
						lutRamsBels.computeIfAbsent(bel.getName(), k -> new ArrayList<>(2))
								.add(bel);
				}
			}
			state = new State();
			state.status = PackStatus.VALID;
		}

		@Override
		public PackStatus validate(Collection<PackCell> changedCells) {
			stack.push(state);

			// check LUT is placed at a valid location
			List<PackCell> changedRamCells = changedCells.stream()
					.filter(c -> ramCellTypes.contains(c.getLibCell()))
					.collect(Collectors.toList());

			if (changedRamCells.isEmpty()) {
				return state.status;
			}

			updateState(changedRamCells);

			if (!ensureValidRamPositions(changedRamCells))
				return PackStatus.INFEASIBLE;

			StatusConditionalsPair result = ensureRamsAreComplete();
			state.status = result.status;
			state.conditionals = result.conditionals;
			if (state.status != PackStatus.VALID)
				return state.status;

			result = ensureDLutsUsed();
			state.status = result.status;
			state.conditionals = result.conditionals;

			return state.status;
		}

		private void updateState(List<PackCell> changedRamCells) {
			state = new State();
			state.status = PackStatus.VALID;
			state.conditionals = null;
			state.incompleteRams = new HashSet<>(stack.peek().incompleteRams);
			state.usedDLuts = new HashMap<>(stack.peek().usedDLuts);

			Set<Ram> rams = changedRamCells.stream()
					.map(c -> (Ram) c.getPropertyValue(Ram.class))
					.collect(Collectors.toSet());
			state.incompleteRams.addAll(rams);
			List<Ram> completedRams = rams.stream()
					.filter(Ram::fullyPacked)
					.collect(Collectors.toList());
			state.incompleteRams.removeAll(completedRams);

			for (PackCell cell : changedRamCells) {
				Bel bel = cell.getLocationInCluster();
				PrimitiveSite site = bel.getSite();
				SiteLutNumberPair pair = new SiteLutNumberPair(site,
						Integer.parseInt(bel.getName().substring(1, 2)));
				state.usedDLuts.computeIfAbsent(pair, k -> false);
				if (bel.getName().charAt(0) == 'D')
					state.usedDLuts.put(pair, true);
			}
		}

		private boolean ensureValidRamPositions(List<PackCell> changedRamCells) {
			for (PackCell cell : changedRamCells) {
				Bel location = cell.getLocationInCluster();
				String ramPosition = (String) cell.getPropertyValue("$RAMPOSITION");
				String locationName = location.getName();
				assert locationName.matches("[A-D][5-6]LUT");
				if (ramPosition.indexOf(ramPosition.charAt(0)) == -1)
					return false;
			}
			return true;
		}

		private StatusConditionalsPair ensureRamsAreComplete() {
			HashMap<PackCell, Set<Bel>> conditionals;
			if (!state.incompleteRams.isEmpty()) {
				conditionals = new HashMap<>();
				for (Ram ram : state.incompleteRams) {
					for (PackCell ramCell : ram.unpackedCells()) {
						Set<Bel> possibleLocations = getPossibleLocations(ramCell);
						if (possibleLocations.isEmpty())
							return new StatusConditionalsPair(PackStatus.INFEASIBLE, null);
						conditionals.put(ramCell, possibleLocations);
					}
				}
				return new StatusConditionalsPair(PackStatus.CONDITIONAL, conditionals);
			}
			return new StatusConditionalsPair(PackStatus.VALID, null);
		}

		private StatusConditionalsPair ensureDLutsUsed() {
			Set<Bel> unusedDLuts = state.usedDLuts.entrySet().stream()
					.filter(e -> !e.getValue())
					.map(Map.Entry::getKey)
					.map(p -> p.site.getBel("D" + p.lutNumber + "LUT"))
					.collect(Collectors.toSet());

			if (unusedDLuts.isEmpty())
				return new StatusConditionalsPair(PackStatus.VALID, null);

			Set<PackCell> connectedRamCells = getConnectedRams();

			if (connectedRamCells.isEmpty())
				return new StatusConditionalsPair(PackStatus.INFEASIBLE, null);

			HashMap<PackCell, Set<Bel>> conditionals = new HashMap<>();
			connectedRamCells.forEach(c -> conditionals.put(c, unusedDLuts));
			return new StatusConditionalsPair(PackStatus.CONDITIONAL, conditionals);
		}

		private Set<PackCell> getConnectedRams() {
			Set<PackCell> connectedCells = new HashSet<>();
			for (Cell cell : cluster.getCells()) {
				for (CellPin pin : cell.getPins()) {
					if (pin.isConnectedToNet() && !isFilteredNet(pin.getNet())) {
						for (CellPin o : pin.getNet().getPins()) {
							if (pin == o)
								continue;
							PackCell oCell = (PackCell) o.getCell();
							if (isValidRam(oCell))
								connectedCells.add(oCell);
						}
					}
				}
			}
			return connectedCells;
		}

		private boolean isFilteredNet(CellNet net) {
			return net.isClkNet() || net.isStaticNet() ||
					net.getPins().size() > 100;
		}

		private boolean isValidRam(PackCell cell) {
			return cell.isValid() && ramCellTypes.contains(cell.getLibCell()) &&
					((String) cell.getPropertyValue("$RAMPOSITION")).indexOf('D') != -1;

		}

		private Set<Bel> getPossibleLocations(PackCell ramCell) {
			Set<Bel> possibles = new HashSet<>();
			String locations = (String) ramCell.getPropertyValue("$RAMPOSITION");
			switch (ramCell.getLibCell().getName()) {
				case "SPRAM32":
				case "DPRAM32":
					for (char ch = 'A'; ch <= 'D'; ch++) {
						if (locations.indexOf(ch) != -1)
							possibles.addAll(lutRamsBels.get(ch + "5LUT"));
					}
				case "SPRAM64":
				case "DPRAM64":
					for (char ch = 'A'; ch <= 'D'; ch++) {
						if (locations.indexOf(ch) != -1)
							possibles.addAll(lutRamsBels.get(ch + "6LUT"));
					}
			}
			return possibles;
		}

		@Override
		public Map<PackCell, Set<Bel>> getConditionals() {
			return state.conditionals;
		}

		@Override
		public void revert() {
			state = stack.pop();
		}
	}

	private static class State {
		PackStatus status;
		Map<SiteLutNumberPair, Boolean> usedDLuts = new HashMap<>();
		Set<Ram> incompleteRams = new HashSet<>();
		Map<PackCell, Set<Bel>> conditionals = null;
	}

	private static class Ram {
		List<PackCell> cells = new ArrayList<>();

		boolean fullyPacked() {
			return cells.stream().allMatch(c -> c.getCluster() != null);
		}

		List<PackCell> unpackedCells() {
			return cells.stream()
					.filter(PackCell::isValid)
					.collect(Collectors.toList());
		}
	}

	private static class SiteLutNumberPair {
		final PrimitiveSite site;
		final int lutNumber;

		public SiteLutNumberPair(PrimitiveSite site, int lutNumber) {
			this.site = site;
			this.lutNumber = lutNumber;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SiteLutNumberPair that = (SiteLutNumberPair) o;
			return lutNumber == that.lutNumber &&
					Objects.equals(site, that.site);
		}

		@Override
		public int hashCode() {
			return Objects.hash(site, lutNumber);
		}

		@Override
		public String toString() {
			return "SiteLutNumberPair{" +
					"site=" + site +
					", lutNumber=" + lutNumber +
					'}';
		}
	}

	private static class StatusConditionalsPair {
		PackStatus status;
		Map<PackCell, Set<Bel>> conditionals;

		public StatusConditionalsPair(PackStatus status, Map<PackCell, Set<Bel>> conditionals) {
			this.status = status;
			this.conditionals = conditionals;
		}
	}
}
