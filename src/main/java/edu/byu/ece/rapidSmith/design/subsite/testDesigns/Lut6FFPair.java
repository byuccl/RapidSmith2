package edu.byu.ece.rapidSmith.design.subsite.testDesigns;

import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.Device;

/**
 *
 */
public class Lut6FFPair extends CellDesign {
	public Lut6FFPair(Device part, CellLibrary cellLibrary) {
		this(part.getPartName(), cellLibrary);
	}

	public Lut6FFPair(String partName, CellLibrary cellLibrary) {
		super("LUT_FF_PAIR", partName);

		build(cellLibrary);
	}

	private void build(CellLibrary cellLibrary) {
		LibraryCell LUT6 = cellLibrary.get("LUT6");
		LibraryCell FF_INIT = cellLibrary.get("FF_INIT");

		Cell lut6Cell = addCell(new Cell("lut6", LUT6));
		Cell ffCell = addCell(new Cell("ff", FF_INIT));
		CellNet cellNet = addNet(new CellNet("net", NetType.WIRE));
		cellNet.connectToPin(lut6Cell.getPin("O6"));
		cellNet.connectToPin(ffCell.getPin("D"));
	}
}
