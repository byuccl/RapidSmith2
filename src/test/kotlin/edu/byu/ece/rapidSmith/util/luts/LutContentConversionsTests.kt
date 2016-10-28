package edu.byu.ece.rapidSmith.util.luts

import org.junit.jupiter.api.*
import org.junit.jupiter.api.DynamicTest.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.*
import java.util.*

class LutContentConversionsTests {
	@TestFactory
	@DisplayName("convert InitString to LutEquation")
	fun testInitStringToLutEquation(): List<DynamicTest> {
		val symmTests = symmetrical.map {
			val (init, eq, name) = it
			dynamicTest(name) {
				val actual = LutEquation.convertToLutEquation(init)
				assertEquals(eq, actual)
			}
		}

		val sensitveTests = orderSensitive.map {
			val (init, eq, name) = it
			dynamicTest(name) {
				val actual = LutEquation.convertToLutEquation(init)
				orderSensitiveEquals(eq, actual)
			}
		}

		return symmTests + sensitveTests
	}

	@TestFactory
	@DisplayName("convert LutEquation to InitString")
	fun testLutEquationToInitString(): List<DynamicTest> {
		val tests = symmetrical + orderSensitive + asymmetrical

		return tests.map {
			val (init, eq, name) = it
			dynamicTest(name) {
				val actual = InitString.convertToInitString(eq, init.numInputs)
				assertEquals(init, actual)
			}
		}
	}

	@TestFactory
	@DisplayName("test LutEquation <-> InitString symmetry")
	fun testSymmetry(): List<DynamicTest> {
		val numRandomTests = 200
		val random = Random(0xDEADBEEFL)

		fun Random.nextNumInputs(): Int {
			val i = nextInt(100)
			return when (i) {
				in 0..40 -> 6
				in 41..80 -> 5
				in 80..86 -> 4
				in 87..92 -> 3
				in 93..97 -> 2
				in 98..99 -> 1
				else -> throw AssertionError("Random.nextInt() is broken")
			}
		}

		val tests = (1..numRandomTests).map {
			val initString = InitString(random.nextLong(), random.nextNumInputs())
			dynamicTest("$initString") {
				val asEq = LutEquation.convertToLutEquation(initString)
				val actual = InitString.convertToInitString(asEq, initString.numInputs)
				assertEquals(initString, actual)
			}
		}
		return tests
	}

	// test these both forward and back
	private val symmetrical: List<ConvertTestCase>
	private val orderSensitive: List<ConvertTestCase>
	// test these only eq -> initString
	private val asymmetrical: List<ConvertTestCase>

