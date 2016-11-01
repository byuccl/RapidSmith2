package edu.byu.ece.rapidSmith.util.luts

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest.*

private val IAE = IllegalArgumentException::class.java


class InitStringTests {
	@Test
	@DisplayName("construct with 6 inputs")
	fun constructWithSixInputs() {
		val init = InitString(0x6FFFFFFFEEEEL, 6)
		assertAll(
			Executable { assertEquals(0x6FFFFFFFEEEEL, init.cfgValue) },
			Executable { assertEquals(6, init.numInputs) }
		)
	}

	@Test
	@DisplayName("copy constructor")
	fun copyConstructor() {
		val init = InitString(0x6FFFFFFFEEEEL, 6)
		val copy = InitString(init)
		assertAll(
			Executable { assertEquals(0x6FFFFFFFEEEEL, copy.cfgValue) },
			Executable { assertEquals(6, copy.numInputs) }
		)
	}

	@Test
	@DisplayName("constructor truncates overly large value -- 5 inputs")
	fun truncateConstructor5Inputs() {
		val init = InitString(0x0FFFFFFFFEL, 5)
		assertEquals(0x0FFFFFFFEL, init.cfgValue)
	}

	@Test
	@DisplayName("constructor truncates overly large value -- 4 inputs")
	fun truncateConstructor4Inputs() {
		val init = InitString(0x0FFFFFFFFEL, 4)
		assertEquals(0x0FFFEL, init.cfgValue)
	}

	@Test
	@DisplayName("constructor truncates overly large value -- 2 inputs")
	fun truncateConstructor2Inputs() {
		val init = InitString(0x0FFFFFFFFEL, 2)
		assertEquals(0xEL, init.cfgValue)
	}

	@Test
	@DisplayName("constructor truncates overly large value -- 1 inputs")
	fun truncateConstructor1Inputs() {
		val init = InitString(0x0FFFFFFFFEL, 1)
		assertEquals(0x2L, init.cfgValue)
	}

	@Test
	@DisplayName("error on too large input to default constructor")
	fun errorLargeInputDefault() {
		assertThrows(IAE, { InitString(0x0F, 7) })
	}

	@Test
	@DisplayName("error on too short input to default constructor")
	fun errorShortInputDefault() {
		assertThrows(IAE, { InitString(0x0F, 0) })
	}

	@Test
	@DisplayName("error on too large input to resize")
	fun errorLargeInputResize() {
		val init = InitString(0x0F, 6)
		assertThrows(IAE, { init.resize(7) })
	}

	@Test
	@DisplayName("error on too short input to resize")
	fun errorShortInputResize() {
		val init = InitString(0x0F, 6)
		assertThrows(IAE, { init.resize(0) })
	}

	@Test
	@DisplayName("downsize truncates -- 5 inputs")
	fun downsize5Inputs() {
		val init = InitString(0x0FFFFFFFFEL, 6)
		init.resize(5)
		assertEquals(0xFFFFFFFEL, init.cfgValue)
	}

	@Test
	@DisplayName("downsize truncates -- 2 inputs")
	fun downsize2Inputs() {
		val init = InitString(0x0FFFFFFFFEL, 6)
		init.resize(2)
		assertEquals(0xE, init.cfgValue)
	}

	@Test
	@DisplayName("downsize truncates -- 1 inputs")
	fun downsize1Inputs() {
		val init = InitString(0x0FFFFFFFFEL, 6)
		init.resize(1)
		assertEquals(2L, init.cfgValue)
	}

	@Test
	@DisplayName("resize same size does not affect -- 6 inputs")
	fun resizeSameSize6Inputs() {
		val init = InitString(0x0FFFFFFFFEL, 6)
		init.resize(6)
		assertEquals(0x0FFFFFFFFEL, init.cfgValue)
	}

	@Test
	@DisplayName("resize same size does not affect -- 5 inputs")
	fun resizeSameSize5Inputs() {
		val init = InitString(0x0FFEL, 5)
		init.resize(5)
		assertEquals(0x0FFEL, init.cfgValue)
	}

	@Test
	@DisplayName("resize same size does not affect -- 1 inputs")
	fun resizeSameSize1Inputs() {
		val init = InitString(2L, 1)
		init.resize(1)
		assertEquals(2L, init.cfgValue)
	}

	@Test
	@DisplayName("upsize duplicates -- 1 -> 5 inputs")
	fun upsize1To5Inputs() {
		val init = InitString(1L, 1)
		init.resize(5)
		assertEquals(0x55555555L, init.cfgValue)
	}

