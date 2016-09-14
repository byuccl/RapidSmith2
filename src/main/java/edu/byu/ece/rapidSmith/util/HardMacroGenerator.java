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
package edu.byu.ece.rapidSmith.util;

import edu.byu.ece.rapidSmith.design.*;
import edu.byu.ece.rapidSmith.design.xdl.*;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLReader;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class reads an XDL design file and converts it to a hard macro.  It will
 * also create a VHDL wrapper to be able to instance the hard macro with the original
 * interface of the design.
 * @author Chris Lavin
 * Created on: Jan 27, 2010
 * Rewritten on: Jun 16 2010 by Jaren Lamprecht
 */

public class HardMacroGenerator {

	/** This variable keeps track of which instances should be removed from the original design */
	private ArrayList<Instance> instancesToRemove;
	/** This variable keeps track of which nets should be removed from the original design */
	private ArrayList<Net> netsToRemove;
	/** This variable keeps track of which instances should be added from the original design */
	private ArrayList<Instance> instancesToAdd;
	/** This variable keeps track of which nets should be added from the original design */
	private ArrayList<Net> netsToAdd;
	/** The XDL design that will be converted to a hard macro */
	private Design design;
	/** The hard macro that will be created, eventually will be added to design */
	private Module hardMacro;
	/** All of the InstanceTypes that are not allowed in hard macros  */
	private static HashSet<SiteType> forbiddenTypes;
	static{
		// Populate the forbidden types for error checking later
		forbiddenTypes = new HashSet<>();
		forbiddenTypes.add(SiteType.TIEOFF);
		forbiddenTypes.add(SiteType.IOB);
		forbiddenTypes.add(SiteType.IOBM);
		forbiddenTypes.add(SiteType.IOBS);
		forbiddenTypes.add(SiteType.PMV);
		forbiddenTypes.add(SiteType.DCM_ADV);
		forbiddenTypes.add(SiteType.ISERDES);
		forbiddenTypes.add(SiteType.OSERDES);
		forbiddenTypes.add(SiteType.ILOGIC);
		forbiddenTypes.add(SiteType.OLOGIC);
	}
	/** The output buffer for the VHDL wrapper to be created */
	private BufferedWriter vhd;
	/** The system's line terminator (\r\n:windows, \n:linux,...) */
	private String newLine;
	/** Stores the bus name found when isBusName() is called */
	private String _busName;
	/** Stores the bus bit number found when isBusName() is called */
	private int _busBitNumber;
	/** Optional file name for comparing VHDL top level names to original (for extra correctness)*/
	private String originalVHDLFileName;
	/**Graceful Failure reason*/
	private static String gracefulFailureReason;
	/**infrequent PIPs which constrain placement */
	private static HashSet<String> forbiddenPips;
	static {
		forbiddenPips = new HashSet<>();
		forbiddenPips.add("OUT_S");
	}
	/** directions for finding slices */
	enum direction{up, down, left, right}
		
	/**
	 * Constructor
	 * @param design The XDL_Design that should be converted to a hard macro
	 */
	public HardMacroGenerator(Design design){
		this.design = design;
		if(!(this.design.getExactFamilyName().contains("virtex4") || this.design.getExactFamilyName().contains("virtex5"))){
			MessageGenerator.briefErrorAndExit("HMG does not support " + design.getExactFamilyName());
		}

		// Get the System's line terminator string
		newLine = System.getProperty("line.separator");
		
		instancesToRemove = new ArrayList<>();
		netsToRemove = new ArrayList<>();
		instancesToAdd = new ArrayList<>();
		netsToAdd = new ArrayList<>();
		hardMacro = new Module();
	
	}
	
	
	
	/**
	 * The main function which will convert XDL_Design design to a hard macro.
	 * @param vhdName The output file name, used to generate the VHDL wrapper file name.
	 * @return The newly created hard macro.
	 */
	public Design convertToHardMacro(String vhdName){
		hardMacro.setName(design.getName() + "_HARD_MACRO");
		hardMacro.addAttribute(new Attribute("_SYSTEM_MACRO","","FALSE"));
		originalVHDLFileName = vhdName;
		
		// Initialize OutputBuffer for VHD file
		String fileNameVHD = design.getName()+ ".vhd";
		
		// Copy the original file to a _orig.vhd name so we can keep it / compare
		// before it gets overwritten
		if(originalVHDLFileName != null && originalVHDLFileName.equals(fileNameVHD)){
			FileTools.copyFile(originalVHDLFileName, design.getName() + "_orig" + ".vhd");
			originalVHDLFileName = design.getName() + "_orig" + ".vhd";
		}
		
		try {
			vhd = new BufferedWriter(new FileWriter(fileNameVHD));
		} catch (IOException e) {
			failAndExit("Problem creating file: " + fileNameVHD);
		}

		for(Instance inst : design.getInstances()){
			if(inst.getName().startsWith("IOBSLICE_")){
				inst.setType(SiteType.IOB);
			}
		}
		
		//check for unplaced instances
		for(Instance instance: design.getInstances()){
			if(!instance.isPlaced()){
				failAndExit("This design is not fully placed.  Did PAR fail?");
			}
		}
		
		// Replace ILOGIC/OLOGIC with SLICE Registers
		handleIOLOGIC();
		
		// Make the design a hard macro
		design.setIsHardMacro(true);
		design.setName(Design.hardMacroDesignName);
		
		//Handle Nets
		handleNets();
		
		//Handle ISERDES/OSERDES
		handleIOSERDES();
		
		// Remove Instances and Nets before handling tieoffs
		design.getInstances().removeAll(instancesToRemove);
		design.getNets().removeAll(netsToRemove);				
		instancesToRemove.clear();
		netsToRemove.clear();
		
		// Remove TIEOFFs or Slices posing as tieoffs
		handleTIEOFFS();

		//add instances and nets
		for(Instance i: instancesToAdd){
			design.addInstance(i);
		}
		for(Net n: netsToAdd){
			design.addNet(n);
		}
		
		// Remove Instances and Nets
		design.getInstances().removeAll(instancesToRemove);
		design.getNets().removeAll(netsToRemove);
		
		netsToRemove.clear();
		instancesToRemove.clear();

		//remove SHAPEs
		handleSHAPE();
		
		// Move hard macro into design file
		for(Instance i: design.getInstances()){
			hardMacro.addInstance(i);
		}
		for(Net n: design.getNets()){
			hardMacro.addNet(n);
		}

		//clear the design
		design.getInstances().clear();
		design.getNets().clear();
		design.getAttributes().clear();
		
		//add the macro to the cleared design
		design.addModule(hardMacro);
		if(hardMacro.getInstances().size() == 0){
			failAndExit("This hard macro does not contain any instances.");
		}
		hardMacro.setAnchor(hardMacro.getInstances().iterator().next());
		
		//check for hard macro errors
		if(design.getName().compareTo("GRACEFUL_FAILURE") == 0){
			return design;
		}
		
		hmErrorCheck();
		
 		// Create VHDL Wrapper before we return
		createVHDLWrapper();
 		
		return design;
	}
	
