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

public final class Kintexu implements FamilyInfo {

    /* ------ AUTO-GENERATED --- DO NOT EDIT BELOW ------ */
    public static final FamilyType FAMILY_TYPE = FamilyType.valueOf("KINTEXU");

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

    private static final HashSet<SiteType> _FIFO_SITES = new HashSet<>();

    public static final Set<SiteType> FIFO_SITES = Collections.unmodifiableSet(_FIFO_SITES);

    @Override
    public Set<SiteType> fifoSites() {
        return FIFO_SITES;
    }

    public static final class TileTypes {

        public static final TileType CLEL_L = TileType.valueOf(FAMILY_TYPE, "CLEL_L");

        public static final TileType NULL = TileType.valueOf(FAMILY_TYPE, "NULL");

        public static final TileType RCLK_INTF_R_L = TileType.valueOf(FAMILY_TYPE, "RCLK_INTF_R_L");

        public static final TileType CFRM_CBRK_L = TileType.valueOf(FAMILY_TYPE, "CFRM_CBRK_L");

        public static final TileType CLE_M_TERM_B = TileType.valueOf(FAMILY_TYPE, "CLE_M_TERM_B");

        public static final TileType INT_RBRK = TileType.valueOf(FAMILY_TYPE, "INT_RBRK");

        public static final TileType RCLK_INT_L = TileType.valueOf(FAMILY_TYPE, "RCLK_INT_L");

        public static final TileType INT_INTERFACE_R_TERM_B = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_R_TERM_B");

        public static final TileType RCLK_CLEL_R_L = TileType.valueOf(FAMILY_TYPE, "RCLK_CLEL_R_L");

        public static final TileType CLEL_R_RBRK = TileType.valueOf(FAMILY_TYPE, "CLEL_R_RBRK");

        public static final TileType RCLK_INTF_GT_R_R = TileType.valueOf(FAMILY_TYPE, "RCLK_INTF_GT_R_R");

        public static final TileType INT = TileType.valueOf(FAMILY_TYPE, "INT");

        public static final TileType CLEL_R_TERM_B = TileType.valueOf(FAMILY_TYPE, "CLEL_R_TERM_B");

        public static final TileType CFRM_L_RBRK = TileType.valueOf(FAMILY_TYPE, "CFRM_L_RBRK");

        public static final TileType INT_INTERFACE_L_RBRK = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_L_RBRK");

        public static final TileType CLEL_R = TileType.valueOf(FAMILY_TYPE, "CLEL_R");

        public static final TileType CLE_M = TileType.valueOf(FAMILY_TYPE, "CLE_M");

        public static final TileType CFRM_L_TERM_B = TileType.valueOf(FAMILY_TYPE, "CFRM_L_TERM_B");

        public static final TileType RCLK_CLE_M_L = TileType.valueOf(FAMILY_TYPE, "RCLK_CLE_M_L");

        public static final TileType RCLK_INTF_PCIE_R_L = TileType.valueOf(FAMILY_TYPE, "RCLK_INTF_PCIE_R_L");

        public static final TileType INT_IBRK_RBRK_L_R = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_RBRK_L_R");

        public static final TileType CFGIO_IOB = TileType.valueOf(FAMILY_TYPE, "CFGIO_IOB");

        public static final TileType INT_INTERFACE_R = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_R");

        public static final TileType INT_TERM_B = TileType.valueOf(FAMILY_TYPE, "INT_TERM_B");

        public static final TileType INT_INT_INTERFACE_XIPHY_RBRK_FT = TileType.valueOf(FAMILY_TYPE, "INT_INT_INTERFACE_XIPHY_RBRK_FT");

        public static final TileType RCLK_CBRK_L = TileType.valueOf(FAMILY_TYPE, "RCLK_CBRK_L");

        public static final TileType INT_INTERFACE_L_TERM_B = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_L_TERM_B");

        public static final TileType RCLK_INTF_L_L = TileType.valueOf(FAMILY_TYPE, "RCLK_INTF_L_L");

        public static final TileType CLE_M_RBRK = TileType.valueOf(FAMILY_TYPE, "CLE_M_RBRK");

        public static final TileType DSP_RBRK = TileType.valueOf(FAMILY_TYPE, "DSP_RBRK");

        public static final TileType INT_IBRK_FSR2IO = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_FSR2IO");

        public static final TileType BRAM = TileType.valueOf(FAMILY_TYPE, "BRAM");

        public static final TileType BRAM_RBRK = TileType.valueOf(FAMILY_TYPE, "BRAM_RBRK");

        public static final TileType RCLK_INT_R = TileType.valueOf(FAMILY_TYPE, "RCLK_INT_R");

        public static final TileType INT_INTERFACE_L = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_L");

        public static final TileType HPIO_TERM_L_RBRK = TileType.valueOf(FAMILY_TYPE, "HPIO_TERM_L_RBRK");

        public static final TileType FSR_GAP = TileType.valueOf(FAMILY_TYPE, "FSR_GAP");

