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
package edu.byu.ece.rapidSmith.device;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * DO NOT USE THIS CLASS! This class was specially developed for the Device wire
 * connections hash map. It is specifically optimized for that purpose. Created
 * on: Mar 18, 2011
 */
public class WireHashMap implements Serializable {
	/**
	 * The default initial capacity - MUST be a power of two.
	 */
	private static final int DEFAULT_INITIAL_CAPACITY = 16;

	/**
	 * The maximum capacity, used if a higher value is implicitly specified by
	 * either of the constructors with arguments. MUST be a power of two <= 1<<30.
	 */
	private static final int MAXIMUM_CAPACITY = 1 << 30;

	/**
	 * The load factor used when none specified in constructor.
	 */
	private static final float DEFAULT_LOAD_FACTOR = 0.85f;
	private static final long serialVersionUID = -1457572150224638234L;

	/**
	 * The keys table. Length MUST Always be a power of two.
	 */
	private int[] keys;

	/**
	 * The corresponding values table.
	 */
	public WireConnection[][] values;

	/**
	 * The number of key-value mappings contained in this map.
	 */
	private int size;

	// These variables are used to track the whether the caches are up to date.
	// A cache is up to date if it is equivalent to the wireHashMapModification
	// value. Any put operation updates the wireHashMapModification value
	private transient int wireHashMapModification = 0;
	private transient int keySetCacheModification = -1; // initialize as out of date
	private transient int valuesCacheModification = -1; // initialize as out of date

	// Caches are stored as soft references to avoid being a memory drain
	// when not in use.
	private transient SoftReference<Set<Integer>> keySetCache;
	private transient SoftReference<ArrayList<WireConnection[]>> valuesCache;

	/**
	 * The next size value at which to resize (capacity * load factor).
	 * 
	 * @serial
	 */
	private int threshold;

	/**
	 * The load factor for the hash table.
	 *
	 * @serial
	 */
	private final float loadFactor;

	private Integer hash;

	/**
	 * This map requires an initial capacity. This map will not grow.
	 * 
	 * @param capacity
	 *            the set capacity for this hash map
	 * @param loadFactor
	 *            the load factor for this hash map before growing
	 */
	private WireHashMap(int capacity, float loadFactor) {
		if (capacity < 0)
			throw new IllegalArgumentException("Illegal initial capacity: " + capacity);
		if (capacity > MAXIMUM_CAPACITY)
			capacity = MAXIMUM_CAPACITY;

		// Find a power of 2 >= initialCapacity
		int finalCapacity = 4;
		while (finalCapacity < capacity)
			finalCapacity <<= 1;

		this.loadFactor = loadFactor;
		threshold = (int) (finalCapacity * loadFactor);

		keys = new int[finalCapacity];
		Arrays.fill(keys, -1);
		values = new WireConnection[finalCapacity][];
		size = 0;
	}

	private WireHashMap(float loadFactor) {
		this.loadFactor = loadFactor;
	}

	public WireHashMap() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Returns the number of key-value mappings in this map.
	 *
	 * @return the number of key-value mappings in this map
	 */
	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	private int indexFor(int key) {
		int i = key & (keys.length - 1);
		while (keys[i] != key && keys[i] != -1) {
			i += 3;
			if (i >= keys.length)
				i = i & 3;
		}
		return i;
	}

	public WireConnection[] get(int key) {
		int i = indexFor(key);
		//if (keys[i] == -1 || keys[i] == 0)
		if (keys[i] == -1)
			return null;
		return values[i];
	}

	public void put(int key, WireConnection[] value) {
		int i = indexFor(key);
		if (keys[i] == -1)
			size++;
		keys[i] = key;
		values[i] = value;
		wireHashMapModification++;

		if (size > threshold) {
			grow();
		}
	}

	/**
	 * Removes the mapping for the specified key from the map.
	 * 
	 * @param key
	 */
	/*
	public void remove(int key) {
		System.out.println("Remove all of key " + key);
		int i = indexFor(key);
		if (keys[i] == -1) {
			// key does not exist
			// TODO: Throw an exception
			return;
		}

		// Remove the values and key
		int valueLength = values[i].length;
		values = ArrayUtils.removeElement(values, values[i]);
		size -= valueLength;
		keys[i] = -1;
		
		wireHashMapModification++;
	}
	*/

