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

class EncodeToDynamicSpecialCasesTest {

    @Test
    fun testTopLevelInt() = assertDynamicForm(42)

    @Test
    fun testTopLevelString() = assertDynamicForm("42")

    @Test
    fun testTopLevelList() = assertDynamicForm(listOf(1, 2, 3))

    @Test
    fun testStringMap() = assertDynamicForm(mapOf("1" to 2, "3" to 4))

    @Test
    fun testByteMap() = assertDynamicForm(mapOf(1.toByte() to 2, 3.toByte() to 4))

    @Test
    fun testCharMap() = assertDynamicForm(mapOf('1' to 2, '3' to 4))

    @Test
    fun testShortMap() = assertDynamicForm(mapOf(1.toShort() to 2, 3.toShort() to 4))

    @Test
    fun testIntMap() = assertDynamicForm(mapOf(1 to 2, 3 to 4))

    @Test
    fun testLongMap()  = assertDynamicForm(mapOf(1L to 2, 3L to 4))

    @Test
    fun testDoubleMap()  = assertDynamicForm(mapOf(1.0 to 2, 3.0 to 4))

    @Test
    fun testFloatMap()  = assertDynamicForm(mapOf(1.0f to 2, 3.0f to 4))


    // TODO jsonElement, jsonObject, wrapped json element/object
}
