package edu.byu.ece.rapidSmith.device;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 */
public final class SitePinTemplate implements Serializable {
	private String name;
	private PrimitiveType primitiveType;
	private PinDirection direction;
	private int internalWire;
	private boolean drivesGeneralFabric;
	private boolean drivenByGeneralFabric;

	public SitePinTemplate(String name, PrimitiveType primitiveType) {
		this.name = name;
		this.primitiveType = primitiveType;
	}

	public String getName() {
		return name;
	}

	public PrimitiveType getPrimitiveType() {
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

	public boolean drivesGeneralFabric() {
		return drivesGeneralFabric;
	}

	public void setDrivesGeneralFabric(boolean drivesGeneralFabric) {
		this.drivesGeneralFabric = drivesGeneralFabric;
	}

	public boolean isDrivenByGeneralFabric() {
		return drivenByGeneralFabric;
	}

	public void setDrivenByGeneralFabric(boolean drivenByGeneralFabric) {
		this.drivenByGeneralFabric = drivenByGeneralFabric;
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
