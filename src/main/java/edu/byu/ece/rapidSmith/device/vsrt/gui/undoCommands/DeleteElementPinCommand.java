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

import com.trolltech.qt.core.Qt.SortOrder;
import com.trolltech.qt.gui.QGraphicsItemInterface;
import com.trolltech.qt.gui.QUndoCommand;

import edu.byu.ece.rapidSmith.device.vsrt.gui.PrimitiveSiteScene;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreeElement;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreePin;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.ElementShape;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Pip;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Wire;

/**
 * This class allows the user to delete an element pin, and undo the deletion if necessary.
 * @author Thomas Townsend 
 * Created on: Jul 31, 2014
 */
public class DeleteElementPinCommand extends QUndoCommand{

	/**Element in the tree view*/
	private QTreeElement element;
	/**Pin to be deleted in the tree view*/
	private QTreePin pin;
	/**Graphics Element*/
	private ElementShape shape;
	/**Wires connected to the element pin to be deleted*/
	private ArrayList<Wire> wires = new ArrayList<Wire>();
	/**Scene where the graphics element resides*/
	private PrimitiveSiteScene scene;
	/**Used to insert the deleted pin into its previous location*/
	private int listIndex;
	
	/**
	 * Constructor: Initializes all variables that are necessary to perform/undo this command
	 * @param element 
	 * @param pin
	 * @param scene
	 */
	public DeleteElementPinCommand(QTreeElement element, QTreePin pin,  PrimitiveSiteScene scene){
		this.element = element;
		this.pin = pin;
		this.scene = scene;
		for (Wire wire : pin.get_wires()) {
			wires.add(wire);
		} 
		//finding the QGraphics item that represents element
		for (QGraphicsItemInterface item : scene.items()) {
			if (item instanceof ElementShape){
				if ( ((ElementShape)item).getTreeElement() == element ) {
					shape = (ElementShape)item; 
					break;
				}
			}
		}
		this.listIndex = element.getPinIndex(pin);
		this.setText("deleting " + pin.get_pinName() + " of " + element.getElement().getName());
	}
	
	/**
	 * Deletes the specified pin from the specified element
	 */
	@Override 
	public void redo(){
		//remove the pin from the element data structures
		this.element.removePin(pin);
		//removing any connections from the pin
		this.pin.remove_allWires();
		//remove pin from the tree view
		this.element.takeChild( this.element.indexOfChild(pin) );
		//removing the pin from the graphics scene
		this.scene.remove_pin(pin.getLastLocation());
		
		if (shape instanceof Pip)
			((Pip) shape).resizePip();
		else
			this.shape.calculateFontSize();
		
		this.shape.updatePinPosition();
		this.scene.update();
	}
	
	/**
	 * Adds the specified pin back to the element from where is was deleted
	 */
	@Override 
	public void undo(){
		//add the pin back into the same place it was previously located
		this.element.addPin(listIndex, pin);
		for (Wire wire : wires) {
			wire.addWireToScene(scene);
			wire.undoRemoveWire();
		}
		this.element.addChild(pin);
		this.element.sortChildren(0, SortOrder.AscendingOrder);
		
		if (shape instanceof Pip)
			((Pip) shape).resizePip();
		else
			this.shape.calculateFontSize();
		
		this.shape.updatePinPosition();
		this.scene.update();
	}
}
