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
package edu.byu.ece.rapidSmith.design.parser;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import edu.byu.ece.rapidSmith.design.Attribute;
import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.design.Instance;
import edu.byu.ece.rapidSmith.design.Module;
import edu.byu.ece.rapidSmith.design.ModuleInstance;
import edu.byu.ece.rapidSmith.design.Net;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.design.Pin;
import edu.byu.ece.rapidSmith.design.PinType;
import edu.byu.ece.rapidSmith.design.Port;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.util.MessageGenerator;
import edu.byu.ece.rapidSmith.util.StringPool;

public class DesignParser{
	public static String CFG = "cfg";
	public static String VCC = "vcc";
	public static String VDD = "vdd";
	public static String GND = "gnd";
	public static String PIP = "pip";
	public static String NET = "net";
	public static String LOAD = "load";
	public static String WIRE = "wire";
	public static String PORT = "port";
	public static String INST = "inst";
	public static String INPIN = "inpin";
	public static String INOUT = "inout";
	public static String POWER = "power";
	public static String PLACED = "placed";
	public static String BONDED = "bonded";
	public static String GROUND = "ground";
	public static String MODULE = "module";
	public static String ENDMODULE = "endmodule";
	public static String DESIGN = "design";
	public static String OUTPIN = "outpin";
	public static String DRIVER = "driver";
	public static String UNPLACED = "unplaced";
	public static String UNBONDED = "unbonded";
	public static String INSTANCE = "instance";
	public static String COMMA = ",";
	public static String SEMICOLON = ";";
	public static String PIP0 = "->";
	public static String PIP1 = "=-";
	public static String PIP2 = "=>";
	public static String PIP3 = "==";
	
	private Design design;
	private Tile pipTile = new Tile();

	/**
	 * @return the design
	 */
	public Design getDesign(){
		return design;
	}

	/**
	 * @param design the design to set
	 */
	public void setDesign(Design design){
		this.design = design;
	}

	private BufferedInputStream reader;
	
	private String fileName;
	
	private ParserState state;
	
	private int lineNumber;
	
	/** A unique set of strings used to avoid duplicate strings in memory */
	private StringPool pool;
	
	Net currNet = null;
	Instance currInstance = null;
	Module currModule = null;
	PIP currPIP = null;
	Pin currPin = null;
	String currModuleAnchorName = null;
	String currModuleInstanceName = null;
	Device dev = null;
	HashMap<String, Pin> modPinMap = null;
	ArrayList<String> portNames = null;
	ArrayList<String> portInstanceNames = null;
	ArrayList<String> portPinNames = null;
	
	public DesignParser(String fileName){
		this.fileName = fileName;

		try{
			reader = new BufferedInputStream(new FileInputStream(fileName));
		}
		catch(FileNotFoundException e){
			e.printStackTrace();
			MessageGenerator.briefErrorAndExit("XDL Design Parser ERROR: Could not find XDL file: " + fileName);
		}
	}	
	
	private void expect(String expectedString, String token, ParserState state){
		if(!expectedString.equals(token)){
			new Exception().printStackTrace();
			MessageGenerator.briefErrorAndExit("Parsing Error: Expected token: " + expectedString +
					", encountered: " + token + " on line: " + lineNumber + " in parser state: " + state.toString());
		}
	}
	
	public Design parseXDL(){
		pool = new StringPool();
		lineNumber = 1;
		state = ParserState.BEGIN_DESIGN;
		char[] buffer = new char[8192];
		try{
			int ch = -1;
			int prev = -1;
			int idx = 0;
			boolean inComment = false;
			while((ch = reader.read()) != -1){
				if(ch == '\n') lineNumber++;
				if(inComment){
					if((prev == '\r' || prev == '\n') && (ch != '\r' && ch != '\n')){
						inComment = false;
						idx = 0;
					}
					else{
						prev = ch;
						continue;
					}
				}
				//System.out.println("ch["+idx+"]=" + ch + "(" + (char) + ch +")");
				switch(ch){
					case ',':
						if(state.equals(ParserState.ATTRIBUTE)){
							buffer[idx++] = (char) ch;
							break;
						}
					case ' ':
					case '"':
						if(state.equals(ParserState.ATTRIBUTE) && prev == '\\'){
							buffer[idx++] = (char) ch;
							break;
						}
					case '\n':
					case '\r':
					case '\t':
						if(idx > 0){
							parseToken(new String(buffer,0, idx));
							idx = 0;
						}
						break;
					case '#':
						if(prev == '\r' || prev == '\n' || prev == -1){
							inComment = true;
							break;
						}
					default:
						buffer[idx++] = (char) ch;
				}
				prev = ch;
			}
		}
		catch(IOException e){
			e.printStackTrace();
			MessageGenerator.briefErrorAndExit("ERROR: IOException while reading XDL file: " + fileName);
		} 
		return design;
	}
	
