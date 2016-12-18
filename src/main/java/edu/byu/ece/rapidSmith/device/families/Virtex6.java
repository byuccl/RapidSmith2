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

package edu.byu.ece.rapidSmith.device.families;

import edu.byu.ece.rapidSmith.device.FamilyType;
import edu.byu.ece.rapidSmith.device.TileType;
import edu.byu.ece.rapidSmith.device.SiteType;
import java.util.*;

public final class Virtex6 implements FamilyInfo {

    /* ------ AUTO-GENERATED --- DO NOT EDIT BELOW ------ */
    public static final FamilyType FAMILY_TYPE = FamilyType.valueOf("VIRTEX6");

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

    public static final class TileTypes {

        public static final TileType VBRK = TileType.valueOf(FAMILY_TYPE, "VBRK");

        public static final TileType LIOI = TileType.valueOf(FAMILY_TYPE, "LIOI");

        public static final TileType B_TERM_DSP = TileType.valueOf(FAMILY_TYPE, "B_TERM_DSP");

        public static final TileType INT = TileType.valueOf(FAMILY_TYPE, "INT");

        public static final TileType CLBLM = TileType.valueOf(FAMILY_TYPE, "CLBLM");

        public static final TileType T_TERM_INT = TileType.valueOf(FAMILY_TYPE, "T_TERM_INT");

        public static final TileType HCLK = TileType.valueOf(FAMILY_TYPE, "HCLK");

        public static final TileType CENTER_SPACE2 = TileType.valueOf(FAMILY_TYPE, "CENTER_SPACE2");

        public static final TileType NULL = TileType.valueOf(FAMILY_TYPE, "NULL");

        public static final TileType B_TERM_INT = TileType.valueOf(FAMILY_TYPE, "B_TERM_INT");

        public static final TileType BRKH_B_TERM_INT = TileType.valueOf(FAMILY_TYPE, "BRKH_B_TERM_INT");

        public static final TileType BRKH = TileType.valueOf(FAMILY_TYPE, "BRKH");

        public static final TileType BRKH_INT = TileType.valueOf(FAMILY_TYPE, "BRKH_INT");

        public static final TileType BRKH_CLB = TileType.valueOf(FAMILY_TYPE, "BRKH_CLB");

        public static final TileType BRAM = TileType.valueOf(FAMILY_TYPE, "BRAM");

        public static final TileType CMT_BUFG_TOP = TileType.valueOf(FAMILY_TYPE, "CMT_BUFG_TOP");

        public static final TileType CLBLL = TileType.valueOf(FAMILY_TYPE, "CLBLL");

        public static final TileType HCLK_BRAM = TileType.valueOf(FAMILY_TYPE, "HCLK_BRAM");

        public static final TileType INT_INTERFACE = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE");

        public static final TileType BRKH_INT_INTERFACE = TileType.valueOf(FAMILY_TYPE, "BRKH_INT_INTERFACE");

        public static final TileType GTX = TileType.valueOf(FAMILY_TYPE, "GTX");

        public static final TileType INT_INTERFACE_TERM = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_TERM");

        public static final TileType BRKH_T_TERM_INT = TileType.valueOf(FAMILY_TYPE, "BRKH_T_TERM_INT");

        public static final TileType CENTER_SPACE_HCLK2 = TileType.valueOf(FAMILY_TYPE, "CENTER_SPACE_HCLK2");

        public static final TileType HCLK_INT_INTERFACE = TileType.valueOf(FAMILY_TYPE, "HCLK_INT_INTERFACE");

        public static final TileType CENTER_SPACE_HCLK1 = TileType.valueOf(FAMILY_TYPE, "CENTER_SPACE_HCLK1");

        public static final TileType HCLK_CLBLM = TileType.valueOf(FAMILY_TYPE, "HCLK_CLBLM");

        public static final TileType CFG_CENTER_1 = TileType.valueOf(FAMILY_TYPE, "CFG_CENTER_1");

        public static final TileType LIOB_FT = TileType.valueOf(FAMILY_TYPE, "LIOB_FT");

        public static final TileType CENTER_SPACE1 = TileType.valueOf(FAMILY_TYPE, "CENTER_SPACE1");

        public static final TileType R_TERM_INT = TileType.valueOf(FAMILY_TYPE, "R_TERM_INT");

        public static final TileType DSP = TileType.valueOf(FAMILY_TYPE, "DSP");

