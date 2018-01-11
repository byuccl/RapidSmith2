package edu.byu.ece.rapidSmith.device

import edu.byu.ece.rapidSmith.RSEnvironment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for parsing the XY coordinates from a site name.
 */
class SiteNameTests {

	@Test
	@DisplayName("Site coordinates are deserialized correctly")
	fun deserializeNames() {
		// Test that Artix device site coordinates match those expected by the name
		val dev = RSEnvironment.defaultEnv().getDevice("xc7a100tcsg324")
		val xySite = dev.getSite("SLICE_X31Y102")
		assertEquals(31, xySite.instanceX)
		assertEquals(102, xySite.instanceY)
	}

	@Test
	@DisplayName("parseCoordinatesFromName parses indices from site name")
	fun simpleWorking() {
		val site = Site()
		val ret = site.parseCoordinatesFromName("SLICE_X5Y201")
		assertTrue(ret)
		assertEquals(5, site.instanceX)
		assertEquals(201, site.instanceY)
	}

	@Test
	@DisplayName("parseCoordinatesFromName ignores name without underscore")
	fun missingUnderscore() {
		val site = Site()
		val ret = site.parseCoordinatesFromName("SLICEX5Y201")
		assertFalse(ret)
		assertEquals(-1, site.instanceX)
		assertEquals(-1, site.instanceY)
	}

	@Test
	@DisplayName("parseCoordinatesFromName ignores site name without X")
	fun missingX() {
		val site = Site()
		val ret = site.parseCoordinatesFromName("SLICE_Y201")
		assertFalse(ret)
		assertEquals(-1, site.instanceX)
		assertEquals(-1, site.instanceY)
	}

	@Test
	@DisplayName("parseCoordinatesFromName ignores site name without Y")
	fun missingY() {
		val site = Site()
		val ret = site.parseCoordinatesFromName("SLICE_X201")
		assertFalse(ret)
		assertEquals(-1, site.instanceX)
		assertEquals(-1, site.instanceY)
	}

	@Test
	@DisplayName("parseCoordinatesFromName ignores site name without X coordinates")
	fun missingXCoordinates() {
		val site = Site()
		val ret = site.parseCoordinatesFromName("SLICE_XY201")
		assertFalse(ret)
		assertEquals(-1, site.instanceX)
		assertEquals(-1, site.instanceY)
	}

	@Test
	@DisplayName("parseCoordinatesFromName ignores site name without Y coordinates")
	fun missingYCoordinates() {
		val site = Site()
		val ret = site.parseCoordinatesFromName("SLICE_X201Y")
		assertFalse(ret)
		assertEquals(-1, site.instanceX)
		assertEquals(-1, site.instanceY)
	}
}
