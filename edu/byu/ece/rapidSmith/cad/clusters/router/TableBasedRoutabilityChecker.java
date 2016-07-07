package edu.byu.ece.rapidSmith.cad.clusters.router;

import edu.byu.ece.rapidSmith.cad.clusters.*;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.Routability;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.util.StackedHashMap;

import java.util.*;

import static edu.byu.ece.rapidSmith.cad.clusters.router.RoutingTable.SinkPinEntry;
import static edu.byu.ece.rapidSmith.cad.clusters.router.RoutingTable.SourcePinEntry;

/**
 *
 */
public class TableBasedRoutabilityChecker implements RoutabilityChecker {
	Cluster<?, ?> cluster;
	ClusterTemplate<?> template;
	Device device;

	//	Map<CellPin, BelPin> pinMap = new HashMap<>();
	StackedHashMap<BelPin, CellPin> bel2CellPinMap = new StackedHashMap<>();
	StackedHashMap<CellPin, BelPin> cell2BelPinMap = new StackedHashMap<>();
	StackedHashMap<CellNet, Source> netSources;
	StackedHashMap<CellNet, Sinks> netSinks;
	StackedHashMap<PinGroup, PinGroupStatus> pinGroupsStatuses = new StackedHashMap<>();

	public TableBasedRoutabilityChecker(
			Cluster<?, ?> cluster, Device device) {
		this.cluster = cluster;
		this.template = cluster.getTemplate();
		this.device = device;
		this.netSources = new StackedHashMap<>();
		this.netSinks = new StackedHashMap<>();
	}

	@Override
	public RoutabilityResult check(Collection<PackCell> changed) {
		initNewNets(changed);
		updateChangedNets(changed);
		Set<PinGroup> changedGroups = getChangedGroups(changed);
		boolean noInvalids = checkGroups(changedGroups);
		if (!noInvalids)
			return new RoutabilityResult(Routability.INFEASIBLE);
		Routability status = pinGroupsStatuses.values().stream()
				.map(pg -> pg.feasibility)
				.reduce(Routability.FEASIBLE, Routability::meet);
		if (status == Routability.CONDITIONAL)
			return new RoutabilityResult(Routability.CONDITIONAL, joinGroupConditionals());
		else
			return new RoutabilityResult(Routability.FEASIBLE);
	}

	// Add sources and sinks for each net in the design.  For now treat
	// all inputs as outside the cluster.  We'll move them into the
	// cluster with the updateSinks/Sources coming next.
	private void initNewNets(Collection<PackCell> changed) {
		for (PackCell cell : changed) {
			for (CellPin pin : cell.getPins()) {
				if (pin.isConnectedToNet()) {
					CellNet net = pin.getNet();
					if (!netSources.containsKey(net)) {
						initNetSourcesAndSinks(net);
					}
				}
			}
		}
	}

	private void initNetSourcesAndSinks(CellNet net) {
		initNetSource(net);
		initNetSinks(net);
	}

	private void initNetSource(CellNet net) {
		Source source = new Source();
		if (net.isStaticNet()) {
			initStaticNetSource(net, source);
		} else {
			CellPin sourcePin = net.getSourcePin();
			PackCell sourceCell = (PackCell) sourcePin.getCell();
			source.cellPin = sourcePin;

			if (sourceCell.getCluster() != null && sourceCell.getCluster() != cluster) {
				initOutsideClusterSource(source, sourcePin);
			} else {
				initUnplacedSource(source, sourcePin, sourceCell);
			}
		}

		netSources.put(net, source);
	}

	private void initStaticNetSource(CellNet net, Source source) {
		if (net.getType() == NetType.VCC) {
			source.vcc = true;
			source.drivesGeneralFabric = true;
		} else {
			assert net.getType() == NetType.GND;
			source.gnd = true;
			source.drivesGeneralFabric = true;
		}
	}

