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
package edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands;

import java.util.ArrayList;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.Qt.MouseButton;
import com.trolltech.qt.gui.QGraphicsItemInterface;
import com.trolltech.qt.gui.QGraphicsSceneMouseEvent;
import com.trolltech.qt.gui.QUndoCommand;

import edu.byu.ece.rapidSmith.device.vsrt.gui.PrimitiveSiteScene;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.ElementShape;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.PinShape;

/**
 * This class allows for the user to undo and redo moving graphics objects. 
 * @author Thomas Townsend
 * Created on: Jul 29, 2014
 */
public class MoveCommand extends QUndoCommand {
	
	/**List of items that have been moved*/
	private ArrayList<QGraphicsItemInterface> movedItems = new ArrayList<QGraphicsItemInterface>();
	/**Parallel Array of the old positions for each item in the movedItems array*/
	private ArrayList<QPointF> oldPositions = new ArrayList<QPointF>();
	/**Parallel Array of the new positions for each item in the movedItems array*/
	private ArrayList <QPointF> newPositions = new ArrayList<QPointF>();
	/**Scene the items are in*/
	private PrimitiveSiteScene scene;
	/**Square size of the grid drawn on the scene*/
	private double pin_width;
	/**True if this command should be pushed on the stack.  (Items have actually moved)*/
	private boolean pushCommand = true;
	
	/**
	 * Constructor
	 * @param scene QGraphicsScene that these items are apart of 
	 * @param items A list of the currently selected items in the graphics scene 
	 */
	public MoveCommand (PrimitiveSiteScene scene, ArrayList<QGraphicsItemInterface> items){
		this.scene = scene;
		pin_width = scene.getSquare_size();
		String name = null;
		int i = 0;
		//only need to worry about the elements that can be moved (we can ignore wires and the overall site)
		for (QGraphicsItemInterface item : items) {
			if (item instanceof ElementShape) {
				name = ((ElementShape) item).getTreeElement().getElement().getName();
				movedItems.add(item); 
				oldPositions.add(((ElementShape) item).getLastPos()); 
				newPositions.add(item.pos());
			}
			else if( item instanceof PinShape) {
				name = ((PinShape) item).getTreePin().get_pinName();
				movedItems.add(item); 
				oldPositions.add( ((PinShape)item).getLastLocation()); 
				newPositions.add(item.pos());
			}
			if (i == 0) {
				checkItemsInSameLocation(item);
			}
			i++;
		}
		if (items.size() == 1)
			this.setText("moving " + name);
		else {
			this.setText("moving selected items");
		}
	}
	
	/**
	 * Moves the items to the position specified by the command. 
	 */
	@Override 
	public void redo(){
		//First, move all items to their new locations (to avoid collisions)
		for (int i = 0; i < this.movedItems.size(); i++) 
			this.movedItems.get(i).setPos( this.newPositions.get(i) );
		
		//Then, update their pin positions
		for (int i = 0; i < this.movedItems.size(); i++)  {
			QGraphicsSceneMouseEvent prevMouseEvent = new QGraphicsSceneMouseEvent();
			prevMouseEvent.setButton( MouseButton.LeftButton );
			prevMouseEvent.setPos( this.newPositions.get(i) );
			//update the pin positions
			this.movedItems.get(i).mouseReleaseEvent(prevMouseEvent);
		}
		
		//select each item again...mouse release event de-selects all but one 
		for (QGraphicsItemInterface item : movedItems) {
			item.setSelected(true);
		}
		this.scene.update();
	}
	
	/**
	 * Moves the items to their original positions before this command was executed. 
	 */
	@Override 
	public void undo(){
		//First, move all items back to their previous positions (to avoid collisions) 
		for (int i = 0; i < this.movedItems.size() ; i++)
			this.movedItems.get(i).setPos( this.oldPositions.get(i) );
		
		//Then, update their pin positions
		for (int i = 0; i < this.movedItems.size() ; i++){
			QGraphicsSceneMouseEvent prevMouseEvent = new QGraphicsSceneMouseEvent();
			prevMouseEvent.setButton( MouseButton.LeftButton );
			prevMouseEvent.setPos( this.oldPositions.get(i) );
			this.movedItems.get(i).mouseReleaseEvent(prevMouseEvent);
		}
		
		//reselect all items...mouseReleaseEvent deselects all but one
		for (QGraphicsItemInterface item : movedItems) 
			item.setSelected(true);
		
		this.scene.update(); 
	}
	
	/**
	 * Calculates the next position of the item on the grid, and if <br>
	 * the item is in the same location as it previously was, then the <br>
	 * command will not be pushed on the stack.
	 * @param item
	 * @return
	 */
	private boolean checkItemsInSameLocation(QGraphicsItemInterface item){
		double remX = item.pos().x() % pin_width;
		double remY = item.pos().y() % pin_width;	
		double offsetX = (remX < pin_width/2) ? remX : -(pin_width - remX) ;
		double offsetY = (remY < pin_width/2) ? remY : -(pin_width - remY) ;
		
		QPointF test = new QPointF(item.pos().x() - offsetX, item.pos().y() - offsetY);
		
		if (item instanceof ElementShape){
			if(((ElementShape) item).getLastPos().equals(test) )
				pushCommand = false;
		}
		else if (item instanceof PinShape ){
			if(((PinShape) item).getLastLocation().equals(test))
				pushCommand = false;
		}
		return !pushCommand;
	}
	
	/**
	 * Returns true if this command should be pushed onto the stack
	 * @return
	 */
	public boolean shouldPushMove(){
		return pushCommand;
	}
}
