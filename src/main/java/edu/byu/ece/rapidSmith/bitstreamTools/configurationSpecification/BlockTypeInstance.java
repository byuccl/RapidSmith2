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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The BlockTypeInstance class represents the layout of a block type for
 * a particular FPGA part. Each part should have as many BlockTypeInstances
 * as there are BlockTypes in the family.
 * 
 * The column layout is represented as a list of BlockSubTypes.
 */
public class BlockTypeInstance {
    
    public BlockTypeInstance(BlockType blockType, List<BlockSubType> columnLayout) {
        _blockType = blockType;
        _columnLayout = new ArrayList<>(columnLayout);
    }
    
    public BlockTypeInstance(BlockType blockType, BlockSubType[] columnLayout) {
        _blockType = blockType;
        _columnLayout = new ArrayList<>(Arrays.asList(columnLayout));
    }
    
    public int getNumColumns() {
        return _columnLayout.size();
    }
    
    public BlockSubType getBlockSubType(int index) {
        if (index >= 0 && index < _columnLayout.size()) {
            return _columnLayout.get(index);
        }
        else {
            return null;
        }
    }
    
    public List<BlockSubType> getColumnLayout() {
        return Collections.unmodifiableList(_columnLayout);
    }
    
    public String toString() {
        String result = _blockType.toString() + " : {";
        Iterator<BlockSubType> it = _columnLayout.iterator();
        while (it.hasNext()) {
            BlockSubType subType = it.next();
            result += subType;
            if (it.hasNext()) {
                result += ", ";
            }
        }
        
        result += "}";
        return result;
    }
    
    protected BlockType _blockType;
    protected List<BlockSubType> _columnLayout;
}
