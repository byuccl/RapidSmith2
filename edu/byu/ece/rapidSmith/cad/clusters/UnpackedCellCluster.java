package edu.byu.ece.rapidSmith.cad.clusters;

import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.Connection;
import edu.byu.ece.rapidSmith.design.subsite.Wire;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;

import java.util.Collection;
import java.util.Collections;

/**
 *
 */
public class UnpackedCellCluster<T extends ClusterType, S extends ClusterSite> extends Cluster<T, S> {
	private PackCell cell;

	public UnpackedCellCluster(PackCell cell) {
		super(cell.getName());
		this.cell = cell;
		constructNets();
	}

	@Override
	public Collection<PackCell> getCells() {
		return Collections.singleton(cell);
	}

	@Override
	public boolean hasCell(PackCell cell) {
		return this.cell.equals(cell);
	}

	public Cell getCell() {
		return cell;
	}

	@Override
	public T getType() {
		return null;
	}

	@Override
	protected void relocateTo(S site) {
		// TODO huh?
	}

	@Override
	public ClusterTemplate<T> getTemplate() {
		return null;
	}

	@Override
	protected Bel getRelocatedBel(Bel bel, Bel newAnchor) {
		return null;
	}

	@Override
	protected BelPin getRelocatedBelPin(BelPin belPin, Bel newAnchor) {
		return null;
	}

	@Override
	protected Wire getRelocatedWire(Wire wire, Bel newAnchor) {
		return null;
	}

	@Override
	protected Connection getRelocatedConnection(Wire sourceWire, Connection c, Bel newAnchor) {
		return null;
	}

	@Override
	public boolean isPlaceable() {
		return false;
	}
}
