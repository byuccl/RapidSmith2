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
 * This file was auto-generated on Tue Feb 02 12:38:22 MST 2010
 * by edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxV5DeviceClassGenerator.
 * See the source code to make changes.
 *
 * Do not modify this file directly.
 */


package edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification;

import java.util.ArrayList;
import java.util.Arrays;

public class V5PartLibrary extends PartLibrary {

	public V5PartLibrary() {
		super();
	}

	protected void addParts() {
		addPart(new XC5VLX20T());
		addPart(new XC5VLX30());
		addPart(new XC5VFX30T());
		addPart(new XC5VLX30T());
		addPart(new XC5VSX35T());
		addPart(new XC5VLX50());
		addPart(new XC5VLX50T());
		addPart(new XC5VSX50T());
		addPart(new XC5VFX70T());
		addPart(new XC5VLX85());
		addPart(new XC5VLX85T());
		addPart(new XC5VSX95T());
		addPart(new XC5VFX100T());
		addPart(new XC5VLX110());
		addPart(new XC5VLX110T());
		addPart(new XC5VFX130T());
		addPart(new XC5VTX150T());
		addPart(new XC5VLX155());
		addPart(new XC5VLX155T());
		addPart(new XC5VFX200T());
		addPart(new XC5VLX220());
		addPart(new XC5VLX220T());
		addPart(new XC5VSX240T());
		addPart(new XC5VTX240T());
		addPart(new XC5VLX330());
		addPart(new XC5VLX330T());
	}

	class XC5VLX20T extends V5ConfigurationSpecification {

