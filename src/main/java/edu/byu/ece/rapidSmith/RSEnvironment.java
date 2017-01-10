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

package edu.byu.ece.rapidSmith;

import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.FamilyType;
import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.util.PartNameTools;
import edu.byu.ece.rapidSmith.util.Exceptions.EnvironmentException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Class for accessing the RapidSmith environment.  The environment exists in a
 * directory in the users file system (usually specified by the "RAPIDSMITH_PATH"
 * environment variable).  A default environment is accessible with the
 * {@link #defaultEnv()} method.
 */
public class RSEnvironment {
	/** Environment Variable Name which points to the rapidSmith project on disk */
	public static final String RSPATH_ENV_VARIABLE = "RAPIDSMITH_PATH";
	/** Suffix of the device part files */
	public static final String DEVICE_FILE_SUFFIX = "_db.dat";
	/** Name of extended family information */
	public static final String FAMILY_INFO_FILENAME = "familyInfo.xml";
	/** The default environment */
	private static RSEnvironment defaultEnv;

	private final Path rsPath;
	private final Map<String, SoftReference<Device>> loadedDevices = new HashMap<>();
	private final Map<String, FamilyType> supportedParts = new HashMap<>();

	/**
	 * Returns the default RapidSmith environment.  Unless overwritten with
	 * {@link #setDefaultEnv(RSEnvironment)}, this environment will default to
	 * the environment returned by calling {@link #RSEnvironment()}.
	 * @return the default RapidSmith environment
	 * @throws EnvironmentException if the environment is not set and the default
	 *   environment cannot be obtained
	 */
	public static RSEnvironment defaultEnv() {
		if (defaultEnv == null)
			defaultEnv = new RSEnvironment();
		return defaultEnv;
	}

	/**
	 * Allows the user to change the default RapidSmith environment at runtime.
	 * @param env the RapidSmith environment to set as the default
	 */
	public static void setDefaultEnv(RSEnvironment env) {
		defaultEnv = env;
	}

	/**
	 * Returns a new RapidSmith environment based on the system "RAPIDSMITH_PATH"
	 * variable.  This method first looks for the variable in the JVM properties and if not
	 * found, searches the environment variables.  If the variable is not found in either
	 * location, this method throws an {@link EnvironmentException}.
	 *
	 * @throws EnvironmentException if the path to the RapidSmith environment cannot be
	 *   found
	 */
	public RSEnvironment() {
		if (System.getProperty(RSPATH_ENV_VARIABLE) != null) {
			String property = System.getProperty(RSPATH_ENV_VARIABLE);
			rsPath = Paths.get(property);
		} else if (System.getenv(RSPATH_ENV_VARIABLE) != null) {
			String property = System.getenv(RSPATH_ENV_VARIABLE);
			rsPath = Paths.get(property);
		} else {
			throw new EnvironmentException(RSPATH_ENV_VARIABLE +
					" environment variable is not set.");
		}
		loadSupportedParts();
	}

	/**
	 * Returns a new RapidSmith environment configured to the specified path.
	 * @param rapidsmithPath path of the environment's directory
	 */
	public RSEnvironment(Path rapidsmithPath) {
		Objects.requireNonNull(rapidsmithPath, "rapidsmithPath");
		rsPath = rapidsmithPath;
		loadSupportedParts();
	}

	private void loadSupportedParts() {
		for (FamilyType family : getAvailableFamilies()) {
			for (String part : getAvailableParts(family)) {
				supportedParts.put(part, family);
			}
		}
	}

	/**
	 * @return the root of this environment
	 */
	public Path getEnvironmentPath() {
		return rsPath;
	}

	/**
	 * @return the path to the devices in this environment
	 */
	public Path getDevicePath() {
		return rsPath.resolve("devices");
	}

	/**
	 * @return the path to the java source files
	 */
	public Path getJavaPath() {
		return rsPath.resolve("src").resolve("main").resolve("java");
	}

	/**
	 * @return the path to resource files used in RapidSmith (such as the image  folder
	 */
	public Path getResourcePath() {
		return rsPath.resolve("src").resolve("main").resolve("resources");
	}
	
	/**
	 * Returns the loaded device with the specified part name.  Once loaded, devices are
	 * cached for quick access.
	 *
	 * @param partName the name of the part to load
	 * @return the loaded device
	 */
	public Device getDevice(String partName) {
		return getDevice(partName, false);
	}

	/**
	 * Returns the loaded device with the specified part name.  Once loaded, devices are
	 * cached for quick access.
	 *
	 * @param partName name of the part to load
	 * @param forceReload if true, forces the part to be reloaded from disk
	 * @return the loaded device
	 */
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

	/**
	 * Loads the family info file for the specified family.  The family info file contains
	 * additional information not found in the XDLRC for creating device files.
	 *
	 * @param family family to get the family info file for
	 * @return the family info xml document
	 */
	public Document loadFamilyInfo(FamilyType family) throws JDOMException, IOException {
		Path path = getPartFolderPath(family).resolve(FAMILY_INFO_FILENAME);
		SAXBuilder builder = new SAXBuilder();
		return builder.build(path.toFile());
	}

	/**
	 * Returns the path of the folder where the device file resides for {@code partName}.
	 *
	 * @param partName name of the part to get its corresponding folder path.
	 * @return the path of the folder where the parts files resides
	 */
	public Path getPartFolderPath(String partName) {
		return getPartFolderPath(getFamilyTypeFromPart(partName));
	}

	/**
	 * Returns the path of the folder where the family type resides.
	 * @param familyType the family type corresponding folder path
	 * @return the path of the folder where the parts of familyType reside
	 */
	public Path getPartFolderPath(FamilyType familyType) {
		return getDevicePath().resolve(familyType.name().toLowerCase());
	}

	/**
	 * Returns a list of all parts for which a device file exists for in this environment.
	 *
	 * @return a list of available devices in this environment
	 */
	public List<String> getAvailableParts() {
		ArrayList<String> allParts = new ArrayList<>();
		for (FamilyType family : getAvailableFamilies()) {
			allParts.addAll(getAvailableParts(family));
		}
		return allParts;
	}

	/**
	 * Returns a list of all parts of the family {@code type} for which a device file
	 * exists for in this environment.
	 *
	 * @return a list of available devices in this environment
	 */
	public List<String> getAvailableParts(FamilyType type) {
		ArrayList<String> allParts = new ArrayList<>();
		String pattern = "_db.dat";
		Path devFamilyPath = getPartFolderPath(type);
		if (!Files.isDirectory(devFamilyPath))
			return allParts;
		try {
			for(Path partPath : Files.newDirectoryStream(devFamilyPath)) {
				String fileName = partPath.getFileName().toString();
				if(fileName.endsWith(pattern)) {
					allParts.add(fileName.replace(pattern, ""));
				}
			}
		} catch (IOException ignored) {
		}
		return allParts;
	}

	/**
	 * Returns a list of family types supported in this environment.
	 *
	 * @return list of all family types installed in this environment
	 */
	public List<FamilyType> getAvailableFamilies() {
		List<FamilyType> allFamilies = new ArrayList<>();
		Path devicePath = getDevicePath();
		if (!Files.exists(devicePath) || !Files.isDirectory(devicePath))
			return allFamilies;
		try {
			for(Path subPath : Files.newDirectoryStream(devicePath)) {
				if (Files.isDirectory(subPath)) {
					try {
						String familyName = subPath.getFileName().toString();
						FamilyType type = FamilyType.valueOf(familyName.toUpperCase());
						Path familyInfoFilePath = getPartFolderPath(type).resolve(FAMILY_INFO_FILENAME);
						if (Files.exists(familyInfoFilePath))
							allFamilies.add(type);
					} catch (IllegalArgumentException ignored) {
					}
				}
			}
		} catch (IOException ignored) {
		}
		return allFamilies;
	}

	/**
	 * Returns the path to the corresponding device file for the part {@code partName} in
	 * this environment.
	 *
	 * @param partName name of the part to get corresponding device file for
	 * @return the full path to the device file for the specified part
	 */
	public Path getDeviceFilePath(String partName) {
		return getDeviceFilePath(getFamilyTypeFromPart(partName), partName);
	}

	private Path getDeviceFilePath(FamilyType family, String partName) {
		return getPartFolderPath(family).resolve(
				PartNameTools.removeSpeedGrade(partName) + DEVICE_FILE_SUFFIX);
	}

	/**
	 * Writes the given device to a compressed, serialized device file in this
	 * environment.
	 *
	 * @param device the device to write
	 * @throws IOException if an exception occurs writing the device file
	 */
	public void writeDeviceFile(Device device) throws IOException {
		Path path = getDeviceFilePath(device.getFamily(), device.getPartName());
		FileTools.writeCompressedDeviceFile(device, path);
	}

	public FamilyType getFamilyTypeFromPart(String partName) {
		String canonicalName = PartNameTools.removeSpeedGrade(partName);
		return supportedParts.get(canonicalName);
	}
}
