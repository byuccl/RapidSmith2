package edu.byu.ece.rapidSmith.cad.packer.AAPack;

/**
 *
 */
public enum Routability {
	FEASIBLE, INFEASIBLE, CONDITIONAL;

	public Routability meet(Routability other) {
		if (this == INFEASIBLE || other == INFEASIBLE)
			return INFEASIBLE;
		if (this == CONDITIONAL || other == CONDITIONAL)
			return CONDITIONAL;
		return FEASIBLE;
	}
}
