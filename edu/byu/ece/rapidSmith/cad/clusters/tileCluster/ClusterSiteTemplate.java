package edu.byu.ece.rapidSmith.cad.clusters.tileCluster;

import edu.byu.ece.rapidSmith.device.*;

import java.util.Map;

/**
 *
 */
public class ClusterSiteTemplate {
	private PrimitiveType type;
	private Map<String, BelTemplate> belTemplates;
	private WireHashMap directRouting;
	private WireHashMap circularRouting;
	private WireHashMap reverseRouting;
	// Map of pin names to pin templates for the source pins
	private Map<String, SitePinTemplate> sources;
	// Map of pin names to pin templates for the sink pins
	private Map<String, SitePinTemplate> sinks;
	// Map of site wires to the pin templates the wires connect to
	private Map<Integer, SitePinTemplate> internalSiteWireMap;
	// Map of the site wires to the bel pin templates the wire connect to
	private Map<Integer, BelPinTemplate> belPins;

	public PrimitiveType getType() {
		return type;
	}

	public void setType(PrimitiveType type) {
		this.type = type;
	}

	public Map<String, BelTemplate> getBelTemplates() {
		return belTemplates;
	}

	public void setBelTemplates(Map<String, BelTemplate> belTemplates) {
		this.belTemplates = belTemplates;
	}

	public WireHashMap getDirectRouting() {
		return directRouting;
	}

	public void setDirectRouting(WireHashMap directRouting) {
		this.directRouting = directRouting;
	}

	public WireHashMap getCircularRouting() {
		return circularRouting;
	}

	public void setCircularRouting(WireHashMap circularRouting) {
		this.circularRouting = circularRouting;
	}

	public WireHashMap getReverseRouting() {
		return reverseRouting;
	}

	public void setReverseRouting(WireHashMap reverseRouting) {
		this.reverseRouting = reverseRouting;
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

	public void setInternalSiteWireMap(Map<Integer, SitePinTemplate> internalSiteWireMap) {
		this.internalSiteWireMap = internalSiteWireMap;
	}

	public Map<Integer, BelPinTemplate> getBelPins() {
		return belPins;
	}

	public void setBelPins(Map<Integer, BelPinTemplate> belPins) {
		this.belPins = belPins;
	}
}
