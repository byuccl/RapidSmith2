/*
 * Copyright (c) 2010 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.device;

import edu.byu.ece.rapidSmith.design.Attribute;
import edu.byu.ece.rapidSmith.design.subsite.SitePip;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents the primitive sites found in XDLRC files.  Theses
 * represent places on the FPGA where a particular instance of a primitive (or
 * XDL 'inst') can reside.
 * @author Chris Lavin
 */
public final class Site implements Serializable{
	/** Name of the primitive site with X and Y coordinates (ie. SLICE_X0Y0) */
	private String name;
	/** The index in the tile's list of PrimitiveSites */
	private int index;
	/** The tile where this site resides */
	private Tile tile;
	/** The X coordinate of the instance (ex: SLICE_X#Y5) */
	private int instanceX;
	/** The Y coordinate of the instance (ex: SLICE_X5Y#) */
	private int instanceY;
	/** The bondedness of the site */
	private BondedType bondedType;
	/** Stores the template of the type that has been assigned to this site. */
	private SiteTemplate template;
	/** List of possible types for this site. */
	private SiteType[] possibleTypes;
	/**
	 * A map of the external wire each pin connects to for each primitive type this
	 * site can be represented as.
	 */
	private Map<SiteType, Map<String, Integer>> externalWires;
	/**
	 * Map of the site pin each wire connecting to the site connects to for each
	 * primitive type this site can be represented as.
	 */
	private Map<SiteType, Map<Integer, SitePinTemplate>> externalWireToPinNameMap;

	/**
	 * Constructor unnamed, tileless primitive site.
	 */
	public Site(){
		name = null;
		tile = null;
		instanceX = -1;
		instanceY = -1;
	}
	
	/**
	 * Returns the name of this primitive site (ex: SLICE_X4Y6).
	 * @return the unique name of this primitive site.
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Sets the name of this primitive site (ex: SLICE_X5Y7).
	 * Also infers the XY coordinates for this site based on the name.
	 * @param name the name to set.
	 */
	public void setName(String name){
		this.name = name;

		// Populate the X and Y coordinates based on name
		if (!name.contains("_X"))
			return;

		// Populate the X and Y coordinates based on name
		int end = name.length();
		int chIndex = name.lastIndexOf('Y');
		this.instanceY = Integer.parseInt(name.substring(chIndex + 1, end));

		end = chIndex;
		chIndex = name.lastIndexOf('X');
		this.instanceX = Integer.parseInt(name.substring(chIndex + 1, end));
	}

	/**
	 * Returns the index of this site in it tile's PrimitiveSite list.
	 * @return this site's index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Sets the index of this site in its tile's PrimitiveSite list.
	 * @param index index of this site
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * Returns the tile in which this primitive site exists.
	 * @return the tile in which this primitive site exists
	 */
	public Tile getTile(){
		return tile;
	}
	
	/**
	 * Sets the tile in which this primitive site exists.
	 * @param location the tile location for this site
	 */
	public void setTile(Tile location){
		this.tile = location;
	}

	/**
	 * Returns the integer X value of the instance location
	 * (ex: SLICE_X5Y10, it will return 5).
	 * @return the X integer value of the site name or -1 if this instance is
	 * not placed or does not have X/Y coordinates in the site name
	 */
	public int getInstanceX(){
		return instanceX;
	}

	/**
	 * Returns the integer Y value of the instance location
	 * (ex: SLICE_X5Y10, it will return 10).
	 * @return The Y integer value of the site name or -1 if this instance is
	 * not placed or does not have X/Y coordinates in the site name
	 */
	public int getInstanceY(){
		return instanceY;
	}

	/**
	 * Returns the current type of this primitive site.
	 * @return the current type of this primitive site
	 */
	public SiteType getType(){
		return getTemplate().getType();
	}

	/**
	 * Updates the type of this site to the specified type.
	 * Does not perform any validation, so this site can mistakenly be given a
	 * type that is not in its possible types set.
	 * <p>
	 * This method obtains the site template from its device, therefore, the
	 * site must already exist in a tile which exists in a device.
	 * @param type the new primitive type for this site
	 */
	public void setType(SiteType type) {
		template = getTile().getDevice().getSiteTemplate(type);
	}

	/**
	 * Returns the default type of this site.
	 * The default type is defined as getPossibleTypes[0].
	 *
	 * @return the default type of this site
	 */
	public SiteType getDefaultType() {
		if (possibleTypes == null)
			return template.getType();
		return possibleTypes[0];
	}

	/**
	 * Returns an array containing the valid primitive types that this site can be
	 * treated as.
	 *
	 * @return the possible types for this site
	 */
	public SiteType[] getPossibleTypes() {
		return possibleTypes;
	}

