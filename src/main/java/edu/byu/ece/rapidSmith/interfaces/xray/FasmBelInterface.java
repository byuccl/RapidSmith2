package edu.byu.ece.rapidSmith.interfaces.xray;

import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.util.Exceptions;
import edu.byu.ece.rapidSmith.util.luts.InitString;
import edu.byu.ece.rapidSmith.util.luts.LutEquation;
import edu.byu.ece.rapidSmith.util.luts.LutInput;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is used for writing FASM instructions for all used logic BELs of a design.
 *
 * @author Dallon Glick
 *
 */
public class FasmBelInterface extends AbstractFasmInterface{
    private final CellDesign design;
    private final BufferedWriter fileout;
    private final Map<Bel, BelRoutethrough> belRoutethroughMap;
    private final Set<Bel> routethroughBels;
    private final Set<Bel> gndSourceBels;
    private final Set<Bel> vccSourceBels;
    private Collection<Bel> usedBels;
    /** Map from sites to a set of shared properties */
    private Map<Site, Set<String>> sharedSiteProperties;

    public FasmBelInterface(Device device, CellDesign design, Map<Bel, BelRoutethrough> belRoutethroughMap, Set<Bel> vccSourceBels, Set<Bel> gndSourceBels, BufferedWriter fileout) {
        super(device, design);
        this.design = design;
        this.fileout = fileout;
        this.belRoutethroughMap = belRoutethroughMap;
        this.routethroughBels = belRoutethroughMap.keySet();
        this.gndSourceBels = gndSourceBels;
        this.vccSourceBels = vccSourceBels;
        this.sharedSiteProperties = new HashMap<>();
        usedBels = design.getUsedBels();
        usedBels.addAll(vccSourceBels);
        usedBels.addAll(gndSourceBels);
        usedBels.addAll(routethroughBels);
    }

    /**
     * @param binaryInitString the binary INIT string
     * @param fullBelName the name of the BEL, formatted for FASM
     * @param onlyTopHalf if true, only print top 32 bits
     * @throws IOException
     */
    private void printLutInitString(String binaryInitString, String fullBelName, boolean onlyTopHalf) throws IOException {
        for (int i = 0; i < binaryInitString.length(); i++) {
            int index = onlyTopHalf ? 32 + i : i;
            char c = binaryInitString.charAt(binaryInitString.length() - 1 - i);
            char oneChar = 49;
            if (c == oneChar) {
                fileout.write(fullBelName + "INIT" + "[" + String.format("%02d", index) + "] " + c + "\n");
            }
        }
    }

    /* LUT BELs */

    /**
     * Gets a LUT Cell's INIT string and properly formats it for Cell-Bel pin remapping.
     * @param lutCell the LUT cell
     * @return the LUT's INIT string
     */
    private String getLutCellInitString(Cell lutCell) {
        String initValString;

        if (!lutCell.getProperties().has("INIT")
                || lutCell.getProperties().get("INIT").getValue() == null) {
            System.err.println("WARNING: LUT Cell " + lutCell.getName() + " has no INIT property. " +
                    "No LUT Equation FASM instructions will be written for this LUT");
            return null;
        }

        Object initValue = lutCell.getProperties().get("INIT").getValue();

        // Figure out what type the initValue is
        if (initValue instanceof String) {
            // Assuming the string INIT property will be hex.
            // Vivado always uses hex strings for LUT init properties.

            // Strip off the leading 16'h..., 8'h..., etc.
            initValString = (String) initValue;
            initValString = initValString.substring(initValString.indexOf('h') + 1);
        } else if (initValue instanceof Long) {
            // Convert long to hex string
            initValString = Long.toHexString((Long) initValue);
        } else {
            throw new Exceptions.ImportExportException("LUT Cell " + lutCell.getName() + "'s INIT value \\" +
                    "is not a long int or a string.");
        }

        return "0x" + initValString;
    }

