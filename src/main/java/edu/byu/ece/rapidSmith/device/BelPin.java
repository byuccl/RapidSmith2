package edu.byu.ece.rapidSmith.device;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *  Class representing a pin on a BEL.
 *
 *  BelPins are created on demand to preserve memory.  BelPins are created via the
 *  {@link edu.byu.ece.rapidSmith.device.Bel#getBelPin(String)} method of the BEL
 *  the pin exists on.
 */
public final class BelPin implements Serializable {
	private static final long serialVersionUID = -402693921202343025L;
	// The BEL this pin exists on
	private final Bel bel;
	// The backing template for this BEL pin
	private final BelPinTemplate template;

	BelPin(Bel bel, BelPinTemplate template) {
		this.bel = bel;
		this.template = template;
	}

	/**
	 * Gets the BEL this pin exists on.
	 * @return the BEL this pin exists on
	 */
	public Bel getBel() {
		return bel;
	}

	/**
	 * Returns the template backing this BelPin.  Shouldn't be needed during
	 * normal use but provided as a luxury.
	 *
	 * @return the template backing this pin
	 */
	public BelPinTemplate getTemplate() {
		return template;
	}

	/**
	 * Returns the name of this pin.
	 *
	 * @return the name of this pin
	 */
	public String getName() {
		return template.getName();
	}

	/**
	 * Returns the site wire connecting to this pin.
	 *
	 * @return the site wire connecting to this pin
	 */
	public SiteWire getWire() {
		return new SiteWire(getBel().getSite(), template.getWire());
	}

	public PinDirection getDirection() {
		return template.getDirection();
	}

	/**
	 * Tests if the pin is acts as an output of the BEL this pin exists on.
	 *
	 * @return true if the pin's direction is out or inout
	 */
	public boolean isOutput() {
		PinDirection direction = getDirection();
		return direction == PinDirection.OUT || direction == PinDirection.INOUT;
	}

	/**
	 * Tests if the pin is acts as an input of the BEL this pin exists on.
	 *
	 * @return true if the pin's direction is in or inout
	 */
	public boolean isInput() {
		PinDirection direction = getDirection();
		return direction == PinDirection.IN || direction == PinDirection.INOUT;
	}

	/**
	 * Returns the site pins that either drive or are driven by this pin.
	 * <p>
	 * This method is especially helpful in determining which site pins a router
	 * should target to reach this BEL pin.  A set is returned since some BEL pins
	 * are reachable from multiple site pins.
	 * <p>
	 * The collection and the site pin objects are built upon each invocation of
	 * this method
	 *
	 * @return set of the site pins that either driver or are driven by this pin
	 * @see #getSitePinNames()
	 */
	public Set<SitePin> getSitePins() {
		bel.getSite().setType(bel.getId().getPrimitiveType());
		if (template.getSitePins() == null)
			return Collections.emptySet();
		Set<SitePin> sitePins = new HashSet<>();
		sitePins.addAll(template.getSitePins().stream()
				.map(sitePinName -> bel.getSite().getSitePin(sitePinName))
				.collect(Collectors.toList()));
		return sitePins;
	}

	/**
	 * Returns the names of the site pins that either drive or are driven by this
	 * pin.
	 * <p>
	 * This method is especially helpful in determining which site pins a router
	 * should target to reach this BEL pin.  A set is returned since some BEL pins
	 * are reachable from multiple site pins.
	 * <p>
	 * Unlike {@link #getSitePins()}, this method does not build any new objects.
	 *
	 * @return set of the site pins that either driver or are driven by this pin
	 * @see #getSitePins()
	 */
	public Set<String> getSitePinNames() {
		Set<String> sitePins = template.getSitePins();
		if (sitePins == null)
			return Collections.emptySet();
		return sitePins;
	}

	@Override
	public int hashCode() {
		return bel.hashCode() * 31 + template.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final BelPin other = (BelPin) obj;
		return Objects.equals(this.bel, other.bel) &&
				Objects.equals(this.template, other.template);
	}

	@Override
	public String toString() {
		return "BelPin{" + bel.getSite().getName() +
				"/" + bel.getName() +
				"." + template.getName() +
				"}";
	}
}
