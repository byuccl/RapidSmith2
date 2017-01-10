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

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.gui.QGraphicsItem;
import com.trolltech.qt.gui.QUndoCommand;

import edu.byu.ece.rapidSmith.device.vsrt.gui.PrimitiveSiteScene;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.ElementShape;

/**
 * This command allows users to add an element to the graphics scene, and undo the action if necessary.
 * @author Thomas Townsend
 * Created on: Jul 29, 2014
 */
public class AddElementCommand extends QUndoCommand {
	/**Item added to the graphics scene*/
	private QGraphicsItem item;
	/**Graphics Scene where the item is added*/
	private PrimitiveSiteScene scene; 
	/**Initial Position of the added item*/
	private QPointF initialPos;
	
	/***
	 * Constructor
	 * @param scene Graphics scene to which the item is added
	 * @param item Graphics item to be added
	 * @param initPos Initial Position of the graphics item 
	 */
	public AddElementCommand(PrimitiveSiteScene scene, QGraphicsItem item, QPointF initPos) {
		this.scene = scene;
		this.item = item; 
		this.initialPos = initPos;
		this.setText("adding " + ((ElementShape)item).getTreeElement().getElement().getName());
	}

	/**
	 * Adds the item to the graphics scene
	 */
	@Override
	public void redo() {
		this.scene.addItem(item);
		this.item.setPos(initialPos);
		this.scene.update();
		
		((ElementShape)item).getTreeElement().setIsPlaced(true);
		
	}
	
	/**
	 * Removes the item from the graphics scene. 
	 */
	@Override 
	public void undo(){
		this.scene.removeItem(item);
		this.scene.update();
		((ElementShape)item).getTreeElement().setIsPlaced(false);
	}
}
