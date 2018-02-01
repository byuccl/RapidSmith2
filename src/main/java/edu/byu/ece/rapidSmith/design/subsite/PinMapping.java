package edu.byu.ece.rapidSmith.design.subsite;

import java.io.BufferedWriter;
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
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.rapidSmith.util.VivadoConsole;

/**
 * Represents the pin mappings for a single cell/bel/properties combination. 
 *
 * @author Brent Nelson
 */
public class PinMapping {
	
	// These are static to ensure they will load from disk only once through the public get functions 
	private static Map<String, PinMapping> pinMappings = null;
	private static Map<String, List<String>> pinMapProperties= null;

   // These variable are per object
	private String cellName;
	private String belName;
	private String hash;
	private Map<String, String> props; 
	private Map<String, List<String>> pins; 

	public String getCellName() {
		return cellName;
	}
	public String getBelName() {
		return belName;
	}
	public String getHash() {
		return hash;
	}
	public Map<String, String> getProps() {
		return props;
	}
	public Map<String, List<String>> getPins() {
		return pins;
	}

	private static Map<String, List<String>> getPinMapProperties(FamilyType family) {
		if (pinMapProperties == null)
			pinMapProperties = loadPinMapProperties(family);
		return pinMapProperties;
	}
	
	private static Map<String, PinMapping> getPinMappings(FamilyType family) {
		if (pinMappings== null)
			pinMappings = loadPinMappings(family);
		return pinMappings;
	}

	private static Map<String, PinMapping> loadPinMappings(FamilyType family) {
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
			   ret.put(key, props);
		   }
		   return ret;
	   }
   }

   private static void printPinMappings(Element e, OutputStream os) throws IOException {
		XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
		xout.output(e, os);
	}
	private static void printPinMappings(Element e, String fileName) throws IOException {
		OutputStream os;
		os = new FileOutputStream(fileName);
		printPinMappings(e, os);
		os.close();
	}

	private static PinMapping loadNewPinMapping(FamilyType family) throws JDOMException, IOException {
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
		else
			System.out.println("WARNING: cannot construct new PinMapping object with NULL parameter.");
	}
	
	private String pinsToString(String s) {
		for (Map.Entry<String, List<String>> entry : pins.entrySet()) 
			s += "  " + entry.getKey() + ":" + entry.getValue().toString() + "\n";
		return s;
	}
	
	private boolean equalHashes(PinMapping newPinMap) {
		return hash.equals(newPinMap.getHash());
	}

	private boolean equalPins(PinMapping newPinMap) {
		return (pins.equals(newPinMap.getPins()));
	}

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
		return root;
		
	}

	private static void savePinMappings(FamilyType family, Map<String, PinMapping> pinmappings) {
	  try {
		  RSEnvironment.defaultEnv().savePinMappings(family, buildDOMPinMappings(pinmappings));
	  } catch (JDOMException | IOException e) {
		  // TODO Auto-generated catch block
		  e.printStackTrace();
	  }
	}
	
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
	 * @param cell The cell to be placed
	 * @param belName The bel the cell is to be placed on
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
		out.write("set libcellName " + cell.getType()+ "\n");
		out.write("set belName " + belName + "\n");
		out.write("set filename newMapping.xml" + "\n");
		out.write("source create_nondefault_pin_mappings.tcl\n");
		out.close();
		VivadoConsole vc = new VivadoConsole(path.toString());
		List<String> results = doCmd(vc, "source setup.tcl", verbose);
		//List<String> results = null;
		
		// If all goes well the file newMapping.xml will be created in the pinMappings subdirectory of the device architecture directory
		// Now, let's load it in and add it to the cache
		PinMapping pm = loadNewPinMapping(family);
		
		// Get the pinsmappings from the cache for this family
		Map<String, PinMapping> pms = getPinMappings(family);
		
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
		s += "Pins:\n";
		s = pinsToString(s);
		return s;
	}
	
	/**
	 * Given a cell and a bel type to place it onto, retrieve the pin mappings for this (if it exists)
	 * @param cell The cell of interest
	 * @param bel The name of the bel for it to be placed on
	 * @return A map of pin mappings for this potential placement
	 */
	public static PinMapping findPinMappingForCell(Cell cell, String bel) {
		FamilyType family = cell.getDesign().getFamily();
		getPinMapProperties(cell.getDesign().getFamily());
		String hash = buildHashForCell(cell, bel);
		PinMapping pm = getPinMappings(family).get(hash); 
		if (pm == null) return null;
		return pm;
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
				PinMapping pm = findPinMappingForCell(
						c, 
						c.getBel().getName());
				System.out.println("Pin mappings for placing cell: " + c + " onto bel: " + c.getBel() + " =");
				System.out.println("  Hash for this is: " + buildHashForCell(c, c.getBel().getName()));
				if (pm == null) {
					System.out.println("    None found.  Will now generate it and add to pin mappings cache.");
					System.out.println("    This will require this program to run Vivado (should be on your path).");
					System.out.println("    Once it is done, re-run this program and it should be found in the cache.");
    				createPinMappings( 
    						c, 
    						c.getBel().getName(), 
    						true);
					
				}
				else
					for (Map.Entry<String, List<String>> pentry : pm.getPins().entrySet()) {
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
