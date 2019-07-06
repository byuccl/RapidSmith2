package edu.byu.ece.rapidSmith.interfaces.xray;

import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.RouteStatus;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is used for writing the FASM instructions for inter-site routing (turning on PIPs)
 *
 * @author Dallon Glick
 *
 */
public class FasmRoutingInterface extends AbstractFasmInterface {
	private final FamilyType family;
	private final CellDesign design;
	private final BufferedWriter fileout;

	public FasmRoutingInterface(Device device, CellDesign design, BufferedWriter fileout) {
		super(device, design);
		this.family = device.getFamily();
		this.design = design;
		this.fileout = fileout;
	}

	/**
	 * Writes FASM instructions for the interconnect PIPs in the net.
	 * Instructions are in the format "tile.source_wire sink_wire"
	 * @param net the net to write instructions for
	 * @throws IOException
	 */
	private void writeNetPips(CellNet net) throws IOException {
		// Assuming all static nets would be made up of pseudo PIPs.
		assert net.isStaticNet() || (net.getIntersiteRouteTreeList().size() < 2);

		// Write name of net as a comment
		fileout.write("# Net: " + net.getName() + "\n");

		Set<Wire> hclkBufferEnableWires = new HashSet<>();

		for (RouteTree intersiteRouteTree : net.getIntersiteRouteTreeList()) {
			for (RouteTree rt : intersiteRouteTree) {
				Connection conn = rt.getConnection();

				// We only need to write instructions for PIPs
				if (conn != null && conn.isPip()) {
					Wire source = conn.getSourceWire();
					Tile sourceTile = source.getTile();
					Wire sink = conn.getSinkWire();
					TileType tileType = sourceTile.getType();
					TileType hclkR = TileType.valueOf(family, "HCLK_R");
					TileType hclkL = TileType.valueOf(family, "HCLK_L");

					// A net may be a partition pin clock net, but not a true clock net. For example, an RM might
					// not use a global clock net and will instead insert a LUT1 buffer for it. In this case, the
					// HCLK buffer should still be enabled, even though the net is terminating on a LUT that drives
					// nothing else.
					if ((net.isClkNet() || net.isPartPinCLKNet())
							&& (tileType.equals(hclkR) || tileType.equals(hclkL))) {
						// Add the HCLK Enable Buffer wires to a set to avoid writing their instructions multiple times
						hclkBufferEnableWires.add(source);
					}

					if (!isPseudoPip(source, sink)) {
						fileout.write(sourceTile.getName() + "." + sink.getName() + " " + source.getName() + "\n");
					}
				}
			}
		}

		// Turn the HCLK Enable Buffers on
		// Assuming that no other net is using these wires (which they shouldn't)
		if (net.isClkNet() || net.isPartPinCLKNet()) {
			for (Wire source : hclkBufferEnableWires) {
				// Turn the HCLK Enable Buffer on
				fileout.write(source.getTile().getName() + ".ENABLE_BUFFER." + source.getName() + " 1\n");
			}
		}
	}

	/**
	 * Writes FASM instructions for all enabled interconnect PIPs in the design.
	 * @throws IOException
	 */
	public void writeDesignPips() throws IOException {
		// Get inter-site nets with loads
		Collection<CellNet> nets = design.getNets().stream()
				.filter(cellNet -> !cellNet.isIntrasite())
				.filter(cellNet -> cellNet.getSinkPins().size() > 0) // Don't include nets with no loads
				.collect(Collectors.toList());

		for (CellNet net : nets) {
			if (!net.getRouteStatus().equals(RouteStatus.FULLY_ROUTED)) {
				System.err.println("WARNING: Net " + net.getName() + " is not fully routed. No FASM instructions for this " +
						"net will be written and the design will not function correctly!");
			}
			writeNetPips(net);
		}
	}
}
