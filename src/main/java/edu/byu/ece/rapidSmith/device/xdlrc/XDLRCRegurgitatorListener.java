package edu.byu.ece.rapidSmith.device.xdlrc;

public class XDLRCRegurgitatorListener extends XDLRCParserListener {
	@Override
	protected void enterXdlResourceReport(pl_XdlResourceReport tokens) {
		System.out.println(String.format("(xdl_resource_report %s %s %s", tokens.version, tokens.part, tokens.family));
	}

	@Override
	protected void exitXdlResourceReport(pl_XdlResourceReport tokens) {
		System.out.println(")");
	}

	@Override
	protected void enterTiles(pl_Tiles tokens) {
		System.out.println(String.format("(tiles %d %d", tokens.rows, tokens.columns));
	}

	@Override
	protected void exitTiles(pl_Tiles tokens) {
		System.out.println("\t)");
	}

	@Override
	protected void enterTile(pl_Tile tokens) {
		System.out.println(String.format("\t(tile %d %d %s %s %d", tokens.row, tokens.column, tokens.name, tokens.type, tokens.site_count));
	}

	@Override
	protected void exitTile(pl_Tile tokens) {
		System.out.println("\t)");
	}

	@Override
	protected void enterPrimitiveSite(pl_PrimitiveSite tokens) {
		System.out.println(String.format("\t\t(primitive_site %s %s %s %d", tokens.name, tokens.type, tokens.bonded, tokens.pinwire_count));
	}

	@Override
	protected void exitPrimitiveSite(pl_PrimitiveSite tokens) {
		System.out.println("\t\t)");
	}

	@Override
	protected void enterPinWire(pl_PinWire tokens) {
		System.out.println(String.format("\t\t\t(pinwire %s %s %s)", tokens.name, tokens.direction, tokens.external_wire));
	}

	@Override
	protected void enterWire(pl_Wire tokens) {
		System.out.printf("\t\t(wire %s %d", tokens.name, tokens.connections_count);
		if (tokens.connections_count == 0)
			System.out.print(")");
		System.out.println();
	}

	@Override
	protected void exitWire(pl_Wire tokens) {
		if (tokens.connections_count > 0)
			System.out.println("\t\t)");
	}

	@Override
	protected void enterConn(pl_Conn tokens) {
		System.out.printf("\t\t\t(conn %s %s)", tokens.tile, tokens.wire);
		System.out.println();
	}

	@Override
	protected void enterPip(pl_Pip tokens) {
		System.out.printf("\t\t(pip %s %s %s %s", tokens.tile, tokens.start_wire, tokens.type, tokens.end_wire);
	}

	@Override
	protected void exitPip(pl_Pip tokens) {
		System.out.println(")");
	}

	@Override
	protected void enterRoutethrough(pl_Routethrough tokens) {
		System.out.printf(" %s %s)", tokens.pins, tokens.site_type);
	}

	@Override
	protected void enterTileSummary(pl_TileSummary tokens) {
		System.out.printf("\t\t(tile_summary %s %s %d %d %d)", tokens.name, tokens.type, tokens.pin_count, tokens.wire_count, tokens.pip_count);
		System.out.println();
	}

	@Override
	protected void enterPrimitiveDefs(pl_PrimitiveDefs tokens) {
		System.out.printf("(primitive_defs %d", tokens.num_defs);
		System.out.println();
	}

	@Override
	protected void exitPrimitiveDefs(pl_PrimitiveDefs tokens) {
		System.out.println(")");
	}

	@Override
	protected void enterPrimitiveDef(pl_PrimitiveDef tokens) {
		System.out.printf("\t(primitive_def %s %d %d", tokens.name, tokens.pin_count, tokens.element_count);
		System.out.println();
	}

	@Override
	protected void exitPrimitiveDef(pl_PrimitiveDef tokens) {
		System.out.println("\t)");
	}

	@Override
	protected void enterPin(pl_Pin tokens) {
		System.out.printf("\t\t(pin %s %s %s)", tokens.internal_name, tokens.external_name, tokens.direction);
		System.out.println();
	}

	@Override
	protected void enterElement(pl_Element tokens) {
		System.out.printf("\t\t(element %s %d", tokens.name, tokens.conn_count);
		if (tokens.isBel)
			System.out.print(" # BEL");
		System.out.println();
	}

	@Override
	protected void exitElement(pl_Element tokens) {
		System.out.println("\t\t)");
	}

	@Override
	protected void enterElementPin(pl_ElementPin tokens) {
		System.out.printf("\t\t\t(pin %s %s)", tokens.name, tokens.direction);
		System.out.println();
	}

	@Override
	protected void enterElementConn(pl_ElementConn tokens) {
		System.out.printf("\t\t\t(conn %s %s %s %s %s)", tokens.element0, tokens.pin0, tokens.direction,
			tokens.element1, tokens.pin1);
		System.out.println();
	}

	@Override
	protected void enterElementCfg(pl_ElementCfg tokens) {
		System.out.print("\t\t\t(cfg");
		for (String cfg : tokens.cfgs)
			System.out.print(" " + cfg);
		System.out.println(")");
	}

	@Override
	protected void enterSummary(pl_Summary tokens) {
		System.out.print("(summary");
		for (String stat : tokens.stats)
			System.out.print(" " + stat);
		System.out.println(")");
	}
}
