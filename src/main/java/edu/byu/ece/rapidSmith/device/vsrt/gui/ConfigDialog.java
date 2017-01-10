/*
 * Copyright (c) 2010-2011 Brigham Young University
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
/**
 * 
 */
package edu.byu.ece.rapidSmith.device.vsrt.gui;

import java.util.ArrayList;

import com.trolltech.qt.core.QEvent;
import com.trolltech.qt.core.QObject;
import com.trolltech.qt.core.Qt.AlignmentFlag;
import com.trolltech.qt.core.Qt.FocusPolicy;
import com.trolltech.qt.core.Qt.Key;
import com.trolltech.qt.core.Qt.Orientation;
import com.trolltech.qt.gui.QAction;
import com.trolltech.qt.gui.QCheckBox;
import com.trolltech.qt.gui.QDialog;
import com.trolltech.qt.gui.QDialogButtonBox;
import com.trolltech.qt.gui.QAbstractItemView.EditTrigger;
import com.trolltech.qt.gui.QAbstractItemView.SelectionMode;
import com.trolltech.qt.gui.QDialogButtonBox.ButtonRole;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QMessageBox;
import com.trolltech.qt.gui.QPalette.ColorRole;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QHBoxLayout;
import com.trolltech.qt.gui.QHeaderView;
import com.trolltech.qt.gui.QIcon;
import com.trolltech.qt.gui.QKeyEvent;
import com.trolltech.qt.gui.QLineEdit;
import com.trolltech.qt.gui.QPalette;
import com.trolltech.qt.gui.QPushButton;
import com.trolltech.qt.gui.QSizePolicy.Policy;
import com.trolltech.qt.gui.QTableWidget;
import com.trolltech.qt.gui.QTableWidgetItem;
import com.trolltech.qt.gui.QToolBar;
import com.trolltech.qt.gui.QVBoxLayout;

import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.Element;

/**
 * This class is used within the GUI to handle the cfg options found within the primitive def section of 
 * XDLRC files.  Some cfg options are on the site level (such as the reset type for each FF in a SLICEL ) and so they should
 * only be included in the primitive def file once.  This class allows you to "promote" such attributes, along with 
 * adding new options, deleting options, and editing cfg options.   
 * 
 * @author Thomas Townsend 
 * Created on: Jun 21, 2014
 */
public class ConfigDialog extends QDialog{
	
	/**Text edit box where the user can enter a new cfg option*/
	private final QLineEdit newConfigOption = new QLineEdit("Name: Option1 Option2 Option3...");
	/**Table that displays all of the current cfg options for either the bel or site*/
	private QTableWidget configOptionTable = new QTableWidget();
	/**Used for inserting new items into the configOptionTable in the proper place*/
	private int currentRow = 0; 
	/**Pallete used to change the text color in the newConfigOption text box*/
	private QPalette pallete = new QPalette();
	/**Element whose config options are currently displayed (null if the user is viewing the site options)*/
	private Element currentElement;
	/**Button used to promote a bel level attribute to a site level attribute*/
	private QAction promoteToSite;
	/**Exit button */
	private QPushButton done;
	/***/
	private QDialogButtonBox applyBox;
	/***/
	private QToolBar editMenuToolBar;
	/***/
	private QToolBar newConfigToolBar;
		
	/**
	 * Constructor:  
	 */
	public ConfigDialog (VSRTool parent){
		super(parent); 
		this.setWindowTitle("Add Bel Configuration Options:");
		this.resize(500, 400);
		
		this.initializeButtons();
		this.initializeNewConfigElementTextBox();
		this.initializeConfigElementTable();
		this.initializeEditMenu();
		this.initializeLayout();
		
	}
	
	/**
	 * Initializes all buttons on the Dialog included, icons, toolTip, and event handlers 
	 */
	private void initializeButtons(){
				
		this.newConfigToolBar = new QToolBar();
		this.newConfigToolBar.setOrientation(Orientation.Horizontal);
		this.newConfigToolBar.addWidget(new QLabel("New Config Option: "));
		this.newConfigToolBar.addWidget(newConfigOption);	
		QAction add = new QAction(new QIcon(VSRTool.getImagePath().resolve("add.gif").toString()), "", this);
		add.triggered.connect(this, "addNewCfgOption()");
		add.setToolTip("Add configuration option");
		
		this.newConfigToolBar.addAction(add);
		this.newConfigToolBar.setStyleSheet("QToolBar {border: 0px; icon-size: 16px}");
		
		//Buttons to close the dialog	
		done = new QPushButton(new QIcon(VSRTool.getImagePath().resolve("apply.png").toString()), "Done");
		done.clicked.connect(this, "closeDialog()");
		applyBox = new QDialogButtonBox(this);
		applyBox.addButton(done, ButtonRole.NoRole);		
	}
	
	
	/**
	 * Initializing the text box where a user enters a new configuration option
	 */
	private void initializeNewConfigElementTextBox(){
		//Initializing line edit where user enters config options
		pallete.setColor(ColorRole.Text, QColor.gray);
		newConfigOption.setPalette(pallete);
		newConfigOption.installEventFilter(this);
		newConfigOption.setFocusPolicy(FocusPolicy.ClickFocus);
	}
	
