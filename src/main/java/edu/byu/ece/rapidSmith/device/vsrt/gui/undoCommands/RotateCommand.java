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

import com.trolltech.qt.gui.QGraphicsItemInterface;
import com.trolltech.qt.gui.QUndoCommand;

import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.ElementShape;

/**
 * This class allows the user to rotate an element and undo the rotation if needed
 * @author Thomas Townsend
 * Created on: Aug 11, 2014
 */
public class RotateCommand extends QUndoCommand{

	/**List of elements that were rotated*/
	private ArrayList<ElementShape> elements = new ArrayList<ElementShape>();
	boolean rotateClockwise;
	
	/**
	 * Constructor: Creates the list of rotated elements and sets the text of this command
	 * @param elements
	 */
	public RotateCommand(ArrayList<QGraphicsItemInterface> elements, boolean rotateClockwise){
		this.rotateClockwise = rotateClockwise;
		String text = "rotate ";
		for (QGraphicsItemInterface item : elements) {
			this.elements.add((ElementShape) item);
			text = text.concat(((ElementShape) item).getTreeElement().text(0) + " "); 
		}
		this.setText(text);
	}
	/**
	 * Rotates the element clockwise by 90 degrees
	 */
	@Override
	public void redo(){
		for (ElementShape element : elements) 
			element.rotate(rotateClockwise);		
	}
	/**
	 * Rotates the element counterclockwise by 90 degrees
	 */
	@Override 
	public void undo(){
		for (ElementShape element : elements) 
			element.rotate(!rotateClockwise);
	}
}//end class 