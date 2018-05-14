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
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.device.TileType;
import java.util.*;

/**
 *
 */
public class FamilyInfos {

    private static final Map<FamilyType, FamilyInfo> familyInfoMap = new LinkedHashMap<>();

    private static final FamilyInfo EMPTY_FAMILYINFO = new EmptyFamilyInfo();

    static {
        familyInfoMap.put(Virtex6.FAMILY_TYPE, new Virtex6());
        familyInfoMap.put(Artix7.FAMILY_TYPE, new Artix7());
        familyInfoMap.put(Kintexu.FAMILY_TYPE, new Kintexu());
        familyInfoMap.put(Zynq.FAMILY_TYPE, new Zynq());
    }

    public static FamilyInfo get(FamilyType family) {
        return familyInfoMap.getOrDefault(family, EMPTY_FAMILYINFO);
    }

    public static Set<FamilyType> supportedFamilies() {
        return Collections.unmodifiableSet(familyInfoMap.keySet());
    }

    private static final class EmptyFamilyInfo implements FamilyInfo {

        @Override
        public List<String> generatedFrom() {
            return Collections.emptyList();
        }

        @Override
        public List<TileType> tileTypes() {
            return Collections.emptyList();
        }

        @Override
        public List<SiteType> siteTypes() {
            return Collections.emptyList();
        }

        @Override
        public Set<TileType> clbTiles() {
            return Collections.emptySet();
        }

        @Override
        public Set<TileType> switchboxTiles() {
            return Collections.emptySet();
        }

        @Override
        public Set<TileType> bramTiles() {
            return Collections.emptySet();
        }

        @Override
        public Set<TileType> dspTiles() {
            return Collections.emptySet();
        }

        @Override
        public Set<TileType> ioTiles() {
            return Collections.emptySet();
        }

        @Override
        public Set<SiteType> sliceSites() {
            return Collections.emptySet();
        }

        @Override
        public Set<SiteType> ioSites() {
            return Collections.emptySet();
        }

        @Override
        public Set<SiteType> dspSites() {
            return Collections.emptySet();
        }

        @Override
        public Set<SiteType> bramSites() {
            return Collections.emptySet();
        }

        @Override
        public Set<SiteType> fifoSites() {
            return Collections.emptySet();
        }
    }
}
