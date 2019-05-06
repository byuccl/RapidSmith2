package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;
import static edu.byu.ece.rapidSmith.util.Exceptions.ParseException;

/**
 * This class is used for parsing the static_resources.rsc file in a RSCP checkpoint.
 * This file is used to specify which resources in a reconfigurable area that the static
 * design uses.
 */
public class StaticResourcesInterface {
	private final Device device;
	private final CellDesign design;
	private final WireEnumerator wireEnumerator;

	// Map from port name(s) to a pair of the static net name and the static portion of the route string
	private Map<String, MutablePair<String, String>> staticRoutemap;

	/**
	 * Creates a new XdcRoutingInterface object.
	 *
	 * @param design {@link CellDesign} to add routing information to
	 * @param device {@link Device} of the specified design
	 */
	public StaticResourcesInterface(CellDesign design, Device device) {
		this.device = device;
		this.wireEnumerator = device.getWireEnumerator();
		this.design = design;
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
					case "RESERVED_WIRES" :
						processReservedWires(toks);
						break;
					case "STATIC_RT" :
						// FIXME: Everything doesn't really need to be split by whitespace for processStaticRoutes.
						processStaticRoutes(toks);
						break;
					case "SITE_RTS":
						// TODO: Process the site routethroughs here
						// Used static sources, LUT Routethroughs, & Site Routethroughs
						// aren't expected to be found within a PR region.
						if (toks.length > 1)
							throw new ParseException("Unexpected Token Content: " + toks[0]);
						break;
					default :
						throw new ParseException("Unrecognized Token: " + toks[0]);
				}
			}
		}

		// The nets that partition pin routes correspond with may have changed during placement import.
		// Find such pins and update the static route map accordingly.
		// TODO: Do this less dumb.
		//	for (Cell port : design.getPorts().collect(Collectors.toList())) {
		//	for (CellPin partPin : port.getPins().stream().filter(p -> p.isPartitionPin()).collect(Collectors.toList())) {
		//		CellNet partPinNet = partPin.getNet();

		//		if (!partPinNet.isStaticNet() && !staticRoutemap.containsKey(partPinNet.getName())) {
		//			MutablePair<String, String> value = staticRoutemap.get(partPin.getPortName());

		//			assert (value != null);
		//			staticRoutemap.remove(partPin.getPortName());
		//			staticRoutemap.put(partPin.getNet().getName(), value);
		//		}
		//	}
		//}

	}

	/**
	 * Processes a string array of reserved wire tokens.
	 *
	 * @param toks a String array of used wire tokens in the form: <br>
	 * {@code RESERVED_WIRES tile0/wire1 tile1/wire2 ...}
	 */
	private void processReservedWires(String[] toks) {
		for(int i = 1; i < toks.length; i++) {
			String[] vals = toks[i].split("/");
			assert vals.length == 2;
			String tileName = vals[0];
			String wireName = vals[1];

			// TODO: Try get Tile, wire
			Tile tile = device.getTile(tileName);
			Wire reservedWire = new TileWire(tile, wireEnumerator.getWireEnum(wireName));

			// Mark all wires in the node as reserved
			for (Wire wire : reservedWire.getWiresInNode()) {
				// Only add the reserved wires that were not part of a partial partition pin route
				if (!design.getReservedWires().containsKey(wire)) {
					// TODO: Provide the name of the static wire that is using this wire
					// TODO: Map to the net/net name?
					// These wires are used by the static design for other nets not in the RM; i.e., they just
					// route through the RM.
					design.addReservedWire(wire);
				}

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
		Set<CellNet> portNets = new HashSet<>();

		// First token (after STATIC_RT) is name of the static-net
		String staticNetName = toks[1];

		// Next tokens are the names of the associated ports
		int i = 2;
		while (!toks[i].equals("{")) {
			// Get the partition pin net. Note that it may be named differently than just the ooc port name.
			Cell oocPort = design.getCell(toks[i]);
			CellPin partPin = oocPort.getPin(toks[i]);
			CellNet partPinNet = partPin.getNet();

			assert (partPinNet != null);
			portNets.add(partPinNet);


			// TODO: Refactor this code a bit.
			portNames.add(partPin.getNet().getName());
			i++;
		}

		// Set the aliases for the cellnet. Note that these are only aliases within the context of the full design.
		for (CellNet portNet : portNets) {
			portNet.setAliases(portNets);
		}

		// Iterate through the rest of the static route
		for (; i < toks.length; i++) {

			if (!toks[i].equals("{") && !toks[i].equals("}")) {
				// Some wires in the static route will be outside of the partial device, but some of the wires will be
				// within the partial device. These wires need to be marked as used so routers know they cannot be used.
				String[] wireToks = toks[i].split("/");
				Tile tile = device.getTile(wireToks[0]);
				Integer wireEnum;

				// If tile is null, try to get the wire from the partial device's OOC_WIRE tile
				if (tile == null) {
					tile = device.getTile("OOC_WIRE_X0Y0");
					wireEnum = wireEnumerator.getWireEnum(wireToks[0] + "/" + wireToks[1]);
				}
				else {
					wireEnum = wireEnumerator.getWireEnum(wireToks[1]);
				}

				// If the wire exists within the partial device, mark it as used
				if (tile != null && wireEnum != null) {
					Wire tileWire = new TileWire(tile, wireEnum);

					// Set the tile wire as used so routers know to not use it
					for (Wire wire : tileWire.getWiresInNode()) {
						design.addReservedWire(wire, portNets);
					}
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

}