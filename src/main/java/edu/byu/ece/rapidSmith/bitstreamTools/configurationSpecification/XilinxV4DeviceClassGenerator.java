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
package edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class generates Java source files that describe Xilinx device specifications
 * for the virtex4 architecture. This executable requires that the Xilinx executables
 * are on your path. Also, this class requires version 11.+
 * 
 */
public class XilinxV4DeviceClassGenerator extends XilinxDeviceClassGenerator {
	
    public XilinxV4DeviceClassGenerator() {
        super("virtex4", V4ConfigurationSpecification.V4_ROW_MASK, V4ConfigurationSpecification.V4_ROW_BIT_POS, V4ConfigurationSpecification.V4_TOP_BOTTOM_MASK, V4ConfigurationSpecification.V4_TOP_BOTTOM_BIT_POS);
    }
    
	/**
	 * Create the Java source code for a part library of the architecture associated
	 * with the class generator instance.
	 */
	public void createJavaSourceFile() {
        String famNumber = Character.toString(_architecture.charAt(_architecture.length()-1));
        String namePrefix = "V" + famNumber;
        String fileName = namePrefix + "PartLibrary.java";
            
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(fileName));        
            createFileHeader(buf);            
            buf.write("package edu.byu.ece.bitstreamTools.configurationSpecification;\n\n");
            buf.write("import java.util.ArrayList;\nimport java.util.Arrays;\n\n");
    
            buf.write("public class " + namePrefix + "PartLibrary extends PartLibrary {\n\n");
            
            // constructor
            buf.write("\tpublic " + namePrefix + "PartLibrary() {\n");
            buf.write("\t\tsuper();\n");
            buf.write("\t}\n\n");
    
            // add parts method
            buf.write("\tprotected void addParts() {\n");
            for (String currPartName : _partNames) {
                buf.write("\t\taddPart(new " + currPartName.toUpperCase() + "());\n");
            }
            buf.write("\t}\n\n");
            
            // define parts
            int i = 0;
            for (String currPartName : _partNames) {
    
                buf.write("\tclass " + currPartName.toUpperCase() +" extends V"+famNumber+"ConfigurationSpecification {\n\n" );
                buf.write("\t\tpublic " +currPartName.toUpperCase() + "() {\n");
                buf.write("\t\t\tsuper();\n");
                buf.write("\t\t\t_deviceName = \""+currPartName.toUpperCase()+"\";\n");
                buf.write("\t\t\t_deviceIDCode = \""+ _deviceIDCodes.get(i)+"\";\n");
    
                // Write out Packages
                buf.write("\t\t\t_validPackages = new String[] {");
                for(String pkg : _validPackages.get(i)){
                    buf.write("\""+ pkg + "\", ");
                }
                buf.write("};\n");
    
                // Write out Speed Grades
                buf.write("\t\t\t_validSpeedGrades = new String[] {");
                for(String speeds : _validSpeedGrades.get(i)){
                    buf.write("\""+ speeds + "\", ");
                }
                buf.write("};\n");
    
                buf.write("\t\t\t_topRows = " + _numTopRows.get(i) + ";\n");
                buf.write("\t\t\t_bottomRows = " + _numBottomRows.get(i) + ";\n");
                
                buf.write("\t\t\t_blockTypeLayouts = new ArrayList<BlockTypeInstance>(Arrays.asList(new BlockTypeInstance[] {\n");
                buf.write("\t\t\t\t\tnew BlockTypeInstance(LOGIC_BLOCK_TYPE, new BlockSubType[] {\n\t\t\t\t\t\t");
                for (BlockSubType blk : _logicLayouts.get(i)) {
                    buf.write(blk + ", ");
                }
                buf.write("\n\t\t\t\t\t}),\n");
                buf.write("\t\t\t\t\tnew BlockTypeInstance(BRAM_INTERCONNECT_BLOCK_TYPE, new BlockSubType[] {\n\t\t\t\t\t\t");
                for (BlockSubType blk : _bramInterconnectLayouts.get(i)){
                    buf.write(blk + ", ");
                }
                buf.write("\n\t\t\t\t\t}),\n");
                buf.write("\t\t\t\t\tnew BlockTypeInstance(BRAM_BLOCK_TYPE, new BlockSubType[] {\n\t\t\t\t\t\t");
                for (BlockSubType blk : _bramContentLayouts.get(i)){
                    buf.write(blk + ", ");
                }
                buf.write("\n\t\t\t\t\t}),\n");
                buf.write("\t\t\t}));\n");
                
                buf.write("\t\t\t_overallColumnLayout = new ArrayList<BlockSubType>(Arrays.asList(new BlockSubType[] {");
                for (BlockSubType blk : _overallColumnLayouts.get(i)) {
                    buf.write(blk + ", ");
                }
                buf.write("}));\n");
                                
                buf.write("\t\t}\n");
                buf.write("\t}\n\n");
                i++;
            }
            
