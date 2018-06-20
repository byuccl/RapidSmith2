package edu.byu.ece.rapidSmith.device.browser;

import java.util.*;

//import edu.byu.ece.rapidSmith.device.browser.PanZoomGestures;
import com.sun.javafx.geom.Line2D;
import edu.byu.ece.rapidSmith.gui.wireItemJavaFx;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.gui.TileView;


/**
 * This class creates an interactive Xilinx FPGA device browser for all of the
 * devices currently installed on RapidSmith.  It provides the user with a 2D view
 * of all tile array in the device.  Allows each tile to be selected (double click)
 * and populate the site and wire lists.  Wire connections can also be drawn
 * by selecting a specific wire in the tile (from the list) and the program will draw
 * all connections that can be made from that wire.  The wire positions on the tile
 * are determined by a hash and are not related to FPGA Editor positions. This is based off
 * the old gui made by Chris Lavin and Mark Padilla except javafx was used to make compatable
 * with 64 bit Java and modern operating systems.
 * @author Jesse Grigg
 * Created on: May 14,2018
 */
public class DeviceBrowserJavaFx extends Application{//QMainWindow {
    /** The javafx Scene for the browser */
    private static Scene scene;
    /** Parent Template Node of most of the application */
    private static BorderPane borderPane = new BorderPane();//becomes new root node
    /**List of available parts*/
    private static List<String> parts;
    /** The Labels for the status bar at the bottom and side bar windows*/
    private static Label statusLabel = new Label();
    private static Label selectPart = new Label("Select a part...");//Labels are useful for displaying text that is required to fit within specific area
    private static Label siteListName = new Label ("Site List");
    private static Label wireListName = new Label("Wire List");

    /** The Stackpane for the side bar (parent node for all side bar objects) */
    private static VBox sideBarPane = new VBox();
    private static VBox bottomPane = new VBox();

    /** Can set Device Browser to not show tiles or sites with these booleans*/
    private boolean hideTiles = false;
    private boolean drawSites = true;
    /** The current device loaded */
    private Device device;

    private WireEnumerator we;
    /**TileWindowJavaFx object is basically a javafx canvas with all of the visuals(such as tiles, wires, etc) drawn on it*/
    private TileWindowJavaFx tileWindow;
    private TileViewTest tileViewTest;

    /** The current part name of the device loaded*/
    private String currPart;
    /**Helps in creating part TreeView and organizing parts by families*/
    private static HashMap<TreeItem<String>, FamilyType> familyItems = new HashMap<>();//this hashMap helps check to see if you already have family in tree

    /**This is the tree of parts to select*/
    private TreeView<String> partTree;

    /**This is the list of sites in the current tile selected*/
    private TableView<Site> siteTable;
    TableColumn<Site, String> siteCol;
    TableColumn<Site, String> typeCol;

    TableColumn<Wire, String> wireCol;
    TableColumn<Wire, String> sinkCol;
    /**Displays the wires for current tile after a tile is double clicked. Child node of sideBarPane*/
    private TableView wireTable;

    /**A constantly updated list of sites for current tile*/
    private ArrayList<Site> sites = new ArrayList<>();//will change as needed
    private ObservableList<Site> siteList;
    /**A constantly updated list of Wires/Connections for current tile*/
    private ArrayList<Wire> actualWires = new ArrayList<>();
    private ArrayList<wireItemJavaFx> wires = new ArrayList<>();//will change as needed
    private ObservableList<wireItemJavaFx> wireList;
    /**An object easier to display in the wireTable since it has private variables for name and number of connections*/
    private wireItemJavaFx displayWire;
    /**This is the current tile that has been selected*/
    private Tile currTile = null;

    private Tile showWiresTile = null;

    private static double sceneX;
    private static double sceneY;
//    private Scale scaleTransform;

    private TileViewJavaFx tileView;
    PanZoomGestures panWindow;


    /** Current center of this view */
    Point2D currCenter;
    /** Stores the last pan of the view */
    Point2D lastPan;
    /** A flag indicating if the right mouse button has been pressed */
    private boolean rightPressed;

    public boolean hasPanned;
    /** The maximum value to which we can zoom out */
    protected static double zoomMin = 0.05;
    /** The maximum value to which we can zoom in */
    protected static double zoomMax = 30;
    /** The rate at which we zoom */
    protected static double scaleFactor = 1.15;

