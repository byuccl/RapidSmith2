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

import com.trolltech.qt.core.Qt.WindowModality;
import com.trolltech.qt.gui.*;
import com.trolltech.qt.gui.QKeySequence.StandardKey;
import edu.byu.ece.rapidSmith.design.explorer.FilterWindow.FilterType;
import edu.byu.ece.rapidSmith.design.xdl.XdlDesign;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.gui.FileFilters;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLReader;
import edu.byu.ece.rapidSmith.timing.PathDelay;
import edu.byu.ece.rapidSmith.timing.PathOffset;
import edu.byu.ece.rapidSmith.timing.TraceReportParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * This class aims to allow a user to examine and modify XDL designs in a much 
 * easier fashion than standard text.
 * @author Chris Lavin
 * Created on: Sep 15, 2010
 */
public class DesignExplorer extends QMainWindow{
	/** Status Bar Label */
	private QLabel statusLabel;
	/** Path to images in RapidSmith */
	private static String rsrcPath = "classpath:images";
	/** Device of the current design that is open */
	protected Device device;
	/** The design that is current and active */
	protected XdlDesign design;
	/** Name of the Program */
	private static String title = "Design Explorer";
	/** File Name of the current design that is open */
	private String currOpenFileName = null;
	/** Status Bar */
	private QStatusBar statusBar;
	/** Keeps track of the tabs within the application */
	QTabWidget tabs;
	/** This is the tile view of the design */
	TileWindow tileWindow;
	/** This is the list of nets in the design */
	FilterWindow netWindow;
	/** This is the list of instances in the design */
	FilterWindow instanceWindow;
	/** This is the list of modules in the design */
	FilterWindow moduleWindow;
	/** This is the list of module instances in the design */
	FilterWindow moduleInstanceWindow;
	
	/** This is the list of path delays in the design */
	private FilterWindow delayWindow;
	/** This is the list of path offsets in the design */
	private FilterWindow offsetWindow;
	/** Optional path delays for the design, loaded from timing report (.TWR) */
	ArrayList<PathDelay> delays;
	/** Optional path offsets for the design, loaded from timing report (.TWR) */
	ArrayList<PathOffset> offsets;
	
	// Names of the tabs
	private static final String TILE_LAYOUT = "Tiles";
	private static final String NETS = "Nets";
	private static final String INSTANCES = "Instances";
	private static final String PIPS = "PIPS";
	private static final String MODULES = "Modules";
	private static final String RESOURCE_REPORT = "Resource Report";
	private static final String PATH_DELAYS = "Path Delays";
	private static final String PATH_OFFSETS = "Path Offsets";
	
	
	public static void main(String[] args){
		QApplication.setGraphicsSystem("raster");
		QApplication.initialize(args);

		String fileToOpen = null;
		String traceFileToOpen = null;
		if(args.length > 0){
			fileToOpen = args[0];
		}
		if(args.length > 1){
			traceFileToOpen = args[1];
		}
		
		DesignExplorer designExplorer = new DesignExplorer(null, fileToOpen, traceFileToOpen);

		designExplorer.show();

		QApplication.execStatic();
	}
	
