package edu.byu.ece.rapidSmith.device.vsrt.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands.AddSitePinGroupCommand;
import edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands.DeleteElementPinCommand;
import edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands.DeleteSitePinCommand;
import edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands.MarkPinUnconnectedCommand;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.*;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.Qt.ContextMenuPolicy;
import com.trolltech.qt.core.Qt.SortOrder;
import com.trolltech.qt.gui.QAbstractItemView.SelectionMode;
import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QContextMenuEvent;
import com.trolltech.qt.gui.QCursor;
import com.trolltech.qt.gui.QIcon;
import com.trolltech.qt.gui.QMenu;
import com.trolltech.qt.gui.QMessageBox;
import com.trolltech.qt.gui.QTreeWidget;
import com.trolltech.qt.gui.QTreeWidgetItem;
import com.trolltech.qt.gui.QUndoStack;
import com.trolltech.qt.gui.QVBoxLayout;
import com.trolltech.qt.gui.QWidget;

/**
 * Contains a QTreeWidget view of the Bels, Site Pins and Site Pips for the 
 * currently selected primitive site
 * @author Thomas Townsend
 * Created: Jun 10, 2014 4:47:45 PM
 */
public class ElementTab extends QWidget {

	private VSRTool parent;
	/**Holds all of the site pins of the currently selected primitive site*/
	private QTreeWidget pins;
	/**Holds all of the bels of the currently selected primitive site*/
	private QTreeWidget bels;
	/**Holds all of the site pips of the currently selected primitive site*/
	private QTreeWidget pips;
	/**Initial brush to make the color of all pins red (indicating they are unconnected)*/
	private QBrush text_brush = new QBrush(VsrtColor.red);
	/**Mapping Site Pins to their corresponding elements so that connections can be generated*/
	private HashMap <QTreePin, Element> pin2ElementMap = new HashMap<QTreePin, Element>();
//	/**Set of inverting pip names */
//	private HashSet <String> pipNames = new HashSet<String>();
//	/**True if connections should be automatically connected*/
//	private boolean shouldGenerate = false; 
	/**True if the mouse is currently under the pip tree */
	private boolean underPip = false;
	/**Undo stack to push commands onto*/
	private QUndoStack undoStack;
	
	
	/*************************************************************************************
	 *	Initialization -- methods used to initialize the element tab and its components  *
	 *************************************************************************************/
	/**
	 * Constructor: 
	 * @param parent
	 * @param undoStack
	 */
	public ElementTab(VSRTool parent, QUndoStack undoStack, PrimitiveSiteScene scene) {
		
		super(parent);
		this.parent = parent;
		this.undoStack = undoStack;
	
		// Create the tree of site pins
		pins = new QTreeWidget();
		pins.setColumnCount(1);
		pins.setStyleSheet("selection-background-color: blue");
		pins.setSelectionMode(SelectionMode.ExtendedSelection);
		pins.setAlternatingRowColors(true);
		
		QTreeWidgetItem pinHeader = new QTreeWidgetItem();
		pinHeader.setText(0, "  Site Pins");
		pinHeader.setIcon(0, new QIcon(VSRTool.getImagePath("add.gif")));
		pins.setHeaderItem(pinHeader);
		pins.header().setClickable(true);
		pins.header().sectionClicked.connect(this, "checkIconClick()");
		
		//create the tree of bel pins
		bels = new QTreeWidget();
		bels.setColumnCount(1);
		bels.setStyleSheet("selection-background-color: blue");
		bels.setHeaderLabel("Bels");
		bels.setAlternatingRowColors(true);
		bels.setContextMenuPolicy(ContextMenuPolicy.CustomContextMenu);
		bels.customContextMenuRequested.connect(this, "showDeleteElementPinMenu()");
	
		//create the tree of site pips
		pips = new QTreeWidget();
		pips.setColumnCount(1);
		pips.setStyleSheet("selection-background-color: blue");
		pips.setHeaderLabel("Site PIPs");
		pips.setAlternatingRowColors(true);
		pips.setContextMenuPolicy(ContextMenuPolicy.CustomContextMenu);
		pips.customContextMenuRequested.connect(this, "showDeleteElementPinMenu()");
	
		QVBoxLayout vbox = new QVBoxLayout(this);
		
		vbox.stretch(1);
		vbox.addWidget(bels);
		vbox.addWidget(pins);
		vbox.addWidget(pips);
		
		//These event handlers add a new element to the scene
		bels.itemDoubleClicked.connect(scene, "addElementToScene(QTreeWidgetItem)");
		pips.itemDoubleClicked.connect(scene, "addElementToScene(QTreeWidgetItem)");
		bels.itemClicked.connect(scene, "selectItemInScene(QTreeWidgetItem)");
		pips.itemClicked.connect(scene, "selectItemInScene(QTreeWidgetItem)");
		
		//Some additional settings for the tree
		bels.setExpandsOnDoubleClick(false);
		pips.setExpandsOnDoubleClick(false);
		pins.setExpandsOnDoubleClick(false);
	}
		
