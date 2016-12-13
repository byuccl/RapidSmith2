package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.*;
import java.util.stream.Collectors;

import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.BelRoutethrough;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.device.Connection;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.SitePin;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileWire;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.WireEnumerator;

import static edu.byu.ece.rapidSmith.util.Exceptions.ParseException;

/**
 * This class is used for parsing and writing routing XDC files in a TINCR checkpoint.
 * Routing.xdc files are used to specify the physical wires that a net in Vivado uses.
 */
public class XdcRoutingInterface {

	private Device device;
	private CellDesign design;
	private WireEnumerator wireEnumerator;
	private HashMap<SitePin, IntrasiteRoute> sitePinToRouteMap;
	private Map<BelPin, CellPin> belPinToCellPinMap;
	private Map<SiteType, Set<String>> staticSourceMap;
	private Set<Bel> staticSourceBels;
	private int currentLineNumber;
	private String currentFile;
	private Map<Bel, BelRoutethrough> belRoutethroughMap;
	
	/**
	 * Creates a new XdcRoutingInterface object.
	 * 
	 * @param design {@link CellDesign} to add routing information to
	 * @param device {@link Device} of the specified design
	 * @param pinMap A map from a {@link BelPin} to its corresponding {@link CellPin} (the cell
	 * 				pin that is currently mapped onto the bel pin)  
	 */
	public XdcRoutingInterface(CellDesign design, Device device, Map<BelPin, CellPin> pinMap) {
		
		this.device = device;
		this.wireEnumerator = device.getWireEnumerator();
		this.design = design;
		this.sitePinToRouteMap = new HashMap<>();
		this.staticSourceMap = new HashMap<>();
		this.belPinToCellPinMap = pinMap;
		this.currentLineNumber = 0;
	}
	
	/**
	 * Returns a set of BELs that are being used as a routethrough.
	 * This is only valid after {@link XdcRoutingInterface::parseRoutingXDC} has been called.
	 * @return A map from a {@link Bel}s to {@link BelRoutethrough}s
	 */
	public Map<Bel, BelRoutethrough> getRoutethroughsBels() {
		
		return (this.belRoutethroughMap == null) ? 
				Collections.emptyMap() :
					belRoutethroughMap;
	}
	
	/**
	 * Returns a set of BELs that are being used as a static source (VCC or GND).
	 * This is only valid after {@link XdcRoutingInterface::parseRoutingXDC} has been called.
	 */
	public Set<Bel> getStaticSourceBels() {
		
		return (staticSourceBels == null) ? 
				Collections.emptySet() :
				staticSourceBels;
	}
		
	/**
	 * Parses the specified routing.xdc file, and applies the physical wire information to the nets of the design
	 * 
	 * @param xdcFile routing.xdc file
	 * @throws IOException
	 */
	public void parseRoutingXDC(String xdcFile) throws IOException {

		currentFile = xdcFile;

		// try-with-resources to guarantee no resource leakage
		try (LineNumberReader br = new LineNumberReader(new BufferedReader(new FileReader(xdcFile)))) {
		
			String line = null;
			while ((line = br.readLine()) != null) {
				this.currentLineNumber = br.getLineNumber();
				String[] toks = line.split("\\s+");
	
				// TODO: I know the order these things appear in the file, so I probably don't need a big switch statement
				// SITE_PIPS -> STATIC_SOURCES -> LUT_RTS -> INTRASITE/INTERSITE/ROUTE
				// Update this if there is a performance issue, but it should be fine
				switch (toks[0]) {
				
					case "SITE_PIPS" : processSitePips(toks);
						break;
					case "INTERSITE" : processIntersitePins(toks);
						break;
					case "INTRASITE" : processIntrasiteRoute(toks);
						break;
					case "ROUTE" : processIntersiteRoute(toks); 
						break;
					case "LUT_RTS" : processLutRoutethroughs(toks); 
						break;
					case "STATIC_SOURCES" : processStaticSources(toks);
						break;
					case "VCC": 
						String[] vcc_wires = br.readLine().split("\\s+");
						assert (vcc_wires[0].equals("START_WIRES"));
						processStaticNet(toks, vcc_wires);
						break; 
					case "GND": 
						String[] gnd_wires = br.readLine().split("\\s+");
						assert (gnd_wires[0].equals("START_WIRES"));
						processStaticNet(toks, gnd_wires);
						break;
					default : 
						throw new ParseException("Unrecognized Token: " + toks[0]);
				}
			}
			
			// compute the routing status for the GND and VCC nets at the end
			if (design.getVccNet() != null) {
				design.getVccNet().computeRouteStatus();
			}
			if (design.getGndNet() != null) {
				design.getGndNet().computeRouteStatus();
			}
		}
	}
	
	/**
	 * Loads the site PIP information for a site.
	 *  
	 * @param toks A string array of the form: <br>
	 * <br>
	 * {@code SITE_PIPS sitename pip0:input0 pip1:input1 ... pinN:inputN"} <br>
	 * <br> 
	 * where space separated elements are different elements in the array
	 */
	private void processSitePips (String[] toks) {
		
		Site site = tryGetSite(toks[1]);
		readUsedSitePips(site, toks);
		createStaticSubsiteRouteTrees(site);
	}
	
