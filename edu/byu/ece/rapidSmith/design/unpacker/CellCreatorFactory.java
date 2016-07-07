package edu.byu.ece.rapidSmith.design.unpacker;

import edu.byu.ece.rapidSmith.design.Instance;
import edu.byu.ece.rapidSmith.device.BelId;

/**
 *
 */
public interface CellCreatorFactory {
	CellCreator build(Instance instance);
}
