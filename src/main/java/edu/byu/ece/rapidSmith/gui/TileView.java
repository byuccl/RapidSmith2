/*
 * Copyright (c) 2010 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.gui;

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
 * This class is written specifically for the DeviceBrowser class and provides
 * the Qt View.  It controls much of the interaction from the user.
 */
public class TileView extends QGraphicsView{
	/** Current center of this view */
	QPointF currCenter;
	/** Stores the last pan of the view */
	QPoint lastPan;
	/** A flag indicating if the right mouse button has been pressed */
	private boolean rightPressed;
	
	public boolean hasPanned;
	/** The maximum value to which we can zoom out */
	protected static double zoomMin = 0.05;
	/** The maximum value to which we can zoom in */
	protected static double zoomMax = 30;
	/** The rate at which we zoom */
	protected static double scaleFactor = 1.15;

	/**
	 * Constructor
	 * @param scene The DeviceBrowser scene
	 */
	public TileView(QGraphicsScene scene){
		super(scene);
	}

	/**
	 * This method is called when any mouse button is pressed.
	 * In this case, a right click will allow the user to pan
	 * the array of tiles.
	 */
	public void mousePressEvent(QMouseEvent event){
		if (event.button().equals(Qt.MouseButton.RightButton)){
			// For panning the view
			rightPressed = true;
			hasPanned = false;
			lastPan = event.pos();
			setCursor(new QCursor(CursorShape.ClosedHandCursor));
		}
		super.mousePressEvent(event);
	}

	/**
	 * This method is called when any mouse button is released.
	 * In this case, this will disallow the user to pan.
	 */
	public void mouseReleaseEvent(QMouseEvent event){
		if(event.button().equals(Qt.MouseButton.RightButton)){
			rightPressed = false;
			setCursor(new QCursor(CursorShape.ArrowCursor));
		}
		super.mouseReleaseEvent(event);
	}

	/**
	 * This method is called when the mouse moves in the window.
	 * This will reset the window based on the mouse panning.
	 */
	public void mouseMoveEvent(QMouseEvent event){
		if (rightPressed){
			if (lastPan != null && !lastPan.isNull()) {
				hasPanned = true;
				// Get how much we panned
				QPointF s1 = mapToScene(new QPoint((int) lastPan.x(),
						(int) lastPan.y()));
				QPointF s2 = mapToScene(new QPoint((int) event.pos().x(),
						(int) event.pos().y()));
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

	/**
	 * This method is called when the mouse wheel or scroll is used.
	 * In this case, it allows the user to zoom in and out of the 
	 * array of tiles. 
	 */
	public void wheelEvent(QWheelEvent event){
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

		// Consulted the following link:
		// http://stackoverflow.com/questions/19113532/qgraphicsview-zooming-in-and-out-under-mouse-position-using-mouse-wheel

		// Find where cursor was pointing before scale
		QPoint offset = mapFromScene(pointBeforeScale);

		// Fix scroll bars and thus cursor position
		offset.subtract(event.pos());
		this.horizontalScrollBar().setValue(offset.x() + this.horizontalScrollBar().value());
		this.verticalScrollBar().setValue(offset.y() + this.verticalScrollBar().value());
	}
	
	/**
	 * This method gets called when a key on the keyboard is pressed.  
	 * In this case, if the '=' key is pressed, it zooms in.  If the
	 * '-' key is pressed, it zooms out.
	 */
	public void keyPressEvent(QKeyEvent event){
		double scaleFactor = 1.15; 
		if (event.key() == Key.Key_Equal.value()){
			// Zoom in (if not at limit)
			if(this.matrix().m11() < zoomMax)
				scale(scaleFactor, scaleFactor);
		} else if(event.key() == Key.Key_Minus.value()){
			// Zoom out (if not at limit)
			if(this.matrix().m11() > zoomMin)
				scale(1.0 / scaleFactor, 1.0 / scaleFactor);
		}		
	}
	
	public void zoomIn(){ 
		// Zoom in (if not at limit)
		if(this.matrix().m11() < zoomMax)
			scale(scaleFactor, scaleFactor);
	}
	
	public void zoomOut(){
		// Zoom out (if not at limit)
		if(this.matrix().m11() > zoomMin)
			scale(1.0 / scaleFactor, 1.0 / scaleFactor);
	}
}

