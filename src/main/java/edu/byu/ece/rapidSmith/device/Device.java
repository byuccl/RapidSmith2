/*
 * Copyright (c) 2010-2011 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.device;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.design.xdl.XdlPin;
import edu.byu.ece.rapidSmith.device.helper.HashPool;
import edu.byu.ece.rapidSmith.primitiveDefs.PrimitiveDefList;
import edu.byu.ece.rapidSmith.util.FamilyType;
import edu.byu.ece.rapidSmith.util.PartNameTools;

import java.io.Serializable;
import java.util.*;

/**
 * This is the main class that stores information about each Xilinx part.  It contains
 * a 2D grid of Tiles which contain all the routing and primitive sites necessary for
 * a placer and router.
 *
 * @author Chris Lavin
 *         Created on: Apr 22, 2010
 */
public class Device implements Serializable {
	//========================================================================//
	// Versions
	//========================================================================//
	/** This is the current device file version (saved in file to ensure proper compatibility) */
	public static final String LATEST_DEVICE_FILE_VERSION = "1.0";
	/** The current release of the tools */
	public static final String rapidSmithVersion = "2.0.0";

	//========================================================================//
	// Class Members
	//========================================================================//
	/** The Xilinx part name of the device (ie. xc4vfx12ff668, omits the speed grade) */
	private String partName;
	/** Number of rows of tiles in the device */
	private int rows;
	/** Number of columns of tiles in the device */
	private int columns;
	/** A 2D array of all the tiles in the device */
	private Tile[][] tiles;
	/** A Map between a tile name (string) and its actual reference */
	private HashMap<String, Tile> tileMap;
	/** Keeps track of all the primitive instances on the device */
	private HashMap<String, Site> primitiveSites;
	/** Keeps track of which Wire objects have a corresponding PIPRouteThrough */
	private Map<Integer, Map<Integer, PIPRouteThrough>> routeThroughMap;
	/** Templates for each primitive type in the device */
	private Map<SiteType, SiteTemplate> siteTemplates;
	/** The wire enumerator for this device */
	private WireEnumerator we;
	/** Primitive defs in the device for reference */
	private PrimitiveDefList primitiveDefs;

	//========================================================================//
	// Objects that are Populated After Parsing
	//========================================================================//
	/** Created on demand when user calls getPrimitiveSiteIndex(), where the ArrayList index is the ordinal of the PrimitiveType */
	private List<Site[]> primitiveSiteIndex;
	/** A set of all TileTypes that have switch matrices in them */
	private HashSet<TileType> switchMatrixTypes;

	private Set<SiteType> sliceTypes;
	private Set<SiteType> bramTypes;
	private Set<SiteType> fifoTypes;
	private Set<SiteType> dspTypes;
	private Set<SiteType> iobTypes;

	/**
	 * Constructor, initializes all objects to null
	 */
	public Device() { }

	/**
	 * Shortcut for {@code RSEnvironment.defaultEnv().getDevice(partName)};
	 *
	 * @param partName the part name of the device to get.
	 * @return the previously loaded device if the part has already been loaded in
	 *   the default environment, else the newly loaded device.
	 */
	public static Device getInstance(String partName) {
		return RSEnvironment.defaultEnv().getDevice(partName);
	}

	//========================================================================//
	// Getter and Setter Methods
	//========================================================================//
	/**
	 * Returns the partName (includes package but not speed grade)
	 * of this device (ex: xc4vfx12ff668).
	 *
	 * @return the part name of this device
	 */
	public String getPartName() {
		return partName;
	}

	/**
	 * Returns the all lower case exact Xilinx family type for this
	 * device (ex: qvirtex4 instead of virtex4).
	 *
	 * @return the exact Xilinx family type for this device
	 */
	public FamilyType getExactFamilyType() {
		return PartNameTools.getExactFamilyTypeFromPart(partName);
	}

	/**
	 * Returns the base family type for this device.
	 * This ensures compatibility with all RapidSmith files. For differentiating
	 * family types (qvirtex4 rather than virtex4) use getExactFamilyType().
	 *
	 * @return the base family type of the part for this device
	 */
	public FamilyType getFamilyType() {
		return PartNameTools.getFamilyTypeFromPart(partName);
	}

	/**
	 * Sets the part name of this device.
	 * This part name should only have the package information but not speed grade
	 * (ex: xc4vfx12ff668).
	 *
	 * @param partName the name of the part
	 */
	public void setPartName(String partName) {
		this.partName = partName;
	}

