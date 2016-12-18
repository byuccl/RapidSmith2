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
package edu.byu.ece.rapidSmith.timing;

import java.io.Serializable;

import edu.byu.ece.rapidSmith.design.xdl.XdlNet;

public class RoutingPathElement extends PathElement implements Serializable{
	
	private static final long serialVersionUID = -8322328106512170640L;
	
	/** The net or physical resource */
	private XdlNet net;

	/**
	 * @param net the net to set
	 */
	public void setNet(XdlNet net){
		this.net = net;
	}

	/**
	 * @return the net
	 */
	public XdlNet getNet(){
		return net;
	}
	
	
}
