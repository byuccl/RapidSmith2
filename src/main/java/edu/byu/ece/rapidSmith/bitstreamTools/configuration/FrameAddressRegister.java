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

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamUtils;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.AbstractConfigurationSpecification;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.BlockSubType;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.BlockType;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

/**
 * Represents the contents of a Xilinx Frame Address Register and performs proper
 * frame address register incrementing. This class stores the FAR as
 * a set of fields in the FAR rather than as a 32 bit number. This facilitates
 * the FAR incrementing.
 *
 * This class contains many static methods that are used to evaluate Frame Address information
 * based on configuration specifications. These static methods form a bridge between the
 * values in the configuration specification and the frame adddress.
 */
public class FrameAddressRegister {

	/**
	 * Creates an initial frame address register initialized to 0. 
	 */
	public FrameAddressRegister(XilinxConfigurationSpecification xcs) {
		configSpec = xcs;
		initFAR();
	}

	public FrameAddressRegister(XilinxConfigurationSpecification xcs, int farAddress) {
		configSpec = xcs;
		setFAR(farAddress);
	}
	
	public void initFAR() {
		top_bottom = 0;
		blockType = 0;
		row = 0;
		column = 0;
		minor = 0;		
	}

	public void setFAR(int address){
		top_bottom = getTopBottomFromAddress(address);
		blockType = getBlockTypeFromAddress(address);
		row = getRowFromAddress(address);
		column = getColumnFromAddress(address);
		minor = getMinorFromAddress(address);
	}

	/**
	 * This function will increment the Frame Address Register (FAR) to the next address.
	 * The frame address register does not follow a sequential increment pattern.  Instead,
	 * it follows a very particular pattern. There are smaller counters within the FAR that 
	 * in the following order (from first to last): minor, column, row, top_bottom, and then 
	 * type.  See Xilinx UG071, Virtex-4 FPGA Configuration User Guide for more detail and
	 * Xilinx UG191, Virtex-5 FPGA Configuration User Guide.
	 *  
	 * @return true if the resulting FAR is valid. return false if increment is invalid.
	 * 
	 * Need better error checking and feedback
	 */
	public boolean incrementFAR() {

		// Check to see if the block number is beyond the last block number.
		// If so, we can't increment and return false (bad FAR address - beyond the end)
		if (blockType >= configSpec.getBlockTypes().size())		
			return false;
				
		// Check to see if we have reached the last frame in a given COnfifguration block.
		if(minor == getFramesPerConfigurationBlock(configSpec, blockType, column) - 1){
			// End of a block. Initialize minor and check column.
			minor = 0;

			//If we have reached the max column for the specified type of block
			//set it to zero the check the row
			if(column == getNumberOfColumns(configSpec, blockType) - 1) {
				column = 0;
				// Last column. Move to a new row.
				//If we have reached the max row, then check the top_bottom,
				//else increment row
				if ((top_bottom == 0 && row == configSpec.getTopNumberOfRows() - 1) ||
						(top_bottom == 1 && row == configSpec.getBottomNumberOfRows() - 1)) {
					row = 0;					
					//If max top_bottom has been reached, set top_bottom to zero and check the
					//block type, else increment top_bottom
					if(top_bottom == 0){
						top_bottom = 1;
					}
					else if(top_bottom == 1){
						top_bottom = 0;
						//If the incremented type is greater than two, it is invalid and we 
						//will return false. Otherwise, we are finished and return true.
						blockType++;
						if(blockType >= configSpec.getBlockTypes().size() ){
							return false;
						}
					}
				} else
					// not last row. Increment.
					row++;
			} else 
				// Not last column. Increment.
				column++;
		}
		else {
			// Still in the middle of a block. Just increment the minor number.
			minor++;
		}
		return true;
	}
	
	public boolean incrementFAR(int num) {
		for (int i = 0; i < num; i++)
			if (!incrementFAR())
				return false;
		return true;
	}
	
	public boolean validFARAddress() {
		if (blockType >= configSpec.getBlockTypes().size())		
			return false;
		return true;
	}
	/**
	 * Convert the current FAR address into a consecutive address
	 */
	public int getConsecutiveAddress() {
		int address = 0;
		int i=0;
		// add addresses of block types that are *before* the current block type
		for (i = 0; i < blockType; i++)
			address += getFramesPerFARBlockType(configSpec, i);
		// add addresses of top for current block type if we are in the bottom
		if (top_bottom > 0)
			address += getFramesInTop(configSpec, blockType);
		
		// add addresses of rows in current block type that are *before* the current row
		// but in the same FAR block type
		address += row * getFramesPerRow(configSpec, blockType);
		// Add addresses of columns in current row that are *before* the current column
		for (i = 0; i < column; i++)
			address += getFramesPerConfigurationBlock(configSpec, blockType, i);
		// Add addresses of frames *before* current frame
		address += minor;
		return address;
	}
	
