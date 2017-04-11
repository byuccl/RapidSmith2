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
package edu.byu.ece.rapidSmith.device.vsrt.gui;

import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.AlignmentFlag;
import com.trolltech.qt.gui.QDialog;
import com.trolltech.qt.gui.QDialogButtonBox;
import com.trolltech.qt.gui.QDialogButtonBox.ButtonRole;
import com.trolltech.qt.gui.QHBoxLayout;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QLayout;
import com.trolltech.qt.gui.QLineEdit;
import com.trolltech.qt.gui.QMessageBox;
import com.trolltech.qt.gui.QPushButton;
import com.trolltech.qt.gui.QRadioButton;
import com.trolltech.qt.gui.QVBoxLayout;


/**
 * When a user adds a new pin to the primitive site, this dialog <br>
 * is displayed which allows them to enter the new pin information.  
 * @author Thomas Townsend
 * Created on: Jul 14, 2014
 */
public class AddPinDialog extends QDialog {

	/**text box used to enter the pin name */
	private QLineEdit textBox;
	/***/
	private QLineEdit count;
	/**clicked to specify pin as input*/
	private QRadioButton input;
	/**clicked to specify pin as output*/
	private QRadioButton output;
	/**Button that adds the new site pin to the scene*/
	private QPushButton add;
	/**Button that closes the dialog*/
	private QPushButton done;
	
	/**
	 * Constructor:
	 */
	public AddPinDialog() {
		this.setWindowTitle(" ");
		this.initializeComponents();
		this.initializeLayout();
		
		//making the dialog not re-sizable
		this.layout().setSizeConstraint( QLayout.SizeConstraint.SetFixedSize );
		this.setSizeGripEnabled( false );
	}
	
	/**
	 * Initializes all components of the dialog
	 */
	private void initializeComponents(){
		textBox = new QLineEdit();
		count = new QLineEdit();
		input = new QRadioButton("Input");
		output = new QRadioButton("Output");
		add = new QPushButton("Add");
		add.clicked.connect(this, "addPin()");
		add.setDefault(true);
		done = new QPushButton("Done");
		done.clicked.connect(this, "closeDialog()");		
	}
	/**
	 * Configures the layout of the dialog
	 */
	private void initializeLayout() {
		QVBoxLayout vbox = new QVBoxLayout(this);
		QHBoxLayout radioButtons = new QHBoxLayout();
		QHBoxLayout nameEnter = new QHBoxLayout();
		QHBoxLayout countEnter = new QHBoxLayout();
		
		nameEnter.addWidget(new QLabel("Pin Name:"));
		nameEnter.addStretch();
		nameEnter.addWidget(textBox);
		vbox.addLayout(nameEnter);
		
		countEnter.addWidget(new QLabel("Count:"));
		countEnter.addStretch();
		countEnter.addWidget(count);
		vbox.addLayout(countEnter);
		
		radioButtons.addWidget(new QLabel("Direction:"));
		radioButtons.addWidget(input);
		radioButtons.addWidget(output);
		//radioButtons.setAlignment(new Qt.Alignment(AlignmentFlag.AlignCenter));
		vbox.addLayout(radioButtons);
		QDialogButtonBox exitOptions = new QDialogButtonBox(this);
		
		exitOptions.addButton(add, ButtonRole.ActionRole); 
		exitOptions.addButton(done, ButtonRole.ActionRole); 
		exitOptions.centerButtons();
		vbox.addWidget(exitOptions);
		vbox.setAlignment(new Qt.Alignment(AlignmentFlag.AlignTop));
	}
	/**
	 * Extracts the pin name entered into the text box
	 * @return
	 */
	public String getPinName(){
		return this.textBox.text();
	}
	/**
	 * Extracts the count entered into the text box
	 * @return
	 */
	public int getCount(){
		return Integer.parseInt( this.count.text() ); 
	}
	/**
	 * Returns whether the user has specified the new pin as input or output
	 * @return
	 */
	public boolean isOutputPin(){
		return (output.isChecked() ? true : false );
	}
	/**
	 * Clears the text box and deselects all radio buttons on the dialog
	 */
	public void resetDialog(){
		textBox.clear();
		count.clear();
		
		input.setAutoExclusive(false);
		input.setChecked(false);
		input.setAutoExclusive(true);
		
		output.setAutoExclusive(false);
		output.setChecked(false);
		output.setAutoExclusive(true);
		
		textBox.setFocus();
	}
	
	/***
	 * Checks to make sure the user has correctly entered all needed information for the new pin, <br>
	 * and if they have, then accepts the dialog (returns 1). 
	 */
	@SuppressWarnings("unused")
	private void addPin(){
		boolean countIsNumber = true;
		
		try{
			int tmp = Integer.parseInt( this.count.text() );
			if( tmp < 1 ) {
				throw new NumberFormatException();
			}
		}catch(NumberFormatException e) {
			countIsNumber = false;
		}
		
		if (!this.textBox.text().equals("") && countIsNumber && (input.isChecked() || output.isChecked())){
			this.accept();
		}
		else if (textBox.text().equals("") ){
			QMessageBox.information(this, "Missing Pin Name!", "Please enter a name for this site pin.");
		}
		else if (!countIsNumber){
			QMessageBox.information(this, "Invalid count specified", "Count is not a number, try again");
		}
		else {
			QMessageBox.information(this, "Missing Pin Direction!", "Please specify as either an input or output pin.");
		}
	}
	
	@SuppressWarnings("unused")
	private void closeDialog(){
		this.reject();
	}
}
