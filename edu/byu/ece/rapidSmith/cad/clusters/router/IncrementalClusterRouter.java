package edu.byu.ece.rapidSmith.cad.clusters.router;

import edu.byu.ece.rapidSmith.cad.clusters.*;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.Routability;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.util.FamilyType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Routing
 * Created by Haroldsen on 3/25/2015.
 */
public class IncrementalClusterRouter {
	private static final int NUM_ROUTE_ITERATIONS = 4;
	public Map<PackCell, Set<Bel>> conditionals;
	private Device device;
	private Cluster<?, ?> cluster;
	private Map<Wire, OccupancyHistoryPair> wireUsage = new HashMap<>();
	public Map<CellNet, List<RouteTree>> routeTreeMap = new HashMap<>();
	private Map<CellNet, SortedNetPins> sortedPins;
	public Map<CellNet, Map<CellPin, BelPin>> belPinMap = new HashMap<>();
	private PinMappings clusterOutputs;
	private Set<Wire> invalidatedWires;

	private int numNetsRouted;
	private int numPinsRouted;

	private static final class OccupancyHistoryPair {
		int occupancy = 0;
		int history = 0;
	}

	private static class PinMappings {
		// True if this pin mapping is only valid for conditional routing.
		boolean conditional = false;
		// True if the pin is reachable from general fabric.  Only valid for sinks.
		boolean generallyDriven = false;
		// True if the pin is reachable from a carry chain.
		boolean isCarryChainSink = false;
		CellPin cellPin = null;
		// Terminals from leaving the cluster.  Includes carry chain terminals.
		List<Wire> edgeWires = Collections.emptyList();
		// Terminals from pins in the cluster.
		List<BelPin> belPins = Collections.emptyList();
	}

	private class SortedNetPins {
		public CellPin sourcePin = null;
		public List<PinMappings> sourcePinMappings;
		public BelPin sourceBelPin = null;
		// Unpacked pins
		public Map<CellPin, PinMappings> conditionalSinks = new HashMap<>();
		// Pins that have been packed that may not be general fabric.
		public Map<CellPin, PinMappings> mustRouteExternalSinks = new HashMap<>();
		public Map<CellPin, PinMappings> internalSinks = new HashMap<>();
		public boolean mustRouteExternal;

		private void setSourcePin(CellPin sourcePin) {
			this.sourcePin = sourcePin;
			PackCell sourceCell = (PackCell) sourcePin.getCell();
			Cluster<?, ?> sourceCluster = sourceCell.getCluster();

			Bel sourceBel = sourceCluster != null ?
					sourceCell.getLocationInCluster() : null;
			assert sourceCluster == cluster ? sourceBel != null : true;
			boolean sourceInCluster = sourceCluster == cluster;

			if (sourceInCluster) {
				PinMappings source = new PinMappings();
				source.cellPin = sourcePin;
				source.belPins = sourcePin.getPossibleBelPins(sourceBel);
				assert source.belPins.size() == 1;
				sourceBelPin = source.belPins.get(0);
				source.edgeWires = Collections.emptyList();
				sourcePinMappings = Collections.singletonList(source);
			} else {
				sourcePinMappings = getInputsForExternalWire(
						sourcePin, sourceBel, internalSinks.values());
			}
		}

		private void setSourceAsStatic(NetType staticSource) {
			PinMappings sourcePinMapping = getExternalInputs();

			Collection<BelPin> sourcePins;
			if (staticSource == NetType.VCC) {
				sourcePins = cluster.getTemplate().getVccSources();
			} else {
				assert staticSource == NetType.GND;
				sourcePins = cluster.getTemplate().getGndSources();
			}
			sourcePinMapping.belPins = sourcePins.stream()
					.filter(p -> !cluster.isBelOccupied(p.getBel()))
					.collect(Collectors.toList());
			sourcePinMappings = Collections.singletonList(sourcePinMapping);
		}

		private PinMappings getExternalInputs() {
			ClusterTemplate<?> template = cluster.getTemplate();
			Set<Wire> sources = new HashSet<>();
			for (PinMappings sinks : internalSinks.values()) {
				for (BelPin sinkBelPin : sinks.belPins) {
					if (sinkBelPin.drivenByGeneralFabric()) {
						sources.addAll(template.getInputsOfSink(sinkBelPin));
					}
				}
			}

			PinMappings pinMapping = new PinMappings();
			pinMapping.edgeWires = new ArrayList<>(sources);

			return pinMapping;
		}

