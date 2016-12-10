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
	}
}
