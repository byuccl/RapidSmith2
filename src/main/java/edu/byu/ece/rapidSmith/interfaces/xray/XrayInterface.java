package edu.byu.ece.rapidSmith.interfaces.xray;

import edu.byu.ece.rapidSmith.design.subsite.BelRoutethrough;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.ImplementationMode;
import edu.byu.ece.rapidSmith.design.subsite.RouteStringTree;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.PIP;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.util.Exceptions;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to interface RapidSmith2 and Project X-Ray.
 * Specifically, it is used to write FASM files for RapidSmith2 {@link CellDesign}s.
 * Currently, this class only supports reconfigurable module (RM) designs.
 *
 * Various improvements can be made, including:
 * 1. Updating to use the most current FASM file format used in the Project X-Ray tools.
 * 2. Adding support for additional primitives and properties, including:
 *    a. WA7USED, WA8USED (not sure if there is really anything to do here)
 *    b. Permanent Latches
 *    c. BRAMs (focusing on BRAMs supported by Yosys)
 *    d. I/O
 * 3. Support for full-device designs.
 */
public final class XrayInterface {
	private final Map<Bel, BelRoutethrough> belRoutethroughMap;
	private final CellDesign design;
	private final Device device;
	private final Set<Bel> gndSourceBels;
	private final Set<Bel> vccSourceBels;
	private final Collection<PIP> staticPips;

	public XrayInterface(CellDesign design, Device device, Map<Bel, BelRoutethrough> belRoutethroughMap, Set<Bel> vccSourceBels, Set<Bel> gndSourceBels, Collection<PIP> staticPips) {
		this.design = design;
		this.device = device;
		this.belRoutethroughMap = belRoutethroughMap;
		this.gndSourceBels = gndSourceBels;
		this.vccSourceBels = vccSourceBels;
		this.staticPips = staticPips;
	}

	/**
	 * Writes an FPGA Assembly (FASM) file for the design. This FASM file can be used as an input into the
	 * Project X-Ray tools.
	 * @param fasmPath path to write the FASM file to.
	 * @throws IOException
	 */
	public void writeFASM(String fasmPath) throws IOException {
		if (!design.getImplementationMode().equals(ImplementationMode.RECONFIG_MODULE)) {
			throw new Exceptions.ImportExportException("Invalid implementation mode for FASM export. Only RECONFIG_MODULE is currently supported.");
		}

		// Create a BufferedWriter to use across the entire FASM writing process
		BufferedWriter fileout = new BufferedWriter(new FileWriter(fasmPath));

		// Write the static resources
		FasmStaticInterface fasmStaticInterface = new FasmStaticInterface(device, design, staticPips, fileout);
		fasmStaticInterface.writeStaticDesignPips();

		// Write properties for all used logic BELs
		FasmBelInterface fasmBelInterface = new FasmBelInterface(device, design, belRoutethroughMap, vccSourceBels, gndSourceBels, fileout);
		fasmBelInterface.processBels();
		Map<Site, Set<String>> sharedSiteProperties = fasmBelInterface.getSharedSiteProperties();

		// Write common site properties  and site PIPs (intra-site routing)
		FasmSiteInterface fasmSiteInterface = new FasmSiteInterface(device, design, fileout);
		fasmSiteInterface.writeSiteProperties(sharedSiteProperties);

		// Write the design's inter-site routing PIPs
		FasmRoutingInterface fasmRoutingInterface = new FasmRoutingInterface(device, design, fileout);
		fasmRoutingInterface.writeDesignPips();

		fileout.close();
	}

}
