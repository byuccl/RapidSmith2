/*
 * Copyright (c) 2010 Brigham Young University
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
package edu.byu.ece.rapidSmith.gui;

import java.util.HashMap;

import com.trolltech.qt.core.Qt.ItemDataRole;
import com.trolltech.qt.gui.QTreeWidget;
import com.trolltech.qt.gui.QTreeWidgetItem;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.util.FamilyType;
import edu.byu.ece.rapidSmith.util.PartNameTools;

public class WidgetMaker {
	
	
	public static QTreeWidget createAvailablePartTreeWidget(String header){
		QTreeWidget treeWidget = new QTreeWidget();
		treeWidget.setColumnCount(1);
		treeWidget.setHeaderLabel(header);
		
		HashMap<FamilyType, QTreeWidgetItem> familyItems = new HashMap<FamilyType, QTreeWidgetItem>();
		HashMap<String, QTreeWidgetItem> subFamilyItems = new HashMap<String, QTreeWidgetItem>();
		
		for(String partName : RSEnvironment.defaultEnv().getAvailableParts()){
			FamilyType type = PartNameTools.getFamilyTypeFromPart(partName);
			QTreeWidgetItem familyItem = familyItems.get(type);
			if(familyItem == null){
				familyItem = new QTreeWidgetItem(treeWidget);
				familyItem.setText(0, PartNameTools.getFormalFamilyNameFromType(type));
				familyItems.put(type, familyItem);
			}
			String subFamilyName = PartNameTools.getSubFamilyFromPart(partName);
			QTreeWidgetItem partItem = null;
			QTreeWidgetItem parent = familyItem;
			if(subFamilyName != null){
				QTreeWidgetItem subFamilyItem = subFamilyItems.get(type.toString() + subFamilyName);
				if(subFamilyItem == null){
					subFamilyItem = new QTreeWidgetItem(parent);
					subFamilyItem.setText(0, subFamilyName);
					subFamilyItems.put(type.toString() + subFamilyName, subFamilyItem);
				}
				parent = subFamilyItem;
			}
			partItem = new QTreeWidgetItem(parent);
			partItem.setText(0, partName);
	        partItem.setData(0, ItemDataRole.AccessibleDescriptionRole, partName);
		}
		return treeWidget;
	}
}
