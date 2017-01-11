package edu.byu.ece.rapidSmith.device.vsrt.gui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import com.trolltech.qt.core.QEvent;
import com.trolltech.qt.core.QObject;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.FocusPolicy;
import com.trolltech.qt.gui.QAbstractItemView.ScrollHint;
import com.trolltech.qt.gui.QKeyEvent;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QLineEdit;
import com.trolltech.qt.gui.QPalette;
import com.trolltech.qt.gui.QTableWidgetItem;
import com.trolltech.qt.gui.QPalette.ColorRole;
import com.trolltech.qt.gui.QVBoxLayout;
import com.trolltech.qt.gui.QWidget;

/**
 * This class layouts and displays all components that the user uses to select a 
 * primitive site to generate connections for 
 * @author Thomas Townsend
 * Created on: Jul 2, 2014
 */
public class SiteSelectTab extends QWidget{

	private VSRTool parent;
	/** Combo box that lists the available family names*/
	private FamilyComboBox combo;
	/** Table that lists all primitive sites of the family*/
	private PrimitiveSiteTable table;
	/** Search Box of primitive site table*/
	private QLineEdit searchbox = new QLineEdit(); 
	/** Text label displayed if search item is not found*/
	private QLabel notfound = new QLabel("<font color='dark red'>Not Found...</font>");
	/** Used to change the text color of the search box*/
	private QPalette pallete = new QPalette();
	/**Filter used to only extract .def files from directories*/
	private FilenameFilter defFilter;
	
	/**
	 * Constructor: 
	 * @param parent
	 */
	public SiteSelectTab(QWidget parent) {
	
		super(parent);
		this.parent = (VSRTool) parent; 
		
		initUI(); 
		move(0,0);
		resize(600,600);
		show();	
	}
	
