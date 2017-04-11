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
package edu.byu.ece.rapidSmith.device.vsrt.gui;

import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.AlignmentFlag;
import com.trolltech.qt.gui.QDialog;
import com.trolltech.qt.gui.QDialogButtonBox;
import com.trolltech.qt.gui.QHBoxLayout;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QLayout;
import com.trolltech.qt.gui.QLineEdit;
import com.trolltech.qt.gui.QMessageBox;
import com.trolltech.qt.gui.QPushButton;
import com.trolltech.qt.gui.QRadioButton;
import com.trolltech.qt.gui.QVBoxLayout;
import com.trolltech.qt.gui.QDialogButtonBox.ButtonRole;

public class StaticBelDialog extends QDialog {

	/**text box used to enter the pin name */
	private QLineEdit nameTextBox;
	/**clicked to specify pin as input*/
	private QRadioButton vcc;
	/**clicked to specify pin as output*/
	private QRadioButton gnd;
	/**Button that adds the new site pin to the scene*/
	private QPushButton add;
	/**Button that closes the dialog*/
	private QPushButton cancel;
	
	public StaticBelDialog() {
		this.setWindowTitle("Add VCC/GND");
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
		nameTextBox = new QLineEdit();
		vcc = new QRadioButton("VCC");
		gnd = new QRadioButton("GND");
		add = new QPushButton("Add");
		add.clicked.connect(this, "addBel()");
		add.setDefault(true);
		cancel = new QPushButton("Cancel");
		cancel.clicked.connect(this, "reject()");	
	}
	/**
	 * Configures the layout of the dialog
	 */
	private void initializeLayout() {
		QVBoxLayout vbox = new QVBoxLayout(this);
		QHBoxLayout radioButtons = new QHBoxLayout();
		QHBoxLayout belNameEnter = new QHBoxLayout();
		
		radioButtons.addWidget(new QLabel("Type:"));
		radioButtons.addWidget(vcc);
		radioButtons.addWidget(gnd);
		vbox.addLayout(radioButtons);
		
		belNameEnter.addWidget(new QLabel("Name:"));
		belNameEnter.addStretch();
		belNameEnter.addWidget(nameTextBox);
		vbox.addLayout(belNameEnter);
		
		QDialogButtonBox exitOptions = new QDialogButtonBox(this);
		
		exitOptions.addButton(add, ButtonRole.ActionRole); 
		exitOptions.addButton(cancel, ButtonRole.ActionRole); 
		exitOptions.centerButtons();
		vbox.addWidget(exitOptions);
		vbox.setAlignment(new Qt.Alignment(AlignmentFlag.AlignTop));
	}
	
	public String getBelName() {
		return this.nameTextBox.text();
	}
		
	public boolean isVcc() {
		return vcc.isChecked();
	}
	
	@SuppressWarnings("unused")
	private void addBel() {
		if (this.nameTextBox.text().trim().equals("")) {
			QMessageBox.information(this, "Missing Element Name", "Enter a valid element name");
		}
		else if (!this.vcc.isChecked() && !this.gnd.isChecked()) {
			QMessageBox.information(this, "Missing Type", "Select either VCC or GND");
		} 
		else {
			this.accept();
		}
	}
}