	private void initOutsideClusterSource(Source source, CellPin sourcePin) {
		source.isPlaced = true;
		PackCell sourceCell = (PackCell) sourcePin.getCell();
		Bel bel = sourceCell.getLocationInCluster();

		// The source cell has already been placed so we know where it is and
		// where it enters this cluster.
		List<BelPin> belPins = sourcePin.getPossibleBelPins(bel);
		assert belPins.size() == 1;
		BelPin belPin = belPins.get(0);
		Integer endSiteIndex = bel.getSite().getIndex();

		source.drivesGeneralFabric = belPin.drivesGeneralFabric();

		for (DirectConnection dc : template.getDirectSourcesOfCluster()) {
			if (endSiteIndex == dc.endSiteIndex &&
					dc.endPin.equals(belPin.getTemplate())) {
				source.sourceWires.add(dc.clusterExit);
			}
		}
	}

	// TODO this creates a union of all possible sources.  In reality only one
	// source can be actual.  More realistically, we need to treat each source
	// individually and verify that the group works for the source.  This could
	// be tricky.
	private void initUnplacedSource(Source source, CellPin sourcePin, Cell sourceCell) {
		// Possible sources contains the BelPinTemplates of all BelPins that can
		// potentially be the source of this net.
		List<BelPinTemplate> possibleSources = new ArrayList<>();
		// The site index of the pin is the source is placed, else null
		// The cell has not been placed so we'll evaluate all possible
		// placements of the cell.  Unfortunately it does require exploring all
		// Site Templates.
		List<BelId> compatibleBels = sourceCell.getLibCell().getPossibleAnchors();
		for (SiteTemplate siteTemplate : device.getSiteTemplates().values()) {
			for (BelTemplate belTemplate : siteTemplate.getBelTemplates().values()) {
				if (compatibleBels.contains(belTemplate.getId())) {
					List<String> pinNames = sourcePin.getPossibleBelPinNames(
							belTemplate.getId());
					pinNames.forEach(n -> possibleSources.add(
							belTemplate.getPinTemplate(n)));
				}
			}
		}

		source.drivesGeneralFabric =
				possibleSources.stream().anyMatch(BelPinTemplate::drivesGeneralFabric);

		for (DirectConnection dc : template.getDirectSourcesOfCluster()) {
			if (possibleSources.contains(dc.endPin))
				source.sourceWires.add(dc.clusterExit);
		}
	}

	// Just create an object.  The sinks will be built when a source is added
	// into the cluster.
	private void initNetSinks(CellNet net) {
		netSinks.put(net, new Sinks());
	}

	private void initOutsideClusterSinks(Sinks sinks, CellPin sinkPin, PackCell sinkCell) {
		// The source cell has already been placed so we know where it is and
		// where it enters this cluster.
		Bel sinkBel = sinkCell.getLocationInCluster();
		List<BelPin> belPins = sinkPin.getPossibleBelPins(sinkBel);
		assert belPins.size() == 1;
		BelPin belPin = belPins.get(0);
		Integer endSiteIndex = sinkBel.getSite().getIndex();

		boolean directSink = false;
		Set<Wire> carrySinks = new HashSet<>();
		for (DirectConnection dc : template.getDirectSinksOfCluster()) {
			if (endSiteIndex == dc.endSiteIndex &&
					dc.endPin.equals(belPin.getTemplate())) {
				carrySinks.add(dc.clusterExit);
				directSink = true;
			}
		}

		boolean drivenGenerally = belPin.drivenByGeneralFabric();
		if (drivenGenerally && directSink) {
			sinks.optionalCarryChains.put(sinkPin, carrySinks);
		} else if (drivenGenerally) {
			sinks.mustLeave |= true;
		} else {
			sinks.requiredCarryChains.put(sinkPin, carrySinks);
		}
	}