	/**
	 * Generates the bel, pin, and pip tree's with the bels, site pins, and site pips
	 * for the currently selected primitive site
	 * @param def Primitive Definition that contains all of the bel, pin, and pip info
	 */
	public String generate_elements(PrimitiveDef def, XMLCommands xml, boolean saveFileExists ) {
		
		this.clear_all();
		QTreeWidgetItem tmp;
				
		//Generate bel, site pin, and site pip info. 
		for (Element element : def.getElements()) {
			if( element.isBel() )
			{
				tmp = new QTreeElement(bels, element);
				tmp.setText(0, element.getName());
				tmp.setForeground(0, text_brush);
				
				if (saveFileExists) {
					QPointF savedLocation = xml.getSavedElementLocation(element.getName());
					if (savedLocation != null) { 
						parent.getScene().addSavedElementToScene(tmp, savedLocation, xml.getSavedElementRotation(element.getName()));
					}
				}
				
			}
			else if( element.isPin() ) {

				tmp = new QTreePin(pins, element.getPins().get(0), true);
				tmp.setText(0, element.getName());
				tmp.setForeground(0, text_brush);
				this.pin2ElementMap.put((QTreePin)tmp, element);
				
				//set location of the pins in the site constructor...
				if (saveFileExists) {
					if( xml.sitePinUnconnected(element.getName() ) ){
						element.getPins().get(0).setConnected(false);
						tmp.setForeground(0, new QBrush(VsrtColor.gray ) ); 
					}
				}
				
			}
			//exclude site pips that are route-through, because they are unneeded
			else if (!element.getName().startsWith("_ROUTE")) {
				tmp = new QTreeElement(pips, element); 
				tmp.setText(0, element.getName());	
				tmp.setForeground(0, text_brush);
				//this.pipNames.add(element.getName());
				
				if (saveFileExists) {
					QPointF savedLocation = xml.getSavedElementLocation(element.getName());
					
					if (savedLocation != null) {
						parent.getScene().addSavedElementToScene(tmp, savedLocation, xml.getSavedElementRotation(element.getName()) );
					}
				}
			}
		}
		//sort each of the lists in alphabetical order
		pins.sortItems(0, SortOrder.AscendingOrder);  
		bels.sortItems(0, SortOrder.AscendingOrder);
		pips.sortItems(0, SortOrder.AscendingOrder);
		
		//If the primitive site only has one bel and no site pips then we can assume that 
		//site pin names and bel pin names will match, and so we can automatically generate
		//the connections if the user chooses to. 
		if ( bels.topLevelItemCount() == 1 ){ //pips.topLevelItemCount() == 0 && 
			
			QMessageBox generateConnections = new QMessageBox();
			generateConnections.setWindowTitle("Generate Connections Automatically?");
			generateConnections.setText("The Primitive Site you selcted has only one bel. "
						+ "This means we can infer the connections of this primitive site for you! "
						+ "Would you like us to automatically generate these connections?");
			generateConnections.setStandardButtons(QMessageBox.StandardButton.Yes, QMessageBox.StandardButton.No);
		
			
			if ( generateConnections.exec() == QMessageBox.StandardButton.Yes.value() )
				return this.bels.topLevelItem(0).text(0);
		}
		
		return null;
	}

