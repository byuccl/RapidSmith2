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

package edu.byu.ece.rapidSmith.util;

import edu.byu.ece.rapidSmith.design.xdl.XdlAttribute;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.primitiveDefs.PrimitiveConnection;
import edu.byu.ece.rapidSmith.primitiveDefs.PrimitiveDef;
import edu.byu.ece.rapidSmith.primitiveDefs.PrimitiveDefPin;
import edu.byu.ece.rapidSmith.primitiveDefs.PrimitiveElement;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 *
 */
public class XDLRCOutputter {
	private static final Pattern INTRASITE_PATTERN =
			Pattern.compile("intrasite:(.*)/(.*)\\.(.*)");

	private String nl = System.lineSeparator();
	private boolean writeWires = true;
	private boolean forceOrdering = false;
	private boolean buildDefsFromTemplates = false;
	private String ind = "\t";

	private WireEnumerator we;
	private Writer out;

	/**
	 * Sets the line separator to use when writing.
	 * <p>
	 * Defaults to <code>System.lineSeparator()</code>.
	 * @param nl the line separator
	 */
	public void setLineSeparator(String nl) {
		this.nl = nl;
	}

	/**
	 * Sets the indent for each level to use when writing.
	 * Warning: the parser may not work if the indent is not spaces or tabs.
	 * <p>
	 * Default is single tab per level.
	 * @param indent the indent to use
	 */
	public void setIndent(String indent) {
		this.ind = indent;
	}

	/**
	 * Determines whether wires and PIPs should be written.
	 * <p>
	 * Defaults to true.
	 * @param writeWires true to write wires and PIPs, false to skip
	 */
	public void writeWires(boolean writeWires) {
		this.writeWires = writeWires;
	}

	/**
	 * Determines if objects should be ordered to ensure consistent ordering of
	 * devices.
	 * In other words, this ensures that equivalent device file representations will
	 * yield identical XDLRC files when written.  This is useful for running
	 * text-based diffing tools to compare the devices.  Run time may be
	 * significantly longer if set.
	 * <p>
	 * Defaults to false.
	 * @param orderWires true to ensure consistent ordering
	 */
	public void forceOrdering(boolean orderWires) {
		this.forceOrdering = orderWires;
	}

	/**
	 * Determines whether to use the primitive def list of the device or to rebuild
	 * the structure from the site templates.  The two should be identical but this
	 * will be useful for validating this equivalency.  Building the defs from the
	 * templates may take longer to run.
	 * <p>
	 * Defaults to false.
	 * @param build false to use the existing primitive defs, true to rebuild from
	 *   the site templates.
	 */
	public void buildDefsFromTemplates(boolean build) {
		this.buildDefsFromTemplates = build;
	}

	/**
	 * Write the specified device to a file provided in the specified path.
	 * The path will be used to create a buffered writer.
	 * @param device the device to write
	 * @param outPath the path to the file to write to
	 * @throws IOException if any IO errors occur while writing
	 */
	public void writeDevice(Device device, Path outPath) throws IOException {
		writeDevice(device, null, outPath);
	}

	/**
	 * Write the specified device to a file provided in the specified path.
	 * The path will be used to create a buffered writer.
	 * @param device the device to write
	 * @param out the writer to write to
	 * @throws IOException if any IO errors occur while writing
	 */
	public void writeDevice(Device device, Writer out) throws IOException {
		writeDevice(device, null, out);
	}

	/**
	 * Write the specified device to a file provided in the specified path.
	 * The path will be used to create a buffered writer.
	 * @param device the device to write
	 * @param tiles set of tiles to write
	 * @param outPath the path to the file to write to
	 * @throws IOException if any IO errors occur while writing
	 */
	public void writeDevice(Device device, Set<Tile> tiles, Path outPath) throws IOException {
		try(Writer bw = Files.newBufferedWriter(outPath, Charset.defaultCharset())) {
			writeDevice(device, tiles, bw);
		}
	}

