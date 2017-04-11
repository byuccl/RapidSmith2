package edu.byu.ece.rapidSmith.device.vsrt.gui;

import java.util.ArrayList;
import java.util.HashSet;

import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QTreeWidget;
import com.trolltech.qt.gui.QTreeWidgetItem;

import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.*;

/**
 * This class is used to display elements (bels and pips), their pins, 
 *  and their connections within the tree view of the GUI  
 * @author Thomas Townsend
 * Created on: Jul 1, 2014
 */
public class QTreeElement extends QTreeWidgetItem{

	/**Element that this QTreeElement represents*/
	private Element element;
	/**Input pins to the element*/
	private ArrayList<QTreePin> in_pins = new ArrayList<QTreePin>();
	/**Output pins from the element*/
	private ArrayList<QTreePin> out_pins = new ArrayList<QTreePin>();
	/**Keeps track of whether this element has been added to the scene already*/
	private boolean isPlaced = false;
	
	private HashSet<QTreePin> connectedPins = new HashSet<QTreePin>();

	
	/**
	 * Constructor
	 * @param view
	 * @param element
	 */
	public QTreeElement (QTreeWidget view, Element element) {
		super(view);
		
		this.element = element;

		//Adding QTreePins to this element
		for (PrimitiveDefPin bel_pin : element.getPins()) {
			
			QTreePin pin_tmp = new QTreePin(this, bel_pin, false);
			pin_tmp.setText(0, bel_pin.getInternalName());
			//pins are initially unconnected, and so their color is red to represent this
			pin_tmp.setForeground(0, new QBrush(VsrtColor.red));
						
			if ( bel_pin.getDirection() == PrimitiveDefPinDirection.OUTPUT ){
				this.out_pins.add(pin_tmp);
			}
			else
			{	this.in_pins.add(pin_tmp);	}
		}
	}

	//getters and setters 
	public void setIsPlaced(boolean isPlaced){
		this.isPlaced = isPlaced;
	}
	
	public boolean isPlaced(){
		return this.isPlaced;
	}

	public Element getElement() {
		return element;
	}

	public void setElement(Element element) {
		this.element = element;
	}

	public ArrayList<QTreePin> getIn_pins() {
		return in_pins;
	}

	public void setIn_pins(ArrayList<QTreePin> in_pins) {
		this.in_pins = in_pins;
	}

	public ArrayList<QTreePin> getOut_pins() {
		return out_pins;
	}

	public void setOut_pins(ArrayList<QTreePin> out_pins) {
		this.out_pins = out_pins;
	}
	
	/**
	 * Gets all pins of this element regardless of direction
	 * @return ArrayList<QTreePin>
	 */
	public ArrayList<QTreePin> get_allPins() {
		ArrayList<QTreePin> tmp = new ArrayList<QTreePin>();
		tmp.addAll(this.in_pins);
		tmp.addAll(this.out_pins);
		return tmp;
	}	
	
	/**
	 * Removes the specifies QTreePin from this element
	 * @param pin
	 */
	public void removePin(QTreePin pin){
		if ( pin.getPin().getDirection() == PrimitiveDefPinDirection.OUTPUT ){
			this.out_pins.remove(pin);
		}
		else 
		{	this.in_pins.remove(pin);	}
		
		//also remove it from the PrimitiveDef data structure
		this.element.removePin(pin.getPin());
	}
	/**
	 * Add a QTreePin to this element
	 * @param index
	 * @param pin
	 */
	public void addPin(int index, QTreePin pin){
		if ( pin.getPin().getDirection() == PrimitiveDefPinDirection.OUTPUT ){
			this.out_pins.add(index, pin);
		}
		else 
		{	this.in_pins.add(index, pin);	}
		
		//add the pin to the PrimitiveDef data structure as well
		this.element.addPin(pin.getPin());
	}
	/**
	 * Returns the index of the pin within the out_pins QTreePin array <br>
	 * if the pin's direction is output, otherwise it returns the index of the <br>
	 * pin in the in_pin array.
	 * @param pin
	 * @return
	 */
	public int getPinIndex(QTreePin pin){
		if ( pin.getPin().getDirection() == PrimitiveDefPinDirection.OUTPUT ){
			return this.out_pins.indexOf(pin);
		}
		else 
		{	return this.in_pins.indexOf(pin);	}
	}
	
	public void markPinAsConnected(QTreePin pin) {
		this.connectedPins.add(pin);
	}
	
	public void markPinAsUnconnected(QTreePin pin) {
		this.connectedPins.remove(pin);
	}
	
	public QColor getBorderColor() {
		
		QColor borderColor = VsrtColor.red;
		int connectedCount = this.connectedPins.size();
		int pinCount = in_pins.size() + out_pins.size();
		
		if (connectedCount > 0) {
			borderColor = (connectedCount == pinCount) ? VsrtColor.darkGreen : VsrtColor.darkOrange; 
		}
		
		return borderColor; 
	}
}