	/**
	 * Loads the site pin information for a net, and creates the intrasite routing
	 * connected to those site pins 
	 *  
	 * @param toks A string array of the form: <br>
	 * <br>
	 * {@code INTERSITE netName site0/pin0 site1/pin1 ... siteN/pinN"} <br>
	 * <br> 
	 * where space separated elements are different elements in the array
	 */
	private void processIntersitePins(String[] toks) {
		
		CellNet net = tryGetCellNet(toks[1]);
		
		// System.out.println(net.getName());
		for (int index = 2 ; index < toks.length; index++) {
			
			String[] sitePinToks = toks[index].split("/");
			
			assert (sitePinToks.length == 2);
			
			Site site = tryGetSite(sitePinToks[0]);
			SitePin pin = tryGetSitePin(site, sitePinToks[1]);
			
			if (pin.isInput()) { // of a site				
				createIntrasiteRoute(pin, net, design.getUsedSitePipsAtSite(site));
			}
			else { // pin is an output of the site
				
				// Most nets only have one source site pin, but CARRRY4 cells can have "more than one" reported from vivado
				if (net.getSourceSitePin() == null) {
					net.setSourceSitePin(pin);
				}
				
				CellPin sourceCellPin = tryGetNetSource(net);
				BelPin sourceBelPin = tryGetMappedBelPin(sourceCellPin);
				createIntrasiteRoute(sourceBelPin, false, false, design.getUsedSitePipsAtSite(site));
			}
		}
	}
	
	/**
	 * Creates routing data structures for an intrasite route
	 *  
	 * @param toks A string array of the form: <br>
	 * <br>
	 * {@code INTRASITE netName} <br>
	 * <br> 
	 * where space separated elements are different elements in the array
	 */
	private void processIntrasiteRoute(String[] toks) {
		
		CellNet net = tryGetCellNet(toks[1]);		
		
		if (net.getSourcePin() == null) {
			return;
		}
		
		CellPin sourceCellPin = tryGetNetSource(net);
		BelPin sourceBelPin = tryGetMappedBelPin(sourceCellPin);
				
		Site site = sourceBelPin.getBel().getSite();
		createIntrasiteRoute(sourceBelPin, true, false, design.getUsedSitePipsAtSite(site));
		net.setIsIntrasite(true);
		net.computeRouteStatus();
	}
	
	/**
	 * Creates the routing data structures for a VCC or GND net
	 * 
	 * @param wireToks A string array of the form: <br>
	 * {@code "VCC tile0/wire0 tile1/wire1 ... tileN/wireN"} <br>
	 * where space separated elements are different elements in the array. The {@code tile/wire} elements
	 * are the wires that are used in the static net. GND could also be the first token. <br>
	 * @param startWires A string array of the form: <br>
	 * {@code "START_WIRES tile0/wire0 tile1/wire1 ... tileN/wireN"} <br>
	 * where space separated elements are different elements in the array. The {@code tile/wire} elements
	 * are the <b>starting wires</b> of the net (i.e. the wires connected to tieoffs).
	 */
	private void processStaticNet(String[] wireToks, String[] startWires) {
		
		CellNet net = tryGetCellNet(wireToks[0]);
		
		// Create a set of all used wires in the static net
		Set<String> wiresInNet = new HashSet<>();
		// The first token is either VCC or START_WIRES, not a wire name
		for (int i = 1; i < wireToks.length; i++ ) {
			String wireName = wireToks[i];
			wiresInNet.add(wireName);
		}
		
		// Recreate the routing structure for each of the start wires
		// The first token is either VCC or START_WIRES, not a wire name
		for (int i = 1; i < startWires.length; i++ ) {
			String startWire = startWires[i];
			RouteTree netRouteTree = recreateRoutingNetwork(net, startWire, wiresInNet);
			net.addIntersiteRouteTree(netRouteTree);
		}
	}
	
	/**
	 * Creates the routing data structures for a net given all of the wires in the net.
	 * 
	 * @param toks A string array of the form: <br>
	 * {@code "ROUTE netName tile0/wire0 tile1/wire1 ... tileN/wireN"} <br>
	 * where space separated elements are different elements in the array. The {@code tile/wire} elements
	 * are the wires that are used in the net {@code netName}.
	 */
	private void processIntersiteRoute(String[] toks) {
		CellNet net = tryGetCellNet(toks[1]);

		Set<String> wiresInNet = new HashSet<>();

		// First 2 tokens are ROUTE <netName>
		String startWire = toks[2];
		wiresInNet.addAll(Arrays.asList(toks).subList(2, toks.length));
		
		RouteTree netRouteTree = recreateRoutingNetwork(net, startWire, wiresInNet);
		net.addIntersiteRouteTree(netRouteTree);		
		net.computeRouteStatus();
	}
	
	/**
	 * Creates a {@link RouteTree} data structure from a set of wires
	 * that are in a net. This RouteTree represents the 
	 * <b>physical intersite</b> route of the net.
	 * 
	 * @param net {@link CellNet} to create a routing data structure for
	 * @param startWireName The name of the wire connected to the source site pin
	 * 						of the net. This is used to initailize the route.
	 * @param wiresInNet A set of wire names that exist in the net.
	 * @return {@link RouteTree} representing the physical intersite route of the net
	 */
	private RouteTree recreateRoutingNetwork(CellNet net, String startWireName, Set<String> wiresInNet) {
		
		// initialize the routing data structure with the start wire
		RouteTree start = initializeRoute(startWireName);
		Queue<RouteTree> searchQueue = new LinkedList<>();
		Set<Wire> visited = new HashSet<>();
		Set<RouteTree> terminals = new HashSet<>();
		
		// initialize the search queue and visited wire set
		searchQueue.add(start); 
		visited.add(start.getWire());
		
		while (!searchQueue.isEmpty()) {
			
			RouteTree routeTree = searchQueue.poll();
						
			// add connecting wires that exist in the net to the search queue
			int connectionCount = 0; 
			for (Connection conn : routeTree.getWire().getWireConnections()) {
				
				Wire sinkWire = conn.getSinkWire();
				
				if (wiresInNet.contains(sinkWire.getFullWireName()) && !visited.contains(sinkWire)) {
					connectionCount++;
					RouteTree sinkTree = routeTree.addConnection(conn);
					searchQueue.add(sinkTree);
					visited.add(sinkWire);
				}
			}
			
			// check to see if the current route tree object is connected to a valid sink site pin 
			// the connection count is used to filter out routethrough site pins
			SitePin sinkSitePin = routeTree.getConnectingSitePin();
			if (sinkSitePin != null && connectionCount == 0) {
				terminals.add(routeTree);
				processSitePinSink(net, sinkSitePin);
			}
		}
			
		// prune useless paths from the route tree (i.e paths that go nowhere
		// TODO: only do this if the net is marked as fully routed
		start.prune(terminals);	
		return start;
	}
	
