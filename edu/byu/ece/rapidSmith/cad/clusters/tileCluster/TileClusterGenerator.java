package edu.byu.ece.rapidSmith.cad.clusters.tileCluster;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import edu.byu.ece.rapidSmith.RapidSmithEnv;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterRoutingBuilder;
import edu.byu.ece.rapidSmith.cad.clusters.PinGroup;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.device.creation.ExtendedDeviceInfo;
import edu.byu.ece.rapidSmith.util.FamilyType;
import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.util.PartNameTools;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 *
 */
public class TileClusterGenerator {
	private TileClusterDevice tcd;
	private List<List<Tile>> tileMatrix;
	private int numBuiltTiles = 0;
	private Map<TileClusterType, Map<List<Tile>, Map<Tile, Tile>>> tileMapsMap = new HashMap<>();

	public TileClusterDevice buildFromDevice(Device device, CellLibrary cellLibrary) {
		tcd = new TileClusterDevice();
		tcd.setPartName(device.getPartName());
		tcd.setSiteTemplates(device.getSiteTemplates());
		tcd.setWireEnumerator(device.getWireEnumerator());
		tcd.setRouteThroughMap(device.getRouteThroughMap());
		tileMatrix = new ArrayList<>();

		Map<TileClusterType, TileClusterTemplate> templates = new HashMap<>();

		Map<List<TileType>, List<List<Tile>>> clusterInstancesMap = new HashMap<>();
		Map<List<TileType>, Collection<TileClusterType>> modesMap = new HashMap<>();

		// for packable tile sets, find all instances of them on the device
		for (List<TileType> clusterTileTypes : packableTileTypes) {
			List<List<Tile>> clusterInstances = findClusterInstances(clusterTileTypes, device);
			if (clusterInstances.size() == 0) {
				System.out.println("No instances of type " + clusterTileTypes);
				continue;
			}
			clusterInstancesMap.put(clusterTileTypes, clusterInstances);
			Collection<List<List<PrimitiveType>>> modes = getModes(clusterInstances.get(0));
			Collection<TileClusterType> types = modes.stream()
					.map(m -> new TileClusterType(clusterTileTypes, m))
					.collect(Collectors.toList());
			modesMap.put(clusterTileTypes, types);
		}

		// Build the new tiles in the cluster
		for (Map.Entry<List<TileType>, List<List<Tile>>> e : clusterInstancesMap.entrySet()) {
			List<List<Tile>> clusterInstances = e.getValue();

			Collection<TileClusterType> types = modesMap.get(e.getKey());
			for (TileClusterType type : types) {
				TileClusterTemplate template = new TileClusterTemplate();
				template.setType(type);
				templates.put(type, template);
				buildClusterTiles(template, clusterInstances);
			}
		}

		// Add the cluster template tiles to the device, finding an absolute location for them.
		tcd.setTileClusters(templates);
		for (TileClusterTemplate template : templates.values()) {
			template.setDevice(tcd);
		}
		buildTileArray(tcd);

		// build the routing for the cluster template
		for (Map.Entry<List<TileType>, List<List<Tile>>> e : clusterInstancesMap.entrySet()) {
			List<List<Tile>> clusterInstances = e.getValue();

			Collection<TileClusterType> types = modesMap.get(e.getKey());
			for (TileClusterType type : types) {
				TileClusterTemplate template = templates.get(type);
				buildClusterRouting(template, clusterInstances, cellLibrary);
			}
		}

		for (Map.Entry<List<TileType>, List<List<Tile>>> e : clusterInstancesMap.entrySet()) {
			Collection<TileClusterType> types = modesMap.get(e.getKey());
			for (TileClusterType type : types) {
				findDirectSourcesAndSinks(templates, e, type);
			}
		}

		// Build the pin group
		for (Map.Entry<List<TileType>, List<List<Tile>>> e : clusterInstancesMap.entrySet()) {
			Collection<TileClusterType> types = modesMap.get(e.getKey());
			for (TileClusterType type : types) {
				TileClusterTemplate template = templates.get(type);
				template.setPinGroups(PinGroup.buildClusterPinGroups(template));
			}
		}

		return tcd;
	}