	/**
	 * Sets the possible types for this site.
	 * The type as index 0 is considered the default type for the site.
	 * This method does not update the type of the site.
	 * @param possibleTypes the possible types for this site
	 */
	public void setPossibleTypes(SiteType[] possibleTypes) {
		this.possibleTypes = possibleTypes;
	}

	/**
	 * Returns the current template backing this site.
	 * The template will change when the site's type changes.
	 * @return the current template backing this site
	 */
	SiteTemplate getTemplate() {
		return template;
	}

	SiteTemplate getTemplate(SiteType type) {
		if (getType() == type)
			return getTemplate();

		return getTile().getDevice().getSiteTemplate(type);
	}

	/**
	 * Returns whether the site is bonded.  IO are either bonded or unbonded.  Non-IO
	 * are always <code>internal</code>.
	 * @return the bondedness of the site
	 */
	public BondedType getBondedType() {
		return bondedType;
	}

	/**
	 * Sets whether the site is bonded.  IO are either bonded or unbonded.  Non-IO
	 * are always <code>internal</code>.
	 * @param bondedType the bondedness of the site
	 */
	public void setBondedType(BondedType bondedType) {
		this.bondedType = bondedType;
	}

	/* Exposed template getter methods */
	/**
	 * Returns a set containing all of the BEL names in the site.
	 * Unlike {@link #getBels()}, no objects are created by this call.
	 *
	 * @return set containing all of the BEL names in the site
	 */
	public Set<String> getBelNames() {
		return getBelNames(getTemplate());
	}

	public Set<String> getBelNames(SiteType type) {
		return getBelNames(getTemplate(type));
	}

	private Set<String> getBelNames(SiteTemplate template) {
		return template.getBelTemplates().keySet();
	}

	/**
	 * Returns the set of all BELs in the site.
	 * Use cautiously as Bel objects are recreated on each call.
	 * @return a new set, possibly empty, of all BELs in the site
	 * @see #getBelNames()
	 */
	public Set<Bel> getBels() {
		return getBels(getTemplate());
	}

	public Set<Bel> getBels(SiteType type) {
		return getBels(getTemplate(type));
	}

	private Set<Bel> getBels(SiteTemplate template) {
		Map<String, BelTemplate> belTemplates = template.getBelTemplates();
		if (belTemplates == null)
			return Collections.emptySet();

		return belTemplates.values().stream()
				.map(t -> new Bel(this, t))
				.collect(Collectors.toSet());
	}

	/**
	 * Returns the BEL of the specified name for the site.
	 * The Bel object will be created upon this call.
	 * @param belName the name of the BEL to return
	 * @return the BEL of the given name or null if no BEL with the specified name
	 *   exist in the (site, type) pair.
	 */
	public Bel getBel(String belName) {
		return getBel(getTemplate(), belName);
	}

	public Bel getBel(BelId belId) {
		return getBel(getTemplate(belId.getPrimitiveType()), belId.getName());
	}

	private Bel getBel(SiteTemplate template, String belName) {
		BelTemplate bt = template.getBelTemplates().get(belName);
		if (bt == null)
			return null;
		return new Bel(this, bt);
	}

	/**
	 * Returns PrimitiveTypes that are compatible with the default site type.
	 * Compatible types are different that possible types.  These are types
	 * that the site type cannot be changed to, but instances of the type be
	 * placed on them.
	 *
	 * @return PrimitiveTypes that are compatible with the default site type
	 */
	public SiteType[] getCompatibleTypes() {
		return getDefaultTemplate().getCompatibleTypes();
	}

	private SiteTemplate getDefaultTemplate() {
		return getTile().getDevice().getSiteTemplate(getDefaultType());
	}

	/**
	 * Returns the wires in the site which source wire connections.
	 *
	 * @return the wires in the site which source wire connections
	 */
	public Set<Integer> getWires() {
		return getWires(getTemplate());
	}

	public Set<Integer> getWires(SiteType type) {
		return getWires(getTemplate(type));
	}

	public Set<Integer> getWires(SiteTemplate template) {
		return template.getWires();
	}

	/**
	 * Returns the wire connections sourced by the specified wire.
	 *
	 * @param wire the source wire
	 * @return the wire connections sourced by the specified wire
	 */
	public WireConnection[] getWireConnections(int wire) {
		return getWireConnections(getTemplate(), wire);
	}

	public WireConnection[] getWireConnections(SiteType type, int wire) {
		return getWireConnections(getTemplate(type), wire);
	}

	public WireConnection[] getWireConnections(SiteTemplate template, int wire) {
		return template.getWireConnections(wire);
	}

