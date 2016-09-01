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
package edu.byu.ece.rapidSmith.primitiveDefs;

import edu.byu.ece.rapidSmith.device.SiteType;

import java.util.HashMap;
import java.util.Iterator;

public class PrimitiveDefList
		extends HashMap<SiteType, PrimitiveDef>
		implements Iterable<PrimitiveDef> {
	public PrimitiveDef getPrimitiveDef(SiteType type){
		return get(type);
	}
	
	public boolean add(PrimitiveDef e) {
		if (containsKey(e.getType()))
			return false;
		put(e.getType(), e);
		return true;
	}

	@Override
	public Iterator<PrimitiveDef> iterator() {
		return values().iterator();
	}
}
