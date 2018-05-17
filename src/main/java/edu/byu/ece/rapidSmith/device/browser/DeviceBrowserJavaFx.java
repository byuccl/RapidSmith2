package edu.byu.ece.rapidSmith.device.browser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import com.trolltech.qt.core.QModelIndex;
import com.trolltech.qt.core.Qt.DockWidgetArea;
import com.trolltech.qt.core.Qt.ItemDataRole;
import com.trolltech.qt.core.Qt.SortOrder;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QDockWidget;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QStatusBar;
import com.trolltech.qt.gui.QTreeWidget;
import com.trolltech.qt.gui.QTreeWidgetItem;
import com.trolltech.qt.gui.QWidget;
import com.trolltech.qt.gui.QDockWidget.DockWidgetFeature;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.gui.TileView;
import edu.byu.ece.rapidSmith.gui.WidgetMaker;

import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.gui.TileView;
import edu.byu.ece.rapidSmith.gui.WidgetMaker;


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
    protected TileView view;
    /** The javafx Scene for the browser */
    private DeviceBrowserScene scene;

    BorderPane borderPane = new BorderPane();//becomes new root node

    /**
     * The label for the status bar at the bottom
     */
    private Label statusLabel;
    private Label selectPart = new Label("Select a part...");//Labels are useful for displaying text that is required to fit within specific area
    private Label siteListName = new Label ("Site List");
    private Label wireListName = new Label("Wire List");

    /**
     * The Stackpane for the side bar
     */
    //private StackPane browserPane = new StackPane();
    private VBox browserPane = new VBox();

    /**
     * The current device loaded
     */
    private Device device;//RS2 class for the device

    private WireEnumerator we;//""

    List<String> parts;

    /**
     * The current part name of the device loaded
     */
    private String currPart;

    /**
     * This is the tree of parts to select
     */
    //private QTreeWidget treeWidget;
    private TreeView<String> partTree;

    /**
     * This is the list of sites in the current tile selected
     */
    //private QTreeWidget siteList;
    private TableView siteTable;
    /**
     * This is the list of wires in the current tile selected
     */
    //private QTreeWidget wireList;
    private TableView wireTable;

    /**
     * A constantly updated list of sites for current tile
     */
    private ObservableList<Site> siteList;
    /**
     * This is the current tile that has been selected
     */
    private Tile currTile = null;

    private boolean hideTiles = false;

    private boolean drawSites = true;

    /**
     * Main method setting up the Qt environment for the program to run.
     *
     * @param args the input arguments
     */
//    public static void main(String[] args) {
//        QApplication.setGraphicsSystem("raster");
//        QApplication.initialize(args);
//        DeviceBrowser testPTB = new DeviceBrowser(null);
//        testPTB.show();
//        QApplication.exec();
//
//        //launch(args);
//    }

    /**
     * Constructor which initializes the GUI and loads the first part found.
     *
     * @param parent The Parent widget, used to add this window into other GUIs.
     */

