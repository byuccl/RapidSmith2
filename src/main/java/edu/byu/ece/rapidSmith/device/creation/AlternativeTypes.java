package edu.byu.ece.rapidSmith.device.creation;

import edu.byu.ece.rapidSmith.device.SiteType;

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
	public SiteType[] types;
	private Set<SiteType> set;
	private Integer hash = null;

	public AlternativeTypes(SiteType[] types) {
		if (types == null)
			types = new SiteType[0];
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