	/**
	 * Initializes the table that displays all configuration options. 
	 */
	private void initializeConfigElementTable(){
		//Row 1 = check box, Row 2 = title, Row 3 = options separated by a space 
		configOptionTable.setColumnCount(3);
		configOptionTable.verticalHeader().hide();
		configOptionTable.setColumnWidth(0, 25);
		//Setting Column headers 
		configOptionTable.setHorizontalHeaderItem(0, new QTableWidgetItem(""));
		configOptionTable.setHorizontalHeaderItem(1, new QTableWidgetItem("Name"));
		configOptionTable.setHorizontalHeaderItem(2, new QTableWidgetItem("Configuration Options"));		
		//Setting other important table options
		configOptionTable.setSelectionMode(SelectionMode.SingleSelection);
		configOptionTable.setEditTriggers(EditTrigger.AllEditTriggers);
		
		QHeaderView header = configOptionTable.horizontalHeader();
		header.setStretchLastSection(true);
	}
	
	
	/**
	 * Creates a new Vertical ButtonBox that holds all of the edit menu options 
	 */
	private void initializeEditMenu(){
		editMenuToolBar = new QToolBar();
		this.editMenuToolBar.setOrientation(Orientation.Vertical);
		this.editMenuToolBar.setStyleSheet("QToolBar {border: 0px; icon-size: 16px}" );
		//this.editMenuToolBar.setStyleSheet("QToolBar {icon-size: 24px}");
		this.editMenuToolBar.setSizePolicy(Policy.Minimum, Policy.Minimum);
		
		//Button that adds the new config option to all bels of that type
		promoteToSite = new QAction(new QIcon(VSRTool.getImagePath().resolve("promoteToSite.png").toString()), "", this);
		promoteToSite.setToolTip("Promote this configuration options to the site level\n"
								+ "\nNOTE: Selecting this will remove the configuration\noptions from all "
								+ "other bels");
		promoteToSite.triggered.connect(this,"promoteToSite()");
		
		//Button that deletes selected options 
		QAction deleteSelected = new QAction(new QIcon(VSRTool.getImagePath().resolve("trash.png").toString()), "", this);
		deleteSelected.triggered.connect(this, "deleteSelected()");
		deleteSelected.setToolTip("Delete Selected");
		
		//Button that selects all config options
		QAction selectAll = new QAction(new QIcon(VSRTool.getImagePath().resolve("editselectall.png").toString()), "", this);
		selectAll.triggered.connect(this, "selectAll()");
		selectAll.setToolTip("Select All");
		
		//Button that de-selects all config options 
		QAction deselectAll = new QAction(new QIcon(VSRTool.getImagePath().resolve("editunselectall.png").toString()), "", this);
		deselectAll.triggered.connect(this, "deselectAll()");
		deselectAll.setToolTip("Deselect All");	
		
		this.editMenuToolBar.addAction(deleteSelected);
		this.editMenuToolBar.addAction(promoteToSite);
		this.editMenuToolBar.addAction(selectAll);
		this.editMenuToolBar.addAction(deselectAll);
	}
	
	/**
	 * Adds each component to the dialog, and places them to look good  
	 */
	private void initializeLayout(){

		QVBoxLayout vbox = new QVBoxLayout(this);
		QVBoxLayout editMenuVBox = new QVBoxLayout();
		QHBoxLayout configOptionTableHBox = new QHBoxLayout();
		
		vbox.addWidget(this.newConfigToolBar);
		
		editMenuVBox.addWidget(this.editMenuToolBar);
		editMenuVBox.addStretch(1);
		configOptionTableHBox.addLayout(editMenuVBox);
		configOptionTableHBox.addWidget(configOptionTable);
		
		vbox.addLayout(configOptionTableHBox);
		vbox.addWidget(applyBox);
	}
		
