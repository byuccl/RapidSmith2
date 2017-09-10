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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import edu.byu.ece.rapidSmith.util.PartNameTools;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

/**
 * This class holds a variety of part name tests. Valid part names
 * come in a variety of formats, and these tests verify that all formats
 * can be handled.
 */
@RunWith(JUnitPlatform.class)
public class PartNameTests {

	/**
	 * This test verifies that the speed grade is removed properly
	 * for a variety of valid part names. Add to the test cases as necessary.
	 */
    @Test
    @DisplayName("Remove Speed Grade Test")
    public void removeSpeedGradeTest() {
		List<String> tests = Arrays.asList("xcku025-ffva1156-1-c", "xc7a100t-csg324-3", "xc7a100tcsg324-3", "xc7a100tcsg324");
		List<String> expected = Arrays.asList("xcku025ffva1156", "xc7a100tcsg324", "xc7a100tcsg324", "xc7a100tcsg324");
		
		for (int i = 0; i < tests.size() ; i++) {
			String result = PartNameTools.removeSpeedGrade(tests.get(i));
			assertEquals(expected.get(i), result, "Speed grade removed incorrectly");
		}
    }
}
