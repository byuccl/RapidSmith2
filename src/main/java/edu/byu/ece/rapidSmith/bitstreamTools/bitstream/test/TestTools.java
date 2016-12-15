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
package edu.byu.ece.rapidSmith.bitstreamTools.bitstream.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamParseException;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamParser;

public class TestTools {

    public static boolean compareBlankBitstreams(String architecture) {
        return compareBlankBitstreams(getPartgenDescription(architecture));
    }
    
    public static boolean compareBlankBitstreams(XilinxPartgenDescription desc) {
        boolean completeMatch = true;
        List<String> partNames = desc.getPartNames();
        for (String partName : partNames) {
            File dir = new File(partName);
            File inputBit = new File(dir, partName + ".bit");
            File outputBit = new File(dir, partName + "Java.bit");
            File inputMcs = new File(dir, partName + ".mcs");
            File outputMcs = new File(dir, partName + "Java.mcs");
            
            // generate input mcs (from promgen)
            try {
                Process p = Runtime.getRuntime().exec("promgen -u 0 " + inputBit.getAbsolutePath() + " -o " + inputMcs.getAbsolutePath());
                p.waitFor();
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // generate output .bit and output .mcs
            Bitstream inputBitstream = null;
            try {
                inputBitstream = BitstreamParser.parseBitstream(inputBit);
            } catch (BitstreamParseException | IOException e) {
                e.printStackTrace();
            }

            FileOutputStream os_bit = null;
            try {
                os_bit = new FileOutputStream(outputBit);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            
            try {
                inputBitstream.outputHeaderBitstream(os_bit);
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            FileOutputStream os_mcs = null;
            try {
                os_mcs = new FileOutputStream(outputMcs);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            inputBitstream.writeBitstreamToMCS(os_mcs);

            // compare bitstreams
            boolean bitMatch = false;
            try {
                Process p = Runtime.getRuntime().exec("diff " + inputBit.getAbsolutePath() + " " + outputBit.getAbsolutePath());
                int result = p.waitFor();
                bitMatch = (result == 0);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // compare mcs files
            boolean mcsMatch = false;
            try {
                Process p = Runtime.getRuntime().exec("diff " + inputMcs.getAbsolutePath() + " " + outputMcs.getAbsolutePath());
                int result = p.waitFor();
                mcsMatch = (result == 0);
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (bitMatch && mcsMatch) {
                System.out.println(".bit and .mcs files match for part: " + partName);
            }
            else {
                if (!bitMatch) {
                    System.out.println(".bit files don't match for part: " + partName);
                }
                if (!mcsMatch) {
                    System.out.println(".mcs files don't match for part: " + partName);
                }
            }
            completeMatch = completeMatch && bitMatch && mcsMatch;            
        }
        return completeMatch;
    }
    
    public static void generateBlankBitstreams(String architecture, boolean debugBitstream) {
        String debugOption = "";
        if (debugBitstream) {
            debugOption = "-g DebugBitstream:Yes ";
        }
        
        XilinxPartgenDescription desc = getPartgenDescription(architecture);
        List<String> partNames = desc.getPartNames();
        
        File baseTempFile = null;
        try {
            baseTempFile = File.createTempFile("JX739fjd", "sdlkf");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        baseTempFile.deleteOnExit();
        
        File xdlFile = new File(baseTempFile.getAbsolutePath() + ".xdl");
        xdlFile.deleteOnExit();
        File ncdFile = new File(baseTempFile.getAbsolutePath() + ".ncd");
        ncdFile.deleteOnExit();
        
        for (String partName : partNames) {
            System.out.println("Generating empty bitstream for: " + partName);
            File bitFileDirectory = new File(partName);
            bitFileDirectory.mkdir();
            File bitFile = new File(bitFileDirectory, partName + ".bit");
            File bgnFile = new File(bitFileDirectory, partName + ".bgn");
            bgnFile.deleteOnExit();
            
            String fullName = partName + desc.getValidPackagesForPart(partName).get(0) + desc.getValidSpeedGradesForPart(partName).get(0);
            
            // Generate small XDL design
            FileWriter fw;
            try {
                fw = new FileWriter(xdlFile);
                fw.write("design \"top\" " + fullName + " v3.2 , cfg \"\";");
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }

            Process p;
            try {
                p = Runtime.getRuntime().exec("xdl -xdl2ncd -force "+ xdlFile.getAbsolutePath() + " " + ncdFile.getAbsolutePath());

                p.waitFor();
                p = Runtime.getRuntime().exec("bitgen " + debugOption + "-w -d " + ncdFile.getAbsolutePath() + " " + bitFile.getAbsolutePath() + " ");
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while((input.readLine()) != null){
                }
                p.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }

        }
        
        baseTempFile.delete();
        xdlFile.delete();
        ncdFile.delete();
    }
    
    public static void generateBlankBitstream(String partName, boolean debugBitstream) {
        String debugOption = "";
        if (debugBitstream) {
            debugOption = "-g DebugBitstream:Yes ";
        }
        
        File baseTempFile = null;
        try {
            baseTempFile = File.createTempFile("JX739fjd", "sdlkf");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        baseTempFile.deleteOnExit();
        
        File xdlFile = new File(baseTempFile.getAbsolutePath() + ".xdl");
        xdlFile.deleteOnExit();
        File ncdFile = new File(baseTempFile.getAbsolutePath() + ".ncd");
        ncdFile.deleteOnExit();
        
        System.out.println("Generating empty bitstream for: " + partName);
 
        File bitFile = new File(partName + (debugBitstream ? "_debug" : "") + ".bit");
        File bgnFile = new File(partName + (debugBitstream ? "_debug" : "") + ".bgn");
        bgnFile.deleteOnExit();
        
        String fullName = partName;//+ desc.getValidPackagesForPart(partName).get(0) + desc.getValidSpeedGradesForPart(partName).get(0);
        
        // Generate small XDL design
        FileWriter fw;
        try {
            fw = new FileWriter(xdlFile);
            fw.write("design \"top\" " + fullName + " v3.2 , cfg \"\";");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Process p;
        try {
            p = Runtime.getRuntime().exec("xdl -xdl2ncd -force "+ xdlFile.getAbsolutePath() + " " + ncdFile.getAbsolutePath());

            p.waitFor();
            p = Runtime.getRuntime().exec("bitgen " + debugOption + "-w -d " + ncdFile.getAbsolutePath() + " " + bitFile.getAbsolutePath() + " ");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((input.readLine()) != null){
            }
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public static XilinxPartgenDescription getPartgenDescription(String architecture) {
        XilinxPartgenDescription result;

        List<String> partNames = new ArrayList<>();
        List<List<String>> validPackages = new ArrayList<>();
        List<List<String>> validSpeedGrades = new ArrayList<>();
        
        BufferedReader input;
        Process p;
        String line;
        ArrayList<String> output = new ArrayList<>();
        try {
            p = Runtime.getRuntime().exec("partgen -arch " + architecture);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((line = input.readLine()) != null){
                output.add(line);
            }
            input.close();
            p.waitFor();
            p.destroy();
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("There was an error running partgen with arch:" + architecture);
            System.err.println("Check that Xilinx tools are on your path.");
            System.err.println("Also, make sure " + architecture + " is a valid Xilinx architecture.");
            System.exit(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int currentLine = 0;

        while (currentLine < output.size() -1 ){

            if(currentLine >= 0){

                //Advance to the next line where the part is named
                String[] tokens = output.get(currentLine).split("\\s+");
                while(currentLine+1 < output.size() -1 && !tokens[0].startsWith("xc")){
                    currentLine++;
                    tokens = output.get(currentLine).split("\\s+");
                }

                // Now we should be on the line with the part name
                partNames.add(tokens[0]);
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
                    tokens = output.get(currentLine).split("\\s+");
                    currValidPackages.add(tokens[1]);                   
                }
                while((currentLine+1 < output.size() -1) && !(output.get(currentLine+1).startsWith("xc")));
                validPackages.add(currValidPackages);
                validSpeedGrades.add(currValidSpeedGrades);
            }
        }

        result = new XilinxPartgenDescription(partNames, validPackages, validSpeedGrades);

        return result;
    }

}
