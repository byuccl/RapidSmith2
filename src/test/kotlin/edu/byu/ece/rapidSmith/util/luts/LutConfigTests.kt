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

package edu.byu.ece.rapidSmith.util.luts

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

private val NPE = NullPointerException::class.java

private val basicLutContents = LutContents(Constant.ZERO, 6)

class LutConfigTests {
	@Nested
	@DisplayName("constructor tests")
	class ConstructorTests {
		@Test
		@DisplayName("null operatingMode arguments cause NPE")
		fun nullOperatingMode() {
			assertThrows(NPE, { LutConfig(null, basicLutContents) })
		}

		@Test
		@DisplayName("null lutContents arguments cause NPE")
		fun nullLutContents() {
			assertThrows(NPE, { LutConfig(OperatingMode.LUT, null) })
		}

		@Test
		@DisplayName("unspecified outpinPin argument results in null outputPin")
		fun nullOutputPin() {
			val config = LutConfig(OperatingMode.LUT, basicLutContents)
			assertNull(config.outputPinName)
		}
	}

	@Nested
	@DisplayName("copy constructor tests")
	class CopyConstructorTests {
		@Test
		@DisplayName("null other arguments cause NPE")
		fun nullLutContents() {
			assertThrows(NPE, { LutConfig(null) })
		}

		@Test
		@DisplayName("copied equals original")
		fun testEquals() {
			val config = LutConfig(OperatingMode.LUT, "O6", basicLutContents)
			val copy = LutConfig(config)
			assertEquals(config, copy)
		}

		@Test
		@DisplayName("copied lut contents is different from original")
		fun testDifferentLutContents() {
			val config = LutConfig(OperatingMode.LUT, "O6", basicLutContents)
			val copy = LutConfig(config)
			assertNotSame(config.contents, copy.contents)
		}
	}

	@Nested
	@DisplayName("setter tests")
	class SetterTests {
		val cfg = LutConfig(OperatingMode.LUT, basicLutContents)

		@Test
		@DisplayName("null operatingMode causes NPE")
		fun nullOperatingMode() {
			assertThrows(NPE, { cfg.operatingMode = null })
		}

		@Test
		@DisplayName("null contents causes NPE")
		fun nullContents() {
			assertThrows(NPE, { cfg.contents = null })
		}
	}


	// TODO make test cases for the remaining

	// parse tests
	// isStaticSourceCheck
	// isVCC and isGnd checks
	// configure as VCC and GND source
	// toXDLAttribute tests
}
