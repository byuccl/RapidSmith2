package edu.byu.ece.rapidSmith.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeoutException;

/**
 * Spawns a new child instance of Vivado in TCL mode and provides methods
 * to send TCL commands to the newly created process. The results of each command are
 * returned in a buffer as strings. In order for this class to operate correctly, the Vivado
 * executable must be on the user's PATH and TINCR must be installed. By default, 
 * commands will thrown a {@link TimeoutException} if they take longer than 1 minute to return
 * a result. You can change this with {@link VivadoConsole#setTimeout(long)}<br>
 *  
 * <br>
 * NOTE: Nested commands are currently not supported due to how the TCL interpreter works. <br>
 * For example: {@code "get_bels -of [get_cells seq_cnt_en_TMR_VOTER_1]"}
 * will return an error or timeout. Instead, create your own function in TCL to do this:  
 * <pre>
 * <code>
 * proc get_bel_of_cell {cell} { 
 *     return [get_bels -of [get_cells $cell]] 
 * } 
 * </code>
 * </pre> 
 * TINCR offers several instructions to minimize the number of nested instructions
 * you may need to send
 *
 * @author Thomas Townsend
 */
public class VivadoConsole {
	
	/** Thread that monitors the output of Vivado and stores it into a return buffer*/
	private VivadoReadThread readThread;
	/** Vivado child process*/
	private Process vivadoProcess;
	/** Output buffer to write Vivado commands to*/
	private BufferedWriter out;
	/** Buffer that vivado output will be stored into*/
	private Queue<String> returnBuffer;
	/** How long to wait for a Vivado result before throwing a timeout exception */
	private static long timeout = 60000;
	/** Variable marked by the read thread to mark the output from vivado as valid*/
	private boolean resultValid;
	
	/**
	 * Creates a new VivadoConsole object, and initializes it to run
	 * in the current directory. 
	 */
	public VivadoConsole() { 
		// Default is to run Vivado in the current directory.
		init(".", false);
	}
	
	/**
	 * Creates a new VivadoConsole object and initializes it to run
	 * in the specified directory. This is useful to prevent Vivado
	 * output files from cluttering up the current directory.
	 * @param runDirectory Directory to run Vivado
	 */
	public VivadoConsole(String runDirectory) {
		init(runDirectory, false);
	}
	
	/**
	 * Creates a new VivadoConsole object and initializes it to run
	 * in the specified directory. Also sets it to throw an exception
	 * if a sent TCL command caused produced an error.
	 * 
	 * @param runDirectory Directory to run Vivado
	 * @param throwOnError <code>true</code> if you want to throw an exception
	 * 					when a TCL error occurs. <code>false</code> otherwise.
	 */
	public VivadoConsole(String runDirectory, boolean throwOnError) {
		init(runDirectory, throwOnError);
	}
	
	/**
	 * Initializes the VivadoConsole object. 
	 * 
	 * @param runDirectory Directory to run the Vivado process in
	 */
	private void init(String runDirectory, boolean throwOnError) {
		returnBuffer = new LinkedList<>();
		createVivadoProcess(runDirectory);
		// start the thread that reads Vivado's output
		readThread = new VivadoReadThread(this, vivadoProcess, throwOnError);
		readThread.start();
		// get the output buffer from the Vivado process 
		out = new BufferedWriter(new OutputStreamWriter(vivadoProcess.getOutputStream()));
	}
	
	/**
	 * Starts a new instance of Vivado in the specified directory. All output
	 * files produced from vivado will be written to that directory.
	 * 
	 * @param runDirectory Directory to run Vivado in
	 */
	private void createVivadoProcess(String runDirectory) {
		
		// TODO: update this to run in linux and windows correctly
		final ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.redirectErrorStream(true);
		processBuilder.directory(new File(runDirectory));
		
		String osType = System.getProperty("os.name").toLowerCase();
		String vivadoCommand = "vivado";
		if (osType.contains("win")) {
			vivadoCommand += ".bat";
		}
		processBuilder.command(vivadoCommand, "-mode", "tcl");
		
		try {
			vivadoProcess = processBuilder.start();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Exceptions.EnvironmentException("Cannot find vivado executable on PATH.");
		}
	}

	/**
	 * Sends a TCL command to be executed in Vivado, and returns the result. 
	 * NOTE: If there was an error in Vivado, a string will be returned that starts
	 * with ERROR. 
	 *  
	 * @param cmd Command to run
	 * @return List of strings that was returned from Vivado. 
	 */
	public synchronized List<String> runCommand(String cmd) {		
		
		resultValid = false;
		String command = "::tincr::run_rapidsmith_command \"" + cmd + "\" \n";   
		try {
			out.write(command);
			out.flush();
		} catch (IOException e1) {
			System.err.println("Error writing command to Vivado: " + cmd);
			e1.printStackTrace();
		}
		
		// wait until the command has finished execution before continuing 	
		waitForResult(cmd);
					
		// Read the result from the return buffer and store it into a list
		List<String> returnList;
		synchronized (returnBuffer) {
			returnList = new ArrayList<>();
			while (!returnBuffer.isEmpty()) {
				returnList.add(returnBuffer.poll());
			}
		}
				
		return returnList;
	}
	
