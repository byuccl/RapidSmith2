package edu.byu.ece.rapidSmith.design.unpacker;

import edu.byu.ece.rapidSmith.design.Instance;
import edu.byu.ece.rapidSmith.design.subsite.LibraryCell;
import edu.byu.ece.rapidSmith.device.BelId;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class DefaultCellCreatorFactory implements CellCreatorFactory {
	private LibraryCell cellType;
	private BelId id;
	private Map<String, String> pinMap = new HashMap<>();
	private Map<String, String> attributes = new HashMap<>();

	@Override
	public CellCreator build(Instance inst) {
		DefaultCellCreator cellCreator = new DefaultCellCreator();
		cellCreator.setCellType(cellType);
		cellCreator.setPinMap(pinMap);
		cellCreator.setAttributes(attributes);
		cellCreator.setInstance(inst);
		cellCreator.setIdentifier(id);
		return cellCreator;
	}

	public void setCellType(LibraryCell cellType) {
		this.cellType = cellType;
	}

	public void setBelId(BelId id) {
		this.id = id;
	}

	public void addAttribute(String name, String rename) {
		attributes.put(name, rename);
	}

	public void addPinMap(String pinName, String pinRename) {
		pinMap.put(pinName, pinRename);
	}
}
