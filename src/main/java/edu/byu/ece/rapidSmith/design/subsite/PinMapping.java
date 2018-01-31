package edu.byu.ece.rapidSmith.design.subsite;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.byu.ece.rapidSmith.RSEnvironment;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.AbstractDesign;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.FamilyType;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.rapidSmith.util.PinMappingsCreator;
import edu.byu.ece.rapidSmith.util.VivadoConsole;

/**
 * Represents the pin mappings for a single cell/bel/properties combination. 
 *
 * @author Brent Nelson
 */
public class PinMapping {
	
	private static Map<FamilyType, Map<String, PinMapping>> pinMappings = null;
	private static Map<FamilyType, Map<String, List<String>>> pinMapProperties= null;

	/**
	 * Return the family-specific pin mapping properties. Fetch from XML file if needed.
	 * @param family The family associated with the pin mapping properties desired.
	 * @return The pin mapping properties requested, if they exist. 
	 */
	private static Map<String, List<String>> getPinMapProperties(FamilyType family) {
		if (pinMapProperties == null)
			pinMapProperties = new HashMap<FamilyType, Map<String, List<String>>>();
		if (pinMapProperties.get(family) == null)
			pinMapProperties.put(family, loadPinMapProperties(family));
		return pinMapProperties.get(family);
	}
	
	/**
	 * Return the family-specific pin mappings. Fetch from XML file if needed.
	 * @param family The family associated with the pin mappingsdesired.
	 * @return The pin mappings requested, if they exist. 
	 */
	private static Map<String, PinMapping> getPinMappings(FamilyType family) {
		if (pinMappings== null)
			pinMappings= new HashMap<FamilyType, Map<String, PinMapping>>();
		if (pinMappings.get(family) == null)
			pinMappings.put(family, loadPinMappings(family));
		return pinMappings.get(family);
	}

  /**
   * Load the pin mappings associated with a family.  
   * @family The family the pin mappings are associated with (best obtained via design.getFamily()).
   * @return A map from hash values to PinMapping objects.
   */	private static Map<String, PinMapping> loadPinMappings(FamilyType family) {
		  Element pm = null;
	  	try {	
			pm = RSEnvironment.defaultEnv().loadPinMappings(family).getRootElement();
		} catch (JDOMException | IOException e) {
		// 	TODO Auto-generated catch block
			e.printStackTrace();
		}	  
	  	if (pm==null)
		  	return null;
	  	else {
	  		Map<String, PinMapping> ret = new HashMap<String, PinMapping>();
	  		for (Element e : pm.getChildren("cell"))
	  			ret.put(e.getAttributeValue("hash"), new PinMapping(e));
	  		return ret;
	  	}
  	}
  
  /**
   * Load the pin mapping properties associated with a family.  
   * @family The family the pin map properties are associated with (best obtained via design.getFamily()).
   * @return pinMapProperties 
   */	
   private static Map<String, List<String>> loadPinMapProperties(FamilyType family) {
	   Element pm = null;
	   try {	
		   pm = RSEnvironment.defaultEnv().loadPinMapProperties(family).getRootElement();
	   } catch (JDOMException | IOException e) {
		   // 	TODO Auto-generated catch block
		   e.printStackTrace();
	   }	  	// 
	   if (pm==null)
		   return null; 
	   else {
		   Map<String, List<String>> ret = new HashMap<String, List<String>>();
		   for (Element e : pm.getChildren("cell")) {
			   List<String> props = new ArrayList<String>();
			   String key = e.getAttributeValue("type") + " " + e.getAttributeValue("bel");
			   for (Element c : e.getChildren("prop")) 
				   props.add(c.getAttributeValue("key"));
			   //System.out.println("Key = " + key + " props = " + props);
			   ret.put(key, props);
		   }
		   return ret;
	   }
   }

   // These variable are per
	private String cellname;
	private String belname;
	private String hash;
	private Map<String, String> props; 
	private Map<String, List<String>> pins; 

