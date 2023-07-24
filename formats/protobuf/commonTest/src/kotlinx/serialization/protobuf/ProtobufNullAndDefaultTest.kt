/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

class ProtobufNullAndDefaultTest {
    @Serializable
    class ProtoWithNullDefault(val s: String? = null)

    @Serializable
    class ProtoWithNullDefaultAlways(@EncodeDefault val s: String? = null)

    @Serializable
    class ProtoWithNullDefaultNever(@EncodeDefault(EncodeDefault.Mode.NEVER) val s: String? = null)

    @Test
    fun testProtobufDropDefaults() {
        val proto = ProtoBuf { encodeDefaults = false }
        assertEquals(0, proto.encodeToByteArray(ProtoWithNullDefault()).size)
        assertFailsWith<SerializationException> { proto.encodeToByteArray(ProtoWithNullDefaultAlways()) }
        assertEquals(0, proto.encodeToByteArray(ProtoWithNullDefaultNever()).size)
    }

    @Test
    fun testProtobufEncodeDefaults() {
        val proto = ProtoBuf { encodeDefaults = true }
        assertFailsWith<SerializationException> { proto.encodeToByteArray(ProtoWithNullDefault()) }
        assertFailsWith<SerializationException> { proto.encodeToByteArray(ProtoWithNullDefaultAlways()) }
        assertEquals(0, proto.encodeToByteArray(ProtoWithNullDefaultNever()).size)
    }
}
