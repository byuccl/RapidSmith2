/*
 * Copyright (c) 2010-2011 Brigham Young University
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
/**
 * 
 */
package edu.byu.ece.rapidSmith.device.vsrt.gui.shapes;

import com.trolltech.qt.core.QRectF;
import com.trolltech.qt.core.Qt.CursorShape;
import com.trolltech.qt.core.Qt.PenStyle;
import com.trolltech.qt.gui.QGraphicsItem;
import com.trolltech.qt.gui.QGraphicsItemInterface;
import com.trolltech.qt.gui.QGraphicsSceneHoverEvent;
import com.trolltech.qt.gui.QGraphicsSceneMouseEvent;
import com.trolltech.qt.gui.QPainter;
import com.trolltech.qt.gui.QStyleOptionGraphicsItem;
import com.trolltech.qt.gui.QWidget;

import edu.byu.ece.rapidSmith.device.vsrt.gui.PrimitiveSiteView;
import edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands.ResizeSiteCommand;

/**
 * This class creates an edge box used to resize the primitive site horizontally <br>
 * and vertically.  It can be extending to bels and site pip if necessary in the future
 * @author Thomas Townsend
 * Created on: Aug 2, 2014
 */
public class ResizeBox extends QGraphicsItem{

	/**Shape of the box*/
	private QRectF shape; 
	/**The site to be resized*/
	private Site siteShape;
	/**The type of resize box (TOP, BOTTOM, LEFT, RIGHT, ETC)*/
	private ResizeBoxType edgeType;
	/**Grid Size*/
	private double pin_width; 
	/**True is the item has been clicked*/
	private boolean itemPressed; 
	/**Cursor shape that should appear when a mouse over event occurs*/
	private CursorShape curShape;
	/***/
	private ResizeSiteCommand resize;
	
	/**
	 * Constructor: 
	 * @param cursor
	 * @param site
	 * @param pin_width
	 * @param edgeType
	 */
	public ResizeBox(ResizeBoxType edgeType, Site site, double pin_width){
		
		this.siteShape = site; 
		this.edgeType = edgeType;
		this.pin_width = pin_width;
		this.setCursorShape();
		this.updateBoxPos();
		this.setFlag(GraphicsItemFlag.ItemIsMovable, false);
		this.setFlag(GraphicsItemFlag.ItemIsSelectable, false);
		this.setAcceptsHoverEvents(true);
	}
	
	/**
	 * Sets the shape of the cursor on mouse hover events based on the type of resize box
	 */
	private void setCursorShape(){
		switch(this.edgeType){
		case TOP :	curShape = CursorShape.SizeVerCursor;
			break;
		case BOTTOM : curShape = CursorShape.SizeVerCursor; 
			break;
		case LEFT : curShape = CursorShape.SizeHorCursor;
			break;
		case RIGHT : curShape = CursorShape.SizeHorCursor; 
			break;
		case TOP_LEFT_CORNER : curShape = CursorShape.SizeFDiagCursor;
			break;
		case TOP_RIGHT_CORNER : curShape = CursorShape.SizeBDiagCursor; 
			break;
		case BOTTOM_RIGHT_CORNER : curShape = CursorShape.SizeFDiagCursor;
			break;
		case BOTTOM_LEFT_CORNER : curShape = CursorShape.SizeBDiagCursor; 
			break;
		default:
			break;
		}
	}
	/**
	 * Based on the type of resize box, this method sets the shape and position of the box <br>
	 * in the proper location
	 */
	public void updateBoxPos(){
		switch(this.edgeType){
		case TOP :
			shape = new QRectF(0,0, siteShape.width()-pin_width, pin_width);
			this.setPos(siteShape.pos().x() + pin_width/2, siteShape.pos().y() -pin_width/2);
			break;
		case BOTTOM : 
			shape = new QRectF(0,0, siteShape.width()-pin_width, pin_width);
			this.setPos(siteShape.pos().x() + pin_width/2, siteShape.pos().y() + siteShape.height() -pin_width/2);
			break;
		case LEFT : 
			shape = new QRectF(0,0, pin_width, siteShape.height() - pin_width);
			this.setPos(siteShape.pos().x() - pin_width/2, siteShape.pos().y()+ pin_width/2);
			break;
		case RIGHT : 
			shape = new QRectF(0,0, pin_width, siteShape.height() - pin_width);
			this.setPos(siteShape.pos().x() + siteShape.width() - pin_width/2, siteShape.pos().y()+ pin_width/2);
			break;
		case TOP_LEFT_CORNER :
			this.shape = new QRectF(0,0,pin_width,pin_width);
			this.setPos(siteShape.pos().x() -pin_width/2, siteShape.pos().y() -pin_width/2);
			break;
		case TOP_RIGHT_CORNER : 
			this.shape = new QRectF(0,0,pin_width,pin_width);
			this.setPos(siteShape.pos().x() + siteShape.width()- pin_width/2, siteShape.pos().y() - pin_width/2);
			break;
		case BOTTOM_RIGHT_CORNER : 
			this.shape = new QRectF(0,0,pin_width,pin_width);
			this.setPos(siteShape.pos().x() + siteShape.width() - pin_width/2, siteShape.pos().y() + siteShape.height() - pin_width/2);
			break;
		case BOTTOM_LEFT_CORNER : 
			this.shape = new QRectF(0,0,pin_width,pin_width);
			this.setPos(siteShape.pos().x() -pin_width/2, siteShape.pos().y() + siteShape.height()-pin_width/2);
			break;
		default:
			break;
		}	
	}
	