    /**
     * Creates and returns a mapping from a LUT's cell pins to its BEL pins.
     * The mapping is a map of the indices of the pins to the indices to change them to.
     * I0 = 1, I1 = 2, etc.
     * @param lutCell the LUT cell to create the mapping for.
     * @return the cell pin to bel pin mapping
     */
    private Map<Integer, Integer> getLutCellToBelPinMap(Cell lutCell) {
        // map of the indices of the pins to the indices to change them to
        Map<Integer, Integer> cellToBelPinMap = new HashMap<>();

        // Pseudo pins don't contribute to the equation and partition pins should never be on a LUT cell.
        Collection<CellPin> inputCellPins = lutCell.getInputPins().stream()
                .filter(cellPin -> !cellPin.isPseudoPin() && !cellPin.isPartitionPin())
                .collect(Collectors.toList());

        for (CellPin cellPin : inputCellPins) {
            // I0 = 1, I1 = 2, etc.
            Integer cellPinIndex = Integer.parseInt(cellPin.getName().substring(1)) + 1;
            Integer belPinIndex = Integer.parseInt(cellPin.getMappedBelPin().getName().substring(1));

            // add the cell to bel pin mapping
            if (!cellPinIndex.equals(belPinIndex))
                cellToBelPinMap.put(cellPinIndex, belPinIndex);
        }
        return cellToBelPinMap;
    }

    /**
     * Remaps a LUT Cell equation to a LUT BEL equation.
     * @param lutCell the LUT Cell
     * @param initValString the INIT string of the LUT Cell
     * @return the LUT BEL Equation
     */
    private LutEquation remapToBelLutEquation(Cell lutCell, String initValString) {
        LutEquation lutEquation;
        // Pseudo pins and partition pins don't contribute to the number of inputs
        int numInputs = (int) lutCell.getInputPins().stream()
                .filter(cellPin -> !cellPin.isPseudoPin() && !cellPin.isPartitionPin())
                .count();

        // Convert the LUT's init string into a minimized sum of products boolean expression
        InitString initString = InitString.parse(initValString, numInputs);
        lutEquation = LutEquation.convertToLutEquation(initString);

        // Use the index of pins to remap
        // Remap from I5, I4, I3, I2, I1, I0 to A6, A5, A4, A3, A2, A1
        Map<Integer, Integer> cellToBelPinMap = getLutCellToBelPinMap(lutCell);

        if (!cellToBelPinMap.isEmpty())
            lutEquation.remapPins(cellToBelPinMap);

        return lutEquation;
    }

    /**
     * Creates and returns a LutEquation for a route-through BEL.
     * @param rtBel the route-through BEL
     * @return A LutEquation for the route-through BEL.
     */
    private LutEquation getRoutethroughLutEquation(Bel rtBel) {
        assert (routethroughBels.contains(rtBel));
        BelRoutethrough belRoutethrough = belRoutethroughMap.get(rtBel);

        String belPinName = belRoutethrough.getInputPin().getName();
        int belPinIndex = Integer.parseInt(belPinName.substring(belPinName.length() - 1));
        return new LutInput(belPinIndex);
    }

    /**
     * Creates and returns a LutEquation for the given lut BEL.
     * @param lutBel the lut BEL to create a LutEquation for
     * @return the LutEquation
     */
    private LutEquation getLutEquation(Bel lutBel) {
        assert (!gndSourceBels.contains(lutBel));
        LutEquation lutEquation;
        if (vccSourceBels.contains(lutBel)) {
            lutEquation = LutEquation.parse("1");
        } else if (routethroughBels.contains(lutBel)) {
            lutEquation = getRoutethroughLutEquation(lutBel);
        }
        else {
            Cell lutCell = design.getCellAtBel(lutBel);
            String initValString = getLutCellInitString(lutCell);
            lutEquation = remapToBelLutEquation(lutCell, initValString);
        }

        return lutEquation;
    }

    /**
     * Write a FASM instruction for the LUT BEL.
     * @param lutBel the Bel to write the instruction for.
     * @throws IOException
     */
    private void processLutBel(Bel lutBel) throws IOException {
        if (gndSourceBels.contains(lutBel)) {
            // Nothing to do for gnd source BELs.
            return;
        }
        Cell lutCell = design.getCellAtBel(lutBel);

        // Get the properly formatted physical LUT Equation
        LutEquation lutEquation = getLutEquation(lutBel);

        if (lutEquation != null) {
            Site site = lutBel.getSite();
            String fullBelName = site.getTile().getName() + "."
                    + getRelativeSliceName(site) + "." + lutBel.getName().charAt(0) + "LUT.";

            // Figure out the correct bits to write instructions for.
            // If the bel is a 6LUT bel
            if (lutBel.getType().equals("LUT6") || lutBel.getType().equals("LUT_OR_MEM6")) {
                String lut5BelName = lutBel.getName().substring(0, 1) + "5LUT";
                // If the lut cell is a 6LUT or the 5BEL is unoccupied
                if (!usedBels.contains(site.getBel(lut5BelName)) || (lutCell != null && lutCell.getType().equals("6LUT"))) {
                    // Print 64 bits of the init equation
                    printLutInitString(getBinaryInitString(lutEquation, 6), fullBelName, false);
                } else {
                    // If the bel is a 6LUT bel and a the 5LUT bel IS being used, only print the top 32 bits
                    printLutInitString(getBinaryInitString(lutEquation, 5), fullBelName, true);
                }
            } else {
                // If the bel is a 5LUT bel, just print the bits for the lower 32 bits.
                printLutInitString(getBinaryInitString(lutEquation, 5), fullBelName, false);
            }
        }
    }

