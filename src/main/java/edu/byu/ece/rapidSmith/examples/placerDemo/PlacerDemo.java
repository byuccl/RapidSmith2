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

package edu.byu.ece.rapidSmith.examples.placerDemo;

import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.ImplementationMode;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.rapidSmith.util.MessageGenerator;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Simulated Annealing placer demo for FPL. The demo can be run in interactive mode (-I)
 * which will show placement updates at certain stages of the annealing (10% acceptance rate for example).
 * If the interactive mode is disabled, then the placer will run as usual. The placer does not support
 * designs with macro cells. To test the placer, run the "cordicPlaced.rscp" found in the "exampleVivadoDesigns"
 * directory.
 * <p>
 * [NOTE]: If you run the placer in interactive mode, the moves/second figure will be incorrect because <br>
 * the time spent looking at the checkpoints in Vivado will be included in the total runtime. TODO: fix this. 
 * <p>
 * Usage: placerTest RSCP TCP_write_location [-I] [-v Vivado_run_directory]
 * <p>
 * TODO: Extract the placer demo into its own class, and create an instance here. This is fine for now though. 
 *
 */
public class PlacerDemo {

	public static final String PART_NAME = "xc7a100tcsg324";
	public static final String CANONICAL_PART_NAME = "xc7a100tcsg324";
	public static final String CELL_LIBRARY = "cellLibrary.xml";
	
	private static Device device;
	
	//List of Benchmarks
	//-------------
	// seven_seg.rscp (629_checkpoints)
	// bram_dsp.rscp
	// tx.rscp
	// cordic.rscp
	// leon3.rscp
	// fht.rscp
	// diffeq2.rscp
	// hilbert.rscp
	// vga_chargen.rscp (629_checkpoints/Final)
	// jpeg.rscp 
	// fir.rscp
	// counter16bit.rscp (Research/Tincr)
	// 5bit_adder.rscp
	public static void main(String[] args) throws IOException {
		
		// Parse the input arguments
		ArrayList<String> pathArgs = new ArrayList<>();
		boolean interactiveMode = parseArgs(args, pathArgs);
		String rscpDirectory = pathArgs.get(0);
		String tcpDirectory = pathArgs.get(1);
		String vivadoInstanceDirectory = pathArgs.size() == 3 ? pathArgs.get(2) : ".";
		
		System.out.println("Loading Device and Design...");
		VivadoCheckpoint vcp = VivadoInterface.loadRSCP(rscpDirectory);
		device = vcp.getDevice();
		CellDesign design = vcp.getDesign();
			
		// create a stream to vivado if in interactive mode
		BufferedWriter out = (interactiveMode) ? createVivadoOutputStream(rscpDirectory, vivadoInstanceDirectory) : null;
		
		// Run the placer
		System.out.println("Placing Design...");
		SimulatedAnnealingPlacer placer = new SimulatedAnnealingPlacer(device, design);
		if (interactiveMode) {
			placer.setVivadoOutputStream(out, tcpDirectory);
		}
		placer.placeDesign();
		
		// Export the design to a TCP file
		System.out.println("Exporting Placed Design...");
		VivadoInterface.writeTCP(tcpDirectory, design, vcp.getDevice(), vcp.getLibCells());
		System.out.println("Successfully created placed TCP at: " + tcpDirectory);
		
		if (interactiveMode) {
			out.close();
		}
	}
	
	/*
	 * Parses and returns the program arguments
	 */
	private static boolean parseArgs(String[] args, ArrayList<String> outputArgs) throws IOException {
		
		// parse the options
		OptionParser parser = new OptionParser();
		parser.nonOptions("RapidSmith Checkpoint").ofType(String.class);
		parser.nonOptions("Output Directory").ofType(String.class);
		parser.acceptsAll(Arrays.asList("interactive", "I"), "Interactive Mode. In this mode, an instance of Vivado will be created, "
									+ "and placer progress will be displayed at certain increments of the placer process");
		parser.acceptsAll(Arrays.asList("vivado","v"), "Directory to run Vivado if interactive mode is enabled").withRequiredArg();
		
		OptionSet options = null;
		try {
			options = parser.parse(args);
		} catch (OptionException e) {
			parser.printHelpOn(System.err);
			System.exit(-1);
		}
		
		if (options.nonOptionArguments().size() != 2) {
			System.out.println(options.nonOptionArguments().size());
			parser.printHelpOn(System.err);
			System.exit(-1);
		}
		
		// add the arguments
		outputArgs.add((String) options.nonOptionArguments().get(0));
		outputArgs.add((String) options.nonOptionArguments().get(1));
		if(options.has("vivado")) {
			outputArgs.add((String)options.valueOf("vivado"));
		}		
		return options.has("interactive");
	}
	
	/*
	 * Creates and returns an output stream that can communicate with Vivado
	 */
	private static BufferedWriter createVivadoOutputStream(String tcpDirectory, String vivadoDirectory) throws IOException {
		System.out.println("Starting Vivado Instance... \n"
				+ "[NOTE] Wait until Vivado GUI appears (may take as long as one minute) and run \"stop_gui\" in the command prompt.\n"
				+ "       Then, continue running the placer;");
		
		Process vivadoProcess = getVivadoProcess(vivadoDirectory);
		
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(vivadoProcess.getOutputStream()));		
		
		// read the netlist and the constraints file for the rscp, and loas the design
		out.write("read_edif -quiet " + Paths.get(tcpDirectory, "netlist.edf").toString().replace("\\", "/") + "\n");
		out.flush();
		
		// TODO: Hardcoded the part for the demo...fix this to be more general
		out.write("link_design -quiet -part xc7a100tcsg324-3 \n");
		out.flush();
		
		out.write("read_xdc -quiet " + Paths.get(tcpDirectory,"constraints.xdc").toString().replace("\\", "/") + "\n");
		out.flush();
		
		out.write("start_gui\n");
		out.flush();
		
		MessageGenerator.promptToContinue();
		return out;
	}
	
	/*
	 * Creates a new Vivado process in the specified directory
	 */
	private static Process getVivadoProcess(String startDirectory) throws IOException {
		
		final ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.directory(new File(startDirectory));
		processBuilder.command("vivado.bat", "-mode", "tcl"); 
		
		Process process = processBuilder.start();
			
		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		
		// Run on a separate thread to not block the demo program thread
		Thread t1 = new Thread(() -> {
			try {
				String line;
				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		t1.start();
		
		return process;
	}
}
