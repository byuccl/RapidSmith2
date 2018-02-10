package edu.byu.ece.rapidSmith.design.subsite;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.byu.ece.rapidSmith.RSEnvironment;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import edu.byu.ece.rapidSmith.device.FamilyType;
import edu.byu.ece.rapidSmith.util.VivadoConsole;

/**
 * Represents the pin mappings for a single cell/bel/properties combination. 
 *
 * @author Brent Nelson
 */
public class PinMapping {
	
	public static final String PIN_MAPPINGS_FILENAME = "pinMappings.xml";
	public static final String NEW_PIN_MAPPINGS_FILENAME = "newMapping.xml";
	public static final String PIN_MAP_PROPERTIES_FILENAME = "pinMapProperties.xml";

	// These are static to ensure they will load from disk only once through the public get functions 
	private static Map<String, PinMapping> pinMappings = null;
	private static Map<String, List<String>> pinMapProperties= null;

   // These variables are per object
	private String cellName;
	private String belName = null;
	private String hash = null;
	private String duplic = null;
	private Map<String, String> props = null; 
	private Map<String, List<String>> pins = null; 

	public String getCellName() {
		return cellName;
	}
	public String getBelName() {
		return belName;
	}
	public String getHash() {
		return hash;
	}
	public String getDuplic() {
		return duplic;
	}
	public Map<String, String> getProps() {
		return props;
	}
	public boolean hasDuplic() {
		return (duplic != null);
	}

	/**
	 * Get the pins of this mapping
	 * @return The pins for this mapping
	 */
	public Map<String, List<String>> getPins() {
		if (hasDuplic()) {
			String key = getDuplic();
			if (pinMappings == null) {
				System.out.println("ERROR: must first load pinMappings before calling getPins()");
				return null;
			}
			else 
				return pinMappings.get(key).getPins();
		}
		else
			return pins;
	}

	// Load the pin map properties from disk if needed.  Then return them.
	private static Map<String, List<String>> getPinMapProperties(FamilyType family) {
		if (pinMapProperties == null)
			pinMapProperties = loadPinMapProperties(family);
		return pinMapProperties;
	}
	
	// Load the pin mappings from disk if needed.  Then return them.
	private static Map<String, PinMapping> getPinMappings(FamilyType family) {
		if (pinMappings== null)
			pinMappings = loadPinMappings(family);
		return pinMappings;
	}