	/**
	 * Write the specified device to a file provided in the specified path.
	 * The path will be used to create a buffered writer.
	 * @param device the device to write
	 * @param tiles set of tiles to write
	 * @param out the writer to write to
	 * @throws IOException if any IO errors occur while writing
	 */
	public void writeDevice(Device device, Set<Tile> tiles, Writer out) throws IOException {
		this.we = device.getWireEnumerator();
		this.out = out;

		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy");

		out.append("# =======================================================" + nl);
		out.append("# XDL REPORT MODE $Revision: 1.8 $" + nl);
		out.append("# time: " + sdf.format(new Date()) + nl);
		out.append("# =======================================================" + nl);
		out.append("(xdl_resource_report v0.2 ");
		out.append(device.getPartName() + " ");
		out.append(device.getFamily().name().toLowerCase() + nl);
		out.append("# **************************************************************************" + nl);
		out.append("# *                                                                        *" + nl);
		out.append("# * Tile Resources                                                         *" + nl);
		out.append("# *                                                                        *" + nl);
		out.append("# **************************************************************************" + nl);
		out.append("(tiles " + device.getRows() + " " + device.getColumns() + nl);

		if (tiles == null) {
			for (int row = 0; row < device.getRows(); row++) {
				for (int col = 0; col < device.getColumns(); col++) {
					writeTile(device.getTile(row, col));
				}
			}
		} else {
			for (Tile tile : tiles)
				writeTile(tile);
		}
		out.append(")" + nl);

		out.append("(primitive_defs " + device.getPrimitiveDefs().size() + nl);
		List<PrimitiveDef> defs;
		if (buildDefsFromTemplates) {
			defs = device.getSiteTemplates().values().stream()
					.map(this::createPrimitiveDef)
					.collect(Collectors.toCollection(ArrayList<PrimitiveDef>::new));
		} else {
			defs = new ArrayList<>(device.getPrimitiveDefs().values());
		}
		if (forceOrdering)
			defs.sort(Comparator.comparing(o -> o.getType().name()));
		for (PrimitiveDef def : defs) {
			writePrimitiveDef(def);
		}

		out.append(")" + nl);
	}

	private void writeTile(Tile tile) throws IOException {
		out.append(ind + "(tile " + tile.getRow() + " " + tile.getColumn() + " " +
				tile.getName() + " " + tile.getType() + " " +
				(tile.getSites() == null ? "0" : tile.getSites().length) + nl);
		int numPinWires = 0;
		if (tile.getSites() != null) {
			for (Site site : tile.getSites()) {
				writeSite(site, writeWires);
				numPinWires += site.getSourcePins().size() + site.getSinkPins().size();
			}
		}

		if (writeWires) {
			List<String> wireNames = tile.getWires().stream()
					.map(Wire::getName)
					.collect(Collectors.toCollection(ArrayList<String>::new));
			if (forceOrdering)
				Collections.sort(wireNames);

			for (String wireName : wireNames) {
				Wire wire = new TileWire(tile, we.getWireEnum(wireName));
				out.append(ind + ind + "(wire ");
				out.append(wireName + " ");

				List<Connection> nonPips = new ArrayList<>();
				for (Connection c : wire.getWireConnections()) {
					if (!c.isPip())
						nonPips.add(c);
				}
				if (forceOrdering)
					nonPips.sort(new ConnectionComparator());

				out.append("" + nonPips.size());
				if (nonPips.size() == 0) {
					out.append(")" + nl);
				} else {
					out.append(nl);
					for (Connection c : nonPips) {
						Wire sinkWire = c.getSinkWire();
						out.append(ind + ind + ind + "(conn ");
						out.append(sinkWire.getTile() + " ");
						out.append(sinkWire.getName() + ")" + nl);
					}
					out.append(ind + ind + ")" + nl);
				}
			}

			int numPips = 0;
			for (String wireName : wireNames) {
				Wire sourceWire = new TileWire(tile, we.getWireEnum(wireName));

				List<Connection> pips = new ArrayList<>();
				for (Connection c : sourceWire.getWireConnections()) {
					if (c.isPip())
						pips.add(c);
				}
				if (forceOrdering)
					pips.sort(new ConnectionComparator());

				for (Connection c : pips) {
					out.append(ind + ind + "(pip ");
					out.append(tile.getName() + " ");
					out.append(wireName + " ");
					out.append(isBidirectionalPip(sourceWire, c.getSinkWire()) ? "=- " : "-> ");
					out.append(c.getSinkWire().getName());

					PIPRouteThrough rt = tile.getDevice().getRouteThrough(sourceWire, c.getSinkWire());
					if (rt != null) {
						out.append(" (_ROUTETHROUGH-" + rt.getInPin() + "-");
						out.append(rt.getOutPin() + " ");
						out.append(rt.getType().name() + "))" + nl);
					} else {
						out.append(")" + nl);
					}
					numPips++;
				}
			}

			out.append(ind + ind + "(tile_summary ");
			out.append(tile.getName() + " ");
			out.append(tile.getType().name() + " ");
			out.append(numPinWires + " ");
			out.append(tile.getWires().size() + " ");
			out.append(numPips + ")" + nl);
		}
		out.append(ind + ")" + nl);
	}

