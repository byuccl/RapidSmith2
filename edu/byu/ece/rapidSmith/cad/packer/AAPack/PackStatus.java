package edu.byu.ece.rapidSmith.cad.packer.AAPack;

/**
 *
 */
public enum PackStatus {
	VALID, INFEASIBLE, CONDITIONAL;

	public static PackStatus meet(PackStatus el1, PackStatus el2) {
		switch (el1) {
			case VALID:
				return el2;
			case CONDITIONAL:
				if (el2 == INFEASIBLE)
					return INFEASIBLE;
				return CONDITIONAL;
			case INFEASIBLE:
				return INFEASIBLE;
		}
		throw new AssertionError("Illegal option");
	}
}
