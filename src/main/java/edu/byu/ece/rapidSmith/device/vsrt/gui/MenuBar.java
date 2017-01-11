package edu.byu.ece.rapidSmith.device.vsrt.gui;

import java.util.ArrayList;

import com.trolltech.qt.gui.QAction;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QIcon;
import com.trolltech.qt.gui.QKeySequence;
import com.trolltech.qt.gui.QMenu;
import com.trolltech.qt.gui.QMenuBar;
import com.trolltech.qt.gui.QMessageBox;
import com.trolltech.qt.gui.QMessageBox.StandardButton;
import com.trolltech.qt.gui.QUndoStack;

/**
 * This class creates a custom menuBar used within the GUI 
 * @author Thomas Townsend
 * Created on: Jul 1, 2014
 */
public class MenuBar extends QMenuBar{

	/**File menu*/
	private QMenu file;
	/**Edit menu*/
	private QMenu edit;
	/**Help Menu*/
	private QMenu help;
	private QMenu view; 
	private QAction viewSiteConfig;
	private QAction viewBelConfig;
	private QAction closeSite; 
	private QAction redo;
	private QAction undo; 
	private QUndoStack undoStack;
	private QAction generateOneBel;
	private QAction save;
	private VSRTool parent;
	
	/**
	 * Constructor:
	 * @param parent
	 * @param undoStack
	 */
	public MenuBar(VSRTool parent, QUndoStack undoStack ){
		super(parent);
		
		this.parent = parent;
		this.undoStack = undoStack;
		
		this.createFileMenu();
	    this.createEditMenu();
	    this.createHelpMenu();  
	}
	/**
	 * 
	 */
	private void createFileMenu(){
		// File Menu --add to this as necessary
		//---------------------------------------
		QAction quit = new QAction(new QIcon(VSRTool.getImagePath("quit.png")), "&Exit", this);
		quit.triggered.connect(QApplication.instance(), "exit()");
		quit.setShortcut("Ctrl+q");
		 
		//save button
		save = new QAction(new QIcon(VSRTool.getImagePath("filesave.png")), "Save", this); 
		save.triggered.connect(parent, "savePrimitiveSite()");
		save.setShortcut("Ctrl+s");
		save.setEnabled(false);
		 
//		//import button
//		QAction Import = new QAction(new QIcon(parent.getRapidSmithPath() + File.separator + "images/fileopen.png"), "Import...", this);
//		Import.triggered.connect(this.parent, "openDirectory(String)");
//		Import.setShortcut("Ctrl+i");
		 
		//view site configurations options button
		viewSiteConfig = new QAction(new QIcon(VSRTool.getImagePath("gear.png")), "Site Config Options", this);
		viewSiteConfig.triggered.connect(this.parent, "showSiteConfigDialog()");
		viewSiteConfig.setEnabled(false);
		 
		//view bel configurations options button
		viewBelConfig = new QAction(new QIcon(VSRTool.getImagePath("bel.png")), "Bel Config Options", this);
		viewBelConfig.triggered.connect(this, "belConfigSelected()");
		viewBelConfig.setEnabled(false);
		
		//Close current site button
		closeSite = new QAction( new QIcon(VSRTool.getImagePath("closePrimitiveSite.png")), "Close Site", this );
		closeSite.triggered.connect(this, "closeSite()");
		closeSite.setVisible(false);
		
		generateOneBel = new QAction( new QIcon(VSRTool.getImagePath("connect2.png")), "Generate One Bel Connections", this );
		generateOneBel.triggered.connect(this.parent, "generateAllOneBelConnections()");
		
		//Adding everything to the file menu
		file = this.addMenu("&File");
		file.addAction(save);
	    //file.addAction(Import);
	    file.addSeparator();
	    file.addAction(closeSite);
	    file.addAction(generateOneBel);
	    view = file.addMenu("&View");
	    view.setIcon(new QIcon(VSRTool.getImagePath("view.png")));
	    view.addAction(viewSiteConfig);
	    view.addAction(viewBelConfig);
	    file.addSeparator();
	    file.addAction(quit);
	}
	/**
	 * 
	 */
	private void createEditMenu(){
		//Edit Menu -- only contains redo and undo
	    //------------------------------------------
	    //undo action
	    undo = new QAction(new QIcon(VSRTool.getImagePath("editundo.png")), "&Undo", this); 
	    undo.setEnabled(false);
		undo.triggered.connect( undoStack, "undo()" );
		undo.setShortcut("Ctrl+z");
	    	    
		//redo action
		redo = new QAction(new QIcon(VSRTool.getImagePath("editredo.png")), "&Redo", this);
		redo.setEnabled(false);
		redo.triggered.connect( undoStack, "redo()" );
		ArrayList<QKeySequence> redoShortcuts = new ArrayList<QKeySequence>();
		redoShortcuts.add(new QKeySequence("Ctrl+y"));
		redoShortcuts.add(new QKeySequence("Ctrl+Shift+z"));
		redo.setShortcuts(redoShortcuts);
		
		undoStack.canRedoChanged.connect(redo, "setEnabled(boolean)");
		undoStack.canUndoChanged.connect(undo, "setEnabled(boolean)");
		undoStack.redoTextChanged.connect(this, "redoChanged()");
		undoStack.undoTextChanged.connect(this, "undoChanged()");
		
		edit = this.addMenu("&Edit");
	    edit.addAction(undo);
	    edit.addAction(redo);
	}
	/**
	 * 
	 */
	private void createHelpMenu(){
		//Help Menu --add to this as necessary
	    help = this.addMenu("&Help");
	    QAction about = new QAction(new QIcon(VSRTool.getImagePath("about.png")), "&About", this);
	    about.triggered.connect(this, "displayAbout()");
	    about.setShortcut("Ctrl+a");
	   
	    help.addAction(about); 
	}
	
