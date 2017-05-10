package edu.byu.ece.rapidSmith.device.vsrt.gui;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.CursorShape;
import com.trolltech.qt.core.Qt.DockWidgetArea;
import com.trolltech.qt.core.Qt.WindowModality;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QCloseEvent;
import com.trolltech.qt.gui.QCursor;
import com.trolltech.qt.gui.QDesktopWidget;
import com.trolltech.qt.gui.QDockWidget;
import com.trolltech.qt.gui.QHBoxLayout;
import com.trolltech.qt.gui.QKeyEvent;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QMessageBox;
import com.trolltech.qt.gui.QProgressDialog;
import com.trolltech.qt.gui.QMessageBox.StandardButton;
import com.trolltech.qt.gui.QMessageBox.StandardButtons;

import com.trolltech.qt.gui.QSizePolicy;
import com.trolltech.qt.gui.QStatusBar;
import com.trolltech.qt.gui.QStyleFactory;
import com.trolltech.qt.gui.QTabWidget;
import com.trolltech.qt.gui.QUndoStack;
import com.trolltech.qt.gui.QVBoxLayout;
import com.trolltech.qt.gui.QDockWidget.DockWidgetFeature;
import com.trolltech.qt.gui.QLayout.SizeConstraint;
import com.trolltech.qt.gui.QWidget;

import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.*;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.*;

/** 
 * This GUI is designed to allow users to easily generate site level connections for
 * Xilinx FPGA primitive sites.  This information cannot be extracted from Vivado's TCL interface
 * and so this GUI was created in order to address that problem. The ultimate goal is to create
 * the primitive def section of XDLRC files   
 */
public class VSRTool extends QMainWindow {

	/**GUI components*/
	private MenuBar menu_bar;
	private ToolBar	tool_bar;
	private ElementTab element_view ;
	private SiteSelectTab site_select;
	private QTabWidget left_tab;
	private QLabel statusLabel;
	private QStatusBar statusBar;
	private QWidget centralWidget;
	private PrimitiveSiteView view;
	private PrimitiveSiteScene scene;
	private QDockWidget navigator; 
	private Site siteShape;
	private ConfigDialog configDialog;

	/**Back end components*/
	private XDLRCPrimitiveDefsParser parser;
	private PrimitiveDef current_site;
	private String currentFamily;
	private String directory = null;
	private ArrayList<String> siteCfgElements = new ArrayList<String>(); 
	private QUndoStack undoStack = new QUndoStack();  
	private XMLCommands xml = new XMLCommands();
	private static String vsrtImagePath;
	public static boolean singleBelMode;
	private HashMap<String, HashSet<String>> sites2arch = new HashMap<String, HashSet<String>>();
	private AddedBelMap addedBelMap;
	
	/********************************************
	 **		 	Initialization Methods 	       **
	 ********************************************/
	// static initializer
	static
	{
		vsrtImagePath = "classpath:images" + File.separator + "vsrt";
		singleBelMode = false;
	}
	
	/**
	 * Constructor
	 * @throws missingPathException 
	 */
	public VSRTool(String dir) throws NullPointerException { 
		super.setWindowTitle("Vivado Subsite Routing Tool");
		//super.setWindowIcon(new QIcon(VSRTool.getImagePath(byuLogo.gif")));
			
		QApplication.setStyle(QStyleFactory.create("Cleanlooks"));
		
		this.initSize();
		this.initUI();
		this.openDirectory(dir);
		this.addedBelMap = new AddedBelMap(dir);
	}
	/**
	 * Initializes the size and position of the GUI based on the screen size of the device <br>
	 * and the number of screens
	 */
	private void initSize() {
		//Finding the number of screens on the device and placing the GUI in the middle of the 1st screen
		int num_screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
		QDesktopWidget qdw = new QDesktopWidget();

		int totalWidth = qdw.width();
		int screenWidth = totalWidth/num_screens;
		int screenHeight = qdw.height();

		int default_width  = (int) (.8*screenWidth);
		int default_height = (int) (.8*screenHeight); 
		int default_x = 0;
		int default_y = 0;
		
		if ( num_screens % 2 == 0  ) { //two or four screens
			default_x = (totalWidth - 2*default_width) / 4;
		} 
		else { //one or three screens
			default_x = (totalWidth - default_width) / 2;
		}
		default_y = (screenHeight - default_height) / 2;
		
		resize(default_width, default_height);
		move(default_x, default_y);
	}
	/**
	 * Initializes the GUI with all of the widgets that it needs
	 */
	private void initUI() {
		
		site_select = new SiteSelectTab(this);
		left_tab = new QTabWidget(this);
		statusLabel = new QLabel("Status Bar");
		statusBar = new QStatusBar();
		centralWidget = new QWidget();
		configDialog = new ConfigDialog(this);
		
		this.initMenuBar();
		this.initNavigator();
		this.initCenterWidget();
		this.initStatusBar();
	}
	
