package edu.byu.ece.rapidSmith.util.luts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import java.lang.AssertionError
import java.util.*

class LutContentsTests {
//	@Nested
//	class ParseInitStringTests {
//		@Test
//		fun testParseInitString() {
//
//		}
//	}
//
//	@Nested
//	class ParseEquationTests {
//		@Test
//		@DisplayName("when =0, returns always false equation")
//		fun testParseZero() {
//			val given = LutContents.parseEquation("0")
//			val initString = given.copyOfInitString
//			val expected = InitString(0L)
//			assertEquals(expected, initString)
//		}
//
//		@Test
//		@DisplayName("when =1, returns always true equation")
//		fun testParseOne() {
//			val given = LutContents.parseEquation("1")
//			val initString = given.copyOfInitString
//			val expected = InitString(-1L) // maximum unsigned value
//			assertEquals(expected, initString)
//		}
//
//		@Test
//		@DisplayName("test unused A6 pin equation")
//		fun testUnusedA6PinEquation() {
//			val given = LutContents.parseEquation("((A6+~A6)*A5)")
//			val equation = given.equation
//			val left = LutInput(5)
//			val right = BinaryOperation(OpType.OR, LutInput(6), LutInput(6, true))
//			val expected = BinaryOperation(OpType.AND, left, right)
//			assertEquals(expected.toString(), equation.toString())
//		}
//
//		@TestFactory
//		@DisplayName("test random equations")
//		fun testRandomEquations(): List<DynamicTest> {
//			val random = Random(0xFEEDL)
//			return (1..50).map { buildRandomEquation(random) }
//				.map { dynamicTest("when =${it.second}") {
//					val actual = LutContents.parseEquation(it.second)
//					val expected = LutContents(it.first)
//					assertEquals(expected, actual)
//				} }
//		}
//
//		private fun buildRandomEquation(random: Random): Pair<LutEquation, String> {
//			val nextInt = random.nextInt(100)
//			return when (nextInt) {
//				in 0..59 -> {
//					val op = OpType.values()[nextInt / 20]
//					val left = buildRandomEquation(random)
//					val right = buildRandomEquation(random)
//					val eq = BinaryOperation(op, left.first, right.first)
//					val str = "${left.second}${op.symbol}${right.second}"
//					Pair(eq, str)
//				}
//				in 60..77 -> {
//					val input = (nextInt % 6) + 1
//					Pair(LutInput(input, false), "~A$input")
//				}
//				in 60..95 -> {
//					val input = (nextInt % 6) + 1
//					Pair(LutInput(input, true), "A$input")
//				}
//				in 96..97 -> Pair(Constant.ZERO, "0")
//				in 98..99 -> Pair(Constant.ONE, "1")
//				else -> throw AssertionError("invalid random value")
//			}
//		}
//	}
}





abstract class ParseEquationTest {
	abstract val testCase: ParseEquationTestCase

	@Test
	fun equationIsIdentical() {
		val actual = LutContents.parseEquation(testCase.string)
		assertEquals(testCase.equation, actual.equation)
	}

	@Test
	fun equationStringIsIdentical() {
		val actual = LutContents.parseEquation(testCase.string)
		assertEquals(testCase.string, actual.equation.toString())
	}

	@Test
	fun deepCopyYieldsIdenticalEquation() {
		val contents = LutContents.parseEquation(testCase.string)

	}
}

data class ParseEquationTestCase(
	val equation: LutEquation,
	val string: String,
	val initString: Long
)

