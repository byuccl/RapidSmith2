package edu.byu.ece.rapidSmith.design.subsite;

/**
 *
 */
public abstract class LibraryMacro extends LibraryCell {
	public LibraryMacro(String name) {
		super(name);
	}

	@Override
	public boolean isMacro() {
		return true;
	}

	@Override
	public boolean isLut() {
		return false;
	}
}
