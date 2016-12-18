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
package edu.byu.ece.rapidSmith.examples;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.QRectF;
import com.trolltech.qt.core.QSize;
import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QGraphicsPixmapItem;
import com.trolltech.qt.gui.QGraphicsRectItem;
import com.trolltech.qt.gui.QGraphicsScene;
import com.trolltech.qt.gui.QGraphicsSceneMouseEvent;
import com.trolltech.qt.gui.QImage;
import com.trolltech.qt.gui.QPainter;
import com.trolltech.qt.gui.QPen;
import com.trolltech.qt.gui.QPixmap;
import com.trolltech.qt.gui.QImage.Format;

import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Tile;

/**
 * @author marc
 * 
 */
public class PartTileBrowserScene extends QGraphicsScene {
	private double currX, currY, prevX, prevY;
	private int tileSize, numCols, numRows;
	private double lineWidth;
	private Device device;
	Signal0 updateStatus = new Signal0();
	private QGraphicsRectItem highlit;
	private QImage qImage;

	PartTileBrowserScene(Device device) {
		this.device = device;
		this.highlit = null;
		this.prevX = 0;
		this.prevY = 0;
		this.tileSize = 20;
		this.lineWidth = 1;
		if (device != null) {
			this.numRows = device.getRows();
			this.numCols = device.getColumns();
		} else {
			this.numRows = 8;
			this.numCols = 8;
		}
		setSceneRect(new QRectF(0, 0, (numCols + 1) * (tileSize + 1),
				(numRows + 1) * (tileSize + 1)));
		drawSliceBackground();
	}

	public void setDevice(Device newDevice) {
		this.device = newDevice;
		this.highlit = null;
		this.prevX = 0;
		this.prevY = 0;
		if (device != null) {
			this.numRows = device.getRows();
			this.numCols = device.getColumns();
		} else {
			this.numRows = 8;
			this.numCols = 8;
		}
		this.clear();
		setSceneRect(new QRectF(0, 0, (numCols + 1) * (tileSize + 1),
				(numRows + 1) * (tileSize + 1)));
		drawSliceBackground();
	}

	private void drawSliceBackground() {

		setBackgroundBrush(new QBrush(QColor.black));
		//Create transparent QPixmap that accepts hovers 
		//  so that moveMouseEvent is triggered
		QPixmap qpm = new QPixmap(new QSize((numCols + 1) * (tileSize + 1),
				(numRows + 1) * (tileSize + 1)));
		qpm.fill(new QColor(255, 255,255, 0));
		QGraphicsPixmapItem background = addPixmap(qpm);
		background.setAcceptsHoverEvents(true);
		background.setZValue(-1);
		// Draw colored tiles onto QImage		
		qImage = new QImage(new QSize((numCols + 1) * (tileSize + 1),
				(numRows + 1) * (tileSize + 1)), Format.Format_RGB16);
		QPainter painter = new QPainter(qImage);

		painter.setPen(new QPen(QColor.black, lineWidth));
		// Draw lines between tiles
		for (int i = 0; i <= numCols; i++) {
			painter.drawLine((i) * tileSize, tileSize, (i) * tileSize,
					(numRows) * tileSize);
		}

		for (int j = 0; j <= numRows; j++) {
			painter.drawLine(tileSize, (j) * tileSize, (numCols) * tileSize,
					(j) * tileSize);
		}

		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numCols; j++) {
				Tile tile = device.getTile(i, j);
				String name = tile.getName();
				int hash = name.hashCode();
				int idx = name.indexOf("_");
				if (idx != -1) {
					hash = name.substring(0, idx).hashCode();
				}
				QColor color = QColor.fromRgb(hash);

				if (name.startsWith("DSP")) {
					// color = QColor.fromRgb(145, 145, 145);
					color = QColor.darkCyan;
				} else if (name.startsWith("BRAM")) {
					// color = QColor.fromRgb(165, 165, 165);
					color = QColor.darkMagenta;
				} else if (name.startsWith("INT")) {
					// color = QColor.fromRgb(125, 125, 125);
					color = QColor.darkYellow;
				} else if (name.startsWith("CLB")) {
					color = QColor.blue;
					// color = QColor.fromRgb(185, 185, 185);
				} else if (name.startsWith("DCM")) {
					// color = QColor.fromRgb(205, 205, 205);
				} else if (name.startsWith("EMPTY")) {
					// color = QColor.white;
				} else {
					// color = QColor.black;
				}

				painter.fillRect(j * tileSize, i * tileSize, tileSize - 2, tileSize - 2, new QBrush(color));
			}
		}

		painter.end();
		
	}
	
	public void drawBackground(QPainter painter, QRectF rect){
		super.drawBackground(painter, rect);
		painter.drawImage(0, 0, qImage);
	}

	@Override
	public void mouseMoveEvent(QGraphicsSceneMouseEvent event) {
		QPointF mousePos = event.scenePos();
		currX = Math.floor((mousePos.x()) / tileSize);
		currY = Math.floor((mousePos.y()) / tileSize);
		if (currX >= 0 && currY >= 0 && currX < numCols && currY < numRows
				&& (currX != prevX || currY != prevY)) {
			this.updateStatus.emit();
			updateCursor();
			prevX = currX;
			prevY = currY;
		}

		super.mouseMoveEvent(event);
	}

	private void updateCursor() {
		if (highlit == null) {
			QPen cursorPen = new QPen(QColor.yellow, 3);
			highlit = addRect(currX * tileSize, currY * tileSize, tileSize - 2,
					tileSize - 2, cursorPen);
		} else {
			highlit.moveBy((currX - prevX) * tileSize, (currY - prevY)
					* tileSize);
		}
	}

	double getCurrX() {
		return currX;
	}

	double getCurrY() {
		return currY;
	}

}
