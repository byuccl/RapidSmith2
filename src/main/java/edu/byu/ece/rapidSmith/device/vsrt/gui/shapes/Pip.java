package edu.byu.ece.rapidSmith.device.vsrt.gui.shapes;

import edu.byu.ece.rapidSmith.device.vsrt.gui.PrimitiveSiteScene;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreeElement;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreePin;
import edu.byu.ece.rapidSmith.device.vsrt.gui.VsrtColor;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.QRectF;
import com.trolltech.qt.core.Qt.AlignmentFlag;
import com.trolltech.qt.core.Qt.LayoutDirection;
import com.trolltech.qt.gui.QPainter;
import com.trolltech.qt.gui.QPolygonF;
import com.trolltech.qt.gui.QStyleOptionGraphicsItem;
import com.trolltech.qt.gui.QWidget;

/**
 * This class is used to add and draw site pips on the device view
 * @author Thomas Townsend
 * Created: Jun 10, 2014 3:36:10 PM
 */
public class Pip extends ElementShape {	
	QPolygonF shape;
		
	/**
	 * Initializes the PIP object
	 * @param element QTreeElement that this drawn pip represents
	 * @param pin_width The size of the grid on the device view which also represents
	 * 					the distance between adjacent pip pins
	 * @param start The starting location of the PIP 
	 */
	public Pip (QTreeElement element, double pin_width, QPointF start) {
		super(element, pin_width, start, false);
		
		this.setPos(start);
		this.calculateShapeSize();
		this.calculateFontSize();
		this.calculateOutputPinLocation();	
		this.createPipShape();	
	}
	/**
	 * 
	 */
	private void calculateShapeSize(){
		this.height = (element.getIn_pins().size() > element.getOut_pins().size()) ? ((double)(element.getIn_pins().size()+1)*pin_width) : 
			 ((double)(element.getOut_pins().size()+1)*pin_width)  ;
		
		this.width = height/2;
		width += width % this.pin_width;
	}
	/**
	 * 
	 */
	private void calculateOutputPinLocation(){
		this.firstOut = ((int) Math.ceil(height/2/this.pin_width));
		this.firstOut -= (element.getIn_pins().size() % 2 == 0) ? 1 : 0 ;
	}
	
	/**
	 * 
	 */
	private void createPipShape(){
		this.bounding = new QRectF(0, 0, width + 5, height + pin_width/2 );
		
		//The shape of a site pip is a trapezoid, and so these 4 points form the drawn trapazoid.
		shape  = new QPolygonF();
		shape.add(0, 0);
		shape.add(0, (int)(height)); 
		shape.add((int)(width),(int)(3*height/4) );
		shape.add((int)(width),(int)(height/4)  );
		shape.add(0, 0);
		path.addPolygon(shape);
		
		if (width == height){
			this.setTransformOriginPoint(width/2 , height/2 );
		}
		else {
			this.setTransformOriginPoint(width/2 - (width/2 % pin_width), height/2 - (height/2 % pin_width));
		}
	}
	
	/**
	 * 
	 */
	public void resizePip(){
		this.calculateShapeSize();
		this.calculateFontSize();
		this.calculateOutputPinLocation();	
		this.createPipShape();	
	}
	
	/**
	 * Paints the PIP object on the device view in its current location
	 * @param painter QPainter object used to do the actual painting
	 * @param item QStyleOptionGraphicsItem specifying the paint options
	 * @param widget QWidget that is being painted on
	 */
	@Override
	public void paint(QPainter painter, QStyleOptionGraphicsItem item, QWidget widget) {
		// TODO Auto-generated method stub
		
		painter.setPen(getBorderColor());
		if ( this.isSelected() ) {
			painter.setBrush(VsrtColor.blue);
			painter.drawPolygon(shape);	
			painter.drawText(0, (int)this.height, (int)this.width, (int)pin_width/2, AlignmentFlag.AlignTop.value(), element.getElement().getName());
			painter.setPen(VsrtColor.white);
		}
		else {
			painter.setBrush(VsrtColor.green);
			painter.drawPolygon(shape);	
			painter.setPen(VsrtColor.black);
			painter.drawText(0, (int)this.height, (int)this.width, (int)pin_width/2, AlignmentFlag.AlignTop.value(), element.getElement().getName());
		}
		
		//only draw pip and pin names if the device view is zoomed in enough that they can be read
		if ( ((PrimitiveSiteScene)this.scene()).get_ViewZoom() > .3 ){
			//Pip name
			painter.setFont(font);
			
			//input pins (left justified)
			int i = firstIn;
			for (QTreePin pin : element.getIn_pins()) { 
				painter.drawText(new QRectF(2,(int)( i*(pin_width/2) ), width/2, pin_width), AlignmentFlag.AlignVCenter.value(), pin.get_pinName() );
				i += 2;
			}
			
			//Output Pins (right justified, and starting in the calculated location)
			i = 1 + 2*(this.firstOut-1);
			painter.setLayoutDirection(LayoutDirection.RightToLeft);
			for (QTreePin pin : element.getOut_pins()) {
					double xPos = this.width/2;  //- ave_char_size*pin.get_pinName().length(); 
					painter.drawText(new QRectF(xPos,(int)( i*(pin_width/2) ), width - xPos, pin_width)
								, AlignmentFlag.AlignVCenter.value(), pin.get_pinName() );
				i += 2;
			}
			painter.setLayoutDirection(LayoutDirection.LeftToRight); 
		}
		
	}	
}
