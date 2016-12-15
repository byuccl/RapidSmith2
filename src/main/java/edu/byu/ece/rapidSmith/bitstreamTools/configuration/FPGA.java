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

import java.util.*;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Packet;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketList;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketOpcode;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketType;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.RegisterType;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.BlockType;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

/**
 * This class models the configuration of a Xilinx FPGA.
 * This class contains the configuration data of a configured FPGA
 * (organized as Frames) and a frame address register. This
 * object is "configured" by using Bitstream objects (pre-parsed
 * Xilinx bitstreams). This class is used for creating, manipulating,
 * and querying bitstreams in Xilinx FPGAs.  
 *
 */
public class FPGA {

	/**
	 * Construct an empty unconfigured FPGA based on the FPGA specification.
	 */
	public FPGA(XilinxConfigurationSpecification spec) {
		this.spec = spec;
		frameBuffer = new FrameData(spec);
		frameAddress = new FrameAddressRegister(spec);
		//configData = new FrameData[frameAddress.getNumberOfFrames()];
		configData = new Frame[FrameAddressRegister.getNumberOfFrames(spec)];
		int frameSize = spec.getFrameSize();
		frameMap = new HashMap<>();
		frameAddress.setFAR(0);
		for (int i = 0; i < configData.length; i++) {
			int currentFAR = frameAddress.getAddress();
			configData[i] = new Frame(frameSize,currentFAR);
			frameMap.put(currentFAR, i);
			frameAddress.incrementFAR();
		    //configData[i] = new FrameData(frameSize);
		}
		frameAddress.setFAR(0);
		init();
	}

	/**
	 * Construct empty FPGA with debug flag (instance specific debug)
	 */
	public FPGA(XilinxConfigurationSpecification spec, boolean debug) {
		this(spec);
		this.DEBUG = debug;
	}
	
	/**
	 * Configure the FPGA object with the data in the bitstream packets.
	 * Iterate over each packet and individually configure them.
	 */
	public void configureBitstream(Bitstream bitstream) {

		// TODO: Check to see if bitstream matches part?
		// TODO: make sure that the bitstream ends on the proper boundry?
		// TODO: keep track of where the packet data ends up on the FPGA? (i.e. a map
		//       between array data and FAR addresses).

		// Iterate through all of the packets of the bitstream 
		PacketList packets = bitstream.getPackets();
		for (Iterator<Packet> i = packets.iterator(); i.hasNext(); ) {
			configureBitstream(i.next());
		}
	}

	/**
	 * Perform 'configuration' operations on an individual packet. This method
	 * will respond to a subset of packets that affect the configuration
	 * process. Many packets will be ignored. The following packets are processed:
	 * 
	 * FAR: update the internal FAR address
	 * 
	 * @param packet
	 */
	protected void configureBitstream(Packet packet) {
		
		// Find all the write packets. Only Writes impact the configuration
		if (packet.getOpcode() == PacketOpcode.WRITE) {
			
			// Access the data associated with the register write
			List<Integer> data = packet.getData();

			// Look for specific registers
			RegisterType rT = packet.getRegType();
		
			// Update the the FAR within FPGA
			if (rT == RegisterType.FAR) {	
				if (data.size() == 0) {
					System.err.println("Warning: FAR write with no data");
				} else {
					int farVal = data.get(0);
					setFAR(farVal);
				}
			}			
			// Write data written to FDRI in the FPGA for packet type 2 FDRI writes
			else if(rT == RegisterType.FDRI || packet.getPacketType() == PacketType.TWO) {
				FDRICommand(packet);
			}
			// Performs a multiple frame write operation
			else if(rT == RegisterType.MFWR){
				MFWRCommand();
			}			
		}
	}

	/**
	 * Execute a FDRI command by configuring the FPGA with the data in the packet.
	 * Note that the FAR must be set before executing this command.
	 * 
	 * @param packet
	 */
	protected void FDRICommand(Packet packet) {
		List<Integer> data = packet.getData();
		configureWithData(data);
	}
	
