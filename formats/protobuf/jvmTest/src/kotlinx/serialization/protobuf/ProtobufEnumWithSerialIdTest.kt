/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.Serializable
import org.junit.*
import kotlin.test.assertTrue

class ProtobufEnumWithSerialIdTest {

    @Serializable
    enum class EnumWithIds(val id: Int) {
        @ProtoId(10)
        FIRST(10),
        @ProtoId(20)
        SECOND(20);
    }

    @Serializable
    data class EnumHolder(@ProtoId(5) val a: EnumWithIds) : IMessage {
        override fun toProtobufMessage(): TestData.EnumHolder =
            TestData.EnumHolder.newBuilder().setA(TestData.TestEnumWithIds.forNumber(a.id)).build()
    }

    @Test
    fun testEnumsSupportSerialIds() {
        assertTrue(readCompare(EnumHolder(EnumWithIds.SECOND)))
        assertTrue(dumpCompare(EnumHolder(EnumWithIds.SECOND)))
    }
}
