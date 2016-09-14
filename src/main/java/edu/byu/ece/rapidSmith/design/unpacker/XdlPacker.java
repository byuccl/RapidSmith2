package edu.byu.ece.rapidSmith.design.unpacker;

import edu.byu.ece.rapidSmith.design.*;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.design.xdl.*;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.util.FamilyType;
import edu.byu.ece.rapidSmith.util.luts.LutConfig;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;

/**
 *
 */
public class XdlPacker {
	private Map<String, Map<BelId, Map<String, String>>> renameMap;
	private Map<String, Map<BelId, Map<String, String>>> valuesMap;
	private PackerUtils packerUtils;

	private Design packedDesign;

	private Set<PIP> pipSet;
	private Set<BelPin> pinSet;
	private Set<Wire> wiresUsedInRoute;

	public XdlPacker(Path packerFile)
			throws JDOMException, IOException {
		renameMap = new HashMap<>();
		valuesMap = new HashMap<>();
		Document doc = loadPackerFile(packerFile);
		getUtilsClass(doc);
		loadRenameMap(doc);
	}

	private Document loadPackerFile(Path packerFile) throws JDOMException, IOException {
		// load the converter document
		Document doc;
		SAXBuilder builder = new SAXBuilder();
		doc = builder.build(packerFile.toFile());
		return doc;
	}