	/**
	 * Creates new {@link TileWire} and {@link RouteTree} objects based on the input
	 * start wire name. This function is used to create an initial RouteTree when
	 * a nets physical routing is being reconstructed.
	 *   
	 * @param startWireName Name of a wire in the currently loaded device
	 * @return RouteTree object representing the start wire
	 */
	private RouteTree initializeRoute(String startWireName) {
		String[] startWireToks = startWireName.split("/");
		Tile tile = tryGetTile(startWireToks[0]);
		int wireEnum = tryGetWireEnum(startWireToks[1]);
		return new RouteTree(new TileWire(tile, wireEnum));
	}
	
	/**
	 * When a route reaches a site pin, this function is called
	 * to mark sink cell pins as routed, and add pseudo pins
	 * to the design is necessary (for VCC and GND nets only)
	 * 
	 * @param net {@link CellNet} the is currently being routed
	 * @param sinkSitePin {@link SitePin} that the net routing has reached
	 */
	private void processSitePinSink(CellNet net, SitePin sinkSitePin) {
		// update the net with the routed cell pins
		IntrasiteRoute internalRoute = sitePinToRouteMap.get(sinkSitePin);
		
		if (internalRoute != null) { 
			internalRoute.setSinksAsRouted();
		}
		else { 
			// implicit intrasite net. An Example is a GND/VCC net going to the A6 pin of a LUT.
			// I does not actually show the bel pin as used, but it is being used.
			// another example is the A1 pin on a SRL cell
			assert(net.isStaticNet()) : "Only static nets should not have site pin information: " + net.getName() + " " + sinkSitePin;
			createStaticNetImplicitSinks(sinkSitePin, net);
		}
	}
	
	/**
	 * Creates a map from {@link BelPin} to {@link BelRoutethrough} for all used routethroughs
	 * found in the routing.rsc file. This map is used to successfully recreate intrasite routing
	 * for nets later in the parse process.
	 * 
	 * @param toks List of routethrough tokens in the form: <br>
	 * {@code LUT_RTS site0/bel0/inputPin0/outputPin0 site1/bel1/inputPin1/outputPin1 ...}
	 */
	private void processLutRoutethroughs(String[] toks) {

		this.belRoutethroughMap = new HashMap<>();
		
		for (int i = 1; i < toks.length; i++) {
			String[] routethroughToks = toks[i].split("/");			
			checkTokenLength(routethroughToks.length, 4);
			
			// TODO: Check that the input pin is an input pin and the output pin is an output pin?
			Site site = tryGetSite(routethroughToks[0]);
			Bel bel = tryGetBel(site, routethroughToks[1]);
			BelPin inputPin = tryGetBelPin(bel, routethroughToks[2]);
			BelPin outputPin = tryGetBelPin(bel, routethroughToks[3]);
		
			belRoutethroughMap.put(bel, new BelRoutethrough(bel, inputPin, outputPin));
		}
	}
	
	/**
	 * Creates routing data structures for static nets (GND/VCC) that are
	 * source by static LUTs.
	 * 
	 * @param toks A list of static source bels in the form: <br>
	 * {@code STATIC_SOURCES site0/bel0/outputPin0 site1/bel1/outputPin1 ... siteN/belN/outputPinN}
	 */
	private void processStaticSources(String[] toks) {
		
		if (toks.length > 1) {
			this.staticSourceBels = new HashSet<>();
		}
		
		for (int i = 1; i < toks.length; i++) {
			String[] staticToks = toks[i].split("/");
			checkTokenLength(staticToks.length, 3);
			
			Site site = tryGetSite(staticToks[0]);
			Bel bel = tryGetBel(site, staticToks[1]);
			BelPin sourcePin = tryGetBelPin(bel, staticToks[2]);
			boolean routeFound = tryCreateStaticIntrasiteRoute(sourcePin, design.getUsedSitePipsAtSite(site));
			assert routeFound : site.getName() + "/" + bel.getName() + "/" + sourcePin.getName();
			staticSourceBels.add(bel);
		}
	}
					
	/**
	 * Parse the used PIPS within a given {@link Site}, and store that information in the current {@link CellDesign}
	 * data structure. These site pips are used to correctly import intrasite routing later in the parse process. 
	 * 
	 * @param site {@link Site} object
	 * @param toks An array of used site PIPS in the form: <br>
	 * {@code SITE_PIPS siteName pip0:input0 pip1:input1 ... pipN:inputN}
	 */
	private void readUsedSitePips(Site site, String[] toks) {
		
		HashSet<Integer> usedSitePips = new HashSet<>();
		
		String namePrefix = "intrasite:" + site.getType() + "/";
		
		//list of site pip wires that are used...
		for(int i = 2; i < toks.length; i++) {
			String pipWireName = (namePrefix + toks[i].replace(":", "."));
			Integer wireEnum = tryGetWireEnum(pipWireName); 
			
			//add the input and output pip wires (there are two of these in RS2)
			// TODO: Is it useful to add the output wires...I don't think these are necessary
			usedSitePips.add(wireEnum); 	
			usedSitePips.add(tryGetWireEnum(pipWireName.split("\\.")[0] + ".OUT"));
		}
		
		design.setUsedSitePipsAtSite(site, usedSitePips);
	}
	
