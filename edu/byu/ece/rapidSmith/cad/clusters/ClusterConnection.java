package edu.byu.ece.rapidSmith.cad.clusters;

import edu.byu.ece.rapidSmith.device.BelPin;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Information about the connecting pin.
 *
 * Created by Haroldsen on 4/12/2015.
 */
public class ClusterConnection implements Comparable<ClusterConnection>, Serializable {
	private BelPin pin;
	private boolean withinSite;
	private int distance;

	public ClusterConnection(BelPin pin, boolean withinSite, int distance) {
		this.pin = pin;
		this.withinSite = withinSite;
		this.distance = distance;
	}

	public BelPin getPin() {
		return pin;
	}

	public boolean isWithinSite() {
		return withinSite;
	}

	public int getDistance() {
		return distance;
	}

	@Override
	public int compareTo(ClusterConnection o) {
		return Comparator.comparing(ClusterConnection::isWithinSite)
				.thenComparingInt(ClusterConnection::getDistance)
				.compare(this, o);
	}
}
