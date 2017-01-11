/**
 * 
 */
package edu.byu.ece.rapidSmith.device.vsrt.gui.shapes;

import edu.byu.ece.rapidSmith.device.vsrt.gui.PrimitiveSiteScene;
import edu.byu.ece.rapidSmith.device.vsrt.gui.QTreePin;
import edu.byu.ece.rapidSmith.device.vsrt.gui.VSRTool;
import edu.byu.ece.rapidSmith.device.vsrt.gui.XMLCommands;
import edu.byu.ece.rapidSmith.device.vsrt.gui.undoCommands.ResizeSiteCommand;
import edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs.PrimitiveDefPinDirection;

import java.util.ArrayList;
import java.util.HashMap;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.QRectF;
import com.trolltech.qt.core.Qt.MouseButton;
import com.trolltech.qt.gui.QGraphicsItem;
import com.trolltech.qt.gui.QGraphicsSceneMouseEvent;
import com.trolltech.qt.gui.QIcon;
import com.trolltech.qt.gui.QMenu;
import com.trolltech.qt.gui.QPainter;
import com.trolltech.qt.gui.QStyleOptionGraphicsItem;
import com.trolltech.qt.gui.QUndoStack;
import com.trolltech.qt.gui.QWidget;

/**
 * This class creates a QGraphicsItem that represents a Primitive Site <br>
 * on the device view of the GUI
 * @author Thomas Townsend
 * Created: Jun 12, 2014 2:15:47 PM
 */
public class Site extends QGraphicsItem {
	
	/**Site input pins on the device view*/
	private ArrayList<PinShape> inPins = new ArrayList<PinShape>(); 
	/**Site output pins on the device view*/
	private ArrayList<PinShape> outPins = new ArrayList<PinShape>(); 
	/**Distance between pins on the device view*/
	private double pin_width;
	/**height of the site in pixels*/
	private double height;
	/**width of the site in pixels*/
	private double width;
	/**Shape of the site (determines where right click events register)*/
	private QRectF shape;
	/**Where pins start showing on the site*/
	private int pinStart;
	/**Site Popup menu*/
	private QMenu popupMenu;
	/**Maps pins to their graphics objects so they can quickly be accessed*/
	private HashMap<QTreePin, PinShape> pin2graphics = new HashMap<QTreePin, PinShape>();
	/**Edge resize boxes*/
	private ArrayList<ResizeBox> resizeEdgeBoxes = new ArrayList<ResizeBox>();
	/***/
	private QUndoStack undo;
	
	/**
	 * Constructor
	 * @param pins
	 * @param scene
	 */
	public Site(ArrayList<QTreePin> pins, PrimitiveSiteScene scene, XMLCommands xml, boolean pinsSaved, QUndoStack undo){

		this.undo = undo;
		this.pin_width = scene.getSquare_size();
		this.addPins(pins);
		popupMenu  = new QMenu();
		popupMenu.addAction(new QIcon(VSRTool.getImagePath("gear.png")), "Site Config Options", this,  "showSiteConfig()"); 
		
		if(pinsSaved){//restore the site to the settings before it was saved
			height = xml.getGraphicsSiteHeight();
			width = xml.getGraphicsSiteWidth();
			this.setPos(xml.getGraphicsSitePos());
			scene.setSceneRect(new QRectF(0, 0,  2* this.x() + width , height+1200));
		}
		else{ //initialize the site 
			height = (inPins.size() > outPins.size()) ? 2*((double)(inPins.size()+1)*pin_width) : 
														2*((double)(outPins.size()+1)*pin_width) ;
			width = 2*height;
			scene.setSceneRect(new QRectF(0, 0, width + 1200, height+1200));
			this.setPos(scene.sceneRect().x() + (scene.sceneRect().width()-width)/2, 
					scene.sceneRect().y() + (scene.sceneRect().height()-height)/2 );
		}
		
		//starts showing pins in the middle of the site
		pinStart = (int) Math.floor(height/pin_width)/4 - 1; 
		scene.addItem(this);
		
		//Adding corner resize boxes
		this.initializeResizeBoxes(scene);
				
		this.placePinsOnScene(pinsSaved, xml);
		
		shape = new QRectF(0,0, (int)this.width, (int)this.height);
		
		//Flags that prevent the site from being moved, and respond to right click events
		this.setFlag(GraphicsItemFlag.ItemIsMovable, false);
		this.setFlag(GraphicsItemFlag.ItemIsSelectable, false);
		this.setAcceptedMouseButtons(MouseButton.RightButton);
		//makes sure that the site is the behind all other items on the scene
		this.setZValue(0);
	}
	