	/********************************
	 *	General -- General methods  *
	 ********************************/
	/**
	 * Removes all elements of each list in the element tab
	 */
	public void clear_all() {
		this.pins.clear();
		this.bels.clear();
		this.pips.clear();
		this.pin2ElementMap.clear();
		//this.shouldGenerate = false;
	}
	/**
	 * @return The site pin list
	 */
	public QTreeWidget getpins() {
		return pins;
	}
	/**
	 * @return The site bel list
	 */
	public QTreeWidget getbels() {
		return bels;
	}
	/**
	 * @return The site pip list
	 */
	public QTreeWidget getpips() {
		return pips;
	}
	
//	/**
//	 * Sets the shouldGenerate variable.  If this variable is true, then connections will be automatically <br>
//	 * generated when the primitive site is written to file.
//	 * @param generate
//	 */
//	public final void setGenerateRemaining(boolean generate){
//		//this.shouldGenerate = generate; 
//	}
	
	/**
	 * Returns the Element that corresponds to the passed in QTreePin
	 * @param pin
	 * @return
	 */
	public Element getPinElement(QTreePin pin){
		return this.pin2ElementMap.get(pin);
	}

	/**
	 * 
	 */
	public void deselectAll(){
		for (QTreeWidgetItem item : this.pips.selectedItems()) 
			item.setSelected(false);
		
		for (QTreeWidgetItem item : this.bels.selectedItems()) 
			item.setSelected(false);
		
		for (QTreeWidgetItem item : this.pins.selectedItems()) 
			item.setSelected(false);
	}
	
	/********************************************************************************
	 *	 Connection Generation -- Methods associated with primitive def generation  *
	 ********************************************************************************/
	/**
	 * Extracts the drawn connections from the PrimitiveSiteScene, and adds them to their <br>
	 * corresponding elements within the PrimitiveDef data structure.
	 */
	public void generateConnections() {
		this.generateBelConnections();
		this.generatePipConnections();
		this.generatePinConnections(); 
	}
	
	/**
	 * Extracts the drawn connections for each bel in the GUI, and adds them to the PrimitiveDef <br>
	 * data structure.  If there is only one bel, these connections are inferred if "shouldGenerate" is true. 
	 */
	private void generateBelConnections(){
	//generate all of the bel connections
		for (int i = 0; i < this.bels.topLevelItemCount(); i++ ){
			for (QTreeWidgetItem pin : this.bels.topLevelItem(i).takeChildren()) {
				ArrayList<QTreeWidgetItem> conns = (ArrayList<QTreeWidgetItem>) pin.takeChildren();
						
				for (QTreeWidgetItem connection : conns){
				
					String[] tmp = connection.text(0).split(" ");// This string has the form "==> A5LUT A1"
					Connection conn = new Connection();
					
					conn.setElement0(this.bels.topLevelItem(i).text(0));
					conn.setElement1(tmp[1]);
					conn.setForwardConnection(tmp[0].equals("==>") ? true : false);
					conn.setPin0(pin.text(0));
					conn.setPin1(tmp[2]);
					
					((QTreeElement)this.bels.topLevelItem(i)).getElement().addConnection(conn);
				}
			}
		}
	}
	/**
	 * Extracts the drawn connections for each pip in the GUI, and adds them to the PrimitiveDef <br>
	 * data structure.  If there is only one bel, these connections are inferred if "shouldGenerate" is true. 
	 */
	private void generatePipConnections(){
		//generate all of the pip connections
		for (int i = 0; i < this.pips.topLevelItemCount(); i++ ){
			for (QTreeWidgetItem pin : this.pips.topLevelItem(i).takeChildren()) {
				ArrayList<QTreeWidgetItem> conns = (ArrayList<QTreeWidgetItem>) pin.takeChildren();
				
				for (QTreeWidgetItem connection : conns){
				
					String[] tmp = connection.text(0).split(" ");// This string has the form "==> A5LUT A1"
					Connection conn = new Connection();
					conn.setElement0(this.pips.topLevelItem(i).text(0));
					conn.setElement1(tmp[1]);
					conn.setForwardConnection(tmp[0].equals("==>") ? true : false);
					conn.setPin0(pin.text(0));
					conn.setPin1(tmp[2]);
					
					((QTreeElement)this.pips.topLevelItem(i)).getElement().addConnection(conn);
				}				
			}
		}
	}
	/**
	 * Extracts the drawn connections for each site pin in the GUI, and adds them to the PrimitiveDef <br>
	 * data structure.  If there is only one bel, these connections are inferred if "shouldGenerate" is true. 
	 */
	private void generatePinConnections(){
		//generate all of the pin connections
		for (int i = 0; i < this.pins.topLevelItemCount(); i++ ){
			QTreePin pin = (QTreePin)pins.topLevelItem(i);
			
			//only add connections if the pin is connected
			if ( pin.getPin().isConnected() ) {
				ArrayList<QTreeWidgetItem> conns = (ArrayList<QTreeWidgetItem>)pin.takeChildren();
				
				for (QTreeWidgetItem connection : conns) {
					String[] tmp = connection.text(0).split(" ");
					Connection conn = new Connection();
					
					conn.setElement0(pin.text(0));
					conn.setElement1(tmp[1]);
					conn.setForwardConnection(tmp[0].equals("==>") ? true : false);
					conn.setPin0(pin.text(0));
					conn.setPin1(tmp[2]);
					
					this.pin2ElementMap.get(pin).addConnection(conn);
				}
			}	
		}
	}
	
