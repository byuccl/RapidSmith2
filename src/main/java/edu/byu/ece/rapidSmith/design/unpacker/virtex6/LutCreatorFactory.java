package edu.byu.ece.rapidSmith.design.unpacker.virtex6;

import edu.byu.ece.rapidSmith.design.xdl.Attribute;
import edu.byu.ece.rapidSmith.design.xdl.Instance;
import edu.byu.ece.rapidSmith.design.xdl.Net;
import edu.byu.ece.rapidSmith.design.xdl.Pin;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.design.unpacker.CellCreator;
import edu.byu.ece.rapidSmith.design.unpacker.CellCreatorFactory;
import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.util.MessageGenerator;
import edu.byu.ece.rapidSmith.util.luts.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class LutCreatorFactory implements CellCreatorFactory {
	private final LibraryCell LUT5;
	private final LibraryCell LUT6;
	private final LibraryCell SRL32;
	private final LibraryCell SRL16;
	private final LibraryCell SPRAM32;
	private final LibraryCell SPRAM64;
	private final LibraryCell DPRAM32;
	private final LibraryCell DPRAM64;
	private final LibraryCell GND;
	private final LibraryCell VCC;

	private static final Pattern RAM_NAME_PATTERN =
			Pattern.compile("(.*?\\w+?)(/[A-D]|/HIGH|/LOW|" +
					"/DP(\\.LOW|\\.HIGH)?|/SP(\\.LOW|\\.HIGH)?|" +
					"_RAM[A-D](_D1)?|)");

	private BelId id;

	public LutCreatorFactory(BelId id, CellLibrary cellLibrary) {
		LUT5 = cellLibrary.get("LUT5");
		LUT6 = cellLibrary.get("LUT6");
		SRL32 = cellLibrary.get("SRL32");
		SRL16 = cellLibrary.get("SRL16");
		SPRAM32 = cellLibrary.get("SPRAM32");
		SPRAM64 = cellLibrary.get("SPRAM64");
		DPRAM32 = cellLibrary.get("DPRAM32");
		DPRAM64 = cellLibrary.get("DPRAM64");
		GND = cellLibrary.get("HARD0");
		VCC = cellLibrary.get("HARD1");

		this.id = id;
	}

	@Override
	public CellCreator build(Instance instance) {
		return new LutCreator(instance, id);
	}
	private class LutCreator extends CellCreator {
		private BelId id;
		private Instance inst;
		private LibraryCell cellType;
		private List<Property> properties;

		private Map<String, String> cellPinMap = new HashMap<>();

		public LutCreator(Instance inst, BelId id) {
			this.id = id;
			this.inst = inst;
			buildLutProperties();
		}

		private void buildLutProperties() {
			properties = new ArrayList<>();

			String LE = "" + id.getName().charAt(0);
			Attribute belAttr = inst.getAttribute(id.getName());
			LutConfig lutCfg = new LutConfig(belAttr.getValue());
			lutCfg.reduce();
			int minNumberOfInputs = lutCfg.getMinNumOfInputs();

			String belOutput = id.getName().contains("5") ? "O5" : "O6";

			if (lutCfg.isStaticSource()) {
				if (lutCfg.isGndSource()) {
					cellType = GND;
					cellPinMap.put(belOutput, "0");
				} else {
					assert lutCfg.isVccSource();
					cellType = VCC;
					cellPinMap.put(belOutput, "1");
				}
				Property property = new Property(cellType.getName(), PropertyType.DESIGN, "");
				properties.add(property);
			} else if (lutCfg.getOperatingMode().equals("LUT")) {
				if (minNumberOfInputs <= 5)
					cellType = LUT5;
				else
					cellType = LUT6;

				int numUsedPins = lutCfg.getNumUsedInputs();
				boolean sharedLut = isSharedLut(LE, inst);
				remapPins(lutCfg, sharedLut);
				if (numUsedPins != lutCfg.getNumUsedInputs())
					MessageGenerator.briefMessage("Required pins less than used pins - " +
							"design may be different though functionally equivalent");
				Property property = new Property(
						cellType.getName(), PropertyType.DESIGN, lutCfg);
				properties.add(property);
				String cellOutput = cellType == LUT5 ? "O5" : "O6";
				cellPinMap.put(belOutput, cellOutput);
			} else {
				assert !lutCfg.getOperatingMode().equals("ROM") : "I can't find any of these so I don't " +
						"know what to do with them";
				String ramModeName = id.getName().substring(0, 2) + "RAMMODE";
				String ramMode = inst.getAttributeValue(ramModeName);

				switch (ramMode) {
					case "SRL16":
						cellType = SRL16;
						cellPinMap.put("A1", null);
						for (int i = 2; i <= 5; i++)
							cellPinMap.put("A" + i, "A" + (i-1));
						cellPinMap.put("A6", null);
						if (id.getName().contains("5"))
							cellPinMap.put("DI1", "DI");
						else
							cellPinMap.put("DI2", "DI");
						remapSRL(4, lutCfg);
						break;
					case "SRL32":
						cellType = SRL32;
						cellPinMap.put("A1", null);
						for (int i = 2; i <= 6; i++)
							cellPinMap.put("A" + i, "A" + (i-1));
						cellPinMap.put("DI1", "DI");
						remapSRL(5, lutCfg);
						break;
					case "SPRAM32":
						cellType = SPRAM32;
						remapStaticsPins(inst, LE);
						if (id.getName().contains("5"))
							cellPinMap.put("DI1", "DI");
						else
							cellPinMap.put("DI2", "DI");
						break;
					case "SPRAM64": {
						int numNonStaticPins = countStaticPins(inst, LE);
						if (numNonStaticPins < 6) {
							cellType = SPRAM32;
							remapStaticsPins(inst, LE);
							ramMode = "SPRAM32";
						} else {
							cellType = SPRAM64;
							for (int i = 1; i <= 6; i++) {
								cellPinMap.put("A" + i, "A" + i);
								cellPinMap.put("WA" + i, "WA" + i);
							}
						}
						cellPinMap.put("DI1", "DI");
						break;
					}
					case "DPRAM32":
						cellType = DPRAM32;
						remapStaticsPins(inst, LE);
						if (id.getName().contains("5"))
							cellPinMap.put("DI1", "DI");
						else
							cellPinMap.put("DI2", "DI");
						break;
					case "DPRAM64": {
						int numStaticPins = countStaticPins(inst, LE);
						if (numStaticPins < 6) {
							cellType = DPRAM32;
							remapStaticsPins(inst, LE);
							ramMode = "DPRAM32";
						} else {
							cellType = DPRAM64;
							for (int i = 1; i <= 6; i++) {
								cellPinMap.put("A" + i, "A" + i);
								cellPinMap.put("WA" + i, "WA" + i);
							}
							for (int i = 7; i <= 8; i++)
								cellPinMap.put("WA" + i, "WA" + i);
						}
						cellPinMap.put("DI1", "DI");
						break;
					}
					default:
						throw new AssertionError("Illegal ram mode");
				}
				cellPinMap.put(belOutput, "O");

				switch (ramMode) {
					case "SPRAM32":
					case "DPRAM32":
					case "SPRAM64":
					case "DPRAM64":
						String cellName = inst.getAttribute(id.getName()).getLogicalName();
						Matcher m = RAM_NAME_PATTERN.matcher(cellName);
						if (!m.matches())
							System.out.println("Failed to match RAM name pattern " + cellName);
						properties.add(new Property("$RAMGROUP", PropertyType.USER, m.group(1)));

						String position;
						switch (m.group(2)) {
							case "/D":
								position = "D";
								break;
							case "/C":
								position = "C";
								break;
							case "/B":
								position = "B";
								break;
							case "/A":
								position = "A";
								break;
							case "/LOW":
							case "/SP.LOW":
								position = "BD";
								break;
							case "/DP.LOW":
								position = "B";
								break;
							case "/HIGH":
							case "/SP.HIGH":
							case "/DP.HIGH":
								position = "AC";
								break;
							case "/DP":
								position = "ABC";
								break;
							case "/SP":
							case "":
								position = "ABCD";
								break;
							case "_RAMD":
							case "_RAMC":
							case "_RAMB":
							case "_RAMA":
							case "_RAMD_D1":
							case "_RAMC_D1":
							case "_RAMB_D1":
							case "_RAMA_D1":
								position = "ABCD";
								break;
							default:
								System.out.println("Failed to match RAM name pattern");
								position = null;
						}
						properties.add(new Property("$RAMPOSITION", PropertyType.USER, position));
				}

				properties.add(new Property("RAMMODE", PropertyType.DESIGN, ramMode));

				// CLKINV attribute
				String value = inst.getAttributeValue("CLKINV");
				if (value != null)
					properties.add(new Property("CLKINV", PropertyType.DESIGN, value));

				Property property = new Property(
						cellType.getName(), PropertyType.DESIGN, lutCfg);
				properties.add(property);
			}
		}

		private boolean isSharedLut(String LE, Instance inst) {
			String lut6 = LE + "6LUT";
			String lut5 = LE + "5LUT";
			return inst.hasAttribute(lut5) && !inst.getAttributeValue(lut5).equals("#OFF") &&
					inst.hasAttribute(lut6) && !inst.getAttributeValue(lut6).equals("#OFF");
		}

		// Determines which BELPIN should map to each cell pin.
		private void remapPins(LutConfig lutCfg, boolean sharedLut) {
			int[] remap = new int[6];
			int j = 1;

			Set<Integer> usedPins = lutCfg.getUsedInputs();
			for (int i = 1; i <= 6; i++) {
				if (usedPins.contains(i)) {
					remap[i-1] = j;
					cellPinMap.put("A" +  i, "A" + j);
					j++;
				} else {
					cellPinMap.put("A" + i, null);
					// always increment the pin the to preserve compatibility between
					// luts
					if (sharedLut)
						j++;
				}
			}

			// only remap when we allow pins to be moved
			if (!sharedLut)
				lutCfg.remapPins(remap);
		}

		private void remapStaticsPins(Instance inst, String LE) {
			for (int i = 1; i <= 6; i++) {
				Pin apin = inst.getPin(LE + i);
				Net anet = apin != null ? apin.getNet() : null;
				Pin wapin = inst.getPin("A" + i);
				Net wanet = wapin != null ? wapin.getNet() : null;

				assert apin != null;
				assert wapin != null;
				assert anet != null;
				assert wanet != null;

				cellPinMap.put("A" +  i, "A" + i);
				cellPinMap.put("WA" +  i, "WA" + i);
			}
		}

		private void remapSRL(int numInputs, LutConfig lutCfg) {
			int[] remap = {0, 1, 2, 3, 4, 5};
			long value = lutCfg.getContents().getCopyOfInitString().getValue();
			value |=  (value & 0x5555555555555555L) << 1;
			if (numInputs == 4) {
				value = (value & 0x00000000FFFFFFFFL) | (value << 32);
			}
			lutCfg.setContents(new LutContents(new InitString(value)));
			lutCfg.remapPins(remap);
		}

		private int countStaticPins(Instance inst, String LE) {
			// Determine the number of non-statically sourced nets
			int nonStaticInputCount = 0;
			for (int i = 1; i <= 6 ; i++) {
				Pin pin = inst.getPin(LE + i);
				Net sourceNet = pin != null ? pin.getNet() : null;
				if (sourceNet == null)
					continue;
				if (!sourceNet.isStaticNet())
					nonStaticInputCount++;
			}

			if (nonStaticInputCount < 6) {
				// I don't know that we need to check the write enable pins,
				// but just to be safe, I will
				int waNonStaticsCount = 0;
				for (int i = 1; i <= 6; i++) {
					Pin pin = inst.getPin("A" + i);
					Net sourceNet = pin != null ? pin.getNet() : null;
					if (sourceNet == null)
						continue;
					if (!sourceNet.isStaticNet())
						waNonStaticsCount++;
				}
				if (waNonStaticsCount > nonStaticInputCount)
					nonStaticInputCount = waNonStaticsCount;
			}
			return nonStaticInputCount;
		}

		@Override
		protected Instance getInstance() {
			return inst;
		}

		@Override
		protected BelId getIdentifier() {
			return id;
		}

		@Override
		protected LibraryCell getCellType() {
			return cellType;
		}

		@Override
		protected Collection<Property> getProperties() {
			return properties;
		}

		@Override
		protected String getCellPin(String belPin) {
			if (cellPinMap.containsKey(belPin))
				return cellPinMap.get(belPin);
			return belPin;
		}
	}
}