	private void initUnplacedSinks(Sinks sinks, CellPin sinkPin, Cell sinkCell) {
		// Possible sources contains the BelPinTemplates of all BelPins that can
		// potentially be the source of this net.
		List<BelPinTemplate> possibleSinks = new ArrayList<>();
		// The site index of the pin is the source is placed, else null
		// The cell has not been placed so we'll evaluate all possible
		// placements of the cell.  Unfortunately it does require exploring all
		// Site Templates.
		List<BelId> compatibleBels = sinkCell.getLibCell().getPossibleAnchors();
		for (SiteTemplate siteTemplate : device.getSiteTemplates().values()) {
			for (BelTemplate belTemplate : siteTemplate.getBelTemplates().values()) {
				if (compatibleBels.contains(belTemplate.getId())) {
					List<String> pinNames = sinkPin.getPossibleBelPinNames(
							belTemplate.getId());
					pinNames.forEach(n -> possibleSinks.add(
							belTemplate.getPinTemplate(n)));
				}
			}
		}

		boolean reachableFromGeneralFabric = possibleSinks.stream()
				.anyMatch(BelPinTemplate::isDrivenByGeneralFabric);

		boolean directSink = false;
		Set<Wire> carryExits = new HashSet<>();
		for (DirectConnection dc : template.getDirectSinksOfCluster()) {
			if (possibleSinks.contains(dc.endPin)) {
				carryExits.add(dc.clusterExit);
				directSink = true;
			}
		}

		if (reachableFromGeneralFabric && directSink) {
			sinks.optionalCarryChains.put(sinkPin, carryExits);
		} else if (directSink) {
			sinks.requiredCarryChains.put(sinkPin, carryExits);
		} else if (reachableFromGeneralFabric) {
			sinks.conditionalMustLeaves.add(sinkPin);
		} else {
			sinks.conditionals.add(sinkPin);
		}
	}

	private void updateChangedNets(Collection<PackCell> changed) {
		for (Cell cell : changed) {
			for (CellPin pin : cell.getPins()) {
				if (pin.isConnectedToNet()) {
					if (pin.isInpin()) {
						updateSinkPin(pin);
					}
					if (pin.isOutpin()) {
						updateSourcePin(pin);
					}
				}
			}
		}
	}

	private void updateSinkPin(CellPin sinkPin) {
		CellNet net = sinkPin.getNet();
		Sinks sinks = netSinks.get(net);
		if (!netSinks.isCurrent(net)) {
			sinks = new Sinks(sinks);
			netSinks.put(net, sinks);
		}
		// remove the pin from the conditionals
		sinks.conditionalMustLeaves.remove(sinkPin);
		sinks.requiredCarryChains.remove(sinkPin);
		sinks.conditionals.remove(sinkPin);

		PackCell sinkCell = (PackCell) sinkPin.getCell();
		Bel sinkBel = sinkCell.getLocationInCluster();
		List<BelPin> possibleBelPins = sinkPin.getPossibleBelPins(sinkBel);
		assert possibleBelPins.size() == 1 : "I don't handle pin permutability";
		BelPin belPin = possibleBelPins.get(0);
		sinks.sinkPinsInCluster.add(sinkPin);
		bel2CellPinMap.put(belPin, sinkPin);
		cell2BelPinMap.put(sinkPin, belPin);
	}

	private void updateSourcePin(CellPin sourcePin) {
		CellNet net = sourcePin.getNet();
		assert !net.isStaticNet();

		Source source = netSources.get(net);
		if (!netSources.isCurrent(net)) {
			source = new Source(source);
			netSources.put(net, source);
		}

		// clear the old source info
		source.sourceWires = Collections.emptyList();

		// update the placement
		assert !source.isPlaced;
		source.isPlaced = true;
		PackCell sourceCell = (PackCell) sourcePin.getCell();
		Bel bel = sourceCell.getLocationInCluster();

		List<BelPin> belPins = sourcePin.getPossibleBelPins(bel);
		assert belPins.size() == 1;
		BelPin belPin = belPins.get(0);
		source.belPin = belPin;
		source.drivesGeneralFabric = belPin.drivesGeneralFabric();
		bel2CellPinMap.put(belPin, sourcePin);
		cell2BelPinMap.put(sourcePin, belPin);

		// update the sinks with external routes now
		Sinks sinks = netSinks.get(net);
		for (CellPin sinkPin : net.getSinkPins()) {
			PackCell sinkCell = (PackCell) sinkPin.getCell();
			if (sinkCell.getCluster() == null) {
				initUnplacedSinks(sinks, sinkPin, sinkCell);
			} else if (sinkCell.getCluster() != cluster) {
				initOutsideClusterSinks(sinks, sinkPin, sinkCell);
			}
		}
	}

