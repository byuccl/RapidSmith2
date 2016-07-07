package edu.byu.ece.rapidSmith.cad.clusters.tileCluster;

import edu.byu.ece.rapidSmith.cad.placer.annealer.TypeSiteCoordinates;
import edu.byu.ece.rapidSmith.cad.placer.annealer.TypeSiteCoordinatesFactory;

/**
 *
 */
public class TileClusterCoordinatesFactory implements
		TypeSiteCoordinatesFactory<TileClusterType, TileClusterSite>
{
	private TileClusterFactory tcFactory;

	public TileClusterCoordinatesFactory(TileClusterFactory tcFactory) {
		this.tcFactory = tcFactory;
	}

	@Override
	public TypeSiteCoordinates<TileClusterType, TileClusterSite> make(TileClusterType t) {
		return new TileClusterCoordinates(tcFactory.getTemplate(t), tcFactory.getLocations(t));
	}
}