        public static final TileType PCIE = TileType.valueOf(FAMILY_TYPE, "PCIE");

        public static final TileType HCLK_CLBLL = TileType.valueOf(FAMILY_TYPE, "HCLK_CLBLL");

        public static final TileType BRKH_DSP = TileType.valueOf(FAMILY_TYPE, "BRKH_DSP");

        public static final TileType RIOI = TileType.valueOf(FAMILY_TYPE, "RIOI");

        public static final TileType IOI_L_INT_INTERFACE_TERM = TileType.valueOf(FAMILY_TYPE, "IOI_L_INT_INTERFACE_TERM");

        public static final TileType LIOB = TileType.valueOf(FAMILY_TYPE, "LIOB");

        public static final TileType HCLK_DSP = TileType.valueOf(FAMILY_TYPE, "HCLK_DSP");

        public static final TileType BRKH_BRAM = TileType.valueOf(FAMILY_TYPE, "BRKH_BRAM");

        public static final TileType HCLK_VBRK = TileType.valueOf(FAMILY_TYPE, "HCLK_VBRK");

        public static final TileType BRKH_IOB = TileType.valueOf(FAMILY_TYPE, "BRKH_IOB");

        public static final TileType BRKH_IOI = TileType.valueOf(FAMILY_TYPE, "BRKH_IOI");

        public static final TileType HCLK_INNER_IOI = TileType.valueOf(FAMILY_TYPE, "HCLK_INNER_IOI");

        public static final TileType IOI_L_INT_INTERFACE = TileType.valueOf(FAMILY_TYPE, "IOI_L_INT_INTERFACE");

        public static final TileType HCLK_IOB = TileType.valueOf(FAMILY_TYPE, "HCLK_IOB");

        public static final TileType HCLK_TERM = TileType.valueOf(FAMILY_TYPE, "HCLK_TERM");

        public static final TileType EMAC_INT_INTERFACE = TileType.valueOf(FAMILY_TYPE, "EMAC_INT_INTERFACE");

        public static final TileType HCLK_CMT_BOT = TileType.valueOf(FAMILY_TYPE, "HCLK_CMT_BOT");

        public static final TileType CMT_PMVA = TileType.valueOf(FAMILY_TYPE, "CMT_PMVA");

        public static final TileType L_TERM_INT = TileType.valueOf(FAMILY_TYPE, "L_TERM_INT");

        public static final TileType CMT_PMVB = TileType.valueOf(FAMILY_TYPE, "CMT_PMVB");

        public static final TileType VFRAME_NOMON = TileType.valueOf(FAMILY_TYPE, "VFRAME_NOMON");

        public static final TileType PCIE_INT_INTERFACE_L = TileType.valueOf(FAMILY_TYPE, "PCIE_INT_INTERFACE_L");

        public static final TileType HCLK_QBUF_L = TileType.valueOf(FAMILY_TYPE, "HCLK_QBUF_L");

        public static final TileType RIOB = TileType.valueOf(FAMILY_TYPE, "RIOB");

        public static final TileType T_TERM_DSP = TileType.valueOf(FAMILY_TYPE, "T_TERM_DSP");

        public static final TileType BRKH_GTX = TileType.valueOf(FAMILY_TYPE, "BRKH_GTX");

        public static final TileType HCLK_GTX = TileType.valueOf(FAMILY_TYPE, "HCLK_GTX");

        public static final TileType HCLK_OUTER_IOI = TileType.valueOf(FAMILY_TYPE, "HCLK_OUTER_IOI");

        public static final TileType CMT_PMVB_BUF_ABOVE = TileType.valueOf(FAMILY_TYPE, "CMT_PMVB_BUF_ABOVE");

        public static final TileType GTX_INT_INTERFACE = TileType.valueOf(FAMILY_TYPE, "GTX_INT_INTERFACE");

        public static final TileType CFG_CENTER_3 = TileType.valueOf(FAMILY_TYPE, "CFG_CENTER_3");

        public static final TileType HCLK_VFRAME = TileType.valueOf(FAMILY_TYPE, "HCLK_VFRAME");

        public static final TileType PCIE_INT_INTERFACE_R = TileType.valueOf(FAMILY_TYPE, "PCIE_INT_INTERFACE_R");

        public static final TileType CMT_TOP = TileType.valueOf(FAMILY_TYPE, "CMT_TOP");