	/**
	 * Initializes the navigator found on the left side of the GUI
	 */
	private void initNavigator(){
		scene = new PrimitiveSiteScene(this, this.undoStack);
		this.element_view = new ElementTab(this, undoStack, scene);
		
		this.left_tab.addTab(site_select, "Select Site");
		this.left_tab.addTab(element_view, "Elements");
		this.left_tab.setEnabled(false);
		
		//adding the tab menu to the left side of the GUI 
		navigator = new QDockWidget(tr("Navigator:"), this);
		navigator.setWidget(left_tab);
		navigator.setFeatures(DockWidgetFeature.NoDockWidgetFeatures);
		navigator.setAllowedAreas(DockWidgetArea.LeftDockWidgetArea);
		addDockWidget(DockWidgetArea.LeftDockWidgetArea, navigator);
	}
	/**
	 * Initializes the central widget of the GUI which includes a vertical toolbar, <br>
	 * QGraphicsScene, and QGraphicsView.
	 */
	private void initCenterWidget(){
		view = new PrimitiveSiteView(this, scene);
		tool_bar = new ToolBar(this, this.menu_bar.getRedoAction(), view, scene, undoStack);

		//setting the Device View to be the center widget 
		QWidget tmp = new QWidget();
		QHBoxLayout hbox = new QHBoxLayout(tmp);
		hbox.setSizeConstraint(SizeConstraint.SetMaximumSize);
		hbox.addSpacing(5);
		hbox.addWidget(tool_bar);
		centralWidget.setSizePolicy(new QSizePolicy(QSizePolicy.Policy.Expanding, QSizePolicy.Policy.Expanding));
		hbox.addWidget(centralWidget);
		hbox.setSpacing(0);
		hbox.setContentsMargins(0, 0, 0, 0);
		setCentralWidget(tmp);
		
		QVBoxLayout vbox = new QVBoxLayout(centralWidget);
		vbox.addWidget(view); 
	}
	/**
	 * Creates a new menu bar and adds it to the GUI
	 */
	private void initMenuBar(){
		this.menu_bar = new MenuBar(this, undoStack);
		this.setMenuBar(menu_bar);
	}
	/**
	 * Creates a new status bar and adds it to the GUI
	 */
	private void initStatusBar(){
		//Status Bar -- update if needs be
		statusLabel.setText("Vsrtool");
		statusBar.addWidget(statusLabel);
		setStatusBar(statusBar);
	}
	/**
	 * 
	 */
	private void initSite2ArchMap(){
		this.sites2arch.clear();
		
		FilenameFilter defFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if(name.endsWith(".def"))
					return true;
				else {	return false; }
			}	
		};
		
		for ( File archDir : new File(this.directory).listFiles() ) {
			if ( archDir.isDirectory() ) {
				for (File primDef : archDir.listFiles(defFilter)) {
					String name = primDef.getName().substring(0, primDef.getName().lastIndexOf(".")); 
					
					try {
						this.sites2arch.get(name).add(archDir.getName());
					} catch ( NullPointerException e ) {
						HashSet<String> archNames = new HashSet<String>();
						archNames.add( archDir.getName() );
						this.sites2arch.put(name, archNames);
					}
				}
			}
		}
	}
	/****************************************************
	 **		 	Primitive Definition Methods 	       **
	 ****************************************************/