		private List<PinMappings> getInputsForExternalWire(
				CellPin sourcePin, Bel sourceBel, Collection<PinMappings> sinksInCluster
		) {
			ClusterTemplate<?> template = cluster.getTemplate();

			// Possible sources contains the BelPinTemplates of all BelPins that can
			// potentially be the source of this net.
			List<BelPinTemplate> possibleSources = new ArrayList<>();
			// The site index of the pin is the source is placed, else null
			Integer endSiteIndex = getPossibleBelPinsForExternalWire(
					sourcePin, sourceBel, possibleSources);
			List<PinMappings> sourcePinMappings = new ArrayList<>(4);

			boolean drivesGeneralFabric =
					possibleSources.stream().anyMatch(BelPinTemplate::drivesGeneralFabric);

			Map<Wire, Boolean> directEntrances = new HashMap<>();
			for (DirectConnection dc : template.getDirectSourcesOfCluster()) {
				if ((endSiteIndex != null && endSiteIndex != dc.endSiteIndex))
					continue;
				if (possibleSources.contains(dc.endPin)) {
					boolean general = dc.clusterPin.drivesGeneralFabric();

					directEntrances.compute(dc.clusterExit,
							(k, v) -> v == null ? general : v || general);
				}
			}

			if (drivesGeneralFabric) {
				PinMappings pinMapping = new PinMappings();
				Set<Wire> sourceWires = getPossibleSourceInputs(sinksInCluster, template);
				if (!sourceWires.isEmpty()) {
					pinMapping.edgeWires = new ArrayList<>(sourceWires);
					pinMapping.cellPin = sourcePin;
					sourcePinMappings.add(pinMapping);
				}
			}

			for (Map.Entry<Wire, Boolean> e : directEntrances.entrySet()) {
				PinMappings pinMapping = new PinMappings();
				Set<Wire> sourceWires;
				if (e.getValue()) {
					sourceWires = getPossibleSourceInputs(sinksInCluster, template);
				} else {
					sourceWires = new HashSet<>();
				}
				sourceWires.add(e.getKey());
				pinMapping.edgeWires = new ArrayList<>(sourceWires);
				pinMapping.cellPin = sourcePin;
				sourcePinMappings.add(pinMapping);
			}

			return sourcePinMappings;
		}

		private Set<Wire> getPossibleSourceInputs(
				Collection<PinMappings> sinksInCluster, ClusterTemplate<?> template
		) {
			Set<Wire> sourceWires = new HashSet<>();
			for (PinMappings sinkMapping : sinksInCluster) {
				for (BelPin sinkPin : sinkMapping.belPins) {
					sourceWires.addAll(template.getInputsOfSink(sinkPin));
				}
			}

			return sourceWires;
		}

		private Integer getPossibleBelPinsForExternalWire(
				CellPin cellPin, Bel bel, List<BelPinTemplate> possibleBelPins
		) {
			PackCell cell = (PackCell) cellPin.getCell();
			assert cell.getCluster() != cluster;

			Integer endIndex = null;
			if (bel != null) {
				// We know where the cell was placed so we know what the source is.
				List<String> pinNames = cellPin.getPossibleBelPinNames(bel.getId());
				endIndex = bel.getSite().getIndex();
				pinNames.forEach(n -> possibleBelPins.add(
						bel.getTemplate().getPinTemplate(n)));
			} else {
				// The cell has not been placed so we'll evaluate all possible
				// placements of the cell.  Unfortunately it does require exploring all
				// Site Templates.
				List<BelId> compatibleBels = cell.getLibCell().getPossibleAnchors();
				for (SiteTemplate siteTemplate : device.getSiteTemplates().values()) {
					for (BelTemplate belTemplate : siteTemplate.getBelTemplates().values()) {
						if (compatibleBels.contains(belTemplate.getId())) {
							List<String> pinNames = cellPin.getPossibleBelPinNames(
									belTemplate.getId());
							pinNames.forEach(n -> possibleBelPins.add(
									belTemplate.getPinTemplate(n)));
						}
					}
				}
			}
			return endIndex;
		}

		private void addInternalSink(CellPin sinkPin) {
			Bel sinkBel = cluster.getCellPlacement((PackCell) sinkPin.getCell());
			assert sinkBel != null;

			PinMappings pinMapping = new PinMappings();
			pinMapping.cellPin = sinkPin;
			pinMapping.belPins = sinkPin.getPossibleBelPins(sinkBel);
			internalSinks.put(sinkPin, pinMapping);
		}

