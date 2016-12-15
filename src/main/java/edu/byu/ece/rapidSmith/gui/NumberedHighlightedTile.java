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
package edu.byu.ece.rapidSmith.gui;

import com.trolltech.qt.gui.QFont;
import com.trolltech.qt.gui.QGraphicsRectItem;
import com.trolltech.qt.gui.QGraphicsTextItem;

import edu.byu.ece.rapidSmith.device.Tile;

public class NumberedHighlightedTile  extends QGraphicsRectItem{
	/** */
	protected QGraphicsTextItem text;
	/** */
	protected TileScene scene;
	/** */
	protected static QFont font4 = new QFont("Arial", 4);
	/** */
	protected static QFont font6 = new QFont("Arial", 6);
	/** */
	protected static QFont font8 = new QFont("Arial", 8);
	
	
	public NumberedHighlightedTile(Tile t, TileScene scene, int number){
		super(0, 0, scene.tileSize - 2, scene.tileSize - 2);
		this.scene = scene;
		this.text = new QGraphicsTextItem(Integer.toString(number));
		int x = scene.getDrawnTileX(t) * scene.tileSize;
		int y = scene.getDrawnTileY(t) * scene.tileSize;
		text.setPos(x-4, y);
		if(number < 100){
			text.setFont(font8);			
		}else if(number < 1000){
			text.setFont(font6);
		}else {
			text.setFont(font4);
		}

		this.moveBy(x, y);
		this.scene.addItem(this);
		this.scene.addItem(text);
	}
	
	public void remove(){
		scene.removeItem(text);
		scene.removeItem(this);
	}
}