	/**
	 * Returns false if there are any unconnected pins on the GUI
	 * @return
	 */
	public boolean testConnections(){
	
		for (int i = 0; i < this.bels.topLevelItemCount(); i++ )
			for (int j = 0; j< this.bels.topLevelItem(i).childCount(); j++ )
				if ( this.bels.topLevelItem(i).child(j).foreground(0).color().value() == VsrtColor.red.value())
					return false;
			
		for (int i = 0; i < this.bels.topLevelItemCount(); i++ )
			if ( this.bels.topLevelItem(i).foreground(0).color().value() == VsrtColor.red.value())
				return false;
		
		
		for (int i = 0; i < this.pips.topLevelItemCount(); i++ )
			for (int j = 0; j < this.pips.topLevelItem(i).childCount(); j++ )
				if ( this.pips.topLevelItem(i).child(j).foreground(0).color().value() == VsrtColor.red.value())
					return false;
					
		return true;
	}
	/******************************************************************
	 *	 Element Pin Section -- Methods associated with element pins  *
	 ******************************************************************/
	/**
	 * Displays a popup menu when a bel or pip pin has been right clicked on
	 */
	public void showDeleteElementPinMenu(){
		this.underPip = pips.underMouse();
		
		if ((underPip ? pips : bels ).selectedItems().get(0).parent() instanceof QTreeElement ) {
			QMenu popupMenu = new QMenu();
			popupMenu.addAction(new QIcon("images/trash.png"), "Delete Element Pin", this, "deleteElementPin()");
			popupMenu.popup(QCursor.pos());		
		}
	}
	
	/**
	 * Deletes the element pin from the 
	 * 1.) Primitive def data structure
	 * 2.) bel/pip tree view and
	 * 3.) Graphics view
	 */
	@SuppressWarnings("unused")
	private void deleteElementPin(){
		try { 
			//get the pin to delete and the element of that pin
			QTreeElement parent = (QTreeElement) (underPip ? pips : bels ).selectedItems().get(0).parent();
			QTreePin pin = (QTreePin) (underPip ? pips : bels ).selectedItems().get(0);
			
			DeleteElementPinCommand deleteElement = new DeleteElementPinCommand(parent, pin, this.parent.getScene());
			this.undoStack.push(deleteElement);
		}
		catch(Exception e) {
		}	
	}
	