		private void addExternalSink(CellPin sinkPin) {
			mustRouteExternal = false;

			PinMappings pinMappings = getOutputsForExternalWire(sinkPin);
			if (pinMappings.conditional) {
				conditionalSinks.put(sinkPin, pinMappings);
			} else {
				if (pinMappings.generallyDriven && !pinMappings.isCarryChainSink)
					mustRouteExternal = true;
				else
					mustRouteExternalSinks.put(sinkPin, pinMappings);
			}
		}

		private PinMappings getOutputsForExternalWire(CellPin sinkPin) {
			PackCell sinkCell = (PackCell) sinkPin.getCell();
			Cluster<?, ?> sinkCluster = sinkCell.getCluster();
			ClusterTemplate<?> template = cluster.getTemplate();
			Bel sinkBel = sinkCluster != null ? sinkCell.getLocationInCluster() : null;
			assert sinkCluster != cluster;

			// find all of the possible sink BelPin types
			List<BelPinTemplate> possibleSinks = new ArrayList<>();
			Integer endIndex = getPossibleBelPinsForExternalWire(
					sinkPin, sinkBel, possibleSinks);

			boolean generallyDriven = false;
			boolean isCarryChainSink = false;
			Set<Wire> sinks = new HashSet<>();

			for (BelPinTemplate belPin : possibleSinks) {
				generallyDriven |= belPin.isDrivenByGeneralFabric();
				for (DirectConnection dc : template.getDirectSinksOfCluster()) {
					if (dc.clusterPin.equals(sourceBelPin) && dc.endPin.equals(belPin)) {
						if (endIndex == null || endIndex == dc.endSiteIndex) {
							sinks.add(dc.clusterExit);
							isCarryChainSink = true;
						}
					}
				}
			}

			if (generallyDriven) {
				// Need the generally driven input wires
				sinks.addAll(template.getOutputs().stream()
						.collect(Collectors.toList()));
			}

			PinMappings pinMapping = new PinMappings();
			pinMapping.cellPin = sinkPin;
			pinMapping.generallyDriven = generallyDriven;
			pinMapping.edgeWires = new ArrayList<>(sinks);
			pinMapping.conditional = sinkBel == null;
			pinMapping.isCarryChainSink = isCarryChainSink;

			return pinMapping;
		}
	}

	private enum RouteStatus {
		SUCCESS, IMPOSSIBLE, CONTENTION, CONDITIONAL;

	}

	private static class RouteToSinkReturn {
		RouteStatus status;
		RouteTree treeSink;
		Object terminal;
	}

	public IncrementalClusterRouter(Cluster<?, ?> cluster, Device device) {
		this.cluster = cluster;
		this.device = device;

		clusterOutputs = new PinMappings();
		clusterOutputs.cellPin = null;
		clusterOutputs.edgeWires = cluster.getTemplate().getOutputs();
	}

	public Routability routeCluster() {
		numNetsRouted = 0;
		numPinsRouted = 0;
//		AAPack.logger.log(Level.FINEST, "ROUTE_START_TIME", System.nanoTime());
		Routability status = routeCluster_impl();
//		AAPack.logger.log(Level.FINEST, "ROUTE_STATUS", status);
//		AAPack.logger.log(Level.FINEST, "ROUTE_NUM_NETS", numNetsRouted);
//		AAPack.logger.log(Level.FINEST, "ROUTE_NUM_PINS", numPinsRouted);
//		AAPack.logger.log(Level.FINEST, "ROUTE_END_TIME", System.nanoTime());

		return status;
	}