	/**
	 * This function removes the shape attributes from instances since adding several HMs
	 * to a design results with the duplicate shape names.
	 */
	public void handleSHAPE(){
		for(Instance i: design.getInstances()){
			ArrayList<Attribute> attributesToRemove = new ArrayList<>();
			for(Attribute a: i.getAttributes()){
				if(a.getValue().contains("XDL_SHAPE")){
					attributesToRemove.add(a);
				}
			}
			i.getAttributes().removeAll(attributesToRemove);
		}
	}
	
	/**
	 * This function is questionable in its necessity as ILOGIC and OLOGIC primitives could
	 * be part of a hard macro. We are assuming that they were not intentional.
	 */
	public void handleIOLOGIC(){
		for(Instance inst : design.getInstances()){
			if(inst.getType().equals(SiteType.ILOGIC)){
				boolean foundBadInstance = false;
				if(inst.getName().contains("XDL_DUMMY_IOI")){
					for(Attribute attr : inst.getAttributes()){
						if(attr.getPhysicalName().equals("_NO_USER_LOGIC")){
							foundBadInstance = true;
							instancesToRemove.add(inst);
							break;
						}
					}
				}
				if(!foundBadInstance){
					Instance newSLICE = createRegisterSlice(inst.getName());
					placeStaticSlice(newSLICE, false);
					newSLICE.setNetList(inst.getNetList());
					instancesToRemove.add(inst);
					instancesToAdd.add(newSLICE);
					for(Net net : inst.getNetList()){
						net.getPIPs().clear();
						for(Pin pin : net.getPins()){
							if(pin.getInstance().equals(inst)){
								pin.setInstance(newSLICE);
								String pinName = pin.getName();
								if(design.getExactFamilyName().contains("virtex4")){
									if(pinName.equals("CE1")) pin.setPinName("CE");
									if(pinName.equals("D")) pin.setPinName("BY");
									if(pinName.equals("Q1")) pin.setPinName("YQ");
								}else if(design.getExactFamilyName().contains("virtex5")){
									//TODO V5
									if(pinName.equals("CE1")) pin.setPinName("CE");
									if(pinName.equals("D")) pin.setPinName("DX");
									if(pinName.equals("Q1")) pin.setPinName("DQ");
								}
							}
						}
					}
				}
			}
			else if(inst.getType().equals(SiteType.OLOGIC)){
				boolean foundBadInstance = false;
				if(inst.getName().contains("XDL_DUMMY_IOI")){
					for(Attribute attr : inst.getAttributes()){
						if(attr.getPhysicalName().equals("_NO_USER_LOGIC")){
							foundBadInstance = true;
							instancesToRemove.add(inst);
							break;
						}
					}
				}
				if(!foundBadInstance){
					Instance newSLICE = createRegisterSlice(inst.getName());
					placeStaticSlice(newSLICE, false);
					newSLICE.setNetList(inst.getNetList());
					instancesToRemove.add(inst);
					instancesToAdd.add(newSLICE);
					for(Net net : inst.getNetList()){
						net.getPIPs().clear();
						for(Pin pin : net.getPins()){
							if(pin.getInstance().equals(inst)){
								pin.setInstance(newSLICE);
								String pinName = pin.getName();
								if(design.getExactFamilyName().contains("virtex4")){
									if(pinName.equals("OCE")) pin.setPinName("CE");
									if(pinName.equals("D1")) pin.setPinName("BY");
									if(pinName.equals("OQ")) pin.setPinName("YQ");
								}else if(design.getExactFamilyName().contains("virtex5")){
									//TODO V5
									if(pinName.equals("OCE")) pin.setPinName("CE");
									if(pinName.equals("D1")) pin.setPinName("DX");
									if(pinName.equals("OQ")) pin.setPinName("DQ");
								}
							}
						}	
					}
				}
			}
		}

		design.getInstances().removeAll(instancesToRemove);
		instancesToRemove.clear();
		
		for(Instance i: instancesToAdd){
			design.addInstance(i);
		}
		//design.getInstances().addAll(instancesToAdd);
		instancesToAdd.clear();
	}
	
	/**
	 * This is a helper method to convertToHardMacro which iterates through the nets
	 * to turn the design into a hard macro
	 */
	private void handleNets(){
		// Iterate through all nets to turn design into hard macro
		Pin pin = null;
		for(Net net: design.getNets()){
			ArrayList<Pin> pins = null;
			
			//check for forbidden pips and unroute
			boolean clearPips = false;
			for(PIP p : net.getPIPs()){
				if(forbiddenPips.contains(p.getStartWireName())
						||forbiddenPips.contains(p.getEndWireName())){
					clearPips = true;
					break;
				}
			}
			if(clearPips){
				net.getPIPs().clear();
			}
			
			if(net.hasAttributes()){
				netsToRemove.add(net);
			}
			else if((pins = isNetConnectedToIOB(net)).size() > 0){
				handleIOBs(net, pins);
			}
			// Handle BUFG
			else if((pin = isNetOutputofBUFG(net)) != null){
				
				//check to see if the BUFG has an IOBSource before removing
				boolean IOBSource = false;
				for(Net n : pin.getInstance().getNetList()){
					if(n.getSource().getInstance().getType().equals(SiteType.IOB)){
						IOBSource = true;
						break;
					}
				}
				
				if(IOBSource){
					
					// Remove BUFG and source pin
					instancesToRemove.add(pin.getInstance());
					net.getPins().remove(pin);
					net.getPIPs().clear();
					
					// Arbitrarily choose an inpin as the new port
					Pin port = net.getPins().get(0);
					String clkname = pin.getInstance().getName();
					String[] parts = clkname.split("_");
					clkname = "";
					for(String p : parts){
						if(p.contains("IBUF") || p.contains("BUFG")){
							break;
						}
						else{
							clkname += p + "_";
						}
					}
					if(clkname.endsWith("_")){
						clkname = clkname.substring(0, clkname.length()-1);
					}
					
					Port newPort = new Port(clkname +"_inport",port);
					hardMacro.getPorts().add(newPort);
					port.setPort(newPort);
					
				}

			}
			// Rip out DCMs/PMV
			else if((pin = isNetConnectedToDCMOrPMV(net)) != null){
				for(Pin p : net.getPins()){
					instancesToRemove.add(p.getInstance());
				}
				netsToRemove.add(net);
			}
			
		}
	}
	
