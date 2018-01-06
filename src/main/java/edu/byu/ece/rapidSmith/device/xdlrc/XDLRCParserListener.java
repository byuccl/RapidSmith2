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

package edu.byu.ece.rapidSmith.device.xdlrc;

import java.util.List;

/**
 *  Base class for XDLRC Parser listeners.  When the parser enters or exists an
 *  XDLRC parse element, the parser will call the associated listener method of
 *  each registered listener.
 */
public abstract class XDLRCParserListener {
	protected void enterXdlResourceReport(pl_XdlResourceReport tokens) { }
	protected void exitXdlResourceReport(pl_XdlResourceReport tokens) { }
	protected void enterTiles(pl_Tiles tokens) { }
	protected void exitTiles(pl_Tiles tokens) { }
	protected void enterTile(pl_Tile tokens) { }
	protected void exitTile(pl_Tile tokens) { }
	protected void enterPrimitiveSite(pl_PrimitiveSite tokens) { }
	protected void exitPrimitiveSite(pl_PrimitiveSite tokens) { }
	protected void enterPinWire(pl_PinWire tokens) { }
	protected void exitPinWire(pl_PinWire tokens) { }
	protected void enterWire(pl_Wire tokens) { }
	protected void exitWire(pl_Wire tokens) { }
	protected void enterConn(pl_Conn tokens) { }
	protected void exitConn(pl_Conn tokens) { }
	protected void enterTileSummary(pl_TileSummary tokens) { }
	protected void exitTileSummary(pl_TileSummary tokens) { }
	protected void enterPip(pl_Pip tokens) { }
	protected void exitPip(pl_Pip tokens) { }
	protected void enterRoutethrough(pl_Routethrough tokens) { }
	protected void exitRoutethrough(pl_Routethrough tokens) { }
	protected void enterPrimitiveDefs(pl_PrimitiveDefs tokens) { }
	protected void exitPrimitiveDefs(pl_PrimitiveDefs tokens) { }
	protected void enterPrimitiveDef(pl_PrimitiveDef tokens) { }
	protected void exitPrimitiveDef(pl_PrimitiveDef tokens) { }
	protected void enterPin(pl_Pin tokens) { }
	protected void exitPin(pl_Pin tokens) { }
	protected void enterElement(pl_Element tokens) { }
	protected void exitElement(pl_Element tokens) { }
	protected void enterElementPin(pl_ElementPin tokens) { }
	protected void exitElementPin(pl_ElementPin tokens) { }
	protected void enterElementConn(pl_ElementConn tokens) { }
	protected void exitElementConn(pl_ElementConn tokens) { }
	protected void enterElementCfg(pl_ElementCfg tokens) { }
	protected void exitElementCfg(pl_ElementCfg tokens) { }
	protected void enterSummary(pl_Summary tokens) { }
	protected void exitSummary(pl_Summary tokens) { }

	public static final class pl_XdlResourceReport {
		public String version;
		public String part;
		public String family;

		public void set(List<String> tokens) {
			this.version = tokens.get(1);
			this.part = tokens.get(2);
			this.family = tokens.get(3);
		}
	}

	public static final class pl_Tiles {
		public int rows;
		public int columns;

		public void set(List<String> tokens) {
			this.rows = Integer.parseInt(tokens.get(1));
			this.columns = Integer.parseInt(tokens.get(2));
		}
	}

	public static final class pl_Tile {
		public int row;
		public int column;
		public String name;
		public String type;
		public int site_count;

		public void set(List<String> tokens) {
			this.row = Integer.parseInt(tokens.get(1));
			this.column = Integer.parseInt(tokens.get(2));
			this.name = tokens.get(3);
			this.type = tokens.get(4);
			this.site_count = Integer.parseInt(tokens.get(5));
		}
	}

	public static final class pl_PrimitiveSite {
		public String name;
		public String type;
		public String bonded;
		public int pinwire_count;

