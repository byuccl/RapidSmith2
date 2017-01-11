package edu.byu.ece.rapidSmith.device.vsrt.gui.shapes;

import java.util.ArrayList;

import edu.byu.ece.rapidSmith.device.vsrt.gui.PrimitiveSiteScene;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreeElement;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreePin;
import edu.byu.ece.rapidSmith.device.vsrt.gui.VSRTool;
import edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands.RemoveCommand;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.QRectF;
import com.trolltech.qt.core.Qt.MouseButton;
import com.trolltech.qt.gui.QFont;
import com.trolltech.qt.gui.QFontMetrics;
import com.trolltech.qt.gui.QGraphicsItem;
import com.trolltech.qt.gui.QGraphicsItemInterface;
import com.trolltech.qt.gui.QGraphicsSceneMouseEvent;
import com.trolltech.qt.gui.QIcon;
import com.trolltech.qt.gui.QMenu;
import com.trolltech.qt.gui.QPainterPath;

/**
 * This class is the basic element drawing class which implements all of the 
 * shared functionality between the Bel and PIP subclasses. 
 * @author Thomas Townsend
 * Created: Jun 10, 2014 3:53:46 PM
 */
public abstract class ElementShape extends QGraphicsItem{

	/**Rectangle that the view uses to determine if the item was selected,hovered over, etc... */
	protected QRectF bounding;
	/**The QTreeElement object that this shape represents */
	protected QTreeElement element;
	/**The last know position of the element*/
	protected QPointF last_pos;
	/**Spacing between pins of the element*/
	protected double pin_width;
	/**The height in pixels of the element*/
	protected double height;
	/**The width in pixels of the element*/
	protected double width;
	/**Font object used for resizing the size of the pin names accordingly*/
	protected QFont font = new QFont();
	/**The position of the first input pin*/
	protected int firstIn = 1; 
	/**The position of the first output pin*/
	protected int firstOut = 1;
	/**Shape of the element*/
	protected QPainterPath path = new QPainterPath();
	/**Rotation of the object (valid values are 0, 90, 180, and 270)*/
	protected double rotationAngle =  0;
	
	public QTreeElement getTreeElement(){
		return this.element;  
	}
	/**
	 * Initializes the element object
	 * @param element QTreeElement that this drawn pip represents
	 * @param pin_width The size of the grid on the device view which also represents
	 * 					the distance between adjacent pip pins
	 * @param start The starting location of the PIP 
	 */
	public ElementShape(QTreeElement element, double pin_width, QPointF startingPos, boolean shouldAddConfig) {
		this.element = element;
		this.pin_width = pin_width;
		this.last_pos = startingPos;
	
		//standard flags that needs to be set in order for the elements to behave as you want 
		this.setFlag(GraphicsItemFlag.ItemIsMovable, true);
		this.setFlag(GraphicsItemFlag.ItemIsSelectable, true);
		this.setAcceptsHoverEvents(true);
		this.setToolTip(element.getElement().getName());
		this.setZValue(1);
	}
	 
	/**
	 * Scans through the pin names of the given element, and determines the largest <br>
	 * font that will show all of the pin names on the graphics object.  This method <br>
	 * should only be called after the protected variable "width" has been set
	 */
	public void calculateFontSize(){
		//
		double fontSize = 20;
		font.setPointSizeF(fontSize);		
		QFontMetrics fm = new QFontMetrics(font);
		
		//finding the longest pin name
		String longestName = "";
		for (QTreePin pin : this.element.get_allPins()) {
			if (pin.get_pinName().length() > longestName.length() ){
				longestName = pin.get_pinName();
			}
			//If the names are the same length, then use QFontMetrics to see which would be longer 
			//one they are actually drawn on the graphics scene
			else if (pin.get_pinName().length() == longestName.length()){
				if (fm.width(pin.get_pinName()) > fm.width(longestName) ) {
					longestName = pin.get_pinName(); 
				}
			}
		}
		//Calculating the smallest font that will fit the largest pin name 
		//into half of the element width. 
		while (fm.width(longestName) >= width/2 || fm.height() > pin_width){
			fontSize -= .50;
			if (fontSize <= 0 )
				break;
			font.setPointSizeF(fontSize);
			fm = new QFontMetrics(font);
		}
	}
	
