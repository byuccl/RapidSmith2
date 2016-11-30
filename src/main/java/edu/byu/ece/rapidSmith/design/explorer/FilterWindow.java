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
package edu.byu.ece.rapidSmith.design.explorer;

import java.util.ArrayList;

import com.trolltech.qt.core.QAbstractItemModel;
import com.trolltech.qt.core.QModelIndex;
import com.trolltech.qt.core.QObject;
import com.trolltech.qt.core.QRegExp;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.CursorShape;
import com.trolltech.qt.core.Qt.Orientation;
import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QCheckBox;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QComboBox;
import com.trolltech.qt.gui.QCursor;
import com.trolltech.qt.gui.QFont;
import com.trolltech.qt.gui.QGridLayout;
import com.trolltech.qt.gui.QItemSelectionModel;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QLineEdit;
import com.trolltech.qt.gui.QSortFilterProxyModel;
import com.trolltech.qt.gui.QStandardItem;
import com.trolltech.qt.gui.QStandardItemModel;
import com.trolltech.qt.gui.QTreeView;
import com.trolltech.qt.gui.QWidget;
import com.trolltech.qt.gui.QAbstractItemView.EditTrigger;
import com.trolltech.qt.gui.QAbstractItemView.ScrollHint;

import edu.byu.ece.rapidSmith.design.xdl.XdlAttribute;
import edu.byu.ece.rapidSmith.design.xdl.XdlInstance;
import edu.byu.ece.rapidSmith.design.xdl.XdlModule;
import edu.byu.ece.rapidSmith.design.xdl.XdlModuleInstance;
import edu.byu.ece.rapidSmith.design.xdl.XdlNet;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.design.xdl.XdlPin;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.timing.LogicPathElement;
import edu.byu.ece.rapidSmith.timing.Path;
import edu.byu.ece.rapidSmith.timing.PathDelay;
import edu.byu.ece.rapidSmith.timing.PathElement;
import edu.byu.ece.rapidSmith.timing.PathOffset;
import edu.byu.ece.rapidSmith.timing.RoutingPathElement;

/**
 * This class creates the various tabs of the DesignExplorer.
 * @author Chris Lavin
 */
public class FilterWindow extends QWidget{
	/** The associated design explorer for this tab */
	private DesignExplorer explorer;
	/** There are 4 types of tabs, this is the type of this tab instance */
	private FilterType type; 
	/** The view associated with this window */
	private QTreeView view;
	/** The listing model for the data */
	private QStandardItemModel model;
	/** A sorting and filter model for the data */
	private MySortFilterProxyModel proxyModel;
	/** A check box to flag case sensitivity in the filtering */
	private QCheckBox filterCaseSensitivityCheckBox;
	/** The selection box of how to filter */
	private QComboBox filterSyntaxComboBox;
	/** The layout object for the window */
	private QGridLayout proxyLayout;
	/** The edit-able text box for the user to enter a filter for the data */
	private QLineEdit filterPatternLineEdit;
	/** The label for the filter pattern edit box */
	private QLabel filterPatternLabel; 
	/** The main selection model */
	private QItemSelectionModel selectionModel;
	/** Keeps a pointing finger cursor handy */
	private QCursor pointingFinger = new QCursor(CursorShape.PointingHandCursor);
	/** Keeps an arrow cursor handy */
	private QCursor arrow = new QCursor(CursorShape.ArrowCursor);
	/** Some tabs have other models (Instances have attributes) */
	private QStandardItemModel[] subModels;
	/** The associated views with the other subModels */
	private QTreeView[] subViews;
	/** A font used for hyperlinks */
	private QFont hyperlink = new QFont();
	/** A brush used for hyperlinks */
	private QBrush blue = new QBrush(QColor.blue);
	
	/** Helps differentiate tab windows */
	enum FilterType {
		NETS,
		INSTANCES,
		MODULES,
		MODULE_INSTANCES,
		DELAYS,
		OFFSETS
	}
	
	/**
	 * Creates a new view object and sets the appropriate signals.
	 * @param signals A flag indicating if the signals should be connected.
	 * @return The new view object.
	 */
	private QTreeView createNewView(boolean signals){
		QTreeView newView = new QTreeView();
		newView.setMouseTracking(true);
		newView.setRootIsDecorated(false);
		newView.setAlternatingRowColors(true);
		if(signals){
			newView.clicked.connect(this, "singleClick(QModelIndex)");
			newView.entered.connect(this, "onHover(QModelIndex)");
		}
		newView.setEditTriggers(EditTrigger.NoEditTriggers);
		return newView;
	}
	