	public WireConnection[] getReverseConnections(int wire) {
		return getReverseConnections(getTemplate(), wire);
	}

	public WireConnection[] getReverseConnections(SiteType type, int wire) {
		return getReverseConnections(getTemplate(type), wire);
	}

	public WireConnection[] getReverseConnections(SiteTemplate template, int wire) {
		return template.getReverseWireConnections(wire);
	}

	public Attribute getPipAttribute(int sourceWire, int sinkWire) {
		return getPipAttribute(getTemplate(), sourceWire, sinkWire);
	}

	public Attribute getPipAttribute(SiteType type, int sourceWire, int sinkWire) {
		return getPipAttribute(getTemplate(type), sourceWire, sinkWire);
	}

	public Attribute getPipAttribute(SiteTemplate template, int sourceWire, int sinkWire) {
		return template.getPipAttributes().get(sourceWire).get(sinkWire);
	}

	public Attribute getPipAttribute(SitePip sitePip) {
		return getPipAttribute(getTemplate(), sitePip);
	}

	public Attribute getPipAttribute(SiteType type, SitePip sitePip) {
		return getPipAttribute(getTemplate(type), sitePip);
	}

	public Attribute getPipAttribute(SiteTemplate template, SitePip sitePip) {
		return template.getPipAttribute(sitePip);
	}

	/**
	 * Returns the names of the source pins on the site.
	 * Unlike {@link #getSourcePins()}, this method will not dynamically create
	 * any objects.
	 * @return names of the source pins of the site
	 */
	public Set<String> getSourcePinNames() {
		return getSourcePinNames(getTemplate());
	}

	public Set<String> getSourcePinNames(SiteType type) {
		return getSourcePinNames(getTemplate(type));
	}

	public Set<String> getSourcePinNames(SiteTemplate template) {
		return template.getSources().keySet();
	}

	/**
	 * Creates and returns the source pins for this site.
	 * The SitePin objects are recreated upon each call.
	 * @return the source pins for this site
	 */
	public List<SitePin> getSourcePins() {
		return getSourcePins(getTemplate());
	}

	public List<SitePin> getSourcePins(SiteType type) {
		return getSourcePins(getTemplate(type));
	}

	public List<SitePin> getSourcePins(SiteTemplate template) {
		Map<String, SitePinTemplate> sourceTemplates = template.getSources();
		List<SitePin> pins = new ArrayList<>(sourceTemplates.size());
		for (SitePinTemplate pinTemplate : sourceTemplates.values()) {
			int externalWire = getExternalWire(template.getType(), pinTemplate.getName());
			pins.add(new SitePin(this, pinTemplate, externalWire));
		}
		return pins;
	}

	/**
	 * Creates and returns the source pin on this site with the specified name.
	 * @param pinName the name of the pin to create
	 * @return the source pin on this site with the specified name or
	 *   null if the pin does not exist
	 */
	public SitePin getSourcePin(String pinName) {
		return getSourcePin(getTemplate(), pinName);
	}

	public SitePin getSourcePin(SiteType type, String pinName) {
		return getSourcePin(getTemplate(type), pinName);
	}

	public SitePin getSourcePin(SiteTemplate template, String pinName) {
		SitePinTemplate pinTemplate = template.getSources().get(pinName);
		if (pinTemplate == null)
			return null;
		return new SitePin(this, pinTemplate, getExternalWire(template.getType(), pinName));
	}

	/**
	 * Returns the names of all sink pins on this site.
	 * Unlike {@link #getSinkPins()}, this method will not dynamically create
	 * any objects.

	 * @return the names of all sink pins on the site
	 */
	public Set<String> getSinkPinNames() {
		return getSinkPinNames(getTemplate());
	}

	public Set<String> getSinkPinNames(SiteType type) {
		return getSinkPinNames(getTemplate(type));
	}

	public Set<String> getSinkPinNames(SiteTemplate template) {
		return template.getSinks().keySet();
	}

	/**
	 * Creates and returns all sink pins on this site.
	 * The SitePin objects are recreated upon each call.
	 * @return all sink pins on this site
	 */
	public List<SitePin> getSinkPins() {
		return getSinkPins(getTemplate());
	}

	public List<SitePin> getSinkPins(SiteType type) {
		return getSinkPins(getTemplate(type));
	}

	public List<SitePin> getSinkPins(SiteTemplate template) {
		Map<String, SitePinTemplate> sinkTemplates = template.getSinks();
		List<SitePin> pins = new ArrayList<>(sinkTemplates.size());
		for (SitePinTemplate pinTemplate : sinkTemplates.values()) {
			int externalWire = getExternalWire(template.getType(), pinTemplate.getName());
			pins.add(new SitePin(this, pinTemplate, externalWire));
		}
		return pins;
	}

