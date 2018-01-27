package edu.byu.ece.rapidSmith.design.subsite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;

import edu.byu.ece.rapidSmith.util.PinMappingsCreator;

/**
 * Represents the pin mappings for a single cell/bel/properties combination. 
 *
 * @author Brent Nelson
 */
public class PinMapping {
	private String cellname;
	private String belname;
	private String hash;
	private Map<String, String> props; 
	private Map<String, List<String>> pins; 

	public String getCellname() {
		return cellname;
	}
	public String getBelname() {
		return belname;
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

	public PinMapping(Element e) {
		if (e != null) {
			cellname = e.getAttributeValue("type");
			belname= e.getAttributeValue("bel");
			hash= e.getAttributeValue("hash");
			//System.out.println("1: " + cellname + " " + belname + " " + hash);
			
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
			System.out.println("WARNING: cannot construct PinMapping object with NULL parameter.");
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
	
	public boolean equalHashes(PinMapping pm) {
		return hash.equals(pm.getHash());
	}

	public boolean equalPins(PinMapping m1) {
		String s1 = pinsToString(null);
		String s2 = m1.pinsToString(null);
		return (s1.equals(s2)); 
	}
	

	public static void main(String[] args) {
		// Create a new temporary design
		CellDesign des = new CellDesign("pmcDesign", "xc7a100tcsg324");
		// Creating this object will load both the pinMappings.xml and pinMapProperties.xml file into the object
    	PinMappingsCreator pmc = new PinMappingsCreator(des);
		// Load a newPinMapping.xml file if it exists in the devices/artix7/pinMappings directory  	
    	Element npm = pmc.loadNewPinMapping();
    	if (npm == null) { 
    		System.out.println("No new pin mapping found");
    		System.exit(1);
    	}
    	// Look to see if this is a duplicate mapping or not (based on hashes)
    	Element dup = pmc.findPinMapping(npm);
    	if (dup == null)
    		System.out.println("newPinMapping is NOT a duplicate");
    	else {
    		System.out.println("newPinMapping IS a duplicate");
    		// All of the above was done with jdom Element data structures.
    		// Now, convert these to PinMapping objects
    		PinMapping pm1 = new PinMapping(npm);
    		PinMapping pm2 = new PinMapping(dup);
    		// Now ask if the new objects have equalHashes and equal pin mappings
    		// By hand modifying the newMapping.xml file you can test that the equalPins() 
    		// routine works and is not dependent on things being in a sorted order
    		System.out.println("Are the hashes equal? " + pm1.equalHashes(pm2));
    		System.out.println("Are the actual pin mappings equal? " + pm1.equalPins(pm2));
    	}
	}

}
