/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToHexString
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals

class InlinedClassesTest {

    @JvmInline
    @Serializable
    value class WrappedUInt(val i: UInt)


    @JvmInline
    @Serializable
    value class DeepWrappedUInt(val wrapped: WrappedUInt)

    @Serializable
    class DeepWrappedUIntHolder(val x: DeepWrappedUInt)


    @Test
    fun testNested() {
        // all nested inlines types should be flattened until the first non-inline property
        val actual = ProtoBuf.encodeToHexString(DeepWrappedUIntHolder(DeepWrappedUInt(WrappedUInt(10u))))
        assertEquals("080a", actual)
    }

}