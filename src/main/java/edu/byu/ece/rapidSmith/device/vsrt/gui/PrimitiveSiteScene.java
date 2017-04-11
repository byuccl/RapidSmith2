package edu.byu.ece.rapidSmith.device.vsrt.gui;

import java.util.ArrayList;
import java.util.HashMap;

import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Bel;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.ElementShape;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.PinShape;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Pip;
import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.WirePart;
import edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands.AddElementCommand;
import edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands.AddWireCommand;
import edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands.MoveCommand;
import edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands.RemoveCommand;
import edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands.RotateCommand;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.PrimitiveDefPinDirection;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.QRectF;
import com.trolltech.qt.core.Qt.AspectRatioMode;
import com.trolltech.qt.core.Qt.CursorShape;
import com.trolltech.qt.core.Qt.MouseButton;
import com.trolltech.qt.gui.QBrush;
import com.trolltech.qt.gui.QGraphicsItemInterface;
import com.trolltech.qt.gui.QGraphicsLineItem;
import com.trolltech.qt.gui.QGraphicsScene;
import com.trolltech.qt.gui.QGraphicsSceneMouseEvent;
import com.trolltech.qt.gui.QGraphicsItem.GraphicsItemFlag;
import com.trolltech.qt.gui.QGraphicsView.DragMode;
import com.trolltech.qt.gui.QLineF;
import com.trolltech.qt.gui.QPainter;
import com.trolltech.qt.gui.QPen;
import com.trolltech.qt.gui.QTreeWidgetItem;
import com.trolltech.qt.gui.QUndoCommand;
import com.trolltech.qt.gui.QUndoStack;

/**
 * Custom QGraphicsScene class created to handle a variety of different needed functions including <br>
 * - Drawing a grid on the background <br>
 * - Adding wires to the scene <br>
 * - Deleting elements and wires from the scene <br>
 * - Keeping track of position to pin mappings so connections between elements can be made
 * 
 * @author Thomas Townsend
 * Created: Jun 10, 2014 4:18:15 PM
 */
public class PrimitiveSiteScene extends QGraphicsScene{
	
	/**The width and height of each square on the grid*/
	private double square_size = 30.0;
	/**When a wire is being drawn, this variable holds the starting position of the wire*/
	private QPointF start = new QPointF(0,0);
	/**Enables drawing wires on the grid*/
	private boolean should_draw_wire = false; 
	/**Enables deleting elements on the grid*/
	private boolean should_delete = false;
	/**Enables zooming to selected area*/
	private boolean zoomToView = false;
	/**True when the left button on the mouse is pressed*/
	private boolean mousePressed = false;
	/**True is items are currently being moved*/
	private boolean itemsMoving = false;
	/***/
	private boolean itemClicked = false;
	/**This is the temporary wire that will be drawn as the user drags and drops a new wire*/
	private QGraphicsLineItem tmp_wire = new QGraphicsLineItem();
	/**Data Structure used to keep track of position to pin mappings so 
	 * that connections can be easily verified, created, or deleted*/
	private HashMap<QPointF,QTreePin> pin_locations = new HashMap<QPointF,QTreePin>();
	/**Parent widget*/
	private VSRTool parent;
	/**Stack used to implement the undo/redo framework of the application*/
	private QUndoStack undoStack; 
	
	/**
	 * Constructor: 
	 * @param parent
	 * @param redoAction
	 * @param undoAction
	 */
	public PrimitiveSiteScene( VSRTool parent, QUndoStack undoStack ){
		super();
		this.parent = parent;
		this.selectionChanged.connect(this, "selectItem()");
		this.undoStack = undoStack;	
	}
	public VSRTool getParent(){
		return parent;
	}
	public boolean shouldDelete(){
		return this.should_delete;
	}
	public boolean shouldZoomToView(){
		return this.zoomToView;
	}
	public boolean shouldDrawWire(){
		return this.should_draw_wire;
	}
	public void resetScene(){
		pin_locations.clear();
		this.clear();
		this.update();
		should_draw_wire = false;
		should_delete = false; 
		this.views().get(0).setDragMode(DragMode.RubberBandDrag);
	}
	/**
	 * @return The current square size of the background grid
	 */
	public double getSquare_size() {
		return square_size;
	}

