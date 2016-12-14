package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelId;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *  Provides a template of possible cells for a design.
 */
public abstract class LibraryCell implements Serializable {
	private final String name;

	public LibraryCell(String name) {
		Objects.nonNull(name);
		this.name = name;
	}

	public final String getName() {
		return name;
	}

	abstract public boolean isMacro();
	abstract public boolean isVccSource();
	abstract public boolean isGndSource();
	abstract public boolean isLut();
	abstract public boolean isPort();
	abstract public Integer getNumLutInputs();
	abstract public List<LibraryPin> getLibraryPins();
	abstract public List<BelId> getPossibleAnchors();
	abstract public List<Bel> getRequiredBels(Bel anchor);
	abstract public Map<String, SiteProperty> getSharedSiteProperties(BelId anchor);

	/**
	 * Returns the {@link LibraryPin} on this LibraryCell with the given name.<p>
	 * Operates in O{# of pins} time.
	 */
	public LibraryPin getLibraryPin(String pinName) {
		for (LibraryPin pin : getLibraryPins()) {
			if (pin.getName().equals(pinName))
				return pin;
		}
		return null;
	}
}
