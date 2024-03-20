/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

class ProtobufNothingTest {
    @Serializable
    /*private*/ data class NullableNothingBox(val value: Nothing?) // `private` doesn't work on the JS legacy target

    @Serializable
    private data class NullablePropertyNotNullUpperBoundParameterizedBox<T : Any>(val value: T?)

    @Serializable
    private data class NullableUpperBoundParameterizedBox<T : Any?>(val value: T)


    @Test
    fun testNothing() {
        testConversion(NullableNothingBox(null), "")
        testConversion(NullablePropertyNotNullUpperBoundParameterizedBox(null), "")
        testConversion(NullableUpperBoundParameterizedBox(null), "")
    }
}
