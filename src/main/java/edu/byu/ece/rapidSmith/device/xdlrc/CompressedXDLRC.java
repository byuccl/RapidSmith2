package edu.byu.ece.rapidSmith.device.xdlrc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

class CompressedXDLRC implements Serializable {
	private static final long serialVersionUID = -4599842580533474172L;
	ArrayList<String> tileNames;
	ArrayList<String> tileTypes;
	ArrayList<String> siteTypes;
	ArrayList<String> wireNames;
	ArrayList<String> pinNames;
	ArrayList<String> rtPins;

	String family, part, version;
	int rows = -1, columns = -1;
	ArrayList<CompressedTile> tiles;
	ArrayList<CompressedDef> primitive_defs;
	ArrayList<String> summary;

	static class CompressedTile implements Serializable {
		private static final long serialVersionUID = -6146519990057939282L;
		int name, type;
		ArrayList<CompressedSite> sites;
		LinkedHashMap<Integer, ArrayList<CompressedConn>> wires;
		ArrayList<CompressedPip> pips;
	}

	static class CompressedSite implements Serializable {
		private static final long serialVersionUID = 1932191052083546707L;
		String name;
		int type;
		int bonded;
		ArrayList<CompressedPinwire> pinwires;
	}

	static class CompressedPinwire implements Serializable {
		private static final long serialVersionUID = 4261604958000102574L;
		int direction, wireName, pinName;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CompressedPinwire that = (CompressedPinwire) o;
			return direction == that.direction &&
				wireName == that.wireName &&
				pinName == that.pinName;
		}

		@Override
		public int hashCode() {
			int hash = direction;
			hash = hash * 8191 + wireName;
			hash = hash * 8191 + pinName;
			return hash;
		}
	}

	static class CompressedConn implements Serializable {
		private static final long serialVersionUID = -3188099516627738787L;
		int sinkTileOffset, sinkWire;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CompressedConn that = (CompressedConn) o;
			return sinkTileOffset == that.sinkTileOffset &&
				sinkWire == that.sinkWire;
		}

		@Override
		public int hashCode() {
			return sinkTileOffset * 8191 + sinkWire;
		}
	}

	static class CompressedPip implements Serializable {
		private static final long serialVersionUID = -3108790735422968898L;
		int source, type, sink;
		public Integer routethrough_type = null;
		public Integer routethrough_pins = null;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CompressedPip that = (CompressedPip) o;
			return source == that.source &&
				type == that.type &&
				sink == that.sink &&
				Objects.equals(routethrough_type, that.routethrough_type) &&
				Objects.equals(routethrough_pins, that.routethrough_pins);
		}

		@Override
		public int hashCode() {
			int hash = source;
			hash = hash * 8191 + sink;
			return hash;
		}
	}

	static class CompressedDef implements Serializable {
		private static final long serialVersionUID = -6431239472188883208L;
		ArrayList<String> strings;
		int name;
		ArrayList<CompressedPin> pins;
		ArrayList<CompressedElement> elements;
	}

	static class CompressedPin implements Serializable {
		private static final long serialVersionUID = 913227610954455618L;
		int innerName, outerName, direction;
	}

	static class CompressedElement implements Serializable {
		private static final long serialVersionUID = 1298291395801543632L;
		public int name;
		public boolean isBel;
		int[] cfgs;
		ArrayList<CompressedElementPin> pins;
		ArrayList<CompressedElementConn> conns;
	}

	static class CompressedElementPin implements Serializable {
		private static final long serialVersionUID = -8605601375151457782L;
		int name, direction;
	}

	static class CompressedElementConn implements Serializable {
		private static final long serialVersionUID = -5922420833353618115L;
		int e1, p1, e2, p2, direction;
	}

	public static int getBondedValue(String value) {
		switch (value) {
			case "internal": return 0;
			case "bonded": return 1;
			case "unbonded": return 2;
		}
		throw new IllegalArgumentException("Invalid bonded value " + value);
	}

	public static String getBondedString(int value) {
		switch (value) {
			case 0: return "internal";
			case 1: return "bonded";
			case 2: return "unbonded";
		}
		throw new IllegalArgumentException("Invalid bonded value " + value);
	}

	public static int getPipTypeValue(String value) {
		switch (value) {
			case "->": return 0;
			case "=-": return 1;
		}
		throw new IllegalArgumentException("Invalid pip type " + value);
	}

	public static String getPipTypeString(int value) {
		switch (value) {
			case 0: return "->";
			case 1: return "=-";
		}
		throw new IllegalArgumentException("Invalid pip type " + value);
	}

	public static int getDirectionValue(String value) {
		switch (value) {
			case "input": return 0;
			case "output": return 1;
		}
		throw new IllegalArgumentException("Invalid direction " + value);
	}

	public static String getDirectionString(int value) {
		switch (value) {
			case 0: return "input";
			case 1: return "output";
		}
		throw new IllegalArgumentException("Invalid direction " + value);
	}

	public static int getElementConnArrowValue(String value) {
		switch (value) {
			case "==>": return 0;
			case "<==": return 1;
		}
		throw new IllegalArgumentException("Invalid arrow " + value);
	}

	public static String getElementConnDirectionString(int value) {
		switch (value) {
			case 0: return "==>";
			case 1: return "<==";
		}
		throw new IllegalArgumentException("Invalid arrow " + value);
	}

}

