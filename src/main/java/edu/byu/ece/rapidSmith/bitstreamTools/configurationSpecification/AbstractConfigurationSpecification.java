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
import java.util.List;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.DummySyncData;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.BitstreamGenerator;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FrameAddressRegister;

/**
 * A base Configuration Specification class that can be used for all architectures.
 * Contains a lot of reusable code.
 */
public abstract class AbstractConfigurationSpecification implements XilinxConfigurationSpecification {

    public String getDeviceName() { return _deviceName; }    
    public String getStringDeviceIDCode() { return _deviceIDCode; }
    public int getTopNumberOfRows() { return _topRows; }
    public int getBottomNumberOfRows() { return _bottomRows; }
    
    public String[] getValidSpeedGrades() { return _validSpeedGrades; }
    public String[] getValidPackages() { return _validPackages; }

    public int getBlockTypeFromFAR(int farAddress) {
        return (farAddress & this.getBlockTypeMask()) >> this.getBlockTypeBitPos();
    }
    
    public int getTopBottomFromFAR(int farAddress) {
        return (farAddress & this.getTopBottomMask()) >> this.getTopBottomBitPos();
    }
    
    public int getRowFromFAR(int farAddress) {
        return (farAddress & this.getRowMask()) >> this.getRowBitPos();
    }
    
    public int getColumnFromFAR(int farAddress) {
        return (farAddress & this.getColumnMask()) >> this.getColumnBitPos();   
    }
    public int getMinorFromFAR(int farAddress) {
        return (farAddress & this.getMinorMask()) >> this.getMinorBitPos();     
    }
    public int getIntDeviceIDCode() {
        return Integer.parseInt(getStringDeviceIDCode(),16);
    }
    
    public String toString() {
        return toString(0);
    }
    
    public String toString(int printLevel) {
        String str = "";
        
        str += "Family:" + getDeviceFamily() + "\n";
        str += "Device:" + getDeviceName() + "\n";
        str += "Frame Size:" + getFrameSize() + "\n";
        str += "Top Rows:" + getTopNumberOfRows() + "\n";
        str += "Bottom Rows:" + getBottomNumberOfRows() + "\n";
        
        str += "Block types & Layout:\n";
        for (BlockType t : getBlockTypes()) {
            List<BlockSubType> subTypes = getBlockSubTypeLayout(t);
            str += "\t" + t.getName() + " (" + subTypes.size() +" columns): ";
            for (BlockSubType s : getBlockSubTypeLayout(t)) {
                str += s.getName() +" ";
            }
            str += "\n";
        }
        
        return str;
    }
    
    // Public static helper methods for debug
    public static String getTopBottom(int topBottom) {
        if (topBottom == 0)
            return "top";
        else
            return "bottom";
    }
    public static String getBlockType(XilinxConfigurationSpecification spec, int blockType) {
        List<BlockType> types = spec.getBlockTypes();
        if (blockType < types.size())
            return types.get(blockType).getName();
        return null;
    }

    public static String getBlockSubtype(XilinxConfigurationSpecification spec, int blockType, int column) {
        List<BlockType> types = spec.getBlockTypes();
        BlockType type = null;
        if (blockType < types.size())
            type = types.get(blockType);
        else
            return null;
        List<BlockSubType> layout = spec.getBlockSubTypeLayout(type);
        BlockSubType subType = null;
        if (column < layout.size()) {
            subType = layout.get(column);
        } else {
            return null;
        }
        return subType.getName();
    }
    
    public BlockSubType getBlockSubtype(XilinxConfigurationSpecification spec, FrameAddressRegister far) {
        List<BlockType> types = spec.getBlockTypes();
        BlockType type = null;
        if (far.getBlockType() < types.size())
            type = types.get(far.getBlockType());
        else
            return null;
        List<BlockSubType> layout = spec.getBlockSubTypeLayout(type);
        BlockSubType subType = null;
        if (far.getColumn() < layout.size()) {
            subType = layout.get(far.getColumn());
        } else {
            return null;
        }
        return subType;
    }
    
    public List<BlockSubType> getOverallColumnLayout() {
        return Collections.unmodifiableList(_overallColumnLayout);
    }
    
    public List<BlockType> getBlockTypes() {
        return Collections.unmodifiableList(_blockTypes);
    }

    public List<BlockSubType> getBlockSubTypeLayout(BlockType blockType) {
        int blockIndex = _blockTypes.indexOf(blockType);
        if (blockIndex >= 0) {
            BlockTypeInstance bti = _blockTypeLayouts.get(blockIndex);
            if (bti != null) {
                return Collections.unmodifiableList(bti.getColumnLayout());
            }
        }
        
        return null;
    }
    
    public List<BlockTypeInstance> getBlockTypeInstances() {
        return Collections.unmodifiableList(_blockTypeLayouts);
    }
    
    public String getDeviceFamily() {
        return _deviceFamily;
    }

    public int getFrameSize() {
        return _frameSize;
    }
    
    public DummySyncData getSyncData() {
        return _dummySyncData;
    }
    
    public BlockType getBRAMContentBlockType() {
        return _bramContentBlockType;
    }
    
    public BlockType getLogicBlockType() {
        return _logicBlockType;
    }
    
    public BitstreamGenerator getBitstreamGenerator() {
        return _bitstreamGenerator;
    }
    
    public int getMinorMask() {
        return _minorMask;
    }
    
    public int getMinorBitPos() {
        return _minorBitPos;
    }
    
    public int getColumnMask() {
        return _columnMask;
    }
    
    public int getColumnBitPos() {
        return _columnBitPos;
    }
    
    public int getRowMask() {
        return _rowMask;
    }
    
    public int getRowBitPos() {
        return _rowBitPos;
    }
    
    public int getTopBottomMask() {
        return _topBottomMask;
    }
    
    public int getTopBottomBitPos() {
        return _topBottomBitPos;
    }
    
    public int getBlockTypeMask() {
        return _blockTypeMask;
    }
    
    public int getBlockTypeBitPos() {
        return _blockTypeBitPos;
    }
    
    protected List<BlockType> _blockTypes;
    
    protected List<BlockTypeInstance> _blockTypeLayouts;
    protected List<BlockSubType> _overallColumnLayout;
    protected String _deviceName;
    protected String _deviceIDCode;
    protected int _topRows;
    protected int _bottomRows;
    protected String[] _validSpeedGrades;
    protected String[] _validPackages;
    
    protected String _deviceFamily;
    protected int _frameSize;
    protected DummySyncData _dummySyncData;
    protected BlockType _bramContentBlockType;
    protected BlockType _logicBlockType;
    protected BitstreamGenerator _bitstreamGenerator;
    protected int _minorMask;
    protected int _minorBitPos;
    protected int _columnMask;
    protected int _columnBitPos;
    protected int _rowMask;
    protected int _rowBitPos;
    protected int _topBottomMask;
    protected int _topBottomBitPos;
    protected int _blockTypeMask;
    protected int _blockTypeBitPos;
    
}
