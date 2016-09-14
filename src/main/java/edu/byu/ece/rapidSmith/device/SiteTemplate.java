package edu.byu.ece.rapidSmith.device;

import edu.byu.ece.rapidSmith.design.xdl.Attribute;
import edu.byu.ece.rapidSmith.design.subsite.SitePip;

import java.io.Serializable;
import java.util.*;

/**
 *  Template to back primitive sites that contains information common to
 *  all primitive sites of a specific type.
 */
public final class SiteTemplate implements Serializable {
	// The type of this site template
	private SiteType type;
	// The templates for the BELs in this site template
	private Map<String, BelTemplate> belTemplates;
	// Primitive types that can be placed on sites of this type
	private SiteType[] compatibleTypes;
	// The intrasite routing graph structure
	private WireHashMap routing;
	private WireHashMap reverseWireConnections;
	// Map of pin names to pin templates for the source pins
	private Map<String, SitePinTemplate> sources;
	// Map of pin names to pin templates for the sink pins
	private Map<String, SitePinTemplate> sinks;
	// Map of site wires to the pin templates the wires connect to
	private transient Map<Integer, SitePinTemplate> internalSiteWireMap;
	// Map of the site wires to the bel pin templates the wire connect to
	private transient Map<Integer, BelPinTemplate> belPins;
	// Map of XDL attributes that should be created for each PIP
	private Map<Integer, Map<Integer, Attribute>> pipAttributes;

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

	public Map<Integer, SitePinTemplate> getInternalSiteWireMap() {
		return internalSiteWireMap;
	}

	public Map<Integer, BelPinTemplate> getBelPins() {
		return belPins;
	}

	public void setBelPins(Map<Integer, BelPinTemplate> belPins) {
		this.belPins = belPins;
	}

	public void setInternalSiteWireMap(Map<Integer, SitePinTemplate> internalSiteWireMap) {
		this.internalSiteWireMap = internalSiteWireMap;
	}

	public Map<Integer, Map<Integer, Attribute>> getPipAttributes() {
		return pipAttributes;
	}

	public Attribute getPipAttribute(SitePip pip) {
		return pipAttributes.get(pip.getStartWire()).get(pip.getEndWire());
	}

	public void setPipAttributes(Map<Integer, Map<Integer, Attribute>> pipAttributes) {
		this.pipAttributes = pipAttributes;
	}

	@Override
	public String toString() {
		return "PrimitiveTemplate{" +
				"type=" + type +
				'}';
	}

	// Builds fast lookup structures for data that is already provided in a
	// different structure
	void constructDependentResources() {
		// Create the internal site wire map by grabbing the internal wires
		// for both the source and sink pins
		internalSiteWireMap = new HashMap<>();
		for (SitePinTemplate sitePin : sources.values()) {
			internalSiteWireMap.put(sitePin.getInternalWire(), sitePin);
		}
		for (SitePinTemplate sitePin : sinks.values()) {
			internalSiteWireMap.put(sitePin.getInternalWire(), sitePin);
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
		private SiteType type;
		private Collection<BelTemplate> belTemplates;
		private SiteType[] compatibleTypes;
		private WireHashMap routing;
		private Collection<SitePinTemplate> sources;
		private Collection<SitePinTemplate> sinks;
		private Map<Integer, Map<Integer, Attribute>> pipAttributes;

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

			return template;
		}
	}

	public SiteTemplateReplace writeReplace() {
		SiteTemplateReplace repl = new SiteTemplateReplace();
		repl.type = type;
		repl.belTemplates = belTemplates.values();
		repl.compatibleTypes = compatibleTypes;
		repl.routing = routing;
		repl.sources = sources.values();
		repl.sinks = sinks.values();
		repl.pipAttributes = pipAttributes;

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