	private boolean isBidirectionalPip(Wire sourceWire, Wire sinkWire) {
		for (Connection rev : sinkWire.getWireConnections()) {
			Wire sourceOfSink = rev.getSinkWire();
			if (sourceWire.equals(sourceOfSink)) {
				return true;
			}
		}
		return false;
	}

	private void writeSite(Site site, boolean writeWires) throws IOException {
		out.append(ind + ind + "(primitive_site " + site.getName() + " ");
		out.append("" + site.getDefaultType());
		out.append(" " + site.getBondedType() + " ");
		int numPins = site.getSinkPins().size() + site.getSourcePins().size();
		out.append("" + numPins);
		if (!writeWires || numPins == 0) {
			out.append(")" + nl);
		} else {
			out.append(nl);

			List<String> sinkPins = new ArrayList<>(site.getSinkPinNames());
			if (forceOrdering)
				Collections.sort(sinkPins);
			for (String pinName : sinkPins) {
				SitePin pin = site.getSinkPin(pinName);
				out.append(ind + ind + ind + "(pinwire ");
				out.append(pinName + " ");
				out.append("input ");
				out.append(we.getWireName(pin.getExternalWire().getWireEnum()));
				out.append(")" + nl);
			}

			List<String> sourcePins = new ArrayList<>(site.getSourcePinNames());
			if (forceOrdering)
				Collections.sort(sourcePins);
			for (String pinName : sourcePins) {
				SitePin pin = site.getSourcePin(pinName);
				out.append(ind + ind + ind + "(pinwire ");
				out.append(pinName + " ");
				out.append("output ");
				out.append(we.getWireName(pin.getExternalWire().getWireEnum()));
				out.append(")" + nl);
			}
			out.append(ind + ind + ")" + nl);
		}
	}

	private void writePrimitiveDef(PrimitiveDef def) throws IOException {
		out.append(ind + "(primitive_def ");
		out.append(def.getType().name() + " ");
		out.append(def.getElements().size() + nl);

		List<PrimitiveDefPin> pins = new ArrayList<>(def.getPins());
		if (forceOrdering)
			pins.sort(Comparator.comparing(PrimitiveDefPin::getExternalName));
		for (PrimitiveDefPin pin : pins) {
			out.append(ind + ind + "(pin ");
			out.append(pin.getInternalName() + " ");
			out.append(directionToString(pin.getDirection()) + ")" + nl);
		}

		List<PrimitiveElement> els = new ArrayList<>(def.getElements());
		if (forceOrdering)
			els.sort(Comparator.comparing(PrimitiveElement::getName));
		for (PrimitiveElement el : els) {
			out.append(ind + ind + "(element ");
			out.append(el.getName() + " ");
			out.append("" + el.getPins().size());

			if (el.isBel()) {
				out.append(" # BEL");
			}
			out.append(nl);

			List<PrimitiveDefPin> elPins = new ArrayList<>(el.getPins());
			if (forceOrdering)
				elPins.sort(Comparator.comparing(PrimitiveDefPin::getExternalName));
			for (PrimitiveDefPin pin : el.getPins()) {
				out.append(ind + ind + ind + "(pin ");
				out.append(pin.getExternalName() + " ");
				out.append(directionToString(pin.getDirection()) + ")" + nl);
			}
			if (el.getCfgOptions() != null) {
				out.append(ind + ind + ind + "(cfg");

				List<String> cfgs = new ArrayList<>(el.getCfgOptions());
				if (forceOrdering)
					Collections.sort(cfgs);
				for (String cfg : cfgs) {
					out.append(" " + cfg);
				}
				out.append(")" + nl);
			}

			List<PrimitiveConnection> conns = new ArrayList<>(el.getConnections());
			if (forceOrdering) {
				conns.sort(Comparator.comparing(PrimitiveConnection::getElement0)
						.thenComparing(PrimitiveConnection::getPin0)
						.thenComparing(PrimitiveConnection::getElement1)
						.thenComparing(PrimitiveConnection::getPin1));
			}
			for (PrimitiveConnection c : conns) {
				out.append(ind + ind + ind + "(conn ");
				out.append(c.getElement0() + " ");
				out.append(c.getPin0() + " ");
				out.append((c.isForwardConnection()) ? "==> " : "<== ");
				out.append(c.getElement1() + " ");
				out.append(c.getPin1() + ")" + nl);
			}

			out.append(ind + ind + ")" + nl);
		}

		out.append(ind + ")" + nl);
	}

