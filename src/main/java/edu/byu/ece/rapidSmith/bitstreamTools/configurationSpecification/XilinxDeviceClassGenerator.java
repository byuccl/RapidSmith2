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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamParseException;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamParser;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Packet;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketList;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.RegisterType;

/**
 * Shared code for all the XilinxDeviceClass generators. This is based on some code
 * Chris Lavin wrote.
 */
public abstract class XilinxDeviceClassGenerator {

    // TODO: We should add a new member of the PartLibrary that specifies the family name
    // of the library. The generated code would need to set this member.
    protected static final String kFilePrefix = "o7aTOwq3";

    /**
     * Construct a new class generator and collect all of the data needed
     * to write Java source code for a part library. Data is collected
     * using the Xilinx partgen, xdl, and bitgen tools.
     * @param architecture
     */
    protected XilinxDeviceClassGenerator(String architecture, int rowMask, int rowBitPos, int topBottomMask, int topBottomBitPos) {

        _partGenOutput = new ArrayList<>();
        _partNames = new ArrayList<>();
        _validPackages = new ArrayList<>();
        _validSpeedGrades = new ArrayList<>();
        _deviceIDCodes = new ArrayList<>();
        _logicLayouts = new ArrayList<>();
        _bramInterconnectLayouts = new ArrayList<>();
        _bramContentLayouts = new ArrayList<>();
        _numTopRows = new ArrayList<>();
        _numBottomRows = new ArrayList<>();
        _architecture = architecture;
        
        _rowMask = rowMask;
        _rowBitPos = rowBitPos;
        _topBottomMask = topBottomMask;
        _topBottomBitPos = topBottomBitPos;

        _xdlrcFile = new File(kFilePrefix + ".xdlrc");
        _xdlrcFile.deleteOnExit();
        _xdlFile = new File(kFilePrefix + ".xdl");
        _xdlFile.deleteOnExit();
        _ncdFile = new File(kFilePrefix + ".ncd");
        _ncdFile.deleteOnExit();
        _bitFile = new File(kFilePrefix + ".bit");
        _bitFile.deleteOnExit();
        _bgnFile = new File(kFilePrefix + ".bgn");
        _bgnFile.deleteOnExit();

        collectData();
    }

    public abstract void createJavaSourceFile();

    public void createFileHeader(Writer writer) throws IOException {
		writer.write("/*" + "\n");
		writer.write(" * Copyright (c) 2010-2011 Brigham Young University" + "\n");
		writer.write(" * " + "\n");
		writer.write(" * This file is part of the BYU RapidSmith Tools." + "\n");
		writer.write(" * " + "\n");
		writer.write(" * BYU RapidSmith Tools is free software: you may redistribute it" + "\n"); 
		writer.write(" * and/or modify it under the terms of the GNU General Public License " + "\n");
		writer.write(" * as published by the Free Software Foundation, either version 2 of " + "\n");
		writer.write(" * the License, or (at your option) any later version." + "\n");
		writer.write(" * "+ "\n");
		writer.write(" * BYU RapidSmith Tools is distributed in the hope that it will be" + "\n"); 
		writer.write(" * useful, but WITHOUT ANY WARRANTY; without even the implied warranty" + "\n");
		writer.write(" * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU " + "\n");
		writer.write(" * General Public License for more details." + "\n");
		writer.write(" * " + "\n");
		writer.write(" * A copy of the GNU General Public License is included with the BYU" + "\n"); 
		writer.write(" * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also " + "\n");
		writer.write(" * get a copy of the license at <http://www.gnu.org/licenses/>." + "\n");
		writer.write(" * " + "\n");
		writer.write(" */" + "\n");

        writer.write("/**\n");
        writer.write(" * This file was auto-generated on " + (new Date()).toString() + "\n");
        writer.write(" * by " + this.getClass().getCanonicalName() + ".\n");
        writer.write(" * See the source code to make changes.\n *\n");
        writer.write(" * Do not modify this file directly.\n");
        writer.write(" */\n\n\n");
    }

