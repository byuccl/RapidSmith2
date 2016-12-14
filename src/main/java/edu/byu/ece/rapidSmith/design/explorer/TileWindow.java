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
package edu.byu.ece.rapidSmith.design.explorer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.trolltech.qt.core.QSize;
import com.trolltech.qt.core.Qt.AspectRatioMode;
import com.trolltech.qt.core.Qt.Orientation;
import com.trolltech.qt.gui.QGraphicsScene;
import com.trolltech.qt.gui.QGraphicsView;
import com.trolltech.qt.gui.QGridLayout;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QLineEdit;
import com.trolltech.qt.gui.QSplitter;
import com.trolltech.qt.gui.QWidget;

import edu.byu.ece.rapidSmith.design.xdl.XdlDesign;
import edu.byu.ece.rapidSmith.design.xdl.XdlNet;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.device.Connection;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileWire;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.gui.TileView;
import edu.byu.ece.rapidSmith.timing.PathDelay;
import edu.byu.ece.rapidSmith.timing.PathElement;
import edu.byu.ece.rapidSmith.timing.RoutingPathElement;

/**
 * This class is used for the tile window tab of the design explorer.
 * It could also be used for other applications as well.
 * @author Chris Lavin
 */
public class TileWindow extends QWidget{
	/** Associated view with this window */
	protected TileView view;
	/** Associated scene with this window */
	protected DesignTileScene scene;
	/** The current design */
	protected XdlDesign design;
	/** The layout for the window */
	private QGridLayout layout;
	/** The sidebar view (for use with timing analysis) */
	private QGraphicsView sidebarView;

	TimingSlider slider;

	private QGraphicsScene sidebarScene;
	/**
	 * Constructor
	 * @param parent
	 */
	TileWindow(QWidget parent){
		super(parent);
		scene = new DesignTileScene();
		view = new TileView(scene);
		layout = new QGridLayout();

		// Side bar setup
		QGridLayout sidebarLayout = new QGridLayout();
		sidebarScene = new QGraphicsScene(this);
		QLineEdit textBox = new QLineEdit();
		sidebarView = new QGraphicsView(sidebarScene);
		slider = new TimingSlider(scene, textBox);
		slider.setFixedHeight(200);
		sidebarLayout.addWidget(new QLabel("Choose\nConstraint:"), 0, 0);
		sidebarLayout.addWidget(slider, 1, 0);
		sidebarLayout.addWidget(textBox, 2, 0);
		sidebarLayout.addWidget(new QLabel("ns"), 2, 1);
		sidebarView.setLayout(sidebarLayout);
		slider.sliderMoved.connect(slider, "updatePaths(Integer)");
		textBox.textChanged.connect(slider, "updateText(String)");

		QSplitter splitter = new QSplitter(Orientation.Horizontal);
		splitter.setEnabled(true);
		sidebarView.setMinimumWidth(90);
		splitter.addWidget(sidebarView);
		splitter.addWidget(view);
		layout.addWidget(splitter);
		this.setLayout(layout);
	}

	/**
	 * Updates the design.
	 * @param design New design to set.
	 */
	public void setDesign(XdlDesign design){
		this.design = design;
		scene.setDesign(this.design);
		scene.initializeScene(true, true);
		scene.setDevice(design.getDevice());
	}

	/**
	 * Moves the cursor to a new tile in the tile array.
	 * @param tile The new tile to move the cursor to.
	 */
	void moveToTile(String tile){
		Tile t = design.getDevice().getTile(tile);
		int tileSize = scene.getTileSize();
		QSize size = this.frameSize();
		view.fitInView(scene.getDrawnTileX(t)*tileSize - size.width()/2,
				scene.getDrawnTileY(t)*tileSize - size.height()/2,
				size.width(), size.height(), AspectRatioMode.KeepAspectRatio);
		view.zoomIn(); view.zoomIn();
		view.zoomIn(); view.zoomIn();
		scene.updateCurrXY(scene.getDrawnTileX(t), scene.getDrawnTileY(t));
		scene.updateCursor();
	}

	void drawCriticalPaths(ArrayList<PathDelay> pathDelays){
		DesignTileScene scn = scene;
		for(PathDelay pd : pathDelays){
			ArrayList<WireJunction> conns = new ArrayList<>();
			for(PathElement pe : pd.getMaxDataPath()){
				if(pe.getType().equals("net")){
					if(pe.getClass().equals(RoutingPathElement.class)){
						RoutingPathElement rpe = (RoutingPathElement) pe;
						XdlNet net = rpe.getNet();
						conns.addAll(getAllConnections(net));
						/*for(Connection conn : conns){
							scn.drawWire(conn);
							//System.out.println(conn.toString(scene.getWireEnumerator()));
						}*/
						//return;
					}
				}
			}
			scn.drawPath(conns, pd);
		}
		scn.sortPaths();
	}

	private ArrayList<WireJunction> getAllConnections(XdlNet net){
		ArrayList<WireJunction> conns = new ArrayList<>();
		Set<Wire> wireSet = new HashSet<>();
		for(PIP p : net.getPIPs()){

			if(scene.tileXMap.get(p.getTile()) != null && scene.tileYMap.get(p.getTile()) != null){
				conns.add(new WireJunction(p));
			}

			Wire start = p.getStartWire();
			Wire end = p.getEndWire();
			wireSet.add(start);
			wireSet.add(end);
		}
		for(PIP p : net.getPIPs()){
			Wire tmp = p.getEndWire();
			//System.out.println("  " + tmp.toString(scene.getWireEnumerator()));
			for(Connection w : tmp.getWireConnections()){
				Wire tmp2 = w.getSinkWire();
				//System.out.println("    " + tmp2.toString(scene.getWireEnumerator()));
				if(!tmp2.getTile().equals(tmp.getTile())){
					for(Connection w2 : tmp2.getWireConnections()){
						Wire tmp3 = w2.getSinkWire();
						//System.out.println("      " + tmp3.toString(scene.getWireEnumerator()));
						if(wireSet.contains(tmp3)){
							if(scene.tileXMap.get(tmp.getTile()) != null && scene.tileYMap.get(tmp2.getTile()) != null){
								WireJunction conn = new WireJunction(tmp, tmp2);
								conns.add(conn);
								//System.out.println("* " + conn.toString(scene.getWireEnumerator()));
							}
						}
					}
				}
			}
		}

		return conns;
	}
}
