package edu.byu.ece.rapidSmith.device;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 */
public final class SitePinTemplate implements Serializable {
	private static final long serialVersionUID = 4547857938761572358L;
	private final String name;
	private final SiteType siteType;
	private PinDirection direction;
	private int internalWire;

	public SitePinTemplate(String name, SiteType siteType) {
		this.name = name;
		this.siteType = siteType;
	}

	public String getName() {
		return name;
	}

	public SiteType getSiteType() {
		return siteType;
	}

	public int getInternalWire() {
		return internalWire;
	}

	public void setInternalWire(int internalWire) {
		this.internalWire = internalWire;
	}

	public PinDirection getDirection() {
		return direction;
	}

	public void setDirection(PinDirection direction) {
		this.direction = direction;
	}

	public boolean isInput() {
		return direction == PinDirection.IN || direction == PinDirection.INOUT;
	}

	public boolean isOutput() {
		return direction == PinDirection.OUT || direction == PinDirection.INOUT;
	}

	@Override
	public String toString() {
		return "SitePinTemplate{" +
				"name='" + name + '\'' +
				", siteType=" + siteType +
				'}';
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, siteType, direction, internalWire);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SitePinTemplate other = (SitePinTemplate) obj;
		return Objects.equals(this.name, other.name) &&
				Objects.equals(this.siteType, other.siteType) &&
				Objects.equals(this.direction, other.direction) &&
				Objects.equals(this.internalWire, other.internalWire);
	}
}
