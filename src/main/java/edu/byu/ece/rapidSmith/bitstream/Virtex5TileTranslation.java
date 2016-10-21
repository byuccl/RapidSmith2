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
package edu.byu.ece.rapidSmith.bitstream;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FPGA;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.Frame;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FrameAddressRegister;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.BlockSubType;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.DeviceLookup;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.V5ConfigurationSpecification;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileType;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * Please note, this class is still under construction.
 * Created on: May 25, 2011
 */
public class Virtex5TileTranslation {
	
	private int clkColumnIndex = -1;
	
	//this seems to be correct
	public int getTopBottom(XilinxConfigurationSpecification spec, Tile tile){
		int row = tile.getTileYCoordinate() / 20;
		if(row < spec.getBottomNumberOfRows()){
			// Bottom
			return 1; 
		}
		else{
			// Top
			return 0;
		}
	}
	
	//this seems to be correct.
	public int getConfigurationRow(XilinxConfigurationSpecification spec, Tile tile){
		int row = tile.getTileYCoordinate() / 20;
		
		if(row < spec.getBottomNumberOfRows()){
			// Bottom
			row = (spec.getBottomNumberOfRows()-1) - row;
		}
		else{
			// Top
			row = row - spec.getBottomNumberOfRows();
		}
		return row;
	}
	
	//this seems to be correct
	public int getConfigurationColumn(XilinxConfigurationSpecification spec, Tile tile){
		//TODO not sure what this was for
//		if(tile.getTileXCoordinate() > (2*getClkColumnIndex(spec)+10)){
//			System.out.println("Clk Element like SYSMON Error is Possible"); 
//			return (getClkColumnIndex(spec));
//		}
		
		if(tile.getType().equals(TileType.CLK_HROW)){
			return getClkColumnIndex(spec);
		}else{
			if(tile.getTileXCoordinate() >= getClkColumnIndex(spec)){
				return tile.getTileXCoordinate() + 1;
			}
			return tile.getTileXCoordinate();
		}

	}
	
	//this seems to be correct.
	public int getClkColumnIndex(XilinxConfigurationSpecification spec){
		if(clkColumnIndex != -1){
			return clkColumnIndex;
		}
		int i=0;
		for(BlockSubType blk : spec.getBlockSubTypeLayout(V5ConfigurationSpecification.LOGIC_INTERCONNECT_BLOCKTYPE)){
			if(blk.equals(V5ConfigurationSpecification.CLK)){
				clkColumnIndex = i;
				return clkColumnIndex;
			}
			i++;
		}
		MessageGenerator.briefError("ERROR: Could not find clk column block type!");
		return -1;
	}
	
	
	public int getFARFromTile(XilinxConfigurationSpecification spec, Tile tile, int minor){
		int topBottom = getTopBottom(spec, tile);
		int row = getConfigurationRow(spec, tile);
		int column = getConfigurationColumn(spec, tile);
		return FrameAddressRegister.createFAR(spec, topBottom, 0, row, column, minor);
	}
	
	public static void main(String[] args){
		String partName = "xc5vlx30t";
		String packageName = "ff665";
		// Load up all the RapidSmith data structures
		Device dev = RSEnvironment.getDefault().getDevice(partName + packageName);
		
		XilinxConfigurationSpecification spec = DeviceLookup.lookupPartV4V5V6(partName);

		FPGA fpga = new FPGA(spec);
		
		// Pick a tile...
		Tile tile = dev.getTile("CLBLL_X14Y65");
		
		// This class has some internal state 
		Virtex5TileTranslation tileTranslator = new Virtex5TileTranslation();
		
		// In order to get the frame, the minor address is needed.  
		int minorAddress = 0;
		
		// *** Here is the good part, we get the FAR address from the tile *** ///
		// NOTE: These methods aren't fully developed yet and there are likely to be bugs
		int farAddress = tileTranslator.getFARFromTile(spec, tile, minorAddress);

		System.out.println("Top/Bottom: " + (FrameAddressRegister.getTopBottomFromAddress(spec, farAddress)==1 ? "bottom" : "top"));
		System.out.println("Block Type: " + FrameAddressRegister.getBlockTypeFromAddress(spec, farAddress));
		System.out.println("       Row: " + FrameAddressRegister.getRowFromAddress(spec, farAddress));
		System.out.println("    Column: " + FrameAddressRegister.getColumnFromAddress(spec, farAddress));
		System.out.println("     Minor: " + FrameAddressRegister.getMinorFromAddress(spec, farAddress));
		
		// Here we can get the frame
		Frame frame = fpga.getFrame(farAddress);
		System.out.println(frame.toString());
	}
}
