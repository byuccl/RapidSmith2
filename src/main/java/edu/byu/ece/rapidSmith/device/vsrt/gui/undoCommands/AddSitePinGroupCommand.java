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
import com.trolltech.qt.gui.QUndoCommand;

import edu.byu.ece.rapidSmith.device.vsrt.gui.ElementTab;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreePin;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.PinShape;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Site;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.Element;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.PrimitiveDef;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.PrimitiveDefPin;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.PrimitiveDefPinDirection;

/**
 * This class allows the user to add a new group of site pins, and undo the action if necessary
 * @author Thomas Townsend 
 * Created on: Jul 30, 2014
 */
public class AddSitePinGroupCommand extends QUndoCommand {
	/**Primitive def pin to be added*/
	private ArrayList<PrimitiveDefPin>  sitePins = new ArrayList<PrimitiveDefPin>() ;
	/**Primitive def pin within the pin element*/
	private ArrayList<PrimitiveDefPin> elementPins = new ArrayList<PrimitiveDefPin>();
	/**Pin element for the new pin*/
	private ArrayList<Element> elements =new ArrayList<Element>();
	/**Tree View pin for the new pin*/
	private ArrayList<QTreePin> treePins = new ArrayList<QTreePin>();
	/**Site to which this new pin will be added*/
	private Site siteShape;
	/**Data structure where the pin will be added*/
	private PrimitiveDef currentSite; 
	/**QWidget that has access to the tree view*/
	private ElementTab elementTab;
	/**Position on the graphics scene where the pin is initially placed*/
	private ArrayList<QPointF> initialPos = new ArrayList<QPointF>();
	/**Graphics object representing the pin*/
	private ArrayList<PinShape> pinShapes = new ArrayList<PinShape>();
	/**Square size on the background grid*/
	private double pin_width;
	/**number of site pins to be created*/
	private int count;

	/**
	 * Constructor
	 * @param pinName
	 * @param isOutput
	 * @param primDef
	 * @param site
	 * @param elementTab
	 * @param init
	 */
	public AddSitePinGroupCommand(String pinName, boolean isOutput, PrimitiveDef primDef, Site site, ElementTab elementTab, QPointF init, int count){
		this.currentSite = primDef;
		this.siteShape = site;
		this.elementTab = elementTab; 
		this.count = count; 
		
		this.pin_width = siteShape.getPinWidth();
		double offsetX = init.x() % pin_width;
		double offsetY = init.y() % pin_width;
		double startX = init.x()-offsetX;
		double startY = init.y()-offsetY;  
		
		for(int i = 0; i < count;  i++) {
			//Creating the necessary primitive def structures
			PrimitiveDefPin sitePin = new PrimitiveDefPin();
			sitePin.setInternalName(pinName + ((count==1) ? "" : i));
			sitePin.setExternalName(pinName + ((count==1) ? "" : i));
			sitePin.setDirection( isOutput ? PrimitiveDefPinDirection.OUTPUT : PrimitiveDefPinDirection.INPUT);
			this.sitePins.add(sitePin);
			
			//Pin found within element (opposite direction)
			PrimitiveDefPin elementPin = new PrimitiveDefPin();
			elementPin.setInternalName(pinName + ((count==1) ? "" : i));
			elementPin.setDirection(isOutput ? PrimitiveDefPinDirection.INPUT : PrimitiveDefPinDirection.OUTPUT);
			this.elementPins.add(elementPin);
			
			//Pin element
			Element element = new Element();
			element.setName(pinName + ((count==1) ? "" : i));
			element.setBel(false);
			element.setPin(true);
			element.addPin(elementPin);
			this.elements.add(element); 
			
			QTreePin treePin = elementTab.createNewSitePin(element);
			this.treePins.add(treePin);
			
			this.pinShapes.add( new PinShape(treePin, pin_width, true) );
			
			this.initialPos.add(new QPointF(startX, startY + (i+1)*pin_width));
			}
		
		if (count == 1){
			this.setText("adding site pin " + pinName);
		} 
		else {
			this.setText("adding " + count + " site pins named " + pinName);
		}
	}
	
	/**
	 * Adds the newly created site pin to the PrimitiveDef data structure and GUI
	 */
	@Override 
	public void redo(){
		for(int i = 0; i < count; i++){
			this.elementTab.addSitePinToTree(this.treePins.get(i), this.elements.get(i));
			this.currentSite.addPin(this.sitePins.get(i));
			this.currentSite.addElement(this.elements.get(i));
			this.siteShape.addPinToGraphicsScene(this.pinShapes.get(i), initialPos.get(i));	
		}
	}
	
	/**
	 * Removes the pin from the PrimitiveDef data structure and GUI
	 */
	@Override 
	public void undo(){
		for(int i = 0; i < count; i++){
			this.elementTab.removeSitePinFromTree(this.treePins.get(i));
			this.currentSite.getPins().remove(this.sitePins.get(i));
			this.currentSite.getElements().remove(this.elements.get(i));
			this.siteShape.removePinsFromGraphicsScene(this.pinShapes.get(i));
		}
	}
}
