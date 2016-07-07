package edu.byu.ece.rapidSmith.cad.clusters;

import edu.byu.ece.rapidSmith.cad.clusters.router.ClusterRouter;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.Routability;
import edu.byu.ece.rapidSmith.design.ClusterDesign;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.util.luts.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public abstract class PackingCompletionUtils<T extends ClusterType, S extends ClusterSite> {
	private CellLibrary cellLibrary;
	private Set<String> rams = new HashSet<>();
	private Set<String> srls = new HashSet<>();
	private CellNet vccNet;
	private CellNet gndNet;

	public PackingCompletionUtils(CellLibrary cellLibrary) {
		this.cellLibrary = cellLibrary;
		srls.add("SRL16");
		srls.add("SRL32");
		rams.add("SPRAM32");
		rams.add("DPRAM32");
		rams.add("SPRAM64");
		rams.add("DPRAM64");
	}

	public void finishClusterConstruction(ClusterDesign<T, S> design, Device device) {
		vccNet = design.getNets().stream()
				.filter(n -> n.getType() == NetType.VCC)
				.findAny().orElseGet(() -> design.addNet(new CellNet("GLOBAL_LOGIC1", NetType.VCC)));
		gndNet = design.getNets().stream()
				.filter(n -> n.getType() == NetType.GND)
				.findAny().orElseGet(() -> design.addNet(new CellNet("GLOBAL_LOGIC0", NetType.GND)));

		unifyCarryChains(design);

		for (Cluster<?, ?> cluster : design.getClusters()) {
			cluster.clearRouting();
			convertLutsToGenericTypes(cluster);
			finalRoute(design, cluster, device);
		}
	}

	protected abstract void unifyCarryChains(ClusterDesign<T, S> design);

	private void convertLutsToGenericTypes(Cluster<?, ?> cluster) {
		List<PackCell> cells = new ArrayList<>(cluster.getCells());
		for (PackCell cell : cells) {
			Bel bel = cell.getLocationInCluster();
			if (bel.getName().matches("[A-D][5-6]LUT")) {
				convertLutTypeToGenericType(cluster, cell);
			}
		}
	}

	private PackCell convertLutTypeToGenericType(Cluster<?, ?> cluster, PackCell cell) {
		CellDesign design = cell.getDesign();
		Bel bel = cell.getLocationInCluster();
		PrimitiveType siteType = bel.getId().getPrimitiveType();
		LibraryCell type = cellLibrary.get(siteType + bel.getName().substring(1));
		PackCell newCell = cell.deepCopy(Collections.singletonMap("type", type));
		replaceCell(design, cluster, cell, newCell);

		assert type.getName().charAt(6) == '5' || type.getName().charAt(6) == '6';
		if (type.getName().charAt(6) == '6') {
			Integer oldNumInputs = cell.getLibCell().getNumLutInputs();
			if (oldNumInputs == null)
				oldNumInputs = 1;
			newCell.updateProperty("$NUM_INPUTS", PropertyType.USER, oldNumInputs);
		}

		if (cell.getLibCell().isVccSource()) {
			setStaticEquationTree(bel, newCell, true);
		} else if (cell.getLibCell().isGndSource()) {
			setStaticEquationTree(bel, newCell, false);
		} else if (cell.getLibCell().isLut()) {
			updateEquationTree(bel, newCell);
		}
		connectRequiredPins(newCell);
		return newCell;
	}

	private void replaceCell(
			CellDesign design, Cluster<?, ?> cluster,
			PackCell oldCell, PackCell newCell
	) {
		assert cluster.getPinMap().isEmpty();

		Map<CellPin, CellNet> netMap = new HashMap<>();
		Bel bel = oldCell.getLocationInCluster();

		int[] remap = {0, 0, 0, 0, 0, 0};
		boolean pinsHaveChanged = false;
		for (CellPin oldPin : oldCell.getPins()) {
			if (oldPin.isConnectedToNet()) {
				CellNet net = oldPin.getNet();
				List<BelPin> possibleBelPins = oldPin.getPossibleBelPins(bel);
				assert possibleBelPins.size() == 1;
				BelPin belPin = possibleBelPins.get(0);
				CellPin newPin = newCell.getPin(belPin.getName());
				assert newPin != null;
				netMap.put(newPin, net);

				String oldPinName = oldPin.getName();
				if (oldPinName.matches("A[1-6]")) {
					int oldIndex = oldPinName.charAt(1) - '0';
					int newIndex = belPin.getName().charAt(1) - '0';
					remap[oldIndex - 1] = newIndex;
					pinsHaveChanged |= oldIndex != newIndex;
				}
			}
		}

		if (pinsHaveChanged) {
			String type = newCell.getLibCell().getName();
			LutConfig cfg = (LutConfig) newCell.getPropertyValue(type);
			cfg.remapPins(remap);
		}

		cluster.removeCell(oldCell);
		oldCell.setCluster(null);
		oldCell.setLocationInCluster(null);
		design.removeCell(oldCell);

		design.addCell(newCell);
		cluster.addCell(bel, newCell);
		newCell.setCluster(cluster);
		newCell.setLocationInCluster(bel);
		netMap.forEach((k, v) -> v.connectToPin(k));
	}

	private void setStaticEquationTree(Bel bel, Cell newCell, boolean vcc) {
		LibraryCell type = newCell.getLibCell();
		Property p = newCell.removeProperty(type.getName());
		assert p != null;

		EquationTree tree;
		if (type.getName().contains("6")) {
			if (vcc) {
				tree = new BinaryOperation(
						OpType.OR, new LutInput(6), new LutInput(6, true));
			} else {
				tree = new BinaryOperation(
						OpType.AND, new LutInput(6), new LutInput(6, true));
			}
		} else {
			tree = vcc ? Constant.ONE : Constant.ZERO;
		}

		LutConfig config = new LutConfig();
		config.setOperatingMode("LUT");
		config.setContents(new LutContents(tree));
		config.setOutputPinName("O" + bel.getName().charAt(1));
		newCell.updateProperty(type.getName(), p.getType(), config);
	}

	private void updateEquationTree(Bel bel, Cell newCell) {
		LibraryCell type = newCell.getLibCell();
		Property p = newCell.removeProperty(type.getName());
		assert p != null;

		if (bel.getName().endsWith("5LUT")) {
			LutConfig config = (LutConfig) p.getValue();
			LutContents contents = config.getContents();
			InitString lut6String = contents.getCopyOfInitString();
			long value = lut6String.getValue() & 0x0FFFFFFFFL;
			InitString newValue = new InitString(value << 32 | value);
			config.setContents(new LutContents(newValue));
			config.setOutputPinName("O5");
			newCell.updateProperty(type.getName(), p.getType(), config);
		} else {
			// TODO maybe shift the pins up if the other LUT5 is unused
			LutConfig config = (LutConfig) p.getValue();
			LutContents contents = config.getContents();
			EquationTree lut6Tree = contents.getCopyOfEquation();
			EquationTree a6Equ = new BinaryOperation(OpType.OR, new LutInput(6), new LutInput(6, true));
			EquationTree top = new BinaryOperation(OpType.AND, a6Equ, lut6Tree);
			config.setContents(new LutContents(top));
			config.setOutputPinName("O6");
			newCell.updateProperty(type.getName(), p.getType(), config);
		}
	}

	private void connectRequiredPins(PackCell cell) {
		connectA6PinToVcc(cell, vccNet);
		connectAllRamCells(cell, vccNet, gndNet);
	}

	private void connectA6PinToVcc(Cell cell, CellNet vccNet) {
		LibraryCell libCell = cell.getLibCell();
		String libCellName = libCell.getName();
		assert libCellName.matches("SLICE[LM][56]LUT");
		if (libCell.getName().charAt(6) == '6') {
			CellPin a6Pin = cell.getPin("A6");
			if (!a6Pin.isConnectedToNet()) {
				vccNet.connectToPin(a6Pin);
			}
		}
	}

	private void connectAllRamCells(PackCell cell, CellNet vccNet, CellNet gndNet) {
		LibraryCell libCell = cell.getLibCell();
		String libCellName = libCell.getName();
		assert libCellName.matches("SLICE[LM][56]LUT");

		String ramMode = (String) cell.getPropertyValue("RAMMODE");
		if (libCellName.startsWith("SLICEM") && ramMode != null) {
			if (srls.contains(ramMode)) {
				CellPin a1Pin = cell.getPin("A1");
				assert !a1Pin.isConnectedToNet();
				if (!a1Pin.isConnectedToNet()) // remove if i can figure out what happened.
					vccNet.connectToPin(a1Pin);
			} else if (rams.contains(ramMode)) {
				for (char pinNum = '1'; pinNum <= '5'; pinNum++) {
					CellPin apin = cell.getPin("A" + pinNum);
					if (!apin.isConnectedToNet()) {
						gndNet.connectToPin(apin);
					}

					CellPin wapin = cell.getPin("WA" + pinNum);
					if (!wapin.isConnectedToNet()) {
						gndNet.connectToPin(wapin);
					}
				}
			}
		}
	}

	private void finalRoute(
			ClusterDesign<?, ?> design, Cluster<?, ?> cluster, Device device
	) {
		// Reached the end of clustering, verify it and choose
		// whether to commit it or roll it back
		ClusterRouter router = new ClusterRouter(cluster, device);
		Routability routability = router.routeCluster();
		if (routability != Routability.FEASIBLE)
			throw new AssertionError("Final route failed");

		Map<CellNet, List<RouteTree>> routeTreeMap = router.routeTreeMap;
		Map<CellPin, BelPin> belPinMap = router.belPinMap.values().stream()
				.flatMap(e -> e.entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		belPinMap.values().forEach(p -> { assert p != null; });
		removeTileWires(routeTreeMap.values());
		trimUnsunkStaticNetRoutes(routeTreeMap);
		addStaticSourceCells(design, cluster, routeTreeMap, belPinMap);
		belPinMap.values().forEach(p -> { assert p != null; });
		cluster.constructNets();

		for (Map.Entry<CellNet, List<RouteTree>> e : routeTreeMap.entrySet()) {
			e.getValue().forEach(v -> cluster.addNetRouteTree(e.getKey(), v));
		}
		belPinMap.forEach(cluster::setPinMapping);
		assert !clusterHasTileWires(cluster);
	}

	private void removeTileWires(Collection<List<RouteTree>> routeTrees) {
		for (List<RouteTree> sourceTrees : routeTrees) {
			List<RouteTree> newSourceTrees = new ArrayList<>();
			for (RouteTree sourceTree : sourceTrees) {
				List<RouteTree> treesToRemove = new ArrayList<>();
				for (RouteTree rt : sourceTree) {
					if (rt.getWire() instanceof TileWire)
						treesToRemove.add(rt);
				}
				if (!treesToRemove.contains(sourceTree))
					newSourceTrees.add(sourceTree);
				for (RouteTree rt : treesToRemove) {
					if (rt.isSourced())
						rt.getSourceTree().removeConnection(rt.getConnection());
					for (RouteTree sink : new ArrayList<>(rt.getSinkTrees())) {
						rt.removeConnection(sink.getConnection());
						if (!treesToRemove.contains(sink)) {
							newSourceTrees.add(sink);
						}
					}
				}
			}
			sourceTrees.clear();
			sourceTrees.addAll(newSourceTrees);
		}
	}

	private void trimUnsunkStaticNetRoutes(Map<CellNet, List<RouteTree>> routes) {
		routes.forEach((net, sourceTrees) -> {
			if (net.isStaticNet()) {
				Iterator<RouteTree> it = sourceTrees.iterator();
				while (it.hasNext()) {
					RouteTree sourceTree = it.next();
					if (!driveSink(sourceTree))
						it.remove();
				}
			}
		});
	}

	private boolean driveSink(RouteTree sourceTree) {
		for (RouteTree rt : sourceTree) {
			if (!rt.getWire().getTerminals().isEmpty())
				return true;
		}
		return false;
	}

	private boolean clusterHasTileWires(Cluster<?, ?> cluster) {
		for (CellNet net : cluster.getNets()) {
			List<RouteTree> sourceTrees = cluster.getRouteTrees(net);
			if (containsTileWire(sourceTrees))
				return true;
		}
		return false;
	}

	private boolean containsTileWire(List<RouteTree> sourceTrees) {
		for (RouteTree sourceTree : sourceTrees) {
			for (RouteTree rt : sourceTree) {
				if (rt.getWire() != null && rt.getWire() instanceof TileWire)
					return true;
			}
		}
		return false;
	}

	private void addStaticSourceCells(
			ClusterDesign<?, ?> design, Cluster<?, ?> cluster,
			Map<CellNet, List<RouteTree>> routeTreeMap,
			Map<CellPin, BelPin> belPinMap
	) {
		for (CellNet net : new ArrayList<>(routeTreeMap.keySet())) {
			NetType type = net.getType();
			if ((type == NetType.GND || type == NetType.VCC) && net.getSourcePin() == null) {
				addAndConnectStaticSource(design, cluster, net, type, routeTreeMap, belPinMap);
			}
		}
	}

	private void addAndConnectStaticSource(
			CellDesign design, Cluster<?, ?> cluster,
			CellNet net, NetType type,
			Map<CellNet, List<RouteTree>> routeTreeMap,
			Map<CellPin, BelPin> belPinMap
	) {
		Map<Wire, CellPin> sinkWire2CellPinMap =
				buildSinkWire2CellPinMap(belPinMap);

		List<RouteTree> vccTreesToAdd = new ArrayList<>();
		Iterator<RouteTree> it = routeTreeMap.get(net).iterator();
		while (it.hasNext()) {
			RouteTree sourceTree = it.next();
			Wire sourceWire = sourceTree.getWire();
			Collection<Connection> sources = sourceWire.getSources();
			if (!sources.isEmpty()) {
				assert sources.size() == 1;
				BelPin source = sources.iterator().next().getBelPin();
				CellPin sourcePin = getOrMakeSourceCell(design, cluster, type, source, belPinMap);
				CellNet sourceNet = getOrCreateSourceNet(
						design, sourcePin, type, source, routeTreeMap, belPinMap);
				for (CellPin sinkPin : getSinksInRouteTree(sinkWire2CellPinMap, sourceTree)) {
					net.disconnectFromPin(sinkPin);
					BelPin sink = belPinMap.remove(sinkPin);

					sourceNet.connectToPin(sinkPin);
					belPinMap.put(sinkPin, sink);
				}
				it.remove();
				routeTreeMap.get(sourceNet).add(sourceTree);

				// TODO only do this if LUT5 is used
				Bel bel = source.getBel();
				if (bel.getName().matches("[A-D][56]LUT")) {
					belPinMap.remove(sourcePin);
					PackCell newCell = convertLutTypeToGenericType(
							cluster, (PackCell) sourcePin.getCell());

					if (bel.getName().charAt(1) == '6') {
						CellPin newPin = newCell.getPin("O6");
						belPinMap.put(newPin, source);

						BelPin a6BelPin = bel.getBelPin("A6");
						assert a6BelPin != null;
						CellPin a6CellPin = newCell.getPin("A6");
						assert a6CellPin != null;
						belPinMap.put(a6CellPin, a6BelPin);
						SitePin sitePin = bel.getSite().getSitePin(bel.getName().charAt(0) + "6");
						RouteTree tree = directRoute(sitePin.getInternalWire(), a6BelPin.getWire());
						vccTreesToAdd.add(tree);
					} else {
						CellPin newPin = newCell.getPin("O5");
						belPinMap.put(newPin, source);
					}
				}
			}
		}

		routeTreeMap.computeIfAbsent(vccNet, k -> new ArrayList<>()).addAll(vccTreesToAdd);
		if (routeTreeMap.get(net).isEmpty()) {
			routeTreeMap.remove(net);
		}
	}

	private Map<Wire, CellPin> buildSinkWire2CellPinMap(Map<CellPin, BelPin> belPinMap) {
		Map<Wire, CellPin> sinkWire2CellPinMap = new HashMap<>();
		for (CellPin sinkCellPin : belPinMap.keySet()) {
			BelPin sinkBelPin = belPinMap.get(sinkCellPin);
			assert sinkBelPin != null;
			sinkWire2CellPinMap.put(sinkBelPin.getWire(), sinkCellPin);
		}
		return sinkWire2CellPinMap;
	}

	private CellPin getOrMakeSourceCell(
			CellDesign design, Cluster<?, ?> cluster,
			NetType type, BelPin source, Map<CellPin, BelPin> pinMap
	) {
		CellPin sourcePin;
		Bel bel = source.getBel();
		if (design.isBelUsed(bel)) {
			assert cluster.isBelOccupied(bel);
			Cell sourceCell = design.getCellAtBel(bel);
			validateSourceCellType(sourceCell, type);
			switch (type) {
				case GND: sourcePin = sourceCell.getPin("0"); break;
				case VCC: sourcePin = sourceCell.getPin("1"); break;
				default: throw new AssertionError("Illegal net type for this method");
			}
		} else {
			LibraryCell sourceType;
			String pinName;
			switch (type) {
				case VCC:
					sourceType = cellLibrary.getVccSource();
					pinName = "1";
					break;
				case GND:
					sourceType = cellLibrary.getGndSource();
					pinName = "0";
					break;
				default:
					throw new AssertionError("Invalid type for this code");
			}

			String name = "GLOBAL_LOGIC" + pinName + "\\:" + source.getName();
			name = design.getUniqueCellName(name);
			PackCell sourceCell = new PackCell(name, sourceType);
			sourceCell.updateProperty(sourceType.getName(), PropertyType.DESIGN, "");
			design.addCell(sourceCell);
			cluster.addCell(bel, sourceCell);
			sourceCell.setCluster(cluster);
			sourceCell.setLocationInCluster(bel);
			sourcePin = sourceCell.getPin(pinName);
			pinMap.put(sourcePin, source);
		}

		return sourcePin;
	}

	private CellNet getOrCreateSourceNet(
			CellDesign design, CellPin sourcePin, NetType type, BelPin source,
			Map<CellNet, List<RouteTree>> routeTreeMap,
			Map<CellPin, BelPin> belPinMap
	) {
		CellNet net = sourcePin.getNet();
		Bel bel = source.getBel();

		if (net != null) {
			if (net.getType() != type)
				throw new AssertionError("source is already connected " +
						"to incompatible net");
		} else if (type == NetType.GND) {
			net = createAndConnectStaticNet(design, sourcePin,
					"GLOBAL_LOGIC1_" + bel.getFullName(),
					NetType.VCC, routeTreeMap);
		} else {
			net = createAndConnectStaticNet(design, sourcePin,
					"GLOBAL_LOGIC0_" + bel.getFullName(),
					NetType.GND, routeTreeMap);
		}

		BelPin existingBelPin = belPinMap.get(sourcePin);

		if (existingBelPin != null && !source.equals(existingBelPin))
			throw new AssertionError(type + " pin connected to another source pin");
		else
			belPinMap.put(sourcePin, source);

		return net;
	}

	private CellNet createAndConnectStaticNet(
			CellDesign design, CellPin sourcePin, String name, NetType type,
			Map<CellNet, List<RouteTree>> routeTreeMap
	) {
		name = design.getUniqueNetName(name);
		CellNet net = new CellNet(name, type);
		design.addNet(net);
		net.connectToPin(sourcePin);
		routeTreeMap.put(net, new ArrayList<>());
		return net;
	}

	private void validateSourceCellType(Cell sourceCell, NetType type) {
		if ((type == NetType.GND && sourceCell.getLibCell().isGndSource()) ||
				(type == NetType.VCC && sourceCell.getLibCell().isVccSource()))
			throw new AssertionError("Source bel used by ");
	}

	private Collection<CellPin> getSinksInRouteTree(
			Map<Wire, CellPin> sinkWire2CellPinMap, RouteTree sourceTree
	) {
		List<CellPin> sinks = new ArrayList<>();
		for (RouteTree rt : sourceTree) {
			Wire wire = rt.getWire();
			assert wire != null;
			CellPin cellPin = sinkWire2CellPinMap.get(wire);
			if (cellPin != null)
				sinks.add(cellPin);
		}
		return sinks;
	}

	private RouteTree directRoute(Wire source, Wire sink) {
		RouteTree sourceTree = new RouteTree(source);
		Queue<RouteTree> q = new ArrayDeque<>();
		q.add(sourceTree);

		RouteTree terminal = null;
		while (!q.isEmpty()) {
			RouteTree rt = q.poll();
			Wire wire = rt.getWire();
			if (wire.equals(sink)) {
				terminal = rt;
				break;
			}
			for (Connection c : wire.getWireConnections()) {
				q.add(rt.addConnection(c));
			}
		}
		sourceTree.prune(Collections.singleton(terminal));
		return sourceTree;
	}
}
