package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import edu.byu.ece.rapidSmith.design.subsite.CellDesign;

public class XdcReader {
	static public ArrayList<XdcConstraint> parseXdcFile(String filePath) throws IOException {
		File f = new File(filePath);
		if ((!f.exists()) || (!f.isFile())) {
			throw new IOException("XDC file path does not exist (" + filePath + ")");
		}

		ArrayList<XdcConstraint> constraints = new ArrayList<XdcConstraint>();

		BufferedReader reader = new BufferedReader(new FileReader(filePath));

		String line = null;
		while ((line = reader.readLine()) != null) {
			XdcConstraint constraint = parseLine(line);
			if (constraint != null)
				constraints.add(constraint);
		}

		reader.close();

		return constraints;

	}

	private CellDesign design;

	public XdcReader(CellDesign design) {
		assert design != null;
		this.design = design;
	}

	public void parseXdcFileIntoDesign(String filePath) throws IOException {
		ArrayList<XdcConstraint> constraints = parseXdcFile(filePath);

		for (XdcConstraint constraint : constraints) {
			design.addVivadoConstraint(constraint);
		}
	}

	static private XdcConstraint parseLine(String line) throws IOException {
		line = line.trim();
		
		if (line.equals(""))
			return null;
		
		if (line.matches("\\s*#.*"))
			return null;
		
		XdcConstraint constraint = new XdcConstraint(line, "");
		return constraint;
	}
}
