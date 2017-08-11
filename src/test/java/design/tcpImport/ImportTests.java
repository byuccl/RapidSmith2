package design.tcpImport;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.interfaces.vivado.EdifInterface;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;

/**
 * This test class verifies that the test benchmarks are imported into RapidSmith without error.
 * These tests should pass before a Pull Request is issued against the RapidSmith
 * repository. Add a new test to this directory for each test benchmark that is added
 * to the src/test/resources folder. 
 */
@RunWith(JUnitPlatform.class)
public class ImportTests {
	private static final Path testDirectory = RSEnvironment.defaultEnv().getEnvironmentPath()
			.resolve("src")
			.resolve("test")
			.resolve("resources")
			.resolve("ImportTests");
	
	/**
	 * Initializes the import tests by disabling warning messages 
	 * in the RapidSmith2 Edif parser.
	 */
	@BeforeAll
	public static void initializeClass() {
		EdifInterface.suppressWarnings(true);
	}
	
	@Test
	@DisplayName("Count16 Series7")
	public void count16Test() throws IOException {
		VivadoInterface.loadRSCP(testDirectory.resolve("RSCP").resolve("artix7").resolve("count16.rscp").toString());
	}
	
	@Test
	@DisplayName("BramDSP Series7")
	public void bramdspTest() throws IOException {
		VivadoInterface.loadRSCP(testDirectory.resolve("RSCP").resolve("artix7").resolve("bramdsp.rscp").toString());
	}
	
	@Test
	@DisplayName("Cordic Series7")
	public void cordicTest() throws IOException {
		VivadoInterface.loadRSCP(testDirectory.resolve("RSCP").resolve("artix7").resolve("cordic.rscp").toString());
	}
	
	@Test
	@DisplayName("SuperCounter Series7")
	public void superCounterTest() throws IOException {
		VivadoInterface.loadRSCP(testDirectory.resolve("RSCP").resolve("artix7").resolve("superCounter.rscp").toString());
	}
	
	@Test
	@DisplayName("Leon3 Series7")
	public void leon3Test() throws IOException {
		VivadoInterface.loadRSCP(testDirectory.resolve("RSCP").resolve("artix7").resolve("leon3.rscp").toString());
	}
	
	@Test
	@DisplayName("Simon Series7")
	public void simonTest() throws IOException {
		VivadoCheckpoint tcp = VivadoInterface.loadRSCP(testDirectory.resolve("RSCP").resolve("artix7").resolve("simon.rscp").toString());
		CellDesign design = tcp.getDesign();
		
		// test that internal properties were imported correctly
		Cell test1 = design.getCell("seq_ram_reg_0_127_0_0/DP.HIGH");
		assertEquals("64'h0000000000000000", test1.getProperties().getStringValue("INIT"), "Internal property INIT is not applied correctly");
		assertEquals("2'b10", test1.getProperties().getStringValue("RAM_ADDRESS_MASK"), "Internal property RAM_ADDRESS_MASK is not applied correctly");
		assertEquals("2'b11", test1.getProperties().getStringValue("RAM_ADDRESS_SPACE"), "Internal property RAM_ADDRESS_SPACE is not applied correctly");
	}
	
	@Test
	@DisplayName("Cordic UltraScale")
	public void cordicUltrascaleTest() throws IOException {
		VivadoInterface.loadRSCP(testDirectory.resolve("RSCP").resolve("kintexu").resolve("cordic.rscp").toString());
	}
	
	@Test
	@DisplayName("Canny UltraScale")
	public void cannyUltrascaleTest() throws IOException {
		VivadoInterface.loadRSCP(testDirectory.resolve("RSCP").resolve("kintexu").resolve("canny.rscp").toString());
	}
	
	@Test
	@DisplayName("CounterTMR UltraScale")
	public void counterTMRUltrascaleTest() throws IOException {
		VivadoInterface.loadRSCP(testDirectory.resolve("RSCP").resolve("kintexu").resolve("counterTmr.rscp").toString());
	}
	
	@Test
	@DisplayName("Flappy Bird UltraScale")
	public void flappyUltrascaleTest() throws IOException {
		VivadoInterface.loadRSCP(testDirectory.resolve("RSCP").resolve("kintexu").resolve("flappy.rscp").toString());
	}
	
	@Test
	@DisplayName("MSP430 UltraScale")
	public void msp430UltrascaleTest() throws IOException {
		VivadoInterface.loadRSCP(testDirectory.resolve("RSCP").resolve("kintexu").resolve("msp430.rscp").toString());
	}
	
	@Test
	@DisplayName("Viterbi UltraScale")
	public void viterbiUltrascaleTest() throws IOException {
		VivadoInterface.loadRSCP(testDirectory.resolve("RSCP").resolve("kintexu").resolve("viterbi.rscp").toString());
	}
}