        public static final TileType VFRAME = TileType.valueOf(FAMILY_TYPE, "VFRAME");

        public static final TileType CFG_CENTER_0 = TileType.valueOf(FAMILY_TYPE, "CFG_CENTER_0");

        public static final TileType HCLK_QBUF_R = TileType.valueOf(FAMILY_TYPE, "HCLK_QBUF_R");

        public static final TileType HCLK_CLBLM_MGT = TileType.valueOf(FAMILY_TYPE, "HCLK_CLBLM_MGT");

        public static final TileType CMT_BUFG_BOT = TileType.valueOf(FAMILY_TYPE, "CMT_BUFG_BOT");

        public static final TileType BRKH_CMT = TileType.valueOf(FAMILY_TYPE, "BRKH_CMT");

        public static final TileType HCLK_CMT_TOP = TileType.valueOf(FAMILY_TYPE, "HCLK_CMT_TOP");

        public static final TileType CFG_CENTER_2 = TileType.valueOf(FAMILY_TYPE, "CFG_CENTER_2");

        public static final TileType CMT_BOT = TileType.valueOf(FAMILY_TYPE, "CMT_BOT");

        public static final TileType EMAC = TileType.valueOf(FAMILY_TYPE, "EMAC");
    }

    public static final class SiteTypes {

        public static final SiteType IODELAYE1 = SiteType.valueOf(FAMILY_TYPE, "IODELAYE1");

        public static final SiteType ILOGICE1 = SiteType.valueOf(FAMILY_TYPE, "ILOGICE1");

        public static final SiteType ISERDESE1 = SiteType.valueOf(FAMILY_TYPE, "ISERDESE1");

        public static final SiteType OLOGICE1 = SiteType.valueOf(FAMILY_TYPE, "OLOGICE1");

        public static final SiteType OSERDESE1 = SiteType.valueOf(FAMILY_TYPE, "OSERDESE1");

        public static final SiteType TIEOFF = SiteType.valueOf(FAMILY_TYPE, "TIEOFF");

        public static final SiteType SLICEM = SiteType.valueOf(FAMILY_TYPE, "SLICEM");

        public static final SiteType SLICEL = SiteType.valueOf(FAMILY_TYPE, "SLICEL");

        public static final SiteType GLOBALSIG = SiteType.valueOf(FAMILY_TYPE, "GLOBALSIG");

        public static final SiteType FIFO18E1 = SiteType.valueOf(FAMILY_TYPE, "FIFO18E1");

        public static final SiteType RAMB18E1 = SiteType.valueOf(FAMILY_TYPE, "RAMB18E1");

        public static final SiteType RAMBFIFO36E1 = SiteType.valueOf(FAMILY_TYPE, "RAMBFIFO36E1");

        public static final SiteType FIFO36E1 = SiteType.valueOf(FAMILY_TYPE, "FIFO36E1");

        public static final SiteType RAMB36E1 = SiteType.valueOf(FAMILY_TYPE, "RAMB36E1");

        public static final SiteType BUFGCTRL = SiteType.valueOf(FAMILY_TYPE, "BUFGCTRL");

        public static final SiteType BUFG = SiteType.valueOf(FAMILY_TYPE, "BUFG");

        public static final SiteType PMVBRAM = SiteType.valueOf(FAMILY_TYPE, "PMVBRAM");

        public static final SiteType GTXE1 = SiteType.valueOf(FAMILY_TYPE, "GTXE1");

        public static final SiteType IPAD = SiteType.valueOf(FAMILY_TYPE, "IPAD");

        public static final SiteType OPAD = SiteType.valueOf(FAMILY_TYPE, "OPAD");

        public static final SiteType FRAME_ECC = SiteType.valueOf(FAMILY_TYPE, "FRAME_ECC");

        public static final SiteType EFUSE_USR = SiteType.valueOf(FAMILY_TYPE, "EFUSE_USR");

        public static final SiteType USR_ACCESS = SiteType.valueOf(FAMILY_TYPE, "USR_ACCESS");

        public static final SiteType STARTUP = SiteType.valueOf(FAMILY_TYPE, "STARTUP");

        public static final SiteType CAPTURE = SiteType.valueOf(FAMILY_TYPE, "CAPTURE");

        public static final SiteType DNA_PORT = SiteType.valueOf(FAMILY_TYPE, "DNA_PORT");