	/**
	 * Waits {@code timeout} milliseconds for Vivado to return a result of a
	 * command. If timeout occurs, a timeout exception is thrown.
	 * 
	 */
	private synchronized void waitForResult(String cmd) {
		
		try {	
			wait(timeout);
			
			if (!resultValid) {
				throw new TimeoutException("Command \"" + cmd +  "\" timed out.");
			}
			
		} catch (InterruptedException | TimeoutException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Clears the return buffer. The buffer should always be
	 * cleared when a new instruction starts
	 */
	private synchronized void clearReturnBuffer() {
		returnBuffer.clear();
	}
	
	/**
	 * Adds a string to the result buffer
	 * 
	 * @param line String to add to the result buffer
	 */
	private synchronized void bufferVivadoOutput(String line) {
		returnBuffer.add(line);
	}
	
	/**
	 * Marks the Vivado output as valid. This is used to check if the read thread
	 * notified the console that the output was valid, or we timed out.s
	 * 
	 * @param resultValid
	 */
	private synchronized void setResultValid(boolean resultValid) {
		this.resultValid = resultValid;
	}
	
	@SuppressWarnings("unused")
	private synchronized void notifyReadThread() {
		// After consuming all of the data, notify the read thread it can continue operation
		synchronized (readThread) {
			readThread.notify();
		}
	}
	
	/**
	 * Closes the connection to Vivado and the Vivado process.
	 */
	public synchronized void close(boolean blockUntilProcessExits) {
		
		try {
			out.write("exit\n");
			out.flush();
		} catch (IOException e1) {
			System.err.println("Error closing Vivado console!");
			e1.printStackTrace();
		}
		
		// Block the running thread until the process exits
		if (blockUntilProcessExits) {
			while (vivadoProcess.isAlive());
		}
	}
		
	/**
	 * Set the amount of time that a the console will wait for output
	 * before timing out. If a command runs out of time, an exception
	 * is thrown. The default timeout is 60 seconds (60,000 milliseconds).
	 * If you want the console to wait an unlimited amount of time for a response, set
	 * the timeout to 0. 
	 * 
	 * @param timoutMilliseconds The timeout period in milliseconds. 0 is you don't the console to timeout.
	 */
	public static void setTimeout(long timoutMilliseconds) {
		timeout = timoutMilliseconds;
	}
	
	/**
	 * Nested class that monitors the output of Vivado, and stores any returned results into a
	 * buffer to be processed.
	 * 
	 * @author Thomas Townsend
	 */
	private class VivadoReadThread extends Thread {

		/** Stream Vivado writes its output to*/
		private BufferedReader inputStream; 
		/** Vivado Console object associated with this readThread*/
		private VivadoConsole console;
		/** Boolean value setting if we should throw an exception when a TCL command results in an error*/
		private boolean throwExceptionOnError;
		/** Boolean variable used to signal when the thread should stop*/
		
		/**
		 * Creates a new {@code VivadoReadThread} object that monitors the output of 
		 * the specified {@code VivadoConsole}. 
		 * @param console Vivado console to monitor
		 * @param process Vivado process to get the {@code InputStream} from  
		 */
		public VivadoReadThread(VivadoConsole console, Process process, boolean throwOnError) {
			this.console = console;
			this.inputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
			this.setDaemon(true);
			this.throwExceptionOnError = throwOnError;
		}
		
		/**
		 * Listens to the output of the member {@code VivadoConsole} and buffers Vivado 
		 * output into a buffer queue.
		 */
		@Override
		public void run() {
			try {
				String line;
				while ((line = inputStream.readLine()) != null) {
					if (line.equals("rs_start")) {
						clearReturnBuffer();
					}
					else if (line.equals("rs_end")) {
						notifyConsole();
					} 
					else if (line.startsWith("ERROR")) {
						if (throwExceptionOnError) {
							throw new TclErrorException(line);
						}
						notifyConsole();
					}
					else {
						bufferReturnLine(line);
					}
				}
			} catch (IOException e) {
				System.err.println("Error reading Vivado output stream!");
				e.printStackTrace();
			}
		}
		
		/**
		 * Waits for the VivadoConsole object to signal that
		 * it has consumed the return data from Vivado.
		 */
		@SuppressWarnings("unused")
		private synchronized void waitForReply() {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Notifies the VivadoConsole that the command has completed
		 * running and to check for output.
		 */
		private synchronized void notifyConsole() {
			synchronized(console) {
				console.setResultValid(true);
				console.notify();
			}
		}
				
		/**
		 * Clears the VivadoConsole return buffer once a new command
		 * has been issued. 
		 */
		private synchronized void clearReturnBuffer() {
			synchronized (console) {
				console.clearReturnBuffer();
			}
		}
		
		/**
		 * Adds a {@code String} to the return buffer of the 
		 * VivadoConsole 
		 * 
		 * @param line String to buffer
		 */
		private synchronized void bufferReturnLine(String line) {
			synchronized (console) {
				console.bufferVivadoOutput(line);
			}
		}
	}
	
	/**
	 * Exception thrown if a TCL command sent from RapidSmith to
	 * Vivado causes an error.
	 * 
	 * @author Thomas Townsend
	 */
	private class TclErrorException extends RuntimeException {
		
		/** Generated unique serial ID for this class*/
		private static final long serialVersionUID = 2043238892707009333L;

		public TclErrorException(String message) {
			super(message);
		}
	}
}
