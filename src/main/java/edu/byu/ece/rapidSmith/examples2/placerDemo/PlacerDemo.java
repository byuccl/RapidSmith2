package edu.byu.ece.rapidSmith.examples2.placerDemo;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import edu.byu.ece.edif.core.EdifNameConflictException;
import edu.byu.ece.edif.core.InvalidEdifNameException;
import edu.byu.ece.edif.util.parse.ParseException;
import edu.byu.ece.rapidSmith.RapidSmithEnv;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.interfaces.vivado.TincrCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.examples2.placerDemo.SimulatedAnnealingPlacer;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * Simulated Annealing placer demo for FPL. The demo can be run in interactive mode (-I) <br>
 * which will show placement updates at certain stages of the annealing (10% acceptance rate for example). <br>
 * If the interactive mode is disabled, then the placer will run as usual. 
 * 
 * [NOTE]: If you run the placer in interactive mode, the moves/second figure will be incorrect because <br>
 * the time spent looking at the checkpoints in Vivado will be included in the total runtime. TODO: fix this. 
 * 
 * Usage: placerTest -I -v <Vivado run directory> <TINCR checkpoint>
 * 
 * TODO: Extract the placer demo into its own class, and create an instance here. This is fine for now though. 
 *
 * @author Thomas Townsend
 *
 */
public class PlacerDemo {

	public static final String PART_NAME = "xc7a100tcsg324";
	public static final String CANONICAL_PART_NAME = "xc7a100tcsg324";
	public static final String CELL_LIBRARY = "cellLibrary.xml";
	
	private static Device device;
	
	public static void classSetup() throws IOException {
		device = RapidSmithEnv.getDefaultEnv().getDevice(CANONICAL_PART_NAME);
	}
	
	//List of Benchmarks
	//-------------
	// seven_seg.tcp (629_checkpoints)
	// bram_dsp.tcp
	// tx.tcp
	// cordic.tcp
	// leon3.tcp
	// fht.tcp
	// diffeq2.tcp
	// hilbert.tcp
	// vga_chargen.tcp (629_checkpoints/Final)
	// jpeg.tcp 
	// fir.tcp
	// counter16bit.tcp (Research/Tincr)
	// 5bit_adder.tcp
	public static void main(String[] args) throws IOException, ParseException, EdifNameConflictException, InvalidEdifNameException {
		
		// Parse the input arguments
		ArrayList<String> pathArgs = new ArrayList<String>();
		boolean interactiveMode = parseArgs(args, pathArgs);
		String tcpDirectory = pathArgs.get(0);
		String vivadoInstanceDirectory = pathArgs.size() == 2 ? pathArgs.get(1) : ".";
		
		// Load device and design
		System.out.println("Loading Device...");
		classSetup();
		
		System.out.println("Loading Design...");			
		TincrCheckpoint tcp = VivadoInterface.loadTCP(tcpDirectory);
		CellDesign design = tcp.getDesign();
		
		// create a stream to vivado if in interactive mode
		BufferedWriter out = (interactiveMode) ? createVivadoOutputStream(tcpDirectory, vivadoInstanceDirectory) : null;
		
		// Run the placer
		System.out.println("Placing Design...");
		SimulatedAnnealingPlacer placer = new SimulatedAnnealingPlacer(device, design);
		if (interactiveMode) {
			placer.setVivadoOutputStream(out, tcpDirectory);
		}
		placer.placeDesign();
		
		// Export the design to a TCP file
		System.out.println("Exporting Placed Design...");
		VivadoInterface.writeTCP(tcpDirectory, design, tcp.getDevice(), tcp.getLibCells());
		System.out.println("Successfully added placement constraints to TINCR checkpoint: " + tcpDirectory);
		
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
		parser.nonOptions("TINCR checkpoint").ofType(String.class);
		parser.acceptsAll(Arrays.asList("interactive", "I"), "Interactive Mode. In this mode, an instance of Vivado will be created, "
									+ "and placer progress will be displayed at certain increments of the placer process");
		parser.acceptsAll(Arrays.asList("vivado","v"), "Directory to run Vivado if interactive mode is enabled").withRequiredArg();
		
		OptionSet options = null;
		try {
			options = parser.parse(args);
		} catch (OptionException e) {
			System.out.println("Hello");
			parser.printHelpOn(System.err);
			System.exit(-1);
		}
		
		if (options.nonOptionArguments().size() != 1) {
			System.out.println(options.nonOptionArguments().size());
			parser.printHelpOn(System.err);
			System.exit(-1);
		}
		
		// add the arguments
		outputArgs.add((String) options.nonOptionArguments().get(0));
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
		
		// read the netlist and the constraints file for the tcp, and loas the design
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
