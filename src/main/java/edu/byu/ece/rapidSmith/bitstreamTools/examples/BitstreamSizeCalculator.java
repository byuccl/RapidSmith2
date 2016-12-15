/*
 * Copyright (c) 2016 Brigham Young University
 *
 * This file is part of the BYU RapidSmith Tools.
 *
 * BYU RapidSmith Tools is free software: you may redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * BYU RapidSmith Tools is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * A copy of the GNU General Public License is included with the BYU
 * RapidSmith Tools. It can be found at doc/LICENSE.GPL3.TXT. You may
 * also get a copy of the license at <http://www.gnu.org/licenses/>.
 */
package edu.byu.ece.rapidSmith.bitstreamTools.examples;

import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.BlockSubType;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.BlockType;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.DeviceLookup;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

public class BitstreamSizeCalculator {

	public static void main(String[] args) {
		
		if (args.length < 1) {
			System.err.println("usage: <executable> <part name> [-noBRAMContent]\n");
			DeviceLookup.printAvailableParts(System.err);
			System.exit(1);
		}

		String partname = args[0];
		boolean ignoreBRAMContent = false;
		if (args.length > 1 && args[1].equalsIgnoreCase("-noBRAMContent")) {
			ignoreBRAMContent = true;
		}

		XilinxConfigurationSpecification spec = DeviceLookup.lookupPartV4V5V6(partname);
		if (spec == null) {
			DeviceLookup.printAvailableParts(System.err);
			System.exit(1);
		}

		int frameSize = spec.getFrameSize();
		int numTopRows = spec.getTopNumberOfRows();
		int numBottomRows = spec.getBottomNumberOfRows();
		
		BlockType bramContentType = spec.getBRAMContentBlockType();
		
		
		int frameCount = 0;
		for (BlockType blockType : spec.getBlockTypes()) {
			if (ignoreBRAMContent && blockType == bramContentType) {
				continue;
			}
			for (BlockSubType blockSubType : spec.getBlockSubTypeLayout(blockType)) {
				//System.out.println(blockSubType + " " + blockSubType.getFramesPerConfigurationBlock());
				frameCount += blockSubType.getFramesPerConfigurationBlock();
			}
		}
		
		int result = 32 * /* 32 bits per word */
					 frameSize * /* Number of words in a frame */ 		
					 (numTopRows + numBottomRows) * /* Number of rows */
					 frameCount; /* Number of frames in the columns of one row for all block types */
		
		System.out.println(result + " bits required to configure " + spec.getDeviceName());
		if (ignoreBRAMContent) {
			System.out.println("(without BRAM content bits)");
		}
	}
}
