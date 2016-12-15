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
/*
 * This file was auto-generated on Tue Feb 02 12:40:46 MST 2010
 * by edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxV6DeviceClassGenerator.
 * See the source code to make changes.
 *
 * Do not modify this file directly.
 */


package edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification;

import java.util.ArrayList;
import java.util.Arrays;

public class V6PartLibrary extends PartLibrary {

	public V6PartLibrary() {
		super();
	}

	protected void addParts() {
		addPart(new XC6VCX75T());
		addPart(new XC6VLX75T());
		addPart(new XC6VCX130T());
		addPart(new XC6VLX130T());
		addPart(new XC6VCX195T());
		addPart(new XC6VLX195T());
		addPart(new XC6VCX240T());
		addPart(new XC6VLX240T());
		addPart(new XC6VSX315T());
		addPart(new XC6VLX365T());
		addPart(new XC6VSX475T());
		addPart(new XC6VLX550T());
		addPart(new XC6VLX760());
	}

	class XC6VCX75T extends V6ConfigurationSpecification {

		public XC6VCX75T() {
			super();
			_deviceName = "XC6VCX75T";
			_deviceIDCode = "042c4093";
			_validPackages = new String[] {"ff484", "ff784", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 2;
			_bottomRows = 1;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLK, CLB, CLB, CLB, CLB, IOB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC6VLX75T extends V6ConfigurationSpecification {

		public XC6VLX75T() {
			super();
			_deviceName = "XC6VLX75T";
			_deviceIDCode = "04244093";
			_validPackages = new String[] {"ff484", "ff784", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 2;
			_bottomRows = 1;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLK, CLB, CLB, CLB, CLB, IOB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC6VCX130T extends V6ConfigurationSpecification {

		public XC6VCX130T() {
			super();
			_deviceName = "XC6VCX130T";
			_deviceIDCode = "042ca093";
			_validPackages = new String[] {"ff484", "ff784", "ff1156", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 2;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLK, CLB, CLB, CLB, CLB, IOB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC6VLX130T extends V6ConfigurationSpecification {

		public XC6VLX130T() {
			super();
			_deviceName = "XC6VLX130T";
			_deviceIDCode = "0424a093";
			_validPackages = new String[] {"ff484", "ff784", "ff1156", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 2;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLK, CLB, CLB, CLB, CLB, IOB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC6VCX195T extends V6ConfigurationSpecification {

		public XC6VCX195T() {
			super();
			_deviceName = "XC6VCX195T";
			_deviceIDCode = "042cc093";
			_validPackages = new String[] {"ff784", "ff1156", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 2;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLK, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC6VLX195T extends V6ConfigurationSpecification {

		public XC6VLX195T() {
			super();
			_deviceName = "XC6VLX195T";
			_deviceIDCode = "0424c093";
			_validPackages = new String[] {"ff784", "ff1156", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 2;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLK, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC6VCX240T extends V6ConfigurationSpecification {

		public XC6VCX240T() {
			super();
			_deviceName = "XC6VCX240T";
			_deviceIDCode = "042d0093";
			_validPackages = new String[] {"ff784", "ff1156", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 3;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLK, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC6VLX240T extends V6ConfigurationSpecification {

		public XC6VLX240T() {
			super();
			_deviceName = "XC6VLX240T";
			_deviceIDCode = "04250093";
			_validPackages = new String[] {"ff784", "ff1156", "ff1759", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 3;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLK, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC6VSX315T extends V6ConfigurationSpecification {

		public XC6VSX315T() {
			super();
			_deviceName = "XC6VSX315T";
			_deviceIDCode = "04286093";
			_validPackages = new String[] {"ff1156", "ff1759", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 3;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLK, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC6VLX365T extends V6ConfigurationSpecification {

		public XC6VLX365T() {
			super();
			_deviceName = "XC6VLX365T";
			_deviceIDCode = "04252093";
			_validPackages = new String[] {"ff1156", "ff1759", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 3;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLK, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC6VSX475T extends V6ConfigurationSpecification {

		public XC6VSX475T() {
			super();
			_deviceName = "XC6VSX475T";
			_deviceIDCode = "04288093";
			_validPackages = new String[] {"ff1156", "ff1759", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 4;
			_bottomRows = 5;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLK, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC6VLX550T extends V6ConfigurationSpecification {

		public XC6VLX550T() {
			super();
			_deviceName = "XC6VLX550T";
			_deviceIDCode = "04256093";
			_validPackages = new String[] {"ff1759", "ff1760", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 4;
			_bottomRows = 5;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLK, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC6VLX760 extends V6ConfigurationSpecification {

		public XC6VLX760() {
			super();
			_deviceName = "XC6VLX760";
			_deviceIDCode = "0423a093";
			_validPackages = new String[] {"ff1760", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 4;
			_bottomRows = 5;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLK, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

}