	private Routability routeCluster_impl() {
		Routability ret = null;
		StatusNetsPair routeStatus = null;
		Set<CellNet> netsWithContention = new HashSet<>();
		Set<CellNet> conditionalNets = new HashSet<>();

		sortPins(cluster);

		int i, j;
		for (j = 0; j < 2; j++) {
			for (i = 0; i < NUM_ROUTE_ITERATIONS; i++) {
				if (j == 0) {
					routeStatus = routeNets(conditionalNets, false);
					if (i >= 2)
						netsWithContention.addAll(routeStatus.contentionNets);
					if (routeStatus.status != RouteStatus.CONTENTION)
						break;
				} else {
					routeStatus = routeNets(conditionalNets, true);
					if (routeStatus.status != RouteStatus.CONTENTION)
						break;
				}
			}

			if (j == 0) {
//				AAPack.logger.log(Level.FINEST, "FIRST_ROUTE_ITERATIONS", Integer.min(i + 1, NUM_ROUTE_ITERATIONS));

				conditionals = new HashMap<>();
				switch (routeStatus.status) {
					case SUCCESS:
						ret = Routability.FEASIBLE;
						break;
					case CONTENTION:
						buildConditionals(netsWithContention, false);
						if (conditionals.isEmpty()) {
							ret = Routability.INFEASIBLE;
							break;
						}
						break;
					case CONDITIONAL:
						boolean isPossible = buildConditionals(conditionalNets, true);
						if (!isPossible) {
							ret = Routability.INFEASIBLE;
						}
						break;
					case IMPOSSIBLE:
						ret = Routability.INFEASIBLE;
				}

				if (ret == Routability.FEASIBLE || ret == Routability.INFEASIBLE)
					break;
			} else {
//				AAPack.logger.log(Level.FINEST, "SECOND_ROUTE_ITERATIONS: ", Integer.min(i, NUM_ROUTE_ITERATIONS));

				switch (routeStatus.status) {
					case SUCCESS:
						ret = Routability.CONDITIONAL;
						break;
					case CONTENTION:
					case CONDITIONAL:
					case IMPOSSIBLE:
						ret = Routability.INFEASIBLE;
				}
			}
		}

		netsWithContention.forEach(this::invalidateNet);
		conditionalNets.forEach(this::invalidateNet);

		return ret;
	}

	private void sortPins(Cluster<?, ?> cluster) {
		sortedPins = new HashMap<>();

		// Adds a sortedNetPins object for each net and adds all internal sinks
		initSortedNetPins(cluster);

		// Sets the source pins and any external sinks
		sortPinsOnNets(cluster);
	}

	private void initSortedNetPins(Cluster<?, ?> cluster) {
		for (Cell cell : cluster.getCells()) {
			Bel bel = ((PackCell) cell).getLocationInCluster();
			if (bel == null)
				continue;
			for (CellPin pin : cell.getPins()) {
				CellNet net = pin.getNet();
				if (net == null)
					continue;

				SortedNetPins sortedNetPins = sortedPins.computeIfAbsent(net, k -> new SortedNetPins());
				if (pin != net.getSourcePin()) {
					sortedNetPins.addInternalSink(pin);
				}
			}
		}
	}

	private void sortPinsOnNets(Cluster<?, ?> cluster) {
		for (Map.Entry<CellNet, SortedNetPins> e : sortedPins.entrySet()) {
			CellNet net = e.getKey();
			SortedNetPins sortedNetPins = e.getValue();
			if (!net.isStaticNet()) {
				CellPin sourcePin = net.getSourcePin();
				assert sourcePin != null;
				sortedNetPins.setSourcePin(sourcePin);

				// if the source pin is in the cluster, add the remaining sinks pins.
				// if it's not in the cluster, I don't need to route the source to any
				// external sinks.
				PackCell sourceCell = (PackCell) sourcePin.getCell();
				if (sourceCell.getCluster() == cluster) {
					Bel sourceBel = sourceCell.getLocationInCluster();
					assert sourceBel != null;
					for (CellPin sinkPin : net.getPins()) {
						if (sinkPin == sourcePin)
							continue;
						PackCell sinkCell = (PackCell) sinkPin.getCell();
						Cluster sinkCluster = sinkCell.getCluster();
						if (sinkCluster != cluster) {
							sortedNetPins.addExternalSink(sinkPin);
						}
					}
				}
			} else {
				// Set the source as static.  Don't worry about pins external to the cluster
				// as GND and VCC are available outside the cluster.
				sortedNetPins.setSourceAsStatic(net.getType());
			}
		}
	}