	private void findDirectSourcesAndSinks(Map<TileClusterType, TileClusterTemplate> templates, Map.Entry<List<TileType>, List<List<Tile>>> e, TileClusterType type) {
		TileClusterTemplate template = templates.get(type);
		Map<List<Tile>, Map<Tile, Tile>> tileMaps = tileMapsMap.get(template.getType());

		DirectSourceAndSinkFinder finder = new DirectSourceAndSinkFinder();

		for (List<Tile> instance : e.getValue()) {
			Map<Tile, Tile> tileMap = tileMaps.get(instance);
			finder.findSourcesAndSinks(instance, tileMap);
		}

		template.setDirectSinksOfCluster(finder.sinks.stream().distinct().collect(Collectors.toList()));
		template.setDirectSourcesOfCluster(finder.sources.stream().distinct().collect(Collectors.toList()));
	}

	private void buildTileArray(TileClusterDevice tcd) {
		int maxColumns = tileMatrix.stream().mapToInt(List::size).max().getAsInt();
		Tile[][] deviceTiles = new Tile[tileMatrix.size()][maxColumns];
		for (int y = 0; y < tileMatrix.size(); y++) {
			List<Tile> row = tileMatrix.get(y);
			for (int x = 0; x < maxColumns; x++) {
				Tile tile;
				if (x < row.size() && row.get(x) != null) {
					tile = row.get(x);
				} else {
					tile = new Tile();
					tile.setName("NULL_X" + x + "Y" + y);
					tile.setType(TileType.NULL);
				}
				tile.setRow(y);
				tile.setColumn(x);
				deviceTiles[y][x] = tile;
			}
		}

		tcd.setTileArray(deviceTiles);
	}

	private static List<List<Tile>> findClusterInstances(List<TileType> tileTypes, Device device) {
		List<List<Tile>> tiles = new ArrayList<>();
		for (Tile tile : device.getTileMap().values()) {
			boolean found = true;
			for (int i = 0; i < tileTypes.size(); i++) {
				Tile indexed = device.getTile(tile.getRow(), tile.getColumn() + i);
				if (indexed == null || indexed.getType() !=  tileTypes.get(i)) {
					found = false;
					break;
				}
			}

			if (found) {
				List<Tile> templates = new ArrayList<>(tileTypes.size());
				for (int i = 0; i < tileTypes.size(); i++) {
					Tile indexed = device.getTile(tile.getRow(), tile.getColumn() + i);
					assert indexed != null;
					assert indexed.getType().equals(tileTypes.get(i));
					templates.add(indexed);
				}
				tiles.add(templates);
			}
		}
		return tiles;
	}

	private static Collection<List<List<PrimitiveType>>> getModes(List<Tile> tiles) {
		List<List<List<PrimitiveType>>> modes = new ArrayList<>();
		Iterator<List<List<PrimitiveType>>> it = new ModeIterator(tiles);
		Set<TileType> tileTypes = tiles.stream().map(Tile::getType).collect(Collectors.toSet());
		while (it.hasNext()) {
			List<List<PrimitiveType>> next = it.next();

			// Some filter code for unnecessary or rarely used
			if (tileTypes.contains(TileType.RIOB) || tileTypes.contains(TileType.LIOB) ||
					tileTypes.contains(TileType.LIOB_FT)) {
				boolean filter = false;
				for (List<PrimitiveType> mode : next) {
					if (mode.contains(PrimitiveType.IPAD) || mode.contains(PrimitiveType.ISERDESE1) || mode.contains(PrimitiveType.OSERDESE1))
						filter = true;
				}
				if (filter)
					continue;
			}
			if (tileTypes.contains(TileType.CLBLM)) {
				boolean hasSliceM = next.get(0).contains(PrimitiveType.SLICEM);
				if (!hasSliceM)
					continue;
			}
			modes.add(next);
		}

		return modes;
	}

	private static class ModeIterator implements Iterator<List<List<PrimitiveType>>> {
		private List<List<Integer>> index = new ArrayList<>();
		private List<Tile> tiles;
		private boolean done = false;