	private String directionToString(PinDirection pinDirection) {
		return pinDirection == PinDirection.IN ? "input" : "output";
	}

	private static class Pin {
		public final PrimitiveElement element;
		public final String pin;

		private Pin(PrimitiveElement el, String pin) {
			this.element = el;
			this.pin = pin;
		}
	}

	private PrimitiveDef createPrimitiveDef(SiteTemplate template) {
		Map<Integer, Pin> pinWiresMap = new HashMap<>();

		PrimitiveDef def = new PrimitiveDef();
		def.setType(template.getType());

		createPinElements(template, pinWiresMap, def);
		createBelElements(template, pinWiresMap, def);
		createPIPMuxElements(template, pinWiresMap, def);
		createSitePinConnections(template, pinWiresMap, def);
		createBelPinConnections(template, pinWiresMap, def);
		return def;
	}

	private void createPinElements(SiteTemplate template, Map<Integer, Pin> pinWiresMap, PrimitiveDef def) {
		for (SitePinTemplate sitePin : template.getSinks().values()) {
			PrimitiveDefPin defSitePin = new PrimitiveDefPin();
			defSitePin.setExternalName(sitePin.getName());
			defSitePin.setInternalName(sitePin.getName());
			defSitePin.setDirection(PinDirection.IN);
			def.addPin(defSitePin);

			PrimitiveElement element = new PrimitiveElement();
			element.setName(sitePin.getName());
			element.setPin(true);

			PrimitiveDefPin elPin = new PrimitiveDefPin();
			elPin.setInternalName(element.getName());
			elPin.setExternalName(element.getName());
			elPin.setDirection(PinDirection.OUT);
			element.addPin(elPin);

			def.addElement(element);
		}

		for (SitePinTemplate sitePin : template.getSources().values()) {
			PrimitiveDefPin defSitePin = new PrimitiveDefPin();
			defSitePin.setExternalName(sitePin.getName());
			defSitePin.setInternalName(sitePin.getName());
			defSitePin.setDirection(PinDirection.OUT);
			def.addPin(defSitePin);

			PrimitiveElement el = new PrimitiveElement();
			el.setName(sitePin.getName());
			el.setPin(true);

			PrimitiveDefPin elPin = new PrimitiveDefPin();
			elPin.setExternalName(el.getName());
			elPin.setInternalName(el.getName());
			elPin.setDirection(PinDirection.IN);
			el.addPin(elPin);

			def.addElement(el);

			pinWiresMap.put(sitePin.getInternalWire(), new Pin(el, el.getName()));
		}
	}

	private void createBelElements(SiteTemplate template, Map<Integer, Pin> pinWiresMap, PrimitiveDef def) {
		for (BelTemplate bel : template.getBelTemplates().values()) {
			PrimitiveElement el = new PrimitiveElement();
			el.setName(bel.getId().getName());
			el.setBel(true);
			for (BelPinTemplate belPin : bel.getSinks().values()) {
				PrimitiveDefPin pin = new PrimitiveDefPin();
				pin.setExternalName(belPin.getName());
				pin.setDirection(PinDirection.IN);
				el.addPin(pin);

				pinWiresMap.put(belPin.getWire(), new Pin(el, belPin.getName()));
			}
			for (BelPinTemplate belPin : bel.getSources().values()) {
				PrimitiveDefPin pin = new PrimitiveDefPin();
				pin.setExternalName(belPin.getName());
				pin.setDirection(PinDirection.OUT);
				el.addPin(pin);
			}
			def.addElement(el);
		}
	}