    /**This method allows for easy expansion of a TreeView object. Mostly for testing purposes*/
    public TreeItem<String> makeBranch(String title, TreeItem<String> parent){
        TreeItem<String> item = new TreeItem<>(title);
        item.setExpanded(true);
        parent.getChildren().add(item);
        return item;
    }
    /**
     * Creates the side bar consisting of a TreeView of Device parts, siteList table, and wireList table
     * Allows resizing of each sidebar Window using a WindowResizer class. Cannot resize windows smaller than default size.
     */
    private void initializeSideBar() {
        partTree = createAvailablePartTree();//uses method call to create tree out of all parts their family types
        siteTable = createSiteTable();
        wireTable = createWireTable();
            partTree.setPrefHeight(100);
            siteTable.setPrefHeight(150);
            wireTable.setPrefHeight(400);
            //Make partTree height resizeable
            WindowResizer resizable = new WindowResizer(partTree);
            resizable.makeResizable(partTree);
            WindowResizer resizable1 = new WindowResizer(siteTable);
            resizable1.makeResizable(siteTable);
            WindowResizer resizable2 = new WindowResizer(wireTable);
            resizable2.makeResizable(wireTable);
            sideBarPane.getChildren().addAll(selectPart,partTree, siteListName, siteTable, wireListName, wireTable);
            borderPane.setLeft(sideBarPane);
    }

    /**
     * This method creates a TreeView so user can select the FPGA device to display
     * @return the new PartTree for viewing in side bar
     */
    public TreeView<String> createAvailablePartTree(){//creates parts Tree View
        TreeView<String> partTreeView = new TreeView<>();
        partTreeView.setShowRoot(false);
        TreeItem<String> root = new TreeItem<>();
        partTreeView.setRoot(root);
        root.setExpanded(true);
        //HashMap<FamilyType, TreeItem<String>> familyItems = new HashMap<>();//this hashMap helps check to see if you already have family in tree
        RSEnvironment env = RSEnvironment.defaultEnv();//get all part names from environment directory
        for(String partName : env.getAvailableParts()){//get individual part name for creating ViewTree
            FamilyType type = env.getFamilyTypeFromPart(partName);//get the FamilyType of current part
            TreeItem<String> familyItem = new TreeItem(type.name());
            FamilyType test = familyItems.get(familyItem);
            if(test == null){//check to see if family type does not exist
                root.getChildren().add(familyItem);//add familyItem to the root of the tree as one of main branches in TreeView
                familyItems.put(familyItem, type);
                //System.out.println("familyItem: "+familyItem.getValue()+" was added as a family branch and to HashMap"); //for testing purposes
            }
            TreeItem<String> partItem = new TreeItem(partName);//else add TreeItem as a leaf Node to the familyItem branch
            //need to get the familyItem from the HashTable else I will attach partName to a new instance of that family branch
            familyItem.getChildren().add(partItem);//add partItem to that family's branch
        }
//        partTreeView.setOnMouseClicked(e -> {
//            if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2) {//If double clicked with primary button
//                TreeItem<String> loadPart = partTreeView.getSelectionModel().getSelectedItem();//grab TreeItem from TreeView
//                // assume it is a device and not a family.
//                if (familyItems.get(loadPart)==null) {
//                    showPart(loadPart.getValue());
//                }
//            }
//        });
        return partTreeView; //return completed TreeView object
}

    public TableView createSiteTable(){
        siteCol = new TableColumn<>("Site");//Create first column
        siteCol.setMinWidth(150);
        siteCol.setCellValueFactory(new PropertyValueFactory<>("name"));//set values of column to be the name of each site

        typeCol = new TableColumn<>("Type");
        typeCol.setMinWidth(150);
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        siteTable = new TableView();//create table for Sites info
        siteTable.getColumns().addAll(siteCol, typeCol);
        return siteTable;
    }