	protected void configureWithData(List<Integer> data) {
		// This method will be called when a FDRI packet with no data is found.
		// Such a call should be ignored - the following packet should be a
		// type 2 data packet.
		if (data.size() == 0)
			return;
		
		int frameSize = getDeviceSpecification().getFrameSize();
		// TODO: is this necessary?
		clearFDRIFrameBuffer();
		// Load the frame buffer and then set the frame
		for(int i = 0; i < data.size(); i+=frameSize) {
			for(int j = 0; (j < frameSize) && (j + i < data.size()); j++) {
				setFDRIBuffer(j, data.get(i+j));
			}
			writeFDRIToCurrentFrame();
			incrementFAR();
		}
		
	}
	
	/**
	 * Allows for multiple frame writes to different addresses.
	 * This function works similar to SetFrame, although frameBuffer is never changed or written to.  As 
	 * with SetFrame, if the write is successful, the FAR is incremented.
	 */
	public void MFWRCommand(){
		writeFDRIToCurrentFrame();
		incrementFAR();
	}
	
	/**
	 * Set the address of the FAR.
	 */
	public void setFAR(int far) {
		if (DEBUG) System.out.println("Setting FAR to 0x"+Integer.toHexString(far));
		frameAddress.setFAR(far);
	}
	public FrameAddressRegister getFAR() {
		return frameAddress;
	}

	public boolean incrementFAR() { 
		return frameAddress.incrementFAR(); 
	}
	
	public XilinxConfigurationSpecification getDeviceSpecification() {
		return spec;
	}
	public void clearFDRIFrameBuffer() {
		frameBuffer.zeroData();
	}
	public boolean setFDRIBuffer(int index, int value) {
		return frameBuffer.setData(index, value);
	}

	/**
	 * Writes the value of the FDRI buffer into the frame at the current FAR.
	 */
	public void writeFDRIToCurrentFrame() {
		Frame currentFrame = getCurrentFrame();
		if (currentFrame == null) {
			//System.out.println("Warning: configuring an invalid frame");
			// it is possible that the current frame is invalid (i.e. a bogus
			// frame at the end of the bitstream). It is not clear what is supposed to happen
			// but this implementation will ignore it.
			return;
		}
		currentFrame.configure(frameBuffer);
		if (DEBUG) System.out.println("Configuring frame "+
				(new FrameAddressRegister(this.spec,currentFrame.frameAddress)));
	}

	/**
	 * Obtain the frame pointed to by the current FAR address. If the current
	 * FAR is invalid, return null.
	 */
	public Frame getCurrentFrame() {
		if (!frameAddress.validFARAddress()) {
			if (DEBUG) System.out.println("Frame Address is invalid:" + 
				frameAddress);
			return null;
		}
		int index = frameAddress.getConsecutiveAddress();
		if (index >= configData.length)
			return null;
		return configData[index];
	}

	/**
	 * Return the frame specified by the farAddress parameter.
	 * This method will have to
	 * perform a frame address to sequential address translation.
	 */
	public Frame getFrame(int farAddress) {
		//int index = FrameAddressRegister.getConsecutiveAddress(spec,farAddress);
		int index = frameMap.get(farAddress);
		if (index >= configData.length)
			return null;
		return configData[index];
	}
	
	/**
	 * Returns the frame from the address set in the FrameAddressRegister.
	 * @param far The FAR containing the address of the frame to get.
	 * @return The frame object with the requested far, or null if the
	 * far is invalid.
	 */
	public Frame getFrame(FrameAddressRegister far) {
		return getFrame(far.getAddress());
	}
	
	public ArrayList<Frame> getAllFrames() {
		ArrayList<Frame> frames = new ArrayList<>(configData.length);
		Collections.addAll(frames, configData);
		return frames;
	}

	public ArrayList<Frame> getConfiguredFrames() {
		ArrayList<Frame> configuredFrames = new ArrayList<>();
		for (int i = 0; i < configData.length; i++) 
			if (configData[i].isConfigured())
				configuredFrames.add(configData[i]);
		return configuredFrames;
	}
	