		public ModeIterator(List<Tile> tiles) {
			this.tiles = tiles;
			for (Tile tile : tiles) {
				PrimitiveSite[] sites = tile.getPrimitiveSites();
				ArrayList<Integer> siteMode = new ArrayList<>();
				for (PrimitiveSite ignored : sites) {
					siteMode.add(0);
				}
				index.add(siteMode);
			}
		}

		@Override
		public boolean hasNext() {
			return !done;
		}

		@Override
		public List<List<PrimitiveType>> next() {
			boolean inc = true;
			ArrayList<List<PrimitiveType>> mode = new ArrayList<>();
			for (int i = 0; i < index.size(); i++) {
				PrimitiveSite[] sites = tiles.get(i).getPrimitiveSites();
				List<Integer> siteIndex = index.get(i);
				ArrayList<PrimitiveType> siteMode = new ArrayList<>();
				for (int j = 0; j < siteIndex.size(); j++) {
					PrimitiveSite site = sites[j];
					int k = siteIndex.get(j);
					siteMode.add(site.getPossibleTypes()[k]);
					if (inc) {
						k += 1;
						if (k == site.getPossibleTypes().length) {
							siteIndex.set(j, 0);
							inc = true;
						} else {
							siteIndex.set(j, k);
							inc = false;
						}
					} else {
						inc = false;
					}
				}
				siteMode.trimToSize();
				mode.add(siteMode);
			}
			mode.trimToSize();

			if (inc)
				done = true;
			return mode;
		}
	}


	private void buildClusterTiles(
			TileClusterTemplate template, List<List<Tile>> clusterInstances
	) {
		Map<List<Tile>, Map<Tile, Tile>> tileMaps = new HashMap<>();
		Map<Point, Tile> tilePointMap = new HashMap<>();

		// first pass
		boolean firstInstance = true;
		for (List<Tile> instance : clusterInstances) {
			setMode(template, instance);

			Set<Tile> tileSet = new HashSet<>();
			tileSet.addAll(instance);
			findSwitchMatrices(instance, tileSet);

			for (Tile tile : tileSet) {
				Tile newTile = tilePointMap.computeIfAbsent(
						new Point(tile.getColumn() - instance.get(0).getColumn(),
								tile.getRow() - instance.get(0).getRow()),
						k -> new Tile()
				);
				if (newTile.getName() == null) {
					newTile.setName("unnamedTile" + numBuiltTiles++);
					newTile.setType(tile.getType());
					newTile.setDevice(tcd);
					newTile.setWireSites(tile.getWireSites());
				}
			}

			Map<Tile, Tile> tileMap = buildTileMap(tilePointMap, tileSet, instance);
			tileMaps.put(instance, tileMap);

			if (firstInstance) {
				buildSites(tileMap);
				setTileAndSiteNames(template, tilePointMap);
				setTilesAndBels(template, instance, tileMap);
			}
			firstInstance = false;
		}

		tileMapsMap.put(template.getType(), tileMaps);
	}

	private void buildClusterRouting(
			TileClusterTemplate template, List<List<Tile>> clusterInstances, CellLibrary cellLibrary
	) {
		Map<List<Tile>, Map<Tile, Tile>> tileMaps = tileMapsMap.get(template.getType());

		// second pass\
		List<Tile> instance = clusterInstances.get(0);
		setMode(template, instance);
		Map<Tile, Tile> tileMap = tileMaps.get(instance);

		template.setVccSources(getStaticSources(tileMap.values(), cellLibrary.getVccSource()));
		template.setGndSources(getStaticSources(tileMap.values(), cellLibrary.getGndSource()));
		ClusterConnectionsBuilder ccBuilder = new ClusterConnectionsBuilder();
		ccBuilder.findSourcesAndSinks(instance, tileMap);
		template.setSourcePins(ccBuilder.sinksOfSources);
		template.setSinkPins(ccBuilder.sourcesOfSinks);
		OutputsAndInputsFinder f = new OutputsAndInputsFinder();
		f.traverse(instance, tileMap);
		template.setInputs(f.inputs);
		template.setOutputs(new ArrayList<>(f.outputs));
		ClusterRoutingBuilder crb = new ClusterRoutingBuilder();
		crb.traverse(instance, tileMap);
		crb.finish();
		for (Tile tile : tileMap.values()) {
			tile.setWireHashMap(crb.forward.get(tile));
			tile.setReverseWireConnections(crb.reverse.get(tile));
		}
	}

