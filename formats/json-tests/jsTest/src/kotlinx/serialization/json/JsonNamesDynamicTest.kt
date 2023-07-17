/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.features.*
import kotlinx.serialization.test.*
import kotlin.test.*


class JsonNamesDynamicTest {
    private val inputString1 = js("""{"foo":"foo"}""")
    private val inputString2 = js("""{"_foo":"foo"}""")

    private fun parameterizedCoercingTest(test: (json: Json, msg: String) -> Unit) {
        for (coercing in listOf(true, false)) {
            val json = Json {
                coerceInputValues = coercing
                useAlternativeNames = true
            }

            test(
                json,
                "Failed test with coercing=$coercing"
            )
        }
    }

    @Test
    fun testParsesAllAlternativeNamesDynamic() {
        for (input in listOf(inputString1, inputString2)) {
            parameterizedCoercingTest { json, msg ->
                val data = json.decodeFromDynamic(JsonNamesTest.WithNames.serializer(), input)
                assertEquals("foo", data.data, msg + "and input '$input'")
            }
        }
    }

    @Test
    fun testEnumSupportsAlternativeNames() {
        val input = js("""{"enumList":["VALUE_A", "someValue", "some_value", "VALUE_B"], "checkCoercion":"someValue"}""")
        val expected = JsonNamesTest.WithEnumNames(
            listOf(
                JsonNamesTest.AlternateEnumNames.VALUE_A,
                JsonNamesTest.AlternateEnumNames.VALUE_A,
                JsonNamesTest.AlternateEnumNames.VALUE_A,
                JsonNamesTest.AlternateEnumNames.VALUE_B
            ), JsonNamesTest.AlternateEnumNames.VALUE_A
        )
        parameterizedCoercingTest { json, msg ->
            assertEquals(expected, json.decodeFromDynamic(input), msg)
        }
    }

    @Test
    fun topLevelEnumSupportAlternativeNames() {
        parameterizedCoercingTest { json, msg ->
            assertEquals(JsonNamesTest.AlternateEnumNames.VALUE_A, json.decodeFromDynamic(js("\"someValue\"")), msg)
        }
    }

    @Test
    fun testThrowsAnErrorOnDuplicateNames2() {
        val serializer = JsonNamesTest.CollisionWithAlternate.serializer()
        parameterizedCoercingTest { json, _ ->
            assertFailsWithMessage<SerializationException>(
                """The suggested name '_foo' for property foo is already one of the names for property data""",
                "Class ${serializer.descriptor.serialName} did not fail"
            ) {
                json.decodeFromDynamic(
                    serializer, inputString2,
                )
            }
        }
    }

}
