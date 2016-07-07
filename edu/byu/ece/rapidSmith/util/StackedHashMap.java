package edu.byu.ece.rapidSmith.util;

import java.util.*;

/**
 *
 */
public class StackedHashMap<K, V> implements Map<K, V> {
	int modCount = 0;
	ArrayDeque<Integer> modificationStack = new ArrayDeque<>();
	private HashMap<K, LevelValuePair<V>> map = new HashMap<>();

	public StackedHashMap() {
		modificationStack.push(0);
	}

	public void checkPoint() {
		modificationStack.push(++modCount);
	}

	public void rollBack() {
		modificationStack.pop();
	}

	public int depth() {
		return modificationStack.size() - 1;
	}

	public boolean isCurrent(Object key) {
		return isCurrentLevel(getLastValidPair(key));
	}

	private void removeInvalidated() {
		List<K> keys = new ArrayList<>(map.keySet());
		keys.forEach(this::getLastValidPair);
	}

	@Override
	public int size() {
		removeInvalidated();
		return (int) map.values().stream().filter(i -> i != null).count();
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return get(key) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		return values().contains(value);
	}

	@Override
	public V get(Object key) {
		LevelValuePair<V> newValue = getLastValidPair(key);
		return newValue != null ? newValue.value : null;
	}

	private LevelValuePair<V> getLastValidPair(Object key) {
		@SuppressWarnings("SuspiciousMethodCalls")
		LevelValuePair<V> oldPair = map.get(key);
		LevelValuePair<V> newPair = oldPair;

		if (oldPair == null)
			return null;

		// Remove old values
		while (newPair != null && shouldRemove(newPair))
			newPair = newPair.parent;

		if (newPair == null) {
			//noinspection SuspiciousMethodCalls
			map.remove(key);
		} else if (newPair != oldPair) {
			//noinspection unchecked
			map.put((K) key, newPair);
		}
		return newPair;
	}

	private boolean shouldRemove(LevelValuePair<V> pair) {
		for (Integer level : modificationStack) {
			if (pair.modification == level)
				return false;
			else if (pair.modification > level)
				return true;
		}
		throw new AssertionError("pair.modification should never be less than" +
				" the lowest level (0)");
	}

	private boolean isCurrentLevel(LevelValuePair<V> pair) {
		return pair.modification == modificationStack.peek();
	}

	@Override
	public V put(K key, V value) {
		Objects.requireNonNull(value);

		LevelValuePair<V> newPair = new LevelValuePair<>(modCount, value);
		LevelValuePair<V> oldPair = getLastValidPair(key);

		if (oldPair != null && isCurrentLevel(oldPair)) {
			// replace the old value as it is the same age as the
			// new value
			newPair.parent = oldPair.parent;
		} else {
			// don't overwrite the old value, it's farther up the chain
			// than the new value
			newPair.parent = oldPair;
		}
		map.put(key, newPair);

		return oldPair != null ? oldPair.value : null;
	}

	@Override
	public V remove(Object key) {
		LevelValuePair<V> oldPair = getLastValidPair(key);
		if (oldPair == null)
			return null;

		V retValue = oldPair.value;
		if (isCurrentLevel(oldPair)) {
			// remove this level
			oldPair = oldPair.parent;
		}

		// if something older exists, add a null value, otherwise just
		// delete the entry
		if (oldPair == null) {
			map.remove(key);
		} else {
			//noinspection unchecked
			LevelValuePair<V> newValue = new LevelValuePair<>(modCount, null);
			newValue.parent = oldPair;
			//noinspection unchecked
			map.put((K) key, newValue);
		}

		return retValue;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
			put(e.getKey(), e.getValue());
		}
	}

	@Override
	public void clear() {
		map = new HashMap<>();
		modificationStack = new ArrayDeque<>();
		modCount = 0;
	}

	@Override
	public Set<K> keySet() {
		return new KeySet();
	}

	@Override
	public Collection<V> values() {
		return new Values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new EntrySet();
	}

	private static class LevelValuePair<V> {
		LevelValuePair<V> parent;
		int modification;
		V value;

		public LevelValuePair(int modification, V value) {
			this.modification = modification;
			this.value = value;
		}
	}

	private class EntrySet implements Set<Entry<K, V>> {
		@Override
		public int size() {
			return StackedHashMap.this.size();
		}

		@Override
		public boolean isEmpty() {
			return StackedHashMap.this.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<Entry<K, V>> iterator() {
			return new EntrySetIterator();
		}

		@Override
		public Object[] toArray() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean add(Entry<K, V> kvEntry) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends Entry<K, V>> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
	}

	private class EntrySetIterator implements Iterator<Map.Entry<K, V>> {
		private Iterator<K> it;
		private Map.Entry<K, V> next = null;
		private Map.Entry<K, V> prev = null;

		public EntrySetIterator() {
			it = new ArrayList<>(map.keySet()).iterator();
		}

		@Override
		public boolean hasNext() {
			if (next != null)
				return true;

			while (it.hasNext()) {
				K key = it.next();
				V value = get(key);
				if (value != null) {
					next = new AbstractMap.SimpleImmutableEntry<>(key, value);
					break;
				}
			}
			return next != null;
		}

		@Override
		public Entry<K, V> next() {
			if (!hasNext())
				throw new NoSuchElementException();
			Entry<K, V> ret = next;
			prev = ret;
			next = null;
			return ret;
		}

		@Override
		public void remove() {
			if (prev == null)
				throw new IllegalStateException();
			StackedHashMap.this.remove(prev.getKey());
			prev = null;
		}
	}

	private class KeySet implements Set<K> {
		@Override
		public int size() {
			return StackedHashMap.this.size();
		}

		@Override
		public boolean isEmpty() {
			return StackedHashMap.this.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return StackedHashMap.this.containsKey(o);
		}

		@Override
		public Iterator<K> iterator() {
			return new KeyIterator();
		}

		@Override
		public Object[] toArray() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean add(K k) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			return StackedHashMap.this.remove(o) != null;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) {
				if (!StackedHashMap.this.containsKey(o))
					return false;
			}
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends K> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			for (Object o : c) {
				if (StackedHashMap.this.remove(o) != null)
					return false;
			}
			return true;
		}

		@Override
		public void clear() {
			StackedHashMap.this.clear();
		}
	}

	private class KeyIterator implements Iterator<K> {
		private Iterator<Entry<K, V>> it;

		public KeyIterator() {
			this.it = entrySet().iterator();
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public K next() {
			return it.next().getKey();
		}
	}

	private class Values extends AbstractCollection<V> implements Collection<V> {
		@Override
		public int size() {
			return StackedHashMap.this.size();
		}

		@Override
		public Iterator<V> iterator() {
			return new ValuesIterator();
		}
	}

	private class ValuesIterator implements Iterator<V> {
		private Iterator<Entry<K, V>> it;

		public ValuesIterator() {
			this.it = entrySet().iterator();
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public V next() {
			return it.next().getValue();
		}
	}
}