		public XC5VLX20T() {
			super();
			_deviceName = "XC5VLX20T";
			_deviceIDCode = "02a56093";
			_validPackages = new String[] {"ff323", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 1;
			_bottomRows = 2;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTP, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VLX30 extends V5ConfigurationSpecification {

		public XC5VLX30() {
			super();
			_deviceName = "XC5VLX30";
			_deviceIDCode = "0286e093";
			_validPackages = new String[] {"ff324", "ff676", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 2;
			_bottomRows = 2;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VFX30T extends V5ConfigurationSpecification {

		public XC5VFX30T() {
			super();
			_deviceName = "XC5VFX30T";
			_deviceIDCode = "03276093";
			_validPackages = new String[] {"ff665", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 2;
			_bottomRows = 2;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VLX30T extends V5ConfigurationSpecification {

		public XC5VLX30T() {
			super();
			_deviceName = "XC5VLX30T";
			_deviceIDCode = "02a6e093";
			_validPackages = new String[] {"ff323", "ff665", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 2;
			_bottomRows = 2;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTP, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VSX35T extends V5ConfigurationSpecification {

		public XC5VSX35T() {
			super();
			_deviceName = "XC5VSX35T";
			_deviceIDCode = "02e72093";
			_validPackages = new String[] {"ff665", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 2;
			_bottomRows = 2;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, IOB, CLK, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTP, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VLX50 extends V5ConfigurationSpecification {

		public XC5VLX50() {
			super();
			_deviceName = "XC5VLX50";
			_deviceIDCode = "02896093";
			_validPackages = new String[] {"ff324", "ff676", "ff1153", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 3;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VLX50T extends V5ConfigurationSpecification {

		public XC5VLX50T() {
			super();
			_deviceName = "XC5VLX50T";
			_deviceIDCode = "02a96093";
			_validPackages = new String[] {"ff1136", "ff665", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 3;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTP, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VSX50T extends V5ConfigurationSpecification {

		public XC5VSX50T() {
			super();
			_deviceName = "XC5VSX50T";
			_deviceIDCode = "02e9a093";
			_validPackages = new String[] {"ff1136", "ff665", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 3;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, IOB, CLK, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTP, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VFX70T extends V5ConfigurationSpecification {

		public XC5VFX70T() {
			super();
			_deviceName = "XC5VFX70T";
			_deviceIDCode = "032c6093";
			_validPackages = new String[] {"ff1136", "ff665", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 4;
			_bottomRows = 4;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VLX85 extends V5ConfigurationSpecification {

		public XC5VLX85() {
			super();
			_deviceName = "XC5VLX85";
			_deviceIDCode = "028ae093";
			_validPackages = new String[] {"ff676", "ff1153", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 3;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VLX85T extends V5ConfigurationSpecification {

		public XC5VLX85T() {
			super();
			_deviceName = "XC5VLX85T";
			_deviceIDCode = "02aae093";
			_validPackages = new String[] {"ff1136", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 3;
			_bottomRows = 3;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTP, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VSX95T extends V5ConfigurationSpecification {

		public XC5VSX95T() {
			super();
			_deviceName = "XC5VSX95T";
			_deviceIDCode = "02ece093";
			_validPackages = new String[] {"ff1136", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 4;
			_bottomRows = 4;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, IOB, CLK, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTP, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VFX100T extends V5ConfigurationSpecification {

		public XC5VFX100T() {
			super();
			_deviceName = "XC5VFX100T";
			_deviceIDCode = "032d8093";
			_validPackages = new String[] {"ff1738", "ff1136", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 4;
			_bottomRows = 4;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VLX110 extends V5ConfigurationSpecification {

		public XC5VLX110() {
			super();
			_deviceName = "XC5VLX110";
			_deviceIDCode = "028d6093";
			_validPackages = new String[] {"ff676", "ff1153", "ff1760", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 4;
			_bottomRows = 4;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VLX110T extends V5ConfigurationSpecification {

		public XC5VLX110T() {
			super();
			_deviceName = "XC5VLX110T";
			_deviceIDCode = "02ad6093";
			_validPackages = new String[] {"ff1136", "ff1738", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 4;
			_bottomRows = 4;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTP, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VFX130T extends V5ConfigurationSpecification {

		public XC5VFX130T() {
			super();
			_deviceName = "XC5VFX130T";
			_deviceIDCode = "03300093";
			_validPackages = new String[] {"ff1738", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 5;
			_bottomRows = 5;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VTX150T extends V5ConfigurationSpecification {

		public XC5VTX150T() {
			super();
			_deviceName = "XC5VTX150T";
			_deviceIDCode = "04502093";
			_validPackages = new String[] {"ff1156", "ff1759", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 5;
			_bottomRows = 5;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							GTX, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VLX155 extends V5ConfigurationSpecification {

		public XC5VLX155() {
			super();
			_deviceName = "XC5VLX155";
			_deviceIDCode = "028ec093";
			_validPackages = new String[] {"ff1153", "ff1760", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 4;
			_bottomRows = 4;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VLX155T extends V5ConfigurationSpecification {

		public XC5VLX155T() {
			super();
			_deviceName = "XC5VLX155T";
			_deviceIDCode = "02aec093";
			_validPackages = new String[] {"ff1136", "ff1738", };
			_validSpeedGrades = new String[] {"-3", "-2", "-1", };
			_topRows = 4;
			_bottomRows = 4;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTP, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VFX200T extends V5ConfigurationSpecification {

		public XC5VFX200T() {
			super();
			_deviceName = "XC5VFX200T";
			_deviceIDCode = "03334093";
			_validPackages = new String[] {"ff1738", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 6;
			_bottomRows = 6;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VLX220 extends V5ConfigurationSpecification {

		public XC5VLX220() {
			super();
			_deviceName = "XC5VLX220";
			_deviceIDCode = "0290c093";
			_validPackages = new String[] {"ff1760", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 4;
			_bottomRows = 4;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VLX220T extends V5ConfigurationSpecification {

		public XC5VLX220T() {
			super();
			_deviceName = "XC5VLX220T";
			_deviceIDCode = "02b0c093";
			_validPackages = new String[] {"ff1738", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 4;
			_bottomRows = 4;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTP, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VSX240T extends V5ConfigurationSpecification {

		public XC5VSX240T() {
			super();
			_deviceName = "XC5VSX240T";
			_deviceIDCode = "02f3e093";
			_validPackages = new String[] {"ff1738", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 6;
			_bottomRows = 6;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, IOB, CLK, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTP, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VTX240T extends V5ConfigurationSpecification {

		public XC5VTX240T() {
			super();
			_deviceName = "XC5VTX240T";
			_deviceIDCode = "0453e093";
			_validPackages = new String[] {"ff1759", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 6;
			_bottomRows = 6;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							GTX, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTX, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VLX330 extends V5ConfigurationSpecification {

		public XC5VLX330() {
			super();
			_deviceName = "XC5VLX330";
			_deviceIDCode = "0295c093";
			_validPackages = new String[] {"ff1760", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 6;
			_bottomRows = 6;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

	class XC5VLX330T extends V5ConfigurationSpecification {

		public XC5VLX330T() {
			super();
			_deviceName = "XC5VLX330T";
			_deviceIDCode = "02b5c093";
			_validPackages = new String[] {"ff1738", };
			_validSpeedGrades = new String[] {"-2", "-1", };
			_topRows = 6;
			_bottomRows = 6;
			_blockTypeLayouts = new ArrayList<>(Arrays.asList(new BlockTypeInstance[]{
					new BlockTypeInstance(LOGIC_INTERCONNECT_BLOCKTYPE, new BlockSubType[]{
							IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, DSP, CLB, CLB, DSP, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, IOB, CLK, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, CLB, CLB, CLB, CLB, IOB, CLB, CLB, CLB, CLB, BRAMINTERCONNECT, GTP, LOGIC_OVERHEAD,
					}),
					new BlockTypeInstance(BRAM_CONTENT_BLOCKTYPE, new BlockSubType[]{
							BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMCONTENT, BRAMOVERHEAD,
					}),
			}));
			_overallColumnLayout = _blockTypeLayouts.get(0).getColumnLayout();
		}
	}

}
