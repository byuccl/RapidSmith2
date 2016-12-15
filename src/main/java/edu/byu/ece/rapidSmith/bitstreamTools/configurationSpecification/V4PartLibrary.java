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
/*
 * This file was auto-generated on Tue Feb 02 12:12:09 MST 2010
 * by edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxV4DeviceClassGenerator.
 * See the source code to make changes.
 *
 * Do not modify this file directly.
 */


package edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification;

import java.util.ArrayList;
import java.util.Arrays;

public class V4PartLibrary extends PartLibrary {

	public V4PartLibrary() {
		super();
	}

	protected void addParts() {
		addPart(new XC4VFX12());
		addPart(new XC4VLX15());
		addPart(new XC4VFX20());
		addPart(new XC4VLX25());
		addPart(new XC4VSX25());
		addPart(new XC4VSX35());
		addPart(new XC4VFX40());
		addPart(new XC4VLX40());
		addPart(new XC4VSX55());
		addPart(new XC4VFX60());
		addPart(new XC4VLX60());
		addPart(new XC4VLX80());
		addPart(new XC4VFX100());
		addPart(new XC4VLX100());
		addPart(new XC4VFX140());
		addPart(new XC4VLX160());
		addPart(new XC4VLX200());
	}

	class XC4VFX12 extends V4ConfigurationSpecification {

