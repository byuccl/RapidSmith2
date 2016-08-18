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
package edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

import edu.byu.ece.rapidSmith.bitstreamTools.configuration.V6BitstreamGenerator;

public class V6ConfigurationSpecification extends V56ConfigurationSpecification {

    public V6ConfigurationSpecification() {
        super();
        _blockTypes = new ArrayList<BlockType>(Arrays.asList(new BlockType[] {LOGIC_INTERCONNECT_BLOCKTYPE, BRAM_CONTENT_BLOCKTYPE}));
        _deviceFamily = V6_FAMILY_NAME;
        _frameSize = V6_FRAME_SIZE;
        _bramContentBlockType = BRAM_CONTENT_BLOCKTYPE;
        _logicBlockType = LOGIC_INTERCONNECT_BLOCKTYPE;
        _bitstreamGenerator = V6BitstreamGenerator.getSharedInstance();
    }
    
	public final static String V6_FAMILY_NAME = "Virtex6";
	public final static int V6_FRAME_SIZE = 81;

	public static final BlockSubType CLB = new BlockSubType("CLB",36); 
	public static final BlockSubType IOB = new BlockSubType("IOB",44); 
	public static final BlockSubType DSP = new BlockSubType("DSP",28); 
	public static final BlockSubType CLK = new BlockSubType("CLK",38); 
	public static final BlockSubType GTX = new BlockSubType("GTX",30);
	public static final BlockSubType GTH = new BlockSubType("GTH",30);
	public static final BlockSubType LOGIC_OVERHEAD = new BlockSubType("LOGIC_OVERHEAD",2); 
	public static final BlockSubType BRAMINTERCONNECT = new BlockSubType("BRAMINTERCONNECT",28); 
	public static final BlockSubType BRAMCONTENT = new BlockSubType("BRAMCONTENT",128); 
	public static final BlockSubType BRAMOVERHEAD = new BlockSubType("BRAMOVERHEAD",2); 

	public static final BlockType LOGIC_INTERCONNECT_BLOCKTYPE = new BlockType("LOGIC", new LinkedHashSet<BlockSubType>(Arrays.asList(
			new BlockSubType[]{
			IOB, 
			CLB, 
			DSP, 
			CLK, 
			GTX,
			GTH,
			BRAMINTERCONNECT,
			LOGIC_OVERHEAD })));
	
	public static final BlockType BRAM_CONTENT_BLOCKTYPE = new BlockType("BRAM", new LinkedHashSet<BlockSubType>(Arrays.asList(
			new BlockSubType[]{
			BRAMCONTENT, 
			BRAMOVERHEAD })));	
}