    /* Flip-Flops and Latches */

    /**
     * Gets and returns a FF/Latch Cell's INIT string with no leading 1'bx.
     * @param ffCell the FF/Latch cell
     * @return the INIT string
     */
    private String getFFLatchInitString(Cell ffCell) {
        if (!ffCell.getProperties().has("INIT")) {
            System.err.println("WARNING: FF/Latch Cell " + ffCell.getName() + " has no INIT property. \\" +
                    "No FASM instruction written.");
            return null;
        }

        String initValString;
        Object initValue = ffCell.getProperties().get("INIT").getValue();
        // Figure out what type the initValue is
        if (initValue instanceof String) {
            // Assuming the string INIT property will be binary (in the form 1'bx).
            // Strip off the leading 1'bx.
            initValString = (String) initValue;
            initValString = initValString.substring(3);
        } else if (initValue instanceof Long || initValue instanceof Integer) {
            initValString = initValue.toString();
        } else {
            throw new Exceptions.ImportExportException("LUT INIT property value is an unexpected type; only strings \\" +
                    " and (long) integers are valid");
        }
        return initValString;
    }

    /**
     * Prints a FASM instruction for a flip-flop or latch's ZINI property.
     * This is the Global Set Reset (GSR) value.
     * @param ffBel the flip-flop/latch BEL
     * @param isFlipFlop whether the BEL is a flip-flop (true) or a latch (false)
     * @throws IOException
     */
    private void printFFLatchZINI(Bel ffBel, boolean isFlipFlop) throws IOException {
        Site site = ffBel.getSite();
        String fullBelName = site.getTile().getName() + "." + getRelativeSliceName(site) + "." + ffBel.getName() + ".";
        Cell ffCell = design.getCellAtBel(ffBel);
        String initValString = getFFLatchInitString(ffCell);

        if (initValString != null) {
            switch (initValString) {
                case "1":
                    if (!isFlipFlop)
                        fileout.write(fullBelName + "ZINI " + "1\n");
                    break;
                case "0":
                    if (isFlipFlop) {
                        fileout.write(fullBelName + "ZINI " + "1\n");
                    }
                    break;
                default:
                    System.err.println("WARNING: Unexpected value for FF/Latch Cell " + ffCell.getName()
                            + "'s BEL INIT " + initValString + ". No FASM instruction written.");
                    break;
            }
        }
    }

    /**
     * Returns whether the clock going into a FF/Latch cell is inverted.
     * TODO: Support for non-string property values?
     * @param ffLatchCell the flip-flop / latch cell to check
     * @return whether the clock is inverted
     */
    private boolean isClkInverted(Cell ffLatchCell) {
        Property clkInvProperty = ffLatchCell.getProperties().get("IS_C_INVERTED");
        String clkInvValue = (clkInvProperty == null) ? "1'b0" : clkInvProperty.getStringValue();
        return clkInvValue.equals("1'b1");
    }

    /**
     * Processes flip-flop and latch bels.
     * @param cell the flip-flop / latch cell
     * @param bel the flip-flop / latch bel
     * @throws IOException
     */
    private void processFlipFlopBel(Cell cell, Bel bel) throws IOException {
        Site site = bel.getSite();
        String fullBelName = site.getTile().getName() + "." + getRelativeSliceName(site) + "." + bel.getName() + ".";
        switch (cell.getType()) {
            case "FDPE":
                printFFLatchZINI(bel, true);
                break;
            case "FDSE":
                printFFLatchZINI(bel, true);
                addSharedSiteProperty(site, "FFSYNC");
                break;
            case "FDRE":
                fileout.write(fullBelName + "ZRST" + " 1\n");
                printFFLatchZINI(bel, true);
                addSharedSiteProperty(site, "FFSYNC");
                break;
            case "FDCE":
                fileout.write(fullBelName + "ZRST" + " 1\n");
                printFFLatchZINI(bel, false);
                break;
            case "LDCE":
                fileout.write(fullBelName + "ZRST" + " 1\n");
                printFFLatchZINI(bel, false);
                addSharedSiteProperty(site, "LATCH");
                break;
            case "LDPE":
                printFFLatchZINI(bel, false);
                addSharedSiteProperty(site, "LATCH");
                break;
            default:
                System.err.println("WARNING: Unrecognized FF/Latch Cell Type for cell " + cell.getName() + ":"
                        + cell.getType() + ". No FASM isntructions will be printed for the cell.");
                break;
        }

        // Add site-wide CLKINV property
        if (cell.isFlipFlop() && isClkInverted(cell)) {
            addSharedSiteProperty(site, "CLKINV");
        } else if (cell.isLatch() && !isClkInverted(cell)) {
            addSharedSiteProperty(site, "CLKINV");
        }
    }