	public boolean itemsFrozen(){
		return this.should_draw_wire || this.should_delete || this.zoomToView ;
	}
	/**
	 * Sets the current square size of the background grid to the parameter passed in. 
	 * @param square_size The new grid size
	 */
	public void setSquare_size(double square_size) {
		this.square_size = square_size;
	}
	
	/**
	 * Updates the position of the QTreePin passed into the function with its new location.
	 * The old location is removed from the pin_locations data structure.  
	 * @param old_point The previous pin location
	 * @param new_point The new pin location
	 * @param pin The pin whose position has been updated. 
	 */
	public void add_pin (QPointF old_point, QPointF new_point,  QTreePin pin){
		
		if (old_point != null) { 
			if(pin_locations.get(old_point) == pin)
				pin_locations.remove(old_point);
		}
		pin_locations.put(new_point, pin);
	}
	
	/**
	 * Removes the pin at the given location from the HashMap
	 * @param pin_location The location of the pin to remove
	 */
	public void remove_pin(QPointF pin_location){
		pin_locations.remove(pin_location);
	}

	public boolean isPinAtLocation(QPointF location){

		return (this.pin_locations.get(location) == null) ? false : true ;
	}

	public QTreePin getTreePin(QPointF location){
		return this.pin_locations.get(location); 
	}
	/**
	 * @return The current zoom of the view object associated with this scene
	 */
	public double get_ViewZoom(){
		return ((PrimitiveSiteView) this.views().get(0)).getZoomLevel(); 
	}
	
	/**
	 * (1) If wire drawing functionality is enabled, then a new wire will be created only if it
	 * starts and ends on an element pin. <br> 
	 * (2) If deleting functionality is enabled, then the element at the current location will be 
	 * removed from the scene <br>
	 * (3) If both of these are disabled, mouseReleaseEvent of QGraphicsScene is called  
	 * @param event QGraphiceSceneMouseEvent containing the mouse release information 
	 */
	protected void mouseReleaseEvent (QGraphicsSceneMouseEvent event) {
		
		if (event.button() == MouseButton.LeftButton )	{
			//draw a wire
			if ( this.should_draw_wire ) {
				double offsetX, offsetY;
				
				//calculating the closest ending grid position
				double remX = event.scenePos().x() % square_size;
				double remY = event.scenePos().y() % square_size;
				offsetX = (remX < square_size/2) ? remX : -(square_size - remX) ;
				offsetY = (remY < square_size/2) ? remY : -(square_size - remY) ;
				
				QPointF end = new QPointF(event.scenePos().x()- offsetX, event.scenePos().y()- offsetY);
				
				//Searching the HashMap for the start and end pin locations
				QTreePin start_pin = this.pin_locations.get(start); 
				QTreePin end_pin = this.pin_locations.get(end);
				
				//removing the temporary wire used from tracing the wire
				this.removeItem(tmp_wire);
				
				//If both pin locations exist and the pin types are different (i.e. one is an input pin and the other is an output pin)
				//then create a new wire and connect the pins.
				if (start_pin != null && end_pin != null && start_pin.getPin().isConnected() && end_pin.getPin().isConnected()
						&& start_pin != end_pin) {
					AddWireCommand addWire = new AddWireCommand(this, start_pin, end_pin, start, end);
					this.undoStack.push(addWire);
				}
			}
			else if ( this.should_delete )
			{//Delete all objects within the draw rectangle
				
				QPointF end = new QPointF(event.scenePos().x(), event.scenePos().y());
				
				ArrayList<QGraphicsItemInterface> items = (ArrayList<QGraphicsItemInterface>) this.items(new QRectF(start, end ));
				
				if ( items.size() == 0 ) 
					items = (ArrayList<QGraphicsItemInterface>) this.items(end) ;
				
				RemoveCommand remove = new RemoveCommand(items, this);
				this.undoStack.push(remove);
			}
			else if ( this.zoomToView ) {
			//fit the selected rectangle into the view
				if (  ((PrimitiveSiteView)this.views().get(0)).shouldZoomIn()   ) {
					QPointF end = new QPointF(event.scenePos().x(), event.scenePos().y());
					this.views().get(0).fitInView(new QRectF(start, end), AspectRatioMode.KeepAspectRatio );
				}
			}
			else if ( this.itemsMoving ) { 
				MoveCommand move = new MoveCommand(this, (ArrayList<QGraphicsItemInterface>) this.selectedItems());
				if (move.shouldPushMove())
					this.undoStack.push(move);
			}
		}
		mousePressed = false;
		itemsMoving = false; 
		itemClicked = false;
		super.mouseReleaseEvent(event);
	}
	/**
	 * 
	 * @param start_pin
	 * @param end_pin
	 */
	public void updateTreeView (QTreePin start_pin, QTreePin end_pin){
		//Change the color of the pins in the element tab GREEN, signifying that it is connected to something
		start_pin.setForeground(0, new QBrush(VsrtColor.darkGreen));
		end_pin.setForeground(0, new QBrush(VsrtColor.darkGreen));
		
		//Check the parent element of each pin...if all pins are connected, then turn it green in the tree view
		this.checkConnectionsComplete( start_pin.parent() );
		this.checkConnectionsComplete( end_pin.parent() );	
	}
	/**
	 * 
	 * @param start_pin
	 * @param end_pin
	 * @return
	 */
	public String generateConnectionText(QTreePin start_pin,QTreePin end_pin){
		String text;
		if ( start_pin.getPin().getDirection() == end_pin.getPin().getDirection() ) {
			text = String.format("==> %s %s", end_pin.get_parentName(), end_pin.get_pinName());
		}
		else if ( start_pin.getPin().getDirection() == PrimitiveDefPinDirection.INOUT ){
			text = (end_pin.getPin().getDirection()  == PrimitiveDefPinDirection.INPUT ) ?
					String.format("==> %s %s", end_pin.get_parentName(), end_pin.get_pinName()) : 
					String.format("<== %s %s", end_pin.get_parentName(), end_pin.get_pinName());
			
		}else {
			text = (start_pin.getPin().getDirection() == PrimitiveDefPinDirection.OUTPUT ) ? 
					String.format("==> %s %s", end_pin.get_parentName(), end_pin.get_pinName()) : 
					String.format("<== %s %s", end_pin.get_parentName(), end_pin.get_pinName());
		}
		return text ; 
	}
	
