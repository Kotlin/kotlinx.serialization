/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import kotlin.test.*

class LenientTest : JsonTestBase() {

    @Serializable
    data class Holder(val i: Int, val l: Long, val b: Boolean, val s: String)
    val value = Holder(1, 2, true, "string")

    @Serializable
    data class ListHolder(val l: List<String>)
    val listValue = ListHolder(listOf("1", "2", "ss"))

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
}