	/**
	 * Searches through the specified {@link Site} for VCC and GND BELs,
	 * and tries to create intrasite routing starting from those BELs if they are used.
	 * There is no way in Vivado to tell if they are used, so we have to search 
	 * each one. If the search reaches a {@link BelPin}, this mean the BEL source is being
	 * used and the site is completed. 
	 *   
	 * @param site {@link Site} object
	 */
	private void createStaticSubsiteRouteTrees(Site site) {
				
		Iterator<BelPin> staticSourceIt = getPowerBelSourcesToSearch(site); 
		while (staticSourceIt.hasNext()) {
			
			BelPin pin = staticSourceIt.next();
			tryCreateStaticIntrasiteRoute(pin, design.getUsedSitePipsAtSite(site));
		}
	}
	
	/**
	 * Searches through all {@link Bel}s in the specified site, and returns an iterator to a
	 * list of all BELs that are VCC or GND. 
	 * 
	 * TODO: Cache the information on a {@link SiteType} basis so that the search only happens
	 * once per site type. This is especially useful for SLICEL and SLICEM types. Right now, I am
	 * caching it on a per site basis, which seems useless... 
	 * 
	 * @param site
	 * @return
	 */
	private Iterator<BelPin> getPowerBelSourcesToSearch(Site site) {
		Set<String> staticSourcesInSite =  staticSourceMap.get(site.getType());

		if (staticSourcesInSite == null) {

			staticSourcesInSite = site.getBels().stream()
									.filter(bel -> bel.getName().contains("VCC") || bel.getName().contains("GND"))
									.map(Bel::getName)
									.collect(Collectors.toSet());

			staticSourceMap.put(site.getType(), staticSourcesInSite);
		}

		return staticSourcesInSite.stream()
							.map(site::getBel)
							.flatMap(bel -> bel.getSources().stream())
							.iterator();
	}
	
	/**
	 * Creates intrasite routing data structures for the route starting at the specified
	 * {@link SitePin}. In particular, this is for static nets (GND/VCC) that have
	 * terminated at a site pin, but the site pin has not been registered earlier during parsing.
	 * This is due to the net connecting to a BelPin, but not a CellPin. In this case, a <b>pseudo pin</b>
	 * will be created and attached to a cell if one exists on the bel.  
	 * 
	 * @param sitePin {@link SitePin} object
	 * @param net VCC or GND {@link CellNet} 
	 */
	private void createStaticNetImplicitSinks(SitePin sitePin, CellNet net) {
		
		IntrasiteRoute staticRoute = new IntrasiteRouteSitePinSource(sitePin, net, true);
		buildIntrasiteRoute(staticRoute, design.getUsedSitePipsAtSite(sitePin.getSite()));
		
		if (!staticRoute.isValid()){
			throw new AssertionError("Static net does not finish...");
		}
		staticRoute.applyRouting();
		// we can set sinks as routed here because we only call this function if we reach a site pin
		staticRoute.setSinksAsRouted();
	}
				
	/**
	 * Creates a route starting at the specified {@link SitePin}, which terminates at BelPins inside the site.
	 * The search is guided by the specified used site pips of the site.
	 * If no valid route is found, an exception is thrown because this function.
	 * expects a route to be found 
	 * 
	 * @param pin Site Pin to start the route
	 * @param net Net attached to that site pin
	 * @param usedSiteWires Set of used pips within the site
	 */
	private void createIntrasiteRoute(SitePin pin, CellNet net, Set<Integer> usedSiteWires) {
	
		IntrasiteRoute route = new IntrasiteRouteSitePinSource(pin, net, false);
		buildIntrasiteRoute(route, usedSiteWires);
		
		if (!route.isValid()) {			
			throw new AssertionError("Valid intrasite route not found from site pin : " + pin + " Net: " + net.getName());
		}
		
		route.applyRouting();
		sitePinToRouteMap.put(pin, route);
	}
	
	/**
	 * Creates a route starting at the specified {@link BelPin}, and terminates in either BelPins of {@link SitePin}s.
	 * The search is guided by the used site pips of the site. If no valid route is found, an exception is thrown 
	 * 
	 * @param pin Bel Pin to start the route
	 * @param isContained True if all sinks of the net are in the same site as the source
	 * @param isStatic True if the source of the net is a VCC or GND bel
	 * @param usedSiteWires Set of used pips within the site
	 */
	private void createIntrasiteRoute(BelPin pin, boolean isContained, boolean isStatic, Set<Integer> usedSiteWires) {
	
		IntrasiteRoute route = new IntrasiteRouteBelPinSource(pin, isContained, isStatic);
		buildIntrasiteRoute(route, usedSiteWires);
		
		if (!route.isValid()) {
			throw new AssertionError("Valid intrasite route not found from bel pin : " + pin);
		}
		
		route.applyRouting();
		route.setSinksAsRouted();
	}
	
	/**
	 * Tries to create a route starting at a GND or VCC bel. <br>
	 * Because no route is guaranteed to exist, routing is only applied <br>
	 * if a valid route is found. Otherwise, nothing happens.
	 * 
	 * @param pin BelPin to start the search
	 * @param usedSiteWires Used site pips in the site
	 * @return True if a route is found
	 */
	private boolean tryCreateStaticIntrasiteRoute(BelPin pin, Set<Integer> usedSiteWires) {
		
		IntrasiteRoute route = new IntrasiteRouteBelPinSource(pin, false, true);
		buildIntrasiteRoute(route, usedSiteWires);
		
		if (route.isValid()) {
			route.applyRouting();
			route.setSinksAsRouted();
			return true;
		}
		return false;
	}
	
