package edu.byu.ece.rapidSmith.interfaces.xray;

import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.families.FamilyInfo;
import edu.byu.ece.rapidSmith.device.families.FamilyInfos;
import edu.byu.ece.rapidSmith.util.Exceptions;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is used for writing the common site property instructions to a FASM file.
 * This includes Site PIPs (intrasite routing), control sets, etc.
 *
 * @author Dallon Glick
 *
 */
public class FasmSiteInterface extends AbstractFasmInterface {
	private final CellDesign design;
	private final BufferedWriter fileout;
	private FamilyInfo familyInfo;

	public FasmSiteInterface(Device device, CellDesign design, BufferedWriter fileout) {
		super(device, design);
		this.design = design;
		this.fileout = fileout;
		familyInfo = FamilyInfos.get(device.getFamily());
	}

	/**
	 * Writes the site properties for a FASM file, including properties shared within sites
	 * and site pips (intra-site routing).
	 * @param sharedSiteProperties a map from sites to sets of shared properties
	 * @throws IOException
	 */
	void writeSiteProperties(Map<Site, Set<String>> sharedSiteProperties) throws IOException {
		writeSharedSiteProperties(sharedSiteProperties);
		writeSitePips();
	}

	/**
	 * Writes the shared site properties (FFSYNC, LATCH, etc.) for a FASM file, using sharedSiteProperties
	 * @param sharedSiteProperties a map from sites to sets of shared properties
	 * @throws IOException
	 */
	private void writeSharedSiteProperties(Map<Site, Set<String>> sharedSiteProperties) throws IOException {
		for (Map.Entry<Site, Set<String>> entry : sharedSiteProperties.entrySet()) {
			Site site = entry.getKey();
			Set<String> properties = entry.getValue();
			String localSliceName = getRelativeSliceName(site);

			fileout.write("# Site " + site.getName() + " properties:\n");
			for (String property : properties) {
				fileout.write(site.getTile() + "." + localSliceName + "." + property + " 1\n");
			}
		}
	}

	/**
	 * Writes the site PIP (routing mux) instructions for a site.
	 * This configures the intra-site routing.
	 * TODO: Support sites besides slices.
	 * @throws IOException
	 */
	private void writeSitePips() throws IOException {
		Map<Site, Map<String, String>> pipInValues = design.getPipInValues();

		Set<Map.Entry<Site, Map<String, String>>> pipInValueEntries = pipInValues.entrySet().stream()
				.filter(entry -> !entry.getValue().isEmpty())
				.collect(Collectors.toSet());

		for (Map.Entry<Site, Map<String, String>> entry : pipInValueEntries) {
			Site site = entry.getKey();
			fileout.write("# Site " + site.getName() + " PIPs:\n");

			if (!familyInfo.sliceSites().contains(site.getType())) {
				throw new Exceptions.ImportExportException("Writing site PIP FASM instructions for non slice-type site " + site.getType().name() + " not supported");
			}

			String localSliceName = getRelativeSliceName(site);

			for (Map.Entry<String, String> pipEntry : entry.getValue().entrySet()) {
				String bel = pipEntry.getKey();
				String val = pipEntry.getValue();

				switch (bel) {
					case "PRECYINIT":
						switch (val) {
							case "1":
							case "AX":
								fileout.write(site.getTile() + "." + localSliceName + "." + bel + "." + val + " 1\n");
								break;
							case "0":
								break;
							case "CIN":
							default:
								fileout.write(site.getTile() + "." + localSliceName + "." + bel + ".CIN" + " 1\n");
								break;
						}
						break;
					case "CEUSEDMUX":
						// Configure ability to drive clock enable (CE) or always enable clock
						switch (val) {
							case "IN":
								// Controlled (CE = mywire)
								fileout.write(site.getTile() + "." + localSliceName + "." + bel + " 1\n");
								break;
							case "1":
							default:
								break;
						}
						break;
					case "SRUSEDMUX":
						// Configure ability to reset FF after GSR (global set reset)
						// (SR input for FFs)
						switch (val) {
							case "IN":
								// Controlled (R = mywire)
								fileout.write(site.getTile() + "." + localSliceName + "." + bel + " 1\n");
								break;
							case "0":
							default:
								break;
						}
						break;
					case "WEMUX":
						switch (val) {
							case "CE":
								fileout.write(site.getTile() + "." + localSliceName + "." + bel + "." + val + " 1\n");
								break;
							case "WE":
							default:
								break;
						}
						break;
					case "ACY0":
					case "BCY0":
					case "CCY0":
					case "DCY0":
						if ("O5".equals(val)) {
							fileout.write(site.getTile() + "." + localSliceName + "." + "CARRY4." + bel + " 1\n");
						}
						// else AX, BX, CX, DX
						break;
					case "ADI1MUX":
					case "BDI1MUX":
					case "CDI1MUX":
						switch (val) {
							case "AI":
							case "BI":
							case "CI":
								fileout.write(site.getTile() + "." + localSliceName + "." + bel + "." + val + " 1\n");
								break;
							default: // BMC31, CMC31, DMC31, DI
								break;
						}
						break;
					// If the key in the database has multiple entries (bits) to set:
					case "AOUTMUX":
					case "BOUTMUX":
					case "COUTMUX":
					case "DOUTMUX":
					case "AFFMUX":
					case "BFFMUX":
					case "CFFMUX":
					case "DFFMUX":
						fileout.write(site.getTile() + "." + localSliceName + "." + bel + " " + val + "\n");
						break;
					// No corresponding bit patterns
					case "COUTUSED":
					case "DUSED":
					case "CUSED":
					case "BUSED":
					case "AUSED":
						break;
					// Site-wide polarity selectors are contained in sharedSiteProperties
					case "CLKINV":
						break;
					default:
						fileout.write(site.getTile() + "." + localSliceName + "." + bel + "." + val + " 1\n");
						break;
				}
			}
		}
	}
}