	/**
	 * Creates and returns the sink pin on this site with the specified name.
	 * @param pinName the name of the pin to create
	 * @return the sink pin on this site with the specified name or
	 *   null if the pin does not exist
	 */
	public SitePin getSinkPin(String pinName) {
		return getSinkPin(getTemplate(), pinName);
	}

	public SitePin getSinkPin(SiteType type, String pinName) {
		return getSinkPin(getTemplate(type), pinName);
	}

	public SitePin getSinkPin(SiteTemplate template, String pinName) {
		SitePinTemplate pinTemplate = template.getSinks().get(pinName);
		if (pinTemplate == null)
			return null;
		return new SitePin(this, pinTemplate, getExternalWire(template.getType(), pinName));
	}

	/**
	 * Creates and returns the pin on this site with the specified name.
	 * @param pinName the name of the pin to create
	 * @return the pin on this site with the specified name
	 */
	public SitePin getSitePin(String pinName) {
		return getSitePin(getTemplate(), pinName);
	}

	public SitePin getSitePin(SiteType type, String pinName) {
		return getSitePin(getTemplate(type), pinName);
	}

	public SitePin getSitePin(SiteTemplate template, String pinName) {
		SitePinTemplate pinTemplate = template.getSinks().get(pinName);
		if (pinTemplate == null)
			pinTemplate = template.getSources().get(pinName);
		if (pinTemplate == null)
			return null;
		return new SitePin(this, pinTemplate, getExternalWire(template.getType(), pinName));
	}

	/**
	 * Creates and returns the pin on the site which connects to the specified
	 * external wire.
	 * @param wire the external wire
	 * @return the pin on the site which connects to the specified external wire or
	 *   null if the wire connects to no pins on this site
	 */
	SitePin getSitePinOfExternalWire(SiteType type, int wire) {
		SitePinTemplate pinTemplate = externalWireToPinNameMap.get(type).get(wire);
		if (pinTemplate == null)
			return null;
		return new SitePin(this, pinTemplate, getExternalWire(type, pinTemplate.getName()));
	}

	/**
	 * Creates and returns the pin on the site which connects to the specified
	 * internal wire.
	 * @param wire the internal wire
	 * @return the pin on thes site which connects to the specified internal wire
	 *   or null if the wire connects to no pins on this site
	 */
	public SitePin getSitePinOfInternalWire(int wire) {
		return getSitePinOfInternalWire(getTemplate(), wire);
	}

	public SitePin getSitePinOfInternalWire(SiteType type, int wire) {
		return getSitePinOfInternalWire(getTemplate(type), wire);
	}

	public SitePin getSitePinOfInternalWire(SiteTemplate template, int wire) {
		SitePinTemplate pinTemplate = template.getInternalSiteWireMap().get(wire);
		if (pinTemplate == null)
			return null;
		return new SitePin(this, pinTemplate, getExternalWire(template.getType(), pinTemplate.getName()));
	}

	// Returns the wire which connects externally to the pin.  Needed to get from
	// inside the site back to the tile routing
	private int getExternalWire(SiteType type, String pinName) {
		return externalWires.get(type).get(pinName);
	}

	/**
	 * Returns the pin of the BEL in this site which connects to the specified wire.
	 * @param wire the site wire
	 * @return the pin of the BEL in this site which connects to the specified wire
	 */
	public BelPin getBelPinOfWire(int wire) {
		return getBelPinOfWire(getTemplate(), wire);
	}

	public BelPin getBelPinOfWire(SiteType type, int wire) {
		return getBelPinOfWire(getTemplate(type), wire);
	}

	public BelPin getBelPinOfWire(SiteTemplate template, int wire) {
		BelPinTemplate pinTemplate = template.getBelPins().get(wire);
		if (pinTemplate == null)
			return null;
		Bel bel = getBel(template, pinTemplate.getId().getName());
		return bel.getBelPin(pinTemplate.getName());
	}

	/**
	 * Sets the mapping of pin names to externally connected wires for each possible
	 * primitive type this site can take.
	 * Used for device creation
	 * @param externalWires the mapping of pin names to externally connected wires
	 */
	public void setExternalWires(Map<SiteType, Map<String, Integer>> externalWires) {
		this.externalWires = externalWires;
	}

	/**
	 * Returns the mapping of pin names to externally connected wires for each
	 * possible primitive type this site can take.
	 * @return the mapping of pin names to externally connected wires
	 */
	public Map<SiteType, Map<String, Integer>> getExternalWires() {
		return externalWires;
	}

