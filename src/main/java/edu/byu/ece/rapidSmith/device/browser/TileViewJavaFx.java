package edu.byu.ece.rapidSmith.device.browser;
import com.sun.javafx.collections.TrackableObservableList;
import com.sun.javafx.collections.VetoableListDecorator;
import com.trolltech.qt.core.QPoint;
import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.CursorShape;
import com.trolltech.qt.core.Qt.Key;
import com.trolltech.qt.gui.QCursor;
import com.trolltech.qt.gui.QGraphicsScene;
import com.trolltech.qt.gui.QGraphicsView;
import com.trolltech.qt.gui.QKeyEvent;
import com.trolltech.qt.gui.QMouseEvent;
import com.trolltech.qt.gui.QWheelEvent;

import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import edu.byu.ece.rapidSmith.gui.DragContextJavaFx;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;//handles button clicks etc.
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

import javafx.geometry.Point2D; //like QPoint and QPointF
import javafx.scene.control.Label;
import javafx.scene.Scene;//like QGraphicsScene?
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import static javafx.scene.transform.Transform.scale;


/**
 * This class is written specifically for the DeviceBrowser class and provides
 * the Zoomable View of the tileWindow.  It controls much of the interaction from the user
 * including pan and zoom upon pressing CTRL and scrolling with mouse.
 */

public class TileViewJavaFx extends ScrollPane {
//    StackPane windowPane;
//    ScrollPane scrollPane;
//    Group group;

    protected static double zoomMin = .05;
    /**
     * The maximum value to which we can zoom in
     */
    protected static double zoomMax = 5;
    /**
     * The rate at which we zoom
     */
    protected static double scaleFactor = 1.15;

    int lineWidth = 1;

    int offset = (int) Math.ceil((lineWidth / 2.0));

    TileWindowJavaFx tileWindow;

    Group group;

    Point2D initialMouse;

    DoubleProperty myScale = new SimpleDoubleProperty(1.0);
    StackPane content;

    ArrayList<Line> lines = new ArrayList<>();

    TileViewJavaFx(TileWindowJavaFx tileWindowIn) {
//        super();
        setup(tileWindowIn);



    }