	/**
	 * Performs an intrasite search starting at either a {@link BelPin} or {@link SitePin}, 
	 * creates a RouteTree data structure of the search, and records all BelPin and SitePin sinks 
	 * of the search. See the {@link IntrasiteRoute} interface to see methods that are called from this function  
	 * 
	 * @param intrasiteRoute {@link IntrasiteRoute} interface. See {@link IntrasiteRouteSitePinSource} and
	 * 						{@link IntrasiteRouteBelPinSource} for more details
	 * @param usedSiteWires
	 */
	private void buildIntrasiteRoute(IntrasiteRoute intrasiteRoute, Set<Integer> usedSiteWires) {
		
		// Initialize the search
		Set<Wire> visitedWires = new HashSet<>(); // used to prevent cycles
		Queue<RouteTree> routeQueue = new LinkedList<>();
		
		RouteTree startRoute = intrasiteRoute.getStartRoute();
		Wire startWire = startRoute.getWire();
		routeQueue.add(startRoute);
		visitedWires.add(startWire);
		
		// continue the search until we have nowhere else to go
		while (!routeQueue.isEmpty()) {
			RouteTree currentRoute = routeQueue.poll();
			Wire currentWire = currentRoute.getWire();

			// reached a used bel pin that is not the source
			if (intrasiteRoute.isValidBelPinSink(currentWire) && !currentWire.equals(startWire)) {
				BelPin bp = currentWire.getTerminals().iterator().next().getBelPin();
				intrasiteRoute.addBelPinSink(bp, currentRoute);
			}
			// reached a site pin
			else if (connectsToSitePin(currentWire)) {
				SitePin sinkPin = currentWire.getPinConnections().iterator().next().getSitePin();	
				intrasiteRoute.addSitePinSink(sinkPin, currentRoute);
			}
			else {
				
				for (Connection conn : currentWire.getWireConnections()) {
										
					// skip wires we already visited
					if (visitedWires.contains(conn.getSinkWire())) {
						continue;
					}
					
					// only add valid search connections to the queue
					if (isQualifiedConnection(conn, currentWire, usedSiteWires)) {
						RouteTree next = currentRoute.addConnection(conn);
						routeQueue.add(next);
						visitedWires.add(next.getWire());
					}
				}
			}
		}
		
		// prune the route tree 
		intrasiteRoute.pruneRoute();
	}
	
	/**
	 *	Returns <code>true</code> if the specified wire connects to 
	 *a {@link SitePin}, <code>false</code> otherwise.
	 * 
	 * @param currentWire {@link Wire} object
	 */
	private boolean connectsToSitePin(Wire currentWire) {
		return !currentWire.getPinConnections().isEmpty();
	}
	
	/**
	 * Returns true if the specified {@link Connection} object is a valid connection to follow in the site. 
	 * A connection is valid if one of the following are satisfied: <br>
	 * <p> <ul>
	 * <li> The connection is a <b>NON-PIP</b> wire connection  
	 * <li> The connection is a used LUT routethrough
	 * <li> The connection is a PIP wire connection that is used
	 * </ul><p> 
	 * 
	 * @param conn {@link Connection} object
	 * @param sourceWire The source {@link Wire} of the connection
	 * @param usedSiteWires A set of used wires in the {@link Site} that is currently being searched
	 */
	private boolean isQualifiedConnection(Connection conn, Wire sourceWire, Set<Integer> usedSiteWires) {
				
		return !conn.isPip() || // the connection is a regular wire connection
				isUsedRoutethrough(conn, sourceWire) || // or, the connection is a used lut routethrough 
				usedSiteWires.contains(sourceWire.getWireEnum()); // or the connection is a used site pip
	}
	
	/**
	 * Returns true if the given connections is a used BEL routethrough. <br>
	 * A connection satisfies this condition if: <br>
	 * <p> <ul>
	 * <li>(1) The connection is a routethrough <br>
	 * <li>(2) The BelPin is a key in the usedRoutethroughMap <br>
	 * <li>(3) The sink wire of the connection matched the value in the usedRoutethroughMap <br>
	 * </p> </ul>
	 * 
	 * @param conn Connection to test 
	 * @return True if the Connection is an available routethrough. False otherwise.
	 */
	private boolean isUsedRoutethrough(Connection conn, Wire sourceWire) {
		
		if (!conn.isRouteThrough()) {
			return false;
		}
		
		// a bel routethrough must also be connected to a BEL pin 
		assert (!sourceWire.getTerminals().isEmpty()) : "Wire: " + sourceWire + " should connect to BelPin!";
		BelPin source = sourceWire.getTerminals().iterator().next().getBelPin();
		
		BelRoutethrough routethrough = this.belRoutethroughMap.get(source.getBel());
		
		return routethrough != null && routethrough.getOutputWire().equals(conn.getSinkWire());
		
		// Wire sinkWire = usedRoutethroughMap.get(source);
		// return sinkWire != null && sinkWire.equals(conn.getSinkWire());
	}
	
	/**
	 * Returns {@code true} if the specified {@link BelPin} is being used 
	 * in the design (i.e. a {@link CellPin} is mapped to it), {@code false} otherwise
	 */
	private boolean isBelPinUsed(BelPin pin) {
		return belPinToCellPinMap.containsKey(pin);
	}
	
	/**
	 * Compares the actual token length of a line in the routing.rsc file against the 
	 * expected token length. If they do not agree, a {@link ParseException} is thrown.
	 * 
	 * @param tokenLength
	 * @param expectedLength
	 */
	private void checkTokenLength(int tokenLength, int expectedLength) {
		
		if (tokenLength != expectedLength) {
			throw new ParseException(String.format("Incorrect number of tokens on line %d of %s.\n"
												+ "Expected: %d Actual: %d", currentLineNumber, currentFile, tokenLength, expectedLength));
		}
	}
	
