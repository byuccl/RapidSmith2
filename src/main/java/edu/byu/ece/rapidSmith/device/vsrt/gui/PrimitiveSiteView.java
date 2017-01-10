package edu.byu.ece.rapidSmith.device.vsrt.gui;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.QRectF;
import com.trolltech.qt.core.Qt.AspectRatioMode;
import com.trolltech.qt.core.Qt.CursorShape;
import com.trolltech.qt.gui.QCursor;
import com.trolltech.qt.gui.QGraphicsView;
import com.trolltech.qt.gui.QPainter;
import com.trolltech.qt.gui.QPixmap;
import com.trolltech.qt.gui.QWheelEvent;

import edu.byu.ece.rapidSmith.device.vsrt.gui.shapes.Site;


/**
 * Custom QGraphicsView class created to handle zooming on wheel scroll events
 * @author Thomas Townsend
 * Jun 10, 2014 3:17:00 PM
 */
public class PrimitiveSiteView extends QGraphicsView{

	VSRTool parent;
	/**Determines if the view should scroll or zoom on a mouseScrollEvent*/
	private boolean should_zoom = false;
	/**Determines if the view should scroll horizontally on a mouse Scroll Event*/
	private boolean shouldHScroll = false;
	/**Zooming interval*/
	final private double scaleFactor = 1.20;
	/**Maximum zoom out amount*/
	private double ZOOM_OUT_MAX = .1;
	/**Maximum zoom in amount*/
	final private double ZOOM_IN_MAX = 3;
	/***/
	private QCursor cursor = new QCursor(); 
	/**Cursor image used when the delete item has been selected*/
	private QPixmap deleteImage;
	/**Cursor image used when the zoom selection option has been selected*/
	private QPixmap zoomSelectionImage;
	/**grid square size*/
	private double pinWidth;
	
	/**
	 * Constructs a new PrimitiveSiteView object
	 * @param parent Top level window
	 * @param scene Top level scene
	 */
	public PrimitiveSiteView(VSRTool parent, PrimitiveSiteScene scene) {
		super(scene);
		this.parent = parent;
		this.pinWidth = scene.getSquare_size();
		//Zoom around the center of the device view
		this.setTransformationAnchor(QGraphicsView.ViewportAnchor.AnchorViewCenter);
		this.setCursor(cursor);
		this.setInteractive(true);
		this.setDragMode(DragMode.RubberBandDrag);	
		
		zoomSelectionImage = new QPixmap( VSRTool.getImagePath().resolve("zoomcursor.png").toString() );
		deleteImage = new QPixmap( VSRTool.getImagePath().resolve("deleteCursor.png").toString() );
		
		//include anti-aliasing to the view so that lines and text look decent when zooming in and out
		this.setRenderHints(QPainter.RenderHint.TextAntialiasing, QPainter.RenderHint.Antialiasing);
		this.setOptimizationFlags(OptimizationFlag.DontAdjustForAntialiasing , OptimizationFlag.DontClipPainter);
		this.setEnabled(false);
	}
	/**
	 * Zooms the device view when the mouse is scrolled and should_scroll is true
	 * @param event QWheelEvent that captures the mouse scroll event 
	 * @return none
	 */
	protected void wheelEvent(QWheelEvent event) {
		if ( this.should_zoom ) {  
			
			if(event.delta() > 0) {
				this.zoom_in();
			}
			else {
				this.zoom_out();
			}
		} 
		else if( this.shouldHScroll ) {
			
			if(event.delta() > 0) {
				this.horizontalScrollBar().setValue(this.horizontalScrollBar().value() - (int)(5*pinWidth));
			}
			else {
				this.horizontalScrollBar().setValue(this.horizontalScrollBar().value() + (int)(5*pinWidth));
			}
		
		}
		else
		{ super.wheelEvent(event); } 
	}
	
	public void scrollHorizontal(int amount){
		this.horizontalScrollBar().setValue(this.horizontalScrollBar().value() + amount);
	}
	/**
	 * Zooms in the device view
	 * @param none
	 * @return none
	 */
	public void zoom_in(){
		if (this.transform().m11() < ZOOM_IN_MAX) 
			scale(scaleFactor, scaleFactor);
	}
	/**
	 * Zooms out the device view by the pre-specified scale factor
	 * @param none
	 * @return none
	 */
	public void zoom_out(){
		if (this.transform().m11() > ZOOM_OUT_MAX )
			scale(1.0/scaleFactor, 1.0/scaleFactor);
	}
	/**
	 * Sets whether or not the view should scroll (default behavior) or zoom when the mouse wheel is scrolled
	 * @param none
	 * @return double Scale factor
	 */
	public void set_shouldZoom(boolean should_scroll){
		this.should_zoom = should_scroll;
	}
	
	/**
	 * Gets the current zoom level <br>
	 * 1 = no zoom <br>
	 * .6 = minimum zoom <br>
	 * 2.6 = maximum zoom
	 * @param none
	 * @return double Zoom level
	 */
	public double getZoomLevel(){
		//return this.zoom_level;
		return this.transform().m11();
	}
	public void setMaxZoomOutLevel(double level){
		this.ZOOM_OUT_MAX = level; 
	}
	
	/**
	 * Returns true if the view is less than the maximum zoom in level
	 * @return
	 */
	public boolean shouldZoomIn(){
		return (this.transform().m11() < ZOOM_IN_MAX ? true  : false); 
	}
	/**
	 * Returns true if the view should horizontal scroll rather than zoom in/out on a mouse wheel scroll
	 * @param scroll
	 */
	public void setShouldHScroll(boolean scroll){
		this.shouldHScroll = scroll;
	}
	
	/**
	 * Sets the cursor shape on the view to the argument passed in
	 * @param shape
	 * @param deleteCursor
	 */
	public void setCursorShape(CursorShape shape, boolean deleteCursor){
		
		if (shape == CursorShape.CustomCursor) 
			if (deleteCursor)
				this.cursor = new QCursor(deleteImage, 0, 0);
			else {
				this.cursor = new QCursor(zoomSelectionImage, 0, 0);
			}
		else{	this.cursor = new QCursor(shape) ; } 
		
		
		this.setCursor(cursor);
	}
	/**
	 * This method zooms out the view on the graphics scene to fit the entire <br>
	 * primitive site into view. 
	 */
	public void zoomToBestFit(){
		Site siteShape = this.parent.getGraphicsSite();
		
		if ( this.isEnabled() ) {
			this.fitInView( new QRectF(new QPointF(siteShape.pos().x() - siteShape.width()/8, siteShape.pos().y() - siteShape.height()/8),
					new QPointF(siteShape.pos().x() + siteShape.width() + siteShape.width()/8 , siteShape.pos().y() + siteShape.height() + siteShape.height()/8))
					,AspectRatioMode.KeepAspectRatio);
		}
		
		this.setMaxZoomOutLevel(this.transform().m11());
	}
}//end class