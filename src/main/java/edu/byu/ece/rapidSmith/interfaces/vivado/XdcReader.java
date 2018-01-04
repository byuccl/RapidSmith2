package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import edu.byu.ece.rapidSmith.design.subsite.CellDesign;

public class XdcReader {
	
	
	/**
	 * @param filePath Path of XDC file to read constraints from
	 * @return List of XDC constraints
	 * @throws IOException If XDC file does not exist
	 */
	static public ArrayList<XdcConstraint> parseXdcFile(String filePath) throws IOException {
		File f = new File(filePath);
		if ((!f.exists()) || (!f.isFile())) {
			throw new FileNotFoundException("XDC file path does not exist (" + filePath + ")");
		}

		ArrayList<XdcConstraint> constraints = new ArrayList<XdcConstraint>();

		BufferedReader reader = new BufferedReader(new FileReader(filePath));

		String line = null;
		while ((line = reader.readLine()) != null) {
			ArrayList<XdcConstraint> lineConstraints = parseLine(line);
			if (lineConstraints != null)
				constraints.addAll(lineConstraints);
		}

		reader.close();

		return constraints;

	}

	private CellDesign design;

	public XdcReader(CellDesign design) {
		assert design != null;
		this.design = design;
	}

	/**
	 * @param filePath Path of XDC file to read constraints from
	 * @throws IOException If XDC file does not exist
	 */
	public void parseXdcFileIntoDesign(String filePath) throws IOException {
		ArrayList<XdcConstraint> constraints = parseXdcFile(filePath);

		for (XdcConstraint constraint : constraints) {
			design.addVivadoConstraint(constraint);
		}
		System.out.println("Added " + constraints.size() + " constraints to design");
	}

	// If the line includes "dict", there is a dictionary of multiple properties
	// on an object with a single set property command.
	// Example 1: set_property -dict "name1 value1 ... nameN valueN" [get_ports {portName}]
	// Example 2: set_property -dict {name1 value1 ... nameN valueN} [get_ports {portName}]
	// TODO: Make more robust
	static private ArrayList<String> splitLineProperties(String line) {
		
		int prefixEndIndex = line.lastIndexOf("-dict") + 5;
		int suffixBeginIndex = line.lastIndexOf("[get_ports");
		String suffix = line.substring(suffixBeginIndex, line.length());
		String prefix = line.substring(0, line.lastIndexOf("-dict"));
		
		// Split up the individual properties
		String properties = line.substring(prefixEndIndex, suffixBeginIndex);
		properties = properties.trim();
		
		ArrayList<String> splitConstraints = new ArrayList<>();
		String[] splitProps = properties.split("\\s+");
		
		for (int i = 1; i < splitProps.length - 1; i+=2)
		{
			splitConstraints.add(prefix + splitProps[i] + " " + splitProps[i+1] + " " + suffix);
		}

	
		return splitConstraints;
	}

	
	static private ArrayList<XdcConstraint> parseLine(String line) throws IOException {
		ArrayList<XdcConstraint> constraints = new ArrayList<>();
		
		line = line.trim();
		
		if (line.equals(""))
			return null;
		
		if (line.matches("\\s*#.*"))
			return null;
		
		// Remove same line comments
		int commentIndex = line.indexOf("#");
		line = (commentIndex != -1) ? line.substring(0, commentIndex) : line;
		line = line.trim();
		
		// Remove braces from [get_ports ... ] suffix
		// TODO: Make more robust
		String suffix = line.substring(line.lastIndexOf("[get_ports"), line.length());
		suffix = suffix.replaceAll("[{}]", "");
		line = line.substring(0, line.lastIndexOf("[get_ports") - 1) + " " + suffix;

		
		if (line.matches("(.*)(-dict)(.*)")) {
			// Split up the properties into individual constraints for ease
			ArrayList<String> splitLines = splitLineProperties(line);
			
			for (String splitLine : splitLines) {
				XdcConstraint constraint = new XdcConstraint(splitLine, "");
				constraints.add(constraint);
			}			
		}
		else {
			XdcConstraint constraint = new XdcConstraint(line, "");
			constraints.add(constraint);
		}		
		
		return constraints;
	}
}
