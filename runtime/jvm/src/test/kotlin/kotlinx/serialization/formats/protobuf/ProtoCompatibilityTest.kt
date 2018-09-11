package kotlinx.serialization.formats.protobuf

import kotlinx.serialization.formats.KTestData
import kotlinx.serialization.formats.toHex
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProtoCompatibilityTest {
    @Test
    fun mapTest() {
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
