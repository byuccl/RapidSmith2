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