	private Set<PinGroup> getChangedGroups(Collection<PackCell> changed) {
		Set<PinGroup> pinGroups = new HashSet<>();
		for (PackCell cell : changed) {
			Collection<CellPin> pins = cell.getPins();
			for (CellPin pin : pins) {
				if (pin.isConnectedToNet()) {
					BelPin belPin = cell2BelPinMap.get(pin);
					PinGroup pinGroup = template.getPinGroup(belPin);
					pinGroups.add(pinGroup);
				} else if (isLUTOpin(pin)) {
					Bel bel = cell.getLocationInCluster();
					List<BelPin> belPins = pin.getPossibleBelPins(bel);
					assert belPins.size() == 1;
					BelPin belPin = belPins.get(0);
					pinGroups.add(template.getPinGroup(belPin));
				}
			}
		}
		return pinGroups;
	}

	private boolean isLUTOpin(CellPin cellPin) {
		Cell cell = cellPin.getCell();
		LibraryCell libCell = cell.getLibCell();
		if (!libCell.isLut())
			return false;
		return cellPin.isOutpin();
	}

	private boolean checkGroups(Set<PinGroup> groupsToCheck) {
		for (PinGroup pg : groupsToCheck) {
			boolean validRowFound = false;
			boolean conditionalRowFound = false;
			List<RoutingTable.Row> tableRows = pg.routingTable.rows;

			PinGroupStatus oldPgStatus = getPinGroupStatus(pg);
			assert oldPgStatus != null;
			ArrayList<RowStatus> rowStatusList = new ArrayList<>();

			int i;
			for (i = 0; i < tableRows.size(); i++) {
				RoutingTable.Row row = tableRows.get(i);

				if (getRowStatus(oldPgStatus, i).feasibility == Routability.INFEASIBLE) {
					rowStatusList.add(new RowStatus(Routability.INFEASIBLE));
					continue;
				}

				RowStatus rowStatus = checkRow(pg, row);
				rowStatusList.add(rowStatus);
				if (rowStatus.feasibility == Routability.CONDITIONAL) {
					conditionalRowFound = true;
				} else if (rowStatus.feasibility == Routability.FEASIBLE) {
					validRowFound = true;
					i++; break;
				}
			}

			PinGroupStatus pgStatus = new PinGroupStatus();
			if (validRowFound) {
				pgStatus.feasibility = Routability.FEASIBLE;
			} else if (conditionalRowFound) {
				pgStatus.feasibility = Routability.CONDITIONAL;
			} else {
				pgStatus.feasibility = Routability.INFEASIBLE;
				return false;
			}

			// fill out remaining rows
			for (; i < tableRows.size(); i++) {
				rowStatusList.add(getRowStatus(oldPgStatus, i));
			}
			pgStatus.rowStatuses = rowStatusList;
			pinGroupsStatuses.put(pg, pgStatus);
		}
		return true;
	}

	private RowStatus getRowStatus(PinGroupStatus oldPgStatus, int i) {
		if (oldPgStatus.rowStatuses == null)
			return new RowStatus(Routability.FEASIBLE);
		if (oldPgStatus.rowStatuses.size() <= i)
			return new RowStatus(Routability.FEASIBLE);
		return oldPgStatus.rowStatuses.get(i);
	}

	private PinGroupStatus getPinGroupStatus(PinGroup pg) {
		PinGroupStatus status = pinGroupsStatuses.get(pg);
		if (status == null)
			return new PinGroupStatus(Routability.FEASIBLE);
		return status;
	}

	private RowStatus checkRow(PinGroup pg, RoutingTable.Row row) {
		RowStatus rowStatus = new RowStatus();

		checkRowSinks(pg, row, rowStatus);
		if (rowStatus.feasibility == Routability.INFEASIBLE)
			return rowStatus;
		checkRowSources(row, rowStatus);
		return rowStatus;
	}

