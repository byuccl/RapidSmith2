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

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import java.lang.AssertionError
import java.lang.IndexOutOfBoundsException
import java.util.*

private val NPE = NullPointerException::class.java

class LutEquationTests {
	private val numRandomTests = 200
	private val testCases: List<LutEqTestCase>

	init {
		val random = Random(0xDADBEBAD)

		// directed tests
		testCases = ArrayList()
		testCases += LutEqTestCase("0", "0", Constant.ZERO)
		testCases += LutEqTestCase("1", "1", Constant.ONE)
		(1..6).forEach {
			testCases += LutEqTestCase("A$it", "A$it", LutInput(it))
			testCases += LutEqTestCase("A$it", "A$it", LutInput(it, false)) // not inverted
			testCases += LutEqTestCase("~A$it", "~A$it", LutInput(it, true)) // inverted
			testCases += LutEqTestCase("(A$it)", "A$it", LutInput(it)) // inside parentheses
			testCases += LutEqTestCase("(~A$it)", "~A$it", LutInput(it, true)) // inside parentheses
		}
		testCases += LutEqTestCase("(A1*A2)", "(A1*A2)", BinaryOperation(OpType.AND, LutInput(1), LutInput(2)))
		testCases += LutEqTestCase("(A1+A2)", "(A1+A2)", BinaryOperation(OpType.OR, LutInput(1), LutInput(2)))
		testCases += LutEqTestCase("(A1@A2)", "(A1@A2)", BinaryOperation(OpType.XOR, LutInput(1), LutInput(2)))
		testCases += LutEqTestCase("A1*A2", "(A1*A2)", BinaryOperation(OpType.AND, LutInput(1), LutInput(2))) // without parens

		// 6LUT pass through
		var innerEq = BinaryOperation(OpType.OR, LutInput(6), LutInput(6, true))
		testCases += LutEqTestCase("(A6+~A6)*(A4)", "((A6+~A6)*A4)", BinaryOperation(OpType.AND, innerEq, LutInput(4)))

		innerEq = BinaryOperation(OpType.OR, LutInput(6), LutInput(6, true))
		testCases += LutEqTestCase("(A6+~A6)*A4", "((A6+~A6)*A4)", BinaryOperation(OpType.AND, innerEq, LutInput(4)))

		// random tests
		repeat(numRandomTests, { testCases += buildRandomEquation(random, 1) })
	}

	@Test
	@DisplayName("BinaryOperation children cannot be null")
	fun binaryOpChildrenNotNull() {
		assertAll(
			Executable { assertThrows(NPE, { BinaryOperation(OpType.OR, LutInput(1), null) }) },
			Executable { assertThrows(NPE, { BinaryOperation(OpType.OR, null, LutInput(2)) }) }
		)
	}

	@Test
	@DisplayName("BinaryOperation operation cannot be null")
	fun binaryOpOperationNotNull() {
		assertThrows(NPE, { BinaryOperation(null, LutInput(1), LutInput(2)) })
	}

	@TestFactory
	@DisplayName("test LutEquation.toString")
	fun test_toString(): List<DynamicTest> {
		return testCases.map { dynamicTest(it.string) {
			assertEquals(it.toString, it.equation.toString())
		} }
	}

	@TestFactory
	@DisplayName("test LutEquation.parse")
	fun test_parse(): List<DynamicTest> {
		return testCases.map { dynamicTest(it.string) {
			assertEquals(it.equation, LutEquation.parse(it.string))
		} }
	}

	@TestFactory
	@DisplayName("test LutEquation.deepCopy equality")
	fun test_deepCopy_equality(): List<DynamicTest> {
		return testCases.map { dynamicTest(it.string) {
			testEquality(it.equation, it.equation.deepCopy())
		} }
	}

	@TestFactory
	@DisplayName("test LutEquation.deepCopy uniqueness")
	fun test_deepCopy_uniqueness(): List<DynamicTest> {
		return testCases.map { dynamicTest(it.string) {
			testUniqueness(it.equation, LutEquation.parse(it.string))
		} }
	}

