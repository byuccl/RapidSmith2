package edu.byu.ece.rapidSmith.gui;

import edu.byu.ece.rapidSmith.device.browser.TileViewJavaFx;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

public class PaneZoomGestures {

    private static final double MAX_SCALE = 30d;
    private static final double MIN_SCALE = .05d;

    private DragContextJavaFx zoomDragContext = new DragContextJavaFx();

    TileViewJavaFx pane;

    public PaneZoomGestures(TileViewJavaFx pane) {
        this.pane = pane;
    }
    public void handleEvent(){
        pane.setOnMouseClicked(e -> handlePan(e));
        pane.setOnMouseDragged(d -> handleDrag(d));
        pane.setOnScroll(s -> handleScroll(s));

    }

    /**basically the handler for when you pan with the mouse secondary button*/
    public void handlePan(MouseEvent e){
        System.out.println("cursor should be closed hand");
        zoomDragContext.mouseAnchorX = e.getSceneX();
        zoomDragContext.mouseAnchorY = e.getSceneY();
        zoomDragContext.translateAnchorX = pane.getTranslateX();
        zoomDragContext.translateAnchorY = pane.getTranslateY();

    }

    /**Handles dragging of mouse when right mouse button is done*/
    public void handleDrag(MouseEvent d) {
        pane.setCursor(Cursor.CLOSED_HAND);
        System.out.println("dragging detected");
        //if you let go of right mouse button then return;
        if (!d.isSecondaryButtonDown()){
            pane.setCursor(Cursor.DEFAULT);
            System.out.println("second button no longer pressed. Cursor should be default");
            return;
        }
        System.out.println("translated coordinates set");
        pane.setTranslateX(zoomDragContext.translateAnchorX + d.getSceneX() - zoomDragContext.mouseAnchorX);
        pane.setTranslateY(zoomDragContext.translateAnchorY + d.getSceneY() - zoomDragContext.mouseAnchorY);
    }

    /**Basically the handler for when you scroll to zoom*/
    public void handleScroll(ScrollEvent s){
        System.out.println("scroll detected!");
        double delta = 1.2;

        double scale = pane.getScale(); // currently we only use Y, same value is used for X
        double oldScale = scale;

        if (s.getDeltaY() < 0)
            scale /= delta;
        else
            scale *= delta;

        scale = clamp( scale, MIN_SCALE, MAX_SCALE);

        double f = (scale / oldScale)-1;

        double dx = (s.getSceneX() - (pane.getBoundsInParent().getWidth()/2 + pane.getBoundsInParent().getMinX()));
        double dy = (s.getSceneY() - (pane.getBoundsInParent().getHeight()/2 + pane.getBoundsInParent().getMinY()));

        pane.setScale( scale);

        // note: pivot value must be untransformed, i. e. without scaling
        pane.setPivot(f*dx, f*dy);
    }



    public static double clamp(double value, double min, double max) {

        if (Double.compare(value, min) < 0)
            return min;

        if (Double.compare(value, max) > 0)
            return max;

        return value;
    }
}