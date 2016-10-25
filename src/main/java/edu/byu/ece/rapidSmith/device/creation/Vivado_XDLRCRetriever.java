package edu.byu.ece.rapidSmith.device.creation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.util.FamilyType;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 *	TODO: In the future it would be nice if this could be integrated with TINCR and Vivado,
 *			and call the TINCR code to create an XDLRC, but for now just hardcode the artix value. 
 */
public class Vivado_XDLRCRetriever implements XDLRCRetriever {

	@Override
	public List<String> getPartsInFamily(FamilyType family) {
		// TODO Auto-generated method stub
		
		List<String> parts = new ArrayList<>();
		parts.add("xc7a100tcsg324");
		return parts;
	}

	@Override
	public Path getXDLRCFileForPart(String part) {
		Path xdlrcFile = RSEnvironment.defaultEnv().getPartFolderPath(part).resolve(part + "_full.xdlrc");
		
		//if the file doesn't exist, then throw an error and 
		if (!Files.isRegularFile(xdlrcFile)) {
			MessageGenerator.briefErrorAndExit("ERROR: XDLRC file " + xdlrcFile + " does not exist. Generate this file before continuing.");
		}
		return xdlrcFile;
	}

	@Override
	public void cleanupXDLRCFile(String part, Path filePath) {
		// TODO create an option in the installer / DeviceFilesCreator to optionally delete the XDLRC after a device file has been generated
		
	}

}
