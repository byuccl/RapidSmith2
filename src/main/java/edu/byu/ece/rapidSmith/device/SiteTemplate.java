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

package edu.byu.ece.rapidSmith.device;

import edu.byu.ece.rapidSmith.design.xdl.XdlAttribute;

import java.io.Serializable;
import java.util.*;

/**
 *  Template to back sites that contains information common to
 *  all sites of a specific type.
 */
public final class SiteTemplate implements Serializable {
	private static final long serialVersionUID = -5857292063197088427L;
	// The type of this site template
	private SiteType type;
	// The templates for the BELs in this site template
	private Map<String, BelTemplate> belTemplates;
	// Site types that can be placed on sites of this type
	private SiteType[] compatibleTypes;
	// The intrasite routing graph structure
	private WireHashMap routing;
	private WireHashMap reverseWireConnections;
	// Map of pin names to pin templates for the source pins
	private Map<String, SitePinTemplate> sources;
	// Map of pin names to pin templates for the sink pins
	private Map<String, SitePinTemplate> sinks;
	// Map of site wires to the pin templates the wires connect to
	private transient Map<Integer, SitePinTemplate> internalWireToSitePinMap;
	// Map of the site wires to the bel pin templates the wire connect to
	private transient Map<Integer, BelPinTemplate> belPins;
	// Map of XDL attributes that should be created for each PIP
	private Map<Integer, Map<Integer, XdlAttribute>> pipAttributes;
	// Map containing the bel routethrough information of the site
	private Map<Integer, Set<Integer>> belRoutethroughMap;


	public SiteType getType() {
		return type;
	}

	public void setType(SiteType type) {
		this.type = type;
	}

	public Map<String, BelTemplate> getBelTemplates() {
		return belTemplates;
	}

	public void setBelTemplates(Map<String, BelTemplate> belTemplates) {
		this.belTemplates = belTemplates;
	}

	public SiteType[] getCompatibleTypes() {
		return compatibleTypes;
	}

	public void setCompatibleTypes(SiteType[] compatibleTypes) {
		this.compatibleTypes = compatibleTypes;
	}

	public WireHashMap getRouting() {
		return routing;
	}

	public void setRouting(WireHashMap routing) {
		this.routing = routing;
	}

	public Set<Integer> getWires() {
		return routing.keySet();
	}

	public WireConnection[] getWireConnections(int wire) {
		return routing.get(wire);
	}

	public Map<String, SitePinTemplate> getSources() {
		return sources;
	}

	public void setSources(Map<String, SitePinTemplate> sources) {
		this.sources = sources;
	}

	public Map<String, SitePinTemplate> getSinks() {
		return sinks;
	}

	public void setSinks(Map<String, SitePinTemplate> sinks) {
		this.sinks = sinks;
	}

	/**
	 * @deprecated Use {@link #getInternalWireToSitePinMap} instead.
	 */
	@Deprecated
	public Map<Integer, SitePinTemplate> getInternalSiteWireMap() {
		return getInternalWireToSitePinMap();
	}
	
	public Map<Integer, SitePinTemplate> getInternalWireToSitePinMap() {
		return internalWireToSitePinMap;
	}

	public Map<Integer, BelPinTemplate> getBelPins() {
		return belPins;
	}

	public void setBelPins(Map<Integer, BelPinTemplate> belPins) {
		this.belPins = belPins;
	}

	/**
	 * @deprecated Use {@link #setInternalWireToSitePinMap} instead.
	 */
	@Deprecated
	public void setInternalSiteWireMap(Map<Integer, SitePinTemplate> internalWireToSitePinMap) {
		setInternalWireToSitePinMap(internalWireToSitePinMap);
	}
	
	public void setInternalWireToSitePinMap(Map<Integer, SitePinTemplate> internalWireToSitePinMap) {
		this.internalWireToSitePinMap = internalWireToSitePinMap;
	}

	public Map<Integer, Map<Integer, XdlAttribute>> getPipAttributes() {
		return pipAttributes;
	}

	public XdlAttribute getPipAttribute(PIP pip) {
		return pipAttributes.get(pip.getStartWire().getWireEnum())
				.get(pip.getEndWire().getWireEnum());
	}

	public void setPipAttributes(Map<Integer, Map<Integer, XdlAttribute>> pipAttributes) {
		this.pipAttributes = pipAttributes;
	}