	/**
	 * This is a helper method to converToHardMacro which removes the
	 * ISERDES and OSERDES
	 */
	private void handleIOSERDES(){
		// Remove ISERDES/OSERDES
		for(Instance inst : design.getInstances()){
			if(inst.getType().equals(SiteType.ISERDES) ||
			   inst.getType().equals(SiteType.OSERDES)){
				instancesToRemove.add(inst);
			}
			boolean foundBadInstance = false;
			if(inst.getName().contains("XDL_DUMMY_CLB")){
				for(Attribute attr : inst.getAttributes()){
					if(attr.getPhysicalName().equals("_NO_USER_LOGIC")){
						foundBadInstance = true;
						break;
					}
				}
				if(!foundBadInstance){
					inst.getAttributes().clear();
					if(design.getExactFamilyName().contains("virtex4")){
						inst.getAttributes().add(new Attribute("G","","#LUT:D=0"));
						inst.getAttributes().add(new Attribute("YUSED","","0"));
					}else if(design.getExactFamilyName().contains("virtex5")){
						//TODO V5
						inst.getAttributes().add(new Attribute("D6LUT","","#LUT:O6=0"));
						inst.getAttributes().add(new Attribute("DUSED","","0"));
					}
				}
			}
		}
	}
	
	/**
	 * This is a helper method to convertToHardMacro which replaces the TIEOFFS or
	 * their equivalent if mapped to a slice
	 */
	private void handleTIEOFFS(){
		int diff = 10000;
		int uniqueGL = 100000;
		for(Net net : design.getNets()){
			if(net.isStaticNet()){
				if(net.getSource().getInstance().getType().equals(SiteType.TIEOFF) || net.getSource().getInstance().getName().contains("XDL_DUMMY_CLB")){
					instancesToRemove.add(net.getSource().getInstance());
					netsToRemove.add(net);

					int num = 0;

					for(Pin p : net.getPins()){
						if(!p.isOutPin()){
							Port newPort = null;
							if(net.getName().startsWith("GLOBAL_LOGIC")){
								newPort = new Port(net.getName()+"_inport" + num,p);
								hardMacro.getPorts().add(newPort);
							}
							else{
								String type = "0";
								if(net.getType() == NetType.VCC){
									type = "1";
								}
								newPort = new Port("GLOBAL_LOGIC"+type+"_"+(uniqueGL++)+"_inport" + num,p);
								hardMacro.getPorts().add(newPort);
							}
							
							// Create a new net for each pin created
							Net newNet = new Net();
							if(net.getName().charAt(12) == '0'){
								newNet.setName("GLOBAL_LOGIC0_" + diff++);
							}
							else{
								newNet.setName("GLOBAL_LOGIC1_" + diff++);
							}
							newNet.getPins().add(p);
							netsToAdd.add(newNet);
							p.setPort(newPort);
						}
						num++;
					}
				}
			}
			// Remove all gnd/vcc nets
			if(net.isStaticNet()){
				if(net.getType()== NetType.VCC)
					net.setName(net.getName() + "_VCC");
				else
					net.setName(net.getName() + "_GND");
				net.setType(NetType.WIRE);
			}
		}
	}

	
	/**
	 * Checks the hard macro for errors such as unsupported instances, duplicates, etc.
	 */
	private void hmErrorCheck(){
		//----------------------------------------------------------------------//
		// ERROR CHECKING
		//----------------------------------------------------------------------//
		HashSet<String> instanceNames = new HashSet<>();
		HashSet<String> instanceLocations = new HashSet<>();
		HashSet<String> netNames = new HashSet<>();
		HashSet<String> portNames = new HashSet<>();
 		for(Instance inst: hardMacro.getInstances()){
			// Check for illegal instances in hard macro
			if(forbiddenTypes.contains(inst.getType())){
				if(inst.getType().equals(SiteType.DCM_ADV) && inst.getName().startsWith("XIL_ML_UNUSED_DCM")){
					System.out.println("Error: " + inst.getType().toString() + ", " + inst.getName() +
					" found in hard macro.  This is a bug that needs to be fixed.");
				}
				else if(!inst.getType().equals(SiteType.DCM_ADV)){
					System.out.println("Error: " + inst.getType().toString() + ", " + inst.getName() +
					" found in hard macro.  This is a bug that needs to be fixed.");
				}
			}
			if(inst.getName().contains("XDL_DUMMY_CLB")){
				for(Attribute attr : inst.getAttributes()){
					if(attr.getPhysicalName().equals("_NO_USER_LOGIC")){
						System.out.println("ERROR: Instance is statically configured, this will not work" +
								" as a hard macro: " + inst.getName());
						break;
					}
				}
			}
			// Check for unique instance names and locations
			if(!instanceNames.contains(inst.getName())){
				instanceNames.add(inst.getName());
			}
			else{
				System.out.println("ERROR: Duplicate instance name: " + inst.getName());
			}
			
			// Check for unique instance placements
			if(!instanceLocations.contains(inst.getPrimitiveSiteName())){
				instanceLocations.add(inst.getPrimitiveSiteName());
			}
			else{
				System.out.println("ERROR: Two instances placed at same location: " + inst.getName() + " " + inst.getPrimitiveSiteName());
			}
		}
 		for(Port port : hardMacro.getPorts()){
 			// Check for unique port names
			if(!portNames.contains(port.getName())){
				portNames.add(port.getName());
			}
			else{
				System.out.println("ERROR: Duplicate port name: " + port.getName());
			}
 		}
 		for(Net net : hardMacro.getNets()){
 			// Check for illegal net types
 			if(net.isStaticNet()){
 				System.out.println("ERROR: Net type is illegal for hard macro: " + net.getName() 
 						+" of type: " + net.getType());
 			}
 			// Check for unique port names
			if(!netNames.contains(net.getName())){
				netNames.add(net.getName());
			}
			else{
				System.out.println("ERROR: Duplicate net name: " + net.getName());
			}
 		}
	}
	
	/**
	 * A helper method which checks if the net is driven by a BUFG.
	 * @param net The net to test.
	 * @return The pin that is the driving the net if driven by a BUFG, null otherwise. 
	 */
	private Pin isNetOutputofBUFG(Net net) {
		if(net.getSource() != null){
			return net.getSource().getInstance().getType().equals(SiteType.BUFG) ? net.getSource() : null;
		}else{
			return null;
		}
	}
	