    public TableView createWireTable(){
        wireCol = new TableColumn<>("Wire");
        wireCol.setMinWidth(150);
        wireCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        sinkCol = new TableColumn("Sink Connections");
        sinkCol.setMinWidth(150);
        sinkCol.setCellValueFactory(new PropertyValueFactory<>("connections"));
        TableView wireTable = new TableView();
        wireTable.getColumns().addAll(wireCol, sinkCol);
        wireTable.setRowFactory(w -> {
            TableRow<wireItemJavaFx> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if(e.getClickCount() == 2 && (!row.isEmpty())){
                    displayWire = row.getItem();
                    showWiresTile = currTile;
                    wireDoubleClicked(displayWire.getName());
                    System.out.println("draw "+displayWire.getName()+" connections");
                }
            });
            return row;
        });
        wireTable.getSortOrder().add(wireCol);
        return wireTable;
    }

    /**
     * This method creates a menu bar across the top of the device browser with a file and help section.
     */
    private void initializeMenuBar() {
        MenuBar menu = new MenuBar();
        Menu menuFile = new Menu("File");
        Menu menuHelp = new Menu("Help");
        menu.getMenus().addAll(menuFile, menuHelp);
        borderPane.setTop(menu);
    }

    /**
     * This method will draw all of the wire connections based on the wire given.
     */
    protected void wireDoubleClicked(String wireName){
        tileViewTest.resetAfterWire();//erase and redraw
        if(showWiresTile == null) return;
        TileWire tileWire = showWiresTile.getWire(wireName);
        if(tileWire == null) return;
        if(tileWire.getWireConnections().isEmpty()) return;
        for(Connection wire : tileWire.getWireConnections()){
            System.out.println("should move to draw method");
            tileViewTest.drawWire(showWiresTile, tileWire, wire.getSinkWire().getTile(), wire.getSinkWire());
//            tileView.drawWireLines(showWiresTile, tileWire, wire.getSinkWire().getTile(), wire.getSinkWire());
        }
    }

    public wireItemJavaFx getDisplayWire(){return displayWire;}

    /**
     * This method gets called each time a user double clicks on a tile.
     *
     * @param tile the tile to update
     */
    protected void updateSelectedTile(Tile tile) {
        currTile = tile;
        updateSiteList();//update Site table
        updateWireList();//update Wire Table
    }

    /**
     * This will update the site list window based on the current
     * selected tile.
//     */
    private void updateSiteList() {
        sites.clear();//clear out sites from last selected tile
        if (currTile == null || currTile.getSites() == null) {
            siteList = FXCollections.observableList(sites);
            siteTable.setItems(siteList);
            return;//just in case
        }
         for(Site site : currTile.getSites()){//fills ArrayList with Sites from currently selected tile,
                sites.add(site);
         }
        siteList = FXCollections.observableList(sites);
        siteTable.setItems(siteList);
    }

    /**
     * This will update the wire list window based on the current
     * selected tile.
     */
    private void updateWireList() {
    //use wires and wiresList
        actualWires.clear();
        wires.clear();
        if (currTile == null || currTile.getWireHashMap() == null) return;
        for (Integer curWire : currTile.getWireHashMap().keySet()) {
              wireItemJavaFx wire = new wireItemJavaFx(we.getWireName(curWire), currTile.getWireConnections(curWire).length);
              wires.add(wire);
        }
//        actualWires.addAll(currTile.getW)
        wireList = FXCollections.observableList(wires);
        wireTable.setItems(wireList);
        wireTable.getSortOrder().add(wireCol);
    }

    /**
     * This method loads a new device based on the part name selected in the TreeView called partsList.
     * @param partName
     */
    protected void showPart(String partName, MouseEvent e) {//had QModelIndex qmIndex as a passed in argument before
            System.out.println("device:"+partName+" was loaded");//working but lists device twice
            currPart = partName;
            device = RSEnvironment.defaultEnv().getDevice(currPart);
            we = device.getWireEnumerator();
//            tileWindow = new TileWindowJavaFx(this, device, hideTiles, drawSites);
//            tileWindow.setDevice(device);
            tileViewTest.setDevice(device);
            statusLabel.setText("Loaded: " + currPart.toUpperCase());
//            tileWindow.initializeScene(hideTiles, drawSites);
            tileViewTest.getChildren().clear();
            tileViewTest.initializeScene(hideTiles, drawSites);
//            tileView.setTileWindow(tileWindow);
            panWindow.setTileView(tileViewTest);
            //System.out.println("value of tileView:"+tileView.toString());

    }

    /**
     * This method updates the status bar each time the mouse moves from tile to tile.
     * @param e The event of moving the mouse cursor
     */
    protected void updateStatus(MouseEvent e) {
//        statusLabel.setText(tileWindow.mouseMoveEvent(e));
        statusLabel.setText(tileViewTest.mouseMoveEvent(e));
    }

    /**If mouse cursor is hovering along a wire then highlight red*/
    protected void checkForWireHighlight(MouseEvent e){
        for(Line2D line : tileWindow.getLines()) {
            Line2D currLine = line;

        }
    }

    /**
     * Method for testing the handling of event of entering a node with the mouse cursor.
     * Prints out the coordinates of where your cursor entered the tileWindow object
     * @param e The event of moving the mouse cursor into tileWindow
     */
    public void enteredCanvas(MouseEvent e){//for testing purposes
        sceneX = e.getX();
        sceneY = e.getY();
        System.out.println("Mouse Entered canvas node at X:"+sceneX+" Y:"+sceneY);

    }

    /**
     * Method for testing the handling of event of entering a node with the mouse cursor.
     * Prints out the coordinates of where your cursor exited the tileWindow object
     * @param e The event of moving the mouse cursor out of tileWindow
     */
    public void exitedCanvas(MouseEvent e) {//for testing purposes
        sceneX = e.getX();
        sceneY = e.getY();
        System.out.println("Mouse Exited canvas node at X:" + sceneX + " Y:" + sceneY);
    }

    /**Standard main method. Initializes application*/
    public static void main(String[] args) {
        launch(args);
    }

    /**Initializes the GUI and loads the first part found.*/
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Gets the available parts in RapidSmith and populates the selection tree
        parts = RSEnvironment.defaultEnv().getAvailableParts();
        if (parts.size() < 1) {
            System.err.println("No available parts.  Please generate part database files.");//error message
            System.exit(1);
        }
        currPart = parts.get(0);
        initializeSideBar();
        initializeMenuBar();
        device = RSEnvironment.defaultEnv().getDevice(currPart);//default program to first device in partsList
        we = device.getWireEnumerator();
        tileWindow = new TileWindowJavaFx(this, device, hideTiles, drawSites);
        partTree.setOnMouseClicked(e -> {
            if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2) {//If double clicked with primary button
                TreeItem<String> loadPart = partTree.getSelectionModel().getSelectedItem();//grab TreeItem from TreeView
                // assume it is a device and not a family.
                if (familyItems.get(loadPart)==null) {
                    showPart(loadPart.getValue(), e);
                }
            }
        });

