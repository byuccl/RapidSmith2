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
package edu.byu.ece.rapidSmith.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class is a special data structure used for Xilinx FPGA devices to help reduce memory footprint
 * of objects.  It keeps exactly one copy of an object of type E and maintains a unique integer enumeration
 * of each object.  It depends on the type E's equals() and hashCode() function to determine uniqueness.
 * @author Chris Lavin
 * Created on: Apr 30, 2010
 * @param <E> The type of object to use.
 */
public class HashPool<E> implements Iterable<E> {
	private final HashMap<E, Integer> map;
	private final ArrayList<E> enumeration;

	public HashPool() {
		map = new HashMap<>(512, 0.4f);
		enumeration = new ArrayList<>();
	}

	/**
	 * Adds the object to the pool if an identical copy doesn't already exist.
	 * @param obj The object to be added
	 * @return The unique object contained in the HashPool
	 */
	public synchronized E add(E obj) {
		if (map.containsKey(obj))
			return enumeration.get(map.get(obj));
		map.put(obj, map.size());
		enumeration.add(obj);
		return obj;
	}

	public int size() {
		return map.size();
	}

	public ArrayList<E> values() {
		return enumeration;
	}

	public int getEnumeration(E obj) {
		return map.get(obj);
	}


	@Override
	public Iterator<E> iterator() {
		return Collections.unmodifiableCollection(enumeration).iterator();
	}
}