	/**
	 * Closes the dialog and updates the bel or site level configuration options if they have been edited
	 */
	@SuppressWarnings("unused")
	private void closeDialog(){
		
		this.hide();
		if (this.currentElement != null) {
			currentElement.clearCfgElements();
			
			for(int i = 0; i < configOptionTable.rowCount(); i++){
				currentElement.addCfgElement(configOptionTable.item(i, 1).text() + ": " + configOptionTable.item(i, 2).text());
			}
		}
		else {
			((VSRTool)this.parent()).clearSiteConfigOptions();
			for(int i = 0; i < configOptionTable.rowCount(); i++){
				((VSRTool)this.parent()).addSiteConfigElement(configOptionTable.item(i, 1).text() + ": " + configOptionTable.item(i, 2).text());
			}
		} 
	}
	
	/**
	 * Extracts the text from the newConfigOption text box,
	 * checks to see if its in the proper format, and adds it to the display table if it is 
	 */
	private void addNewCfgOption()
	{
		String text = this.newConfigOption.text().trim();

		if (text.indexOf(":") != -1)
			this.addConfigOption(text);
		else{//display and info message letting the user know the proper input format
			QMessageBox.information(this, "Incorrect Format", "The configuration option you entered was given in the "
															+ "incorrect format:  Please follow this pattern:\n\n"
															+ "Name: Option1 Option2 Option3\n\n"
															+ "Example: 'D5FFINIT: INIT0 INIT1'");
		}		
	}
	
	/**
	 * eventFilter used to change the color of the text in the newConfigOption text edit box when the focus of the 
	 * widget has changed. 
	 * @param object Object that called the function
	 * @param event Type of event (focus changed)
	 * @return false 
	 */
	@Override
	public boolean eventFilter(QObject object, QEvent event){
		
		if(object == this.newConfigOption)
		{
			editBoxFocusChanged(event);
		}
		
		return false;
	}
	
	/**
	 * Updates the newConfigOption text edit box when the box either loses or gains keyboard focus
	 * @param event Focus in/Out Event
	 */
	private void editBoxFocusChanged(QEvent event){
		
		if( event.type() == QEvent.Type.FocusIn ) { 	
			this.pallete.setColor(ColorRole.Text, QColor.black);
			newConfigOption.setPalette(pallete);
			if( this.newConfigOption.text().equals("Name: Option1 Option2 Option3...") ) {
				newConfigOption.clear();
			}
		} 
		else if ( event.type() == QEvent.Type.FocusOut ) {
			if( this.newConfigOption.text().equals("") ) {
				newConfigOption.setText("Name: Option1 Option2 Option3...");
			    pallete.setColor(ColorRole.Text, QColor.gray);
			    newConfigOption.setPalette(pallete);
			}
		}
	}
	
	/**
	 * Adds a new configuration option to the bel/site
	 * @param text new Configuration Option to add to the bel/site
	 */
	private void addConfigOption(String text) {

		//making sure the text is spaced properly
		text = text.replaceAll("\\s+", " ");
		
		//Parsing the new option and adding it to the configOptionTable
		configOptionTable.setRowCount(currentRow + 1);
		configOptionTable.setCellWidget(currentRow, 0, new QCheckBox());
		QTableWidgetItem test = new QTableWidgetItem(text.substring(0, text.indexOf(":")));
		test.setTextAlignment(AlignmentFlag.AlignCenter.value());
		configOptionTable.setItem(currentRow, 1, test);
		test = new QTableWidgetItem(text.substring(text.indexOf(":")+1));
		test.setTextAlignment(AlignmentFlag.AlignCenter.value());
		configOptionTable.setItem(currentRow, 2, test);		
		currentRow++;
		
		//add this configOption to either the element or site
		if (!newConfigOption.text().equals("Name: Option1 Option2 Option3...")) {
			newConfigOption.clear();
			newConfigOption.setFocus();
			if (currentElement != null)
				this.currentElement.addCfgElement(text);
			else
			{	((VSRTool)this.parent()).addSiteConfigElement(text);	}
		}
		this.resizeTableColumns();
	}
		
	/**
	 * Removes the selected configuration option from the bel/site. 
	 * @param row Table row to be deleted
	 */
	private void deleteOption(int row){
		//remove from display table
		configOptionTable.removeRow(row);
		
		//remove from element/site data structure
		if(currentElement != null)
			this.currentElement.removeCfgElement(row);
		else
		{	((VSRTool)this.parent()).removeSiteConfigElement(row);	}
		
		this.currentRow--;
		this.resizeTableColumns();
	}
	