	/*************************************************************
	 *	 Site Pin Section -- Methods associated with site pins   *
	 *************************************************************/
	/**
	 * Creates a new QTreePin based on the element passed into the method, <br>
	 * but does not yet add it to the site pin tree
	 * @param e
	 * @return
	 */
	public QTreePin createNewSitePin(Element e){
		QTreePin treePin = new QTreePin(pins, e.getPins().get(0), true);
		treePin.setText(0, e.getName() );
		treePin.setForeground(0, text_brush);
		pins.takeTopLevelItem(pins.topLevelItemCount()-1);
		return treePin;
	}
	/**
	 * Adds the pin passed into the method to the site pin tree, and <br>
	 * the pin2Element hash map.  
	 * @param pin
	 * @param e
	 */
	public void addSitePinToTree(QTreePin pin, Element e){
		pins.addTopLevelItem(pin);
		pin2ElementMap.put(pin, e);
		pins.sortItems(0, SortOrder.AscendingOrder); 
	}
	/**
	 * Removes the pin passed into the method from the site pin tree, and <br>
	 * the pin2Element hash map;
	 * @param pin
	 */
	public void removeSitePinFromTree(QTreePin pin){
		pin.remove_allWires();
		pin2ElementMap.remove(pin);
		pins.takeTopLevelItem( pins.indexOfTopLevelItem(pin) );
	}
	
	/**
	 * Deletes site pins currently selected in the site pin tree view. 
	 */
	@SuppressWarnings("unused")
	private void deleteSitePins(){
		
		DeleteSitePinCommand deleteSitePins = new DeleteSitePinCommand((ArrayList<QTreeWidgetItem>) pins.selectedItems(), 
													parent.getCurrentSite(), parent.getGraphicsSite(),this);
		this.undoStack.push(deleteSitePins);
	}
	
	/**
	 * Marks the selected site pins as unconnected
	 */
	@SuppressWarnings("unused")
	private void markSitePinsUnconnected(){
		MarkPinUnconnectedCommand unconnect = new MarkPinUnconnectedCommand(parent.getScene(), (ArrayList<QTreeWidgetItem>) pins.selectedItems());
		this.undoStack.push(unconnect);
	}
	
	/**
	 * Sorts the QTreePin values within the pin2ElementMap into an ArrayList and returns it
	 * @return a sorted ArrayList of site pins 
	 */
	public ArrayList<QTreePin> getSitePins() {
		
		ArrayList<QTreePin> list = new ArrayList<QTreePin>(this.pin2ElementMap.keySet());
		Collections.sort(list);
		return list;
	}
	
	/**
	 * Overridden right click event handler for right click events on the site pin view. <br>
	 * Displays a popup menu with the option to delete the site pin.
	 */
	@Override
	protected void contextMenuEvent(QContextMenuEvent event){
		
		if (event.pos().x() > pins.pos().x() && event.pos().x() < pins.pos().x() + pins.width()
				&& event.pos().y() > pins.pos().y() + pins.header().height() && event.pos().y() < pins.pos().y() + pins.height()) {

			QMenu popupMenu = new QMenu();
			popupMenu.addAction(new QIcon(VSRTool.getImagePath("trash.png")), "Delete Selected Pins", this, "deleteSitePins()");
			popupMenu.addAction(new QIcon(VSRTool.getImagePath("unconnected.png")), "Mark As Unconnected", this, "markSitePinsUnconnected()");
			popupMenu.popup(event.globalPos());
		}
		
	}
	
	/**
	 * Checks to see if the "add site pin" icon has been clicked. <br>
	 * If it has, then the add site pin dialog is displayed so the user can enter a new site pin. 
	 */
	public void checkIconClick(){
	
		int iconSize = pins.header().height() - 5;
		
		int x = QCursor.pos().x() - pins.mapToGlobal(pins.header().pos() ).x();
		int y = QCursor.pos().y() - pins.mapToGlobal(pins.header().pos() ).y();
		
		if((x < iconSize && y < iconSize) && this.pins.topLevelItemCount() > 0) {
			AddPinDialog test = new AddPinDialog();
			
			//keeps displaying the dialog until the user does not want to enter more pins
			while( test.exec() == 1 ) { 
				
				AddSitePinGroupCommand addSitePinGroup = new AddSitePinGroupCommand(test.getPinName(), test.isOutputPin(), parent.getCurrentSite(), 
																parent.getGraphicsSite(), this, parent.getPlacementPosition(), test.getCount());
				this.undoStack.push(addSitePinGroup);
				test.resetDialog();
			}
		}	
	}
	
}//end of class