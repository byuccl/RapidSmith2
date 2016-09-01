package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelId;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 */
public class SimpleLibraryCell extends LibraryCell {
	/** List of types of BELs cells of this type can be placed on */
	private List<BelId> compatibleBels;
	/** List of LibraryPins of this LibraryCell */
	private List<LibraryPin> libraryPins;
	private Map<BelId, Map<String, SiteProperty>> sharedSiteProperties;

	private boolean isVccSource;
	private boolean isGndSource;
	private Integer numLutInputs = null;
	private boolean isPort;

	public SimpleLibraryCell(String name) {
		super(name);
	}

	@Override
	public boolean isMacro() {
		return false;
	}

	@Override
	public boolean isPort() {
		return isPort;
	}
	
	@Override
	public boolean isVccSource() {
		return isVccSource;
	}

	public void setVccSource(boolean isVccSource) {
		this.isVccSource = isVccSource;
	}

	@Override
	public boolean isGndSource() {
		return isGndSource;
	}

	public void setIsPort(boolean isPort) {
		this.isPort = isPort;
	}
	
	public void setGndSource(boolean isGndSource) {
		this.isGndSource = isGndSource;
	}

	public void setNumLutInputs(Integer numInputs) {
		this.numLutInputs = numInputs;
	}

	@Override
	public boolean isLut() {
		return numLutInputs != null;
	}

	@Override
	public Integer getNumLutInputs() {
		return numLutInputs;
	}

	/**
	 * @return the templates of the pins that reside on cells of this type
	 */
	@Override
	public List<LibraryPin> getLibraryPins() {
		return libraryPins;
	}

	/**
	 * List containing the templates of all this pins on this site
	 */
	public void setLibraryPins(List<LibraryPin> libraryPins) {
		this.libraryPins = libraryPins;
	}

	/**
	 * @return list of the {@link BelId}s of BELs that cells of this type can be
	 * placed on
	 */
	@Override
	public List<BelId> getPossibleAnchors() {
		return compatibleBels;
	}

	/**
	 * List containing the {@link BelId}s of BELs that cells of this type can be
	 * placed on.
	 */
	public void setPossibleBels(List<BelId> possibleBels) {
		this.compatibleBels = possibleBels;
	}

	@Override
	public List<Bel> getRequiredBels(Bel anchor) {
		return Collections.singletonList(anchor);
	}

	@Override
	public Map<String, SiteProperty> getSharedSiteProperties(BelId anchor) {
		return sharedSiteProperties.get(anchor);
	}

	public void setSharedSiteProperties(
			Map<BelId, Map<String, SiteProperty>> sharedSiteProperties
	) {
		this.sharedSiteProperties = sharedSiteProperties;
	}

	@Override
	/**
	 * Library cells with the same name are considered equals.
	 */
	public boolean equals(Object o) {
		// I'd prefer this to be identity equals but I think that would break
		// code somewhere.
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LibraryCell that = (LibraryCell) o;
		return Objects.equals(getName(), that.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName());
	}

	@Override
	public String toString() {
		return "LibraryCell{" +
				"name='" + getName() + '\'' +
				'}';
	}
}