            buf.write("}\n");
            buf.flush();
    
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error writing file: " + fileName);
            System.exit(1);
        }
    
    }

    protected void getPartColumnTypes(){
        BufferedReader in;
        String line;
        String[] tokens;
        
        /*
         * A list of a list of BlockSubTypes. The outer list is for all the different
         * parts of the architecture and the inner list are the columns of each part.
         * 
         * The inner list is the overall list of column types for the part.
         */
        _overallColumnLayouts = new ArrayList<>();
        
        for (String partName : _partNames) {
            System.out.println("Generating/parsing .xdlrc for " + partName);
            // Generate XDLRC first
            generateBriefXDLRCFile(partName, _xdlrcFile);
            
            //int currNumRows = 0;
            List<BlockSubType> currLogicLayout = new ArrayList<>();
            List<BlockSubType> currBramInterconnectLayout = new ArrayList<>();
            List<BlockSubType> currBramContentLayout = new ArrayList<>();
            List<BlockSubType> currOverallColumnLayout = new ArrayList<>();
            
            try {
                in = new BufferedReader(new FileReader(_xdlrcFile));
                line = in.readLine();
                while(line != null){
                    tokens = line.split("\\s+");
    
    
    
                    if(tokens.length > 1 && tokens[1].equals("(tile")){
                        if(tokens[1].equals("(tile")){
                            //currNumRows = Integer.parseInt(tokens[2]) / 36;
                        }
    
                        if(Integer.parseInt(tokens[2]) == 1){
                            if(tokens[5].equals("CLB")){
                                currLogicLayout.add(V4ConfigurationSpecification.CLB);
                                currOverallColumnLayout.add(V4ConfigurationSpecification.CLB);
                            }
                            else if(tokens[5].contains("CLK")){
                                currLogicLayout.add(V4ConfigurationSpecification.CLK);
                                currOverallColumnLayout.add(V4ConfigurationSpecification.CLK);
                            }
                            else if(tokens[5].contains("IOIS") || tokens[5].contains("DCM") || tokens[5].contains("EMPTY_MONITOR")){
                                currLogicLayout.add(V4ConfigurationSpecification.IOB);
                                currOverallColumnLayout.add(V4ConfigurationSpecification.IOB);
                            }
                            else if(tokens[5].contains("BRAM")){
                                currBramInterconnectLayout.add(V4ConfigurationSpecification.BRAMINTERCONNECT);
                                currBramContentLayout.add(V4ConfigurationSpecification.BRAMCONTENT);
                                currOverallColumnLayout.add(V4ConfigurationSpecification.BRAMINTERCONNECT);
                            }
                            else if(tokens[5].contains("DSP")){
                                currLogicLayout.add(V4ConfigurationSpecification .DSP);
                                currOverallColumnLayout.add(V4ConfigurationSpecification.DSP);
                            }
                            else if(tokens[5].contains("EMPTY_MGT")){
                                currLogicLayout.add(V4ConfigurationSpecification.MGT);
                                currOverallColumnLayout.add(V4ConfigurationSpecification.MGT);
                            }
                        }
                    }
                    line = in.readLine();
                }
                in.close();
    
                currLogicLayout.add(V4ConfigurationSpecification.LOGIC_OVERHEAD);
                currBramContentLayout.add(V4ConfigurationSpecification.BRAMOVERHEAD);
                currBramInterconnectLayout.add(V4ConfigurationSpecification.BRAMOVERHEAD);
                currOverallColumnLayout.add(V4ConfigurationSpecification.LOGIC_OVERHEAD);
                
                _logicLayouts.add(currLogicLayout);
                _bramContentLayouts.add(currBramContentLayout);
                _bramInterconnectLayouts.add(currBramInterconnectLayout);
                _overallColumnLayouts.add(currOverallColumnLayout);
                
            } 
            catch (FileNotFoundException e) {
                e.printStackTrace();
                System.err.println("Error opening temporary file: " + _xdlrcFile.getAbsolutePath());
                System.exit(1);
            }
            catch (IOException e){
                e.printStackTrace();
                System.err.println("Error reading temporary file: " + _xdlrcFile.getAbsolutePath());
                System.exit(1);         
            }
        }
    }
	
	/**
	 * Allows this class to run stand alone from the rest of the project
	 * @param args The architectures for which to generate the java files (ie. virtex4, virtex5 ...)
	 */
	public static void main(String args[]){
	    XilinxV4DeviceClassGenerator gen = new XilinxV4DeviceClassGenerator();
	    gen.createJavaSourceFile();
	}

    protected List<List<BlockSubType>> _overallColumnLayouts;
	
}
