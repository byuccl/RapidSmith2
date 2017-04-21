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
package edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands;

import java.util.List;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QTreeWidgetItem;
import com.trolltech.qt.gui.QUndoCommand;

import edu.byu.ece.rapidSmith.device.vsrt.gui.PrimitiveSiteScene;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreePin;
import edu.byu.ece.rapidSmith.device.vsrt.gui.VsrtColor;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.PinShape;

/**
 * In Single BEL operating mode, this command can be used to
 * add site pins and BEL pins to the graphics scene. When a pin 
 * is added to the graphics scene, any connections to the pin 
 * that previously existed are removed. 
 */
public class AddPinToSceneCommand extends QUndoCommand {

	/**Item added to the graphics scene*/
	private PinShape pinShape;
	/**Graphics Scene where the item is added*/
	private PrimitiveSiteScene scene; 
	/**Initial Position of the added item*/
	private QPointF initialPos;
	/** Position that the {@link PrimitiveSiteScene} thinks the pin is located**/
	private QPointF relativePos;
	/** Connections in the QTreePin that were removed when the pin was added to the scene**/
	private List<QTreeWidgetItem> oldConnections;
	
	/***
	 * Creates a new AddPinToSceneCommand
	 * 
	 * @param scene Graphics scene to which the item is added
	 * @param pinShape Pin graphics item to be added to the scene
	 */
	public AddPinToSceneCommand(PrimitiveSiteScene scene, PinShape pinShape) {
		this.scene = scene;
		this.pinShape = pinShape; 
		
		// find the first valid location to place the site pin on the grid
		QPointF init = scene.getParent().getPlacementPosition();
		double pinHeight = pinShape.getHeight();
		double offsetX = init.x() % pinHeight;
		double offsetY = init.y() % pinHeight;
		double startX = (init.x() + pinHeight)-offsetX;
		double startY = (init.y() + pinHeight)-offsetY;
		this.initialPos = getPinLocation(startX, startY);
		this.relativePos = new QPointF(initialPos.x(), initialPos.y() + pinShape.getHeight());
		
		this.oldConnections = null;
		this.setText(String.format("adding %s pin %s", pinShape.isSitePin() ? "site" : "bel",  pinShape.getName()));
	}

	/**
	 * Adds the pin to the graphics scene and removes all previous tree connections
	 */
	@Override
	public void redo() {
		// add pin to the graphics scene
		scene.addItem(pinShape);
		pinShape.setPinPos(initialPos);
		scene.update();
		
		// update the tree view for the pin 
		QTreePin treePin = pinShape.getTreePin(); 
		treePin.setIsPlaced(true);
		oldConnections = treePin.takeChildren();
		treePin.setForeground(0, new QBrush(VsrtColor.red));
	}
	
	/**
	 * Removes the pin from the graphics scene and restores all previous tree connections. 
	 */
	@Override 
	public void undo() {
		// remove the pin from the graphics scene
		scene.removeItem(pinShape);
		scene.update();
		scene.remove_pin(relativePos);
		
		// restore the pin's tree view to its initial state (with connections)
		QTreePin treePin = pinShape.getTreePin();
		treePin.setIsPlaced(false);
		
		if (oldConnections.size() > 0) {
			treePin.addChildren(oldConnections);
			treePin.setForeground(0, new QBrush(VsrtColor.darkGreen));
		}
	}
	
	/**
	 * Finds the next valid placement location for a pin shape
	 * 
	 * @param xStart Initial X location
	 * @param yStart Initial Y location
	 * 
	 * @return {@link QPointF} location to place the pin
	 */
	private QPointF getPinLocation(double xStart, double yStart) {
		double pin_width = pinShape.getHeight();
		QPointF pinPos = new QPointF(xStart, yStart + pin_width);
		
		// look for valid location (i.e. where no pin is currently placed)
		while ( scene.isPinAtLocation(pinPos) ) {
			pinPos.setY(pinPos.y() + pin_width);
		}
		
		pinPos.setY(pinPos.y() - pin_width); 
		return pinPos;
	}
}
