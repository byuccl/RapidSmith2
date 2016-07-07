package edu.byu.ece.rapidSmith.cad.packer.AAPack.packers;

import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class PrepackerUtils {
	public static void replaceCellWith(Cell oldCell, Cell newCell, CellDesign design) {
		Map<CellPin, CellNet> netsMap = new HashMap<>();

		for (CellPin oldPin : oldCell.getPins()) {
			if (oldPin.isConnectedToNet()) {
				CellNet net = oldPin.getNet();
				CellPin newPin = newCell.getPin(oldPin.getName());
				netsMap.put(newPin, net);
			}
		}

		design.removeCell(oldCell);

		design.addCell(newCell);
		netsMap.forEach((p, n) -> n.connectToPin(p));
	}
}