	/**
	 * Tries to retrieve the Site object with the given site name <br>
	 * from the currently loaded device. If the site does not exist <br>
	 * a {@link ParseException} is thrown <br>
	 * 
	 * @param siteName Name of the site to retrieve
	 */
	private Site tryGetSite(String siteName) {
		
		Site site = device.getPrimitiveSite(siteName);
		
		if (site == null) {
			throw new ParseException("Site \"" + siteName + "\" not found in the current device. \n" 
									+ "On line " + this.currentLineNumber + " of " + currentFile);
		}
		
		return site;
	}
	
	/**
	 * Tries to retrieve the Tile object with the given name from the currently
	 * loaded device. If no such tile exists, a {@link ParseException} is thrown.
	 * 
	 * @param tileName Name of the tile to get a handle of
	 * @return {@link Tile} object
	 */
	private Tile tryGetTile(String tileName) {
		Tile tile = device.getTile(tileName);
		
		if (tile == null) {
			throw new ParseException("Tile \"" + tileName + "\" not found in device " + device.getPartName() + ". \n"  
					+ "On line " + this.currentLineNumber + " of " + currentFile); 
		}
		return tile;
	}
	
	/**
	 * Tries to retrieve the CellNet object with the given name <br>
	 * from the currently loaded design. If the net does not exist <br>
	 * a ParseException is thrown.
	 * 
	 * @param netName Name of net to retrieve
	 * @return CellNet
	 */
	private CellNet tryGetCellNet(String netName) {
		
		CellNet net = null;
		switch (netName) {
			case "VCC":
				net = design.getVccNet();
				break;
			case "GND":
				net = design.getGndNet();
				break;
			default:
				net = design.getNet(netName);
				break;
		}
		
		if (net == null) {
			throw new ParseException("Net \"" + netName + "\" does not exist in the current design. Validate Edif is correct.\n" 
									+ "On line " + this.currentLineNumber + " of " + currentFile);
		}
				
		return net;
	}
	
	/**
	 * Tries to retrieve the SitePin object specified by the site <br>
	 * and pin name. If the pin does not exist, a ParseException is thrown. <br>
	 *  
	 * @param site Site object
	 * @param pinName Name of the pin on the site
	 * @return SitePin
	 */
	private SitePin tryGetSitePin(Site site, String pinName) {
		
		SitePin pin = site.getSitePin(pinName);
		
		if (pin == null) {
			throw new ParseException(String.format("SitePin: \"%s/%s\" does not exist in the current device\n"
												   + "On line %d of %s", site.getName(), pinName, currentLineNumber, currentFile));
		}
		
		return pin;
	}
	
	/**
	 * Tries to retrieve the source of a CellNet. If the net does not have a source, <br>
	 * a ParseException is thrown because all nets with routing information are expected <br>
	 * to have a source cell pin.
	 * 
	 * @param net CellNet to get the source of
	 * @return CellPin
	 */
	private CellPin tryGetNetSource(CellNet net) {
		
		CellPin source = net.getSourcePin();
		
		if (source == null) {
			throw new ParseException("Net \"" + net.getName() +"\" does not have a source pin!\n" 
									+ "On line " + this.currentLineNumber + " of " + currentFile);
		}
		
		return source;
	}
	
	/**
	 * Tries to retrieve a BEL object from the currently loaded device. <br>
	 * If the BEL does not exist, a ParseException is thrown. <br>
	 * 
	 * @param site Site where the BEL resides
	 * @param belName Name of the BEL within the site
	 * @return Bel
	 */
	private Bel tryGetBel(Site site, String belName) {
		
		Bel bel = site.getBel(belName);
		
		if (bel == null) {
			throw new ParseException(String.format("Bel: \"%s/%s\" does not exist in the current device"
												 + "On line %d of %s", site.getName(), belName, currentLineNumber, currentFile));
		}
		
		return bel;
	}
	
	/**
	 * Tries to retrieve a BelPin object from the currently loaded device <br>
	 * If the pin does not exist, a ParseException is thrown. <br>
	 * 
	 * @param bel Bel which the pin is attached
	 * @param pinName Name of the bel pin
	 * @return BelPin
	 */
	private BelPin tryGetBelPin(Bel bel, String pinName) {
		
		BelPin pin = bel.getBelPin(pinName);
		
		if (pin == null) {
			throw new ParseException(String.format("BelPin: \"%s/%s\" does not exist in the current device"
												 + "On line %d of %s", bel.getName(), pinName, currentLineNumber, currentFile));
		}
		
		return pin;
	}
	
	/**
	 * Tries to get the BelPin that the specified CellPin is mapped to.
	 * If this function is called, it is expected that the CellPin maps
	 * to exactly one BelPin (it is a source pin). 
	 * @param cellPin CellPin to get the BelPin mapping of
	 * @return BelPin
	 */
	private BelPin tryGetMappedBelPin(CellPin cellPin) {
		
		int mapCount = cellPin.getMappedBelPinCount(); 
		
		if (mapCount != 1) {
			throw new ParseException(String.format("Cell pin source \"%s\" should map to exactly one BelPin, but maps to %d\n"
												+ "On %d of %s", cellPin.getName(), mapCount, currentLineNumber, currentFile));
		}
		
		return cellPin.getMappedBelPin();
	}
	
	
	/**
	 * Tries to retrieve the integer enumeration of a wire name in the currently loaded device <br>
	 * If the wire does not exist, a ParseException is thrown <br>
	 * @param wireName
	 * @return
	 */
	private int tryGetWireEnum(String wireName) {
		
		Integer wireEnum = wireEnumerator.getWireEnum(wireName);
		
		if (wireEnum == null) {
			throw new ParseException(String.format("Wire: \"%s\" does not exist in the current device"
												 + "On line %d of %s", wireName, currentLineNumber, currentFile));
		}
		
		return wireEnum;
	}
	