	/**
	 * Returns the number of rows of tiles in this device.
	 *
	 * @return the number of rows of tiles in the device.
	 */
	public int getRows() {
		return rows;
	}

	/**
	 * Returns the number of columns of tiles in this device.
	 *
	 * @return the number of columns of tiles in the device.
	 */
	public int getColumns() {
		return columns;
	}

	/**
	 * Returns the 2D array of tiles that define the layout of this device.
	 *
	 * @return the tiles of this device.
	 */
	public Tile[][] getTiles() {
		return tiles;
	}

	/**
	 * Returns the map of tile names to tiles for this device.
	 *
	 * @return the map of tile names to tiles for this device
	 */
	public Map<String, Tile> getTileMap() {
		return tileMap;
	}

	/**
	 * Returns the current tile in the device based on absolute row and column indices
	 *
	 * @param row    the absolute row index (0 starting at top)
	 * @param column the absolute column index (0 starting at the left)
	 * @return the tile specified by row and column or null if the indices are out of bounds
	 */
	public Tile getTile(int row, int column) {
		if (row < 0 || column < 0 || row > this.rows - 1 || column > this.columns - 1) {
			return null;
		}
		return tiles[row][column];
	}

	/**
	 * Returns the tile in the device with the specified name.
	 *
	 * @param tile the name of the tile to get.
	 * @return the tile with the name tile, or null if it does not exist.
	 */
	public Tile getTile(String tile) {
		return tileMap.get(tile);
	}

	/**
	 * Returns the tile associated with the specified unique tile number.
	 * <p>
	 * Each tile in a device can be referenced by a unique integer which is a combination
	 * of its row and column index.  This will return the tile with the unique index
	 * provided.
	 *
	 * @param uniqueTileNumber the unique tile number of the tile to get
	 * @return the tile with the uniqueTileNumber, or null if none exists.
	 */
	public Tile getTile(int uniqueTileNumber) {
		int row = uniqueTileNumber / columns;
		int col = uniqueTileNumber % columns;
		return getTile(row, col);
	}

	/**
	 * Returns the map of site names to primitive sites for this device.
	 *
	 * @return the map of site name to primitive sites for this device
	 */
	public Map<String, Site> getPrimitiveSites() {
		return primitiveSites;
	}

	/**
	 * Returns the primitive site in the device with the specified name.
	 *
	 * @param name name of the primitive site to get.
	 * @return the primitive site with the name, or null if no site with the name
	 *   exists in the device
	 */
	public Site getPrimitiveSite(String name) {
		return this.primitiveSites.get(name);
	}

	/**
	 * Checks if this wire is RouteThrough.
	 *
	 * @param w the wire connection to test
	 * @return true if the wire is a routeThrough
	 */
	public boolean isRouteThrough(WireConnection w) {
		return routeThroughMap.containsKey(w.getWire());
	}

	public boolean isRouteThrough(PIP pip) {
		return isRouteThrough(pip.getStartWire(), pip.getEndWire());
	}

	public boolean isRouteThrough(Integer startWire, Integer endWire) {
		if (!routeThroughMap.containsKey(endWire))
			return false;
		return routeThroughMap.get(endWire).containsKey(startWire);
	}

	/**
	 * Returns the PIPRouteThrough object for a specified WireConnection.
	 *
	 * @param pip the pip which has a corresponding PIPRouteThrough
	 * @return the PIPRouteThrough object or null if the pip is not a
	 *   route through
	 */
	public PIPRouteThrough getRouteThrough(PIP pip) {
		return getRouteThrough(pip.getStartWire(), pip.getEndWire());
	}

	/**
	 * Returns the PIPRouteThrough object for a specified WireConnection.
	 *
	 * @param startWire the source wire of the corresponding PIPRouteThrough
	 * @param endWire the sink wire of the corresponding PIPRouteThrough
	 * @return the PIPRouteThrough object or null if the pip is not a
	 *   route through
	 */
	public PIPRouteThrough getRouteThrough(Integer startWire, Integer endWire) {
		Map<Integer, PIPRouteThrough> sourceMap = routeThroughMap.get(endWire);
		if (sourceMap == null)
			return null;
		return sourceMap.get(startWire);
	}


