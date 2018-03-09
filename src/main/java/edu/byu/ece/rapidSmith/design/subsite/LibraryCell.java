/*
 * Copyright (c) 2016 Brigham Young University
 *
 * This file is part of the BYU RapidSmith Tools.
 *
 * BYU RapidSmith Tools is free software: you may redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * BYU RapidSmith Tools is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * A copy of the GNU General Public License is included with the BYU
 * RapidSmith Tools. It can be found at doc/LICENSE.GPL3.TXT. You may
 * also get a copy of the license at <http://www.gnu.org/licenses/>.
 */

package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelId;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *  Provides a template of possible cells for a design.
 */
public abstract class LibraryCell implements Serializable {
	private static final long serialVersionUID = -7850247997306342388L;
	/** Name of the Library cell*/
	private final String name;
	/** List of LibraryPins of this LibraryCell */
	private List<LibraryPin> libraryPins;
	/** Map holding the default properties for a Cell instance*/
	private Map<String, Property> defaultProperties;
	/** Cell configuration properties */
	private final Map<String, LibraryCellProperty> configurableProperties;

	/**
	 * Library Cell constructor
	 * @param name String name of the library cell (i.e. LUT6)
	 */
	public LibraryCell(String name) {
		Objects.nonNull(name);
		this.name = name;
		configurableProperties = new HashMap<>();
	}

	/**
	 * Returns the name of the Library Cell (i.e. LUT6)
	 */
	public final String getName() {
		return name;
	}

	/**
	 * @return the templates of the pins that reside on cells of this type
	 */
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

	// Cell property configuration methods 
	/**
	 * Adds a new configurable property to the library cell. This is package
	 * private and should not be used by normal users.
	 * 
	 * @param libCellProperty {@link LibraryCellProperty} object
	 */
	void addConfigurableProperty(LibraryCellProperty libCellProperty) {
		this.configurableProperties.put(libCellProperty.getName(), libCellProperty);
	}
	
	/**
	 * Adds a new default cell {@link Property} to the LibraryCell. This is
	 * package private and should not be called by normal users. 
	 */
	void addDefaultProperty(Property property) {
		
		if (defaultProperties == null) {
			defaultProperties = new HashMap<>();
		}
		
		defaultProperties.put(property.getKey(), property);		
	}
	
	/**
	 * Returns the default property map of the library cell.
	 * This is package private and should not be used by regular
	 * users.
	 */
	Map<String, Property> getDefaultPropertyMap() {
		return defaultProperties;
	}
	
	/**
	 * Returns a set of property names that are configurable
	 * on {@link Cell} instances of the library cell.
	 */
	public Set<String> getConfigurableProperties() {
		return configurableProperties.keySet();
	}
	
	/**
	 * Returns the default value for the specified configurable property 
	 * of the library cell. If the property does not exist on the library 
	 * cell or there is no default, {@code null} is returned.
	 * 
	 * @param propertyName Name of the property
	 */
	public String getDefaultValue(String propertyName) {
		
		if (defaultProperties == null) {
			return null;
		}
		
		Property prop = defaultProperties.get(propertyName);
		return prop == null ? null : prop.getStringValue();
	}
	
	/**
	 * Returns the default value for the specified configurable property of the library cell.
	 * If the property does not exist on the library cell, {@code null} is returned.
	 * 
	 * @param property {@link Property} object that has been added to a {@Cell}
	 */
	public String getDefaultValue(Property property) {
		return getDefaultValue(property.getKey());
	}
		
	/**
	 * Returns the possible values for the specified configurable property of
	 * the library cell (if they exist). If the property does not exist, 
	 * {@code null} is returned
	 * 
	 * @param propertyName Name of the property
	 */
	public String[] getPossibleValues(String propertyName) {
		LibraryCellProperty prop = configurableProperties.get(propertyName);
		return prop == null ? null : prop.getPossibleValues(); 
	}
	
	/**
	 * Returns the possible values for the specified configurable property of
	 * the library cell (if they exist) . If the property does not exist, 
	 * {@code null} is returned
	 * 
	 * @param property {@link Property} object that has been added to a {@Cell}
	 */
	public String[] getPossibleValues(Property property) {
		LibraryCellProperty prop = configurableProperties.get(property.getKey());
		return prop == null ? null : prop.getPossibleValues(); 
	}
	
	/**
	 * Returns {@code true} if the configuration property of the given name
	 * is read-only. If the configuration property is not read-only or the 
	 * property does not exist on the library cell, {@code false} will be
	 * returned.
	 * 
	 * TODO: move read-only attribute to the actual {@link Property} class?
	 * 
	 * @param propertyName Name of the property
	 */
	public boolean isReadonlyProperty(String propertyName) {
		LibraryCellProperty prop = configurableProperties.get(propertyName);
		return prop == null ? false : prop.isReadonly(); 
	}
	
	// Abstract functions
	/**
	 * Returns {@code true} if the cell is a library cell is a library macro, 
	 * {@code false} otherwise.
	 */
	abstract public boolean isMacro();
	/**
	 * Returns {@code true} if the cell is a VCC source, {@code false} otherwise.
	 */
	abstract public boolean isVccSource();
	/**
	 * Returns {@code true} if the cell is a GND source, {@code false} otherwise.
	 */
	abstract public boolean isGndSource();
	/**
	 * Returns {@code true} if the cell is a LUT cell (LUT1, LUT2, etc.), {@code false} otherwise.
	 */
	abstract public boolean isLut();
	/**
	 * Returns {@code true} if the cell represents a top-level port cell.
	 */
	abstract public boolean isPort();
	/**
	 * If the cell is a LUT cell, this returns the number of LUT inputs
	 * on the cell.
	 */
	abstract public Integer getNumLutInputs();
	
	/**
	 * Returns a List of {@link BelId} objects that represent where the
	 * current cell can be placed. Since macro cells cannot be placed
	 * directly in RapidSmith, null is returned if this function is used 
	 * for a library macro
	 */
	abstract public List<BelId> getPossibleAnchors();

	/**
	 * Returns a list of site properties that are shared across a {@link Bel} Type.
	 * For example, all Flip Flop Bels in a Site, must all be either rising edge or
	 * falling edge. This property would be returned in this list. For Macro cells,
	 * {@code null} is returned.
	 *  
	 * @param anchor {@link Bel}
	 */
	abstract public Map<String, SiteProperty> getSharedSiteProperties(BelId anchor);


		/**
	 	 * For macro cells, returns a list of required bel object that are
	 	 * needed to place the macro. This functionality is currently unimplemented
	 	 * and should not be used.
	 	 * @param anchor Anchor {@link Bel} for the macro
	 	 */
			abstract public List<Bel> getRequiredBels(Bel anchor);
		
}