	/**
	 * Sets the item pressed flag, and de-selects all items in the scene in preparation <br>
	 * for site resizing. 
	 */
	@Override
	public void mousePressEvent(QGraphicsSceneMouseEvent event){
		if ( siteShape.canResize() ) {
			itemPressed = true;
			for (QGraphicsItemInterface item : this.scene().selectedItems()) 
				item.setSelected(false);
			
			this.resize = new ResizeSiteCommand(siteShape);
			
		}
	}
	
	/**
	 * Snaps the site (and its pins) to the grid, and updates the positions of each resize box 
	 * of the site.  
	 */
	@Override
	public void mouseReleaseEvent(QGraphicsSceneMouseEvent event){
		if (itemPressed && siteShape.canResize()){
			itemPressed = false;
			this.resize.setNewSiteInformation(siteShape);
			siteShape.pushSiteResizeCommand(resize);
			
		}
	}
	
	/**
	 * Resizes the site based on the corner this box is in, and the direction <br>
	 * of the mouse movement. 
	 */
	@Override
	public void mouseMoveEvent(QGraphicsSceneMouseEvent event){
		if ( itemPressed ){
			double xDiff = event.scenePos().x() - event.lastScenePos().x();
			double yDiff = event.scenePos().y() - event.lastScenePos().y();
			
			switch(this.edgeType){
			case TOP :
				if (yDiff < 0 || siteShape.height() > 2*this.pin_width ) {
					this.setPos(this.pos().x(), this.pos().y() + yDiff );
					this.siteShape.changeHeight(-yDiff);
					this.siteShape.changePos(0, yDiff);
				}
				break;
			case BOTTOM :
				if (yDiff > 0 || siteShape.height() > 2*this.pin_width ) {
					this.setPos(this.pos().x(), this.pos().y() + yDiff );
					this.siteShape.changeHeight(yDiff);
				}
				break;
			case LEFT : 
				if (xDiff < 0 || siteShape.width() > 2*this.pin_width ) {
					this.setPos(this.pos().x() + xDiff, this.pos().y() );
					this.siteShape.changePos(xDiff, 0);
					this.siteShape.changeWidth(-xDiff);
					this.siteShape.updatePinPositions(xDiff, false);
				}
				break;
			case RIGHT :
				if (xDiff > 0 || siteShape.width() > 2*this.pin_width ) {
					this.setPos(this.pos().x() + xDiff, this.pos().y() );
					this.siteShape.changeWidth(xDiff);
					this.siteShape.updatePinPositions(xDiff, true);	
				}

				break;
			case TOP_LEFT_CORNER :
				if (xDiff < 0 || siteShape.width() > 2*this.pin_width && siteShape.height() > 2*this.pin_width ) {
					this.siteShape.changePos(xDiff, xDiff);
					this.siteShape.updatePinPositions(xDiff, false);
					this.siteShape.changeWidth(-xDiff);
					this.siteShape.changeHeight(-xDiff);
				}
			break;
			case TOP_RIGHT_CORNER : 
				if (yDiff < 0 || siteShape.width() > 2*this.pin_width && siteShape.height() > 2*this.pin_width ) {
					this.siteShape.changePos(0, yDiff);
					this.siteShape.updatePinPositions(-yDiff, true);
					this.siteShape.changeWidth(-yDiff);
					this.siteShape.changeHeight(-yDiff);
				}
				break;
			case BOTTOM_RIGHT_CORNER : 
				if (xDiff > 0 || siteShape.width() > 2*this.pin_width && siteShape.height() > 2*this.pin_width ) {
					this.siteShape.updatePinPositions(xDiff, true);
					this.siteShape.changeHeight( xDiff );
					this.siteShape.changeWidth ( xDiff );
				}
				break;
			case BOTTOM_LEFT_CORNER: 
				if (xDiff < 0 || siteShape.width() > 2*this.pin_width && siteShape.height() > 2*this.pin_width ) {
					this.siteShape.changePos(xDiff, 0);
					this.siteShape.updatePinPositions(xDiff, false);
					this.siteShape.changeWidth(-xDiff);
					this.siteShape.changeHeight(-xDiff);
				}
				break;
			default:
				break;
			}
			siteShape.scene().update();	
		}
	}
	
	/**
	 * Changes the view cursor to the appropriate shape when the mouse is above it
	 */
	@Override
	public void hoverEnterEvent (QGraphicsSceneHoverEvent event){
		if ( siteShape.canResize() )
			((PrimitiveSiteView)this.scene().views().get(0)).setCursorShape(curShape, false);
	}
	/**
	 * Changes the view cursor back to an arrow once the mouse leaves the box
	 */
	@Override
	public void hoverLeaveEvent (QGraphicsSceneHoverEvent event){
		if ( siteShape.canResize() )
			((PrimitiveSiteView)this.scene().views().get(0)).setCursorShape(CursorShape.ArrowCursor, false);
	}
	
	/**
	 * Returns the shape of the resize box
	 */
	@Override
	public QRectF boundingRect() {
		// TODO Auto-generated method stub
		return shape;
	}

	/**
	 * Paints the resize box without an outline 
	 */
	@Override
	public void paint(QPainter painter, QStyleOptionGraphicsItem option, QWidget widget) {
		// TODO Auto-generated method stub
		painter.setPen(PenStyle.NoPen);
		painter.drawRect(shape);
	}
}