	/**
	 * Creates a new remove command, and pushes that command onto the undoStack.
	 */
	@SuppressWarnings("unused")
	private void sendRemoveElementCommand(){
		ArrayList<QGraphicsItemInterface> items = new ArrayList<QGraphicsItemInterface> ();
		items.add(this);
		RemoveCommand remove = new RemoveCommand(items, (PrimitiveSiteScene)this.scene());
		((PrimitiveSiteScene)this.scene()).pushCommand(remove);
	}
	
	/**
	 * Removes the element from the scene that is attached too
	 * along with any existing wires that are attached to it. 
	 */
	public void deleteElement() {
		
		PrimitiveSiteScene tmp = (PrimitiveSiteScene)this.scene();

		for (QTreePin pin : this.element.get_allPins()) {
			//removing pin from the data structure mapping locations to pins
			tmp.remove_pin(pin.getLastLocation());
			//removing all pin wires
			pin.remove_allWires();
		}

		this.element.setIsPlaced(false);
		tmp.removeItem(this);	
	}
		
	
	@SuppressWarnings("unused")
	private void showConfig(){
		((PrimitiveSiteScene)this.scene()).getParent().showBelConfigOptions(element.getElement());
	}
	
	/**
	 * Returns the rectangle that bounds the element object in the scene
	 * @return QRectF Rectangle describing where the object is. 
	 */
	@Override
	public QRectF boundingRect() {
		// TODO Auto-generated method stub
		return bounding;
	}

	/**
	 * Checks the graphics scene and returns true if there are any colliding items
	 * @return
	 */
	private boolean checkCollisions(){
		for (QGraphicsItemInterface item : this.scene().collidingItems(this)) {
			if (item instanceof ElementShape || item instanceof PinShape){
				return true;
			}
		}
		return false;
	}
	/**
	 * 
	 * @return
	 */
	public QPointF getLastPos(){
		return this.last_pos;
	}
	/**
	 * Updates the input and output pin positions when the element is moved,
	 * as well as displays the pop-up menu when the element is right clicked. 
	 * @param event QGraphiceSceneMouseEvent containing the mouse release information 
	 */
	public void mouseReleaseEvent (QGraphicsSceneMouseEvent event) {
		super.mouseReleaseEvent(event);
		//if the button released was the left button, this means the object was moved
		//and the pin positions need to be updated. 
		if (event.button() == MouseButton.LeftButton)
		{
			//checking to see if this element is colliding with any other element
			boolean colliding;
			colliding = this.checkCollisions();
		
			//Snapping the elements to the nearest valid grid position
			if ( colliding ) {
				this.setPos(last_pos);
			}else {
				//snapping element to grid
				double remX = this.pos().x() % pin_width;
				double remY = this.pos().y() % pin_width;	
				double offsetX = (remX < pin_width/2) ? remX : -(pin_width - remX) ;
				double offsetY = (remY < pin_width/2) ? remY : -(pin_width - remY) ;
				
				//making sure pins do not overlap
				if (remX < pin_width / 2) {
					this.setPos(this.pos().x() - pin_width/2, this.pos().y());
					colliding = this.checkCollisions();
					this.setPos(this.pos().x() + pin_width/2, this.pos().y());
				}
				else {
					this.setPos(this.pos().x() + pin_width/2, this.pos().y());
					colliding = this.checkCollisions();
					this.setPos(this.pos().x() - pin_width/2, this.pos().y());
				}
				if ( colliding ) {
					this.setPos(last_pos);
				}
				else { this.setPos(new QPointF(this.pos().x() - offsetX, this.pos().y() - offsetY)); }					
			}
			this.last_pos = this.pos();
			
			this.updatePinPosition();
		}
		//If the button released was the right button, then display the pop-up menu
		else if (event.button() == MouseButton.RightButton) {
			QMenu popup_menu = new QMenu();
			popup_menu.addAction(new QIcon(VSRTool.getImagePath("trash.png")), "Remove", this, "sendRemoveElementCommand()"); //delete bel option
			if (this instanceof Bel)
				popup_menu.addAction(new QIcon(VSRTool.getImagePath("bel.png")), "Bel Config Options", this,  "showConfig()"); 
			popup_menu.popup(event.screenPos());
		}
	}

	
	/**
	 * Redraws the wires connected to each pin of the element as the element is being moved 
	 * @param event QGraphiceSceneMouseEvent containing the mouse move information 
	 */
	public void mouseMoveEvent (QGraphicsSceneMouseEvent event) {
	
		super.mouseMoveEvent(event);
		
		int i = this.firstIn;
		int j = this.firstOut;
		
		//updates wires connected to input pins
		for (QTreePin pin : this.element.getIn_pins()) {
			QPointF new_location = this.getPinLocation(true, i);
			
			for (Wire wire : pin.get_wires()) 
				wire.update_pin_position(pin.getLast_move_location(), new_location);
			
			pin.setLast_move_location(new_location);
			i++;
		}
		
		//update wires connected to output pins
		for (QTreePin pin : this.element.getOut_pins()) {
			QPointF new_location = this.getPinLocation(false, j);
		
			for (Wire wire : pin.get_wires()) 
				wire.update_pin_position(pin.getLast_move_location(), new_location);
			
			pin.setLast_move_location(new_location);
			j++;
		}	
	}
	/**
	 * Returns the shape where the object is selectable.  
	 */
	@Override 
	public QPainterPath shape(){
		return path;
	}
	
