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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.trolltech.qt.core.QFile;
import com.trolltech.qt.core.QIODevice.OpenModeFlag;
import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.gui.QGraphicsItemInterface;
import com.trolltech.qt.gui.QTreeWidgetItem;
import com.trolltech.qt.xml.QDomDocument;
import com.trolltech.qt.xml.QDomElement;
import com.trolltech.qt.xml.QDomNodeList;

import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.ElementShape;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.PinShape;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Site;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Wire;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.WirePart;

/**
 * This class is designed to allow the user to save and load their progress for a primitive site via XML <br>
 * @author Thomas Townsend
 * Created on: Aug 7, 2014
 */
public class XMLCommands {

	/**Used to parse saved XML files*/
	QDomDocument doc = new QDomDocument("XML");
	/**Maps Element names (A5FF) to their previous location on the graphics scene*/
	HashMap<String, QPointF> elementLocations = new HashMap<String, QPointF>();
	/**Maps Element names (A5FF) to their previous rotation on the scene*/
	HashMap<String, Double> elementRotation = new HashMap<String, Double>(); 
	/**Maps Site pin names (A5) to their previous location on the graphics scene*/
	HashMap<String, QPointF> sitePins = new HashMap<String, QPointF>();
	/**Set of all site pins that have been marked as unconnected in the primitive site*/
	HashSet<String> unconnectedSites = new HashSet<String>();
	
	/************************************************
	 * 		Methods for creating the XML files		*
	 ************************************************/
	public XMLCommands(){}
	/**
	 * Creates an XML element that stores an x-y coordinate
	 * @param name Name of the element
	 * @param pos 
	 * @return
	 */
	private QDomElement saveQPointF(String name, QPointF pos){
		QDomElement element = doc.createElement(name);
		element.setAttribute("xPos", pos.x());
		element.setAttribute("yPos", pos.y());
		return element;
	}
	/**
	 * Creates an XML element that represents a boolean value (true or false)
	 * @param name Name to call the element
	 * @param value
	 * @return
	 */
	private QDomElement saveBoolean(String name, boolean value){
		QDomElement element = doc.createElement(name);
		element.appendChild(doc.createTextNode(value ? "true" : "false" ));
		return element;
	}
	/**
	 * Creates an XML element for a string value (a name for example)
	 * @param name Name of the XML element
	 * @param value
	 * @return
	 */
	private QDomElement saveString (String name, String value) {
		QDomElement element = doc.createElement(name);
		element.appendChild(doc.createTextNode(value));
		return element;
	}
	/**
	 * Creates an XML element that stores the name and last position of an <br>
	 * ElementShape (bel or pip) on the graphics scene
	 * @param graphicsElement 
	 * @return
	 */
	private QDomElement saveElementShape(ElementShape graphicsElement){
		QDomElement element = doc.createElement( "Element" );
		
		element.appendChild(this.saveString("Name", graphicsElement.getTreeElement().getElement().getName() ) );
		element.appendChild(this.saveQPointF("LastPos", graphicsElement.getLastPos()));
		element.setAttribute("rotationAngle", graphicsElement.getRotationAngle());
		
		return element;
	}
	/**
	 * Creates an XML element that stores the name and last position of a SitePin <br>
	 * along with whether that pin is connected to anything
	 * @param sitePin
	 * @return
	 */
	private QDomElement saveSitePinShape(PinShape sitePin){
		QDomElement element = doc.createElement("SitePin");
		
		element.appendChild(this.saveString("Name", sitePin.getTreePin().getPin().getInternalName() ) );
		element.appendChild(this.saveQPointF("LastPos", sitePin.getLastLocation()));
		element.appendChild(this.saveBoolean("isConnected", sitePin.getTreePin().getPin().isConnected()));
		
		return element;
	}
	/**
	 * Creates an XML element that stores the width, height, xPos, and yPos of the opened primitive site <br>
	 * on the graphics scene
	 * @param site
	 * @return
	 */
	private QDomElement saveSite(Site site){
		QDomElement element = doc.createElement("GraphicsSite");
		element.setAttribute("width", site.width());
		element.setAttribute("height", site.height());
		element.setAttribute( "xPos", site.pos().x() );
		element.setAttribute( "yPos", site.pos().y() );
	
		return element;
	}
	/**
	 * 
	 * Creates an XML element that stores all of the necessary information about <br>
	 * each wire on the graphics scene. 
	 * @param wire
	 * @return
	 */
	private QDomElement saveWire(Wire wire){
		QDomElement element = doc.createElement("Wire");
	
		//Save the text represting the connection (=> AF55 A) for example
		element.appendChild(this.saveString("startConn", wire.getTree_start().text(0)));
		element.appendChild(this.saveString("endConn", wire.getTree_end().text(0)));
		//Save the two ends of the wire
		element.appendChild(this.saveQPointF("StartPos", wire.getFirstWirePart().line().p1()));
		element.appendChild(this.saveQPointF("EndPos", wire.getFirstWirePart().line().p2()));
	
		return element;
	}
	/**
	 * Creates an XML element that stores information for each object currently on the <br>
	 * graphics scene.  
	 * @param scene
	 * @param fileLocation
	 * @return
	 */
	public boolean saveGraphicsScene(PrimitiveSiteScene scene, String fileLocation, ArrayList<String> siteCfgOptions){
		QDomDocument document = new QDomDocument("XML");
		
		QDomElement element = document.createElement("GraphicsScene");
		try {
			
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileLocation), Charset.defaultCharset(),
															StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			
			for (QGraphicsItemInterface item : scene.items()) {
				if (item instanceof ElementShape) {
					element.appendChild(this.saveElementShape((ElementShape) item)); 
				}
				else if (item instanceof PinShape){
					element.appendChild(this.saveSitePinShape((PinShape) item)); 
				}
				else if (item instanceof Site){
					element.appendChild(this.saveSite( (Site)item) );
				}
				else if (item instanceof WirePart){
					element.appendChild(this.saveWire(((WirePart) item).getParentWire()));
				}
			}
			
			
			for (String cfg : siteCfgOptions) {
				element.appendChild(this.saveString("SiteCfgOption", cfg));
			}
			
			document.appendChild(element);
			
			writer.write(document.toString());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}	
	
