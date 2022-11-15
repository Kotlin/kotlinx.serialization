/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

class MapEntryTest {

    @Serializable
    data class Wrapper(val e: Map.Entry<Int, Int>)

    @Test
    fun testEntry() {
        val e = Wrapper(mapOf(1 to 1).entries.single())
        val output = ProtoBuf.encodeToHexString(Wrapper.serializer(), e)
        assertEquals("0a0408011001", output)
        assertEquals(e.e.toPair(), ProtoBuf.decodeFromHexString(Wrapper.serializer(), output).e.toPair())
    }
}
