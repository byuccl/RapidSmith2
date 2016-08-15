package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.BondedType;
import edu.byu.ece.rapidSmith.device.Site;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 *  Cells represent a primitive logical element in a cell design which can map
 *  onto a single BEL on the device.  Once created, the name and type of the cell
 *  are immutable.  Pins on the cell are initialized upon cell creation based upon
 *  the cell's type.
 */
public class Cell extends PropertyObject {
	/** Unique name of this instance */
	private String name;
	/** The CellDesign this cell exists in */
	private CellDesign design;
	/** Type of the cell (LUT6, FF, DSP48, ...) */
	private LibraryCell libCell;
	/** IO Bondedness for this pad cells.  Use internal for non-IO pad cells. */
	private BondedType bonded;
	/** BEL in the device this site is placed on */
	private Bel anchor;
	/** Mapping of pin names to CellPin objects of this cell */
	private Map<String, CellPin> pinMap;

	/**
	 * Creates a new cell with specified name and type.
	 *
	 * @param name name of the new cell
	 * @param libCell the library cell to base this cell on
	 */
	public Cell(String name, LibraryCell libCell) {
		Objects.requireNonNull(name);
		Objects.requireNonNull(libCell);

		this.name = name;
		this.libCell = libCell;
		this.bonded = BondedType.INTERNAL;

		this.design = null;
		this.anchor = null;
		
		this.pinMap = new HashMap<>();
		for (LibraryPin pin : libCell.getLibraryPins()) {
			this.pinMap.put(pin.getName(), new CellPin(this, pin));
		}

		// TODO subcells for hierarchical macros
	}

	/**
	 * Returns the name of this cell.
	 */
	public final String getName() {
		return this.name;
	}

	void setName(String name) {
		assert name != null;

		this.name = name;
	}

	/**
	 * Returns true if this cell is part of a design.
	 */
	public final boolean isInDesign() {
		return design != null;
	}

	/**
	 * Returns the design this cell exists in.
	 */
	public final CellDesign getDesign() {
		return design;
	}

	void setDesign(CellDesign design) {
		assert design != null;

		this.design = design;
	}

	void clearDesign() {
		this.design = null;
	}

	/**
	 * Returns the library cell this cell is backed by.
	 */
	public final LibraryCell getLibCell() {
		return libCell;
	}

	/**
	 * Returns true if this cell acts as a VCC source.
	 */
	public boolean isVccSource() {
		return getLibCell().isVccSource();
	}

	/**
	 * Returns true if this cell acts as a ground source.
	 */
	public boolean isGndSource() {
		return getLibCell().isGndSource();
	}

	/**
	 * Returns true if this cell is a top-level port of the design
	 */
	public boolean isPort() {
		return getLibCell().isPort();
	}
	
	/**
	 * Returns the bondedness of this cell.  IO are either BONDED or UNBONDED,
	 * all others INTERNAL.
	 */
	public BondedType getBonded() {
		return bonded;
	}

	/**
	 * Sets the bonded parameter for this cell.
	 */
	public void setBonded(BondedType bonded) {
		Objects.requireNonNull(bonded);

		this.bonded = bonded;
	}

	/**
	 * Returns true if this cell is placed on a BEL.
	 */
	public final boolean isPlaced() {
		return anchor != null;
	}

	public final List<Cell> getSubcells() {
		// TODO get the subcells once we have support
		return Collections.emptyList();
	}

	/**
	 * Returns the BEL this cell is placed at in the design.
	 *
	 * @return the BEL this cell is placed at in the design
	 */
	public final Bel getAnchor() {
		return anchor;
	}

	public final List<BelId> getPossibleAnchors() {
		return getLibCell().getPossibleAnchors();
	}

	public final List<Bel> getRequiredBels(Bel anchor) {
		return getLibCell().getRequiredBels(anchor);
	}

	/**
	 * Returns the site this cell resides at.
	 *
	 * @return the site this cell resides
	 */
	public final Site getAnchorSite() {
		return anchor == null ? null : anchor.getSite();
	}

	void place(Bel anchor) {
		assert anchor != null;

		this.anchor = anchor;
	}

