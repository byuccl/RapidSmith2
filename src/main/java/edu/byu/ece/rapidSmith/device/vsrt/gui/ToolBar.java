package edu.byu.ece.rapidSmith.device.vsrt.gui;

import com.trolltech.qt.core.Qt.Orientation;
import com.trolltech.qt.gui.QAction;
import com.trolltech.qt.gui.QGraphicsView;
import com.trolltech.qt.gui.QIcon;
import com.trolltech.qt.gui.QMenu;
import com.trolltech.qt.gui.QToolBar;
import com.trolltech.qt.gui.QToolButton;
import com.trolltech.qt.gui.QUndoStack;
import com.trolltech.qt.gui.QUndoView;
import com.trolltech.qt.gui.QWidgetAction;


/**
 * Toolbar that contains several different options for the GUI including: <br>
 * (1) Draw Wire <br>
 * (2) Delete Element <br>
 * (3) Drag Scene <br>
 * (4) Zoom In <br>
 * (5) Zoom Out (More to come)
 * @author Thomas Townsend
 * Created: Jun 10, 2014 5:05:54 PM
 */
public class ToolBar extends QToolBar{
	
	private VSRTool parent;

	private QAction save_site;
	/**Enables the ability to draw wires on the scene when activated*/
	private QAction draw_wire;
	/**Enables the ability to delete elements on the scene when activated*/
	private QAction delete;
	/**Enables the ability to drag the scene around when activated*/
	private QAction drag_mode; 
	/**Enables the ability to zoom in on the scene when activated*/
	private QAction zoom_in;
	/**Enables the ability to zoom out on the scene when activated*/
	private QAction zoom_out;
	/**Zoom selection option*/
	private QAction zoomToView; 
	/**Generate remaining connections option*/
	private QAction generateRemaining;
	/**View site config options*/
	private QAction viewSiteConfigOptions;
	/**View selected bel config options*/
	private QAction viewBelConfigOptions;
	/**Used to expand/hide the navigator*/
	private QAction toggleView;
	/***/
	private QAction rotateClockwise;
	/***/
	private QAction rotateCounter;
	/***/
	private QAction zoomBestFit;
	/***/
	private QToolButton undoMenu; 
	/***/
	private PrimitiveSiteScene scene;
	
	/**
	 * 
	 * @param parent
	 * @param redo
	 * @param view
	 * @param scene
	 * @param undoStack
	 */
	public ToolBar (VSRTool parent, QAction redo, QGraphicsView view, PrimitiveSiteScene scene, QUndoStack undoStack) {
		super("main toolbar", parent);
		
		this.parent = parent;
		this.scene = scene;
		
		this.initializeButtons(view, undoStack);
	    
	   this.layoutToolbar(redo);
	}
	