	/**
	 * Creates and places each resize box into the appropriate location on the site <br>
	 * That is, 4 corner resize boxes are created, and placed in each corner.  And <br>
	 * 4 edge resize boxes are created and placed on each edge.  
	 * @param scene
	 */
	private void initializeResizeBoxes(PrimitiveSiteScene scene){
		//Adding resize boxes
		this.resizeEdgeBoxes.add(new ResizeBox( ResizeBoxType.BOTTOM_RIGHT_CORNER, this, scene.getSquare_size() ) );
		this.resizeEdgeBoxes.add(new ResizeBox( ResizeBoxType.TOP_RIGHT_CORNER, this, scene.getSquare_size() ) );
		this.resizeEdgeBoxes.add(new ResizeBox( ResizeBoxType.TOP_LEFT_CORNER, this, scene.getSquare_size() ) );
		this.resizeEdgeBoxes.add(new ResizeBox( ResizeBoxType.BOTTOM_LEFT_CORNER,  this, scene.getSquare_size() ) );	
		this.resizeEdgeBoxes.add(new ResizeBox( ResizeBoxType.TOP, this, scene.getSquare_size() ) );
		this.resizeEdgeBoxes.add(new ResizeBox( ResizeBoxType.BOTTOM, this, scene.getSquare_size() ) );
		this.resizeEdgeBoxes.add(new ResizeBox( ResizeBoxType.LEFT, this, scene.getSquare_size() ) );
		this.resizeEdgeBoxes.add(new ResizeBox( ResizeBoxType.RIGHT, this, scene.getSquare_size() ) );
		
		for (ResizeBox box : this.resizeEdgeBoxes) {
			this.scene().addItem(box);
		}
	}
	
	private void placePinsOnScene(boolean pinsSaved, XMLCommands xml) {
		
		//Adding each site input pin to the device view
		for (int i = 0, j = pinStart; i < inPins.size(); i++, j++) {
			this.scene().addItem(inPins.get(i));
			QPointF savedLocation = xml.getSavedSitePinLocation(inPins.get(i).getTreePin().get_pinName());
			if (pinsSaved && savedLocation != null){
				inPins.get(i).setPinPos(savedLocation);
			}else {
				inPins.get(i).setPinPos(new QPointF(this.x(), this.y() + (j+1)*this.pin_width));
			}
		}
		//Adding each site output pin to the device view
		for (int i = 0, j = pinStart; i < outPins.size(); i++, j++){
			this.scene().addItem(outPins.get(i));
			QPointF savedLocation = xml.getSavedSitePinLocation(outPins.get(i).getTreePin().get_pinName()); 
			if(pinsSaved && savedLocation != null) {
				outPins.get(i).setPinPos(savedLocation);
			}else {
				outPins.get(i).setPinPos(new QPointF(this.x()+ width, this.y() + (j+1)*this.pin_width));
			}
		}
			
	}
	
	/**
	 * Returns the width of the site 
	 * @return
	 */
	public double width(){
		return this.width;
	}
	/**
	 * Returns the height of the site
	 * @return
	 */
	public double height(){
		return this.height;
	}
		
	/**
	 * Add the site pins (pinShape objects) to the site
	 * @param pins
	 */
	private void addPins(ArrayList<QTreePin> pins){
		
		for (QTreePin pin : pins) {
			PinShape tmp = new PinShape(pin, pin_width);
			if( pin.getPin().getDirection() == PrimitiveDefPinDirection.OUTPUT ) {
				this.inPins.add(tmp);
			}
			else{ 
				this.outPins.add(tmp); 
			}
			this.pin2graphics.put(pin, tmp);
		}
	}
	
	public PinShape getGraphicsPin(QTreePin pin){
		return this.pin2graphics.get(pin);
	}
	
	/**
	 * Finds the first valid position for the pin to be placed on the graphics object <br>
	 * and adds it to the graphics scene at that location.
	 * @param pin
	 * @param init
	 */
	public void addPinToGraphicsScene(PinShape pin, QPointF init){
		QPointF pinPos = new QPointF(init.x(), init.y());
		//add to the actual scene
		this.scene().addItem(pin);
		//look for valid location
		while ( ((PrimitiveSiteScene)this.scene()).isPinAtLocation(pinPos) ) 
			pinPos.setY(pinPos.y() + pin_width);
		
		pinPos.setY(pinPos.y() - pin_width);
		pin.setPinPos(pinPos);
		
		//adding pin to the site arrays
		if( pin.getTreePin().getPin().getDirection() == PrimitiveDefPinDirection.OUTPUT )
			this.inPins.add(pin);
		else 
			this.outPins.add(pin);
		
		//adding it to this hashmap as well
		this.pin2graphics.put(pin.getTreePin(), pin);
		this.scene().update();	
	}
	
	/**
	 * Removes the pin from the graphics view
	 * @param pin
	 */
	public void removePinsFromGraphicsScene(PinShape pin){
		//remove from the actual graphics scene
		this.scene().removeItem(pin);
		//remove pin location from position to pin map
		((PrimitiveSiteScene)this.scene()).remove_pin( pin.getTreePin().getLastLocation() );
		
		//remove from site array
		if ( pin.getTreePin().getPin().getDirection() == PrimitiveDefPinDirection.OUTPUT ){
			this.outPins.remove(pin);
		}
		else {
			this.inPins.remove(pin);
		}
		//remove from this hash map as well
		this.pin2graphics.remove(pin.getTreePin());
		
		this.scene().update();
	}

