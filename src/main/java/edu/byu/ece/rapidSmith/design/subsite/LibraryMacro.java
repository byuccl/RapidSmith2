package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelId;

import java.util.List;

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