	/**
	 * Removes a WireConnection from the value array for the specified key.
	 * @param key
	 * @param value
	 */
	/*
	public void remove(int key, WireConnection value) {		
		int i = indexFor(key);
		if(keys[i] == -1) {
			// key does not exist
			// TODO: Throw an exception
			return;
		}
		
		// Find the value and remove it from the map
		values[i] = ArrayUtils.removeElement(values[i], value);
		size--;
		wireHashMapModification++;
	}
	*/

	private void grow() {
		int newCapacity = keys.length * 2;
		threshold = (int) (newCapacity * loadFactor);
		int[] oldKeys = keys;
		WireConnection[][] oldValues = values;
		keys = new int[newCapacity];
		Arrays.fill(keys, -1);
		values = new WireConnection[newCapacity][];
		size = 0;
		for (int i = 0; i < oldKeys.length; i++) {
			if (oldKeys[i] != -1) {
				put(oldKeys[i], oldValues[i]);
			}
		}
	}

	public Set<Integer> keySet() {
		// check if the cached keySets are current
		Set<Integer> keySet = keySetCache == null ? null : keySetCache.get();
		if (keySet != null && keySetCacheModification == wireHashMapModification)
			return keySet;
		keySetCacheModification = wireHashMapModification;

		// build the keyset cache
		keySet = new HashSet<>();
		for (int key : keys) {
			if (key != -1)
				keySet.add(key);
		}
		keySetCache = new SoftReference<>(keySet);
		return keySet;
	}

	public ArrayList<WireConnection[]> values() {
		// check if the cached values are current;
		ArrayList<WireConnection[]> valuesList = valuesCache == null ? null : valuesCache.get();
		if (valuesList != null && valuesCacheModification == wireHashMapModification)
			return valuesList;
		valuesCacheModification = wireHashMapModification;

		// build the values cache
		valuesList = new ArrayList<>(size);
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != -1)
				valuesList.add(values[i]);
		}
		valuesCache = new SoftReference<>(valuesList);
		return valuesList;
	}

	@Override
	public int hashCode() {
		if (hash != null)
			return hash;
		hash = 0;
		for (Integer i : keySet()) {
			hash += i * 7;
			if (get(i) != null) {
				hash += Arrays.deepHashCode(get(i)) * 13;
			}
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if ((obj == null) || (getClass() != obj.getClass()))
			return false;

		WireHashMap other = (WireHashMap) obj;
		if (!keySet().equals(other.keySet())) {
			return false;
		}
		for (Integer key : keySet()) {
			if (!Arrays.deepEquals(get(key), other.get(key))) {
				return false;
			}
		}
		return true;
	}

	private static class WireHashMapReplace implements Serializable {
		private static final long serialVersionUID = -7320168883881265737L;
		private int arrSize;
		private float loadFactor;
		private int[] keys;
		private int[] indices;
		private WireConnection[][] values;

		@SuppressWarnings("unused")
		private WireHashMap readResolve() {
			WireHashMap whm = new WireHashMap(loadFactor);
			whm.keys = new int[arrSize];
			Arrays.fill(whm.keys, -1);
			whm.values = new WireConnection[arrSize][];

			for (int i = 0; i < keys.length; i++) {
				whm.keys[indices[i]] = keys[i];
				whm.values[indices[i]] = values[i];
			}

			whm.size = keys.length;
			whm.threshold = (int) (arrSize * loadFactor);
			return whm;
		}
	}

	@SuppressWarnings("unused")
	private WireHashMapReplace writeReplace() {
		WireHashMapReplace repl = new WireHashMapReplace();
		repl.arrSize = keys.length;
		repl.keys = new int[size];
		repl.indices = new int[size];
		repl.values = new WireConnection[size][];

		int j = 0;
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != -1) {
				repl.keys[j] = keys[i];
				repl.indices[j] = i;
				repl.values[j] = values[i];
				j++;
			}
		}

		repl.loadFactor = loadFactor;

		return repl;
	}

	public static WireHashMap EMPTY_WIRE_HASHMAP;

	static {
		EMPTY_WIRE_HASHMAP = new WireHashMap();
		EMPTY_WIRE_HASHMAP.size = 0;
		EMPTY_WIRE_HASHMAP.keys = new int[1];
		EMPTY_WIRE_HASHMAP.keys[0] = -1;
		EMPTY_WIRE_HASHMAP.values = new WireConnection[0][0];
	}
}