        public static final SiteType DCIRESET = SiteType.valueOf(FAMILY_TYPE, "DCIRESET");

        public static final SiteType ICAP = SiteType.valueOf(FAMILY_TYPE, "ICAP");

        public static final SiteType BSCAN = SiteType.valueOf(FAMILY_TYPE, "BSCAN");

        public static final SiteType CFG_IO_ACCESS = SiteType.valueOf(FAMILY_TYPE, "CFG_IO_ACCESS");

        public static final SiteType IOBS = SiteType.valueOf(FAMILY_TYPE, "IOBS");

        public static final SiteType IOB = SiteType.valueOf(FAMILY_TYPE, "IOB");

        public static final SiteType IOBM = SiteType.valueOf(FAMILY_TYPE, "IOBM");

        public static final SiteType DSP48E1 = SiteType.valueOf(FAMILY_TYPE, "DSP48E1");

        public static final SiteType PCIE_2_0 = SiteType.valueOf(FAMILY_TYPE, "PCIE_2_0");

        public static final SiteType BUFO = SiteType.valueOf(FAMILY_TYPE, "BUFO");

        public static final SiteType BUFIODQS = SiteType.valueOf(FAMILY_TYPE, "BUFIODQS");

        public static final SiteType BUFR = SiteType.valueOf(FAMILY_TYPE, "BUFR");

        public static final SiteType IDELAYCTRL = SiteType.valueOf(FAMILY_TYPE, "IDELAYCTRL");

        public static final SiteType DCI = SiteType.valueOf(FAMILY_TYPE, "DCI");

        public static final SiteType BUFHCE = SiteType.valueOf(FAMILY_TYPE, "BUFHCE");

        public static final SiteType PMVIOB = SiteType.valueOf(FAMILY_TYPE, "PMVIOB");

        public static final SiteType IBUFDS_GTXE1 = SiteType.valueOf(FAMILY_TYPE, "IBUFDS_GTXE1");

        public static final SiteType PMV = SiteType.valueOf(FAMILY_TYPE, "PMV");

        public static final SiteType PPR_FRAME = SiteType.valueOf(FAMILY_TYPE, "PPR_FRAME");

        public static final SiteType MMCM_ADV = SiteType.valueOf(FAMILY_TYPE, "MMCM_ADV");

        public static final SiteType SYSMON = SiteType.valueOf(FAMILY_TYPE, "SYSMON");

        public static final SiteType TEMAC_SINGLE = SiteType.valueOf(FAMILY_TYPE, "TEMAC_SINGLE");
    }

