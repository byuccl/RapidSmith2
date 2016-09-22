package edu.byu.ece.rapidSmith.design.unpacker.virtex6;

import edu.byu.ece.rapidSmith.design.xdl.XdlInstance;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.design.subsite.LibraryCell;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.design.subsite.PropertyType;
import edu.byu.ece.rapidSmith.design.unpacker.CellCreator;
import edu.byu.ece.rapidSmith.design.unpacker.CellCreatorFactory;
import edu.byu.ece.rapidSmith.device.BelId;

import java.util.*;

/**
 *
 */
public class Carry4CreatorFactory implements CellCreatorFactory {
	private final LibraryCell CARRY4;

	private BelId id;

	public Carry4CreatorFactory(BelId id, CellLibrary cellLibrary) {
		this.id = id;
		CARRY4 = cellLibrary.get("CARRY4");
	}

	@Override
	public CellCreator build(XdlInstance instance) {
		return new Carry4Creator(instance, id);
	}

	private class Carry4Creator extends CellCreator {
		private BelId id;
		private XdlInstance inst;
		private List<Property> properties;
		private char highestUsed;

		private Map<String, String> cellPinMap = new HashMap<>();

		public Carry4Creator(XdlInstance inst, BelId id) {
			this.id = id;
			this.inst = inst;
			buildLutProperties();
			highestUsed = getHighestUsed();
			buildCellPinMap();
		}

		private void buildLutProperties() {
			properties = new ArrayList<>();
			String value = inst.getAttributeValue(CARRY4.getName());
			Property property = new Property(
					CARRY4.getName(), PropertyType.DESIGN, value);
			properties.add(property);
		}

		private char getHighestUsed() {
			if (inst.testAttributeValue("COUTUSED", "0"))
				return 'D';
			if (inst.testAttributeValue("DOUTMUX", "CY"))
				return 'D';
			if (inst.testAttributeValue("DOUTMUX", "XOR"))
				return 'D';
			if (inst.testAttributeValue("DFFMUX", "CY"))
				return 'D';
			if (inst.testAttributeValue("DFFMUX", "XOR"))
				return 'D';
			if (inst.testAttributeValue("COUTMUX", "CY"))
				return 'C';
			if (inst.testAttributeValue("COUTMUX", "XOR"))
				return 'C';
			if (inst.testAttributeValue("CFFMUX", "CY"))
				return 'C';
			if (inst.testAttributeValue("CFFMUX", "XOR"))
				return 'C';
			if (inst.testAttributeValue("BOUTMUX", "CY"))
				return 'B';
			if (inst.testAttributeValue("BOUTMUX", "XOR"))
				return 'B';
			if (inst.testAttributeValue("BFFMUX", "CY"))
				return 'B';
			if (inst.testAttributeValue("BFFMUX", "XOR"))
				return 'B';
			return 'A';
		}

		private void buildCellPinMap() {
			cellPinMap.put("CYINIT", "CYINIT");
			cellPinMap.put("CIN", "CIN");
			cellPinMap.put("DI0", "DI0");
			cellPinMap.put("S0", "S0");
			cellPinMap.put("O0", "O0");
			cellPinMap.put("CO0", "CO0");

			if (highestUsed < 'B')
				return;
			cellPinMap.put("DI1", "DI1");
			cellPinMap.put("S1", "S1");
			cellPinMap.put("O1", "O1");
			cellPinMap.put("CO1", "CO1");

			if (highestUsed < 'C')
				return;
			cellPinMap.put("DI2", "DI2");
			cellPinMap.put("S2", "S2");
			cellPinMap.put("O2", "O2");
			cellPinMap.put("CO2", "CO2");

			if (highestUsed < 'D')
				return;
			cellPinMap.put("DI3", "DI3");
			cellPinMap.put("S3", "S3");
			cellPinMap.put("O3", "O3");
			cellPinMap.put("CO3", "CO3");
		}

		@Override
		protected XdlInstance getInstance() {
			return inst;
		}

		@Override
		protected BelId getIdentifier() {
			return id;
		}

		@Override
		protected LibraryCell getCellType() {
			return CARRY4;
		}

		@Override
		protected Collection<Property> getProperties() {
			return properties;
		}

		@Override
		protected String getCellPin(String belPin) {
			if (cellPinMap.containsKey(belPin))
				return cellPinMap.get(belPin);
			return null;
		}
	}
}
