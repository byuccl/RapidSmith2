package edu.byu.ece.rapidSmith.interfaces;

import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.ImplementationMode;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.util.Exceptions;

import java.util.regex.Pattern;

public abstract class AbstractXdcInterface {

    protected final Device device;
    protected final CellDesign design;
    protected final WireEnumerator wireEnumerator;
    protected int currentLineNumber;
    protected String currentFile;
    protected ImplementationMode implementationMode;
    protected Pattern pipNamePattern;

    public AbstractXdcInterface(Device device, CellDesign design) {
        this.device = device;
        this.design = design;
        this.wireEnumerator = device.getWireEnumerator();
        this.currentLineNumber = 0;
        this.implementationMode = design.getImplementationMode();
        this.pipNamePattern = Pattern.compile("(.*)/.*\\.([^<]*)((?:<<)?->>?)(.*)");
    }

    /**
     * Tries to retrieve a BelPin object from the currently loaded device <br>
     * If the pin does not exist, a ParseException is thrown. <br>
     *
     * @param bel Bel which the pin is attached
     * @param pinName Name of the bel pin
     * @return BelPin
     */
    protected BelPin tryGetBelPin(Bel bel, String pinName) {
        BelPin pin = bel.getBelPin(pinName);

        if (pin == null) {
            throw new Exceptions.ParseException(String.format("BelPin: \"%s/%s\" does not exist in the current device"
                    + "On line %d of %s", bel.getName(), pinName, currentLineNumber, currentFile));
        }
        return pin;
    }

    /**
     * Tries to retrieve the Tile object with the given name from the currently
     * loaded device. If no such tile exists, a {@link Exceptions.ParseException} is thrown.
     *
     * @param tileName Name of the tile to get a handle of
     * @return {@link Tile} object
     */
    protected Tile tryGetTile(String tileName) {
        Tile tile = device.getTile(tileName);

        // TODO: Check that the node is exactly the right one. ie make sure the true tile name matches as well.
        if (tile == null && implementationMode == ImplementationMode.RECONFIG_MODULE) {
            // Assume the tile is outside the partial device boundaries.
            tile = device.getTile("OOC_WIRE_X0Y0");
        }

        if (tile == null) {
            throw new Exceptions.ParseException("Tile \"" + tileName + "\" not found in device " + device.getPartName() + ". \n"
                    + "On line " + this.currentLineNumber + " of " + currentFile);
        }
        return tile;
    }

    /**
     * Tries to retrieve the integer enumeration of a wire name in the currently loaded device <br>
     * If the wire does not exist, a ParseException is thrown <br>
     */
    protected int tryGetWireEnum(String wireName) {

        Integer wireEnum = wireEnumerator.getWireEnum(wireName);

        if (wireEnum == null) {
            throw new Exceptions.ParseException(String.format("Wire: \"%s\" does not exist in the current device. \n"
                    + "On line %d of %s", wireName, currentLineNumber, currentFile));
        }

        return wireEnum;
    }

    /**
     * Tries to retrieve the Site object with the given site name
     * from the currently loaded device. If the site does not exist
     * a ParseException is thrown
     *
     * @param siteName Name of the site to retrieve
     */
    protected Site tryGetSite(String siteName) {

        Site site = device.getSite(siteName);

        if (site == null) {
            throw new Exceptions.ParseException("Site \"" + siteName + "\" not found in the current device. \n"
                    + "On line " + this.currentLineNumber + " of " + currentFile);
        }

        return site;
    }

    /**
     * Tries to retrieve a BEL object from the currently loaded device. <br>
     * If the BEL does not exist, a ParseException is thrown. <br>
     *
     * @param site Site where the BEL resides
     * @param belName Name of the BEL within the site
     * @return Bel
     */
    protected Bel tryGetBel(Site site, String belName) {

        Bel bel = site.getBel(belName);

        if (bel == null) {
            throw new Exceptions.ParseException(String.format("Bel: \"%s/%s\" does not exist in the current device"
                    + "On line %d of %s", site.getName(), belName, currentLineNumber, currentFile));
        }

        return bel;
    }

    /**
     * Tries to retrieve the Cell object with the given name
     * from the currently loaded design. If the cell does not exist,
     * a ParseException is thrown
     *
     * @param cellName Name of the cell to retrieve
     */
    protected Cell tryGetCell(String cellName) {

        Cell cell = design.getCell(cellName);

        if (cell == null) {
            throw new Exceptions.ParseException("Cell \"" + cellName + "\" not found in the current design. \n"
                    + "On line " + this.currentLineNumber + " of " + currentFile);
        }

        return cell;
    }

}
