package edu.byu.ece.rapidSmith.examples;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jdom2.JDOMException;

import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;

public class TestDeepCopy {

	public static void main(String[] args) throws IOException, JDOMException {
		String rscpName = "";
		
		if (args.length < 1) {
			System.out.print("Enter RSCP name (without extension): ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			rscpName = br.readLine();
			if (!rscpName.equals("all"))
				processDesign(rscpName + ".rscp");
			else {
				processDesign("add.rscp");
				processDesign("count16.rscp");
				processDesign("cordic.rscp");
				processDesign("regfile.rscp");
				processDesign("simon.rscp");
			}
		}
		if (args.length == 1)
			processDesign(args[0]);
	}
	
	public static void processDesign(String rscpName) throws IOException {

		// Load a checkpoint
		System.out.println("Loading Design: " + rscpName);
		VivadoCheckpoint vcp = VivadoInterface.loadRSCP(rscpName);
		CellDesign design = vcp.getDesign();
		
		DesignAnalyzer.prettyPrintDesign(design);
		//for (Cell c: design.getCells()) {
			//if (c.isMacro()) {
				//System.out.println("  Macro: " + c.getName());
				//for (Cell cc : c.getInternalCells()) 
					//System.out.println("    Internal: " + cc.getName());
			//}
		//}
		
		System.out.println("Deep copy ...");
		CellDesign newdes = design.deepCopy();
		
		DesignAnalyzer.prettyPrintDesign(newdes);
		
		//System.out.println("Design compare...");
		design.compare(newdes);
		
		System.out.println("Done...");

	// Print <const0> and <const1> nets
		for (CellNet n : design.getNets())
			if (n.getName().equals("<const0>")) 
				System.out.println(n.getName());
			else if (n.getName().equals("<const1>")) 
				System.out.println(n.getName());
	}


}