    /* LUT RAMs (Distributed RAMs) */

    /**
     * Prints the INIT property for a 64-LUTRAM. Only RAMD64E tested.
     * Should be used only for internal LUTRAM cells (not the macros)
     * @param bel the LUT RAM Bel
     * @param fullBelName name of the BEL, formatted for FASM
     * @throws IOException
     */
    private void printRam64Init(Bel bel, String fullBelName) throws IOException {
        Cell cell = design.getCellAtBel(bel);
        assert (cell.isInternal());
        String internalCellName = cell.getName().substring(cell.getName().lastIndexOf('/') + 1);
        Cell parentCell = cell.getParent();

        // TODO: Don't assume String INIT or that the property is in HEX.
        String ramInit = parentCell.getProperties().get("INIT").getStringValue();

        // Remove the leading 'h
        ramInit = ramInit.substring(ramInit.lastIndexOf("h") + 1);

        // Switch on the LUT RAM Macro type
        switch (parentCell.getType()) {
            case "RAM64X1D":
                // RAM64X1D => (RAMD64E, RAMD64E)
                // For RAM64X1D, both the DP RAM64E and SP RAM64E share the same init value,
                // which is equal to the macro's init value
                assert (internalCellName.equals("SP") || internalCellName.equals("DP"));
                printLutInitString(hexToBinaryInitString(ramInit), fullBelName, false);
                break;
            case "RAM128X1D":
                // For RAM128X1D (DP.HIGH, DP.LOW, SP.HIGH, and SP.LOW):
                // The LOW internal cells init properties are equal to the parent cell's init property's lower 64 bits
                // The HIGH internal cells init properties are equal to the parent cell's init property's higher 64 bits
                assert (internalCellName.equals("SP.HIGH") || internalCellName.equals("SP.LOW") || internalCellName.equals("DP.HIGH") || internalCellName.equals("DP.LOW"));
                String parentInitBinaryString = hexToBinaryInitString(ramInit);
                assert (parentInitBinaryString.length() == 128);

                // TODO: Test that this works as expected.
                switch (internalCellName) {
                    case "SP.HIGH":
                    case "DP.HIGH":
                        printLutInitString(parentInitBinaryString.substring(0, 63), fullBelName, false);
                        break;
                    case "SP.LOW":
                    case "DP.LOW":
                        printLutInitString(parentInitBinaryString.substring(64), fullBelName, false);
                        break;
                    default:
                        break;
                }
                break;
            default:
                System.err.println("WARNING: Unexpected macro type for LUT RAM macro " + parentCell.getName() + ": "
                        + parentCell.getType() + ". " + "No LUT RAM INIT instruction will be written for "
                        + cell.getName() + ".");
                break;
        }
    }

    /**
     * Processes and writes FASM instructions for a LUTRAM bel.
     * @param cell the LUT RAM cell
     * @param bel the LUT RAM bel
     * @throws IOException
     */
    private void processLutRam(Cell cell, Bel bel) throws IOException {
        Site site = bel.getSite();
        // Replace 6LUT/5LUT with "LUT"
        String fullBelName = site.getTile().getName() + "." + getRelativeSliceName(site) + "." + bel.getName().charAt(0) + "LUT.";
        switch (cell.getType()) {
            case "RAMD32":
            case "RAMS32":
                // Don't write the same thing twice for 6LUT and 5LUT
                // QUESTION: Is it safe to assume a MEM5 will always be paired with a MEM6? (probably not)
                if (bel.getType().equals("LUT_OR_MEM5"))
                    break;

                fileout.write(fullBelName + "RAM 1\n");
                fileout.write(fullBelName + "SMALL 1\n");
                break;
            case "RAMD64E":
                assert (bel.getType().equals("LUT_OR_MEM6"));
                String lut5Name = bel.getName().charAt(0) + "5LUT";
                assert (!design.isBelUsed(bel.getSite().getBel(lut5Name)));
                fileout.write(fullBelName + "RAM 1\n");
                // Both the DP RAM64E and SP RAM64E share the same init value, which is equal to the
                // macro's init value
                // Note that INIT values for memories are quite different from LUT init values and
                // do not repeat like LUT init values.
                printRam64Init(bel, fullBelName);
                break;
        }
    }

