package edu.byu.ece.rapidSmith.examples;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jdom2.JDOMException;

import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.PinMapping;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;

public class CreateDynamicPinMappings {
	public static void main(String[] args) throws IOException, JDOMException {
		if (args.length < 1) {
			System.err.println("Usage: PinMapping tincrCheckpointName");
			System.exit(1);
		}
		
		// Load a TINCR checkpoint.  Then, find any RAMB* or FIFO* cells in it since 
		// they are one of the few to have dynamic pin mappings.  
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
				PinMapping pm = PinMapping.findPinMappingForCell(
						c, 
						c.getBel().getName());
				System.out.println("Pin mappings for placing cell: " + c + " onto bel: " + c.getBel() + " =");
				System.out.println("  Hash for this is: " + PinMapping.buildHashForCell(c, c.getBel().getName()));
				if (pm == null) {
					System.out.println("    None found.  Will now generate it and add to pin mappings cache.");
					System.out.println("    This will require this program to run Vivado (should be on your path).");
					System.out.println("    Once it is done, re-run this program and it should be found in the cache.");
    				PinMapping.createPinMappings( 
    						c, 
    						c.getBel().getName(), 
    						true);
					
				}
				else {
					// See if this entry has its own unique pin mappings.
					if (pm.hasDuplic())   
						System.out.println("  Pins are duplicate of: " + pm.getDuplic() + "\n  They are:");
					// Regardless, getPins() will get the right ones
					for (Map.Entry<String, List<String>> pentry : pm.getPins().entrySet()) 
						System.out.println("  " + pentry.getKey() + " -> " + pentry.getValue());
				}
			}
		}
		System.out.println("Done...");
	}
}
