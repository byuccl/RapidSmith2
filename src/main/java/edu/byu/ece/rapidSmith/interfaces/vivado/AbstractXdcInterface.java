package edu.byu.ece.rapidSmith.interfaces.vivado;

import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.ImplementationMode;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import edu.byu.ece.rapidSmith.util.Exceptions;

public abstract class AbstractXdcInterface {

	protected final Device device;
	protected int currentLineNumber;
	protected String currentFile;
	protected final WireEnumerator wireEnumerator;
	protected ImplementationMode implementationMode;

	public AbstractXdcInterface(Device device, CellDesign design) {
		this.device = device;
		this.wireEnumerator = device.getWireEnumerator();
		this.currentLineNumber = 0;
		this.implementationMode = design.getImplementationMode();
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

}
