package edu.byu.ece.rapidSmith.design.unpacker;

import edu.byu.ece.rapidSmith.design.xdl.Instance;

/**
 *
 */
public interface CellCreatorFactory {
	CellCreator build(Instance instance);
}