	private String getCellname() {
		return cellname;
	}
	private String getBelname() {
		return belname;
	}
	private String getHash() {
		return hash;
	}
	private Map<String, String> getProps() {
		return props;
	}
	private Map<String, List<String>> getPins() {
		return pins;
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

	public static PinMapping createPinMappingFromPath(Path path) throws JDOMException, IOException {
		SAXBuilder builder = new SAXBuilder();
		Document d = builder.build(path.toFile());
		Element e = d.getRootElement().getChild("cell");
		printPinMappings(e, System.out);
		return new PinMapping(e);
	}
	
	public PinMapping(Element e) {
		if (e != null) {
			cellname = e.getAttributeValue("type");
			belname= e.getAttributeValue("bel");
			hash= e.getAttributeValue("hash");
			System.out.println("1: " + cellname + " " + belname + " " + hash);
			
			props = new HashMap<String, String>();
			for (Element p : e.getChild("properties").getChildren("property"))
				props.put(p.getAttributeValue("key"), p.getAttributeValue("val"));
			
			pins = new HashMap<String, List<String>>();
			Element ps = e.getChild("pins");
			for (Element pp : ps.getChildren("pin")) {
				String cp = pp.getAttributeValue("cellPin"); 
				String bp = pp.getAttributeValue("belPin");
				//System.out.println("Got: " + cp + ":" + bp);
				List<String> bps = null;
				if (pins.get(cp) == null) 
					bps = new ArrayList<String>();
				else
					bps = pins.get(cp);
				//System.out.println("Adding " + bp + " to " + bps);
				bps.add(bp);
				Collections.sort(bps);
				pins.put(cp, bps);
			}
		}
		else
			System.out.println("WARNING: cannot construct new PinMapping object with NULL parameter.");
	}
	
	private String pinsToString(String s) {
		for (Map.Entry<String, List<String>> entry : pins.entrySet()) 
			s += "  " + entry.getKey() + ":" + entry.getValue().toString() + "\n";
		return s;
	}
	
	public String toString() {
		String s = "";
		s = "Hash:[ " + hash + " ]\n";
		s += "Properties:\n";
		for (Map.Entry<String, String> entry : props.entrySet()) 
			s += "  " + entry.getKey() + ":" + entry.getValue() + "\n";
		s += "Pins:\n";
		s = pinsToString(s);
		return s;
	}
	
	private boolean equalHashes(PinMapping newPinMap) {
		return hash.equals(newPinMap.getHash());
	}

	private boolean equalPins(PinMapping newPinMap) {
		return (pins.equals(newPinMap.getPins()));
	}

	private static Element buildDOMPinMappings(Map<String, PinMapping> pinmappings) {
		Element root = new Element("cells");
		for (Map.Entry<String, PinMapping> entry : pinmappings.entrySet()) { 
			PinMapping pm = entry.getValue();
			Element e_c = new Element("cell");
			e_c.setAttribute("type", pm.getCellname());
			e_c.setAttribute("bel", pm.getBelname());
			e_c.setAttribute("hash", pm.getHash());

			Element e_properties = new Element("properties");
			e_c.addContent(e_properties);
			Element e_pins= new Element("pins");
			e_c.addContent(e_pins);
			

			for (Map.Entry<String, String> pentry : pm.getProps().entrySet()) {
				String key = pentry.getKey();
				String val = pentry.getValue();
				Element tmp = new Element("property");
				tmp.setAttribute("key", key);
				tmp.setAttribute("val", val);
				e_properties.addContent(tmp);
			}
			e_properties.sortChildren(new SortProperties());

			for (Map.Entry<String, List<String>> pentry : pm.getPins().entrySet()) {
				String cp= pentry.getKey();
				List<String> bps = pentry.getValue();
				for (String s : bps) {
					Element tmp = new Element("pin");
					tmp.setAttribute("cellPin", cp);
					tmp.setAttribute("belPin", s);
					e_pins.addContent(tmp);
				}
			}
			e_pins.sortChildren(new SortPins());
		}
		printPinMappings(root, System.out);
		return root;
		
	}

	/**
	 * Write a list of pin mappings to the proper directory as pinMappings.xml
	 * @param family The FPGA family for the pinmappings (they are family-specific).  
	 * This determines the directory the pinMappings.xml file is saved in. 
	 * @param pinmappings The list of pinmappings to convert to DOM2 and write to the XML file. 
	 */
	private static void savePinMappings(FamilyType family, Map<String, PinMapping> pinmappings) {
	  try {
		  RSEnvironment.defaultEnv().savePinMappings(family, buildDOMPinMappings(pinmappings));
	  } catch (JDOMException | IOException e) {
		  // TODO Auto-generated catch block
		  e.printStackTrace();
	  }
	}
	
	private static String buildHashForCell(Cell cell, Bel bel, FamilyType family) {
		getPinMappings(family);
		getPinMapProperties(family);
		String[] tmp = bel.getFullName().split("/");
		String belname = tmp[tmp.length-1];
		String partialhash = cell.getType() + " " + belname;
		String hash = partialhash;
		//System.out.println("Hash = [" + hash + "]");
		List<String> props = getPinMapProperties(family).get(partialhash);
		if (props == null)
			return null;
		for (String s : props)
			hash += " " + cell.getProperties().getValue(s).toString();
		return hash;
	}
	
	/**
	 * Given a cell and a bel to place it onto, retrieve the pin mappings for this (if it exists)
	 * @param cell The cell of interest
	 * @param bel The bel for it to be placed on
	 * @param family The family associated with the design
	 * @return A map of pin mappings for this potential placement
	 */
	public static Map<String, List<String>> findPinMappingForCell(Cell cell, Bel bel, FamilyType family) {
		
		getPinMapProperties(cell.getDesign().getFamily());
		String hash = buildHashForCell(cell, bel, family);
		PinMapping pm = getPinMappings(family).get(hash); 
		if (pm == null) return null;
		return pm.getPins();
	}
	
	private static List<String> doCmd(VivadoConsole vc, String cmd, boolean verbose) {
		if (verbose) 
			System.out.println("Running command: " + cmd);
		return vc.runCommand(cmd);
	}

	public static List<String> createPinMappings(
			Path path,
			FamilyType family, 
			String partname,
			Cell cell,
			Bel bel, 
			boolean verbose
			) throws IOException, JDOMException {
		
		getPinMapProperties(family);
		String[] tmp = bel.getFullName().split("/");
		String belname = tmp[tmp.length-1];
		String partialhash = cell.getType() + " " + belname;
		List<String> props = getPinMapProperties(family).get(partialhash);
		String propstring = "";
		for (String p : props) {
			propstring += p + " " + cell.getProperties().getValue(p) + " ";
		}
		
		BufferedWriter out = new BufferedWriter(new FileWriter(path.resolve("setup.tcl").toString()));
		out.write("set config_dict [dict create " + propstring + "]\n");
		out.write("set partname " + partname + "\n");
		out.write("set libcellname " + cell.getType()+ "\n");
		out.write("set belname " + belname + "\n");
		out.write("set filename newMapping.xml" + "\n");
		out.write("source create_nondefault_pin_mappings.tcl\n");
		out.close();
		VivadoConsole vc = new VivadoConsole(path.toString());
		//List<String> results = doCmd(vc, "source setup.tcl", verbose);
		List<String> results = null;
		
		// If all goes well the file newMapping.xml will be created in the pinMappings subdirectory of the architecture directory
		// Now, let's load it in and add it to the cache
		PinMapping pm = createPinMappingFromPath(path.resolve("newMapping.xml"));
		Map<String, PinMapping> pms = pinMappings.get(family);
		pms.put(pm.getHash(), pm);
		savePinMappings(family, pms);
		return results;
  }
	public static void main(String[] args) throws IOException, JDOMException {
		if (args.length < 1) {
			System.err.println("Usage: PinMapping tincrCheckpointName");
			System.exit(1);
		}
		
		// Load a TINCR checkpoint.  Then, find any RAMB* or FIFO* cells in it.  
		// For each of these consult the pin mappings cache and, if they exist, 
		// print out the mappings associated with the cell. 
		System.out.println("Loading design " + args[0] + "...");
		VivadoCheckpoint vcp = VivadoInterface.loadRSCP(args[0]);
		CellDesign design = vcp.getDesign();
		for (Cell c : design.getCells()) {
			if (c.getType().startsWith("RAMB") || c.getType().startsWith("FIFO")) {
				// The limitation of following lines of code is that this cell is 
				// already placed and so we know the bel.  In reality, you will 
				// usually be asking the question regarding a potential cell placement 
				// onto a  bel. 
				Map<String, List<String>> pms = findPinMappingForCell(
						c, 
						c.getBel(), 
						design.getFamily());
				System.out.println("Pin mappings for placing cell: " + c + " onto bel: " + c.getBel() + " =");
				System.out.println("  Hash for this is: " + buildHashForCell(c, c.getBel(), design.getFamily()));
				if (pms == null) {
					System.out.println("    None found.  Will now generate it and add to pin mappings cache.");
					System.out.println("    This will require this program to run Vivado (should be on your path).");
					System.out.println("    Once it is done, re-run this program and it should be found in the cache.");
					Path path = RSEnvironment.defaultEnv().getPartFolderPath(design.getFamily()).resolve("pinMappings");
    				createPinMappings(path, 
    						design.getFamily(),
    						design.getPartName(), 
    						c, 
    						c.getBel(), 
    						true);
					
				}
				else
					for (Map.Entry<String, List<String>> pentry : pms.entrySet()) {
						System.out.println("  " + pentry.getKey() + " -> " + pentry.getValue());
					}
			}
		}
		System.out.println("Done...");
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