	private void getUtilsClass(Document doc) {
		String creatorClassName = doc.getRootElement().getChildText("utils_class");

		try {
			Class<?> clazz = Class.forName(creatorClassName);
			Constructor<?> ctor = clazz.getConstructors()[0];
			Object obj = ctor.newInstance();
			packerUtils = (PackerUtils) obj;
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void loadRenameMap(Document doc) throws JDOMException, IOException {
		Element rootEl = doc.getRootElement();
		for (Element cellEl : rootEl.getChildren("cell")) {
			Map<BelId, Map<String, String>> belsMap = new HashMap<>();
			Map<BelId, Map<String, String>> valueMap = new HashMap<>();
			for (Element belEl : cellEl.getChildren("bel")) {
				Element idEl = belEl.getChild("id");
				BelId belId = new BelId(SiteType.valueOf(idEl.getChildText("primitive_type")),
						idEl.getChildText("name"));
				Map<String, String> attrsMap = new HashMap<>();
				Map<String, String> valueUpdateMap = new HashMap<>();
				for (Element attrEl : belEl.getChild("attributes").getChildren("attribute")) {
					String attrName = attrEl.getChildText("name");
					String attrRename = attrEl.getChildText("rename");
					String value = attrEl.getChildText("value");
					if (attrRename == null)
						attrRename = attrName;
					attrsMap.put(attrName, attrRename);
					if (value != null)
						valueUpdateMap.put(attrName, value);

				}
				belsMap.put(belId, attrsMap);
				valueMap.put(belId, valueUpdateMap);
			}
			renameMap.put(cellEl.getChildText("name"), belsMap);
			valuesMap.put(cellEl.getChildText("name"), valueMap);
		}
	}

	public Design pack(CellDesign cellDesign) {
		packedDesign = new Design(cellDesign.getName(), cellDesign.getPartName());
		packedDesign.setNCDVersion(Design.DEFAULT_NCD_VERSION);

		packerUtils.prepare(cellDesign);
		packDesignAttributes(cellDesign);
		setPrimitiveTypes(cellDesign);
		buildInstances(cellDesign);
		buildNets(cellDesign);
		packerUtils.finish(packedDesign);
		return packedDesign;
	}

	private boolean attributesConflict(Instance inst, Property property, String attrName) {
		return inst.hasAttribute(attrName) &&
				!Objects.equals(inst.getAttributeValue(attrName), property.getValue());
	}

	private void setPrimitiveTypes(CellDesign design) {
		for (Site site : design.getUsedSites()) {
			SiteType type = null;
			for (Cell cell : design.getCellsAtSite(site)) {
				Bel bel = cell.getAnchor();
				SiteType belType = bel.getSite().getType();
				if (type == null) {
					type = belType;
					site.setType(type);
				} else if (type != belType) {
					throw new AssertionError("BELs of different site types in the same site detected");
				}
			}
		}
	}

	private void buildInstances(CellDesign cellDesign) {
		// Since instance map to primitive sites, we'll build the instance by
		// collecting all the BELs at each site
		for (Site site : cellDesign.getUsedSites()) {
			Instance inst = new Instance();
			// guaranteed to be unique but it will probably not be identical to
			// the original XDL
			inst.setName(site.getName());
			inst.setType(site.getType());

			// Now pack all the cells in the site into the instance
			for (Cell cell : cellDesign.getCellsAtSite(site)) {
				packCell(inst, cell);
			}

			packedDesign.addInstance(inst);
			inst.place(site);
		}
	}

	private void packCell(Instance inst, Cell cell) {
		// BELs in the site should all be of the same primitive type
		assert cell.getAnchor().getId().getPrimitiveType() == inst.getType();

		BelId belId = cell.getAnchor().getId();
		String belName = belId.getName();
		String cellType = cell.getLibCell().getName();

		updatePropertyValues(cell, belId, cellType);
		// Add the BEL attribute for this cell
		inst.addAttribute(belName, cell.getName(), cell.getPropertyValue(cellType).toString());
		packCellProperties(inst, cell, belId, cellType);
	}

	// Correct the values when the placement of the cell dictates
	// the value e.g HARD1 placed on LUT needs a LUT equation
	private void updatePropertyValues(Cell cell, BelId belId, String cellType) {
		if (cell.getLibCell().isLut())
			updateLutPins(cell);

		Map<String, String> valueMap = valuesMap.get(cellType).get(belId);
		for (Property property : cell.getProperties()) {
			if (property.getType() != PropertyType.DESIGN)
				continue;

			String propertyName = property.getKey().toString();

			if (valueMap.containsKey(propertyName))
				property.setValue(valueMap.get(propertyName));
		}
	}

	private void updateLutPins(Cell cell) {
		int[] remap = new int[6];
		for (CellPin cellPin : cell.getPins()) {
			if (cellPin.isOutpin() || cellPin.getNet() == null || cellPin.getName().length() != 2)
				continue;
			int pinIndex = cellPin.getName().charAt(1) - '1';
			if (pinIndex < 0 || pinIndex > 5)
				continue;
			remap[(pinIndex)] = cellPin.getBelPin().getName().charAt(1) - '0';
		}
		String cellName = cell.getLibCell().getName();
		LutConfig config = (LutConfig) cell.getPropertyValue(cellName);
		config.remapPins(remap);
		if (cellName.startsWith("LUT")) {
			config.setOutputPinName("O" + cell.getAnchor().getName().charAt(1));
		}
	}

	private void packCellProperties(Instance inst, Cell cell, BelId belId, String cellType) {
		// Add the cell's properties as an attribute
		Map<String, String> attrsMap = renameMap.get(cellType).get(belId);
		for (Property property : cell.getProperties()) {
			String propertyName = property.getKey().toString();
			// This is the property for the BEL itself
			if (propertyName.equals(cellType) || !property.getType().equals(PropertyType.DESIGN))
				continue;

			// handles FFINIT -> AFFINIT type mappings
			String attrName = attrsMap.get(propertyName);

			assert attrName != null : "attribute rename map incomplete";
			assert !attributesConflict(inst, property, attrName) : "attribute conflict detected";

			String propertyValue = property.getValue().toString();
			inst.addAttribute(attrName, "", propertyValue);
		}
	}

	private void packDesignAttributes(CellDesign cellDesign) {
		// TODO create the needed attributes
//		for (Attribute attr : cellDesign.getAttributes())
//			packedDesign.addAttribute(attr.getPhysicalName(), attr.getValue());
	}

	private void buildNets(CellDesign cellDesign) {
		for (CellNet cellNet : cellDesign.getNets()) {
			// all unvisited pips
			pipSet = new HashSet<>(cellNet.getPips());
			// all of the sink pins of the net
			pinSet = new HashSet<>();
			for (CellPin pin : cellNet.getPins()) {
				if (!pin.isInpin())
					continue;
				assert pin.getBelPin() != null;
				pinSet.add(pin.getBelPin());
			}

			// starting from the source, traverse out and find all of the sinks.
			// Need to identify the site pins
			wiresUsedInRoute = new HashSet<>();
			CellPin sourcePin = cellNet.getSourcePin();
			Net net;
			if (sourcePin == null) {
				if (cellNet.getType() == NetType.GND) {
					net = new Net("global_gnd_net", NetType.GND);
				} else if (cellNet.getType() == NetType.VCC) {
					net = new Net("global_vcc_net", NetType.VCC);
				} else {
					throw new AssertionError("Unsourced net must be GND or VCC");
				}
			} else if (sourcePin.getBelPin() == null) {
				assert sourcePin.getCell().getLibCell().getName().equals("TIEOFF");

				if (sourcePin.getName().equals("0")) {
					net = new Net("global_gnd_net", NetType.GND);
				} else { // HARD1
					net = new Net("global_vcc_net", NetType.VCC);
				}
			} else {
				SiteWire sourceWire = sourcePin.getBelPin().getWire();
				NetBooleanPair netBooleanPair = traverseSourceSite(cellNet, sourceWire);
				if (netBooleanPair != null)
					net = netBooleanPair.net;
				else net = null;
			}

			if (net == null)
				continue;

			// find the sitepin source of any remaining belPins and add it to the returned net
			while (!pinSet.isEmpty()) {
				BelPin belPin = pinSet.iterator().next();
				findRouteToBelSink(net, belPin);
				pinSet.remove(belPin);
			}
			// adding the sink pins may have changed it so add the net
			if (packedDesign.getNet(net.getName()) == null && net.getPins().size() > 1)
				packedDesign.addNet(net);
		}
	}

	private void findRouteToBelSink(Net net, BelPin belPin) {
		Instance inst = packedDesign.getInstanceAtPrimitiveSite(belPin.getBel().getSite());
		for (SitePin sinkPin : belPin.getSitePins()) {
			if (traverseSinkSite(sinkPin.getInternalWire())) {
				net.addPin(new Pin(false, sinkPin.getName(), inst));
			}
		}
	}

	// find the site pin sourcing the net
	private NetBooleanPair traverseSourceSite(CellNet cellNet, Wire sourceWire) {
		Site site = sourceWire.getSite();
		Instance inst = packedDesign.getInstanceAtPrimitiveSite(site);
		int numNets = 0;
		NetBooleanPair preferredNet = null;

		// prevents following routes in circles, needed to handle long lines
		wiresUsedInRoute.add(sourceWire);

		for (Connection c : sourceWire.getWireConnections()) {
			Wire sinkWire = c.getSinkWire();
			if (wiresUsedInRoute.contains(sinkWire))
				continue;

			if (c.isPip()) {
				// if pip is not in the route, don't follow it
				SitePip sitePip = (SitePip) c.getPip();
				if (!pipSet.remove(sitePip))
					continue;

				// Add the site PIP attribute to the instance
				SiteTemplate template = packedDesign.getDevice().getSiteTemplate(inst.getType());
				inst.addAttribute(new Attribute(template.getPipAttribute(sitePip)));
			}

			SitePin sitePin = site.getSitePinOfInternalWire(sinkWire.getWireEnum());
			if (sitePin != null) {
				Net net = connectToSitePin(cellNet, site, inst, sitePin);

				// If a sink is found in traversing this net, then the net is
				// worth adding to the design.
				// The preferred net is the net we'll use to source any unsourced sinks
				// for a non-fully routed net.  Hopefully there will only be one.
				if (net.getPins().size() > 1)
					packedDesign.addNet(net);

				if (preferredNet == null || !preferredNet.drivesGeneralFabric) {
					preferredNet = new NetBooleanPair(net, sitePin.drivesGeneralFabric());
				}
				numNets++;
			} else {
				// follow the site to the sinks if the net is routed
				NetBooleanPair pair = traverseSourceSite(cellNet, sinkWire);
				if (pair != null) {
					Net net = pair.net;
					if (net.getPins().size() > 1)
						packedDesign.addNet(net);
					if (preferredNet == null || !preferredNet.drivesGeneralFabric) {
						preferredNet = pair;
					}
				}
			}
		}
		wiresUsedInRoute.remove(sourceWire);

		// Add the net
		if (preferredNet != null) {
			Net net = preferredNet.net;
			if (net.getPins().size() > 1 && packedDesign.getNet(net.getName()) == null)
				packedDesign.addNet(net);
		}
		if (numNets > 1 && !pinSet.isEmpty())
			System.out.println("Multiple possible nets for pins.  Output may not be accurate");
		return preferredNet;
	}

	private static class NetBooleanPair {
		Net net;
		boolean drivesGeneralFabric;

		public NetBooleanPair(Net net, boolean drivesGeneralFabric) {
			this.net = net;
			this.drivesGeneralFabric = drivesGeneralFabric;
		}
	}

	private Net connectToSitePin(CellNet cellNet, Site site,
			Instance inst, SitePin sitePin)
	{
		Net net = new Net();
		net.setName(site.getName() + "_" + sitePin.getName());
		net.setType(determineNetType(cellNet));

		// Add the source pin to the net
		Pin pin = new Pin(true, sitePin.getName(), inst);
		net.addPin(pin);

		// Traverse the net to find all of the sinks
		traverseGeneralFabric(net, sitePin, sitePin.getExternalWire());

		return net;
	}

	private NetType determineNetType(CellNet cellNet) {
		// TODO make generic
		String sourceType = cellNet.getSourcePin().getCell().getLibCell().getName();
		switch (sourceType) {
			case "HARD1":
				return NetType.VCC;
			case "HARD0":
				return NetType.GND;
			default:
				return NetType.WIRE;
		}
	}

	// traverse the general fabric looking for sites
	private void traverseGeneralFabric(Net net, SitePin sourcePin, Wire sourceWire) {
		traverseGeneralFabric(net, sourceWire);

		if (isSliceCarrySource(sourcePin)) {
			for (BelPin sinkPin : new ArrayList<>(pinSet)) {
				if (isSliceCarrySink(sinkPin)) {
					findRouteToBelSink(net, sinkPin);
				}
			}
		}
		wiresUsedInRoute.remove(sourceWire);
	}

	// traverse the general fabric looking for sites
	private void traverseGeneralFabric(Net net, Wire sourceWire) {
		wiresUsedInRoute.add(sourceWire);
		for (Connection c : sourceWire.getWireConnections()) {
			Wire sinkWire = c.getSinkWire();

			if (wiresUsedInRoute.contains(sinkWire))
				continue;

			if (c.isPip()) {
				// if the pip is not in the route, don't follow it
				PIP pip = c.getPip();
				if (!pipSet.remove(pip))
					continue;

				// following the route found the pip so add it
				net.addPIP(pip);
			}

			// Checks if this wire connection connects to a sink pin
			SitePin sinkPin = sinkWire.getTile().getSitePinOfWire(sinkWire.getWireEnum());
			if (sinkPin != null) {
				// Only continue if the sink pin is on a used site
				Instance inst = packedDesign.getInstanceAtPrimitiveSite(sinkPin.getSite());
				if (inst != null) {
					// follow the wire through the site and add it if it connects to a BEL pin
					if (traverseSinkSite(sinkPin.getInternalWire())) {
						net.addPin(new Pin(false, sinkPin.getName(), inst));
					}
				}
			}

			traverseGeneralFabric(net, sinkWire);
		}

		wiresUsedInRoute.remove(sourceWire);
	}

	private static boolean isSliceCarrySource(SitePin sourcePin) {
		Site site = sourcePin.getSite();
		FamilyType family = site.getTile().getDevice().getFamilyType();
		switch (family) {
			case VIRTEX6:
				SiteType siteType = site.getType();
				if (siteType == SiteType.SLICEL || siteType == SiteType.SLICEM) {
					return sourcePin.getName().equals("COUT");
				}
		}
		return false;
	}

	private boolean isSliceCarrySink(BelPin sinkPin) {
		return sinkPin.getBel().getName().equals("CARRY4") && sinkPin.getName().equals("CIN");
	}

	private boolean traverseSinkSite(Wire sourceWire) {
		wiresUsedInRoute.add(sourceWire);

		Site site = sourceWire.getSite();
		// find the instance of this site
		Instance inst = packedDesign.getInstanceAtPrimitiveSite(site);
		assert inst != null;

		boolean reachesSiteSink = false;

		BelPin belPin = site.getBelPinOfWire(sourceWire.getWireEnum());
		if (belPin != null) {
			if (pinSet.remove(belPin)) {
				reachesSiteSink = true;
			}
		}

		for (Connection c : sourceWire.getWireConnections()) {
			Wire sinkWire = c.getSinkWire();

			if (wiresUsedInRoute.contains(sinkWire))
				continue;
			if (c.isPip()) {
				// if pip is not in the route, don't follow it
				SitePip sitePip = (SitePip) c.getPip();
				if (!pipSet.remove(sitePip))
					continue;

				// the pip was found so add it as an attribute to the instance
				SiteTemplate template = packedDesign.getDevice().getSiteTemplate(inst.getType());
				inst.addAttribute(new Attribute(template.getPipAttribute(sitePip)));
			}

			if (traverseSinkSite(sinkWire))
				reachesSiteSink = true;
		}

		wiresUsedInRoute.remove(sourceWire);
		return reachesSiteSink;
	}
}