	/**
	 * Event Handling Method used to show the site configuration options dialog
	 */
	@SuppressWarnings("unused")
	private void showSiteConfig(){
		((PrimitiveSiteScene)this.scene()).getParent().showSiteConfigDialog();
	}
	
	@Override
	public QRectF boundingRect() { 
		// TODO Auto-generated method stub
		return shape;
	}

	@Override
	public void paint(QPainter painter, QStyleOptionGraphicsItem arg1, QWidget arg2) {
		// TODO Auto-generated method stub
		painter.drawRect(shape);
	
	}
	
	/*******************************************************
	 * 				Site Resize Methods					   * 
	 *******************************************************/
	/**
	 * Changes the width of the site while it is being resized
	 * @param amount
	 */
	public void setWidth(double width){
		shape.setWidth(width);
		this.width = width;
	}
	public void setHeight(double height){
		shape.setHeight(height);
		this.height = height;
	}
	public void changeWidth(double amount){
		this.width += amount;
		shape.setWidth(this.width);
	}
	/**
	 * Changes the height of the site while it is being resized. 
	 * @param amount
	 */
	public void changeHeight(double amount) {
		this.height += amount;
		shape.setHeight(this.height);	
	}
	/**
	 * This method updates the position of the site on the graphics scene <br>
	 * while it is being resized.
	 * @param Xamount
	 * @param Yamount
	 */
	public void changePos(double Xamount, double Yamount ) {
		this.setPos(this.pos().x() + Xamount, this.pos().y() + Yamount );
	}
	
	/**
	 * Places each resize box into the correct location after the site is done being resized. 
	 */
	public void updateResizeBoxPositions(){	
		for (ResizeBox box : this.resizeEdgeBoxes) {
			box.updateBoxPos();
		}
		this.scene().update();
	}
	
	/**
	 * This method moves all of either the input or output site pins by the specified amount. <br>
	 * Used for moving the pins while the site is in the process of being resized. 
	 * @param amount
	 * @param moveOutputPins
	 */
	public void updatePinPositions(double amount, boolean moveOutputPins){
		if(moveOutputPins){
			for (PinShape pin : this.outPins) {
				pin.setPos(pin.pos().x() + amount, pin.pos().y());
			}
		}else {
			for (PinShape pin : this.inPins) {
				pin.setPos(pin.pos().x()+ amount, pin.pos().y());
			}
		}	
	}
	
	/**
	 * After a site has been resized, this method will snap the site to the nearest valid location <br>
	 * on the grid.  Also, it will update the pin positions for each site pin on the grid.  
	 */
	public void snapSiteToGrid(){
		//calculate the nearest valid grid location 
		double remX = this.pos().x() % pin_width;
		double remY = this.pos().y() % pin_width;	
		double offsetX = (remX < pin_width/2) ? remX : -(pin_width - remX) ;
		double offsetY = (remY < pin_width/2) ? remY : -(pin_width - remY) ;
		
		//set the position there
		this.setPos(new QPointF(this.pos().x() - offsetX, this.pos().y() - offsetY));
	
		//update the width to be an integer multiple of the grid size
		remX = this.width() % pin_width;
		offsetX = (remX < pin_width/2) ? remX : -(pin_width - remX) ;
		this.changeWidth(-offsetX);
		
		//snap any moving site pins to the grid as well, and update their positions on the grid 
		QGraphicsSceneMouseEvent event = new QGraphicsSceneMouseEvent();
		event.setButton(MouseButton.LeftButton);
		for (PinShape pin : this.outPins) {
			pin.mouseReleaseEvent(event);
		}
		for (PinShape pin : this.inPins) {
			pin.mouseReleaseEvent(event);
		}
	}
	
	/**
	 * Returns true is the site can be resized.
	 * @return
	 */
	public boolean canResize(){
		return !((PrimitiveSiteScene)this.scene()).itemsFrozen();
	}
	 
	//Need to override all of these to accept rightclick events only
	@Override 
	public void mouseMoveEvent(QGraphicsSceneMouseEvent event){
	}
	@Override
	public void mousePressEvent (QGraphicsSceneMouseEvent event){
	}
	@Override
	public void mouseReleaseEvent(QGraphicsSceneMouseEvent event){
		if(event.button() == MouseButton.RightButton){
			this.popupMenu.popup(event.screenPos());
		}
	}
	public double getPinWidth(){
		return this.pin_width;
	}
	public ArrayList<PinShape> getInPins(){
		return this.inPins;
	}
	public ArrayList<PinShape> getOutPins(){
		return this.outPins;
	}
	public void pushSiteResizeCommand(ResizeSiteCommand cmd){
		this.undo.push(cmd);
	}
}