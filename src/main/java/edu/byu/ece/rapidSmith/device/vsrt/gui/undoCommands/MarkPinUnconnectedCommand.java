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
package edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands;

import java.util.ArrayList;

import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QTreeWidgetItem;
import com.trolltech.qt.gui.QUndoCommand;

import edu.byu.ece.rapidSmith.device.vsrt.gui.PrimitiveSiteScene;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreePin;

/**
 * This class allows users to mark site pins as unconnected, and undo the action if necessary
 * @author Thomas Townsend
 * Created on: Jul 30, 2014
 */
public class MarkPinUnconnectedCommand extends QUndoCommand{

	/**Scene that contains the site pin graphics object*/
	private PrimitiveSiteScene scene;
	/**Pins to mark as unconnected*/
	private ArrayList<QTreeWidgetItem> markedPins;
	
	/**
	 * Constructor 
	 * @param scene
	 * @param pins
	 */
	public MarkPinUnconnectedCommand (PrimitiveSiteScene scene, ArrayList<QTreeWidgetItem> pins){
		this.scene = scene;
		this.markedPins = pins; 
		this.setText(" marking pins as unconnected");
	}
	
	/**
	 * Marks the site pins as unconnected
	 */
	@Override
	public void redo(){
		this.swapConnected();
	}
	/**
	 * Marks the site pins as connected
	 */
	@Override 
	public void undo(){
		this.swapConnected();
	}
	
	/**
	 * This method changes the connectivity of each selected site pin.  If the site pin is connected <br>
	 * than it is changed to unconnected and vice versa.
	 */
	private void swapConnected(){
		for (QTreeWidgetItem item : markedPins) {

			boolean connected = ((QTreePin) item).getPin().isConnected();
			((QTreePin) item).getPin().setConnected(!connected);
			//Show unconnected pins as gray in the tree view and graphics view
			item.setForeground(0, new QBrush((connected) ? QColor.gray : QColor.red));
			item.setSelected(false);
		}
		scene.update();
	}
}