        public static final TileType RCLK_GAP4 = TileType.valueOf(FAMILY_TYPE, "RCLK_GAP4");

        public static final TileType XIPHY_L = TileType.valueOf(FAMILY_TYPE, "XIPHY_L");

        public static final TileType RCLK_HPIO_L = TileType.valueOf(FAMILY_TYPE, "RCLK_HPIO_L");

        public static final TileType DSP = TileType.valueOf(FAMILY_TYPE, "DSP");

        public static final TileType BRAM_TERM_B = TileType.valueOf(FAMILY_TYPE, "BRAM_TERM_B");

        public static final TileType INT_IBRK_RBRK_IO2XIPHY = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_RBRK_IO2XIPHY");

        public static final TileType RCLK_RCLK_INTF_XIPHY_LEFT_L_FT = TileType.valueOf(FAMILY_TYPE, "RCLK_RCLK_INTF_XIPHY_LEFT_L_FT");

        public static final TileType INT_INTERFACE_R_RBRK = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_R_RBRK");

        public static final TileType INT_IBRK_XIPHY2INT = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_XIPHY2INT");

        public static final TileType CFRM_CBRK_R = TileType.valueOf(FAMILY_TYPE, "CFRM_CBRK_R");

        public static final TileType INT_INT_INTERFACE_XIPHY_FT = TileType.valueOf(FAMILY_TYPE, "INT_INT_INTERFACE_XIPHY_FT");

        public static final TileType RCLK_CLEL_R = TileType.valueOf(FAMILY_TYPE, "RCLK_CLEL_R");

        public static final TileType RCLK_RCLK_BRAM_L_AUXCLMP_FT = TileType.valueOf(FAMILY_TYPE, "RCLK_RCLK_BRAM_L_AUXCLMP_FT");

        public static final TileType CLEL_L_RBRK = TileType.valueOf(FAMILY_TYPE, "CLEL_L_RBRK");

        public static final TileType RCLK_CLE_M_R = TileType.valueOf(FAMILY_TYPE, "RCLK_CLE_M_R");

        public static final TileType HPIO_L = TileType.valueOf(FAMILY_TYPE, "HPIO_L");

        public static final TileType FSR_GAP_TERM_B = TileType.valueOf(FAMILY_TYPE, "FSR_GAP_TERM_B");

        public static final TileType HPIO_L_TERM_B = TileType.valueOf(FAMILY_TYPE, "HPIO_L_TERM_B");

        public static final TileType HPIO_RBRK_L = TileType.valueOf(FAMILY_TYPE, "HPIO_RBRK_L");

        public static final TileType GTH_R = TileType.valueOf(FAMILY_TYPE, "GTH_R");

        public static final TileType INT_IBRK_TERM_B_IO2XIPHY = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_TERM_B_IO2XIPHY");

        public static final TileType DSP_TERM_B = TileType.valueOf(FAMILY_TYPE, "DSP_TERM_B");

        public static final TileType RCLK_INTF_L_R = TileType.valueOf(FAMILY_TYPE, "RCLK_INTF_L_R");

        public static final TileType RCLK_DSP_L = TileType.valueOf(FAMILY_TYPE, "RCLK_DSP_L");

        public static final TileType RCLK_CBRK_R = TileType.valueOf(FAMILY_TYPE, "RCLK_CBRK_R");

        public static final TileType CFRM_R_RBRK = TileType.valueOf(FAMILY_TYPE, "CFRM_R_RBRK");

        public static final TileType CFG_GAP_CFGTOP = TileType.valueOf(FAMILY_TYPE, "CFG_GAP_CFGTOP");

        public static final TileType INT_INTERFACE_PCIE_L = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_PCIE_L");

        public static final TileType RCLK_CLEL_R_R = TileType.valueOf(FAMILY_TYPE, "RCLK_CLEL_R_R");

        public static final TileType RCLK_IBRK_IO2XIPHY = TileType.valueOf(FAMILY_TYPE, "RCLK_IBRK_IO2XIPHY");

        public static final TileType INT_INTERFACE_PCIE_R = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_PCIE_R");

        public static final TileType INT_TERM_L_IO_TERM_B = TileType.valueOf(FAMILY_TYPE, "INT_TERM_L_IO_TERM_B");

        public static final TileType INT_INTERFACE_PCIE_L_RBRK = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_PCIE_L_RBRK");

        public static final TileType RCLK_BRAM_L = TileType.valueOf(FAMILY_TYPE, "RCLK_BRAM_L");

        public static final TileType RCLK_CLEM_CLKBUF_L = TileType.valueOf(FAMILY_TYPE, "RCLK_CLEM_CLKBUF_L");

        public static final TileType INT_IBRK_RBRK_FSR2IO = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_RBRK_FSR2IO");

        public static final TileType CFGIO_CFG_RBRK = TileType.valueOf(FAMILY_TYPE, "CFGIO_CFG_RBRK");

        public static final TileType FSR_GAP_RBRK = TileType.valueOf(FAMILY_TYPE, "FSR_GAP_RBRK");