	/**
	 * Initializes the filter window
	 * @param parent The parent Qt widget class.
	 * @param type The type of filter window to instantiate.
	 */
	public FilterWindow(QWidget parent, FilterType type){
		super(parent);
		this.type = type;
		this.explorer = (DesignExplorer) parent;
		
		view = createNewView(true);
		
		hyperlink.setUnderline(true);
		
		loadCurrentDesignData();
		
        filterPatternLineEdit = new QLineEdit();
        filterPatternLabel = new QLabel(tr("&Filter pattern:"));
        filterPatternLabel.setBuddy(filterPatternLineEdit);

        filterSyntaxComboBox = new QComboBox();
        filterSyntaxComboBox.addItem(tr("Regular expression"),
                                     QRegExp.PatternSyntax.RegExp);
        filterSyntaxComboBox.addItem(tr("Wildcard"),
                                     QRegExp.PatternSyntax.Wildcard);
        filterSyntaxComboBox.addItem(tr("Fixed string"),
                                     QRegExp.PatternSyntax.FixedString);
        
        filterCaseSensitivityCheckBox = new QCheckBox(tr("Case sensitive"));
        filterCaseSensitivityCheckBox.setChecked(true);
        
        filterPatternLineEdit.textChanged.connect(this, "textFilterChanged()");
        filterSyntaxComboBox.currentIndexChanged.connect(this, "textFilterChanged()");
        filterCaseSensitivityCheckBox.toggled.connect(this, "textFilterChanged()");
        
        proxyLayout = new QGridLayout();
        proxyLayout.addWidget(view, 0, 0, 1, 4);
        proxyLayout.addWidget(filterPatternLabel, 1, 0);
        proxyLayout.addWidget(filterPatternLineEdit, 1, 1);
        proxyLayout.addWidget(filterSyntaxComboBox, 1, 2);
        proxyLayout.addWidget(filterCaseSensitivityCheckBox, 1, 3);
		
        switch(type){
        	case INSTANCES:
        		subViews = new QTreeView[1];
        		subViews[0] = createNewView(false);
        		proxyLayout.addWidget(new QLabel("Attributes"));
        		proxyLayout.addWidget(subViews[0], 3, 0, 1, 5);
        		subModels = new QStandardItemModel[1];
        		subModels[0] = new QStandardItemModel(0, 3, this);
        		setHeaders(subModels[0], new String[]{"Physical Name", "Logical Name", "Value"});
        		subViews[0].setModel(subModels[0]);
        		break;
        	case NETS:
        		subViews = new QTreeView[2];
        		subModels = new QStandardItemModel[2];
        		
        		subViews[0] = createNewView(true);        		
        		proxyLayout.addWidget(new QLabel("Pins"));
        		proxyLayout.addWidget(subViews[0], 3, 0, 1, 5);
        		
        		subModels[0] = new QStandardItemModel(0, 3, this);
        		subModels[0].setObjectName("Pins");
        		setHeaders(subModels[0], new String[]{"Direction", "Instance Name", "Pin Name"});
        		subViews[0].setModel(subModels[0]);
        		
        		subViews[1] = createNewView(true);        		
        		proxyLayout.addWidget(new QLabel("PIPs"));
        		proxyLayout.addWidget(subViews[1], 6, 0, 1, 5);
        		subModels[1] = new QStandardItemModel(0, 3, this);
        		subModels[1].setObjectName("PIPs");
        		setHeaders(subModels[1], new String[]{"Tile", "Start Wire", "End Wire"});
        		subViews[1].setModel(subModels[1]);
        		break;
        	case MODULES:
        		break;        		
        	case MODULE_INSTANCES:
        		break; 
        	case DELAYS:
        		subViews = new QTreeView[1];
        		subViews[0] = createNewView(true);
        		proxyLayout.addWidget(new QLabel("Maximum Data Path"));
        		proxyLayout.addWidget(subViews[0], 3, 0, 1, 5);
        		subModels = new QStandardItemModel[1];
        		subModels[0] = new QStandardItemModel(0, 4, this);
        		subModels[0].setObjectName("Max");
        		setHeaders(subModels[0], new String[]{"Location", "Delay Type", "Delay (ns)", "Physical Resource"});
        		subViews[0].setModel(subModels[0]);
        		break;
        	case OFFSETS:
        		subViews = new QTreeView[2];
        		subModels = new QStandardItemModel[2];
        		
        		subViews[0] = createNewView(true);        		
        		proxyLayout.addWidget(new QLabel("Maximum Data Path"));
        		proxyLayout.addWidget(subViews[0], 3, 0, 1, 5);
        		
        		subModels[0] = new QStandardItemModel(0, 4, this);
        		subModels[0].setObjectName("Max");
        		setHeaders(subModels[0], new String[]{"Location", "Delay Type", "Delay (ns)", "Physical Resource"});
        		subViews[0].setModel(subModels[0]);
        		
        		subViews[1] = createNewView(true);        		
        		proxyLayout.addWidget(new QLabel("Minimum Data Path"));
        		proxyLayout.addWidget(subViews[1], 6, 0, 1, 5);
        		subModels[1] = new QStandardItemModel(0, 4, this);
        		subModels[1].setObjectName("Min");
        		setHeaders(subModels[1], new String[]{"Location", "Delay Type", "Delay (ns)", "Physical Resource"});
        		subViews[1].setModel(subModels[1]);
        		break;
        }
        setLayout(proxyLayout);        
        textFilterChanged();
	}
	
