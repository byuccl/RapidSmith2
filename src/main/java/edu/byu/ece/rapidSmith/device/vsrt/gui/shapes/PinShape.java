/**
 * 
 */
package edu.byu.ece.rapidSmith.device.vsrt.gui.shapes;

import edu.byu.ece.rapidSmith.device.vsrt.gui.PrimitiveSiteScene;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreePin;
import edu.byu.ece.rapidSmith.device.vsrt.gui.VsrtColor;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.PrimitiveDefPinDirection;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.QRectF;
import com.trolltech.qt.core.Qt.AlignmentFlag;
import com.trolltech.qt.core.Qt.LayoutDirection;
import com.trolltech.qt.core.Qt.MouseButton;
import com.trolltech.qt.gui.QFont;
import com.trolltech.qt.gui.QFontMetrics;
import com.trolltech.qt.gui.QGraphicsItem;
import com.trolltech.qt.gui.QGraphicsSceneMouseEvent;
import com.trolltech.qt.gui.QPainter;
import com.trolltech.qt.gui.QPainterPath;
import com.trolltech.qt.gui.QStyleOptionGraphicsItem;
import com.trolltech.qt.gui.QWidget;

/**
 * This class is used to draw Site Pins on the device view of the GUI
 * @author Thomas Townsend
 * Created: Jun 13, 2014 4:34:52 PM
 * Updated: Jun 17th 
 */
public class PinShape extends QGraphicsItem{
	
	/**Pin that this Graphics Item represents*/
	private QTreePin pin; 
	/**height of the pin */
	private double height;
	/**Bounding rectangle of the site pin*/
	private QRectF bounding;
	/**Device view grid location where this pin was last located*/
	private QPointF lastLocation; 
	/**Used to determine size of site pin name*/
	private QFont font = new QFont();
	/**Selectable area of the object*/
	private QPainterPath path = new QPainterPath();

	/**
	 * Constructor
	 * @param pin
	 * @param pin_width
	 */
	public PinShape(QTreePin pin, double pin_width){	
		this.pin = pin;
		this.font.setPointSizeF(pin_width/3);
		this.height = pin_width;
		
		this.setFlag(GraphicsItemFlag.ItemIsMovable, true);
		this.setFlag(GraphicsItemFlag.ItemIsSelectable, true);
		this.setAcceptsHoverEvents(true);
		
		//QFontMetrics is used to calculate what the length of the pin name will be once it is drawn. This makes it 
		//easy to create the bounding and shape rect of the object.
		QFontMetrics fm = new QFontMetrics(font);
		//Input Pins
		if (pin.getPin().getDirection() == PrimitiveDefPinDirection.OUTPUT) 
			bounding = new QRectF( -(double)fm.width(pin.getPin().getInternalName()), 3*pin_width/4, (double) fm.width(pin.getPin().getInternalName())+ pin_width/2, pin_width/2 + 2);		
		else //Output Pins
			bounding = new QRectF( -pin_width/2, 3*pin_width/4, (double) fm.width(pin.getPin().getInternalName())+ pin_width/2 + 2, pin_width/2 + 2);		
		
		path.addRect(this.bounding);
		this.setZValue(1);
	}

	/**
	 * Returns the QTreePin associated with this graphics pin
	 */
	 
	public QTreePin getTreePin(){
		return pin;
	}
	
	/**
	 * Moves the pin to the new location passed into the function, and
	 * updates the proper data structures to reflect the change 
	 * @param pos New pin position
	 */
	public void setPinPos(QPointF pos) {
		QPointF newPinLocation = new QPointF(pos.x(), pos.y() + this.height );
		
		if(((PrimitiveSiteScene)this.scene()).isPinAtLocation(newPinLocation) == false ) { 
			//redraw all of the wires to the new pin location
			for (Wire wire : pin.get_wires()) {
				wire.update_pin_position(pin.getLastLocation(), newPinLocation);
			}
		
			//updating the pin position within the PrimitiveSiteScene pin2gridLocations HashMap
			((PrimitiveSiteScene)this.scene()).add_pin(pin.getLastLocation(), newPinLocation, pin);
			//Updated pin for future moves
			this.pin.setLast_move_location(newPinLocation);
			this.pin.setLastLocation(newPinLocation);	
			this.setPos(pos);
			this.lastLocation = pos; 
		} else
		{ 	this.setPos(this.lastLocation);  }	
	}
	
	public QPointF getPinPos(){
		return pin.getLastLocation(); 
	}
	
	public QPointF getLastLocation(){
		return this.lastLocation;
	}
	
	@Override
	public QRectF boundingRect() {
		// TODO Auto-generated method stub
		return bounding;
	}

	/**
	 * Paint the site pin 
	 *
	 **/
	@Override
	public void paint(QPainter painter, QStyleOptionGraphicsItem arg1, QWidget arg2) {
		// TODO Auto-generated method stub
		if ( this.isSelected() ) 
			painter.setPen(VsrtColor.blue);
		else if ( !this.pin.getPin().isConnected() )
			painter.setPen(VsrtColor.darkGray); 
		else {	painter.setPen(VsrtColor.black);	}
		painter.setFont(font);
		
		//Input Pins
		if (this.pin.getPin().getDirection() == PrimitiveDefPinDirection.OUTPUT) {
			painter.setLayoutDirection(LayoutDirection.RightToLeft);
			painter.drawText((int)-this.bounding.width(),3*(int)height/4, (int)bounding.width(), (int)height/2, AlignmentFlag.AlignVCenter.value(), pin.get_pinName() );
			painter.drawLine(0, (int)height, (int)height/2, (int)height);
		}else { //Output Pins:
			painter.drawText(3,3*(int)height/4, (int)bounding.width(), (int)height/2, AlignmentFlag.AlignVCenter.value(), pin.get_pinName() );
			painter.drawLine(0, (int)height, -(int)height/2, (int)height);
		}
	}
	
	@Override 
	public QPainterPath shape(){
		return path;
	}
	
	/**
	 * Updates the input and output pin positions when the element is moved,
	 * as well as displays the pop-up menu when the element is right clicked. 
	 * @param event QGraphiceSceneMouseEvent containing the mouse release information 
	 */
	public void mouseReleaseEvent (QGraphicsSceneMouseEvent event) {
		
		//if the button released was the left button, this means the object was moved
		//and the pin positions need to be updated. 
		super.mouseReleaseEvent(event);
		if (event.button() == MouseButton.LeftButton)
		{

				double offsetX, offsetY;
				
				double remX = this.pos().x() % height;
				double remY = this.pos().y() % height;
				
				offsetX = (remX < height/2) ? remX : -(height - remX) ;
				offsetY = (remY < height/2) ? remY : -(height - remY) ;

				this.setPinPos(new QPointF(this.pos().x() - offsetX, this.pos().y() - offsetY));
		} 
	}
}
