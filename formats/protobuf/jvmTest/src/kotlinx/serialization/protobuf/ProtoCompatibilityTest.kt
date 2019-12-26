/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProtoCompatibilityTest {

    @Test
    fun testMap() {
        val mapData = KTestData.KTestMap(mapOf("a" to "b", "c" to "d"), emptyMap())
        val kxData = ProtoBuf.dump(mapData)
        val kxHex = ProtoBuf.dumps(mapData)
        val protoHex = mapData.toProtobufMessage().toHex()
        assertTrue(kxHex.equals(protoHex, ignoreCase = true))
        val deserializedData: KTestData.KTestMap = ProtoBuf.loads(kxHex)
        val parsedMsg = mapData.toProtobufMessage().parserForType.parseFrom(kxData)
        assertEquals(mapData, deserializedData)
        assertEquals(parsedMsg, deserializedData.toProtobufMessage())
    }
}
