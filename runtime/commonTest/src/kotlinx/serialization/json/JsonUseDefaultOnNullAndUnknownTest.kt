/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import kotlin.test.*

class JsonUseDefaultOnNullAndUnknownTest : JsonTestBase() {
    @Serializable
    data class WithBoolean(val b: Boolean = false)

    @Serializable
    data class WithEnum(val e: SampleEnum = SampleEnum.OptionC)

    @Serializable
    data class MultipleValues(
        val data: StringData,
        val data2: IntData = IntData(0),
        val i: Int = 42,
        val e: SampleEnum = SampleEnum.OptionA,
        val foo: String
    )

    val json = Json {
        coerceInputValues = true
        isLenient = true
    }

    private fun <T> doTest(inputs: List<String>, expected: T, serializer: KSerializer<T>) {
        for (input in inputs) {
            parametrizedTest(json) {
                assertEquals(expected, decodeFromString(serializer, input), "Failed on input: $input")
            }
        }
    }

    @Test
    fun testUseDefaultOnNonNullableBoolean() = doTest(
        listOf(
            """{"b":false}""",
            """{"b":null}""",
            """{}""",
        ),
        WithBoolean(),
        WithBoolean.serializer()
    )

    @Test
    fun testUseDefaultOnUnknownEnum() {
        doTest(
            listOf(
                """{"e":unknown_value}""",
                """{"e":"unknown_value"}""",
                """{"e":null}""",
                """{}""",
            ),
            WithEnum(),
            WithEnum.serializer()
        )
        assertFailsWith<JsonDecodingException> {
            json.decodeFromString(WithEnum.serializer(), """{"e":{"x":"definitely not a valid enum value"}}""")
        }
        assertFailsWith<JsonDecodingException> { // test user still sees exception on missing quotes
            Json(json) { isLenient = false }.decodeFromString(WithEnum.serializer(), """{"e":unknown_value}""")
        }
    }

    @Test
    fun testUseDefaultInMultipleCases() {
        val testData = mapOf(
            """{"data":{"data":"foo"},"data2":null,"i":null,"e":null,"foo":"bar"}""" to MultipleValues(
                StringData("foo"),
                foo = "bar"
            ),
            """{"data":{"data":"foo"},"data2":{"intV":42},"i":null,"e":null,"foo":"bar"}""" to MultipleValues(
                StringData(
                    "foo"
                ), IntData(42), foo = "bar"
            ),
            """{"data":{"data":"foo"},"data2":{"intV":42},"i":0,"e":"NoOption","foo":"bar"}""" to MultipleValues(
                StringData("foo"),
                IntData(42),
                i = 0,
                foo = "bar"
            ),
            """{"data":{"data":"foo"},"data2":{"intV":42},"i":0,"e":"OptionC","foo":"bar"}""" to MultipleValues(
                StringData("foo"),
                IntData(42),
                i = 0,
                e = SampleEnum.OptionC,
                foo = "bar"
            ),
        )
        for ((input, expected) in testData) {
            assertEquals(expected, json.decodeFromString(MultipleValues.serializer(), input), "Failed on input: $input")
        }
    }

}
