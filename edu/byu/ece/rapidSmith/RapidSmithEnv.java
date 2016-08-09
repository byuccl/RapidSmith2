package edu.byu.ece.rapidSmith;

import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.util.FamilyType;
import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.util.MessageGenerator;
import edu.byu.ece.rapidSmith.util.PartNameTools;
import edu.byu.ece.rapidSmith.util.EnvironmentException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class RapidSmithEnv {
	/** Environment Variable Name which points to the rapidSmith project on disk */
	public static final String rapidSmithPathVariableName = "RAPIDSMITH_PATH";
	/** Suffix of the device part files */
	public static final String deviceFileSuffix = "_db.dat";
	/** Name of extended family information */
	public static final String familyInfoFileName = "familyInfo.xml";
	/** The default environment */
	private static final RapidSmithEnv defaultEnv = new RapidSmithEnv();

	private Path devicePath;
	private Path sourcePath;
	private Map<String, SoftReference<Device>> loadedDevices = new HashMap<>();

	public static RapidSmithEnv getDefaultEnv() {
		return defaultEnv;
	}

	public RapidSmithEnv() {
		if (System.getProperty("RAPIDSMITH_PATH") != null) {
			String property = System.getProperty("RAPIDSMITH_PATH");
			devicePath = Paths.get(property, "devices");
			sourcePath = Paths.get(property);
		} else if (System.getenv("RAPIDSMITH_PATH") != null) {
			String property = System.getenv("RAPIDSMITH_PATH");
			devicePath = Paths.get(property, "devices");
			sourcePath = Paths.get(property);
		}
		else throw new EnvironmentException("RAPIDSMITH_PATH environment variable is not set.");
	}

	public RapidSmithEnv(Map<String, Path> variables) {
		this();
		for (Map.Entry<String, Path> e : variables.entrySet()) {
			try {
				Field field = getClass().getDeclaredField(e.getKey());
				field.set(this, e.getValue());
			} catch (NoSuchFieldException | IllegalAccessException e1) {
				MessageGenerator.briefError("No such variable " + e.getKey());
			}
		}
	}

	public Path getDevicePath() {
		if (devicePath == null)
			MessageGenerator.briefErrorAndExit("No device path defined.");
		return devicePath;
	}

	public void setDevicePath(Path devicePath) {
		this.devicePath = devicePath;
	}

	public Path getSourcePath() {
		if (sourcePath == null)
			MessageGenerator.briefErrorAndExit("No device path defined.");
		return sourcePath;
	}

	public void setSourcePath(Path sourcePath) {
		this.sourcePath = sourcePath;
	}

	public Device getDevice(String partName) {
		return getDevice(partName, false);
	}

	public Device getDevice(String partName, boolean forceReload) {
		String canonicalName = PartNameTools.removeSpeedGrade(partName);

		Device device;
		if (!forceReload && loadedDevices.containsKey(canonicalName)) {
			device = loadedDevices.get(canonicalName).get();
			if (device != null)
				return device;
		}

		Path path = getDeviceFilePath(canonicalName);
		device = FileTools.loadDevice(path);
		if (device == null)
			return null;

		loadedDevices.put(canonicalName, new SoftReference<>(device));
		return device;
	}

	public Document loadFamilyInfo(String part) {
		FamilyType family = PartNameTools.getFamilyTypeFromPart(part);
		return loadFamilyInfo(family);
	}

	public Document loadFamilyInfo(FamilyType family) {
		Path path = getPartFolderPath(family).resolve(familyInfoFileName);
		SAXBuilder builder = new SAXBuilder();
		try {
			return builder.build(path.toFile());
		} catch (JDOMException | IOException e) {
			return null;
		}
	}

	/**
	 * Gets and returns the path of the folder where the part files resides for partName.
	 * @param partName Name of the part to get its corresponding folder path.
	 * @return The path of the folder where the parts files resides.
	 */
	public Path getPartFolderPath(String partName){
		return getPartFolderPath(PartNameTools.getFamilyTypeFromPart(partName));
	}

	/**
	 * Gets and returns the path of the folder where the family type resides.
	 * @param familyType The family type corresponding folder path.
	 * @return The path of the folder where the parts of familyType reside.
	 */
	public Path getPartFolderPath(FamilyType familyType){
		familyType = PartNameTools.getBaseTypeFromFamilyType(familyType);
		return devicePath.resolve(familyType.name().toLowerCase());
	}


	/**
	 * Checks for all device files present in the current RapidSmith path and returns
	 * a list of strings of those part names available to be used by the tool.
	 * @return A list of available Xilinx parts for use by the tools.
	 */
	public List<String> getAvailableParts(){
		ArrayList<String> allParts = new ArrayList<>();
		for (FamilyType family : getAvailableFamilies()) {
			allParts.addAll(getAvailableParts(family));
		}
		return allParts;
	}

	/**
	 * Checks for all device files present in the current RapidSmith family path and returns
	 * a list of strings of those part names available to be used by the tool within the specified family.
	 * @param type The specified family type.
	 * @return A list of available Xilinx parts for the given family type
	 */
	public List<String> getAvailableParts(FamilyType type){
		ArrayList<String> allParts = new ArrayList<>();
		String pattern = "_db.dat";
		Path devFamilyPath = devicePath.resolve(type.toString().toLowerCase());
		if (!Files.isDirectory(devFamilyPath))
			return allParts;
		try {
			for(Path partPath : Files.newDirectoryStream(devFamilyPath)) {
				String fileName = partPath.getFileName().toString();
				if(fileName.endsWith(pattern)){
					allParts.add(fileName.replace(pattern, ""));
				}
			}
		} catch (IOException ignored) {
		}
		return allParts;
	}

	/**
	 * This method returns an ArrayList of family types currently supported
	 * @return ArrayList of all family types installed
	 */
	public List<FamilyType> getAvailableFamilies() {
		List<FamilyType> allFamilies = new ArrayList<>();
		if (!Files.exists(devicePath) || !Files.isDirectory(devicePath))
			return allFamilies;
		try {
			for(Path subPath : Files.newDirectoryStream(devicePath)) {
				if (!Files.isDirectory(subPath))
					continue;
				try {
					FamilyType type = FamilyType.valueOf(subPath.getFileName().toString().toUpperCase());
					allFamilies.add(type);
				} catch (IllegalArgumentException ignored) {
				}
			}
		} catch (IOException ignored) {
		}

		return allFamilies;
	}

	/**
	 * Gets the device file path and name for the given partName.
	 * @param partName Name of the part to get corresponding device file for.
	 * @return The full path to the device file specified by partName.
	 */
	public Path getDeviceFilePath(String partName) {
		return getPartFolderPath(partName).resolve(
				PartNameTools.removeSpeedGrade(partName) + deviceFileSuffix);
	}

	public void writeCompressedDeviceFile(Device device) throws IOException {
		Path path = getDeviceFilePath(device.getPartName());
		FileTools.writeCompressedDeviceFile(device, path);
	}
}