	public Map<SiteType, Map<Integer, SitePinTemplate>> getExternalWireToPinNameMap() {
		return externalWireToPinNameMap;
	}

	/**
	 * Sets the mapping of wires to the names of the pins the wires connect to for
	 * each possible primitive type this site can take.
	 * @param externalWireToPinNameMap the mapping of wires to pin names
	 */
	public void setExternalWireToPinNameMap(
			Map<SiteType, Map<Integer, SitePinTemplate>> externalWireToPinNameMap) {
		this.externalWireToPinNameMap = externalWireToPinNameMap;
	}

	// Site compatibility
	/**
	 * This method will check if the PrimitiveType otherType can be placed
	 * at this primitive site.  Most often only if they are
	 * equal can this be true.  However there are a few special cases that require
	 * extra handling.  For example a SLICEL can reside in a SLICEM site but not 
	 * vice versa.  
	 * @param otherType The primitive type to try to place on this site.
	 * @return True if otherType can be placed at this primitive site, false otherwise.
	 */
	public boolean isCompatiblePrimitiveType(SiteType otherType){
		for (SiteType compat : getCompatibleTypes())
			if (compat == otherType)
				return true;
		return false;
	}
	
	/**
	 * This method gets the type of otherSite and calls the other method
	 * public boolean isCompatiblePrimitiveType(PrimitiveType otherType);
	 * See that method for more information.
	 * @param otherSite The other site to see if its type is compatible with this site.
	 * @return True if compatible, false otherwise.
	 */
	public boolean isCompatiblePrimitiveType(Site otherSite){
		return isCompatiblePrimitiveType(otherSite.getType());
	}

