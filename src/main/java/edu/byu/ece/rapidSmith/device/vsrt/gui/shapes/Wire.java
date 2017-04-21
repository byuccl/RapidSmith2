package edu.byu.ece.rapidSmith.device.vsrt.gui.shapes;

import edu.byu.ece.rapidSmith.device.vsrt.gui.PrimitiveSiteScene;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreeElement;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreePin;
import edu.byu.ece.rapidSmith.device.vsrt.gui.VSRTool;
import edu.byu.ece.rapidSmith.device.vsrt.gui.VsrtColor;

import java.util.ArrayList;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QGraphicsItemInterface;
import com.trolltech.qt.gui.QLineF;
import com.trolltech.qt.gui.QTreeWidgetItem;

/**
 * This class represents a wire connecting two pins on the device view graphics scene.
 * Currently, it draws a straight line between pins, but I hope to add functionality where 
 * the wire runs along the grid sometime in the near future. 
 * @author Thomas Townsend
 * Created on: Jul 2, 2014
 */
public class Wire {
	
	/**Start Pin connection item*/
	QTreeWidgetItem start;
	/**End Pin connection item*/
	QTreeWidgetItem end;
	/**Set of wire part QGraphicsItems that represent this wire on the graphics scene*/
	ArrayList<WirePart> lines = new ArrayList<WirePart>();
	/**Actual start pin*/
	QTreePin startParent;
	/**Actual end pin*/
	QTreePin endParent;
	
	ElementShape startShape;
	ElementShape endShape;
	

	/**
	 * Constructor:
	 * @param start
	 * @param end
	 */
	public Wire (QPointF start, QPointF end) {
		WirePart tmp = new WirePart(start,end, this ); 
		lines.add(tmp); 
	}
	
	//getters and setters
	public QTreeWidgetItem getTree_start() {
		return start;
	}
	public void setTree_start(QTreeWidgetItem start) {
		this.start = start;
		this.startParent = (QTreePin) start.parent();
	}
	public QTreePin getTreePinStart() {
		return startParent;
	}
	public QTreePin getTreePinEnd() {
		return endParent;
	}
	public QTreeWidgetItem getTree_end() {
		return end;
	}
	public void setTree_end(QTreeWidgetItem end) {
		this.end = end;
		this.endParent = (QTreePin) end.parent();		
	}
	public WirePart getFirstWirePart(){
		return this.lines.get(0);
	}

	/**
	 * Adds each wire-part of this wire to the scene
	 * @param scene
	 */
	public void addWireToScene(PrimitiveSiteScene scene){
		for (WirePart item : lines) {
			scene.addItem(item);
		}
	}
	
	/**
	 * Removes the wire from the QGraphicsScene, along with any underlying connections to 
	 * the pins it was connecting
	 */
	public void removeWire() {

		//remove the start of the wire
		if (startParent.childCount() == 1) { //if this was the only connection of the pin, change the pins color back to red
			startParent.setForeground(0, new QBrush(VsrtColor.red));
			this.updateElementColor(startParent.parent());
			
			if (startShape!=null) {
				((QTreeElement)startParent.parent()).markPinAsUnconnected(startParent);
			}
		}
	
		startParent.remove_wire(this);
		startParent.removeChild(start);

		//now remove the other end of the wire
		if (endParent.childCount() == 1) {
			endParent.setForeground(0, new QBrush(VsrtColor.red));
			this.updateElementColor(endParent.parent());
			if (endShape!=null) {
				((QTreeElement)endParent.parent()).markPinAsUnconnected(endParent);
			}
		}
		
		endParent.remove_wire(this);
		endParent.removeChild(end);

		//remove all wire parts from the graphics scene
		for (WirePart item : lines) {
			item.scene().removeItem(item);
		}
		this.disconnect();
	}
	
	/**
	 * Updates the positions of the pins that this wire is connecting. <br>
	 * Used when the pin is moved on the graphics scene, so that the wire will <br>
	 * move with the pin. 
	 * @param old_point Old position of the pin
	 * @param new_point New position of the pin
	 */
	public void update_pin_position(QPointF old_point, QPointF new_point){
		
		for (WirePart item : lines) {
			//Only update the part of the wire that was moved
			if(item.line().p1().equals(old_point))
			{	item.setLine(new QLineF(new_point, item.line().p2()));	}
			else
			{	item.setLine(new QLineF(item.line().p1(), new_point));	}
		
		}
		lines.get(0).scene().update();			
	}

	/**
	 * Changes the color of the parent pin if this wire was its only connection
	 * @param parent
	 */
	private void updateElementColor(QTreeWidgetItem parent){
		if (parent != null)
			parent.setForeground(0, new QBrush(VsrtColor.red) );
	}
	
	/**
	 * Adds the wire, who was previously deleted, back into the GUI, and <br>
	 * generates all of the necessary connections. 
	 */
	public void undoRemoveWire(){
		//need to un-comment this is I ever get to implementing wires drawing on the grid.
		//if (startParent.indexOfChild(start) == -1) {
			this.startParent.setForeground(0, new QBrush(VsrtColor.darkGreen));
			this.startParent.addChild(start);
			this.startParent.add_wire(this);
			this.startParent.updateParentColor();
			
			this.endParent.addChild(end);
			this.endParent.add_wire(this);
			this.endParent.setForeground(0, new QBrush(VsrtColor.darkGreen));
			this.endParent.updateParentColor();
			
			this.connect();
		//}
	}
	
	public void setShapeConnections(QGraphicsItemInterface startItem, QGraphicsItemInterface endItem ) {
		
		boolean startIsElement = false, endIsElement=false;
		
		if (startItem instanceof ElementShape) {
			this.startShape = (ElementShape) startItem;
			startIsElement = true;
		} else if(!(startItem instanceof PinShape) ){
			throw new AssertionError("Parameter startItem should be ElementShape of PinShape");
		}
		
		if (endItem instanceof ElementShape) {
			this.endShape = (ElementShape) endItem;
			endIsElement = true;
		} else if(!(endItem instanceof PinShape)) {
			throw new AssertionError("Parameter endItem should be ElementShape of PinShape: " + endItem.getClass());
		}
		
		if (!VSRTool.singleBelMode) {
			if (!startIsElement && !endIsElement) {
				throw new AssertionError("Either the startItem or the endItem needs to be of type ElementShape");
			}
		} 
		/*
		else {
			if (((PinShape)startItem).isSitePin() == ((PinShape)endItem).isSitePin()) {
				throw new AssertionError("Cannot connect two site pins or bel pins together");
			}
		}
		*/
	}
		
	public void connect() {
		if (startShape!=null) {
			this.startShape.connectToWire(this);
			((QTreeElement)this.startParent.parent()).markPinAsConnected(startParent);
			startShape.update();
		}
		if (endShape!=null) {
			this.endShape.connectToWire(this);
			((QTreeElement)this.endParent.parent()).markPinAsConnected(endParent);
			endShape.update();
		}	
	}
	
	public void disconnect() {
		if (startShape!=null) {
			this.startShape.disconnectWire(this);
			startShape.update();
		}
		if (endShape!=null) {
			this.endShape.disconnectWire(this);
			endShape.update();
		}
	}
	
	public void hideWire() {
		for(WirePart wire: lines) {
			wire.hide(); 
		}
	}
	
	public void showWire() {
		for (WirePart wire : lines) {
			wire.show();
		}
	}
	
	public boolean isVisible() {
		return this.getFirstWirePart().isVisible();
	}
	
	public ElementShape getStartShape() {
		return this.startShape;
	}
	
	public ElementShape getEndShape() {
		return this.endShape;
	}

}//end class
