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
import java.util.HashSet;
import java.util.Set;

import com.trolltech.qt.core.Qt.MouseButton;
import com.trolltech.qt.gui.QGraphicsItemInterface;
import com.trolltech.qt.gui.QGraphicsSceneMouseEvent;
import com.trolltech.qt.gui.QUndoCommand;
import com.trolltech.qt.gui.QGraphicsItem.GraphicsItemFlag;

import edu.byu.ece.rapidSmith.device.vsrt.gui.PrimitiveSiteScene;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreePin;
import edu.byu.ece.rapidSmith.device.vsrt.gui.VSRTool;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.ElementShape;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.PinShape;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Wire;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.WirePart;

/**
 * This class allows user to undo/redo removing a group of items from the scene
 * @author Thomas Townsend
 * Created on: Jul 29, 2014
 */
public class RemoveCommand extends QUndoCommand {
	/**Items removed from the scene */
	private ArrayList<QGraphicsItemInterface> items;
	/**Graphics scene where the items are removed from*/
	private PrimitiveSiteScene scene;

	/**
	 * Constructor: Initializes the command and adds wires that weren't selected, but are apart of <br>
	 * 				bels that were selected to the items list.  
	 * @param items Items to be deleted
	 * @param scene Graphics scene that the deleted items were apart of
	 */
	public RemoveCommand(ArrayList<QGraphicsItemInterface> items, PrimitiveSiteScene scene){
		this.items = items;
		int itemCount = 0;
		String itemName = null;
		//making sure all of the wires attached to bels are accounted for, so they can be re-added when necessary
		Set<QGraphicsItemInterface> unaccountedWires = new HashSet<QGraphicsItemInterface>();
		for (QGraphicsItemInterface item : items) {
			if ( item instanceof ElementShape ) {
				for (QTreePin pin : ((ElementShape) item).getTreeElement().get_allPins()) {
					for (Wire wire : pin.get_wires()) {
						if ( !items.contains( wire.getFirstWirePart()) && !unaccountedWires.contains(wire.getFirstWirePart()) ){
							unaccountedWires.add( wire.getFirstWirePart() ); 
						}
					}
				}
				itemCount++;
				itemName = ((ElementShape) item).getTreeElement().getElement().getName();
			}
		}
		//adding all wires that weren't in the original list, to that list
		items.addAll(unaccountedWires);
		this.scene = scene;
		if (itemCount == 1)
			setText("deleting " + itemName);
		else {
			setText("deleting selected items");
		}
	}
	
	/**
	 * Removes each of the items in the "items" list from the graphics scene
	 */
	@Override
	public void redo(){
		for( int i = items.size()-1; i >=0 ; i-- ) {
			if(items.get(i) instanceof ElementShape) {
				((ElementShape) items.get(i)).deleteElement();
			}
			else if (items.get(i) instanceof WirePart)
			{
				if ( items.get(i).scene() != null )
					((WirePart) items.get(i)).getParentWire().removeWire();
			}
			else if(items.get(i) instanceof PinShape && VSRTool.singleBelMode) {
				((PinShape)items.get(i)).deletePin();
			}
		}	
		this.scene.update();
	}

	/**
	 * Adds each of the previously removed items back into the graphics scene.
	 */
	@Override 
	public void undo(){
		
		//used to reinitialize graphics items flags to what they currently are, not what they were when they were deleted. 
		boolean itemsMovable = !scene.shouldDelete() && !scene.shouldDrawWire() && !scene.shouldZoomToView(); 
		
		for (QGraphicsItemInterface item : items) {
			//if ( item instanceof ElementShape || item instanceof WirePart ||  )
			//	this.scene.addItem(item);
		
			if (item instanceof ElementShape){
				this.scene.addItem(item);
				((ElementShape)item).getTreeElement().setIsPlaced(true);
				
				QGraphicsSceneMouseEvent mouseEvent = new QGraphicsSceneMouseEvent();
				mouseEvent.setPos(item.pos());
				mouseEvent.setButton(MouseButton.LeftButton);
				//used to update pin positions on the graphics view
				item.mouseReleaseEvent( mouseEvent );
				//making items movable/not movable based on the current conditions of the graphics scene 
				item.setFlag( GraphicsItemFlag.ItemIsMovable, itemsMovable ) ;
				item.setFlag( GraphicsItemFlag.ItemIsSelectable, itemsMovable );
			}
			else if (item instanceof WirePart ){
				this.scene.addItem(item);
				((WirePart) item).getParentWire().undoRemoveWire();
			}
			else if (item instanceof PinShape && VSRTool.singleBelMode) {
				this.scene.addItem(item);
				PinShape pinShape = (PinShape) item;
				pinShape.getTreePin().setIsPlaced(true);
				
				QGraphicsSceneMouseEvent mouseEvent = new QGraphicsSceneMouseEvent();
				mouseEvent.setPos(item.pos());
				mouseEvent.setButton(MouseButton.LeftButton);
				//used to update pin positions on the graphics view
				pinShape.mouseReleaseEvent( mouseEvent );
				//making items movable/not movable based on the current conditions of the graphics scene 
				pinShape.setFlag( GraphicsItemFlag.ItemIsMovable, itemsMovable ) ;
				pinShape.setFlag( GraphicsItemFlag.ItemIsSelectable, itemsMovable );
			}
		}
	}
}