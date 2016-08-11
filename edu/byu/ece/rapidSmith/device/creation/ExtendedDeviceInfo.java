package edu.byu.ece.rapidSmith.device.creation;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import edu.byu.ece.rapidSmith.RapidSmithEnv;
import edu.byu.ece.rapidSmith.design.subsite.Connection;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.device.helper.HashPool;
import edu.byu.ece.rapidSmith.device.helper.WireArray;
import edu.byu.ece.rapidSmith.util.FileTools;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ExtendedDeviceInfo implements Serializable {
	private transient ExecutorService threadPool;

	private transient final HashPool<WireConnection> connPool = new HashPool<>();
	private transient final HashPool<WireArray> connArrayPool = new HashPool<>();
	private transient final HashPool<WireHashMap> whmPool = new HashPool<>();

	private Map<String, WireHashMap> reversedWireHashMap = new HashMap<>(); // tile names to wirehashmap
	private Map<SiteType, WireHashMap> reversedSubsiteRouting = new HashMap<>();
	private Map<SiteType, Set<String>> pinsDrivingFabric = new HashMap<>(); // site template -> pin names
	private Map<SiteType, Set<String>> pinsDrivenByFabric = new HashMap<>(); // site template -> pin names

	public void buildExtendedInfo(Device device) {
		System.out.println("started at " + new Date());
		reverseWireHashMap(device);
		reverseSubsiteWires(device);
		System.out.println("reversed done at " + new Date());
		threadPool = Executors.newFixedThreadPool(8);
		buildDrivesGeneralFabric(device);
		buildDrivenByGeneralFabric(device);
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(2, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		System.out.println("driving done at " + new Date());

		storeValuesIntoStructure(device);
		Path partFolderPath = getExtendedInfoPath(device);
		try {
			writeCompressedFile(this, partFolderPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("compress done at " + new Date());
	}

	public static void writeCompressedFile(ExtendedDeviceInfo info, Path path) throws IOException {
		Hessian2Output hos = null;
		try {
			hos = FileTools.getCompactWriter(path);
			hos.writeObject(info);
		} finally {
			if (hos != null) {
				hos.close();
			}
		}
	}

	private void storeValuesIntoStructure(Device device) {
		for (Tile tile : device.getTileMap().values()) {
			reversedWireHashMap.put(tile.getName(), tile.getReverseWireHashMap());
		}
		for (SiteTemplate template : device.getSiteTemplates().values()) {
			reversedSubsiteRouting.put(template.getType(), template.getReversedWireHashMap());
		}
	}

	private void reverseWireHashMap(Device device) {
		threadPool = Executors.newFixedThreadPool(8);
		for (Tile[] arr : device.getTiles()) {
			for (Tile tile : arr) {
				threadPool.execute(() -> getReverseMapForTile(device, tile));
			}
		}
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(2, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		threadPool = null;
	}

	private void getReverseMapForTile(Device device, Tile tile) {
		Map<Integer, List<WireConnection>> reverseMap = new HashMap<>();
		for (Tile[] srcArr : device.getTiles()) {
			for (Tile srcTile : srcArr) {
				for (int srcWire : srcTile.getWires()) {
					for (WireConnection c : srcTile.getWireConnections(srcWire)) {
						if (c.getTile(srcTile) == tile) {
							WireConnection reverse = new WireConnection(
									srcWire, -c.getRowOffset(),
									-c.getColumnOffset(), c.isPIP());
							WireConnection pooled = connPool.add(reverse);
							reverseMap.computeIfAbsent(c.getWire(), k -> new ArrayList<>())
									.add(pooled);
						}
					}
				}
			}
		}

		WireHashMap wireHashMap = new WireHashMap();
		for (Map.Entry<Integer, List<WireConnection>> e : reverseMap.entrySet()) {
			List<WireConnection> v = e.getValue();
			WireArray wireArray = new WireArray(v.toArray(new WireConnection[v.size()]));
			wireHashMap.put(e.getKey(), connArrayPool.add(wireArray).array);
		}

		tile.setReverseWireConnections(whmPool.add(wireHashMap));
	}

	private void reverseSubsiteWires(Device device) {
		for (SiteTemplate site : device.getSiteTemplates().values()) {
			WireHashMap reversed = getReverseMapForSite(site);
			site.setReverseWireConnections(reversed);
		}
	}

	private WireHashMap getReverseMapForSite(SiteTemplate site) {
		Map<Integer, List<WireConnection>> reverseMap = new HashMap<>();
		for (int srcWire : site.getWires()) {
			for (WireConnection c : site.getWireConnections(srcWire)) {
				WireConnection reverse = new WireConnection(
						srcWire, -c.getRowOffset(),
						-c.getColumnOffset(), c.isPIP());
				WireConnection pooled = connPool.add(reverse);
				reverseMap.computeIfAbsent(c.getWire(), k -> new ArrayList<>())
						.add(pooled);
			}
		}

		WireHashMap wireHashMap = new WireHashMap();
		for (Map.Entry<Integer, List<WireConnection>> e : reverseMap.entrySet()) {
			List<WireConnection> v = e.getValue();
			WireArray wireArray = new WireArray(v.toArray(new WireConnection[v.size()]));
			wireHashMap.put(e.getKey(), connArrayPool.add(wireArray).array);
		}

		return wireHashMap;
	}

	private void buildDrivesGeneralFabric(Device device) {
		for (Site site : device.getPrimitiveSites().values()) {
			threadPool.execute(() -> buildDrivesForSite(device, site));
		}
	}

	private void buildDrivesForSite(Device device, Site site) {
		Map<SiteType, Set<String>> map = pinsDrivingFabric;
		for (SiteType type : site.getPossibleTypes()) {
			synchronized (map) {
				map.putIfAbsent(type, new HashSet<>());
			}
			site.setType(type);
			Set<String> pinSet = map.get(type);
			for (SitePin sitePin : site.getSourcePins()) {
				if (doesPinDriveGeneralFabric(device, sitePin)) {
					if (pinSet == null)
						System.out.println("null iwth " + type + " " + Objects.toString(sitePin));
					try {
						synchronized (pinSet) {
							try {
								pinSet.add(sitePin.getName());
							} catch (Exception e) {
								System.out.println("piSet " + Objects.toString(pinSet));
								System.out.println("sitePin " + Objects.toString(sitePin));
							}
						}
					} catch (Exception e) {
						System.out.println("I just caught an exception here \n" + e);
					}
				}
			}
		}
	}

	private static class WireDistancePair {
		public Wire wire;
		public int distance;

		public WireDistancePair(Wire wire, int distance) {
			this.wire = wire;
			this.distance = distance;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			WireDistancePair that = (WireDistancePair) o;
			return Objects.equals(wire, that.wire);
		}

		@Override
		public int hashCode() {
			return Objects.hash(wire);
		}
	}

	private boolean doesPinDriveGeneralFabric(Device device, SitePin sitePin) {
		Wire sourceWire = sitePin.getExternalWire();

		Queue<WireDistancePair> queue = new LinkedList<>();
		Set<Wire> queuedWires = new HashSet<>();
		queue.add(new WireDistancePair(sourceWire, 0));
		queuedWires.add(sourceWire);

		while (!queue.isEmpty()) {
			WireDistancePair wirePair = queue.poll();
			Wire wire = wirePair.wire;
			if (device.getSwitchMatrixTypes().contains(wire.getTile().getType()))
				return true;

			for (Connection c : wire.getWireConnections()) {
				Wire sink = c.getSinkWire();
				if (!queuedWires.contains(sink)) {
					int newDistance = wirePair.distance;
					if (!wire.getTile().equals(sink.getTile()))
						newDistance += 1;
					queuedWires.add(sink);
					queue.add(new WireDistancePair(sink, newDistance));
				}
			}
		}
		return false;
	}

	private void buildDrivenByGeneralFabric(Device device) {
		for (Site site : device.getPrimitiveSites().values()) {
			threadPool.execute(() -> buildDrivenByForSite(device, site));
		}
	}

	private void buildDrivenByForSite(Device device, Site site) {
		Map<SiteType, Set<String>> map = pinsDrivenByFabric;
		for (SiteType type : site.getPossibleTypes()) {
			Set<String> pinSet;
			synchronized (map) {
				pinSet = map.computeIfAbsent(type, k -> new HashSet<>());
			}
			site.setType(type);
			for (SitePin sitePin : site.getSinkPins()) {
				if (isPinDrivenByGeneralFabric(device, sitePin)) {
					try {
						synchronized (pinSet) {
							try {
								pinSet.add(sitePin.getName());
							} catch (Exception e) {
								System.out.println("piSet " + Objects.toString(pinSet));
								System.out.println("sitePin " + Objects.toString(sitePin));
							}
						}
					} catch (Exception e) {
						System.out.println("I just caught an exception here \n" + e);
					}
				}
			}
		}
	}

	private boolean isPinDrivenByGeneralFabric(Device device, SitePin sitePin) {
		Wire sourceWire = sitePin.getExternalWire();

		Queue<WireDistancePair> queue = new LinkedList<>();
		Set<Wire> queuedWires = new HashSet<>();
		queue.add(new WireDistancePair(sourceWire, 0));
		queuedWires.add(sourceWire);

		while (!queue.isEmpty()) {
			WireDistancePair wirePair = queue.poll();
			Wire wire = wirePair.wire;
			if (device.getSwitchMatrixTypes().contains(wire.getTile().getType()))
				return true;

			for (Connection c : getReverseConnection(wire)) {
				Wire sink = c.getSinkWire();
				if (!queuedWires.contains(sink)) {
					int newDistance = wirePair.distance;
					if (!wire.getTile().equals(sink.getTile()))
						newDistance += 1;
					queuedWires.add(sink);
					queue.add(new WireDistancePair(sink, newDistance));
				}
			}
		}
		return false;
	}

	private Iterable<Connection> getReverseConnection(Wire wire) {
		WireConnection[] cs = wire.getTile().getReverseConnections(wire.getWireEnum());
		if (cs == null)
			return Collections.emptyList();
		ArrayList<Connection> reversed = new ArrayList<>();
		for (WireConnection c : cs) {
			reversed.add(Connection.getReveserTileWireConnection((TileWire) wire, c));
		}
		return reversed;
	}

	public static void loadExtendedInfo(Device device) {
		Path partFolderPath = getExtendedInfoPath(device);
		ExtendedDeviceInfo info;
		try {
			info = loadCompressedFile(partFolderPath);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load");
		}
		for (Tile tile : device.getTileMap().values()) {
			tile.setReverseWireConnections(info.reversedWireHashMap.get(tile.getName()));
		}

		for (SiteType type : info.reversedSubsiteRouting.keySet()) {
			SiteTemplate template = device.getSiteTemplate(type);
			template.setReverseWireConnections(info.reversedSubsiteRouting.get(type));
		}

		for (SiteType type : info.pinsDrivingFabric.keySet()) {
			SiteTemplate template = device.getSiteTemplate(type);
			Set<String> pinsDrivingFabric = info.pinsDrivingFabric.get(type);
			for (String pinName : pinsDrivingFabric) {
				template.getSitePin(pinName).setDrivesGeneralFabric(true);
			}
			for (BelPinTemplate belPin : template.getBelPins().values()) {
				if (belPin.getSitePins().stream().anyMatch(pinsDrivingFabric::contains))
					belPin.setDrivesGeneralFabric(true);
			}
		}

		for (SiteType type : info.pinsDrivenByFabric.keySet()) {
			SiteTemplate template = device.getSiteTemplate(type);
			Set<String> pinsDrivenByFabric = info.pinsDrivenByFabric.get(type);
			for (String pinName : pinsDrivenByFabric) {
				template.getSitePin(pinName).setDrivenByGeneralFabric(true);
			}
			for (BelPinTemplate belPin : template.getBelPins().values()) {
				if (belPin.getSitePins().stream().anyMatch(pinsDrivenByFabric::contains))
					belPin.setDrivenByGeneralFabric(true);
			}
		}
	}

	private static Path getExtendedInfoPath(Device device) {
		RapidSmithEnv env = RapidSmithEnv.getDefaultEnv();
		Path partFolderPath = env.getPartFolderPath(device.getFamilyType());
		partFolderPath = partFolderPath.resolve(device.getPartName() + "_info.dat");
		return partFolderPath;
	}

	public static ExtendedDeviceInfo loadCompressedFile(Path path) throws IOException {
		Hessian2Input hos = null;
		ExtendedDeviceInfo info = null;
		try {
			hos = FileTools.getCompactReader(path);
			info = (ExtendedDeviceInfo) hos.readObject();
		} finally {
			if (hos != null) {
				hos.close();
			}
		}
		return info;
	}
}
