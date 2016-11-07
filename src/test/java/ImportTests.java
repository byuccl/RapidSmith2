import org.junit.runner.RunWith;
import org.junit.platform.runner.JUnitPlatform;

/**
 * This class holds TINCR checkpoint import tests.
 * 
 * @author Thomas Townsend
 */
public class ImportTests {
	
	@RunWith(JUnitPlatform.class)
	public static class SuperCounterTest extends ImportTest {
		@Override
		public String getCheckpointName() {
			return "superCounter";
		}
	}
	
	
	@RunWith(JUnitPlatform.class)
	public static class count16Test extends ImportTest {
		@Override
		public String getCheckpointName() {
			return "count16";
		}
	}
}