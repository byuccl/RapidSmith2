package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.design.AbstractDesign;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.Site;

import java.util.*;

/**
 *  This class represents a logical netlist consisting of cells interconnected by
 *  nets along with cell placement information.  This class supports constant time
 *  look up of cells by both name and location.
 *
 *  Placement information of cells in the CellDesign are updated through calls to
 *  {@link #placeCell(Cell, edu.byu.ece.rapidSmith.device.Bel)}.  The
 *  placement information is stored in a two-level map of primitive sites and
 *  BELs allowing quick checking of cells located at both levels of hierarchy.
 *
 *  CellDesigns preserve attributes found in XDL designs for simpler conversion
 *  back to XDL.
 */
public class CellDesign extends AbstractDesign {
	/** This is a list of all the cells in the design */
	private Map<String, Cell> cellMap;
	/** A map used to keep track of all used primitive sites used by the design */
	private Map<Site, Map<Bel, Cell>> placementMap;
	/** This is a list of all the nets in the design */
	private Map<String, CellNet> netMap;
	/** Map of properties of this design. */
	private Map<Object, Property> propertyMap;
	/** Map from a site to the used SitePip wires in the site*/
	private HashMap<Site, HashSet<Integer>> usedSitePipsMap;
	/** The VCC RapidSmith net */
	private CellNet vccNet;
	/** The GND RapidSmith net */
	private CellNet gndNet;
	
	/**
	 * Constructor which initializes all member data structures. Sets name and
	 * partName to null.
	 */
	public CellDesign() {
		super();
		init();
	}

	/**
	 * Creates a new design and populates it with the given design name and
	 * part name.
	 *
	 * @param designName The name of the newly created design.
	 * @param partName   The target part name of the newly created design.
	 */
	public CellDesign(String designName, String partName) {
		super(designName, partName);
		init();
	}