	/**
	 * Populates the window with appropriate design/timing data.
	 */
	public void loadCurrentDesignData(){
		switch(this.type){
			case NETS:
				model = new QStandardItemModel(0, 7, this);
				setHeaders(model, new String[]
				          {"Name", "Type", "Source Instance", "Fanout", "PIP Count", "Module Instance Name", "Module Name"});
				if(explorer.design == null) break;
				for(XdlNet net : explorer.design.getNets()){
		        	if(net.getPins().size() == 0) continue; 
	        		ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
	        		items.add(new DesignItem(net.getName(), net));
	        		items.add(new DesignItem(net.getType().toString(), net));
	        		items.add(createNewHyperlinkItem(net.getSource() == null ? null : net.getSource().getInstanceName(), net));
	        		items.add(new DesignItem(String.format("%3d", net.getPins().size()-1), net));
	        		items.add(new DesignItem(String.format("%5d", net.getPIPs().size()), net));
	        		items.add(createNewHyperlinkItem(net.getModuleInstance()==null ? null : net.getModuleInstance().getName(), net));
	        		items.add(createNewHyperlinkItem(net.getModuleTemplate()==null ? null : net.getModuleTemplate().getName(), net));
	        		model.appendRow(items);			        		
		        }
	            break;
			case INSTANCES:
				model = new QStandardItemModel(0, 6, this);
				setHeaders(model, new String[]
				          {"Name", "Type", "Primitive Site", "Tile", "Module Instance Name", "Module Name"});
				if(explorer.design == null) break;
            	for(XdlInstance instance : explorer.design.getInstances()){
            		ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
            		items.add(new DesignItem(instance.getName(), instance));
            		items.add(new DesignItem(instance.getType().toString(), instance));
            		items.add(createNewHyperlinkItem(instance.getPrimitiveSiteName(), instance));
            		items.add(createNewHyperlinkItem(instance.getPrimitiveSite()==null ? null : instance.getPrimitiveSite().getTile().toString(), instance));	            		
            		items.add(createNewHyperlinkItem(instance.getModuleInstanceName(), instance));
            		items.add(createNewHyperlinkItem(instance.getModuleTemplate()==null ? null : instance.getModuleTemplate().getName(), instance));
	        		model.appendRow(items);
                }
	            break;
			case MODULES:
				model = new QStandardItemModel(0, 6, this);
				setHeaders(model, new String[]
				          {"Name", "Anchor Name", "Anchor Site", "Instance Count", "Net Count", "Port Count"});
				if(explorer.design == null) break;
	        	for(XdlModule module : explorer.design.getModules()){
            		ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
            		items.add(new DesignItem(module.getName(), module));
            		items.add(new DesignItem(module.getAnchor().getName(), module));
            		items.add(createNewHyperlinkItem(module.getAnchor().getPrimitiveSiteName(), module));
	        		items.add(new DesignItem(String.format("%5d", module.getInstances().size(), module)));
	        		items.add(new DesignItem(String.format("%5d", module.getNets().size(), module)));
	        		items.add(new DesignItem(String.format("%5d", module.getPorts().size(), module)));
	        		model.appendRow(items);
		        }
	            break;
			case MODULE_INSTANCES:
				model = new QStandardItemModel(0, 4, this);
				setHeaders(model, new String[]
				          {"Name", "Anchor Name", "Anchor Site", "Module Template"});
				if(explorer.design == null) break;
	        	for(XdlModuleInstance moduleInstance : explorer.design.getModuleInstances()){
	        		ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
            		items.add(new DesignItem(moduleInstance.getName(), moduleInstance));
            		items.add(new DesignItem(moduleInstance.getAnchor().getName(), moduleInstance));
            		items.add(createNewHyperlinkItem(moduleInstance.getAnchor().getPrimitiveSiteName(), moduleInstance));
            		items.add(createNewHyperlinkItem(moduleInstance.getModule().getName(), moduleInstance));
	        		model.appendRow(items);
		        }
	            break;
			case DELAYS:
				model = new QStandardItemModel(0, 5, this);
				setHeaders(model, new String[]
				          {"Delay", "Source", "Destination", "Data Path Delay", "Levels of Logic"});
	            if(explorer.delays == null) break;
            	for(PathDelay pd : explorer.delays){
            		ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
            		items.add(new DesignItem(String.format("%5.3f", pd.getDelay()) + "ns", pd));
            		items.add(new DesignItem(pd.getSource(), pd));
            		items.add(new DesignItem(pd.getDestination(), pd));
            		items.add(new DesignItem(String.format("%5.3f", pd.getDataPathDelay()) + "ns", pd));
            		items.add(new DesignItem(String.format("%5d", pd.getLevelsOfLogic(), pd)));
            		model.appendRow(items);
            	}
				break;
			case OFFSETS:
				model = new QStandardItemModel(0, 7, this);
				setHeaders(model, new String[]
				          {"Offset", "Source", "Destination", "Data Path Delay", "Levels of Logic (Data)", "Clock Path Delay", "Levels of Logic (Clock)"});
	            if(explorer.offsets == null) break;
            	for(PathOffset po : explorer.offsets){
            		ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();	            		
            		items.add(new DesignItem(String.format("%5.3f", po.getOffset()) + "ns", po));
            		items.add(new DesignItem(po.getSource(), po));
            		items.add(new DesignItem(po.getDestination(), po));
            		items.add(new DesignItem(String.format("%5.3f", po.getDataPathDelay()) + "ns", po));
            		items.add(new DesignItem(String.format("%5d", po.getLevelsOfLogic(), po)));
            		items.add(new DesignItem(String.format("%5.3f", po.getClockPathDelay()) + "ns", po));
            		items.add(new DesignItem(String.format("%5d", po.getClockLevelsOfLogic(), po)));
            		model.appendRow(items);
            	}
				break;
		}

		proxyModel = new MySortFilterProxyModel(this);
        proxyModel.setSourceModel(model);
        proxyModel.setDynamicSortFilter(true);
        this.selectionModel = new QItemSelectionModel(proxyModel);
        
        view.setModel(proxyModel);
        view.setSelectionModel(selectionModel);
        
        view.setSortingEnabled(true);
        view.sortByColumn(0, Qt.SortOrder.AscendingOrder);
        
	}
	