	private boolean buildConditionals(Set<CellNet> conditionalNets, boolean conditionalMode) {
		for (CellNet net : conditionalNets) {
			PackCell sourceCell = (PackCell) net.getSourcePin().getCell();
			Cluster<?, ?> sourceCluster = sourceCell.getCluster();
			if (sourceCluster == null) {
				List<BelPin> possibleSourcePins = getPossibleSourcePins(net);
				if (!possibleSourcePins.isEmpty()) {
					for (PinMappings sourcePinMapping : sortedPins.get(net).sourcePinMappings) {
						sourcePinMapping.belPins = possibleSourcePins;
						sourcePinMapping.conditional = true;
						Set<Bel> sourceBels = possibleSourcePins.stream()
								.map(BelPin::getBel)
								.distinct()
								.collect(Collectors.toSet());
						if (!sourceBels.isEmpty()) {
							conditionals.computeIfAbsent(sourceCell, k -> new HashSet<>()).addAll(sourceBels);
						} else if (conditionalMode) {
							return false;
						}
					}
				} else if (conditionalMode) {
					return false;
				}
			} else {
				Bel sourceBel = sourceCell.getLocationInCluster();
				if (sourceBel == null) {
					if (conditionalMode)
						return false;
					else
						continue;
				}

				SortedNetPins sortedNetPins = sortedPins.get(net);
				assert sortedNetPins.sourcePinMappings.size() == 1;
				List<BelPin> sourceBelPins = sortedNetPins.sourcePinMappings.get(0).belPins;
				assert sourceBelPins.size() == 1;
				BelPin sourceBelPin = sourceBelPins.get(0);

				List<BelPin> possBelPins = getPossibleSinkBelpins(sourceBelPin);

				Map<PackCell, Set<Bel>> conditionalMap = new HashMap<>();
				// get the sinks of the net and determine which BELs they'll fit into
				for (Map.Entry<CellPin, PinMappings> e : sortedNetPins.conditionalSinks.entrySet()) {
					PackCell sinkCell = (PackCell) e.getKey().getCell();
					List<BelPin> sinkPins = new ArrayList<>();
					List<BelId> possiblePackings = sinkCell.getLibCell().getPossibleAnchors();
					for (BelPin belPin : possBelPins) {
						if (possiblePackings.contains(belPin.getBel().getId())) {
							sinkPins.add(belPin);
						}
					}

					if (!sinkPins.isEmpty()) {
						e.getValue().belPins = sinkPins;
						e.getValue().conditional = true;
						if (!conditionalMap.containsKey(sinkCell)) {
							Set<Bel> sinkBels = sinkPins.stream()
									.map(BelPin::getBel)
									.collect(Collectors.toSet());
							conditionalMap.put(sinkCell, sinkBels);
						} else {
							Set<Bel> bels = conditionalMap.get(sinkCell);
							bels.retainAll(sinkPins);
							if (bels.isEmpty() && conditionalMode) {
								return false;
							}
						}
					} else if (conditionalMode) {
						return false;
					}
				}

				for (Map.Entry<PackCell, Set<Bel>> e : conditionalMap.entrySet()) {
					conditionals.computeIfAbsent(e.getKey(), k -> new HashSet<>()).addAll(e.getValue());
				}
			}
		}
		return true;
	}

	private List<BelPin> getPossibleSinkBelpins(BelPin sourceBelPin) {
		return cluster.getTemplate().getSinksOfSource(sourceBelPin).stream()
							.filter(ClusterConnection::isWithinSite)
							.map(ClusterConnection::getPin)
							.filter(p -> !cluster.isBelOccupied(p.getBel()))
							.collect(Collectors.toList());
	}

	private List<BelPin> getPossibleSourcePins(CellNet net) {
		Set<BelPin> possibleSourcePins = null;
		SortedNetPins sortedNetPins = sortedPins.get(net);
		CellPin sourcePin = sortedNetPins.sourcePin;
		for (PinMappings sinkPins : sortedNetPins.internalSinks.values()) {
//			Bel sinkBel = cluster.getCellPlacement(sinkPin.getCell());
			Set<BelPin> sourcePins = new HashSet<>();
			for (BelPin sinkBelPin : sinkPins.belPins) {
				for (ClusterConnection cc : cluster.getTemplate().getSourcesOfSink(sinkBelPin)) {
					if (cc.isWithinSite() && !cluster.isBelOccupied(cc.getPin().getBel())) {
						BelPin sourceBelPin = cc.getPin();
						BelId sourceBelType = sourceBelPin.getBel().getId();
						List<String> allowedSourceBelPins = sourcePin.getPossibleBelPinNames(sourceBelType);
						if (allowedSourceBelPins.contains(sourceBelPin.getName()))
							sourcePins.add(sourceBelPin);
					}
				}
			}

			if (possibleSourcePins == null) {
				possibleSourcePins = new HashSet<>(sourcePins);
			} else {
				possibleSourcePins.addAll(sourcePins);
			}
		}
		assert possibleSourcePins != null;
		return new ArrayList<>(possibleSourcePins);
	}

	public void invalidateNet(CellNet net) {
		if (routeTreeMap.containsKey(net))
			unrouteNet(net);
	}

