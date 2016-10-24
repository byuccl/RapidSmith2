package edu.byu.ece.rapidSmith.design.unpacker;

import edu.byu.ece.rapidSmith.design.xdl.XdlDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;

/**
 *
 */
public interface PackerUtils {
	void prepare(CellDesign design);

	void finish(XdlDesign design);
}
