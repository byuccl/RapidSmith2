/*
 * Copyright (c) 2010 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.util;

import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.device.TileType;
import edu.byu.ece.rapidSmith.device.creation.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * This class generates java files for the device package for Xilinx FPGAs
 * to create enumerations for tile type names and primitive type names.  
 * Specifically it creates the files: PrimitiveType.java, TileType.java, Utils.java.
 * @author Chris Lavin
 */
public class TileAndPrimitiveEnumerator{
	/** Utility for gathering family parts and generating XDLRC files */
	private XDLRCRetriever xdlrcRetriever;
	/** Keeps all the unique tile names/types in a sorted set */
	private SortedSet<String> tileSet;
	/** Keeps all the unique instance names/types in a sorted set */
	private SortedSet<String> primitiveSet;
	/** Stores the names of the FPGA family names (Virtex4, Virtex5, ...)*/
	private SortedSet<FamilyType> families;
//	private static final FamilyType[] families = {FamilyType.ARTIX7,
//		FamilyType.KINTEX7, FamilyType.SPARTAN2,
//		FamilyType.SPARTAN2E, FamilyType.SPARTAN3, FamilyType.SPARTAN3A,
//		FamilyType.SPARTAN3ADSP, FamilyType.SPARTAN3E, FamilyType.SPARTAN6,
//		FamilyType.VIRTEX, FamilyType.VIRTEX2, FamilyType.VIRTEX2P,
//		FamilyType.VIRTEX4, FamilyType.VIRTEX5, FamilyType.VIRTEX6,
//		FamilyType.VIRTEX7, FamilyType.VIRTEXE, FamilyType.ZYNQ};
	/** All of the part names to check */ 
	public ArrayList<String> partNames;
	/** List of all xdlrc file names generated */
	public ArrayList<String> xdlrcFileNames;
	/** Stores the system's line terminator ("\n", "\r\n", "\r", etc)*/
	public static final String nl = System.getProperty("line.separator");

	/** Name of the PrimitiveType enum type to be created */
	public static final String primitiveTypeName = "PrimitiveType";
	/** Name of the TileType enum type to be created */
	public static final String tileTypeName = "TileType";
	/** Name of the Utils class to be created */
	public static final String utilsName = "Utils";
	
	/**
	 * Initializes the class to all new empty data structures
	 */
	public TileAndPrimitiveEnumerator(XDLRCRetriever xdlrcRetriever){
		this.xdlrcRetriever = xdlrcRetriever;
		tileSet = new TreeSet<>();
		primitiveSet = new TreeSet<>();
		families = new TreeSet<>();
		partNames = new ArrayList<>();
		xdlrcFileNames = new ArrayList<>();

		getOldSupport();
	}

	public void addSupportForFamily(FamilyType family) {
		families.add(family);
		XDLRCParser parser = new XDLRCParser();
		parser.registerListener(new TileAndPrimitiveFinderListener());
		parser.registerListener(new XDLRCParseProgressListener());

		for (String part : xdlrcRetriever.getPartsInFamily(family)) {
			Path xdlrcPath = xdlrcRetriever.getXDLRCFileForPart(part);
			try {
				parser.parse(xdlrcPath);
			} catch (IOException e) {
				MessageGenerator.briefErrorAndExit("Error detected while parsing " +
						"file " + xdlrcPath);
			}
			xdlrcRetriever.cleanupXDLRCFile(part, xdlrcPath);
		}
	}

	/**
	 * Populates the tile sets and primitive sets with the currently existing
	 * tile and primitive type data.
	 */
	private void getOldSupport() {
		Collections.addAll(families, TileType.SUPPORTED_FAMILIES);

		for (TileType tileType : TileType.values()) {
			tileSet.add(tileType.name());
		}

		for (SiteType primitiveType : SiteType.values()) {
			primitiveSet.add(primitiveType.name());
		}
	}

	public void writeEnumFiles(Path pathToSource) {
		writeTileTypeEnumFile(pathToSource);
		writePrimitiveTypeEnumFile(pathToSource);
	}

	private class TileAndPrimitiveFinderListener extends XDLRCParserListener {
		@Override
		protected void enterTile(List<String> tokens) {
			tileSet.add(tokens.get(4));
		}

		@Override
		protected void enterPrimitiveDef(List<String> tokens) {
			primitiveSet.add(tokens.get(1));
		}
	}

