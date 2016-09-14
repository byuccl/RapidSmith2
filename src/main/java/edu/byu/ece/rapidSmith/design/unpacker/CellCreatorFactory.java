package edu.byu.ece.rapidSmith.design.unpacker;

import edu.byu.ece.rapidSmith.design.xdl.XdlInstance;

/**
 *
 */
public interface CellCreatorFactory {
	CellCreator build(XdlInstance instance);
}