	private void setTileAndSiteNames(TileClusterTemplate template, Map<Point, Tile> tilePointMap) {
		int leftCol = tilePointMap.keySet().stream().mapToInt(p -> p.x).min().getAsInt();
		int rightCol = tilePointMap.keySet().stream().mapToInt(p -> p.x).max().getAsInt();
		int topRow = tilePointMap.keySet().stream().mapToInt(p -> p.y).min().getAsInt();
		int botRow = tilePointMap.keySet().stream().mapToInt(p -> p.y).max().getAsInt();

		for (int j = 0; j <= botRow - topRow; j++) {
			ArrayList<Tile> row = new ArrayList<>();
			for (int i = 0; i <= rightCol - leftCol; i++) {
				Tile tile = tilePointMap.get(new Point(i + leftCol, j + topRow));
				if (tile == null) {
					row.add(null);
					continue;
				}
				String tileName = tile.getType() + "_" + template.getType().toString() + "_X" + i + "Y" + j;
				tile.setName(tileName);
				PrimitiveSite[] sites = tile.getPrimitiveSites();
				if (sites != null) {
					for (PrimitiveSite site : sites) {
						int index = site.getIndex();
						site.setName(tileName + "_" + site.getType() + "_X" + index + "Y" + index);
					}
				}
				row.add(tile);
			}
			row.trimToSize();
			tileMatrix.add(row);
		}
	}

	private Set<BelPin> getStaticSources(Collection<Tile> tileSet, LibraryCell sourceCell) {
		List<LibraryPin> pinList = sourceCell.getLibraryPins();
		assert pinList.size() == 1;
		LibraryPin sourcePin = pinList.get(0);
		Set<BelPin> sourcePins = new HashSet<>();
		for (Tile tile : tileSet) {
			PrimitiveSite[] sites = tile.getPrimitiveSites();
			if (sites == null)
				continue;
			for (PrimitiveSite site : sites) {
				if (site.getType() == PrimitiveType.TIEOFF)
					continue;
				for (Bel bel : site.getBels()) {
					List<String> pinNames = sourcePin.getPossibleBelPins(bel.getId());
					if (pinNames == null)
						continue;
					for (String pinName : pinNames) {
						BelPin belPin = bel.getBelPin(pinName);
						sourcePins.add(belPin);
					}
				}
			}
		}
		return sourcePins;
	}

	private void setTilesAndBels(TileClusterTemplate template, List<Tile> instance, Map<Tile, Tile> tileMap) {
		List<Bel> bels = new ArrayList<>();
		List<Tile> tiles = new ArrayList<>();
		for (Tile tile : instance) {
			Tile newTile = tileMap.get(tile);
			for (PrimitiveSite site : newTile.getPrimitiveSites()) {
				for (Bel bel : site.getBels())
					bels.add(bel);
			}
			tiles.add(newTile);
		}
		template.setBels(bels);
		template.setTiles(tiles);
	}

	private Map<Tile, Tile> buildTileMap(Map<Point, Tile> tileMap, Set<Tile> tileSet, List<Tile> instance) {
		Tile instanceTile = instance.get(0);
		Map<Tile, Tile> retMap = new HashMap<>();
		for (Tile tile : tileSet) {
			retMap.put(tile, tileMap.get(new Point(tile.getColumn() - instanceTile.getColumn(),
					tile.getRow() - instanceTile.getRow())));
		}
		return retMap;
	}