	private StatusNetsPair routeNets(
			Set<CellNet> conditionalNets, boolean conditional
	) {
		StatusNetsPair status = new StatusNetsPair();
		status.status = RouteStatus.SUCCESS;
		for (CellNet net : sortedPins.keySet()) {
			assert sortedPins.get(net) != null;

			// if in conditional nets, then we already know the status of this net
			if (conditionalNets.contains(net))
				continue;

			if (routeTreeMap.containsKey(net)) {
				List<RouteTree> rts = routeTreeMap.get(net);
				if (noContentionForRoute(rts))
					continue;
				unrouteNet(net);
			}

			if (!net.isStaticNet())
				status.contentionNets.add(net);
			numNetsRouted += 1;
			RouteStatus netRouteStatus = routeNet(net, conditional);
			// exit early if the route isn't feasible
			switch (netRouteStatus) {
				case IMPOSSIBLE:
					status.status = netRouteStatus;
					return status;
				case CONTENTION:
					status.status = RouteStatus.CONTENTION;
					break;
				case CONDITIONAL:
					conditionalNets.add(net);
					if (status.status != RouteStatus.CONTENTION)
						status.status = RouteStatus.CONDITIONAL;
					break;
				case SUCCESS:
					// do nothing, status equals its previous value
					break;
			}
		}

		return status;
	}

	private static class StatusNetsPair {
		RouteStatus status;
		List<CellNet> contentionNets = new ArrayList<>();
	}

	private boolean noContentionForRoute(List<RouteTree> sourceTrees) {
		for (RouteTree sourceTree : sourceTrees) {
			Stack<RouteTree> stack = new Stack<>();
			stack.push(sourceTree);

			while (!stack.isEmpty()) {
				RouteTree rt = stack.pop();
				int occupancy = wireUsage.get(rt.getWire()).occupancy;
				if (occupancy > 1)
					return false;
				rt.getSinkTrees().forEach(stack::push);
			}
		}
		return true;
	}

	private void unrouteNet(CellNet net) {
		List<RouteTree> sourceTrees = routeTreeMap.remove(net);
		for (RouteTree sourceTree : sourceTrees) {
			for (RouteTree rt : sourceTree) {
				OccupancyHistoryPair pair = wireUsage.get(rt.getWire());
				pair.occupancy -= 1;
				assert pair.occupancy >= 0;
			}
		}
		belPinMap.remove(net);
	}

	private RouteStatus routeNet(CellNet net, boolean conditional) {
		SortedNetPins sortedNetPins = sortedPins.get(net);
		int bestCost = Integer.MAX_VALUE;
		List<RouteTree> bestSourceTrees = null;
		for (PinMappings sources : sortedNetPins.sourcePinMappings) {
			Map<CellPin, BelPin> pinMap = belPinMap.computeIfAbsent(net, k -> new HashMap<>());

			List<RouteTree> sourceTrees = new ArrayList<>();
			for (Wire sourceWire : sources.edgeWires) {
				RouteTree rt = new RouteTree(sourceWire);
				wireUsage.computeIfAbsent(sourceWire, k -> new OccupancyHistoryPair());
				rt.setCost(calculateSourceCost(sourceWire));
				sourceTrees.add(rt);
			}
			for (BelPin sourcePin : sources.belPins) {
				SiteWire wire = sourcePin.getWire();
				RouteTree rt = new RouteTree(wire);
				wireUsage.computeIfAbsent(wire, k -> new OccupancyHistoryPair());
				rt.setCost(calculateSourceCost(wire));
				sourceTrees.add(rt);
			}
			if (!conditional && !net.isStaticNet()) {
				if (!sources.belPins.isEmpty()) {
					// Check that their is only one source.
					assert sources.belPins.size() == 1;
					assert sources.edgeWires.isEmpty();
					pinMap.put(net.getSourcePin(), sources.belPins.get(0));
				}
			}
			boolean foundExternalPath = false;

			RouteStatus status = RouteStatus.SUCCESS;
			Set<RouteTree> sinkTrees = new HashSet<>();
			for (PinMappings sink : sortedNetPins.internalSinks.values()) {
				RouteToSinkReturn ret = routeToSink(sourceTrees, sinkTrees, sink);
				status = ret.status;
				if (ret.status == RouteStatus.IMPOSSIBLE)
					break;
				assert ret.terminal instanceof BelPin;
				pinMap.put(sink.cellPin, (BelPin) ret.terminal);
				updateSourceTrees(ret.treeSink);
			}
			if (status == RouteStatus.IMPOSSIBLE)
				continue;

			if (sortedNetPins.mustRouteExternal) {
				RouteToSinkReturn ret = routeToSink(sourceTrees, sinkTrees, clusterOutputs);
				status = ret.status;
				if (ret.status != RouteStatus.IMPOSSIBLE) {
					assert ret.terminal instanceof Wire;
					foundExternalPath = true;
					updateSourceTrees(ret.treeSink);
				}
			}
			if (status == RouteStatus.IMPOSSIBLE)
				continue;

			for (PinMappings sink : sortedNetPins.mustRouteExternalSinks.values()) {
				// Check if we've already accomplished this route
				if (foundExternalPath && sink.generallyDriven)
					continue;
				RouteToSinkReturn ret = routeToSink(sourceTrees, sinkTrees, sink);
				status = ret.status;
				if (ret.status == RouteStatus.IMPOSSIBLE)
					break;
				if (ret.terminal instanceof TileWire) {
					if (sink.generallyDriven && clusterOutputs.edgeWires.contains(ret.terminal))
						foundExternalPath = true;
				} else {
					assert ret.terminal instanceof BelPin;
					pinMap.put(sink.cellPin, (BelPin) ret.terminal);
				}

				updateSourceTrees(ret.treeSink);
			}
			if (status == RouteStatus.IMPOSSIBLE)
				continue;

			for (PinMappings sink : sortedNetPins.conditionalSinks.values()) {
				if (foundExternalPath && sink.generallyDriven)
					continue;
				RouteToSinkReturn ret = routeToSink(sourceTrees, sinkTrees, sink);
				status = ret.status;
				if (ret.status == RouteStatus.IMPOSSIBLE)
					break;

				if (sink.generallyDriven && ret.terminal instanceof TileWire &&
						clusterOutputs.edgeWires.contains(ret.terminal))
					foundExternalPath = true;
				updateSourceTrees(ret.treeSink);
			}

			if (status == RouteStatus.IMPOSSIBLE)
				continue;

			pruneSourceTrees(sourceTrees, sinkTrees, true);

			if (!noContentionForRoute(sourceTrees)) {
				bestSourceTrees = sourceTrees;
				break;
			} else {
				int routeTreeCost = calculateRouteTreeCost(sourceTrees);
				if (routeTreeCost < bestCost) {
					bestSourceTrees = sourceTrees;
					bestCost = routeTreeCost;
				}
			}
		}

		if (bestSourceTrees == null)
			return RouteStatus.IMPOSSIBLE;
		commitRoute(net, bestSourceTrees);
		if (!noContentionForRoute(bestSourceTrees))
			return RouteStatus.CONTENTION;
		return RouteStatus.SUCCESS;
	}

