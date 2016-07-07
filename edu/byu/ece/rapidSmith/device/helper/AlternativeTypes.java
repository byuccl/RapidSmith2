package edu.byu.ece.rapidSmith.device.helper;

import edu.byu.ece.rapidSmith.device.PrimitiveType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class AlternativeTypes {
	/**
	 * Sources of the tile
	 */
	public PrimitiveType[] types;
	private Set<PrimitiveType> set;
	private Integer hash = null;

	public AlternativeTypes(PrimitiveType[] types) {
		if (types == null)
			types = new PrimitiveType[0];
		this.types = types;
		set = new HashSet<>(Arrays.asList(types));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if (hash == null) {
			hash = set.hashCode();
		}
		return hash;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;

		AlternativeTypes other = (AlternativeTypes) obj;
		return set.equals(other.set);
	}
}
