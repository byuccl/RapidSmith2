package edu.byu.ece.rapidSmith.design.unpacker;

import edu.byu.ece.rapidSmith.design.*;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class XdlUnpacker {
	private UnpackerUtils unpackerUtils;
	private Map<BelId, CellCreatorFactory> cellCreatorFactories;

	private Device device;
	private CellDesign cellDesign;
	private boolean preserveRouting;

	private Map<SiteType, Site> siteTemplates;
	private Map<Cell, CellCreator> cellCreators;

	private Map<Net, CellNet> netMap;
	private Map<Bel, Cell> instanceBelToCellMap;

	private CellNet cellNet;
	private CellNet gndNet;
	private CellNet vccNet;

	public XdlUnpacker(CellLibrary cellLibrary, Path unpackerFile)
			throws JDOMException, IOException {
		Document doc = loadUnpackerFile(unpackerFile);
		getUtilsClass(doc);
		cellCreatorFactories = CellCreatorFactoryFactory.createCellCreators(doc, cellLibrary);
	}

	private Document loadUnpackerFile(Path unpackerFile)
			throws JDOMException, IOException {
		// load the converter document
		Document doc;
		SAXBuilder builder = new SAXBuilder();
		doc = builder.build(unpackerFile.toFile());
		return doc;
	}

	private void getUtilsClass(Document doc) {
		String creatorClassName = doc.getRootElement().getChildText("utils_class");

		try {
			Class<?> clazz = Class.forName(creatorClassName);
			Constructor<?> ctor = clazz.getConstructors()[0];
			Object obj = ctor.newInstance();
			unpackerUtils = (UnpackerUtils) obj;
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public CellDesign unpack(Design design) {
		return unpack(design, true);
	}

	private void loadSiteTemplates() {
		siteTemplates = new HashMap<>();
		for (Site site : device.getPrimitiveSites().values()) {
			for (SiteType possible : site.getPossibleTypes()) {
				siteTemplates.put(possible, site);
			}
		}
	}

	public CellDesign unpack(Design design, boolean preserveRouting) {
		device = design.getDevice();
		cellCreators = new HashMap<>();

		loadSiteTemplates();

		this.preserveRouting = preserveRouting;

		design = unpackerUtils.prepareForUnpacker(design);

		cellDesign = new CellDesign(design.getName(), design.getPartName());

		// Let's start by mapping over all of the nets in the design
		setPrimitiveTypes(design);
		createAndAddGlobalNets(design);
		createAndAddInstances(design);
		cleanupSinglePinNets();

		// Clean up unneeded class variables
		siteTemplates = null;
		cellCreators = null;
		netMap = null;
		instanceBelToCellMap = null;
		device = null;

		CellDesign ret = cellDesign;
		cellDesign = null;

		return ret;
	}

	private void setPrimitiveTypes(Design design) {
		// Sets the type of the primitive site this pin exists on
		for (Instance inst : design.getInstances()) {
			if (inst.isPlaced())
				inst.getPrimitiveSite().setType(inst.getType());
		}
	}

	private void createAndAddGlobalNets(Design design) {
		netMap = new HashMap<>();
		for (Net net : design.getNets()) {
			// Don't deal with pin-less nets, most likely they are the IO pseudo nets
			if (net.getPins().isEmpty())
				continue;

			CellNet cellNet;
			if (preserveRouting || net.getType() == NetType.WIRE) {
				cellNet = new CellNet(net.getName(), net.getType());

				// add all routing
				buildRouteTreeFromPips(net.getSource(), net.getPIPs());
			} else if (net.getType() == NetType.GND) {
				if (gndNet == null) {
					gndNet = new CellNet(net.getName(), NetType.GND);
					cellDesign.addNet(gndNet);
				}
				cellNet = gndNet;
			} else if (net.getType() == NetType.VCC) {
				if (vccNet == null) {
					vccNet = new CellNet(net.getName(), NetType.VCC);
					cellDesign.addNet(vccNet);
				}
				cellNet = vccNet;
			} else {
				throw new UnsupportedOperationException("Unsupported net type.");
			}
			if (!cellNet.isStaticNet())
				cellDesign.addNet(cellNet);
			netMap.put(net, cellNet);
		}
	}

	// Build one or more route trees based on the PIPs in the net
	private List<RouteTree> buildRouteTreeFromPips(Pin sourcePin, Collection<PIP> pips) {
		Set<PIP> pipSet = new HashSet<>(pips);
		Map<Wire, RouteTree> routeTreeMap = new HashMap<>();
		Set<Wire> visited = new HashSet<>();

		if (sourcePin != null && sourcePin.getInstance().isPlaced()) {
			Site site = sourcePin.getInstance().getPrimitiveSite();
			Wire wire = site.getSitePin(sourcePin.getName()).getExternalWire();
			traverseRoute(wire, pipSet, routeTreeMap, visited);
		}

		// Handle any remaining PIPs, these PIPs are source-less
		while (!pipSet.isEmpty()) {
			PIP pip = pipSet.iterator().next();
			Wire sourceWire = new TileWire(pip.getTile(), pip.getStartWire());
			traverseRoute(sourceWire, pipSet, routeTreeMap, visited);
		}

		// Get all of the source route trees that have been generated
		return routeTreeMap.values().stream()
				.filter(rt -> !rt.isSourced())
				.collect(Collectors.toList());
	}

	// Returns the route tree starting from the source wire.  Returns null if it comes to an end
	private RouteTree traverseRoute(
			Wire source, Set<PIP> pipSet,
			Map<Wire, RouteTree> routeTreeMap, Set<Wire> visited
	) {
		RouteTree rt = null;
		for (Connection c : source.getWireConnections()) {
			// If connection is a pip that isn't in the set, don't follow it
			if (c.isPip() && !pipSet.remove(c.getPip()))
				continue;
			Wire sinkWire = c.getSinkWire();
			if (visited.add(sinkWire)) {
				RouteTree sink;
				// check if already traversed
				if (routeTreeMap.containsKey(sinkWire)) {
					sink = routeTreeMap.get(sinkWire);
				} else {
					// Follow the wire until it ends
					sink = traverseRoute(sinkWire, pipSet, routeTreeMap, visited);

					// If the sink ends at a pin and doesn't use a route through, add it
					if (sink == null && c.isPinConnection())
						sink = new RouteTree(sinkWire);
				}
				// If a path was found add it to the tree and add the wire/route pair
				// to the map
				if (sink != null) {
					if (rt == null)
						rt = new RouteTree(source);
					rt.addConnection(c, sink);
					routeTreeMap.put(sinkWire, sink);
				}
				visited.remove(sinkWire);
			}
		}
		return rt;
	}

	private void createAndAddInstances(Design design) {
		// Now to handle the instances
		for (Instance inst : design.getInstances()) {
			// Map from the bel names to the cell at that location
			instanceBelToCellMap = new HashMap<>();
			// Use the cell creators to create a cell for each used BEL in the instance
			createAndAddCells(inst);

			// Create nets from the input ports and find connected pins and ports
			createAndConnectNetsFromInputPorts(inst);
			// Create nets from the BEL output ports and find connected pins and ports
			createAndConnectNetsFromCells(inst);
		}
	}

	private void createAndAddCells(Instance inst) {
		// We'll work off of the BEL templates in the site to distinguish which
		// attributes are actually BELs
		Site siteTemplate = siteTemplates.get(inst.getType());
		if (inst.isPlaced())
			siteTemplate = inst.getPrimitiveSite();
		siteTemplate.setType(inst.getType());
		for (Bel belTemplate : siteTemplate.getBels()) {
			// This BEL is used if the attribute exists and is not configured
			// to #OFF
			Attribute attr = inst.getAttribute(belTemplate.getId().getName());
			if (attr != null && !attr.getValue().equals("#OFF")) {
				// Make a cell creator object for this cell, create the cell,
				// and add it to the molecule
				CellCreatorFactory cellCreatorFactory = cellCreatorFactories.get(belTemplate.getId());
				CellCreator cellCreator = cellCreatorFactory.build(inst);
				Cell cell = cellCreator.createCell(cellDesign);
				cellCreators.put(cell, cellCreator);
				cellDesign.addCell(cell);
				if (inst.isPlaced()) {
					cellDesign.placeCell(cell, belTemplate);
				}

				// store it in the instanceBelToCellMap map for later lookup
				instanceBelToCellMap.put(belTemplate, cell);
			}
		}
	}

	private void createAndConnectNetsFromInputPorts(Instance inst) {
		// create nets all of the input ports/pins
		for (Pin pin : inst.getPins()) {
			if (pin.isOutPin() || pin.getNet() == null)
				continue;

			cellNet = netMap.get(pin.getNet());
			Site siteTemplate = siteTemplates.get(inst.getType());
			if (inst.isPlaced())
				siteTemplate = inst.getPrimitiveSite();
			SiteWire siteWire = siteTemplate.getSitePin(pin.getName()).getInternalWire();
			buildIntrasiteRoutingNetwork(inst, siteWire);
		}
	}

	private void createAndConnectNetsFromCells(Instance inst) {
		for (Bel bel : instanceBelToCellMap.keySet()) {
			// get the bel template to determine the names of the pins to add to the cell
			Cell cell = instanceBelToCellMap.get(bel);

			createAndConnectNetsFromPins(inst, bel, cell);
			mapCellPinToBelPin(bel, cell);
		}
	}

	private void cleanupSinglePinNets() {
		List<CellNet> toRemove = cellDesign.getNets().stream()
				.filter(n -> !n.isStaticNet())
				.filter(n -> n.getPins().size() <= 1)
				.collect(Collectors.toList());

		toRemove.forEach(cellDesign::disconnectNet);
		toRemove.forEach(cellDesign::removeNet);
	}

	private void createAndConnectNetsFromPins(
			Instance inst, Bel belTemplate, Cell cell)	{
		for (BelPin belPin : belTemplate.getSources()) {
			// determine the cell pin to create for this bel pin
			String cellPinName = cellCreators.get(cell).getCellPin(belPin.getName());

			// This is an unused pin on the BEL
			if (cellPinName == null)
				continue;

			CellPin cellPin = cell.getPin(cellPinName);

			// This is an unused pin on the BEL or an inout pin that was already handled
			if (cellPin == null || cellPin.getNet() != null)
				continue;

			// create a cellNet stemming from this pin and make the pin the
			// source of this net
			String netName = "subsite_net__" + cell.getName() + "_" + belPin.getName();
			if (cell.isVccSource()) {
				if (vccNet == null) {
					vccNet = new CellNet(netName, NetType.VCC);
					cellDesign.addNet(vccNet);
				}
				cellNet = vccNet;
				cellDesign.removeCell(cell);
			} else if (cell.isGndSource()) {
				if (gndNet == null) {
					gndNet = new CellNet(netName, NetType.GND);
					cellDesign.addNet(gndNet);
				}
				cellNet = gndNet;
				cellDesign.removeCell(cell);
			} else {
				cellNet = new CellNet(netName, NetType.WIRE);
				cellDesign.addNet(cellNet);
				cellNet.connectToPin(cellPin);
			}

			// find and add all of its sinks of this net
			Site siteTemplate = siteTemplates.get(inst.getType());
			if (inst.isPlaced())
				siteTemplate = inst.getPrimitiveSite();
			SiteWire sourceWire = new SiteWire(siteTemplate, belPin.getWire().getWireEnum());
			buildIntrasiteRoutingNetwork(inst, sourceWire);
		}
	}

	// Identify the cell pin for each BEL pin on the BEL and add the mapping to
	// the molecule
	private void mapCellPinToBelPin(Bel belTemplate, Cell cell) {
		for (BelPin belPin : belTemplate.getSources()) {
			getAndAddCellPin(cell, belPin);
		}

		for (BelPin belPin : belTemplate.getSinks()) {
			getAndAddCellPin(cell, belPin);
		}
	}

	private void getAndAddCellPin(Cell cell, BelPin belPin) {
		// determine the cell pin to create for this bel pin
		if (preserveRouting) {
			String cellPinName = cellCreators.get(cell).getCellPin(belPin.getName());
			CellPin cellPin = cellPinName != null ? cell.getPin(cellPinName) : null;
			if (cellPin != null)
				cellPin.mapToBelPin(belPin);
		}
	}

	private void buildIntrasiteRoutingNetwork(
			Instance inst, SiteWire sourceWire) {
		// follow the pips from the source wire and connect pins and ports
		RouteTree rt = findSinks(sourceWire, inst);
		if (preserveRouting && rt != null) // TODO allow for intrasite option routing
			cellNet.addIntersiteRouteTree(rt);
	}

	private RouteTree findSinks(SiteWire sourceWire, Instance inst) {
		Site siteTemplate = sourceWire.getSite();
		RouteTree rt = null;

		// recursively follow this wire until sinks are found
		for (Connection c : sourceWire.getAllConnections().collect(Collectors.toList())) {
			Wire sinkWire = c.getSinkWire();
			if (c.isTerminal())
				continue;
			BelPin belPin = siteTemplate.getBelPinOfWire(sinkWire.getWireEnum());
			if (belPin != null) {
				rt = connectToBel(sourceWire, rt, c, belPin);
			} else if (c.isPinConnection()) {
				rt = connectToSitePin(sourceWire, inst, rt, c);
			} else if (c.isPip()) {
				if (pipUsedInInstances(sourceWire, sinkWire, inst)) {
					RouteTree sinkTree = findSinks(((SiteWire) sinkWire), inst);
					// always add the pip if it exists
					if (sinkTree == null)
						sinkTree = new RouteTree(sinkWire);
					rt = initRouteTree(rt, sourceWire);
					rt.addConnection(c, sinkTree);
				}
			} else {
				// Follow and only add to tree if it connects to something interesting
				RouteTree sinkTree = findSinks(((SiteWire) sinkWire), inst);
				if (sinkTree != null) {
					rt = initRouteTree(rt, sourceWire);
					rt.addConnection(c, sinkTree);
				}
			}
		}

		return rt;
	}

	private RouteTree connectToBel(
			SiteWire sourceWire, RouteTree rt,
			Connection c, BelPin belPin) {
		// This route connects to a BEL pin.  Obtain the cell pin that
		// relates to this BEL pin and add it to the pin list.
		Cell cell = instanceBelToCellMap.get(belPin.getBel());
		if (cell == null)
			return rt;
		String cellPinName = cellCreators.get(cell).getCellPin(belPin.getName());
		CellPin cellPin = cellPinName != null ? cell.getPin(cellPinName) : null;
		// The cell pin can be null if the the cell contains a subset of
		// pins of the BEL.  In this case, there are pins on the BEL that
		// are not used by the cell.  e.g. the WE input of a LUT6 being
		// placed on a SLICEM/A6LUT
		if (cellPin == null)
			return rt;

		// Don't connect VCC and GND going to LUTRAM WA and A pins
		if (cellNet.isStaticNet() && ignoreStatics(cell, cellPin))
			return rt;

		if (cellPin.getNet() != null) {
			assert cellPin.getNet().getSourcePin().isInpin();
			mergeCellNetWithExistingNet(cellPin.getNet());
			rt = initRouteTree(rt, sourceWire);
			rt.addConnection(c);
		} else {
			cellNet.connectToPin(cellPin);
			rt = initRouteTree(rt, sourceWire);
			rt.addConnection(c);
		}
		return rt;
	}

	private boolean ignoreStatics(Cell cell, CellPin cellPin) {
		return cell.getLibCell().getName().contains("LUTRAM") &&
				cellPin.getName().matches("W?A\\d");
	}

	private RouteTree connectToSitePin(
			SiteWire sourceWire, Instance inst,
			RouteTree rt, Connection c) {
		// This is a site pin wire, add its related port to the port list
		SitePin sitePin = c.getSitePin();
		String sitePinName = sitePin.getName();
		Pin origDesignPin = inst.getPin(sitePinName);

		// if null, then this pin is unused in the design and no port exists
		if (origDesignPin != null && origDesignPin.getNet() != null) {
			// Route will reach pin, but not poke out into tile fabric
			Net outerNet = origDesignPin.getNet();
			mergeCellNetWithExistingNet(netMap.get(outerNet));
			rt = initRouteTree(rt, sourceWire);
		}
		return rt;
	}

	private void mergeCellNetWithExistingNet(CellNet globalNet) {
		if (cellNet == globalNet)
			return;

		for (CellPin pin : new ArrayList<>(cellNet.getPins())) {
			cellNet.disconnectFromPin(pin);
			globalNet.connectToPin(pin);
		}
		cellNet.getIntersiteRouteTreeList().forEach(globalNet::addIntersiteRouteTree);
		cellNet.unroute();
		if (!cellNet.isStaticNet())
			cellDesign.removeNet(cellNet);
		cellNet = globalNet;
	}

	private boolean pipUsedInInstances(SiteWire sourceWire, Wire sinkWire, Instance inst) {
		Attribute pipAttr = sourceWire.getSite().getPipAttribute(
				sourceWire.getWireEnum(), sinkWire.getWireEnum());
		Attribute attr = inst.getAttribute(pipAttr.getPhysicalName());
		return attr != null && attr.getValue().equals(pipAttr.getValue());
	}

	private RouteTree initRouteTree(RouteTree rt, Wire sourceWire) {
		if (rt == null)
			rt = new RouteTree(sourceWire);
		return rt;
	}
}