	/**
	 * A helper method to check if a net connects to the un-needed DCM/PMV stuff.
	 * @param net The net to check.
	 * @return The pin connected to a DCM or PMV.
	 */
	private Pin isNetConnectedToDCMOrPMV(Net net) {
		for(Pin pin : net.getPins()){
			if((pin.getInstance().getType().equals(SiteType.DCM_ADV) && pin.getInstanceName().startsWith("XIL_ML_UNUSED_DCM"))||
					pin.getInstance().getType().equals(SiteType.PMV)){
				return pin;
			}
		}
		return null;
	}
	
	/**
	 * Creates a register slice
	 * @param name
	 * @return the newly created slice instance
	 */
	private Instance createRegisterSlice(String name){
		Instance inst = new Instance();
		inst.setName(name);
		inst.setType(SiteType.SLICEL);
		if(design.getExactFamilyName().contains("virtex4")){
			inst.getAttributes().add(new Attribute("DYMUX","","BY"));
			inst.getAttributes().add(new Attribute("FFY","","#FF"));
			inst.getAttributes().add(new Attribute("FFY_INIT_ATTR","","INIT0"));
			inst.getAttributes().add(new Attribute("FFY_SR_ATTR","","SRLOW"));
			inst.getAttributes().add(new Attribute("SYNC_ATTR","","SYNC"));
		}else if(design.getExactFamilyName().contains("virtex5")){
			//TODO V5
			//might need?: inst.getAttributes().add(new Attribute("CEUSED","","0"));
			inst.getAttributes().add(new Attribute("DFFMUX","","DX"));
			inst.getAttributes().add(new Attribute("DFF","","#FF"));
			inst.getAttributes().add(new Attribute("DFFINIT","","INIT0"));
			inst.getAttributes().add(new Attribute("DFFSR","","SRLOW"));
			inst.getAttributes().add(new Attribute("SYNC_ATTR","","SYNC"));
		}
		return inst;
	}
	
	/**
	 * Returns a list of pins that are connected to IOBs in the current net.
	 * @param net The net to test for IOB connections.
	 * @return A list of pins connected to IOBs, the list will be empty if the net does not connect to IOBs.
	 */
	private ArrayList<Pin> isNetConnectedToIOB(Net net){
		ArrayList<Pin> list = new ArrayList<>();
		for(Pin pin : net.getPins()){
			if(isPinConnectedToIOB(pin)){
				list.add(pin);
			}
		}
		return list;
	}
	
	/**
	 * A helper function to isNetConnectedToIOB which tests if a pin is connected to an IOB.
	 * @param pin The pin to test.
	 * @return True if the pin is connected to an IOB, false otherwise.
	 */
	private boolean isPinConnectedToIOB(Pin pin){
		if(pin.getInstance().getType().equals(SiteType.IOB)||
			pin.getInstance().getType().equals(SiteType.IOBM)||
			pin.getInstance().getType().equals(SiteType.IOBS)){
			return true;
		}
		return false;
	}
	
	/**
	 * A helper method to convertToHardMacro() which handles the cases of the nets being 
	 * connected to IOBs.
	 * @param net The current net to convert.
	 * @param pins The pins that are connected to IOBs.
	 */
	private void handleIOBs(Net net, ArrayList<Pin> pins) {
		// Add code to detect feedback on external nets 
		// where the output is connected to IOBs, Xilinx has a bug in XDL conversion
		
		// If this is a net with more than one IOB, we currently only 
		// have designs that are statically source'd that do this
		if(pins.size() > 1){ // CASE 5:
			if(!net.isStaticNet()){
				// Check for pass through nets, an IOB that drive IOBs
				boolean inputFound = false;
				boolean outputFound = false;
				for(Pin p : pins){
					if(!p.isOutPin()){
						inputFound = true;
					}
					else if(p.isOutPin()){
						outputFound = true;
					}
				}
				if(inputFound && outputFound){
					handlePassThruIOBs(net,pins);
					return;
				}
				
				String outputNames = "MULTI_OUTPUT_PORT";
				
				
				for(Pin pin : pins){
					// Remove the IOB instance
					if(isTriState(pin.getInstance())){
						failGracefully("This design contains tri-state IO, which is not yet supported by the HMG.");
					}
					instancesToRemove.add(pin.getInstance());
					outputNames += "-" + pin.getInstanceName();
					net.getPins().remove(pin);
				}	
				net.getPIPs().clear();
				Port newPort = new Port(outputNames+"_outport",net.getSource());
				hardMacro.getPorts().add(newPort);
				net.getSource().setPort(newPort);
			}
			else{
				// Remove the net, we'll create new nets for each IOB 
				netsToRemove.add(net);
				
				// Remove the TIEOFF, Xilinx will choke if we don't
				if(net.getSource() == null){
					failAndExit("This net does not have a source: " + net);
				}
				if(!net.getSource().getInstance().getType().equals(SiteType.TIEOFF)){
					failAndExit("1. This case is unexpected. Talk to Chris about getting it implemented.");
				}
				
				instancesToRemove.add(net.getSource().getInstance());
				
				for(Pin pin : pins){
					// Remove the IOB instance
					if(isTriState(pin.getInstance())){
						failGracefully("This design contains tri-state IO, which is not yet supported by the HMG.");
					}
					instancesToRemove.add(pin.getInstance());
					
					// Create New Net and static SLICE source with Pin connected to static SLICE
					Net newNet = new Net(pin.getInstance().getName() +"_"+ net.getType(), NetType.WIRE);
					Instance inst = createStaticSliceSource(net.getType());
					placeStaticSlice(inst, true);
					Pin newPin = null;
					if(design.getExactFamilyName().contains("virtex4")){
						 newPin = new Pin(true,"Y",inst);
					}else if(design.getExactFamilyName().contains("virtex5")){
						//TODO V5
						newPin = new Pin(true,"D",inst);
					}
					newNet.replaceSource(newPin);
					netsToAdd.add(newNet);
					instancesToAdd.add(inst);
					Port newPort = new Port(pin.getInstance().getName()+"_outport",newPin);
					hardMacro.getPorts().add(newPort);
					newPin.setPort(newPort);
				}
			}
		}
		else{ // This is just a single IOB
			// There should only be 1 pin 
			Pin pin = pins.get(0);
			// Remove IOB
			if(isTriState(pin.getInstance())){
				failGracefully("Sorry, this design contains tri-state IO.  Tri-state IO is not yet supported by the HMG.");
			}
			instancesToRemove.add(pin.getInstance());
			net.getPins().remove(pin);
			net.getPIPs().clear();
			
			if(pin.isOutPin()){
				// Check if this net drives the BUFG, if so, we don't want to create any ports
				// We'll do that with the output of the BUFG
				for(Pin p : net.getPins()){
					if(p.getInstance().getType().equals(SiteType.BUFG)){
						netsToRemove.add(net);
						return;
					}
				}
				// CASE 1:
				// CASE 2:
				// Now create a port, we just choose an arbitrary one if there are more than one
				Pin p = net.getPins().get(0);
				//XDL_Pin p = pins.get(0); -- This is wrong here because we already removed this pin
				Port newPort = new Port(pin.getInstance().getName()+"_inport",p);
				hardMacro.addPort(newPort);
				p.setPort(newPort);
			}
			else{
				// Check if this is a static net
				if(net.isStaticNet()){
					if(!net.getSource().getInstance().getType().equals(SiteType.TIEOFF)){
						failAndExit("2. This case is unexpected. Talk to Chris about getting it implemented.");
					}
					
					instancesToRemove.add(net.getSource().getInstance());
					netsToRemove.add(net);
					
					// Create New Net and static SLICE source with Pin connected to static SLICE
					Net newNet = new Net(pin.getInstance().getName() + "_" + net.getType(),NetType.WIRE);
					Instance inst = createStaticSliceSource(net.getType());
					placeStaticSlice(inst, true);
					Pin newPin = null;
					if(design.getExactFamilyName().contains("virtex4")){
						 newPin = new Pin(true,"Y",inst);
					}else if(design.getExactFamilyName().contains("virtex5")){
						//TODO V5
						newPin = new Pin(true,"D",inst);
					}
					newNet.getPins().add(newPin);
					newNet.replaceSource(newPin);
					netsToAdd.add(newNet);
					instancesToAdd.add(inst);
					Port newPort = new Port(pin.getInstance().getName()+"_outport",newPin);
					hardMacro.getPorts().add(newPort);
					newPin.setPort(newPort);
				}
				else{
					
					// CASE 3:
					// CASE 4:
					Pin p = net.getSource();
					Port newPort = new Port(pin.getInstance().getName()+"_outport",p);
					hardMacro.getPorts().add(newPort);
					p.setPort(newPort);
				}
			}
		}
	}
	
