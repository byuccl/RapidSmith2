package edu.byu.ece.rapidSmith.device.creation;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import edu.byu.ece.rapidSmith.RSEnvironment;
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

	public void buildExtendedInfo(Device device) {
		System.out.println("started at " + new Date());
		reverseWireHashMap(device);
		reverseSubsiteWires(device);
		System.out.println("reversed done at " + new Date());
		threadPool = Executors.newFixedThreadPool(8);
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

	private static class WireDistancePair {
		public Wire wire;
		int distance;

		WireDistancePair(Wire wire, int distance) {
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
	}

	private static Path getExtendedInfoPath(Device device) {
		RSEnvironment env = RSEnvironment.defaultEnv();
		Path partFolderPath = env.getPartFolderPath(device.getFamily());
		partFolderPath = partFolderPath.resolve(device.getPartName() + "_info.dat");
		return partFolderPath;
	}

	public static ExtendedDeviceInfo loadCompressedFile(Path path) throws IOException {
		Hessian2Input hos = null;
		ExtendedDeviceInfo info;
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
