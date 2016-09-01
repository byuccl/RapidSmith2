package edu.byu.ece.rapidSmith.util;

import edu.byu.ece.rapidSmith.device.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class RoutingInverter {
	public static Map<TileType, WireHashMap> invertRouting(Device device) {
		Map<TileType, Map<Integer, Set<WireConnection>>> setOfWcs = new HashMap<>();

		for (Tile tile : device.getTileMap().values()) {
			setOfWcs.computeIfAbsent(tile.getType(), k -> new HashMap<>());
			for (int source : tile.getWires()) {
				for (WireConnection wc : tile.getWireConnections(source)) {
					int sink = wc.getWire();
					TileType sinkTile = wc.getTile(tile).getType();

					WireConnection inverse = new WireConnection(
							source, -wc.getRowOffset(),
							-wc.getColumnOffset(), wc.isPIP());

					Map<Integer, Set<WireConnection>> map =
							setOfWcs.computeIfAbsent(sinkTile, k -> new HashMap<>());
					Set<WireConnection> wcSet = map.computeIfAbsent(sink, k -> new HashSet<>());
					wcSet.add(inverse);
				}
			}
		}

		Map<TileType, WireHashMap> invertedMap = new HashMap<>();
		for (TileType tt : setOfWcs.keySet()) {
			WireHashMap whm = new WireHashMap();
			invertedMap.put(tt, whm);
			for (Integer wire : setOfWcs.get(tt).keySet()) {
				Set<WireConnection> set = setOfWcs.get(tt).get(wire);
				WireConnection[] arr = set.toArray(new WireConnection[set.size()]);
				whm.put(wire, arr);
			}
		}

		return invertedMap;
	}
}