	private int calculateRouteTreeCost(List<RouteTree> routeTrees) {
		int cost = 0;
		for (RouteTree source : routeTrees) {
			for (RouteTree rt : source) {
				cost += calculateWireCost(rt.getWire());
			}
		}
		return cost;
	}

	private void updateSourceTrees(RouteTree sinkTree) {
		RouteTree rt = sinkTree;
		while (rt != null) {
			rt.setCost(0);
			rt = rt.getSourceTree();
		}
	}

	private void pruneSourceTrees(
			List<RouteTree> sourceTrees, Set<RouteTree> sinkTrees, boolean removeSources) {
		Iterator<RouteTree> it = sourceTrees.iterator();
		while (it.hasNext()) {
			RouteTree rt = it.next();
			if (!rt.prune(sinkTrees) && removeSources) {
				it.remove();
			}
		}
	}

	private RouteToSinkReturn routeToSink(
			List<RouteTree> sourceTrees, Set<RouteTree> sinkTrees, PinMappings sinks
	) {
		PriorityQueue<RouteTree> pq = new PriorityQueue<>();
		Map<Wire, Integer> wireCosts = new HashMap<>();
		invalidatedWires = new HashSet<>();

		for (RouteTree sourceTree : sourceTrees) {
			for (RouteTree rt : sourceTree) {
				pq.add(rt);
				wireCosts.put(rt.getWire(), rt.getCost());
			}
		}

		if (device.getFamilyType() == FamilyType.VIRTEX6) {
			CellPin cellPin = sinks.cellPin;
			if (!sinks.belPins.isEmpty()) {
				PackCell cell = (PackCell) cellPin.getCell();
				if (isSrlDIPinOnSliceLut(cellPin)) {
					PrimitiveSite site = cell.getLocationInCluster().getSite();
					ClusterTemplate<?> template = cluster.getTemplate();
					invalidateWire(template.getWire(site, "intrasite:SLICEM/CDI1MUX.DI"));
					invalidateWire(template.getWire(site, "intrasite:SLICEM/CDI1MUX.DMC31"));
					invalidateWire(template.getWire(site, "intrasite:SLICEM/BDI1MUX.DI"));
					invalidateWire(template.getWire(site, "intrasite:SLICEM/BDI1MUX.CMC31"));
					invalidateWire(template.getWire(site, "intrasite:SLICEM/ADI1MUX.BDI1"));
					invalidateWire(template.getWire(site, "intrasite:SLICEM/ADI1MUX.BMC31"));
				}
			}
		}

		// Determine the terminal wires for this route
		Set<Wire> terminals = new HashSet<>();
		// Sinks within the cluster
		for (BelPin belPin : sinks.belPins) {
			boolean excludePin = false;
			// Disallow placing a LUT6 cell on the LUT6 BEL if the LUT5 BEL is occupied
			if (belPin.getBel().getName().contains("LUT") && belPin.getName().equals("A6")) {
				PrimitiveSite pinSite = belPin.getBel().getSite();
				char le = belPin.getBel().getName().charAt(0);
				Bel lut5 = pinSite.getBel(le + "5LUT");
				if (cluster.isBelOccupied(lut5))
					excludePin = true;
			}
			if (!excludePin)
				terminals.add(belPin.getWire());
		}
		// Any direct connection sinks
		for (Wire edgeWire : sinks.edgeWires) {
			terminals.add(edgeWire);
		}
		// If there are generally driven sinks, then all output wires should be added.
		if (sinks.generallyDriven) {
			terminals.addAll(clusterOutputs.edgeWires);
		}

		RouteToSinkReturn ret = new RouteToSinkReturn();
		ret.status = RouteStatus.IMPOSSIBLE;
		Set<Wire> processedWires = new HashSet<>();

		// Allows for rechecking for the output
		while (!pq.isEmpty()) {
			RouteTree lowestCost = pq.poll();
			Wire wire = lowestCost.getWire();
			if (!processedWires.add(wire))
				continue;

			if (terminals.contains(wire)) {
				ret.status = RouteStatus.SUCCESS;
				ret.treeSink = lowestCost;
				sinkTrees.add(lowestCost);

				if (wire instanceof SiteWire) {
					BelPin belPin = wire.getSite().getBelPinOfWire(wire.getWireEnum());
					assert belPin != null;
					ret.terminal = belPin;
				} else {
					ret.terminal = wire;
				}
				break;
			}

			Stream<Connection> conns = wire.getAllConnections()
					.filter(c -> !c.isTerminal());
			conns.forEach(c -> {
					// I don't think I care about route throughs.  Check the old cluster routing
					// if I do since I had some code to handle them there.
					Wire sinkWire = c.getSinkWire();

					int wireCost = lowestCost.getCost() +
							calculateWireCost(sinkWire);
					if (wireCost < wireCosts.getOrDefault(sinkWire, Integer.MAX_VALUE)) {
						RouteTree sink = lowestCost.addConnection(c);
						sink.setCost(wireCost);
						pq.add(sink);
					}
			});
		}

		pruneSourceTrees(sourceTrees, sinkTrees, false);
		invalidatedWires = null;
		numPinsRouted += 1;
		return ret;
	}

