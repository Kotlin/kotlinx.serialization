/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("ReplaceArrayOfWithLiteral") // https://youtrack.jetbrains.com/issue/KT-22578

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonNamesTest : JsonTestBase() {

    @Serializable
    data class WithNames(@JsonNames("foo", "_foo") val data: String)

    @Serializable
    enum class AlternateEnumNames {
        @JsonNames("someValue", "some_value")
        VALUE_A,
        VALUE_B
    }

    @Serializable
    data class WithEnumNames(
        val enumList: List<AlternateEnumNames>,
        val checkCoercion: AlternateEnumNames = AlternateEnumNames.VALUE_B
    )

    @Serializable
    data class CollisionWithAlternate(
        @JsonNames("_foo") val data: String,
        @JsonNames("_foo") val foo: String
    )

    private val inputString1 = """{"foo":"foo"}"""
    private val inputString2 = """{"_foo":"foo"}"""

    private fun parameterizedCoercingTest(test: (json: Json, streaming: JsonTestingMode, msg: String) -> Unit) {
        for (coercing in listOf(true, false)) {
            val json = Json {
                coerceInputValues = coercing
                useAlternativeNames = true
            }
            parametrizedTest { streaming ->
                test(
                    json, streaming,
                    "Failed test with coercing=$coercing and streaming=$streaming"
                )
            }
        }
    }

    @Test
    fun testEnumSupportsAlternativeNames() {
        val input = """{"enumList":["VALUE_A", "someValue", "some_value", "VALUE_B"], "checkCoercion":"someValue"}"""
        val expected = WithEnumNames(
            listOf(
                AlternateEnumNames.VALUE_A,
                AlternateEnumNames.VALUE_A,
                AlternateEnumNames.VALUE_A,
                AlternateEnumNames.VALUE_B
            ), AlternateEnumNames.VALUE_A
        )
        parameterizedCoercingTest { json, streaming, msg ->
            assertEquals(expected, json.decodeFromString(input, streaming), msg)
        }
    }

    @Test
    fun topLevelEnumSupportAlternativeNames() {
        parameterizedCoercingTest { json, streaming, msg ->
            assertEquals(AlternateEnumNames.VALUE_A, json.decodeFromString("\"someValue\"", streaming), msg)
        }
    }

    @Test
    fun testParsesAllAlternativeNames() {
        for (input in listOf(inputString1, inputString2)) {
            parameterizedCoercingTest { json, streaming, _ ->
                val data = json.decodeFromString(WithNames.serializer(), input, jsonTestingMode = streaming)
                assertEquals("foo", data.data, "Failed to parse input '$input' with streaming=$streaming")
            }
        }
    }

    @Test
    fun testThrowsAnErrorOnDuplicateNames() {
        val serializer = CollisionWithAlternate.serializer()
        parameterizedCoercingTest { json, streaming, _ ->
            assertFailsWithMessage<SerializationException>(
                """The suggested name '_foo' for property foo is already one of the names for property data""",
                "Class ${serializer.descriptor.serialName} did not fail with streaming=$streaming"
            ) {
                json.decodeFromString(
                    serializer, inputString2,
                    jsonTestingMode = streaming
                )
            }
        }
    }
}
