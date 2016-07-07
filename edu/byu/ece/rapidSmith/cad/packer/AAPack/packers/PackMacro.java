package edu.byu.ece.rapidSmith.cad.packer.AAPack.packers;

import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.LibraryMacro;
import edu.byu.ece.rapidSmith.device.Bel;

import java.util.Map;

/**
 *
 */
public abstract class PackMacro extends LibraryMacro {
	public PackMacro(String name) {
		super(name);
	}

	abstract Map<Cell, Bel> getRelocatedCells(Bel anchor);
}
