package edu.byu.ece.rapidSmith.cad.clusters;

import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;

import java.util.*;

/**
 *
 */
public abstract class Cluster<T extends ClusterType, S extends ClusterSite> {
	private final String name;
	protected double cost;
	protected Bel anchor;
	protected ClusterChain<T, S> chain;
	protected Map<Bel, PackCell> placementMap = new HashMap<>();
	protected Map<PackCell, Bel> cellLocationMap = new HashMap<>();
	protected Map<CellPin, BelPin> pinMap = new HashMap<>();
	protected Map<CellNet, List<RouteTree>> internalNets = null;
	protected Map<CellNet, List<RouteTree>> externalNets = null;

	protected S placement;
	private CarryChainGroup carryGroup;
	private Integer carryIndex;

	public Cluster(String name) {
		Objects.requireNonNull(name);

		this.name = name;
	}

	// Field getters and setters
	public String getName() {
		return name;
	}

	public T getType() {
		return getTemplate().getType();
	}

	public abstract ClusterTemplate<T> getTemplate();

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public ClusterChain<T, S> getChain() {
		return chain;
	}

	public void setChain(ClusterChain<T, S> chain) {
		this.chain = chain;
	}

	// Cells in cluster methods
	public PackCell addCell(Bel bel, PackCell cell) {
		Objects.requireNonNull(bel);
		Objects.requireNonNull(cell);
		assert !hasCell(cell);
		assert !placementMap.containsKey(bel);

		placementMap.put(bel, cell);
		cellLocationMap.put(cell, bel);

		return cell;
	}

	public void removeCell(PackCell cell) {
		Bel bel = cellLocationMap.remove(cell);
		placementMap.remove(bel);
		cellLocationMap.remove(cell);

		cell.setLocationInCluster(null); // TODO move this outside of here since cell can have multiple locations
	}

	public Collection<PackCell> getCells() {
		return cellLocationMap.keySet();
	}

	public boolean hasCell(PackCell cell) {
		return cellLocationMap.containsKey(cell);
	}

	public boolean isBelOccupied(Bel bel) {
		assert bel != null;

		return placementMap != null && placementMap.containsKey(bel);
	}

	public boolean isFull() {
		return placementMap.size() == getTemplate().getNumBelsAvailable();
	}

	public Map<Bel, PackCell> getPlacementMap() {
		return placementMap;
	}

	public Bel getCellPlacement(PackCell cell) {
		assert cell != null;

		if (cellLocationMap == null)
			return null;
		return cellLocationMap.get(cell);
	}

	public PackCell getCellAtBel(Bel bel) {
		assert bel != null;

		if (placementMap == null)
			return null;
		return placementMap.get(bel);
	}

	// Nets in cluster methods
	public void constructNets() {
		internalNets = new HashMap<>();
		externalNets = new HashMap<>();

		Set<CellNet> nets = new HashSet<>();
		for (PackCell cell : getCells()) {
			for (CellPin pin : cell.getPins()) {
				if (!pin.isConnectedToNet())
					continue;
				nets.add(pin.getNet());
			}
		}

		for (CellNet net : nets) {
			boolean leavesCluster = false;
			for (CellPin oPin : net.getPins()) {
				PackCell oCell = (PackCell) oPin.getCell();
				if (!hasCell(oCell)) {
					leavesCluster = true;
					break;
				}
			}
			if (leavesCluster)
				externalNets.put(net, new ArrayList<>(1));
			else
				internalNets.put(net, new ArrayList<>(1));
		}
	}

	public Collection<CellNet> getNets() {
		List<CellNet> nets = new ArrayList<>();
		nets.addAll(getInternalNets());
		nets.addAll(getExternalNets());
		return nets;
	}

	public Collection<CellNet> getInternalNets() {
		assert internalNets != null;
		return internalNets.keySet();
	}

	public Collection<CellNet> getExternalNets() {
		assert externalNets != null;
		return externalNets.keySet();
	}

	public void clearRouting() {
		if (internalNets != null) internalNets.values().forEach(List::clear);
		if (externalNets != null) externalNets.values().forEach(List::clear);
	}

	public void addNetRouteTree(CellNet net, RouteTree routeTree) {
		assert net != null;
		assert routeTree != null;
		assert externalNets != null;
		assert internalNets != null;

		if (internalNets.containsKey(net)) {
			List<RouteTree> routeTrees = internalNets.get(net);
			if (routeTrees == null) {
				routeTrees = new ArrayList<>();
				internalNets.put(net, routeTrees);
			}
			routeTrees.add(routeTree);
		} else if (externalNets.containsKey(net)) {
			List<RouteTree> routeTrees = externalNets.get(net);
			if (routeTrees == null) {
				routeTrees = new ArrayList<>();
				externalNets.put(net, routeTrees);
			}
			routeTrees.add(routeTree);
		} else {
			throw new AssertionError("Cluster does not have net");
		}
	}

	public List<RouteTree> getRouteTrees(CellNet net) {
		assert net != null;
		assert externalNets != null;
		assert internalNets != null;

		if (internalNets.containsKey(net)) {
			return internalNets.get(net);
		} else if (externalNets.containsKey(net)) {
			return externalNets.get(net);
		} else {
			return null;
		}
	}

	public Map<CellNet, List<RouteTree>> getRouteTreeMap() {
		assert externalNets != null;
		assert internalNets != null;

		Map<CellNet, List<RouteTree>> routeTreeMap = new HashMap<>();
		routeTreeMap.putAll(internalNets);
		routeTreeMap.putAll(externalNets);
		return routeTreeMap;
	}

