/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
import kotlin.test.*
import kotlin.test.assertFailsWith

class DecodeFromDynamicSpecialCasesTest {

    @Test
    fun testTopLevelInt() {
        val dyn = js("42")
        val parsed = Json.decodeFromDynamic<Int>(dyn)
        assertEquals(42, parsed)
    }

    @Test
    fun testTopLevelString() {
        val dyn = js(""""42"""")
        val parsed = Json.decodeFromDynamic<String>(dyn)
        assertEquals("42", parsed)
    }

    @Test
    fun testTopLevelList() {
        val dyn = js("[1, 2, 3]")
        val parsed = Json.decodeFromDynamic<List<Int>>(dyn)
        assertEquals(listOf(1, 2, 3), parsed)
    }

    @Test
    fun testStringMap() = testMapWithPrimitiveKey("1", "2")

    @Test
    fun testByteMap() = testMapWithPrimitiveKey(1.toByte(), 2.toByte())

    @Test
    fun testCharMap() = testMapWithPrimitiveKey('1', '2')

    @Test
    fun testShortMap() = testMapWithPrimitiveKey(1.toShort(), 2.toShort())

    @Test
    fun testIntMap() = testMapWithPrimitiveKey(1, 2)

    @Test
    fun testLongMap()  = testMapWithPrimitiveKey(1L, 2L)

    @Test
    fun testDoubleMap()  = testMapWithPrimitiveKey(1.0, 2.0)

    @Test
    fun testFloatMap()  = testMapWithPrimitiveKey(1.0f, 2.0f)

    private inline fun <reified T> testMapWithPrimitiveKey(k1: T, k2: T) {
        val map = mapOf(k1 to 3, k2 to 4)
        val dyn = js("{1:3, 2:4}")
        val parsed = Json.decodeFromDynamic<Map<T, Int>>(dyn)
        assertEquals(map, parsed)
    }

    // TODO jsonElement, jsonObject, wrapped json element/object
}