//
//        // Setup the scene and view for the GUI
//        scene = new DeviceBrowserScene(device, hideTiles, drawSites, this);
//        view = new TileView(scene);
//        setCentralWidget(view);
//
//        // Setup some signals for when the user interacts with the view
//        scene.updateStatus.connect(this, "updateStatus(String, Tile)");
//        scene.updateTile.connect(this, "updateTile(Tile)");
//
//        // Initialize the status bar at the bottom
//        statusLabel = new QLabel("Status Bar");
//        statusLabel.setText("Status Bar");
//        QStatusBar statusBar = new QStatusBar();
//        statusBar.addWidget(statusLabel);
//        setStatusBar(statusBar);
//
//        // Set the opening default window size to 1024x768 pixels
//        resize(1024, 768);
//    }
    /**make Branch method creates a branch for the treeView
     */
    public TreeItem<String> makeBranch(String title, TreeItem<String> parent){
        TreeItem<String> item = new TreeItem<>(title);
        item.setExpanded(true);
        parent.getChildren().add(item);
        return item;
    }
    /**
     * Populates the treeWidget with the various parts and families of devices
     * currently available in this installation of RapidSmith.  It also creates
     * the windows for the site list and wire list.
     */
    private void initializeSideBar() {

        partTree = createAvailablePartTree();//uses method call to create tree out of all parts their family types
        siteTable = createSiteTable();
        wireTable = createWireTable();
        partTree.getSelectionModel().selectedItemProperty()
                .addListener((v, oldValue, newValue) -> {
                    if(newValue != null){//and check that item is a device and not just family name

                        System.out.println(newValue+" Loaded");
                    }
                    //load partName
                });
        partTree.setOnMouseClicked(e -> {
            if(e.getButton().equals(MouseButton.PRIMARY)){
                if(e.getClickCount() == 2){//and value is not familyName
                    //Load device to tile view
                    System.out.println("Double clicked");
                }
            }
        });

          browserPane.getChildren().addAll(selectPart,partTree, siteListName, siteTable, wireListName, wireTable);
          borderPane.setLeft(browserPane);
    }

    public static TreeView<String> createAvailablePartTree(){//creates parts Tree View
        TreeView<String> partTreeView = new TreeView<>();
        partTreeView.setShowRoot(false);
        TreeItem<String> root = new TreeItem<>();
        partTreeView.setRoot(root);
        root.setExpanded(true);
        HashMap<FamilyType, TreeItem<String>> familyItems = new HashMap<>();//this hashMap helps check to see if you already have family in tree
        RSEnvironment env = RSEnvironment.defaultEnv();//get all part names from environment directory
        for(String partName : env.getAvailableParts()){//get individual part name for creating ViewTree
            FamilyType type = env.getFamilyTypeFromPart(partName);//get the FamilyType of current part
            TreeItem<String> familyItem = familyItems.get(type);//create treeItem from that family type
            System.out.println("Created familyItem:"+ type.name());

            if(familyItem == null){//check to see if family type does not exist
                familyItem = new TreeItem<>(type.name());
                root.getChildren().add(familyItem);//add familyItem to the root of the tree as one of main branches in TreeView
                familyItems.put(type, familyItem);
                System.out.println("familyItem was null(should happen twice");
            }
            TreeItem<String> partItem = new TreeItem(partName);//else add TreeItem as a leaf Node to the familyItem branch
            //need to get the familyItem from the HashTable else I will attach partName to a new instance of that family branch
            familyItem.getChildren().add(partItem);//add partItem to that family's branch
        }
        return partTreeView; //return completed TreeView object
}

    public static TableView createSiteTable(){
        TableView siteTable = new TableView();
        TableColumn<Site, String> siteCol = new TableColumn("Site");
        TableColumn<Site, String> typeCol = new TableColumn("Type");
        siteTable.getColumns().addAll(siteCol,typeCol);
        return siteTable;
    }

    public static TableView createWireTable(){
        TableView wireTable = new TableView();
        TableColumn<Wire, String> wireCol = new TableColumn("Wire");
        TableColumn<Wire, String> sinkCol = new TableColumn("Sink Connections");
        wireTable.getColumns().add(wireCol);
        wireTable.getColumns().add(sinkCol);
        return wireTable;
    }

    /**
     * This method creates a menu bar across the top of the device browser with a file and help section.
     */
    private void initializeMenuBar(){
        MenuBar menu = new MenuBar();
        Menu menuFile = new Menu("File");
        Menu menuHelp = new Menu("Help");
        menu.getMenus().addAll(menuFile, menuHelp);
        borderPane.setTop(menu);


    }
    /**
     * This method will draw all of the wire connections based on the wire given.
     *
     * @param index The index of the wire in the wire list.
     */