//	/***
//	 * This method reads in the directory that the user last had open (found within the "lastDirectory" text file) <br> 
//	 *  and loads the contents of the directory into the GUI.  If the text file does not yet exist, <br>
//	 *  this function does nothing.
//	 */
//	private void importDirectory(){
//		BufferedReader reader;
//		
//		try {
//			reader = Files.newBufferedReader(Paths.get("lastDirectory.txt"), Charset.defaultCharset());
//			this.directory = reader.readLine();
//			this.loadArchitecturesAndSites(directory);
//			this.initSite2ArchMap();
//			reader.close();
//		} catch (Exception e) {
//			this.directory = null;
//		}
//	}

	/**
	 * This function is called when the "Import" button has been clicked on the menu bar. <br>
	 * It opens a QFileDialog and allows the user to choose the appropriate directory that <br>
	 * contains the family/architecture folders and primitive def files within them. 
	 */
	public void openDirectory(String dir) throws NullPointerException {
		this.directory = dir;
	
		this.loadArchitecturesAndSites(directory);
		
		//Create save directory structure
		File complete = new File(this.directory + File.separator + "CompletePrimitiveDefinitions");
		if( !complete.exists() ) {
			complete.mkdir();
			for (String arch : this.site_select.getAvailableFamilies()) 
				new File(complete.getAbsolutePath() + File.separator + arch ).mkdir();
		}
		
		this.initSite2ArchMap();
	}
	
	/**
	 * This method creates a new PrimitiveDefParser and parses the .def file that was 
	 * selected by the user into a PrimitiveDef data structure.  This data structure is
	 * then added to the elementTab to display the available bels, site pins, and site pips
	 * to the user.  Also, the site and site pins are added to the device graphics view.      
	 * @param family Family/Architecture name of the primitive site. 
	 * @param site_name Name of the primitive site name. 
	 */
	public void get_primitive_def (String family, String site_name) {
		
		parser = new XDLRCPrimitiveDefsParser();

		parser.parseXDLRCFile(this.directory + File.separator  + family + File.separator + site_name + ".def");

		//we know that there is only going to be one primitive site
		current_site = parser.getPrimitiveDefs().get(0);
		currentFamily = family;
		
		singleBelMode = openInSingleBelMode(current_site);
		
		boolean saveFileExists = false;
		if (!singleBelMode) {
			saveFileExists = this.xml.parseSceneXML(this.directory + File.separator  + family + File.separator + site_name + ".xml");
		}

		//extracting the elements from the PrimitiveDef data structure and displaying its contents
		this.element_view.generate_elements(current_site, this.xml, saveFileExists);
		if (saveFileExists) {
			this.siteCfgElements = this.xml.loadSiteCfgElements();
		}
		
		this.element_view.setSingleBelMode(singleBelMode, scene);
		this.openSite(family, site_name, saveFileExists);
	}
	
	/**
	 * For sites that can be run in "Single BEL Mode", this function
	 * asks the user if they want to run VSRT in single BEL mode or
	 * in the regular mode.
	 *   
	 * @param def {@link PrimitiveDef} to open
	 * @return {@code true} if the user wants to run the tool in Single BEL Mode, 
	 * 			{@code false} otherwise
	 */
	private boolean openInSingleBelMode(PrimitiveDef def) {
		
		// is the primitive def does not represent a single BEL site, return false 
		if (!def.isSingleBelSite()) {
			return false;
		}
		
		// Otherwise create a new dialog box and ask the user if they want to run the GUI in "Single Bel Mode"
		QMessageBox singleBelMode = new QMessageBox();
		singleBelMode.setWindowTitle("Open Single Bel Mode?");
		singleBelMode.setText("The Primitive Site you selected has only one main bel. "
				+ "This means many of the connections have been inferred in Vivado for this site. "
				+ "Do you want to open the site in single bel mode?");
		
		singleBelMode.setStandardButtons(QMessageBox.StandardButton.Yes, QMessageBox.StandardButton.No);
		
		return singleBelMode.exec() == QMessageBox.StandardButton.Yes.value();	
	}
	
	/**
	 * 
	 * @param family
	 * @param site_name
	 */
	private void openSite(String family, String site_name, boolean siteSaved){
		this.setCursor(new QCursor( CursorShape.WaitCursor) );
		//enabling/disabling parts of the GUI once a site has been selected
		this.tool_bar.enableConfigs(true);
		this.left_tab.setCurrentIndex(1);
		this.site_select.setEnabled(false);
		this.view.setEnabled(true);
		this.menu_bar.enableShowConfigs(true);
		this.statusLabel.setText("Current Site -- " + family + ": " + site_name);
		this.tool_bar.setEnabled(true);

		siteShape = new Site(element_view.getSitePins(), scene, this.xml, siteSaved, this.undoStack);
				
		if (siteSaved)
			this.xml.loadWires(scene);

		view.zoomToBestFit();
		this.setCursor(new QCursor( CursorShape.ArrowCursor) );
	}
	/**
	 * This method requests a save file location from the user <br>
	 * and writes the complete primitive def file to that location. 
	 * 
	 * @param generateAutomatically True if the applications should automatically generate the site wire connections
	 * @param singleElementName The element name used if the generateAutomatically argument is true
	 */
	private void writePrimitiveDef(boolean generateAutomatically, boolean guiUsed){
	
		if ( (generateAutomatically ? true : this.element_view.testConnections() ) ){
			
			//Let the user select which architectures they want to save the primitive definition to
			ChooseArchitecturesDialog test = new ChooseArchitecturesDialog(this.sites2arch.get(this.site_select.currentSite()), this.site_select.currentFamily());
			test.exec();

			//Generate the connections for the primitive definition
			if ( generateAutomatically )
				this.current_site.generateConnectionsAutomatically();
			else 
				this.element_view.generateConnections();
			
			this.current_site.addSiteCfgOptions(siteCfgElements);
			
			//Print each complete primitive def file and xml save file to the appropriate location for each architecture
			for (String archName : test.getCheckedArchitectures()) {
				String saveFile = this.directory + File.separator + "CompletePrimitiveDefinitions" + File.separator + archName 
								+ File.separator + this.site_select.currentSite().replace("_ALTERNATE", "") + ".def";
				
				String xmlFile = this.directory + File.separator + archName + File.separator + 
								 this.site_select.currentSite() + ".xml";
				
				try {
					BufferedWriter writer = Files.newBufferedWriter(Paths.get(saveFile), 
							Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
					
					//Print the new primitive def file 
					writer.write(this.current_site.toString(false));
					writer.flush();
					
					//Save the file
					this.saveSiteProgress(xmlFile, this.directory + File.separator + archName + File.separator + this.site_select.currentSite() + ".def" );
					//Save the primitive sites as completed
					this.site_select.addCompletedSite( archName, this.site_select.currentSite() );
					
					writer.close();

				
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			this.closeSite(false);
		}
		else {
			QMessageBox.critical(this, "Unconnected Pins!", "Unconnected pins have been detected.  Make sure to either connect each pin, or mark any unused pins as unconnected.");
		}
	}
	
	/**
	 * This method searches through the primitive sites generated from Vivado, and creates the intrasite connections <br> 
	 * for them if they only have one bel.
	 */
	public void generateAllOneBelConnections(){
		if (this.directory != null ) {
			
			FilenameFilter defFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if(name.endsWith(".def"))
						return true;
					else {	return false; }
				}	
			};
			String fileLocation = this.directory + File.separator + "CompletePrimitiveDefinitions";
		
			//Count how many .def files there are
			int fileCount = 0;
			ArrayList<File> archFiles = new ArrayList<File>();
			
			for (File archDir : new File(this.directory).listFiles()) {
				if (archDir.isDirectory()) {
					fileCount += archDir.listFiles(defFilter).length;
					archFiles.add(archDir);
				}
			}
			//displays progress to user
			QProgressDialog progress = new QProgressDialog("Generating Connections...", "Cancel", 0, fileCount, this);
			progress.setWindowModality(WindowModality.WindowModal);
			progress.show();
			
			this.setCursor(new QCursor( CursorShape.WaitCursor) );
			
			for (File archDir : archFiles) {
				if (progress.wasCanceled())
					break;
				
				for (File primDef : archDir.listFiles(defFilter)) {
					progress.setValue(progress.value() + 1);
					this.parser = new XDLRCPrimitiveDefsParser();
					parser.parseXDLRCFile(primDef.getAbsolutePath());
					this.current_site = parser.getPrimitiveDefs().get(0); 
					
					if( current_site.belCount() == 1 ) { //&& !primDef.getName().endsWith("_ALTERNATE.def")){
						this.writePrimitiveDefOneBel(fileLocation, archDir.getName(), primDef.getName().substring(0, primDef.getName().lastIndexOf(".")));
						this.site_select.addCompletedSite(archDir.getName(), primDef.getName().substring(0, primDef.getName().lastIndexOf(".")));
					}
				}
			}
			this.site_select.load_new_family();
			this.setCursor(new QCursor( CursorShape.ArrowCursor ) );
			
			progress.close();
		}
	}
	/**
	 * 
	 * @param family
	 * @param site_name
	 */
	public void writePrimitiveDefOneBel(String location, String family, String site_name){
		try {
			String writeLocation = location + File.separator + family  + File.separator + site_name.replace("_ALTERNATE", "") +".def";  
			
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(writeLocation), Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

			this.current_site.generateConnectionsAutomatically();
			
			writer.write(this.current_site.toString(false));
			writer.flush();
			writer.close();
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Saves the current graphics scene information to an XML file
	 */
	private void saveSiteProgress(String xmlLocation, String oldFileLocation){

		if (!VSRTool.singleBelMode) {
			this.xml.saveGraphicsScene(scene, xmlLocation, this.siteCfgElements  );
		}
		try {
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(oldFileLocation), 
					Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			
					writer.write(this.current_site.toString(true));
					writer.flush();
					writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * This method clears the previous primitive site information <br>
	 * from the elementTab and PrimitiveSite graphics scene so that the <br>
	 * user can generate connections for another primitive site. 
	 */
	public void closeSite(boolean saveProgress){
		if (saveProgress) {
			if (VSRTool.singleBelMode) {
				this.element_view.generateConnections();
			}
			this.saveSiteProgress(this.directory + File.separator + this.site_select.getXMLLocation(), 
								  this.directory + File.separator + this.site_select.getSiteLocation());
		}
		
		singleBelMode = false;
		this.element_view.setSingleBelMode(false, scene);
		this.scene.resetScene();
		this.element_view.clear_all();
		this.left_tab.setCurrentIndex(0);
		this.statusLabel.setText("VXDL Tool");
		this.view.setEnabled(false);
		this.menu_bar.enableShowConfigs(false);
		this.site_select.setEnabled(true);
		this.tool_bar.enableConfigs(false);
		this.undoStack.clear();
		this.tool_bar.setEnabled(false);
		this.siteCfgElements.clear();
	}
	/**
	 * 	
	 */
	public void generateRemainingConnections (){
		if (!this.scene.hasConnections() ){ 
			this.writePrimitiveDef(true, true);
		}
		else {
			QMessageBox.critical(this, "Connections Exist!", "In order to generate connections automatically, "
														   + "make sure to delete any connections that have been drawn.");
		}
	}
	/**
	 * 
	 * @param directory
	 */
	private void loadArchitecturesAndSites(String directory) throws NullPointerException{
		this.left_tab.setEnabled(true);
		this.site_select.loadArchitecturesAndSites(directory);
	}
	
	/**
	 * Writes the completed primitive def file to a place where the user chooses <br>
	 * and resets the application so the user can generate connections for another <br>
	 * primitive site.
	 */
	public void savePrimitiveSite(){
		if ( this.view.isEnabled() ) { 
			this.writePrimitiveDef(false, true);
		}
	}
	
	/**
	 * Shows message dialog asking if the user would like to save their progress. <br>
	 * The answer to this question is returned.
	 * @return
	 */
	public StandardButton askUserToSave(){
		StandardButtons buttons = new StandardButtons();
		buttons.set(StandardButton.Yes, StandardButton.No, StandardButton.Cancel);
		
		return QMessageBox.question(this, "Save before closing", 
				"Would you like to save your progress before closing?", buttons)  ;
	}
	/********************************************
	 **		 	Config Dialog Methods 	       **
	 ********************************************/
	
	/**
	 * Displays the configuration option dialog for the element argument  
	 * @param element The Bel to display configuration options for
	 */
	public void showBelConfigOptions(Element element){
		
		this.configDialog.displayBelCfgOptions((element != null) ? element : 
											((QTreeElement)this.element_view.getbels().currentItem()).getElement());
		this.configDialog.exec();
	}
	
	/**
	 * Displays the configuration option dialog for the current Primitive site
	 */
	public void showSiteConfigDialog(){
		this.configDialog.displaySiteCfgOptions(this.siteCfgElements);
		this.configDialog.exec();	
	}
	
	
	/**
	 * Add a new site level configuration option
	 * @param cfgElement
	 */
	public void addSiteConfigElement(String cfgElement){
		this.siteCfgElements.add(cfgElement);
	}
	
	/**
	 * Remove a site level configuration option
	 * @param index
	 */
	public void removeSiteConfigElement(int index){
		this.siteCfgElements.remove(index);
	}
	public void clearSiteConfigOptions(){
		this.siteCfgElements.clear();
	}
	
	/********************************************
	 **		 		Getters/Setters 	       **
	 ********************************************/
	public static String getImagePath(String image){
		return vsrtImagePath + File.separator + image;
	}
	
	/**
	 * Returns the position on the view that a new item should be placed
	 * @return
	 */
	public QPointF getPlacementPosition(){
		return this.view.mapToScene( this.centralWidget.pos() );
	}
	/**
	 * Returns the currently opened Primitive Def data structure
	 */
	public PrimitiveDef getCurrentSite(){
		return this.current_site;
	}
	/**
	 * Returns the graphics element representing a primitive site
	 */
	public Site getGraphicsSite(){
		return this.siteShape;
	}
	/**
	 * Returns the graphics scene
	 */
	public PrimitiveSiteScene getScene(){
		return this.scene; 
	}
	/**
	 * Returns the QWidget that holds all of the elements in the application
	 * @return
	 */
	public ElementTab getElementView(){
		return this.element_view;
	}
	/**
	 * Returns the tool bar of the main window
	 * @return
	 */
	public ToolBar getToolBar(){
		return this.tool_bar;
	}
	
	public QDockWidget getNavigator(){
		return this.navigator;
	}
	
	public void disableLeftTab(){
		this.left_tab.setEnabled(false);
	}
	
	/**
	 * Adds a new BEL to the "addedBels.txt" file 
	 * 
	 * @param bel Name of BEL to add
	 */
	public void registerAddedBel(String bel) {
		addedBelMap.add(currentFamily, current_site.getType(), bel);
	}
	
	/**
	 * Removes a BEL from the "addedBels.txt" file 
	 * 
	 * @param bel Name of BEL to remove
	 */
	public void removeAddedBel(String bel) {
		addedBelMap.remove(currentFamily, current_site.getType(), bel);
	}
	
	/********************************************
	 **		 Overridden event handlers         **
	 ********************************************/
	/**
	 * This method allows the user to save their progress before <br>
	 * exiting the application.
	 */
	@Override
	protected void closeEvent(QCloseEvent event){
		if ( this.view.isEnabled() ) { 			
			StandardButton result = this.askUserToSave();
			
			if (result == StandardButton.Yes){
				if (VSRTool.singleBelMode) {
					this.element_view.generateConnections();
				}
				this.saveSiteProgress(this.directory + File.separator + this.site_select.getXMLLocation(), 
						  			  this.directory + File.separator + this.site_select.getSiteLocation());
			}else if (result == StandardButton.Cancel){
				event.ignore();
			}
		}
		
		// Save the added bels to the file when the user exits
		try {
			addedBelMap.print();
		} catch(IOException e) {}
	}
	
	/**
	 * Overridden function called when a keyboard key is pressed. If the pressed key was the control key
	 * then zooming in/out via the mouse wheel will be enabled on the device view. 
	 * @param event QKeyEvent object that holds the information as to what key was pressed
	 */
	protected void keyPressEvent(QKeyEvent event) {

		if(event.key() == Qt.Key.Key_Control.value()) 
			view.set_shouldZoom(true); 
		else if (event.key() == Qt.Key.Key_Shift.value()){
			view.setShouldHScroll(true);
		}

		super.keyPressEvent(event);
	}

	/**
	 * Overridden function called when a keyboard key is released. If the released key was the control key
	 * then zooming in/out via the mouse wheel will be disabled on the device view. 
	 * @param event QKeyEvent object that holds the information as to what key was released
	 */
	protected void keyReleaseEvent(QKeyEvent event) {
		if(event.key() == Qt.Key.Key_Control.value())	
			view.set_shouldZoom(false);	
		else if (event.key() == Qt.Key.Key_Shift.value()){
			view.setShouldHScroll(false);
		}
		super.keyReleaseEvent(event);
	}
	
	/**
	 * Creates a new GUI, and displays it to the user.
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(args.length != 1){
			System.out.println("USAGE: java edu.byu.ece.rapidSmith.device.vsrt.gui.VSRTool <Import Directory>");
		}
		else if (!new File(args[0]).exists())
		{
			System.out.println("ERROR: Directory does not exist");
		}
		else { 
			try {
			// TODO Auto-generated method stub
			QApplication.setGraphicsSystem("raster");
			QApplication.initialize(args);
			VSRTool test = new VSRTool(args[0]);
			test.show();
			QApplication.exec(); 
			}
			catch(NullPointerException e){
				System.out.println("ERROR: No Primitive Definitions found in the specified directory!\n" + 
									"\tThis should be the directory generated from the 'extract_all_partial_primitive_defs' TCL script");
			}
		}
	}
	
	/* ***********************************
	 *        Nested Classes
	 ************************************/
	
	/**
	 * Nested class used to keep track of the BELs that have been added to VSRT
	 * sites. In Ultrascale, this will be GND and VCC BELs
	 */
	private class AddedBelMap {
		private Map<String, Map<String, Set<String>>> addedBelMap;
		private String vsrtDirectory;
				
		public AddedBelMap(String vsrtDirectory) {
			this.vsrtDirectory = vsrtDirectory;
			addedBelMap = new HashMap<String, Map<String, Set<String>>>();
			
			Path belPath = null;
			try {
				belPath = Paths.get(vsrtDirectory).resolve("addedBels.txt");
			} catch (InvalidPathException e ) {
				return;
			}
			
			// Parse the "addedBels.txt" file which is in the form: 
			//kintexu slicel bels 
			BufferedReader br;
			String line;
			
			try {
				br = new BufferedReader(new FileReader(belPath.toFile()));
				
				while ((line = br.readLine()) != null) {
					String[] toks = line.split("\\s+");
					assert (toks.length == 3);
					this.add(toks[0], toks[1], toks[2]);
				}
			}
			catch (IOException e ) {
				
			}			
		}
		
		/**
		 * Returns true if the specified BEL has been added in VSRT for the 
		 * specified family and site.
		 * 
		 * @param family Name of the family (i.e. artix7)
		 * @param site Name of the site (i.e. SLICEL)
		 * @param bel Name of the bel (i.e. HARD0GND)
		 * 
		 * @return {@code true} if the specified bel has already been added to the map. {@code false} otherwise
		 */
		@SuppressWarnings("unused")
		public boolean contains(String family, String site, String bel) {
			
			if (addedBelMap.containsKey(family.toLowerCase())){
				Set<String> bels = addedBelMap.get(family.toLowerCase()).getOrDefault(site.toUpperCase(), Collections.emptySet());
				return bels.contains(bel);
			}
			
			return false;
		}
		
		/**
		 * Adds the specified BEL (based on its current family and site) to
		 * the map. 
		 * 
		 * @param family Name of the family (i.e. artix7)
		 * @param site Name of the site (i.e. SLICEL)
		 * @param bel Name of the bel (i.e. HARD0GND)
		 */
		public void add(String family, String site, String bel) {
			getBelSet(getSiteMap(family.toLowerCase()), site.toUpperCase()).add(bel);
		}
		
		/**
		 * Removes the specified BEL (based on its current family and site) from
		 * the map. 
		 * 
		 * @param family Name of the family (i.e. artix7)
		 * @param site Name of the site (i.e. SLICEL)
		 * @param bel Name of the bel (i.e. HARD0GND)
		 */
		public void remove(String family, String site, String bel) {
			getBelSet(getSiteMap(family.toLowerCase()), site.toUpperCase()).remove(bel);
		}
		
		private Map<String, Set<String>> getSiteMap(String family) {
			
			if (addedBelMap.containsKey(family)) {
				return addedBelMap.get(family);
			} 
			else {
				Map<String, Set<String>> siteMap = new HashMap<String, Set<String>>();
				addedBelMap.put(family, siteMap);
				return siteMap;
			}
		}
		
		private Set<String> getBelSet(Map<String, Set<String>> siteMap, String site) {
			if (siteMap.containsKey(site)) {
				return siteMap.get(site);
			}
			else {
				Set<String> belSet = new HashSet<String>();
				siteMap.put(site, belSet);
				return belSet;
			}
		}
		
		/**
		 * Prints the addedBelMap to an "addedBel.txt" file in the current VSRT
		 * directory.
		 * 
		 * @throws IOException
		 */
		public void print() throws IOException {
			BufferedWriter fileout = new BufferedWriter (new FileWriter(Paths.get(vsrtDirectory).resolve("addedBels.txt").toString()));
			
			for (String family : addedBelMap.keySet()) {
				Map<String, Set<String>> siteMap = addedBelMap.get(family);
				for (String site : siteMap.keySet()) {
					for (String bel : siteMap.get(site)) {
						fileout.write(family + " " + site + " " + bel + "\n");
					}
				}
			}
			fileout.close();
		}
	}
	
}//end class