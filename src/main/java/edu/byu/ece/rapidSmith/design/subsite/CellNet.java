package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.device.PinDirection;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 *  Represents a net in a cell design.  Cell nets connect the pins on cells.
 *  Cell nets contain a set of pins they connect to and a set of PIPs which
 *  define the routing of the net.
 *
 *  Cell nets can contain multiple source pins, however, in this case only one of
 *  the pins can be designated as an output pin and all other sources must be an
 *  inout pin.  When an output pin exists, it is designated as the source; when
 *  absent, the source is one of the inout pins.
 *
 *  Cell nets may be members of a molecule.
 */
public class CellNet implements Serializable {
	/** Unique name of the net */
	private String name;
	private NetType type;
	private CellDesign design;
	/** Source and sink pins of the net */
	private List<CellPin> pins;
	private CellPin sourcePin;
	/** Routing resources or Programmable-Interconnect-Points */
	private List<RouteTree> routeTrees;
	private Map<Object, Property> propertyMap;

	/**
	 * Creates a new net with the given name.
	 *
	 * @param name the name for the net
	 */
	public CellNet(String name, NetType type) {
		Objects.requireNonNull(name);

		this.name = name;
		this.type = type;
		init();
	}

	private void init() {
		this.pins = new ArrayList<>(3);
	}

	/**
	 * Returns the name of the net.
	 *
	 * @return the name of the net
	 */
	public String getName() {
		return name;
	}

	public NetType getType() {
		return type;
	}

	public void setType(NetType type) {
		this.type = type;
	}

	/**
	 * Returns true if this net is in a design.
	 *
	 * @return true if this net is in a design
	 */
	public boolean isInDesign() {
		return design != null;
	}

	/**
	 * Returns the design this net is a part of.
	 *
	 * @return the design the net is a part of
	 */
	public CellDesign getDesign() {
		return design;
	}

	void setDesign(CellDesign design) {
		this.design = design;
	}
	
	/**
	 * Returns true if this cell contains a property with the specified name.
	 *
	 * @param propertyKey the name of the property to check for
	 * @return true if this cell contains a property with the specified name
	 */
	public boolean hasProperty(Object propertyKey) {
		Objects.requireNonNull(propertyKey);

		return getProperty(propertyKey) != null;
	}

	/**
	 * Returns the property from this cell with the specified name.
	 *
	 * @param propertyKey name of the property to get
	 * @return the property with the specified name
	 */
	public Property getProperty(Object propertyKey) {
		Objects.requireNonNull(propertyKey);

		if (propertyMap == null)
			return null;
		return propertyMap.get(propertyKey);
	}

	/**
	 * Returns the properties of this cell.  The returned collection should not be
	 * modified by the user.
	 *
	 * @return the properties of this cell
	 */
	public Collection<Property> getProperties() {
		if (propertyMap == null)
			return Collections.emptyList();
		return propertyMap.values();
	}

	/**
	 * Updates or adds the property to this cell.
	 *
	 * @param property the property to add or update
	 */
	public void updateProperty(Property property) {
		Objects.requireNonNull(property);

		if (this.propertyMap == null)
			this.propertyMap = new HashMap<>();
		this.propertyMap.put(property.getKey(), property);
	}

	/**
	 * Updates the value of the property in this cell with the specified name or
	 * creates and adds it if it is not already present.
	 *
	 * @param propertyKey the name of the property
	 * @param value the value to set the property to
	 */
	public void updateProperty(Object propertyKey, PropertyType type, Object value) {
		Objects.requireNonNull(propertyKey);
		Objects.requireNonNull(type);
		Objects.requireNonNull(value);

		updateProperty(new Property(propertyKey, type, value));
	}
	
	/**
	 * Updates or adds the properties in the provided collection to the properties
	 * of this cell.
	 *
	 * @param properties the properties to add or update
	 */
	public void updateProperties(Collection<Property> properties) {
		Objects.requireNonNull(properties);

		properties.forEach(this::updateProperty);
	}

	/**
	 * Removes the property with the specified name.  Returns the removed property.
	 *
	 * @param propertyKey hte name of the property to remove
	 * @return the removed property
	 */
	public Property removeProperty(Object propertyKey) {
		Objects.requireNonNull(propertyKey);

		if (propertyMap == null)
			return null;
		return propertyMap.remove(propertyKey);
	}
	
	/**
	 * Returns the pins (source and sinks) of this net.  This structure should not
	 * be modified by the user.
	 *
	 * @return the pins of this net
	 */
	public Collection<CellPin> getPins() {
		return pins;
	}

	public Collection<CellPin> getSinkPins() {
		return getPins().stream()
				.filter(p -> p != sourcePin)
				.collect(Collectors.toList());
	}

	/**
	 * Checks if this net has a source pin.
	 *
	 * @return true if this net has a source pin
	 */
	public boolean isSourced() {
		return getSourcePin() != null;
	}