	/**
	 * This is the parser state machine that decides how the token created from
	 * parseXDL should be applied to the design.  It does some amount of error
	 * checking but is not full proof.
	 * @param token The token to parse into the design.
	 */
	private void parseToken(String token){
		//System.out.println(lineNumber + "<" + token + ">");

		switch(state){
			case BEGIN_DESIGN:
				expect(DESIGN, token, ParserState.BEGIN_DESIGN);
				state = ParserState.DESIGN_NAME;
				break;
			case DESIGN_NAME:
				design.setName(pool.getUnique(token));
				state = ParserState.PART_NAME;
				break;
			case PART_NAME:
				if(design.isHardMacro()){
					if(token.endsWith(";")){
						state = ParserState.XDL_STATEMENT;
						token = token.substring(0, token.length()-1);
					}
					else{
						state = ParserState.CFG_STRING;
					}
				}
				else{
					state = ParserState.NCD_VERSION;					
				}
				design.setPartName(pool.getUnique(token));
				dev = design.getDevice();
				break;
			case NCD_VERSION:
				design.setNCDVersion(pool.getUnique(token));
				state = ParserState.CFG_STRING;
				break;
			case CFG_STRING:
				if(token.equals(CFG)) state = ParserState.ATTRIBUTE;
				else if(token.equals(SEMICOLON)){
					if(currModule != null){
						state = ParserState.MODULE_STATEMENT;
					}
					else{
						state = ParserState.XDL_STATEMENT;										
					}
				}
				else expect("cfg or ;", token, ParserState.CFG_STRING);
				break;
			case ATTRIBUTE:
				if(token.equals(SEMICOLON)){
					currInstance = null;
					currNet = null;

					if(currModule != null){
						state = ParserState.MODULE_STATEMENT;
					}
					else{
						currInstance = null;
						currNet = null;
						state = ParserState.XDL_STATEMENT;										
					}
				}
				else if(token.equals(COMMA) && currNet != null){
					state = ParserState.NET_STATEMENT;
				}
				else{
					Attribute attribute = createAttribute(token);
					if(currInstance != null) currInstance.addAttribute(attribute);
					else if(currNet != null){
						currNet.addAttribute(attribute);
						if(attribute.getPhysicalName().equals("_MACRO")){
					      ModuleInstance mi = design.getModuleInstance(attribute.getValue());
					      currNet.setModuleInstance(mi);
					      mi.addNet(currNet);
					      Module module = mi.getModule();
					      currNet.setModuleTemplate(module);
					      currNet.setModuleTemplateNet(module.getNet(currNet.getName().replaceFirst(mi.getName() + "/", "")));
					    }
					}
					else if(currModule != null) currModule.addAttribute(attribute);
					else design.addAttribute(attribute);
				}
				break;
			case XDL_STATEMENT:
				if(token.equals(INST)|| token.equals(INSTANCE)){
					currInstance = new Instance();
					state = ParserState.INSTANCE_NAME;
				}
				else if(token.equals(NET)){
					currNet = new Net();
					state = ParserState.NET_NAME;
				}
				else if(token.equals(MODULE)){
					currModule = new Module();
					modPinMap = new HashMap<String, Pin>();
					portNames = new ArrayList<String>();
					portInstanceNames = new ArrayList<String>();
					portPinNames = new ArrayList<String>();
					state = ParserState.MODULE_NAME;
				}
				else if(token.equals(ENDMODULE)){
					state = ParserState.END_MODULE_NAME;
				}
				else{
					expect("inst, net, module or endmodule", token, ParserState.XDL_STATEMENT);
				}
				break;
			case INSTANCE_NAME:
				currInstance.setName(pool.getUnique(token));
				currInstance.setDesign(design);
				if(currModule == null){
					design.addInstance(currInstance);
				}
				else{
					currModule.addInstance(currInstance);
					currInstance.setModuleTemplate(currModule);
					if(currInstance.getName().equals(currModuleAnchorName)){
						currModule.setAnchor(currInstance);
					}
				}
				state = ParserState.INSTANCE_TYPE;
				break;
			case INSTANCE_TYPE:
				  SiteType t = Utils.createPrimitiveType(token);
				  if(t == null){
				    MessageGenerator.briefErrorAndExit("XDL Design Parser Error in file: "+ fileName +", Failed parsing Instance type: \"" + token + "\"");
				  }
				  currInstance.setType(t);
				  state = ParserState.INSTANCE_PLACED;
				break;
			case INSTANCE_PLACED:
				if(token.equals(PLACED)) state = ParserState.INSTANCE_TILE;
				else if(token.equals(UNPLACED)) state = ParserState.INSTANCE_BONDED;
				else expect("placed or unplaced", token, ParserState.INSTANCE_PLACED);
				break;
			case INSTANCE_TILE:
				Tile tile = dev.getTile(token);
				if(tile == null){
					MessageGenerator.briefErrorAndExit("XDL Design Parser Error in file: "+ fileName +", Invalid tile " +
							token + " on line " + lineNumber);
				}
				state = ParserState.INSTANCE_SITE;
				break;
			case INSTANCE_SITE:
				Site site = dev.getPrimitiveSite(token);
				if(site == null){
					MessageGenerator.briefErrorAndExit("XDL Design Parser Error in file: "+ fileName +", Invalid primitive site " +
							token + " on line " + lineNumber);
				}
				if(currModule != null){
					currInstance.setSite(dev.getPrimitiveSite(token));
				}else{
					currInstance.place(dev.getPrimitiveSite(token));					
				}
				state = ParserState.MODULE_INSTANCE_TOKEN;
				break;
			case INSTANCE_BONDED:
				if(token.equals(COMMA)){
					state = ParserState.MODULE_INSTANCE_TOKEN;
				}
				else if(token.equals(CFG)){
					state = ParserState.ATTRIBUTE;
				}
				else if(token.equals(MODULE)){
					state = ParserState.MODULE_INSTANCE_NAME;
				}
				else if(token.equals(BONDED)){
					currInstance.setBonded(true);
					state = ParserState.MODULE_INSTANCE_TOKEN;
				}
				else if(token.equals(UNBONDED)){
					currInstance.setBonded(false);
					state = ParserState.MODULE_INSTANCE_TOKEN;
				}
				else{
					expect("bonded, unbonded or ,", token, ParserState.INSTANCE_BONDED);
				}
				break;
			case MODULE_INSTANCE_TOKEN:
				if(token.equals(CFG)) state = ParserState.ATTRIBUTE;
				else if(token.equals(MODULE)) state = ParserState.MODULE_INSTANCE_NAME;
				else expect("cfg or module", token, ParserState.MODULE_INSTANCE_TOKEN);
				break;
			case MODULE_INSTANCE_NAME:
				currModuleInstanceName = pool.getUnique(token);
				state = ParserState.MODULE_TEMPLATE_NAME;
				break;
			case MODULE_TEMPLATE_NAME:
				currInstance.setModuleTemplate(design.getModule(token));
				state = ParserState.MODULE_TEMPLATE_INSTANCE_NAME;
				break;
			case MODULE_TEMPLATE_INSTANCE_NAME:
				currInstance.setModuleTemplateInstance(currInstance.getModuleTemplate().getInstance(token));
				ModuleInstance moduleInstance = design.addInstanceToModuleInstances(currInstance, currModuleInstanceName);
				if(currInstance.getModuleTemplateInstance().equals(currInstance.getModuleTemplate().getAnchor())){
					moduleInstance.setAnchor(currInstance);
				}
				state = ParserState.CFG_STRING;
				break;
			case NET_NAME:
				currNet.setName(pool.getUnique(token));
				if(currModule == null) design.addNet(currNet);
				else currModule.addNet(currNet);
				state = ParserState.NET_TYPE;
				break;
			case NET_TYPE:
				if(token.equals(COMMA) || token.equals(WIRE)){
					currNet.setType(NetType.WIRE);
				}
				else if(token.equals(CFG)){
					state = ParserState.ATTRIBUTE;
					break;
				}
				else if(token.equals(GND) || token.equals(GROUND)){
					currNet.setType(NetType.GND);
				}
				else if(token.equals(VCC) || token.equals(POWER)){
					currNet.setType(NetType.VCC);
				}
				else if(token.equals(INPIN)){
					currPin = new Pin();
					currPin.setIsOutputPin(false);
					currNet.addPin(currPin);
					state = ParserState.PIN_INSTANCE_NAME;
					break;
				}
				else if(token.equals(OUTPIN)){
					currPin = new Pin();
					currPin.setIsOutputPin(true);
					if(currNet.getSource() != null){
						MessageGenerator.briefErrorAndExit("XDL Design Parser Error in file: "+ fileName +", The net " +
							currNet.getName() + " has two or more outpins (line " +
							lineNumber + ")");
					}
					currNet.addPin(currPin);
					state = ParserState.PIN_INSTANCE_NAME;
					break;
				}
				else if(token.equals(INOUT)){
					currPin = new Pin();
					currPin.setPinType(PinType.INOUT);
					currNet.addPin(currPin);
					state = ParserState.PIN_INSTANCE_NAME;
					break;
				}
				else{
					expect("wire, vcc or power, gnd or ground or ,",token, ParserState.NET_TYPE);
				}
				state = ParserState.NET_STATEMENT; 
				break;
			case NET_STATEMENT:
				if(token.equals(PIP)){
					currPIP = new PIP();
					currNet.addPIP(currPIP);
					state = ParserState.PIP_TILE; 
				}
				else if(token.equals(INPIN)){
					currPin = new Pin();
					currPin.setIsOutputPin(false);
					currNet.addPin(currPin);
					state = ParserState.PIN_INSTANCE_NAME;
				}
				else if(token.equals(OUTPIN)){
					currPin = new Pin();
					currPin.setIsOutputPin(true);
					if(currNet.getSource() != null){
						MessageGenerator.briefErrorAndExit("XDL Design Parser Error in file: "+ fileName +", The net " +
							currNet.getName() + " has two or more outpins (line " +
							lineNumber + ")");
					}
					currNet.addPin(currPin);
					state = ParserState.PIN_INSTANCE_NAME;
				}
				else if(token.equals(INOUT)){
					currPin = new Pin();
					currPin.setPinType(PinType.INOUT);
					currNet.addPin(currPin);
					state = ParserState.PIN_INSTANCE_NAME;
					break;
				}
				else if(token.equals(SEMICOLON)){
					state = ParserState.XDL_STATEMENT;
				}
				else if(token.equals(CFG)){
					state = ParserState.ATTRIBUTE;
				}
				break;
			case PIN_INSTANCE_NAME:
				Instance inst;
				if(currModule == null) inst = design.getInstance(token);
				else inst = currModule.getInstance(token);
				if(inst == null){
					MessageGenerator.briefErrorAndExit("ERROR: Could not find instance " +
						token + " on line " + lineNumber);
				}
				currPin.setInstance(inst);
				inst.addToNetList(currNet);
				state = ParserState.PIN_NAME;
				break;
			case PIN_NAME:
				currPin.setPinName(pool.getUnique(token));
				currPin.getInstance().addPin(currPin);
				if(currModule != null){
				    modPinMap.put(currPin.getInstanceName() + currPin.getName(), currPin);
				}
				state = ParserState.NET_STATEMENT;
				break;
			case PIP_TILE:
				pipTile = dev.getTile(token);
				if(pipTile == null){
					MessageGenerator.briefErrorAndExit("Invalid tile " +
							token + " on line " + lineNumber);
				}
				state = ParserState.PIP_WIRE0;
				break;
			case PIP_WIRE0:
				int wire0 = dev.getWireEnumerator().getWireEnum(token);
				if(wire0 == -1) {
					MessageGenerator.briefErrorAndExit("ERROR: Invalid wire: " +
							token + " found on line " + lineNumber);
				}
				currPIP.setStartWire(new TileWire(pipTile, wire0));
				state = ParserState.PIP_CONN_TYPE;
				break;
			case PIP_CONN_TYPE:
				if(token.equals(PIP0) || token.equals(PIP1) || token.equals(PIP2) || token.equals(PIP3)){
					state = ParserState.PIP_WIRE1;
				}
				else{
					expect("->, =-, ==, or =>", token, ParserState.PIP_CONN_TYPE);
				}
				break;
			case PIP_WIRE1:
				int wire1 = dev.getWireEnumerator().getWireEnum(token);
				if(wire1 == -1) {
					MessageGenerator.briefErrorAndExit("XDL Design Parser Error in file: "+ fileName +", Invalid wire: " +
							token + " found on line " + lineNumber);
				}
				currPIP.setEndWire(new TileWire(pipTile, wire1));
				pipTile = null;
				state = ParserState.NET_STATEMENT; 
				break;
			case MODULE_NAME:
				currModule.setName(pool.getUnique(token));
				state = ParserState.MODULE_ANCHOR_NAME;
				break;
			case MODULE_ANCHOR_NAME:
				currModuleAnchorName = pool.getUnique(token);
				state = ParserState.CFG_STRING;
				break;
			case MODULE_STATEMENT:
				if(token.equals(PORT)){
					state = ParserState.PORT_NAME;
				}
				if(token.equals(INST)|| token.equals(INSTANCE)){
					currInstance = new Instance();
					state = ParserState.INSTANCE_NAME;
				}
				else if(token.equals(NET)){
					currNet = new Net();
					state = ParserState.NET_NAME;
				}
				else if(token.equals(ENDMODULE)){
					state = ParserState.END_MODULE_NAME;
				}
				break;
			case PORT_NAME:
				portNames.add(pool.getUnique(token));
				state = ParserState.PORT_INSTANCE_NAME;
				break;
			case PORT_INSTANCE_NAME:
				portInstanceNames.add(pool.getUnique(token));
				state = ParserState.PORT_PIN_NAME;
				break;
			case PORT_PIN_NAME:
				portPinNames.add(pool.getUnique(token));
				state = ParserState.END_PORT;
				break;
			case END_PORT:
				expect(SEMICOLON, token, ParserState.END_PORT);
				state = ParserState.MODULE_STATEMENT;
				break;
			case END_MODULE_NAME:
				if(!currModule.getName().equals(token)){
					MessageGenerator.briefErrorAndExit("XDL Design Parser Error in file: "+ fileName +", Mismatched module names: " +
						currModule.getName() + " and " + token + " at line: " + lineNumber);
				}
				state = ParserState.END_MODULE;
				break;
			case END_MODULE:
				expect(SEMICOLON, token, ParserState.END_MODULE);
				design.addModule(currModule);
				for(int i = 0; i <portNames.size(); i++){
					String key = portInstanceNames.get(i) + portPinNames.get(i);
					currModule.addPort(new Port(portNames.get(i), modPinMap.get(key)));
				}
				portNames = null;
				portInstanceNames = null;
				portPinNames = null;
				modPinMap = null;
				currModuleAnchorName = null;
				currModule = null;
				state = ParserState.XDL_STATEMENT;
				break;
		}
	}
	
	/**
	 * This method will take a string and parse it into the 3-part attribute.  It 
	 * detects escaped colons ('\:') and includes them as part of the logicalName if
	 * present.
	 * @param attribute The original token string found from parsing the XDL file.
	 * @return A new attribute object populated from the string attribute.
	 */
	private Attribute createAttribute(String attribute){
	    int break1 = attribute.indexOf(':');
	    int break2 = attribute.indexOf(':', break1 + 1);
	    while(attribute.charAt(break2-1) == '\\'){
	    	break2 = attribute.indexOf(':', break2 + 1);
	    }
	    String physicalName = pool.getUnique(attribute.substring(0, break1));
	    String logicalName = pool.getUnique(attribute.substring(break1 + 1, break2));
	    String value = pool.getUnique(attribute.substring(break2 + 1, attribute.length()));
		return new Attribute(physicalName, logicalName, value);
	}
}
