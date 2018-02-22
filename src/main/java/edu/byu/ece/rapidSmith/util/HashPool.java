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
package edu.byu.ece.rapidSmith.util;

import java.util.*;

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
		map = new HashMap<>();
		enumeration = new ArrayList<>();
	}

	public HashPool(int initialSize) {
		map = new HashMap<>((int) (1.34 * initialSize));
		enumeration = new ArrayList<>(initialSize);
	}

	/**
	 * Adds an object to the pool.
	 * <p/>
	 * If the object is distinct from all others in the pool, the object will be
	 * added and the value is returned.  If a similar object already exists
	 * in the pool, this object is not added and the similar object is returned
	 * instead.
	 *
	 * @param obj the object to add
	 * @return the integer enumeration assigned to this object by this pool
	 */
	public synchronized E add(E obj) {
		Objects.requireNonNull(obj);
		Integer get = map.get(obj);
		if (get != null)
			return enumeration.get(get);
		map.put(obj, map.size());
		enumeration.add(obj);
		return obj;
	}

	/**
	 * Adds an object to the pool.
	 * <p/>
	 * If the object is distinct from all others in the pool, the object will be
	 * added and its integer mapping returned.  If a similar object already exists
	 * in the pool, this object is not added and the integer mapping for the
	 * similar object is returned.
	 *
	 * @param obj the object to add
	 * @return the integer enumeration assigned to this object by this pool
	 */
	public synchronized Integer add2(E obj) {
		Objects.requireNonNull(obj);
		Integer get = map.get(obj);
		if (get != null)
			return get;
		Integer e = map.size();
		map.put(obj, e);
		enumeration.add(obj);
		return e;
	}

	public int size() {
		return map.size();
	}

	public ArrayList<E> values() {
		return enumeration;
	}

	public Integer getEnumeration(E obj) {
		return map.get(obj);
	}

	@Override
	public Iterator<E> iterator() {
		return Collections.unmodifiableCollection(enumeration).iterator();
	}
}