	private void initializeButtons(QGraphicsView view, QUndoStack undoStack){
		toggleView = new QAction(new QIcon(VSRTool.getImagePath().resolve("hide.png").toString()), "", this);
		toggleView.triggered.connect(this, "changeDockWidgetVisibility()");
		toggleView.setToolTip("Hide Navigator View");
		
	    save_site = new QAction(new QIcon(VSRTool.getImagePath().resolve("filesave.png").toString()), "Save Primitive Site", this );
	    save_site.triggered.connect(parent, "savePrimitiveSite()");
	 
	    draw_wire = new QAction(new QIcon(VSRTool.getImagePath().resolve("draw_wire.png").toString()), "Draw Wire (W)", this );
	    draw_wire.setCheckable(true);
	    draw_wire.toggled.connect(this, "draw_wire_toggled()");
	    draw_wire.setShortcut("w");
		    
	    delete = new QAction(new QIcon(VSRTool.getImagePath().resolve("editcut.png").toString()), "Remove", this );
	    delete.setCheckable(true);
	    delete.toggled.connect(this, "delete_toggled()" );
	    delete.setShortcut("d");
	    
	    drag_mode = new QAction(new QIcon(VSRTool.getImagePath().resolve("drag.gif").toString()), "Drag View", this);
	    drag_mode.setCheckable(true);
	    drag_mode.toggled.connect(this, "drag_toggled()");
	    drag_mode.setShortcut("Ctrl+d");
	    
	    zoom_in = new QAction(new QIcon(VSRTool.getImagePath().resolve("zoomin.png").toString()), "Zoom In", this);
	    zoom_in.triggered.connect(view, "zoom_in()");
	    zoom_in.setShortcut("Ctrl++");
	    
	    zoom_out = new QAction(new QIcon(VSRTool.getImagePath().resolve("zoomout.png").toString()), "Zoom Out", this);
	    zoom_out.triggered.connect(view, "zoom_out()");
	    zoom_out.setShortcut("Ctrl+-");
	    
	    zoomToView = new QAction(new QIcon(VSRTool.getImagePath().resolve("zoomselection.png").toString()), "Zoom To Selected", this);
	    zoomToView.toggled.connect(this, "zoomToViewToggled()");
	    zoomToView.setCheckable(true);
	    
	    zoomBestFit = new QAction(new QIcon(VSRTool.getImagePath().resolve("zoombestfit2.png").toString()), "Zoom Best Fit", this);
	    zoomBestFit.triggered.connect(view, "zoomToBestFit()");
	    
	    generateRemaining = new QAction(new QIcon(VSRTool.getImagePath().resolve("generateConnections.gif").toString()), tr("Generate Remaining Connections and save Primitive Def"), this);
	    generateRemaining.triggered.connect(parent, "generateRemainingConnections()");
	    generateRemaining.setEnabled(false);
	    
	    viewSiteConfigOptions = new QAction(new QIcon(VSRTool.getImagePath().resolve("site.gif").toString()), tr("View site configuration options"), this);
	    viewSiteConfigOptions.triggered.connect(parent, "showSiteConfigDialog()");
	    viewSiteConfigOptions.setEnabled(false);
	    
	    viewBelConfigOptions = new QAction(new QIcon(VSRTool.getImagePath().resolve("bel.png").toString()), tr("View selected bel configuration options"), this);
	    viewBelConfigOptions.triggered.connect(this, "showBelConfig()");
	    viewBelConfigOptions.setEnabled(false);
	    
	    rotateClockwise = new QAction (new QIcon(VSRTool.getImagePath().resolve("rotateClockwise.png").toString()), tr("Rotate the selected items clockwise by 90 degrees (Ctrl+R)"), this);
	    rotateClockwise.triggered.connect(scene, "rotateItemsClockwise()");
	    rotateClockwise.setShortcut("Ctrl+r");
	    
	    rotateCounter = new QAction (new QIcon(VSRTool.getImagePath().resolve("rotateCounterclockwise.png").toString()), tr("Rotate the selected items Counterclockwise by 90 degrees (Ctrl+Shift+R)"), this);
	    rotateCounter.triggered.connect(scene, "rotateItemsCounterclockwise()");
	    rotateCounter.setShortcut("Ctrl+Shift+r");
	     	 
	    //Creating a QUndoView so the user can undo/redo multiple things at a time
	    QMenu menu = new QMenu();
	    QUndoView list = new QUndoView(undoStack);
	    list.setEmptyLabel("<Initial State>");
	    list.setAlternatingRowColors(true);
	    QWidgetAction action = new QWidgetAction(menu);
	    action.setDefaultWidget(list);
	    menu.addAction(action);

	    undoMenu = new QToolButton();
	    undoMenu.setIcon(new QIcon(VSRTool.getImagePath().resolve("editundo.png").toString()));
	    undoMenu.setMenu(menu);
	    undoMenu.setPopupMode(QToolButton.ToolButtonPopupMode.MenuButtonPopup);
	    undoMenu.clicked.connect(undoStack, "undo()");
	    undoMenu.setEnabled(false);
	    
	    undoStack.canUndoChanged.connect(undoMenu, "setEnabled(boolean)");
	}
	
	private void layoutToolbar(QAction redo){
		 //Add each action to the menu bar
	    this.addAction(toggleView);
	    this.addAction(save_site);
	    this.addAction(generateRemaining);
	    this.addSeparator();  
	    this.addWidget(undoMenu);
	    this.addAction(redo);
	    this.addSeparator();
	    this.addAction(draw_wire);
	    this.addAction(delete);
	    this.addAction(rotateClockwise);
	    this.addAction(rotateCounter);
	    this.addAction(drag_mode);
	    this.addAction(zoom_in);
	    this.addAction(zoom_out);
	    this.addAction(zoomToView);
	    this.addAction(zoomBestFit);
	    this.addSeparator();
	    this.addAction(viewSiteConfigOptions);
	    this.addAction(viewBelConfigOptions);
	    
	    this.setOrientation(Orientation.Vertical);
	    this.setFloatable(true);
		this.setStyleSheet("QToolBar { border: 0px; icon-size:20px }");
		this.setEnabled(false);
	}
	
