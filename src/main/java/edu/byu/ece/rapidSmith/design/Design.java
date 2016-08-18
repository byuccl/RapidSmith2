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
package edu.byu.ece.rapidSmith.design;

import edu.byu.ece.rapidSmith.RapidSmithEnv;
import edu.byu.ece.rapidSmith.design.parser.DesignParser;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * The design class houses an entire XDL design or hard macro.  It keeps
 * track of all of its member instances, nets, modules and attributes
 * and can load/import save/export XDL files.  When an XDL design is loaded 
 * into this class it also populates the Device and WireEnumerator classes
 * that correspond to the part this design targets.
 * @author Chris Lavin
 * Created on: Jun 22, 2010
 */
public class Design extends AbstractDesign {

	public static final String DEFAULT_NCD_VERSION = "v3.2"; // current and final supported version of XDL

	private static final long serialVersionUID = 6586577338969915167L;
	/**  XDL is typically generated from NCD, this is the version of that NCD file (v3.2 is typical) */
	private String NCDVersion;
	/**  All of the attributes in the design */
	private ArrayList<Attribute> attributes;
	/** This is the list of modules or macros in the design */
	private HashMap<String,Module> modules;
	/** Keeps track of module instances and groups them according to module instance name */
	private HashMap<String,ModuleInstance> moduleInstances;
	/** This is a list of all the instances of primitives and macros in the design */
	private HashMap<String,Instance> instances;
	/** A map used to keep track of all used primitive sites used by the design */
	private HashMap<Site,Instance> usedPrimitiveSites;
	/** This is a list of all the nets in the design */
	private HashMap<String,Net> nets;
	/** A flag designating if this is a design or hard macro */
	private boolean isHardMacro;
	/**  This is the actual part database device for the design specified by partName */
	private transient Device dev;

	/** This is the special design name used by Xilinx to denote an XDL design as a hard macro */
	public static final String hardMacroDesignName = "__XILINX_NMC_MACRO";

	/**
	 * Constructor that creates a blank design.
	 * The design name and partName are set as null,  The NCD version is set to
	 * the default "3.2".  The design is set as not a hard macro.  All other
	 * structures are initialized as empty.
	 */
	public Design(){
		super();
		init();
	}

	/**
	 * Creates a new design and populates it with the given design name and
	 * part name.
	 * The device is loaded based upon the supplied part name.  The NCD version is
	 * set to the default "3.2".  The design is set as not a hard macro.  All other
	 * structures are initialized as empty.
	 *
	 * @param designName the name for created design
	 * @param partName the target part for the design
	 */
	public Design(String designName, String partName){
		super(designName, partName);
		init();
	}

	private void init() {
		modules = new HashMap<>();
		instances = new HashMap<>();
		usedPrimitiveSites = new HashMap<>();
		nets = new HashMap<>();
		attributes = new ArrayList<>();
		isHardMacro = false;
		moduleInstances = new HashMap<>();
		NCDVersion = Design.DEFAULT_NCD_VERSION;

	}

	/**
	 * @deprecated replaced by <code>Design(Path)</code>.
	 */
	@Deprecated
	public Design(String xdlFileName){
		this(Paths.get(xdlFileName));
	}

	/**
	 * Constructs a design loaded from the specified XDL file.
	 *
	 * @param xdlFilePath path to the XDL file
	 */
	public Design(Path xdlFilePath) {
		this();
		loadXDLFile(xdlFilePath);
	}

	/**
	 * Sets the part name and loads the device.
	 * The part name should include package and speed grade (ex. xc4vfx12ff668-10).
	 * @param partName name of the Xilinx FPGA part.
	 */
	@Override
	public void setPartName(String partName) {
		super.setPartName(partName);
		loadDevice();
	}

	/**
	 * Returns the device specific to this part.
	 * This should be the same device loaded with the XDL design file.
	 *
	 * @return the device specific to this design's part
	 */
	public Device getDevice() {
		return dev;
	}

