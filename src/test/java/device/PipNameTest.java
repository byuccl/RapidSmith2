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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 
 */
@RunWith(JUnitPlatform.class)
public class PipNameTest {

	/**
	 * This test verifies that Vivado PIP names are parsed properly
	 * during routing import into RapidSmith2
	 */
    @Test
    @DisplayName("Vivado Pip Name Test")
    public void vivadoPipNameTest() {
    	
    	Pattern pipNamePattern = Pattern.compile("(.*)/.*\\.([^<]*)((?:<<)?->>?)(.*)"); 
    	
		List<String> tests = Arrays.asList("RCLK_DSP_CLKBUF_L_X31Y149/RCLK_DSP_CLKBUF_L.CLK_HROUTE_L0<<->>CLK_HROUTE_R0", 
			                               "XIPHY_L_X0Y60/XIPHY_L.CLK_LEAF_MUX_XIPHY_9_CLK_LEAF->>XIPHY_BITSLICE_TILE_281_RX_CLK", 
			                               "XIPHY_L_X0Y60/XIPHY_L.XIPHY_BITSLICE_TILE_281_RX_CLK->XIPHY_BITSLICE_TILE_281_TX_OCLKDIV_PIN");
		
		List<String> expected = Arrays.asList("RCLK_DSP_CLKBUF_L_X31Y149 CLK_HROUTE_L0 <<->> CLK_HROUTE_R0", 
				                              "XIPHY_L_X0Y60 CLK_LEAF_MUX_XIPHY_9_CLK_LEAF ->> XIPHY_BITSLICE_TILE_281_RX_CLK",
				                              "XIPHY_L_X0Y60 XIPHY_BITSLICE_TILE_281_RX_CLK -> XIPHY_BITSLICE_TILE_281_TX_OCLKDIV_PIN");
		
		for (int i = 0; i < tests.size() ; i++) {
			Matcher matcher = pipNamePattern.matcher(tests.get(i));
			
			assertTrue(matcher.matches(), "Pip name " + tests.get(i) + " does not match pattern");
			assertEquals(4, matcher.groupCount(), "Matcher group count incorrect");
			
			String tile = matcher.group(1);
			String sourceWire = matcher.group(2);
			String connType = matcher.group(3);
			String sinkWire = matcher.group(4);
			
			String[] expectedValues = expected.get(i).split("\\s+");
			
			assertEquals(expectedValues[0], tile, "PIP tile name is incorrect: " + tests.get(i));
			assertEquals(expectedValues[1], sourceWire, "PIP source wire name is incorrect: " + tests.get(i));
			assertEquals(expectedValues[2], connType, "PIP connection type is incorrect: " + tests.get(i));
			assertEquals(expectedValues[3], sinkWire, "PIP sink wire name is incorrect: " + tests.get(i));
		}
    }
}