	public void setFARAddressFromConsecutiveAddress(int consecutiveAddress){
		int address = consecutiveAddress;
		//int i=0;

		// Determine block number
		blockType = 0;
		for (blockType = 0;                                      // Initialize block type to 0
			address >= getFramesPerFARBlockType(configSpec, blockType);      // See if address is large enough to skip current block
			address -= getFramesPerFARBlockType(configSpec, blockType++))    // If so, reduce the address count and increment block count
			;

		// Determine Top or Bottom
		if (address >= getFramesInTop(configSpec, blockType)) {
			top_bottom = 1;
			address -= getFramesInTop(configSpec, blockType);
		}

		// Determine Row
		row = address / getFramesPerRow(configSpec, blockType);
		address -= getFramesPerRow(configSpec, blockType) * row;
		
		// Determine column
		for (column = 0;                                      // Initialize block type to 0
			address >= getFramesPerConfigurationBlock(configSpec, blockType, column);
			address -= getFramesPerConfigurationBlock(configSpec, blockType, column++))
			;
		// detrmine minor
		minor = address;
	}

	public int getTopBottomFromAddress(int address) {
		return getTopBottomFromAddress(configSpec,address); 
	}
	public int getAddressFromTopBottom(int topBottom) {
		return getAddressFromTopBottom(configSpec, topBottom);
	}
	public int getBlockTypeFromAddress(int address) {
		return getBlockTypeFromAddress(configSpec, address);
	}
	public int getAddressFromBlockType(int blockType) {
		return getAddressFromBlockType(configSpec, blockType);
	}
	public int getColumnFromAddress(int address) {
		return getColumnFromAddress(configSpec, address);
	}
	public int getAddressFromColumn(int column) {
		return getAddressFromColumn(configSpec, column);
	}
	public int getRowFromAddress(int address) {
		return getRowFromAddress(configSpec, address);
	}
	public int getAddressFromRow(int row) {
		return getAddressFromRow(configSpec, row);
	}
	public int getMinorFromAddress(int address) {
		return getMinorFromAddress(configSpec, address);
	}
	public int getAddressFromMinor(int minor) {
		return getAddressFromMinor(configSpec, minor);
	}

	public XilinxConfigurationSpecification getConfigurationSpecification() {
		return configSpec;
	}

	/**
	 * Returns the FAR address as an integer based on the values of the FAR 
	 * address fields.
	 */
	public int getAddress() {
		int far = 0;
		far |= getAddressFromTopBottom(top_bottom);
		far |= getAddressFromBlockType(blockType);
		far |= getAddressFromRow(row);
		far |= getAddressFromColumn(column);
		far |= getAddressFromMinor(minor);		
		return far;
	}

	public String getHexAddress() {
		return BitstreamUtils.toHexString(getAddress());
	}
	
	public int getTopBottom() { return top_bottom; }
	public int getBlockType() { return blockType; }
	public int getRow() { return row; }
	public int getColumn() { return column; }
	public int getMinor() { return minor; }

	/**
	 * Prints out information about a frame address.
	 */
	public String toString() {
		return toString(1);
	}
	
	/**
	 * Level 0: hex address only
	 * Level 1: Single line string that decodes the FAR
	 */
	public String toString(int level) {
		if (level == 0)
			return getHexAddress();
				
		StringBuffer sb = new StringBuffer();

		// Print address
		sb.append("FAR="+BitstreamUtils.toHexString(getAddress())+", ");
				// Print top & bottom
		sb.append(AbstractConfigurationSpecification.getTopBottom(top_bottom));
		sb.append(" Type=" + AbstractConfigurationSpecification.getBlockType(configSpec, blockType) + " ("+blockType+")");		
		sb.append(", Row="+ row);
		sb.append(", Column=" + column +" (" +
				AbstractConfigurationSpecification.getBlockSubtype(configSpec, blockType, column) + "),");
		sb.append(" Minor="+ minor);
		return sb.toString();			
	}

