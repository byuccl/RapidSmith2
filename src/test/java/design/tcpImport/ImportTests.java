package design.tcpImport;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.interfaces.vivado.TincrCheckpoint;
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
	
	@Test
	@DisplayName("Count16 Import Test")
	public void count16Test() throws IOException {
		VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("count16.tcp").toString());
	}
	
	@Test
	@DisplayName("BramDSP Import Test")
	public void bramdspTest() throws IOException {
		VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("bramdsp.tcp").toString());
	}
	
	@Test
	@DisplayName("Cordic Import Test")
	public void cordicTest() throws IOException {
		VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("cordic.tcp").toString());
	}
	
	@Test
	@DisplayName("SuperCounter Import Test")
	public void superCounterTest() throws IOException {
		VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("superCounter.tcp").toString());
	}
	
	@Test
	@DisplayName("Leon3 Import Test")
	public void leon3Test() throws IOException {
		VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("leon3.tcp").toString());
	}
	
	@Test
	@DisplayName("Simon Import Test")
	public void simonTest() throws IOException {
		TincrCheckpoint tcp = VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("simon.tcp").toString());
		CellDesign design = tcp.getDesign();
		
		// test that internal properties were imported correctly
		Cell test1 = design.getCell("seq_ram_reg_0_127_0_0/DP.HIGH");
		assertEquals("64'h0000000000000000", test1.getProperties().getStringValue("INIT"), "Internal property INIT is not applied correctly");
		assertEquals("2'b10", test1.getProperties().getStringValue("RAM_ADDRESS_MASK"), "Internal property RAM_ADDRESS_MASK is not applied correctly");
		assertEquals("2'b11", test1.getProperties().getStringValue("RAM_ADDRESS_SPACE"), "Internal property RAM_ADDRESS_SPACE is not applied correctly");
	}
}