	@Override
	public String toString() {
		return "{PrimitiveSite " + name + "}";
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	// Static initialization stuff
//	static{
//		HashMap<PrimitiveType, PrimitiveType[]> compatibleSites;
//		for (FamilyType specificFamilyType : FamilyType.values()){
//			compatibleSites = new HashMap<>();
//			switch (PartNameTools.getBaseTypeFromFamilyType(specificFamilyType)){
//				case SPARTAN2:
//					compatibleSites.put(PrimitiveType.IOB,
//							new PrimitiveType[]{PrimitiveType.PCIIOB});
//					break;
//				case SPARTAN2E:
//					compatibleSites.put(PrimitiveType.IOB,
//							new PrimitiveType[]{PrimitiveType.DLLIOB, PrimitiveType.PCIIOB});
//					break;
//				case SPARTAN3:
//					compatibleSites.put(PrimitiveType.IOB,
//							new PrimitiveType[]{PrimitiveType.DIFFM, PrimitiveType.DIFFS});
//					compatibleSites.put(PrimitiveType.SLICEL,
//							new PrimitiveType[]{PrimitiveType.SLICEM});
//					break;
//				case SPARTAN3A:
//					compatibleSites.put(PrimitiveType.DIFFM,
//							new PrimitiveType[]{PrimitiveType.DIFFMLR, PrimitiveType.DIFFMTB});
//					compatibleSites.put(PrimitiveType.DIFFMI,
//							new PrimitiveType[]{PrimitiveType.DIFFMLR, PrimitiveType.DIFFMTB});
//					compatibleSites.put(PrimitiveType.DIFFMI_NDT,
//							new PrimitiveType[]{PrimitiveType.DIFFMLR, PrimitiveType.DIFFMTB});
//					compatibleSites.put(PrimitiveType.DIFFS,
//							new PrimitiveType[]{PrimitiveType.DIFFSLR, PrimitiveType.DIFFSTB});
//					compatibleSites.put(PrimitiveType.DIFFSI,
//							new PrimitiveType[]{PrimitiveType.DIFFSLR, PrimitiveType.DIFFSTB});
//					compatibleSites.put(PrimitiveType.DIFFSI_NDT,
//							new PrimitiveType[]{PrimitiveType.DIFFSLR, PrimitiveType.DIFFSTB});
//					compatibleSites.put(PrimitiveType.IBUF,
//							new PrimitiveType[]{PrimitiveType.DIFFMLR, PrimitiveType.DIFFMTB, PrimitiveType.DIFFSLR, PrimitiveType.DIFFSTB, PrimitiveType.DIFFSI_NDT, PrimitiveType.DIFFMI_NDT});
//					compatibleSites.put(PrimitiveType.IOB,
//							new PrimitiveType[]{PrimitiveType.IBUF, PrimitiveType.DIFFMLR, PrimitiveType.DIFFMTB, PrimitiveType.DIFFSLR, PrimitiveType.DIFFSTB});
//					compatibleSites.put(PrimitiveType.IOBLR,
//							new PrimitiveType[]{PrimitiveType.DIFFSLR, PrimitiveType.DIFFMLR});
//					compatibleSites.put(PrimitiveType.SLICEL,
//							new PrimitiveType[]{PrimitiveType.SLICEM});
//					break;
//				case SPARTAN3ADSP:
//					compatibleSites.put(PrimitiveType.DIFFM,
//							new PrimitiveType[]{PrimitiveType.DIFFMLR, PrimitiveType.DIFFMTB});
//					compatibleSites.put(PrimitiveType.DIFFMI,
//							new PrimitiveType[]{PrimitiveType.DIFFMLR, PrimitiveType.DIFFMTB});
//					compatibleSites.put(PrimitiveType.DIFFMI_NDT,
//							new PrimitiveType[]{PrimitiveType.DIFFMLR, PrimitiveType.DIFFMTB});
//					compatibleSites.put(PrimitiveType.DIFFS,
//							new PrimitiveType[]{PrimitiveType.DIFFSLR, PrimitiveType.DIFFSTB});
//					compatibleSites.put(PrimitiveType.DIFFSI,
//							new PrimitiveType[]{PrimitiveType.DIFFSLR, PrimitiveType.DIFFSTB});
//					compatibleSites.put(PrimitiveType.DIFFSI_NDT,
//							new PrimitiveType[]{PrimitiveType.DIFFSLR, PrimitiveType.DIFFSTB});
//					compatibleSites.put(PrimitiveType.IBUF,
//							new PrimitiveType[]{PrimitiveType.DIFFMI_NDT, PrimitiveType.DIFFSI_NDT, PrimitiveType.DIFFMLR, PrimitiveType.DIFFMTB, PrimitiveType.DIFFSLR, PrimitiveType.DIFFSTB});
//					compatibleSites.put(PrimitiveType.IOB,
//							new PrimitiveType[]{PrimitiveType.IBUF, PrimitiveType.DIFFMLR, PrimitiveType.DIFFMTB, PrimitiveType.DIFFSLR, PrimitiveType.DIFFSTB});
//					compatibleSites.put(PrimitiveType.IOBLR,
//							new PrimitiveType[]{PrimitiveType.IOBLR, PrimitiveType.DIFFMLR, PrimitiveType.DIFFSLR});
//					compatibleSites.put(PrimitiveType.SLICEL,
//							new PrimitiveType[]{PrimitiveType.SLICEM});
//					break;
//				case SPARTAN3E:
//					compatibleSites.put(PrimitiveType.DIFFMI,
//							new PrimitiveType[]{PrimitiveType.DIFFM});
//					compatibleSites.put(PrimitiveType.DIFFSI,
//							new PrimitiveType[]{PrimitiveType.DIFFS});
//					compatibleSites.put(PrimitiveType.IBUF,
//							new PrimitiveType[]{PrimitiveType.IOB, PrimitiveType.DIFFM, PrimitiveType.DIFFMI, PrimitiveType.DIFFS, PrimitiveType.DIFFSI});
//					compatibleSites.put(PrimitiveType.IOB,
//							new PrimitiveType[]{PrimitiveType.DIFFM, PrimitiveType.DIFFS});
//					compatibleSites.put(PrimitiveType.SLICEL,
//							new PrimitiveType[]{PrimitiveType.SLICEM});
//					break;
//				case SPARTAN6:
//					compatibleSites.put(PrimitiveType.BUFG,
//							new PrimitiveType[]{PrimitiveType.BUFGMUX});
//					compatibleSites.put(PrimitiveType.BUFIO2FB_2CLK,
//							new PrimitiveType[]{PrimitiveType.BUFIO2FB});
//					compatibleSites.put(PrimitiveType.BUFIO2_2CLK,
//							new PrimitiveType[]{PrimitiveType.BUFIO2});
//					compatibleSites.put(PrimitiveType.DCM_CLKGEN,
//							new PrimitiveType[]{PrimitiveType.DCM});
//					compatibleSites.put(PrimitiveType.IOB,
//							new PrimitiveType[]{PrimitiveType.IOBM, PrimitiveType.IOBS});
//					compatibleSites.put(PrimitiveType.IODRP2,
//							new PrimitiveType[]{PrimitiveType.IODELAY2});
//					compatibleSites.put(PrimitiveType.IODRP2_MCB,
//							new PrimitiveType[]{PrimitiveType.IODELAY2});
//					compatibleSites.put(PrimitiveType.ISERDES2,
//							new PrimitiveType[]{PrimitiveType.ILOGIC2});
//					compatibleSites.put(PrimitiveType.OSERDES2,
//							new PrimitiveType[]{PrimitiveType.OLOGIC2});
//					compatibleSites.put(PrimitiveType.SLICEL,
//							new PrimitiveType[]{PrimitiveType.SLICEM});
//					compatibleSites.put(PrimitiveType.SLICEX,
//							new PrimitiveType[]{PrimitiveType.SLICEL, PrimitiveType.SLICEM});
//					break;
//				case VIRTEX:
//					compatibleSites.put(PrimitiveType.IOB,
//							new PrimitiveType[]{PrimitiveType.PCIIOB});
//					break;
//				case VIRTEX2:
//					compatibleSites.put(PrimitiveType.IOB,
//							new PrimitiveType[]{PrimitiveType.DIFFM, PrimitiveType.DIFFS});
//					break;
//				case VIRTEX2P:
//					compatibleSites.put(PrimitiveType.IOB,
//							new PrimitiveType[]{PrimitiveType.DIFFM, PrimitiveType.DIFFS});
//					break;
//				case VIRTEX4:
//					compatibleSites.put(PrimitiveType.BUFG,
//							new PrimitiveType[]{PrimitiveType.BUFGCTRL});
//					compatibleSites.put(PrimitiveType.IOB,
//							new PrimitiveType[]{PrimitiveType.IOBM, PrimitiveType.IOBS, PrimitiveType.LOWCAPIOB});
//					compatibleSites.put(PrimitiveType.IOBM,
//							new PrimitiveType[]{PrimitiveType.NOMCAPIOB});
//					compatibleSites.put(PrimitiveType.IOBS,
//							new PrimitiveType[]{PrimitiveType.NOMCAPIOB});
//					compatibleSites.put(PrimitiveType.IPAD,
//							new PrimitiveType[]{PrimitiveType.IOBM, PrimitiveType.IOBS, PrimitiveType.LOWCAPIOB});
//					compatibleSites.put(PrimitiveType.ILOGIC,
//							new PrimitiveType[]{PrimitiveType.ISERDES});
//					compatibleSites.put(PrimitiveType.OLOGIC,
//							new PrimitiveType[]{PrimitiveType.OSERDES});
//					compatibleSites.put(PrimitiveType.SLICEL,
//							new PrimitiveType[]{PrimitiveType.SLICEM});
//					break;
//				case VIRTEX5:
//					compatibleSites.put(PrimitiveType.BUFG,
//							new PrimitiveType[]{PrimitiveType.BUFGCTRL});
//					compatibleSites.put(PrimitiveType.FIFO36_72_EXP,
//							new PrimitiveType[]{PrimitiveType.RAMBFIFO36});
//					compatibleSites.put(PrimitiveType.FIFO36_EXP,
//							new PrimitiveType[]{PrimitiveType.RAMBFIFO36});
//					compatibleSites.put(PrimitiveType.IOB,
//							new PrimitiveType[]{PrimitiveType.IOBM, PrimitiveType.IOBS});
//					compatibleSites.put(PrimitiveType.IPAD,
//							new PrimitiveType[]{PrimitiveType.IOBM, PrimitiveType.IOBS});
//					compatibleSites.put(PrimitiveType.ISERDES,
//							new PrimitiveType[]{PrimitiveType.ILOGIC});
//					compatibleSites.put(PrimitiveType.OSERDES,
//							new PrimitiveType[]{PrimitiveType.OLOGIC});
//					compatibleSites.put(PrimitiveType.RAMB18X2,
//							new PrimitiveType[]{PrimitiveType.RAMBFIFO36});
//					compatibleSites.put(PrimitiveType.RAMB18X2SDP,
//							new PrimitiveType[]{PrimitiveType.RAMBFIFO36});
//					compatibleSites.put(PrimitiveType.RAMB36SDP_EXP,
//							new PrimitiveType[]{PrimitiveType.RAMBFIFO36});
//					compatibleSites.put(PrimitiveType.RAMB36_EXP,
//							new PrimitiveType[]{PrimitiveType.RAMBFIFO36});
//					compatibleSites.put(PrimitiveType.RAMBFIFO18,
//							new PrimitiveType[]{PrimitiveType.RAMBFIFO36});
//					compatibleSites.put(PrimitiveType.RAMBFIFO18_36,
//							new PrimitiveType[]{PrimitiveType.RAMBFIFO36});
//					compatibleSites.put(PrimitiveType.SLICEL,
//							new PrimitiveType[]{PrimitiveType.SLICEM});
//					break;
//				case ARTIX7:
//				case KINTEX7:
//				case VIRTEX7:
//				case ZYNQ:
//					compatibleSites.put(PrimitiveType.BUFG,
//							new PrimitiveType[]{PrimitiveType.BUFGCTRL});
//					compatibleSites.put(PrimitiveType.FIFO36E1,
//							new PrimitiveType[]{PrimitiveType.RAMBFIFO36E1});
//					compatibleSites.put(PrimitiveType.ILOGICE2,
//							new PrimitiveType[]{PrimitiveType.ILOGICE3});
//					compatibleSites.put(PrimitiveType.IOB,
//							new PrimitiveType[]{PrimitiveType.IOB18, PrimitiveType.IOB18M, PrimitiveType.IOB18S, PrimitiveType.IOB33, PrimitiveType.IOB33M, PrimitiveType.IOB33S});
//					compatibleSites.put(PrimitiveType.IOB18,
//							new PrimitiveType[]{PrimitiveType.IOB18M, PrimitiveType.IOB18S});
//					compatibleSites.put(PrimitiveType.IOB33,
//							new PrimitiveType[]{PrimitiveType.IOB33M, PrimitiveType.IOB33S});
//					compatibleSites.put(PrimitiveType.IOBM,
//							new PrimitiveType[]{PrimitiveType.IOB18M, PrimitiveType.IOB33M});
//					compatibleSites.put(PrimitiveType.IOBS,
//							new PrimitiveType[]{PrimitiveType.IOB18S, PrimitiveType.IOB33S});
//					compatibleSites.put(PrimitiveType.IPAD,
//							new PrimitiveType[]{PrimitiveType.IOB18M, PrimitiveType.IOB18S, PrimitiveType.IOB33M, PrimitiveType.IOB33S});
//					compatibleSites.put(PrimitiveType.ISERDESE2,
//							new PrimitiveType[]{PrimitiveType.ILOGICE2, PrimitiveType.ILOGICE3});
//					compatibleSites.put(PrimitiveType.OLOGICE2,
//							new PrimitiveType[]{PrimitiveType.OLOGICE3});
//					compatibleSites.put(PrimitiveType.OSERDESE2,
//							new PrimitiveType[]{PrimitiveType.OLOGICE2, PrimitiveType.OLOGICE3});
//					compatibleSites.put(PrimitiveType.PHASER_IN,
//							new PrimitiveType[]{PrimitiveType.PHASER_IN_PHY});
//					compatibleSites.put(PrimitiveType.PHASER_IN_ADV,
//							new PrimitiveType[]{PrimitiveType.PHASER_IN_PHY});
//					compatibleSites.put(PrimitiveType.PHASER_OUT,
//							new PrimitiveType[]{PrimitiveType.PHASER_OUT_PHY});
//					compatibleSites.put(PrimitiveType.PHASER_OUT_ADV,
//							new PrimitiveType[]{PrimitiveType.PHASER_OUT_PHY});
//					compatibleSites.put(PrimitiveType.RAMB18E1,
//							new PrimitiveType[]{PrimitiveType.FIFO18E1});
//					compatibleSites.put(PrimitiveType.RAMB36E1,
//							new PrimitiveType[]{PrimitiveType.RAMBFIFO36E1});
//					compatibleSites.put(PrimitiveType.SLICEL,
//							new PrimitiveType[]{PrimitiveType.SLICEM});
//					break;
//				case VIRTEXE:
//					compatibleSites.put(PrimitiveType.IOB,
//							new PrimitiveType[]{PrimitiveType.DLLIOB, PrimitiveType.PCIIOB});
//					break;
//				default:
//					break;
//			}
//			compatibleTypesArray[specificFamilyType.ordinal()] = compatibleSites;
//		}
//	}

	/*
	   Class and method for optimized Hessian serialization.
	 */
	private static class PrimitiveSiteReplace implements Serializable {
		/** Name of the primitive site with X and Y coordinates (ie. SLICE_X0Y0) */
		private String name;
		private SiteType[] possibleTypes;
		private Map<SiteType, Map<String, Integer>> externalWires;
		private BondedType bondedType;

		@SuppressWarnings("UnusedDeclaration")
		private Site readResolve() {
			Site site = new Site();
			site.setName(name);
			site.possibleTypes = possibleTypes;
			site.externalWires = externalWires;
			site.bondedType = bondedType;
			return site;
		}
	}

	@SuppressWarnings("UnusedDeclaration")
	private PrimitiveSiteReplace writeReplace() {
		PrimitiveSiteReplace repl = new PrimitiveSiteReplace();
		repl.name = name;
		repl.possibleTypes = possibleTypes;
		repl.externalWires = externalWires;
		repl.bondedType = bondedType;
		return repl;
	}
}