        public static final TileType INT_INTERFACE_GT_R = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_GT_R");

        public static final TileType RCLK_INT_TERM_L = TileType.valueOf(FAMILY_TYPE, "RCLK_INT_TERM_L");

        public static final TileType CFG_CFG_PCIE_RBRK = TileType.valueOf(FAMILY_TYPE, "CFG_CFG_PCIE_RBRK");

        public static final TileType HPIO_CBRK_IO = TileType.valueOf(FAMILY_TYPE, "HPIO_CBRK_IO");

        public static final TileType RCLK_IBRK_XIPHY2INT = TileType.valueOf(FAMILY_TYPE, "RCLK_IBRK_XIPHY2INT");

        public static final TileType INT_INTERFACE_GT_R_RBRK = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_GT_R_RBRK");

        public static final TileType RCLK_INTF_PCIE_L_R = TileType.valueOf(FAMILY_TYPE, "RCLK_INTF_PCIE_L_R");

        public static final TileType RCLK_BRAM_R = TileType.valueOf(FAMILY_TYPE, "RCLK_BRAM_R");

        public static final TileType RCLK_CBRK_IO = TileType.valueOf(FAMILY_TYPE, "RCLK_CBRK_IO");

        public static final TileType RCLK_TERM_L = TileType.valueOf(FAMILY_TYPE, "RCLK_TERM_L");

        public static final TileType RCLK_RCLK_BRAM_L_BRAMCLMP_FT = TileType.valueOf(FAMILY_TYPE, "RCLK_RCLK_BRAM_L_BRAMCLMP_FT");

        public static final TileType RCLK_CLEL_L = TileType.valueOf(FAMILY_TYPE, "RCLK_CLEL_L");

        public static final TileType HRIO_L = TileType.valueOf(FAMILY_TYPE, "HRIO_L");

        public static final TileType CFRM_RBRK_PCIE = TileType.valueOf(FAMILY_TYPE, "CFRM_RBRK_PCIE");

        public static final TileType RCLK_IBRK_R_L = TileType.valueOf(FAMILY_TYPE, "RCLK_IBRK_R_L");

        public static final TileType CFG_GAP_CFGBOT = TileType.valueOf(FAMILY_TYPE, "CFG_GAP_CFGBOT");

        public static final TileType INT_IBRK_TERM_B_OUTER_FT = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_TERM_B_OUTER_FT");

        public static final TileType INT_IBRK_IO2XIPHY = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_IO2XIPHY");

        public static final TileType INT_IBRK_R_L = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_R_L");

        public static final TileType INT_TERM_L_IO = TileType.valueOf(FAMILY_TYPE, "INT_TERM_L_IO");

        public static final TileType INT_INTERFACE_PCIE_L_TERM_B = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_PCIE_L_TERM_B");

        public static final TileType INT_IBRK_TERM_B_XIPHY2INT = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_TERM_B_XIPHY2INT");

        public static final TileType HPIO_HPIO_TERM_B_LEFT_OUTER_FT = TileType.valueOf(FAMILY_TYPE, "HPIO_HPIO_TERM_B_LEFT_OUTER_FT");

        public static final TileType GTH_R_RBRK = TileType.valueOf(FAMILY_TYPE, "GTH_R_RBRK");

        public static final TileType XIPHY_L_TERM_B = TileType.valueOf(FAMILY_TYPE, "XIPHY_L_TERM_B");

        public static final TileType INT_INT_INTERFACE_XIPHY_TERM_B_FT = TileType.valueOf(FAMILY_TYPE, "INT_INT_INTERFACE_XIPHY_TERM_B_FT");

        public static final TileType HPIO_TERM_L = TileType.valueOf(FAMILY_TYPE, "HPIO_TERM_L");

        public static final TileType INT_IBRK_TERM_B_R_L = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_TERM_B_R_L");

        public static final TileType GTH_R_TERM_B = TileType.valueOf(FAMILY_TYPE, "GTH_R_TERM_B");

        public static final TileType XIPHY_L_RBRK = TileType.valueOf(FAMILY_TYPE, "XIPHY_L_RBRK");

        public static final TileType HPHRIO_RBRK_L = TileType.valueOf(FAMILY_TYPE, "HPHRIO_RBRK_L");

        public static final TileType CFRM_AMS_CFGIO = TileType.valueOf(FAMILY_TYPE, "CFRM_AMS_CFGIO");

        public static final TileType CLK_IBRK_FSR2IO = TileType.valueOf(FAMILY_TYPE, "CLK_IBRK_FSR2IO");

        public static final TileType INT_INTERFACE_PCIE_R_RBRK = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_PCIE_R_RBRK");

        public static final TileType INT_IBRK_RBRK_R_L = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_RBRK_R_L");

        public static final TileType RCLK_HRIO_L = TileType.valueOf(FAMILY_TYPE, "RCLK_HRIO_L");

        public static final TileType INT_IBRK_RBRK_XIPHY2INT = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_RBRK_XIPHY2INT");