	/**
	 * Returns the source of this net.  The source is the out pin in the net or
	 * if no outpin exists, then one of the inout pins.
	 *
	 * @return The current source of this net, or null if it does not exist.
	 */
	public CellPin getSourcePin() {
		return sourcePin;
	}

	/**
	 * Returns all of the pins that source the net including the inout pins.
	 *
	 * @return all of the pins that source the net
	 */
	public List<CellPin> getAllSourcePins() {
		return getPins().stream()
				.filter(CellPin::isOutpin)
				.collect(Collectors.toList());
	}

	/**
	 * Checks if this net contains multiple source pins.  This can be true only
	 * if it contains inout pins.
	 *
	 * @return true if this net contains multiple source pins.
	 */
	public boolean isMultiSourced() {
		return getAllSourcePins().size() > 1;
	}

	/**
	 * Adds a collection of pins to this net.
	 *
	 * @param pinsToAdd the collection of pins to add
	 */
	public void connectToPins(Collection<CellPin> pinsToAdd) {
		Objects.requireNonNull(pinsToAdd);

		pinsToAdd.forEach(this::connectToPin);
	}

	/**
	 * Adds a pin to this net.  It is an error to add multiple output pins
	 * (excluding inout pins).
	 *
	 * @param pin the new pin to add
	 */
	public void connectToPin(CellPin pin) {
		Objects.requireNonNull(pin);
		if (pins.contains(pin))
			throw new DesignAssemblyException("Pin already exists in net.");
		if (pin.getNet() != null)
			throw new DesignAssemblyException("Pin already connected to net.");

		pins.add(pin);
		pin.setNet(this);

		if (sourcePin == null && pin.isOutpin()) {
			sourcePin = pin;
		} else if (pin.getDirection() == PinDirection.OUT) {
			assert sourcePin != null;
			if (sourcePin.getDirection() == PinDirection.OUT)
				throw new DesignAssemblyException("Cannot create multiply-sourced net.");
			sourcePin = pin;
		}
	}

	/**
	 * Removes a pin from this net.
	 *
	 * @param pin the pin to remove
	 */
	public void disconnectFromPin(CellPin pin) {
		Objects.requireNonNull(pin);

		boolean used = pins.remove(pin);
		if (!used)
			throw new DesignAssemblyException("Pin not found in net");

		if (sourcePin == pin) {
			sourcePin = null;
			List<CellPin> sourcePins = getAllSourcePins();
			if (!sourcePins.isEmpty()) {
				assert sourcePins.stream()
						.map(p -> p.getDirection() != PinDirection.OUT)
						.reduce(true, Boolean::logicalAnd);
				sourcePin = sourcePins.get(0);
			}
		}

		pin.clearNet();
	}

	/**
	 * Returns the fan-out (number of sinks) of this net.  More formally, the
	 * number of pins if the net has no source else the number of pins minus 1.
	 *
	 * @return the fan-out of this net
	 */
	public int getFanOut() {
		if (getSourcePin() == null)
			return getPins().size();
		else
			return getPins().size() - 1;
	}

	/**
	 * Checks if a net is a clk net and should use the clock routing resources.
	 * More specifically, checks if at least half of the pins of this net contain
	 * the substring "CLK" in their names.
	 *
	 * @return true if this net is a clock net
	 */
	public boolean isClkNet() {
		// TODO It'd be nice to identify all of the clock pins on the cells
		// This is kind of difficult to quantify, but we'll assume that if any
		// of the pins on this net are name CLK or CK, then the net is a clock
		// net
		Collection<CellPin> cellPins = getPins();
		for (CellPin p : cellPins) {
			if (p.getName().contains("CK") || p.getName().contains("CLK") || p.getName().equals("C") ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the PIPs (routing resources) used by this net.
	 *
	 * @return the PIPs used by this net
	 */
	public Collection<RouteTree> getRouteTrees() {
		if (routeTrees == null)
			return Collections.emptyList();
		return routeTrees;
	}

	public void addRouteTree(RouteTree rt) {
		Objects.requireNonNull(rt);

		if (routeTrees == null)
			routeTrees = new ArrayList<>();
		routeTrees.add(rt);
	}

	/**
	 * This removes all PIPs from this net, causing it to be in an unrouted state.
	 * PIPs from placed relatively-routed molecules are preserved.
	 */
	public void unroute() {
		routeTrees = null;
	}

	public boolean isRouted() {
		return routeTrees != null;
	}

	public CellNet deepCopy() {
		CellNet copy = new CellNet(getName(), getType());
		if (routeTrees != null)
			routeTrees.forEach(rt -> copy.addRouteTree(rt.deepCopy()));
		return copy;
	}

	// Use equality equals

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return "CellNet{" + getName() + "}";
	}

	public boolean isStaticNet() {
		return type == NetType.VCC || type == NetType.GND;
	}

	public Collection<PIP> getPips() {
		if (routeTrees == null)
			return Collections.emptySet();
		Set<PIP> pipSet = new HashSet<>();
		for (RouteTree tree : routeTrees) {
			pipSet.addAll(tree.getAllPips());
		}
		return pipSet;
	}
}
