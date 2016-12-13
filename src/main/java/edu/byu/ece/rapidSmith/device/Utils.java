/*
 * Copyright (c) 2010 Brigham Young University
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
package edu.byu.ece.rapidSmith.device;

import java.util.HashSet;

/**
 * This is a helper class for creating PrimitiveTypes and TileTypes
 * as well as helping to categorize TileTypes. 
 */
public class Utils{

	private static HashSet<TileType> clbs;
	
	private static HashSet<TileType> dsps;
	
	private static HashSet<TileType> brams;
	
	private static HashSet<TileType> ints;
	
	/**
	 * Returns a PrimitiveType enum based on the given string. If such
	 * an enum does not exist, it will return null.
	 * @param s The string to be converted to an enum type
	 * @return The PrimitiveType corresponding to the string s, null if none exists.
	 */
	public static SiteType createPrimitiveType(String s){
		return SiteType.valueOf(s.toUpperCase());
	}

	/**
	 * Returns a TileType enum based on the given string s.  If such an enum
	 * does not exist, it will return null
	 * @param s The string to be converted to an enum type
	 * @return The TileType corresponding to String s, null if none exists.
	 */
	public static TileType createTileType(String s){
		return TileType.valueOf(s.toUpperCase());
	}
	
	/**
	 * Determines if the provided tile type contains SLICE primitive sites
	 * of any type.
	 * @param type The tile type to test for.
	 * @return True if this tile type has SLICE (any kind) primitive sites.
	 */
	public static boolean isCLB(TileType type){
		return clbs.contains(type);
	}
	
	/**
	 * Determines if the provided tile type contains DSP primitive sites
	 * of any type.
	 * @param type The tile type to test for.
	 * @return True if this tile type has DSP (any kind) primitive sites.
	 */
	public static boolean isDSP(TileType type){
		return dsps.contains(type);
	}
	
	/**
	 * Determines if the provided tile type contains BRAM primitive sites
	 * of any type.
	 * @param type The tile type to test for.
	 * @return True if this tile type has BRAM (any kind) primitive sites.
	 */
	public static boolean isBRAM(TileType type){
		return brams.contains(type);
	}
	
	/**
	 * Determines if the provided tile type contains BRAM primitive sites
	 * of any type.
	 * @param type The tile type to test for.
	 * @return True if this tile type has BRAM (any kind) primitive sites.
	 */
	public static boolean isSwitchBox(TileType type){
		return ints.contains(type);
	}

	static{
		clbs = new HashSet<>();
		clbs.add(TileType.CLB);
		clbs.add(TileType.CLBLL);
		clbs.add(TileType.CLBLM);
		clbs.add(TileType.CLEXL);
		clbs.add(TileType.CLEXM);
		clbs.add(TileType.CLBLL_L);
		clbs.add(TileType.CLBLL_R);
		clbs.add(TileType.CLBLM_L);
		clbs.add(TileType.CLBLM_R);
		
		dsps = new HashSet<>();
		dsps.add(TileType.DSP);
		dsps.add(TileType.DSP_L);
		dsps.add(TileType.DSP_R);
		dsps.add(TileType.MACCSITE2);
		dsps.add(TileType.MACCSITE2_BRK);
		dsps.add(TileType.BRAMSITE);
		dsps.add(TileType.BRAMSITE2);
		dsps.add(TileType.BRAMSITE2_BRK);
		
		brams = new HashSet<>();
		brams.add(TileType.BRAM);
		brams.add(TileType.BRAM_L);
		brams.add(TileType.BRAM_R);
		brams.add(TileType.LBRAM);
		brams.add(TileType.RBRAM);
		brams.add(TileType.BRAMSITE);
		brams.add(TileType.BRAMSITE2);
		brams.add(TileType.BRAMSITE2_3M);
		brams.add(TileType.BRAMSITE2_3M_BRK);
		brams.add(TileType.BRAMSITE2_BRK);
		brams.add(TileType.MBRAM);

		ints = new HashSet<>();
		ints.add(TileType.INT);
		ints.add(TileType.INT_L);
		ints.add(TileType.INT_R);
		ints.add(TileType.INT_SO);
		ints.add(TileType.INT_SO_DCM0);
		ints.add(TileType.INT_BRAM);
		ints.add(TileType.INT_BRAM_BRK);
		ints.add(TileType.INT_BRK);
		ints.add(TileType.INT_GCLK);
		ints.add(TileType.IOI_INT);
		ints.add(TileType.INT_TERM);
		ints.add(TileType.INT_TERM_BRK);
		ints.add(TileType.LIOI_INT);
		ints.add(TileType.LIOI_INT_BRK);
		
	}
}
