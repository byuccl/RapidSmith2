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
package edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Contains a List of related configuration specification parts.
 *
 */
public abstract class PartLibrary {

	public PartLibrary() {
		_parts = new ArrayList<XilinxConfigurationSpecification>();
		addParts();
	}

	public XilinxConfigurationSpecification getPartFromDeviceName(String name) {		
		for (Iterator<XilinxConfigurationSpecification> i = _parts.iterator(); i.hasNext(); ) {
			XilinxConfigurationSpecification spec = i.next();
			if (spec.getDeviceName().equalsIgnoreCase(name))
				return spec;
		}
		return null;
	}

	public XilinxConfigurationSpecification getPartFromDeviceNameIgnoreCase(String name) {
		for (Iterator<XilinxConfigurationSpecification> i = _parts.iterator(); i.hasNext(); ) {
			XilinxConfigurationSpecification spec = i.next();
			if (spec.getDeviceName().equalsIgnoreCase(name))
				return spec;
		}
		return null;
	}

	// TODO: This method should take a large part name that includes the appended package
	// and find the correct part. This is mostly parsing of the name to separate the 
	// device name from the package name.
	public XilinxConfigurationSpecification getPartFromDevicePackageName(String name) {
		for (Iterator<XilinxConfigurationSpecification> i = _parts.iterator(); i.hasNext(); ) {
			XilinxConfigurationSpecification spec = i.next();
			if (spec.getDeviceName().equals(name))
				return spec;
		}
		return null;
	}

	public XilinxConfigurationSpecification getPartFromIDCode(String name) {		
		for (Iterator<XilinxConfigurationSpecification> i = _parts.iterator(); i.hasNext(); ) {
			XilinxConfigurationSpecification spec = i.next();
			if (spec.getStringDeviceIDCode().equalsIgnoreCase(name))
				return spec;
		}
		return null;
	}

	public List<XilinxConfigurationSpecification> getParts() {
		return new ArrayList<XilinxConfigurationSpecification>(_parts);
	}
	
	protected void addPart(XilinxConfigurationSpecification spec) {
		_parts.add(spec);
	}
	
	protected abstract void addParts();

	protected ArrayList<XilinxConfigurationSpecification> _parts;
	

}
