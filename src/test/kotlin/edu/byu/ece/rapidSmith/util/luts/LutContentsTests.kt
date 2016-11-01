package edu.byu.ece.rapidSmith.util.luts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.*

private val NPE = NullPointerException::class.java
private val IAE = IllegalArgumentException::class.java

class LutContentsTests {
	@Nested
	@DisplayName("tests for equation constructor")
	class EquationConstructor {
		@Test
		@DisplayName("null argument to equation parameter causes NPE")
		fun nullEquationParam() {
			assertThrows(NPE, { LutContents(null as LutEquation?, 5) })
		}

		@Test
		@DisplayName("numInputs argument > NUM_SUPPORTED_INPUTS causes IAE")
		fun tooLargeNumInputs() {
			assertThrows(IAE, {
				LutContents(Constant.ZERO, InitString.MAX_SUPPORTED_INPUTS + 1)
			})
		}

		@Test
		@DisplayName("numInputs argument < 1 causes IAE")
		fun tooSmallNumInputs() {
			assertThrows(IAE, { LutContents(Constant.ZERO, 0) })
		}

		@Test
		@DisplayName("equation is equal to provided reducible equation argument")
		fun equationEquals() {
			val eq = BinaryOperation(OpType.OR, LutInput(6), LutInput(6, true))
			val contents = LutContents(eq, 6)
			assertEquals(eq, contents.equation)
		}

		@Test
		@DisplayName("numInputs is same as provided argument")
		fun numInputsEquals() {
			val contents = LutContents(Constant.ONE, 4)
			assertEquals(4, contents.numInputs)
		}
	}

	@Nested
	@DisplayName("tests for InitString constructor")
	class InitStringConstructor {
		@Test
		@DisplayName("null argument to initString parameter causes NPE")
		fun nullInitStringParam() {
			assertThrows(NPE, { LutContents(null as InitString?, 5) })
		}

		@Test
		@DisplayName("numInputs argument > NUM_SUPPORTED_INPUTS causes IAE")
		fun tooLargeNumInputs() {
			assertThrows(IAE, {
				LutContents(InitString(0, 3), InitString.MAX_SUPPORTED_INPUTS + 1)
			})
		}

		@Test
		@DisplayName("numInputs argument < 1 causes IAE")
		fun tooSmallNumInputs() {
			assertThrows(IAE, { LutContents(InitString(0, 3), 0) })
		}

		@Test
		@DisplayName("initString is equal to provided argument")
		fun equationEquals() {
			val init = InitString(0x55L, 6)
			val contents = LutContents(init, 6)
			assertEquals(init, contents.initString)
		}

		@Test
		@DisplayName("initString resized when strings numInputs value differs from numInputs argument")
		fun initStringResized() {
			val init = InitString(0x55L, 6)
			val contents = LutContents(init, 2)

			val copy = InitString(init)
			copy.resize(2)
			assertEquals(copy, contents.initString)
		}

		@Test
		@DisplayName("numInputs is same as provided argument")
		fun numInputsEquals() {
			val contents = LutContents(Constant.ONE, 4)
			assertEquals(4, contents.numInputs)
		}
	}

	@Nested
	@DisplayName("tests for copy constructor")
	class CopyConstructor {
		@Test
		@DisplayName("null argument to other causes NPE")
		fun nullInitStringParam() {
			assertThrows(NPE, { LutContents(null as LutContents?) })
		}

		@Test
		@DisplayName("copy is equivalent to original")
		fun copyEquals() {
			val init = InitString(0x55L, 6)
			val contents = LutContents(init, 6)
			assertEquals(contents, LutContents(contents))
		}

		@Test
		@DisplayName("copied equation is equivalent to original reducible equation")
		fun equationEquals() {
			val eq = BinaryOperation(OpType.OR, LutInput(6), LutInput(6, true))
			val contents = LutContents(eq, 6)
			assertEquals(eq, LutContents(contents).equation)
		}

		@Test
		@DisplayName("numInputs of copy is same as original")
		fun numInputsEquals() {
			val contents = LutContents(Constant.ONE, 4)
			assertEquals(4, LutContents(contents).numInputs)
		}
	}

	@Nested
	@DisplayName("tests for getters")
	class TestGetters {
		@Test
		@DisplayName("getEquation returns a copy")
		fun getEquationReturnsCopy() {
			val eq = BinaryOperation(OpType.OR, LutInput(6), LutInput(6, true))
			val contents = LutContents(eq, 6)
			val modified = contents.equation
			modified.remapPins(mapOf(Pair(6, 4)))
			assertAll(
				Executable { assertNotSame(eq, contents.equation) },
				Executable { assertNotEquals(modified, contents.equation) }
			)
		}

