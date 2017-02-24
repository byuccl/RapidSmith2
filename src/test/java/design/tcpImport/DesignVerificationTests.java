package design.tcpImport;
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

import org.junit.runner.RunWith;
import org.junit.jupiter.api.Tag;
import org.junit.platform.runner.JUnitPlatform;

/**
 * This class holds TINCR checkpoint import tests.
 * 
 * @author Thomas Townsend
 */
public class DesignVerificationTests {
	
	@RunWith(JUnitPlatform.class)
	@Tag("slow")
	public static class SuperCounterTest extends DesignVerificationTest {
		@Override
		public String getCheckpointName() {
			return "superCounter";
		}
	}
	
	@RunWith(JUnitPlatform.class)
	@Tag("slow")
	public static class count16Test extends DesignVerificationTest {
		@Override
		public String getCheckpointName() {
			return "count16";
		}
	}
	
	@RunWith(JUnitPlatform.class)
	@Tag("slow")
	public static class cordicTest extends DesignVerificationTest {
		@Override
		public String getCheckpointName() {
			return "cordic";
		}
	}
}