	private static void setMode(TileClusterTemplate template, List<Tile> instance) {
		List<List<PrimitiveType>> mode = template.getType().getMode();
		assert mode.size() == instance.size();
		for (int i = 0; i < mode.size(); i++) {
			List<PrimitiveType> templateSites = mode.get(i);
			PrimitiveSite[] sites = instance.get(i).getPrimitiveSites();
			assert sites != null && templateSites.size() == sites.length;
			for (int j = 0; j < templateSites.size(); j++) {
				sites[j].setType(templateSites.get(j));
			}
		}
	}

	private void buildSites(Map<Tile, Tile> tileMap) {
		for (Tile oldTile : tileMap.keySet()) {
			PrimitiveSite[] sites = oldTile.getPrimitiveSites();
			if (sites == null)
				continue;
			PrimitiveSite[] newSites = new PrimitiveSite[sites.length];
			Tile newTile = tileMap.get(oldTile);
			for (int i = 0; i < sites.length; i++) {
				PrimitiveSite oldSite = sites[i];
				assert oldSite.getIndex() == i;
				PrimitiveSite newSite = new PrimitiveSite();
				newSite.setIndex(oldSite.getIndex());
				newSite.setTile(newTile);
				PrimitiveType type = oldSite.getType();
				PrimitiveType[] possTypes = {type};
				newSite.setPossibleTypes(possTypes);
				newSite.setType(type);

				Map<PrimitiveType, Map<Integer, SitePinTemplate>> extWireToPinNameMap = new HashMap<>(1, 100.0f);
				extWireToPinNameMap.put(type, oldSite.getExternalWireToPinNameMap().get(type));
				newSite.setExternalWireToPinNameMap(extWireToPinNameMap);

				Map<PrimitiveType, Map<String, Integer>> externalWires = new HashMap<>(1, 100.0f);
				externalWires.put(type, oldSite.getExternalWires().get(type));
				newSite.setExternalWires(externalWires);

				newSites[i] = newSite;
			}
			newTile.setPrimitiveSites(newSites);
		}
	}

	private void findSwitchMatrices(List<Tile> root, Set<Tile> tileSet) {
		findSwitchMatrices(root, tileSet, true);
		findSwitchMatrices(root, tileSet, false);
	}

	private void findSwitchMatrices(List<Tile> root, Set<Tile> tileSet, boolean forward) {
		Queue<Wire> wireQueue = new LinkedList<>();
		Set<Wire> queuedWires = new HashSet<>();
		for (Tile tile : root) {
			for (PrimitiveSite site : tile.getPrimitiveSites()) {
				List<SitePin> sourcePins = forward ? site.getSourcePins() : site.getSinkPins();
				for (SitePin sourcePin : sourcePins) {
					Wire sourceWire = sourcePin.getExternalWire();
					wireQueue.add(sourceWire);
					queuedWires.add(sourceWire);
				}
			}
		}

		while (!wireQueue.isEmpty()) {
			Wire sourceWire = wireQueue.poll();

			for (Connection conn : getWireConnections(sourceWire, forward)) {
				Wire sinkWire = conn.getSinkWire();
				Tile sinkTile = sinkWire.getTile();
				if (root.contains(sinkTile) || INTERFACE_TILES.contains(sinkTile.getType())) {
					// add wire connection to stack
					if (!queuedWires.contains(sinkWire)) {
						wireQueue.add(sinkWire);
						queuedWires.add(sinkWire);
					}
					tileSet.add(sinkTile);
				} else if (SWITCH_MATRIX_TILES.contains(sinkTile.getType())) {
					tileSet.add(sinkTile);
				}
			}
		}
	}

	private Collection<Connection> getWireConnections(Wire sourceWire, boolean forward) {
		Tile sourceTile = sourceWire.getTile();
		WireConnection[] wcs = forward ?
				sourceTile.getWireConnections(sourceWire.getWireEnum()) :
				sourceTile.getReverseConnections(sourceWire.getWireEnum());
		if (wcs == null)
			return Collections.emptyList();

		Collection<Connection> connections = new ArrayList<>();
		for (WireConnection wc : wcs) {
			Tile sinkTile = wc.getTile(sourceTile);
			if (sinkTile == null)
				continue;

			Connection c = forward ?
					Connection.getTileWireConnection((TileWire) sourceWire, wc) :
					Connection.getReveserTileWireConnection((TileWire) sourceWire, wc);
			connections.add(c);
		}
		return connections;
	}

