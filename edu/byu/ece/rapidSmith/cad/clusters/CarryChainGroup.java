package edu.byu.ece.rapidSmith.cad.clusters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class CarryChainGroup {
	private int nextIndex = 0;
	private Map<Integer, Set<PackCell>> cellsInGroup = new HashMap<>();

	public int getUniqueIndex() {
		return nextIndex++;
	}

	public Map<Integer, Set<PackCell>> getCellsInGroup() {
		return cellsInGroup;
	}

	public void absorbGroup(CarryChainGroup o, Map<Integer, Integer> indexMap) {
		for (Map.Entry<Integer, Set<PackCell>> oCells : o.cellsInGroup.entrySet()) {
			Integer newIndex = indexMap.get(oCells.getKey());
			if (newIndex == null)
				newIndex = getUniqueIndex();
			for (PackCell cell : oCells.getValue()) {
				cell.setCarryGroup(this);
				cell.setCarryIndex(newIndex);
			}
			cellsInGroup.computeIfAbsent(newIndex, k -> new HashSet<>())
					.addAll(oCells.getValue());
		}
		o.cellsInGroup = null;
	}

	public void addCell(PackCell cell, int index) {
		cell.setCarryGroup(this);
		cell.setCarryIndex(index);
		cellsInGroup.computeIfAbsent(index, k -> new HashSet<>()).add(cell);
	}
}