	// Pin mapping methods
	public void setPinMapping(CellPin cellPin, BelPin belPin) {
		assert cellPin != null;
		assert belPin != null;
		pinMap.put(cellPin, belPin);
	}

	public BelPin removePinMapping(CellPin cellPin) {
		return pinMap.remove(cellPin);
	}

	public BelPin getPinMapping(CellPin sourcePin) {
		return pinMap.get(sourcePin);
	}

	public Map<CellPin, BelPin> getPinMap() {
		return pinMap;
	}

	// placement methods
	public abstract boolean isPlaceable();

	public boolean isPlaced() {
		return placement != null;
	}

	public S getPlacement() {
		return placement;
	}

	public void place(S site) {
		assert site != null;
		placement = site;
	}

	public void unplace() {
		placement = null;
	}

	public void commitPlacement() {
		relocateTo(placement);
	}

	// Relocating methods

	protected abstract void relocateTo(S site);

	public void relocate(Bel newAnchor) {
		Objects.requireNonNull(newAnchor);

		assert anchor != null;
		assert placementMap != null;

		HashMap<Bel, PackCell> relocatedBelMap = new HashMap<>();
		for (Map.Entry<Bel, PackCell> e : placementMap.entrySet())
			relocateBel(newAnchor, relocatedBelMap, e);
		placementMap = relocatedBelMap;

		cellLocationMap = new HashMap<>();
		for (Map.Entry<Bel, PackCell> e : placementMap.entrySet())
			cellLocationMap.put(e.getValue(), e.getKey());

		Map<CellPin, BelPin> relocatePinMap = new HashMap<>();
		for (Map.Entry<CellPin, BelPin> e : pinMap.entrySet()) {
			relocatePinMap.put(e.getKey(), relocatePin(e.getValue(), newAnchor));
		}
		pinMap = relocatePinMap;

		Map<CellNet, List<RouteTree>> relocateTreeMap = new HashMap<>();
		for (Map.Entry<CellNet, List<RouteTree>> e : getRouteTreeMap().entrySet()) {
			List<RouteTree> list = new ArrayList<>(e.getValue().size());
			for (RouteTree rt : e.getValue()) {
				list.add(relocateRouteTree(rt, newAnchor));
			}
			relocateTreeMap.put(e.getKey(), list);
		}
		clearRouting();
		relocateTreeMap.forEach((cell, list) ->
				list.forEach( t -> addNetRouteTree(cell, t))
		);

		anchor = newAnchor;
	}

	private void relocateBel(Bel newAnchor, HashMap<Bel, PackCell> relocatedMap, Map.Entry<Bel, PackCell> e) {
		PackCell cell = e.getValue();
		Bel relocatedBel = getRelocatedBel(e.getKey(), newAnchor);
		relocatedBel.getSite().setType(e.getKey().getId().getPrimitiveType());
		relocatedMap.put(relocatedBel, cell);
	}

	private BelPin relocatePin(BelPin belPin, Bel newAnchor) {
		return getRelocatedBelPin(belPin, newAnchor);
	}

	private RouteTree relocateRouteTree(RouteTree template, Bel newAnchor) {
		Map<RouteTree, RouteTree> map = new HashMap<>();

		for (RouteTree rt : template) {
			if (rt.getSourceTree() == null) {
				Wire newWire = getRelocatedWire(rt.getWire(), newAnchor);
				map.put(rt, new RouteTree(newWire));
			} else {
				if (rt.getWire() instanceof TileWire)
					throw new AssertionError("Tile Wire found");

				RouteTree sourceTree = map.get(rt.getSourceTree());
				Connection newConn = getRelocatedConnection(
						sourceTree.getWire(), rt.getConnection(), newAnchor);
				map.put(rt, sourceTree.addConnection(newConn));
			}
		}

		return map.get(template);
	}

	protected abstract Bel getRelocatedBel(Bel bel, Bel newAnchor);

	protected abstract BelPin getRelocatedBelPin(BelPin belPin, Bel newAnchor);

	protected abstract Wire getRelocatedWire(Wire wire, Bel newAnchor);

	protected abstract Connection getRelocatedConnection(
			Wire sourceWire, Connection connection, Bel newAnchor);

	public void setRouteTreeMap(Map<CellNet, List<RouteTree>> newMap) {
		for (Map.Entry<CellNet, List<RouteTree>> e : internalNets.entrySet()) {
			List<RouteTree> newRouteTree = newMap.get(e.getKey());
			if (newRouteTree == null)
				newRouteTree = new ArrayList<>(0);
			e.setValue(newRouteTree);
		}

		for (Map.Entry<CellNet, List<RouteTree>> e : externalNets.entrySet()) {
			List<RouteTree> newRouteTree = newMap.get(e.getKey());
			if (newRouteTree == null)
				newRouteTree = new ArrayList<>(0);
			e.setValue(newRouteTree);
		}
	}

	public CarryChainGroup getCarryGroup() {
		return carryGroup;
	}

	public void setCarryGroup(CarryChainGroup carryGroup) {
		this.carryGroup = carryGroup;
	}

	public void setCarryIndex(Integer carryIndex) {
		this.carryIndex = carryIndex;
	}

	public Integer getCarryIndex() {
		return carryIndex;
	}
}