	/**
	 * Sets the WireConnection to PIPRouteThrough object map.
	 *
	 * @param startWire the source wire of the route through
	 * @param endWire the sink wire of the route through
	 * @param rt the route through object
	 */
	public void addRouteThrough(Integer startWire, Integer endWire, PIPRouteThrough rt) {
		if (routeThroughMap == null)
			routeThroughMap = new HashMap<>();
		if (!routeThroughMap.containsKey(endWire)) {
			routeThroughMap.put(endWire, new HashMap<>(4));
		}
		Map<Integer, PIPRouteThrough> sourceMap = routeThroughMap.get(endWire);

		// TODO remove if clean
		if (sourceMap.containsKey(startWire) && !Objects.equals(sourceMap.get(startWire), rt))
			System.out.println("Warning: overriding routethrough is used" + rt);

		sourceMap.put(startWire, rt);
	}

	/**
	 * Returns the wire enumerator for this device.
	 * @return the wire enumerator for this device
	 */
	public WireEnumerator getWireEnumerator() {
		return we;
	}

	/**
	 * Sets the wire enumerator for this device.
	 * @param we the wire enumerator
	 */
	public void setWireEnumerator(WireEnumerator we) {
		this.we = we;
	}

	/**
	 * Returns the primitive defs for this device.
	 *
	 * @return the primitive defs for this device
	 */
	public PrimitiveDefList getPrimitiveDefs() {
		return primitiveDefs;
	}

	/**
	 * Sets the primitive defs for this device.
	 *
	 * @param primitiveDefs the primitive defs
	 */
	public void setPrimitiveDefs(PrimitiveDefList primitiveDefs) {
		this.primitiveDefs = primitiveDefs;
	}

	/**
	 * Returns the external wire on the instance pin.
	 *
	 * @param pin the pin to get the external name from.
	 * @return the wire enumeration of the internal pin on the instance primitive of pin.
	 */
	public TileWire getPrimitiveExternalPin(XdlPin pin) {
		return pin.getInstance().getPrimitiveSite().getSitePin(pin.getName()).getExternalWire();
	}

	public void setSwitchMatrixTypes(HashSet<TileType> types) {
		this.switchMatrixTypes = types;
	}

	/**
	 * This will return a set of all unique TileTypes which are considered
	 * to have a switch matrix or routing switch box in them.
	 *
	 * @return A set of all TileTypes which have a switch matrix in them.
	 */
	public HashSet<TileType> getSwitchMatrixTypes() {
		return switchMatrixTypes;
	}

	public Set<SiteType> getSliceTypes() {
		return sliceTypes;
	}

	public void setSliceTypes(Set<SiteType> sliceTypes) {
		this.sliceTypes = sliceTypes;
	}

	public Set<SiteType> getBramTypes() {
		return bramTypes;
	}

	public void setBramTypes(Set<SiteType> bramTypes) {
		this.bramTypes = bramTypes;
	}

	public Set<SiteType> getFifoTypes() {
		return fifoTypes;
	}

	public void setFifoTypes(Set<SiteType> fifoTypes) {
		this.fifoTypes = fifoTypes;
	}

	public Set<SiteType> getDspTypes() {
		return dspTypes;
	}

	public void setDspTypes(Set<SiteType> dspTypes) {
		this.dspTypes = dspTypes;
	}

	public Set<SiteType> getIOBTypes() {
		return iobTypes;
	}

	public void setIOBTypes(Set<SiteType> iobTypes) {
		this.iobTypes = iobTypes;
	}

	/**
	 * Returns the site templates for this device.
	 * The site templates expose the BELs and routing structure of each
	 * primitive type.
	 *
	 * @return the site templates for this device
	 */
	public Map<SiteType, SiteTemplate> getSiteTemplates() {
		return siteTemplates;
	}

	/**
	 * Returns the site template for the specified primitive type.
	 * The site template exposes the BELs and routing structure of the primitive
	 * type.
	 * @param type the primitive type of the site to get
	 * @return the site template for the specified primitive type
	 */
	public SiteTemplate getSiteTemplate(SiteType type) {
		return siteTemplates.get(type);
	}

	public void setSiteTemplates(Map<SiteType, SiteTemplate> primitiveTemplates) {
		this.siteTemplates = primitiveTemplates;
	}