	/************************************************
	 * 		Methods for parsing the XML file		*
	 ************************************************/
	/**
	 * Loads the XML file found at the given location into the a QDomDocument,
	 * and then parses that XML file to retrieve all of the necessary graphics 
	 * object information
	 * @param filename
	 * @return
	 */
	public boolean parseSceneXML (String location){
		doc.clear();
		QFile file = new QFile(location);
		
		if (!file.open(OpenModeFlag.ReadOnly, OpenModeFlag.Text)) 
			return false;
		
		if (!doc.setContent(file).success)
			return false;	
		
		//clear data structures to make way for new information 
		this.elementLocations.clear();
		this.sitePins.clear();
		this.unconnectedSites.clear();
		
		//parse elements and site pins
		QDomElement scene = doc.documentElement();
		this.parseElements(scene);
		this.parseSitePins(scene);

		return true;
	}
	/**
	 * This method finds all of the elements in the QDomDocument <br> 
	 * with the name of "Element", and stores their previous location <br> 
	 * in the elementLocation hashmap.
	 * @param root
	 */
	private void parseElements(QDomElement root) {
		QDomNodeList elements = root.elementsByTagName("Element");
		
		for (int i = 0; i < elements.count(); i++) {
			QDomElement element = elements.at(i).toElement();
			String name = element.elementsByTagName("Name").at(0).toElement().text();
			this.elementRotation.put(name, new Double (Double.parseDouble(element.attribute("rotationAngle") ) ) );
			
			QDomElement pos = element.elementsByTagName("LastPos").at(0).toElement();
			
			double y = Double.parseDouble( pos.attribute("yPos") );
			double x = Double.parseDouble( pos.attribute("xPos") );
			this.elementLocations.put(name, new QPointF(x, y));
		}
	}
	
