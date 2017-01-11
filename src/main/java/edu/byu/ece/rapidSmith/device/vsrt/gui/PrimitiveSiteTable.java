package edu.byu.ece.rapidSmith.device.vsrt.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.SortOrder;
import com.trolltech.qt.gui.QContextMenuEvent;
import com.trolltech.qt.gui.QHeaderView;
import com.trolltech.qt.gui.QIcon;
import com.trolltech.qt.gui.QMenu;
import com.trolltech.qt.gui.QMouseEvent;
import com.trolltech.qt.gui.QTableWidget;
import com.trolltech.qt.gui.QTableWidgetItem;
import com.trolltech.qt.gui.QWidget;

/**
 * This class implements the QTableWidget that displays all of the 
 * available primitive sites for a given architecture/family. 
 * @author Thomas Townsend
 * Created on: Jul 2, 2014
 */
public class PrimitiveSiteTable extends QTableWidget {

	private SiteSelectTab parent;
	/** The file location of the current family */
	private File currentFamily;
	/** List of all current primitive sites */
	private ArrayList<QTableWidgetItem> table_items = new ArrayList<QTableWidgetItem>(); 
	/**Directory containing all primitive def files*/
	private String directory;
	/**File filter so that I extract only the .def files from the given directory*/
	private FilenameFilter defFilter;
	/**Maps architectures to completes primitive sites for that architecture*/
	private HashMap<String, HashSet<String>> completedSites = new HashMap<String, HashSet<String>> ();
	/**Icon representing when a primitive site has been completed*/
	private QIcon checked;
	/**Icon representing when a primitive site has not been completed*/
	private QIcon unchecked; 

	/**
	 * Initializes the table with the appropriate settings 
	 * @param parent QWidget who this table is apart of 
	 * @param filter Filter used to distinguish .def files 
	 */
	public PrimitiveSiteTable (QWidget parent, FilenameFilter filter) {
		super (parent);
		this.parent = (SiteSelectTab) parent; 
	
		// initialize icons
		checked =  new QIcon(VSRTool.getImagePath("check.gif"));
		unchecked =  new QIcon(VSRTool.getImagePath("uncheck.gif"));
		
		//Table Settings
		this.setColumnCount(1);
		this.verticalHeader().hide();
		this.horizontalHeader().hide();
		this.setSelectionMode(SelectionMode.SingleSelection);
		this.setDragEnabled(false);
			
		QHeaderView header = this.horizontalHeader();
		header.setStretchLastSection(true);
		
		//Highlight the selected cell with a nice color
		this.setStyleSheet("selection-background-color: blue"); 
		
		//Creating a comparator for QTableWidgetItems so that they can be sorted in alphabetical order. 
		Collections.sort(table_items, new Comparator<QTableWidgetItem>() {
			public int compare(QTableWidgetItem item1, QTableWidgetItem item2) {
				return item1.text().compareTo(item2.text()); 
			}
		});
		
		this.defFilter = filter;
		//this.getCompletedSites();
	}

	/**
	 * Parses the "completedSites.txt" file and populates the completedSites <br>
	 * data structure with the contents. 
	 */
	public void getCompletedSites(){
		BufferedReader reader;
		this.completedSites.clear();
		try {
			String[] tokens;
			reader = Files.newBufferedReader(Paths.get(this.directory + File.separator + "completedSites.txt"), Charset.defaultCharset());
		
			while (true){
				String site = reader.readLine();
				if (site == null)
					break;
				else{
					tokens = site.trim().split("\\s+");
					HashSet<String> values = this.completedSites.get( tokens[0] );
					if (values != null )
						values.add( tokens[1] );
					else {
						HashSet<String> newlist = new HashSet<String>();
						newlist.add( tokens[1] );
						this.completedSites.put( tokens[0] , newlist );
					}
					
				}
			}

			reader.close();
		} catch (Exception e) {}
	}
	
	/**
	 * Set the current directory where the .def files are located <br>
	 * This is changed when the value of the architecture combo box is changed 
	 * @param dir
	 */
	public void setDirectory(String dir){
		this.directory = dir;
	}
	