	private void invalidateWire(Wire wire) {
		assert wire != null;
		invalidatedWires.add(wire);
	}

	private boolean isSrlDIPinOnSliceLut(CellPin sinkPin) {
		PackCell cell = (PackCell) sinkPin.getCell();
		return cell.getLibCell().getName().equals("SRL16") &&
				sinkPin.getName().equals("DI") &&
				cell.getLocationInCluster().getName().matches("[A-D]5LUT");
	}

	private int calculateSourceCost(Wire wire) {
		return calculateWireCost(wire);
	}

	private int calculateWireCost(Wire wire) {
		if (invalidatedWires != null && invalidatedWires.contains(wire))
			return 10000;

		int cost = 1;
		OccupancyHistoryPair pair = wireUsage.computeIfAbsent(wire, k -> new OccupancyHistoryPair());
		cost += 4 * pair.occupancy;
		cost += 2 * pair.history;

		return cost;
	}

	private void commitRoute(CellNet net, List<RouteTree> sourceTrees) {
		for (RouteTree sourceTree : sourceTrees) {
			Stack<RouteTree> stack = new Stack<>();
			stack.push(sourceTree);

			while (!stack.isEmpty()) {
				RouteTree rt = stack.pop();
				OccupancyHistoryPair pair = wireUsage.get(rt.getWire());
				pair.occupancy += 1;
				// only increment the historical for shared wires
				if (pair.occupancy > 1)
					pair.history += 1;
				rt.getSinkTrees().forEach(stack::push);
			}
		}
		routeTreeMap.put(net, sourceTrees);
	}
}
