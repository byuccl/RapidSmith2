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

package edu.byu.ece.rapidSmith.examples;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.device.BelPin;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class BramAnalyzer {
	static String[] designNames = {
/*			"ram_sdp_0_0",
			"ram_sdp_1_1",
			"ram_sdp_2_2",
			"ram_sdp_4_4",
			"ram_sdp_9_9",
			"ram_sdp_18_18",
			"ram_sdp_36_36",
			"ram_tdp_0_0",
			"ram_tdp_1_1",
			"ram_tdp_2_2",
			"ram_tdp_4_4",
			"ram_tdp_9_9",
			"ram_tdp_18_18",
			"ram_tdp_36_36",
*/			"fifo1", "fifo2", "fifo3"
	};
	
	    // part name and cell library  
	static private Document pinMappings;
	static private Document pinMapProperties;
	
	public static void main(String[] args) throws IOException, JDOMException {

		// Load a TINCR checkpoint
		//System.out.println("Loading Design...");
		for (String designName : designNames) {
			VivadoCheckpoint vcp = VivadoInterface.loadRSCP(designName + ".rscp");
			new BramAnalyzer(vcp.getDesign());
		}
		//printPinMappings(pinMappings, System.out);
		System.out.println("\nDone...");
	}
	
	public BramAnalyzer(CellDesign design) throws IOException, JDOMException {
		
		pinMappings = RSEnvironment.defaultEnv().loadPinMappings(design.getFamily());
		pinMapProperties= RSEnvironment.defaultEnv().loadPinMapProperties(design.getFamily());
		
		// Build pin mappings
		Element newMapping = buildPinMappings(design, true, design.getName());
		
		//printPinMappings(newMapping, System.out);

		// Compute hashes and add them
		//printPinMappings(newMapping, System.out);
		for (Element e : newMapping.getChildren()) {
			String hash = buildHash(e);
			e.setAttribute("hash", hash);
		
			// See if this mapping is already present
			if (!isDuplicateMapping(hash)) {
				System.out.println(design.getName() + " pin mappings: not duplicate, adding");
				pinMappings.getRootElement().addContent(e.clone());
				RSEnvironment.defaultEnv().savePinMappings(design.getFamily(), pinMappings);
			}
			else
				System.out.println(design.getName() + " pin mappings: duplicate, ignoring...");
		}
	}

	public Element buildPinMappings(CellDesign design, boolean cellBelPinMappings, String designName) {
		Element cells = new Element("cells");
		// Print the cells
		for (Cell c : design.getCells()) {
			if (c.getType().startsWith("RAMB") || c.getType().startsWith("FIFO")) {
				Element e = buildPinMappingForCell(c, cellBelPinMappings, designName);
				cells.addContent(e);
			}
		}
		return cells;
	}		
		
	public Element buildPinMappingForCell(Cell c, boolean cellBelPinMappings, String designName)
	{
		if (!c.isPlaced()) 
			return null;
		else {
			Element e_c = new Element("cell");
			e_c.setAttribute("type", c.getType());
			e_c.setAttribute("bel", c.getBel().getName());
			e_c.setAttribute("design", designName);
			e_c.setAttribute("instance", c.getName());
			
			Element e_properties = new Element("properties");
			e_c.addContent(e_properties);
			Element e_pins= new Element("pins");
			e_c.addContent(e_pins);
			

			List<Property> props = new ArrayList<Property>();
			for (Property p : c.getProperties()) 
				if (!p.getKey().startsWith("INIT_") && !p.getKey().startsWith("INITP_"))
					props.add(p);

			for (Property p : props) {
				String k = p.getKey();
				Object v = p.getValue();
//				if (!v.equals(c.getLibCell().getDefaultValue(p))) {
				if (isPinMapProperty(k)) {
					Element e_p = new Element("property");
					e_p.setAttribute("key", k.toString());
					e_p.setAttribute("val", v.toString());
					e_properties.addContent(e_p);
				}
			}
			e_properties.sortChildren(new SortProperties());

			for (CellPin cp : c.getPins()) {
				if (!c.isMacro()) {
					if (c.isPlaced()) {
						if (cellBelPinMappings) {
							for (BelPin bp1 : cp.getMappedBelPins()) {
								Element e_pin = new Element("pin");
								e_pin.setAttribute("cellPin", cp.getName());
								e_pin.setAttribute("belPin", bp1.getName());
								e_pins.addContent(e_pin);
							}
						}
					}
				}
			}
			e_pins.sortChildren(new SortPins());
			return e_c;
		}
	}

	public boolean isDuplicateMapping(String hash) {
		Element root = pinMappings.getRootElement();
		for (Element p : root.getChildren("cell"))
			if (p.getAttributeValue("hash").equals(hash))
				return true;
		return false;
	}
	
	public String buildHash(Element newMapping) {
		String hash = newMapping.getAttributeValue("type") + " " + newMapping.getAttributeValue("bel") + " ";

		// Build list of properties which contribute to the hash
		List<String> props = new ArrayList<String>();
		for (Element c : pinMapProperties.getRootElement().getChildren("cell")) {
			if (c.getAttributeValue("type").equals(newMapping.getAttributeValue("type")) && 
					c.getAttributeValue("bel").equals(newMapping.getAttributeValue("bel"))) {
				for (Element p : c.getChildren("prop"))
					props.add(p.getAttributeValue("key"));
			}
		}
		
		// Now, using that list, build hash
		for (String s : props) {
			for (Element p : newMapping.getChild("properties").getChildren()) {
				if (p.getAttributeValue("key").equals(s))
					hash += p.getAttributeValue("val") + " ";
			}
		}
		return hash;
	}
	
	public static void printPinMappings(Document d, OutputStream os) {
		printPinMappings(d.getRootElement(), os);
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
	
	public boolean isPinMapProperty(String key) {
		Element root = pinMapProperties.getRootElement();
		for (Element c : root.getChildren("cell")) {
			for (Element prop : c.getChildren("prop")) {
				if (key.equals(prop.getAttributeValue("key"))) 
					return true;
			}
		}
		return false;
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