	/**
	 * Creates a routing.xdc file from the nets of the given design. <br>
	 * This file can be imported into Vivado to constrain the physical location of nets. 
	 * 
	 * @param xdcOut Location to write the routing.xdc file
	 * @param design Design with nets to export
	 * @throws IOException
	 */
	public void writeRoutingXDC(String xdcOut, CellDesign design) throws IOException {
		
		BufferedWriter fileout = new BufferedWriter (new FileWriter(xdcOut));
		
		//write the routing information to the TCL script
		for(CellNet net : design.getNets()) {
			// System.out.println(net.getName());
			// only print nets that have routing information. Grab the first RouteTree of the net and use this as the final route
			if ( net.getIntersiteRouteTree() != null ) {
				fileout.write(String.format("set_property ROUTE %s [get_nets {%s}]\n", getVivadoRouteString(net), net.getName()));
			}
		}
		
		fileout.close();
	}
	
	/**
	 * Creates the Vivado equivalent route string of the specified net. <br>
	 * If the net is a generic net (i.e. not VCC or GND), the first RouteTree <br>
	 * in the net's list of RouteTrees is assumed to be the route to print. <br>
	 * For GND and VCC nets, all RouteTrees in the net's list of RouteTrees <br>
	 * are printed. This function can be used to incrementally update the <br>
	 * ROUTE property of a net in Vivado.
	 *  
	 * @param net CellNet to create a Vivado ROUTE string for
	 * @return Vivado ROUTE string
	 */
	public static String getVivadoRouteString(CellNet net) {
		
		if (net.getType().equals(NetType.WIRE)) {
			// assuming the first RouteTree is the actual route
			RouteTree route = net.getIntersiteRouteTree();// .getRouteTrees().iterator().next();
			return createVivadoRoutingString(route.getFirstSource());
		}
		
		// otherwise we assume its a VCC or GND net, which has a special Route string
		String routeString = "{ ";
		for (RouteTree rt : net.getIntersiteRouteTreeList()) {
			routeString += "( " + createVivadoRoutingString(rt.getFirstSource()) + ") ";
		}
		return routeString + " }"; 		
	}
	
	/*
	 * Creates and formats the route tree into a string that Vivado understands and can be applied to a Vivado net
	 * TODO: refactor...this code is confusing to read
	 */
	private static String createVivadoRoutingString (RouteTree rt) {
		
		RouteTree currentRoute = rt; 
		String routeString = "{ ";
			
		while ( true ) {
			Tile t = currentRoute.getWire().getTile();
			routeString = routeString.concat(t.getName() + "/" + currentRoute.getWire().getWireName() + " ");
						
			ArrayList<RouteTree> children = (ArrayList<RouteTree>) currentRoute.getSinkTrees();
			
			if (children.size() == 0)
				break;
			
			ArrayList<RouteTree> trueChildren = new ArrayList<>();
			for(RouteTree child: children) {
				Connection c = child.getConnection();
				if (c.isPip() || c.isRouteThrough()) {
					trueChildren.add(child);
				}
				else { // if its a regular wire connection and we don't want to add this to the route tree					
					trueChildren.addAll(child.getSinkTrees());
				}
			}
			
			if (trueChildren.size() == 0)
				break;
			
			for(int i = 0; i < trueChildren.size() - 1; i++) 
				routeString = routeString.concat(createVivadoRoutingString(trueChildren.get(i)));
			
			currentRoute = trueChildren.get(trueChildren.size() - 1) ; 
		}
		
		return routeString + "} ";
	}
	
	/* **************
	 * 	Nested Types
	 * **************/
	
	/**
	 * Interface used to create an intrasite route sourced from either a {@link BelPin} or {@link SitePin}
	 * 
	 * @author Thomas Townsend
	 */
	public interface IntrasiteRoute {
		boolean addBelPinSink(BelPin belPin, RouteTree terminal);
		boolean addSitePinSink(SitePin sitePin, RouteTree terminal);
		void pruneRoute();
		void applyRouting();
		boolean isValid();
		RouteTree getStartRoute();
		void setSinksAsRouted();
		boolean isValidBelPinSink(Wire currentWire);
	}
	
	/**
	 * Class used to represent an IntrasiteRoute starting at a {@link SitePin}
	 */
	public final class IntrasiteRouteSitePinSource implements IntrasiteRoute {
		
		private final SitePin source;
		private final CellNet net; 
		private final Set<BelPin> belPinSinks;
		private final Set<RouteTree> terminals;
		private final RouteTree route;
		private final boolean allowUnusedBelPins; 
		
		public IntrasiteRouteSitePinSource (SitePin source, CellNet net, boolean allowUnusedBelPinSinks) {
			this.source = source;
			this.net = net;
			this.belPinSinks = new HashSet<>();
			this.terminals = new HashSet<>();
			this.route = new RouteTree(source.getInternalWire());
			this.allowUnusedBelPins = allowUnusedBelPinSinks;
		}
				
		@Override
		public RouteTree getStartRoute() {
			return route;
		}
	
		@Override
		public boolean addBelPinSink(BelPin belPin, RouteTree terminal) {
			
			CellPin sinkCellPin = belPinToCellPinMap.get(belPin);
			
			if (allowUnusedBelPins) {
				if (sinkCellPin != null) {
					throw new AssertionError("Only unused bel pin sinks expected: " + net.getName() + " " + belPin);
				}
			}
			else {
				if (sinkCellPin == null || !sinkCellPin.getNet().equals(this.net)) {
					return false;
				}
			}
						
			terminals.add(terminal);
			belPinSinks.add(belPin);
			return true;
		}