	/**
	 * Inserts a LUT to isolate input and output IOBs.  This is a restriction of hard macros. 
	 * @param net The current net to convert.
	 * @param pins The pins that are connected to IOBs.
	 */
	private void handlePassThruIOBs(Net net, ArrayList<Pin> pins) {
		System.out.println("WARNING: Design contains wires only connected to IOBs, net: " + net.getName());
		System.out.println("         This net has been divided by a LUT. Performance of signal will go down.");
		net.getPIPs().clear();
		Pin inPin = null;
		
		for(Pin pin : pins){
			// For each output IOB, create a new LUT and net
			if(!pin.isOutPin()){
				Net newOutputNet = new Net();
				Instance newLUT = createPassThruSlice();
				placeStaticSlice(newLUT, true);
				instancesToAdd.add(newLUT);
				newOutputNet.setName(net.getName() + "_OUTPUT");
				Pin newLUTPin = null;
				if(design.getExactFamilyName().contains("virtex4")){
					 newLUTPin = new Pin(true,"Y",newLUT);
				}else if(design.getExactFamilyName().contains("virtex5")){
					//TODO V5
					newLUTPin = new Pin(true,"D",newLUT);
				}
				newOutputNet.getPins().add(newLUTPin);
				Pin newPin = null;
				if(design.getExactFamilyName().contains("virtex4")){
					 newPin = new Pin(false,"G1",newLUT);
				}else if(design.getExactFamilyName().contains("virtex5")){
					//TODO V5
					newPin = new Pin(false,"D1",newLUT);
				}
				net.getPins().add(newPin);
				Port newPort = new Port(pin.getInstance().getName()+"_outport",
						newLUTPin);
				hardMacro.getPorts().add(newPort);
				newLUTPin.setPort(newPort);
			}
			else{
				inPin = pin;
			}
			
			// Remove the IOB instance
			instancesToRemove.add(pin.getInstance());
			net.getPins().remove(pin);
		}	
		if(net.getPins().size() < 1){
			failAndExit("ERROR: Check net: " + net.getName());
		}
		boolean containsBUFG = false;
		Pin bufgPin = null;
		for(Pin p : net.getPins()){
			if(p.getInstance().getType().equals(SiteType.BUFG)){
				containsBUFG = true;
				bufgPin = p;
			}
		}
		if(!containsBUFG){
			Port newPort = new Port(inPin.getInstance().getName()+"_inport",
					net.getPins().get(0));
			hardMacro.getPorts().add(newPort);
			net.getPins().get(0).setPort(newPort);
				
		}
		else{
			net.getPins().remove(bufgPin);
		}
		
		return;		
	}
	
	/**
	 * This function will look at already taken SLICE locations and try to place the slice
	 * in a vacant location next to one of the already occupied slices.  This is only for 
	 * static slices.
	 * @param slice The slice to be placed.
	 */
	private void placeStaticSlice(Instance slice, boolean updateName){
		int xAvg = 0;
		int yAvg = 0;
		int avg = 0;
		HashSet<Site> sliceLocations = new HashSet<>();
		// Find all used SLICEs in the current design 
		for(Instance inst : design.getInstances()){
			if(inst.getType().equals(SiteType.SLICEL) ||
					inst.getType().equals(SiteType.SLICEM)){
				sliceLocations.add(inst.getPrimitiveSite());
				if(!inst.getName().contains("slice")){
					xAvg += inst.getInstanceX();
					yAvg += inst.getInstanceY();
					avg++;
				}
			}
		}
		// Also includes SLICEs that will be added to the design
		for(Instance inst : instancesToAdd){
			if(inst.getType().equals(SiteType.SLICEL) ||
					inst.getType().equals(SiteType.SLICEM)){
				sliceLocations.add(inst.getPrimitiveSite());
				if(!inst.getName().contains("slice")){
					xAvg += inst.getInstanceX();
					yAvg += inst.getInstanceY();
					avg++;
				}
			}
		}
		
		//using the average in this way here is a quick and dirty way to exclude PORT_SLICE_HARD_MACROs
		//from being included in the averaging so that the new slices are centered correctly
		
		if(avg == 0){
			failAndExit("ERROR: This design contains static outputs (vcc/gnd) that could not " +
			"be supplied by a SLICE.  Does this design have any SLICEs in it?");
		}
		
		xAvg = xAvg / avg;
		yAvg = yAvg / avg;
		
		int x = xAvg;
		int y = yAvg;
		int maxX = xAvg+1;
		int maxY = yAvg+1;
		int minX = xAvg-1;
		int minY = yAvg;
		direction dir = direction.down;
		
		Site s = null;
		
		while(true){
			if((s = design.getDevice().getPrimitiveSites().get("SLICE_X" + Integer.toString(x) + "Y" + Integer.toString(y)))!=null){				
				if(!sliceLocations.contains(s)){
						slice.place(s);
						if(updateName){
							slice.setName("RS_DUMMY_"  + s);
						}
						return;
				}
			}
			if(dir == direction.down){
				if(y == minY){
					dir = direction.left;
					minY--;
					x--;
				}else{
					y--;
				}
			}else if(dir == direction.left){
				if(x == minX){
					dir = direction.up;
					minX--;
					y++;
				}else{
					x--;
				}			
			}else if(dir == direction.up){
				if(y == maxY){
					dir = direction.right;
					maxY++;
					x++;
				}else{
					y++;
				}	
			}else if(dir == direction.right){
				if(x == maxX){
					dir = direction.down;
					maxX++;
					y--;
				}else{
					x++;
				}
			}
		}

	}
	
