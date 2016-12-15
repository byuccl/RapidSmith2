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

package edu.byu.ece.rapidSmith.device;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.stream.Stream;

/**
 *  Class representing the Basic Elements of Logic.  BELs are the most basic
 *  logical blocks exposed by Xilinx.  Examples include LUTs, FFs and RAMB18s.
 *
 *  As BELs do not have unique names, they are identified by their unique
 *  (site, name) pair.  Due to the number of BELs in a device, BELs are created
 *  upon request using the {@link edu.byu.ece.rapidSmith.device.Site#getBel(java.lang.String)}
 *  method on the site of the desired BEL.
 */
public final class Bel implements Serializable {
	private static final long serialVersionUID = -4092803033961128002L;
	// The backing template for this BEL
	private BelTemplate template;
	// The site the BEL exists in
	private Site site;

	// BelPins all have to be uniquely created for each BEL.  While most BELs do
	// not have many pins, I don't want to be constantly recreating these pin
	// objects so we'll create a cache to store the pins.  Use a soft reference
	// to allow freeing the BelPins if they're not actively being used to reduce
	// the memory footprint.
	private transient SoftReference<Map<String, BelPin>> sources;
	private transient SoftReference<Map<String, BelPin>> sinks;

	/**
	 * Creates a new BEL in the given site backed by the given template.
	 *
	 * Use Site.getBel to create a new BEL
	 */
	Bel(Site site, BelTemplate template) {
		assert site != null;
		assert template != null;

		this.site = site;
		this.template = template;
	}

	/**
	 * Returns the site this BEL exists in.
	 *
	 * @return the site this BEL exists in
	 */
	public Site getSite() {
		return site;
	}

	/**
	 * Returns the id of this BEL (ex: SLICEL/AFF).
	 *
	 * @return the id of this BEL
	 * @see #getName()
	 * @see #getFullName()
	 */
	public BelId getId() {
		return template.getId();
	}

	/**
	 * Returns the name of the BEL (ex: AFF, F7MUX).
	 *
	 * @return the name of this BEL
	 * @see #getId()
	 * @see #getFullName()
	 */
	public String getName() {
		return template.getId().getName();
	}

	/**
	 * Returns the type of the BEL (ex: LUT6, LUTORMEM5, FF, SELMUX2_1).
	 * <p>
	 * The type is not a part of XDLRC.  It is usually obtained from PlanAhead and
	 * is used to help group similarly functioning BELs.
	 *
	 * @return the type of this BEL
	 */
	public String getType() {
		return template.getType();
	}

	/**
	 * Returns the BEL pin with the specified name.
	 * This method will look in both the sources and the sinks for this pin.
	 *
	 * @param pinName name of the BEL pin to return
	 * @return the BelPin with the given name or null if no pin with the specified
	 *   name exists on this BEL.
	 */
	public BelPin getBelPin(String pinName) {
		// Since we don't know whether the pin is a source or sink of this BEL, we
		// need to check both to locate it.

		// First check if it exists in the source pins cache to avoid duplicating it.
		Map<String, BelPin> sourcePins = (sources == null) ? null : sources.get();
		if (sourcePins != null) {
			BelPin pin = sourcePins.get(pinName);
			if (pin != null)
				return pin;
		}
		// Check if the pin is a source, and if so, create a new BelPin object
		// and return it.
		BelPinTemplate sourceTemplate = template.getSources().get(pinName);
		if (sourceTemplate != null)
			return new BelPin(this, sourceTemplate);

		// It's not a source pin, now do the same checks to see if it's a sink pin
		Map<String, BelPin> sinkPins = (sinks == null) ? null : sinks.get();
		if (sinkPins != null) {
			BelPin pin = sinkPins.get(pinName);
			if (pin != null)
				return pin;
		}
		BelPinTemplate sinkTemplate = template.getSinks().get(pinName);
		if (sinkTemplate != null)
			return new BelPin(this, sinkTemplate);

		// No pins of this name exist on this BEL.  Return null.
		return null;
	}

	/**
	 * Return the source pins of this BEL.
	 * <p>
	 * The source pin objects are created dynamically upon the first call of this
	 * method.  The pins are stored in a soft reference based cache.
	 *
	 * @return a collection containing the source pins of this BEL
	 */
	public Collection<BelPin> getSources() {
		// Check if the cache is valid, if not create it.
		Map<String, BelPin> sourcePins = (sources == null) ? null : sources.get();
		if (sourcePins == null) {
			sourcePins = buildSources();
		}
		return sourcePins.values();
	}

	private Map<String, BelPin> buildSources() {
		// Build the source pins and store the structure in a cache.
		Map<String, BelPin> sourcePins = new HashMap<>(template.getSources().size());
		for (BelPinTemplate belPinTemplate : template.getSources().values()) {
			sourcePins.put(belPinTemplate.getName(), new BelPin(this, belPinTemplate));
		}
		sources = new SoftReference<>(sourcePins);
		return sourcePins;
	}

	/**
	 * Return the sink pins of this BEL.
	 * <p>
	 * The sink pin objects are created dynamically upon the first call of this
	 * method.  The pins are stored in a soft reference based cache.
	 *
	 * @return a collection containing the sink pins of this BEL
	 */
	public Collection<BelPin> getSinks() {
		// Check if the cache is valid, if not create it.
		Map<String, BelPin> sinkPins = (sinks == null) ? null : sinks.get();
		if (sinkPins == null) {
			sinkPins = buildSinks();
		}
		return sinkPins.values();
	}

	public Stream<BelPin> getBelPins() {
		return Stream.concat(getSources().stream(), getSinks().stream());
	}

	private Map<String, BelPin> buildSinks() {
		// Build the sink pins and store the structure in the cache.
		Map<String, BelPin> sinkPins = new HashMap<>(template.getSinks().size());
		for (BelPinTemplate belPinTemplate : template.getSinks().values()) {
			sinkPins.put(belPinTemplate.getName(), new BelPin(this, belPinTemplate));
		}
		sinks = new SoftReference<>(sinkPins);
		return sinkPins;
	}

	/**
	 * Returns the wire the pin of the specified name connects to.
	 * <p>
	 * This method bypasses the creation of a BelPin object so it should be faster.
	 *
	 * @param pinName the name of the pin
	 * @return the wire the pin of the specified name connects to or null if no pin
	 *   of the name exists on the BEL
	 */
	public SiteWire getWireOfPin(String pinName) {
		// Check both the sources and sinks structures to find the pin.
		if (template.getSources().containsKey(pinName)) {
			int wireEnum = template.getSources().get(pinName).getWire();
			return new SiteWire(this.getSite(), wireEnum);
		}
		if (template.getSinks().containsKey(pinName)) {
			int wireEnum = template.getSinks().get(pinName).getWire();
			return new SiteWire(this.getSite(), wireEnum);
		}
		return null;
	}

	@Override
	public int hashCode() {
		return template.hashCode() * 31 + site.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final Bel other = (Bel) obj;
		return Objects.equals(this.template, other.template) &&
				Objects.equals(this.site, other.site);
	}

	/**
	 * Returns the full name of this BEL (ex: SLICE_X5Y9/AFF).
	 *
	 * @return the full name of this BEL
	 * @see #getName()
	 * @see #getId()
	 */
	public String getFullName() {
		return site.getName() + "/" + getName();
	}

	@Override
	public String toString() {
		return "Bel{" +
				site.getName() +
				"/" + getName() +
				"}";
	}

	public BelTemplate getTemplate() {
		return template;
	}
}
