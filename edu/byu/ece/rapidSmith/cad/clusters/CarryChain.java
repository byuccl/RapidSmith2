package edu.byu.ece.rapidSmith.cad.clusters;

import edu.byu.ece.rapidSmith.design.subsite.CellPin;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class CarryChain {
	private Set<PackCell> cells = new HashSet<>();
	private int numPackedCells = 0;

	public static CarryChain connect(
			CellPin sourcePin, CellPin sinkPin
	) {
		CarryChain cc, o;
		PackCell source = (PackCell) sourcePin.getCell();
		PackCell sink = (PackCell) sinkPin.getCell();

		if (source.getCarryChain() != null) {
			cc = source.getCarryChain();
			o = sink.getCarryChain();
			cc.addCell(sink);
		} else if (sink.getCarryChain() != null) {
			cc = sink.getCarryChain();
			o = null;
			cc.addCell(source);
		} else {
			cc = new CarryChain();
			o = null;
			cc.addCell(source);
			cc.addCell(sink);
		}

		if (cc != o && o != null) {
			o.cells.forEach(cc::addCell);
		}

		source.addSinkCarryCell(sourcePin, sinkPin);
		sink.addSourceCarryCell(sinkPin, sourcePin);

		return cc;
	}

	private CarryChain() {}

	private void addCell(PackCell cell) {
		cells.add(cell);
		cell.setCarryChain(this);
	}

	public boolean isPartiallyPlaced() {
		return numPackedCells != 0;
	}

	public void incrementNumPackedCells() {
		numPackedCells++;
	}

	public void decrementNumPackedCells() {
		numPackedCells--;
	}

	public Set<PackCell> getCells() {
		return cells;
	}
}
