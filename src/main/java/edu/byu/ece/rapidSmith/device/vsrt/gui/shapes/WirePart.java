/**
 * 
 */
package edu.byu.ece.rapidSmith.device.vsrt.gui.shapes;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.QRectF;
import com.trolltech.qt.gui.QGraphicsLineItem;
import com.trolltech.qt.gui.QLineF;
import com.trolltech.qt.gui.QPainterPath;
import com.trolltech.qt.gui.QGraphicsItem.GraphicsItemFlag;
import com.trolltech.qt.gui.QPixmap;

/**
 * 
 * @author Thomas Townsend
 * Created: Jun 11, 2014 3:56:03 PM
 */
public class WirePart extends QGraphicsLineItem{

	Wire parent;
	QRectF bounding;
	QPainterPath path = new QPainterPath();
	QPixmap deleteImage = new QPixmap("images/remove.gif"); 
	
	public WirePart(QPointF start, QPointF end, Wire parent) {
	
		this.setLine(new QLineF(start, end));
		this.parent = parent;
		
		this.setFlag(GraphicsItemFlag.ItemIsSelectable, false);
		this.setFlag(GraphicsItemFlag.ItemIsMovable, false);
		this.setAcceptHoverEvents(true);
		
		if (start.y() == end.y()) {
			this.bounding = new QRectF(start.x(), start.y()-5, end.x() - start.x(), 10);  

		}
		else {
			this.bounding = new QRectF(start.x() - 5, start.y(), 10, end.y() - start.y()); 
		}			
		
		path.addRect(bounding);
		this.setZValue(2);
	}
	
	public Wire getParentWire(){
		return parent; 
	}

//Need these once I update wire grid...
//	@Override
//	public QRectF boundingRect() {
//		// TODO Auto-generated method stub
//		return bounding;
//	}
//	
//	@Override 
//	public QPainterPath shape(){
//
//		return path; 
//	}
}