	@TestFactory
	@DisplayName("test LutEquation.equals")
	fun test_equals(): List<DynamicTest> {
		val random = Random(0x8888L)
		val firstSet = (1..250).map { testCases[random.nextInt(testCases.size)] }
		val tests = firstSet.zip((1..250).map { testCases[random.nextInt(testCases.size)] })

		return tests.map {
				val(first, second) = it
				val equal = first.toString == second.toString
				val testName = "$first${if (equal) "==" else "!="}$second"

				dynamicTest(testName) {
					if (equal)
						testEquality(first.equation, second.equation)
					else
						testInequality(first.equation, second.equation)
				}
			}
	}

	@Test
	@DisplayName("equation is not equal to its equivalent but not same format form")
	fun EquivalentButDifferentFormattedEquationsAreNotEqual() {
		val eqSub = BinaryOperation(OpType.AND, LutInput(2), LutInput(3))
		val eq1 = BinaryOperation(OpType.AND, eqSub, LutInput(2))
		val eq2 = BinaryOperation(OpType.AND, LutInput(2), eqSub)

		testInequality(eq1, eq2)
	}

	@Test
	@DisplayName("equation is not equal to its minimized self")
	fun functionallyEquivalentButDifferentFormattedEquationsAreNotEqual() {
		val eqSub = BinaryOperation(OpType.OR, LutInput(6), LutInput(6, true))
		val eq1 = BinaryOperation(OpType.AND, eqSub, LutInput(3))
		val eq2: LutEquation = LutInput(3)

		testInequality(eq1, eq2)
	}

	@TestFactory
	@DisplayName("test remap with empty map")
	fun testRemapWithEmptyMap(): List<DynamicTest> {
		val mapping = emptyMap<Int, Int>()
		return testCases.map { dynamicTest(it.equation.toString(), {
			val copy = it.equation.deepCopy()
			copy.remapPins(mapping)
			compareWithRemapped(it.equation, copy, mapping)
		}) }
	}

	@TestFactory
	@DisplayName("test direct remapping")
	fun testDirectRemapping(): List<DynamicTest> {
		val mapping = (1..6).associate { Pair(it, it) }
		return testCases.map { dynamicTest(it.equation.toString(), {
			val copy = it.equation.deepCopy()
			copy.remapPins(mapping)
			compareWithRemapped(it.equation, copy, mapping)
		}) }
	}

	@TestFactory
	@DisplayName("test fully specified remap")
	fun testFullySpecifiedRemap(): List<DynamicTest> {
		val numMappingsToTest = 10
		val random = Random(0x444444L)
		val mappings = Array(numMappingsToTest, {
			val remainingInputs = arrayListOf(1, 2, 3, 4, 5, 6)
			Collections.shuffle(remainingInputs, random)
			(1..6).associate { Pair(it, remainingInputs.removeLast()) }
		})

		return testCases.flatMap { t -> mappings.map { sh -> Pair(t, sh) } }
			.map {
				val (case, mapping) = it
				val testName = "${case.equation} remapped with $mapping"
				dynamicTest(testName) {
					val copy = case.equation.deepCopy()
					copy.remapPins(mapping)
					compareWithRemapped(case.equation, copy, mapping)
				}
			}
	}

	@TestFactory
	@DisplayName("test remap with partial map")
	fun testRemapWithPartialMap(): List<DynamicTest> {
		val numMappingsToTest = 10
		val random = Random(0x444444L)
		val mappings = Array(numMappingsToTest, {
			val remainingInputs = arrayListOf(1, 2, 3, 4, 5, 6)
			Collections.shuffle(remainingInputs, random)
			(1..random.nextInt(5)+1).associate { Pair(it, remainingInputs.removeLast()) }
		})

		return testCases.flatMap {t -> mappings.map { sh -> Pair(t, sh) } }
			.map {
				val (case, mapping) = it
				val testName = "${case.equation} remapped with $mapping"
				dynamicTest(testName) {
					val copy = case.equation.deepCopy()
					copy.remapPins(mapping)
					compareWithRemapped(case.equation, copy, mapping)
				}
			}
	}

