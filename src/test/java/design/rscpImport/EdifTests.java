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
package design.rscpImport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import edu.byu.ece.rapidSmith.interfaces.vivado.EdifInterface;
import edu.byu.ece.rapidSmith.util.Exceptions;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

/**
 * This class is used to test the {@link EdifInterface}. Add tests as necessary.
 */
@RunWith(JUnitPlatform.class)
public class EdifTests {

	@Test
	@DisplayName("Parse Exception")
	public void exceptionTest() throws IOException {
		//String expectedMessage = "java.io.FileNotFoundException: bogusEdifFile.edf (The system cannot find the file specified)";
		expectThrows(Exceptions.ParseException.class, () -> EdifInterface.parseEdif("bogusEdifFile.edf", null));
		//assertEquals(expectedMessage, exception.getMessage(), "Wrong exception message thrown.");
	}
}
