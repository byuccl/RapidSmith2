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

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.DummySyncData;

/**
 * Configuration specification common between the V5 and V6 families. 
 */
public abstract class V56ConfigurationSpecification extends AbstractConfigurationSpecification {
	
    public V56ConfigurationSpecification() {
        _dummySyncData = DummySyncData.V5_V6_STANDARD_DUMMY_SYNC_DATA;
        _minorMask = V56_MINOR_MASK;
        _minorBitPos = V56_MINOR_BIT_POS;
        _columnMask = V56_COLUMN_MASK;
        _columnBitPos = V56_COLUMN_BIT_POS;
        _rowMask = V56_ROW_MASK;
        _rowBitPos = V56_ROW_BIT_POS;
        _topBottomMask = V56_TOP_BOTTOM_MASK;
        _topBottomBitPos = V56_TOP_BOTTOM_BIT_POS;
        _blockTypeMask = V56_BLOCK_TYPE_MASK;
        _blockTypeBitPos = V56_BLOCK_TYPE_BIT_POS;
    }
    
	public static final int V56_TOP_BOTTOM_BIT_POS = 20;
	public static final int V56_TOP_BOTTOM_MASK = 0x1 << V56_TOP_BOTTOM_BIT_POS;
	public static final int V56_BLOCK_TYPE_BIT_POS = 21;
	public static final int V56_BLOCK_TYPE_MASK = 0x7 << V56_BLOCK_TYPE_BIT_POS;
	public static final int V56_ROW_BIT_POS = 15;
	public static final int V56_ROW_MASK = 0x1F << V56_ROW_BIT_POS;
	public static final int V56_COLUMN_BIT_POS = 7;
	public static final int V56_COLUMN_MASK = 0xFF << V56_COLUMN_BIT_POS;
	public static final int V56_MINOR_BIT_POS = 0;
	public static final int V56_MINOR_MASK = 0x7F << V56_MINOR_BIT_POS;
	
}

