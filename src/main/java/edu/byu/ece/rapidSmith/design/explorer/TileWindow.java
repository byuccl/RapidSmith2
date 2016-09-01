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
import java.util.HashMap;

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

import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.design.ModuleInstance;
import edu.byu.ece.rapidSmith.design.Net;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.WireConnection;
import edu.byu.ece.rapidSmith.gui.GuiModuleInstance;
import edu.byu.ece.rapidSmith.gui.TileView;
import edu.byu.ece.rapidSmith.router.Node;
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
	protected Design design;
	/** The layout for the window */
	private QGridLayout layout;
	/** The sidebar view (for use with timing analysis) */
	protected QGraphicsView sidebarView;
	
	protected TimingSlider slider;
	
	protected QGraphicsScene sidebarScene;
	/**
	 * Constructor
	 * @param parent 
	 */
	public TileWindow(QWidget parent){
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
	public void setDesign(Design design){
		this.design = design;
		scene.setDesign(this.design);
		scene.initializeScene(true, true);
		scene.setDevice(design.getDevice());

		// Create hard macro blocks
		for(ModuleInstance mi : design.getModuleInstances()){
			scene.addItem(new GuiModuleInstance(mi, scene, false));
		}
	}
	
	/**
	 * Moves the cursor to a new tile in the tile array.
	 * @param tile The new tile to move the cursor to.
	 */
	public void moveToTile(String tile){
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
	
	public void drawCriticalPaths(ArrayList<PathDelay> pathDelays){
		DesignTileScene scn = (DesignTileScene) scene;
		for(PathDelay pd : pathDelays){
			ArrayList<Connection> conns = new ArrayList<Connection>();
			for(PathElement pe : pd.getMaxDataPath()){
				if(pe.getType().equals("net")){
					if(pe.getClass().equals(RoutingPathElement.class)){
						RoutingPathElement rpe = (RoutingPathElement) pe;
						Net net = rpe.getNet();
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
	
	public ArrayList<Connection> getAllConnections(Net net){
		ArrayList<Connection> conns = new ArrayList<Connection>();
		HashMap<Node, Node> nodeMap = new HashMap<Node, Node>();
		for(PIP p : net.getPIPs()){
			
			if(scene.tileXMap.get(p.getTile()) != null && scene.tileYMap.get(p.getTile()) != null){
				conns.add(new Connection(p));
			}
			
			Node start = new Node(p.getTile(), p.getStartWire(), null, 0);
			Node end = new Node(p.getTile(), p.getEndWire(), null, 0);
			nodeMap.put(start, start);
			nodeMap.put(end, end);
		}
		Node tmp = new Node();
		Node tmp2 = new Node();
		Node tmp3 = new Node();
		for(PIP p : net.getPIPs()){
			tmp.setTileAndWire(p.getTile(), p.getEndWire());
			//System.out.println("  " + tmp.toString(scene.getWireEnumerator()));
			if(tmp.getConnections() == null) continue;
			for(WireConnection w : tmp.getConnections()){
				tmp2.setTileAndWire(w.getTile(tmp.getTile()), w.getWire());
				//System.out.println("    " + tmp2.toString(scene.getWireEnumerator()));
				if(!tmp2.getTile().equals(tmp.getTile()) && tmp2.getConnections() != null){
					for(WireConnection w2 : tmp2.getConnections()){
						tmp3.setTileAndWire(w2.getTile(tmp2.getTile()), w2.getWire());
						//System.out.println("      " + tmp3.toString(scene.getWireEnumerator()));
						if(nodeMap.get(tmp3) != null){
							if(scene.tileXMap.get(tmp.getTile()) != null && scene.tileYMap.get(tmp2.getTile()) != null){
								Connection conn = new Connection(tmp.getTile(), tmp2.getTile(), tmp.getWire(), tmp2.getWire()); 
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