		@Test
		@DisplayName("getInitString returns a copy")
		fun getInitStringReturnsCopy() {
			val init = InitString(0, 6)
			val contents = LutContents(init, 6)
			val modified = contents.initString
			modified.resize(3)
			assertAll(
				Executable { assertNotSame(init, contents.initString) },
				Executable { assertNotEquals(modified, contents.initString) }
			)
		}

		@Test
		@DisplayName("getInitString returns value even if not provided")
		fun initStringIsCreated() {
			val eq = BinaryOperation(OpType.OR, LutInput(6), LutInput(6, true))
			val contents = LutContents(eq, 6)
			assertEquals(InitString.convertToInitString(eq, 6), contents.initString)
		}

		@Test
		@DisplayName("getInitString returns value even if not provided")
		fun lutEquationIsCreated() {
			val init = InitString(0x4543234FL, 6)
			val contents = LutContents(init, 6)
			assertEquals(LutEquation.convertToLutEquation(init), contents.equation)
		}
	}

	@Nested
	@DisplayName("tests for updateConfiguration(LutEquation) method")
	class TestEquationUpdateConfiguration {
		abstract class Tests {
			abstract var contents: LutContents

			@Test
			@DisplayName("null argument for updateConfiguration causes NPE")
			fun nullEquationParam() {
				assertThrows(NPE, { contents.updateConfiguration(null as LutEquation?) })
			}

			@Test
			@DisplayName("equation is updated")
			fun equationEquals() {
				val eq = BinaryOperation(OpType.OR, LutInput(6), LutInput(6, true))
				val contents = LutContents(eq, 6)
				assertEquals(eq, contents.equation)
			}

			@Test
			@DisplayName("initString is updated")
			fun equivalentInitString() {
				val eq = BinaryOperation(OpType.OR, LutInput(6), LutInput(6, true))
				val contents = LutContents(eq, 6)
				assertEquals(InitString.convertToInitString(eq, 6), contents.initString)
			}
		}

		@Nested
		@DisplayName("InitString never initialized")
		class ConstructedWithoutInitString : Tests() {
			override lateinit var contents: LutContents

			@BeforeEach
			fun beforeEach() {
				contents = LutContents(Constant.ZERO, 6)
			}
		}

		@Nested
		@DisplayName("InitString initialized before tests")
		class InitStringCalled : Tests() {
			override lateinit var contents: LutContents

			@BeforeEach
			fun beforeEach() {
				contents = LutContents(Constant.ZERO, 6)
				contents.initString // make the initString not null
			}
		}
	}

	@Nested
	@DisplayName("tests for updateConfiguration(InitString) method")
	class TestInitStringUpdateConfiguration {
		abstract class Tests {
			abstract var contents: LutContents

			@Test
			@DisplayName("null argument for updateConfiguration causes NPE")
			fun nullEquationParam() {
				assertThrows(NPE, { contents.updateConfiguration(null as InitString?) })
			}

			@Test
			@DisplayName("initString is updated")
			fun equationEquals() {
				val initString = InitString(0x45678L, 6)
				val contents = LutContents(initString, 6)
				assertEquals(initString, contents.initString)
			}

			@Test
			@DisplayName("initString is updated")
			fun equivalentInitString() {
				val init = InitString(0x45678L, 6)
				val contents = LutContents(init, 6)
				assertEquals(LutEquation.convertToLutEquation(init), contents.equation)
			}

			@Test
			@DisplayName("initString is resized")
			fun initStringIsResized() {
				val init = InitString(0x45678L, 6)
				val contents = LutContents(init, 3)
				val copy = InitString(init)  // shouldn't need to make a copy but just to be safe
				copy.resize(3)
				assertEquals(copy, contents.initString)
			}
		}

		@Nested
		@DisplayName("InitString never initialized")
		class ConstructedWithoutInitString : Tests() {
			override lateinit var contents: LutContents

			@BeforeEach
			fun beforeEach() {
				contents = LutContents(InitString(0x123L, 6), 6)
			}
		}

		@Nested
		@DisplayName("InitString initialized before tests")
		class InitStringCalled : Tests() {
			override lateinit var contents: LutContents

			@BeforeEach
			fun beforeEach() {
				contents = LutContents(InitString(0x123L, 6), 6)
				contents.equation // make the equation is not null
			}
		}
	}

	@Nested
	@DisplayName("test updateNumInputs")
	class UpdateNameInputsTests {
		abstract class Tests {
			abstract var contents: LutContents

