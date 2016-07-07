package edu.byu.ece.rapidSmith.cad.clusters;

import edu.byu.ece.rapidSmith.design.subsite.Wire;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.BelPinTemplate;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 */
public class DirectConnection implements Serializable {
	public final BelPinTemplate endPin;
	public final int endSiteIndex;
	public final BelPin clusterPin;
	public final Wire clusterExit;
	// A property of the sink tile.  All sinks in the same tile have the same index.
	public Integer endTileIndex;

	public DirectConnection(
			BelPin endPin, BelPin clusterPin, Wire clusterExit, Integer endTileIndex
	) {
		this.endPin = endPin.getTemplate();
		this.endSiteIndex = endPin.getBel().getSite().getIndex();
		this.clusterPin = clusterPin;
		this.clusterExit = clusterExit;
		this.endTileIndex = endTileIndex;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DirectConnection that = (DirectConnection) o;
		return Objects.equals(endSiteIndex, that.endSiteIndex) &&
				Objects.equals(endTileIndex, that.endTileIndex) &&
				Objects.equals(endPin, that.endPin) &&
				Objects.equals(clusterPin, that.clusterPin);
	}

	@Override
	public int hashCode() {
		return Objects.hash(endPin, endSiteIndex, clusterPin, endTileIndex);
	}
}
