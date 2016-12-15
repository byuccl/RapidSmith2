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
package edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification;

import java.util.List;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.DummySyncData;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.BitstreamGenerator;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FrameAddressRegister;

/**
 * Specifies the parameters for the configuration data associated
 * with a Xilinx FPGA. This interface is independent of the family
 * and the part. Classes that implement this interface provide
 * all the appropriate parameter data for querying the configuration
 * bitstream. 
 * 
 * 
 * @author wirthlin
 *
 */
public interface XilinxConfigurationSpecification {

    // Specific for a given device family
    String getDeviceFamily();
    int getFrameSize();
    List<BlockType> getBlockTypes();
    DummySyncData getSyncData();
    BlockType getBRAMContentBlockType();
    BlockType getLogicBlockType();
    
    BitstreamGenerator getBitstreamGenerator();
    
    // FAR addressing
    int getMinorMask();
    int getMinorBitPos();
    int getColumnMask();
    int getColumnBitPos();
    int getRowMask();
    int getRowBitPos();
    int getTopBottomMask();
    int getTopBottomBitPos();
    int getBlockTypeMask();
    int getBlockTypeBitPos();
    
    // specific for a given device within a family  
    String getDeviceName();
    String[] getValidPackages();
    String[] getValidSpeedGrades();
    String getStringDeviceIDCode();
    int getIntDeviceIDCode();
    int getTopNumberOfRows();
    int getBottomNumberOfRows();
    
    List<BlockSubType> getBlockSubTypeLayout(BlockType bt);
    int getBlockTypeFromFAR(int farAddress);
    int getTopBottomFromFAR(int farAddress);
    int getRowFromFAR(int farAddress);
    int getColumnFromFAR(int farAddress);
    int getMinorFromFAR(int farAddress);
    List<BlockSubType> getOverallColumnLayout();
    List<BlockTypeInstance> getBlockTypeInstances();
    
    BlockSubType getBlockSubtype(XilinxConfigurationSpecification spec, FrameAddressRegister far);
}

/* Ideas for making this more general:
 * 1. Create an abstract class that represents a block type (Logic, BRAM, BRAM interconnect)
 *    - Indicates the block code used to define the block (in the FAR)
 *    - Has a string associated with it
 *    - Returns an array of valid block sub types (see below). BlockSubType[] getValidBlockSubTypes()
 * 2. Create an abstract class that represents a block sub-type (IO, CLB, DSP, CLK, MGT, etc.)
 *    - Has a name associated with it
 *    - Points to a super block type
 *    - Indicates how many frames are assocaited with this block subtype
 * 3. Each family architecture specification defines a block type for all valid block types
 *    - Returns an array of block types (BlockType[] getValidBlockTypes())
 * 4. Each device indicates how many columns of each block type there are
 *    - int getBlockColumns(BlockType b) 
 * 5. Each device indicates the layout of block sub types for each block type
 *      BlockSubType[] getBlockTypeLayout(Blocktype b)
 *      
 */
