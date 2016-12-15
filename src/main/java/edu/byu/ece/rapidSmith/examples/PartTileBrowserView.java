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

import com.trolltech.qt.core.QPoint;
import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.CursorShape;
import com.trolltech.qt.core.Qt.Key;
import com.trolltech.qt.gui.QCursor;
import com.trolltech.qt.gui.QGraphicsScene;
import com.trolltech.qt.gui.QGraphicsView;
import com.trolltech.qt.gui.QKeyEvent;
import com.trolltech.qt.gui.QMouseEvent;
import com.trolltech.qt.gui.QWheelEvent;

/**
 * @author marc
 *
 */
public class PartTileBrowserView extends QGraphicsView {

	private QPoint lastPan;
	private boolean rightPressed;
	private double zoomMin;
	private double zoomMax;
	private static double scaleFactor = 1.15;//how fast we zoom

	PartTileBrowserView(QGraphicsScene scene) {
		super(scene);
		zoomMin = 0.05;
		zoomMax = 30;
	}

	

	

	public void mousePressEvent(QMouseEvent event) {
		if (event.button().equals(Qt.MouseButton.RightButton)) {
			// For panning the view
			rightPressed = true;
			lastPan = event.pos();
			setCursor(new QCursor(CursorShape.ClosedHandCursor));
		}
		super.mousePressEvent(event);
	}

	public void mouseReleaseEvent(QMouseEvent event) {
		if (event.button().equals(Qt.MouseButton.RightButton)) {
			rightPressed = false;
			setCursor(new QCursor(CursorShape.ArrowCursor));
		}
		super.mouseReleaseEvent(event);
	}

	public void mouseMoveEvent(QMouseEvent event) {
		if (rightPressed) {
			if (lastPan != null && !lastPan.isNull()) {
				// Get how much we panned
				QPointF s1 = mapToScene(new QPoint(lastPan.x(),
						lastPan.y()));
				QPointF s2 = mapToScene(new QPoint(event.pos().x(),
						event.pos().y()));
				QPointF delta = new QPointF(s1.x() - s2.x(), s1.y() - s2.y());
				lastPan = event.pos();
				// Scroll the scrollbars ie. do the pan
				double zoom = this.matrix().m11();
				this.horizontalScrollBar().setValue((int) (this.horizontalScrollBar().value()+zoom*delta.x()));
				this.verticalScrollBar().setValue((int) (this.verticalScrollBar().value()+zoom*delta.y()));
			}
		}
		super.mouseMoveEvent(event);
	}

	public void wheelEvent(QWheelEvent event) {
		// Get the position of the mouse before scaling, in scene coords
		QPointF pointBeforeScale = mapToScene(event.pos());

		// Scale the view ie. do the zoom
		double zoom = this.matrix().m11();
		if (event.delta() > 0) {
			// Zoom in (if not at limit)
			if(zoom < zoomMax)
				scale(scaleFactor, scaleFactor);
		} else {
			// Zoom out (if not at limit)
			if(zoom > zoomMin)
				scale(1.0 / scaleFactor, 1.0 / scaleFactor);
		}

		// Get the position after scaling, in scene coords
		QPointF pointAfterScale = mapToScene(event.pos());

		// Get the offset of how the screen moved
		QPointF offset = new QPointF(
				pointBeforeScale.x() - pointAfterScale.x(), pointBeforeScale
						.y()
						- pointAfterScale.y());
		this.horizontalScrollBar().setValue((int) (this.horizontalScrollBar().value()+zoom*offset.x()));
		this.verticalScrollBar().setValue((int) (this.verticalScrollBar().value()+zoom*offset.y()));
	}

	
	public void keyPressEvent(QKeyEvent event){
		double scaleFactor = 1.15; 
		if (event.key() == Key.Key_Equal.value()) {
			// Zoom in (if not at limit)
			if(this.matrix().m11() < zoomMax)
				scale(scaleFactor, scaleFactor);
		} else if(event.key() == Key.Key_Minus.value()){
			// Zoom out (if not at limit)
			if(this.matrix().m11() > zoomMin)
				scale(1.0 / scaleFactor, 1.0 / scaleFactor);
		}		
	}
}