	private void checkRowSources(RoutingTable.Row row, RowStatus rowStatus) {
		for (Map.Entry<BelPin, SourcePinEntry> e : row.sourcePins.entrySet()) {
			BelPin belPin = e.getKey();
			CellPin cellPin = bel2CellPinMap.get(belPin);
			if (cellPin == null)
				continue;

			IsRowValidForSourceReturn result = isRowValidForSource(
					row, cellPin, belPin);

			if (result.status == Routability.CONDITIONAL) {
				rowStatus.feasibility = Routability.CONDITIONAL;
				mergeConditionalsInRow(rowStatus, result.conditionalSinks);
			} else if (result.status == Routability.INFEASIBLE) {
				rowStatus.feasibility = Routability.INFEASIBLE;
			}

			if (rowStatus.feasibility == Routability.INFEASIBLE) {
				// exit early condition
				break;
			}
		}
	}

	private void checkRowSinks(PinGroup pg, RoutingTable.Row row, RowStatus rowStatus) {
		for (Map.Entry<BelPin, SinkPinEntry> e : row.sinkPins.entrySet()) {
			assert rowStatus.feasibility != Routability.INFEASIBLE;

			BelPin belPin = e.getKey();
			CellPin cellPin = bel2CellPinMap.get(belPin);
			if (cellPin == null)
				continue;
			IsRowValidForSinkReturn result = isRowValidForSink(
					pg, row, cellPin, belPin);

			rowStatus.feasibility = rowStatus.feasibility.meet(result.status);

			if (result.status == Routability.CONDITIONAL) {
				PackCell sourceCell = (PackCell) cellPin.getNet()
						.getSourcePin().getCell();
				Map<PackCell, Set<Bel>> condMap =
						Collections.singletonMap(sourceCell,
								Collections.singleton(result.conditionalSource));
				mergeConditionalsInRow(rowStatus, condMap);
			}

			if (rowStatus.feasibility != Routability.INFEASIBLE) {
				CellNet net = cellPin.getNet();
				assert net != null;
				if (rowStatus.claimedSources.containsKey(result.claimedSource)) {
					if (!rowStatus.claimedSources.get(result.claimedSource).equals(net)) {
						rowStatus.feasibility = Routability.INFEASIBLE;
						break; // exit early
					}
				} else {
					rowStatus.claimedSources.put(result.claimedSource, net);
				}
			}

			if (rowStatus.feasibility == Routability.INFEASIBLE) {
				// exit early condition
				break;
			}
		}
	}

	private void mergeConditionalsInRow(
			RowStatus rowStatus, Map<PackCell, ? extends Collection<Bel>> toAdd
	) {
		for (Map.Entry<PackCell, ? extends Collection<Bel>> e : toAdd.entrySet()) {
			PackCell cell = e.getKey();
			Collection<Bel> bels = e.getValue();
			assert !bels.contains(null);

			// sinks can only have a single conditional source
			if (rowStatus.conditionals.containsKey(cell)) {
				Set<Bel> prevBels = rowStatus.conditionals.get(cell);
				prevBels.retainAll(bels);
				if (prevBels.isEmpty()) {
					rowStatus.feasibility = Routability.INFEASIBLE;
				}
			} else {
				rowStatus.conditionals.put(cell, new HashSet<>(bels));
			}
		}
	}

