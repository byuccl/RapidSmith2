package edu.byu.ece.rapidSmith.design.subsite;

/**
 * This class represents a PseudoCell. It is used to represent a cell that is
 * not logically represented, but is physically present. For example, this can
 * be used to represent static (VCC or GND) source LUTs in a design.
 *
 */
public class PseudoCell extends Cell {
    private static final long serialVersionUID = -878991201074735228L;

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