package edu.byu.ece.rapidSmith.cad.clusters.tileCluster;

import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.cad.clusters.Cluster;

import java.util.*;

/**
 *
 */
public class TileCluster extends Cluster<TileClusterType, TileClusterSite>
		implements Comparable<TileCluster> {
	private TileClusterTemplate template;

	public TileCluster(String name, TileClusterTemplate tileTemplate) {
		super(name);
		this.template = tileTemplate;
		anchor = tileTemplate.getAnchor();
	}

	@Override
	protected void relocateTo(TileClusterSite site) {
		int siteIndex = anchor.getSite().getIndex();
		PrimitiveSite s = site.getTile().getPrimitiveSite(siteIndex);
		relocate(s.getBel(anchor.getId()));
	}

	@Override
	public TileClusterTemplate getTemplate() {
		return template;
	}

	@Override
	protected Bel getRelocatedBel(Bel bel, Bel newAnchor) {
		return template.getRelocatedBel(bel, anchor, newAnchor);
	}

	@Override
	protected BelPin getRelocatedBelPin(BelPin belPin, Bel newAnchor) {
		return template.getRelocatedBelPin(belPin, anchor, newAnchor);
	}

	@Override
	protected Wire getRelocatedWire(Wire wire, Bel newAnchor) {
		return template.getRelocatedWire(wire, anchor, newAnchor);
	}

	@Override
	protected Connection getRelocatedConnection(Wire sourceWire, Connection connection, Bel newAnchor) {
		return template.getRelocatedConnection(sourceWire, connection, anchor, newAnchor);
	}

	@Override
	public PackCell addCell(Bel bel, PackCell cell) {
		Objects.requireNonNull(bel);
		Objects.requireNonNull(cell);

		if (!getTemplate().getBels().contains(bel))
				throw new DesignAssemblyException("Bel is in a different tile from " +
						"this clusters template.");

		return super.addCell(bel, cell);
	}

	@Override
	public boolean isPlaceable() {
		return true;
	}

	@Override
	public int compareTo(TileCluster o) {
		return Double.compare(getCost(), o.getCost());
	}



/*
	private void validateActualLocation(Bel actualBel) {
		assert actualBel.getSite().getIndex() == getAnchorBel().getSite().getIndex();
		assert actualBel.getId().equals(getAnchorBel().getId());
	}
*/
}