//        tileWindow.setOnMouseMoved(e -> {
//            updateStatus(e);
//            checkForWireHighlight(e);
//        });
//        tileWindow.setOnMouseClicked(e -> {//highlights tile selected by double click
//            if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2) {
//                tileWindow.mouseDoubleClickEvent(e);
//                updateSelectedTile(tileWindow.getSelectedTile());
//            }
//            else if(e.isSecondaryButtonDown() || (e.getClickCount() == 2 && e.getButton().equals(MouseButton.SECONDARY)))
//            {
//                tileWindow.rightClickEvent(e);
//                e.consume();
//            }
//        });
//        tileView = new TileViewJavaFx(tileWindow);

        /**New TileView model**************
         *
         *
         *
         * */
        tileViewTest = new TileViewTest(this, device, hideTiles, drawSites);
        tileViewTest.setOnMouseClicked(e -> {//highlights tile selected by double click
            if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2) {
                tileViewTest.mouseDoubleClickEvent(e);
                updateSelectedTile(tileViewTest.getSelectedTile());
            }
            else if(e.isSecondaryButtonDown() || (e.getClickCount() == 2 && e.getButton().equals(MouseButton.SECONDARY)))
            {
                tileViewTest.rightClickEvent(e);
                e.consume();
            }
        });
        ObservableList<Line> drawnWires = tileViewTest.getDrawnWires();
         tileViewTest.setOnMouseMoved(e -> {
            updateStatus(e);
            checkForWireHighlight(e);
         });
        panWindow = new PanZoomGestures(tileViewTest);



        bottomPane.getChildren().add(statusLabel);
//            borderPane.setCenter(tileViewTest);//change as needed to display either tileWindow or tileView
        borderPane.setCenter(panWindow);//change as needed to display either tileWindow or tileView
        borderPane.setBottom(bottomPane);
        scene = new Scene(borderPane, 1024, 768);
        primaryStage.setTitle("Device Browser Testing");
        primaryStage.setScene(scene);
        primaryStage.show();

    }//end Application
}
