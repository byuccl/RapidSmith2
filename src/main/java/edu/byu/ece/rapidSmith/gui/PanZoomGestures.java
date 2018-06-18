//package edu.byu.ece.rapidSmith.gui;
//
//import edu.byu.ece.rapidSmith.device.browser.TileViewJavaFx;
//import javafx.scene.Cursor;
//import javafx.scene.Group;
//import javafx.scene.Node;
//import javafx.scene.input.MouseButton;
//import javafx.scene.input.MouseEvent;
//import javafx.scene.input.ScrollEvent;
//
//public class PanZoomGestures {
//
//    private static final double MAX_SCALE = 30d;
//    private static final double MIN_SCALE = .05d;
//
//    private DragContextJavaFx zoomDragContext = new DragContextJavaFx();
//
//    TileViewJavaFx pane;
//
//    public PanZoomGestures(TileViewJavaFx pane) {
//        this.pane = pane;
//    }
//    public void handleEvent(){
//        pane.setOnMouseClicked(e -> handlePan(e));
//        pane.setOnMouseDragged(d -> handleDrag(d));
//        pane.setOnScroll(s -> zoomOnScroll(s));
//
//    }
//
//    /**basically the handler for when you pan with the mouse secondary button*/
//    public void handlePan(MouseEvent e){
//        zoomDragContext.mouseAnchorX = e.getSceneX();
//        zoomDragContext.mouseAnchorY = e.getSceneY();
//        zoomDragContext.translateAnchorX = pane.getTranslateX();
//        zoomDragContext.translateAnchorY = pane.getTranslateY();
//
//    }
//
//    /**Handles dragging of mouse when right mouse button is done*/
//    public void handleDrag(MouseEvent d) {
//        //pane.setCursor(Cursor.CLOSED_HAND);
//        pane.setTranslateX(zoomDragContext.translateAnchorX + d.getX() - zoomDragContext.mouseAnchorX);
//        pane.setTranslateY(zoomDragContext.translateAnchorY + d.getY() - zoomDragContext.mouseAnchorY);
//    }
//
//    /**Basically the handler for when you scroll to zoom*/
//    public void zoomOnScroll(ScrollEvent s){
//       // System.out.println("scroll detected!");
//        double delta = 1.2;
//
//        double scale = pane.getScale(); // currently we only use Y, same value is used for X
//        double oldScale = scale;
//
//        if (s.getDeltaY() < 0)
//            scale /= delta;
//        else
//            scale *= delta;
//
//        scale = clamp( scale, MIN_SCALE, MAX_SCALE);
//
//        //double f = (scale / oldScale)-1;
//        double f = scale -oldScale;
//
//        double dx = (s.getX() - (pane.getBoundsInParent().getWidth()/2 + pane.getBoundsInParent().getMinX()));
//        double dy = (s.getY() - (pane.getBoundsInParent().getHeight()/2 + pane.getBoundsInParent().getMinY()));
//
//        pane.setScale( scale);
//
//        // note: pivot value must be untransformed, i. e. without scaling
//        pane.setPivot(f*dx, f*dy);
//
//    }
//
//
//    public void resetZoom () {
//        double scale = 1.0d;
//
//        double x = pane.getTranslateX();
//        double y = pane.getTranslateY();
//
//        pane.setPivot(x, y);
//    }
//
//    public static double clamp(double value, double min, double max) {
//
//        if (Double.compare(value, min) < 0)
//            return min;
//
//        if (Double.compare(value, max) > 0)
//            return max;
//
//        return value;
//    }
//}
//
