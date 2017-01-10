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
import com.trolltech.qt.gui.QTreeWidgetItem;
import com.trolltech.qt.gui.QUndoCommand;

import edu.byu.ece.rapidSmith.device.vsrt.gui.ElementTab;
import edu.byu.ece.rapidSmith.device.vsrt.gui.PrimitiveSiteScene;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreePin;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.PinShape;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Site;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Wire;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.Element;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.PrimitiveDef;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.PrimitiveDefPin;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.PrimitiveDefPinDirection;

/**
 * This class allows users to delete a set of site pins, and undo the deletion if necessary
 * @author Thomas Townsend
 * Created on: Jul 30, 2014
 */
public class DeleteSitePinCommand extends QUndoCommand{

	/**Primitive Def Pins to be deleted*/
	private ArrayList<PrimitiveDefPin>  sitePins = new ArrayList<PrimitiveDefPin>();
	/**Parallel array of element pins to be deleted*/
	private ArrayList<PrimitiveDefPin> elementPins = new ArrayList<PrimitiveDefPin>();
	/**Parallel array of pin elements to be deleted*/
	private ArrayList<Element>  elements = new ArrayList<Element>();
	/**Parallel array of QTreePin objects to be deleted*/
	private ArrayList<QTreePin> treePins = new ArrayList<QTreePin>();
	/**Parallel array of site pin graphics objects to delete*/
	private ArrayList<PinShape> pinShapes = new ArrayList<PinShape>();
	/**Site object that the site pin is apart of*/
	private Site siteShape;
	/**Primitive Def Data structure to delete the pins from*/
	private PrimitiveDef currentSite; 
	/**QWidget that holds all of the QTreePin information*/
	private ElementTab elementTab;
	/**Parallel array of the last known pin positions for each site pin being deleted*/
	private ArrayList<QPointF> lastPinPos = new ArrayList<QPointF>();
	/**Parallel array of the wires attached to each pin*/
	private ArrayList<ArrayList<Wire> > pinWires = new ArrayList<ArrayList<Wire>>(); 
	
	/**
	 * Constructor
	 * @param pins
	 * @param primDef
	 * @param site
	 * @param elementTab
	 */
	public DeleteSitePinCommand(ArrayList<QTreeWidgetItem> pins, PrimitiveDef primDef, Site site, ElementTab elementTab){
		this.elementTab = elementTab;
		this.currentSite = primDef;
		this.siteShape = site;
	
		int i = 0;
		//initializing each parallel array based on the list of QTreePin passed into the constructor
		for (QTreeWidgetItem pin1 : pins) {
			QTreePin pin = (QTreePin)pin1;
			treePins.add(pin);
			elementPins.add(pin.getPin());
			elements.add(elementTab.getPinElement(pin));
			pinShapes.add(site.getGraphicsPin(pin));
			
			//getting the wires attached to each pin so we can re-add them later
			pinWires.add(new ArrayList<Wire>() );
			for (Wire wire : pin.get_wires()) {
				pinWires.get(i).add(wire);
			}
			
			//creating a new primitive def pin with the opposite direction of the pin element...used for undoing the deletion operation
			PrimitiveDefPin sitePin = new PrimitiveDefPin();
			sitePin.setConnected(true);
			sitePin.setDirection(pin.getPin().getDirection() == PrimitiveDefPinDirection.OUTPUT ? PrimitiveDefPinDirection.INPUT 
								: PrimitiveDefPinDirection.OUTPUT );
			sitePin.setExternalName(pin.getPin().getInternalName());
			sitePin.setInternalName(pin.getPin().getInternalName());
			sitePins.add(sitePin);
			
			//calculating the last pin position for each of these sites
			lastPinPos.add(new QPointF(pinShapes.get(i).pos().x(), pinShapes.get(i).pos().y() + siteShape.getPinWidth() ) );
			i++;	
		}	
		
		this.setText("deleting selected site pins");
	}
	
	/**
	 * Deletes the selected site pins
	 */
	@Override 
	public void redo(){
		for (int i = 0; i < this.treePins.size(); i++) {
			//removing pin from the tree view
			this.elementTab.removeSitePinFromTree(treePins.get(i));
			//removing the pin from the primitive def data structure
			this.currentSite.getPins().remove(sitePins.get(i));
			this.currentSite.getElements().remove(elements.get(i));
			//removing the pin from the graphics scene
			this.siteShape.removePinsFromGraphicsScene(pinShapes.get(i));
		}
	}
	/**
	 * Adds the deleted site pins back to the primitive def
	 */
	@Override 
	public void undo(){
		for (int i = 0; i < this.treePins.size(); i++) {
			this.elementTab.addSitePinToTree(treePins.get(i), elements.get(i));
			this.currentSite.addPin(sitePins.get(i));
			this.currentSite.addElement(elements.get(i));
			this.siteShape.addPinToGraphicsScene(pinShapes.get(i), lastPinPos.get(i));
			
			//re-add each pin wire 
			for (Wire wire : this.pinWires.get(0)) {
				wire.addWireToScene( (PrimitiveSiteScene) siteShape.scene());
				wire.undoRemoveWire();
			}
		}
	}
}