		public XC4VFX12() {
			super();
			_deviceName = "XC4VFX12";
			_deviceIDCode = "01e58093";
			_validPackages = new String[] {"sf363", "ff668", };
			_validSpeedGrades = new String[] {"-12", "-11", "-10", };
			_topRows = 2;
			_bottomRows = 2;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VLX15 extends V4ConfigurationSpecification {

		public XC4VLX15() {
			super();
			_deviceName = "XC4VLX15";
			_deviceIDCode = "01658093";
			_validPackages = new String[] {"sf363", "ff668", "ff676", };
			_validSpeedGrades = new String[] {"-12", "-11", "-10", };
			_topRows = 2;
			_bottomRows = 2;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VFX20 extends V4ConfigurationSpecification {

		public XC4VFX20() {
			super();
			_deviceName = "XC4VFX20";
			_deviceIDCode = "01e64093";
			_validPackages = new String[] {"ff672", };
			_validSpeedGrades = new String[] {"-12", "-11", "-10", };
			_topRows = 2;
			_bottomRows = 2;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							MGT, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, MGT, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{MGT, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, MGT, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VLX25 extends V4ConfigurationSpecification {

		public XC4VLX25() {
			super();
			_deviceName = "XC4VLX25";
			_deviceIDCode = "0167c093";
			_validPackages = new String[] {"sf363", "ff668", "ff676", };
			_validSpeedGrades = new String[] {"-12", "-11", "-10", };
			_topRows = 3;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VSX25 extends V4ConfigurationSpecification {

		public XC4VSX25() {
			super();
			_deviceName = "XC4VSX25";
			_deviceIDCode = "02068093";
			_validPackages = new String[] {"ff668", };
			_validSpeedGrades = new String[] {"-12", "-11", "-10", };
			_topRows = 2;
			_bottomRows = 2;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VSX35 extends V4ConfigurationSpecification {

		public XC4VSX35() {
			super();
			_deviceName = "XC4VSX35";
			_deviceIDCode = "02088093";
			_validPackages = new String[] {"ff668", };
			_validSpeedGrades = new String[] {"-12", "-11", "-10", };
			_topRows = 3;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VFX40 extends V4ConfigurationSpecification {

		public XC4VFX40() {
			super();
			_deviceName = "XC4VFX40";
			_deviceIDCode = "01e8c093";
			_validPackages = new String[] {"ff672", "ff1152", };
			_validSpeedGrades = new String[] {"-12", "-11", "-10", };
			_topRows = 3;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							MGT, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, MGT, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{MGT, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, MGT, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VLX40 extends V4ConfigurationSpecification {

		public XC4VLX40() {
			super();
			_deviceName = "XC4VLX40";
			_deviceIDCode = "016a4093";
			_validPackages = new String[] {"ff668", "ff1148", };
			_validSpeedGrades = new String[] {"-12", "-11", "-10", };
			_topRows = 4;
			_bottomRows = 4;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VSX55 extends V4ConfigurationSpecification {

		public XC4VSX55() {
			super();
			_deviceName = "XC4VSX55";
			_deviceIDCode = "020b0093";
			_validPackages = new String[] {"ff1148", };
			_validSpeedGrades = new String[] {"-12", "-11", "-10", };
			_topRows = 4;
			_bottomRows = 4;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VFX60 extends V4ConfigurationSpecification {

		public XC4VFX60() {
			super();
			_deviceName = "XC4VFX60";
			_deviceIDCode = "01eb4093";
			_validPackages = new String[] {"ff672", "ff1152", };
			_validSpeedGrades = new String[] {"-12", "-11", "-10", };
			_topRows = 4;
			_bottomRows = 4;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							MGT, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, MGT, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{MGT, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, MGT, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VLX60 extends V4ConfigurationSpecification {

		public XC4VLX60() {
			super();
			_deviceName = "XC4VLX60";
			_deviceIDCode = "016b4093";
			_validPackages = new String[] {"ff668", "ff1148", };
			_validSpeedGrades = new String[] {"-12", "-11", "-10", };
			_topRows = 4;
			_bottomRows = 4;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VLX80 extends V4ConfigurationSpecification {

		public XC4VLX80() {
			super();
			_deviceName = "XC4VLX80";
			_deviceIDCode = "016d8093";
			_validPackages = new String[] {"ff1148", };
			_validSpeedGrades = new String[] {"-12", "-11", "-10", };
			_topRows = 5;
			_bottomRows = 5;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VFX100 extends V4ConfigurationSpecification {

		public XC4VFX100() {
			super();
			_deviceName = "XC4VFX100";
			_deviceIDCode = "01ee4093";
			_validPackages = new String[] {"ff1152", "ff1517", };
			_validSpeedGrades = new String[] {"-12", "-11", "-10", };
			_topRows = 5;
			_bottomRows = 5;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							MGT, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, MGT, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{MGT, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, MGT, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VLX100 extends V4ConfigurationSpecification {

		public XC4VLX100() {
			super();
			_deviceName = "XC4VLX100";
			_deviceIDCode = "01700093";
			_validPackages = new String[] {"ff1148", "ff1513", };
			_validSpeedGrades = new String[] {"-12", "-11", "-10", };
			_topRows = 6;
			_bottomRows = 6;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VFX140 extends V4ConfigurationSpecification {

		public XC4VFX140() {
			super();
			_deviceName = "XC4VFX140";
			_deviceIDCode = "01f14093";
			_validPackages = new String[] {"ff1517", };
			_validSpeedGrades = new String[] {"-11", "-10", };
			_topRows = 6;
			_bottomRows = 6;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							MGT, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, CLB, CLB, MGT, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{MGT, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, MGT, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VLX160 extends V4ConfigurationSpecification {

		public XC4VLX160() {
			super();
			_deviceName = "XC4VLX160";
			_deviceIDCode = "01718093";
			_validPackages = new String[] {"ff1148", "ff1513", };
			_validSpeedGrades = new String[] {"-12", "-11", "-10", };
			_topRows = 6;
			_bottomRows = 6;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,}));
		}
	}

	class XC4VLX200 extends V4ConfigurationSpecification {

		public XC4VLX200() {
			super();
			_deviceName = "XC4VLX200";
			_deviceIDCode = "01734093";
			_validPackages = new String[] {"ff1513", };
			_validSpeedGrades = new String[] {"-11", "-10", };
			_topRows = 6;
			_bottomRows = 6;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[]{
							BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMINTERCONNECT, BRAMOVERHEAD,
					}),
					new BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = new ArrayList<>(Arrays.asList(new BlockSubType[]{IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, DSP, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, LOGIC_OVERHEAD,}));
		}
	}

}
