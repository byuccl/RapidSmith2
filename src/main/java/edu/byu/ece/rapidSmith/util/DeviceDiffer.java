package edu.byu.ece.rapidSmith.util;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class DeviceDiffer {
	private Device deviceGold;
	private Device deviceTest;
	private WireEnumerator weGold;
	private WireEnumerator weTest;
	private boolean verbose;

	private HashSet<Pair> diffedWireHashMaps;

	private final class Pair {
		private final WireHashMap gold;
		private final WireHashMap test;

		public Pair(WireHashMap gold, WireHashMap test) {
			this.gold = gold;
			this.test = test;
		}

		public boolean equals(Object o) {
			if (o.getClass() != Pair.class)
				return false;
			Pair obj = (Pair) o;
			return obj.gold == gold && obj.test == test;
		}
	}

	private DifferenceList differences;

	public DeviceDiffer() {
		this(false);
	}

	public DeviceDiffer(boolean verbose) {
		this.verbose = verbose;
	}

	public void setGold(Device device) {
		this.deviceGold = device;
		this.weGold = device.getWireEnumerator();
	}

	public void setTest(Device device) {
		this.deviceTest = device;
		this.weTest = device.getWireEnumerator();
	}

	public DifferenceList diff() {
		diffedWireHashMaps = new HashSet<>();
		differences = new DifferenceList("device", deviceGold.getPartName(), verbose);

		if (!deviceGold.getPartName().equals(deviceTest.getPartName())) {
			differences.add("name", deviceGold.getPartName(), deviceTest.getPartName());
		}

		if (deviceGold.getRows() != deviceTest.getRows()) {
			differences.add("rows", "" + deviceGold.getRows(), "" + deviceTest.getRows());
		}

		if (deviceGold.getColumns() != deviceTest.getColumns()) {
			differences.add("columns", "" + deviceGold.getColumns(), "" + deviceTest.getColumns());
		}

		// diff tileMap
		diffTileMap();

		// TODO update Routethrough Map
//		Map<String, PIPRouteThrough> unseenRouteThroughs =
//				new HashMap<>(deviceTest.getRouteThroughMap().size());
//		for (WireConnection wc : deviceTest.getRouteThroughMap().keySet()) {
//			unseenRouteThroughs.put(weTest.getWireName(wc.getWire()),
//					deviceTest.getRouteThrough(wc));
//		}
//		for (WireConnection goldWc : deviceGold.getRouteThroughMap().keySet()) {
//			PIPRouteThrough testRT = unseenRouteThroughs.remove(
//					weGold.getWireName(goldWc.getWire()));
//			if (testRT == null) {
//				differences.add("routethrough", weGold.getWireName(goldWc.getWire()),
//						"none");
//				continue;
//			}
//
//			if (deviceGold.getRouteThrough(goldWc).getType() != testRT.getType()) {
//				differences.down("routethrough", weGold.getWireName(goldWc.getWire()));
//				differences.add("type", "" + deviceGold.getRouteThrough(goldWc).getType(),
//						"" + testRT.getType());
//				differences.up();
//			}
//		}
//		for (String rt : unseenRouteThroughs.keySet()) {
//			differences.add("routethrough", "none", rt);
//		}


		return differences;
	}

	private void diffTileMap() {
		Set<String> unseenTiles = new HashSet<>(deviceTest.getTileMap().keySet());
		for (String tileName : deviceGold.getTileMap().keySet()) {
			if (!unseenTiles.remove(tileName)) {
				differences.add("tile", tileName, "none");
				continue;
			}
			differences.down("tile", tileName);
			diffTiles(deviceGold.getTile(tileName), deviceTest.getTile(tileName));
			differences.up();
		}
		for (String tileName : unseenTiles) {
			differences.add("tile", "none", tileName);
		}
	}

	private void diffTiles(Tile gold, Tile test) {
		if (gold.getType() != test.getType()) {
			differences.add("type",	gold.getType().name(), test.getType().name());
		}

		if (gold.getRow() != test.getRow()) {
			differences.add("row", "" + gold.getRow(), "" + test.getRow());
		}

		if (gold.getColumn() != test.getColumn()) {
			differences.add("column", "" + gold.getColumn(), "" + test.getColumn());
		}

		diffSites(gold, test);
		diffTilesSources(gold, test);
		diffTileSinks(gold, test);
		diffWireHashMaps(gold.getWireHashMap(), test.getWireHashMap());
	}

	private void diffSites(Tile gold, Tile test) {
		if (gold.getSites() == null) {
			if (test.getSites() != null) {
				for (Site site : test.getSites()) {
					differences.add("primitive_site", "none", site.getName());
				}
			}
		} else {
			if (test.getSites() == null) {
				for (Site site : gold.getSites()) {
					differences.add("primitive_site", site.getName(), "none");
				}
			} else {
				int numGoldSites = gold.getSites().length;
				int numTestSites = test.getSites().length;
				int i, j;
				for (i = 0, j = 0; i < numGoldSites && j < numTestSites; i++, j++) {
					Site goldSite = gold.getSites()[i];
					Site testSite = test.getSites()[j];
					if (!goldSite.getName().equals(testSite.getName())) {
						boolean matched = false;
						int m = i, n = j;
						for (; n < numTestSites && !matched; n++) {
							matched = goldSite.getName().equals(test.getSites()[n].getName());
						}
						if (!matched)
							n = j;
						for (; m < numTestSites && !matched; m++) {
							matched = gold.getSites()[m].getName().equals(testSite.getName());
						}
						if (!matched)
							m = i;
						i = m;
						j = n;
						goldSite = gold.getSites()[i];
						testSite = test.getSites()[j];
					}
					diffSite(goldSite, testSite);
				}

				for (; i < numGoldSites; i++) {
					differences.add("primitive_site ",
							gold.getSites()[i].getName(), "none");
				}
				for (; j < numTestSites; j++) {
					differences.add("primitive_site ",
							"none", test.getSites()[j].getName());
				}
			}
		}
	}

	private void diffTilesSources(Tile gold, Tile test) {
		Set<String> sources = new HashSet<>();
		if (test.getSources() != null) {
			for (Wire source : test.getSources()) {
				sources.add(source.getWireName());
			}
		}
		if (gold.getSources() != null) {
			for (Wire source : gold.getSources()) {
				String sourceName = source.getWireName();
				if (!sources.remove(sourceName)) {
					differences.add("source", sourceName, "none");
				}
			}
		}
		for (String sourceName : sources) {
			differences.add("source", "none", sourceName);
		}
	}

	private void diffTileSinks(Tile gold, Tile test) {
		Set<String> sinks = new HashSet<>();
		if (test.getSinks() != null) {
			sinks.addAll(test.getSinks().keySet().stream()
					.map(weTest::getWireName)
					.collect(Collectors.toList()));
		}
		if (gold.getSinks() != null) {
			for (Integer sink : gold.getSinks().keySet()) {
				String sinkName = weGold.getWireName(sink);
				if (!sinks.remove(sinkName)) {
					differences.add("sinkPin", sinkName, "none");
//				} else if (gold.getSinkPin(sink).switchMatrixSinkWire == -1) {
//					// removed for v0.4 vs v0.5 testing
//					if (test.getSinkPin(weTest.getWireEnum(sinkName)).switchMatrixSinkWire != -1) {
//						differences.down("sinkPin", sinkName);
//						differences.add("switchbox" + sinkName, "no switchbox source", weTest.getWireName(
//								test.getSinkPin(weTest.getWireEnum(sinkName))
//								.switchMatrixSinkWire));
//						differences.up();
//					}
//				} else if (test.getSinkPin(weTest.getWireEnum(sinkName)).switchMatrixSinkWire == -1) {
//					differences.down("sinkPin", sinkName);
//					differences.add("switchbox", weGold.getWireName(gold.getSinkPin(sink).switchMatrixSinkWire),
//							"no switchbox source");
//					differences.up();
//				} else {
//					differences.down("sinkPin", sinkName);
//					String goldSwitchMatrixWire = weGold.getWireName(
//							gold.getSinkPin(sink).switchMatrixSinkWire);
//					String testSwitchMatrixWire = weTest.getWireName(
//							test.getSinkPin(weTest.getWireEnum(sinkName)).switchMatrixSinkWire);
//					if (!goldSwitchMatrixWire.equals(testSwitchMatrixWire)) {
//						differences.add("sinkMatrixWire", goldSwitchMatrixWire, testSwitchMatrixWire);
//					}
//
//					int goldSwitchMatrixOffset = gold.getSinkPin(sink).switchMatrixTileOffset;
//					int testSwitchMatrixOffset = test.getSinkPin(
//							weTest.getWireEnum(sinkName)).switchMatrixTileOffset;
//					if (goldSwitchMatrixOffset != testSwitchMatrixOffset) {
//						differences.add("sinkMatrixOffset", "" + goldSwitchMatrixOffset,
//								"" + testSwitchMatrixOffset);
//					}
//					differences.up();
				}
			}
		}
		for (String sinkName : sinks) {
			differences.add("sinkPin", "none", sinkName);
		}
	}

	private void diffSite(Site gold, Site test) {
		String siteName = gold.getName();
		differences.down("site", siteName);

		if (!gold.getName().equals(test.getName())) {
			differences.add("name", gold.getName(), test.getName());
		}

		if (gold.getType() != test.getType()) {
			differences.add("type", gold.getType().name(), test.getType().name());
		}

//		Set<String> unseenPins = new HashSet<>(test.getPins().keySet());
//		for (String pinName : gold.getPins().keySet()) {
//			if (!unseenPins.remove(pinName)) {
//				differences.add("pin", pinName, "none");
//			} else {
//				String goldPinWireName = weGold.getWireName(gold.getPins().get(pinName));
//				String testPinWireName = weTest.getWireName(test.getPins().get(pinName));
//				if (!goldPinWireName.equals(testPinWireName)) {
//					differences.down("pin", pinName);
//					differences.add("pinWire", goldPinWireName, testPinWireName);
//					differences.up();
//				}
//			}
//		}
//		for (String pinName : unseenPins) {
//			differences.add("pin", "none", pinName);
//		}
		differences.up();
	}

	private void diffWireHashMaps(WireHashMap gold, WireHashMap test) {
		if (diffedWireHashMaps.contains(new Pair(gold, test)))
			return;
		Set<String> unseenSources = new HashSet<>();
		if (test != null) {
			unseenSources.addAll(test.keySet().stream()
					.map(weTest::getWireName)
					.collect(Collectors.toList()));
		}
		if (gold != null) {
			for (int sourceNumber : gold.keySet()) {
				String source = weGold.getWireName(sourceNumber);
				if (!unseenSources.remove(source)) {
					differences.down("sourcewire", source);
					for (WireConnection wc : gold.get(sourceNumber)) {
						if (filterWireConnectionFixes1(wc)) continue;
						differences.add("sink", weGold.getWireName(wc.getWire()), "none");
					}
					differences.up();
				} else {
					assert test != null;
					differences.down("sourcewire", source);
					diffWireConnections(gold.get(sourceNumber),
							test.get(weTest.getWireEnum(source)));
					differences.up();
				}
			}
		}
		for (String source : unseenSources) {
			assert test != null;

			int testSourceNumber = weTest.getWireEnum(source);
			differences.down("sourcewire", source);
			for (WireConnection wc : test.get(testSourceNumber)) {
				if (filterWireConnectionFixes2(wc)) continue;
				differences.add("sink", "none", source);
			}
			differences.up();
		}

		diffedWireHashMaps.add(new Pair(gold, test));
	}

	private boolean filterWireConnectionFixes1(WireConnection wc) {
		// Masks a bug in v0.4 where some wires were being improperly declared as
		// site sources and sinks causing upstream counnections to be maintained
		WireType goldWireType = weGold.getWireType(wc.getWire());
		WireType testWireType = weTest.getWireType(weTest.getWireEnum(weGold.getWireName(wc.getWire())));
		return (goldWireType == WireType.SITE_SOURCE || goldWireType == WireType.SITE_SINK) &&
				!(testWireType == WireType.SITE_SOURCE || testWireType == WireType.SITE_SINK);
	}

	private boolean filterWireConnectionFixes2(WireConnection wc) {
		// Masks a bug in v0.4 where some wires were inadvertently being declared
		// site sources and sinks and not LONG like they should have been
		String sinkName = weTest.getWireName(wc.getWire());
		WireType goldWireType = weGold.getWireType(weGold.getWireEnum(sinkName));
		return (goldWireType == WireType.SITE_SOURCE || goldWireType == WireType.SITE_SINK) &&
				weTest.getWireType(weTest.getWireEnum(sinkName)) == WireType.LONG;
	}

	private void diffWireConnections(WireConnection[] gold, WireConnection[] test) {
		Map<String, Set<WireConnection>> unseenConnections = new HashMap<>();

		if (test != null) {
			for (WireConnection wc : test) {
				String testWire = weTest.getWireName(wc.getWire());
				if (!unseenConnections.containsKey(testWire)) {
					unseenConnections.put(testWire, new HashSet<>());
				}
				unseenConnections.get(testWire).add(wc);
			}
		}
		if (gold != null) {
			outer : for (WireConnection goldWc : gold) {
				String connName = weGold.getWireName(goldWc.getWire());
				Set<WireConnection> possibles = unseenConnections.get(
						connName);
				WireConnection testWc;
				if (possibles == null) {
					differences.add("connection", connName, "none");
				} else if (possibles.size() > 1) {
					Iterator<WireConnection> it = possibles.iterator();
					while (it.hasNext()) {
						WireConnection wc = it.next();
						if (wc.getRowOffset() == goldWc.getRowOffset() &&
								wc.getColumnOffset() == goldWc.getColumnOffset()) {
							it.remove();
							if (wc.isPIP() != goldWc.isPIP()) {
								differences.down("connection", connName);
								differences.add("pip", "" + goldWc.isPIP(), "" + wc.isPIP());
								differences.up();
							}
							continue outer;
						}
					}
					differences.down("connection", connName);
					differences.add("connection",
							"(" + goldWc.getRowOffset() + " " + goldWc.getColumnOffset() + ")",
							"none");
					differences.up();
				} else if (possibles.size() == 1) {
					testWc = possibles.iterator().next();
					unseenConnections.remove(connName);
					differences.down("connection", connName);
					if (goldWc.getRowOffset() != testWc.getRowOffset()) {
						differences.add("rowOffset", "" + goldWc.getRowOffset(),
								"" + testWc.getRowOffset());
					}
					if (goldWc.getColumnOffset() != testWc.getColumnOffset()) {
						differences.add("columnOffset", "" + goldWc.getColumnOffset(),
								"" + testWc.getColumnOffset());
					}
					if (goldWc.isPIP() != testWc.isPIP()) {
						differences.add("pip", "" + goldWc.isPIP(), "" + testWc.isPIP());
					}
					differences.up();
				}
			}
		}
		for (String wc : unseenConnections.keySet()) {
			if (filterWireConnectionFixes2(unseenConnections.get(wc).iterator().next()))
				continue;
			differences.add("connection", "none", wc);
		}
	}

	public static class DifferenceList {
		private boolean verbose;
		private DifferenceList curLevel;
		private String property;
		private String name;
		private List<DifferenceList> diffs;

		private DifferenceList() { } // for Difference class

		public DifferenceList(String type, String name) {
			this.property = type;
			this.name = name;
			curLevel = null;
			diffs = new ArrayList<>();
		}

		public DifferenceList(String type, String name, boolean verbose) {
			this.property = type;
			this.name = name;
			this.verbose = verbose;
			curLevel = null;
			diffs = new ArrayList<>();
		}

		public void down(String type, String name) {
			if (curLevel != null)
				curLevel.down(type, name);
			else
				curLevel = new DifferenceList(type, name);
		}

		public void up() {
			if (curLevel == null)
				return;

			if (curLevel.curLevel != null) {
				curLevel.up();
				return;
			}

			if (curLevel.diffs.size() > 0)
				diffs.add(curLevel);
			curLevel = null;
		}

		public void add(String property, String expected, String actual) {
			Difference diff = new Difference(property, expected, actual);
			if (curLevel != null)
				curLevel.add(diff);
			else {
				if (verbose)
					System.out.println(diff);
				diffs.add(diff);
			}
		}

		public void add(Difference diff) {
			if (curLevel != null)
				curLevel.add(diff);
			else {
				if (verbose)
					System.out.println(diff);
				diffs.add(diff);
			}
		}

		public String toString() {
			if (diffs.size() == 0)
				return "equivalent";
			return toString(new StringBuilder(), "");
		}

		protected String toString(StringBuilder sb, String indent) {
			sb.append(indent).append("(").append(property).append(" ").append(name).append("\n");
			String nextIndent = indent + "  ";
			for (DifferenceList dl : diffs) {
				//noinspection ResultOfMethodCallIgnored
				dl.toString(sb, nextIndent);
			}
			sb.append(indent).append(")\n");
			return sb.toString();
		}

		public static final class Difference extends DifferenceList {
			public String property;
			public String expected;
			public String actual;

			private Difference(String property, String expected, String actual) {
				this.property = property;
				this.expected = expected;
				this.actual = actual;
			}

			protected String toString(StringBuilder sb, String indent) {
				sb.append(indent).append("(").append(property).append(" ").append(expected)
						.append(" ").append(actual).append(")\n");
				return null;
			}
		}
	}

	public static void main(String[] args) {
		RSEnvironment rsGoldEnv = new RSEnvironment(Paths.get(args[0]));
		RSEnvironment rsTestEnv = new RSEnvironment(Paths.get(args[1]));
		String partName = args[2];
		boolean verbose = false;
		if (args.length > 3)
			verbose = Boolean.getBoolean(args[3]);

		Device goldDevice = rsGoldEnv.getDevice(partName);
		Device testDevice = rsTestEnv.getDevice(partName);

		DeviceDiffer differ = new DeviceDiffer(verbose);
		differ.setGold(goldDevice);
		differ.setTest(testDevice);

		DifferenceList diffs = differ.diff();

		if (args.length >= 5) {
			try {
				BufferedWriter bw = Files.newBufferedWriter(Paths.get(args[4]), Charset.defaultCharset());
				bw.write(diffs.toString());
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println(diffs);
		}
	}
}
