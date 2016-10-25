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
package edu.byu.ece.rapidSmith.device.browser;

import java.util.ArrayList;
import java.util.List;

import com.trolltech.qt.core.QModelIndex;
import com.trolltech.qt.core.Qt.DockWidgetArea;
import com.trolltech.qt.core.Qt.ItemDataRole;
import com.trolltech.qt.core.Qt.SortOrder;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QDockWidget;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QStatusBar;
import com.trolltech.qt.gui.QTreeWidget;
import com.trolltech.qt.gui.QTreeWidgetItem;
import com.trolltech.qt.gui.QWidget;
import com.trolltech.qt.gui.QDockWidget.DockWidgetFeature;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.gui.TileView;
import edu.byu.ece.rapidSmith.gui.WidgetMaker;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * This class creates an interactive Xilinx FPGA device browser for all of the
 * devices currently installed on RapidSmith.  It provides the user with a 2D view
 * of all tile array in the device.  Allows each tile to be selected (double click)
 * and populate the primitive site and wire lists.  Wire connections can also be drawn
 * by selecting a specific wire in the tile (from the list) and the program will draw
 * all connections that can be made from that wire.  The wire positions on the tile
 * are determined by a hash and are not related to FPGA Editor positions.   
 * @author Chris Lavin and Marc Padilla
 * Created on: Nov 26, 2010
 */
public class DeviceBrowser extends QMainWindow{
	/** The Qt View for the browser */
	protected TileView view;
	/** The Qt Scene for the browser */
	private DeviceBrowserScene scene;
	/** The label for the status bar at the bottom */
	private QLabel statusLabel;
	/** The current device loaded */
	Device device;
	/** The current wire enumerator */
	WireEnumerator we;
	/** The current part name of the device loaded */
	private String currPart;
	/** This is the tree of parts to select */
	private QTreeWidget treeWidget;
	/** This is the list of primitive sites in the current tile selected */
	private QTreeWidget primitiveList;
	/** This is the list of wires in the current tile selected */
	private QTreeWidget wireList;
	/** This is the current tile that has been selected */
	private Tile currTile = null;
	
	protected boolean hideTiles = false;
	
	protected boolean drawPrimitives = true; 
	/**
	 * Main method setting up the Qt environment for the program to run.
	 * @param args the input arguments
	 */
	public static void main(String[] args){
		QApplication.setGraphicsSystem("raster");
		QApplication.initialize(args);
		DeviceBrowser testPTB = new DeviceBrowser(null);
		testPTB.show();
		QApplication.exec();
	}

	/**
	 * Constructor which initializes the GUI and loads the first part found.
	 * @param parent The Parent widget, used to add this window into other GUIs.
	 */
	public DeviceBrowser(QWidget parent){
		super(parent);
		
		// set the title of the window
		setWindowTitle("Device Browser");
		
		initializeSideBar();
		
		// Gets the available parts in RapidSmith and populates the selection tree
		List<String> parts = RSEnvironment.defaultEnv().getAvailableParts();
		if(parts.size() < 1){
			MessageGenerator.briefErrorAndExit("Error: No available parts. " +
					"Please generate part database files.");
		}
		if(parts.contains("xc6vcx75tff784")){
			currPart = "xc6vcx75tff784";
		}
		else{
			currPart = parts.get(0);
		}
		
		device = RSEnvironment.defaultEnv().getDevice(currPart);
		we = device.getWireEnumerator();

		// Setup the scene and view for the GUI
		scene = new DeviceBrowserScene(device, hideTiles, drawPrimitives, this);
		view = new TileView(scene);
		setCentralWidget(view);

		// Setup some signals for when the user interacts with the view
		scene.updateStatus.connect(this, "updateStatus(String, Tile)");
		scene.updateTile.connect(this, "updateTile(Tile)");
		
		// Initialize the status bar at the bottom
		statusLabel = new QLabel("Status Bar");
		statusLabel.setText("Status Bar");
		QStatusBar statusBar = new QStatusBar();
		statusBar.addWidget(statusLabel);
		setStatusBar(statusBar);
		
		// Set the opening default window size to 1024x768 pixels
		resize(1024, 768);
	}

