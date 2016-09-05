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

import com.caucho.hessian.io.Deflation;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import edu.byu.ece.rapidSmith.device.*;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * This class is specifically written to allow for efficient file import/export of different semi-primitive
 * data types and structures.  The read and write functions of this class are only guaranteed to work with
 * those specified in this class and none else.  The goal of this class is to load faster than Serialized
 * Java and produce smaller files as well.
 * 
 * @author Chris Lavin
 * Created on: Apr 22, 2010
 */
public class FileTools {
	/** Name of the Virtex 5 RAMB Primitive Pin Mapping Patch File */
	public static final String v5RAMBPinMappingFileName = "v5RAMBPins.dat";
	/** Folder where device files are kept */
	public static final String deviceFolderName = "devices";

	//===================================================================================//
	/* Generic Read/Write Serialization Methods                                          */
	//===================================================================================//
	/**
	 * This is a simple method that writes the elements of an ArrayList of Strings
	 * into lines in the text file fileName.
	 * @param lines The ArrayList of Strings to be written
	 * @param fileName Name of the text file to save the ArrayList to
	 */
	public static void writeLinesToTextFile(ArrayList<String> lines, String fileName) {
		String nl = System.getProperty("line.separator");
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));

			for (int i = 0; i < lines.size(); i++) {
				bw.write(lines.get(i) + nl);
			}

			bw.close();
		}
		catch(IOException e){
			MessageGenerator.briefErrorAndExit("Error writing file: " +
					fileName + File.separator + e.getMessage());
		}
	}
	
	/**
	 * This is a simple method that will read in a text file and put each line in a
	 * string and put all the lines in an ArrayList.  The user is cautioned not
	 * to open extremely large files with this method.
	 * @param fileName Name of the text file to get parts from
	 * @return An ArrayList containing strings of each line in the file. 
	 */
	public static ArrayList<String> getLinesFromTextFile(String fileName){
		String line = null;
		BufferedReader br;
		ArrayList<String> lines = new ArrayList<String>();
		try{
			br = new BufferedReader(new FileReader(fileName));
			
			while((line = br.readLine()) != null){
				lines.add(line);
			}
			br.close();
		}
		catch(FileNotFoundException e){
			MessageGenerator.briefErrorAndExit("ERROR: Could not find file: " + fileName);
		} 
		catch(IOException e){
			MessageGenerator.briefErrorAndExit("ERROR: Could not read from file: " + fileName);
		}
		
		return lines;
	}
	
	//===================================================================================//
	/* Generic File Manipulation Methods                                                 */
	//===================================================================================//	

	/**
	 * Takes a file name and removes everything after the last '.' inclusive
	 * @param path The input file name
	 * @return the substring of fileName if it contains a '.', it returns fileName otherwise
	 */
	public static Path removeFileExtension(Path path){
		String asString = path.toString();
		int endIndex = asString.lastIndexOf('.');
		if(endIndex != -1){
			path = Paths.get(asString.substring(0, endIndex));
		}
		return path;
	}

	public static Path replaceFileExtension(Path path, String newExtension) {
		String asString = path.toString();
		int endIndex = asString.lastIndexOf('.');
		if(endIndex != -1) {
			asString = asString.substring(0, endIndex);
		}
		return Paths.get( + '.' + newExtension);
	}

	public static String getFileExtension(Path path) {
		String asString = path.toString();
		int endIndex = asString.lastIndexOf('.');
		if(endIndex != -1) {
			return asString.substring(endIndex + 1);
		} else {
			return null;
		}
	}

	/**
	 * Creates a directory in the current path called dirName.
	 * @param dirName Name of the directory to be created.
	 * @return True if the directory was created or already exists, false otherwise.
	 */
	public static boolean makeDir(String dirName){
		File dir = new File(dirName); 
		if(!(dir.exists())){
			return dir.mkdir();
		}
		return true;
	}
	
	/**
	 * Creates a directory in the current path called dirName.
	 * @param dirName Name of the directory to be created.
	 * @return True if the directory and implicit parent directories were created, false otherwise.
	 */
	public static boolean makeDirs(String dirName){
		return new File(dirName).mkdirs();
	}
	
	/**
	 * Gets the size of the file in bytes.
	 * @param fileName Name of the file to get the size of.
	 * @return The number of bytes used by the file.
	 */
	public static long getFileSize(String fileName){
		return new File(fileName).length();
	}
	
	/**
	 * Delete the file/folder in the file system called fileName 
	 * @param fileName Name of the file to delete
	 * @return True for successful deletion, false otherwise.
	 */
	public static boolean deleteFile(String fileName){
	    // A File object to represent the filename
	    File f = new File(fileName);

	    // Make sure the file or directory exists and isn't write protected
	    if (!f.exists())
	      throw new IllegalArgumentException(
	          "Delete: no such file or directory: " + fileName);

	    if (!f.canWrite())
	      throw new IllegalArgumentException("Delete: write protected: "
	          + fileName);

	    // If it is a directory, make sure it is empty
	    if (f.isDirectory()) {
	      String[] files = f.list();
	      if (files.length > 0)
	        throw new IllegalArgumentException(
	            "Delete: directory not empty: " + fileName);
	    }

	    // Attempt to delete it
	    boolean success = f.delete();

	    if (!success)
	      throw new IllegalArgumentException("Delete: deletion failed");
		
		return success;
	}
	
	/**
	 * Deletes everything in the directory given by path, but does not
	 * delete the folder itself.
	 * @param path The path to the folder where all its contents will be deleted.
	 * @return True if operation was successful, false otherwise.
	 */
	public static boolean deleteFolderContents(String path){
		File currDirectory = new File(path);
		if(currDirectory.exists()){
			try {
				for(File file : currDirectory.listFiles()){
					if(file.isDirectory()){
						if(!deleteFolder(file.getCanonicalPath())){
							return false;
						}
					}
					else{
						if(!deleteFile(file.getCanonicalPath())){
							return false;
						}
					}
				}				
			}
			catch(IOException e){
				return false;
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Delete the folder and recursively files and folders below
	 * @param folderName
	 * @return true for successful deletion, false otherwise
	 */
	public static boolean deleteFolder(String folderName){
		// A file object to represent the filename
		File f = new File(folderName);
		
		if(!f.exists() || !f.isDirectory()){
			throw new IllegalArgumentException("Delete: no such directory: " + folderName);
		}
		
		for(File i: f.listFiles()){
			if(i.isDirectory()){
				deleteFolder(i.getAbsolutePath());
			}else if(i.isFile()){
				if(!i.delete()){
					throw new IllegalArgumentException("Delete: deletion failed: " + i.getAbsolutePath());
				}
			}
		}
		return deleteFile(folderName);
	}

	public static boolean renameFile(String oldFileName, String newFileName){
		File oldFile = new File(oldFileName);
		return oldFile.renameTo(new File(newFileName));
	}
	
	/**
	 * Copies a file from one location (src) to another (dst).  This implementation uses the java.nio
	 * channels (because supposedly it is faster).
	 * @param src Source file to read from
	 * @param dst Destination file to write to
	 * @return True if operation was successful, false otherwise.
	 */
	public static boolean copyFile(String src, String dst){
	    FileChannel inChannel = null;
	    FileChannel outChannel = null;
		try {
			inChannel = new FileInputStream(new File(src)).getChannel();
			outChannel = new FileOutputStream(new File(dst)).getChannel();
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} 
		catch (FileNotFoundException e){
			e.printStackTrace();
			MessageGenerator.briefError("ERROR could not find/access file(s): " + src + " and/or " + dst);
			return false;
		} 
		catch (IOException e){
			MessageGenerator.briefError("ERROR copying file: " + src + " to " + dst);
			return false;
		}
		finally {
			try {
				if(inChannel != null)
					inChannel.close();
				if(outChannel != null) 
					outChannel.close();
			} 
			catch (IOException e) {
				MessageGenerator.briefError("Error closing files involved in copying: " + src + " and " + dst);
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Copies a folder and its files from the path defined in srcDirectoryPath to a new folder
	 * at dstDirectoryPath.  If the recursive flag is set it will also recursively copy subfolders
	 * from the source to the destination.
	 * @param srcDirectoryPath The name of the source folder to copy.
	 * @param dstDirectoryPath The destination of where the copy of the folder should be located.
	 * @param recursive A flag denoting if the sub folders of source should be copied.
	 * @return True if operation was successful, false otherwise.
	 */
	public static boolean copyFolder(String srcDirectoryPath, String dstDirectoryPath, boolean recursive){
		File srcDirectory = new File(srcDirectoryPath);
		File dstDirectory = new File(dstDirectoryPath + File.separator + srcDirectory.getName());
		if(srcDirectory.exists() && srcDirectory.isDirectory()){
			if(!dstDirectory.exists()){
				dstDirectory.mkdirs();
			}
			for(File file : srcDirectory.listFiles()){
				if(!file.isDirectory()){
					if(!copyFile(file.getAbsolutePath(), dstDirectory.getAbsolutePath() + File.separator + file.getName())){
						return false;
					}
				}
				else if(file.isDirectory() && recursive){
					if(!copyFolder(file.getAbsolutePath(), dstDirectory.getAbsolutePath(), true)){
						return false;
					}
				}

			}
			return true;
		}
		MessageGenerator.briefError("ERROR: copyFolder() - Cannot find directory: " + srcDirectoryPath);
		return false;
	}
	
	/**
	 * Copies the folder contents of the folder specified by src to folder specified as dst.  It will
	 * copy all files in it to the new location.  If the recursive
	 * flag is set, it will copy everything recursively in the folder src to dst.
	 * @param src The source folder to copy.
	 * @param dst The location of where the copy of the contents of src will be located.
	 * @param recursive A flag indicating if sub folders and their contents should be
	 * copied.
	 * @return True if operation is successful, false otherwise.
	 */
	public static boolean copyFolderContents(String src, String dst, boolean recursive){
		File srcDirectory = new File(src);
		File dstDirectory = new File(dst);
		if(srcDirectory.exists() && srcDirectory.isDirectory()){
			if(!dstDirectory.exists()){
				MessageGenerator.briefError("ERROR: Could find destination directory " + dstDirectory.getAbsolutePath());
			}
			for(File file : srcDirectory.listFiles()){
				if(!file.isDirectory()){
					if(!copyFile(file.getAbsolutePath(), dstDirectory.getAbsolutePath() + File.separator + file.getName())){
						return false;
					}
				}
				else if(file.isDirectory() && recursive){
					if(!copyFolder(file.getAbsolutePath(), dst, true)){
						MessageGenerator.briefError("ERROR: While copying folder " + file.getAbsolutePath() +
								" to " + dst + File.separator + file.getName());
						return false;
					}
				}
			}
			return true;
		}
		MessageGenerator.briefError("ERROR: copyFolderContents() - Cannot find directory: " + src);
		return false;
	}
	
	//===================================================================================//
	/* Simple Device/WireEnumeration Load Methods & Helpers                              */
	//===================================================================================//
	public static Hessian2Input getCompactReader(Path filePath) throws IOException {
		Hessian2Input his = new Hessian2Input(
				new BufferedInputStream(Files.newInputStream(filePath)));
		his.setCloseStreamOnClose(true);
		return new Deflation().unwrap(his);

	}

	public static Hessian2Output getCompactWriter(Path filePath) throws IOException {
		Hessian2Output hos = new Hessian2Output(
				new BufferedOutputStream(Files.newOutputStream(filePath)));
		hos.setCloseStreamOnClose(true);
		return new Deflation().wrap(hos);
	}

	public static void writeCompressedDeviceFile(Device device, Path path) throws IOException {
		Hessian2Output hos = null;
		try {
			hos = getCompactWriter(path);
			hos.writeObject(device);
		} finally {
			if (hos != null) {
				hos.close();
			}
		}
	}

	public static Device loadDevice(Path filePath) {
		try {
			return (Device) getCompactReader(filePath).readObject();
		} catch (IOException e) {
			return null;
		}
	}



	
	/**
	 * This method will get and return the current time as a string
	 * formatted in the same way used in most Xilinx report and XDL
	 * files.  The format used in the using the same syntax as SimpleDateFormat
	 * which is "EEE MMM dd HH:mm:ss yyyy".
	 * @return Current date and time as a formatted string.
	 */
	public static String getTimeString(){
		SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
		return formatter.format(new java.util.Date());
	}

	/**
	 * Gets and returns the file separator character for the given OS
	 */
	public static String getDirectorySeparator(){
		if(FileTools.cygwinInstalled()){
			return "/";
		}
		else{
			return File.separator;
		}
	}

	/**
	 * Checks if Cygwin is installed on the system
	 */
	public static boolean cygwinInstalled(){
		return System.getenv("CYGWIN") != null;
	}
}
