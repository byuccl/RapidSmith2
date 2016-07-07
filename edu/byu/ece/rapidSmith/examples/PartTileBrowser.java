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
package edu.byu.ece.rapidSmith.examples;

import java.util.ArrayList;
import java.util.List;

import com.trolltech.qt.core.QModelIndex;
import com.trolltech.qt.core.Qt.DockWidgetArea;
import com.trolltech.qt.core.Qt.ItemDataRole;
import com.trolltech.qt.core.Qt.WindowModality;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QDockWidget;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QProgressDialog;
import com.trolltech.qt.gui.QStatusBar;
import com.trolltech.qt.gui.QTreeWidget;
import com.trolltech.qt.gui.QWidget;
import com.trolltech.qt.gui.QDockWidget.DockWidgetFeature;

import edu.byu.ece.rapidSmith.RapidSmithEnv;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.gui.WidgetMaker;
import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * This class is an example of how RapidSmith could be used to build
 * interactive tools using Qt or other GUI packages.  This class
 * creates a zoom-able 2D array of the tiles found in the devices installed 
 * with RapidSmith.  This example requires the Qt Jambi (Qt for Java)
 * jars to run.
 * @author marc
 */
public class PartTileBrowser extends QMainWindow{
	/** This is the Qt View object for the tile browser */
	private PartTileBrowserView view;
	/** This is the container for the text in the Status Bar at the bottom of the screen */
	private QLabel statusLabel;
	/** This is the Qt Scene object for the tile browser */
	private PartTileBrowserScene scene;
	/** The current device that has been loaded */
	Device device;
	/** The current part name */
	private String currPartName;
	/** This is the part chooser widget */
	private QTreeWidget treeWidget;

	/**
	 * Main method
	 * @param args
	 */
	public static void main(String[] args){
		// This line fixes slow performance under Linux
		QApplication.setGraphicsSystem("raster");
		
		QApplication.initialize(args);
		PartTileBrowser testPTB = new PartTileBrowser(null);
		testPTB.show();
		QApplication.exec();
	}

	/**
	 * Constructor of a new PartTileBrowser
	 * @param parent Parent widget to which this object belongs.
	 */
	public PartTileBrowser(QWidget parent) {
		super(parent);
		setWindowTitle("Part Tile Browser");

		createTreeView();
		List<String> parts = RapidSmithEnv.getDefaultEnv().getAvailableParts();
		if(parts.size() < 1){
			MessageGenerator.briefErrorAndExit("Error: No available parts. Please generate part database files.");
		}
		currPartName = parts.get(0);
		device = RapidSmithEnv.getDefaultEnv().getDevice(currPartName);
		
		scene = new PartTileBrowserScene(device);

		view = new PartTileBrowserView(scene);

		setCentralWidget(view);
		
		scene.updateStatus.connect(this, "updateStatus()");
		statusLabel = new QLabel("Status Bar");
		statusLabel.setText("Status Bar");
		QStatusBar statusBar = new QStatusBar();
		statusBar.addWidget(statusLabel);
		setStatusBar(statusBar);

	}

	private void createTreeView() {
		treeWidget = WidgetMaker.createAvailablePartTreeWidget("Select a part...");	
		treeWidget.doubleClicked.connect(this,"showPart(QModelIndex)");
		
		QDockWidget dockWidget = new QDockWidget(tr("Part Browser"), this);
		dockWidget.setAllowedAreas(DockWidgetArea.LeftDockWidgetArea);
		dockWidget.setWidget(treeWidget);
		dockWidget.setFeatures(DockWidgetFeature.NoDockWidgetFeatures);
		addDockWidget(DockWidgetArea.LeftDockWidgetArea, dockWidget);
	}

	@SuppressWarnings("unused")
	private void showPart(QModelIndex qmIndex){
		Object data = qmIndex.data(ItemDataRole.AccessibleDescriptionRole);
		if( data != null){
			if(currPartName.equals(data))
				return;
			currPartName = (String) data;
			QProgressDialog progress = new QProgressDialog("Loading "+currPartName.toUpperCase()+"...", "", 0, 100, this);
			progress.setWindowTitle("Load Progress");
			progress.setWindowModality(WindowModality.WindowModal);
			progress.setCancelButton(null);
			progress.show();
			progress.setValue(10);
			
			device = RapidSmithEnv.getDefaultEnv().getDevice(currPartName);
			progress.setValue(100);
			scene.setDevice(device);
			statusLabel.setText("Loaded: "+currPartName.toUpperCase());

			
		}
	}
	void updateStatus() {
		int x = (int) scene.getCurrX();
		int y = (int) scene.getCurrY();
		if (x >= 0 && x < device.getColumns() && y >= 0 && y < device.getRows()){
			String tileName = device.getTile(y, x).getName();
			statusLabel.setText("Part: "+currPartName.toUpperCase() +"  Tile: "+ tileName+" ("+x+","+y+")");
		}
	}

}
