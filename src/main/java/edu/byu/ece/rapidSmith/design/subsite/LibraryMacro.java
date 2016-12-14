package edu.byu.ece.rapidSmith.design.subsite;

/**
 *
 */
public abstract class LibraryMacro extends LibraryCell {
	private static final long serialVersionUID = 282290704449047358L;

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
