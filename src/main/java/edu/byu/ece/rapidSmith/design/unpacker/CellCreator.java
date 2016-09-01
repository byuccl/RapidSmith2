package edu.byu.ece.rapidSmith.design.unpacker;

import edu.byu.ece.rapidSmith.design.Instance;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.LibraryCell;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.device.BelId;

import java.util.Collection;
import java.util.Map;

/**
*
*/
public abstract class CellCreator {
	public Cell createCell(CellDesign design) {
		BelId identifier = getIdentifier();
		// XDL does not like : characters in attributes
		String belName = getInstance().getAttribute(identifier.getName()).getLogicalName();
		String name = design.getUniqueCellName(belName);
		LibraryCell libCell = getCellType();
		Cell cell = new Cell(name, libCell);
		cell.updateProperties(getProperties());

		return cell;
	}

	protected abstract Instance getInstance();
	protected abstract BelId getIdentifier();
	protected abstract LibraryCell getCellType();
	protected abstract Collection<Property> getProperties();
	protected abstract String getCellPin(String belPin);
}