	/**
	 * Adds the license and comment header with package information to all files generated.
	 * @param bw The stream to write to.
	 * @throws IOException
	 */
	public void addHeaderToFile(BufferedWriter bw) throws IOException{
		String nl = System.getProperty("line.separator");
		bw.write("/*" + nl);
		bw.write(" * Copyright (c) 2010-2011 Brigham Young University" + nl);
		bw.write(" * " + nl);
		bw.write(" * This file is part of the BYU RapidSmith Tools." + nl);
		bw.write(" * " + nl);
		bw.write(" * BYU RapidSmith Tools is free software: you may redistribute it" + nl); 
		bw.write(" * and/or modify it under the terms of the GNU General Public License " + nl);
		bw.write(" * as published by the Free Software Foundation, either version 2 of " + nl);
		bw.write(" * the License, or (at your option) any later version." + nl);
		bw.write(" * "+ nl);
		bw.write(" * BYU RapidSmith Tools is distributed in the hope that it will be" + nl); 
		bw.write(" * useful, but WITHOUT ANY WARRANTY; without even the implied warranty" + nl);
		bw.write(" * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU " + nl);
		bw.write(" * General Public License for more details." + nl);
		bw.write(" * " + nl);
		bw.write(" * A copy of the GNU General Public License is included with the BYU" + nl); 
		bw.write(" * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also " + nl);
		bw.write(" * get a copy of the license at <http://www.gnu.org/licenses/>." + nl);
		bw.write(" * " + nl);
		bw.write(" */" + nl);
		bw.write("package edu.byu.ece.rapidSmith.device;");
		bw.write(nl + nl);
		bw.write("/*" + nl);
		bw.write(" * This file was generated by:" + nl);
		bw.write(" *   " + getClass().toString() + nl);
		bw.write(" * Generated on:" + nl);
		bw.write(" *   " + FileTools.getTimeString() + nl);
		bw.write(" * The following Xilinx families are supported:" + nl);
		bw.write(" *   ");
		for(FamilyType family : families){
			bw.write(family.toString().toLowerCase() + " ");
		}
		bw.write(nl);
		bw.write(" */" + nl);
		bw.write(nl);
	}

	private void writeTileTypeEnumFile(Path pathToSource) {
		Path path = getPathToEnumFiles(pathToSource);
		try {
			BufferedWriter bw = Files.newBufferedWriter(
					path.resolve(tileTypeName + ".java"), Charset.defaultCharset());
			addHeaderToFile(bw);
			bw.write("/**" + nl);
			bw.write(" * This enum enumerates all of the Tile types of the following FPGA families: " + nl);
			bw.write(" *   ");
			for (FamilyType family : families) {
				bw.write(family.toString().toLowerCase() + " ");
			}
			bw.write(nl);
			bw.write(" */" + nl);
			bw.write("public enum " + tileTypeName + "{" + nl);

			Iterator<String> itr = tileSet.iterator();
			bw.write("\t" + itr.next());
			while (itr.hasNext()) {
				bw.write("," + nl + "\t" + itr.next());
			}
			bw.write(";" + nl + nl);

			bw.write("\tpublic static final FamilyType[] SUPPORTED_FAMILIES =" + nl);
			bw.write("\t\t\t{" + nl);
			for (FamilyType family : families) {
				bw.write("\t\t\t\t\tFamilyType." + family.name() + "," + nl);
			}
			bw.write("\t\t\t}" + nl);

			bw.write("}" + nl);
			bw.close();
		}
		catch (IOException e) {
			MessageGenerator.briefErrorAndExit("Problems writing class: " + tileTypeName);
		}
	}

	private void writePrimitiveTypeEnumFile(Path pathToSource) {
		Path path = getPathToEnumFiles(pathToSource);
		Path pathName = path.resolve(primitiveTypeName + ".java");
		try(BufferedWriter bw = Files.newBufferedWriter(pathName, Charset.defaultCharset())) {
			addHeaderToFile(bw);
			bw.write("/**" + nl);
			bw.write(" * This enum enumerates all of the Primitive types of the following FPGA families: " + nl);
			bw.write(" *   ");
			for (FamilyType family : families) {
				bw.write(family.toString().toLowerCase() + " ");
			}
			bw.write(nl);
			bw.write(" */" + nl);
			bw.write("public enum " + primitiveTypeName + "{"+nl);

			Iterator<String> itr = primitiveSet.iterator();
			bw.write("\t" + itr.next());
			while (itr.hasNext()) {
				bw.write("," + nl + "\t" + itr.next());
			}
			bw.write(";" + nl + nl);

			bw.write("\tpublic static final FamilyType[] SUPPORTED_FAMILIES =" + nl);
			bw.write("\t\t\t{" + nl);
			for (FamilyType family : families) {
				bw.write("\t\t\t\t\tFamilyType." + family.name() + "," + nl);
			}
			bw.write("\t\t\t}" + nl);

			bw.write("}" + nl);
		}
		catch (IOException e) {
			MessageGenerator.briefErrorAndExit("Problems writing class: " + primitiveTypeName);
		}
	}

	public static Path getPathToEnumFiles(Path pathToSource){
		return pathToSource.resolve("edu")
				.resolve("byu")
				.resolve("ece")
				.resolve("rapidSmith")
				.resolve("device");
	}
	
	/**
	 * Allows this class to be invoked from the command line.  
	 */
	public static void main(String args[]) {
		XDLRCRetriever xdlrcRetriever = new ISE_XDLRCRetriever();
		// Create a new instance of this class
		TileAndPrimitiveEnumerator me = new TileAndPrimitiveEnumerator(xdlrcRetriever);
		MessageGenerator.printHeader(me.getClass().getCanonicalName());

		System.out.println("Enumerating Tiles and Primitives...");
		
		// Create the .java files for the Design package
		Path pathToSource = Paths.get(args[0]);
		for (int i = 1; i < args.length; i++) {
			String fileArg = args[i];
			me.addSupportForFamily(FamilyType.valueOf(fileArg));
		}
		me.writeEnumFiles(pathToSource);

		System.out.println("DONE!");
	}
}
