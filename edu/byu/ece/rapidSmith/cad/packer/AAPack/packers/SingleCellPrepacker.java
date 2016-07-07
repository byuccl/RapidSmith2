package edu.byu.ece.rapidSmith.cad.packer.AAPack.packers;

import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.*;
import edu.byu.ece.rapidSmith.design.subsite.Cell;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static edu.byu.ece.rapidSmith.cad.packer.AAPack.packers.PrepackerUtils.replaceCellWith;

/**
 *
 */
public class SingleCellPrepacker implements Prepacker {
	public SingleCellPrepacker() { }

	@Override
	public void prepack(Collection<Cell> cells) {
		for (Cell cell : cells) {
			if (!(cell instanceof PackCell)) {
				PackCell packCell = new PackCell(cell);
				replaceCellWith(cell, packCell, cell.getDesign());
				if (ignoredTypes.contains(cell.getLibCell().getName()))
					packCell.setPackable(false);
				else
					packCell.setPackable(true);
			}
		}
	}

	private static final Set<String> ignoredTypes = new HashSet<>();
	static {
		ignoredTypes.add("BUFG");
		ignoredTypes.add("PAD");
		ignoredTypes.add("IOB_OUTBUF");
		ignoredTypes.add("IOB_INBUF");
		ignoredTypes.add("IOBM_OUTBUF");
		ignoredTypes.add("PULL_OR_KEEP1");
		ignoredTypes.add("IDDR_FF");
		ignoredTypes.add("ODDR_FF");
	}
}
