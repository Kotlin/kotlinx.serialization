/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.formats.protobuf

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlinx.serialization.formats.*
import kotlinx.serialization.formats.proto.TestData
import org.junit.Test
import kotlin.test.assertTrue

@Suppress("PLUGIN_ERROR")
@Serializable
enum class EnumWithIds(val id: Int) {
    @SerialId(10)
    FIRST(10),
    @SerialId(20)
    SECOND(20);
}

@Serializable
data class EnumHolder(@SerialId(5) val a: EnumWithIds) : IMessage {
    override fun toProtobufMessage(): TestData.EnumHolder =
        TestData.EnumHolder.newBuilder().setA(TestData.TestEnumWithIds.forNumber(a.id)).build()
}

class ProtobufEnumWithSerialIdTest {
    @Test
    fun enumsSupportSerialIds() {
        assertTrue(readCompare(EnumHolder(EnumWithIds.SECOND), alwaysPrint = true))
    }

    @Test
    fun enumsSupportSerialIds2() {
        assertTrue(dumpCompare(EnumHolder(EnumWithIds.SECOND), alwaysPrint = true))
    }
}