        public static final TileType CFG_CFG = TileType.valueOf(FAMILY_TYPE, "CFG_CFG");

        public static final TileType CLE_M_R = TileType.valueOf(FAMILY_TYPE, "CLE_M_R");

        public static final TileType RCLK_DSP_CLKBUF_L = TileType.valueOf(FAMILY_TYPE, "RCLK_DSP_CLKBUF_L");

        public static final TileType INT_INTERFACE_PCIE_R_TERM_B = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_PCIE_R_TERM_B");

        public static final TileType INT_IBRK_L_R = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_L_R");

        public static final TileType CFRM_TERM_B = TileType.valueOf(FAMILY_TYPE, "CFRM_TERM_B");

        public static final TileType INT_IBRK_TERM_B_FSR2IO = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_TERM_B_FSR2IO");

        public static final TileType HRIO_TERM_B_L = TileType.valueOf(FAMILY_TYPE, "HRIO_TERM_B_L");

        public static final TileType PCIE = TileType.valueOf(FAMILY_TYPE, "PCIE");

        public static final TileType CFG_CTR_TERM_B = TileType.valueOf(FAMILY_TYPE, "CFG_CTR_TERM_B");

        public static final TileType CFRM_B = TileType.valueOf(FAMILY_TYPE, "CFRM_B");

        public static final TileType HRIO_RBRK_L = TileType.valueOf(FAMILY_TYPE, "HRIO_RBRK_L");

        public static final TileType RCLK_AMS_CFGIO = TileType.valueOf(FAMILY_TYPE, "RCLK_AMS_CFGIO");

        public static final TileType INT_TERM_L_IO_RBRK = TileType.valueOf(FAMILY_TYPE, "INT_TERM_L_IO_RBRK");

        public static final TileType CFRM_CFG = TileType.valueOf(FAMILY_TYPE, "CFRM_CFG");

        public static final TileType CLEL_L_TERM_B = TileType.valueOf(FAMILY_TYPE, "CLEL_L_TERM_B");

        public static final TileType INT_IBRK_TERM_B_L_R = TileType.valueOf(FAMILY_TYPE, "INT_IBRK_TERM_B_L_R");

        public static final TileType INT_INTERFACE_GT_R_TERM_B = TileType.valueOf(FAMILY_TYPE, "INT_INTERFACE_GT_R_TERM_B");

        public static final TileType RCLK_IBRK_L_R = TileType.valueOf(FAMILY_TYPE, "RCLK_IBRK_L_R");

        public static final TileType CFRM_R_TERM_B = TileType.valueOf(FAMILY_TYPE, "CFRM_R_TERM_B");

        public static final TileType CFG_PCIE_AMS_RBRK = TileType.valueOf(FAMILY_TYPE, "CFG_PCIE_AMS_RBRK");

        public static final TileType CFRM_RBRK_CFGIO = TileType.valueOf(FAMILY_TYPE, "CFRM_RBRK_CFGIO");

        public static final TileType CFRM_RBRK_B = TileType.valueOf(FAMILY_TYPE, "CFRM_RBRK_B");

        public static final TileType AMS = TileType.valueOf(FAMILY_TYPE, "AMS");
    }

    public static final class SiteTypes {

        public static final SiteType SLICEL = SiteType.valueOf(FAMILY_TYPE, "SLICEL");

        public static final SiteType BUFCE_LEAF_X16 = SiteType.valueOf(FAMILY_TYPE, "BUFCE_LEAF_X16");

        public static final SiteType BUFCE_ROW = SiteType.valueOf(FAMILY_TYPE, "BUFCE_ROW");

        public static final SiteType SLICEM = SiteType.valueOf(FAMILY_TYPE, "SLICEM");

        public static final SiteType RAMBFIFO18 = SiteType.valueOf(FAMILY_TYPE, "RAMBFIFO18");

        public static final SiteType FIFO18_0 = SiteType.valueOf(FAMILY_TYPE, "FIFO18_0");

        public static final SiteType RAMB180 = SiteType.valueOf(FAMILY_TYPE, "RAMB180");

        public static final SiteType RAMB181 = SiteType.valueOf(FAMILY_TYPE, "RAMB181");

        public static final SiteType RAMBFIFO36 = SiteType.valueOf(FAMILY_TYPE, "RAMBFIFO36");

        public static final SiteType FIFO36 = SiteType.valueOf(FAMILY_TYPE, "FIFO36");

        public static final SiteType RAMB36 = SiteType.valueOf(FAMILY_TYPE, "RAMB36");

        public static final SiteType BITSLICE_CONTROL = SiteType.valueOf(FAMILY_TYPE, "BITSLICE_CONTROL");

        public static final SiteType BITSLICE_RX_TX = SiteType.valueOf(FAMILY_TYPE, "BITSLICE_RX_TX");

        public static final SiteType BITSLICE_COMPONENT_RX_TX = SiteType.valueOf(FAMILY_TYPE, "BITSLICE_COMPONENT_RX_TX");