    public void setup(TileWindowJavaFx in){
        tileWindow = in;
        group = new Group(tileWindow);
        content = new StackPane(group);
        tileWindow.layoutBoundsProperty().addListener((observable, oldBounds, newBounds) -> {
            // keep it at least as large as the content
            content.setMinWidth(newBounds.getWidth());
            content.setMinHeight(newBounds.getHeight());
        });

        setContent(content);//Scrollpane now has stackpane as contents
        setPannable(true);//takes care of panning capability

        content.setPrefSize(tileWindow.getWidth(), tileWindow.getHeight());//set preferred size to same as tileWindow
        content.setStyle("-fx-background-color: black;");//set background to black

        viewportBoundsProperty().addListener((observable, oldBounds, newBounds) -> {
            // use viewport size, if not too small for zoomTarget
            content.setPrefSize(newBounds.getWidth(), newBounds.getHeight());
        });

//          content.setOnScrollStarted(e-> {
//              initialMouse = new Point2D(e.getX(), e.getY());
//              content.setTranslateX((e.getX()));
//              content.setTranslateY(e.getY());
//          });

        content.setOnScroll(evt -> {//zoom on scroll...
            if (evt.isControlDown()) {//...but only if control key is pressed
                evt.consume();//consume control key press
                final double zoomFactor = evt.getDeltaY() > 0 ? 1.2 : 1 / 1.2;//basically the scale
//                  double delta = 1.2; //zoom factor
//                  double scale = getScale();
//                  double oldScale = scale;
//                  if (evt.getDeltaY() < 0) {
//                      System.out.println("scrolled down");
//                      scale /= delta;
//                  }
//                  else{
//                      System.out.println("scrolled up");
//                      scale *= delta;
//                  }
//                  //scale = clamp( scale, zoomMin, zoomMax);
//                  double f = (scale / oldScale)-1;
//                  double dx = (evt.getSceneX() - (getWidth()/2 + getMinX()));
//                  double dy = (evt.getSceneY() - (content.getBoundsInParent().getHeight()/2 + content.getBoundsInParent().getMinY()));
//                  setScale(scale);
//                  // note: pivot value must be untransformed, i. e. without scaling
//                  //setPivot(f*dx, f*dy);

                Bounds groupBounds = content.getLayoutBounds();
                final Bounds viewportBounds = getViewportBounds();

//                  // calculate pixel offsets from [0, 1] range
                double valX = getHvalue() * (groupBounds.getWidth() - viewportBounds.getWidth());
                double valY = getVvalue() * (groupBounds.getHeight() - viewportBounds.getHeight());
//
//                  // convert content coordinates to zoomTarget coordinates
                Point2D posInZoomTarget = content.parentToLocal(content.parentToLocal(new Point2D(evt.getX(), evt.getY())));
//
//                  // calculate adjustment of scroll position (pixels)
                Point2D adjustment = content.getLocalToParentTransform().deltaTransform(posInZoomTarget.multiply(zoomFactor - 1));
//
//                  // do the resizing of the tileWindow(canvas) itself by setting the scale of the "picture"
//                     to the current scale times the calculated zoom factor

                if (!(zoomFactor * tileWindow.getScaleX() > zoomMax || zoomFactor * tileWindow.getScaleX() < zoomMin)) {
                    setPivot(evt);
                    tileWindow.setScaleX(zoomFactor * tileWindow.getScaleX());
                    tileWindow.setScaleY(zoomFactor * tileWindow.getScaleY());
                    layout();

                    groupBounds = group.getLayoutBounds();
                    setHvalue((valX + adjustment.getX()) / (groupBounds.getWidth() - viewportBounds.getWidth()));
                    setVvalue((valY + adjustment.getY()) / (groupBounds.getHeight() - viewportBounds.getHeight()));
                }
            }
        });

    }
    public void setPivot(ScrollEvent e) {
        tileWindow.setTranslateX(e.getX());
        tileWindow.setTranslateY(e.getY());
    }

    public static double clamp( double value, double min, double max) {

        if( Double.compare(value, min) < 0)
            return min;

        if( Double.compare(value, max) > 0)
            return max;

        return value;
    }
    public double getScale() {
        return myScale.get();
    }

    public void setScale( double scale) {
        myScale.set(scale);
    }

    public void setTileWindow(TileWindowJavaFx in){
        setup(in);
    }

    public void drawWireLines(Tile src, Wire wireSrc, Tile dst, Wire wireDst) {
        WireEnumerator we = tileWindow.we;
        double enumSize = we.getWires().length;
        double offsetX1 = 1 / enumSize;
        double offsetX2 = 10 % enumSize;
        double offsetY = -2;
        HashMap tileXMap = tileWindow.tileXMap;
        HashMap tileYMap = tileWindow.tileYMap;
        int tileSize =  tileWindow.tileSize;
        try {
            double x1 = (double) ((Integer)tileXMap.get(src) * tileSize + (wireSrc.getWireEnum() % tileSize));
            double y1 = (double) ((Integer)tileYMap.get(src) * tileSize + (wireSrc.getWireEnum() * tileSize)) / enumSize;
            double x2 = (double) ((Integer)tileXMap.get(dst) * tileSize + (wireDst.getWireEnum() % tileSize));
            double y2 = (double) ((Integer)tileYMap.get(dst) * tileSize + (wireDst.getWireEnum() * tileSize)) / enumSize;
//            gc.setStroke(Color.ORANGE);
//            gc.setLineWidth(lineWidth);
//            gc.strokeLine(x1, y1, x2, y2);

            Line line = new Line(x1,y1,x2,y2);
            line.setStroke(Color.ORANGE);
            lines.add(line);
            group.getChildren().add(line);

        } catch (NullPointerException e) {
            //System.out.println("Error, new Device means need to click on tile");
            return;
        }
    }

    public void removeWires(){
        group.getChildren().remove(lines);
    }
}