	public BelTemplate getBelTemplate(BelId id) {
		return getSiteTemplate(id.getPrimitiveType()).getBelTemplates().get(id.getName());
	}

//	/**
//	 * This will creating a routing node from the pin given.
//	 *
//	 * @param pin The pin to create the routing node from.
//	 * @return A new node populated with the pin's tile and wire. The parent
//	 * field is null and level is zero.
//	 */
//	public Node getNodeFromPin(Pin pin) {
//		Integer wire = getPrimitiveExternalPin(pin);
//		if (wire == null) return null;
//		return new Node(pin.getTile(), wire, null, 0);
//	}

//	/**
//	 * This will creating a routing node from the pin given.
//	 *
//	 * @param pin The pin to create the routing node from.
//	 * @return A new node populated with the pin's tile and wire. The parent
//	 * field is null and level is zero.
//	 */
//	public Node getNodeFromPin(Pin pin, Node parent, int level) {
//		Integer wire = getPrimitiveExternalPin(pin);
//		if (wire == null) return null;
//		return new Node(pin.getTile(), wire, parent, level);
//	}


	/**
	 * A method to get the corresponding primitive site for current in a different tile.
	 * For example in a Virtex 4, there are 4 slices in a CLB tile, when moving a hard macro
	 * the current slice must go in the same spot in a new CLB tile
	 *
	 * @param current     The current primitive site of the instance.
	 * @param newSiteTile The tile of the new proposed site.
	 * @return The corresponding site in tile newSite, or null if no corresponding site exists.
	 */
	public static Site getCorrespondingPrimitiveSite(Site current, Tile newSiteTile) {
		if (newSiteTile == null) {
			//MessageGenerator.briefError("ERROR: Bad input to Device.getCorrespondingPrimitiveSite(), newSiteTile==null");
			return null;
		}
		if (newSiteTile.getPrimitiveSites() == null) {
			return null;
		}

		Site[] sites = newSiteTile.getPrimitiveSites();
		return sites[current.getIndex()];
	}

	/**
	 * This method will get (create if null) a data structure which stores all
	 * of the device's primitive sites by type.  To get all of the primitive
	 * sites of a particular type, use the PrimitiveType.ordinal() method to
	 * get the representative integer and use that value to index into the
	 * ArrayList.  This will return an array of all primitive sites of that
	 * same type.
	 *
	 * @return The data structure which stores all of the primitive sites
	 * separated by type.
	 */
	private List<Site[]> getPrimitiveSiteIndex() {
		if (primitiveSiteIndex == null) {
			createPrimitiveSiteIndex();
		}
		return primitiveSiteIndex;
	}

	/**
	 * This method will get all compatible primitive sites for a particular
	 * primitive type in this device.  For example, a SLICEL can be placed at
	 * all SLICEL sites AND all SLICEM sites.  If the type given were SLICEL,
	 * this method would return an array of all SLICEL and SLICEM sites.
	 *
	 * @param type The type for which to find compatible primitive sites.
	 * @return An array of compatible sites suitable for placement of a
	 * primitive of type type.
	 */
	public Site[] getAllCompatibleSites(SiteType type) {
		// Check if there are sites of the given type
		List<Site> compatibleList = new ArrayList<>();
		Site[] match = getAllSitesOfType(type);
		if (match != null) {
			compatibleList.addAll(Arrays.asList(match));
		}

		// Check for other compatible site types
		SiteType[] compatibleTypes = getSiteTemplate(type).getCompatibleTypes();
		if (compatibleTypes != null) {
			for (SiteType compatibleType : compatibleTypes) {
				match = getAllSitesOfType(compatibleType);
				if (match != null) {
					compatibleList.addAll(Arrays.asList(match));
				}
			}
		}

		// If there are no compatible sites, return null
		if (compatibleList.size() == 0) {
			return null;
		}
		return compatibleList.toArray(new Site[compatibleList.size()]);
	}

	/**
	 * Gets and returns an array of all primitive sites of the given primitive type.
	 *
	 * @param type The primitive type of the site to get.
	 * @return An array of all primitive sites in the device with primitive type type.
	 */
	public Site[] getAllSitesOfType(SiteType type) {
		return getPrimitiveSiteIndex().get(type.ordinal());
	}

