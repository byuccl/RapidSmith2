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

import com.trolltech.qt.gui.QUndoCommand;

import edu.byu.ece.rapidSmith.device.vsrt.gui.ElementTab;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreeElement;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.Element;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.PrimitiveDef;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.PrimitiveDefPin;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.PrimitiveDefPinDirection;

/**
 * Adds a VCC or GND Bel to the primitive site. 
 *
 */
public class AddBelCommand extends QUndoCommand {

	/**Data structure where the pin will be added*/
	private PrimitiveDef primitiveDef; 
	/**QWidget that has access to the tree view*/
	private ElementTab elementTab;
	
	QTreeElement belTreeElement;
	
	public AddBelCommand(String name, boolean isVcc, ElementTab elementTab, PrimitiveDef def) {
		// Create the new primitive def element
		Element belElement = new Element();
		belElement.setBel(true);
		belElement.setName(name);
	
		// create the new primitive def pin for the element above
		PrimitiveDefPin outputPin = new PrimitiveDefPin();
		outputPin.setDirection(PrimitiveDefPinDirection.OUTPUT);
		String pinName = isVcc ? "P" : "G";
		outputPin.setInternalName(pinName);
		
		belElement.addPin(outputPin);
		
		this.belTreeElement = elementTab.createNewBel(belElement);
		this.elementTab = elementTab;
		this.primitiveDef = def;
	}
	
	/**
	 * Adds the newly created Bel to the PrimitiveDef data structure and Tree View
	 */
	@Override 
	public void redo(){
		elementTab.addBelToTree(belTreeElement);
		primitiveDef.addElement(belTreeElement.getElement());
	}
	
	/**
	 * Removes the Bel from the PrimitiveDef data structure and Tree View
	 */
	@Override 
	public void undo(){
		elementTab.removeBelFromTree(belTreeElement);
		primitiveDef.getElements().remove(belTreeElement.getElement());
	}
}
