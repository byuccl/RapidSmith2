/*
 * Copyright (c) 2010-2011 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.bitstreamTools.configuration;


/**
 * Class for performing logical masking operations on FPGA objects.
 * 
 * TODO: move to a different package. This doesn't belong in the base configuration package.
 * 
 */
public class FPGAOperation {

	public enum OPERATORS { XOR, AND, OR, NOT, MASK }

	public static void ANDoperation(FPGA fpga1, FPGA fpga2) {
		operation(fpga1,fpga2, OPERATORS.AND);
	}
	
	public static void XORoperation(FPGA fpga1, FPGA fpga2) {
		operation(fpga1,fpga2, OPERATORS.XOR);
	}
	
	public static void ORoperation(FPGA fpga1, FPGA fpga2) {
		operation(fpga1,fpga2, OPERATORS.OR);
	}

	public static void MASKoperation(FPGA fpga1, FPGA fpga2) {
		operation(fpga1,fpga2, OPERATORS.MASK);
	}

	public static void NOToperation(FPGA fpga1) {
		operation(fpga1, null, OPERATORS.NOT);
	}
	
	public static void operation(FPGA fpga1, FPGA fpga2, OPERATORS op) {
		
		// Set FAR address of both FPGAs
		fpga1.setFAR(0);
		if (fpga2 != null)
			fpga2.setFAR(0);

		Frame f = null;
		do {
			f = fpga1.getCurrentFrame();
			FrameData fd = f.getData();

			Frame g = null;
			FrameData gd = null;
			
			if (fpga2 != null) {
				g = fpga2.getCurrentFrame();
				gd = g.getData();
			}
			
			// TODO: look at this: we may be making bitstreams that are
			// unnecessarily too large
			//if (g.isConfigured()) {
				
				
				switch(op) {
				case XOR:
					fd.XORDATA(gd);
					break;
				case AND:
					fd.ANDData(gd);
					break;
				case OR:
					fd.ORData(gd);
					break;
				case MASK:
					fd.MASKData(gd);
					break;
				case NOT:
					fd.NOTData();
					break;
				}
				
				// Configure the frame if it was already configured
				if (f.isConfigured())
					f.configure(fd);

				if (DEBUG) {
					// If the frames are the same, then the resulting frame should be empty. Print non-empty
					if (!fd.isEmpty())
						System.out.println("FAR update:"+fpga1.getFAR().getHexAddress());
				}
			//}

			fpga1.incrementFAR();
			if (fpga2 != null)
				fpga2.incrementFAR();
			f = fpga1.getCurrentFrame();
		} while (f != null);
	}
	
	public static boolean DEBUG = false;

}
