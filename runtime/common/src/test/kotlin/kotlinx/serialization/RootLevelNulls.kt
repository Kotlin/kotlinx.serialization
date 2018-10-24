package kotlinx.serialization

import kotlinx.serialization.cbor.CBOR
import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.internal.makeNullable
import kotlinx.serialization.json.JSON
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.*

class RootLevelNullsTest {
    @Serializable
    data class Simple(val a: Int = 42)

    @Test
    fun nullableJson() {
        val obj: Simple? = null
        val json = JSON.stringify(makeNullable(Simple.serializer()), obj)
        assertEquals("null", json)
    }

    @Test
    fun nullableCbor() {
        val obj: Simple? = null
        val content = (CBOR as BinaryFormat).dump(makeNullable(Simple.serializer()), obj)
        assertTrue(content.contentEquals(byteArrayOf(0xf6.toByte())))
    }

    @Test
    fun nullableProtobuf() {
        val obj: Simple? = null
        assertFailsWith<SerializationException> {
            (ProtoBuf.plain as BinaryFormat).dump(makeNullable(Simple.serializer()), obj)
        }
    }
}
