package edu.byu.ece.rapidSmith.design.unpacker.virtex6;

import edu.byu.ece.rapidSmith.design.xdl.XdlAttribute;
import edu.byu.ece.rapidSmith.design.xdl.XdlInstance;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.design.subsite.LibraryCell;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.design.subsite.PropertyType;
import edu.byu.ece.rapidSmith.design.unpacker.CellCreator;
import edu.byu.ece.rapidSmith.design.unpacker.CellCreatorFactory;
import edu.byu.ece.rapidSmith.design.unpacker.DefaultCellCreator;
import edu.byu.ece.rapidSmith.device.BelId;

import java.util.*;

/**
 *
 */
public class RegInitCreatorFactory implements CellCreatorFactory {
	private final LibraryCell FF_INIT;
	private final LibraryCell REG_INIT;

	private BelId id;
	private Map<String, String> attributes = new HashMap<>();

	public RegInitCreatorFactory(BelId id, CellLibrary cellLibrary) {
		this.id = id;
		FF_INIT = cellLibrary.get("FF_INIT");
		REG_INIT = cellLibrary.get("REG_INIT");
		attributes.put(id.getName() + "INIT", "INIT");
		attributes.put(id.getName() + "SR", "SR");
		attributes.put("CLKINV", "CLKINV");
	}

	@Override
	public CellCreator build(XdlInstance instance) {
		return new RegInitCreator(id, instance);
	}

	private class RegInitCreator extends DefaultCellCreator {
		private LibraryCell cellType;
		public RegInitCreator(BelId id, XdlInstance inst) {
			setIdentifier(id);
			setInstance(inst);
			setAttributes(attributes);

			XdlAttribute belAttr = inst.getAttribute(getIdentifier().getName());
			if (id.getName().contains("5")) {
				cellType = FF_INIT;
			} else {
				if (belAttr.getValue().equals("#FF"))
					cellType = FF_INIT;
				else
					cellType = REG_INIT;
			}
		}

		@Override
		protected LibraryCell getCellType() {
			return cellType;
		}

		@Override
		protected Collection<Property> getProperties() {
			List<Property> properties = new ArrayList<>();
			XdlInstance inst = getInstance();
			for (XdlAttribute attr : inst.getAttributes()) {
				if (attributes.containsKey(attr.getPhysicalName())) {
					Property property = new Property(
							attributes.get(attr.getPhysicalName()),
							PropertyType.DESIGN,
							attr.getValue());
					properties.add(property);
				}
			}

			// Add the SYNC_ATTR attribute only if the reset is used
			String srUsedMux = inst.getAttributeValue("SRUSEDMUX");
			if (srUsedMux != null && !srUsedMux.equals("#OFF")) {
				if (inst.hasAttribute("SYNC_ATTR")) {
					String value = inst.getAttributeValue("SYNC_ATTR");
					Property property = new Property(
							"SYNC_ATTR",
							PropertyType.DESIGN,
							value);
					properties.add(property);
				}
			}

			String cfgValue = inst.getAttributeValue(getIdentifier().getName());
			if (cellType == FF_INIT)
				cfgValue = "";
			properties.add(new Property(getCellType().getName(), PropertyType.DESIGN, cfgValue));

			return properties;
		}
	}
}
