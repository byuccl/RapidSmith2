package edu.byu.ece.rapidSmith.device;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 */
public final class SitePinTemplate implements Serializable {
	private static final long serialVersionUID = 4547857938761572358L;
	private final String name;
	private final SiteType primitiveType;
	private PinDirection direction;
	private int internalWire;

	public SitePinTemplate(String name, SiteType primitiveType) {
		this.name = name;
		this.primitiveType = primitiveType;
	}

	public String getName() {
		return name;
	}

	public SiteType getPrimitiveType() {
		return primitiveType;
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
				", primitiveType=" + primitiveType +
				'}';
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, primitiveType, direction, internalWire);
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
				Objects.equals(this.primitiveType, other.primitiveType) &&
				Objects.equals(this.direction, other.direction) &&
				Objects.equals(this.internalWire, other.internalWire);
	}
}
