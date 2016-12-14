/*
 * Copyright (c) 2010-2011 Brigham Young University
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
package edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;

/**
 * Contains a number of static methods for looking up specific parts from a library
 * and returning the corresponding XilinxConfigurationSpecification object.
 * 
 * There are two types of part names:
 * 
 *   Root Part Name: This refers to a part name that does not have the package information
 *   
 *   Package Part Name: This refers to a part name that includes the package information
 * 
 */
public class DeviceLookup {

    protected static DeviceLookup _singleton = null;
    
    /**
     * This constructor is protected so that only this class can create an instance
     * (so there can only be a single instance of the class ever). The reason for
     * using a singleton instance instead of just a bunch of static methods is that
     * the methods require the use of a list of part libraries. The singleton instance
     * eliminates the need to create the list of part libraries every time one of the
     * static methods is used. This way, the list is created only once (the first time
     * it is needed) and saved for later use.
     */
    protected DeviceLookup() {
        _libraries = new ArrayList<>(3);
        _libraries.add(new V4PartLibrary());
        _libraries.add(new V5PartLibrary());
        _libraries.add(new V6PartLibrary()); 
    }
    
    /**
     * Get (create if necessary) the singleton instance of this class.
     */
    public static DeviceLookup sharedInstance() {
        if (_singleton == null) {
            _singleton = new DeviceLookup();
        }
        return _singleton;
    }
    
    /**
     * Searches for a configuration specification from first a string and if null it
     * searches for the part from the bitstream. 
     * 
     * This method will exit if there is an error and is best used in main functions. 
     * 
     */
    public static XilinxConfigurationSpecification lookupPartFromPartnameOrBitstreamExitOnError(String partName, Bitstream bitstream) {

    	XilinxConfigurationSpecification partInfo = null;

    	// first see if the part can be found from the String
    	if (partName != null) {
    		partInfo = DeviceLookup.lookupPartV4V5V6(partName);
    		if (partInfo == null) {
				System.err.println("Part " + partName + " is not a valid part name. Valid part names include:");
				printAvailableParts(System.err);
				System.exit(1);    			
    		}
    		return partInfo;
    	}
    	
    	// If no partname was provided, see if the name can be found from the bitstream
    	if (bitstream == null) {
			System.err.println("No partname or bitstream provided. Provide a valid partname:");
			printAvailableParts(System.err);
			System.exit(1);    			
    	}

    	if (bitstream.getHeader() != null) {
			partName = bitstream.getHeader().getPartName();
			// strip the package name from the original part name
			partName = DeviceLookup.getRootDeviceName(partName);
			partInfo = DeviceLookup.lookupPartV4V5V6(partName);
			if (partInfo == null) {
				System.err.println("Invalid Part Name:"+partName);
				DeviceLookup.printAvailableParts(System.err);
				System.exit(1);
			}				
		} else {
			System.err.println("Part name was not found in the bitstream. Provide a valid partname:");
			printAvailableParts(System.err);
			System.exit(1);
		}		
    	return partInfo;
    }

    public static XilinxConfigurationSpecification lookupPartFromPartnameOrBitstreamExitOnError(Bitstream bitstream) {
    	return lookupPartFromPartnameOrBitstreamExitOnError(null,bitstream);
    }
    

    /**
     * Performs a part lookup based on the header information in a bitstream. Returns a null
     * if there is no header or if there is no valid match.
     */
    public static XilinxConfigurationSpecification lookupPartFromBitstream(Bitstream bitstream) {

    	XilinxConfigurationSpecification partInfo = null;
    	String partName;
    	
    	// first see if the part can be 
    	if (bitstream.getHeader() != null) {
			partName = bitstream.getHeader().getPartName();
			// strip the package name from the original part name
			partName = DeviceLookup.getRootDeviceName(partName);
			partInfo = DeviceLookup.lookupPartV4V5V6(partName);
			return partInfo;
    	}		
    	return partInfo;
    }

    /**
     * Given a root device name, lookup the part within the V4, V5, and V6 part libraries
     * and return the appropriate XilinxConfigurationSpecification. <code>null</code> is
     * returned if a matching part cannot be found.
     * 
     * @param partName Name of the device.
     * @return The corresponding spec or null if none exists.
     */
    public static XilinxConfigurationSpecification lookupPartV4V5V6(String partName) {
    	if (partName == null)
    		return null;
    	DeviceLookup lookup = sharedInstance();
        return lookup.lookupPartV4V5V6Protected(partName);
    }

