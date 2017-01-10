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

import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.PinShape;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Site;

/**
 * This class allows the user to resize a primitive site, and undo those changes if necessary
 * @author Thomas Townsend
 * Created on: Aug 11, 2014
 */
public class ResizeSiteCommand extends QUndoCommand{

	private Site site;
	
	/**New Site information*/
	private QPointF newPos;
	private double newWidth;
	private double newHeight;
	
	private ArrayList<QPointF> newInPinPos = new ArrayList<QPointF>();
	private ArrayList<QPointF> newOutPinPos= new ArrayList<QPointF>();
	
	/**Old Site information*/
	private QPointF oldPos;
	private double oldWidth;
	private double oldHeight;
	
	private ArrayList<QPointF> oldInPinPos = new ArrayList<QPointF>();
	private ArrayList<QPointF> oldOutPinPos= new ArrayList<QPointF>();
	
	int count = 0;
	/**
	 * Constructor: Records the site information before it has been resized
	 * @param site
	 */
	public ResizeSiteCommand(Site site){
		this.site = site;
		
		this.oldPos = site.pos();
		this.oldWidth = site.width();
		this.oldHeight = site.height();
		
		for (PinShape pin : site.getInPins()) 
			oldInPinPos.add(pin.pos());
		
		for (PinShape pin : site.getOutPins()) 
			oldOutPinPos.add(pin.pos());
		
		this.setText("Resizing Site");
	}
	
	/**
	 * Resizes the site to the new size and location 
	 */
	@Override
	public void redo(){
		if (count != 0) {
			site.setPos(this.newPos);
			site.setWidth(newWidth);
			site.setHeight(newHeight);
			
			int i = 0;
			for (PinShape pin : site.getInPins()) {
				pin.setPos(this.newInPinPos.get(i));
				i++;
			}
			
			i = 0;
			for (PinShape pin : site.getOutPins()) {
				pin.setPos(this.newOutPinPos.get(i));
				i++;
			}
		}
		count++; 
		
		site.snapSiteToGrid();
		site.updateResizeBoxPositions();
	}
	
	/**
	 * Resizes the site to the old size and location 
	 */
	@Override
	public void undo(){
		site.setPos(this.oldPos);
		site.setWidth(oldWidth);
		site.setHeight(oldHeight);
		
		int i = 0;
		for (PinShape pin : site.getInPins()) {
			pin.setPos(this.oldInPinPos.get(i));
			i++;
		}
		
		i = 0;
		for (PinShape pin : site.getOutPins()) {
			pin.setPos(this.oldOutPinPos.get(i));
			i++;
		}
		
		site.snapSiteToGrid();
		site.updateResizeBoxPositions();
	}
	
	/**
	 * 
	 * @param other
	 * @return
	 */
	@Override
	public boolean mergeWith(QUndoCommand other){

		if ( other instanceof ResizeSiteCommand )
			return true;
		
		return false;
	}
	
	/**
	 * Necessary in order to merge commands of this type together
	 * @return
	 */
	@Override
	public int id(){
		return 1; 
	}
	
	
	/**
	 * Records the site information after it has been resized. 
	 * @param site
	 */
	public void setNewSiteInformation(Site site){
		this.newPos = site.pos();
		this.newWidth = site.width();
		this.newHeight = site.height();
		
		for (PinShape pin : site.getInPins()) 
			newInPinPos.add(pin.pos());
		
		for (PinShape pin : site.getOutPins()) 
			newOutPinPos.add(pin.pos());
	}
}
