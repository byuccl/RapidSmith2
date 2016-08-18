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
package edu.byu.ece.rapidSmith.design.explorer;

import com.trolltech.qt.gui.QLineEdit;
import com.trolltech.qt.gui.QSlider;

public class TimingSlider extends QSlider{

	private DesignTileScene scene;
	
	private QLineEdit textBox;
	
	private float maxDelay;
	
	private float minDelay;

	private boolean selfCall = false;
	
	/**
	 * @param scene
	 */
	public TimingSlider(DesignTileScene scene, QLineEdit textBox) {
		super();
		this.scene = scene;
		this.textBox = textBox;
		
	}

	public void setDelays(){
		minDelay = scene.getCurrLines().get(0).getPath().getDelay();
		maxDelay = scene.getCurrLines().get(scene.getCurrLines().size()-1).getPath().getDelay();
		updatePaths(99);
		setTickPosition(TickPosition.TicksBothSides);
	}
	
	public void updateText(String text){
		if(selfCall == true){
			selfCall = false;
			return;
		}
		try{
			float constraint = Float.parseFloat(text);
			int index = (int) ((constraint-minDelay)/(maxDelay-minDelay) * 100.0);
			if(index > 99) index = 99;
			if(index < 0) index = 0;
			setSliderPosition(index);
			updateLines(constraint);
		}
		catch(NumberFormatException e){
			return;
		}
	}
	

	public void updatePaths(Integer value){
		float multiplier = scene.getCurrLines().size()/100;
		int index = (int)(value*multiplier);
		if(value == 0) index = 0;
		if(value >= 99) index = scene.getCurrLines().size()-1;
		float constraint = scene.getCurrLines().get(index).getPath().getDelay();
		selfCall = true;
		textBox.setText(String.format("%5.3f", constraint));
		updateLines(constraint);
	}
	
	public void updateLines(float constraint){
		for(PathItem item : scene.getCurrLines()){
			if(item.getPath().getDelay() > constraint){
				item.setHighlighted();
			}
			else{
				item.setUnhighlighted();
			}
		}
	}
}
