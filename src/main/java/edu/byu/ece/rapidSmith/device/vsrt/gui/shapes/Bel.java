package edu.byu.ece.rapidSmith.device.vsrt.gui.shapes;

import edu.byu.ece.rapidSmith.device.vsrt.gui.PrimitiveSiteScene;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreeElement;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreePin;
import edu.byu.ece.rapidSmith.device.vsrt.gui.VsrtColor;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.PrimitiveDefPinDirection;

import java.util.HashSet;
import java.util.Set;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.QRectF;
import com.trolltech.qt.core.Qt.AlignmentFlag;
import com.trolltech.qt.core.Qt.LayoutDirection;
import com.trolltech.qt.gui.QPainter;
import com.trolltech.qt.gui.QStyleOptionGraphicsItem;
import com.trolltech.qt.gui.QWidget;


/**
 * This class is used to add and draw Bels on the device view
 * @author Thomas Townsend
 * June 10, 2014 3:18:38 PM
 */

public class Bel extends ElementShape{
	
	/**Bounding shape where the item can be selected */	
	QRectF shape;
		
	/**
	 * Initializes the Bel object
	 * @param element  QTreeElement that this drawn bel represents
	 * @param pin_width The size of the grid on the device view which also represents
	 * 					the distance between adjacent bel pins. 
	 * @param startingPos The starting location of the PIP 
	 */
	public Bel(QTreeElement element, double pin_width, QPointF startingPos){  //BelType bel_type){
		super(element, pin_width, startingPos, true);

		//Uses the max between the number of input or output pins to determine the height of the bel
		this.height = (element.getIn_pins().size() > element.getOut_pins().size()) ? ((double)(element.getIn_pins().size()+1)*pin_width) : 
																					 ((double)(element.getOut_pins().size()+1)*pin_width);
		this.width = .7*this.height;
		//matching the width of the bel to the nearest grid square length
		this.width = this.width + (this.pin_width - (this.width % this.pin_width) );
		
		//Setting font size based on the width of the bel
		this.calculateFontSize();
		
		this.bounding = new QRectF(this.x(), this.y(), this.width + 5, this.height+ pin_width/2);
		this.setPos(startingPos);
		
		shape = new QRectF(0,0, (int)this.width, (int)this.height);
		path.addRect(shape);
		this.setCacheMode(CacheMode.DeviceCoordinateCache);		
		
		if (width == height){
			this.setTransformOriginPoint(width/2 , height/2 );
		}
		else {
			this.setTransformOriginPoint(width/2 - (width/2 % pin_width), height/2 - (height/2 % pin_width));
		}
	}
	
	/**
	 * Paints the Bel object on the device view in its current location
	 * @param painter QPainter object used to do the actual painting
	 * @param item QStyleOptionGraphicsItem specifying the paint options
	 * @param widget QWidget that is being painted on
	 */
	@Override
	public void paint(QPainter painter, QStyleOptionGraphicsItem item, QWidget widget) {
		//painter.rotate(90);
		// TODO Auto-generated method stub
		if ( this.isSelected() ) {
			painter.setBrush(VsrtColor.blue);
			painter.drawRect(shape);
			painter.drawText(0, (int)this.height, (int)this.width, (int)pin_width/2, AlignmentFlag.AlignTop.value(), element.getElement().getName());
			painter.setPen(VsrtColor.white);
		}
		else {
			painter.setBrush(VsrtColor.yellow);
			painter.setPen(VsrtColor.black);
			painter.drawRect(shape);
			painter.drawText(0, (int)this.height, (int)this.width, (int)pin_width/2, AlignmentFlag.AlignTop.value(), element.getElement().getName());
		}
		
		//Right now, the shape of a bel is just a rectangle
		//painter.drawRect(shape);	
		
		
		//only draw bel and pin names if the device view is zoomed in enough that they can be read
		if ( ((PrimitiveSiteScene)this.scene()).get_ViewZoom() > .3 ){ 

			painter.setFont(font);
			//draw the input pin names in white in their corresponding position (left justified)
			int i = 1;
			
			for (QTreePin pin : (this.rotationAngle==180) ? element.getOut_pins() : element.getIn_pins()) { 
				//helps user see which pins are of type inout.
				if (pin.getPin().getDirection() == PrimitiveDefPinDirection.INOUT)
					painter.setPen(VsrtColor.darkGray);

				painter.drawText(new QRectF(3,(int)( i*(pin_width/2) ), width/2, pin_width), AlignmentFlag.AlignVCenter.value(), pin.get_pinName() );
				i += 2;
			}
			//draw the output pin names in white in their corresponding position (right justified)
			i = 1;
			painter.setLayoutDirection(LayoutDirection.RightToLeft);
			for (QTreePin pin : (this.rotationAngle==180) ? element.getIn_pins() :element.getOut_pins()) {
					double xPos = this.width/2; 
					
					if (pin.getPin().getDirection() == PrimitiveDefPinDirection.INOUT)
						painter.setPen(VsrtColor.darkGray);
					
					painter.drawText(new QRectF(xPos,(int)( i*(pin_width/2) ), width-xPos, pin_width)
								, AlignmentFlag.AlignVCenter.value(), pin.get_pinName() );
				i += 2;
			}
			painter.setLayoutDirection(LayoutDirection.LeftToRight);
		}	
	}
}