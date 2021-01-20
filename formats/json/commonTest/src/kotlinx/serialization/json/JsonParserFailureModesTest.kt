/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonParserFailureModesTest : JsonTestBase() {

    @Serializable
    data class Holder(
        val id: Long
    )

    @Test
    fun testFailureModes() = noLegacyJs { // fixme: legacyJs will work in 1.4.30-RC
        parametrizedTest {
            assertFailsWith<JsonDecodingException> {
                default.decodeFromString(
                    Holder.serializer(),
                    """{"id": "}""",
                    it
                )
            }
            assertFailsWith<JsonDecodingException> {
                default.decodeFromString(
                    Holder.serializer(),
                    """{"id": ""}""",
                    it
                )
            }
            assertFailsWith<JsonDecodingException> {
                default.decodeFromString(
                    Holder.serializer(),
                    """{"id":a}""",
                    it
                )
            }
            assertFailsWith<JsonDecodingException> {
                default.decodeFromString(
                    Holder.serializer(),
                    """{"id":2.0}""",
                    it
                )
            }
            assertFailsWith<JsonDecodingException> {
                default.decodeFromString(
                    Holder.serializer(),
                    """{"id2":2}""",
                    it
                )
            }
            assertFailsWith<JsonDecodingException> { default.decodeFromString(Holder.serializer(), """{"id"}""", it) }
            assertFailsWith<JsonDecodingException> { default.decodeFromString(Holder.serializer(), """{"id}""", it) }
            assertFailsWith<JsonDecodingException> { default.decodeFromString(Holder.serializer(), """{"i}""", it) }
            assertFailsWith<JsonDecodingException> { default.decodeFromString(Holder.serializer(), """{"}""", it) }
            assertFailsWithMissingField { default.decodeFromString(Holder.serializer(), """{}""", it) }
            assertFailsWith<JsonDecodingException> { default.decodeFromString(Holder.serializer(), """{""", it) }
            assertFailsWith<JsonDecodingException> { default.decodeFromString(Holder.serializer(), """}""", it) }
            assertFailsWith<JsonDecodingException> { default.decodeFromString(Holder.serializer(), """{""", it) }
        }
    }

    @Serializable
    class BooleanHolder(val b: Boolean)

    @Test
    fun testBoolean() = parametrizedTest {
        assertFailsWith<JsonDecodingException> { default.decodeFromString(BooleanHolder.serializer(), """{"b": fals}""", it) }
        assertFailsWith<JsonDecodingException> { default.decodeFromString(BooleanHolder.serializer(), """{"b": 123}""", it) }
    }
}