	/**
	 * Returns the wire enumerator specific to this part.
	 * This should be the wire enumerator associated with the same device loaded
	 * with the XDL design file.
	 *
	 * @return the wire enumerator specific to this design's part
	 */
	public WireEnumerator getWireEnumerator() {
		return dev.getWireEnumerator();
	}

	/**
	 * Sets the device specific to this part.
	 * Generally only used by the parser when loading a design, but could be used
	 * to convert a design to a different part (among a host of other transformations).
	 *
	 * @param dev the device to set this design with
	 */
	public void setDevice(Device dev) {
		this.dev = dev;
	}

	private void loadDevice(){
		dev = RapidSmithEnv.getDefaultEnv().getDevice(partName);
	}

	/**
	 * @deprecated device is loaded by default
	 */
	@Deprecated
	public void loadDeviceAndWireEnumerator() {
		loadDevice();
	}

	/**
	 * Returns the NCD version present in the XDL design.
	 * The NCD version defaults to 3.2 unless overwritten.
	 * @return the NCD version string
	 */
	public String getNCDVersion() {
		return this.NCDVersion;
	}

	/**
	 * Sets the NCD version as shown in the XDL file.
	 *
	 * @param ver the new NCD version
	 */
	public void setNCDVersion(String ver) {
		this.NCDVersion = ver;
	}

	/**
	 * Returns the Collection containing the attributes of this design.
	 * @return the attributes of this design.
	 */
	public ArrayList<Attribute> getAttributes() {
		return attributes;
	}

	/**
	 * Sets the attributes for this design.
	 *
	 * @param attributes A collection containing the new attributes to associate
	 *   with this design.
	 */
	public void setAttributes(ArrayList<Attribute> attributes) {
		this.attributes = attributes;
	}

	/**
	 * Creates a new attribute with the given values and adds it to this design.
	 *
	 * @param physicalName physical name of the attribute
	 * @param logicalName logical name of the attribute - unused at the design level
	 * @param value value of the attribute
	 */
	public void addAttribute(String physicalName, String logicalName, String value) {
		attributes.add(new Attribute(physicalName, logicalName, value));
	}

	/**
	 * Creates a new attribute with the given values and an empty logical name and
	 * adds it to this design.
	 *
	 * @param physicalName physical name of the attribute
	 * @param value value of the attribute
	 */
	public void addAttribute(String physicalName, String value) {
		addAttribute(physicalName, "", value);
	}

	/**
	 * Add the attribute to the design.
	 *
	 * @param attribute the attribute to add
	 */
	public void addAttribute(Attribute attribute) {
		attributes.add(attribute);
	}

