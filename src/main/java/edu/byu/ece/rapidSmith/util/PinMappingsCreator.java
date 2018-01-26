package edu.byu.ece.rapidSmith.util;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Manages dynamic pin mappings.  It turns out that, for some cells,
 * how their pins are mapped to bel pins when placed is not static.
 * Rather, the mappings are sometimes a function of the cell's
 * properties. This class contains routines for creating and managing
 * those mappings.
 *
 * @author Brent Nelson
 */
public class PinMappingsCreator {
	
  private List<String> doCmd(VivadoConsole vc, String cmd, boolean verbose) {
	  if (verbose) 
		  System.out.println("Running command: " + cmd);
	  return vc.runCommand(cmd);
  }


  /**
   * Creates the pin mappings and writes them to an XML file.  It does
   * this by creating a script ("setup.tcl") which sets things up and
   * then which calls a second script
   * ("create_nondefault_pin_mappings.tcl") to create the actual
   * mappings.  Once it creates the scripts, it starts up a separate
   * thread running Vivado and tells it to run those scripts.
   * 
   * The associated main() routine in this class shows how to call it.
   * 
   * @param dir The directory to run in
   * @param libcellname The name of the library cell which is to be placed and analyzed
   * @param belname The name of the bel it is to be placed on
   * @param outfilename The name of the output file for the XML
   * @param properties A string consisting of alternating property keys and values
   * @param verbose Whether to echo commands sent to Vivado
   * @return A list of strings representing the results from the Vivado console
   */
  public List<String> createPinMappings(
                                        String dir, 
                                        String libcellname, 
		  String belname, 
		  String outfilename, 
		  String properties, 
		  boolean verbose
		  ) throws IOException {

	  BufferedWriter out = new BufferedWriter(new FileWriter("setup.tcl"));
	  out.write("set config_dict [dict create " + properties + "]\n");
	  out.write("set libcellname " + libcellname + "\n");
	  out.write("set belname " + belname + "\n");
	  out.write("set filename " + outfilename + "\n");
	  out.write("source create_nondefault_pin_mappings.tcl\n");
	  out.close();
	  VivadoConsole vc = new VivadoConsole(dir);
	  return doCmd(vc, "source setup.tcl", verbose);
  }

public static void main(String[] args) throws IOException {
    //vc.setTimeout(180000L);  // For long timeouts (3 seconds)
	System.out.println("Starting...");
    String properties = "WRITE_WIDTH_A 4 READ_WIDTH_A 4 WRITE_WIDTH_B 18 READ_WIDTH_B 18 RAM_MODE SDP";

    PinMappingsCreator pmc = new PinMappingsCreator();
    List<String> ret = pmc.createPinMappings("C:\\git\\RapidSmith2\\devices\\artix7\\pinMappings",
                                   "RAMB18E1", "RAMB18E1", "newMappings.xml",
                                   properties, false
                                   );
    for (String s : ret)
    	System.out.println(s);
    System.out.println("All done...");
  }

}