	/**
	 * 
	 * @param parent
	 */
	private void checkConnectionsComplete(QTreeWidgetItem parent){

		if (parent != null && parent.foreground(0).color().value() != VsrtColor.darkGreen.value() ) {
			boolean changeColor = true;
			for (int i = 0; i < parent.childCount(); i++) {
				if( parent.child(i).childCount() == 0 ) {
					changeColor = false; break;
				}
			}
			if (changeColor) { parent.setForeground(0, new QBrush(VsrtColor.darkGreen) );   }
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean hasConnections(){
		for (QGraphicsItemInterface item : this.items()) {
			if (item instanceof WirePart)
				return true;
		}
		return false;
	}
	
	/**
	 * When the mouse is pressed on the grid, this method calculates the closest starting position 
	 * for a wire if drawing wires is enabled
	 * @param event QGraphiceSceneMouseEvent containing the mouse position information 
	 */
	protected void mousePressEvent (QGraphicsSceneMouseEvent event) {
		
		if ( event.button() == MouseButton.LeftButton ) {
			start = event.scenePos();
			
			if ( this.itemAt( event.scenePos() ) instanceof ElementShape || this.itemAt( event.scenePos() ) instanceof PinShape )
				this.itemClicked = true; 
			
			if (this.should_draw_wire )	{
		
				double offsetX, offsetY;
				
				offsetX = (event.scenePos().x() % square_size < square_size/2) ? event.scenePos().x() % square_size : -(square_size - event.scenePos().x() % square_size) ;
				offsetY = (event.scenePos().y() % square_size < square_size/2) ? event.scenePos().y() % square_size : -(square_size - event.scenePos().y() % square_size) ;
				
				start = new QPointF(event.scenePos().x() - offsetX, event.scenePos().y() - offsetY );
				this.tmp_wire = new QGraphicsLineItem(new QLineF(start, start));
				this.tmp_wire.setZValue(2);
				this.addItem(tmp_wire);
				}
			mousePressed = true;
		}
		super.mousePressEvent(event);
	}
	
	/**
	 * When the mouse moves and drawing wires is enabled, this method draws a 
	 * temporary line that follows the cursor
	 * @param event QGraphiceSceneMouseEvent containing the mouse position information 
	 */
	protected void mouseMoveEvent (QGraphicsSceneMouseEvent event) {
		super.mouseMoveEvent(event);
			
		if ( mousePressed ){
			if (should_draw_wire )
				this.tmp_wire.setLine(new QLineF(start, event.scenePos()));
			else if ( !should_delete && !zoomToView ) {//this.views().get(0).dragMode() == DragMode.RubberBandDrag ) {
				for (QGraphicsItemInterface item : this.selectedItems()) {
					if ( !item.isUnderMouse() && item instanceof ElementShape )
						item.mouseMoveEvent(event);
				}
			}
			
			if (this.itemClicked) 
				this.itemsMoving = true;
		
		}
	}
	
	/**
	 * Selects the currently selected graphics item in the bel/pip tree view as well 
	 */
	@SuppressWarnings("unused")
	private void selectItem(){

		try {
			QTreeWidgetItem selected = ((ElementShape)this.selectedItems().get(0)).getTreeElement();
				
			for(QTreeWidgetItem item: selected.treeWidget().selectedItems())
				item.setSelected(false);
			
			selected.setSelected(true);	
			selected.treeWidget().scrollToItem(selected);
		
		}
		catch (Exception e){
			if ( this.views().get(0).underMouse() )
				parent.getElementView().deselectAll();
		}
	}
	
	/**
	 * Draws a grid on the background of the scene in order to help the user
	 * know where to place bels and draw wire connections
	 * @param painter QPainter object that paints the background
	 * @param rect Bounding rectangle of the scene.
	 */
	protected void drawBackground(QPainter painter, QRectF rect)
	{
		QPen pen = new QPen(VsrtColor.lightGray);
		pen.setWidth(1);
		
		painter.setPen(pen);
		double startY = rect.top() - Math.IEEEremainder(rect.top(), square_size);
		
		//draw a horizontal line every square_size amount starting at the top left-hand corner of the grid
		for(; startY < rect.bottom(); startY += square_size)
			painter.drawLine(new QPointF(rect.left(), startY), new QPointF(rect.right(), startY));
		
		double startX = rect.left() - Math.IEEEremainder(rect.left(), square_size);
		//draw a vertical line every square size amount starting at the top left-hand corner of the grid 
		for (; startX < rect.right(); startX += square_size)
			painter.drawLine(new QPointF(startX, rect.top()), new QPointF(startX, rect.bottom()));	
		
	}
	public void pushCommand(QUndoCommand cmd){
		this.undoStack.push(cmd);
	}
	/**
	 * 
	 * @param element
	 */
	public void selectItemInScene(QTreeWidgetItem element){
		try {
			//get the pip to highlight
			QTreeElement selected = (QTreeElement) element;
			
			//find the corresponding graphics item select it
			for (QGraphicsItemInterface item : this.items()) {
				if ( item instanceof ElementShape){
					if( ((ElementShape)item).getTreeElement() == selected ) {
						item.setSelected(true);
						//this.views().get(0).ensureVisible(item);
					}
					else {item.setSelected(false); }
				}
				else { //deselect all other items
					item.setSelected(false);
				}
			}
		} catch (Exception e){}
	}
	
	/**
	 * 
	 * @param element
	 * @param location
	 */
	public ElementShape addSavedElementToScene(QTreeWidgetItem element, QPointF location, double rotation){
		try {
			QTreeElement tmp = (QTreeElement) element;
			ElementShape item = null;
			
			if (!tmp.isPlaced()) {
				if (tmp.getElement().isBel() )
					item = new Bel(tmp, this.square_size, location) ;
				else{ 
					item = new Pip(tmp, this.square_size, location ) ;
				}
	
				this.addItem(item);
				item.setPos(location);
				this.update();
				((ElementShape)item).getTreeElement().setIsPlaced(true);
				item.rotateTo(rotation);
				//((ElementShape)item).updatePinPosition();
			}
			return item;
		}
		catch(ClassCastException e){
			return null;
		}
	}
	/**
	 * 
	 * @param element
	 */
	public void addElementToScene(QTreeWidgetItem element){
		try {
			QTreeElement tmp = (QTreeElement) element;
			ElementShape item;
			
			if (!tmp.isPlaced()) {
				if (tmp.getElement().isBel() )
					item = new Bel(tmp, this.square_size, parent.getPlacementPosition()) ;
				else{ 
					item = new Pip(tmp, this.square_size, parent.getPlacementPosition() ) ;
				}
				AddElementCommand add = new AddElementCommand(this, item, item.pos());
				this.undoStack.push(add);
				this.parent.getToolBar().untoggleAll();
			}
		}
		catch(ClassCastException e){}
	}
	/**
	 * Enable drawing wires on the device view  
	 * @param enable  
	 */
	public void enable_draw_wire(boolean enable) {
		this.should_draw_wire = enable;
		this.views().get(0).setDragMode((should_draw_wire) ? DragMode.NoDrag : DragMode.RubberBandDrag);
		//if drawing wires is enabled, disable the ability to move and select object 
		freeze_items(!enable);

		//Change the cursor image to a cross when drawing wires 
		((PrimitiveSiteView)this.views().get(0)).setCursorShape((enable)? CursorShape.CrossCursor: CursorShape.ArrowCursor, false);
	}
	/**
	 * Enables deleting items on the device view  
	 * @param enable  
	 */
	public void enable_delete(boolean enable) {
		this.should_delete = enable;
		((PrimitiveSiteView)this.views().get(0)).setCursorShape((enable)? CursorShape.CustomCursor: CursorShape.ArrowCursor, true);
		freeze_items(!enable);
	}
	/**
	 * Enable drag mode on the device view 
	 * @param enable  
	 */
	public void set_view_draggable(boolean drag) {
		((PrimitiveSiteView)this.views().get(0)).setDragMode(drag ? DragMode.ScrollHandDrag : DragMode.RubberBandDrag );
		((PrimitiveSiteView)this.views().get(0)).setInteractive(!drag); 
	}
	
	/**
	 * Enables/Disables the zoom selection functionality on the device view 
	 * @param enable
	 */
	public void setZoomToView(boolean enable){
		this.zoomToView = enable; 
		((PrimitiveSiteView)this.views().get(0)).setCursorShape((enable ? CursorShape.CustomCursor : CursorShape.ArrowCursor), false);
		freeze_items(!enable);
	}
	/**
	 *  Freeze all items on the scene so that they are not movable or selectable.
	 *  This allows for items to be deleted, wires to be drawn, and the scene to be dragged,
	 *  without moving the bels or pips on the scene. 
	 * @param freeze  
	 */
	private void  freeze_items (boolean freeze){
		for (QGraphicsItemInterface item : this.items()) {
			if( (item instanceof ElementShape) || (item instanceof PinShape)   ){ 
				item.setFlag(GraphicsItemFlag.ItemIsMovable, freeze);
				item.setFlag(GraphicsItemFlag.ItemIsSelectable, freeze);
			} 
		}	
	}
	/**
	 *	Creates a new clockwise rotate command and pushes it onto the undoStack
	 */
	public void rotateItemsClockwise(){
		if (this.selectedItems().size() > 0 ) { 
			RotateCommand rotate = new RotateCommand((ArrayList<QGraphicsItemInterface>) this.selectedItems(), true);
			this.undoStack.push(rotate);
		}
	}
	/**
	 *	Creates a new counterclockwise rotate command and pushes it onto the undoStack
	 */
	public void rotateItemsCounterclockwise(){
		if (this.selectedItems().size() > 0 ) { 
			RotateCommand rotate = new RotateCommand((ArrayList<QGraphicsItemInterface>) this.selectedItems(), false);
			this.undoStack.push(rotate);
		}
	}
	
	public void hideWires() {	
		this.items().stream()
			.filter(item -> (item instanceof ElementShape))
			.map(item -> (ElementShape) item)
			.forEach(elShape -> elShape.hideWires());
	}
	
	public void showWires() {
		this.items().stream()
			.filter(item -> (item instanceof ElementShape))
			.map(item -> (ElementShape) item)
			.forEach(elShape -> elShape.showWires());
	}
	
}//end class