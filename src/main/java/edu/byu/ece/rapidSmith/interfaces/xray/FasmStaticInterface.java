package edu.byu.ece.rapidSmith.interfaces.xray;

import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.RouteStringTree;
import edu.byu.ece.rapidSmith.device.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is used for writing the properties of the static design (in a partial reconfiguration design)
 * to a FASM file. These properties are not part of the reconfigurable module, but MUST be included in a FASM file.
 *
 * @author Dallon Glick
 *
 */
public class FasmStaticInterface extends AbstractFasmInterface {
	private final BufferedWriter fileout;
	private final Map<String, RouteStringTree> staticRouteStringMap;

	public FasmStaticInterface(Device device, CellDesign design, BufferedWriter fileout) {
		super(device, design);
		this.staticRouteStringMap = design.getStaticRouteStringMap();
		this.fileout = fileout;
	}

	private Wire getWire(String fullWireName) {
		String[] toks = fullWireName.split("/");
		String tileName = toks[0];
		String wireName = toks[1];

		Tile tile = tryGetTile(tileName);

		if (tile.getName().equals("OOC_WIRE_X0Y0")) {
			wireName = tileName + "/" + wireName;
		}

		return tile.getWire(wireName);
	}

	/**
	 * Writes the FASM instruction for a PIP.
	 * The instruction is in the format "tile.source_wire sink_wire"
	 * @param pip the PIP to write a FASM instruction for.
	 */
	private void writePipInstruction(Connection pip) throws IOException {
		Wire source = pip.getSourceWire();
		Tile sourceTile = source.getTile();
		Wire sink = pip.getSinkWire();

		if (!isPseudoPip(source, sink)) {
			fileout.write(sourceTile.getName() + "." + sink.getName() + " " + source.getName() + "\n");
		}
	}

	/**
	 * Write FASM instructions for all PIPs found in the static design (that are contained within
	 * the partial device)
	 * @throws IOException
	 */
	public void writeStaticDesignPips() throws IOException {
		for (Map.Entry<String, RouteStringTree> entry : staticRouteStringMap.entrySet()) {
			fileout.write("# Static Design Net: " + entry.getKey() + "\n");

			// Write instructions for every PIP that is present in the partial device
			for (RouteStringTree stringTree : entry.getValue()) {
				if (stringTree.getSourceTree() != null) {
					RouteStringTree parentTree = stringTree.getSourceTree();

					// Try to get the parent wire
					Wire parentWire = getWire(parentTree.getWireName());

					// Try to get the current (child) wire
					Wire childWire = getWire(stringTree.getWireName());

					if (parentWire != null && childWire != null) {
						// Find the PIP that connects these wires

						Connection conn = getPipConn(parentWire, childWire);
						if (conn != null) {
							writePipInstruction(conn);
						}
					}
				}
			}
		}
	}

	/**
	 * Gets all forward PIPs from the start wire, searching through its direct connections.
	 * @param startWire the wire where the PIPs ultimately begin
	 * @return Collection of the forward PIPs
	 */
	private Collection<Connection> getNodeForwardPips(Wire startWire) {
		// Keep a list of visited wires to prevent looping between bi-directional direct connections
		Set<Wire> visited = new HashSet<>();
		visited.add(startWire);

		Stack<Connection> directConns = new Stack<>();

		// Gather all direct connections (long wires have more than one)
		directConns.addAll(startWire.getWireConnections().stream()
				.filter(Connection::isDirectConnection)
				.collect(Collectors.toList()));

		// Gather all PIPs
		Collection<Connection> nodePips = startWire.getWireConnections().stream()
				.filter(Connection::isPip)
				.collect(Collectors.toList());

		// Find all PIPs connected to direct connections
		while (directConns.size() > 0) {
			Connection directConn = directConns.pop();
			visited.add(directConn.getSinkWire());
			directConns.addAll(directConn.getSinkWire().getWireConnections().stream()
					.filter(Connection::isDirectConnection)
					.filter(connection -> (!visited.contains(connection.getSinkWire())))
					.collect(Collectors.toList()));
			nodePips.addAll(directConn.getSinkWire().getWireConnections().stream()
					.filter(Connection::isPip)
					.collect(Collectors.toList()));
		}


		return nodePips;
	}

	/**
	 * Gets all reverse PIPs from the start wire, searching through its direct connections.
	 * @param startWire the wire where the PIPs ultimately begin
	 * @return Collection of the reverse PIPs
	 */
	private Collection<Connection> getNodeReversePips(Wire startWire) {
		// Keep a list of visited wires to prevent looping between bi-directional direct connections
		Set<Wire> visited = new HashSet<>();
		visited.add(startWire);

		Stack<Connection> directConns = new Stack<>();

		// Gather all direct connections (long wires have more than one)
		directConns.addAll(startWire.getReverseWireConnections().stream()
				.filter(Connection::isDirectConnection)
				.collect(Collectors.toList()));

		// Gather all PIPs
		Collection<Connection> nodePips = startWire.getReverseWireConnections().stream()
				.filter(Connection::isPip)
				.collect(Collectors.toList());

		// Find all PIPs connected to direct connections
		while (directConns.size() > 0) {
			Connection directConn = directConns.pop();
			visited.add(directConn.getSinkWire());
			directConns.addAll(directConn.getSinkWire().getReverseWireConnections().stream()
					.filter(Connection::isDirectConnection)
					.filter(connection -> (!visited.contains(connection.getSinkWire())))
					.collect(Collectors.toList()));

			nodePips.addAll(directConn.getSinkWire().getReverseWireConnections().stream()
					.filter(Connection::isPip)
					.collect(Collectors.toList()));
		}
		return nodePips;
	}

	/**
	 * Get the forward PIP connection that connects two wires together.
	 * Searches through direct connections to find PIPs.
	 * @param parentWire the start wire
	 * @param childWire the end wire
	 * @return the PIP connection
	 */
	private Connection getPipConn(Wire parentWire, Wire childWire) {
		// The matching forward PIP will be in parentConns and the matching reverse
		// PIP will be in childReverseConns
		Collection<Connection> parentConns = getNodeForwardPips(parentWire);
		Collection<Connection> childReverseCons = getNodeReversePips(childWire);

		// Get the unique start wires from childReverseCons
		Set<Wire> startWires = childReverseCons.stream()
				.map(Connection::getSourceWire)
				.collect(Collectors.toSet());

		// Get all connections in parentConns that end in any of the childReverseCon's start wires
		Set<Connection> possibleConnections = parentConns.stream()
				.filter(connection -> startWires.contains(connection.getSinkWire()))
				.collect(Collectors.toSet());

		if (possibleConnections.size() == 0) {
			// If the parent wire connects to a wire outside of the partial device, there is no PIP
			// within the partial device to enable.
			return null;
		}

		assert (possibleConnections.size() == 1);
		return possibleConnections.iterator().next();



	}



}