	//////////////////////////////////////////////////////////////////////////////////////////////
	// Static Methods
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// Methods for extracting specific fields from a FAR address
	public static int getTopBottomFromAddress(XilinxConfigurationSpecification spec, int address) {
		return (address & spec.getTopBottomMask()) >>> spec.getTopBottomBitPos();
	}
	public static int getBlockTypeFromAddress(XilinxConfigurationSpecification spec, int address) {
		return (address & spec.getBlockTypeMask()) >>> spec.getBlockTypeBitPos();
	}
	public static int getColumnFromAddress(XilinxConfigurationSpecification spec, int address) {
		return (address & spec.getColumnMask()) >>> spec.getColumnBitPos();
	}
	public static int getRowFromAddress(XilinxConfigurationSpecification spec, int address) {
		return (address & spec.getRowMask()) >>> spec.getRowBitPos();
	}
	public static int getMinorFromAddress(XilinxConfigurationSpecification spec, int address) {
		return (address & spec.getMinorMask()) >>> spec.getMinorBitPos();
	}

	// Methods for creating FAR addresses from field values
	public static int getAddressFromTopBottom(XilinxConfigurationSpecification spec, int topBottom) {
		return topBottom << spec.getTopBottomBitPos();
	}
	public static int getAddressFromBlockType(XilinxConfigurationSpecification spec, int blockType) {
		return blockType << spec.getBlockTypeBitPos();
	}
	public static int getAddressFromColumn(XilinxConfigurationSpecification spec, int column) {
		return column << spec.getColumnBitPos();
	}
	public static int getAddressFromRow(XilinxConfigurationSpecification spec, int row) {
		return row << spec.getRowBitPos();
	}
	public static int getAddressFromMinor(XilinxConfigurationSpecification spec, int minor) {
		return (minor) << spec.getMinorBitPos();
	}

	// Methods for determining frame counts for various bitstream regions
	public static int getFramesPerFARBlockType(XilinxConfigurationSpecification spec, int block) {	
		return getFramesInTop(spec, block) + getFramesInBottom(spec, block);
	}
	
	public static int getFramesInTop(XilinxConfigurationSpecification spec, int block) {
		return getFramesPerRow(spec, block) * spec.getTopNumberOfRows();
	}
	
	public static int getFramesInBottom(XilinxConfigurationSpecification spec, int block) {
		return getFramesPerRow(spec, block) * spec.getBottomNumberOfRows();
	}
	
	public static int getFramesPerRow(XilinxConfigurationSpecification spec, int block) {
		int frames = 0;
		for (int i = 0; i < getNumberOfColumns(spec, block); i++) {
			frames += getFramesPerConfigurationBlock(spec, block,i);
		}
		return frames;
	}
	public static int getFramesPerConfigurationBlock(XilinxConfigurationSpecification spec, int blockNum, int column) {
		BlockType type = spec.getBlockTypes().get(blockNum);
		BlockSubType subType = spec.getBlockSubTypeLayout(type).get(column);
		int result = subType.getFramesPerConfigurationBlock();
		return result;
	}

	public static int getNumberOfColumns(XilinxConfigurationSpecification spec, int blockNum) {
	    BlockType type = spec.getBlockTypes().get(blockNum);
	    int result = spec.getBlockSubTypeLayout(type).size();
	    return result;
	}

	public static int getNumberOfFramesPerBlockRow(XilinxConfigurationSpecification spec, int blockNum) {
		int columns = getNumberOfColumns(spec, blockNum);
		int frames = 0;
		for (int i = 0; i < columns; i++)
			frames+=getFramesPerConfigurationBlock(spec, blockNum, i);
		return frames;			
	}
	
	public static int getNumberOfFramesPerBlockTop(XilinxConfigurationSpecification spec, int blockNum) {
		return getNumberOfFramesPerBlockRow(spec, blockNum) * spec.getTopNumberOfRows();
	}
	
	public static int getNumberOfFramesPerBlockBottom(XilinxConfigurationSpecification spec, int blockNum) {
		return getNumberOfFramesPerBlockRow(spec, blockNum) * spec.getBottomNumberOfRows();
	}
	
	public static int getNumberOfFramesPerBlock(XilinxConfigurationSpecification spec, int blockNum) {
		return getNumberOfFramesPerBlockTop(spec, blockNum) + getNumberOfFramesPerBlockBottom(spec, blockNum);
	}
	