	public void setBelRoutethroughs(Map<Integer, Set<Integer>> belRoutethroughs) {
		this.belRoutethroughMap = belRoutethroughs;
	}

	public boolean isRoutethrough(Integer startWire, Integer endWire) {

		if (belRoutethroughMap == null) {
			return false;
		}

		Set<Integer> sinks = belRoutethroughMap.get(startWire);
		return sinks != null && sinks.contains(endWire);
	}

	@Override
	public String toString() {
		return "SiteTemplate{" +
				"type=" + type +
				'}';
	}

	// Builds fast lookup structures for data that is already provided in a
	// different structure
	void constructDependentResources() {
		// Create the internal site wire map by grabbing the internal wires
		// for both the source and sink pins
		internalWireToSitePinMap = new HashMap<>();
		for (SitePinTemplate sitePin : sources.values()) {
			internalWireToSitePinMap.put(sitePin.getInternalWire(), sitePin);
		}
		for (SitePinTemplate sitePin : sinks.values()) {
			internalWireToSitePinMap.put(sitePin.getInternalWire(), sitePin);
		}

		// Create the wire to bel pin maps by inferringthe information from the
		// bel pin templates
		belPins = new HashMap<>();
		for (BelTemplate belTemplate : belTemplates.values()) {
			for (BelPinTemplate belPin : belTemplate.getSources().values()) {
				belPins.put(belPin.getWire(), belPin);
			}
			for (BelPinTemplate belPin : belTemplate.getSinks().values()) {
				belPins.put(belPin.getWire(), belPin);
			}
		}
	}

	// Convenience method to search both source and sink site pins
	public SitePinTemplate getSitePin(String name) {
		if (sources.containsKey(name))
			return sources.get(name);
		if (sinks.containsKey(name))
			return sinks.get(name);
		return null;
	}

	public void setReverseWireConnections(WireHashMap reverseWireConnections) {
		this.reverseWireConnections = reverseWireConnections;
	}

	public WireConnection[] getReverseWireConnections(int wire) {
		return reverseWireConnections.get(wire);
	}

	public WireHashMap getReversedWireHashMap() {
		return reverseWireConnections;
	}

	// for hessian compression
	private static class SiteTemplateReplace implements Serializable  {
		private static final long serialVersionUID = 4409516349602480310L;
		private SiteType type;
		private Collection<BelTemplate> belTemplates;
		private SiteType[] compatibleTypes;
		private WireHashMap routing;
		private WireHashMap reverseWireConnections;
		private Collection<SitePinTemplate> sources;
		private Collection<SitePinTemplate> sinks;
		private Map<Integer, Map<Integer, XdlAttribute>> pipAttributes;
		private Map<Integer, Set<Integer>> belRoutethroughMap;

		public Object readResolve() {
			SiteTemplate template = new SiteTemplate();
			template.type = type;
			if (belTemplates != null) {
				template.belTemplates = new HashMap<>();
				for (BelTemplate belTemplate : belTemplates) {
					template.belTemplates.put(belTemplate.getId().getName(), belTemplate);
				}
			}
			template.compatibleTypes = compatibleTypes;
			template.routing = routing;
			template.reverseWireConnections = reverseWireConnections;
			if (sources != null) {
				template.sources = new HashMap<>();
				for (SitePinTemplate pin : sources) {
					template.sources.put(pin.getName(), pin);
				}
			}

			if (sinks != null) {
				template.sinks = new HashMap<>();
				for (SitePinTemplate pin : sinks) {
					template.sinks.put(pin.getName(), pin);
				}
			}
			template.pipAttributes = pipAttributes;
			template.belRoutethroughMap = belRoutethroughMap;

			return template;
		}
	}

	public SiteTemplateReplace writeReplace() {
		SiteTemplateReplace repl = new SiteTemplateReplace();
		repl.type = type;
		repl.belTemplates = belTemplates.values();
		repl.compatibleTypes = compatibleTypes;
		repl.routing = routing;
		repl.reverseWireConnections = reverseWireConnections; 
		repl.sources = sources.values();
		repl.sinks = sinks.values();
		repl.pipAttributes = pipAttributes;
		repl.belRoutethroughMap = belRoutethroughMap;

		return repl;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SiteTemplate template = (SiteTemplate) o;
		return Objects.equals(type, template.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type);
	}
}
