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
package edu.byu.ece.rapidSmith.device.browser;

import com.trolltech.qt.core.Qt.PenStyle;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QGraphicsLineItem;
import com.trolltech.qt.gui.QGraphicsSceneHoverEvent;
import com.trolltech.qt.gui.QGraphicsSceneMouseEvent;
import com.trolltech.qt.gui.QPen;

import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.gui.TileScene;

/**
 * This class is used with the DeviceBrowser to draw wire connections
 * on the array of tiles.
 * @author Chris Lavin
 * Created on: Nov 26, 2010
 */
public class WireConnectionLine extends QGraphicsLineItem{
	/** Keeps a red pen handy for highlighting wire connections on mouse over */
	private static QPen highlighted  = new QPen(QColor.red, 0.25, PenStyle.SolidLine);
	/** Keeps a yellow pen for drawing the wire connections */
	private static QPen unHighlighted = new QPen(QColor.yellow, 0.25, PenStyle.SolidLine);
	/** The current DeviceBrowser scene */
	private TileScene scene;
	/** The current tile */
	private Tile tile;
	/** The current wire */
	private int wire;

	/**
	 * Creates a new wire connection line.
	 * @param x1 Starting X coordinate.
	 * @param y1 Starting Y coordinate.
	 * @param x2 Ending X coordinate.
	 * @param y2 Ending Y coordinate.
	 * @param scene The DeviceBrowser scene.
	 * @param tile The tile.
	 * @param wire The wire.
	 */
	WireConnectionLine(double x1, double y1, double x2, double y2,
	                   TileScene scene, Tile tile, int wire){
		super(x1, y1, x2, y2);
		this.scene = scene;
		this.tile = tile;
		this.wire = wire;
		highlighted = new QPen(QColor.red, 0.25, PenStyle.SolidLine);
	}

	@Override
	public void hoverEnterEvent(QGraphicsSceneHoverEvent event){
		this.setPen(highlighted);
	}

	@Override
	public void hoverLeaveEvent(QGraphicsSceneHoverEvent event){
		this.setPen(unHighlighted);
	}

	@Override
	public void mousePressEvent(QGraphicsSceneMouseEvent event){
		if(scene.getClass().equals(DeviceBrowserScene.class)){
			((DeviceBrowserScene)scene).drawConnectingWires(tile, wire);
		}
	}

	/**
	 * @return the scene
	 */
	public TileScene getScene() {
		return scene;
	}

	/**
	 * @param scene the scene to set
	 */
	public void setScene(TileScene scene) {
		this.scene = scene;
	}
}
