package edu.byu.ece.rapidSmith.cad.packer.AAPack;

import edu.byu.ece.rapidSmith.design.subsite.Cell;

import java.util.Collection;
import java.util.List;

/**
 *
 */
public interface Prepacker {
	void prepack(Collection<Cell> cells);
}