	/**
	 * This method enables populating lower tables with data based on clicked
	 * data from the main table.  It also serves to implement the hyperlink
	 * functionality of the program.  This method is only called by Qt.
	 * @param index Index of the object that was clicked on.
	 */
	protected void singleClick(QModelIndex index){
		String data = (String)index.data();
		switch(type){
			case NETS:
				if(index.model().objectName().equals("Pins")){
					if(index.column() == 1) switchToInstanceTab(data);
					break;
				}
				else if(index.model().objectName().equals("PIPs")){
					if(index.column() == 0) switchToTileTab(data);
					break;
				}
				subModels[0].removeRows(0, subModels[0].rowCount());
				subModels[1].removeRows(0, subModels[1].rowCount());
				
				XdlNet net = explorer.design.getNet(proxyModel.data(index.row(),0).toString());
				// Populate Pins
				for(XdlPin pin : net.getPins()){
					ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
					items.add(new DesignItem(pin.getPinType().toString().toLowerCase(), pin));
					items.add(createNewHyperlinkItem(pin.getInstanceName(), pin)); 
					items.add(new DesignItem(pin.getName(), pin));
					subModels[0].appendRow(items);
				}
				subViews[0].setSortingEnabled(true);
				subViews[0].sortByColumn(0, Qt.SortOrder.AscendingOrder);
				// Populate PIPs
				for(PIP pip : net.getPIPs()){
					ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
					items.add(createNewHyperlinkItem(pip.getTile().getName(), pip));
					items.add(new DesignItem(pip.getStartWire().getWireName(), pip));
					items.add(new DesignItem(pip.getEndWire().getWireName(), pip));
					subModels[1].appendRow(items);
				}
				subViews[1].setSortingEnabled(true);
				subViews[1].sortByColumn(0, Qt.SortOrder.AscendingOrder);

				if(index.column() == 2){
					switchToInstanceTab(data);
					break;
				}
				else if(index.column() == 5){ // Module Instance
					switchToModuleInstanceTab(data);
					break;
				}
				else if(index.column() == 6){ // Module
					switchToModuleTab(data);
					break;
				}
				break;
			case INSTANCES:
				// populate attributes
				subModels[0].removeRows(0, subModels[0].rowCount());
				XdlInstance instance = explorer.design.getInstance(proxyModel.data(index.row(),0).toString());
				for(XdlAttribute attribute : instance.getAttributes()){
					ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
					items.add(new DesignItem(attribute.getPhysicalName(), attribute));
					items.add(new DesignItem(attribute.getLogicalName(), attribute));
					items.add(new DesignItem(attribute.getValue(), attribute));
					subModels[0].appendRow(items);
				}
				subViews[0].setSortingEnabled(true);
				subViews[0].sortByColumn(0, Qt.SortOrder.AscendingOrder);
				
				// check for hyperlinks
				if(index.column() == 2){
					switchToTileTab(explorer.device.getPrimitiveSite(data).getTile().getName());
				}
				else if(index.column() == 3){
					switchToTileTab(data);
				}
				else if(index.column() == 3){
					switchToTileTab(data);
				}
				else if(index.column() == 4){
					switchToModuleInstanceTab(data);
				}
				else if(index.column() == 5){
					switchToModuleTab(data);
				}
				break;
			case MODULES:
				if(index.column() == 2) {
					String tileName = explorer.device.getPrimitiveSite(data).getTile().getName();
					switchToTileTab(tileName);
				}
				break;
			case MODULE_INSTANCES:
				if(index.column() == 2){
					String tileName = explorer.device.getPrimitiveSite(data).getTile().getName();
					switchToTileTab(tileName);
				}
				else if(index.column() == 3) switchToModuleTab((String)index.data());
				break;
			case DELAYS:
				if(index.model().objectName().equals("Max")){
					if(index.column() == 0){
						Tile t = explorer.device.getPrimitiveSite(data.substring(0, data.indexOf('.'))).getTile();
						switchToTileTab(t.getName());
					}
					else if(index.column() == 3){
						if(((String)index.model().data(index.row(), 1)).startsWith("net")){
							switchToNetTab(data);
						}
						else{
							switchToInstanceTab(data);
						}
					}
				}
				else{
					subModels[0].removeRows(0, subModels[0].rowCount());
					subViews[0].setSortingEnabled(true);
					subViews[0].sortByColumn(0, Qt.SortOrder.AscendingOrder);
					Path pd = (Path)((DesignItem)model.itemFromIndex(proxyModel.mapToSource(index))).refObject;
					for(PathElement pe : pd.getMaxDataPath()){
						if(pe.getClass().equals(LogicPathElement.class)){
							LogicPathElement lpe = (LogicPathElement) pe;
							ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
							items.add(createNewHyperlinkItem(lpe.getInstance().getPrimitiveSiteName() + "." + (lpe.getPin()==null ? "<null>":lpe.getPin().getName()), pd));
							items.add(new DesignItem(lpe.getType().toString(), pd));
							items.add(new DesignItem(String.format("%5.3f", lpe.getDelay(), pd)));
							items.add(createNewHyperlinkItem(lpe.getInstance().getName(), pd));
							subModels[0].appendRow(items);
						}
						else{
							RoutingPathElement rpe = (RoutingPathElement) pe;
							ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
							items.add(createNewHyperlinkItem(rpe.getPin().getInstance().getPrimitiveSiteName() + "." + rpe.getPin().getName(), pd));
							items.add(new DesignItem(rpe.getType().toString() + " (fanout=" + rpe.getNet().getFanOut() + ")", pd));
							items.add(new DesignItem(String.format("%5.3f", rpe.getDelay(), pd)));
							items.add(createNewHyperlinkItem(rpe.getNet().getName(), pd));
							subModels[0].appendRow(items);
						}
					}					
				}
				break;
			case OFFSETS:
				if(index.model().objectName().equals("Max")){
					if(index.column() == 0){
						Tile t = explorer.device.getPrimitiveSite(data.substring(0, data.indexOf('.'))).getTile();
						switchToTileTab(t.getName());
					}
					else if(index.column() == 3){
						if(((String)index.model().data(index.row(), 1)).startsWith("NET")){
							switchToNetTab(data);
						}
						else{
							switchToInstanceTab(data);
						}
					}
				}
				else if(index.model().objectName().equals("Min")){
					if(index.column() == 0){
						Tile t = explorer.device.getPrimitiveSite(data.substring(0, data.indexOf('.'))).getTile();
						switchToTileTab(t.getName());
					}
					else if(index.column() == 3){
						if(((String)index.model().data(index.row(), 1)).startsWith("NET")){
							switchToNetTab(data);
						}
						else{
							switchToInstanceTab(data);
						}
					}
				}
				else{
					subModels[0].removeRows(0, subModels[0].rowCount());
					subModels[1].removeRows(0, subModels[1].rowCount());
					subViews[0].setSortingEnabled(true);
					subViews[0].sortByColumn(0, Qt.SortOrder.AscendingOrder);
					subViews[1].setSortingEnabled(true);
					subViews[1].sortByColumn(0, Qt.SortOrder.AscendingOrder);
					Path pd2 = (Path)((DesignItem)model.itemFromIndex(proxyModel.mapToSource(index))).refObject;
					for(PathElement pe : pd2.getMaxDataPath()){
						if(pe.getClass().equals(LogicPathElement.class)){
							LogicPathElement lpe = (LogicPathElement) pe;
							ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
							items.add(createNewHyperlinkItem(lpe.getInstance().getPrimitiveSiteName() + "." + lpe.getPin().getName(), pd2));
							items.add(new DesignItem(lpe.getType().toString(), pd2));
							items.add(new DesignItem(String.format("%5.3f", lpe.getDelay()), pd2));
							items.add(createNewHyperlinkItem(lpe.getInstance().getName(), pd2));
							subModels[0].appendRow(items);
						}
						else{
							RoutingPathElement rpe = (RoutingPathElement) pe;
							ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
							items.add(createNewHyperlinkItem(rpe.getPin().getInstance().getPrimitiveSiteName() + "." + rpe.getPin().getName(), pd2));
							items.add(new DesignItem(rpe.getType().toString() + " (fanout=" + rpe.getNet().getFanOut() + ")", pd2));
							items.add(new DesignItem(String.format("%5.3f", rpe.getDelay()), pd2));
							items.add(createNewHyperlinkItem(rpe.getNet().getName(), pd2));
							subModels[0].appendRow(items);
						}
					}
					
					for(PathElement pe : ((PathOffset)pd2).getMinDataPath()){
						if(pe.getClass().equals(LogicPathElement.class)){
							LogicPathElement lpe = (LogicPathElement) pe;
							ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
							items.add(createNewHyperlinkItem(lpe.getInstance().getPrimitiveSiteName() + "." + lpe.getPin().getName(), pd2));
							items.add(new DesignItem(lpe.getType().toString(), pd2));
							items.add(new DesignItem(String.format("%5.3f", lpe.getDelay()), pd2));
							items.add(createNewHyperlinkItem(lpe.getInstance().getName(), pd2));
							subModels[1].appendRow(items);
						}
						else{
							RoutingPathElement rpe = (RoutingPathElement) pe;
							ArrayList<QStandardItem> items = new ArrayList<QStandardItem>();
							items.add(createNewHyperlinkItem(rpe.getPin().getInstance().getPrimitiveSiteName() + "." + rpe.getPin().getName(), pd2));
							items.add(new DesignItem(rpe.getType().toString() + " (fanout=" + rpe.getNet().getFanOut() + ")", pd2));
							items.add(new DesignItem(String.format("%5.3f", rpe.getDelay()), pd2));
							items.add(createNewHyperlinkItem(rpe.getNet().getName(), pd2));
							subModels[1].appendRow(items);
						}
					}					
				}
				break;
		}
	}
	
