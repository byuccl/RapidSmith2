/*
 * Copyright (c) 2016 Brigham Young University
 *
 * This file is part of the BYU RapidSmith Tools.
 *
 * BYU RapidSmith Tools is free software: you may redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * BYU RapidSmith Tools is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * A copy of the GNU General Public License is included with the BYU
 * RapidSmith Tools. It can be found at doc/LICENSE.GPL3.TXT. You may
 * also get a copy of the license at <http://www.gnu.org/licenses/>.
 */

package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.PinDirection;
import edu.byu.ece.rapidSmith.device.SitePin;
import edu.byu.ece.rapidSmith.util.Exceptions;

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
	
	/** Unique Serialization ID for this class*/
	private static final long serialVersionUID = 6082237548065721803L;
	/** Unique name of the net */
	private String name;
	/**Type of net*/
	private NetType type;
	/** Design the net is attached to*/
	private CellDesign design;
	/** Sink pins of the net */
	private Set<CellPin> pins;
	/** Source pin of the net*/
	private CellPin sourcePin;
	/** Property map for the Net*/
	private Map<Object, Property> propertyMap;
	/** Set of CellPins that have been marked as routed in the design*/
	private Set<CellPin> routedSinks; 
	/** Set to true if this net is contained within a single site's boundaries*/
	private boolean isIntrasite;
	/** Route status of the net*/
	private RouteStatus routeStatus;
	
	// Physical route information
	/** SitePin source of the net (i.e. where the net leaves the site)*/
	private SitePin sourceSitePin;
	/** Route Tree connecting to the source pin of the net*/
	private RouteTree source;
	/** List of intersite RouteTree objects for the net*/
	private List<RouteTree> intersiteRoutes;
	/** Maps a connecting BelPin of the net, to the RouteTree connected to the BelPin*/
	private Map<BelPin, RouteTree> belPinToSinkRTMap;
	/** Maps a connecting SitePin of the net, to the RouteTree connected to the SitePin*/
	private Map<SitePin, RouteTree> sitePinToRTMap;

	/**
	 * Creates a new net with the given name.
	 *
	 * @param name the name for the net
	 */
	public CellNet(String name, NetType type) {
		Objects.requireNonNull(name);

		this.name = name;
		this.type = type;
		this.isIntrasite = false;
		init();
	}

	private void init() {
		this.pins = new HashSet<>();
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

	/**
	 * Returns the sink pins of the net. This structure should not be modified by the user.
	 * 
	 * @return CellPin sinks of the net
	 */
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
			throw new Exceptions.DesignAssemblyException("Pin already exists in net.");
		if (pin.getNet() != null)
			throw new Exceptions.DesignAssemblyException("Pin already connected to net.");

		pins.add(pin);
		pin.setNet(this);

		if (sourcePin == null && pin.isOutpin()) {
			sourcePin = pin;
		} else if (pin.getDirection() == PinDirection.OUT) {
			assert sourcePin != null;
			if (sourcePin.getDirection() == PinDirection.OUT)
				throw new Exceptions.DesignAssemblyException("Cannot create multiply-sourced net.");
			sourcePin = pin;
		}
	}
	
	/**
	 * Returns the number of pseudo pins connected to the net.
	 */
	public int getPseudoPinCount() {
		
		int pseudoPinCount = 0;
		
		for (CellPin pin : pins) {
			if (pin.isPseudoPin()) {
				pseudoPinCount++;
			}
		}
		
		return pseudoPinCount;
	}

	/**
	 * Removes a collection of pins from the net. 
	 * 
	 * @param pins Collection of pins to remove
	 */
	public void disconnectFromPins(Collection<CellPin> pins) {
		pins.forEach(this::disconnectFromPin);
	}

	/**
	 * Tests if the specified pin is attached to the net
	 * 
	 * @param pin CellPin to test
	 * @return <code>true</code> if the pin is attached to the net, <code>false</code> otherwise
	 */
	public boolean isConnectedToPin(CellPin pin) {
		return pins.contains(pin);
	}
	
	/**
	 * Disconnects the net from all of its current pins
	 */
	public void detachNet() { 
		
		pins.forEach(CellPin::clearNet);
		
		if (sourcePin != null) {
			sourcePin = null;
		}
		
		pins.clear();
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
			throw new Exceptions.DesignAssemblyException("Pin not found in net");

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
	 * More specifically, checks if the pins connected to this net are of {@link CellPinType#CLOCK}.
	 *
	 * @return true if this net is a clock net
	 */
	public boolean isClkNet() {
		
		for (CellPin p : this.pins) {
			if (p.getType() == CellPinType.CLOCK) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if a net is a clk net and should use the clock routing resources.
	 * More specifically, checks if at least half of the pins of this net contain
	 * the substring "CLK", "CK", or "C" in their names. This function should only be used if
	 * the design was imported by the XDL unpacker and will soon be removed. For Vivado 
	 * imported designs, use {@link CellNet#isClkNet} instead.
	 * 
	 * @deprecated
	 * @return <code>true</code> if the net is a clock net. <code>false</code> otherwise.
	 */
	public boolean isClkNetXDL() {
		
		Collection<CellPin> cellPins = getPins();
		for (CellPin p : cellPins) {
			if (p.getName().contains("CK") || p.getName().contains("CLK") || p.getName().equals("C") ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the net is a VCC (logic high) net
	 */
	public boolean isVCCNet() {

		return type.equals(NetType.VCC);
	}

	/**
	 * Returns true if the net is a GND (logic low) net
	 */
	public boolean isGNDNet() {

		return type.equals(NetType.GND);
	}

	public CellNet deepCopy() {
		CellNet copy = new CellNet(getName(), getType());
		if (intersiteRoutes != null)
			intersiteRoutes.forEach(rt -> copy.addIntersiteRouteTree(rt.deepCopy()));
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

	/**
	 * Returns true if the net is either a VCC or GND net. 
	 * 
	 * @return 
	 */
	public boolean isStaticNet() {
		return type == NetType.VCC || type == NetType.GND;
	}
	
	/* **********************************
	 * 	    Physical Route Functions
	 * **********************************/
	
	/**
	 * Sets the {@link SitePin} source of the net. This is used to
	 * set the source of a net when loading a Tincr Checkpoint. If you are
	 * writing a intersite router, this will give you the site pin where the 
	 * route needs to start.
	 * @param sitePin {@link SitePin} 
	 */
	public void setSourceSitePin(SitePin sitePin) {
		this.sourceSitePin = sitePin;
	}
	
	/**
	 * Gets the {@link SitePin} where this net is sourced.
	 * @return {@link SitePin}
	 */
	public SitePin getSourceSitePin() {
		return this.sourceSitePin;
	}
	
	/**
	 * Gets the {@link BelPin} where this net is sourced
	 * @return {@link BelPin}
	 */
	public BelPin getSourceBelPin() {
		return this.sourcePin.getMappedBelPin();
	}
	
	/**
	 * Returns a collection of pips that are used in this nets physical route
	 * @return
	 */
	public Collection<PIP> getPips() {
		if (intersiteRoutes == null)
			return Collections.emptySet();
		Set<PIP> pipSet = new HashSet<>();
		for (RouteTree tree : intersiteRoutes) {
			pipSet.addAll(tree.getAllPips());
		}
		return pipSet;
	}

	/**
	 * Marks the net as intrasite (completely contained within a site) or not (streches across site boundaries). 
	 * 
	 * @param isInstrasite Boolean 
	 */
	public void setIsIntrasite(boolean isInstrasite) {
		this.isIntrasite = isInstrasite;
	}
	
	/**
	 * Returns True if the net is an intrasite net. False otherwise
	 * @return
	 */
	public boolean isIntrasite() {
		return isIntrasite;
	}
	
	/**
	 * Returns all of the unrouted sinks of the net
	 * @return
	 */
	public Set<CellPin> getUnroutedSinks() {
		
		if (routedSinks == null || routedSinks.isEmpty()) {
			return (Set<CellPin>) getSinkPins(); 
		}
		
		return pins.stream()
					.filter(pin -> !routedSinks.contains(pin) && pin.isInpin())
					.collect(Collectors.toSet());
	}
	
	/**
	 * Returns all of the routed sinks of the net
	 * @return
	 */
	public Set<CellPin> getRoutedSinks() {
		
		if (routedSinks == null) {
			return Collections.emptySet();
		}
		
		return routedSinks; 
	}
	
	/**
	 * Mark a collection of pins in the net that have been routed. It is up to the user
	 * to keep the routed sinks up-to-date.
	 * 
	 * @param cellPin Collection of cell pins to be marked as routed
	 */
	public void addRoutedSinks(Collection<CellPin> cellPin) {
		cellPin.forEach(this::addRoutedSink);
	}
	
	/**
	 * Marks the specified pin as being routed. It is up to the user to keep the
	 * routed sinks up-to-date. 
	 * 
	 * @param cellPin CellPin object to mark as routed
	 */
	public void addRoutedSink(CellPin cellPin) {
		
		if (!pins.contains(cellPin)) {
			throw new IllegalArgumentException("CellPin" + cellPin.getName() + " not attached to net. "
					+ "Cannot be added to the routed sinks of the net!");
		}
		
		if (cellPin.isOutpin()) {
			throw new IllegalArgumentException(String.format("CellPin %s is an output pin. Cannout be added as a routed sink!", cellPin.getName()));
		}
		
		if(routedSinks == null) {
			routedSinks = new HashSet<>();
		}
		routedSinks.add(cellPin);
	}
	
	/**
	 * Marks a cellPin attached to the net as unrouted. 
	 * 
	 * @param cellPin {@link CellPin}
	 * @return <code>true</code> if the cellPin was successfully removed.  
	 * 		<code>false</code> if the cellPin is not marked as a routed pin of the net.  
	 */
	public boolean removeRoutedSink(CellPin cellPin) {
		return routedSinks.remove(cellPin);
	}
	
	/**
	 * This removes all PIPs from this net, causing it to be in an unrouted state.
	 * PIPs from placed relatively-routed molecules are preserved.
	 */
	public void unroute() {
		intersiteRoutes = null;
		routedSinks = null;
	}
	
	/**
	 * Sets the route tree starting at the source BelPin, and ending on the site pin where it leaves the site.
	 * For intrasite nets, it will end on another BelPin within the site.
	 * @param source
	 */
	public void setSourceRouteTree(RouteTree source) {
		
		this.source = source;
	}
	
	/**
	 * Returns the starting intrasite route of the net
	 * @return
	 */
	public RouteTree getSourceRouteTree() {
		return source;
	}
	
	/**
	 * Adds an intersite RouteTree object to the net. An intersite route
	 * starts at a site pin, and ends at one or more site pins. In general,
	 * a net will have exactly one intersite route tree, but GND and VCC
	 * nets will have more than one (since they are sourced by multiple tieoff locations)
	 *  
	 * @param intersite The RouteTree to add
	 */
	public void addIntersiteRouteTree(RouteTree intersite) {	
		Objects.requireNonNull(intersite);

		if (intersiteRoutes == null) {
			intersiteRoutes = new ArrayList<>();
		}
		this.intersiteRoutes.add(intersite);
	}
	
	/**
	 * Sets the list of intersite route trees to the specified list.
	 * @param routes
	 */
	public void setIntersiteRouteTrees(List<RouteTree> routes) {
		this.intersiteRoutes = routes;
	}
	
	/**
	 * Returns the first intersite route associated with the net. 
	 * Use this function for general nets which should only have one
	 * Route Tree.  
	 * 
	 * @return
	 */
	public RouteTree getIntersiteRouteTree() {
		
		if (intersiteRoutes == null || intersiteRoutes.isEmpty()) {
			return null;
		}
		
		return intersiteRoutes.get(0);
	}
	
	/**
	 * Returns all intersite RouteTree objects associated with this net.
	 * 
	 * @return A List of RouteTree objects
	 */
	public List<RouteTree> getIntersiteRouteTreeList() {
	
		if (intersiteRoutes == null) {
			return Collections.emptyList();
		}
		return intersiteRoutes;
	}
	
	/**
	 * @return <code>true</code> if this net has one intersite {@link RouteTree}
	 * 		object connected to it. <code>false</code> otherwise.
	 */
	public boolean hasIntersiteRouting() {
		return intersiteRoutes != null && intersiteRoutes.size() > 0;
	}
	
	/**
	 * Adds a RouteTree object that connects to the specified BelPin. 
	 * 
	 * @param bp Connecting BelPin
	 * @param route RouteTree leading to that BelPin
	 */
	public void addSinkRouteTree(BelPin bp, RouteTree route) {
		
		if (belPinToSinkRTMap == null) {
			belPinToSinkRTMap = new HashMap<>();
		}
		belPinToSinkRTMap.put(bp, route);
	}
	
	/**
	 * Adds a RouteTree object that starts at the specified SitePin
	 * 
	 * @param sp Source SitePin
	 * @param route RouteTree sourced by the SitePin
	 */
	public void addSinkRouteTree(SitePin sp, RouteTree route) {
		
		if (sitePinToRTMap == null) {
			sitePinToRTMap = new HashMap<>();
		}
		sitePinToRTMap.put(sp, route);
	}

	/**
	 * Returns the RouteTree object connected to the given SitePin object. 
	 * This RouteTree contains wires INSIDE the Site, and will connect to
	 * several BelPin sinks within the Site of the SitePin.
	 * 
	 * @param belPin Input (sink) SitePin
	 * @return
	 */
	public RouteTree getSinkRouteTree(SitePin sitePin) {
				
		return sitePinToRTMap == null ? null : sitePinToRTMap.get(sitePin);
	}

	/**
	 * Returns a set of SitePins that the net is currently connected to.
	 * @return
	 */
	public Set<SitePin> getSitePins() {
		return sitePinToRTMap == null ? null : sitePinToRTMap.keySet();
	}
	
	/**
	 * Returns the SitePin to RouteTree Map of the cell net. Should not 
	 * be modified by the user. 
	 * @return
	 */
	public Map<SitePin, RouteTree> getSitePinRouteTrees() {
		return sitePinToRTMap;
	}
	
	/**
	 * Returns a list of RouteTree connected to sink SitePin objects
	 */
	public List<RouteTree> getSinkSitePinRouteTrees() {
		
		if (sitePinToRTMap == null) {
			return Collections.emptyList();
		}
		
		return sitePinToRTMap.keySet().stream()
									.filter(SitePin::isInput)
									.map(sp -> sitePinToRTMap.get(sp))
									.collect(Collectors.toList());
	}
	
	/**
	 * Returns a RouteTree object that is connected to the specified CellPin. If the CellPin
	 * is connected to multiple RouteTree objects (because it is mapped to multiple BelPins)
	 * then only one of the RouteTrees will be returned. To return all of the route trees, call
	 * {@link #getSinkRouteTrees}. Only use this function if you know that the CellPin maps to a single BelPin.
	 * 
	 * @param cellPin sink CellPin
	 * @return A RouteTree that is connected to the specified CellPin. If no
	 * 		   connecting RouteTree exists, null is returned. If more than one
	 * 		   RouteTree is connected to the CellPin, then one of the RouteTrees
	 * 		   will be returned (no guarantee which that will be)
	 */
	public RouteTree getSinkRouteTree(CellPin cellPin) {
		
		BelPin belPin = cellPin.getMappedBelPin();
		return belPinToSinkRTMap.get(belPin);
	}
	
	/**
	 * Returns all RouteTrees of this net that are connected to the specified CellPin.
	 * If the CellPin only maps to one BelPin, use {@link #getSinkRouteTree(CellPin, CellNet) }
	 * instead.
	 * 
	 * @param cellPin sink CellPin
	 * @return A Set of RouteTree objects that cellPin is connected to.
	 */
	public Set <RouteTree> getSinkRouteTrees(CellPin cellPin) {
		
		Set<RouteTree> connectedRouteTrees = new HashSet<>();
		
		for (BelPin belPin : cellPin.getMappedBelPins()) {
			if (belPinToSinkRTMap.containsKey(belPin)) {
				connectedRouteTrees.add(belPinToSinkRTMap.get(belPin));
			}
		}
		
		return connectedRouteTrees;
	}
	
	/**
	 * Gets the RouteTree object connected to the specified BelPin of the net
	 * 
	 * @param belPin Input BelPin
	 * @return A {@link RouteTree} that connects to {@code belPin}. If the belPin
	 * 		does not attach the net net, <code>null</code> is returned.
	 */
	public RouteTree getSinkRouteTree(BelPin belPin) {
		return belPinToSinkRTMap == null ? null : belPinToSinkRTMap.get(belPin);
	}
	
	/**
	 * Returns a set of BelPins that the net is currently connected to.
	 * 
	 * TODO: Could this be done by simply taking all the sink cell pins and getting the
	 * 		 corresponding BelPin? 
	 */
	public Set<BelPin> getBelPins() {
		
		Set<BelPin> connectedBelPins = new HashSet<>();
		
		if (belPinToSinkRTMap != null) {
			connectedBelPins.addAll(belPinToSinkRTMap.keySet());
		}
		
		BelPin belPinSource = sourcePin.getMappedBelPin();
		if (belPinSource != null) {
			connectedBelPins.add(belPinSource);
		}
		
		return connectedBelPins;
	}
	
	/**
	 * Returns the BelPin to RouteTree map of the net
	 */
	public Map<BelPin, RouteTree> getBelPinRouteTrees() {
		return belPinToSinkRTMap;
	}
		
	/**
	 * Returns the current route status of net without recomputing the status. If the routing has changed,
	 * to recompute the route status first use {@link CellNet:computeRouteStatus}.
	 * Possible statuses in include: <br>
	 * 1.) UNROUTED - no sink cell pins have been routed <br>
	 * 2.) PARTIALLY_ROUTED - some, but not all, sink cell pins that have been mapped to bel pins have been routed<br>
	 * 3.) FULLY_ROUTED - all sink cell pins that are mapped to bel pins have been routed <br> 
	 * 
	 * @return The {@link RouteStatus} of the current net
	 */
	public RouteStatus getRouteStatus() {
		return routeStatus;
	}
	
	/**
	 * Computes and stores the route status of the net. This function should be called to recompute the status
	 * of the route if the routing structure has been modified and the . If the routing structure has not been modified,
	 * then {@link CellNet:getRouteStatus} should be used instead. Possible statuses include: <br>
	 * <br>
	 * 1.) <b>UNROUTED</b> - no sink cell pins have been routed <br>
	 * 2.) <b>PARTIALLY_ROUTED</b> - some, but not all, sink cell pins <b>that have been mapped to bel pins</b> have been routed<br>
	 * 3.) <b>FULLY_ROUTED</b> - all sink cell pins <b>that are mapped to bel pins</b> have been routed <br> 
	 * <br>
	 * The complexity of this method is O(n) where n is the number of pins connected to the net.
	 * 
	 * @return The current RouteStatus of the net
	 */
	public RouteStatus computeRouteStatus() {
		
		int subtractCount = sourcePin.isMapped() ? 1 : 0;
		
		if (routedSinks == null || routedSinks.isEmpty()) {
			routeStatus = RouteStatus.UNROUTED;
		}
		else if (routedSinks.size() == pins.stream().filter(CellPin::isMapped).count() - subtractCount) {
			routeStatus = RouteStatus.FULLY_ROUTED;
		}
		else {
			routeStatus = RouteStatus.PARTIALLY_ROUTED;
		}
		
		return routeStatus;
	}
}
