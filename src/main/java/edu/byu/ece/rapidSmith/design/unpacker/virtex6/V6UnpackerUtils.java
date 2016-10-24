package edu.byu.ece.rapidSmith.design.unpacker.virtex6;

import edu.byu.ece.rapidSmith.design.xdl.XdlDesign;
import edu.byu.ece.rapidSmith.design.xdl.XdlInstance;
import edu.byu.ece.rapidSmith.design.unpacker.UnpackerUtils;

/**
 *
 */
public class V6UnpackerUtils implements UnpackerUtils {
	private XdlInstance staticSourceInst;

	@Override
	public XdlDesign prepareForUnpacker(XdlDesign design) {
		// TODO return a new design
		// Design prepped = design.deepCopy();
		for (XdlInstance inst : design.getInstances()) {
			switch (inst.getType()) {
				case SLICEL :
				case SLICEM :
					detailStaticSlice(inst, true);
					detailStaticSlice(inst, false);
					break;
				case TIEOFF :
					detailStaticTieoff(inst, true);
					detailStaticTieoff(inst, false);
					break;
			}
		}

		return design;
	}

	private void detailStaticSlice(XdlInstance inst, boolean isVcc) {
		String attrName = isVcc ? "_VCC_SOURCE" : "_GND_SOURCE";
		if (!inst.hasAttribute(attrName))
			return;

		String source = inst.getAttributeValue(attrName);
		assert source.length() == 1;
		inst.removeAttribute(attrName);
		inst.removeAttribute("_NO_USER_LOGIC");
		String belName = source + "6LUT";
		String cellName = inst.getName() + "__" + (isVcc ? "VCCSOURCE" : "GNDSOURCE");
		String cellConfig = "#LUT:O6=" + (isVcc ? "1" : "0");
		inst.addAttribute(belName, cellName, cellConfig);
		inst.addAttribute(source + "USED", "", "0");
	}

	private void detailStaticTieoff(XdlInstance inst, boolean isVcc) {
		String attrName = isVcc ? "_VCC_SOURCE" : "_GND_SOURCE";
		if (!inst.hasAttribute(attrName))
			return;

		inst.removeAttribute(attrName);
		inst.removeAttribute("_NO_USER_LOGIC");
		String belName = isVcc ? "HARD1VCC" : "HARD0GND";
		String cellName = inst.getName() + "__" + (isVcc ? "VCCSOURCE" : "GNDSOURCE");
		inst.addAttribute(belName, cellName, "");
	}
}