	/**
	 * This function will create an new SLICEL that will drive a static 0 or 1 indicated by
	 * the type
	 * @param netType Valid types are "gnd" and "vcc"
	 * @return The newly create instance
	 */
	private Instance createStaticSliceSource(NetType netType){
		Instance inst = new Instance();
		inst.setName("RS_DUMMY_CLB");
		inst.setType(SiteType.SLICEL);
		if(design.getExactFamilyName().contains("virtex4")){
			inst.getAttributes().add(new Attribute("G","","#LUT:D=" + (netType.equals(NetType.GND) ? "0" :"1")));
			inst.getAttributes().add(new Attribute("YUSED","","0"));
		}else if(design.getExactFamilyName().contains("virtex5")){
			//TODO V5
			inst.getAttributes().add(new Attribute("DUSED","","0"));
			inst.getAttributes().add(new Attribute("D6LUT","","#LUT:O6=" + (netType.equals(NetType.GND) ? "0" :"1")));
		}
		return inst;
	}
	
	/**
	 * Creates a slice for a signal to pass through
	 * @return the slice
	 */
	private Instance createPassThruSlice(){
		Instance inst = new Instance();
		inst.setName("XDL_LUT");
		inst.setType(SiteType.SLICEL);
		if(design.getExactFamilyName().contains("virtex4")){
			inst.getAttributes().add(new Attribute("YUSED","","0"));
			inst.getAttributes().add(new Attribute("G","","#LUT:D=A1"));			
		}else if(design.getExactFamilyName().contains("virtex5")){
			//TODO V5
			inst.getAttributes().add(new Attribute("DUSED","","0"));
			inst.getAttributes().add(new Attribute("D6LUT","","#LUT:O6=A1"));
		}
		return inst;
	}
	
	/**
	 * Returns true if the given IOB is a tri-state buffer
	 * @param instance
	 * @return True if tri-state
	 */
	public boolean isTriState(Instance instance){
		for(Attribute cfg:instance.getAttributes()){
			if(cfg.getPhysicalName().compareTo("TUSED") == 0){
				if(cfg.getValue().compareTo("#OFF") != 0){
					return true;
				}else{
					break;
				}
			}
		}
		return false;
	}
	