	/**
	 * Enables drawing wires on the scene, and disables any other functionality that may be checked    
	 */
	@SuppressWarnings("unused")
	private void draw_wire_toggled(){
		
		if( this.draw_wire.isChecked() ) {
			//will have to update this once I add more icons in the toolbar
			if(delete.isChecked())
				delete.setChecked(false);
			if(drag_mode.isChecked())
				drag_mode.setChecked(false);
			if (zoomToView.isChecked() )
				zoomToView.setChecked(false);
			
			scene.enable_draw_wire(true);
		}
		else
		{	scene.enable_draw_wire(false);		}
	}
	
	/**
	 * Enables deleting elements on the scene, and disables any other functionality that may be checked    
	 */
	@SuppressWarnings("unused")
	private void delete_toggled(){
		
		if( this.delete.isChecked() ) {
			//will have to update this once I add more icons in the toolbar
			if(draw_wire.isChecked())
				draw_wire.setChecked(false);
			if(drag_mode.isChecked())
				drag_mode.setChecked(false);
			if (zoomToView.isChecked() )
				zoomToView.setChecked(false);
				
			scene.enable_delete(true);
		}
		else
		{	scene.enable_delete(false);	}
	}
	
	/**
	 * Enables dragging elements on the scene, and disables any other functionality that may be checked    
	 */
	@SuppressWarnings("unused")
	private void drag_toggled() {
		
		if ( this.drag_mode.isChecked() ) {
			if(draw_wire.isChecked())
				draw_wire.setChecked(false);
			if(delete.isChecked())
				delete.setChecked(false);
			if (zoomToView.isChecked() )
				zoomToView.setChecked(false);
			
			this.scene.set_view_draggable(true);			
		}
		else
		{	this.scene.set_view_draggable(false);  }
	}

	@SuppressWarnings("unused")
	private void zoomToViewToggled() {
		
		if ( this.zoomToView.isChecked() ) {
			if(draw_wire.isChecked())
				draw_wire.setChecked(false);
			if(delete.isChecked())
				delete.setChecked(false);
			if (drag_mode.isChecked() )
				drag_mode.setChecked(false);
			
			
			this.scene.setZoomToView(true);		
		}
		else
		{	this.scene.setZoomToView(false);		}
	}
	/**
	 * Disables all of the options on the toolbar that may be currently checked  
	 */
	public void untoggleAll(){
	if(draw_wire.isChecked())
		draw_wire.setChecked(false);
	if(delete.isChecked())
		delete.setChecked(false);
	if(drag_mode.isChecked())
		drag_mode.setChecked(false);
	if (zoomToView.isChecked() )
		zoomToView.setChecked(false);
	}
	
	public void setGenerate(boolean canGenerate){
		this.generateRemaining.setEnabled(canGenerate);
	}
	public void enableConfigs(boolean enable){
		this.viewBelConfigOptions.setEnabled(enable);
		this.viewSiteConfigOptions.setEnabled(enable);
	}
	
	@SuppressWarnings("unused")
	private void showBelConfig(){
		parent.showBelConfigOptions(null);
	}
	/**
	 * This method hides and expands the navigator view
	 */
	@SuppressWarnings("unused")
	private void changeDockWidgetVisibility(){
		if ( parent.getNavigator().isHidden() ) {
			toggleView.setIcon(new QIcon(VSRTool.getImagePath().resolve("hide.png").toString()));	
			toggleView.setToolTip("Hide Navigator View");
			parent.getNavigator().setHidden(false);
		
		} else {
			toggleView.setIcon(new QIcon(VSRTool.getImagePath().resolve("expand.gif").toString()));
			toggleView.setToolTip("Expand Navigator View");
			parent.getNavigator().setHidden(true);
		}
	}
}