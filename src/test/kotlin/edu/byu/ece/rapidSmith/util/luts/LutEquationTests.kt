package edu.byu.ece.rapidSmith.util.luts

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import java.lang.AssertionError
import java.util.*

class LutEquationTests {
	@Test
	@DisplayName("test constant 0")
	fun zero() {
		makeLutEquationBaseTest(Constant.ZERO).executable.execute()
	}

	@Test
	@DisplayName("test constant 1")
	fun one() {
		makeLutEquationBaseTest(Constant.ONE).executable.execute()
	}

	@Test
	@DisplayName("test single LutInput")
	fun singleLutInput() {
		makeLutEquationBaseTest(LutInput(1)).executable.execute()
	}

	@Test
	@DisplayName("test inverted LutInput")
	fun invertedLutInput() {
		makeLutEquationBaseTest(LutInput(1, true)).executable.execute()
	}

	@TestFactory
	@DisplayName("test binary operations")
	fun testBinaryOperations(): List<DynamicTest> {
		return OpType.values().map { BinaryOperation(it, LutInput(1), LutInput(2)) }
			.map(::makeLutEquationBaseTest)
	}

	@TestFactory
	@DisplayName("test random LutEquations")
	fun testRandom(): List<DynamicTest> {
		val random = Random(0xDADBEBAD)
		return Array(200, {
					makeLutEquationBaseTest(buildRandomEquation(random, 1))
		}).toList()
	}

	// TODO move to coherence test class
	@Test
	@DisplayName("test init string 0x0000_0000_0000_0000")
	fun testConvertZeroInitString() {
		val eq = LutEquation.convertToLutEquation(InitString(0, 6))
		assertEquals(Constant.ZERO, eq)
	}

	@Test
	@DisplayName("test init string 0xFFFF_FFFF_FFFF_FFFF")
	fun testConvertOneInitString() {
		// -1 = 0xFFFF
		val eq = LutEquation.convertToLutEquation(InitString(-1, 6))
		assertEquals(Constant.ONE, eq)
	}

	@Test
	@DisplayName("test init string 0x0000_0000_FFFF_FFFF")
	fun testConvertInvertedA6InitString() {
		val eq = LutEquation.convertToLutEquation(InitString(0xFFFFFFFFL, 6))
		assertEquals(LutInput(6, true), eq)
	}
}

private fun makeLutEquationBaseTest(equation: LutEquation): DynamicTest {
	return dynamicTest(equation.toString()) {
		assertAll(
			Executable { toStringTest(equation) },
			Executable { deepCopyEqualityTest(equation) },
			Executable { deepCopyUniquenessTest(equation) }
		)
	}
}

private fun toStringTest(equation: LutEquation) {
	val strValue = equation.toString()
	val parsed = LutEquation.parse(strValue)
	testEquality(equation, parsed)
}

private fun deepCopyEqualityTest(equation: LutEquation) {
	val copy = equation.deepCopy()
	testEquality(equation, copy)
}

private fun deepCopyUniquenessTest(equation: LutEquation) {
	val copy = equation.deepCopy()
	isUnique(equation, copy)
}

private fun isUnique(original: LutEquation, copy: LutEquation) {
	assertEquals(original::class, copy::class)
	return when(original) {
		is Constant -> { /* immutable */ }
		is LutInput -> assertNotSame(original, copy)
		is BinaryOperation -> {
			assertNotSame(original, copy)
			copy as BinaryOperation
			isUnique(original.left, copy.left)
			isUnique(original.right, copy.right)
		}
		else -> throw AssertionError("unrecognized LutEquation subclass")
	}
}

private fun testEquality(expected: LutEquation, actual: LutEquation) {
	assertAll(
		Executable { assertTrue { expected == actual } },
		Executable { assertTrue { actual == expected } },
		Executable { assertEquals(expected.hashCode(), actual.hashCode()) }
	)
}

private fun buildRandomEquation(random: Random, depth: Int): LutEquation {
	val nextInt = random.nextInt(100)

	return when (nextInt) {
		in 0..95/depth -> {
			val op = OpType.values()[nextInt % 3]
			val left = buildRandomEquation(random, depth+1)
			val right = buildRandomEquation(random, depth+1)
			BinaryOperation(op, left, right)
		}
		98 -> Constant.ZERO
		99 -> Constant.ONE
		else -> {
			val input = (nextInt % 6) + 1
			LutInput(input, if (nextInt % 2 == 0) true else false)
		}
	}
}