	/**
	 * Checks if the design has an attribute with the specified physical name.
	 *
	 * @param physicalName the physical name of the attribute to check for
	 * @return true if the design contains an attribute with the specified
	 *   physical name
	 */
	public boolean hasAttribute(String physicalName) {
		for (Attribute attr : attributes) {
			if (attr.getPhysicalName().equals(physicalName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines if this design is a hard macro.
	 * @return true if this design is a hard macro
	 */
	public boolean isHardMacro(){
		return this.isHardMacro;
	}

	/**
	 * Sets the design as a hard macro or not (a hard macro
	 * will have only one module as a member of the design).
	 * @param value true if it is a hard macro
	 */
	public void setIsHardMacro(boolean value){
		this.isHardMacro = value;
	}

	/**
	 * Returns the core module that corresponds to this class if it
	 * is a hard macro (a hard macro design will have only one module).
	 * @return the module of the hard macro, null if this is not a hard macro design
	 */
	public Module getHardMacro(){
		if(isHardMacro){
			return modules.values().iterator().next();
		}
		return null;
	}

	/**
	 * Returns the instance in this design with the specified name.
	 * @param name the name of the instance to get
	 * @return the instance with the specified name, or null if it does not exist
	 */
	public Instance getInstance(String name){
		return instances.get(name);
	}

	/**
	 * Returns the Collection of instances in the design.
	 * @return the instances of the design
	 */
	public Collection<Instance> getInstances(){
		return instances.values();
	}

	/**
	 * Returns the map of instance names to instances in this design.
	 * @return the map of instances in this design
	 */
	public HashMap<String,Instance> getInstanceMap(){
		return instances;
	}

	/**
	 * Adds an instance to this design.
	 * @param inst the instance to add
	 */
	public void addInstance(Instance inst){
		if(inst.isPlaced()){
			setPrimitiveSiteUsed(inst.getPrimitiveSite(), inst);
		}
		inst.setDesign(this);
		instances.put(inst.getName(), inst);
	}

	/**
	 * Carefully removes the instance with the specified name and its pins and
	 * possibly nets from this design.
	 * Nets are only removed if they are empty after removal of the instance's pins.
	 * This method CANNOT remove instances that are part of a ModuleInstance.
	 * @param name the instance name in the design to remove
	 * @return true if the operation was successful
	 */
	public boolean removeInstance(String name){
		return removeInstance(getInstance(name));
	}

	/**
	 * Carefully removes the specified instance with its pins and possibly nets
	 * from this design.
	 * Nets are only removed if they are empty after removal of the instance's pins.
	 * This method CANNOT remove instances that are part of a ModuleInstance.
	 * @param instance the instance in the design to remove
	 * @return true if the operation was successful
	 */
	public boolean removeInstance(Instance instance){
		if(instance.getModuleInstance() != null){
			return false;
		}
		for(Pin p : instance.getPins()){
			// TODO - We can sort through PIPs to only remove those that need
			// to be removed, we just need a method to do that
			if(p.getNet() != null){
				p.getNet().unroute();
				if(p.getNet().getPins().size() == 1){
					nets.remove(p.getNet().getName());
				}else{
					p.getNet().removePin(p);
				}
			}
		}
		instances.remove(instance.getName());
		releasePrimitiveSite(instance.getPrimitiveSite());
		instance.setDesign(null);
		instance.setNetList(null);
		return true;
	}

	/**
	 * Returns the net in this design with the specified name.
	 * @param name the name of the net to get
	 * @return the net with the specified name, or null if it does not exist
	 */
	public Net getNet(String name){
		return nets.get(name);
	}

	/**
	 * Returns the Collections of nets in this design.
	 * @return the nets in this design
	 */
	public Collection<Net> getNets(){
		return nets.values();
	}

	/**
	 * Returns the map of nets in this design.
	 * @return the nets of this design
	 */
	public HashMap<String,Net> getNetMap(){
		return nets;
	}

	/**
	 * Adds a net to this design.
	 * @param net the net to add
	 */
	public void addNet(Net net){
		nets.put(net.getName(), net);
	}

	/**
	 * Sets the nets of this design.
	 * @param netList the new list of nets for this design
	 */
	public void setNets(Collection<Net> netList){
		nets.clear();
		for(Net net : netList) {
			addNet(net);
		}
	}

	/**
	 * Removes the net with the specified name from this design.
	 * @param name the name of the net to remove
	 */
	public void removeNet(String name){
		Net n = getNet(name);
		if(n != null)
			removeNet(n);
	}

	/**
	 * Removes the net from this design
	 * @param net the net to remove from the design
	 */
	public void removeNet(Net net){
		for(Pin p : net.getPins()){
			p.getInstance().getNetList().remove(net);
			if(net.equals(p.getNet())){
				p.setNet(null);
			}
		}
		nets.remove(net.getName());
	}

	/**
	 * Returns the module in this design with the specified name.
	 * @param name the name of the module to get
	 * @return the module with the specified name, if it exists, null otherwise
	 */
	public Module getModule(String name){
		return modules.get(name);
	}

	/**
	 * Returns the Collection of modules in this design.
	 * @return the modules of this design
	 */
	public Collection<Module> getModules(){
		return modules.values();
	}

	/**
	 * Adds a module to this design.
	 * @param module the module to add
	 */
	public void addModule(Module module){
		modules.put(module.getName(), module);
	}

	/**
	 * Returns the moduleInstance called with the specified name.
	 * @param name the name of the moduleInstance to get
	 * @return the moduleInstance name, or null if it does not exist
	 */
	public ModuleInstance getModuleInstance(String name){
		return moduleInstances.get(name);
	}

	/**
	 * Returns the Collection of all of the module instances in this design.
	 * @return the module instances in this design
	 */
	public Collection<ModuleInstance> getModuleInstances(){
		return moduleInstances.values();
	}

	/**
	 * Returns the Map of all of the module instance members separated by
	 * module instance name.
	 * @return the Map containing all current module instances.
	 */
	public HashMap<String, ModuleInstance> getModuleInstanceMap(){
		return moduleInstances;
	}

	/**
	 * Creates, adds to design, and returns a new ModuleInstance called
	 * <code>name</code>and based on <code>module</code>.
	 * The module is also added to the design if not already present.
	 * @param name the name of the new ModuleInstance created
	 * @param module the module for the new ModuleInstance
	 * @return the new ModuleInstance
	 */
	public ModuleInstance createModuleInstance(String name, Module module){
		if(modules.get(module.getName()) == null)
			modules.put(module.getName(), module);

		ModuleInstance modInst = new ModuleInstance(name, this);
		moduleInstances.put(modInst.getName(), modInst);
		modInst.setModule(module);
		String prefix = modInst.getName()+"/";
		HashMap<Instance,Instance> inst2instMap = new HashMap<>();
		for(Instance templateInst : module.getInstances()){
			Instance inst = new Instance();
			inst.setName(prefix+templateInst.getName());
			inst.setModuleTemplate(module);
			inst.setModuleTemplateInstance(templateInst);
			for(Attribute attr : templateInst.getAttributes()){
				inst.addAttribute(attr);
			}
			inst.setBonded(templateInst.getBonded());
			inst.setType(templateInst.getType());

			this.addInstance(inst);
			inst.setModuleInstance(modInst);
			modInst.addInstance(inst);
			if(templateInst.equals(module.getAnchor())){
				modInst.setAnchor(inst);
			}
			inst2instMap.put(templateInst,inst);
		}

		HashMap<Pin,Port> pinToPortMap = new HashMap<>();
		for(Port port : module.getPorts()){
			pinToPortMap.put(port.getPin(),port);
		}

		for(Net templateNet : module.getNets()){
			Net net = new Net(prefix+templateNet.getName(), templateNet.getType());

			HashSet<Instance> instanceList = new HashSet<>();
			Port port = null;
			for(Pin templatePin : templateNet.getPins()){
				Port temp = pinToPortMap.get(templatePin);
				port = (temp != null)? temp : port;
				Instance inst = inst2instMap.get(templatePin.getInstance());
				if(inst == null)
					System.out.println("DEBUG: could not find Instance "+prefix+templatePin.getInstanceName());
				instanceList.add(inst);
				Pin pin = new Pin(templatePin.isOutPin(), templatePin.getName(), inst);
				net.addPin(pin);
			}

			if(port == null){
				modInst.addNet(net);
				net.addAttribute("_MACRO", "", modInst.getName());
				net.setModuleInstance(modInst);
				net.setModuleTemplate(module);
				net.setModuleTemplateNet(templateNet);
			}else{
				net.setName(prefix+port.getName());
			}
			this.addNet(net);
			if(templateNet.hasAttributes()){
				for(Attribute a: templateNet.getAttributes()){
					if(a.getPhysicalName().contains("BELSIG")){
						net.addAttribute(new Attribute(a.getPhysicalName(), a.getLogicalName().replace(a.getValue(), modInst.getName() + "/" + a.getValue()), modInst.getName() + "/" + a.getValue()));
					}else{
						net.addAttribute(a);
					}
				}
			}
			for(Instance inst : instanceList){
				inst.addToNetList(net);
			}
		}
		return modInst;
	}

	/**
	 * Adds the instance to the module instance with the specified name.
	 * The instance is updated to reflect membership in its new module instance.
	 * If no module instance of the specified name exists in this design, then a new
	 * module instance is created and added to the design.
	 *
	 * This function allows the separation of an instance based on its hard macro instance
	 * name.
	 * @param inst the instance to add
	 * @param moduleInstanceName the name of the module instance to add the instance to
	 * @return the module instance that the instance was added to
	 */
	public ModuleInstance addInstanceToModuleInstances(Instance inst, String moduleInstanceName){
		ModuleInstance mi = moduleInstances.get(moduleInstanceName);

		if(mi == null){
			mi = new ModuleInstance(moduleInstanceName, this);
			moduleInstances.put(moduleInstanceName, mi);
			mi.setModule(inst.getModuleTemplate());
		}
		mi.addInstance(inst);
		inst.setModuleInstance(mi);
		return mi;
	}

	/**
	 * Checks if the specified primitive site is used in this design.
	 * @param site the site to check for
	 * @return true if this design uses the specified primitive site
	 */
	public boolean isPrimitiveSiteUsed(Site site){
		return usedPrimitiveSites.containsKey(site);
	}

	/**
	 * Returns the instance which resides at the specified site.
	 * @param site the site of the desired instance
	 * @return the instance at the specified site, or null if the site is unoccupied
	 */
	public Instance getInstanceAtPrimitiveSite(Site site){
		return usedPrimitiveSites.get(site);
	}

	/**
	 * Returns the set of used primitive sites occupied by this
	 * design's instances and module instances.
	 * @return the set of used primitive sites in this design
	 */
	public Set<Site> getUsedPrimitiveSites(){
		return usedPrimitiveSites.keySet();
	}

	/**
	 * Marks a primitive site as used by a particular instance.
	 * @param site the site to be marked as used
	 * @param inst the instance using the site or null if the primitive site
	 * is null
	 * @return the instance previously at the specified site
	 */
	Instance setPrimitiveSiteUsed(Site site, Instance inst){
		if(site == null) return null;
		return usedPrimitiveSites.put(site, inst);
	}

	/**
	 * Removes the instance at the specified primitive site freeing up the site.
	 * @param site the site to free
	 * @return the instance previously at the specified site
	 */
	Instance releasePrimitiveSite(Site site){
		return usedPrimitiveSites.remove(site);
	}

	/**
	 * Clears out all the used sites in the design, use with caution.
	 */
	public void clearUsedPrimitiveSites(){
		usedPrimitiveSites.clear();
	}

	/**
	 * Unroutes the current design by removing all PIPs.
	 */
	public void unrouteDesign(){
		// Just remove all the PIPs
		for(Net net : nets.values()){
			net.getPIPs().clear();
		}
	}

	/**
	 * Removes hierarchy from the design by disconnecting all instances and nets
	 * from their module instances and removing the module instances.
	 * This method cannot be used on hard macros and will return without modifying
	 * this design if called.
	 */
	public void flattenDesign(){
		if(isHardMacro){
			MessageGenerator.briefError("ERROR: Cannot flatten a hard macro design");
			return;
		}
		for(ModuleInstance mi : moduleInstances.values()){
			for(Instance instance : mi.getInstances()){
				instance.detachFromModule();
			}
			for(Net net : mi.getNets()){
				net.detachFromModule();
			}
		}
		modules.clear();
	}

	/**
	 * @deprecated replaced with <code>loadXDLFile(Path)</code>
	 */
	public void loadXDLFile(String fileName){
		loadXDLFile(Paths.get(fileName));
	}

	/**
	 * Loads this instance of design with the XDL design found in
	 * the specified file.
	 * @param filePath the path to the XDL file to load
	 */
	public void loadXDLFile(Path filePath) {
		DesignParser parser = new DesignParser(filePath.toString());
		parser.setDesign(this);
		parser.parseXDL();
	}

	/**
	 * Saves the XDL design to a minimalist XDL file.  This is the same
	 * as saveXDLFile(fileName, false);
	 * @param fileName name of the file to save the design to
	 */
	public void saveXDLFile(String fileName){
		saveXDLFile(Paths.get(fileName), false);
	}

	/**
	 * Saves the XDL design to a minimalist XDL file.  This is the same
	 * as saveXDLFile(fileName, false);
	 * @param filePath path to the file to save the design to
	 */
	public void saveXDLFile(Path filePath) {
		saveXDLFile(filePath, false);
	}

	public float getMaxClkPeriodOfModuleInstances(){
		float maxModulePeriod = 0.0f;
		int missingClockRate = 0;
		for(ModuleInstance mi : getModuleInstances()){
			float currModuleClkPeriod = mi.getModule().getMinClkPeriod();
			if(currModuleClkPeriod != Float.MAX_VALUE){
				if(currModuleClkPeriod > maxModulePeriod)
					maxModulePeriod = currModuleClkPeriod;
			}
			else{
				missingClockRate++;
			}
		}
		return maxModulePeriod;
	}

	public String getMaxClkPeriodOfModuleInstancesReport(){
		String nl = System.getProperty("line.separator");
		float maxModulePeriod = 0.0f;
		int missingClockRate = 0;
		for(ModuleInstance mi : getModuleInstances()){
			float currModuleClkPeriod = mi.getModule().getMinClkPeriod();
			if(currModuleClkPeriod != Float.MAX_VALUE){
				if(currModuleClkPeriod > maxModulePeriod)
					maxModulePeriod = currModuleClkPeriod;
			}
			else{
				missingClockRate++;
			}
		}
		StringBuilder sb = new StringBuilder(nl + "Theoretical Min Clock Period: " +
				String.format("%6.3f", maxModulePeriod) +
				" ns (" + 1000.0f*(1/maxModulePeriod) +" MHz)" + nl);
		if(missingClockRate > 0){
			sb.append("  (Although, ").append(missingClockRate).append(" module instances did not have min clock period stored)").append(nl);
		}

		return sb.toString();
	}

	/**
	 * @deprecated replaced by <code>saveXDLFile(Path, boolean)</code>
	 */
	@Deprecated
	public void saveXDLFile(String fileName, boolean addComments) {
		saveXDLFile(Paths.get(fileName), addComments);
	}

	/**
	 * Saves the XDL design with PIPs and optional comments.
	 * @param filePath path to the file to save the design to
	 * @param addComments adds the same comments found in XDL designs created by the
	 * Xilinx xdl tool if true, otherwise no comments are added
	 */
	public void saveXDLFile(Path filePath, boolean addComments) {
		saveXDLFile(filePath, addComments, true);
	}

	/**
	 * Saves the XDL design with optional comments.
	 * @param filePath path to the file to save the design to
	 * @param addComments adds the same comments found in XDL designs created by the
	 * Xilinx xdl tool if true, otherwise no comments are added
	 * @param addPips true if PIPs should be written or false to ignore them
	 */
	public void saveXDLFile(Path filePath, boolean addComments, boolean addPips) {
		String nl = System.lineSeparator();
		BufferedWriter bw = null;
		try {
			bw = Files.newBufferedWriter(filePath, Charset.defaultCharset());
			XDLOutputter outputter = new XDLOutputter();
			outputter.output(this, bw);
		} catch (IOException e) {
			MessageGenerator.briefError("Error writing XDL file: " +
					filePath.toString() + File.separator + e.getMessage());
		} finally {
			try {
				if (bw != null)
					bw.close();
			} catch (IOException ignored) { }
		}
	}

	/**
	 * @deprecated replaced with <code>saveXDLFileWithoutPIPs(Path)</code>
	 */
	public void saveXDLFileWithoutPIPs(String fileName){
		saveXDLFileWithoutPIPs(Paths.get(fileName));
	}

	/**
	 * Saves the design to XDL without comments or PIPs.
	 * @param filePath the path to the file to write
	 */
	public void saveXDLFileWithoutPIPs(Path filePath) {
		saveXDLFile(filePath, false, false);
	}

	/**
	 *
	 * @deprecated replaced with <code>saveComparableXDLFile(Path)</code>
	 */
	@Deprecated
	public void saveComparableXDLFile(String fileName){
		saveComparableXDLFile(Paths.get(fileName));
	}

	/**
	 * Saves an XDL file in a manner that is easily comparable with another.
	 * Namely, all instances, nets, pins, etc are alphabetically ordered.  This
	 * method is equivalent to saveXDLFile(filePath, true)
	 * @param filePath the path to file to write to
	 */
	public void saveComparableXDLFile(Path filePath) {
		saveXDLFile(filePath, true);
	}
}