        public static final SiteType BITSLICE_RXTX_RX = SiteType.valueOf(FAMILY_TYPE, "BITSLICE_RXTX_RX");

        public static final SiteType BITSLICE_RXTX_TX = SiteType.valueOf(FAMILY_TYPE, "BITSLICE_RXTX_TX");

        public static final SiteType BITSLICE_TX = SiteType.valueOf(FAMILY_TYPE, "BITSLICE_TX");

        public static final SiteType BUFGCE_DIV = SiteType.valueOf(FAMILY_TYPE, "BUFGCE_DIV");

        public static final SiteType BUFGCE = SiteType.valueOf(FAMILY_TYPE, "BUFGCE");

        public static final SiteType BUFGCTRL = SiteType.valueOf(FAMILY_TYPE, "BUFGCTRL");

        public static final SiteType MMCME3_ADV = SiteType.valueOf(FAMILY_TYPE, "MMCME3_ADV");

        public static final SiteType PLLE3_ADV = SiteType.valueOf(FAMILY_TYPE, "PLLE3_ADV");

        public static final SiteType PLL_SELECT_SITE = SiteType.valueOf(FAMILY_TYPE, "PLL_SELECT_SITE");

        public static final SiteType RIU_OR = SiteType.valueOf(FAMILY_TYPE, "RIU_OR");

        public static final SiteType XIPHY_FEEDTHROUGH = SiteType.valueOf(FAMILY_TYPE, "XIPHY_FEEDTHROUGH");

        public static final SiteType DSP48E2 = SiteType.valueOf(FAMILY_TYPE, "DSP48E2");

        public static final SiteType HARD_SYNC = SiteType.valueOf(FAMILY_TYPE, "HARD_SYNC");

        public static final SiteType HPIOBDIFFINBUF = SiteType.valueOf(FAMILY_TYPE, "HPIOBDIFFINBUF");

        public static final SiteType HPIOBDIFFOUTBUF = SiteType.valueOf(FAMILY_TYPE, "HPIOBDIFFOUTBUF");

        public static final SiteType HPIO_VREF_SITE = SiteType.valueOf(FAMILY_TYPE, "HPIO_VREF_SITE");

        public static final SiteType HPIOB = SiteType.valueOf(FAMILY_TYPE, "HPIOB");

        public static final SiteType BUFG_GT_SYNC = SiteType.valueOf(FAMILY_TYPE, "BUFG_GT_SYNC");

        public static final SiteType BUFG_GT = SiteType.valueOf(FAMILY_TYPE, "BUFG_GT");

        public static final SiteType GTHE3_CHANNEL = SiteType.valueOf(FAMILY_TYPE, "GTHE3_CHANNEL");

        public static final SiteType GTHE3_COMMON = SiteType.valueOf(FAMILY_TYPE, "GTHE3_COMMON");

        public static final SiteType HRIODIFFINBUF = SiteType.valueOf(FAMILY_TYPE, "HRIODIFFINBUF");

        public static final SiteType HRIODIFFOUTBUF = SiteType.valueOf(FAMILY_TYPE, "HRIODIFFOUTBUF");

        public static final SiteType HRIO = SiteType.valueOf(FAMILY_TYPE, "HRIO");

        public static final SiteType CONFIG_SITE = SiteType.valueOf(FAMILY_TYPE, "CONFIG_SITE");

        public static final SiteType PCIE_3_1 = SiteType.valueOf(FAMILY_TYPE, "PCIE_3_1");

        public static final SiteType SYSMONE1 = SiteType.valueOf(FAMILY_TYPE, "SYSMONE1");
    }

