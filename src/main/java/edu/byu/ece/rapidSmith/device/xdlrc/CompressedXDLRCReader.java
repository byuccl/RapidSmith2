package edu.byu.ece.rapidSmith.device.xdlrc;

import com.caucho.hessian.io.Hessian2Input;
import edu.byu.ece.rapidSmith.device.xdlrc.CompressedXDLRC.*;
import edu.byu.ece.rapidSmith.device.xdlrc.XDLRCParserListener.*;
import edu.byu.ece.rapidSmith.util.FileTools;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CompressedXDLRCReader {
	private final List<XDLRCParserListener> listeners;

	private pl_Conn pl_conn = new pl_Conn();
	private pl_Pip pl_pip = new pl_Pip();
	private pl_Wire pl_wire = new pl_Wire();
	private pl_PinWire pl_pinwire = new pl_PinWire();

	public CompressedXDLRCReader() {
		this.listeners = new ArrayList<>();
	}

	public static void main(String[] args) throws IOException {
		CompressedXDLRCReader reader = new CompressedXDLRCReader();
		reader.registerListener(new XDLRCRegurgitatorListener());
		reader.parse(Paths.get(args[0]));
	}

	/**
	 * Register a new listener with this parser.
	 * @param listener listener to register with this parser
	 */
	public void registerListener(XDLRCParserListener listener) {
		listeners.add(listener);
	}

	/**
	 * Clears all listeners currently associated with this parser.
	 */
	public void clearListeners() {
		listeners.clear();
	}

	public void parse(Path xdlrcFilePath) throws IOException {
		Hessian2Input compactReader = FileTools.getCompactReader(xdlrcFilePath);
		CompressedXDLRC cxdlrc = (CompressedXDLRC) compactReader.readObject();
		traverse(cxdlrc);
	}

	public void traverse(CompressedXDLRC cxdlrc) throws IOException {
		pl_XdlResourceReport xdlrr = new pl_XdlResourceReport();
		xdlrr.family = cxdlrc.family;
		xdlrr.part = cxdlrc.part;
		xdlrr.version = cxdlrc.version;
		listeners.forEach(it -> it.enterXdlResourceReport(xdlrr));

		parseTiles(cxdlrc);
		parseDefs(cxdlrc);

		pl_Summary summary = new pl_Summary();
		summary.stats = new ArrayList<>(cxdlrc.summary);
		listeners.forEach(it -> it.enterSummary(summary));
		listeners.forEach(it -> it.exitSummary(summary));

		listeners.forEach(it -> it.exitXdlResourceReport(xdlrr));
	}

	private void parseTiles(CompressedXDLRC cxdlrc) {
		pl_Tiles tiles = new pl_Tiles();
		tiles.rows = cxdlrc.rows;
		tiles.columns = cxdlrc.columns;
		listeners.forEach(it -> it.enterTiles(tiles));
		int tileIndex = 0;
		for (CompressedTile tile : cxdlrc.tiles) {
			visit(cxdlrc, tile, tileIndex++);
		}
		listeners.forEach(it -> it.exitTiles(tiles));
	}

	private void visit(CompressedXDLRC cxdlrc, CompressedTile ctile, int index) {
		pl_Tile tile = new pl_Tile();

		tile.name = cxdlrc.tileNames.get(ctile.name);
		tile.type = cxdlrc.tileTypes.get(ctile.type);
		tile.row = index / cxdlrc.columns;
		tile.column = index % cxdlrc.columns;
		tile.site_count = ctile.sites.size();
		listeners.forEach(it -> it.enterTile(tile));

		int pinCount = 0;
		for (CompressedSite site : ctile.sites) {
			pinCount += visit(cxdlrc, site);
		}
		ctile.wires.forEach((wire, conns) -> visit(cxdlrc, ctile, wire, conns));
		ctile.pips.forEach(pip -> visit(cxdlrc, tile, pip));
		summarize(ctile, tile, pinCount);

		listeners.forEach(it -> it.exitTile(tile));
	}

	private void summarize(CompressedTile ctile, pl_Tile tile, int pinCount) {
		pl_TileSummary summary = new pl_TileSummary();
		summary.name = tile.name;
		summary.type = tile.type;
		summary.pin_count = pinCount;
		summary.pip_count = ctile.pips.size();
		summary.wire_count = ctile.wires.size();
		listeners.forEach(it -> it.enterTileSummary(summary));
		listeners.forEach(it -> it.exitTileSummary(summary));
	}

	private int visit(CompressedXDLRC cxdlrc, CompressedSite csite) {
		pl_PrimitiveSite site = new pl_PrimitiveSite();
		site.name = csite.name;
		site.type = cxdlrc.siteTypes.get(csite.type);
		site.bonded = CompressedXDLRC.getBondedString(csite.bonded);
		site.pinwire_count = csite.pinwires.size();

		listeners.forEach(it -> it.enterPrimitiveSite(site));

		csite.pinwires.forEach(p -> visit(cxdlrc, p));

		listeners.forEach(it -> it.exitPrimitiveSite(site));
		return csite.pinwires.size();
	}

	private void visit(CompressedXDLRC cxdlrc, CompressedPinwire p) {
		pl_pinwire.name = cxdlrc.pinNames.get(p.pinName);
		pl_pinwire.external_wire = cxdlrc.wireNames.get(p.wireName);
		pl_pinwire.direction = CompressedXDLRC.getDirectionString(p.direction);
		listeners.forEach(it -> it.enterPinWire(pl_pinwire));
		listeners.forEach(it -> it.exitPinWire(pl_pinwire));
	}

	private void visit(CompressedXDLRC cxdlrc, CompressedTile ctile, Integer wire, ArrayList<CompressedConn> conns) {
		pl_wire.name = cxdlrc.wireNames.get(wire);
		pl_wire.connections_count = conns.size();
		listeners.forEach(it -> it.enterWire(pl_wire));

		for (CompressedConn conn : conns) {
			int actual = ctile.name + conn.sinkTileOffset;
			pl_conn.tile = cxdlrc.tileNames.get(actual);
			pl_conn.wire = cxdlrc.wireNames.get(conn.sinkWire);
			listeners.forEach(it -> it.enterConn(pl_conn));
			listeners.forEach(it -> it.exitConn(pl_conn));
		}

		listeners.forEach(it -> it.exitWire(pl_wire));
	}

	private void visit(CompressedXDLRC cxdlrc, pl_Tile tile, CompressedPip pip) {
		pl_pip.tile = tile.name;
		pl_pip.start_wire = cxdlrc.wireNames.get(pip.source);
		pl_pip.end_wire = cxdlrc.wireNames.get(pip.sink);
		pl_pip.type = CompressedXDLRC.getPipTypeString(pip.type);

		listeners.forEach(it -> it.enterPip(pl_pip));

		if (pip.routethrough_pins != null) {
			pl_Routethrough rt = new pl_Routethrough();
			rt.site_type = cxdlrc.siteTypes.get(pip.routethrough_type);
			rt.pins = cxdlrc.rtPins.get(pip.routethrough_pins);
			listeners.forEach(it -> it.enterRoutethrough(rt));
			listeners.forEach(it -> it.exitRoutethrough(rt));
		}

		listeners.forEach(it -> it.exitPip(pl_pip));
	}

	private void parseDefs(CompressedXDLRC cxdlrc) {
		pl_PrimitiveDefs defs = new pl_PrimitiveDefs();
		defs.num_defs = cxdlrc.primitive_defs.size();
		listeners.forEach(it -> it.enterPrimitiveDefs(defs));
		cxdlrc.primitive_defs.forEach(this::visit);
		listeners.forEach(it -> it.exitPrimitiveDefs(defs));
	}

	private void visit(CompressedDef cdef) {
		pl_PrimitiveDef def = new pl_PrimitiveDef();
		def.name = cdef.strings.get(cdef.name);
		def.element_count = cdef.elements.size();
		def.pin_count = cdef.pins.size();
		listeners.forEach(it -> it.enterPrimitiveDef(def));

		cdef.pins.forEach(cpin -> visit(cdef, cpin));
		cdef.elements.forEach(cel -> visit(cdef, cel));

		listeners.forEach(it -> it.exitPrimitiveDef(def));
	}

	private void visit(CompressedDef cdef, CompressedPin cpin) {
		pl_Pin pin = new pl_Pin();
		pin.external_name = cdef.strings.get(cpin.outerName);
		pin.internal_name = cdef.strings.get(cpin.innerName);
		pin.direction = CompressedXDLRC.getDirectionString(cpin.direction);
		listeners.forEach(it -> it.enterPin(pin));
		listeners.forEach(it -> it.exitPin(pin));
	}

	private void visit(CompressedDef cdef, CompressedElement cel) {
		pl_Element el = new pl_Element();
		el.name = cdef.strings.get(cel.name);
		el.isBel = cel.isBel;
		el.conn_count = (cel.conns != null) ? cel.conns.size() : 0;
		listeners.forEach(it -> it.enterElement(el));

		if (cel.pins != null)
			cel.pins.forEach(cpin -> visit(cdef, cpin));

		if (cel.cfgs != null) {
			pl_ElementCfg cfgs = new pl_ElementCfg();
			cfgs.cfgs = Arrays.stream(cel.cfgs)
				.mapToObj(it -> cdef.strings.get(it))
				.collect(Collectors.toList());
			listeners.forEach(it -> it.enterElementCfg(cfgs));
			listeners.forEach(it -> it.exitElementCfg(cfgs));
		}

		if (cel.conns != null)
			cel.conns.forEach(cconn -> visit(cdef, cconn));

		listeners.forEach(it -> it.exitElement(el));
	}

	private void visit(CompressedDef cdef, CompressedElementConn cconn) {
		pl_ElementConn conn = new pl_ElementConn();
		conn.element0 = cdef.strings.get(cconn.e1);
		conn.pin0 = cdef.strings.get(cconn.p1);
		conn.element1 = cdef.strings.get(cconn.e2);
		conn.pin1 = cdef.strings.get(cconn.p2);
		conn.direction = CompressedXDLRC.getElementConnDirectionString(cconn.direction);
		listeners.forEach(it -> it.enterElementConn(conn));
		listeners.forEach(it -> it.exitElementConn(conn));
	}

	private void visit(CompressedDef cdef, CompressedElementPin cpin) {
		pl_ElementPin pin = new pl_ElementPin();
		pin.name = cdef.strings.get(cpin.name);
		pin.direction = CompressedXDLRC.getDirectionString(cpin.direction);
		listeners.forEach(it -> it.enterElementPin(pin));
		listeners.forEach(it -> it.exitElementPin(pin));
	}
}
