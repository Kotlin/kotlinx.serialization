/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class JsonCoerceInputValuesDynamicTest {
    val json = Json {
        coerceInputValues = true
        isLenient = true
    }

    private fun <T> doTest(inputs: List<dynamic>, expected: T, serializer: KSerializer<T>) {
        for (input in inputs) {
            assertEquals(expected, json.decodeFromDynamic(serializer, input), "Failed on input: $input")
        }
    }

    @Test
    fun testUseDefaultOnNonNullableBooleanDynamic() = doTest(
        listOf(
            js("""{"b":false}"""),
            js("""{"b":null}"""),
            js("""{}"""),
        ),
        JsonCoerceInputValuesTest.WithBoolean(),
        JsonCoerceInputValuesTest.WithBoolean.serializer()
    )

    @Test
    fun testUseDefaultOnUnknownEnum() {
        doTest(
            listOf(
                js("""{"e":"unknown_value"}"""),
                js("""{"e":null}"""),
                js("""{}"""),
            ),
            JsonCoerceInputValuesTest.WithEnum(),
            JsonCoerceInputValuesTest.WithEnum.serializer()
        )
        assertFailsWith<SerializationException> {
            json.decodeFromDynamic(
                JsonCoerceInputValuesTest.WithEnum.serializer(),
                js("""{"e":{"x":"definitely not a valid enum value"}}""")
            )
        }
    }

    @Test
    fun testUseDefaultInMultipleCases() {
        val testData = mapOf<dynamic, JsonCoerceInputValuesTest.MultipleValues>(
            Pair(
                js("""{"data":{"data":"foo"},"data2":null,"i":null,"e":null,"foo":"bar"}"""),
                JsonCoerceInputValuesTest.MultipleValues(
                    StringData("foo"),
                    foo = "bar"
                )
            ),
            Pair(
                js("""{"data":{"data":"foo"},"data2":{"intV":42},"i":null,"e":null,"foo":"bar"}"""),
                JsonCoerceInputValuesTest.MultipleValues(
                    StringData(
                        "foo"
                    ), IntData(42), foo = "bar"
                )
            ),
            Pair(
                js("""{"data":{"data":"foo"},"data2":{"intV":42},"i":0,"e":"NoOption","foo":"bar"}"""),
                JsonCoerceInputValuesTest.MultipleValues(
                    StringData("foo"),
                    IntData(42),
                    i = 0,
                    foo = "bar"
                )
            ),
            Pair(
                js("""{"data":{"data":"foo"},"data2":{"intV":42},"i":0,"e":"OptionC","foo":"bar"}"""),
                JsonCoerceInputValuesTest.MultipleValues(
                    StringData("foo"),
                    IntData(42),
                    i = 0,
                    e = SampleEnum.OptionC,
                    foo = "bar"
                )
            ),
        )
        for ((input, expected) in testData) {
            assertEquals(
                expected,
                json.decodeFromDynamic(JsonCoerceInputValuesTest.MultipleValues.serializer(), input),
                "Failed on input: $input"
            )
        }
    }
}