    static {
        _GENERATED_FROM.add("xcku025-ffva1156-1-c");
        _TILE_TYPES.add(TileTypes.CLEL_L);
        _TILE_TYPES.add(TileTypes.NULL);
        _TILE_TYPES.add(TileTypes.RCLK_INTF_R_L);
        _TILE_TYPES.add(TileTypes.CFRM_CBRK_L);
        _TILE_TYPES.add(TileTypes.CLE_M_TERM_B);
        _TILE_TYPES.add(TileTypes.INT_RBRK);
        _TILE_TYPES.add(TileTypes.RCLK_INT_L);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_R_TERM_B);
        _TILE_TYPES.add(TileTypes.RCLK_CLEL_R_L);
        _TILE_TYPES.add(TileTypes.CLEL_R_RBRK);
        _TILE_TYPES.add(TileTypes.RCLK_INTF_GT_R_R);
        _TILE_TYPES.add(TileTypes.INT);
        _TILE_TYPES.add(TileTypes.CLEL_R_TERM_B);
        _TILE_TYPES.add(TileTypes.CFRM_L_RBRK);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_L_RBRK);
        _TILE_TYPES.add(TileTypes.CLEL_R);
        _TILE_TYPES.add(TileTypes.CLE_M);
        _TILE_TYPES.add(TileTypes.CFRM_L_TERM_B);
        _TILE_TYPES.add(TileTypes.RCLK_CLE_M_L);
        _TILE_TYPES.add(TileTypes.RCLK_INTF_PCIE_R_L);
        _TILE_TYPES.add(TileTypes.INT_IBRK_RBRK_L_R);
        _TILE_TYPES.add(TileTypes.CFGIO_IOB);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_R);
        _TILE_TYPES.add(TileTypes.INT_TERM_B);
        _TILE_TYPES.add(TileTypes.INT_INT_INTERFACE_XIPHY_RBRK_FT);
        _TILE_TYPES.add(TileTypes.RCLK_CBRK_L);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_L_TERM_B);
        _TILE_TYPES.add(TileTypes.RCLK_INTF_L_L);
        _TILE_TYPES.add(TileTypes.CLE_M_RBRK);
        _TILE_TYPES.add(TileTypes.DSP_RBRK);
        _TILE_TYPES.add(TileTypes.INT_IBRK_FSR2IO);
        _TILE_TYPES.add(TileTypes.BRAM);
        _TILE_TYPES.add(TileTypes.BRAM_RBRK);
        _TILE_TYPES.add(TileTypes.RCLK_INT_R);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_L);
        _TILE_TYPES.add(TileTypes.HPIO_TERM_L_RBRK);
        _TILE_TYPES.add(TileTypes.FSR_GAP);
        _TILE_TYPES.add(TileTypes.RCLK_GAP4);
        _TILE_TYPES.add(TileTypes.XIPHY_L);
        _TILE_TYPES.add(TileTypes.RCLK_HPIO_L);
        _TILE_TYPES.add(TileTypes.DSP);
        _TILE_TYPES.add(TileTypes.BRAM_TERM_B);
        _TILE_TYPES.add(TileTypes.INT_IBRK_RBRK_IO2XIPHY);
        _TILE_TYPES.add(TileTypes.RCLK_RCLK_INTF_XIPHY_LEFT_L_FT);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_R_RBRK);
        _TILE_TYPES.add(TileTypes.INT_IBRK_XIPHY2INT);
        _TILE_TYPES.add(TileTypes.CFRM_CBRK_R);
        _TILE_TYPES.add(TileTypes.INT_INT_INTERFACE_XIPHY_FT);
        _TILE_TYPES.add(TileTypes.RCLK_CLEL_R);
        _TILE_TYPES.add(TileTypes.RCLK_RCLK_BRAM_L_AUXCLMP_FT);
        _TILE_TYPES.add(TileTypes.CLEL_L_RBRK);
        _TILE_TYPES.add(TileTypes.RCLK_CLE_M_R);
        _TILE_TYPES.add(TileTypes.HPIO_L);
        _TILE_TYPES.add(TileTypes.FSR_GAP_TERM_B);
        _TILE_TYPES.add(TileTypes.HPIO_L_TERM_B);
        _TILE_TYPES.add(TileTypes.HPIO_RBRK_L);
        _TILE_TYPES.add(TileTypes.GTH_R);
        _TILE_TYPES.add(TileTypes.INT_IBRK_TERM_B_IO2XIPHY);
        _TILE_TYPES.add(TileTypes.DSP_TERM_B);
        _TILE_TYPES.add(TileTypes.RCLK_INTF_L_R);
        _TILE_TYPES.add(TileTypes.RCLK_DSP_L);
        _TILE_TYPES.add(TileTypes.RCLK_CBRK_R);
        _TILE_TYPES.add(TileTypes.CFRM_R_RBRK);
        _TILE_TYPES.add(TileTypes.CFG_GAP_CFGTOP);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_PCIE_L);
        _TILE_TYPES.add(TileTypes.RCLK_CLEL_R_R);
        _TILE_TYPES.add(TileTypes.RCLK_IBRK_IO2XIPHY);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_PCIE_R);
        _TILE_TYPES.add(TileTypes.INT_TERM_L_IO_TERM_B);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_PCIE_L_RBRK);
        _TILE_TYPES.add(TileTypes.RCLK_BRAM_L);
        _TILE_TYPES.add(TileTypes.RCLK_CLEM_CLKBUF_L);
        _TILE_TYPES.add(TileTypes.INT_IBRK_RBRK_FSR2IO);
        _TILE_TYPES.add(TileTypes.CFGIO_CFG_RBRK);
        _TILE_TYPES.add(TileTypes.FSR_GAP_RBRK);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_GT_R);
        _TILE_TYPES.add(TileTypes.RCLK_INT_TERM_L);
        _TILE_TYPES.add(TileTypes.CFG_CFG_PCIE_RBRK);
        _TILE_TYPES.add(TileTypes.HPIO_CBRK_IO);
        _TILE_TYPES.add(TileTypes.RCLK_IBRK_XIPHY2INT);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_GT_R_RBRK);
        _TILE_TYPES.add(TileTypes.RCLK_INTF_PCIE_L_R);
        _TILE_TYPES.add(TileTypes.RCLK_BRAM_R);
        _TILE_TYPES.add(TileTypes.RCLK_CBRK_IO);
        _TILE_TYPES.add(TileTypes.RCLK_TERM_L);
        _TILE_TYPES.add(TileTypes.RCLK_RCLK_BRAM_L_BRAMCLMP_FT);
        _TILE_TYPES.add(TileTypes.RCLK_CLEL_L);
        _TILE_TYPES.add(TileTypes.HRIO_L);
        _TILE_TYPES.add(TileTypes.CFRM_RBRK_PCIE);
        _TILE_TYPES.add(TileTypes.RCLK_IBRK_R_L);
        _TILE_TYPES.add(TileTypes.CFG_GAP_CFGBOT);
        _TILE_TYPES.add(TileTypes.INT_IBRK_TERM_B_OUTER_FT);
        _TILE_TYPES.add(TileTypes.INT_IBRK_IO2XIPHY);
        _TILE_TYPES.add(TileTypes.INT_IBRK_R_L);
        _TILE_TYPES.add(TileTypes.INT_TERM_L_IO);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_PCIE_L_TERM_B);
        _TILE_TYPES.add(TileTypes.INT_IBRK_TERM_B_XIPHY2INT);
        _TILE_TYPES.add(TileTypes.HPIO_HPIO_TERM_B_LEFT_OUTER_FT);
        _TILE_TYPES.add(TileTypes.GTH_R_RBRK);
        _TILE_TYPES.add(TileTypes.XIPHY_L_TERM_B);
        _TILE_TYPES.add(TileTypes.INT_INT_INTERFACE_XIPHY_TERM_B_FT);
        _TILE_TYPES.add(TileTypes.HPIO_TERM_L);
        _TILE_TYPES.add(TileTypes.INT_IBRK_TERM_B_R_L);
        _TILE_TYPES.add(TileTypes.GTH_R_TERM_B);
        _TILE_TYPES.add(TileTypes.XIPHY_L_RBRK);
        _TILE_TYPES.add(TileTypes.HPHRIO_RBRK_L);
        _TILE_TYPES.add(TileTypes.CFRM_AMS_CFGIO);
        _TILE_TYPES.add(TileTypes.CLK_IBRK_FSR2IO);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_PCIE_R_RBRK);
        _TILE_TYPES.add(TileTypes.INT_IBRK_RBRK_R_L);
        _TILE_TYPES.add(TileTypes.RCLK_HRIO_L);
        _TILE_TYPES.add(TileTypes.INT_IBRK_RBRK_XIPHY2INT);
        _TILE_TYPES.add(TileTypes.CFG_CFG);
        _TILE_TYPES.add(TileTypes.CLE_M_R);
        _TILE_TYPES.add(TileTypes.RCLK_DSP_CLKBUF_L);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_PCIE_R_TERM_B);
        _TILE_TYPES.add(TileTypes.INT_IBRK_L_R);
        _TILE_TYPES.add(TileTypes.CFRM_TERM_B);
        _TILE_TYPES.add(TileTypes.INT_IBRK_TERM_B_FSR2IO);
        _TILE_TYPES.add(TileTypes.HRIO_TERM_B_L);
        _TILE_TYPES.add(TileTypes.PCIE);
        _TILE_TYPES.add(TileTypes.CFG_CTR_TERM_B);
        _TILE_TYPES.add(TileTypes.CFRM_B);
        _TILE_TYPES.add(TileTypes.HRIO_RBRK_L);
        _TILE_TYPES.add(TileTypes.RCLK_AMS_CFGIO);
        _TILE_TYPES.add(TileTypes.INT_TERM_L_IO_RBRK);
        _TILE_TYPES.add(TileTypes.CFRM_CFG);
        _TILE_TYPES.add(TileTypes.CLEL_L_TERM_B);
        _TILE_TYPES.add(TileTypes.INT_IBRK_TERM_B_L_R);
        _TILE_TYPES.add(TileTypes.INT_INTERFACE_GT_R_TERM_B);
        _TILE_TYPES.add(TileTypes.RCLK_IBRK_L_R);
        _TILE_TYPES.add(TileTypes.CFRM_R_TERM_B);
        _TILE_TYPES.add(TileTypes.CFG_PCIE_AMS_RBRK);
        _TILE_TYPES.add(TileTypes.CFRM_RBRK_CFGIO);
        _TILE_TYPES.add(TileTypes.CFRM_RBRK_B);
        _TILE_TYPES.add(TileTypes.AMS);
        _SITE_TYPES.add(SiteTypes.SLICEL);
        _SITE_TYPES.add(SiteTypes.BUFCE_LEAF_X16);
        _SITE_TYPES.add(SiteTypes.BUFCE_ROW);
        _SITE_TYPES.add(SiteTypes.SLICEM);
        _SITE_TYPES.add(SiteTypes.RAMBFIFO18);
        _SITE_TYPES.add(SiteTypes.FIFO18_0);
        _SITE_TYPES.add(SiteTypes.RAMB180);
        _SITE_TYPES.add(SiteTypes.RAMB181);
        _SITE_TYPES.add(SiteTypes.RAMBFIFO36);
        _SITE_TYPES.add(SiteTypes.FIFO36);
        _SITE_TYPES.add(SiteTypes.RAMB36);
        _SITE_TYPES.add(SiteTypes.BITSLICE_CONTROL);
        _SITE_TYPES.add(SiteTypes.BITSLICE_RX_TX);
        _SITE_TYPES.add(SiteTypes.BITSLICE_COMPONENT_RX_TX);
        _SITE_TYPES.add(SiteTypes.BITSLICE_RXTX_RX);
        _SITE_TYPES.add(SiteTypes.BITSLICE_RXTX_TX);
        _SITE_TYPES.add(SiteTypes.BITSLICE_TX);
        _SITE_TYPES.add(SiteTypes.BUFGCE_DIV);
        _SITE_TYPES.add(SiteTypes.BUFGCE);
        _SITE_TYPES.add(SiteTypes.BUFGCTRL);
        _SITE_TYPES.add(SiteTypes.MMCME3_ADV);
        _SITE_TYPES.add(SiteTypes.PLLE3_ADV);
        _SITE_TYPES.add(SiteTypes.PLL_SELECT_SITE);
        _SITE_TYPES.add(SiteTypes.RIU_OR);
        _SITE_TYPES.add(SiteTypes.XIPHY_FEEDTHROUGH);
        _SITE_TYPES.add(SiteTypes.DSP48E2);
        _SITE_TYPES.add(SiteTypes.HARD_SYNC);
        _SITE_TYPES.add(SiteTypes.HPIOBDIFFINBUF);
        _SITE_TYPES.add(SiteTypes.HPIOBDIFFOUTBUF);
        _SITE_TYPES.add(SiteTypes.HPIO_VREF_SITE);
        _SITE_TYPES.add(SiteTypes.HPIOB);
        _SITE_TYPES.add(SiteTypes.BUFG_GT_SYNC);
        _SITE_TYPES.add(SiteTypes.BUFG_GT);
        _SITE_TYPES.add(SiteTypes.GTHE3_CHANNEL);
        _SITE_TYPES.add(SiteTypes.GTHE3_COMMON);
        _SITE_TYPES.add(SiteTypes.HRIODIFFINBUF);
        _SITE_TYPES.add(SiteTypes.HRIODIFFOUTBUF);
        _SITE_TYPES.add(SiteTypes.HRIO);
        _SITE_TYPES.add(SiteTypes.CONFIG_SITE);
        _SITE_TYPES.add(SiteTypes.PCIE_3_1);
        _SITE_TYPES.add(SiteTypes.SYSMONE1);
    }

    /* ------ AUTO-GENERATED --- DO NOT EDIT ABOVE ------ */
    static {
    /* ------ CLASSIFICATIONS GO HERE ------ */
    	
    	/* ***********************
    	 * 		Tile Types
    	 * ***********************/
        _CLB_TILES.add(TileTypes.CLE_M);
        _CLB_TILES.add(TileTypes.CLE_M_R);
        _CLB_TILES.add(TileTypes.CLEL_L);
        _CLB_TILES.add(TileTypes.CLEL_R);
        
        _SWITCHBOX_TILES.add(TileTypes.INT);

        _BRAM_TILES.add(TileTypes.BRAM);
        
        _DSP_TILES.add(TileTypes.DSP);
	
        _IO_TILES.add(TileTypes.HPIO_L);
        _IO_TILES.add(TileTypes.HRIO_L);
        _IO_TILES.add(TileTypes.AMS);
        _IO_TILES.add(TileTypes.GTH_R);
          	
    	/* ***********************
    	 * 		Site Types
    	 * ***********************/
    	_SLICE_SITES.add(SiteTypes.SLICEL);
        _SLICE_SITES.add(SiteTypes.SLICEM);

        _BRAM_SITES.add(SiteTypes.RAMB180);
        _BRAM_SITES.add(SiteTypes.RAMB181);
        _BRAM_SITES.add(SiteTypes.RAMB36);
        _BRAM_SITES.add(SiteTypes.RAMBFIFO18);
        _BRAM_SITES.add(SiteTypes.RAMBFIFO36);

        _FIFO_SITES.add(SiteTypes.FIFO18_0);
        _FIFO_SITES.add(SiteTypes.FIFO36);
        _FIFO_SITES.add(SiteTypes.RAMBFIFO18);
        _FIFO_SITES.add(SiteTypes.RAMBFIFO36);
        
        _DSP_SITES.add(SiteTypes.DSP48E2);

        _IO_SITES.add(SiteTypes.HPIOB);
        _IO_SITES.add(SiteTypes.HRIO);
        _IO_SITES.add(SiteTypes.SYSMONE1);
        _IO_SITES.add(SiteTypes.GTHE3_CHANNEL);
        _IO_SITES.add(SiteTypes.GTHE3_COMMON);
    }
}