			@Test
			@DisplayName("numInputs argument > NUM_SUPPORTED_INPUTS causes IAE")
			fun tooLargeNumInputs() {
				assertThrows(IAE, {
					contents.updateNumInputs(InitString.MAX_SUPPORTED_INPUTS + 1)
				})
			}

			@Test
			@DisplayName("numInputs argument < 1 causes IAE")
			fun tooSmallNumInputs() {
				assertThrows(IAE, { contents.updateNumInputs(0) })
			}

			@Test
			@DisplayName("numInputs value is updated")
			fun numInputsUpdated() {
				contents.updateNumInputs(3)
				assertEquals(3, contents.numInputs)
			}

			@Test
			@DisplayName("equation is updated")
			fun equationUpdated() {
				contents.updateNumInputs(3)
				val expected = LutInput(1)
				assertEquals(expected, contents.equation)
			}

			@Test
			@DisplayName("initString is updated")
			fun initStringUpdated() {
				contents.updateNumInputs(3)
				val expected = InitString(0xAA, 3)
				assertEquals(expected, contents.initString)
			}
		}

		class InitializedWithEquation : Tests() {
			override lateinit var contents: LutContents

			@BeforeEach
			fun beforeEach() {
				val equation = BinaryOperation(OpType.OR, LutInput(4), LutInput(1))
				contents = LutContents(equation, 6)
			}
		}

		class InitializedWithInitString : Tests() {
			override lateinit var contents: LutContents

			@BeforeEach
			fun beforeEach() {
				val equation = BinaryOperation(OpType.OR, LutInput(4), LutInput(1))
				contents = LutContents(InitString.convertToInitString(equation, 6), 6)
			}
		}
	}

	@Test
	@DisplayName("test getUsedInputs with reducible equation")
	fun testGetUsedInputs() {
		val eq = BinaryOperation(OpType.OR, LutInput(6), LutInput(6, true))
		val contents = LutContents(eq, 6)
		assertEquals(setOf(6), contents.usedInputs)
	}

	@Test
	@DisplayName("test getUsedInputs on A6&A5")
	fun testGetUsedInputsWithA6AndA5() {
		val eq = BinaryOperation(OpType.AND, LutInput(6), LutInput(5, true))
		val contents = LutContents(eq, 6)
		assertEquals(setOf(5, 6), contents.usedInputs)
	}

	@Test
	@DisplayName("test getRequiredInputs on A6&~A6")
	fun testGetRequiredInputsWithZero() {
		val eq = BinaryOperation(OpType.AND, LutInput(6), LutInput(6, true))
		val contents = LutContents(eq, 6)
		assertEquals(emptySet<Int>(), contents.requiredInputs)
	}

	@Test
	@DisplayName("test getRequiredInputs on A6+~A6")
	fun testGetRequiredInputsWithOne() {
		val eq = BinaryOperation(OpType.OR, LutInput(6), LutInput(6, true))
		val contents = LutContents(eq, 6)
		assertEquals(emptySet<Int>(), contents.requiredInputs)
	}

	@Test
	@DisplayName("test getRequiredInputs on A6&A5")
	fun testGetRequiredInputsWithA6AndA5() {
		val eq = BinaryOperation(OpType.AND, LutInput(6), LutInput(5, true))
		val contents = LutContents(eq, 6)
		assertEquals(setOf(5, 6), contents.requiredInputs)
	}

	@Test
	@DisplayName("test reduceToMinSize with reducible equation")
	fun testReduceToMinSizeReducible() {
		val eq = BinaryOperation(OpType.OR, LutInput(6), LutInput(6, true))
		val contents = LutContents(eq, 6)
		contents.reduceToMinSize()
		assertAll(
			Executable { assertEquals(InitString(0, 6), contents.initString) },
			Executable { assertEquals(Constant.ZERO, contents.equation) }
		)
	}

	@Test
	@DisplayName("reduceToMinSize shifts inputs down to minimum possibles")
	fun testReduceToMinSizeShiftsDownInputs() {
		val eq = BinaryOperation(OpType.OR, LutInput(2), LutInput(6, true))
		val contents = LutContents(eq, 6)
		contents.reduceToMinSize()
		assertEquals(InitString(0xB, 2).resize(6), contents.initString)
	}

	@Test
	@DisplayName("equals should perform functional equivalency")
	fun testFunctionalEquivalence() {
		val eq1 = BinaryOperation(OpType.AND, LutInput(6), LutInput(6, true))
		val eq2 = Constant.ZERO

		val c1 = LutContents(eq1, 6)
		val c2 = LutContents(eq2, 6)
		assertAll(
			Executable { assertEquals(c1, c2) },
			Executable { assertEquals(c2, c1) },
			Executable { assertEquals(c1.hashCode(), c2.hashCode()) }
		)
	}
}