	/**
	 * This method extracts all primitive def files from the current directory, <br>
	 * and displays them on the table
	 * @param family The family/architecture for the primitive sites
	 * @return Returns true if the family folder was found
	 */
	public boolean load_table (String family) {
		//opening a handle to the directory
		this.currentFamily = new File(this.directory + File.separator + family); 

		this.setRowCount(currentFamily.listFiles(this.defFilter).length);
		
		//clear all items in the table to make way for the the new primitive sites
		this.clear();
		table_items.clear();
	 
		//Adding each .def file of the given family/architecture into the primitive site table
		//with appropriate settings 
		int row = 0;
		for (File file: currentFamily.listFiles(this.defFilter)) {
				String sitename = file.getName().substring(0, file.getName().length() - 4);
				QTableWidgetItem test = new QTableWidgetItem (sitename); 
				if ( this.completedSites.get(currentFamily.getName()) != null ) {
					if ( this.completedSites.get(currentFamily.getName()).contains(sitename)  )
						test.setIcon(checked);
					else { test.setIcon(unchecked); }
				}
				else { test.setIcon(unchecked);  } 
				
				table_items.add(test);
				table_items.get(row).setTextAlignment(Qt.AlignmentFlag.AlignCenter.value());
				
				//Making items in the table not editable
				table_items.get(row).setFlags(Qt.ItemFlag.ItemIsEnabled,Qt.ItemFlag.ItemIsSelectable );
	
				this.setItem(row, 0, table_items.get(row));
				row++;
		}
		this.sortByColumn(0, SortOrder.AscendingOrder);
		return true;
	}
	
	/**
	 * Gets all items that are currently in the table
	 * @param non
	 * @return ArrayList<QTableWidgetItem>
	 */
	public ArrayList<QTableWidgetItem> get_table_items(){
		return table_items; 
	}
	
	/**
	 * Overridden mouse double click event handler which activates a primitive site 
	 * so that the connections can be drawn  
	 */
	@Override
	protected void mouseDoubleClickEvent(QMouseEvent event) {
		parent.select_site();
		
	}
	
	/**
	 * Mark a site as completed.  This will add it to the "completedSites.txt" <br>
	 * file as well as the completedSites data structure
	 */
	public void siteCompleted(){
		//update the hashmap data structure
		QTableWidgetItem selected = this.selectedItems().get(0); 
		selected.setIcon(checked);
		selected.setSelected(false);
		
		this.addCompletedSite(this.currentFamily.getName(), selected.text() );
	}
	
	public void addCompletedSite(String arch, String primDef){
		HashSet<String> completed = this.completedSites.get( arch );
		boolean shouldWriteToFile = true;
		if (completed != null) 
			if ( !completed.contains(primDef) )
				completed.add(primDef);
			else {	shouldWriteToFile = false;	}
		else{
			completed = new HashSet<String>();
			completed.add(primDef);
			this.completedSites.put(arch, completed);
		}
		if (shouldWriteToFile) {
			try{	
				FileWriter fw = new FileWriter(this.directory + File.separator + "completedSites.txt", true);
				fw.write(arch + " " + primDef + "\n");
				fw.flush();
				fw.close(); 
			}catch (IOException e){}
		}
	}
	/**
	 * Context menu event used to display the option for a user <br>
	 * to mark a site as complete.
	 */
	protected void contextMenuEvent(QContextMenuEvent event){
		QMenu tmp = new QMenu();
		if ( this.selectedItems().get(0).icon().cacheKey() == unchecked.cacheKey() )
			tmp.addAction(new QIcon(VSRTool.getImagePath("check.gif")), "Mark Site As Complete", this, "siteCompleted()");
	// NOTE: May update this later with users being able to make a site incomplete, but right now this is unnecessary
	//	else {
	//		tmp.addAction(null, "Mark Site As Incomplete", this, "siteCompleted()");
	//	}
		tmp.popup(event.globalPos());
	}
	
}//end of class
