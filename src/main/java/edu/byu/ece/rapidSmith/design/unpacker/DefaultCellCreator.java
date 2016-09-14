package edu.byu.ece.rapidSmith.design.unpacker;

import edu.byu.ece.rapidSmith.design.xdl.Attribute;
import edu.byu.ece.rapidSmith.design.xdl.Instance;
import edu.byu.ece.rapidSmith.design.subsite.LibraryCell;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.design.subsite.PropertyType;
import edu.byu.ece.rapidSmith.device.BelId;

import java.util.*;

/**
*
*/
public class DefaultCellCreator extends CellCreator {
	private BelId id;
	private LibraryCell cellType;
	private Instance instance;

	private Map<String, String> pinMap;
	private Map<String, String> attributes;

	protected void setIdentifier(BelId id) {
		this.id = id;
	}

	protected void setCellType(LibraryCell cellType) {
		this.cellType = cellType;
	}

	protected void setPinMap(Map<String, String> pinMap) {
		this.pinMap = pinMap;
	}

	protected void setAttributes(Map<String, String> directAttributes) {
		this.attributes = directAttributes;
	}

	protected void setInstance(Instance instance) {
		this.instance = instance;
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
	public Instance getInstance() {
		return instance;
	}

	@Override
	protected Collection<Property> getProperties() {
		List<Property> properties = new ArrayList<>();
		for (Attribute attr : instance.getAttributes()) {
			if (attributes.containsKey(attr.getPhysicalName())) {
				Property property = new Property(
						attributes.get(attr.getPhysicalName()),
						PropertyType.DESIGN,
						attr.getValue());
				properties.add(property);
			}
		}
		String cfgValue = instance.getAttributeValue(id.getName());
		properties.add(new Property(getCellType().getName(), PropertyType.DESIGN, cfgValue));

		return properties;
	}

	@Override
	protected String getCellPin(String belPin) {
		if (pinMap == null || !pinMap.containsKey(belPin)) {
			return belPin;
		}
		return pinMap.get(belPin);
	}
}
