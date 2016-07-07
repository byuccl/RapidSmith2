package edu.byu.ece.rapidSmith.cad.clusters;

import edu.byu.ece.rapidSmith.design.subsite.CellPin;

import java.util.Objects;

/**
 *
 */
public class CarryChainConnection {
	private CellPin clusterPin;
	private PackCell endCell;
	private CellPin endPin;

	public CarryChainConnection(CellPin clusterPin, CellPin endPin) {
		this.clusterPin = clusterPin;
		this.endCell = (PackCell) endPin.getCell();
		this.endPin = endPin;
	}

	public CellPin getClusterPin() {
		return clusterPin;
	}

	public PackCell getEndCell() {
		return endCell;
	}

	public CellPin getEndPin() {
		return endPin;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CarryChainConnection that = (CarryChainConnection) o;
		return Objects.equals(clusterPin, that.clusterPin) &&
				Objects.equals(endCell, that.endCell);
	}

	@Override
	public int hashCode() {
		return Objects.hash(clusterPin, endCell);
	}

	@Override
	public String toString() {
		return "CarryChainConnection{" +
				"clusterPin=" + clusterPin +
				", endCell=" + endCell +
				'}';
	}
}
