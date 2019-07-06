package edu.byu.ece.rapidSmith.interfaces.xray;

import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.ImplementationMode;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.device.families.FamilyInfo;
import edu.byu.ece.rapidSmith.device.families.FamilyInfos;
import edu.byu.ece.rapidSmith.util.Exceptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractFasmInterface {
    protected final Device device;
    protected final CellDesign design;
    private final FamilyInfo familyInfo;
    private final ImplementationMode implementationMode;

    AbstractFasmInterface(Device device, CellDesign design) {
        this.device = device;
        this.design = design;
        familyInfo = FamilyInfos.get(device.getFamily());
        implementationMode = design.getImplementationMode();
    }

    /**
     * Gets the relative slice name for a slice site.
     * Slices with an even x coordinate are X0, while slices with an odd x coordinate are X1.
     * @param site the slice to get the relative name for.
     * @return the relative slice name
     */
    String getRelativeSliceName(Site site) {
        if (!familyInfo.sliceSites().contains(site.getType())) {
            throw new Exceptions.ImportExportException("Cannot get relative slice name for non-slice site type " + site.getType().name());
        }

        String suffix = ((site.getInstanceX() & 1) == 0) ? "_X0" : "_X1";
        return site.getType().name() + suffix;
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
            throw new Exceptions.ImportExportException("Tile " + tileName + " not found in device " + device.getPartName() + ".\n");
        }
        return tile;
    }

    /**
     * Returns whether or not a PIP is a Pseudo PIP of an interconnect tile.
     * TODO: Incorporate pseudo PIP information into the RS2 device representation to avoid pattern matching.
     * @param source the start wire of the PIP
     * @param sink the end wire of the PIP
     * @return whether the PIP is an interconnect pseudo PIP
     */
    protected boolean isIntPseudoPip(Wire source, Wire sink) {
        Pattern byp_sink = Pattern.compile("(BYP_BOUNCE|BYP|BYP_L)(\\d+)");
        Pattern byp_alt = Pattern.compile("BYP_ALT(\\d+)");

        Pattern fan_sink = Pattern.compile("(FAN|FAN_L|FAN_BOUNCE)(\\d+)");
        Pattern fan_alt = Pattern.compile("FAN_ALT(\\d+)");

        Pattern gclk_sink = Pattern.compile("(GCLK_B|GCLK_L_B)(\\d+)_(WEST|EAST)");
        Pattern gclk_source = Pattern.compile("(GCLK_B|GCLK_L_B)(\\d+)");

        Matcher sinkMatcher = byp_sink.matcher(sink.getName());
        Matcher sourceMatcher = byp_alt.matcher(source.getName());
        if (sinkMatcher.matches() && sourceMatcher.matches() && sinkMatcher.group(2).equals(sourceMatcher.group(1))) {
            return true;
        }

        sinkMatcher = fan_sink.matcher(sink.getName());
        sourceMatcher = fan_alt.matcher(source.getName());
        if (sinkMatcher.matches() && sourceMatcher.matches() && sinkMatcher.group(2).equals(sourceMatcher.group(1))) {
            return true;
        }

        // All INT_L/INT_R default pseudo-PIPs connect to VCC_WIRE.
        // By default, VCC_WIRE drives these PIPs if no other driver is configured.
        if (source.getName().equals("VCC_WIRE"))
            return true;

        sinkMatcher = gclk_sink.matcher(sink.getName());
        sourceMatcher = gclk_source.matcher(source.getName());
        return sinkMatcher.matches() && sourceMatcher.matches() && sinkMatcher.group(2).equals(sourceMatcher.group(2));
    }

    /**
     * Checks if a PIP (startWire->endWire) is a pseudo PIP.
     * @param startWire start wire of a PIP
     * @param endWire end wire of a PIP
     * @return whether the PIP is a psuedo PIP
     */
    protected boolean isPseudoPip(Wire startWire, Wire endWire) {
        // All PIPs in CLB tiles are "always-on" pseudo PIPs
        if (startWire.getTile().getType().toString().contains("CLB")) {
            return true;
        }

        // Check if it is an INT pseudo PIP
        return isIntPseudoPip(startWire, endWire);
    }

}