	/**
	 * This function will create the VHDL wrapper based on the hard macro created and the 
	 * different special cases that have changed the interface because of Xilinx hard macro restrictions.
	 */
	private void createVHDLWrapper(){
		String designName = hardMacro.getName();
		HashMap<String,Integer> busNames = new HashMap<>();
		HashMap<String,String> busDirections = new HashMap<>();
		ArrayList<String> signalNames = new ArrayList<>();
		ArrayList<String> multiOutputPortAssignments = new ArrayList<>();
		int multiCount = 0;
		
		HashMap<String,String[]> signalMap = null;
		
		for(Port port : hardMacro.getPorts()){
			String portName = port.getName();
			String busName;
			Integer bitNumber = -1;
			if(portName.contains("MULTI_OUTPUT_PORT")){
				String[] parts = portName.split("-");
				for(String p : parts){
					if(p.contains("<")){
						busName = p.substring(0, p.indexOf("<"));
						bitNumber = Integer.parseInt(p.substring(p.indexOf("<")+1,p.indexOf(">")));
						if(busNames.get(busName) != null){
							if(busNames.get(busName) < bitNumber){
								busNames.put(busName,bitNumber);
								busDirections.put(busName,"out");
							}
						}
						else{
							busNames.put(busName,bitNumber);
							busDirections.put(busName,"out");
						}
					}else if(!p.contains("MULTI_OUTPUT_PORT")){
						signalNames.add(p.replace("_outport", "") + "\t: out std_logic");
					}
				}
			}
			else if(portName.contains("<")){
				busName = portName.substring(0, portName.indexOf("<"));
				bitNumber = Integer.parseInt(portName.substring(portName.indexOf("<")+1,portName.indexOf(">")));
				if(busNames.get(busName) != null){
					if(busNames.get(busName) < bitNumber){
						busNames.put(busName,bitNumber);
						busDirections.put(busName,portName.contains("_inport") ? "in" : "out");
					}
				}
				else{
					busNames.put(busName,bitNumber);
					busDirections.put(busName,portName.contains("_inport") ? "in" : "out");
				}
			}
			else if(!portName.contains("GLOBAL_LOGIC")){
				if(portName.endsWith("_inport")){
					//signalNames.add(portName.replace("_inport", ""));
					signalNames.add(portName.replace("_inport", "") + "\t: in std_logic");
				}
				else{
					//signalNames.add(portName.replace("_outport", ""));
					signalNames.add(portName.replace("_outport", "") + "\t: out std_logic");
				}
				
			}
		}
		
		for(Port port : hardMacro.getPorts()){
			String portName = port.getName();
			
			// Replace invalid VHDL identifier characters
			portName = portName.replaceAll("\\.","_");
			portName = portName.replaceAll("/","_");
			portName = portName.replaceAll("<","_");
			portName = portName.replaceAll(">","_");
			portName = portName.replaceAll("\\(", "_");
			portName = portName.replaceAll("\\)", "_");
			
			// Store up MultiOutput port names
			if(portName.startsWith("MULTI_OUTPUT_PORT")){
				String[] parts = portName.split("-");
				for(String part : parts){
					String assignment;
					if(isBusName(part, busNames)){
						assignment = _busName + "(" + _busBitNumber +
						") <= multipleOutput_" + multiCount +";" + newLine;
						assignment = assignment.replaceAll("_{2,}","_");
						multiOutputPortAssignments.add(assignment);
					}
					else if (!part.startsWith("MULTI_OUTPUT_PORT")){
						
						if(part.endsWith("_outport")) part = part.replace("_outport", "");
						if(part.endsWith("_inport")) part = part.replace("_inport", "");
						assignment = part + " <= multipleOutput_" +
						multiCount +";" + newLine;
						assignment = assignment.replaceAll("_{2,}","_");
						multiOutputPortAssignments.add(assignment);
					}
					
				}
				multiCount++;
			}
			portName = portName.replaceAll("-","_");
			portName = portName.replaceAll("_{2,}","_");
			port.setName(portName);
		}
		
		// Check against original VHDL file (if given)
		
		if(originalVHDLFileName != null){
			signalMap = new HashMap<>();
			try {
				BufferedReader reader = new BufferedReader(new FileReader(originalVHDLFileName));
				String line = null;
				boolean startParsing = false;
				boolean stopParsing = false;
				
				while((line = reader.readLine()) != null){
					if(startParsing && !stopParsing){
						Pattern pattern = Pattern.compile("\\s*([^:\\s]*)\\s*:\\s*([^\\s]*)\\s+([^;]*)[;\\s]*");					
						Matcher m = pattern.matcher(line);
						while(m.find()){
							if(m.group(2).toLowerCase().equals("in") || m.group(2).toLowerCase().equals("out") || m.group(2).toLowerCase().equals("inout")){
								String[] tmp = new String[3];
								tmp[0] = m.group(1); // signal_name								
								tmp[1] = m.group(2).toLowerCase(); // in/out/inout
								tmp[2] = m.group(3); // std_logic/std_logic_vector
								signalMap.put(tmp[0], tmp);
							}
						}
					}
					if(line.toLowerCase().contains("entity ")){
						startParsing = true;
						stopParsing = false;
					}
					if(line.toLowerCase().contains("end ")){
						stopParsing = true;
						
					}
				}
				
			} catch (FileNotFoundException e1) {
				failAndExit("Could not find VHDL file: " + originalVHDLFileName);
			} catch (IOException e) {
				failAndExit("Problem reading VHDL file: " + originalVHDLFileName);
			}
			
			// Do a little error checking.  Make sure the signal names extracted from XDL
			// match those from the supplied VHDL file
			String[] match = null;
			for(String s : signalNames){
				String[] parts = s.replace(':',' ').split("\\s+");	
				match = signalMap.get(parts[0]);
				if(match != null){
					if(!match[1].equals(parts[1])){
						failAndExit("ERROR: Signal " + match[0] + " has wrong direction.");
					}
				}
				else{
					failAndExit("ERROR: Supplied VHDL file "+originalVHDLFileName+" does not have top level signal: " + parts[0]);
				}
			}
		}
		
		try {
			vhd.write(newLine);
			vhd.write("-------------------------------------------------------------------" + newLine);
			vhd.write("-- edu.byu.ece.rapidSmith.xdlUtilities.HardMacroGenerator" + newLine);
			vhd.write("-- time: " + FileTools.getTimeString() + newLine);
			vhd.write("-- " + newLine);
			vhd.write("-- This is the VHDL wrapper to correctly wrap the hard macro as " + newLine);
			vhd.write("-- it should be interfaced with its initial interface. " + newLine);
			vhd.write("--  - Chris Lavin 1/26/2010 " + newLine);
			vhd.write("-------------------------------------------------------------------" + newLine);
			vhd.write(newLine);
			vhd.write("-------------------------------------------------------------------" + newLine);
			vhd.write("-- LIBRARIES " + newLine);
			vhd.write("-------------------------------------------------------------------" + newLine);
			vhd.write("library ieee;" + newLine);
			vhd.write("use ieee.std_logic_1164.all;" + newLine);
			vhd.write("use ieee.std_logic_unsigned.all;" + newLine);
			vhd.write(newLine);
			vhd.write("-------------------------------------------------------------------" + newLine);
			vhd.write("-- ENTITY " + newLine);
			vhd.write("-------------------------------------------------------------------" + newLine);
			vhd.write("entity " + designName.replace("_HARD_MACRO", "") +" is" + newLine);
			vhd.write("  port (" + newLine);
			if(signalMap != null){ // Use names from user supplied VHDL file
				boolean firstTime = true;
				for(String[] signal : signalMap.values()){
					if(firstTime){
						firstTime = false;
					}
					else{
						vhd.write(";" + newLine);
					}
					String tmp = signal[2].toLowerCase();
					if(tmp.contains("(")){
						tmp = tmp.replace("unsigned", "std_logic_vector");
						tmp = tmp.replace("signed", "std_logic_vector");
					}
					else{
						tmp = tmp.replace("unsigned", "std_logic");
						tmp = tmp.replace("signed", "std_logic");
					}
					vhd.write("    " + signal[0] + " : " + signal[1] + " " + tmp);
				}
				vhd.write(newLine);
			}
			else{ // Use the names extracted from XDL and hope for the best
				for(String sig : signalNames){
					vhd.write("    " + sig);
					if(busNames.keySet().size() == 0){
						if(signalNames.get(signalNames.size()-1).equals(sig)){
							vhd.write(newLine);
						}
						else{
							vhd.write(";" + newLine);
						}	
					}
					else{
						vhd.write(";" + newLine);
					}
				}
				ArrayList<String> busNamesItr = new ArrayList<>();
				busNamesItr.addAll(busNames.keySet());
				for(String sig : busNamesItr){
					int max = busNames.get(sig);
					vhd.write("    " + sig + "\t: "+ busDirections.get(sig)
							+" std_logic_vector("+max+" downto 0)");
					if(busNamesItr.get(busNamesItr.size()-1).equals(sig)){
						vhd.write(newLine);
					}
					else{
						vhd.write(";" + newLine);
					}					
				}
			}
			vhd.write("  );" + newLine);
			vhd.write("end "+ designName.replace("_HARD_MACRO", "") + ";" + newLine);
			vhd.write("-------------------------------------------------------------------" + newLine);
			vhd.write("-- ARCHITECTURE" + newLine);
			vhd.write("-------------------------------------------------------------------" + newLine);
			vhd.write("architecture behavioral of " + designName.replace("_HARD_MACRO", "") + " is" + newLine + newLine); // Add entity name
			vhd.write("component " + hardMacro.getName() + newLine);
			vhd.write("port(" + newLine);
			int portCounter = hardMacro.getPorts().size();
			for(Port port : hardMacro.getPorts()){
				
				vhd.write("  " + port.getName() + " :\t" +
						(port.getName().contains("_inport") ? "in" : "out") +
						" std_logic");
				
				portCounter--;
				if(portCounter == 0){
					vhd.write(newLine);
				}
				else{
					vhd.write(";" + newLine);
				}					
			}
			vhd.write(");" + newLine);
			vhd.write("end component;" + newLine + newLine);
			
			multiCount = 0;
			for(Port port : hardMacro.getPorts()){
				if(port.getName().startsWith("MULTI_OUTPUT_PORT")){
					vhd.write("signal multipleOutput_" + multiCount + " : std_logic;" + newLine);
					multiCount++;
				}
			}
			
			// Add all the signals we need
			vhd.write("signal ground : std_logic := '0';" + newLine);
			vhd.write("signal power : std_logic := '1';" + newLine + newLine);

			vhd.write("-------------------------------------------------------------------" + newLine);
			vhd.write("-- BEGIN" + newLine);
			vhd.write("-------------------------------------------------------------------" + newLine);
			vhd.write("begin" + newLine);
			if(multiOutputPortAssignments.size() > 0){
				vhd.write(newLine);
				vhd.write("-- Multiple Output Signal Assignments" + newLine);
				for(String s : multiOutputPortAssignments){
					vhd.write(s);
				}
				
				vhd.write(newLine);
			}
			
			vhd.write("instance0 : " + hardMacro.getName() + newLine);
			vhd.write("  port map(" + newLine);
			multiCount = 0;
			portCounter = hardMacro.getPorts().size();
			for(Port port : hardMacro.getPorts()){
				
				vhd.write("  " + port.getName() + " => ");
				if(port.getName().startsWith("GLOBAL_LOGIC0")){
					vhd.write("ground");
				}
				else if(port.getName().startsWith("GLOBAL_LOGIC1")){
					vhd.write("power");
				}
				else if(isBusName(port.getName(),busNames)){
					vhd.write(_busName + "(" + _busBitNumber + ")");
				}
				else if(port.getName().startsWith("MULTI_OUTPUT_PORT")){
					vhd.write("multipleOutput_"+multiCount);
					multiCount++;
				}
				else{
					vhd.write(port.getName().substring(0, port.getName().lastIndexOf("_")));
				}
				
				portCounter--;
				if(portCounter == 0){
					vhd.write(newLine);
				}
				else{
					vhd.write("," + newLine);
				}					
			}	
			vhd.write("  );" + newLine);
			vhd.write("end behavioral;" + newLine);
			vhd.write("-------------------------------------------------------------------" + newLine);
			vhd.write("-- END OF FILE" + newLine);
			vhd.write("-------------------------------------------------------------------" + newLine);
			vhd.write(newLine);
			vhd.close();
		} catch (IOException e) {
			failAndExit("Prolem writing vhd file.");
		}
		
	}
	
