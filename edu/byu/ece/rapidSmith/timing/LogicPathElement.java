/*
 * Copyright (c) 2010 Brigham Young University
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
package edu.byu.ece.rapidSmith.timing;

import java.io.Serializable;
import java.util.ArrayList;

import edu.byu.ece.rapidSmith.design.Instance;

public class LogicPathElement extends PathElement implements Serializable{

	private static final long serialVersionUID = -1302954929866402566L;

	/** The instance or physical resource */
	private Instance instance;
	/** The logical resources (FFs,...) part of this path element */
	private ArrayList<String> logicalResources = new ArrayList<String>();
	
	
	/**
	 * @return the instance
	 */
	public Instance getInstance() {
		return instance;
	}
	/**
	 * @param instance the instance to set
	 */
	public void setInstance(Instance instance) {
		this.instance = instance;
	}
	/**
	 * @return the logicalResources
	 */
	public ArrayList<String> getLogicalResources() {
		return logicalResources;
	}
	/**
	 * @param logicalResources the logicalResources to set
	 */
	public void setLogicalResources(ArrayList<String> logicalResources) {
		this.logicalResources = logicalResources;
	}
	/**
	 * Adds a logical resource name to the logic path element.
	 * @param resource The resource to add.
	 */
	public void addLogicalResource(String resource){
		logicalResources.add(resource);
	}
}
