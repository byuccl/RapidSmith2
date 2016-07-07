package edu.byu.ece.rapidSmith.cad.clusters.router;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterTemplate;
import edu.byu.ece.rapidSmith.cad.clusters.DirectConnection;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.Routability;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.util.FamilyType;
import edu.byu.ece.rapidSmith.util.luts.LutConfig;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Routing -- adapted from now IncrementalClusterRouter
 * Created by Haroldsen on 4/8/2015.
 */
public class ClusterRouter {
	private static final int NUM_ROUTE_ITERATIONS = 4;
	private static Pattern lutPattern = Pattern.compile("([A-D])([56])LUT");
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
					.filter(p -> !isBelOccupied(p.getBel()))
					.collect(Collectors.toList());
			sourcePinMappings = Collections.singletonList(sourcePinMapping);
		}

		private boolean isBelOccupied(Bel bel) {
			boolean belOccupied = cluster.isBelOccupied(bel);
			if (belOccupied)
				return true;

			String belName = bel.getName();
			Matcher matcher = lutPattern.matcher(belName);
			if (matcher.matches()) {
				if (matcher.group(2).equals("5")) {
					String lut6Name = matcher.group(1) + "6LUT";
					Bel lut6 = bel.getSite().getBel(lut6Name);
					PackCell cellAtLut6 = cluster.getCellAtBel(lut6);
					if (cellAtLut6 == null)
						return false;
					int numInputs;
					if (cellAtLut6.getLibCell().getName().matches("SLICE[LM]6LUT")) {
						numInputs = (int) cellAtLut6.getPropertyValue("$NUM_INPUTS");
					} else {
						numInputs = cellAtLut6.getLibCell().getNumLutInputs();
					}
					if (numInputs == 6)
						return true;

					LutConfig cfg = (LutConfig) cellAtLut6.getPropertyValue(
							cellAtLut6.getLibCell().getName());
					return !cfg.getOperatingMode().equals("LUT");
				} else {
					assert matcher.group(2).equals("6");
					String lut5Name = matcher.group(1) + "5LUT";
					Bel lut5 = bel.getSite().getBel(lut5Name);
					PackCell cellAtLut5 = cluster.getCellAtBel(lut5);
					if (cellAtLut5 == null)
						return false;
					LutConfig cfg = (LutConfig) cellAtLut5.getPropertyValue(
							cellAtLut5.getLibCell().getName());
					return !cfg.getOperatingMode().equals("LUT");
				}
			} else {
				return false;
			}
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

			for (BelPinTemplate source : possibleSources) {
				PinMappings pinMapping = new PinMappings();
				Set<Wire> edgeWires = new HashSet<>();


				// add general interconnects
				if (source.drivesGeneralFabric()) {
					Set<Wire> sourceWires = getPossibleSourceInputs(sinksInCluster, template);
					if (!sourceWires.isEmpty()) {
						edgeWires.addAll(sourceWires);
					}
				}

				// add direct connection inputs
				for (DirectConnection dc : template.getDirectSourcesOfCluster()) {
					if ((endSiteIndex != null && endSiteIndex != dc.endSiteIndex))
						continue;

					if (dc.endPin.equals(source)) {
						edgeWires.add(dc.clusterExit);
					}
				}

				pinMapping.cellPin = sourcePin;
				pinMapping.edgeWires = new ArrayList<>(edgeWires);
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
			if (pinMappings.generallyDriven && !pinMappings.isCarryChainSink)
				mustRouteExternal = true;
			else
				mustRouteExternalSinks.put(sinkPin, pinMappings);
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
			pinMapping.isCarryChainSink = isCarryChainSink;

			return pinMapping;
		}
	}

	private enum RouteStatus {
		SUCCESS, IMPOSSIBLE, CONTENTION

	}

	private static class RouteToSinkReturn {
		RouteStatus status;
		RouteTree treeSink;
		Object terminal;
	}

	public ClusterRouter(Cluster<?, ?> cluster, Device device) {
		this.cluster = cluster;
		this.device = device;

		clusterOutputs = new PinMappings();
		clusterOutputs.cellPin = null;
		clusterOutputs.edgeWires = cluster.getTemplate().getOutputs();
	}

	public Routability routeCluster() {
		numNetsRouted = 0;
		numPinsRouted = 0;

		return routeCluster_impl();
	}

	private Routability routeCluster_impl() {
		Routability ret = null;
		StatusNetsPair routeStatus = null;

		sortPins(cluster);

		int i;
		for (i = 0; i < NUM_ROUTE_ITERATIONS; i++) {
			routeStatus = routeNets();
			if (routeStatus.status != RouteStatus.CONTENTION)
				break;
		}

		switch (routeStatus.status) {
			case SUCCESS:
				ret = Routability.FEASIBLE;
				break;
			case CONTENTION:
			case IMPOSSIBLE:
				ret = Routability.INFEASIBLE;
		}

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

	private StatusNetsPair routeNets() {
		StatusNetsPair status = new StatusNetsPair();
		status.status = RouteStatus.SUCCESS;
		for (CellNet net : sortedPins.keySet()) {
			assert sortedPins.get(net) != null;

			if (routeTreeMap.containsKey(net)) {
				List<RouteTree> rts = routeTreeMap.get(net);
				if (noContentionForRoute(rts))
					continue;
				unrouteNet(net);
			}

			if (!net.isStaticNet())
				status.contentionNets.add(net);
			numNetsRouted += 1;
			RouteStatus netRouteStatus = routeNet(net);
			// exit early if the route isn't feasible
			switch (netRouteStatus) {
				case IMPOSSIBLE:
					status.status = netRouteStatus;
					return status;
				case CONTENTION:
					status.status = RouteStatus.CONTENTION;
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

	private RouteStatus routeNet(CellNet net) {
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
			if (!net.isStaticNet()) {
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
				RouteToSinkReturn ret = routeToSink(sourceTrees, net, sinkTrees, sink);
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
				RouteToSinkReturn ret = routeToSink(sourceTrees, net, sinkTrees, clusterOutputs);
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
				RouteToSinkReturn ret = routeToSink(sourceTrees, net, sinkTrees, sink);
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
			List<RouteTree> sourceTrees, Set<RouteTree> sinkTrees, boolean removeSources
	) {
		Iterator<RouteTree> it = sourceTrees.iterator();
		while (it.hasNext()) {
			RouteTree rt = it.next();
			if (!rt.prune(sinkTrees) && removeSources) {
				it.remove();
			}
		}
	}

	private RouteToSinkReturn routeToSink(
			List<RouteTree> sourceTrees, CellNet net,
			Set<RouteTree> sinkTrees, PinMappings sinks
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
			if (!sinks.belPins.isEmpty()) {
				PackCell cell = (PackCell) sinks.cellPin.getCell();
				PrimitiveSite site = cell.getLocationInCluster().getSite();
				ClusterTemplate<?> template = cluster.getTemplate();

				Set<Wire> wiresToInvalidate = new HashSet<>();
				wiresToInvalidate.add(template.getWire(site, "intrasite:SLICEM/CDI1MUX.DI"));
				wiresToInvalidate.add(template.getWire(site, "intrasite:SLICEM/BDI1MUX.DI"));
				wiresToInvalidate.add(template.getWire(site, "intrasite:SLICEM/ADI1MUX.BDI1"));
				wiresToInvalidate.add(template.getWire(site, "intrasite:SLICEM/CDI1MUX.DMC31"));
				wiresToInvalidate.add(template.getWire(site, "intrasite:SLICEM/BDI1MUX.CMC31"));
				wiresToInvalidate.add(template.getWire(site, "intrasite:SLICEM/ADI1MUX.BMC31"));

				releaseDIWires(wiresToInvalidate, sinks.belPins, sinks.cellPin, net);

				for (Wire wireToInvalidate : wiresToInvalidate) {
					invalidateWire(wireToInvalidate);
				}
			}
		}

		// Determine the terminal wires for this route
		Set<Wire> terminals = new HashSet<>();
		for (BelPin belPin : sinks.belPins) {
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

	private void releaseDIWires(
			Set<Wire> toInvalidate, List<BelPin> sinkBelPins, CellPin sinkCellPin, CellNet net
	) {
		if (sinkBelPins.isEmpty())
			return;

		ClusterTemplate<?> template = cluster.getTemplate();
		BelPin sinkBelPin = sinkBelPins.get(0);
		CellPin sourcePin = net.getSourcePin();
		Bel sinkBel = sinkBelPin.getBel();
		PrimitiveSite site = sinkBel.getSite();
		if (sourcePin != null) {
			if (sinkBel.getName().matches("[A-D][56]LUT") && sinkBelPin.getName().equals("DI1")) {
				if (sourcePin.getName().equals("MC31")) {
					switch (sinkBel.getName()) {
						case "A6LUT":
						case "A5LUT":
							toInvalidate.remove(template.getWire(
									site, "intrasite:SLICEM/ADI1MUX.BMC31"));
							break;
						case "B6LUT":
						case "B5LUT":
							toInvalidate.remove(template.getWire(
									site, "intrasite:SLICEM/BDI1MUX.CMC31"));
							break;
						case "C6LUT":
						case "C5LUT":
							toInvalidate.remove(template.getWire(
									site, "intrasite:SLICEM/CDI1MUX.DMC31"));
							break;
					}
				}

				String rammode = (String) sinkCellPin.getCell().getPropertyValue("RAMMODE");
				if (rammode.contains("RAM")) {
					switch (sinkBel.getName()) {
						case "A6LUT":
						case "A5LUT":
							toInvalidate.remove(template.getWire(
									site, "intrasite:SLICEM/ADI1MUX.BDI1"));
							break;
						case "B6LUT":
						case "B5LUT":
							toInvalidate.remove(template.getWire(
									site, "intrasite:SLICEM/BDI1MUX.DI"));
							break;
						case "C6LUT":
						case "C5LUT":
							toInvalidate.remove(template.getWire(
									site, "intrasite:SLICEM/CDI1MUX.DI"));
							break;
					}
				}
			}
		}
	}

	private void invalidateWire(Wire wire) {
		assert wire != null;
		invalidatedWires.add(wire);
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