	private IsRowValidForSinkReturn isRowValidForSink(
			PinGroup pg, RoutingTable.Row tableRow,	CellPin cellPin, BelPin belPin
	) {
		IsRowValidForSinkReturn result = new IsRowValidForSinkReturn();

		Source source = netSources.get(cellPin.getNet());
		SinkPinEntry entry = tableRow.sinkPins.get(belPin);
		Routability status = Routability.FEASIBLE;

		if (entry.sourcePin != null) {
			BelPin entryPin = entry.sourcePin;
			result.claimedSource = entryPin;

			if (source.isPlaced) {
				if (!entryPin.equals(source.belPin))
					status = Routability.INFEASIBLE;
			} else if (source.vcc) {
				if (!template.getVccSources().contains(entryPin) ||
						isBelOccupied(entryPin.getBel())) {
					status = Routability.INFEASIBLE;
				}
			} else if (source.gnd) {
				if (!template.getGndSources().contains(entryPin) ||
						isBelOccupied(entryPin.getBel())) {
					status = Routability.INFEASIBLE;
				}
			} else { // TODO with macros, this needs to be changed to attempt to place the BEL
				Cell sourceCell = source.cellPin.getCell();
				Bel entrySourceBel = entry.sourcePin.getBel();
				if (cluster.isBelOccupied(entrySourceBel)) {
					status = Routability.INFEASIBLE;
				} else {
					List<BelId> possibleBels = sourceCell.getLibCell().getPossibleAnchors();
					if (possibleBels.contains(entrySourceBel.getId())) {
						List<BelPin> possiblePins = source.cellPin.getPossibleBelPins(
								entrySourceBel);
						if (possiblePins.contains(entry.sourcePin)) {
							status = Routability.CONDITIONAL;
							result.conditionalSource = entrySourceBel;
						} else {
							status = Routability.INFEASIBLE;
						}
					} else {
						status = Routability.INFEASIBLE;
					}
				}
			}
		} else if (entry.drivenByGeneralFabric) {
			result.claimedSource = entry.sourceClusterPin;
			if (source.isPlaced && pinIsInCluster(source.cellPin)) {
				assert source.belPin != null;
				if (pg.getSourceBelPins().contains(source.belPin)) {
					SourcePinEntry sourcePinEntry = tableRow.sourcePins.get(source.belPin);
					if (!sourcePinEntry.drivesGeneralFabric)
						status = Routability.INFEASIBLE;
				} else {
					// That the source drives general fabric will always
					// be true because it had to reach this pin.
					if (!source.drivesGeneralFabric)
						status = Routability.INFEASIBLE;
				}
			} else {
				if (!source.drivesGeneralFabric)
					status = Routability.INFEASIBLE;
			}
		} else if (entry.sourceClusterPin != null) {
			result.claimedSource = entry.sourceClusterPin;
			if (!source.sourceWires.contains(entry.sourceClusterPin))
				status = Routability.INFEASIBLE;
		} else {
			throw new AssertionError("No source specified");
		}

		result.status = status;
		return result;
	}

	private boolean isBelOccupied(Bel bel) {
		boolean belOccupied = cluster.isBelOccupied(bel);
		if (belOccupied)
			return true;

		String belName = bel.getName();
		if (belName.endsWith("5LUT")) {
			String lut6Name = belName.charAt(0) + "6LUT";
			Bel lut6 = bel.getSite().getBel(lut6Name);
			PackCell cellAtLut6 = cluster.getCellAtBel(lut6);
			if (cellAtLut6 == null)
				return false;
			if (cellAtLut6.getLibCell().getNumLutInputs() == 6)
				return true;
			if (!cellAtLut6.getLibCell().getName().startsWith("LUT"))
				return true;
			return false;
		} else if (belName.endsWith("6LUT")) {
			String lut5Name = belName.charAt(0) + "5LUT";
			Bel lut5 = bel.getSite().getBel(lut5Name);
			PackCell cellAtLut5 = cluster.getCellAtBel(lut5);
			if (cellAtLut5 == null)
				return false;

			if (!cellAtLut5.getLibCell().getName().startsWith("LUT"))
				return true;
			return false;
		} else {
			return false;
		}
	}

	private boolean pinIsInCluster(CellPin pin) {
		PackCell cell = (PackCell) pin.getCell();
		return cell.getCluster() == cluster;
	}

	private static class IsRowValidForSinkReturn {
		Routability status;
		Object claimedSource;
		Bel conditionalSource;
	}

	private static class IsRowValidForSourceReturn {
		Routability status;
		Map<PackCell, List<Bel>> conditionalSinks = new HashMap<>();
	}