	public List<Frame> getConsecutiveFrames(int farAddress, int numFrames) {
		int c_far = FrameAddressRegister.getConsecutiveAddress(spec, farAddress);
		List<Frame> frames = new ArrayList<>(numFrames);
		for (int i = 0; i < numFrames; i++)
			frames.add(configData[c_far+i]);
		return frames;
	}
	
	public List<Frame> getConfigurationBlockFrames(int topBottom, BlockType blockType, int row, int column) {
		int blockNum = FrameAddressRegister.getBlockTypeNumber(spec, blockType);
		int startingFAR = FrameAddressRegister.createFAR(spec, topBottom, blockNum, row, column, 0);
		int numFrames = FrameAddressRegister.getFramesPerConfigurationBlock(spec, blockNum, column);
		return getConsecutiveFrames(startingFAR, numFrames);
	}
	
	/**
	 * Return the contents of a set of continuous frames as a String.
	 */
	public String getFrameContents(int startFrame, int numberOfFrames) {
		StringBuilder sb = new StringBuilder();
		XilinxConfigurationSpecification partInfo = getDeviceSpecification();
		FrameAddressRegister far = new FrameAddressRegister(partInfo, startFrame);

		// Iterate over the number of requested frames but stop if we 
		// get to an invalid frame address
		for (int i = startFrame; i < (startFrame + numberOfFrames) && far.validFARAddress(); i++) {
			Frame f = getFrame(far);
			if (f != null) {
				if (f.isConfigured()) {
					FrameData data = f.getData();

					sb.append(far+"\n");
					sb.append(data);
				} else {
					sb.append(far + " Not configured\n");
				}
			}
			far.incrementFAR();
		}
		return sb.toString();
	}
	
	/**
	 * Return the contents of a set of continuous frames as a String.
	 */
	public String getFrameContents() {
		return getFrameContents(0,FrameAddressRegister.getNumberOfFrames(getDeviceSpecification()));
	}

	/**
	 * Compares every frame of this FPGA with the FPGA passed in as a parameter and 
	 * returns a list of those frames that differ in contents. This method will return a null
	 * if the parameter FPGA is a different device than this FPGA.
	 * 
	 * @param ignoreUnconfiguredFrames If true, this method will only compare frames
	 * that are configured on both FPGAs.
	 */
	public ArrayList<Integer> getDifferingFrames(FPGA fpga, boolean ignoreUnconfiguredFrames) {
		XilinxConfigurationSpecification spec = fpga.getDeviceSpecification();
		if (this.spec != spec) {
			return null;
		}

		ArrayList<Integer> dFrames = new ArrayList<>();
		FrameAddressRegister far = new FrameAddressRegister(spec);
		
		for (; far.validFARAddress(); far.incrementFAR()) {
			Frame f1 = getFrame(far);
			Frame f2 = fpga.getFrame(far);

			// Check #1: see if frames are configured or not
			if ( (!f1.isConfigured() && f2.isConfigured()) ||
				 (f1.isConfigured() && !f2.isConfigured())) {
				if (!ignoreUnconfiguredFrames)
					dFrames.add(far.getAddress());
			} else {
			
				// both frames configured
				FrameData d1 = f1.getData();
				FrameData d2 = f2.getData();
				if (!d1.isEqual(d2)) {	
					dFrames.add(far.getAddress());
				}
			}
		}
		return dFrames;
	}
	
	
	public String getFrameData() {
		return spec.getDeviceName() + "\n";
	}
	
	protected void init() {
		setFAR(0);
	}
	
	protected boolean DEBUG = false;
	
	protected FrameData frameBuffer;
	//protected final FrameData[] configData;
	protected final Frame[] configData;
	protected FrameAddressRegister frameAddress;
	protected XilinxConfigurationSpecification spec;
	private HashMap<Integer, Integer> frameMap;
	
}
