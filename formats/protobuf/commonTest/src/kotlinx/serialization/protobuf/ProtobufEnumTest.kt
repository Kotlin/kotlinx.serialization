/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

class ProtobufEnumTest {

    enum class SomeEnum { ALPHA, BETA, GAMMA }

    @Serializable
    data class EnumWithUnion(@ProtoNumber(5) val s: String,
                             @ProtoNumber(6) val e: SomeEnum = SomeEnum.ALPHA,
                             @ProtoNumber(7) val i: Int = 42)

    @Test
    fun testEnumWithUnion() {
        val data = EnumWithUnion("foo", SomeEnum.BETA)
        val hex = ProtoBuf.encodeToHexString(EnumWithUnion.serializer(), data)
        val restored = ProtoBuf.decodeFromHexString(EnumWithUnion.serializer(), hex)
        assertEquals(data, restored)
    }

    @Serializable
    class EnumHolder(val e: SomeEnum)

    @Test
    fun testUnknownValue() {
        val bytes = ProtoBuf.encodeToByteArray(EnumHolder(SomeEnum.ALPHA))
        bytes[1] = 3
        assertFailsWith<SerializationException> { ProtoBuf.decodeFromByteArray<EnumHolder>(bytes) }
        bytes[1] = -1
        assertFailsWith<SerializationException> { ProtoBuf.decodeFromByteArray<EnumHolder>(bytes) }

    }
}
