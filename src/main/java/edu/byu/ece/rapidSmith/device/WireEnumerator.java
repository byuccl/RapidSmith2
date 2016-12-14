package edu.byu.ece.rapidSmith.device;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains the mapping from wire enumeration to wire names as well as
 * types and directions of the wires.
 * Created by Travis Haroldsen on 11/15/14.
 */
public class WireEnumerator implements Serializable {
    /** Map of wire names to wire enumerations */
    private Map<String, Integer> wireMap;
    /** Names of each wire enumeration */
    private String[] wires;
    /** Types of each wire */
    private WireType[] wireTypes;
    /** The Directions of each wire */
    private WireDirection[] wireDirections;

    /**
     * Returns the map of wire names to wire enumerations.
     *
     * @return the map of wire names to wire enumerations
     */
    public Map<String, Integer> getWireMap() {
        return wireMap;
    }

    /**
     * Returns the enumeration for the wire of the specified name.
     *
     * @param wireName name of the wire
     * @return the enumeration of the wire with the specified name or null if no
     *   wire with the name exists in the device
     */
    public Integer getWireEnum(String wireName) {
        return wireMap.get(wireName);
    }

    /**
     * Sets the map of wire names to wire enumerations.
     *
     * @param wireMap the map of wire names to wire enumerations
     */
    public void setWireMap(Map<String, Integer> wireMap) {
        this.wireMap = wireMap;
    }

    /**
     * Returns the array containing all of the wire names in the device.  The wires are
     * arranged according to their enumerations, i.e. the name of the wire with
     * enumeration 10 can be obtained by getWires()[10];
     *
     * @return the array containing all of the wire names in the device
     */
    public String[] getWires() {
        return wires;
    }

    /**
     * Returns the name of the wire with the specified enumeration.
     *
     * @param wireEnum the wire enumeration
     * @return the name of the wire with the specified enumeration
     */
    public String getWireName(int wireEnum) {
        return wires[wireEnum];
    }

    /**
     * Sets the array of wire names for the device.  The array should be ordered
     * such that the wire name at each index corresponds to the wire with the same
     * enumeration as the index.
     *
     * @param wires the array of wire names
     */
    public void setWires(String[] wires) {
        this.wires = wires;
    }

    /**
     * Returns the array containing the types of the wires.
     * The array acts as a map of wire enumeration to wire types.
     *
     * @return the array containing the types of the wire
     */
    public WireType[] getWireTypes() {
        return wireTypes;
    }

    /**
     * Returns the type of the wire with the specified enumeration.
     *
     * @param wireEnum enumeration of wire of interest
     * @return the type of the wire with the specified enumeration
     */
    public WireType getWireType(int wireEnum) {
        return wireTypes[wireEnum];
    }

    /**
     * Sets the wire types arrays.
     *
     * @param wireTypes the array of wire types
     * @see #getWireTypes()
     */
    public void setWireTypes(WireType[] wireTypes) {
        this.wireTypes = wireTypes;
    }

    /**
     * Returns the array of the wire directions.
     * The array acts as a map of wire enumeration to wire directions.
     *
     * @return the array of the wire directions
     */
    public WireDirection[] getWireDirections() {
        return wireDirections;
    }

    /**
     * Returns the wire direction of the specified wire.
     * @param wireEnum enumeration of wire of interest
     * @return the wire direction of the specified wire
     */
    public WireDirection getWireDirection(int wireEnum) {
        return wireDirections[wireEnum];
    }

    /**
     * Sets the wire directions array.
     *
     * @param wireDirections the array of wire directions
     * @see #getWireDirections()
     */
    public void setWireDirections(WireDirection[] wireDirections) {
        this.wireDirections = wireDirections;
    }

    /*
       For Hessian compression.  Avoids writing duplicate data.
     */
    private static class WireEnumeratorReplace implements Serializable {
	    private static final long serialVersionUID = -2476200871171034998L;
	    private String[] wires;
        private WireType[] wireTypes;
        private WireDirection[] wireDirections;

        @SuppressWarnings("UnusedDeclaration")
        private WireEnumerator readResolve() {
            WireEnumerator we = new WireEnumerator();
            we.wires = wires;
            we.wireTypes = wireTypes;
            we.wireDirections = wireDirections;

            we.wireMap = new HashMap<>((int) (wires.length / 0.75 + 1));
            for (int i = 0; i < wires.length; i++) {
                we.wireMap.put(wires[i], i);
            }

            return we;
        }
    }

    private Object writeReplace() {
        WireEnumeratorReplace repl = new WireEnumeratorReplace();
        repl.wires = wires;
        repl.wireTypes = wireTypes;
        repl.wireDirections = wireDirections;

        return repl;
    }
}
