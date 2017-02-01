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

import edu.byu.ece.rapidSmith.design.AbstractDesign;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.util.Exceptions;
import edu.byu.ece.rapidSmith.interfaces.vivado.XdcConstraint;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	private static final long serialVersionUID = -807318199842395826L;
	/** This is a list of all the cells in the design */
	private Map<String, Cell> cellMap;
	/** A map used to keep track of all used primitive sites used by the design */
	private Map<Site, Map<Bel, Cell>> placementMap;
	/** This is a list of all the nets in the design */
	private Map<String, CellNet> netMap;
	/** The properties of this design. */
	private final PropertyList properties;
	/** Map from a site to the used SitePip wires in the site*/
	private HashMap<Site, Set<Integer>> usedSitePipsMap;
	/** The VCC RapidSmith net */
	private CellNet vccNet;
	/** The GND RapidSmith net */
	private CellNet gndNet;
	/** List of Vivado constraints on the design **/
	private List<XdcConstraint> vivadoConstraints;
	
	/**
	 * Constructor which initializes all member data structures. Sets name and
	 * partName to null.
	 */
	public CellDesign() {
		super();
		init();
		properties = new PropertyList();
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
		properties = new PropertyList();
	}

	private void init() {
		cellMap = new HashMap<>();
		placementMap = new HashMap<>();
		netMap = new HashMap<>();
		usedSitePipsMap = new HashMap<>();
	}

	/**
	 * Returns the properties of this design in a {@link PropertyList}.  Properties
	 * may contain metadata about a design including user-defined metadata.
	 * @return a {@code PropertyList} containing the properties of this design
	 */
	public PropertyList getProperties() {
		return properties;
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
	 * Returns a stream of {@link Cell} objects of the specified type. 
	 * TODO: Should there be a link to a {@link CellLibrary} in this class?
	 * 
	 * @param libCellType Name of the {@link LibraryCell} to filter by
	 */
	public Stream<Cell> getCellsOfType(String libCellType, CellLibrary library) {
		LibraryCell libraryCell = library.get(libCellType);
		
		if (libraryCell == null) {
			Stream.empty();
		}
		
		return cellMap.values().stream().filter(c -> c.getLibCell() == libraryCell);
	}

	/**
	 * Returns a stream of {@link Cell} objects that are top-level ports of the design.
	 */
	public Stream<Cell> getPorts(){
		return cellMap.values().stream().filter(c -> c.isPort());
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
			throw new Exceptions.DesignAssemblyException("Cell already in a design.");

		return registerCell(cell);
	}

	private Cell registerCell(Cell cell) {
		if (hasCell(cell.getName()))
			throw new Exceptions.DesignAssemblyException("Cell with name already exists in design.");

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
			throw new Exceptions.DesignAssemblyException("Cannot remove cell not in the design.");

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
			throw new Exceptions.DesignAssemblyException("Cannot disconnect cell not in the design.");

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
			throw new Exceptions.DesignAssemblyException("Cannot add net from another design.");

		return addNet_impl(net);
	}

	protected CellNet addNet_impl(CellNet net) {
		if (hasNet(net.getName()))
			throw new Exceptions.DesignAssemblyException("Net with name already exists in design.");

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
			throw new Exceptions.DesignAssemblyException("Cannot remove connected net.");

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
			throw new Exceptions.DesignAssemblyException("Cannot disconnect net not in the design.");

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
			throw new Exceptions.DesignAssemblyException("Cannot place cell not in the design.");
		if (cell.isPlaced())
			throw new Exceptions.DesignAssemblyException("Cannot re-place cell.");

		List<Bel> requiredBels = cell.getLibCell().getRequiredBels(anchor);
		if (!canPlaceCellAt_impl(requiredBels))
			throw new Exceptions.DesignAssemblyException("Cell already placed at location.");

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
			throw new Exceptions.DesignAssemblyException("Cannot unplace cell not in the design.");

		unplaceCell_impl(cell);
	}

	private void unplaceCell_impl(Cell cell) {
		assert cell.getDesign() == this;
		assert cell.isPlaced();

		Site site = cell.getSite();
		Map<Bel, Cell> sitePlacementMap = placementMap.get(site);
		sitePlacementMap.remove(cell.getBel());
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

	public void setUsedSitePipsAtSite(Site ps, Set<Integer> usedWires) {
		this.usedSitePipsMap.put(ps, usedWires);
	}

	public  Set<Integer> getUsedSitePipsAtSite(Site ps) {
		return this.usedSitePipsMap.getOrDefault(ps, Collections.emptySet());
	}

	/**
	 * Returns a list of XDC contraints on the design.
	 */
	public List<XdcConstraint> getVivadoConstraints() {
		return this.vivadoConstraints;
	}
	
	/**
	 * Add an XDC constraint to the design
	 * @param constraint {@link XdcConstraint} to add to the design
	 */
	public void addVivadoConstraint(XdcConstraint constraint) {
		
		if (this.vivadoConstraints == null) {
			vivadoConstraints = new ArrayList<>();
		}
		vivadoConstraints.add(constraint);
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
				designCopy.placeCell(cellCopy, cell.getBel());
				for (CellPin cellPin : cell.getPins()) {
					if (cellPin.getMappedBelPinCount() > 0) {
						CellPin copyPin = cellCopy.getPin(cellPin.getName());
						cellPin.getMappedBelPins().forEach(copyPin::mapToBelPin);
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
