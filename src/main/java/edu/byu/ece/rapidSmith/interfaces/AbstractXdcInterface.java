package edu.byu.ece.rapidSmith.interfaces;

import edu.byu.ece.edif.core.*;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.interfaces.vivado.XdcRoutingInterface;
import edu.byu.ece.rapidSmith.util.Exceptions;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractXdcInterface {

	protected final Device device;
	protected int currentLineNumber;
	protected String currentFile;
	protected final WireEnumerator wireEnumerator;

	public AbstractXdcInterface(Device device) {
		this.device = device;
		this.wireEnumerator = device.getWireEnumerator();
		this.currentLineNumber = 0;
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
