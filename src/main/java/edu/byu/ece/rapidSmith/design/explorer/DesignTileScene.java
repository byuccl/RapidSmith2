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
package edu.byu.ece.rapidSmith.design.explorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.trolltech.qt.core.Qt.PenStyle;
import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QPainterPath;
import com.trolltech.qt.gui.QPen;

import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.gui.TileScene;
import edu.byu.ece.rapidSmith.timing.PathDelay;

public class DesignTileScene extends TileScene {

	private QPen wirePen;
	private ArrayList<PathItem> currLines;

	private float minDelay = Float.MAX_VALUE;

	private float maxDelay = Float.MIN_VALUE;

	DesignTileScene(){
		super();
		currLines = new ArrayList<>();
		wirePen = new QPen(QColor.yellow, 0.75, PenStyle.SolidLine);
	}


	void drawPath(ArrayList<WireJunction> conns, PathDelay pd){
		double enumSize = we.getWires().length;
		QPainterPath path = new QPainterPath();

		for(WireJunction conn : conns){
			Wire startWire = conn.getStartWire();
			Wire endWire = conn.getEndWire();
			double x1 = (double) tileXMap.get(startWire.getTile())*tileSize  + (startWire.getWireEnum()%tileSize);
			double y1 = (double) tileYMap.get(startWire.getTile())*tileSize  + (startWire.getWireEnum()*tileSize)/enumSize;
			double x2 = (double) tileXMap.get(endWire.getTile())*tileSize  + (endWire.getWireEnum()%tileSize);
			double y2 = (double) tileYMap.get(endWire.getTile())*tileSize  + (endWire.getWireEnum()*tileSize)/enumSize;

			path.moveTo(x1, y1);
			path.lineTo(x2, y2);
		}
		PathItem item = new PathItem(path, pd);
		item.setBrush(QBrush.NoBrush);
		item.setPen(wirePen);
		item.setAcceptHoverEvents(true);
		item.setZValue(20);
		addItem(item);
		currLines.add(item);

		item.setToolTip(pd.getSource() + " to \n" + pd.getDestination() + "\n (" + String.format("%.3f", pd.getDelay()) + "ns)");
		if(pd.getDelay() > maxDelay){
			maxDelay = pd.getDelay();
		}
		else if(pd.getDelay() < minDelay){
			minDelay = pd.getDelay();
		}
	}

	void sortPaths(){
		PathItem[] paths = new PathItem[currLines.size()];
		paths = currLines.toArray(paths);
		Arrays.sort(paths);
		currLines.clear();
		Collections.addAll(currLines, paths);
	}
	
	/*public void drawWire(Connection conn){
		double enumSize = we.getWires().length;
		//System.out.println(conn.startTile + " " + conn.startWire + " " + tileXMap.get(conn.startTile));
		double x1 = (double) tileXMap.get(conn.startTile)*tileSize  + (conn.startWire%tileSize);
		double y1 = (double) tileYMap.get(conn.startTile)*tileSize  + (conn.startWire*tileSize)/enumSize;
		double x2 = (double) tileXMap.get(conn.endTile)*tileSize  + (conn.endWire%tileSize);
		double y2 = (double) tileYMap.get(conn.endTile)*tileSize  + (conn.endWire*tileSize)/enumSize;
		
		QPainterPath path = new QPainterPath();
		path.moveTo(x1, y1);
		path.lineTo(x2, y2);
		PathItem line = new PathItem(path, );
		
		line.setToolTip(conn.toString(we));
		line.setPen(wirePen);
		addItem(line);
		currLines.add(line);
	}*/

	/**
	 * @return the currLines
	 */
	public ArrayList<PathItem> getCurrLines() {
		return currLines;
	}
}
