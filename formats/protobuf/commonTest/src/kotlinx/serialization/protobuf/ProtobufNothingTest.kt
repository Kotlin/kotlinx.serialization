/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

class ProtobufNothingTest {
    @Serializable
    data class ParameterizedBox<T : Any>(val value: T?)

    @Serializable
    data class NullableNothingBox(val value: Nothing?)

    private inline fun <reified T> testConversion(data: T, expectedHexString: String) {
        val string = ProtoBuf.encodeToHexString(data).uppercase()
        assertEquals(expectedHexString, string)
        assertEquals(data, ProtoBuf.decodeFromHexString(string))
    }

    @Test
    fun testNothing() {
        testConversion(NullableNothingBox(null), "")
        testConversion(ParameterizedBox(null), "")
    }
}