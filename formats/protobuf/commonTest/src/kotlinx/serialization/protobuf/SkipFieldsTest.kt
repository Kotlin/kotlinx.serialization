/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf


import kotlinx.serialization.*
import kotlin.test.*

class SkipFieldsTest {

    @Serializable
    data class Holder(val value: Int)

    @Test
    fun testSkipZeroId() {
        // first value with id = 0
        val hexString = "000f082a"
        val holder = ProtoBuf.decodeFromHexString<Holder>(hexString)
        assertEquals(42, holder.value)
    }

    @Test
    fun testSkipBigId() {
        // first value with id = 2047 and takes 2 bytes
        val hexString = "f87f20082a"
        val holder = ProtoBuf.decodeFromHexString<Holder>(hexString)
        assertEquals(42, holder.value)
    }

    @Test
    fun testSkipString() {
        // first value is size delimited (string) with id = 42
        val hexString = "d2020c48656c6c6f20576f726c6421082a"
        val holder = ProtoBuf.decodeFromHexString<Holder>(hexString)
        assertEquals(42, holder.value)
    }
}