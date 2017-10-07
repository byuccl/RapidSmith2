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
package device;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import static org.junit.jupiter.api.Assertions.*;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.util.Exceptions;

/**
 * 
 *
 */
@RunWith(JUnitPlatform.class)
public class LoadingDeviceTests {

	/**
	 * Tests that the correct exception is thrown when loading a part into RapidSmith 
	 * with an invalid family type 
	 */
	@Test
	@DisplayName("Missing Family Test")
	public void loadDevice1() throws IOException {
	
		String expectedExceptionMessage = "Cannot find device file for part: \"unknownPartName\".\n" 
				 + "If the device files for the part exist, make sure your RapidSmithPath variable is properly set. \n"
				+ "If the device files don't exist, view the RapidSmith2 Tech Report for instructions on how to generate a new device file for this part.";
		
		Throwable exception = assertThrows(Exceptions.EnvironmentException.class, () -> RSEnvironment.defaultEnv().getDevice("unknownPartName"));
		assertEquals(expectedExceptionMessage, exception.getMessage(), "Wrong exception message thrown! " + exception.getMessage());
	}
	
	/**
	 * Tests that the correct exception is thrown when loading a part into RapidSmith 
	 * that is missing a device file (but has a valid family type). 
	 */
	@Test
	@DisplayName("Missing Device File Test")
	public void loadDevice2() throws IOException {
	
		String expectedExceptionMessage = "Cannot find device file for part: \"xc7a75tftg256-3\".\n"
				 + "If the device files for the part exist, make sure your RapidSmithPath variable is properly set. \n"
				+ "If the device files don't exist, view the RapidSmith2 Tech Report for instructions on how to generate a new device file for this part.";
		
		Throwable exception = assertThrows(Exceptions.EnvironmentException.class, () -> RSEnvironment.defaultEnv().getDevice("xc7a75tftg256-3"));
		assertEquals(expectedExceptionMessage, exception.getMessage(), "Wrong exception message thrown! " + exception.getMessage());
	}
}