	//========================================================================//
	// Object Population Methods
	//========================================================================//
	/**
	 * Initializes the tile array and wire pool.  This is done after the tile dimensions have
	 * been parsed from the .xdlrc file.  Also sets the number of rows and columns
	 * of the device.
	 *
	 * @param rows number of rows in the device
	 * @param columns number of columns in the device
	 */
	public void createTileArray(int rows, int columns) {
		this.rows = rows;
		this.columns = columns;
		tiles = new Tile[rows][columns];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				tiles[i][j] = new Tile();
				tiles[i][j].setColumn(j);
				tiles[i][j].setRow(i);
				tiles[i][j].setDevice(this);
			}
		}
	}

	public void setTileArray(Tile[][] tiles) {
		this.tiles = tiles;
		this.rows = tiles.length;
		this.columns = tiles[0].length;
	}

	/**
	 * This will create a data structure which organizes all primitive sites by types.
	 * The outer ArrayList uses the PrimitiveType.ordinal() value as the index for
	 * each type of primitive site.
	 */
	private void createPrimitiveSiteIndex() {
		List<List<Site>> tmp = new ArrayList<>(SiteType.values().length);
		for (int i = 0; i < SiteType.values().length; i++) {
			tmp.add(new ArrayList<>());
		}

		for (int i = 0; i < this.rows; i++) {
			for (int j = 0; j < this.columns; j++) {
				Site[] sites = tiles[i][j].getPrimitiveSites();
				if (sites == null) continue;
				for (Site site : sites) {
					tmp.get(site.getDefaultType().ordinal()).add(site);
				}
			}
		}

		List<Site[]> index = new ArrayList<>();
		for (List<Site> list : tmp) {
			if (list.size() == 0) {
				index.add(null);
			} else {
				Site[] tmpArray = new Site[list.size()];
				index.add(list.toArray(tmpArray));
			}
		}
		this.primitiveSiteIndex = index;
	}


	/*
	 *  Device construction methods
	 *
	 *  Some of the information is available in other structures, but we need to
	 *  reorganize it for fast lookup.  This method builds these structures after
	 *  the device representation has been built or read in.
	 */
	/**
	 * Construct tileMap and primitiveSites data structures from the tile array.
	 * Used only in creating and loading devices.
	 */
	public void constructTileMap() {
		tileMap = new HashMap<>(getRows() * getColumns());
		primitiveSites = new HashMap<>();
		for (Tile[] tileArray : tiles) {
			for (Tile tile : tileArray) {
				tileMap.put(tile.getName(), tile);
				if (tile.getPrimitiveSites() == null)
					continue;
				for (Site ps : tile.getPrimitiveSites())
					primitiveSites.put(ps.getName(), ps);
			}
		}
	}

	/**
	 * Builds resources that are fully dependent upon other resources
	 * provided during the loading and creation process.
	 * <p>
	 * Specifically, this method does three things:
	 *   initializes the site types to their default types
	 *
	 */
	public void constructDependentResources() {
		setPrimitiveSiteTypes();
		for (SiteTemplate siteTemplate : siteTemplates.values())
			siteTemplate.constructDependentResources();
		constructSiteExternalConnections();
	}

	/**
	 * Sets the type of each primitive site to the first type in the
	 * alternatives list.
	 */
	private void setPrimitiveSiteTypes() {
		for (Site site : primitiveSites.values()) {
			site.setType(site.getPossibleTypes()[0]);
		}
	}

	/*
	   Builds the wireSites structure for the tiles and the external wire to
	   pin name map for the primitive sites.  These are built here because they
	   require the pools to reduce the memory footprint.
	 */
	private void constructSiteExternalConnections() {
		// These pools help to reuse instances to reduce memory
		HashPool<Map<Integer, Integer>> wireSitesPool = new HashPool<>();
		HashPool<Map<Integer, SitePinTemplate>> sitePinMapPool = new HashPool<>();
		HashPool<Map<SiteType, Map<Integer, SitePinTemplate>>> extConnPool = new HashPool<>();
		for (Tile tile : tileMap.values()) {
			Map<Integer, Integer> wireSites = new HashMap<>();
			if (tile.getPrimitiveSites() == null)
				continue;

			for (Site site : tile.getPrimitiveSites()) {
				Map<SiteType, Map<String, Integer>> externalWiresMap = site.getExternalWires();
				Map<SiteType, Map<Integer, SitePinTemplate>> extConns = new HashMap<>();

				for (SiteType siteType : site.getPossibleTypes()) {
					SiteTemplate siteTemplate = getSiteTemplate(siteType);
					Map<String, Integer> externalWires = externalWiresMap.get(siteType);

					Map<Integer, SitePinTemplate> typeExternalConnections = new HashMap<>();
					for (SitePinTemplate tmplate : siteTemplate.getSinks().values()) {
						Integer externalWire = externalWires.get(tmplate.getName());
						// Since sitePins are created on request based upon the siteTemplate,
						// the tile needs to know which site in the tile the wire connects to.
						// Wiresites contains that information stored as the index of the
						// site of interest.  Using the index means the wireSites can be used
						// across similar tiles.
						wireSites.put(externalWire, site.getIndex());
						// for the SiteTemplate, contains the mapping of wire to the specific pin
						typeExternalConnections.put(externalWire, tmplate);
					}
					for (SitePinTemplate tmplate : siteTemplate.getSources().values()) {
						Integer externalWire = externalWires.get(tmplate.getName());
						wireSites.put(externalWire, site.getIndex());
						typeExternalConnections.put(externalWire, tmplate);
					}
					extConns.put(siteType, sitePinMapPool.add(typeExternalConnections));
				}
				site.setExternalWireToPinNameMap(extConnPool.add(extConns));
			}

			tile.setWireSites(wireSitesPool.add(wireSites));
		}
	}

	public Map<Integer, Map<Integer, PIPRouteThrough>> getRouteThroughMap() {
		return routeThroughMap;
	}

	public void setRouteThroughMap(Map<Integer, Map<Integer, PIPRouteThrough>> routeThroughMap) {
		this.routeThroughMap = routeThroughMap;
	}

	/*
	   For Hessian compression.  Avoids writing duplicate data.
	 */
	protected static class DeviceReplace implements Serializable {
		private String version;
		private String partName;
		private Tile[][] tiles;
		private Map<Integer, Map<Integer, PIPRouteThrough>> routeThroughMap;
		private Collection<SiteTemplate> siteTemplates;
		private WireEnumerator we;
		private PrimitiveDefList primitiveDefs;
		private HashSet<TileType> switchMatrixTypes;
		private Set<SiteType> sliceTypes;
		private Set<SiteType> bramTypes;
		private Set<SiteType> fifoTypes;
		private Set<SiteType> dspTypes;
		private Set<SiteType> iobTypes;

		public void readResolve(Device device) {
			device.partName = partName;
			device.tiles = tiles;
			device.rows = tiles.length;
			device.columns = tiles[0].length;
			for (int row = 0; row < device.rows; row++) {
				for (int col = 0; col < device.columns; col++) {
					tiles[row][col].setDevice(device);
					tiles[row][col].setRow(row);
					tiles[row][col].setColumn(col);
				}
			}
			device.routeThroughMap = routeThroughMap;
			device.siteTemplates = new HashMap<>();
			for (SiteTemplate template : siteTemplates) {
				device.siteTemplates.put(template.getType(), template);
			}
			device.switchMatrixTypes = switchMatrixTypes;
			device.sliceTypes = sliceTypes;
			device.bramTypes = bramTypes;
			device.fifoTypes = fifoTypes;
			device.dspTypes = dspTypes;
			device.iobTypes = iobTypes;
			device.we = we;
			device.primitiveDefs = primitiveDefs;

			device.constructTileMap();
			device.constructDependentResources();
		}

		@SuppressWarnings("UnusedDeclaration")
		private Device readResolve() {
			if (!version.equals(LATEST_DEVICE_FILE_VERSION))
				return null;
			Device device = new Device();
			readResolve(device);
			return device;
		}
	}

	private DeviceReplace writeReplace() {
		DeviceReplace repl = new DeviceReplace();
		writeReplace(repl);
		return repl;
	}

	public void writeReplace(DeviceReplace repl) {
		repl.version = LATEST_DEVICE_FILE_VERSION;
		repl.partName = partName;
		repl.tiles = tiles;
		repl.routeThroughMap = routeThroughMap;
		repl.siteTemplates = siteTemplates.values();
		repl.we = we;
		repl.primitiveDefs = primitiveDefs;
		repl.switchMatrixTypes = switchMatrixTypes;
		repl.sliceTypes = sliceTypes;
		repl.bramTypes = bramTypes;
		repl.fifoTypes = fifoTypes;
		repl.dspTypes = dspTypes;
		repl.iobTypes = iobTypes;
	}
}