	/**
	 * Resizes the dialog to to fit all columns of the table into view
	 */
	private void resizeTableColumns(){
		configOptionTable.resizeColumnToContents(1);
		configOptionTable.setColumnWidth(1, configOptionTable.columnWidth(1) + 20);
		configOptionTable.resizeColumnToContents(2);
		this.resize(configOptionTable.width()//configOptionTable.columnWidth(1) + configOptionTable.columnWidth(2) + configOptionTable.columnWidth(0)
					+ this.editMenuToolBar.iconSize().width() + 50 , this.height());
	}
	
	
	/**
	 * Removes a configuration option from the bel level (each bel that has it) and adds it to the site level 
	 * (will only show up once in the primitive def file)
	 */
	@SuppressWarnings("unused")
	private void promoteToSite(){
		for (int i = configOptionTable.rowCount()-1; i >= 0; i--){
			if ( ((QCheckBox)configOptionTable.cellWidget(i, 0)).isChecked() ) {
				String cfgName = configOptionTable.item(i, 1).text();
				((VSRTool)this.parent()).addSiteConfigElement(cfgName + ": " + configOptionTable.item(i, 2).text().trim());
				deleteOption(i);
				this.removeConfigFromAllBels(cfgName);
			}
		}
	}
	
	/**
	 * Searches the table for selected items and removes them from the bel/site they are apart of 
	 */
	@SuppressWarnings("unused")
	private void deleteSelected(){
		for (int i = configOptionTable.rowCount()-1; i >= 0; i--){
			if ( ((QCheckBox)configOptionTable.cellWidget(i, 0)).isChecked() )
					deleteOption(i);
		}		
	}
	
	/**
	 * Check all items in the table
	 */
	@SuppressWarnings("unused")
	private void selectAll(){
		for (int i = 0 ; i < configOptionTable.rowCount(); i++){
			((QCheckBox)configOptionTable.cellWidget(i, 0)).setChecked(true);	
		}
	}
	
	/**
	 * Un-checkes all items in the table
	 */
	@SuppressWarnings("unused")
	private void deselectAll(){
		for (int i = 0 ; i < configOptionTable.rowCount(); i++){
			((QCheckBox)configOptionTable.cellWidget(i, 0)).setChecked(false);	
		}
	}
	
	/**
	 * Overrided function that will add a new config option if "ENTER" is pressed while focus
	 * is on the newConfigOption text box
	 * @param event QKeyEvent that contains the key that was pressed
	 */
	@Override
	protected void keyPressEvent(QKeyEvent event){
		
		if (newConfigOption.hasFocus() && (event.key() == Key.Key_Return.value() 
										|| event.key() == Key.Key_Enter.value()))
			this.addNewCfgOption();
	}
	
	/**
	 * Displays the configuration options currently available within the bel element passed
	 * into the function
	 * @param element Element whose configuration options are to be displayed 
	 */
	public void displayBelCfgOptions(Element element){
		this.setWindowTitle("Configuration Options: " + element.getName());
		this.promoteToSite.setEnabled(true);
		this.currentElement = element;
		configOptionTable.setRowCount(0);
		currentRow = 0;
		
		//displaying configuration options on the table
		if (element.getCfgElements() != null) {
			for (String cfgElement : element.getCfgElements()) {
				this.addConfigOption(cfgElement.trim());
			}
		}
	}
	/**
	 * Displays the configuration options currently available within the selected site 
	 * (This will be empty unless a cfg option has been promoted to the site level)
	 * @param cfgOptions ArrayList of site configuration options
	 */
	public void displaySiteCfgOptions(ArrayList<String> cfgOptions){
		this.setWindowTitle("Site Configuration Options: ");
		this.promoteToSite.setEnabled(false);
		this.currentElement = null;
		configOptionTable.setRowCount(0);
		currentRow = 0;
		
		for (String cfgOption : cfgOptions) {
			this.addConfigOption(cfgOption.trim());
		}
	}
	/**
	 * Removes the configuration option with the given name from any bel that currently has it. <br>
	 * This function is only called when a configuration option has been promoted to the site level. <br>
	 * @param name Name of the configuration option that is to be removed
	 */
	private void removeConfigFromAllBels(String name){
		for (Element element : ((VSRTool)this.parent()).getCurrentSite().getElements()) 
			element.removeCfgElement(name);
	}
}