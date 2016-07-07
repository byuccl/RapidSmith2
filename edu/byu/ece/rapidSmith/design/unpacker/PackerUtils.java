package edu.byu.ece.rapidSmith.design.unpacker;

import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;

/**
 *
 */
public interface PackerUtils {
	void prepare(CellDesign design);

	void finish(Design design);
}