	/**
	 * This method is called when the mouse enters a new data cell in a table.
	 * This method will change the mouse from an arrow to a pointing finger
	 * to indicate to the user that the data is hyperlinked.
	 * @param index Index of the data that was clicked on.
	 */
	protected void onHover(QModelIndex index){
		if(((String)index.data()).equals("")){
			view.setCursor(arrow);
			if(subViews != null && subViews.length > 0){
				subViews[0].setCursor(arrow);
				if(subViews.length > 1){
					subViews[1].setCursor(arrow);					
				}
			}
			return;
		}
		switch(type){
			case NETS:
				if(index.model().objectName().equals("Pins")){
					if(index.column() == 1) subViews[0].setCursor(pointingFinger);
					else subViews[0].setCursor(arrow);
					break;
				}
				else if(index.model().objectName().equals("PIPs")){
					if(index.column() == 0) subViews[1].setCursor(pointingFinger);
					else subViews[1].setCursor(arrow);
					break;
				}
				if(index.column() == 2 || index.column() > 4) view.setCursor(pointingFinger);
				else view.setCursor(arrow);
				break;
			case INSTANCES:
				if(index.column() > 1) view.setCursor(pointingFinger);
				else view.setCursor(arrow);
				break;
			case MODULES:
				if(index.column() == 2) view.setCursor(pointingFinger);
				else view.setCursor(arrow);
				break;
			case MODULE_INSTANCES:
				if(index.column() > 1) view.setCursor(pointingFinger);
				else view.setCursor(arrow);
				break;
			case DELAYS:
				if(index.column() == 0 || index.column() == 3) subViews[0].setCursor(pointingFinger);
				else subViews[0].setCursor(arrow);
				break;
			case OFFSETS:
				if(index.model().objectName().equals("Max")){
					if(index.column() == 0 || index.column() == 3) subViews[0].setCursor(pointingFinger);
					else subViews[0].setCursor(arrow);
					break;
				}
				else if(index.model().objectName().equals("Min")){
					if(index.column() == 0 || index.column() == 3) subViews[1].setCursor(pointingFinger);
					else subViews[1].setCursor(arrow);
					break;
				}
				break;

		}
	}
	
