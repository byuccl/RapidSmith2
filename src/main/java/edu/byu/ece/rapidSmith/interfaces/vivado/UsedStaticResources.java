/*
 * Copyright (c) 2018 Brigham Young University
 *
 * This file is part of the BYU RapidSmith Tools.
 *
 * BYU RapidSmith Tools is free software: you may redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * BYU RapidSmith Tools is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * A copy of the GNU General Public License is included with the BYU
 * RapidSmith Tools. It can be found at doc/LICENSE.GPL3.TXT. You may
 * also get a copy of the license at <http://www.gnu.org/licenses/>.
 */
package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;
import org.apache.commons.lang3.tuple.MutablePair;

import static edu.byu.ece.rapidSmith.util.Exceptions.ParseException;

/**
 * This class is used for parsing the static_resources.rsc file in a RSCP checkpoint.
 * This file is used to specify which resources in a reconfigurable area that the static
 * design uses.
 */
public class UsedStaticResources {

	private final Device device;
	private final CellDesign design;
	private final CellLibrary libCells;
	private final WireEnumerator wireEnumerator;
	private Pattern pipNamePattern;
	private static final String BUFFER_INIT_STRING = "2'h2";

	private int currentLineNumber;
	private String currentFile;
    // Map from port name(s) to a pair of the static net name and the static portion of the route string
	private Map<String, MutablePair<String, String>> staticRoutemap;
	private Map<String, String> oocPortMap; // Map from port name to the associated partition pin's node

	private Collection<CellNet> multiPortSinkNets;
	//private Set<Wire> reservedWires;

	/**
	 * Creates a new XdcRoutingInterface object.
	 * 
	 * @param design {@link CellDesign} to add routing information to
	 * @param device {@link Device} of the specified design
	 */
	public UsedStaticResources(CellDesign design, Device device, CellLibrary libCells) {
		this.device = device;
		this.wireEnumerator = device.getWireEnumerator();
		this.design = design;
		this.libCells = libCells;
		this.pipNamePattern = Pattern.compile("(.*)/.*\\.([^<]*)((?:<<)?->>?)(.*)");
		//reservedWires = new HashSet<>();
		this.multiPortSinkNets = getMultiPortSinkNets();

	}
	
	/**
	 * Parses the specified static_resources.rsc file, and marks used resources in the design as used.
	 * 
	 * @param resourcesFile static_resources.rsc file
	 * @throws IOException
	 */
	public void parseResourcesRSC(String resourcesFile) throws IOException {
		// Regex used to split lines via whitespace
		Pattern whitespacePattern = Pattern.compile("\\s+");
		
		// try-with-resources to guarantee no resource leakage
		try (LineNumberReader br = new LineNumberReader(new BufferedReader(new FileReader(resourcesFile)))) {
		
			String line;
			while ((line = br.readLine()) != null) {
				String[] toks = whitespacePattern.split(line);
	
				switch (toks[0]) {
					case "USED_PIPS" : 
						processUsedPips(toks);
						break;
					case "STATIC_RT" :
						// FIXME: Everything doesn't really need to be split by whitespace for processStaticRoutes.
						processStaticRoutes(toks);
						break;
					case "VCC_SOURCES" : 
					case "GND_SOURCES" : 
					case "LUT_RTS" : 
					case "SITE_RTS":
						// Used static sources, LUT Routethroughs, & Site Routethroughs
						// aren't expected to be found within a PR region.
						if (toks.length > 1)
							throw new ParseException("Unexpected Token Content: " + toks[0]);
						break;
					case "PART_PIN" : processOocPort(toks);
						break;
					case "VCC_PART_PINS":
						processStaticPartPins(toks, true);
						break;
					case "GND_PART_PINS":
						processStaticPartPins(toks, false);
						break;
					default : 
						throw new ParseException("Unrecognized Token: " + toks[0]);
				}
			}
		}
	}

