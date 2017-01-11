package edu.byu.ece.rapidSmith.device.vsrt.gui;

import java.util.ArrayList;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.gui.QTreeWidget;
import com.trolltech.qt.gui.QTreeWidgetItem;

import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.PrimitiveDefPin;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Wire;

/**
 * This class is used to display pins and their connections within the tree view of the GUI  
 * @author Thomas Townsend
 * Created on: Jul 1, 2014
 */
public class QTreePin extends QTreeWidgetItem implements Comparable<QTreePin>{
	/**Primitive Def Pin that this QTreePin represents*/
	private PrimitiveDefPin pin;
	/**Parent element who this pin is apart of*/
	private QTreeWidgetItem parent;
	/**Last location on the grid where this pin was located*/
	private QPointF last_set_location = null;
	/**List of wires that are attached to this pin*/
	private ArrayList<Wire> wires = new ArrayList<Wire>();
	/**Used to keep wires attached to pins when elements are being moved on the graphics scene*/
	private QPointF last_move_location = new QPointF(0,0);
	/**Set to true if the QTreePin is a site pin, not a bel pin*/
	private boolean isSitePin;
	
	/**
	 * Constructor 1:
	 * @param parent
	 * @param pin
	 * @param isSitePin
	 */
	public QTreePin(QTreeWidgetItem parent, PrimitiveDefPin pin, boolean isSitePin) {
		super(parent); 
		this.parent = parent;
		this.pin = pin;
		this.isSitePin = isSitePin;
	}
	/**
	 * Constructor 2:
	 * @param parent
	 * @param pin
	 * @param isSitePin
	 */
	public QTreePin(QTreeWidget parent, PrimitiveDefPin pin, boolean isSitePin) {
		super(parent);

		this.pin = pin;
		this.isSitePin = isSitePin;
	}
	
	//getters and setters
	public PrimitiveDefPin getPin() {
		return pin;
	} 
	public QPointF getLastLocation() {
		return this.last_set_location;
	}
	public void setLastLocation(QPointF location) {
		this.last_set_location = location;	
	}
	public boolean isSitePin(){
		return this.isSitePin;
	}
	public QPointF getLast_move_location() {
		return last_move_location;
	}
	public void setLast_move_location(QPointF last_moved_location) {
		this.last_move_location = last_moved_location;
	}
	/**
	 * Returns the name of the parent element that this pin is apart of.  If the pin is a site pin
	 * then the name of the pin is returned instead
	 * @return String 
	 */
	public String get_parentName(){
		
		if(this.isSitePin) {
			return this.getPin().getInternalName();
		}
		
		QTreeElement tmp = (QTreeElement) parent;
		return tmp.text(0);
	}
	/**
	 * Returns the PrimitiveDefPin of this QTreePin
	 * @return
	 */
	public String get_pinName(){
		return pin.getInternalName();
	}
	
	/**
	 * When an element is removed from the graphics scene, 
	 * this method removes each wire that was connected to the pin.   
	 */
	public void remove_allWires(){
		for (int i = wires.size() - 1 ; i >=0 ; i--)  {
			wires.get(i).removeWire();
		}
	}
	
	/**
	 * Adds a connecting wire to this pin
	 * @param wire
	 */
	public void add_wire(Wire wire){
		wires.add(wire);
	}
	/**
	 * Removes a previously connected wire from the pin
	 * @param wire
	 */
	public void remove_wire(Wire wire){
		wires.remove(wire);
	}
	/**
	 * Returns the currently connected wires to this pin
	 * @return
	 */
	public ArrayList<Wire> get_wires(){
		return wires;
	}
	
	@Override
	public int hashCode() {
		return pin.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		QTreePin other = (QTreePin) obj;
		if (last_move_location == null) {
			if (other.last_move_location != null)
				return false;
		} else if (!last_move_location.equals(other.last_move_location))
			return false;
		if (last_set_location == null) {
			if (other.last_set_location != null)
				return false;
		} else if (!last_set_location.equals(other.last_set_location))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (pin == null) {
			if (other.pin != null)
				return false;
		} else if (!pin.equals(other.pin))
			return false;
		if (wires == null) {
			if (other.wires != null)
				return false;
		} else if (!wires.equals(other.wires))
			return false;
		return true;
	}
	
	/** 
	 * Custom compareTo function used to sort sitePin names properly on the GUI <br>
	 * When the pin names match from the site to the bel, it makes it easier to generate<br>
	 * those connections.
	 * @return 0 if the two objects are equal, -1 if this pin should be placed <br> 
	 *  	   before the argument, and 1 if this pin should be placed after the argument <br> 
	 */
	@Override
	public int compareTo(QTreePin o) {
		// TODO Auto-generated method stub
		
		String pin1 = o.get_pinName();
		String pin2 = this.get_pinName();
	
		//Comparing the names from the leftmost digit to the rightmost digit
		int i = 0, j = 0;
		for(i = 0 , j = 0; (i < pin1.length()) && (j < pin2.length()) ; j++,i++ ){
			
			//If we reach a digit within the pin names, this means that pins are of the form
			//"pinname5" and "pinname10" ... i.e. the have the same name with a number at the end
			//this needs special handling
			if(Character.isDigit(pin1.charAt(i)) && Character.isDigit(pin2.charAt(j)) )
				break;
			else if( pin1.charAt(i) > pin2.charAt(j) ) {
				return -1;
			}
			else if ( pin1.charAt(i) < pin2.charAt(j) ) {
				return 1;
			}
		}
		
		if( pin1.length() == pin2.length() ){
			for(; (i < pin1.length()) && (j < pin2.length()) ; j++,i++ ){
				if( pin1.charAt(i) > pin2.charAt(j) ) {
					return -1;
				}
				else if ( pin1.charAt(i) < pin2.charAt(j) ) {
					return 1;
				}
			}
			return 0;
		}
		//whichever pin name is shorter should be placed before
		//For example "A10" should be placed after "A3" 
		else if(pin1.length() > pin2.length() ){
			return -1;
		}
		else {
			return 1;
		}
	}	
}
