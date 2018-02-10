package edu.byu.ece.rapidSmith.device.xdlrc;

import com.caucho.hessian.io.Hessian2Output;
import edu.byu.ece.rapidSmith.device.xdlrc.CompressedXDLRC.*;
import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.util.HashPool;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class XDLRCCompressor {
	public CompressedXDLRC compressXdlrc(Path xdlrcPath) throws IOException {
		XDLRCParser parser = new XDLRCParser();

		// first pass
		NamesListener namesListener = new NamesListener();
		parser.registerListener(namesListener);
		parser.registerListener(new XDLRCParseProgressListener());
		parser.parse(xdlrcPath);

		parser.clearListeners();
		CompressedXDLRC cxdlrc = new CompressedXDLRC();
		parser.registerListener(new CompressorListener(cxdlrc, namesListener));
		parser.registerListener(new XDLRCParseProgressListener());
		parser.parse(xdlrcPath);
		return cxdlrc;
	}

	public static void main(String[] args) {
		XDLRCCompressor compressor = new XDLRCCompressor();
		CompressedXDLRC cxdlrc = null;
		try {
			cxdlrc = compressor.compressXdlrc(Paths.get(args[0]));
		} catch (IOException e) {
			System.err.println("Error parsing xdlrc file: ");
			e.printStackTrace();
		}
		Path output;
		if (args.length > 1) {
			output = Paths.get(args[1]);
		} else {
			output = Paths.get(args[0].substring(0, args[0].lastIndexOf('.')) + ".cxdlrc");
		}

		Hessian2Output hos = null;
		try {
			hos = FileTools.getCompactWriter(output);
			hos.writeObject(cxdlrc);
		} catch (IOException e) {
			System.err.println("Error writing to file");
			e.printStackTrace();
		} finally {
			if (hos != null) {
				try {
					hos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class NamesListener extends XDLRCParserListener {
		HashPool<String> wireNames = new HashPool<>(50000);
		HashPool<String> tileNames = new HashPool<>(100000);

		@Override
		protected void enterTile(pl_Tile tokens) {
			tileNames.add2(tokens.name);
		}

		@Override
		protected void enterWire(pl_Wire tokens) {
			wireNames.add2(tokens.name);
		}

	}

	private class CompressorListener extends XDLRCParserListener {
		private HashPool<String> wireNames;
		private HashPool<String> tileNames;
		private HashPool<String> tileTypes = new HashPool<>(128);
		private HashPool<String> siteTypes = new HashPool<>(128);
		private HashPool<String> pinNames = new HashPool<>(16000);
		private HashPool<String> rtPins = new HashPool<>(128);
		private HashPool<CompressedConn> connsPool = new HashPool<>(2000000);
		private HashPool<ArrayList<CompressedConn>> connsListPool = new HashPool<>(1000000);
		private HashPool<CompressedPip> pipsPool = new HashPool<>(100000);
		private HashPool<ArrayList<CompressedPip>> pipListPool = new HashPool<>(100000);
		private HashPool<CompressedPinwire> pinWiresPool = new HashPool<>(10000);
		private HashPool<LinkedHashMap<Integer, ArrayList<CompressedConn>>> wireMapsPool = new HashPool<>(100000);

		CompressedXDLRC xdlrc;
		private CompressedTile currTile;
		private CompressedSite currSite;
		private ArrayList<CompressedConn> currWire;
		private CompressedPip currPip;
		private CompressedDef currDef;
		private CompressedElement currElement;
		private HashPool<String> currDefStrings;

		private int pipListCount;
		private int pipMatchCount;
		private int connListCount;
		private int connMatchCount;
		private int wireListCount;
		private int wireMatchCount;

		CompressorListener(CompressedXDLRC xdlrc, NamesListener names) {
			this.xdlrc = xdlrc;
			this.wireNames = names.wireNames;
			this.tileNames = names.tileNames;
		}

		@Override
		protected void enterXdlResourceReport(pl_XdlResourceReport tokens) {
			xdlrc.family = tokens.family;
			xdlrc.part = tokens.part;
			xdlrc.version = tokens.version;

			pipListCount = 0;
			pipMatchCount = 0;
		}

		@Override
		protected void exitXdlResourceReport(pl_XdlResourceReport tokens) {
			xdlrc.tileNames = tileNames.values();
			xdlrc.tileTypes = tileTypes.values();
			xdlrc.siteTypes = siteTypes.values();
			xdlrc.wireNames = wireNames.values();
			xdlrc.pinNames = pinNames.values();
			xdlrc.rtPins = rtPins.values();

			System.out.println(String.format("Matched %d of %d pip lists: %f%%", pipMatchCount, pipListCount, ((double) pipMatchCount) / pipListCount * 100));
			System.out.println(String.format("Matched %d of %d conn lists: %f%%", connMatchCount, connListCount, ((double) connMatchCount) / connListCount * 100));
			System.out.println(String.format("Matched %d of %d wire maps: %f%%", wireMatchCount, wireListCount, ((double) wireMatchCount) / wireListCount * 100));
		}

		@Override
		protected void enterTiles(pl_Tiles tokens) {
			xdlrc.rows = tokens.rows;
			xdlrc.columns = tokens.columns;
			xdlrc.tiles = new ArrayList<>(tokens.rows * tokens.columns);
		}

		@Override
		protected void enterTile(pl_Tile tokens) {
			currTile = new CompressedTile();
			currTile.name = tileNames.getEnumeration(tokens.name);
			currTile.type = tileTypes.add2(tokens.type);
			currTile.sites = new ArrayList<>();
			currTile.wires = new LinkedHashMap<>();
			currTile.pips = new ArrayList<>();
		}

		@Override
		protected void exitTile(pl_Tile tokens) {
			ArrayList<CompressedPip> pips = currTile.pips;
			ArrayList<CompressedPip> compressedPips = pipListPool.add(pips);

			pipListCount++;
			if (compressedPips == pips) {
				compressedPips.trimToSize();
			} else {
				currTile.pips = compressedPips;
				pipMatchCount++;
			}

			wireListCount++;
			LinkedHashMap<Integer, ArrayList<CompressedConn>> wires = currTile.wires;
			LinkedHashMap<Integer, ArrayList<CompressedConn>> compressedWires = wireMapsPool.add(wires);
			if (wires != compressedWires) {
				currTile.wires = compressedWires;
				wireMatchCount++;
			}

			xdlrc.tiles.add(currTile);
			currTile = null;
		}

		@Override
		protected void enterPrimitiveSite(pl_PrimitiveSite tokens) {
			currSite = new CompressedSite();
			currSite.name = tokens.name;
			currSite.type = siteTypes.add2(tokens.type);
			currSite.bonded = CompressedXDLRC.getBondedValue(tokens.bonded);
			currSite.pinwires = new ArrayList<>();
		}

		@Override
		protected void exitPrimitiveSite(pl_PrimitiveSite tokens) {
			currTile.sites.add(currSite);
			currSite = null;
		}

		@Override
		protected void enterPinWire(pl_PinWire tokens) {
			CompressedPinwire p = new CompressedPinwire();
			p.wireName = wireNames.getEnumeration(tokens.external_wire);
			p.pinName = pinNames.add2(tokens.name);
			p.direction = CompressedXDLRC.getDirectionValue(tokens.direction);
			currSite.pinwires.add(pinWiresPool.add(p));
		}

		@Override
		protected void enterWire(pl_Wire tokens) {
			currWire = new ArrayList<>();
		}

		@Override
		protected void exitWire(pl_Wire tokens) {
			connListCount++;
			ArrayList<CompressedConn> compressed = connsListPool.add(currWire);
			if (compressed == currWire) {
				compressed.trimToSize();
			} else {
				connMatchCount++;
			}
			currTile.wires.put(wireNames.getEnumeration(tokens.name), compressed);
			currWire = null;
		}

		@Override
		protected void enterConn(pl_Conn tokens) {
			CompressedConn c = new CompressedConn();
			c.sinkTileOffset = tileNames.getEnumeration(tokens.tile) - currTile.name;
			c.sinkWire = wireNames.getEnumeration(tokens.wire);
			currWire.add(connsPool.add(c));
		}

		@Override
		protected void enterPip(pl_Pip tokens) {
			currPip = new CompressedPip();
			currPip.source = wireNames.getEnumeration(tokens.start_wire);
			currPip.sink = wireNames.getEnumeration(tokens.end_wire);
			currPip.type = CompressedXDLRC.getPipTypeValue(tokens.type);
		}

		@Override
		protected void exitPip(pl_Pip tokens) {
			currTile.pips.add(pipsPool.add(currPip));
			currPip = null;
		}

		@Override
		protected void enterRoutethrough(pl_Routethrough tokens) {
			currPip.routethrough_type = siteTypes.add2(tokens.site_type);
			currPip.routethrough_pins = rtPins.add2(tokens.pins);
		}

		@Override
		protected void enterPrimitiveDefs(pl_PrimitiveDefs tokens) {
			xdlrc.primitive_defs = new ArrayList<>(tokens.num_defs);
		}

		@Override
		protected void enterPrimitiveDef(pl_PrimitiveDef tokens) {
			currDefStrings = new HashPool<>(500);
			currDef = new CompressedDef();
			currDef.name = currDefStrings.add2(tokens.name);
			currDef.pins = new ArrayList<>();
			currDef.elements = new ArrayList<>();
		}

		@Override
		protected void exitPrimitiveDef(pl_PrimitiveDef tokens) {
			xdlrc.primitive_defs.add(currDef);
			currDef.strings = currDefStrings.values();
			currDef = null;
			currDefStrings = null;
		}

		@Override
		protected void enterPin(pl_Pin tokens) {
			CompressedPin pin = new CompressedPin();
			pin.innerName = currDefStrings.add2(tokens.internal_name);
			pin.outerName = currDefStrings.add2(tokens.external_name);
			pin.direction = CompressedXDLRC.getDirectionValue(tokens.direction);
			currDef.pins.add(pin);
		}

		@Override
		protected void enterElement(pl_Element tokens) {
			currElement = new CompressedElement();
			currElement.name = currDefStrings.add2(tokens.name);
			currElement.isBel = tokens.isBel;
			currElement.pins = new ArrayList<>();
			currElement.conns = new ArrayList<>();
		}

		@Override
		protected void exitElement(pl_Element tokens) {
			if (currElement.pins.isEmpty())
				currElement.pins = null;
			if (currElement.conns.isEmpty())
				currElement.conns = null;
			currDef.elements.add(currElement);
			currElement = null;
		}

		@Override
		protected void enterElementPin(pl_ElementPin tokens) {
			CompressedElementPin pin = new CompressedElementPin();
			pin.name = currDefStrings.add2(tokens.name);
			pin.direction = CompressedXDLRC.getDirectionValue(tokens.direction);
			currElement.pins.add(pin);
		}

		@Override
		protected void enterElementConn(pl_ElementConn tokens) {
			CompressedElementConn conn = new CompressedElementConn();
			conn.e1 = currDefStrings.add2(tokens.element0);
			conn.p1 = currDefStrings.add2(tokens.pin0);
			conn.e2 = currDefStrings.add2(tokens.element1);
			conn.p2 = currDefStrings.add2(tokens.pin1);
			conn.direction = CompressedXDLRC.getElementConnArrowValue(tokens.direction);
			currElement.conns.add(conn);
		}

		@Override
		protected void enterElementCfg(pl_ElementCfg tokens) {
			int[] mapped = new int[tokens.cfgs.size()];
			List<String> cfgs = tokens.cfgs;
			for (int i = 0; i < cfgs.size(); i++) {
				String cfg = cfgs.get(i);
				mapped[i] = currDefStrings.add2(cfg);
			}
			currElement.cfgs = mapped;
		}

		@Override
		protected void enterSummary(pl_Summary tokens) {
			xdlrc.summary = new ArrayList<>(tokens.stats);
		}
	}
}
