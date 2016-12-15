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

/**
 * The BlockSubType class represents a block subtype for a specific FPGA
 * family (i.e. V4, V5, V6). Each family should have it's own Set of
 * BlockSubTypes that are shared across all of the parts in the family.
 * 
 * A BlockSubType contains a name and an int representing the number of
 * configuration frames required by the BlockSubType.
 */
public class BlockSubType {
    
    public BlockSubType(String name, int framesPerConfigurationBlock) {
        _name = name;
        _framesPerConfigurationBlock = framesPerConfigurationBlock;
    }
    
    public int getFramesPerConfigurationBlock() {
        return _framesPerConfigurationBlock;
    }
    
    public String getName() {
        return _name;
    }
    
    public String toString() {
        return _name;
    }
    
    protected String _name;
    
    protected int _framesPerConfigurationBlock;
}
