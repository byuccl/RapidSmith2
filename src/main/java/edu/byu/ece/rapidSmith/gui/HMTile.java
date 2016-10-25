/*
 * Copyright (c) 2010 Brigham Young University
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
package edu.byu.ece.rapidSmith.gui;

import com.trolltech.qt.gui.QBitmap;
import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QGraphicsItemInterface;
import com.trolltech.qt.gui.QGraphicsRectItem;
import com.trolltech.qt.gui.QPen;
import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.Tile;

import java.awt.*;

/**
 * @author marc
 *
 */
public class HMTile extends QGraphicsRectItem {
	
	private Tile tile;
	private boolean containsSLICEM;
	private boolean isAnchor;
	static final QColor GREEN = new QColor(0, 255, 0, 190);
	static final QColor ORANGE = new QColor(255, 153, 51, 190);
	static final QColor RED = new QColor(255, 0, 0, 190);
	static final QBitmap anchorPixmap = new QBitmap("classpath:images/anchor.bmp");
	static final QBrush ANCHOR_GREEN = new QBrush(GREEN, anchorPixmap);
	static final QBrush ANCHOR_ORANGE = new QBrush(ORANGE, anchorPixmap);
	static final QBrush ANCHOR_RED = new QBrush(RED, anchorPixmap);

	public HMTile(Tile newTile, TileScene scene, QGraphicsItemInterface parent, boolean hasSLICEM, boolean isAnchor)
	{
		super(0,0,scene.tileSize - 2, scene.tileSize - 2, parent);
		this.tile = newTile;
		this.containsSLICEM = hasSLICEM;
		this.isAnchor = isAnchor;
		
	}

	public HMTile(Tile newTile, TileScene scene, QGraphicsItemInterface parent)
	{
		this(newTile,scene,parent,false,false);
	}

	public Tile getTile()
	{
		return tile;
	}

	public boolean containsSLICEM(){
		return containsSLICEM;
	}

	public void setState(GuiShapeState newState){
		
		switch (newState) {
			case VALID:
				this.setPen(new QPen(GREEN));
				if(isAnchor) 
					this.setBrush(ANCHOR_GREEN);
				else
					this.setBrush(new QBrush(GREEN));
				break;
			case COLLIDING:
				this.setPen(new QPen(ORANGE));
				if(isAnchor) 
					this.setBrush(ANCHOR_ORANGE);
				else
					this.setBrush(new QBrush(ORANGE));
				break;
			case INVALID:
				this.setPen(new QPen(RED));
				if(isAnchor) 
					this.setBrush(ANCHOR_RED);
				else
					this.setBrush(new QBrush(RED));
				break;
			default:
				break;
		}
	}
}
