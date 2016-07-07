/*
 * Copyright (c) 2010-2011 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.cad.placer.annealer;

import edu.byu.ece.rapidSmith.device.Device;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an alignment constraint on a placement group. This abstract class does not have any state.
 */
public abstract class PlacementAlignment<CSITE> {

	public abstract boolean isValidPlacement(CSITE site);

	public static PlacementAlignment getDefaultAlignment() {
		return new DefaultPlacementAlignment();
	}

	public Set<CSITE> determinePlaceableSites(Device device, Set<CSITE> potentialSites) {
		Set<CSITE> setSites = new HashSet<>();
		for (CSITE site : potentialSites)
			if (isValidPlacement(site))
				setSites.add(site);
		return setSites;
	}

	public Set<CSITE> determinePlaceableSites(Device device, CSITE[] potentialSites) {
		Set<CSITE> setSites = new HashSet<>();
		for (CSITE site : potentialSites)
			if (isValidPlacement(site))
				setSites.add(site);
		return setSites;
	}

}


/**
 * A simple alignment object that will align to any compatible site.
 */
class DefaultPlacementAlignment<CSITE> extends PlacementAlignment<CSITE> {

	public DefaultPlacementAlignment() {
	}

	public boolean isValidPlacement(CSITE site) {
		// TODO probably want to get this from the cluster templates
		return true;
	}
}