	private void createPIPMuxElements(SiteTemplate template, Map<Integer, Pin> pinWiresMap, PrimitiveDef def) {
		for (int source : template.getPipAttributes().keySet()) {
			int sink = template.getPipAttributes().get(source).keySet().iterator().next();
			XdlAttribute attr = template.getPipAttributes().get(source).values().iterator().next();

			String elName = attr.getPhysicalName();
			PrimitiveElement el = def.getElement(elName);
			if (el == null) {
				el = new PrimitiveElement();
				el.setName(elName);
				el.setMux(true);
				PrimitiveDefPin sinkPin = new PrimitiveDefPin();
				sinkPin.setExternalName(getPinNameFromWire(we.getWireName(sink)));
				sinkPin.setDirection(PinDirection.OUT);
				el.addPin(sinkPin);
			}
			PrimitiveDefPin sourcePin = new PrimitiveDefPin();
			sourcePin.setExternalName(attr.getValue());
			sourcePin.setDirection(PinDirection.IN);
			el.addPin(sourcePin);
			el.addCfgOption(attr.getValue());
			def.addElement(el);

			pinWiresMap.put(source, new Pin(el, attr.getValue()));
		}
	}

	private void createSitePinConnections(SiteTemplate template, Map<Integer, Pin> pinWiresMap, PrimitiveDef def) {
		for (SitePinTemplate sitePin : template.getSinks().values()) {
			PrimitiveElement el = def.getElement(sitePin.getName());

			int sitePinWire = sitePin.getInternalWire();
			WireConnection[] wcs = template.getWireConnections(sitePinWire);
			if (wcs == null)
				continue;

			Queue<Integer> wires = new LinkedList<>();
			for (WireConnection wc : wcs) {
				wires.add(wc.getWire());
			}

			while (!wires.isEmpty()) {
				int wire = wires.poll();
				if (pinWiresMap.containsKey(wire)) {
					Pin sink = pinWiresMap.get(wire);

					PrimitiveConnection fc = new PrimitiveConnection();
					fc.setElement0(el.getName());
					fc.setPin0(el.getName());
					fc.setForwardConnection(true);
					fc.setElement1(sink.element.getName());
					fc.setPin1(sink.pin);
					el.addConnection(fc);

					PrimitiveConnection bc = new PrimitiveConnection();
					bc.setElement0(el.getName());
					bc.setPin0(el.getName());
					bc.setForwardConnection(false);
					bc.setElement1(sink.element.getName());
					bc.setPin1(sink.pin);
					sink.element.addConnection(bc);

					continue;
				}

				wcs = template.getWireConnections(wire);
				if (wcs == null)
					continue;
				for (WireConnection wc : wcs) {
					wires.add(wc.getWire());
				}
			}
		}
	}

	private void createBelPinConnections(SiteTemplate template, Map<Integer, Pin> pinWiresMap, PrimitiveDef def) {
		for (BelTemplate bel : template.getBelTemplates().values()) {
			PrimitiveElement el = def.getElement(bel.getId().getName());

			for (BelPinTemplate belPin : bel.getSources().values()) {
				int pinWire = belPin.getWire();
				WireConnection[] wcs = template.getWireConnections(pinWire);
				if (wcs == null)
					continue;

				Queue<Integer> wires = new LinkedList<>();
				for (WireConnection wc : wcs) {
					wires.add(wc.getWire());
				}

				while (!wires.isEmpty()) {
					int wire = wires.poll();
					if (pinWiresMap.containsKey(wire)) {
						Pin sink = pinWiresMap.get(wire);

						PrimitiveConnection fc = new PrimitiveConnection();
						fc.setElement0(el.getName());
						fc.setPin0(belPin.getName());
						fc.setForwardConnection(true);
						fc.setElement1(sink.element.getName());
						fc.setPin1(sink.pin);
						el.addConnection(fc);

						PrimitiveConnection bc = new PrimitiveConnection();
						bc.setElement0(sink.element.getName());
						bc.setPin0(sink.pin);
						bc.setForwardConnection(false);
						bc.setElement1(el.getName());
						bc.setPin1(belPin.getName());
						sink.element.addConnection(bc);

						continue;
					}

					wcs = template.getWireConnections(wire);
					if (wcs == null)
						continue;
					for (WireConnection wc : wcs) {
						wires.add(wc.getWire());
					}
				}
			}
		}
	}

	private String getPinNameFromWire(String wireName) {
		Matcher mo = INTRASITE_PATTERN.matcher(wireName);
		//noinspection ResultOfMethodCallIgnored
		mo.find();
		return mo.group(3);
	}