	/**
	 * Processes the "VCC_PART_PINS" token in the static_resources.rsc of a RSCP.
	 * Expected Format: VCC_PART_PINS partPinName partPinName ...
	 * @param toks An array of space separated string values parsed from the placement.rsc
	 */
	private void processStaticPartPins(String[] toks, boolean isVcc) {
		for (int i = 1; i < toks.length; i++) {

			if (oocPortMap == null) {
				oocPortMap = new HashMap<>();
			}
			String oocPortVal = isVcc? "VCC" : "GND";

			oocPortMap.put(toks[i], oocPortVal);
			String portName = toks[i];
			Cell portCell = design.getCell(portName);

			// Remove the port cell's pin from the design (they are outside of the partial device)
			assert (portCell.getPins().size() == 1);
			CellPin cellPin = portCell.getPins().iterator().next();
			CellNet net = cellPin.getNet();

			// Detach the port's pin from its current net
			assert (net != null);
			//if (net != null)
			net.disconnectFromPin(cellPin);

			// Re-assign the net's pins to either VCC or GND
			CellNet staticNet = isVcc? design.getVccNet() : design.getGndNet();

			// Assuming driver has already been removed
			List<CellPin> pins = new ArrayList<>(net.getPins());
			net.disconnectFromPins(pins);
			design.removeNet(net);
			staticNet.connectToPins(pins);

			CellPin partPin = new PartitionPin( portName, null, PinDirection.IN);
			portCell.attachPartitionPin(partPin);
		}
	}

	// Get the number of port and/or partition pin sinks a net has
	private long getNumPortSinks(CellNet net) {
		return net.getPins().stream()
				.filter( cellPin -> (cellPin.getDirection().equals(PinDirection.IN) && (cellPin.isPartitionPin() || cellPin.getCell().isPort())))
				.count();
	}

	// Get all nets with more than one port as a sink
	private Collection<CellNet> getMultiPortSinkNets() {
		Collection<CellNet> nets = new ArrayList<>();
		for (CellNet net : design.getNets()) {
			if (net.getPins().stream().filter(cellPin -> cellPin.getDirection().equals(PinDirection.IN) && cellPin.getCell().isPort()).count() > 1)
				nets.add(net);
		}
		return nets;
	}

