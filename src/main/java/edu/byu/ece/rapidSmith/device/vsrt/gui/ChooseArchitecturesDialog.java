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
import java.util.HashSet;
import java.util.Iterator;

import com.trolltech.qt.gui.QCheckBox;
import com.trolltech.qt.gui.QDialog;
import com.trolltech.qt.gui.QHBoxLayout;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QPushButton;
import com.trolltech.qt.gui.QVBoxLayout;

/**
 * @author Thomas Townsend
 * Created on: Aug 26, 2014
 */
public class ChooseArchitecturesDialog extends QDialog{

	private ArrayList<QCheckBox> checkBoxes = new ArrayList<QCheckBox>();
	private ArrayList<QLabel> archs = new ArrayList<QLabel>();
	
	/**
	 * 
	 * @param archNames
	 */
	public ChooseArchitecturesDialog(HashSet<String> archNames, String currentArch){
		
		QVBoxLayout vbox = new QVBoxLayout(this);
		vbox.addWidget( new QLabel("Select where to save") );
		this.setWindowTitle("Select Architectures.");
		Iterator<String> it = archNames.iterator();
		
		for (int i = 0; it.hasNext() ; i++) {
			String name = it.next();
			archs.add(new QLabel( name ) );
			checkBoxes.add( new QCheckBox() );
			QHBoxLayout tmp = new QHBoxLayout();
			tmp.addWidget( checkBoxes.get(i) );
			tmp.addWidget( archs.get(i) );
			
			if(name.equals(currentArch) )
				this.checkBoxes.get(i).setChecked(true);
			
			tmp.addStretch();
			vbox.addLayout(tmp);		
		} 
		QHBoxLayout tmp = new QHBoxLayout();
		tmp.addStretch();
		QPushButton save = new QPushButton("Save");
		save.clicked.connect(this, "accept()");
		tmp.addWidget(save);
		tmp.addStretch();
		
		vbox.addLayout(tmp);
	}
	
	/**
	 * 
	 * @return
	 */
	public ArrayList<String> getCheckedArchitectures(){
		ArrayList<String> tmp = new ArrayList<String>();
		
		for (int i = 0; i < this.checkBoxes.size(); i++ )
			if( this.checkBoxes.get(i).isChecked() )
				tmp.add(this.archs.get(i).text());
		
		return tmp;
	}
	
	/**
	 * Probably not needed
	 */
	public void clearDialog(){
		this.checkBoxes = new ArrayList<QCheckBox>();
		this.archs = new ArrayList<QLabel>();
	}
}//end class
