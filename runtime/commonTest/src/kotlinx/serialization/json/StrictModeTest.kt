/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class StrictModeTest : JsonTestBase() {

    @Serializable
    data class Holder(val i: Int, val l: Long, val b: Boolean, val s: String)
    val value = Holder(1, 2, true, "string")

    @Serializable
    data class ListHolder(val l: List<String>)
    val listValue = ListHolder(listOf("1", "2", "ss"))

    @Test
    fun testQuotedInt() = parametrizedTest {
        val json = """{"i":"1", "l":2, "b":true, "s":"string"}"""
        assertEquals(value, strict.parse(Holder.serializer(), json, it))
        assertEquals(value, nonStrict.parse(Holder.serializer(), json, it))
    }

    @Test
    fun testQuotedLong() = parametrizedTest {
        val json = """{"i":1, "l":"2", "b":true, "s":"string"}"""
        assertEquals(value, strict.parse(Holder.serializer(), json, it))
        assertEquals(value, nonStrict.parse(Holder.serializer(), json, it))
    }

    @Test
    fun testQuotedBoolean() = parametrizedTest {
        val json = """{"i":1, "l":2, "b":"true", "s":"string"}"""
        assertFailsWith<JsonException> { strict.parse(Holder.serializer(), json, it) }
        assertEquals(value, nonStrict.parse(Holder.serializer(), json, it))
    }

    @Test
    fun testUnquotedStringValue() = parametrizedTest {
        val json = """{"i":1, "l":2, "b":true, "s":string}"""
        assertFailsWith<JsonException> { strict.parse(Holder.serializer(), json, it) }
        assertEquals(value, nonStrict.parse(Holder.serializer(), json, it))
    }

    @Test
    fun testUnquotedKey() = parametrizedTest {
        val json = """{"i":1, "l":2, b:true, "s":"string"}"""
        assertFailsWith<JsonDecodingException> { strict.parse(Holder.serializer(), json, it) }
        assertEquals(value, nonStrict.parse(Holder.serializer(), json, it))
    }

    @Test
    fun testUnquotedArray() = parametrizedTest {
        val json = """{"l":[1, 2, ss]}"""
        assertFailsWith<JsonException> { strict.parse(ListHolder.serializer(), json, it) }
        assertEquals(listValue, nonStrict.parse(ListHolder.serializer(), json, it))
    }

    @Test
    fun testUnquotedArray2() = parametrizedTest {
        val json = """{"l":[1, 2, "ss"]}"""
        assertFailsWith<JsonException> { strict.parse(ListHolder.serializer(), json, it) }
        assertEquals(listValue, nonStrict.parse(ListHolder.serializer(), json, it))
    }
}