	public static void main(String[] args) {
		OptionParser parser = new OptionParser();
		parser.acceptsAll(Arrays.asList("tile", "t"), "Prints only the specified tile").withRequiredArg();
		parser.accepts("wires", "Print wires of tiles");
		parser.acceptsAll(Arrays.asList("ordered", "o"), "Ensure consistent ordering");
		parser.acceptsAll(Arrays.asList("build_defs", "b"), "Build primitive defs section from site templates");
		parser.nonOptions("<device> [<output file>]");

		Arguments arguments = new Arguments(parser, args).invoke();
		Path output = arguments.getOutput();
		Device device = arguments.getDevice();
		Set<Tile> tiles = arguments.getTiles();

		XDLRCOutputter outputter = new XDLRCOutputter();
		outputter.writeWires(arguments.writeWires());
		outputter.forceOrdering(arguments.ordered());
		outputter.buildDefsFromTemplates(arguments.buildDefs());

		Writer out = null;
		try {
			out = Files.newBufferedWriter(output, Charset.defaultCharset());
		} catch (IOException e) {
			System.err.println("Could not open file for writing " + output);
			System.exit(-3);
		}

		try {
			outputter.writeDevice(device, tiles, out);
		} catch (IOException e) {
			System.err.println("Error writing to file.");
		} finally {
			try {
				out.close();
			} catch (IOException ignored) { }
		}
	}

	private static class Arguments {
		private final OptionParser parser;
		private final String[] args;
		private Device device;
		private Set<Tile> tiles;
		private boolean writeWires;
		private boolean ordered;
		private boolean buildDefs;
		private Path output;

		public Arguments(OptionParser parser, String... args) {
			this.parser = parser;
			this.args = args;
		}

		public Device getDevice() {
			return device;
		}

		public Set<Tile> getTiles() {
			return tiles;
		}

		public boolean writeWires() {
			return writeWires;
		}

		public boolean ordered() {
			return ordered;
		}

		public boolean buildDefs() {
			return buildDefs;
		}

		public Path getOutput() {
			return output;
		}

		public Arguments invoke() {
			OptionSet options = null;
			try {
				options = parser.parse(args);
			} catch (OptionException e) {
				try {
					parser.printHelpOn(System.err);
				} catch (IOException ignored) {
				}
				System.exit(-1);
			}

			if (options.nonOptionArguments().size() < 1) {
				try {
					parser.printHelpOn(System.err);
				} catch (IOException ignored) {
				}
				System.exit(-1);
			}

			try {
				device = Device.getInstance((String) options.nonOptionArguments().get(0));
			} catch (NullPointerException e) {
				device = null;
			}
			if (device == null) {
				System.err.println("Error loading part " + options.nonOptionArguments().get(0));
				System.exit(-2);
			}

			tiles = null;
			if (options.has("tile")) {
				// use a linked hash set to maintain the order tiles are added while
				// removing duplicates
				tiles = new LinkedHashSet<>();
				//noinspection unchecked,RedundantCast
				for (Object tileName : options.valuesOf("tile")) {
					Pattern pattern = null;
					try {
						pattern = Pattern.compile((String) tileName);
					} catch (PatternSyntaxException e) {
						System.err.println("Could not compile pattern " + tileName);
						System.exit(-4);
					}
					for (int row = 0; row < device.getRows(); row++) {
						for (int col = 0; col < device.getColumns(); col++) {
							Tile tile = device.getTile(0, 0);
							if (pattern.matcher(tile.getName()).matches()) {
								tiles.add(tile);
							}
						}
					}
				}
			}

			writeWires = options.has("wires");
			ordered = options.has("ordered");
			buildDefs = options.has("build_defs");

			output = Paths.get(device.getPartName() + ".xdlrc");
			if (options.nonOptionArguments().size() >= 2) {
				String p = (String) options.nonOptionArguments().get(1);
				if (!p.contains("."))
					p += ".xdlrc";
				output = Paths.get(p);
			}
			return this;
		}
	}

	private class ConnectionComparator implements Comparator<Connection> {
		@Override
		public int compare(Connection o1, Connection o2) {
			return Comparator.comparing((Connection o) -> o.getSinkWire().getTile().getName())
					.thenComparing(o -> o.getSinkWire().getWireName())
					.compare(o1, o2);
		}
	}
}