	/**
	 * Populates the treeWidget with the various parts and families of devices
	 * currently available in this installation of RapidSmith.  It also creates
	 * the windows for the primitive site list and wire list.
	 */
	private void initializeSideBar(){
		treeWidget = WidgetMaker.createAvailablePartTreeWidget("Select a part...");
		treeWidget.doubleClicked.connect(this,"showPart(QModelIndex)");
		
		QDockWidget dockWidget = new QDockWidget(tr("Part Browser"), this);
		dockWidget.setWidget(treeWidget);
		dockWidget.setFeatures(DockWidgetFeature.DockWidgetMovable);
		addDockWidget(DockWidgetArea.LeftDockWidgetArea, dockWidget);
		
		// Create the primitive site list window
		primitiveList = new QTreeWidget();
		primitiveList.setColumnCount(2);
		ArrayList<String> headerList = new ArrayList<String>();
		headerList.add("Site");
		headerList.add("Type");
		primitiveList.setHeaderLabels(headerList);
		primitiveList.setSortingEnabled(true);
		
		QDockWidget dockWidget2 = new QDockWidget(tr("Primitive List"), this);
		dockWidget2.setWidget(primitiveList);
		dockWidget2.setFeatures(DockWidgetFeature.DockWidgetMovable);
		addDockWidget(DockWidgetArea.LeftDockWidgetArea, dockWidget2);
		
		// Create the wire list window
		wireList = new QTreeWidget();
		wireList.setColumnCount(2);
		ArrayList<String> headerList2 = new ArrayList<String>();
		headerList2.add("Wire");
		headerList2.add("Sink Connections");
		wireList.setHeaderLabels(headerList2);
		wireList.setSortingEnabled(true);
		QDockWidget dockWidget3 = new QDockWidget(tr("Wire List"), this);
		dockWidget3.setWidget(wireList);
		dockWidget3.setFeatures(DockWidgetFeature.DockWidgetMovable);
		addDockWidget(DockWidgetArea.LeftDockWidgetArea, dockWidget3);

		// Draw wire connections when the wire name is double clicked
		wireList.doubleClicked.connect(this, "wireDoubleClicked(QModelIndex)");
	}
	
	/**
	 * This method will draw all of the wire connections based on the wire given.
	 * @param index The index of the wire in the wire list.
	 */
	public void wireDoubleClicked(QModelIndex index){
		scene.clearCurrentLines();
		if(currTile == null) return;
		if(index.column() != 0) return;
		int currWire = we.getWireEnum(index.data().toString());
		if(currWire < 0) return;
		if(currTile.getWireConnections(we.getWireEnum(index.data().toString())) == null) return;
		for(WireConnection wire : currTile.getWireConnections(we.getWireEnum(index.data().toString()))){
			scene.drawWire(currTile, currWire, wire.getTile(currTile), wire.getWire());
		}
	}
	
	/**
	 * This method gets called each time a user double clicks on a tile.
	 * @param tile the tile to update
	 */
	protected void updateTile(Tile tile){
		currTile = tile;
		updatePrimitiveList();
		updateWireList();
	}
	
	/**
	 * This will update the primitive list window based on the current
	 * selected tile.
	 */
	protected void updatePrimitiveList(){
		primitiveList.clear();
		if(currTile == null || currTile.getPrimitiveSites() == null) return;
		for(Site ps : currTile.getPrimitiveSites()){
			QTreeWidgetItem treeItem = new QTreeWidgetItem();
			treeItem.setText(0, ps.getName());
			treeItem.setText(1, ps.getType().toString());
			primitiveList.insertTopLevelItem(0, treeItem);
		}
	}

	/**
	 * This will update the wire list window based on the current
	 * selected tile.
	 */
	protected void updateWireList(){
		wireList.clear();
		if(currTile == null || currTile.getWireHashMap() == null) return;
		for(Integer wire : currTile.getWireHashMap().keySet()) {
			QTreeWidgetItem treeItem = new QTreeWidgetItem();
			treeItem.setText(0, we.getWireName(wire));
			WireConnection[] connections = currTile.getWireConnections(wire);
			treeItem.setText(1, String.format("%3d", connections == null ? 0 : connections.length));
			wireList.insertTopLevelItem(0, treeItem);
		}
		wireList.sortByColumn(0, SortOrder.AscendingOrder);
	}

	/**
	 * This method loads a new device based on the part name selected in the 
	 * treeWidget.
	 * @param qmIndex The index of the part to load.
	 */
	protected void showPart(QModelIndex qmIndex){
		Object data = qmIndex.data(ItemDataRole.AccessibleDescriptionRole);
		if( data != null){
			if(currPart.equals(data))
				return;
			currPart = (String) data;			
			device = RSEnvironment.defaultEnv().getDevice(currPart);
			we = device.getWireEnumerator();
			scene.setDevice(device);
			scene.initializeScene(hideTiles, drawPrimitives);
			statusLabel.setText("Loaded: "+currPart.toUpperCase());
		}
	}
	
	/**
	 * This method updates the status bar each time the mouse moves from a 
	 * different tile.
	 * @param text the new text for the status
	 * @param tile unused
	 */
	protected void updateStatus(String text, Tile tile){
		statusLabel.setText(text);
		//currTile = tile;
		//System.out.println("currTile=" + tile);
	}
}
