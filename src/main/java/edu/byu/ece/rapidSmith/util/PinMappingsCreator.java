package edu.byu.ece.rapidSmith.util;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;

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
	private CellDesign design;
	private Element pinMappings;
	private Element pinMapProperties;

	public PinMappingsCreator(CellDesign design) {
		this.design = design;
		this.pinMappings = loadPinMappings();
		this.pinMapProperties= loadPinMapProperties();
	}
	
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
   * @param partname The name of the part to map the design to
   * @param libcellname The name of the library cell which is to be placed and analyzed
   * @param belname The name of the bel it is to be placed on
   * @param outfilename The name of the output file for the XML
   * @param properties A string consisting of alternating property keys and values
   * @param verbose Whether to echo commands sent to Vivado
   * @return A list of strings representing the results from the Vivado console
   */
  public List<String> createPinMappings(
                                        String dir,
                                        String partname,
                                        String libcellname, 
                                        String belname, 
                                        String properties, 
                                        boolean verbose
                                        ) throws IOException {

    BufferedWriter out = new BufferedWriter(new FileWriter("setup.tcl"));
    out.write("set config_dict [dict create " + properties + "]\n");
    out.write("set partname " + partname + "\n");
    out.write("set libcellname " + libcellname + "\n");
    out.write("set belname " + belname + "\n");
    out.write("set filename newMapping.xml" + "\n");
    out.write("source create_nondefault_pin_mappings.tcl\n");
    out.close();
    VivadoConsole vc = new VivadoConsole(dir);
    return doCmd(vc, "source setup.tcl", verbose);
  }

	public String buildHash(Cell cell) {
		String[] tmp = cell.getBel().getFullName().split("/");
		String belname = tmp[tmp.length-1];
		String hash = cell.getType() + " " + belname + " ";
		System.out.println("Hash1 = " + hash);

		// Build list of properties which contribute to the hash
		List<String> props = new ArrayList<String>();
		for (Element c : pinMapProperties.getChildren("cell")) {
			if (c.getAttributeValue("type").equals(cell.getType()) && 
					c.getAttributeValue("bel").equals(belname)) {
				for (Element p : c.getChildren("prop"))
					props.add(p.getAttributeValue("key"));
			}
		}
		System.out.println("Props: " + props);
		// Now, using that list, build hash
		for (String s : props) {
			hash += cell.getProperties().getValue(s) + " ";
		}
		System.out.println("Hash2 = " + hash);
		return hash;
	}
	
	public boolean isPinMapProperty(String key) {
		for (Element c : pinMapProperties.getChildren("cell")) {
			for (Element prop : c.getChildren("prop")) {
				if (key.equals(prop.getAttributeValue("key"))) 
					return true;
			}
		}
		return false;
	}
	
  public Element loadPinMapProperties() {
	  Document pinMapProperties = null;
	  try {
		pinMapProperties= RSEnvironment.defaultEnv().loadPinMapProperties(design.getFamily());
	} catch (JDOMException | IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  if (pinMapProperties == null)
		  return null;
	  else
		  return pinMapProperties.getRootElement();
  }
  
  public Element loadNewPinMapping() {
	  Document newPinMapping = null;
	  try {
		newPinMapping = RSEnvironment.defaultEnv().loadNewPinMapping(design.getFamily());
	} catch (JDOMException | IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}	  
	  if (newPinMapping==null)
		  return null;
	  else
		  return newPinMapping.getRootElement().getChild("cell");
  }
  
  public void addNewPinMapping() {
	Element newPinMapping = loadNewPinMapping();
	if (newPinMapping == null) 
		return;
	if (!isDuplicatePinMapping(newPinMapping)) {
		pinMappings.addContent(newPinMapping);
	}
  }
  
  public Element loadPinMappings() {
	  Document pm = null;
	  try {
		pm = RSEnvironment.defaultEnv().loadPinMappings(design.getFamily());
	} catch (JDOMException | IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}	  
	  if (pm==null)
		  return null;
	  else
		  return pm.getRootElement();
  }
  
  public void savePinMappings() {
	  try {
		RSEnvironment.defaultEnv().savePinMappings(design.getFamily(), pinMappings.getDocument());
	} catch (JDOMException | IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }

  public boolean isDuplicatePinMapping(Element newPinMapping) {
	  return (findPinMapping(newPinMapping) != null);
  }

  public boolean isDuplicatePinMapping(String hash) {
	  return (findPinMapping(hash) != null);
  }
  
  public Element findPinMapping(Element newPinMapping) {
	  String hash = newPinMapping.getAttributeValue("hash");
	  return findPinMapping(hash);
  }
  
  public Element findPinMapping(String hash) {
		for (Element p : pinMappings.getChildren("cell"))
			if (p.getAttributeValue("hash").equals(hash))
				return p;
		return null;
	}

	public static void printPinMappings(Element e, String fileName) {
		OutputStream os;
		try {
			os = new FileOutputStream(fileName);
			printPinMappings(e, os);
			os.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public static void printPinMappings(Element e, OutputStream os) {
				XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
		try {
			xout.output(e, os);
			os.close();
		} catch (IOException err) {
			// TODO Auto-generated catch block
			err.printStackTrace();
		}
	}
	
  public static void main(String[] args) throws IOException {
    //vc.setTimeout(180000L);  // For long timeouts (3 seconds)

	// Set the following parameters to do a sample run 
	String dirName = "C:\\git\\RapidSmith2\\devices\\artix7\\pinMappings";
	String libcellname = "RAMB18E1";
	String belname = "RAMB18E1";
	String partName = "xc7a100tcsg324";
    String properties = "WRITE_WIDTH_A 4 READ_WIDTH_A 4 WRITE_WIDTH_B 18 READ_WIDTH_B 18 RAM_MODE SDP";
    
        System.out.println("Starting...");

    // No design specified so make a temp one and add it   
    if (args.length < 1) {
    	CellDesign des = new CellDesign("pmcDesign", partName);
    	PinMappingsCreator pmc = new PinMappingsCreator(des);
    	List<String> ret = pmc.createPinMappings(dirName, partName, libcellname, belname, properties, false);
    	for (String s : ret)
    		System.out.println(s);
    	pmc.addNewPinMapping();
    }
    else { // Design was specified, compute hash and see if it is a duplicate
    	System.out.println("Loading design: " + args[0] + ".rscp");
    	CellDesign des = VivadoInterface.loadRSCP(args[0] + ".rscp").getDesign();
    	Cell cell = null;
    	for (Cell c : des.getCells()) {
    		if (c.getType().startsWith("FIFO")) {
    			cell = c;
    			break;
    		}
    		if (c.getType().startsWith("RAMB")) {
    			cell = c;
    			break;
    		}
    	}
    	if (cell == null) {
    		System.out.println("No RAMB or FIFO cells found in design, exiting.");
    	}
    	else {
    		System.out.println("Cell: " + cell.getName() + " " + cell.getType());
    		PinMappingsCreator pmc = new PinMappingsCreator(des);
    		String hash = pmc.buildHash(cell);
    		if (!pmc.isDuplicatePinMapping(hash)) {
    			List<String> ret = pmc.createPinMappings(dirName, partName, libcellname, belname, properties, false);
    			for (String s : ret)
    				System.out.println(s);
    			pmc.addNewPinMapping();
    		}
    	}
    }

    System.out.println("All done...");
  }

}

class SortProperties implements Comparator<Element> {
    public int compare(Element e1, Element e2) {
    	return e1.getAttributeValue("key").compareTo(e2.getAttributeValue("key"));
    }
}
class SortPins implements Comparator<Element> {
    public int compare(Element e1, Element e2) {
    	int r = e1.getAttributeValue("cellPin").compareTo(e2.getAttributeValue("cellPin"));
    	if (r != 0) return r;
    	return e1.getAttributeValue("belPin").compareTo(e2.getAttributeValue("belPin"));
    }
}