	/**
	 * A helper function that can take in a string and determine if it is a member of a bus.
	 * If it is a bus it will populate the variables _busName and _busBitNumber based in the name.
	 * Otherwise it sets those variables to null and -1 respectively.
	 * @param name Name of the string to check for a bus name
	 * @param map The map of all the bus names already found.
	 * @return True if name is a bus name, false otherwise.
	 */
	private boolean isBusName(String name, HashMap<String,Integer> map){
		String part;
		int ndx = 0;
		int startIndex;
		while(ndx < name.length()){
			startIndex = name.indexOf("_", ndx);
			if(startIndex < 0){
				_busName = null;
				_busBitNumber = -1;
				return false;
			}
			part = name.substring(0, startIndex);
			if(map.get(part) != null){
				_busName = part;
				try{
					_busBitNumber = Integer.parseInt(name.substring(part.length()+1, name.indexOf("_", part.length()+1)));
				}
				catch(NumberFormatException e){
					ndx = part.length() + 1;
					continue;
				}
				if(_busBitNumber > map.get(_busName)) {
					map.put(_busName, _busBitNumber);
				}
				return true;
			}
			else{
				ndx = part.length() + 1;
			}
		}
		
		_busName = null;
		_busBitNumber = -1;		
		return false;
	}
	
	/**
	 * A simple error function to reduce code size.
	 * @param s The error string
	 */
	public static void failAndExit(String s){
		System.out.println(s);
		System.exit(1);
	}
	
	/**
	 * For designs with detected unsupported structures
	 * @param s error message
	 */
	public void failGracefully(String s){
		gracefulFailureReason = s;
		design.setName("GRACEFUL_FAILURE");
	}
	
	public static void main(String[] args) throws IOException {
		if(args.length < 2 || args.length > 3){
			System.out.println("USAGE: <input.xdl|input.ncd> <output file type: xdl|nmc> [optional: original_vhdl_top.vhd]");
			System.exit(0);
		}		
		Design input;
		Design output; 
		String originalVHDLFileName = args.length==3 ? args[2] : null;
		// If we are supplied an NCD, convert it to XDL
		Path inputFile = Paths.get(args[0]);
		if(Objects.equals(FileTools.getFileExtension(inputFile), "ncd")) {
			FileConverter.convertNCD2XDL(inputFile);
			if (!(Files.exists(inputFile))) {
				HardMacroGenerator.failAndExit("XDL Generation failed, check your NCD file for correctness.");
			}
			input = new XDLReader().readDesign(FileTools.replaceFileExtension(inputFile, "xdl"));
		}
		else{
			input = new XDLReader().readDesign(inputFile);
		}
		
		HardMacroGenerator hmTool = new HardMacroGenerator(input);
		output = hmTool.convertToHardMacro(originalVHDLFileName);
		
		//detect graceful failure
		if(output.getName().compareTo("GRACEFUL_FAILURE") == 0){
			failAndExit(gracefulFailureReason);
		}
		
		// Output NMC if desired
		Path xdlFile = Paths.get(hmTool.hardMacro.getName() + ".xdl");
		XDLWriter writer = new XDLWriter();
		if (args[1].toLowerCase().endsWith("nmc")) {
			writer.writeXDL(output, xdlFile);
			FileConverter.convertXDL2NMC(xdlFile);
			if (!(Files.exists(xdlFile))) {
				HardMacroGenerator.failAndExit("NMC Generation failed, re-run by hand to get error message.");
			}
		} else {
			writer.writeXDL(output, xdlFile);
		}
	}
}