	public static int getNumberOfFrames(XilinxConfigurationSpecification spec) {	    
	    int blockTypes = spec.getBlockTypes().size();
		int frames = 0;
		for (int i = 0; i < blockTypes; i++)
			frames += getNumberOfFramesPerBlock(spec, i);
		return frames;
	}

	// Misc. static methods

	/**
	 * Create an integer frame address from the various fields that make up the frame address
	 * register. 
	 */
	public static int createFAR(XilinxConfigurationSpecification spec, int topBottom, int blockType,
			int row, int column, int minor) {
		int far = 0;
		far |= getAddressFromTopBottom(spec, topBottom);
		far |= getAddressFromBlockType(spec, blockType);
		far |= getAddressFromRow(spec, row);
		far |= getAddressFromColumn(spec, column);
		far |= getAddressFromMinor(spec, minor);
		
		return far;
	}

	/**
	 * Determine the starting FAR address for a given block type
	 */
	public static int createBlockStartingFAR(XilinxConfigurationSpecification spec, int blockType) {
		return createFAR(spec,0,blockType,0,0,0);
	}

	/**
	 * Deterimine the block type number from a fixed block type object.
	 */
	public static int getBlockTypeNumber(XilinxConfigurationSpecification spec, BlockType blocktype) {
		
	    return spec.getBlockTypes().indexOf(blocktype);
	}
	
	public static int getBRAMContentFrameAddress(XilinxConfigurationSpecification spec) {
		int blockTypeNum = getBlockTypeNumber(spec, spec.getBRAMContentBlockType());
		return createBlockStartingFAR(spec,blockTypeNum);
	}

	public static int getNumberOfBRAMContentFrames(XilinxConfigurationSpecification spec) {
		int blockTypeNum = getBlockTypeNumber(spec, spec.getBRAMContentBlockType());	
		return getFramesPerFARBlockType(spec, blockTypeNum);
	}
	
	public static String toString(XilinxConfigurationSpecification spec, int address) {
		FrameAddressRegister far = new FrameAddressRegister(spec,address);
		return far.toString();
	}
	
	/**
	 * Return the 'consecutive' address from a FAR address. This is usually
	 * used for accessing frame data from the sequential frame data array.
	 */
	public static int getConsecutiveAddress(XilinxConfigurationSpecification spec, int address) {
		FrameAddressRegister far = new FrameAddressRegister(spec,address);
		return far.getConsecutiveAddress();		
		
	}
	
	////////////////////////////////
	// Class Fields
	////////////////////////////////
	

	
	/**
	 * The configuration specification object that this class needs to know
	 * how to incrememnt and manipulate the FAR. 
	 */
	protected XilinxConfigurationSpecification configSpec;

	/** 
	 * Determines if we are accessing the top or bottom partition of the FPGA. 
	 * 
	 * 0 = top
	 * 1 = bottom
	 */
	protected int top_bottom;
	
	/** 
	 * Represents bits 21:19 (V4) or 23:21 (V5) of the Frame Address Register (FAR).
	 * Determines the type of block currently addressed. 
	 *   Virtex-4 Block types are:
	 *   Logic blocks, CLB/IO/CLK/DSP/MGT: 000
	 *   Block RAM interconnect blocks: 001
	 *   Block RAM content blocks: 010
	 *   CFG_CLB blocks: 011
	 *   CFG_BRAM blocks: 100
	 *   A normal V4 bitstream does not include CFG_CLB or CFG_BRAM
	 *   Virtex-5 Block types are:
	 *   Logic blocks and interconnect, CLB/IO/CLK/DSP/BRAM interconnect: 000
	 *   Block RAM content blocks: 001
	 *   Interconnect and Block Special Frames (used in partial reconfiguration): 010
	 *   Block RAM Non-Configuration Frames (not accessible by users): 011
	 */
	protected int blockType;
	
	/** 
	 * Represents bits 18:14 (V4) or 19:15 (V5) of the Frame Address Register (FAR).
	 * Determines the row currently being addressed. 
	 */
	protected int row;
	
	/** 
	 * Represents bits 13:6 (V4) or 14:7 (V5) of the Frame Address Register (FAR).
	 * Determines the column currently being addressed. 
	 */
	protected int column;
	
	/** 
	 * Represents bits 5:0 (V4) or 6:0 (V5) of the Frame Address Register (FAR).
	 * Determines the frame within the block currently being addressed. 
	 */
	protected int minor;
	
}