    /**
     * Method that starts all of the work.
     */
    protected void collectData() {
        List<String> partGenOutput = generatePartgenOutput(_architecture);
        parseParts(partGenOutput);
        getPartColumnTypes();
        getDeviceIDCodesAndRowCounts();
    }

    /**
     * This method should fill in the following class members:
     * 
     *  _logicLayouts.add(currLogicLayout);
     *  _bramContentLayouts.add(currBramContentLayout);
     *  _bramInterconnectLayouts.add(currBramInterconnectLayout);
     * 
     */
    protected abstract void getPartColumnTypes();

    /**
     * Creates an XDLRC file based on the currPartName.
     */
    protected void generateBriefXDLRCFile(String partName, File file){
        try {   
            Process p = Runtime.getRuntime().exec("xdl -report " + partName + " " + file.getAbsolutePath());
            p.waitFor();
        } 
        catch (IOException e) {
            e.printStackTrace();
            System.out.println("XDLRC Generation failed");
            System.exit(1);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }   
    }

    /**
     *  This method will create an empty bitstream for the part specified by currPartName.
     *  It will then parse the bitstream and find the corresponding device IDCODE and store
     *  it in deviceIDCode.
     */
    protected void getDeviceIDCodesAndRowCounts(){

        int i = 0;
        for (String currPartName : _partNames) {
            System.out.println("Extracting device ID code and row counts for " + currPartName);
            Process p;
            String fullPartName = currPartName + _validPackages.get(i).get(0) + _validSpeedGrades.get(i).get(0);
            try {
                // Generate small XDL design
                FileWriter fw = new FileWriter(_xdlFile);
                fw.write("design \"top\" "+fullPartName+" v3.2 , cfg \"\";");
                fw.close();

                // This is a debug bitstream
                p = Runtime.getRuntime().exec("xdl -xdl2ncd -force "+ kFilePrefix + ".xdl" + " " + kFilePrefix + ".ncd");
                p.waitFor();
                p = Runtime.getRuntime().exec("bitgen -g DebugBitstream:Yes -w -d "+ kFilePrefix + ".ncd ");
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while((input.readLine()) != null){
                }
                p.waitFor();

                // Parse bitstream for ID code
                _deviceIDCodes.add(parseBitstreamForIDCode(kFilePrefix + ".bit"));

                // Parse bitstream for row counts
                Bitstream bitstream = null;
                try {
                    bitstream = BitstreamParser.parseBitstream(kFilePrefix + ".bit");
                } catch (BitstreamParseException | IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                Set<Integer> topRows = new HashSet<>();
                Set<Integer> bottomRows = new HashSet<>();

                PacketList packets = bitstream.getPackets();
                for (Packet packet : packets) {
                    if (packet.getRegType() == RegisterType.LOUT) {
                        int farAddress = packet.getData().get(0);
                        int currentRow = (farAddress & _rowMask) >>> _rowBitPos;
                        int currentTopBottom = (farAddress & _topBottomMask) >>> _topBottomBitPos;
                        
                        Set<Integer> rowSet = (currentTopBottom == 0) ? topRows : bottomRows;
                        rowSet.add(currentRow);
                    }
                }
                
                _numTopRows.add(topRows.size());
                _numBottomRows.add(bottomRows.size());                
                
                p.destroy();


            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("There was an error running xilinx tools.  Check that Xilinx tools are on your path.");
                System.exit(1);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
            i++;
        }
    }

    /**
     * Extracts the IDCode value from the bitstream
     * @param bitstreamFileName The name of the bit file
     */
    protected String parseBitstreamForIDCode(String bitstreamFileName){
        FileInputStream input;
        BufferedInputStream buffer;
        DataInputStream data;

        int thisByte;
        String result = null;
        try {
            input = new FileInputStream(bitstreamFileName);
            buffer = new BufferedInputStream(input);
            data = new DataInputStream(buffer);

            byte[] sync_word = {(byte) 0xaa, (byte) 0x99, (byte) 0x55, (byte) 0x66};
            int matchedBytes = 0;
            // Find sync word
            while(matchedBytes < 4){
                thisByte = data.readByte();
                if (thisByte == sync_word[matchedBytes]) {
                    matchedBytes++;
                }
                else {
                    matchedBytes = 0;
                }

            }

            // Advance to the write ID Code packet
            while(data.readInt() != 0x30018001){}

            result = Integer.toHexString(data.readInt());
            // Pad with zeros at front to get all 8 characters
            while(result.length() < 8){
                result = "0" + result;
            }
            data.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error parsing bitstream " + bitstreamFileName);
            System.exit(1);
        }
        return result;
    }

    /**
     * This method parses the List of Strings that was found in the pargen output.
     * It creates a list of part names (_partNames), the valid packages for each
     * partname (_validPackages), and the valid speed grades for each part (_validSpeedGrades)
     * 
     * 
     */
    protected void parseParts(List<String> partgenOutput) {
        int currentLine = 0;

        while (currentLine < partgenOutput.size() -1 ){

            if(currentLine >= 0){

                //Advance to the next line where the part is named
                String[] tokens = partgenOutput.get(currentLine).split("\\s+");
                while(currentLine+1 < partgenOutput.size() -1 && !tokens[0].startsWith("xc")){
                    currentLine++;
                    tokens = partgenOutput.get(currentLine).split("\\s+");
                }

                // Now we should be on the line with the part name
                _partNames.add(tokens[0]);
                List<String> currValidPackages = new ArrayList<>();
                List<String> currValidSpeedGrades = new ArrayList<>();

                // Add the speed grades
                for(String token : tokens){
                    if(token.startsWith("-")){
                        currValidSpeedGrades.add(token);
                    }
                }
                // Add packages
                do {
                    currentLine++;
                    tokens = partgenOutput.get(currentLine).split("\\s+");
                    currValidPackages.add(tokens[1]);                   
                }
                while((currentLine+1 < partgenOutput.size() -1) && !(partgenOutput.get(currentLine+1).startsWith("xc")));
                _validPackages.add(currValidPackages);
                _validSpeedGrades.add(currValidSpeedGrades);
            }
        }       
    }

    /**
     * This function runs Xilinx partgen and stores all output in an ArrayList of Strings
     * where each string is a line from the partgen output. This output is stored in the
     * class member named "output". It is parsed by teh "parseNextPart" method.
     * 
     * You give it an architecture and it will tell you what the valid parts are for the
     * architecture.
     * 
     * @param arch The parameter to pass to partgen -arch
     */
    protected static List<String> generatePartgenOutput(String arch){
        BufferedReader input;
        Process p;
        String line;
        ArrayList<String> output = new ArrayList<>();
        try {
            p = Runtime.getRuntime().exec("partgen -arch " + arch);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((line = input.readLine()) != null){
                output.add(line);
            }
            input.close();
            p.waitFor();
            p.destroy();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("There was an error running partgen with arch:"+arch);
            System.err.println("Check that Xilinx tools are on your path.");
            System.err.println("Also, make sure " + arch + " is a valid Xilinx architecture.");
            System.exit(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return output;
    }

    
    protected List<String> _partGenOutput;

    protected List<String> _partNames;

    protected List<List<String>> _validPackages;

    protected List<List<String>> _validSpeedGrades;

    protected List<String> _deviceIDCodes;

    /**
     * Outer list is 
     */
    protected List<List<BlockSubType>> _logicLayouts;

    protected List<List<BlockSubType>> _bramInterconnectLayouts;

    protected List<List<BlockSubType>> _bramContentLayouts;

    protected List<Integer> _numTopRows;
    protected List<Integer> _numBottomRows;

    protected String _architecture;

    protected File _xdlrcFile;
    protected File _xdlFile;
    protected File _ncdFile;
    protected File _bitFile;
    protected File _bgnFile;
    
    protected int _rowMask;
    protected int _rowBitPos;
    protected int _topBottomMask;
    protected int _topBottomBitPos;
}