    /**
     * Processes and prints FASM instructions for an SRL
     * @param cell the SRL cell
     * @param bel the SRL bel
     * @throws IOException
     */
    private void processSrl(Cell cell, Bel bel) throws IOException {
        Site site = bel.getSite();
        // Replace 6LUT/5LUT with "LUT"
        String fullBelName = site.getTile().getName() + "." + getRelativeSliceName(site) + "." + bel.getName().charAt(0) + "LUT.";

        switch (cell.getType()) {
            case "SRL16E":
                fileout.write(fullBelName + "SMALL 1\n");
            case "SRLC32E":
                fileout.write(fullBelName + "SRL 1\n");
                break;
        }
    }

    /* General */

    /**
     * Converts a hex INIT string to a binary INIT string
     * @param hexInitString the init string in hex (with no leading 0x or 'h)
     * @return the binary INIT string
     */
    private String hexToBinaryInitString(String hexInitString) {
        String binaryInitString = new BigInteger(hexInitString, 16).toString(2);
        String formatPad = "%" + (hexInitString.length() * 4) + "s";
        return String.format(formatPad, binaryInitString).replace(" ", "0");
    }

    /**
     * Gets the binary INIT string, given a LUT Equation and the # of inputs
     * @param lutEquation The LUT equation
     * @param numInputs # of inputs into the LUT equation
     * @return the binary INIT string
     */
    private String getBinaryInitString(LutEquation lutEquation, int numInputs) {
        InitString newInitString = InitString.convertToInitString(lutEquation, numInputs);

        // Remove leading 0x from newInitString
        String hexInitString = newInitString.toString().substring(2);
        return hexToBinaryInitString(hexInitString);
    }

    /**
     * Processes all the used BELs in the design and writes corresponding FASM instructions.
     * @throws IOException
     */
    public void processBels() throws IOException {
        for (Bel bel : usedBels) {
            Cell cell = design.getCellAtBel(bel);
            switch (bel.getType()) {
                // SLICEL Bels
                case "LUT6":
                case "LUT5":
                    processLutBel(bel);
                    break;
                // SLICEM Bels
                case "LUT_OR_MEM6":
                case "LUT_OR_MEM5":
                    if (routethroughBels.contains(bel) || vccSourceBels.contains(bel) || gndSourceBels.contains(bel)) {
                        processLutBel(bel);
                        break;
                    } else {
                        switch (cell.getType()) {
                            case "RAMD32":
                            case "RAMS32":
                            case "RAMD64E":
                                processLutRam(cell, bel);
                                break;
                            case "SRL16E":
                            case "SRLC32E":
                                processSrl(cell, bel);
                                break;
                            case "LUT1":
                            case "LUT2":
                            case "LUT3":
                            case "LUT4":
                            case "LUT5":
                            case "LUT6":
                                processLutBel(bel);
                                break;
                            default:
                                System.err.println("Warning: Unrecognized LUT cell type " + cell.getType());
                                break;
                        }
                    }
                    break;
                case "REG_INIT":
                case "FF_INIT":
                    processFlipFlopBel(cell, bel);
                    break;
                case "CARRY4": // CARRY4 cells
                    // configuration is taken care of by ACY0, ... , DCY0 site pips
                    break;
                case "SELMUX2_1":
                    // Inner RAM cell. 2:1 MUX. Taken care by site pips set elsewhere.
                    break;
                // Unsupported Bel Types (at this time)
                case "PAD":
                case "IOB33_OUTBUF":
                case "IOB33_INBUF_EN":
                case "BUFG_BUFG":
                    break;
                default:
                    System.err.println("Warning: Unrecognized bel type " + bel.getType());
                    break;
            }
        }
    }

    /**
     * Adds a site and an associated property to the map of shared site properties.
     * @param site the site the property belongs to
     * @param propertyToSet the property as a string
     */
    private void addSharedSiteProperty(Site site, String propertyToSet) {
        Set<String> siteProps = sharedSiteProperties.getOrDefault(site, new HashSet<>());
        siteProps.add(propertyToSet);
        sharedSiteProperties.put(site, siteProps);
    }

    /**
     * @return the shared site properties map
     */
    public Map<Site, Set<String>> getSharedSiteProperties() {
        return sharedSiteProperties;
    }

}