	void unplace() {
		this.anchor = null;
	}

	public Map<SiteProperty, Object> getSharedSiteProperties() {
		return getSharedSiteProperties(anchor.getId());
	}

	public Map<SiteProperty, Object> getSharedSiteProperties(BelId belId) {
		Map<SiteProperty, Object> returnMap = new HashMap<>();

		Map<String, SiteProperty> referenceMap =
				getLibCell().getSharedSiteProperties(belId);
		for (Map.Entry<String, SiteProperty> e : referenceMap.entrySet()) {
			if (hasProperty(e.getKey()) && getProperty(e.getKey()).getType() == PropertyType.DESIGN) {
				returnMap.put(e.getValue(), getPropertyValue(e.getKey()));
			}
		}
		return returnMap;
	}

	/**
	 * Returns the nets that connect to the pins of this cell.
	 */
	public final Collection<CellNet> getNetList() {
		return pinMap.values().stream()
				.filter(pin -> pin.getNet() != null)
				.map(CellPin::getNet)
				.collect(Collectors.toSet());
	}

	/**
	 * Returns the pin on this cell with the specified name.
	 */
	public final CellPin getPin(String pinName) {
		return pinMap.get(pinName);
	}

	/**
	 * Returns all of the pins on this net.  The returned collection should not
	 * be modified by the user.
	 *
	 * @return A collection of unique pins being used on this cell.
	 */
	public final Collection<CellPin> getPins() {
		return pinMap.values();
	}

	/**
	 * Returns all of the output pins on this net.
	 */
	public final Collection<CellPin> getOutputPins() {
		return pinMap.values().stream()
				.filter(CellPin::isOutpin)
				.collect(Collectors.toList());
	}

	/**
	 * Returns all of the input pins on this net.
	 */
	public final Collection<CellPin> getInputPins() {
		return pinMap.values().stream()
				.filter(CellPin::isInpin)
				.collect(Collectors.toList());
	}

	/**
	 * Returns a deep copy of this cell.  The deep copy does not have any design
	 * or cluster information.
	 * @return a deep copy of this cell
	 */
	public Cell deepCopy() {
		return deepCopy(Collections.emptyMap());
	}

	/**
	 * Returns a deep copy of this cell except with changes specified in the
	 * changes described in the <i>changes</i> argument map.  The valid changes
	 * described in the map that are accepted are <br>
	 * <ul>
	 *     <li>name -> String</li>
	 *     <li>type -> LibraryCell</li>
	 * </ul><br>
	 * Other values in the map are ignored.
	 *
	 * @param changes map containing the changes to be made to the cell copy
	 * @return a deep copy of this cell with specified changes
	 */
	public Cell deepCopy(Map<String, Object> changes) {
		return deepCopy(Cell::new, changes);
	}

	protected Cell deepCopy(BiFunction<String, LibraryCell, Cell> cellFactory) {
		return deepCopy(cellFactory, Collections.emptyMap());
	}

	protected Cell deepCopy(
			BiFunction<String, LibraryCell, Cell> cellFactory,
			Map<String, Object> changes
	) {
		String name;
		LibraryCell libCell;

		if (changes.containsKey("name"))
			name = (String) changes.get("name");
		else
			name = getName();

		if (changes.containsKey("type"))
			libCell = (LibraryCell) changes.get("type");
		else
			libCell = getLibCell();

		Cell cellCopy = cellFactory.apply(name, libCell);
		cellCopy.setBonded(getBonded());
		getProperties().forEach(p ->
				cellCopy.updateProperty(copyAttribute(getLibCell(), libCell, p))
		);
		return cellCopy;
	}

	private Property copyAttribute(LibraryCell oldType, LibraryCell newType, Property orig) {
		if (!oldType.equals(newType) && orig.getKey().equals(oldType.getName())) {
			return new Property(newType.getName(), orig.getType(), orig.getValue());
		} else {
			return orig.deepCopy();
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return "Cell{" + getName() + " " + (isPlaced() ? "@" + getAnchor().getFullName() : "") + "}";
	}
}
