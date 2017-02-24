package design.tcpImport;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;

/**
 * This test class verifies that the test benchmarks are imported into RapidSmith without error.
 * These tests should pass before a Pull Request is issued against the RapidSmith
 * repository. Add a new test to this directory for each test benchmark that is added
 * to the src/test/resources folder. 
 */
@RunWith(JUnitPlatform.class)
public class ImportTest {
	private static final Path testDirectory = RSEnvironment.defaultEnv().getEnvironmentPath()
			.resolve("src")
			.resolve("test")
			.resolve("resources")
			.resolve("ImportTests");
	
	@Test
	@DisplayName("Count16 Import Error Test")
	public void count16Test() throws IOException {
		VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("count16.tcp").toString());
	}
	
	@Test
	@DisplayName("BramDSP Import Error Test")
	public void bramdspTest() throws IOException {
		VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("bramdsp.tcp").toString());
	}
	
	@Test
	@DisplayName("Cordic Import Error Test")
	public void cordicTest() throws IOException {
		VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("cordic.tcp").toString());
	}
	
	@Test
	@DisplayName("SuperCounter Import Error Test")
	public void superCounterTest() throws IOException {
		VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("superCounter.tcp").toString());
	}
	
	@Test
	@DisplayName("Leon3 Import Error Test")
	public void leon3Test() throws IOException {
		VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("leon3.tcp").toString());
	}
}
