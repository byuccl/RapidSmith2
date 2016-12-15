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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.DummySyncData;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.V4BitstreamGenerator;

/**
 * Configuration Specification File for the V4 
 * 
 */
public class V4ConfigurationSpecification extends AbstractConfigurationSpecification {
    
    public V4ConfigurationSpecification() {
        _blockTypes = new ArrayList<>(Arrays.asList(new BlockType[]{LOGIC_BLOCK_TYPE, BRAM_INTERCONNECT_BLOCK_TYPE, BRAM_BLOCK_TYPE}));
        _deviceFamily = V4_FAMILY_NAME;
        _frameSize = V4_FRAME_SIZE;
        _dummySyncData = DummySyncData.V4_STANDARD_DUMMY_SYNC_DATA;
        _bramContentBlockType = BRAM_BLOCK_TYPE;
        _logicBlockType = LOGIC_BLOCK_TYPE;
        _bitstreamGenerator = V4BitstreamGenerator.getSharedInstance();
        _minorMask = V4_MINOR_MASK;
        _minorBitPos = V4_MINOR_BIT_POS;
        _columnMask = V4_COLUMN_MASK;
        _columnBitPos = V4_COLUMN_BIT_POS;
        _rowMask = V4_ROW_MASK;
        _rowBitPos = V4_ROW_BIT_POS;
        _topBottomMask = V4_TOP_BOTTOM_MASK;
        _topBottomBitPos = V4_TOP_BOTTOM_BIT_POS;
        _blockTypeMask = V4_BLOCK_TYPE_MASK;
        _blockTypeBitPos = V4_BLOCK_TYPE_BIT_POS;
    }
    
    public final static String V4_FAMILY_NAME = "Virtex4";
    public final static int V4_FRAME_SIZE = 41;

    public static final int V4_TOP_BOTTOM_BIT_POS = 22;
    public static final int V4_TOP_BOTTOM_MASK = 0x1 << V4_TOP_BOTTOM_BIT_POS;
    public static final int V4_BLOCK_TYPE_BIT_POS = 19;
    public static final int V4_BLOCK_TYPE_MASK = 0x7 << V4_BLOCK_TYPE_BIT_POS;
    public static final int V4_ROW_BIT_POS = 14;
    public static final int V4_ROW_MASK = 0x1F << V4_ROW_BIT_POS;
    public static final int V4_COLUMN_BIT_POS = 6;
    public static final int V4_COLUMN_MASK = 0xFF << V4_COLUMN_BIT_POS;
    public static final int V4_MINOR_BIT_POS = 0;
    public static final int V4_MINOR_MASK = 0x3F << V4_MINOR_BIT_POS;
    
    public static final BlockSubType IOB = new BlockSubType("IOB",30); 
    public static final BlockSubType CLB = new BlockSubType("CLB",22); 
    public static final BlockSubType DSP = new BlockSubType("DSP",21); 
    public static final BlockSubType CLK = new BlockSubType("CLK",3); 
    public static final BlockSubType MGT = new BlockSubType("MGT",20); 
    public static final BlockSubType LOGIC_OVERHEAD = new BlockSubType("LOGIC_OVERHEAD",2); 
    public static final BlockSubType BRAMINTERCONNECT = new BlockSubType("BRAMINTERCONNECT",20); 
    public static final BlockSubType BRAMCONTENT = new BlockSubType("BRAMCONTENT",64); 
    public static final BlockSubType BRAMOVERHEAD = new BlockSubType("BRAMOVERHEAD",2);
    
    public static final BlockType LOGIC_BLOCK_TYPE = new BlockType("LOGIC", new LinkedHashSet<>(Arrays.asList(new BlockSubType[]{IOB, CLB, DSP, CLK, MGT, LOGIC_OVERHEAD})));
    public static final BlockType BRAM_BLOCK_TYPE = new BlockType("BRAM", new LinkedHashSet<>(Arrays.asList(new BlockSubType[]{BRAMCONTENT, BRAMOVERHEAD})));
    public static final BlockType BRAM_INTERCONNECT_BLOCK_TYPE = new BlockType("BRAMINTERCONNECT", new LinkedHashSet<>(Arrays.asList(new BlockSubType[]{BRAMINTERCONNECT, BRAMOVERHEAD})));

}

