package edu.byu.ece.rapidSmith.device;

import java.io.Serializable;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 *  Template that backs BelPin objects.  A template exists for every
 *  (BEL id, pin) pair.
 *
 *  @see edu.byu.ece.rapidSmith.device.BelPin
 */
public final class BelPinTemplate implements Serializable {
	private String name;
	// BEL id for the pins this template backs
	private BelId id;
	private PinDirection direction;
	// Names of the site pins that drive or are driven by the BEL pins
	private Set<String> sitePins;
	// Wire the BEL pin connects to
	private int wire;

	public BelPinTemplate(BelId id, String name) {
		this.name = name;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BelId getId() {
		return id;
	}

	public void setId(BelId id) {
		this.id = id;
	}

	public PinDirection getDirection() {
		return direction;
	}

	public void setDirection(PinDirection direction) {
		this.direction = direction;
	}

	public Set<String> getSitePins() {
		if (sitePins == null)
			return Collections.emptySet();
		return sitePins;
	}

	public void addSitePin(String sitePin) {
		if (sitePins == null)
			sitePins = new TreeSet<>();
		sitePins.add(sitePin);
	}

	public int getWire() {
		return wire;
	}

	public void setWire(int wire) {
		this.wire = wire;
	}

	@Override
	public String toString() {
		return "BelPinTemplate{" + id.toString() + "." + name + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BelPinTemplate that = (BelPinTemplate) o;
		return Objects.equals(name, that.name) &&
				Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode() * 31 + name.hashCode();
	}
}