		@Override
		public boolean addSitePinSink(SitePin sitePin, RouteTree terminal) {
			throw new AssertionError("Intrasite Route starting at input site pin should not reach output site pin " + source);
		}

		@Override
		public void pruneRoute() {
			route.prune(terminals);
		}

		@Override
		public void applyRouting() {

			net.addSinkRouteTree(source, route);
			
			for (BelPin pin : this.belPinSinks) {
				net.addSinkRouteTree(pin, route);
				 
				if (allowUnusedBelPins) {
					createAndAttachPseudoPin(pin);
				}
			}
		}

		@Override
		public boolean isValid() {
			return belPinSinks.size() > 0;
		}
		
		@Override
		public void setSinksAsRouted() {
			// TODO: only do this for used bel pins
			belPinSinks.stream()
						.filter(belPin -> belPinToCellPinMap.containsKey(belPin))
						.map(belPin -> belPinToCellPinMap.get(belPin))
						.forEach(cellPin -> net.addRoutedSink(cellPin));
		}

		@Override
		public boolean isValidBelPinSink(Wire currentWire) {
			
			Collection<Connection> terminals = currentWire.getTerminals(); 
						
			// BEL pin sink is valid if the wire connects to
			// a bel pin and either:
			// (1) The net is a static net (GND or VCC)
			// (2) The BelPin is being used (i.e. a cell pin has been mapped to it)
			return !terminals.isEmpty() &&
					(allowUnusedBelPins || isBelPinUsed(terminals.iterator().next().getBelPin()));
		}
		
		// TODO: document this
		private void createAndAttachPseudoPin(BelPin belPin) {
			Cell cell = design.getCellAtBel(belPin.getBel());
			
			// Leads to a routethrough bel. Currently we cannot attach a pseudo pin to a bel, so we return.
			// TODO: create routethroughs as we import so we will never run into this case?
			if (cell == null) {
				// System.out.println("RouteThrough Test: " + belPin.getBel().getFullName() + "/" + belPin.getName());
				return;
			}
			
			assert (cell != null) : "Expected a cell to be mapped to a bel." ;
			assert (net.isStaticNet()) : "Net should be a static net!";
			String pinName = (net.isVCCNet() ? "VCC_pseudo" : "GND_pseudo") + cell.getPseudoPinCount();
			CellPin pseudo = cell.attachPseudoPin(pinName, belPin.getDirection());
			assert (pseudo != null) : "Pseudo pin should never be null!";
			net.connectToPin(pseudo);
			pseudo.mapToBelPin(belPin);
			belPinToCellPinMap.put(belPin, pseudo);
		}
	}
	
	/**
	 * Class used to represent an IntrasiteRoute starting at a {@link BelPin}
	 */
	public final class IntrasiteRouteBelPinSource implements IntrasiteRoute {
		
		private final BelPin source;
		private final Set<BelPin> belPinSinks;
		private final Set<SitePin> sitePinSinks;
		private final RouteTree route;
		private final Set<RouteTree> terminals;
		private final boolean isContained;
		private final boolean isStatic;
		
		public IntrasiteRouteBelPinSource (BelPin source, boolean isContained, boolean isStatic) {
			this.source = source;
			this.isContained = isContained;
			this.isStatic = isStatic;
			this.belPinSinks = new HashSet<>();
			this.sitePinSinks = new HashSet<>();
			this.terminals = new HashSet<>();
			this.route = new RouteTree(source.getWire());
		}

		@Override
		public boolean addBelPinSink(BelPin belPin, RouteTree terminal) {

			this.belPinSinks.add(belPin);
			this.terminals.add(terminal);
			
			return true;
		}

		@Override
		public boolean addSitePinSink(SitePin sitePin, RouteTree terminal) {
			
			// || isStatic
			if (isContained) {
				throw new AssertionError("Contained instrasite route should not reach site pin: " + sitePin );
			}
						
			sitePinSinks.add(sitePin);
			terminals.add(terminal);
			return true;
		}

		@Override
		public void pruneRoute() {
			route.prune(terminals);
		}

		@Override
		public void applyRouting() {
					
			// statically sourced nets (VCC and GND) don't have a net attached to the bel pin
			if (!isStatic) {
				
				CellNet net = belPinToCellPinMap.get(source).getNet();
				net.setSourceRouteTree(route);
				
				for (SitePin sp : sitePinSinks) {
					net.addSinkRouteTree(sp, route);
				}
			}
			
			for (BelPin bp: belPinSinks) {
				CellNet net = belPinToCellPinMap.get(bp).getNet();
				net.addSinkRouteTree(bp, route);
			}
		}

		@Override
		public boolean isValid() {
			
			// completely intrasite routes should not hit a site pin
			// || isStatic
			if (isContained) {
				return belPinSinks.size() > 0 && sitePinSinks.size() == 0;
			}
			
			return belPinSinks.size() > 0 || sitePinSinks.size() > 0;
		}

		@Override
		public RouteTree getStartRoute() {
			return route;
		}
		
		@Override
		public void setSinksAsRouted() {

			// by definition, if the source of a intrasite is a bel pin,
			// then all bel pin sinks are routed
			for (BelPin belPin : belPinSinks) {
				CellPin cellPin = belPinToCellPinMap.get(belPin);
				cellPin.getNet().addRoutedSink(cellPin);
			}
		}

		@Override
		public boolean isValidBelPinSink(Wire currentWire) {
			Collection<Connection> terminals = currentWire.getTerminals(); 
			
			return !terminals.isEmpty() &&
					isBelPinUsed(terminals.iterator().next().getBelPin());
		}
	}
}