	/**
	 * Constructor for the design explorer
	 * @param parent Parent QWidget for window hierarchy.
	 * @param fileToOpen The name of the design to open
	 * @param traceFileToOpen Name of the trace report file (TWR) to load 
	 */
	private DesignExplorer(QWidget parent, String fileToOpen, String traceFileToOpen) {
		super(parent);
		
		setupFileActions();
		setWindowTitle(title);

		// Setup Windows
		tabs = new QTabWidget();
		setCentralWidget(tabs);
		
		setupTileWindow();
		statusLabel = new QLabel("Status Bar");
		statusLabel.setText("Status Bar");
		statusBar = new QStatusBar();
		statusBar.addWidget(statusLabel);
		setStatusBar(statusBar);
		tileWindow.scene.updateStatus.connect(statusLabel, "setText(String)");
		
		
		netWindow = new FilterWindow(this, FilterType.NETS);
		instanceWindow = new FilterWindow(this, FilterType.INSTANCES);
		moduleWindow = new FilterWindow(this, FilterType.MODULES);
		moduleInstanceWindow = new FilterWindow(this, FilterType.MODULE_INSTANCES);

		tabs.addTab(netWindow, NETS);
		tabs.addTab(instanceWindow, INSTANCES);
		tabs.addTab(moduleWindow, MODULES);

		if(fileToOpen != null){
			try {
				internalOpenDesign(fileToOpen);
			} catch (IOException e) {
				System.err.println("Error loading design file");
				System.exit(2);
			}
		}
		
		if(traceFileToOpen != null){
			try {
				internalLoadDesignTimingInfo(traceFileToOpen);
			} catch (IOException e) {
				System.err.println("Failed to load trace file");
			}
		}
		
		if(currOpenFileName == null) {
			try {
				openDesign();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Set the opening default window size to 1024x768 pixels
		}
		resize(1024, 768);
	}

	/**
	 * Initializes the tile window
	 */
	private void setupTileWindow(){
        tileWindow = new TileWindow(this);
        int tabIndex = tabs.addTab(tileWindow, TILE_LAYOUT);
        tabs.setCurrentIndex(tabIndex);
	}
	
	/**
	 * Creates the message when choosing about on the menu.
	 */
	protected void about(){
		QMessageBox.information(this, "Info",
				"This is the first try \nat an XDL Design Explorer.");
	}
	
	/**
	 * Opens a file chooser dialog to load an XDL file.
	 */
	protected void openDesign() throws IOException {
		String fileName = QFileDialog.getOpenFileName(this, "Choose a file...",
				".", FileFilters.xdlFilter);
		if(fileName.endsWith(".xdl")){
			internalOpenDesign(fileName);
		}
	}
	
	/**
	 * Opens a file chooser dialog to load a TWR (timing report) file.
	 */
	protected void loadDesignTimingInfo() throws IOException {
		String fileName = QFileDialog.getOpenFileName(this, "Choose corresponding timing report...",
				".", FileFilters.twrFilter);
		if(fileName.endsWith(".twr")){
			internalLoadDesignTimingInfo(fileName);
		}
	}
	
	/**
	 * Loads the timing report fileName into the delay and offset windows.
	 * @param fileName Name of the TWR (timing report) file to load.
	 */
	private void internalLoadDesignTimingInfo(String fileName) throws IOException {
		TraceReportParser parser = new TraceReportParser();
		parser.parseTWR(fileName, design);
		delays = parser.getPathDelays();
		offsets = parser.getPathOffsets();

		// Create 2 more tabs for timing information
		delayWindow = new FilterWindow(this, FilterType.DELAYS);
		offsetWindow = new FilterWindow(this, FilterType.OFFSETS);
		tabs.addTab(delayWindow, PATH_DELAYS);
		tabs.addTab(offsetWindow, PATH_OFFSETS);
		
		tileWindow.drawCriticalPaths(delays);
		tileWindow.slider.setDelays();
	}
	
	/**
	 * Loads the XDL design fileName into the design explorer.
	 * @param fileName Name of the XDL file to load.
	 */
	private void internalOpenDesign(String fileName) throws IOException {
		currOpenFileName = fileName;
		String shortFileName = fileName.substring(fileName.lastIndexOf('/')+1);
		QProgressDialog progress = new QProgressDialog("Loading "+currOpenFileName+"...", "", 0, 100, this);
		progress.setWindowTitle("Load Progress");
		progress.setWindowModality(WindowModality.WindowModal);
		progress.setCancelButton(null);
		progress.show();
		progress.setValue(0);	
		progress.setValue(10);
		progress.setValue(20);
		design = new XDLReader().readDesign(Paths.get(fileName));
		progress.setValue(50);
		device = design.getDevice();
		progress.setValue(70);
		progress.setValue(80);
		progress.setValue(90);
		setWindowTitle(shortFileName + " - " + title);
		progress.setValue(100);
		statusBar().showMessage(currOpenFileName + " loaded.", 2000);
		tileWindow.setDesign(design);
		netWindow.loadCurrentDesignData();
		instanceWindow.loadCurrentDesignData();
		moduleWindow.loadCurrentDesignData();
		moduleInstanceWindow.loadCurrentDesignData();
	}
	
	/**
	 * This method builds the actions associated with the toolbar.
	 * @param name Name of the action
	 * @param image An image/icon associated with the action
	 * @param shortcut A shortcut key to press on the keyboard to activate 
	 * this action.
	 * @param slot The method to call when the action is made.
	 * @param menu Which menu to add the action to.
	 * @param toolBar The toolbar to add the action to.
	 * @return The action object.
	 */
	private QAction action(String name, String image, Object shortcut,
			String slot, QMenu menu, QToolBar toolBar) {
		QAction a = new QAction(name, this);

		if (image != null)
			a.setIcon(new QIcon(rsrcPath + File.separator + image + ".png"));
		if (menu != null)
			menu.addAction(a);
		if (toolBar != null)
			toolBar.addAction(a);
		if (slot != null)
			a.triggered.connect(this, slot);

		if (shortcut instanceof String)
			a.setShortcut((String) shortcut);
		else if (shortcut instanceof QKeySequence.StandardKey)
			a.setShortcuts((QKeySequence.StandardKey) shortcut);

		return a;
	}

	/**
	 * Creates a file menu with actions.  These are also added to the tool bar.
	 */
	private void setupFileActions(){
		QToolBar tb = new QToolBar(this);
		tb.setWindowTitle(tr("File Actions"));
		addToolBar(tb);

		QMenu fileMenu = new QMenu(tr("&File"), this);
		menuBar().addMenu(fileMenu);

		action(tr("Open XDL Design"), "open", StandardKey.Open, "openDesign()",fileMenu, tb);		
		action(tr("Load Timing Info"), "openTimingReport", StandardKey.UnknownKey, "loadDesignTimingInfo()",fileMenu, tb);
		fileMenu.addSeparator();
		action(tr("&Quit"), null, "Ctrl+Q", "close()", fileMenu, null);
	}

}