	private IsRowValidForSourceReturn isRowValidForSource(
			RoutingTable.Row tableRow, CellPin cellPin,	BelPin belPin
	) {
		IsRowValidForSourceReturn result = new IsRowValidForSourceReturn();
		result.status = Routability.FEASIBLE;
		SourcePinEntry entry = tableRow.sourcePins.get(belPin);
		CellNet net = cellPin.getNet();
		Sinks sinks = netSinks.get(net);

		Set<CellPin> conditionals = new HashSet<>();

		if (sinks.mustLeave) {
			if (!entry.drivesGeneralFabric) {
				result.status = Routability.INFEASIBLE;
				return result;
			}
		} else if (!sinks.conditionalMustLeaves.isEmpty()) {
			if (!entry.drivesGeneralFabric) {
				conditionals.addAll(sinks.conditionalMustLeaves);
			}
		}

		conditionals.addAll(sinks.conditionals);

		// Check if the sink in the cluster is outside the pin group
		// and requires exiting and re-entering to reach.
		for (CellPin sinkInCluster : sinks.sinkPinsInCluster) {
			BelPin sinkBelPin = cell2BelPinMap.get(sinkInCluster);
			if (!tableRow.sinkPins.containsKey(sinkBelPin)) {
				if (!(sinkBelPin.drivenByGeneralFabric() && entry.drivesGeneralFabric)) {
					result.status = Routability.INFEASIBLE;
					return result;
				}
			}
		}

		for (Map.Entry<CellPin, Set<Wire>> cce : sinks.requiredCarryChains.entrySet()) {
			if (Collections.disjoint(entry.drivenClusterPins, cce.getValue())) {
				PackCell cell = (PackCell) cce.getKey().getCell();
				if (cell.getCluster() == null) {
					conditionals.add(cce.getKey());
				} else {
					result.status = Routability.INFEASIBLE;
				}

				if (result.status == Routability.INFEASIBLE)
					return result;
			}
		}

		if (!entry.drivesGeneralFabric) {
			for (Map.Entry<CellPin, Set<Wire>> cce : sinks.optionalCarryChains.entrySet()) {
				if (Collections.disjoint(entry.drivenClusterPins, cce.getValue())) {
					PackCell cell = (PackCell) cce.getKey().getCell();
					if (cell.getCluster() == null) {
						conditionals.add(cce.getKey());
					} else {
						result.status = Routability.INFEASIBLE;
					}

					if (result.status == Routability.INFEASIBLE)
						return result;
				}
			}
		}

		if (!conditionals.isEmpty())
			checkConditionalSinks(result, entry, conditionals);
		return result;
	}

	private void checkConditionalSinks(
			IsRowValidForSourceReturn result, SourcePinEntry entry, Set<CellPin> pins
	) {
		// quick check to see if I should even bother checking
		if (entry.drivenSinks.size() < pins.size()) {
			result.status = Routability.INFEASIBLE;
		}

		result.status = Routability.CONDITIONAL;
		List<BelPin> connectedSinks = entry.drivenSinks;
		for (CellPin sinkPin : pins) {
			List<Bel> conditionalBels = getConditionalSinks(connectedSinks, sinkPin);

			if (conditionalBels.isEmpty()) {
				result.status = Routability.INFEASIBLE;
				break;
			}
			result.conditionalSinks.put((PackCell) sinkPin.getCell(), conditionalBels);
		}
	}

	private List<Bel> getConditionalSinks(List<BelPin> connectedSinks, CellPin sinkPin) {
		// TODO Need to change this to correctly handle macros
		Set<BelId> anchors = new HashSet<>(
				sinkPin.getCell().getPossibleAnchors());
		Set<Bel> conditionals = new HashSet<>();
		for (BelPin belPin : connectedSinks) {
			Bel bel = belPin.getBel();
			if (isPossibleConditionalBel(sinkPin, anchors, belPin, bel))
				conditionals.add(bel);
		}

		return new ArrayList<>(conditionals);
	}

	private boolean isPossibleConditionalBel(CellPin sinkPin, Set<BelId> anchors, BelPin belPin, Bel bel) {
		return !cluster.isBelOccupied(bel) &&
				anchors.contains(bel.getId()) &&
				sinkPin.getPossibleBelPins(bel).contains(belPin);
	}

