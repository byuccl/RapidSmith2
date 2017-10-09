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

package edu.byu.ece.rapidSmith.device.creation;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.util.HashPool;

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
	private static final long serialVersionUID = -459840872618980717L;
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
		for (Tile tile : device.getTiles()) {
			threadPool.execute(() -> getReverseMapForTile(device, tile));
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
		for (Tile srcTile : device.getTiles()) {
			for (Wire srcWire : srcTile.getWires()) {
				int srcEnum = srcWire.getWireEnum();
				for (WireConnection c : srcTile.getWireConnections(srcEnum)) {
					if (c.getTile(srcTile) == tile) {
						WireConnection reverse = new WireConnection(
								srcEnum, -c.getRowOffset(),
								-c.getColumnOffset(), c.isPIP());
						WireConnection pooled = connPool.add(reverse);
						reverseMap.computeIfAbsent(c.getWire(), k -> new ArrayList<>())
								.add(pooled);
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

	/**
	 * @deprecated See @link{{@link Device#loadExtendedInfo()}}
	 * @param device the device to load info for
	 */
	public static void loadExtendedInfo(Device device) {
		device.loadExtendedInfo();
		}

	public static Path getExtendedInfoPath(Device device) {
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
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			if (hos != null) {
				hos.close();
			}
		}
		return info;
	}

	public Map<String, WireHashMap> getReversedWireMap() {
		return reversedWireHashMap;
	}

	public Map<SiteType, WireHashMap> getReversedSubsiteRouting() {
		return reversedSubsiteRouting;
	}
}