//    protected void wireDoubleClicked(QModelIndex index) {
//        scene.clearCurrentLines();
//        if (currTile == null) return;
//        if (index.column() != 0) return;
//        int currWire = we.getWireEnum(index.data().toString());
//        if (currWire < 0) return;
//        if (currTile.getWireConnections(we.getWireEnum(index.data().toString())) == null) return;
//        for (WireConnection wire : currTile.getWireConnections(we.getWireEnum(index.data().toString()))) {
//            scene.drawWire(currTile, currWire, wire.getTile(currTile), wire.getWire());
//        }
//    }

    /**
     * This method gets called each time a user double clicks on a tile.
     *
     * @param tile the tile to update
     */
    protected void updateTile(Tile tile) {
        currTile = tile;
        updateSiteList();
       // updateWireList();
    }

    /**
     * This will update the site list window based on the current
     * selected tile.
//     */
    private void updateSiteList() {
        siteTable.getItems().clear();
        if (currTile == null || currTile.getSites() == null) return;
        siteList.addAll(currTile.getSites());
        siteTable.getVisibleLeafColumn(0).setCellValueFactory(new PropertyValueFactory("name"));
        siteTable.setItems(siteList);
//        for (Site ps : currTile.getSites()) {
//            //TreeItem<String> treeItem = new TreeItem<>();
//            QTreeWidgetItem treeItem = new QTreeWidgetItem();
//            treeItem.setText(0, ps.getName());
//            treeItem.setText(1, ps.getType().toString());
//            siteList.insertTopLevelItem(0, treeItem);
//        }
    }

    /**
     * This will update the wire list window based on the current
     * selected tile.
     */
//    private void updateWireList() {
//        wireList.clear();
//        if (currTile == null || currTile.getWireHashMap() == null) return;
//        for (Integer wire : currTile.getWireHashMap().keySet()) {
//            QTreeWidgetItem treeItem = new QTreeWidgetItem();
//            treeItem.setText(0, we.getWireName(wire));
//            WireConnection[] connections = currTile.getWireConnections(wire);
//            treeItem.setText(1, String.format("%3d", connections == null ? 0 : connections.length));
//            wireList.insertTopLevelItem(0, treeItem);
//        }
//        wireList.sortByColumn(0, SortOrder.AscendingOrder);
//    }

    /**
     * This method loads a new device based on the part name selected in the
     * treeWidget.
     *
     * @param qmIndex The index of the part to load.
     */
//    protected void showPart(QModelIndex qmIndex) {
//        Object data = qmIndex.data(ItemDataRole.AccessibleDescriptionRole);
//        if (data != null) {
//            if (currPart.equals(data))
//                return;
//            currPart = (String) data;
//            device = RSEnvironment.defaultEnv().getDevice(currPart);
//            we = device.getWireEnumerator();
//            scene.setDevice(device);
//            scene.initializeScene(hideTiles, drawSites);
//            statusLabel.setText("Loaded: " + currPart.toUpperCase());
//        }
//    }

    /**
     * This method updates the status bar each time the mouse moves from a
     * different tile.
     *
     * @param text the new text for the status
     * @param tile unused
     */
    protected void updateStatus(String text, Tile tile) {
        statusLabel.setText(text);
        currTile = tile;
        System.out.println("currTile=" + tile);
    }
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        Circle cir = new Circle(200, 200, 100);
        cir.setFill(Color.RED);
        // Gets the available parts in RapidSmith and populates the selection tree
        parts = RSEnvironment.defaultEnv().getAvailableParts();

        if (parts.size() < 1) {
            System.err.println("No available parts.  Please generate part database files.");
            System.exit(1);
        }
        currPart = parts.get(0);
        device = RSEnvironment.defaultEnv().getDevice(currPart);
        we = device.getWireEnumerator();
//        Device testDevice= new Device();
//        for(String part: parts) {
//            System.out.println("current Part:");
//            System.out.println(part);
//            System.out.println("The Family Type is:");
//            testDevice = RSEnvironment.defaultEnv().getDevice(part);
//            System.out.println(testDevice.getFamily().toString());
//        }
        initializeSideBar();
        initializeMenuBar();
        //initializeTileView();

        Scene scene = new Scene(borderPane, 1000, 600);
        primaryStage.setTitle("Device Browser Testing");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

}
