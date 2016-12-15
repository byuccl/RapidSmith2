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
package edu.byu.ece.rapidSmith.bitstreamTools.configuration;

/**
 * Represents a configuration frame within a Xilinx FPGA. This object has
 * a frame address, frame data, and a configuration tag indicating
 * whether the frame has been configured or not.
 *
 */
public class Frame {

	public Frame(int frameSize, int frameAddress) {
	    data = new FrameData(frameSize);
		this.frameAddress = frameAddress;
		configured = false;
	}

	public boolean isConfigured() {
		return configured;
	}
	
	public void configure(FrameData frameData) {
		data = new FrameData(frameData);
		configured = true;
	}
	
	public int getFrameAddress() {
		return frameAddress;
	}

	/**
	 * Reset the frame. This is the equivalent of hitting the PROG pin.
	 */
	public void reset() {
		data = null;
		configured = false;
	}
	
	public void clear() {
		if (configured)
			data.zeroData();
	}
	
	// TODO: should I return something that the user cannot change?
	// The user could mess around with the data by accessing
	// this object.
	public FrameData getData() {
		return data;
	}
	
	public void setData(FrameData data){
		this.data = data;
	}
	
	public String toString() {
		StringBuilder string = new StringBuilder();
		if (!configured) {
			return "Not Configured";
		}

		if (configured)
			string.append(data.toString());
		else
			string.append("\t<Not Configured>\n");
		string.append("\n");		
		return string.toString();		
	}
	
	
	// Remove?
	public String toXML()
	{
		StringBuilder string = new StringBuilder();
		string.append("<frame>");
		if (configured)
			string.append(data.toString());
		else
			string.append("\t<Not Configured>\n");
		string.append("\n</frame>");		
		return string.toString();
	}	

	protected boolean configured;
	protected int frameAddress;
	protected FrameData data;
	
}
