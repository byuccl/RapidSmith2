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
	/** This is a list of all internal cell in the design*/
	private Map<String, Cell> internalCellMap;
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
		internalCellMap = new HashMap<>();
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

	/**
	 * Returns {@code true} if a {@link Cell} with the specified name
	 * is in the design, {@code false} otherwise. The cell can be a leaf 
	 * cell, macro cell, or internal cell. 
	 * 
	 * @param fullName Name of a cell
	 */
	public boolean hasCell(String fullName) {
		Objects.requireNonNull(fullName);

		return cellMap.containsKey(fullName) || internalCellMap.containsKey(fullName);
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
	 * Returns the cell in this design with the specified name.  
	 *
	 * @param fullName name of the cell to return
	 * @return the cell, or null if it does not exist
	 */
	public Cell getCell(String fullName) {
		Objects.requireNonNull(fullName);

		// first look in the regular cell map
		Cell firstAttempt = cellMap.get(fullName); 
		
		// If it isn't found there, look in the internalCellMap
		return firstAttempt == null ? internalCellMap.get(fullName) : firstAttempt;
	}

	/**
	 * Returns all of the cells in this design.  The returned collection should not
	 * be modified. It returns the leaf cells and macro cells of the design.
	 *
	 * @return the cells in this design
	 */
	public Collection<Cell> getCells() {
		return cellMap.values();
	}
	
	/**
	 * Returns a flattened view of the cells in the netlist. Macro
	 * cells are not returned in this list, only leaf and internal cells
	 * are returned.
	 */
	public Stream<Cell> getLeafCells() {
		return cellMap.values().stream().flatMap(c -> flatten(c));
	}
	
	/**
	 * Returns a list of only the internal cells of the specified cell. If the input
	 * cell is not a macro, then a singleton list is returned. The lists are converted to
	 * streams before returning.
	 * 
	 * @param cell {@link Cell} object to flatten
	 * @return A {@link Stream} of internal cells
	 */
	private Stream<Cell> flatten(Cell cell) {
		return cell.isMacro() ? cell.getInternalCells().stream() : Collections.singletonList(cell).stream();
	}
	
	/**
	 * Returns a {@link Stream} of all cells in the design that are macro cells.
	 */
	public Stream<Cell> getMacros() {
		return cellMap.values().stream().filter(Cell::isMacro);
	}
	
	/**
	 * Returns a stream of {@link Cell} objects of the specified type. 
	 * 
	 * @param libCellType Name of the {@link LibraryCell} to filter by
	 */
	public Stream<Cell> getCellsOfType(LibraryCell libraryCell) {
		Objects.requireNonNull(libraryCell, "LibraryCell parameter cannot be null");
				
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
		if (cell.isInternal())
			throw new Exceptions.DesignAssemblyException("Cannot add internal cell to design. Must add parent macro instead.");
		
		return registerCell(cell);
	}

	private Cell registerCell(Cell cell) {
		if (hasCell(cell.getName()))
			throw new Exceptions.DesignAssemblyException("Cell with name already exists in design.");

		cell.setDesign(this);
		cellMap.put(cell.getName(), cell);
		
		// add all internal nets when a macro is added to the design
		if (cell.isMacro()) {
			for (Cell internal : cell.getInternalCells()) {
				internalCellMap.put(internal.getName(), internal);
				internal.setDesign(this);
			}
			cell.getInternalNets().forEach(this::addNet);
		}
		
		return cell;
	}

	/**
	 * Disconnects and removes the specified cell from this design. For macro cells,
	 * all internal nets and cells are also removed from the design. Internal cells
	 * cannot be removed from the design, an exception will be thrown. Instead,
	 * remove the parent macro cell. 
	 *
	 * @param cell the cell in this design to remove
	 */
	public void removeCell(Cell cell) {
		Objects.requireNonNull(cell);
		if (cell.getDesign() != this)
			throw new Exceptions.DesignAssemblyException("Cannot remove cell not in the design.");
		if (cell.isInternal()) 
			throw new IllegalArgumentException("Cannot remove internal cell from the design. Remove the macro parent cell.");
		
		removeCell_impl(cell);
	}

	private void removeCell_impl(Cell cell) {
		
		cellMap.remove(cell.getName());
		cell.clearDesign();
		
		// remove all of the internal cells and nets if a macro cell is removed
		if (cell.isMacro()) {
			for (Cell iCell: cell.getInternalCells()) {
				iCell.clearDesign();
				unplaceCell_impl(iCell);
				internalCellMap.remove(iCell.getName());
			}
			cell.getPins().stream().filter(CellPin::isConnectedToNet).forEach(p -> p.getNet().disconnectFromPin(p));
			cell.getInternalNets().forEach(this::removeNet_impl);
		}
		else {
			disconnectCell_impl(cell);
		}
	}

	/**
	 * Disconnects without removing the specified cell from this design.  
	 * Internal cells cannot be disconnected and if attempted, this method will
	 * throw a DesignAssemblyException.
	 *
	 * @param cell the cell to disconnect from this design
	 */
	public void disconnectCell(Cell cell) {
		Objects.requireNonNull(cell);
		if (cell.getDesign() != this)
			throw new Exceptions.DesignAssemblyException("Cannot disconnect cell not in the design.");
		if (cell.isInternal())
			throw new IllegalArgumentException("Cannot disconnect internal cell. Disconnet macro cell instead");
		
		// for macros, disconnect the sub-cells
		if (cell.isMacro()) {
			cell.getInternalCells().forEach(this::unplaceCell_impl);
			cell.getPins().stream().filter(CellPin::isConnectedToNet).forEach(p -> p.getNet().disconnectFromPin(p));
		}
		else { // leaf cell
			disconnectCell_impl(cell);
		}
	}

	private void disconnectCell_impl(Cell cell) {

		unplaceCell_impl(cell);

		// disconnect the cell's pins from their nets
		for (CellPin pin : cell.getPins()) {
			CellNet net = pin.getNet();
			if (net != null)
				net.disconnectFromPin(pin);
		}
	}

	/**
	 * Returns {@code true} if the {@link CellNet} with the specified name
	 * is currently in the design. Internal nets to macros will be returned from this function
	 * 
	 * @param netName String name of net to find in the design
	 */
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

	/**
	 * Returns a collection of {@link CellNet}s currently in the design.
	 * Internal macro nets are included in the list.
	 */
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
		if (net.isInternal())
			throw new IllegalArgumentException("Cannot remove internal net " + net.getName());
		if (!net.getPins().isEmpty())
			throw new Exceptions.DesignAssemblyException("Cannot remove connected net." + net.getName());

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
	 * Internal nets should not be specified in this function. Internal macro nets
	 * cannot be disconnected.
	 * 
	 * @param net the net to disconnect
	 */
	public void disconnectNet(CellNet net) {
		Objects.requireNonNull(net);
		if (net.getDesign() != this)
			throw new Exceptions.DesignAssemblyException("Cannot disconnect net not in the design.");
		if (net.isInternal()) 
			throw new Exceptions.DesignAssemblyException("Cannot disconnect internal net.");
		
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
	 * Returns the cell at the specified BEL in this design. Only internal and
	 * leaf cells will be returned from this function. Macro cells will not be 
	 * returned since they cannot be placed.
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
	 * Returns a collection of cells at the specified site in this design. Only internal and
	 * leaf cells will be returned from this function. Macro cells will not be 
	 * returned since they cannot be placed.
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
	
	/**
	 * Returns a collections of {@link Site}s that currently have
	 * one or more {@Cell} objects placed there. 
	 */
	public Collection<Site> getUsedSites() {
		return placementMap.keySet();
	}

	/**
	 * Returns {@code true} if the specified {@link Cell} can be placed onto
	 * the specified {@link Bel}.
	 * 
	 * @param cell {@link Cell} to place
	 * @param anchor {@link Bel} to place the cell on
	 */
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
	 * CellPins are not mapped to BelPins in this function. 
	 * In general, best placement practices are to wait until the BEL placement
	 * is finalized, and then apply the pin mappings.
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
			throw new Exceptions.DesignAssemblyException("Cell is already placed. Cannot re-place cell.");
		if (cell.isMacro()) 
			throw new Exceptions.DesignAssemblyException("Cannot place macro cell. Can only place internal cells to the macro.");
		
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
	 * Unplaces the specified cell in this design. The input cell can either be a leaf cell,
	 * or a macro cell. If the specified cell is a macro cell, all internal cells will be unplaced.
	 * This function also undoes any pin mappings of the cell. 
	 *
	 * @param cell the {@link Cell} to unplace.
	 */
	public void unplaceCell(Cell cell) {
		Objects.requireNonNull(cell);
		if (cell.getDesign() != this)
			throw new Exceptions.DesignAssemblyException("Cannot unplace cell not in the design.");

		// for macros, unplace all internal cells
		if (cell.isMacro()) {
			cell.getInternalCells().forEach(this::unplaceCell_impl);
		}
		else {
			unplaceCell_impl(cell);
		}
	}

	private void unplaceCell_impl(Cell cell) {
		
		assert(!cell.isMacro());
		
		// if the cell is not placed, return
		if (!cell.isPlaced()) {
			return;
		}
		
		Site site = cell.getSite();
		Map<Bel, Cell> sitePlacementMap = placementMap.get(site);
		sitePlacementMap.remove(cell.getBel());
		if (sitePlacementMap.size() == 0)
			placementMap.remove(site);
		
		cell.unplace();
		
		// undo all cell pin mappings (if they exists)
		cell.getPins().forEach(CellPin::clearPinMappings);
	}

	/**
	 * Unroutes the INTERSITE portions of all nets currently in the design.
	 * This function is currently not recommended for use. Further testing is needed.
	 */
	@Deprecated
	public void unrouteDesign() {
		getNets().forEach(CellNet::unroute);
	}
	
	/**
	 * Unplaces the design. The design is first unrouted. All CellPin to BelPin 
	 * mappings are undone as well. This function is currently not recommended for use.
	 * Further testing is needed.   
	 */
	@Deprecated
	public void unplaceDesign() {
		unrouteDesign();

		for (Cell cell : getCells()) {
			if (cell.isMacro()) {
				cell.getInternalCells().forEach(this::unplaceCell_impl);
			}
			else {
				this.unplaceCell_impl(cell);
			}
		}
	}
	
	/**
	 * Set the INTRASITE routing of a {@link Site}. If you are modifying the logic within 
	 * a {@link Site}, this function needs to be called before exporting a design. 
	 * 
	 * @param ps {@link Site} to route
	 * @param usedWires Set of wire enumerations that are used within a site.
	 */
	public void setUsedSitePipsAtSite(Site ps, Set<Integer> usedWires) {
		this.usedSitePipsMap.put(ps, usedWires);
	}

	/**
	 * Returns the used wires (as enumerations), of the specified {@link Site}
	 * 
	 * @param ps {@link Site} object
	 */
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
	 * Creates and returns a deep copy of the current CellDesign.
	 */
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
