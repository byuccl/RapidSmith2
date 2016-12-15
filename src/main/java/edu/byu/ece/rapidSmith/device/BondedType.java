package edu.byu.ece.rapidSmith.device;

/**
 * Created by haroldsen on 12/20/14.
 */
public enum BondedType {
	BONDED("bonded"),
	UNBONDED("unbonded"),
	INTERNAL("internal");

	private final String string;
	BondedType(String string) {
		this.string = string;
	}

	public String toString() {
		return string;
	}
}
