package edu.byu.ece.rapidSmith.device.browser;

/**
 * {@link windowResizer} This is a height resizing utility to help with resizing the side bar window menus to view more information
 * @author JesseGrigg
 */
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

public class WindowResizer {
        /**
         * The margin around the control that a user can click in to start resizing
         * the region.
         */
        private static final int RESIZE_MARGIN = 5;

        private Region region;//container for node which needs resizing

        private double y;//y coordinate of region window

        private boolean initMinHeight;

        private boolean dragging;//flag that tells if mouse dragged after click

        public WindowResizer(Region aRegion) {
            region = aRegion;
        }

//    private WindowResizer(TreeView<String> partList) {
//        parts = partList;
//    }

        public void makeResizable(Region region) {
            final WindowResizer resizer = new WindowResizer(region);
            region.setOnMousePressed(e-> resizer.mousePressed(e));//if mouse clicked
            region.setOnMouseDragged(e -> resizer.mouseDragged(e));//and drag mouse
            region.setOnMouseMoved(e -> resizer.mouseOver(e));//set cursor to resize cursor and adjust height of window
            region.setOnMouseReleased(e -> resizer.mouseReleased(e));//mouse click released, stop adjusting and return cursor normals
        }

        protected void mouseReleased(MouseEvent event) {
            dragging = false;
            region.setCursor(Cursor.DEFAULT);
        }

        protected void mouseOver(MouseEvent event) {
            if(isInDraggableZone(event) || dragging) {
                region.setCursor(Cursor.S_RESIZE);
            }
            else {
                region.setCursor(Cursor.DEFAULT);
            }
        }

        protected boolean isInDraggableZone(MouseEvent event) {
            return event.getY() > (region.getHeight() - RESIZE_MARGIN);
        }



        protected void mouseDragged(MouseEvent event) {
            if(!dragging) {
                return;
            }

            double mousey = event.getY();

            double newHeight = region.getMinHeight() + (mousey - y);

            region.setMinHeight(newHeight);

            y = mousey;
        }

        protected void mousePressed(MouseEvent event) {

            // ignore clicks outside of the draggable margin
            if(!isInDraggableZone(event)) {
                return;
            }

            dragging = true;

            // make sure that the minimum height is set to the current height once,
            // setting a min height that is smaller than the current height will
            // have no effect
            if (!initMinHeight) {
                region.setMinHeight(region.getHeight());
                initMinHeight = true;
            }

            y = event.getY();
        }
}

