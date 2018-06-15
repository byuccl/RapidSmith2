package edu.byu.ece.rapidSmith.gui;

/**
 * This class was made so that the Device Browser could easily display the wire names and number
 * of connections when double clicked. TableView Objects in the JavaFx library are most easily
 * managed by observable lists of a single object type.
 */

public class wireItemJavaFx {
    int connections;
    String name;

    public wireItemJavaFx(String inName, int inConnection){
        name = inName;
        connections = inConnection;
    }
//    public void setConnections(int numberConnections){connections = numberConnections;}
//    public void setName(String inName){name = inName;}

    public  String getName(){return name;}
    public int getConnections(){return connections;}

}