	private static final List<List<TileType>> packableTileTypes;
	public static final Set<TileType> SWITCH_MATRIX_TILES;
	protected static final Set<TileType> INTERFACE_TILES;
	static {
		packableTileTypes = new ArrayList<>();

		packableTileTypes.add(Collections.singletonList(TileType.CLBLM));
		packableTileTypes.add(Collections.singletonList(TileType.CLBLL));
		packableTileTypes.add(Collections.singletonList(TileType.DSP));
		packableTileTypes.add(Collections.singletonList(TileType.BRAM));
		packableTileTypes.add(Collections.singletonList(TileType.GTX));
		packableTileTypes.add(Collections.singletonList(TileType.EMAC));
		packableTileTypes.add(Collections.singletonList(TileType.HCLK_BRAM));
		packableTileTypes.add(Collections.singletonList(TileType.PCIE));
		packableTileTypes.add(Collections.singletonList(TileType.CFG_CENTER_1));
		packableTileTypes.add(Collections.singletonList(TileType.CFG_CENTER_0));
		packableTileTypes.add(Collections.singletonList(TileType.CFG_CENTER_2));
		packableTileTypes.add(Collections.singletonList(TileType.CFG_CENTER_3));
		packableTileTypes.add(Collections.singletonList(TileType.CMT_PMVA));
		packableTileTypes.add(Collections.singletonList(TileType.CMT_TOP));
//		packableTileTypes.add(Arrays.asList(TileType.LIOI));
//		packableTileTypes.add(Arrays.asList(TileType.RIOI));
//		packableTileTypes.add(Arrays.asList(TileType.LIOI));

		INTERFACE_TILES = new HashSet<>();
		INTERFACE_TILES.add(TileType.INT_INTERFACE);
		INTERFACE_TILES.add(TileType.IOI_L_INT_INTERFACE);
		INTERFACE_TILES.add(TileType.GTX_INT_INTERFACE);
		INTERFACE_TILES.add(TileType.EMAC_INT_INTERFACE);
		INTERFACE_TILES.add(TileType.PCIE_INT_INTERFACE_L);
		INTERFACE_TILES.add(TileType.PCIE_INT_INTERFACE_R);

		SWITCH_MATRIX_TILES = new HashSet<>();
		SWITCH_MATRIX_TILES.add(TileType.INT);
	}

	public static void main(String[] args) {
		String part = args[0];
		String cellLibraryPath = args[1];
		FamilyType family = PartNameTools.getFamilyTypeFromPart(part);
		RapidSmithEnv env = RapidSmithEnv.getDefaultEnv();

		CellLibrary cellLibrary;
		try {
			cellLibrary = new CellLibrary(Paths.get(cellLibraryPath));
		} catch (IOException e) {
			System.out.println("Error loading cellLibrary");
			return;
		}

		Path deviceDir = env.getDevicePath().resolve(family.name().toLowerCase());
		Path templatePath = deviceDir.resolve(part + "_template.dev");
		if (Files.exists(templatePath)) {
			boolean current = false;
			try {
				Hessian2Input his = FileTools.getCompactReader(templatePath);
				Object o = his.readObject();
				String version = (String) o.getClass().getDeclaredField("version").get(o);
				if (Objects.equals(version, TileClusterDevice.CURRENT_VERSION)) {
					current = true;
				}
			} catch (Exception ignored) {
			}

			if (current) {
				System.out.println("Part " + part + " already exists, skipping");
				return;
			}
		}
		System.out.println("Generating template for " + part);

		Device device = Device.getInstance(part);
		ExtendedDeviceInfo.loadExtendedInfo(device);

		TileClusterDevice templateDevice = new TileClusterGenerator().buildFromDevice(device, cellLibrary);

		try {
			Hessian2Output hos = FileTools.getCompactWriter(templatePath);
			hos.writeObject(templateDevice);
			hos.close();
		} catch (IOException e) {
			System.out.println("Error writing for device ...");
		}
	}
}
