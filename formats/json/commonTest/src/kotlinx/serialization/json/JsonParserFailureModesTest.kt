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
    fun testFailureModes() = parametrizedTest {
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
        // 9223372036854775807 is Long.MAX_VALUE
        assertFailsWith<JsonDecodingException> {
            default.decodeFromString(
                Holder.serializer(),
                """{"id":${Long.MAX_VALUE}""" + "00" + "}",
                it
            )
        }
        // -9223372036854775808 is Long.MIN_VALUE
        assertFailsWith<JsonDecodingException> {
            default.decodeFromString(
                Holder.serializer(),
                """{"id":9223372036854775808}""",
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

    @Serializable
    class BooleanHolder(val b: Boolean)

    @Test
    fun testBoolean() = parametrizedTest {
        assertFailsWith<JsonDecodingException> {
            default.decodeFromString(
                BooleanHolder.serializer(),
                """{"b": fals}""",
                it
            )
        }
        assertFailsWith<JsonDecodingException> {
            default.decodeFromString(
                BooleanHolder.serializer(),
                """{"b": 123}""",
                it
            )
        }
    }

    @Serializable
    class PrimitiveHolder(
        val b: Byte = 0, val s: Short = 0, val i: Int = 0
    )

    @Test
    fun testOverflow() = parametrizedTest {
        // Byte overflow
        assertFailsWith<JsonDecodingException> { default.decodeFromString<PrimitiveHolder>("""{"b": 128}""", it) }
        // Short overflow
        assertFailsWith<JsonDecodingException> { default.decodeFromString<PrimitiveHolder>("""{"s": 32768}""", it) }
        // Int overflow
        assertFailsWith<JsonDecodingException> {
            default.decodeFromString<PrimitiveHolder>(
                """{"i": 2147483648}""",
                it
            )
        }
    }

    @Test
    fun testNoOverflow() = parametrizedTest {
        default.decodeFromString<PrimitiveHolder>("""{"b": ${Byte.MAX_VALUE}}""", it)
        default.decodeFromString<PrimitiveHolder>("""{"b": ${Byte.MIN_VALUE}}""", it)
        default.decodeFromString<PrimitiveHolder>("""{"s": ${Short.MAX_VALUE}}""", it)
        default.decodeFromString<PrimitiveHolder>("""{"s": ${Short.MIN_VALUE}}""", it)
        default.decodeFromString<PrimitiveHolder>("""{"i": ${Int.MAX_VALUE}}""", it)
        default.decodeFromString<PrimitiveHolder>("""{"i": ${Int.MIN_VALUE}}""", it)
        default.decodeFromString<Holder>("""{"id": ${Long.MIN_VALUE.toString()}}""", it)
        default.decodeFromString<Holder>("""{"id": ${Long.MAX_VALUE.toString()}}""", it)
    }

    @Test
    fun testInvalidNumber() = parametrizedTest {
        assertFailsWith<JsonDecodingException> { default.decodeFromString<Holder>("""{"id":-}""", it) }
        assertFailsWith<JsonDecodingException> { default.decodeFromString<Holder>("""{"id":+}""", it) }
        assertFailsWith<JsonDecodingException> { default.decodeFromString<Holder>("""{"id":--}""", it) }
        assertFailsWith<JsonDecodingException> { default.decodeFromString<Holder>("""{"id":1-1}""", it) }
        assertFailsWith<JsonDecodingException> { default.decodeFromString<Holder>("""{"id":0-1}""", it) }
        assertFailsWith<JsonDecodingException> { default.decodeFromString<Holder>("""{"id":0-}""", it) }
        assertFailsWith<JsonDecodingException> { default.decodeFromString<Holder>("""{"id":a}""", it) }
        assertFailsWith<JsonDecodingException> { default.decodeFromString<Holder>("""{"id":-a}""", it) }
    }
}
