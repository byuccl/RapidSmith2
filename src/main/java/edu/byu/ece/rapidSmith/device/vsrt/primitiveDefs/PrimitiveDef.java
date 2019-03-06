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
package edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class PrimitiveDef implements Serializable{

	private static final long serialVersionUID = -7246158565182505932L;
	private String type;
	private ArrayList<PrimitiveDefPin> pins;
	private ArrayList<Element> elements;
	
	/**These members are used for testing only*/
	/******************************************/
	private HashMap<String, PrimitiveDefPin> pinSet; 
	private HashMap<String, Element> elementSet; 
	/******************************************/
	//added for use with the vxdlTool only
	private int numElements;
	private ArrayList<String> siteCfgElements = new ArrayList<String>();
	
	private HashSet<String> pipNames;
	private HashSet<String> belPinNames;
	private HashSet<String> sitePinNames;
	private Element bel = null;
	private boolean isSingleBelSite;

	public PrimitiveDef(){
		setType(null);
		pins = new ArrayList<PrimitiveDefPin>();
		elements = new ArrayList<Element>();
		pinSet = new HashMap<String, PrimitiveDefPin>();
		elementSet = new HashMap<String, Element>();
		isSingleBelSite = false;
	}
	
	public void setIsSingleBelSite(boolean isSingleBel) {
		this.isSingleBelSite = isSingleBel;
	}
	
	public boolean isSingleBelSite() {
		return this.isSingleBelSite;
	}
	
	public void addPin(PrimitiveDefPin p){
		pins.add(p);
		pinSet.put(p.getInternalName(), p);
	}
	public void addElement(Element e){
		elements.add(e);
		elementSet.put(e.getName(), e);
	}
	public void addSiteCfgOptions(ArrayList<String> cfgs){
		this.siteCfgElements = cfgs; 
	}
	// Setters and Getters
	public void setType(String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}
	public ArrayList<PrimitiveDefPin> getPins() {
		return pins;
	}
	public void setPins(ArrayList<PrimitiveDefPin> pins) {
		this.pins = pins;
		
		for (PrimitiveDefPin pin : pins) {
			this.pinSet.put(pin.getInternalName(), pin);
		}
	}
	public ArrayList<Element> getElements() {
		return elements;
	}
	public void setElements(ArrayList<Element> elements) {
		this.elements = elements;
		
		for (Element element : elements) {
			this.elementSet.put(element.getName(), element);
		}
	}
	private void calculateTotalNumElements(){
		this.numElements = 0;
		for (Element element : this.elements) {
			this.numElements += 1 + ((element.getCfgElements() != null) ?  element.getCfgElements().size() : 0);
		}
		this.numElements += this.siteCfgElements.size();
	}
	
	public int belCount(){
		int count = 0;
		
		for (Element element : this.elements) 
			count += element.isBel() ? 1 : 0;
		
		return count;
	}
	
	//for testing only 
	public PrimitiveDefPin getPin(PrimitiveDefPin p){
		return this.pinSet.get( p.getInternalName() );
	}
	
	public Element getElement(Element e){
		return this.elementSet.get(e.getName()); 
	}
		
	/**
	 * Initializes the primitive site for automatic .def generation
	 * @param alternate
	 */
	public void initializeOneBelPrimitiveDef (boolean alternate) {
		//Get INV pips and the single bel name
		pipNames = new HashSet<String>();
		belPinNames = new HashSet<String>();
		sitePinNames = new HashSet<String>();
	
		for (Element element : this.elements) {
			if (element.isBel()) {
				bel = element;
				
				for (PrimitiveDefPin pin : element.getPins()) 
					belPinNames.add(pin.getInternalName());
			}
			else if (element.isPin())
				sitePinNames.add(element.getName());
			else 
				pipNames.add(element.getName());
		}
		
		//should I add missing site pins before removing the unconnected pins? 
		this.removeUnconnectedPins();
		
		if (alternate)
			this.addMissingSitePins();
	}
	
	/**
	 * When processing an alternate site, site pins can be missing. <br>
	 * This method creates any site pins where there are corresponding bel pins
	 */
	private void addMissingSitePins(){
		for (PrimitiveDefPin pin : bel.getPins()) {
			if (!sitePinNames.contains( pin.getInternalName() )){
				
				sitePinNames.add( pin.getInternalName() );
				
				//create a primitive def pin object
				PrimitiveDefPin p = new PrimitiveDefPin();
				p.setInternalName( pin.getInternalName() );
				p.setExternalName( pin.getInternalName() );
				p.setDirection(pin.getDirection());
				
				this.addPin(p);
				
				//Also create a new element object.
				Element e = new Element(); 
				PrimitiveDefPin p2 = new PrimitiveDefPin();
				p2.setInternalName( pin.getInternalName() );
			//	p2.setExternalName( pin.getInternalName() );
				p2.setDirection( ( pin.getDirection() == PrimitiveDefPinDirection.OUTPUT ? PrimitiveDefPinDirection.INPUT : PrimitiveDefPinDirection.OUTPUT ));
				
				e.addPin(p2);
				e.setBel(false);
				e.setPin(true);
				e.setName(pin.getInternalName());
				
				this.addElement(e);
			
			}
		}
	}
	
	private void removeUnconnectedPins(){
		for (int i = pins.size()-1; i >=0; i--) 
			if (!this.belPinNames.contains(pins.get(i).getInternalName() ) ) 
				this.pins.remove(i);	
			
		
		for (int i = elements.size()-1; i >=0; i--) 
			if ( elements.get(i).isPin() && !this.belPinNames.contains( elements.get(i).getName())) 
				this.elements.remove(i);
	}
	/**
	 * If a primitive site has one bel, then this method infers the intra-site connections
	 */
	public void generateConnectionsAutomatically(){
		
		//Generate each connection
		for (Element element : this.elements) {
			//Generate Bel Connections
			if (element.isBel()){
				this.generateBelConnections(element);
			//Generate Pin connections
			}else if (element.isPin() && element.getPins().get(0).isConnected()){
				this.generateSitePinConnections(element);
			//Generate Site pip connections
			}else if (!element.getName().startsWith("_ROUTE") ){
				this.generateSitePipConnections(element);
			}
		}
	}
	/**
	 * Generates the connections for each bel in the primitive site
	 * @param element
	 */
	private void generateBelConnections(Element element){
		for (PrimitiveDefPin pin : element.getPins()) {
			Connection conn = new Connection();
			
			conn.setElement0(element.getName() );
			conn.setForwardConnection( pin.getDirection() == PrimitiveDefPinDirection.OUTPUT ? true : false);
			conn.setPin0( pin.getInternalName() );
			
			if (pipNames.contains(pin.getInternalName() + "INV") ) {
				conn.setElement1( pin.getInternalName() + "INV");
				conn.setPin1("OUT");
			}else {
				conn.setElement1( pin.getInternalName() );
				conn.setPin1( pin.getInternalName() );
			}
			element.addConnection(conn);
		}
	}
	/**
	 * Generates the connections for each site pin in the primitive site
	 * @param element
	 * @param pipNames
	 * @param belName
	 */
	private void generateSitePinConnections(Element element){
		Connection conn = new Connection();
		conn.setElement0( element.getName() );
		conn.setForwardConnection(element.getPins().get(0).getDirection() == PrimitiveDefPinDirection.OUTPUT ); 
		conn.setPin0(element.getName());
		conn.setPin1(element.getName());	
		
		if ( pipNames.contains(element.getName() + "INV") ) { 
			conn.setElement1( element.getName() + "INV" );
			element.addConnection(conn);
			
			conn = new Connection();
			conn.setElement0( element.getName() );
			conn.setForwardConnection( element.getPins().get(0).getDirection() == PrimitiveDefPinDirection.OUTPUT ); 
			conn.setPin0(element.getName());
			conn.setElement1( element.getName() + "INV" );
			conn.setPin1( element.getName() + "_B");
			element.addConnection(conn);
		}
		else if ( belPinNames.contains( element.getName()) ) { //if a site pin doesn't have a corresponding bel connection, ignore it because it is unconnected. 
			
			//if the site pin is of type inout, we have to generate a connection in both directions
			if (element.getPins().get(0).getDirection() == PrimitiveDefPinDirection.INOUT){
				Connection conn2 = new Connection();
				conn2.setElement0( element.getName() );
				conn2.setForwardConnection( !conn.isForwardConnection() );
				conn2.setPin0(element.getName());
				conn2.setPin1(element.getName());
				conn2.setElement1( bel.getName() );
				element.addConnection(conn2);
				
				//Now add this connection in the opposite direction to the bel element 
				Connection belConn = new Connection();
				belConn.setElement0( bel.getName() );
				belConn.setPin0( element.getName() );
				belConn.setForwardConnection(true);
				belConn.setElement1( element.getName() );
				belConn.setPin1( element.getName() );
				bel.addConnection(belConn);
			} 
			
			conn.setElement1( bel.getName() );
			element.addConnection(conn);
		}
	}
	/**
	 * Generates the connections for each site pip in the primitive site
	 * @param element
	 * @param belName
	 */
	private void generateSitePipConnections(Element element){
		for (PrimitiveDefPin pin : element.getPins()) {
			Connection conn = new Connection();
			conn.setElement0( element.getName() );
			conn.setPin0( pin.getInternalName() );
			
			if ( pin.getDirection() == PrimitiveDefPinDirection.OUTPUT ) {
				conn.setForwardConnection(true);
				conn.setElement1( bel.getName() );
				conn.setPin1( element.getName().replace("INV", "") );
			}else {
				conn.setForwardConnection(false);
				conn.setElement1( element.getName().replace("INV", "") );
				conn.setPin1( element.getName().replace("INV", "") );
			}
			element.addConnection(conn);
		}
	}
	
	//Don't use the default toString function when using this class, use this function!
	public String toString(boolean savePrint){
		this.calculateTotalNumElements();
		StringBuilder s = new StringBuilder();
		String nl = System.getProperty("line.separator");
		s.append("\t(primitive_def " + type +" "+ pins.size() + " " + (this.numElements - (savePrint ? this.siteCfgElements.size() : 0)) 
				+ (this.isSingleBelSite ? " #SBS" :"") + nl);
		for(PrimitiveDefPin p : pins){
			s.append("\t\t"+p.toString(savePrint)+nl);
		}
		for(Element e : elements){
			s.append("\t\t"+e.toString(savePrint)+nl);
		}
		
		if (!savePrint) {
		//Only used for the VSRTool GUI
			if(siteCfgElements.size() > 0) {
				for(int k = 0;k <  siteCfgElements.size(); k++) {
					String[] tokens = siteCfgElements.get(k).trim().split("\\s+");
					s.append("\t\t(element " + tokens[0].replace(":", "") + " 0" + nl);
					s.append("\t\t\t(cfg");
					for(int i = 1; i < tokens.length; i++)
					{	s.append(" " + tokens[i]); 	}
					s.append(")" + nl);
					s.append("\t\t)" + nl ); 
				}
			}
		}
		
		s.append("\t)");
		return s.toString();
	}
}