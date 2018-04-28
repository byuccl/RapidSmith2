package edu.byu.ece.rapidSmith.design.subsite;

public class PseudoCell extends Cell {


    /**
     * Creates a new cell with specified name and type.
     *
     * @param name    name of the new cell
     * @param libCell the library cell to base this cell on
     */
    public PseudoCell(String name, LibraryCell libCell) {
        super(name, libCell);
    }
}