	/**
	 * This is a signaled method that executes each time the text box used for filtering
	 * the table entries.
	 */
	private void textFilterChanged(){
        QRegExp.PatternSyntax syntax;
        int index = filterSyntaxComboBox.currentIndex();
        syntax = (QRegExp.PatternSyntax) filterSyntaxComboBox.itemData(index);

        Qt.CaseSensitivity caseSensitivity;
        if (filterCaseSensitivityCheckBox.isChecked())
            caseSensitivity = Qt.CaseSensitivity.CaseSensitive;
        else
            caseSensitivity = Qt.CaseSensitivity.CaseInsensitive;

        QRegExp regExp = new QRegExp(filterPatternLineEdit.text(),
                                     caseSensitivity, syntax);
        proxyModel.setFilterRegExp(regExp);
    }
	
	/**
	 * This is a class to allow sorting of the tables in each window.
	 * @author Chris Lavin
	 */
    private class MySortFilterProxyModel extends QSortFilterProxyModel {
        private MySortFilterProxyModel(QObject parent) {
            super(parent);
        }
        @Override
        protected boolean filterAcceptsRow(int sourceRow, QModelIndex sourceParent){
            QRegExp filter = filterRegExp();
            QAbstractItemModel model = sourceModel();
            boolean matchFound = false;

            for(int i=0; i < model.columnCount(); i++){
            	Object data = model.data(sourceModel().index(sourceRow, i, sourceParent));
            	matchFound |= data != null && filter.indexIn(data.toString()) != -1;
            }
            return matchFound;
        }