		public void set(List<String> tokens) {
			this.name = tokens.get(1);
			this.type = tokens.get(2);
			this.bonded = tokens.get(3);
			this.pinwire_count = Integer.parseInt(tokens.get(4));
		}
	}

	public static final class pl_PinWire {
		public String name;
		public String direction;
		public String external_wire;

		public void set(List<String> tokens) {
			this.name = tokens.get(1);
			this.direction = tokens.get(2);
			this.external_wire = tokens.get(3);
		}
	}

	public static final class pl_Wire {
		public String name;
		public int connections_count;

		public void set(List<String> tokens) {
			this.name = tokens.get(1);
			this.connections_count = Integer.parseInt(tokens.get(2));
		}
	}

	public static final class pl_Conn {
		public String tile;
		public String wire;

		public void set(List<String> tokens) {
			this.tile = tokens.get(1);
			this.wire = tokens.get(2);
		}
	}

	public static final class pl_TileSummary {
		// (tile_summary <name> <type> <pin_count> <wire_count> <pip_count>
		public String name;
		public String type;
		public int pin_count;
		public int wire_count;
		public int pip_count;

		public void set(List<String> tokens) {
			this.name = tokens.get(1);
			this.type = tokens.get(2);
			this.pin_count = Integer.parseInt(tokens.get(3));
			this.wire_count = Integer.parseInt(tokens.get(4));
			this.pip_count = Integer.parseInt(tokens.get(5));
		}
	}

	public static final class pl_Pip {
		public String tile;
		public String start_wire;
		public String type;
		public String end_wire;

		public void set(List<String> tokens) {
			this.tile = tokens.get(1);
			this.start_wire = tokens.get(2);
			this.type = tokens.get(3);
			this.end_wire = tokens.get(4);
		}
	}

	public static final class pl_Routethrough {
		public String pins;
		public String site_type;

		public void set(List<String> tokens) {
			this.pins = tokens.get(5);
			this.site_type = tokens.get(6);
		}
	}

	public static final class pl_PrimitiveDefs {
		public int num_defs;

		public void set(List<String> tokens) {
			this.num_defs = Integer.parseInt(tokens.get(1));
		}
	}

	public static final class pl_PrimitiveDef {
		public String name;
		public int pin_count;
		public int element_count;

		public void set(List<String> tokens) {
			this.name = tokens.get(1);
			this.pin_count = Integer.parseInt(tokens.get(2));
			this.element_count = Integer.parseInt(tokens.get(3));
		}
	}

	public static final class pl_Pin {
		public String external_name;
		public String internal_name;
		public String direction;

		public void set(List<String> tokens) {
			this.external_name = tokens.get(1);
			this.internal_name = tokens.get(2);
			this.direction = tokens.get(3);
		}
	}

	public static final class pl_Element {
		public String name;
		public int conn_count;
		public boolean isBel;

		public void set(List<String> tokens) {
			this.name = tokens.get(1);
			this.conn_count = Integer.parseInt(tokens.get(2));

			isBel = tokens.size() >= 5 && tokens.get(3).equals("#") && tokens.get(4).equals("BEL");
		}
	}

	public static final class pl_ElementPin {
		public String name;
		public String direction;

		public void set(List<String> tokens) {
			this.name = tokens.get(1);
			this.direction = tokens.get(2);
		}
	}

	public static final class pl_ElementConn {
		public String element0;
		public String pin0;
		public String direction;
		public String element1;
		public String pin1;

		public void set(List<String> tokens) {
			this.element0 = tokens.get(1);
			this.pin0 = tokens.get(2);
			this.direction = tokens.get(3);
			this.element1 = tokens.get(4);
			this.pin1 = tokens.get(5);
		}
	}

	public static final class pl_ElementCfg {
		public List<String> cfgs;

		public void set(List<String> tokens) {
			this.cfgs = tokens.subList(1, tokens.size());
		}
	}

	public static final class pl_Summary {
		public List<String> stats;

		public void set(List<String> tokens) {
			stats = tokens.subList(1, tokens.size());
		}
	}
}