	/**
	 * This function updates the pin positions on the graphics scene for each element pin 
	 */
	public void updatePinPosition(){
		int i = 1;
		PrimitiveSiteScene scene = (PrimitiveSiteScene) this.scene();
		QPointF new_location;
		
		//updating input pin positions
		for (QTreePin pin : element.getIn_pins()) {
			
			new_location = this.getPinLocation(true, i);
			
			scene.add_pin(pin.getLastLocation(),new_location, pin);
		
			for (Wire wire : pin.get_wires()) 
				wire.update_pin_position(pin.getLast_move_location(), new_location);
			
			pin.setLastLocation( new_location );
			pin.setLast_move_location( new_location );
			i++;
		}
		
		//updating output pin positions
		i=this.firstOut;
		for (QTreePin pin : element.getOut_pins()) {
		
			new_location = this.getPinLocation(false, i);
			//update the data structure keeping track of the position to pin mappings for the new pin locations
			scene.add_pin(pin.getLastLocation(), new_location, pin);
			
			//update the position of each wire that is connected to the bel
			for (Wire wire : pin.get_wires()) 
				wire.update_pin_position(pin.getLast_move_location(), new_location);
			
			//update the position of each pin of the bel
			pin.setLastLocation( new_location );
			pin.setLast_move_location(new_location);
			i++;
		}
	}
	
	/**
	 * Returns the corresponding pin position for the Nth input or output element pin <br>
	 * based on the current rotation of the object
	 * @param input
	 * @param pos
	 * @return
	 */
	private QPointF getPinLocation(boolean input, int pos){
		QPointF new_location;
		double x = this.mapToScene( this.path.boundingRect().topLeft() ).x();
		double y = this.mapToScene( this.path.boundingRect().topLeft() ).y();
		
		if (rotationAngle == 0) {
			new_location = new QPointF(x + (input ? 0 : width), y + pos*pin_width);
		}else if (rotationAngle == 90) {	
			new_location = new QPointF(x - pos*pin_width , y + (input ? 0 : width) );
		}
		else if (rotationAngle == 180) {
			new_location = new QPointF(x + (input ? width : 0), y + pos*pin_width);
		}else {
			new_location = new QPointF(x + pos*pin_width, y - (input ? 0 : width) );
		}
		
		return new_location;
	}
	/**
	 * Rotates the element by 90 degrees clockwise if the argument passed in is true <br>
	 * else, it rotates it counterclockwise. 
	 */
	public void rotate(boolean clockwise) {
		if (clockwise) {
			this.rotationAngle = (rotationAngle==270) ? 0 : rotationAngle + 90;
		}else {
			this.rotationAngle = (rotationAngle==0) ? 270 : rotationAngle - 90;
		}
			
		this.setRotation( rotationAngle==180 ? 0 : rotationAngle );
		
		this.updatePinPosition();
		this.scene().update();
	}
	/**
	 * Rotates the element to the specified angle...the only values that should <br>
	 * be passed into this function are 0, 90, 180, and 270
	 */
	public void rotateTo(double angle) {
		this.rotationAngle = angle;
		
		this.setRotation( rotationAngle==180 ? 0 : rotationAngle );
		
		this.updatePinPosition();
		this.scene().update();
	}
	/**
	 * Returns the current rotation angle of the element 
	 */
	public double getRotationAngle(){
		return this.rotationAngle;
	}
	public void setRotationAngle(double angle){
		this.rotationAngle = angle; 
	}
}
