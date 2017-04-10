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
package edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands;

import java.util.ArrayList;
import java.util.List;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.gui.QGraphicsItem;
import com.trolltech.qt.gui.QGraphicsItemInterface;
import com.trolltech.qt.gui.QTreeWidgetItem;
import com.trolltech.qt.gui.QUndoCommand;

import edu.byu.ece.rapidSmith.device.vsrt.gui.PrimitiveSiteScene;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreePin;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Bel;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.ElementShape;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Pip;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Wire;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.PrimitiveDefPinDirection;

/**
 * This command allows the user to undo/redo an added wire action. 
 * @author Thomas Townsend
 * Created on: Jul 29, 2014
 */
public class AddWireCommand extends QUndoCommand {
	/**The default added wire*/
	private Wire wire1 = null;
	/**The second wire added (just in case it is an inout-inout pin connection)*/
	private Wire wire2 = null;
	/**Graphics scene where the wires are added*/
	private PrimitiveSiteScene scene;
	/**List of Elements wire1 is connected to*/
	private List<ElementShape> wire1ElementList = new ArrayList<ElementShape>();
	/**List of Elements wire2 is connected to*/
	private List<ElementShape> wire2ElementList = new ArrayList<ElementShape>();
	
	/**
	 * Constructor: Creates the wires(s), and generates the connections for each wire 
	 * @param scene Graphics scene to which the wires are added 
	 * @param start_pin Start pin of the wire
	 * @param end_pin End pin of the wire
	 * @param start Start point of the graphics item on the graphics scene
	 * @param end End point of the graphics item on the graphics scene
	 */
	public AddWireCommand (PrimitiveSiteScene scene, QTreePin start_pin, QTreePin end_pin, QPointF start, QPointF end){
		this.scene = scene; 
		
		QGraphicsItemInterface startItem = scene.itemAt(start.x()-1, start.y());
		if (!(startItem instanceof ElementShape)) {
			startItem = scene.itemAt(start.x()+1, start.y());
			assert (startItem instanceof ElementShape);
		}
		
		QGraphicsItemInterface endItem = scene.itemAt(end.x()-1, end.y());
		if (!(endItem instanceof ElementShape)) {
			endItem = scene.itemAt(end.x()+1, end.y());
			assert (startItem instanceof ElementShape);
		}
				
		//two connections need to be generated for pins that are both of type inout
		if (start_pin.getPin().getDirection() == PrimitiveDefPinDirection.INOUT 
				&& end_pin.getPin().getDirection()==PrimitiveDefPinDirection.INOUT) {
			wire1 = new Wire(start, end);
			wire2 = new Wire(start, end);
		
			/*
			((ElementShape)startItem).connectToWire(wire1);
			((ElementShape)startItem).connectToWire(wire2);
			((ElementShape)endItem).connectToWire(wire1);
			((ElementShape)endItem).connectToWire(wire2);
			*/
			
			wire1.setShapeConnections((ElementShape)startItem, (ElementShape)endItem);
			wire2.setShapeConnections((ElementShape)startItem, (ElementShape)endItem);
			
			//wire1ElementList.add((ElementShape)startItem);
			//wire1ElementList.add((ElementShape)endItem);
			//wire2ElementList.add((ElementShape)endItem);
			//wire2ElementList.add((ElementShape)startItem);
			
			QTreeWidgetItem conn1 = new QTreeWidgetItem(start_pin);
			QTreeWidgetItem conn2 = new QTreeWidgetItem(start_pin);
							
			conn1.setText(0 , String.format("==> %s %s", end_pin.get_parentName(), end_pin.get_pinName()) );
			conn2.setText(0 , String.format("<== %s %s", end_pin.get_parentName(), end_pin.get_pinName()) );
			wire1.setTree_start(conn1);
			wire2.setTree_start(conn2);

			//generate connections for other site of the wire
			conn1 = new QTreeWidgetItem(end_pin);
			conn2 = new QTreeWidgetItem(end_pin);
							
			conn1.setText(0 , String.format("==> %s %s", start_pin.get_parentName(), start_pin.get_pinName()) );
			conn2.setText(0 , String.format("<== %s %s", start_pin.get_parentName(), start_pin.get_pinName()) );
			wire1.setTree_end(conn1);
			wire2.setTree_end(conn2);
									
			this.scene.updateTreeView(start_pin, end_pin);
		}
		else {//if ((start_pin.getPin().getDirection() != end_pin.getPin().getDirection())) {	
			wire1 = new Wire(start, end);
			
			/*
			((ElementShape)startItem).connectToWire(wire1);
			((ElementShape)endItem).connectToWire(wire1);
			*/
			
			wire1.setShapeConnections((ElementShape)startItem, (ElementShape)endItem);
			
			//wire1ElementList.add((ElementShape)startItem);
			//wire1ElementList.add((ElementShape)endItem);
			
			//Create the connection on the start pin
			QTreeWidgetItem tmp = new QTreeWidgetItem(start_pin); 
			tmp.setText(0, this.scene.generateConnectionText(start_pin, end_pin));
			wire1.setTree_start(tmp);
			
			//Create the connection on the ending pin
			tmp = new QTreeWidgetItem(end_pin);
			tmp.setText(0, this.scene.generateConnectionText(end_pin, start_pin) );
			wire1.setTree_end(tmp);
		
			this.scene.updateTreeView(start_pin, end_pin);		
		}	
		this.setText("adding wire connecting " + start_pin.get_pinName() + " to " + end_pin.get_pinName());
	}
	
	/**
	 * Adds the wire(s) to the scene.
	 */
	@Override 
	public void redo(){
		wire1.addWireToScene(scene);
		wire1.undoRemoveWire();
		wire1.connect();
		// wire1ElementList.forEach(e -> e.connectToWire(wire1));
		if (wire2 != null){
			wire2.addWireToScene(scene);
			wire2.undoRemoveWire();
			// wire2ElementList.forEach(e -> e.connectToWire(wire2));
			wire2.disconnect();
		}
	}
	
	/**
	 * Removes the wire(s) from the scene
	 */
	@Override 
	public void undo(){
		wire1.removeWire();
		wire1.disconnect();
		//wire1ElementList.forEach(e -> e.disconnectWire(wire1));
		
		if(wire2 != null) {
			wire2.removeWire();
			wire2.disconnect();
			//wire2ElementList.forEach(e -> e.disconnectWire(wire2));
		}
	}
}