	/**
	 * Processes the "PART_PIN" token in the static_resources.rsc of a RSCP. Specifically,
	 * this function adds the OOC port and corresponding port wire to the oocPortMap
	 * data structure for later processing.
	 *
	 * Expected Format: PART_PIN portName Tile/Wire direction
	 * @param toks An array of space separated string values parsed from the placement.rsc
	 */
	private void processOocPort(String[] toks) {

		// TODO: Add currentLineNumber back in
		//assert (toks.length == 4) : String.format("Token error on line %d: Expected format is \"OOC_PORT\" PortName Tile/Wire ", this.currentLineNumber);

		if (this.oocPortMap == null) {
			this.oocPortMap = new HashMap<>();
		}

		oocPortMap.put(toks[1], toks[2]);
		String portName = toks[1];

		Cell portCell = design.getCell(portName);

		String[] wireToks = toks[2].split("/");
		// TODO: Try get tile, etc.
		Tile tile = device.getTile(wireToks[0]);

		TileWire partPinWire;

		// If tile is null, the tile is probably outside of the partial device boundaries (do a better check for this?)
		// See if the partition pin node exists in the hierarchical port
		// TODO: Check that the node is exactly the right one. ie make sure the true tile name matches as well.
		if (tile == null) {
			tile = device.getTile("HIER_PORT_X0Y0");
			// TODO: Handle OWIRE as well? (I don't think this can occur)
			partPinWire = tile.getWire("IWIRE:" + wireToks[0] + "/" + wireToks[1]);
		}
		else {
			partPinWire = tile.getWire(wireToks[1]);
		}
		assert (partPinWire != null);

		// Add the partition pin node to the list of reserved wires
		design.addReservedWire(partPinWire);
		//reservedWires.add(partPinWire);
		String direction = toks[3];

		PinDirection pinDirection;
		switch (direction) {
			case "IN" : pinDirection = PinDirection.IN; break;
			case "OUT" : pinDirection = PinDirection.OUT; break;
			case "INOUT" :
			default: throw new AssertionError("Invalid direction");
		}


		// Remove the port cell's pin from the design (they are outside of the partial device)
		assert (portCell.getPins().size() == 1);
		CellPin cellPin = portCell.getPins().iterator().next();
		CellNet net = cellPin.getNet();

		// From Vivado: If the the cell pin has no net, that means that this part pin is a sink (source to the RM)
		// that is not used by this RM. We need to insert a LUT1 buffer and tie some nets.
		// From Yosys: If the net associated with the port only has one pin and it is a PAD source, then this part pin is
		// a sink (source to the RM) that is not used by this RM. We need to insert a LUT1 buffer and tie some nets.
		boolean unusedPartPinDriver = false; // true if the part pin, which is a driver to the RM, is unused

		if (net != null) {

			// TODO: Get rid of the static check. Merge code.
			if (!net.isStaticNet() && multiPortSinkNets.contains(net)) {
				assert (pinDirection == PinDirection.IN);
				// This net drives more than one partition pin.
				// This seems similar (maybe identical) to the VCC/GND case.

				// We need to insert a buffer cell and net.

				net.disconnectFromPin(cellPin);
				CellPin partPin = new PartitionPin( portName, partPinWire, pinDirection);
				portCell.attachPartitionPin(partPin);

				insertBufferPartitionPinNet(partPin, net);
				return;
			}
			else {
				net.disconnectFromPin(cellPin);

				// If pinDirection is out, then that means the port pin was a source.
				if (pinDirection == PinDirection.OUT && net.getFanOut() == 0) {
					// The net has no sinks.
					unusedPartPinDriver = true;

				}
			}



		}

		// TODO: Figure out if this is a true assumption
		// TODO: Learn how to handle this case when using Yosys
		// Note! The below may only true when exporting a synthesized RM from Vivado!
		// If the net is GND, this part pin is a driver, but the RM's HDL is not driving it with anything.
		// We must insert a LUT1 buffer to drive the part pin.

		// Add the partition pin to the port cell
		// Get the partpin node

		//PartitionPin partPin = new PartitionPin("partPin." + portName + "." + direction, partPinWire, pinDirection);
		CellPin partPin = new PartitionPin( portName, partPinWire, pinDirection);
		portCell.attachPartitionPin(partPin);

		if (unusedPartPinDriver || net == null || net.isStaticNet())
			insertBufferPartitionPinNet(partPin, net);
		else
			net.connectToPin(partPin);
		//partPin.setCell(portCell);
	}


	private void insertBufferPartitionPinNet(CellPin partPin, CellNet net) {
		switch (partPin.getDirection()) {
			case IN:
				// The signal is leaving the partial device

				// If it doesn't have a net, assume it needs to be GND.
				if (net == null)
					net = design.getGndNet();

				//assert (net.isStaticNet());

				// Make a LUT1 buffer.
				//String bufferName = ((net.isVCCNet()) ? "VCC" : "GND") + "_Inserted_" + partPin.getPortName();
				String bufferName = net.getName() + "_Inserted_" + partPin.getPortName();
				Cell lutCell = new Cell(bufferName, libCells.get("LUT1"));
				lutCell.getProperties().update(new Property("INIT", PropertyType.EDIF, BUFFER_INIT_STRING));
				design.addCell(lutCell);

				// Drive the LUT1 with GND/VCC
				//design.getGndNet().connectToPin(lutCell.getPin("I0"));
				net.connectToPin(lutCell.getPin("I0"));

				// Make a net to drive the partition pin
				CellNet partPinDriverNet = new CellNet(partPin.getPortName(), NetType.WIRE);
				design.addNet(partPinDriverNet);

				partPinDriverNet.connectToPin(lutCell.getPin("O"));
				partPinDriverNet.connectToPin(partPin);
				break;
			case OUT:
				//assert (staticNet == null);
				// The signal is coming from outside the partial device

				// We need to make a new net, carefully creating the name to be what is expected (needs to match
				// what the static_resources file has).
				// TODO: Is it satisfactory to just use the name of the port as the name of this net?

				// The net will be null coming from Vivado, but will already exists if coming from Yosys
				if (net == null) {
					net = new CellNet(partPin.getPortName(), NetType.WIRE);
					design.addNet(net);
				}


				// Create a LUT1 buffer. The net will drive this LUT, but the LUT's output will go nowhere.
				lutCell = new Cell("IN_BUF_Inserted_" + partPin.getPortName(), libCells.get("LUT1"));
				design.addCell(lutCell);
				lutCell.getProperties().update(new Property("INIT", PropertyType.EDIF, BUFFER_INIT_STRING));

				// Connect the net to the part pin and to the LUT1 buffer
				net.connectToPin(partPin);
				net.connectToPin(lutCell.getPin("I0"));

				break;
			case INOUT:
			default: throw new AssertionError("Invalid direction");
		}
	}
	
