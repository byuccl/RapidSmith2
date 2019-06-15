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
import edu.byu.ece.rapidSmith.device.PinDirection;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.interfaces.vivado.XdcConstraint;
import edu.byu.ece.rapidSmith.util.Exceptions;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;

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
	/** For design imported from Vivado, this fields contains how Vivado implemented the design (regular or out-of-context)*/
	private ImplementationMode mode;
	/** Map of used PIPs to their Input Values in a Site **/
	private Map<Site, Map<String, String>> pipInValues;
	/** Map of partition pins (ooc ports) to their ooc tile and node **/
	private Map<String, String> partPinMap;
	/** Set of reserved sites */
	private Set<Site> reservedSites;


	// TODO: Should be un-modifiable?
	//rivate Set<Wire> reservedWires;
	private Map<Wire, Set<CellNet>> reservedWires;

	// TODO: Re-think these three
	/**Map of out-of-context ports to their ooc tile and node **/
	private Map<String, String> oocPortMap;
	/** Map from RM port name(s) to the static net's name */
	private Map<String, String> reconfigStaticNetMap;
	/** Map from the static net name to the route string tree */
	private Map<String, RouteStringTree> staticRouteStringMap;

	/**
	 * Constructor which initializes all member data structures. Sets name and
	 * partName to null.
	 */
	public CellDesign() {
		super();
		_init();
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
		_init();
		properties = new PropertyList();
	}

	private void _init() {
		cellMap = new HashMap<>();
		internalCellMap = new HashMap<>();
		placementMap = new HashMap<>();
		netMap = new HashMap<>();
		usedSitePipsMap = new HashMap<>();
		mode = ImplementationMode.REGULAR;
		pipInValues = new HashMap<>();
		//reservedWires = new HashSet<>();
		reservedWires = new HashMap<>();
		reservedSites = new HashSet<>();
	}

	/**
	 * Sets the implementation mode of the design. There are currently two options
	 * for the design mode: 
	 * <ul>
	 * <li> REGULAR
	 * <li> OUT_OF_CONTEXT
	 * </ul>
	 * 
	 * These match the possible implementation modes in Vivado. On RSCP import,
	 * the implementation mode is set to match that of the Vivado design.
	 * 
	 * @param mode {@link ImplementationMode} to set the design
	 */
	public void setImplementationMode(ImplementationMode mode) {
		this.mode = mode;
	}
	
	/**
	 * Returns the {@link ImplementationMode} of the design
	 */
	public ImplementationMode getImplementationMode() {
		return this.mode;
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
		return cellMap.values().stream().flatMap(c -> _flatten(c));
	}
	
	/**
	 * Returns a flattened view of the in-context cells in the netlist. Macro
	 * cells are not returned in this list, only leaf and internal cells
	 * are returned.
	 * WARNING: Ports are assumed to be out-of-context. This is true for 7-Series RMs, but is
	 * not necessarily true for ultrascale RMs.
	 */
	public Stream<Cell> getInContextLeafCells() {
		// TODO: For non 7-series designs, determine which ports are in-context and which are out-of-context
		// TODO: Also check ImplementationMode.OUT_OF_CONTEXT
		if (this.mode.equals(ImplementationMode.RECONFIG_MODULE))
			return (cellMap.values().stream().flatMap(c -> _flatten(c))).filter(it -> !it.isPort());
		else
			return cellMap.values().stream().flatMap(c -> _flatten(c));
	}


    /**
	 * Returns a list of only the internal cells of the specified cell. If the input
	 * cell is not a macro, then a singleton list is returned. The lists are converted to
	 * streams before returning.
	 * 
	 * @param cell {@link Cell} object to flatten
	 * @return A {@link Stream} of internal cells
	 */
	private Stream<Cell> _flatten(Cell cell) {
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
	 * @param libraryCell Name of the {@link LibraryCell} to filter by
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
		
		return _registerCell(cell);
	}

	private Cell _registerCell(Cell cell) {
		if (hasCell(cell.getName()))
			throw new Exceptions.DesignAssemblyException("Cell with name already exists in design: " + cell.getName());

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
		
		_removeCell(cell);
	}

	private void _removeCell(Cell cell) {
		
		cellMap.remove(cell.getName());
		cell.clearDesign();
		
		// remove all of the internal cells and nets if a macro cell is removed
		if (cell.isMacro()) {
			for (Cell iCell: cell.getInternalCells()) {
				iCell.clearDesign();
				_unplaceCell(iCell);
				internalCellMap.remove(iCell.getName());
			}
			cell.getPins().stream().filter(CellPin::isConnectedToNet).forEach(p -> p.getNet().disconnectFromPin(p));
			cell.getInternalNets().forEach(this::_removeNet);
		}
		else {
			_disconnectCell(cell);
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
			cell.getInternalCells().forEach(this::_unplaceCell);
			cell.getPins().stream().filter(CellPin::isConnectedToNet).forEach(p -> p.getNet().disconnectFromPin(p));
		}
		else { // leaf cell
			_disconnectCell(cell);
		}
	}

	private void _disconnectCell(Cell cell) {

		_unplaceCell(cell);

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

		return _addNet(net);
	}

	private CellNet _addNet(CellNet net) {
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

		_removeNet(net);
	}

	private void _removeNet(CellNet net) {
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
		
		_disconnectNet(net);
	}

	private void _disconnectNet(CellNet net) {
		List<CellPin> pins = new ArrayList<>(net.getPins());
		pins.forEach(net::disconnectFromPin);
		net.unrouteFull();
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
	 * Get all nets that have more than one port as a sink
	 * @return
	 */
	public Collection<CellNet> getMultiPortSinkNets() {
		Collection<CellNet> nets = new ArrayList<>();
		for (CellNet net : getNets()) {
			if (net.getPins().stream().filter(cellPin -> cellPin.getDirection().equals(PinDirection.IN) && cellPin.getCell().isPort()).count() > 1)
				nets.add(net);
		}
		return nets;
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

	public Collection<Bel> getUsedBelsAtSite(Site site) {
		Objects.requireNonNull(site);

		return placementMap.get(site).keySet();
	}

	public Collection<Bel> getUsedBels() {
		Collection<Bel> usedBels = new ArrayList<>();
		for (Site site : getUsedSites()) {
			usedBels.addAll(placementMap.get(site).keySet());
		}
		return usedBels;
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
	 * one or more {@link Cell} objects placed there.
	 */
	public Collection<Site> getUsedSites() {
		return placementMap.keySet();
	}

	/**
	 * Returns {@code true} if the specified {@link Cell} can be placed onto
	 * the specified {@link Bel}.
	 * 
	 * @param cell {@link Cell} to place
	 * @param bel {@link Bel} to place the cell on
	 */
	public boolean canPlaceCellAt(Cell cell, Bel bel) {
		return bel != null && !isBelUsed(bel) &&
			cell.getLibCell().getPossibleAnchors().contains(bel.getId());
	}

	/**
	 * Places the cell at the specified BEL in this design.
	 * No cells should exist at the specified BEL in this design.
	 * CellPins are not mapped to BelPins in this function. 
	 * In general, best placement practices are to wait until the BEL placement
	 * is finalized, and then apply the pin mappings.
	 *
	 * @param cell the cell to place
	 * @param bel the BEL where the cell is to be placed
	 */
	public void placeCell(Cell cell, Bel bel) {
		Objects.requireNonNull(cell);
		Objects.requireNonNull(bel);
		if (cell.getDesign() != this)
			throw new Exceptions.DesignAssemblyException("Cannot place cell not in the design.");
		if (cell.isPlaced())
			throw new Exceptions.DesignAssemblyException("Cell is already placed. Cannot re-place cell: " + cell.getName());
		if (cell.isMacro())
			throw new Exceptions.DesignAssemblyException("Cannot place macro cell. Can only place internal cells to the macro.");
		if (isBelUsed(bel))
			throw new Exceptions.DesignAssemblyException("Cell already placed at location.");

		_placeCell(cell, bel);
	}

	/**
	 * Same as {@link #placeCell(Cell, Bel)} but also checks that the BEL is a valid type for
	 * the cell and that the type of the BEL does not clash with other BELs already used in
	 * the site.  The type of the site is then updated to match the site type.
	 * <p/>
	 * When comparing against existing BELs used in the site, the check only compares against
	 * one BEL in the site, chosen in a non-deterministic manner.
	 *
	 * @param cell the cell to place
	 * @param bel the BEL where the cell is to be placed
	 *
	 * @throws Exceptions.DesignAssemblyException if the BEL types are incompatible
	 */
	public void placeCellSafe(Cell cell, Bel bel) {
		Objects.requireNonNull(cell);
		Objects.requireNonNull(bel);
		if (cell.getDesign() != this)
			throw new Exceptions.DesignAssemblyException("Cannot place cell not in the design.");
		if (cell.isPlaced())
			throw new Exceptions.DesignAssemblyException("Cell is already placed. Cannot re-place cell: " + cell.getName());
		if (cell.isMacro())
			throw new Exceptions.DesignAssemblyException("Cannot place macro cell. Can only place internal cells to the macro.");
		if (!canPlaceCellAt(cell, bel))
			throw new Exceptions.DesignAssemblyException("Cell already placed at location.");

		_validateCellPlacement(bel);
		bel.getSite().setType(bel.getId().getSiteType());

		_placeCell(cell, bel);
	}

	// Checks that the BEL to be occupied is compatible with other used BELs in the site.
	private void _validateCellPlacement(Bel bel) {
		Map<Bel, Cell> existingBels = placementMap.getOrDefault(bel.getSite(), emptyMap());
		Optional<Bel> existingType = existingBels.keySet().stream().findAny();
		existingType.ifPresent(t -> {
			if (t.getId().getSiteType() != bel.getId().getSiteType())
				throw new Exceptions.DesignAssemblyException("Site types of BELs in site differ from existing");
		});
	}

	private void _placeCell(Cell cell, Bel bel) {
		// update the placement map
		Map<Bel, Cell> sitePlacementMap = placementMap.get(bel.getSite());
		if (sitePlacementMap == null) {
			sitePlacementMap = new HashMap<>();
			placementMap.put(bel.getSite(), sitePlacementMap);
		} else {
			assert sitePlacementMap.get(bel) == null;
		}
		sitePlacementMap.put(bel, cell);

		// set the location in the cell
		cell.place(bel);
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
			cell.getInternalCells().forEach(this::_unplaceCell);
		}
		else {
			_unplaceCell(cell);
		}
	}

	private void _unplaceCell(Cell cell) {
		assert(!cell.isMacro());

		_clearCellPlacement(cell);

		// undo all cell pin mappings (if they exists)
		cell.getPins().forEach(CellPin::clearPinMappings);
	}

	private void _clearCellPlacement(Cell cell) {
		// if the cell is not placed, return
		if (!cell.isPlaced())
			return;

		// remove the location from the placement map
		Site site = cell.getSite();
		Map<Bel, Cell> sitePlacementMap = placementMap.get(site);
		sitePlacementMap.remove(cell.getBel());
		if (sitePlacementMap.size() == 0)
			placementMap.remove(site);

		// clear the location from the cell
		cell.unplace();
	}

	/**
	 * Unroutes the INTERSITE portions of all nets currently in the design.
	 * This function is currently not recommended for use. Further testing is needed.
	 */
	public void unrouteDesign() {
		getNets().forEach(CellNet::unrouteIntersite);
	}

	/**
	 * Unroutes the INTERSITE and INTRASITE portions of all nets currently in the design.
	 * This function is currently not recommended for use. Further testing is needed.
	 */
	public void unrouteDesignFull() {
		getNets().forEach(CellNet::unrouteFull);
		getCells().forEach(Cell::clearPinMappings);
	}
	
	/**
	 * Unplaces the design. The design is first unrouted. All CellPin to BelPin 
	 * mappings are undone as well. This function is currently not recommended for use.
	 * Further testing is needed.   
	 */
	public void unplaceDesign() {
		unrouteDesign();

		for (Cell cell : getCells()) {
			if (cell.isMacro()) {
				cell.getInternalCells().forEach(this::_unplaceCell);
			}
			else {
				this._unplaceCell(cell);
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

	public boolean isSitePipAtSiteUsed(Site site, String sitePip) {
		Map<String, String> sitePips = pipInValues.get(site);

		if (sitePips == null)
			return false;

		return sitePips.containsKey(sitePip);
	}

	/**
	 * Add a mapping of used PIPs to their input route in a site. 
	 * @param ps {@link Site} to route
	 * @param pipInVals Map of used PIPs to its input wire
	 */
	public void addPIPInputValsAtSite(Site ps, Map<String, String> pipInVals){
		this.pipInValues.put(ps, pipInVals);
	}

	public void addPipInputValAtSite(Site site, String pip, String inputVal) {
		if (this.getPIPInputValsAtSite(site) == null) {
			Map<String, String> pipToInputVals = new HashMap<>();
			pipToInputVals.put(pip, inputVal);
			this.pipInValues.put(site, pipToInputVals);
		}
		else {
			this.getPIPInputValsAtSite(site).put(pip, inputVal);
		}
	}

	public void addPipInputValAtSite(Site site, Map<String, String> pipInputVals) {
		if (this.getPIPInputValsAtSite(site) == null) {
			Map<String, String> pipToInputVals = new HashMap<>(pipInputVals);
			this.pipInValues.put(site, pipToInputVals);
		}
		else {
			this.getPIPInputValsAtSite(site).putAll(pipInputVals);
		}
	}

	/**
	 * Returns a mapping of used PIPs to their input route
	 * @param ps {@link Site} object
	 */
	public Map<String, String> getPIPInputValsAtSite(Site ps){
		return this.pipInValues.getOrDefault(ps, null);
	}
	
	public void setPipInValues(Map<Site, Map<String, String>> newVals){
		this.pipInValues = newVals;
	}
	
	public Map<Site, Map<String, String>> getPipInValues(){
		return this.pipInValues;
	}

	/**
	 * Returns a list of XDC constraints on the design. If there are no constraints on the design,
	 * an empty list is returned.
	 */
	public List<XdcConstraint> getVivadoConstraints() {
		return vivadoConstraints == null ? Collections.emptyList() : this.vivadoConstraints;
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
	 * Creates a map from port cells to the sites the cells are constrained to be placed on according to the
	 * imported constraints.xdc.
	 * @return the map from port cells to their sites
	 */
	public Map<Cell, Site> getPortConstraintMap() {
		Map<Cell, Site> portConstraintMap = new HashMap<>();
		if (vivadoConstraints == null)
			return portConstraintMap;

		for (XdcConstraint constraint : vivadoConstraints) {
			if (constraint.getPackagePinConstraint() == null)
				continue;

			// Get the port cell
			Cell portCell = this.getCell(constraint.getPackagePinConstraint().getPortName());
			assert(portCell.isPort());

			// Get the package pin's site
			Site site = device.getSite(constraint.getPackagePinConstraint().getPinName());
			assert(site != null);

			// Add to the port map
			portConstraintMap.put(portCell, site);
		}

		return portConstraintMap;
	}

	/**
	 * @return the partPinMap
	 */
	public Map<String, String> getPartPinMap() {
		return partPinMap;
	}

	/**
	 * @param partPinMap the partPinMap to set
	 */
	public void setPartPinMap(Map<String, String> partPinMap) {
		this.partPinMap = partPinMap;
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

	/**
	 * @return the oocPortMap
	 */
	public Map<String, String> getOocPortMap() {
		return oocPortMap;
	}

	/**
	 * @param oocPortMap the oocPortMap to set
	 */
	public void setOocPortMap(Map<String, String> oocPortMap) {
		this.oocPortMap = oocPortMap;
	}

	public Map<String, RouteStringTree> getStaticRouteStringMap() {
		return staticRouteStringMap;
	}

	public Map<Wire, Set<CellNet>> getReservedWires() {
		return reservedWires;
	}

	// TODO: Add reservedNode: Reserve all wires in a node, given a wire
	// reserves an entire node - start wire, end wire,
	// and wires that branch off, etc. ex: INT_R_X39Y15/SS2END_N0_3

	// For when the net is in the static design
	public void addReservedWire(Wire reservedWire) {
		if (reservedWires.containsKey(reservedWire)) {
			reservedWires.get(reservedWire).add(null);
		}
		else {
			Set<CellNet> nets = new HashSet<>();
			reservedWires.put(reservedWire, nets);
		}
	}

	/**
	 * Adds a site to the list of reserved sites.
	 * @param site
	 */
	public void addReservedSite(Site site) {
		reservedSites.add(site);
	}

	public Set<Site> getReservedSites() {
		return reservedSites;
	}

	public void addReservedWire(Wire reservedWire, Set<CellNet> nets) {
		if (reservedWires.containsKey(reservedWire)) {
			reservedWires.get(reservedWire).addAll(nets);
		}
		else {
			reservedWires.put(reservedWire, nets);
		}

		for (CellNet net : nets) {
			net.addReservedWire(reservedWire);
		}
	}

	public void addReservedWire(Wire reservedWire, CellNet net) {
		if (reservedWires.containsKey(reservedWire)) {
			// TODO: Check that these nets are aliases of each other; i.e., they are allowed to share the same wire
			reservedWires.get(reservedWire).add(net);

			//if (reservedWires.get(reservedWire).size() > 1)
			//{
			//	System.out.println("Warning: The following nets all have reserved " + reservedWire.getFullName());
			//	for (CellNet cellNet : reservedWires.get(reservedWire)) {
			//		System.out.println("  " + cellNet.getName());
			//	}
			//}



		}
		else {
			Set<CellNet> nets = new HashSet<>();
			nets.add(net);
			reservedWires.put(reservedWire, nets);
		}

		//net.addReservedWire(reservedWire);
	}

	public boolean isWireReserved(Wire wire) {
		return reservedWires.containsKey(wire);
		//return reservedWires.contains(wire);
	}

	public boolean isWireReservedByNet(Wire wire, CellNet net) {
		if (reservedWires.containsKey(wire)) {
			return reservedWires.get(wire).contains(net);
		}
		return false;
	}

	public void setReservedWires(Map<Wire, Set<CellNet>> reservedWires) {
		this.reservedWires = reservedWires;
	}

	public Map<String, String> getReconfigStaticNetMap() {
		return reconfigStaticNetMap;
	}

	public void setReconfigStaticNetMap(Map<String, String> reconfigStaticNetMap) {
		this.reconfigStaticNetMap = reconfigStaticNetMap;
	}
}

