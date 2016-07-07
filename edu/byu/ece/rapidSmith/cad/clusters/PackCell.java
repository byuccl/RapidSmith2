package edu.byu.ece.rapidSmith.cad.clusters;

import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.LibraryCell;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelId;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class PackCell extends Cell {
	private boolean valid;
	private boolean packable;
	private Cluster<?, ?> cluster;
	private Bel clusterLocation;
	private CarryChain carryChain;
	private CarryChainGroup ccGroup;
	private Integer ccIndex;

	private double initialGain;
	private Double gain;
	// private int timingGain;

	protected Set<CarryChainConnection> sinkCarryChainConnections;
	protected Set<CarryChainConnection> sourceCarryChainConnections;

	public PackCell(String name, LibraryCell libCell) {
		super(name, libCell);
	}

	public PackCell(Cell other) {
		super(other.getName(), other.getLibCell());
		setBonded(other.getBonded());
		updateProperties(other.getProperties());
	}

	public void setPackable(boolean packable) {
		this.packable = packable;
	}

	public boolean isPackable() {
		return packable;
	}

	public Double getGain() {
		return gain;
	}

	public void setGain(Double gain) {
		this.gain = gain;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public void setCluster(Cluster<?, ?> cluster) {
		if (carryChain != null) {
			if (getCluster() == null && cluster != null)
				carryChain.incrementNumPackedCells();
			else if (getCluster() != null && cluster == null) {
				carryChain.decrementNumPackedCells();
			}
		}
		this.cluster = cluster;
	}

	/**
	 * Returns the cluster this cell exists in.
	 */
	public final <T extends ClusterType, S extends ClusterSite> Cluster<T, S> getCluster() {
		@SuppressWarnings("unchecked")
		Cluster<T, S> cluster = (Cluster<T, S>) this.cluster;
		return cluster;
	}

	public Bel getLocationInCluster() {
		return clusterLocation;
	}

	public void setLocationInCluster(Bel bel) {
		clusterLocation = bel;
	}

	public List<Bel> getPossibleAnchors(ClusterTemplate<?> cluster) {
		List<BelId> belIds = getPossibleAnchors();
		return cluster.getBels().stream()
				.filter(b -> belIds.contains(b.getId()))
				.collect(Collectors.toList());
	}

	public int getNumExposedPins() {
		int numNetsLeavingMolecule = 0;
		for (CellPin pin : getPins()) {
			if (pin.isConnectedToNet()) {
				CellNet net = pin.getNet();
				boolean netLeavesMolecule = false;
				for (CellPin oPin : net.getPins()) {
					Cell otherCell = oPin.getCell();
					if (otherCell != this) {
						netLeavesMolecule = true;
						break;
					}
				}

				if (netLeavesMolecule)
					numNetsLeavingMolecule++;

			}
		}

		return numNetsLeavingMolecule;
	}

	public void addSinkCarryCell(CellPin sourcePin, CellPin sinkPin) {
		if (sinkCarryChainConnections == null)
			sinkCarryChainConnections = new HashSet<>(2);
		CarryChainConnection ccc = new CarryChainConnection(sourcePin, sinkPin);
		sinkCarryChainConnections.add(ccc);
	}

	public Set<CarryChainConnection> getSinkCarryChainConnections() {
		return sinkCarryChainConnections != null ? sinkCarryChainConnections : Collections.emptySet();
	}

	public void addSourceCarryCell(CellPin sinkPin, CellPin sourcePin) {
		if (sourceCarryChainConnections == null)
			sourceCarryChainConnections = new HashSet<>(2);
		CarryChainConnection ccc = new CarryChainConnection(sinkPin, sourcePin);
		sourceCarryChainConnections.add(ccc);
	}

	public Set<CarryChainConnection> getSourceCarryChainConnections() {
		return sourceCarryChainConnections != null ? sourceCarryChainConnections : Collections.emptySet();
	}

	public CarryChain getCarryChain() {
		return carryChain;
	}

	public void setCarryChain(CarryChain carryChain) {
		this.carryChain = carryChain;
	}

	public CarryChainGroup getCarryGroup() {
		return ccGroup;
	}

	public Integer getCarryIndex() {
		return ccIndex;
	}

	public void setCarryGroup(CarryChainGroup group) {
		this.ccGroup = group;
	}

	public void setCarryIndex(Integer index) {
		this.ccIndex = index;
	}

	public void setInitialGain(double initialGain) {
		this.initialGain = initialGain;
	}

	public double getInitialGain() {
		return initialGain;
	}

	@Override
	public PackCell deepCopy() {
		return deepCopy(Collections.emptyMap());
	}

	@Override
	public PackCell deepCopy(Map<String, Object> changes) {
		PackCell cellCopy = (PackCell) super.deepCopy(PackCell::new, changes);
		cellCopy.setValid(valid);
		cellCopy.setPackable(packable);
		return cellCopy;
	}
}