        @Override
        protected boolean lessThan(QModelIndex left, QModelIndex right) {
            boolean result = false;
            Object leftData = sourceModel().data(left);
            Object rightData = sourceModel().data(right);

            QRegExp emailPattern = new QRegExp("([\\w\\.]*@[\\w\\.]*)");

            String leftString = leftData.toString();
            if(left.column() == 1 && emailPattern.indexIn(leftString) != -1)
                leftString = emailPattern.cap(1);

            String rightString = rightData.toString();
            if(right.column() == 1 && emailPattern.indexIn(rightString) != -1)
                rightString = emailPattern.cap(1);

            result = leftString.compareTo(rightString) < 0;
            return result;
        }
    }
    
    private DesignItem createNewHyperlinkItem(String value, Object refObject){
    	DesignItem item = new DesignItem(value, refObject);
    	item.setFont(hyperlink);
		item.setForeground(blue);
		return item;
    }
    
    private void setHeaders(QStandardItemModel model, String[] headers){
    	for (int i = 0; i < headers.length; i++) {
			model.setHeaderData(i, Orientation.Horizontal, headers[i]);
		}
    }
    
    /**
     * Helper method to switch to a Net Tab
     * @param data Name of the net
     */
    private void switchToNetTab(String data){
    	if(data.equals("")) return;
    	explorer.tabs.setCurrentWidget(explorer.netWindow);
		for(int i = 0; i < explorer.netWindow.proxyModel.rowCount(); i++){
			if(explorer.netWindow.proxyModel.data(i, 0).toString().equals(data)){
				QModelIndex dstIndex = explorer.netWindow.proxyModel.index(i, 0);
				explorer.netWindow.view.scrollTo(dstIndex,ScrollHint.PositionAtCenter);
				explorer.netWindow.selectionModel.setCurrentIndex(dstIndex, QItemSelectionModel.SelectionFlag.SelectCurrent);
			}
		}
    }
    
