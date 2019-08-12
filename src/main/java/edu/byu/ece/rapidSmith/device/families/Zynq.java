/*
 * Copyright (c) 2018 Brigham Young University
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
package edu.byu.ece.rapidSmith.device.families;

import edu.byu.ece.rapidSmith.device.FamilyType;
import edu.byu.ece.rapidSmith.device.TileType;
import edu.byu.ece.rapidSmith.device.SiteType;
import java.util.*;

public final class Zynq implements FamilyInfo {

    /* ------ AUTO-GENERATED --- DO NOT EDIT BELOW ------ */
    public static final FamilyType FAMILY_TYPE = FamilyType.valueOf("ZYNQ");

    private static final ArrayList<String> _GENERATED_FROM = new ArrayList<>();

    public static final List<String> GENERATED_FROM = Collections.unmodifiableList(_GENERATED_FROM);

    @Override
    public List<String> generatedFrom() {
        return GENERATED_FROM;
    }

    private static final ArrayList<TileType> _TILE_TYPES = new ArrayList<>();

    public static final List<TileType> TILE_TYPES = Collections.unmodifiableList(_TILE_TYPES);

    @Override
    public List<TileType> tileTypes() {
        return TILE_TYPES;
    }

    private static final ArrayList<SiteType> _SITE_TYPES = new ArrayList<>();

    public static final List<SiteType> SITE_TYPES = Collections.unmodifiableList(_SITE_TYPES);

    @Override
    public List<SiteType> siteTypes() {
        return SITE_TYPES;
    }

    private static final HashSet<TileType> _CLB_TILES = new HashSet<>();

    public static final Set<TileType> CLB_TILES = Collections.unmodifiableSet(_CLB_TILES);

    @Override
    public Set<TileType> clbTiles() {
        return CLB_TILES;
    }

    private static final HashSet<TileType> _CLB_L_TILES = new HashSet<>();

    public static final Set<TileType> CLB_L_TILES = Collections.unmodifiableSet(_CLB_L_TILES);

    @Override
    public Set<TileType> clblTiles() {
        return CLB_L_TILES;
    }

    private static final HashSet<TileType> _CLB_R_TILES = new HashSet<>();

    public static final Set<TileType> CLB_R_TILES = Collections.unmodifiableSet(_CLB_R_TILES);

    @Override
    public Set<TileType> clbrTiles() {
        return CLB_R_TILES;
    }

    private static final HashSet<TileType> _SWITCHBOX_TILES = new HashSet<>();

    public static final Set<TileType> SWITCHBOX_TILES = Collections.unmodifiableSet(_SWITCHBOX_TILES);

    @Override
    public Set<TileType> switchboxTiles() {
        return SWITCHBOX_TILES;
    }

    private static final HashSet<TileType> _BRAM_TILES = new HashSet<>();

    public static final Set<TileType> BRAM_TILES = Collections.unmodifiableSet(_BRAM_TILES);

    @Override
    public Set<TileType> bramTiles() {
        return BRAM_TILES;
    }

    private static final HashSet<TileType> _DSP_TILES = new HashSet<>();

    public static final Set<TileType> DSP_TILES = Collections.unmodifiableSet(_DSP_TILES);

    @Override
    public Set<TileType> dspTiles() {
        return DSP_TILES;
    }

    private static final HashSet<TileType> _IO_TILES = new HashSet<>();

    public static final Set<TileType> IO_TILES = Collections.unmodifiableSet(_IO_TILES);

    @Override
    public Set<TileType> ioTiles() {
        return IO_TILES;
    }

    private static final HashSet<SiteType> _SLICE_SITES = new HashSet<>();

    public static final Set<SiteType> SLICE_SITES = Collections.unmodifiableSet(_SLICE_SITES);

    @Override
    public Set<SiteType> sliceSites() {
        return SLICE_SITES;
    }

    private static final HashSet<SiteType> _BRAM_SITES = new HashSet<>();

    public static final Set<SiteType> BRAM_SITES = Collections.unmodifiableSet(_BRAM_SITES);

    @Override
    public Set<SiteType> bramSites() {
        return BRAM_SITES;
    }

    private static final HashSet<SiteType> _DSP_SITES = new HashSet<>();

    public static final Set<SiteType> DSP_SITES = Collections.unmodifiableSet(_DSP_SITES);

    @Override
    public Set<SiteType> dspSites() {
        return DSP_SITES;
    }

    private static final HashSet<SiteType> _IO_SITES = new HashSet<>();

    public static final Set<SiteType> IO_SITES = Collections.unmodifiableSet(_IO_SITES);

    @Override
    public Set<SiteType> ioSites() {
        return IO_SITES;
    }

    private static final HashSet<SiteType> _FIFO_SITES = new HashSet<>();

    public static final Set<SiteType> FIFO_SITES = Collections.unmodifiableSet(_FIFO_SITES);

    @Override
    public Set<SiteType> fifoSites() {
        return FIFO_SITES;
    }

    public static final class TileTypes {

        public static final TileType INT_L = TileType.valueOf(FAMILY_TYPE, "INT_L");

        public static final TileType PCIE_NULL = TileType.valueOf(FAMILY_TYPE, "PCIE_NULL");

        public static final TileType CLBLL_L = TileType.valueOf(FAMILY_TYPE, "CLBLL_L");

        public static final TileType B_TERM_INT = TileType.valueOf(FAMILY_TYPE, "B_TERM_INT");

        public static final TileType RIOI3_SING = TileType.valueOf(FAMILY_TYPE, "RIOI3_SING");

        public static final TileType CLBLL_R = TileType.valueOf(FAMILY_TYPE, "CLBLL_R");

        public static final TileType NULL = TileType.valueOf(FAMILY_TYPE, "NULL");

        public static final TileType CLBLM_L = TileType.valueOf(FAMILY_TYPE, "CLBLM_L");

        public static final TileType HCLK_FEEDTHRU_1 = TileType.valueOf(FAMILY_TYPE, "HCLK_FEEDTHRU_1");

        public static final TileType BRKH_CLB = TileType.valueOf(FAMILY_TYPE, "BRKH_CLB");

        public static final TileType INT_INTERFACE_L = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_L");

        public static final TileType HCLK_VBRK = TileType.valueOf(FAMILY_TYPE, "HCLK_VBRK");

        public static final TileType CLBLM_R = TileType.valueOf(FAMILY_TYPE, "CLBLM_R");

        public static final TileType LIOI3_TBYTESRC = TileType.valueOf(FAMILY_TYPE, "LIOI3_TBYTESRC");

        public static final TileType MONITOR_MID_PELE1 = TileType.valueOf(FAMILY_TYPE, "MONITOR_MID_PELE1");

        public static final TileType BRAM_L = TileType.valueOf(FAMILY_TYPE, "BRAM_L");

        public static final TileType HCLK_R = TileType.valueOf(FAMILY_TYPE, "HCLK_R");

        public static final TileType HCLK_CLB = TileType.valueOf(FAMILY_TYPE, "HCLK_CLB");

        public static final TileType CLK_BUFG_TOP_R = TileType.valueOf(FAMILY_TYPE, "CLK_BUFG_TOP_R");

        public static final TileType CLK_BUFG_REBUF = TileType.valueOf(FAMILY_TYPE, "CLK_BUFG_REBUF");

        public static final TileType INT_R = TileType.valueOf(FAMILY_TYPE, "INT_R");

        public static final TileType HCLK_L = TileType.valueOf(FAMILY_TYPE, "HCLK_L");

        public static final TileType BRKH_INT_PSS = TileType.valueOf(FAMILY_TYPE, "BRKH_INT_PSS");

        public static final TileType HCLK_INT_INTERFACE = TileType.valueOf(FAMILY_TYPE, "HCLK_INT_INTERFACE");

        public static final TileType INT_FEEDTHRU_1 = TileType.valueOf(FAMILY_TYPE, "INT_FEEDTHRU_1");

        public static final TileType VBRK = TileType.valueOf(FAMILY_TYPE, "VBRK");

        public static final TileType T_TERM_INT = TileType.valueOf(FAMILY_TYPE, "T_TERM_INT");

        public static final TileType HCLK_BRAM = TileType.valueOf(FAMILY_TYPE, "HCLK_BRAM");

        public static final TileType BRKH_INT = TileType.valueOf(FAMILY_TYPE, "BRKH_INT");

        public static final TileType HCLK_CMT_L = TileType.valueOf(FAMILY_TYPE, "HCLK_CMT_L");

        public static final TileType HCLK_FEEDTHRU_2 = TileType.valueOf(FAMILY_TYPE, "HCLK_FEEDTHRU_2");

        public static final TileType HCLK_VFRAME = TileType.valueOf(FAMILY_TYPE, "HCLK_VFRAME");

        public static final TileType BRAM_R = TileType.valueOf(FAMILY_TYPE, "BRAM_R");

        public static final TileType HCLK_DSP_L = TileType.valueOf(FAMILY_TYPE, "HCLK_DSP_L");

        public static final TileType TERM_CMT = TileType.valueOf(FAMILY_TYPE, "TERM_CMT");

        public static final TileType BRAM_INT_INTERFACE_L = TileType.valueOf(FAMILY_TYPE, "BRAM_INT_INTERFACE_L");

        public static final TileType INT_INTERFACE_PSS_L = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_PSS_L");

        public static final TileType LIOB33_SING = TileType.valueOf(FAMILY_TYPE, "LIOB33_SING");

        public static final TileType HCLK_IOI3 = TileType.valueOf(FAMILY_TYPE, "HCLK_IOI3");

        public static final TileType RIOB33 = TileType.valueOf(FAMILY_TYPE, "RIOB33");

        public static final TileType BRKH_BRAM = TileType.valueOf(FAMILY_TYPE, "BRKH_BRAM");

        public static final TileType DSP_R = TileType.valueOf(FAMILY_TYPE, "DSP_R");

        public static final TileType CLK_FEED = TileType.valueOf(FAMILY_TYPE, "CLK_FEED");

        public static final TileType DSP_L = TileType.valueOf(FAMILY_TYPE, "DSP_L");

        public static final TileType INT_FEEDTHRU_2 = TileType.valueOf(FAMILY_TYPE, "INT_FEEDTHRU_2");

        public static final TileType BRKH_CMT = TileType.valueOf(FAMILY_TYPE, "BRKH_CMT");

        public static final TileType PSS2 = TileType.valueOf(FAMILY_TYPE, "PSS2");

        public static final TileType LIOI3_SING = TileType.valueOf(FAMILY_TYPE, "LIOI3_SING");

        public static final TileType BRKH_DSP_R = TileType.valueOf(FAMILY_TYPE, "BRKH_DSP_R");

        public static final TileType HCLK_IOB = TileType.valueOf(FAMILY_TYPE, "HCLK_IOB");

        public static final TileType BRKH_CLK = TileType.valueOf(FAMILY_TYPE, "BRKH_CLK");

        public static final TileType CLK_TERM = TileType.valueOf(FAMILY_TYPE, "CLK_TERM");

        public static final TileType RIOI3_TBYTETERM = TileType.valueOf(FAMILY_TYPE, "RIOI3_TBYTETERM");

        public static final TileType CMT_TOP_L_LOWER_T = TileType.valueOf(FAMILY_TYPE, "CMT_TOP_L_LOWER_T");

        public static final TileType CMT_TOP_L_UPPER_T = TileType.valueOf(FAMILY_TYPE, "CMT_TOP_L_UPPER_T");

        public static final TileType CMT_TOP_L_UPPER_B = TileType.valueOf(FAMILY_TYPE, "CMT_TOP_L_UPPER_B");

        public static final TileType BRKH_TERM_INT = TileType.valueOf(FAMILY_TYPE, "BRKH_TERM_INT");

        public static final TileType CLK_MTBF2 = TileType.valueOf(FAMILY_TYPE, "CLK_MTBF2");

        public static final TileType IO_INT_INTERFACE_L = TileType.valueOf(FAMILY_TYPE, "IO_INT_INTERFACE_L");

        public static final TileType HCLK_FIFO_L = TileType.valueOf(FAMILY_TYPE, "HCLK_FIFO_L");

        public static final TileType R_TERM_INT = TileType.valueOf(FAMILY_TYPE, "R_TERM_INT");

        public static final TileType HCLK_FEEDTHRU_1_PELE = TileType.valueOf(FAMILY_TYPE, "HCLK_FEEDTHRU_1_PELE");

        public static final TileType INT_INTERFACE_R = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_R");

        public static final TileType VFRAME = TileType.valueOf(FAMILY_TYPE, "VFRAME");

        public static final TileType CLK_PMVIOB = TileType.valueOf(FAMILY_TYPE, "CLK_PMVIOB");

        public static final TileType HCLK_DSP_R = TileType.valueOf(FAMILY_TYPE, "HCLK_DSP_R");

        public static final TileType RIOI3_TBYTESRC = TileType.valueOf(FAMILY_TYPE, "RIOI3_TBYTESRC");

        public static final TileType CMT_FIFO_L = TileType.valueOf(FAMILY_TYPE, "CMT_FIFO_L");

        public static final TileType RIOI3 = TileType.valueOf(FAMILY_TYPE, "RIOI3");

        public static final TileType CMT_TOP_L_LOWER_B = TileType.valueOf(FAMILY_TYPE, "CMT_TOP_L_LOWER_B");

        public static final TileType CMT_TOP_R_UPPER_B = TileType.valueOf(FAMILY_TYPE, "CMT_TOP_R_UPPER_B");

        public static final TileType CMT_PMV_L = TileType.valueOf(FAMILY_TYPE, "CMT_PMV_L");

        public static final TileType IO_INT_INTERFACE_R = TileType.valueOf(FAMILY_TYPE, "IO_INT_INTERFACE_R");

        public static final TileType CLK_BUFG_BOT_R = TileType.valueOf(FAMILY_TYPE, "CLK_BUFG_BOT_R");

        public static final TileType CLK_PMV2 = TileType.valueOf(FAMILY_TYPE, "CLK_PMV2");

        public static final TileType MONITOR_TOP_PELE1 = TileType.valueOf(FAMILY_TYPE, "MONITOR_TOP_PELE1");

        public static final TileType CFG_SECURITY_BOT_PELE1 = TileType.valueOf(FAMILY_TYPE, "CFG_SECURITY_BOT_PELE1");

        public static final TileType CLK_HROW_TOP_R = TileType.valueOf(FAMILY_TYPE, "CLK_HROW_TOP_R");

        public static final TileType CMT_TOP_R_UPPER_T = TileType.valueOf(FAMILY_TYPE, "CMT_TOP_R_UPPER_T");

        public static final TileType CMT_PMV = TileType.valueOf(FAMILY_TYPE, "CMT_PMV");

        public static final TileType BRAM_INT_INTERFACE_R = TileType.valueOf(FAMILY_TYPE, "BRAM_INT_INTERFACE_R");

        public static final TileType CMT_FIFO_R = TileType.valueOf(FAMILY_TYPE, "CMT_FIFO_R");

        public static final TileType HCLK_CMT = TileType.valueOf(FAMILY_TYPE, "HCLK_CMT");

        public static final TileType RIOB33_SING = TileType.valueOf(FAMILY_TYPE, "RIOB33_SING");

        public static final TileType BRKH_DSP_L = TileType.valueOf(FAMILY_TYPE, "BRKH_DSP_L");

        public static final TileType CMT_TOP_R_LOWER_T = TileType.valueOf(FAMILY_TYPE, "CMT_TOP_R_LOWER_T");

        public static final TileType HCLK_TERM = TileType.valueOf(FAMILY_TYPE, "HCLK_TERM");

        public static final TileType CFG_SECURITY_MID_PELE1 = TileType.valueOf(FAMILY_TYPE, "CFG_SECURITY_MID_PELE1");

        public static final TileType PSS0 = TileType.valueOf(FAMILY_TYPE, "PSS0");

        public static final TileType LIOI3 = TileType.valueOf(FAMILY_TYPE, "LIOI3");

        public static final TileType PSS1 = TileType.valueOf(FAMILY_TYPE, "PSS1");

        public static final TileType L_TERM_INT = TileType.valueOf(FAMILY_TYPE, "L_TERM_INT");

        public static final TileType CFG_SECURITY_TOP_PELE1 = TileType.valueOf(FAMILY_TYPE, "CFG_SECURITY_TOP_PELE1");

        public static final TileType CFG_CENTER_TOP = TileType.valueOf(FAMILY_TYPE, "CFG_CENTER_TOP");

        public static final TileType CFG_CENTER_MID = TileType.valueOf(FAMILY_TYPE, "CFG_CENTER_MID");

        public static final TileType CFG_CENTER_BOT = TileType.valueOf(FAMILY_TYPE, "CFG_CENTER_BOT");

        public static final TileType CLK_HROW_BOT_R = TileType.valueOf(FAMILY_TYPE, "CLK_HROW_BOT_R");

        public static final TileType PSS4 = TileType.valueOf(FAMILY_TYPE, "PSS4");

        public static final TileType LIOI3_TBYTETERM = TileType.valueOf(FAMILY_TYPE, "LIOI3_TBYTETERM");

        public static final TileType LIOB33 = TileType.valueOf(FAMILY_TYPE, "LIOB33");

        public static final TileType MONITOR_BOT_PELE1 = TileType.valueOf(FAMILY_TYPE, "MONITOR_BOT_PELE1");

        public static final TileType CMT_TOP_R_LOWER_B = TileType.valueOf(FAMILY_TYPE, "CMT_TOP_R_LOWER_B");

        public static final TileType CLK_PMV2_SVT = TileType.valueOf(FAMILY_TYPE, "CLK_PMV2_SVT");

        public static final TileType PSS3 = TileType.valueOf(FAMILY_TYPE, "PSS3");

        public static final TileType CLK_PMV = TileType.valueOf(FAMILY_TYPE, "CLK_PMV");
    }

    public static final class SiteTypes {

        public static final SiteType TIEOFF = SiteType.valueOf(FAMILY_TYPE, "TIEOFF");

        public static final SiteType SLICEL = SiteType.valueOf(FAMILY_TYPE, "SLICEL");

        public static final SiteType IDELAYE2 = SiteType.valueOf(FAMILY_TYPE, "IDELAYE2");

        public static final SiteType ILOGICE3 = SiteType.valueOf(FAMILY_TYPE, "ILOGICE3");

        public static final SiteType ILOGICE2 = SiteType.valueOf(FAMILY_TYPE, "ILOGICE2");

        public static final SiteType ISERDESE2 = SiteType.valueOf(FAMILY_TYPE, "ISERDESE2");

        public static final SiteType OLOGICE3 = SiteType.valueOf(FAMILY_TYPE, "OLOGICE3");

        public static final SiteType OLOGICE2 = SiteType.valueOf(FAMILY_TYPE, "OLOGICE2");

        public static final SiteType OSERDESE2 = SiteType.valueOf(FAMILY_TYPE, "OSERDESE2");

        public static final SiteType SLICEM = SiteType.valueOf(FAMILY_TYPE, "SLICEM");

        public static final SiteType FIFO18E1 = SiteType.valueOf(FAMILY_TYPE, "FIFO18E1");

        public static final SiteType RAMB18E1 = SiteType.valueOf(FAMILY_TYPE, "RAMB18E1");

        public static final SiteType RAMBFIFO36E1 = SiteType.valueOf(FAMILY_TYPE, "RAMBFIFO36E1");

        public static final SiteType FIFO36E1 = SiteType.valueOf(FAMILY_TYPE, "FIFO36E1");

        public static final SiteType RAMB36E1 = SiteType.valueOf(FAMILY_TYPE, "RAMB36E1");

        public static final SiteType BUFGCTRL = SiteType.valueOf(FAMILY_TYPE, "BUFGCTRL");

        public static final SiteType BUFG = SiteType.valueOf(FAMILY_TYPE, "BUFG");

        public static final SiteType BUFMRCE = SiteType.valueOf(FAMILY_TYPE, "BUFMRCE");

        public static final SiteType IOB33 = SiteType.valueOf(FAMILY_TYPE, "IOB33");

        public static final SiteType BUFIO = SiteType.valueOf(FAMILY_TYPE, "BUFIO");

        public static final SiteType BUFR = SiteType.valueOf(FAMILY_TYPE, "BUFR");

        public static final SiteType IDELAYCTRL = SiteType.valueOf(FAMILY_TYPE, "IDELAYCTRL");

        public static final SiteType IOB33S = SiteType.valueOf(FAMILY_TYPE, "IOB33S");

        public static final SiteType IPAD = SiteType.valueOf(FAMILY_TYPE, "IPAD");

        public static final SiteType IOB33M = SiteType.valueOf(FAMILY_TYPE, "IOB33M");

        public static final SiteType DSP48E1 = SiteType.valueOf(FAMILY_TYPE, "DSP48E1");

        public static final SiteType IOPAD = SiteType.valueOf(FAMILY_TYPE, "IOPAD");

        public static final SiteType PS7 = SiteType.valueOf(FAMILY_TYPE, "PS7");

        public static final SiteType PHASER_IN_PHY = SiteType.valueOf(FAMILY_TYPE, "PHASER_IN_PHY");

        public static final SiteType PHASER_IN = SiteType.valueOf(FAMILY_TYPE, "PHASER_IN");

        public static final SiteType PHASER_IN_ADV = SiteType.valueOf(FAMILY_TYPE, "PHASER_IN_ADV");

        public static final SiteType PHASER_OUT_PHY = SiteType.valueOf(FAMILY_TYPE, "PHASER_OUT_PHY");

        public static final SiteType PHASER_OUT = SiteType.valueOf(FAMILY_TYPE, "PHASER_OUT");

        public static final SiteType PHASER_OUT_ADV = SiteType.valueOf(FAMILY_TYPE, "PHASER_OUT_ADV");

        public static final SiteType PLLE2_ADV = SiteType.valueOf(FAMILY_TYPE, "PLLE2_ADV");

        public static final SiteType PHASER_REF = SiteType.valueOf(FAMILY_TYPE, "PHASER_REF");

        public static final SiteType PHY_CONTROL = SiteType.valueOf(FAMILY_TYPE, "PHY_CONTROL");

        public static final SiteType IN_FIFO = SiteType.valueOf(FAMILY_TYPE, "IN_FIFO");

        public static final SiteType OUT_FIFO = SiteType.valueOf(FAMILY_TYPE, "OUT_FIFO");

        public static final SiteType MMCME2_ADV = SiteType.valueOf(FAMILY_TYPE, "MMCME2_ADV");

        public static final SiteType PMV2 = SiteType.valueOf(FAMILY_TYPE, "PMV2");

        public static final SiteType BUFHCE = SiteType.valueOf(FAMILY_TYPE, "BUFHCE");

        public static final SiteType DNA_PORT = SiteType.valueOf(FAMILY_TYPE, "DNA_PORT");

        public static final SiteType EFUSE_USR = SiteType.valueOf(FAMILY_TYPE, "EFUSE_USR");

        public static final SiteType BSCAN = SiteType.valueOf(FAMILY_TYPE, "BSCAN");

        public static final SiteType CAPTURE = SiteType.valueOf(FAMILY_TYPE, "CAPTURE");

        public static final SiteType DCIRESET = SiteType.valueOf(FAMILY_TYPE, "DCIRESET");

        public static final SiteType FRAME_ECC = SiteType.valueOf(FAMILY_TYPE, "FRAME_ECC");

        public static final SiteType ICAP = SiteType.valueOf(FAMILY_TYPE, "ICAP");

        public static final SiteType STARTUP = SiteType.valueOf(FAMILY_TYPE, "STARTUP");

        public static final SiteType USR_ACCESS = SiteType.valueOf(FAMILY_TYPE, "USR_ACCESS");

        public static final SiteType XADC = SiteType.valueOf(FAMILY_TYPE, "XADC");
    }

    static {
        _GENERATED_FROM.add("xc7z020clg400");
        _TILE_TYPES.add(TileTypes.INT_L);
        _TILE_TYPES.add(TileTypes.PCIE_NULL);
        _TILE_TYPES.add(TileTypes.CLBLL_L);
        _TILE_TYPES.add(TileTypes.B_TERM_INT);
        _TILE_TYPES.add(TileTypes.RIOI3_SING);
        _TILE_TYPES.add(TileTypes.CLBLL_R);
        _TILE_TYPES.add(TileTypes.NULL);
        _TILE_TYPES.add(TileTypes.CLBLM_L);
        _TILE_TYPES.add(TileTypes.HCLK_FEEDTHRU_1);
        _TILE_TYPES.add(TileTypes.BRKH_CLB);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_L);
        _TILE_TYPES.add(TileTypes.HCLK_VBRK);
        _TILE_TYPES.add(TileTypes.CLBLM_R);
        _TILE_TYPES.add(TileTypes.LIOI3_TBYTESRC);
        _TILE_TYPES.add(TileTypes.MONITOR_MID_PELE1);
        _TILE_TYPES.add(TileTypes.BRAM_L);
        _TILE_TYPES.add(TileTypes.HCLK_R);
        _TILE_TYPES.add(TileTypes.HCLK_CLB);
        _TILE_TYPES.add(TileTypes.CLK_BUFG_TOP_R);
        _TILE_TYPES.add(TileTypes.CLK_BUFG_REBUF);
        _TILE_TYPES.add(TileTypes.INT_R);
        _TILE_TYPES.add(TileTypes.HCLK_L);
        _TILE_TYPES.add(TileTypes.BRKH_INT_PSS);
        _TILE_TYPES.add(TileTypes.HCLK_INT_INTERFACE);
        _TILE_TYPES.add(TileTypes.INT_FEEDTHRU_1);
        _TILE_TYPES.add(TileTypes.VBRK);
        _TILE_TYPES.add(TileTypes.T_TERM_INT);
        _TILE_TYPES.add(TileTypes.HCLK_BRAM);
        _TILE_TYPES.add(TileTypes.BRKH_INT);
        _TILE_TYPES.add(TileTypes.HCLK_CMT_L);
        _TILE_TYPES.add(TileTypes.HCLK_FEEDTHRU_2);
        _TILE_TYPES.add(TileTypes.HCLK_VFRAME);
        _TILE_TYPES.add(TileTypes.BRAM_R);
        _TILE_TYPES.add(TileTypes.HCLK_DSP_L);
        _TILE_TYPES.add(TileTypes.TERM_CMT);
        _TILE_TYPES.add(TileTypes.BRAM_INT_INTERFACE_L);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_PSS_L);
        _TILE_TYPES.add(TileTypes.LIOB33_SING);
        _TILE_TYPES.add(TileTypes.HCLK_IOI3);
        _TILE_TYPES.add(TileTypes.RIOB33);
        _TILE_TYPES.add(TileTypes.BRKH_BRAM);
        _TILE_TYPES.add(TileTypes.DSP_R);
        _TILE_TYPES.add(TileTypes.CLK_FEED);
        _TILE_TYPES.add(TileTypes.DSP_L);
        _TILE_TYPES.add(TileTypes.INT_FEEDTHRU_2);
        _TILE_TYPES.add(TileTypes.BRKH_CMT);
        _TILE_TYPES.add(TileTypes.PSS2);
        _TILE_TYPES.add(TileTypes.LIOI3_SING);
        _TILE_TYPES.add(TileTypes.BRKH_DSP_R);
        _TILE_TYPES.add(TileTypes.HCLK_IOB);
        _TILE_TYPES.add(TileTypes.BRKH_CLK);
        _TILE_TYPES.add(TileTypes.CLK_TERM);
        _TILE_TYPES.add(TileTypes.RIOI3_TBYTETERM);
        _TILE_TYPES.add(TileTypes.CMT_TOP_L_LOWER_T);
        _TILE_TYPES.add(TileTypes.CMT_TOP_L_UPPER_T);
        _TILE_TYPES.add(TileTypes.CMT_TOP_L_UPPER_B);
        _TILE_TYPES.add(TileTypes.BRKH_TERM_INT);
        _TILE_TYPES.add(TileTypes.CLK_MTBF2);
        _TILE_TYPES.add(TileTypes.IO_INT_INTERFACE_L);
        _TILE_TYPES.add(TileTypes.HCLK_FIFO_L);
        _TILE_TYPES.add(TileTypes.R_TERM_INT);
        _TILE_TYPES.add(TileTypes.HCLK_FEEDTHRU_1_PELE);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_R);
        _TILE_TYPES.add(TileTypes.VFRAME);
        _TILE_TYPES.add(TileTypes.CLK_PMVIOB);
        _TILE_TYPES.add(TileTypes.HCLK_DSP_R);
        _TILE_TYPES.add(TileTypes.RIOI3_TBYTESRC);
        _TILE_TYPES.add(TileTypes.CMT_FIFO_L);
        _TILE_TYPES.add(TileTypes.RIOI3);
        _TILE_TYPES.add(TileTypes.CMT_TOP_L_LOWER_B);
        _TILE_TYPES.add(TileTypes.CMT_TOP_R_UPPER_B);
        _TILE_TYPES.add(TileTypes.CMT_PMV_L);
        _TILE_TYPES.add(TileTypes.IO_INT_INTERFACE_R);
        _TILE_TYPES.add(TileTypes.CLK_BUFG_BOT_R);
        _TILE_TYPES.add(TileTypes.CLK_PMV2);
        _TILE_TYPES.add(TileTypes.MONITOR_TOP_PELE1);
        _TILE_TYPES.add(TileTypes.CFG_SECURITY_BOT_PELE1);
        _TILE_TYPES.add(TileTypes.CLK_HROW_TOP_R);
        _TILE_TYPES.add(TileTypes.CMT_TOP_R_UPPER_T);
        _TILE_TYPES.add(TileTypes.CMT_PMV);
        _TILE_TYPES.add(TileTypes.BRAM_INT_INTERFACE_R);
        _TILE_TYPES.add(TileTypes.CMT_FIFO_R);
        _TILE_TYPES.add(TileTypes.HCLK_CMT);
        _TILE_TYPES.add(TileTypes.RIOB33_SING);
        _TILE_TYPES.add(TileTypes.BRKH_DSP_L);
        _TILE_TYPES.add(TileTypes.CMT_TOP_R_LOWER_T);
        _TILE_TYPES.add(TileTypes.HCLK_TERM);
        _TILE_TYPES.add(TileTypes.CFG_SECURITY_MID_PELE1);
        _TILE_TYPES.add(TileTypes.PSS0);
        _TILE_TYPES.add(TileTypes.LIOI3);
        _TILE_TYPES.add(TileTypes.PSS1);
        _TILE_TYPES.add(TileTypes.L_TERM_INT);
        _TILE_TYPES.add(TileTypes.CFG_SECURITY_TOP_PELE1);
        _TILE_TYPES.add(TileTypes.CFG_CENTER_TOP);
        _TILE_TYPES.add(TileTypes.CFG_CENTER_MID);
        _TILE_TYPES.add(TileTypes.CFG_CENTER_BOT);
        _TILE_TYPES.add(TileTypes.CLK_HROW_BOT_R);
        _TILE_TYPES.add(TileTypes.PSS4);
        _TILE_TYPES.add(TileTypes.LIOI3_TBYTETERM);
        _TILE_TYPES.add(TileTypes.LIOB33);
        _TILE_TYPES.add(TileTypes.MONITOR_BOT_PELE1);
        _TILE_TYPES.add(TileTypes.CMT_TOP_R_LOWER_B);
        _TILE_TYPES.add(TileTypes.CLK_PMV2_SVT);
        _TILE_TYPES.add(TileTypes.PSS3);
        _TILE_TYPES.add(TileTypes.CLK_PMV);
        _SITE_TYPES.add(SiteTypes.TIEOFF);
        _SITE_TYPES.add(SiteTypes.SLICEL);
        _SITE_TYPES.add(SiteTypes.IDELAYE2);
        _SITE_TYPES.add(SiteTypes.ILOGICE3);
        _SITE_TYPES.add(SiteTypes.ILOGICE2);
        _SITE_TYPES.add(SiteTypes.ISERDESE2);
        _SITE_TYPES.add(SiteTypes.OLOGICE3);
        _SITE_TYPES.add(SiteTypes.OLOGICE2);
        _SITE_TYPES.add(SiteTypes.OSERDESE2);
        _SITE_TYPES.add(SiteTypes.SLICEM);
        _SITE_TYPES.add(SiteTypes.FIFO18E1);
        _SITE_TYPES.add(SiteTypes.RAMB18E1);
        _SITE_TYPES.add(SiteTypes.RAMBFIFO36E1);
        _SITE_TYPES.add(SiteTypes.FIFO36E1);
        _SITE_TYPES.add(SiteTypes.RAMB36E1);
        _SITE_TYPES.add(SiteTypes.BUFGCTRL);
        _SITE_TYPES.add(SiteTypes.BUFG);
        _SITE_TYPES.add(SiteTypes.BUFMRCE);
        _SITE_TYPES.add(SiteTypes.IOB33);
        _SITE_TYPES.add(SiteTypes.BUFIO);
        _SITE_TYPES.add(SiteTypes.BUFR);
        _SITE_TYPES.add(SiteTypes.IDELAYCTRL);
        _SITE_TYPES.add(SiteTypes.IOB33S);
        _SITE_TYPES.add(SiteTypes.IPAD);
        _SITE_TYPES.add(SiteTypes.IOB33M);
        _SITE_TYPES.add(SiteTypes.DSP48E1);
        _SITE_TYPES.add(SiteTypes.IOPAD);
        _SITE_TYPES.add(SiteTypes.PS7);
        _SITE_TYPES.add(SiteTypes.PHASER_IN_PHY);
        _SITE_TYPES.add(SiteTypes.PHASER_IN);
        _SITE_TYPES.add(SiteTypes.PHASER_IN_ADV);
        _SITE_TYPES.add(SiteTypes.PHASER_OUT_PHY);
        _SITE_TYPES.add(SiteTypes.PHASER_OUT);
        _SITE_TYPES.add(SiteTypes.PHASER_OUT_ADV);
        _SITE_TYPES.add(SiteTypes.PLLE2_ADV);
        _SITE_TYPES.add(SiteTypes.PHASER_REF);
        _SITE_TYPES.add(SiteTypes.PHY_CONTROL);
        _SITE_TYPES.add(SiteTypes.IN_FIFO);
        _SITE_TYPES.add(SiteTypes.OUT_FIFO);
        _SITE_TYPES.add(SiteTypes.MMCME2_ADV);
        _SITE_TYPES.add(SiteTypes.PMV2);
        _SITE_TYPES.add(SiteTypes.BUFHCE);
        _SITE_TYPES.add(SiteTypes.DNA_PORT);
        _SITE_TYPES.add(SiteTypes.EFUSE_USR);
        _SITE_TYPES.add(SiteTypes.BSCAN);
        _SITE_TYPES.add(SiteTypes.CAPTURE);
        _SITE_TYPES.add(SiteTypes.DCIRESET);
        _SITE_TYPES.add(SiteTypes.FRAME_ECC);
        _SITE_TYPES.add(SiteTypes.ICAP);
        _SITE_TYPES.add(SiteTypes.STARTUP);
        _SITE_TYPES.add(SiteTypes.USR_ACCESS);
        _SITE_TYPES.add(SiteTypes.XADC);
    }

    /* ------ AUTO-GENERATED --- DO NOT EDIT ABOVE ------ */
    static {
        /* ------ CLASSIFICATIONS GO HERE ------ */
        /* ***********************
         * 		Tile Types
         * ***********************/
        _CLB_TILES.add(TileTypes.CLBLL_L);
        _CLB_TILES.add(TileTypes.CLBLL_R);
        _CLB_TILES.add(TileTypes.CLBLM_L);
        _CLB_TILES.add(TileTypes.CLBLM_R);
        _CLB_L_TILES.add(TileTypes.CLBLL_L);
        _CLB_L_TILES.add(TileTypes.CLBLM_L);
        _CLB_R_TILES.add(TileTypes.CLBLL_R);
        _CLB_R_TILES.add(TileTypes.CLBLM_R);
        _SWITCHBOX_TILES.add(TileTypes.INT_L);
        _SWITCHBOX_TILES.add(TileTypes.INT_R);
        _BRAM_TILES.add(TileTypes.BRAM_L);
        _BRAM_TILES.add(TileTypes.BRAM_R);
        _DSP_TILES.add(TileTypes.DSP_L);
        _DSP_TILES.add(TileTypes.DSP_R);
        _IO_TILES.add(TileTypes.LIOB33_SING);
        _IO_TILES.add(TileTypes.LIOB33);
        _IO_TILES.add(TileTypes.RIOB33);
        _IO_TILES.add(TileTypes.RIOB33_SING);
        /* ***********************
         * 		Site Types
         * ***********************/
        _SLICE_SITES.add(SiteTypes.SLICEL);
        _SLICE_SITES.add(SiteTypes.SLICEM);
        _BRAM_SITES.add(SiteTypes.RAMB18E1);
        _BRAM_SITES.add(SiteTypes.RAMB36E1);
        _BRAM_SITES.add(SiteTypes.RAMBFIFO36E1);
        _FIFO_SITES.add(SiteTypes.FIFO18E1);
        _FIFO_SITES.add(SiteTypes.FIFO36E1);
        _FIFO_SITES.add(SiteTypes.IN_FIFO);
        _FIFO_SITES.add(SiteTypes.OUT_FIFO);
        _FIFO_SITES.add(SiteTypes.RAMBFIFO36E1);
        _DSP_SITES.add(SiteTypes.DSP48E1);
        _IO_SITES.add(SiteTypes.IOB33);
        _IO_SITES.add(SiteTypes.IOB33S);
        _IO_SITES.add(SiteTypes.IOB33M);
        _IO_SITES.add(SiteTypes.IPAD);
        _IO_SITES.add(SiteTypes.IOPAD);
    }
}
