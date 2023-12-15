/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.test.assertFailsWithSerial
import kotlin.test.*

class JsonCoerceInputValuesTest : JsonTestBase() {
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

    @Serializable
    data class NullableEnumHolder(
        val enum: SampleEnum?
    )

    @Serializable
    class Uncoercable(
        val s: String
    )

    @Serializable
    class UncoercableEnum(
        val e: SampleEnum
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
        assertFailsWithSerial("JsonDecodingException") {
            json.decodeFromString(WithEnum.serializer(), """{"e":{"x":"definitely not a valid enum value"}}""")
        }
        assertFailsWithSerial("JsonDecodingException") { // test user still sees exception on missing quotes
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

    @Test
    fun testNullSupportForEnums() = parametrizedTest(json) {
        var decoded = decodeFromString<NullableEnumHolder>("""{"enum": null}""")
        assertNull(decoded.enum)

        decoded = decodeFromString<NullableEnumHolder>("""{"enum": OptionA}""")
        assertEquals(SampleEnum.OptionA, decoded.enum)
    }

    @Test
    fun propertiesWithoutDefaultValuesDoNotChangeErrorMsg() {
        val json2 = Json(json) { coerceInputValues = false }
        parametrizedTest { mode ->
            val e1 = assertFailsWith<SerializationException>() { json.decodeFromString<Uncoercable>("""{"s":null}""", mode) }
            val e2 = assertFailsWith<SerializationException>() { json2.decodeFromString<Uncoercable>("""{"s":null}""", mode) }
            assertEquals(e2.message, e1.message)
        }
    }

    @Test
    fun propertiesWithoutDefaultValuesDoNotChangeErrorMsgEnum() {
        val json2 = Json(json) { coerceInputValues = false }
        parametrizedTest { mode ->
            val e1 = assertFailsWith<SerializationException> { json.decodeFromString<UncoercableEnum>("""{"e":"UNEXPECTED"}""", mode) }
            val e2 = assertFailsWith<SerializationException> { json2.decodeFromString<UncoercableEnum>("""{"e":"UNEXPECTED"}""", mode) }
            assertEquals(e2.message, e1.message)
        }
    }
}
