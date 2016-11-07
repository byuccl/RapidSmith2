import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;

import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.interfaces.vivado.TincrCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.rapidSmith.util.VivadoConsole;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class ImportTestJunit4 {
	
	private static VivadoConsole console;
	private static TincrCheckpoint tcp;
	private String checkpointName;
	
	private static final String TCP_LOCATION = "C:\\Users\\ecestudent\\Documents\\Research\\Tincr\\TestTCP\\";
	private static final String DCP_LOCATION = "C:/Users/ecestudent/Documents/Research/Tincr/TestDCP/";
	private static final String vivadoRunDirectory = "C:\\Users\\ecestudent\\Documents\\Research\\Tincr\\VivadoStart";
	// TODO: implement the tests like this
	//private static final String TEST_FILES ="";
	
	public ImportTestJunit4(String checkpointName) {
		super();
		this.checkpointName = checkpointName;
		loadTCP(checkpointName);
		loadDCP(checkpointName);
	}
	
	private void loadTCP(String checkpointName) {
		
		String tcpCheckpointFile = TCP_LOCATION + checkpointName + ".tcp";
		try {
			tcp = VivadoInterface.loadTCP(tcpCheckpointFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadDCP(String checkpointName) {
		
		console = new VivadoConsole(vivadoRunDirectory);
		String dcpCheckpointFile = DCP_LOCATION + checkpointName + ".dcp";
		console.runCommand("open_checkpoint " + dcpCheckpointFile);
	}
	
	@Test
	public void testCellPlacement() {
		System.out.println("Running Placement Test for: " + checkpointName);
		CellDesign design = tcp.getDesign();
		
		for (Cell cell: design.getCells()) {
		
			if (cell.isGndSource() || cell.isVccSource()) {
				continue;
			}
			
			String command = null;
			if (cell.isPort()) {
				String portSite = (cell.getAnchorSite() == null) ? null : cell.getAnchorSite().getName();
				command = String.format("tincr::test_port_placement %s %s", cell.getName(), portSite);
			}
			else {
				String belName = cell.getAnchor() == null ? "" : cell.getAnchor().getFullName();
				command = String.format("tincr::test_cell_placement %s %s", cell.getName(), belName);
			}
			
			List<String> result = console.runCommand(command); 
			if (!result.get(0).equals("1")) {
				fail(String.format("Current Test: %s\nInvalid cell placement for cell: %s\n"
					+ "Expected: %s Actual: %s", checkpointName, cell.getName(), result.get(1), result.get(2)));
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Parameters(name="{0}")
    public static Collection testCases() {
		
		return Arrays.asList(
			"superCounter",
			"count16"
		);
    }
	
	@AfterClass
	public static void cleanup() {
		console.close(false);
		tcp = null;
		console = null;
	}
}