    static {
        _GENERATED_FROM.add("xc6vlx75tff484");
        _TILE_TYPES.add(TileTypes.VBRK);
        _TILE_TYPES.add(TileTypes.LIOI);
        _TILE_TYPES.add(TileTypes.B_TERM_DSP);
        _TILE_TYPES.add(TileTypes.INT);
        _TILE_TYPES.add(TileTypes.CLBLM);
        _TILE_TYPES.add(TileTypes.T_TERM_INT);
        _TILE_TYPES.add(TileTypes.HCLK);
        _TILE_TYPES.add(TileTypes.CENTER_SPACE2);
        _TILE_TYPES.add(TileTypes.NULL);
        _TILE_TYPES.add(TileTypes.B_TERM_INT);
        _TILE_TYPES.add(TileTypes.BRKH_B_TERM_INT);
        _TILE_TYPES.add(TileTypes.BRKH);
        _TILE_TYPES.add(TileTypes.BRKH_INT);
        _TILE_TYPES.add(TileTypes.BRKH_CLB);
        _TILE_TYPES.add(TileTypes.BRAM);
        _TILE_TYPES.add(TileTypes.CMT_BUFG_TOP);
        _TILE_TYPES.add(TileTypes.CLBLL);
        _TILE_TYPES.add(TileTypes.HCLK_BRAM);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE);
        _TILE_TYPES.add(TileTypes.BRKH_INT_INTERFACE);
        _TILE_TYPES.add(TileTypes.GTX);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_TERM);
        _TILE_TYPES.add(TileTypes.BRKH_T_TERM_INT);
        _TILE_TYPES.add(TileTypes.CENTER_SPACE_HCLK2);
        _TILE_TYPES.add(TileTypes.HCLK_INT_INTERFACE);
        _TILE_TYPES.add(TileTypes.CENTER_SPACE_HCLK1);
        _TILE_TYPES.add(TileTypes.HCLK_CLBLM);
        _TILE_TYPES.add(TileTypes.CFG_CENTER_1);
        _TILE_TYPES.add(TileTypes.LIOB_FT);
        _TILE_TYPES.add(TileTypes.CENTER_SPACE1);
        _TILE_TYPES.add(TileTypes.R_TERM_INT);
        _TILE_TYPES.add(TileTypes.DSP);
        _TILE_TYPES.add(TileTypes.PCIE);
        _TILE_TYPES.add(TileTypes.HCLK_CLBLL);
        _TILE_TYPES.add(TileTypes.BRKH_DSP);
        _TILE_TYPES.add(TileTypes.RIOI);
        _TILE_TYPES.add(TileTypes.IOI_L_INT_INTERFACE_TERM);
        _TILE_TYPES.add(TileTypes.LIOB);
        _TILE_TYPES.add(TileTypes.HCLK_DSP);
        _TILE_TYPES.add(TileTypes.BRKH_BRAM);
        _TILE_TYPES.add(TileTypes.HCLK_VBRK);
        _TILE_TYPES.add(TileTypes.BRKH_IOB);
        _TILE_TYPES.add(TileTypes.BRKH_IOI);
        _TILE_TYPES.add(TileTypes.HCLK_INNER_IOI);
        _TILE_TYPES.add(TileTypes.IOI_L_INT_INTERFACE);
        _TILE_TYPES.add(TileTypes.HCLK_IOB);
        _TILE_TYPES.add(TileTypes.HCLK_TERM);
        _TILE_TYPES.add(TileTypes.EMAC_INT_INTERFACE);
        _TILE_TYPES.add(TileTypes.HCLK_CMT_BOT);
        _TILE_TYPES.add(TileTypes.CMT_PMVA);
        _TILE_TYPES.add(TileTypes.L_TERM_INT);
        _TILE_TYPES.add(TileTypes.CMT_PMVB);
        _TILE_TYPES.add(TileTypes.VFRAME_NOMON);
        _TILE_TYPES.add(TileTypes.PCIE_INT_INTERFACE_L);
        _TILE_TYPES.add(TileTypes.HCLK_QBUF_L);
        _TILE_TYPES.add(TileTypes.RIOB);
        _TILE_TYPES.add(TileTypes.T_TERM_DSP);
        _TILE_TYPES.add(TileTypes.BRKH_GTX);
        _TILE_TYPES.add(TileTypes.HCLK_GTX);
        _TILE_TYPES.add(TileTypes.HCLK_OUTER_IOI);
        _TILE_TYPES.add(TileTypes.CMT_PMVB_BUF_ABOVE);
        _TILE_TYPES.add(TileTypes.GTX_INT_INTERFACE);
        _TILE_TYPES.add(TileTypes.CFG_CENTER_3);
        _TILE_TYPES.add(TileTypes.HCLK_VFRAME);
        _TILE_TYPES.add(TileTypes.PCIE_INT_INTERFACE_R);
        _TILE_TYPES.add(TileTypes.CMT_TOP);
        _TILE_TYPES.add(TileTypes.VFRAME);
        _TILE_TYPES.add(TileTypes.CFG_CENTER_0);
        _TILE_TYPES.add(TileTypes.HCLK_QBUF_R);
        _TILE_TYPES.add(TileTypes.HCLK_CLBLM_MGT);
        _TILE_TYPES.add(TileTypes.CMT_BUFG_BOT);
        _TILE_TYPES.add(TileTypes.BRKH_CMT);
        _TILE_TYPES.add(TileTypes.HCLK_CMT_TOP);
        _TILE_TYPES.add(TileTypes.CFG_CENTER_2);
        _TILE_TYPES.add(TileTypes.CMT_BOT);
        _TILE_TYPES.add(TileTypes.EMAC);
        _SITE_TYPES.add(SiteTypes.IODELAYE1);
        _SITE_TYPES.add(SiteTypes.ILOGICE1);
        _SITE_TYPES.add(SiteTypes.ISERDESE1);
        _SITE_TYPES.add(SiteTypes.OLOGICE1);
        _SITE_TYPES.add(SiteTypes.OSERDESE1);
        _SITE_TYPES.add(SiteTypes.TIEOFF);
        _SITE_TYPES.add(SiteTypes.SLICEM);
        _SITE_TYPES.add(SiteTypes.SLICEL);
        _SITE_TYPES.add(SiteTypes.GLOBALSIG);
        _SITE_TYPES.add(SiteTypes.FIFO18E1);
        _SITE_TYPES.add(SiteTypes.RAMB18E1);
        _SITE_TYPES.add(SiteTypes.RAMBFIFO36E1);
        _SITE_TYPES.add(SiteTypes.FIFO36E1);
        _SITE_TYPES.add(SiteTypes.RAMB36E1);
        _SITE_TYPES.add(SiteTypes.BUFGCTRL);
        _SITE_TYPES.add(SiteTypes.BUFG);
        _SITE_TYPES.add(SiteTypes.PMVBRAM);
        _SITE_TYPES.add(SiteTypes.GTXE1);
        _SITE_TYPES.add(SiteTypes.IPAD);
        _SITE_TYPES.add(SiteTypes.OPAD);
        _SITE_TYPES.add(SiteTypes.FRAME_ECC);
        _SITE_TYPES.add(SiteTypes.EFUSE_USR);
        _SITE_TYPES.add(SiteTypes.USR_ACCESS);
        _SITE_TYPES.add(SiteTypes.STARTUP);
        _SITE_TYPES.add(SiteTypes.CAPTURE);
        _SITE_TYPES.add(SiteTypes.DNA_PORT);
        _SITE_TYPES.add(SiteTypes.DCIRESET);
        _SITE_TYPES.add(SiteTypes.ICAP);
        _SITE_TYPES.add(SiteTypes.BSCAN);
        _SITE_TYPES.add(SiteTypes.CFG_IO_ACCESS);
        _SITE_TYPES.add(SiteTypes.IOBS);
        _SITE_TYPES.add(SiteTypes.IOB);
        _SITE_TYPES.add(SiteTypes.IOBM);
        _SITE_TYPES.add(SiteTypes.DSP48E1);
        _SITE_TYPES.add(SiteTypes.PCIE_2_0);
        _SITE_TYPES.add(SiteTypes.BUFO);
        _SITE_TYPES.add(SiteTypes.BUFIODQS);
        _SITE_TYPES.add(SiteTypes.BUFR);
        _SITE_TYPES.add(SiteTypes.IDELAYCTRL);
        _SITE_TYPES.add(SiteTypes.DCI);
        _SITE_TYPES.add(SiteTypes.BUFHCE);
        _SITE_TYPES.add(SiteTypes.PMVIOB);
        _SITE_TYPES.add(SiteTypes.IBUFDS_GTXE1);
        _SITE_TYPES.add(SiteTypes.PMV);
        _SITE_TYPES.add(SiteTypes.PPR_FRAME);
        _SITE_TYPES.add(SiteTypes.MMCM_ADV);
        _SITE_TYPES.add(SiteTypes.SYSMON);
        _SITE_TYPES.add(SiteTypes.TEMAC_SINGLE);
    }

    /* ------ AUTO-GENERATED --- DO NOT EDIT ABOVE ------ */
    static {
        /* ------ CLASSIFICATIONS GO HERE ------ */
        _CLB_TILES.add(TileTypes.CLBLL);
        _CLB_TILES.add(TileTypes.CLBLM);
        _SWITCHBOX_TILES.add(TileTypes.INT);
        _BRAM_TILES.add(TileTypes.BRAM);
        _DSP_TILES.add(TileTypes.DSP);
        _IO_TILES.add(TileTypes.LIOB);
        _IO_TILES.add(TileTypes.RIOB);
        _IO_TILES.add(TileTypes.LIOB_FT);
        _SLICE_SITES.add(SiteTypes.SLICEL);
        _SLICE_SITES.add(SiteTypes.SLICEM);
        _BRAM_SITES.add(SiteTypes.RAMB18E1);
        _BRAM_SITES.add(SiteTypes.RAMB36E1);
        _BRAM_SITES.add(SiteTypes.RAMBFIFO36E1);
        _DSP_SITES.add(SiteTypes.DSP48E1);
        _IO_SITES.add(SiteTypes.IOB);
        _IO_SITES.add(SiteTypes.IOBS);
        _IO_SITES.add(SiteTypes.IOBM);
        _IO_SITES.add(SiteTypes.IPAD);
        _IO_SITES.add(SiteTypes.OPAD);
    }
}