	/**
	 * This method finds all of elements in the QDomDocument <br>
	 * with the name of "SitePin", and stores their previous location <br> 
	 * in the sitePins hashmap.  Also, if they are not connected <br>
	 * the site pin is added to the unconnected hashset.
	 * @param root
	 * @param root
	 */
	private void parseSitePins(QDomElement root) {
		QDomNodeList sitePins = root.elementsByTagName("SitePin");
		
		for(int i = 0; i < sitePins.count(); i++){
			QDomElement sitePin = sitePins.at(i).toElement();
			String name = sitePin.elementsByTagName("Name").at(0).toElement().text();
			
			QDomElement pos = sitePin.elementsByTagName("LastPos").at(0).toElement();
			
			double y = Double.parseDouble( pos.attribute("yPos") );
			double x = Double.parseDouble( pos.attribute("xPos") );
			
			this.sitePins.put(name, new QPointF(x , y));
			
			String connected = sitePin.elementsByTagName("isConnected").at(0).toElement().text();
			
			if ( connected.equals("false") ) 
				this.unconnectedSites.add(name);
		}
	}
	/**
	 * This method finds all of elements in the QDomDocument <br>
	 * with the name of "Wire", creates a new wire with the <br>
	 * extracted information, and adds the wire to the graphics scene. <br>
	 * This method should be called AFTER all of the bels, pips, and site pins <br>
	 * have been added to the graphics scene and placed in their previous locations
	 * @param scene
	 */
	public void loadWires (PrimitiveSiteScene scene){
		QDomElement root = doc.documentElement();
		QDomNodeList wires = root.elementsByTagName("Wire");

		for (int i = 0; i < wires.count(); i++){
			QDomElement wire = wires.at(i).toElement();
			String startConnText = wire.elementsByTagName("startConn").at(0).toElement().text();
			String endConnText = wire.elementsByTagName("endConn").at(0).toElement().text();
			
			QDomElement startPos = wire.elementsByTagName("StartPos").at(0).toElement();
			
			double starty = Double.parseDouble( startPos.attribute("yPos") );
			double startx = Double.parseDouble( startPos.attribute("xPos") );
			QPointF startPoint = new QPointF(startx, starty); 
			
			
			QDomElement endPos = wire.elementsByTagName("EndPos").at(0).toElement();
			
			double endy = Double.parseDouble( endPos.attribute("yPos") );
			double endx = Double.parseDouble( endPos.attribute("xPos") );
			QPointF endPoint = new QPointF(endx, endy); 
			
			QTreePin startPin = scene.getTreePin(startPoint);
			QTreePin endPin   = scene.getTreePin( endPoint );
			
			if (startPin != null && endPin !=null) {
				QTreeWidgetItem startConn = new QTreeWidgetItem(startPin);
				startConn.setText(0, startConnText);
				QTreeWidgetItem endConn = new QTreeWidgetItem(endPin);
				endConn.setText(0, endConnText);
				
				Wire graphicsWire = new Wire(startPoint, endPoint);
				graphicsWire.setTree_start(startConn);
				graphicsWire.setTree_end(endConn);
				
				startPin.add_wire(graphicsWire);
				endPin.add_wire(graphicsWire);
				
				graphicsWire.addWireToScene(scene);
				scene.updateTreeView(startPin, endPin);
			}
		}
	}
	
	/**
	 * Extracts each saved site config elements and returns them in an ArrayList 
	 * @return ArrayList of site config elements
	 */
	public ArrayList<String> loadSiteCfgElements(){
		QDomElement root = doc.documentElement();
		QDomNodeList cfgOptions = root.elementsByTagName("SiteCfgOption");
		ArrayList<String> tmp = new ArrayList<String>();
		
		for (int i = 0; i < cfgOptions.count(); i++){
			QDomElement cfg = cfgOptions.at(i).toElement();
			tmp.add( cfg.text() );
		}
		
		return tmp;
	}
	
	/**
	 * This method finds the element within the XML file with the name of "GraphiscSite" <br>
	 * and returns the saved width attribute.
	 * @return
	 */
	public double getGraphicsSiteWidth(){
		return Double.parseDouble(doc.documentElement().elementsByTagName("GraphicsSite").at(0).toElement().attribute("width")); 
	}
	/**
	 * This method finds the element within the XML file with the name of "GraphiscSite" <br>
	 * and returns the saved height attribute.
	 * @return
	 */
	public double getGraphicsSiteHeight(){
		return Double.parseDouble(doc.documentElement().elementsByTagName("GraphicsSite").at(0).toElement().attribute("height"));
	}
	
	public QPointF getGraphicsSitePos(){
		double x = Double.parseDouble(doc.documentElement().elementsByTagName("GraphicsSite").at(0).toElement().attribute("xPos"));
		double y = Double.parseDouble(doc.documentElement().elementsByTagName("GraphicsSite").at(0).toElement().attribute("yPos")); 
		
		return new QPointF(x, y);
	}
	/**
	 * Returns the previous location of the Primitive Def Element with <br>
	 * the given name.  If no location was found, null is returned. 
	 * @param elementName
	 * @return
	 */
	public QPointF getSavedElementLocation(String elementName){
		return this.elementLocations.get(elementName);
	}
	
	public double getSavedElementRotation(String elementName) {
		return this.elementRotation.get(elementName).doubleValue();
	}
	/**
	 * Returns the previous location of the Primitive Site Pin with <br>
	 * the given name.  If no location was found, null is returned. 
	 * @param pinName
	 * @return
	 */
	public QPointF getSavedSitePinLocation(String pinName){
		return this.sitePins.get(pinName);
	}
	
	/**
	 * Returns true if the Primitive Site Pin with the give name <br>
	 * is unconnected.
	 * @param elementName
	 * @return
	 */
	public boolean sitePinUnconnected (String pinName){
		return this.unconnectedSites.contains(pinName); 
	}
}//end class