	/**
	 * Initializes and adds all components that will go on this panel
	 * @param none
	 * @return none
	 */
	private void initUI() {
		
	     QVBoxLayout vbox = new QVBoxLayout(this);
	     
	     //File filter to only extract .def files
	     this.defFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if(name.endsWith(".def"))
						return true;
					else {	return false; }
				}	
			};
	     
		 //Family/Architecture combo box
	     combo = new FamilyComboBox(this, defFilter);
	     //Primitive Site Table
	     table = new PrimitiveSiteTable(this, defFilter);
	     
	     //layout
	     vbox.stretch(1);
	     vbox.addWidget(combo);
	     vbox.addWidget(searchbox);
	     vbox.addWidget(notfound);
	     vbox.addWidget(table);  
	     	   	 
	     this.initializeSearchbox();
	}
	
	/**
	 * Sets up the searchbox that can be used to search the primitive site table 
	 * for a primitive site 
	 */
	private void initializeSearchbox(){
	     searchbox.textChanged.connect(this, "search_table()");
	     searchbox.setText("Search...");
	     pallete.setColor(ColorRole.Text, VsrtColor.gray);
	     searchbox.setPalette(pallete);
	     searchbox.setFocusPolicy(FocusPolicy.ClickFocus);
	     searchbox.installEventFilter(this);

	     notfound.hide();
	}
	
	/**
	 * @return The site that is currently being edited on the device view 
	 */
	public String currentSite(){
		return this.table.currentItem().text();
	}
	public String currentFamily(){
		return this.combo.currentText();
	}
	public ArrayList<String> getAvailableFamilies(){
		ArrayList<String> tmp = new ArrayList<String>();
		
		for (int i = 0; i < this.combo.count() ; i++) 
			tmp.add(this.combo.itemText(i));
		
		return tmp;
	}
	public String getXMLLocation(){
		return this.combo.currentText() + File.separator + this.table.currentItem().text() + ".xml" ; 
	}
	public String getSiteLocation(){
		return this.combo.currentText() + File.separator + this.table.currentItem().text() + ".def";
	}
	/**
	 * Moves the highlighter on the primitive site table up or down 
	 * if either of those keys are pressed   
	 * @param Key event
	 * @return none
	 */
	@Override 
	protected void keyPressEvent(QKeyEvent event) {
		
		//This will return either 0 or 1 items since only 1 item in the table can be selected at a time
		ArrayList<QTableWidgetItem> selected =  (ArrayList<QTableWidgetItem>)table.selectedItems();
		
		if ( event.key() == Qt.Key.Key_Down.value() ) {
			
			if (selected.size() != 0) {
			
				if( selected.get(0).row() !=  table.rowCount()-1 )
					table.setCurrentCell(selected.get(0).row()+1,0);
			}
		}
		else if ( event.key() == Qt.Key.Key_Up.value() ) {
			
			if (selected.size() != 0) {
				
				if( selected.get(0).row() !=  0 )
					table.setCurrentCell(selected.get(0).row()-1,0);
			}
		}
		else if (event.key() == Qt.Key.Key_Enter.value())
		{
			if( notfound.isHidden() ) {
				this.select_site();
			}
		}
	}
	/**
	 * Overridden function which creates custom focus-event handlers
	 * for the search box 
	 * @param object The object where an event has occurred
	 * @param event The event that was triggered
	 * @return boolean
	 */
	@Override
	public boolean eventFilter(QObject object, QEvent event){
		
		if(object == this.searchbox)
		{
			search_focus_changed(event);
		}
		
		return false;
	}
	
	/**
	 * Updates the search box when it loses/gains focus 
	 * @param event The event type
	 * @return none
	 */
	private void search_focus_changed(QEvent event){
		
		//If the event is a focus in event, change the text color to black 
		//and clear the searchbox if the default text "Search..." is in the searchbox
		if( event.type() == QEvent.Type.FocusIn ) { 
			
			this.pallete.setColor(ColorRole.Text, VsrtColor.black);
			searchbox.setPalette(pallete);
			if( this.searchbox.text().equals("Search...") ) {
				searchbox.clear();
			}
		} 
		//If the event is a focus out event, if the text box is empty, change the text color to 
		//gray and display the default "Search..." message
		else if ( event.type() == QEvent.Type.FocusOut ) {
			if( this.searchbox.text().equals("") ) {
				searchbox.setText("Search...");
			    pallete.setColor(ColorRole.Text, VsrtColor.gray);
			    searchbox.setPalette(pallete);
			}
			notfound.hide();
		}
	}
	
	/**
	 * Searches the primitive site table based on the text in searchbox, and 
	 * selects the item in the table if it is found.  If it is not found, than 
	 * a "not found" message will be displayed to the user. 
	 * 
	 */
	@SuppressWarnings("unused")
	private void search_table() {
		
		//case insensitive search
		String search = searchbox.text().toLowerCase().trim();
		boolean found = false;
		
		//if there is actually text in the box then search through the table 
		if ( !search.equals("") )
		{
			// Searching through the table items to find first match
			for (QTableWidgetItem item : table.get_table_items()) {
				//If the text is found, the first item that matches will be highlighed
				//and scrolled to the top (if possible)
				if ( item.text().toLowerCase().startsWith(search) ) {
					table.setCurrentCell(item.row(), item.column());
					table.scrollToItem(item, ScrollHint.PositionAtTop);
					found = true;
					break;
				}
			}
			// If the item is not found, then display a message to the user letting them know 
			if (!found) {
				notfound.show();
				table.scrollToTop();
				table.clearSelection();
			}
			else {
				notfound.hide();
			}
		} 
		//If there is no text in the searchbox, clear the selected items of the table and scroll to the top
		else {
			notfound.hide();
			table.scrollToTop();
			table.clearSelection(); 
		}
		
	}
	
	/**
	 * This method passes the selected primitive site name to its parent widget so that 
	 * the parent widget can parse the corresponding .def file and initialize it on the graphics view
	 */
	public void select_site() {
		ArrayList<QTableWidgetItem> selected = (ArrayList<QTableWidgetItem>) table.selectedItems();
		
		//Only do this if the primitve site is selected
		if (selected.size() > 0) {
			parent.get_primitive_def(combo.currentText(),(selected.get(0).text()));
		}
	}
	
	/**
	 * When a new family is selected in the combo box, 
	 * the primitive type table is updated to reflect the new family's/architectures
	 * primitive sites 
	 */
	public void load_new_family (){
		table.scrollToTop();
		table.load_table( combo.itemText(combo.currentIndex()));
	}
	
	/**
	 * This method reinitializes the family/architecture combo box and 
	 * primitive site table to with the files found in the directory parameter
	 * @param directory Directory that contains all of the family directories and
	 *                  primitive def files. 
	 */
	public void loadArchitecturesAndSites(String directory) throws NullPointerException{
		//clear any families currently in the combo box
		this.combo.currentIndexChanged.disconnect();
		this.combo.clear();
		this.combo.init_cb(directory);
		
		//clear any primitive sites in the table
		this.table.setDirectory(directory);
		this.table.clear();
		this.table.setRowCount(0);
		
		this.table.getCompletedSites();
		
		//If the directory is valid, then initialize everything
		if(this.combo.count() > 0) 
			this.table.load_table(this.combo.getFirstFamily());
		else {
			throw new NullPointerException();
		}
	}
	
	public void addCompletedSite(String a, String b){
		this.table.addCompletedSite(a, b);
	}
}
