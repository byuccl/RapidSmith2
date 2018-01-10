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
package edu.byu.ece.rapidSmith.device;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.util.HashPool;
import edu.byu.ece.rapidSmith.primitiveDefs.PrimitiveDefList;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * This is the main class that stores information about each Xilinx part.  It contains
 * a 2D grid of Tiles which contain all the routing and sites necessary for
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
	public static final String LATEST_DEVICE_FILE_VERSION = "1.1";
	/** The current release of the tools */
	public static final String rapidSmithVersion = "2.0.0";
	private static final long serialVersionUID = -5032202328911688776L;

	//========================================================================//
	// Class Members
	//========================================================================//
	/** The Xilinx part name of the device (ie. xc4vfx12ff668, omits the speed grade) */
	private String partName;
	/** The Xilinx family of this part */
	private FamilyType family;
	/** Number of rows of tiles in the device */
	private int rows;
	/** Number of columns of tiles in the device */
	private int columns;
	/** A 2D array of all the tiles in the device */
	private Tile[][] tiles;
	/** A Map between a tile name (string) and its actual reference */
	private HashMap<String, Tile> tileMap;
	/** Keeps track of all the sites on the device */
	private HashMap<String, Site> sites;
	/** Keeps track of which Wire objects have a corresponding PIPRouteThrough */
	private Map<Integer, Map<Integer, PIPRouteThrough>> routeThroughMap;
	/** Templates for each site type in the device */
	private Map<SiteType, SiteTemplate> siteTemplates;
	/** The wire enumerator for this device */
	private WireEnumerator we;
	/** Primitive defs in the device for reference */
	private PrimitiveDefList primitiveDefs;

	//========================================================================//
	// Objects that are Populated After Parsing
	//========================================================================//
	/** Created on demand when user calls getSitesOfTypeMap() */
	private Map<SiteType, ArrayList<Site>> sitesOfTypeMap;
	/** Maps the pad bel name to the corresponding package pin */
	private Map<String, PackagePin> packagePinMap;
	
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

	public static Device getInstance(String partName, boolean loadExtendedInfo) {
		Device device = getInstance(partName);
		if (loadExtendedInfo && !device.hasExtendedInfo()) {
			device.loadExtendedInfo();
		}
		return device;
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
	 * Returns the base family type for this device.
	 * This ensures compatibility with all RapidSmith files. For differentiating
	 * family types (qvirtex4 rather than virtex4) use getExactFamilyType().
	 *
	 * @return the base family type of the part for this device
	 */
	public FamilyType getFamily() {
		return family;
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
	 * Sets the family of this device.
	 *
	 * @param family the name of the part
	 */
	public void setFamily(FamilyType family) {
		this.family = family;
	}

	@Deprecated
	public boolean hasExtendedInfo() {
		return true;
	}

	@Deprecated
	public boolean loadExtendedInfo() {
		// all info has been moved into the device.  Does nothing now
		return true;
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
	 * Returns a collection of all tiles in this device.
	 *
	 * @return the tiles of this device.
	 */
	public Collection<Tile> getTiles() {
		return Collections.unmodifiableCollection(tileMap.values());
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
	 * Returns the map of site names to sites for this device.
	 *
	 * @return the map of site name to sites for this device
	 */
	public Map<String, Site> getSites() {
		return sites;
	}

	/**
	 * Returns the site in the device with the specified name.
	 *
	 * @param name name of the site to get.
	 * @return the site with the name, or null if no site with the name
	 *   exists in the device
	 */
	public Site getSite(String name) {
		return this.sites.get(name);
	}

	/**
	 * Checks if this PIP is RouteThrough.
	 *
	 * @param pip the pip to test
	 * @return true if the PIP is a routeThrough
	 */
	public boolean isRouteThrough(PIP pip) {
		return isRouteThrough(pip.getStartWire(), pip.getEndWire());
	}

	/**
	 * Checks if the two wires are connected through a RouteThrough.
	 *
	 * @param startWire the startWire to test
	 * @param endWire the endWire to test
	 * @return true if the PIP is a routeThrough
	 */
	public boolean isRouteThrough(Wire startWire, Wire endWire) {
		return getRouteThrough(startWire, endWire) != null;
	}

	boolean isRouteThrough(Integer startWire, Integer endWire) {
		return getRouteThrough(startWire, endWire) != null;
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
	public PIPRouteThrough getRouteThrough(Wire startWire, Wire endWire) {
		return getRouteThrough(startWire.getWireEnum(), endWire.getWireEnum());
	}

	/**
	 * Returns the PIPRouteThrough object for a specified WireConnection.
	 *
	 * @param startWire the source wire of the corresponding PIPRouteThrough
	 * @param endWire the sink wire of the corresponding PIPRouteThrough
	 * @return the PIPRouteThrough object or null if the pip is not a
	 *   route through
	 */
	PIPRouteThrough getRouteThrough(Integer startWire, Integer endWire) {
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
	 * Returns the site templates for this device.
	 * The site templates expose the BELs and routing structure of each
	 * site type.
	 *
	 * @return the site templates for this device
	 */
	public Map<SiteType, SiteTemplate> getSiteTemplates() {
		return siteTemplates;
	}

	/**
	 * Returns the site template for the specified site type.
	 * The site template exposes the BELs and routing structure of the site
	 * type.
	 * @param type the site type of the site to get
	 * @return the site template for the specified site type
	 */
	public SiteTemplate getSiteTemplate(SiteType type) {
		return siteTemplates.get(type);
	}

	public void setSiteTemplates(Map<SiteType, SiteTemplate> siteTemplates) {
		this.siteTemplates = siteTemplates;
	}

	public BelTemplate getBelTemplate(BelId id) {
		return getSiteTemplate(id.getSiteType()).getBelTemplates().get(id.getName());
	}

	/**
	 * A method to get the corresponding site for current in a different tile.
	 * For example in a Virtex 4, there are 4 slices in a CLB tile, when moving a hard macro
	 * the current slice must go in the same spot in a new CLB tile
	 *
	 * @param current     The current site of the instance.
	 * @param newSiteTile The tile of the new proposed site.
	 * @return The corresponding site in tile newSite, or null if no corresponding site exists.
	 */
	public static Site getCorrespondingSite(Site current, Tile newSiteTile) {
		if (newSiteTile == null) {
			//MessageGenerator.briefError("ERROR: Bad input to Device.getCorrespondingSite(), newSiteTile==null");
			return null;
		}
		if (newSiteTile.getSites() == null) {
			return null;
		}

		Site[] sites = newSiteTile.getSites();
		return sites[current.getIndex()];
	}

	/**
	 * This method will get (create if null) a data structure which stores all
	 * of the device's sites by type.  To get all of the
	 * sites of a particular type, use the SiteType.ordinal() method to
	 * get the representative integer and use that value to index into the
	 * ArrayList.  This will return an array of all sites of that
	 * same type.
	 *
	 * @return The data structure which stores all of the sites
	 * separated by type.
	 */
	private Map<SiteType, ArrayList<Site>> getSitesOfTypeMap() {
		if (sitesOfTypeMap == null) {
			createSitesOfTypeMap();
		}
		return sitesOfTypeMap;
	}

	/**
	 * This method will return all compatible sites for a particular site type in
	 * this device.  For example, a SLICEL can be placed at all SLICEL sites and
	 * all SLICEM sites.  If the type given were SLICEL, this method would return
	 * a list of all SLICEL and SLICEM sites.
	 *
	 * Note -- The list is rebuilt each method call.
	 *
	 * @param type the type for which to find compatible sites.
	 * @return a list of compatible sites suitable for placement of an
	 * instance of type type.
	 */
	public List<Site> getAllCompatibleSites(SiteType type) {
		// Check if there are sites of the given type
		List<Site> compatibleList = new ArrayList<>();
		List<Site> match = getAllSitesOfType(type);
		if (match != null) {
			compatibleList.addAll(match);
		}

		// Check for other compatible site types
		SiteType[] compatibleTypes = getSiteTemplate(type).getCompatibleTypes();
		if (compatibleTypes != null) {
			for (SiteType compatibleType : compatibleTypes) {
				match = getAllSitesOfType(compatibleType);
				if (match != null) {
					compatibleList.addAll(match);
				}
			}
		}

		// If there are no compatible sites, return empty list
		if (compatibleList.size() == 0) {
			return Collections.emptyList();
		}
		return compatibleList;
	}

	/**
	 * Gets and returns an array of all sites of the given site type.
	 *
	 * @param type The site type of the site to get.
	 * @return An array of all sites in the device with site type.
	 */
	public List<Site> getAllSitesOfType(SiteType type) {
		return getSitesOfTypeMap().get(type);
	}

	/**
	 * Adds a package pin to the device.
	 */
	public void addPackagePin(PackagePin packagePin) {
		if (this.packagePinMap == null) {
			this.packagePinMap = new HashMap<String, PackagePin>();
		}
		this.packagePinMap.put(packagePin.getSite() + "/" + packagePin.getBel(), packagePin);
	}
	
	/**
	 * Returns the package pin of the corresponding pad bel. If no package pin is mapped to the
	 * bel, then {@code NULL} is returned.
	 *  
	 * @param bel Bel object
	 */
	public PackagePin getPackagePin(Bel bel) {
		return this.packagePinMap == null ? null : this.packagePinMap.get(bel.getFullName());
	}
	
	/**
	 * Returns a collection of package pins for the device. Package pins represent
	 * valid placement locations for ports in Vivado.
	 */
	public Collection<PackagePin> getPackagePins() {
		return this.packagePinMap == null ? 
			Collections.emptyList() :
			Collections.unmodifiableCollection(this.packagePinMap.values());
	}
	
	/**
	 * Returns a stream of package pins that can access the dedicated clock
	 * routing network of the device.
	 */
	public Stream<PackagePin> getClockPads() {
		return this.packagePinMap == null ? Stream.empty() : this.packagePinMap.values().stream().filter(pp -> pp.isClockPad());
	}
	
	/**
	 * Returns the {@link Bel} object of the specified package pin. If the package
	 * pin is incorrectly formatted and does not map to a valid bel, {@code null}
	 * will be returned.
	 * 
	 * @param packagePin Package pin object
	 */
	public Bel getPackagePinBel(PackagePin packagePin) {
		Site site = this.getSite(packagePin.getSite());
		return site == null ? null : site.getBel(packagePin.getBel());
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
	 * This will create a data structure which organizes all sites by types.
	 * The outer ArrayList uses the SiteType.ordinal() value as the index for
	 * each type of site.
	 */
	private void createSitesOfTypeMap() {
		Map<SiteType, ArrayList<Site>> tmp = new HashMap<>();

		for (int i = 0; i < this.rows; i++) {
			for (int j = 0; j < this.columns; j++) {
				Site[] sites = tiles[i][j].getSites();
				if (sites == null) continue;
				for (Site site : sites) {
					tmp.computeIfAbsent(site.getDefaultType(), k -> new ArrayList<>()).add(site);
				}
			}
		}

		tmp.values().forEach(ArrayList::trimToSize);
		this.sitesOfTypeMap = tmp;
	}


	/*
	 *  Device construction methods
	 *
	 *  Some of the information is available in other structures, but we need to
	 *  reorganize it for fast lookup.  This method builds these structures after
	 *  the device representation has been built or read in.
	 */
	/**
	 * Construct tileMap and sites data structures from the tile array.
	 * Used only in creating and loading devices.
	 */
	public void constructTileMap() {
		tileMap = new HashMap<>(getRows() * getColumns());
		sites = new HashMap<>();
		for (Tile[] tileArray : tiles) {
			for (Tile tile : tileArray) {
				tileMap.put(tile.getName(), tile);
				if (tile.getSites() == null)
					continue;
				for (Site ps : tile.getSites())
					sites.put(ps.getName(), ps);
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
		setSiteTypes();
		for (SiteTemplate siteTemplate : siteTemplates.values())
			siteTemplate.constructDependentResources();
		constructSiteExternalConnections();
	}

	/**
	 * Sets the type of each site to the first type in the
	 * alternatives list.
	 */
	private void setSiteTypes() {
		for (Site site : sites.values()) {
			site.setTypeUnchecked(site.getPossibleTypes()[0]);
		}
	}

	/*
	   Builds the wireSites structure for the tiles and the external wire to
	   pin name map for the sites.  These are built here because they
	   require the pools to reduce the memory footprint.
	 */
	private void constructSiteExternalConnections() {
		// These pools help to reuse instances to reduce memory
		HashPool<Map<Integer, Integer>> wireSitesPool = new HashPool<>();
		HashPool<Map<Integer, SitePinTemplate>> sitePinMapPool = new HashPool<>();
		HashPool<Map<SiteType, Map<Integer, SitePinTemplate>>> extConnPool = new HashPool<>();
		for (Tile tile : tileMap.values()) {
			Map<Integer, Integer> wireSites = new HashMap<>();
			if (tile.getSites() == null)
				continue;

			for (Site site : tile.getSites()) {
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
				site.setExternalWireToPinMap(extConnPool.add(extConns));
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
		private static final long serialVersionUID = -3274492099960921694L;
		private String version;
		private String partName;
		private FamilyType family;
		private Tile[][] tiles;
		private Map<Integer, Map<Integer, PIPRouteThrough>> routeThroughMap;
		private Collection<SiteTemplate> siteTemplates;
		private WireEnumerator we;
		private PrimitiveDefList primitiveDefs;
		private Map<String, PackagePin> packagePinMap;

		public void readResolve(Device device) {
			device.partName = partName;
			device.family = family;
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
			device.we = we;
			device.primitiveDefs = primitiveDefs;

			device.constructTileMap();
			device.constructDependentResources();
			device.packagePinMap = packagePinMap;
		}

		@SuppressWarnings("unused")
		private Device readResolve() {
			if (!version.equals(LATEST_DEVICE_FILE_VERSION))
				return null;
			Device device = new Device();
			readResolve(device);
			return device;
		}
	}

	@SuppressWarnings("unused")
	private DeviceReplace writeReplace() {
		DeviceReplace repl = new DeviceReplace();
		writeReplace(repl);
		return repl;
	}

	public void writeReplace(DeviceReplace repl) {
		repl.version = LATEST_DEVICE_FILE_VERSION;
		repl.partName = partName;
		repl.family = family;
		repl.tiles = tiles;
		repl.routeThroughMap = routeThroughMap;
		repl.siteTemplates = siteTemplates.values();
		repl.we = we;
		repl.primitiveDefs = primitiveDefs;
		repl.packagePinMap = packagePinMap;
	}
}