	private Map<PackCell, Set<Bel>> joinGroupConditionals() {
		Map<PackCell, Set<Bel>> conditionals = new HashMap<>();
		for (PinGroupStatus pgStatus : pinGroupsStatuses.values()) {
			if (pgStatus.feasibility == Routability.FEASIBLE)
				continue;

			if (pgStatus.conditionals == null)
				buildGroupConditionals(pgStatus);

			for (Map.Entry<PackCell, Set<Bel>> e : pgStatus.conditionals.entrySet()) {
				conditionals.computeIfAbsent(e.getKey(), k -> new HashSet<>())
						.addAll(e.getValue());
			}
		}
		return conditionals;
	}

	private void buildGroupConditionals(PinGroupStatus pgStatus) {
		Map<PackCell, Set<Bel>> conditionals = new HashMap<>();
		for (RowStatus rowStatus : pgStatus.rowStatuses) {
			if (rowStatus.feasibility == Routability.INFEASIBLE)
				continue;
			assert rowStatus.feasibility == Routability.CONDITIONAL;
			assert rowStatus.conditionals != null;
			assert !rowStatus.conditionals.isEmpty();

			for (Map.Entry<PackCell, Set<Bel>> e : rowStatus.conditionals.entrySet()) {
				conditionals.computeIfAbsent(e.getKey(), k -> new HashSet<>())
						.addAll(e.getValue());
			}
		}
		pgStatus.conditionals = conditionals;
	}

	private static class Source {
		boolean isPlaced = false;
		boolean vcc = false;
		boolean gnd = false;
		boolean drivesGeneralFabric;
		BelPin belPin = null;
		List<Wire> sourceWires = new ArrayList<>();
		CellPin cellPin = null;

		public Source() { }

		public Source(Source other) {
			this.isPlaced = other.isPlaced;
			this.vcc = other.vcc;
			this.gnd = other.gnd;
			this.drivesGeneralFabric = other.drivesGeneralFabric;
			this.belPin = other.belPin;
			this.sourceWires = new ArrayList<>(other.sourceWires);
			this.cellPin = other.cellPin;
		}
	}

	private static class Sinks {
		List<CellPin> sinkPinsInCluster = new ArrayList<>();
		boolean mustLeave;
		Set<CellPin> conditionalMustLeaves = new HashSet<>();
		Set<CellPin> conditionals = new HashSet<>();
		Map<CellPin, Set<Wire>> requiredCarryChains = new HashMap<>();
		Map<CellPin, Set<Wire>> optionalCarryChains = new HashMap<>();

		public Sinks() { }

		public Sinks(Sinks other) {
			sinkPinsInCluster = new ArrayList<>(other.sinkPinsInCluster);
			mustLeave = other.mustLeave;
			conditionalMustLeaves = new HashSet<>(other.conditionalMustLeaves);
			requiredCarryChains = new HashMap<>(other.requiredCarryChains);
			optionalCarryChains = new HashMap<>(other.optionalCarryChains);
		}
	}

	@Override
	public void checkpoint() {
		bel2CellPinMap.checkPoint();
		cell2BelPinMap.checkPoint();
		netSources.checkPoint();
		netSinks.checkPoint();
		pinGroupsStatuses.checkPoint();
	}

	@Override
	public void rollback() {
		bel2CellPinMap.rollBack();
		cell2BelPinMap.rollBack();
		netSources.rollBack();
		netSinks.rollBack();
		pinGroupsStatuses.rollBack();
	}

	private static class RowStatus {
		Routability feasibility;
		Map<PackCell, Set<Bel>> conditionals;
		Map<Object, CellNet> claimedSources;

		public RowStatus() {
			feasibility = Routability.FEASIBLE;
			conditionals = new HashMap<>();
			claimedSources = new HashMap<>();
		}

		public RowStatus(Routability feasibility) {
			this.feasibility = feasibility;
		}
	}

	private static class PinGroupStatus {
		Routability feasibility;
		Map<PackCell, Set<Bel>> conditionals;
		ArrayList<RowStatus> rowStatuses;

		public PinGroupStatus() { }

		public PinGroupStatus(Routability feasibility) {
			this.feasibility = feasibility;
		}
	}
}
