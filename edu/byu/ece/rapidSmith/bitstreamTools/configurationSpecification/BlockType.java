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

import java.util.Collections;
import java.util.Set;

/**
 * The BlockType class represents one of the block types for a specific FPGA
 * family (i.e. V4, V5, V6). Each family should have a Set of BlockTypes that
 * indicates the valid block types that are used by the family. Each BlockType
 * has a List of SubBlockTypes indicating the valid column types for the block.
 */
public class BlockType {

    BlockType(String name, Set<BlockSubType> validBlockSubTypes) {
        _name = name;
        _validBlockSubTypes = validBlockSubTypes;
    }
    
    public String getName() {
        return _name;
    }
    
    public String toString() {
        return _name;
    }
    
    public Set<BlockSubType> getValidBlockSubTypes() {
        return Collections.unmodifiableSet(_validBlockSubTypes);
    }
    
    protected String _name;
    protected Set<BlockSubType> _validBlockSubTypes;
}
