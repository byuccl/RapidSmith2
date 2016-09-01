package edu.byu.ece.rapidSmith.util.luts;

/**
 * Created by Haroldsen on 2/21/2015.
 */
public enum OpType {
    AND, OR, XOR;

    public String toString() {
        switch (this) {
            case AND: return "*";
            case OR: return "+";
            case XOR: return "@";
        }
        throw new AssertionError("Unknown operation");
    }
}