	/**
	 * Load the pinmappings for a family from disk.
	 * @param family The name of the family
	 * @return A map of the pin mappings
	 */
	public static Map<String, PinMapping> loadPinMappings(FamilyType family) {
		  Element pm = null;
	  	try {
	  		Path path = RSEnvironment.defaultEnv().getPartFolderPath(family).resolve(PIN_MAPPINGS_FILENAME);
	  		File tmp = new File(path.toString());
	  		if (!tmp.exists()) {
	  			System.out.println("Pin mappings file doesn't exist, creating: " + path.toString());
	  			FileWriter out = new FileWriter(path.toString());
	  			out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<cells>\n</cells>\n");
	  			out.close();
	  		}
	  		SAXBuilder builder = new SAXBuilder();
			pm = builder.build(path.toFile()).getRootElement();
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
	 * Load the pinmap properties for a family from disk.  This file tells, for a given cell/bel combo, which properties "matter" - 
	 * that is, which properties affect the pin mappings.
	 * @param family The name of the family
	 * @return A map of the pin map properties.
	 */
   public static Map<String, List<String>> loadPinMapProperties(FamilyType family) {
	   Element pm = null;
	   try {	
		   Path path = RSEnvironment.defaultEnv().getPartFolderPath(family).resolve(PIN_MAP_PROPERTIES_FILENAME);
		   SAXBuilder builder = new SAXBuilder();
		   pm = builder.build(path.toFile()).getRootElement();
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
			   ret.put(key, props);
		   }
		   return ret;
	   }
   }

   // Useful for debugging the JDOM representation but otherwise not used
   private static void printPinMappings(Element e, OutputStream os) throws IOException {
		XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
		xout.output(e, os);
	}
   
   // Useful for debugging the JDOM representation but otherwise not used
	private static void printPinMappings(Element e, String fileName) throws IOException {
		OutputStream os;
		os = new FileOutputStream(fileName);
		printPinMappings(e, os);
		os.close();
	}

	/**
	 * Load pin mappings for a single cell/bel combo from disk.  To be used to combined with the overall cache.
	 * @param family The family for the part.  Needed to determine the directory to place the file into.
	 * @return The pin mappings loaded from disk.
	 */
	public static PinMapping loadNewPinMapping(FamilyType family) throws JDOMException, IOException {
		Path path = RSEnvironment.defaultEnv().getPartFolderPath(family).resolve("pinMappings").resolve("newMapping.xml");		
		SAXBuilder builder = new SAXBuilder();
		Document d = builder.build(path.toFile());
		Element e = d.getRootElement().getChild("cell");
		PinMapping pm = new PinMapping(e);
		return pm;	
	}
	
	private PinMapping(Element e) {
		if (e != null) {
			cellName = e.getAttributeValue("type");
			belName= e.getAttributeValue("bel");
			hash= e.getAttributeValue("hash");
			
			props = new HashMap<String, String>();
			for (Element p : e.getChild("properties").getChildren("property"))
				props.put(p.getAttributeValue("key"), p.getAttributeValue("val"));
			
			Element dup = e.getChild("duplic");
			if (dup != null) {
				duplic = dup.getAttributeValue("hash");
				System.out.println("Found dup: " + duplic); 
				pins = null;	
			}
			else {
				pins = new HashMap<String, List<String>>();
				Element ps = e.getChild("pins");
				for (Element pp : ps.getChildren("pin")) {
					String cp = pp.getAttributeValue("cellPin"); 
					String bp = pp.getAttributeValue("belPin");
					List<String> bps = null;
					if (pins.get(cp) == null) 
						bps = new ArrayList<String>();
					else
						bps = pins.get(cp);
					bps.add(bp);
					Collections.sort(bps);
					pins.put(cp, bps);
				}
			}
		}
		else
			System.out.println("WARNING: cannot construct new PinMapping object with NULL parameter.");
	}
	
	// Convert the list of pins to a string.  Useful for debugging/visualization.
	private String pinsToString(String s) {
		for (Map.Entry<String, List<String>> entry : pins.entrySet()) 
			s += "  " + entry.getKey() + ":" + entry.getValue().toString() + "\n";
		return s;
	}
	
	// Convert the pinmappings data structure to a JDOM representation which can be written to disk
	private static Element buildDOMPinMappings(Map<String, PinMapping> pinmappings) throws IOException {
		Element root = new Element("cells");
		for (Map.Entry<String, PinMapping> entry : pinmappings.entrySet()) { 
			PinMapping pm = entry.getValue();
			Element e_c = new Element("cell");
			root.addContent(e_c);
			e_c.setAttribute("type", pm.getCellName());
			e_c.setAttribute("bel", pm.getBelName());
			e_c.setAttribute("hash", pm.getHash());
			
			Element e_properties = new Element("properties");
			e_c.addContent(e_properties);
			for (Map.Entry<String, String> pentry : pm.getProps().entrySet()) {
				String key = pentry.getKey();
				String val = pentry.getValue();
				Element tmp = new Element("property");
				tmp.setAttribute("key", key);
				tmp.setAttribute("val", val);
				e_properties.addContent(tmp);
			}
			e_properties.sortChildren(new SortProperties());

			if (pm.hasDuplic()) {
				Element e_dup = new Element("duplic");
				e_c.addContent(e_dup);
				e_dup.setAttribute("hash", pm.getDuplic());
			}
			else {	
				Element e_pins= new Element("pins");
				e_c.addContent(e_pins);

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
		}
		return root;
	}

	/**
	 * Save the specified pin mappings to disk.
	 * @param family The family for the part.  Needed to determine the directory to place the file into.
	 * @param pinmappings The pin mappings to be saved.
	 */
	public static void savePinMappings(FamilyType family, Map<String, PinMapping> pinmappings) {
	  try {
		  Element e = buildDOMPinMappings(pinmappings);
		  Path path = RSEnvironment.defaultEnv().getPartFolderPath(family).resolve(PIN_MAPPINGS_FILENAME);
		  OutputStream os = new FileOutputStream(path.toString());
		  XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
		  xout.output(e, os);
		  os.close();
	  } catch (IOException e) {
		  // TODO Auto-generated catch block
		  e.printStackTrace();
	  }
	}
	
	// Create a hash string for a cell and the name of a bel it is to be placed onto.
	// The cell is a handle to a real cell with its properties set as desired.
	// The belName is not a full bel name but rather the name of the type, as in RAMB18E1.
	private static String buildHashForCell(Cell cell, String belName) {
		FamilyType family = cell.getDesign().getFamily();
		getPinMappings(family);
		getPinMapProperties(family);
		String partialhash = cell.getType() + " " + belName;
		String hash = partialhash;
		List<String> props = getPinMapProperties(family).get(partialhash);
		if (props == null)
			return null;
		for (String s : props)
			hash += " " + cell.getProperties().getValue(s).toString();
		return hash;
	}
	
	// Run a command through the VivadoConsole.  
	private static List<String> doCmd(VivadoConsole vc, String cmd, boolean verbose) {
		if (verbose) 
			System.out.println("Running command: " + cmd);
		List<String> ret = vc.runCommand(cmd);
		if (verbose)
			System.out.println(ret);
		return ret;
	}

	/**
	 * Run Vivado using the VivadoConsole to place a cell onto a bel and then record the pin mappings.  
	 * The resulting pinmappings will be placed into a file such as: "$RAPIDSMITH_PATH/devices/artix7/pinMappings/newMapping.xml"
	 * @param cell The {@link Cell} to be placed
	 * @param belName The name of the bel the cell is to be placed on
	 * @param verbose Whether to echo the commands being executed in Vivado and the results
	 * @return The results from running Vivado
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static List<String> createPinMappings(
			Cell cell,
			String belName, 
			boolean verbose
			) throws IOException, JDOMException {
		

		FamilyType family = cell.getDesign().getFamily(); 
		Path path = RSEnvironment.defaultEnv().getPartFolderPath(family).resolve("pinMappings");
		String partname = cell.getDesign().getPartName();
		String partialhash = cell.getType() + " " + belName;
		List<String> props = getPinMapProperties(family).get(partialhash);
		String propstring = "";
		for (String p : props) {
			propstring += p + " " + cell.getProperties().getValue(p) + " ";
		}
		
		BufferedWriter out = new BufferedWriter(new FileWriter(path.resolve("setup.tcl").toString()));
		out.write("set config_dict [dict create " + propstring + "]\n");
		out.write("set partname " + partname + "\n");
		out.write("set libcellname " + cell.getType()+ "\n");
		out.write("set belname " + belName + "\n");
		out.write("set filename newMapping.xml" + "\n");
		out.write("source create_nondefault_pin_mappings.tcl\n");
		out.close();
		VivadoConsole vc = new VivadoConsole(path.toString());
		List<String> results = doCmd(vc, "source setup.tcl", verbose);
		//List<String> results = null;
		if (verbose)
			for (String s : results)
				System.out.println(s);
		
		// If all goes well the file newMapping.xml will be created in the pinMappings subdirectory of the device architecture directory
		// Now, let' load it in and add it to the cache
		PinMapping pm = loadNewPinMapping(family);
		
		// Get the pinsmappings from the cache for this family
		Map<String, PinMapping> pms = getPinMappings(family);
		
		// See if the pinmappings are a duplicate of another already in the cache
		// If so, point them to the other entry using the duplic member
		// Note: at this point pm.hasDuplic() == false
		for (Map.Entry<String, PinMapping> entry : pms.entrySet()) {
			String hash = entry.getKey();
			PinMapping pm2 = entry.getValue();
			if (pm.getPins().equals(pm2.getPins())) {
				pm.pins = null;
				pm.duplic = hash;
			}
		}
		
		// Add the new mapping to the mappings for this family
		pms.put(pm.getHash(), pm);
		
		savePinMappings(family, pms);
		return results;
	}
	
	public String toString() {
		String s = "";
		s = "Hash:[ " + hash + " ]\n";
		s += "Properties:\n";
		for (Map.Entry<String, String> entry : props.entrySet()) 
			s += "  " + entry.getKey() + ":" + entry.getValue() + "\n";
		if (hasDuplic()) {
			s += "Duplic: " + getDuplic();
		}
		else {
			s += "Pins:\n";
			s = pinsToString(s);
		}
		return s;
	}
	
	/**
	 * Given a cell and a bel type to place it onto, retrieve the pin mappings for this (if it exists)
	 * @param cell The {@link Cell} of interest
	 * @param bel The name of the bel for it to be placed on
	 * @return A map of pin mappings for this potential placement
	 */
	public static PinMapping findPinMappingForCell(Cell cell, String bel) {
		FamilyType family = cell.getDesign().getFamily();
		getPinMapProperties(family);
		String hash = buildHashForCell(cell, bel);
		PinMapping pm = getPinMappings(family).get(hash); 
		if (pm == null) return null;
		return pm;
	}
	
}

// Helper class for the sorting required above
class SortProperties implements Comparator<Element> {
    public int compare(Element e1, Element e2) {
    	return e1.getAttributeValue("key").compareTo(e2.getAttributeValue("key"));
    }
}

// Helper class for the sorting required above
class SortPins implements Comparator<Element> {
    public int compare(Element e1, Element e2) {
    	int r = e1.getAttributeValue("cellPin").compareTo(e2.getAttributeValue("cellPin"));
    	if (r != 0) return r;
    	return e1.getAttributeValue("belPin").compareTo(e2.getAttributeValue("belPin"));
    }
}