    /**
     * Helper method to switch to the Instance Tab
     * @param data Name of the instance
     */
    private void switchToInstanceTab(String data){
    	if(data.equals("")) return;
    	explorer.tabs.setCurrentWidget(explorer.instanceWindow);
		for(int i = 0; i < explorer.instanceWindow.proxyModel.rowCount(); i++){
			if(explorer.instanceWindow.proxyModel.data(i, 0).toString().equals(data)){
				QModelIndex dstIndex = explorer.instanceWindow.proxyModel.index(i, 0);
				explorer.instanceWindow.view.scrollTo(dstIndex,ScrollHint.PositionAtCenter);
				explorer.instanceWindow.selectionModel.setCurrentIndex(dstIndex, QItemSelectionModel.SelectionFlag.SelectCurrent);
			}
		}
    }
    
    /**
     * Helper method to switch to the Module Tab
     * @param data Name of the module name
     */
    private void switchToModuleTab(String data){
    	if(data.equals("")) return;
    	explorer.tabs.setCurrentWidget(explorer.moduleWindow);
		for(int i = 0; i < explorer.moduleWindow.proxyModel.rowCount(); i++){
			if(explorer.moduleWindow.proxyModel.data(i, 0).toString().equals(data)){
				QModelIndex dstIndex = explorer.moduleWindow.proxyModel.index(i, 0);
				explorer.moduleWindow.view.scrollTo(dstIndex,ScrollHint.PositionAtCenter);
				explorer.moduleWindow.selectionModel.setCurrentIndex(dstIndex, QItemSelectionModel.SelectionFlag.SelectCurrent);
			}
		}
    }
    
    /**
     * Helper method to switch to the ModuleInstance Tab
     * @param data Name of the module instance
     */
    private void switchToModuleInstanceTab(String data){
    	if(data.equals("")) return;
    	explorer.tabs.setCurrentWidget(explorer.moduleInstanceWindow);
		for(int i = 0; i < explorer.moduleInstanceWindow.proxyModel.rowCount(); i++){
			if(explorer.moduleInstanceWindow.proxyModel.data(i, 0).toString().equals(data)){
				QModelIndex dstIndex = explorer.moduleInstanceWindow.proxyModel.index(i, 0);
				explorer.moduleInstanceWindow.view.scrollTo(dstIndex,ScrollHint.PositionAtCenter);
				explorer.moduleInstanceWindow.selectionModel.setCurrentIndex(dstIndex, QItemSelectionModel.SelectionFlag.SelectCurrent);
			}
		}    	
    }
    
    /**
     * Helper method to switch to the Tile Tab
     * @param data Name of the tile
     */
    private void switchToTileTab(String data){
    	if(data.equals("")) return;
    	explorer.tabs.setCurrentWidget(explorer.tileWindow);
		explorer.tileWindow.moveToTile(data);    	
    }
    
    public class DesignItem extends QStandardItem{
    	private Object refObject;

    	public DesignItem(){
    		super();
    	}
    	
    	public DesignItem(String text){
    		super(text);
    	}
    	
    	public DesignItem(String text, Object refObject){
    		super(text);
    		this.refObject = refObject;
    	}
    	
		/**
		 * @return the refObject
		 */
		public Object getRefObject() {
			return refObject;
		}

		/**
		 * @param refObject the refObject to set
		 */
		public void setRefObject(Object refObject) {
			this.refObject = refObject;
		}
    	
    	
    }
}