	init {
		val symmetrical = ArrayList<ConvertTestCase>()
		symmetrical += ConvertTestCase(InitString(0L, 6), Constant.ZERO)
		symmetrical += ConvertTestCase(InitString(0L, 5), Constant.ZERO)
		symmetrical += ConvertTestCase(InitString(-1L, 6), Constant.ONE)
		symmetrical += ConvertTestCase(InitString(-1L, 5), Constant.ONE, "(0xFFFFFFFFFFFFFFFF, 5) <-> 1")
		symmetrical += ConvertTestCase(InitString(0x0FFFFFFFFL, 5), Constant.ONE)
		symmetrical += ConvertTestCase(InitString(0x0FFFFFFFFL.inv(), 6),        LutInput(6))
		symmetrical += ConvertTestCase(InitString(0x0FFFFFFFFL, 6),              LutInput(6, true))
		symmetrical += ConvertTestCase(InitString(0x0FFFF0000FFFFL.inv(), 6),    LutInput(5))
		symmetrical += ConvertTestCase(InitString(0x0FFFF0000FFFFL, 6),          LutInput(5, true))
		symmetrical += ConvertTestCase(InitString(0x00FF00FF00FF00FFL.inv(), 6), LutInput(4))
		symmetrical += ConvertTestCase(InitString(0x00FF00FF00FF00FFL, 6),       LutInput(4, true))
		symmetrical += ConvertTestCase(InitString(0x0F0F0F0F0F0F0F0FL.inv(), 6), LutInput(3))
		symmetrical += ConvertTestCase(InitString(0x0F0F0F0F0F0F0F0FL, 6),       LutInput(3, true))
		symmetrical += ConvertTestCase(InitString(0x3333333333333333L.inv(), 6), LutInput(2))
		symmetrical += ConvertTestCase(InitString(0x3333333333333333L, 6),       LutInput(2, true))
		symmetrical += ConvertTestCase(InitString(0x5555555555555555L.inv(), 6), LutInput(1))
		symmetrical += ConvertTestCase(InitString(0x5555555555555555L, 6),       LutInput(1, true))

		symmetrical += ConvertTestCase(InitString(0x0FFFF0000FFFFL.inv(), 5),    LutInput(5), "(0xFFFF0000FFFF0000, 5) <-> A5")
		symmetrical += ConvertTestCase(InitString(0x0FFFF0000FFFFL, 5),          LutInput(5, true), "(0x000FFFF0000FFFF, 5) <-> ~A5")
		symmetrical += ConvertTestCase(InitString(0x00FF00FF00FF00FFL.inv(), 5), LutInput(4), "(0xFF00FF00FF00FF00, 5) <-> A5")
		symmetrical += ConvertTestCase(InitString(0x00FF00FF00FF00FFL, 5),       LutInput(4, true), "(0x00FF00FF00FF00FF, 5) <-> ~A4")
		symmetrical += ConvertTestCase(InitString(0x0F0F0F0F0F0F0F0FL.inv(), 5), LutInput(3), "(0xF0F0F0F0F0F0F0F0, 5) <-> A5")
		symmetrical += ConvertTestCase(InitString(0x0F0F0F0F0F0F0F0FL, 5),       LutInput(3, true), "(0x0F0F0F0F0F0F0F0F, 5) <-> ~A3")
		symmetrical += ConvertTestCase(InitString(0x3333333333333333L.inv(), 5), LutInput(2), "(0xCCCCCCCCCCCCCCCC, 5) <-> A5")
		symmetrical += ConvertTestCase(InitString(0x3333333333333333L, 5),       LutInput(2, true), "(0x3333333333333333, 5) <-> ~A2")
		symmetrical += ConvertTestCase(InitString(0x5555555555555555L.inv(), 5), LutInput(1), "(0xAAAAAAAAAAAAAAAA, 5) <-> A5")
		symmetrical += ConvertTestCase(InitString(0x5555555555555555L, 5),       LutInput(1, true), "(0x5555555555555555, 5) <-> ~A1")

		symmetrical += ConvertTestCase(InitString(0x0000FFFFL.inv(), 5),    LutInput(5))
		symmetrical += ConvertTestCase(InitString(0x0000FFFFL, 5),          LutInput(5, true))
		symmetrical += ConvertTestCase(InitString(0x00FF00FFL.inv(), 5), LutInput(4))
		symmetrical += ConvertTestCase(InitString(0x00FF00FFL, 5),       LutInput(4, true))
		symmetrical += ConvertTestCase(InitString(0x0F0F0F0FL.inv(), 5), LutInput(3))
		symmetrical += ConvertTestCase(InitString(0x0F0F0F0FL, 5),       LutInput(3, true))
		symmetrical += ConvertTestCase(InitString(0x33333333L.inv(), 5), LutInput(2))
		symmetrical += ConvertTestCase(InitString(0x33333333L, 5),       LutInput(2, true))
		symmetrical += ConvertTestCase(InitString(0x55555555L.inv(), 5), LutInput(1))
		symmetrical += ConvertTestCase(InitString(0x55555555L, 5),       LutInput(1, true))

		this.symmetrical = symmetrical

		val orderSensitive = ArrayList<ConvertTestCase>()

		orderSensitive += ConvertTestCase(InitString(0x0000FFFFL, 6), BinaryOperation(OpType.AND, LutInput(6, true), LutInput(5, true)))
		orderSensitive += ConvertTestCase(InitString(0x00FF00FFL, 6), BinaryOperation(OpType.AND, LutInput(6, true), LutInput(4, true)))
		orderSensitive += ConvertTestCase(InitString(0x0F0F0F0FL, 6), BinaryOperation(OpType.AND, LutInput(6, true), LutInput(3, true)))
		orderSensitive += ConvertTestCase(InitString(0x33333333L, 6), BinaryOperation(OpType.AND, LutInput(6, true), LutInput(2, true)))
		orderSensitive += ConvertTestCase(InitString(0x55555555L, 6), BinaryOperation(OpType.AND, LutInput(6, true), LutInput(1, true)))

		orderSensitive += ConvertTestCase(InitString(0x0000FFFFL.inv(), 6), BinaryOperation(OpType.OR, LutInput(6), LutInput(5)))
		orderSensitive += ConvertTestCase(InitString(0x00FF00FFL.inv(), 6), BinaryOperation(OpType.OR, LutInput(6), LutInput(4)))
		orderSensitive += ConvertTestCase(InitString(0x0F0F0F0FL.inv(), 6), BinaryOperation(OpType.OR, LutInput(6), LutInput(3)))
		orderSensitive += ConvertTestCase(InitString(0x33333333L.inv(), 6), BinaryOperation(OpType.OR, LutInput(6), LutInput(2)))
		orderSensitive += ConvertTestCase(InitString(0x55555555L.inv(), 6), BinaryOperation(OpType.OR, LutInput(6), LutInput(1)))

		this.orderSensitive = orderSensitive

		val assymetrical = ArrayList<ConvertTestCase>()

		assymetrical += ConvertTestCase(InitString(0x6666666666666666L, 6), BinaryOperation(OpType.XOR, LutInput(1), LutInput(2)))
		assymetrical += ConvertTestCase(InitString(-1, 6), BinaryOperation(OpType.OR, LutInput(6), LutInput(6, true)))
		assymetrical += ConvertTestCase(InitString(0, 6), BinaryOperation(OpType.AND, LutInput(6), LutInput(6, true)))

		this.asymmetrical = assymetrical
	}

	private fun orderSensitiveEquals(expected: LutEquation, actual: LutEquation) {
		assumeTrue { expected is BinaryOperation }
		expected as BinaryOperation
		assumeTrue { expected.left is LutInput }
		assumeTrue { expected.right is LutInput }
		val expectedInputs = setOf(expected.left, expected.right)

		assertTrue(actual is BinaryOperation)
		actual as BinaryOperation

		assertAll(
			Executable { assertEquals(expected.op, actual.op) },
			Executable { assertTrue(actual.left is LutInput) },
			Executable { assertTrue(actual.right is LutInput) },
			Executable { assertEquals(expectedInputs, setOf(actual.left, actual.right)) }
		)
	}

	private data class ConvertTestCase(
		val initString: InitString,
		val eq: LutEquation,
		val name: String = "($initString, ${initString.numInputs}) <-> $eq"
	)
}
