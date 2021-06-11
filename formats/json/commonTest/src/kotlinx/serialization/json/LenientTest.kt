/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.internal.JsonDecodingException
import kotlinx.serialization.json.internal.JsonException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LenientTest : JsonTestBase() {

    @Serializable
    data class Holder(val i: Int, val l: Long, val b: Boolean, val s: String)
    val value = Holder(1, 2, true, "string")

    @Serializable
    data class ListHolder(val l: List<String>)
    private val listValue = ListHolder(listOf("1", "2", "ss"))

    @Test
    fun testQuotedInt() = parametrizedTest {
        val json = """{"i":"1", "l":2, "b":true, "s":"string"}"""
        assertEquals(value, default.decodeFromString(Holder.serializer(), json, it))
        assertEquals(value, lenient.decodeFromString(Holder.serializer(), json, it))
    }

    @Test
    fun testQuotedLong() = parametrizedTest {
        val json = """{"i":1, "l":"2", "b":true, "s":"string"}"""
        assertEquals(value, default.decodeFromString(Holder.serializer(), json, it))
        assertEquals(value, lenient.decodeFromString(Holder.serializer(), json, it))
    }

    @Test
    fun testQuotedBoolean() = parametrizedTest {
        val json = """{"i":1, "l":2, "b":"true", "s":"string"}"""
        assertFailsWith<JsonDecodingException> { default.decodeFromString(Holder.serializer(), json, it) }
        assertEquals(value, lenient.decodeFromString(Holder.serializer(), json, it))
    }

    @Test
    fun testUnquotedStringValue() = parametrizedTest {
        val json = """{"i":1, "l":2, "b":true, "s":string}"""
        assertFailsWith<JsonDecodingException> { default.decodeFromString(Holder.serializer(), json, it) }
        assertEquals(value, lenient.decodeFromString(Holder.serializer(), json, it))
    }

    @Test
    fun testUnquotedKey() = parametrizedTest {
        val json = """{"i":1, "l":2, b:true, "s":"string"}"""
        assertFailsWith<JsonDecodingException> { default.decodeFromString(Holder.serializer(), json, it) }
        assertEquals(value, lenient.decodeFromString(Holder.serializer(), json, it))
    }

    @Test
    fun testUnquotedStringInArray() = parametrizedTest {
        val json = """{"l":[1, 2, ss]}"""
        assertFailsWith<JsonDecodingException> { default.decodeFromString(ListHolder.serializer(), json, it) }
        assertEquals(listValue, lenient.decodeFromString(ListHolder.serializer(), json, it))
    }

    @Serializable
    data class StringWrapper(val s: String)

    @Test
    fun testNullsProhibited() = parametrizedTest {
        assertEquals(StringWrapper("nul"), lenient.decodeFromString("""{"s":nul}""", it))
        assertEquals(StringWrapper("null1"), lenient.decodeFromString("""{"s":null1}""", it))
        assertFailsWith<JsonException> { lenient.decodeFromString<StringWrapper>("""{"s":null}""", it) }
    }

    @Serializable
    data class NullableString(val s: String?)

    @Test
    fun testNullsAllowed() = parametrizedTest {
        assertEquals(NullableString("nul"), lenient.decodeFromString("""{"s":nul}""", it))
        assertEquals(NullableString("null1"), lenient.decodeFromString("""{"s":null1}""", it))
        assertEquals(NullableString(null), lenient.decodeFromString("""{"s":null}""", it))
    }
}