    /**
     * Given a package device name, return the corresponding {@link XilinxConfigurationSpecification}.
     * This will call getRootDeviceName to strip the package and then call
     * lookupPartV4V5V6.
     * @return the corresponding XilinxConfigurationSpecification or null if none exists.
     */
    public static XilinxConfigurationSpecification lookupPartV4V5V6withPackageName(String packageName) {
    	String rootName = getRootDeviceName(packageName);
    	return lookupPartV4V5V6(rootName);
    }
    
    /**
     * Given a device name with trailing package name (no separator -- this is how
     * the name is found in a bitstream header), return the root device name. This
     * method will work whether or not the argument includes the "XC" at the beginning
     * of the part name (the names from the bitstream header do NOT include the "XC").
     * The resulting device name WILL have the "XC" prefix whether or not the argument
     * does.
     * 
     * @param partName Name of the device
     * @return Root name of the device or null if it could not be determined.
     */
    public static String getRootDeviceName(String partName) {
        DeviceLookup lookup = sharedInstance();
        return lookup.getRootDeviceNameProtected(partName);        
    }

    /**
     * Given a device name with trailing package name (no separator -- this is how
     * the name is found in a bitstream header), return the package name. This
     * method will work whether or not the argument includes the "XC" at the beginning
     * of the part name (the names from the bitstream header do NOT include the "XC").
     * 
     * @param partName Name of the device
     * @return the package string from the part name.
     */
    public static String getPackageName(String partName) {
        DeviceLookup lookup = sharedInstance();
        return lookup.getPackageNameProtected(partName);
    }
    
    /**
     * Print a list of all available parts in the V456 libraries on the given OutputStream.
     * @param out
     */
    public static void printAvailableParts(OutputStream out) {
    	PrintWriter pw = new PrintWriter(out);
    	pw.println("Available Parts:");
    	for (PartLibrary lib : sharedInstance()._libraries) {
    		boolean printLibrary = false;
    		for (XilinxConfigurationSpecification d : lib.getParts()) {
    			if (!printLibrary) {
    				pw.print("\t"+d.getDeviceFamily()+":");
    				printLibrary = true;
    			}
    			pw.print(d.getDeviceName()+", ");
    		}
    		pw.println();
    	}
    	pw.flush();
    	pw.close();
    }

    //////////////////////////////  Protected Methods  //////////////////////////////
    ///                                                                           ///
    
    /**
     * Given a device name with trailing package name (no separator -- this is how
     * the name is found in a bitstream header), return the root device name. This
     * method will work whether or not the argument includes the "XC" at the beginning
     * of the part name (the names from the bitstream header do NOT include the "XC").
     * The resulting device name WILL have the "XC" prefix whether or not the argument
     * does.
     * 
     * @param partName Name of the device.
     * @return The root name of the device, or null if it could not be determined.
     */
	protected String getRootDeviceNameProtected(String partName) {
	    if (Character.isDigit(partName.charAt(0))) {
	        partName = "XC" + partName;
	    }
	    String result = null;
	    for (PartLibrary lib : _libraries) {
	        for (XilinxConfigurationSpecification spec : lib.getParts()) {
	            String deviceRoot = spec.getDeviceName();
	            if (partName.toLowerCase().startsWith(deviceRoot.toLowerCase())) {
	                result = deviceRoot;
	            }
	        }
	    }
	    return result;
	}
	
	/**
	 * Parses a partname that contains both the device and package information.
	 * Returns the package information from the name.
	 */
	protected String getPackageNameProtected(String partName) {
		String result = null;
	    String rootDevice = getRootDeviceNameProtected(partName);
		if (rootDevice != null) {
			int startIndex = partName.toLowerCase().indexOf(rootDevice.substring(2).toLowerCase());
		    result = partName.substring(startIndex + rootDevice.length() - 2);
		}
		return result;
	}
	
	
    /**
     * Given a root device name, lookup the part within the V4, V5, and V6 part libraries
     * and return the appropriate XilinxConfigurationSpecification. <code>null</code> is
     * returned if a matching part cannot be found.
     * 
     * @param partName Name of the device
     * @return Corresponding spec or null if none exists.
     */
	protected XilinxConfigurationSpecification lookupPartV4V5V6Protected(String partName) {
		for (PartLibrary lib : _libraries) {
			XilinxConfigurationSpecification spec = lib.getPartFromDeviceName(partName);
			if (spec != null)
				return spec;
		}
		return null;
	}
	
	/**
	 * This is the list of part libraries owned by the singleton instance
	 */
	protected List<PartLibrary> _libraries;
	
}