	@Test
	@DisplayName("upsize duplicates -- 1 -> 6 inputs")
	fun upsize1To6Inputs() {
		val init = InitString(1L, 1)
		init.resize(6)
		assertEquals(0x5555555555555555L, init.cfgValue)
	}

	@Test
	@DisplayName("upsize duplicates -- 4 -> 5 inputs")
	fun upsize4To5Inputs() {
		val init = InitString(1L, 4)
		init.resize(5)
		assertEquals(0x00010001L, init.cfgValue)
	}

	@Test
	@DisplayName("upsize duplicates -- 4 -> 6 inputs")
	fun upsize4To6Inputs() {
		val init = InitString(1L, 4)
		init.resize(6)
		assertEquals(0x0001000100010001L, init.cfgValue)
	}

	@Test
	@DisplayName("resize updates the numInputs variable -- growth")
	fun resizeUpdateInputsGrowth() {
		val init = InitString(1L, 4)
		init.resize(5)
		assertEquals(5, init.numInputs)
	}

	@Test
	@DisplayName("resize to same maintains the numInputs variable")
	fun resizeUpdateInputsSame() {
		val init = InitString(1L, 4)
		init.resize(4)
		assertEquals(4, init.numInputs)
	}

	@Test
	@DisplayName("resize updates the numInputs variable -- shrink")
	fun resizeUpdateInputs() {
		val init = InitString(1L, 4)
		init.resize(3)
		assertEquals(3, init.numInputs)
	}

	@Test
	@DisplayName("test equality")
	fun testEquality() {
		val init1 = InitString(1L, 4)
		val init2 = InitString(1L, 4)
		assertAll(
			Executable { assertTrue(init1 == init2) },
			Executable { assertTrue(init2 == init1) },
			Executable { assertEquals(init1.hashCode(), init2.hashCode()) }
		)
	}

	@Test
	@DisplayName("equals checks number of inputs")
	fun testEqualsDifferentInputs() {
		val init1 = InitString(1L, 4)
		val init2 = InitString(1L, 5)

		assertAll(
			Executable { assertTrue(init1 != init2) },
			Executable { assertTrue(init2 != init1) }
		)
	}

	@TestFactory
	@DisplayName("toString tests")
	fun test_toString(): List<DynamicTest> {
		val tests = listOf(
			Pair(InitString(2, 1), "0x2"),
			Pair(InitString(5, 2), "0x5"),
			Pair(InitString(0x0E, 3), "0x0E"),
			Pair(InitString(0x0E, 4), "0x000E"),
			Pair(InitString(0x0E, 5), "0x0000000E"),
			Pair(InitString(0x0E, 6), "0x000000000000000E"),
			Pair(InitString(0x5555555555555555L.inv(), 6), "0xAAAAAAAAAAAAAAAA")
		)

		return tests.map { dynamicTest(it.second) {
			assertEquals(it.second, it.first.toString())
		} }
	}

	@TestFactory
	@DisplayName("parse tests")
	fun test_parse(): List<DynamicTest> {
		val tests = listOf(
			Pair(InitString(2, 1), "0x2"),
			Pair(InitString(5, 2), "0x5"),
			Pair(InitString(0x0E, 3), "0x0E"),
			Pair(InitString(0x0E, 4), "0x000E"),
			Pair(InitString(0x0E, 5), "0x0000000E"),
			Pair(InitString(0x0E, 6), "0x000000000000000E"),
			Pair(InitString(0x5555555555555555L.inv(), 6), "0xAAAAAAAAAAAAAAAA"),
			Pair(InitString(2, 1), "0b10"),
			Pair(InitString(5, 2), "0b0101"),
			Pair(InitString(0x0E, 3), "0b1110"),
			Pair(InitString(0x0E, 4), "0b0001110"),
			Pair(InitString(0xFE, 5), "0b00011111110"),
			Pair(InitString(0x6E, 6), "0b0000000000001101110"),
			Pair(InitString(0x5555555555555555L.inv(), 6), "0b1010101010101010101010101010101010101010101010101010101010101010")
		)

		return tests.map { dynamicTest(it.second) {
			assertEquals(it.first, InitString.parse(it.second, it.first.numInputs))
		} }
	}

	@Test
	@DisplayName("error on too large input to parse")
	fun errorLargeInputParse() {
		assertThrows(IAE, { InitString.parse("0x0F", 7) })
	}

	@Test
	@DisplayName("error on too short input to parse")
	fun errorShortInputParse() {
		assertThrows(IAE, { InitString.parse("0x0F", 0) })
	}
}