	@TestFactory
	@DisplayName("test LutEquation.getUsedInputs")
	fun test_getUsedInputs(): List<DynamicTest> {
		return testCases.map { dynamicTest(it.string) {
			val expected = computeUsedInputs(it.equation)
			assertEquals(expected, it.equation.usedInputs)
		} }
	}
}

private fun buildRandomEquation(random: Random, depth: Int): LutEqTestCase {
	val nextInt = random.nextInt(100)

	return when (nextInt) {
		in 0..95/depth -> {
			val op = OpType.values()[nextInt % 3]
			val symbol = when(op) {
				OpType.AND -> '*'
				OpType.OR -> '+'
				OpType.XOR -> '@'
			}

			val (leftStr, unused1, leftEq) = buildRandomEquation(random, depth+1)
			val (rightStr, unused2, rightEq) = buildRandomEquation(random, depth+1)
			val str = "($leftStr$symbol$rightStr)"
			val equation = BinaryOperation(op, leftEq, rightEq)
			LutEqTestCase(str, str, equation)
		}
		98 -> LutEqTestCase("0", "0", Constant.ZERO)
		99 -> LutEqTestCase("1", "1", Constant.ONE)
		else -> {
			val input = (nextInt % 6) + 1
			val inverted = if (nextInt % 2 == 0) true else false
			val str = "${if (inverted) "~" else ""}A$input"
			LutEqTestCase(str, str, LutInput(input, inverted))
		}
	}
}

private fun testUniqueness(original: LutEquation, copy: LutEquation) {
	assertEquals(original.javaClass, copy.javaClass)
	return when(original) {
		is Constant -> { /* immutable */ }
		is LutInput -> assertNotSame(original, copy)
		is BinaryOperation -> {
			assertNotSame(original, copy)
			copy as BinaryOperation
			testUniqueness(original.left, copy.left)
			testUniqueness(original.right, copy.right)
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

private fun testInequality(eq1: LutEquation, eq2: LutEquation) {
	assertAll(
		Executable { assertTrue { eq1 != eq2 } },
		Executable { assertTrue { eq2 != eq1 } }
	)
}

private fun compareWithRemapped(
	orig: LutEquation, remapped: LutEquation, mapping: Map<Int, Int>
) {
	when (orig) {
		is BinaryOperation -> {
			assertEquals(BinaryOperation::class.java, remapped.javaClass)
			remapped as BinaryOperation
			compareWithRemapped(orig.left, remapped.left, mapping)
			compareWithRemapped(orig.right, remapped.right, mapping)
		}
		is LutInput -> {
			assertEquals(LutInput::class.java, remapped.javaClass)
			remapped as LutInput

			val newIndex = mapping[orig.index] ?: orig.index
			assertEquals(newIndex, remapped.index)
		}
		is Constant -> {
			assertEquals(Constant::class.java, remapped.javaClass)
			assertSame(orig, remapped)
		}
	}
}

private fun computeUsedInputs(equation: LutEquation): Set<Int> {
	val q: Queue<LutEquation> = ArrayDeque()
	q.add(equation)

	val usedInputs = HashSet<Int>()
	while (q.isNotEmpty()) {
		val next = q.poll()
		when (next) {
			is BinaryOperation -> { q.add(next.left); q.add(next.right) }
			is LutInput -> usedInputs += next.index
			is Constant -> {} // nothing
			else -> throw AssertionError("illegal LutEquation type")
		}
	}
	return usedInputs
}

private fun <K> ArrayList<K>.removeLast(): K {
	val lastIndex = size - 1
	if (lastIndex < 0)
		throw IndexOutOfBoundsException()
	return removeAt(lastIndex)
}

private data class LutEqTestCase(
		val string: String,
		val toString: String,
		val equation: LutEquation
)