	private void init() {
		cellMap = new HashMap<>();
		placementMap = new HashMap<>();
		netMap = new HashMap<>();
		usedSitePipsMap = new HashMap<>();
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
	 * Removes the property with the specified name.  Returns the removed property.
	 *
	 * @param propertyKey the key of the property to remove
	 * @return the removed property
	 */
	public Property removeProperty(Object propertyKey) {
		Objects.requireNonNull(propertyKey);

		if (propertyMap == null)
			return null;
		return propertyMap.remove(propertyKey);
	}
	
	public boolean hasCell(String fullName) {
		Objects.requireNonNull(fullName);

		return cellMap.containsKey(fullName);
	}

	public String getUniqueCellName(String proposedName) {
		if (!hasCell(proposedName))
			return proposedName;

		String newName;
		int i = 0;
		do {
			i++;
			newName = proposedName + "_" + i;
		} while (hasCell(newName));
		return newName;
	}

	/**
	 * Returns the cell in this design with the specified name.  In the case of
	 * molecules, cells are stored using their full hierarchical names.
	 *
	 * @param fullName name of the cell to return
	 * @return the cell, or null if it does not exist
	 */
	public Cell getCell(String fullName) {
		Objects.requireNonNull(fullName);

		return cellMap.get(fullName);
	}

	/**
	 * Returns all of the cells in this design.  The returned collection should not
	 * be modified.
	 *
	 * @return the cells in this design
	 */
	public Collection<Cell> getCells() {
		return cellMap.values();
	}

	/**
	 * Adds a cell to this design.  The name of this added cell should be unique
	 * to this design.  The cell should not be part of another design and should
	 * not have any placement information.  Returns the added cell for convenience.
	 *
	 * @param cell the cell to add
	 * @return the added cell
	 */
	public Cell addCell(Cell cell) {
		Objects.requireNonNull(cell);
		if (cell.isInDesign())
			throw new DesignAssemblyException("Cell already in a design.");

		return registerCell(cell);
	}

	private Cell registerCell(Cell cell) {
		if (hasCell(cell.getName()))
			throw new DesignAssemblyException("Cell with name already exists in design.");

		cell.setDesign(this);
		cellMap.put(cell.getName(), cell);
		return cell;
	}

	/**
	 * Disconnects and removes the specified cell from this design.
	 *
	 * The cell should not be a part of a molecule.
	 *
	 * @param cell the cell in this design to remove
	 */
	public void removeCell(Cell cell) {
		Objects.requireNonNull(cell);
		if (cell.getDesign() != this)
			throw new DesignAssemblyException("Cannot remove cell not in the design.");

		removeCell_impl(cell);
	}

	private void removeCell_impl(Cell cell) {
		disconnectCell_impl(cell);
		cellMap.remove(cell.getName());
		cell.clearDesign();
	}

	/**
	 * Disconnects without removing the specified cell from this design.  This is
	 * accomplished by unplacing the cell and disconnecting all of its pins.
	 *
	 * The cell should not be a part of a molecule.
	 *
	 * @param cell the cell to disconnect from this design
	 */
	public void disconnectCell(Cell cell) {
		Objects.requireNonNull(cell);
		if (cell.getDesign() != this)
			throw new DesignAssemblyException("Cannot disconnect cell not in the design.");

		disconnectCell_impl(cell);
	}

	private void disconnectCell_impl(Cell cell) {
		if (cell.isPlaced())
			unplaceCell_impl(cell);

		// disconnect the cell's pins from their nets
		for (CellPin pin : cell.getPins()) {
			CellNet net = pin.getNet();
			if (net != null)
				net.disconnectFromPin(pin);
		}
	}

	public boolean hasNet(String netName) {
		Objects.requireNonNull(netName);

		return netMap.containsKey(netName);
	}

	public String getUniqueNetName(String proposedName) {
		if (!hasNet(proposedName))
			return proposedName;

		String newName;
		int i = 0;
		do {
			i++;
			newName = proposedName + "_" + i;
		} while (hasNet(newName));
		return newName;
	}


	/**
	 * Returns the net in this design with the specified name.  Nets of molecules
	 * are stored using their full hierarchical name.
	 *
	 * @param netName name of the net to return
	 * @return the net with the specified name, or null if it does not exist
	 */
	public CellNet getNet(String netName) {
		Objects.requireNonNull(netName);

		return netMap.get(netName);
	}

	public Collection<CellNet> getNets() {
		return netMap.values();
	}

	/**
	 * Adds a net to this design.  Names of nets must be unique.
	 *
	 * @param net The net to add.
	 */
	public CellNet addNet(CellNet net) {
		Objects.requireNonNull(net);
		if (net.isInDesign())
			throw new DesignAssemblyException("Cannot add net from another design.");

		return addNet_impl(net);
	}

	protected CellNet addNet_impl(CellNet net) {
		if (hasNet(net.getName()))
			throw new DesignAssemblyException("Net with name already exists in design.");

		if (net.isVCCNet()) {
			// if (vccNet != null) {
			// 	throw new DesignAssemblyException("VCC net already exists in design.");
			// }
			vccNet = net;
		}
		else if (net.isGNDNet()) {
			// if (gndNet != null) {
			// 	throw new DesignAssemblyException("GND net already exists in design.");
			// }
			gndNet = net;
		} 
		
		// TODO: should VCC and GND nets be added to the net data structure
		netMap.put(net.getName(), net);
		net.setDesign(this);
		
		return net;
	}

	/**
	 * Disconnects and removes a net from this design.
	 *
	 * @param net the net to remove from this design
	 */
	public void removeNet(CellNet net) {
		Objects.requireNonNull(net);
		if (net.getDesign() != this)
			return;
		if (!net.getPins().isEmpty())
			throw new DesignAssemblyException("Cannot remove connected net.");

		removeNet_impl(net);
	}

	private void removeNet_impl(CellNet net) {
		net.setDesign(null);
		
		if (net.isVCCNet()) {
			vccNet = null;
		} 
		else if (net.isGNDNet()) {
			gndNet = null;
		}
		else {
			netMap.remove(net.getName());
		}
	}

	/**
	 * Disconnects the specified net from this design without removing it.  This
	 * method unroutes the net and removes it from the netlist of the pins it is on.
	 *
	 * @param net the net to disconnect
	 */
	public void disconnectNet(CellNet net) {
		Objects.requireNonNull(net);
		if (net.getDesign() != this)
			throw new DesignAssemblyException("Cannot disconnect net not in the design.");

		disconnectNet_impl(net);
	}

	private void disconnectNet_impl(CellNet net) {
		List<CellPin> pins = new ArrayList<>(net.getPins());
		pins.forEach(net::disconnectFromPin);
		net.unroute();
	}

	/**
	 * Returns the power(VCC) net of the design
	 * 
	 * @return
	 */
	public CellNet getVccNet() {
		return vccNet;
	}
	
	/**
	 * Returns the ground(GND) net of the design
	 * 
	 * @return
	 */
	public CellNet getGndNet() {
		return gndNet;
	}

	/**
	 * Returns the cell at the specified BEL in this design.
	 *
	 * @param bel the BEL of the desired cell
	 * @return the cell at specified BEL, or null if the BEL is unoccupied
	 */
	public Cell getCellAtBel(Bel bel) {
		Objects.requireNonNull(bel);

		Map<Bel, Cell> sitePlacementMap = placementMap.get(bel.getSite());
		if (sitePlacementMap == null)
			return null;
		return sitePlacementMap.get(bel);
	}

	/**
	 * Returns a collection of cells at the specified site in this design.
	 *
	 * @param site the site of the desired cells
	 * @return the instance at site, or null if the primitive site is unoccupied
	 */
	public Collection<Cell> getCellsAtSite(Site site) {
		Objects.requireNonNull(site);

		Map<Bel, Cell> sitePlacementMap = placementMap.get(site);
		if (sitePlacementMap == null)
			return null;
		return sitePlacementMap.values();
	}

	/**
	 * Tests if the specified BEL is occupied in this design.
	 *
	 * @param bel the BEL to test
	 * @return true if a cell is placed at the BEL, else false
	 */
	public boolean isBelUsed(Bel bel) {
		Objects.requireNonNull(bel);

		Map<Bel, Cell> sitePlacementMap = placementMap.get(bel.getSite());
		return sitePlacementMap != null && sitePlacementMap.containsKey(bel);
	}

	/**
	 * Tests if any BELs in the the specified primitive site are occupied in
	 * this design.
	 *
	 * @param site the site to test
	 * @return true if this design uses any BELs in site, else false
	 */
	public boolean isSiteUsed(Site site) {
		Objects.requireNonNull(site);

		Map<Bel, Cell> sitePlacementMap = placementMap.get(site);
		return sitePlacementMap != null && !sitePlacementMap.isEmpty();
	}

	public Collection<Site> getUsedSites() {
		return placementMap.keySet();
	}

	public boolean canPlaceCellAt(Cell cell, Bel anchor) {
		List<Bel> requiredBels = cell.getLibCell().getRequiredBels(anchor);
		return canPlaceCellAt_impl(requiredBels);
	}

	private boolean canPlaceCellAt_impl(List<Bel> requiredBels) {
		for (Bel bel : requiredBels) {
			if (bel == null || isBelUsed(bel))
				return false;
		}
		return true;
	}

	/**
	 * Places the cell at the specified BEL in this design.
	 * No cells should exist at the specified BEL in this design.
	 *
	 * @param cell the cell to place
	 * @param anchor the BEL where the cell is to be placed
	 */
	public void placeCell(Cell cell, Bel anchor) {
		Objects.requireNonNull(cell);
		Objects.requireNonNull(anchor);
		if (cell.getDesign() != this)
			throw new DesignAssemblyException("Cannot place cell not in the design.");
		if (cell.isPlaced())
			throw new DesignAssemblyException("Cannot re-place cell.");

		List<Bel> requiredBels = cell.getLibCell().getRequiredBels(anchor);
		if (!canPlaceCellAt_impl(requiredBels))
			throw new DesignAssemblyException("Cell already placed at location.");

		placeCell_impl(cell, anchor, requiredBels);
	}

	private void placeCell_impl(Cell cell, Bel anchor, List<Bel> requiredBels) {
		requiredBels.forEach(b -> placeCellAt(cell, b));
		cell.place(anchor);
	}

	private void placeCellAt(Cell cell, Bel bel) {
		Map<Bel, Cell> sitePlacementMap = placementMap.get(bel.getSite());
		if (sitePlacementMap == null) {
			sitePlacementMap = new HashMap<>();
			placementMap.put(bel.getSite(), sitePlacementMap);
		} else {
			assert sitePlacementMap.get(bel) == null;
		}
		sitePlacementMap.put(bel, cell);
	}

	/**
	 * Unplaces the cell in this design.  The cell must be placed in this design
	 * and not be a part of a relatively-placed molecule.
	 *
	 * @param cell the cell to unplace.
	 */
	public void unplaceCell(Cell cell) {
		Objects.requireNonNull(cell);
		if (cell.getDesign() != this)
			throw new DesignAssemblyException("Cannot unplace cell not in the design.");

		unplaceCell_impl(cell);
	}

	private void unplaceCell_impl(Cell cell) {
		assert cell.getDesign() == this;
		assert cell.isPlaced();

		Site site = cell.getAnchorSite();
		Map<Bel, Cell> sitePlacementMap = placementMap.get(site);
		sitePlacementMap.remove(cell.getAnchor());
		if (sitePlacementMap.size() == 0)
			placementMap.remove(site);
		cell.unplace();
	}

	/**
	 * Unroutes the current design by removing all PIPs.
	 */
	public void unrouteDesign() {
		// Just remove all the PIPs
		getNets().forEach(CellNet::unroute);

		for (Cell cell : getCells()) {
			cell.getPins().forEach(CellPin::clearPinMappings);
		}
	}

	public void setUsedSitePipsAtSite(Site ps, HashSet<Integer> usedWires) {
		this.usedSitePipsMap.put(ps, usedWires);
	}

	public  HashSet<Integer> getUsedSitePipsAtSite(Site ps) {
		return this.usedSitePipsMap.get(ps);
	}

	/**
	 * Unplaces the design.  The design is first unrouted.
	 */
	public void unplaceDesign() {
		unrouteDesign();

		getCells().forEach(this::unplaceCell_impl);
	}

	public CellDesign deepCopy() {
		CellDesign designCopy = new CellDesign();
		designCopy.setName(getName());
		designCopy.setPartName(getPartName());

		for (Cell cell : getCells()) {
			Cell cellCopy = cell.deepCopy();
			designCopy.addCell(cellCopy);
			if (cell.isPlaced()) {
				designCopy.placeCell(cellCopy, cell.getAnchor());
				for (CellPin cellPin : cell.getPins()) {
					if (cellPin.getMappedBelPinCount() > 0) {
						CellPin copyPin = cellCopy.getPin(cellPin.getName());
						cellPin.getMappedBelPins().forEach(pin -> copyPin.mapToBelPin(pin));
					}
				}
			}
		}

		for (CellNet net : getNets()) {
			CellNet netCopy = net.deepCopy();
			designCopy.addNet(netCopy);
			for (CellPin cellPin : net.getPins()) {
				Cell cellCopy = designCopy.getCell(cellPin.getCell().getName());
				CellPin copyPin = cellCopy.getPin(cellPin.getName());
				netCopy.connectToPin(copyPin);
			}
		}

		return designCopy;
	}
}