	/**
	 * Processes an array of used PIP tokens and marks used PIP wire connections as used.
	 * 
	 * @param toks a String array of used PIP tokens in the form: <br>
	 * {@code LUT_RTS tile0.tileType/sourceWire0->>sinkWire0 tile1.tileType/sourceWire1->sinkWire1 ...}
	 */
	private void processUsedPips(String[] toks) {		
		
		for (int i = 1; i < toks.length; i++ ) {			
			Matcher m = pipNamePattern.matcher(toks[i]);
			
			if (m.matches()) {
				String tileName = m.group(1);
				String source = m.group(2);
				String sink = m.group(4);

				Tile tile = device.getTile(tileName);
				Wire startWire = new TileWire(tile, wireEnumerator.getWireEnum(source));
				Wire sinkWire = new TileWire(tile, wireEnumerator.getWireEnum(sink));

				// Mark both the startWire and sinkWire as being reserved.
				design.addReservedWire(startWire);
				design.addReservedWire(sinkWire);
				//reservedWires.add(startWire);
				//reservedWires.add(sinkWire);
		
				//PIP pip = new PIP(startWire, sinkWire);
				//tile.setUsedPIP(pip, false); // Mark the PIP as used
	 				
			}
			else {
				throw new ParseException("Invalid Pip String configuration: " + toks[i]);
			}
		}	
	}

	/**
	 *
	 * @param toks
	 */
	private void processStaticRoutes(String[] toks) {
		assert(toks.length > 3);
		StringBuilder staticRouteString = new StringBuilder();
		ArrayList<String> portNames  = new ArrayList<>();

		// First token (after STATIC_RT) is name of the static-net
		String staticNetName = toks[1];

		// Next tokens are the names of the associated ports
		int i = 2;
		while (!toks[i].equals("{")) {
			portNames.add(toks[i]);
			i++;
		}

		// Iterate through the rest of the static route
		for (; i < toks.length; i++) {

			if (!toks[i].equals("{") && !toks[i].equals("}")) {
				// Some wires in the static route will be outside of the partial device, but some of the wires will be
				// within the partial device. These wires need to be marked as used so routers know they cannot be used.
				String[] wireToks = toks[i].split("/");
				Tile tile = device.getTile(wireToks[0]);
				Integer wireEnum = wireEnumerator.getWireEnum(wireToks[1]);

				// If the wire exists within the partial device, mark it as used
				if (tile != null && wireEnum != null) {
					Wire tileWire = new TileWire(tile, wireEnum);

					// Set the tile wire as used so routers know to not use it
					design.addReservedWire(tileWire);
					//reservedWires.add(tileWire);
				}
			}

			// Append the "{", "}", or wire to the static route string
			staticRouteString.append(toks[i]).append(" ");
		}

		MutablePair<String, String> netRoute = new MutablePair<>(staticNetName, staticRouteString.toString());

		if (this.staticRoutemap == null)
			this.staticRoutemap = new HashMap<>();

		for (String portName : portNames) {
			staticRoutemap.put(portName, netRoute);
		}

	}

    public Map<String, MutablePair<String, String>> getStaticRoutemap() {
        return staticRoutemap;
    }


	/**
	 * @return the oocPortMap
	 */
	public Map<String, String> getOocPortMap() {
		return oocPortMap;
	}

}