	/**
	 * Displays the "about" Dialog that describes the purpose of the tool, and gives them a link
	 * to where they can find new information
	 */
	@SuppressWarnings("unused")
	private void displayAbout(){
		// TODO: Change this link to point to online documentation rather than github master branch
		QMessageBox.about(this, "Vivado Subsite Routing Tool", tr("This tool is designed to generate connections "
								+ "between bels on the primitive site level in a quick and easy manner. "
								+ "Once the TCL script 'GetAllPrimitiveDefs' has been run, "
								+ "simply import the generated directory, choose an architecture, "
								+ "a primitive site, and draw the connections! Read the "
								+ "<a href=\"https://github.com/byuccl/RapidSmith2/tree/master/doc\"> VSRT User Guide</a> "
								+ "to learn more."));
	}
	
	/**
	 * Calls a method in the main window to display the currently selected bel config options 
	 */
	@SuppressWarnings("unused")
	private void belConfigSelected(){
		this.parent.showBelConfigOptions(null);
	}
	
	/**
	 * Enables/Disables the buttons that should only be used while a primitive site is open. 
	 * @param enable
	 */
	public void enableShowConfigs(boolean enable){
		this.viewBelConfig.setEnabled(enable);
		this.viewSiteConfig.setEnabled(enable);
		this.save.setEnabled(enable);
		this.closeSite.setVisible(enable);
		this.generateOneBel.setEnabled(!enable);
	}
	
	/**
	 * Returns the undo action so that it can be added to other parts of the GUI
	 * @return
	 */
	public QAction getUndoAction (){
		return this.undo;
	}
	/**
	 * Returns the redo action so that it can be added to other parts of the GUI
	 * @return
	 */
	public QAction getRedoAction(){
		return this.redo; 
	}
	/**
	 * Updates the tool tip for the redo button to reflect the latest action <br>
	 * that can be redone
	 */
	public void redoChanged(){
		redo.setToolTip("Redo " + undoStack.redoText());
	}
	/**
	 * Updates the tool tip for the undo button to reflect the latest action <br>
	 * that can be undone
	 */
	public void undoChanged(){
		undo.setToolTip("Undo " + undoStack.undoText());
	}
	
	@SuppressWarnings("unused")
	private void closeSite(){
		StandardButton result = parent.askUserToSave();
		
		if(result == StandardButton.Yes){
			parent.closeSite(true);
		}else if (result == StandardButton.No){
			parent.closeSite(false);
		}
	}
}