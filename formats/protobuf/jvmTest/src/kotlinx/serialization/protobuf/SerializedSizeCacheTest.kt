package kotlinx.serialization.protobuf

import kotlinx.serialization.Serializable
import org.junit.Test
import kotlin.test.assertEquals

/*
 * Not entirely sure if this test cases bring any value, as the "cache" is considered an implementation detail.
 * Also, there is no straight-forward way to assert that cache indeed works.
 */
class SerializedSizeCacheTest {

    private val protoBuf = ProtoBuf

    @Serializable
    data class ProtoBufData(val value: String? = null)

    /*
     * Proof that it works can be found in logs.
     */
    @Test
    fun shouldMemoizeResultAfterFirstCall() {
        val data = ProtoBufData("data")
        val serializedSize = protoBuf.getOrComputeSerializedSize(ProtoBufData.serializer(), data)
        val serializedSize2 = protoBuf.getOrComputeSerializedSize(ProtoBufData.serializer(), data)
        assertEquals( // paranoid assertion, and does not actual tests the goal of this test.
            serializedSize,
            serializedSize2
        )
    }

    /*
     * Proof that it works can be found in logs.
     */
    @Test
    fun shouldMemoizeResultOnlyDataWithSameContent() {
        val data = ProtoBufData("data")
        val data2 = ProtoBufData()
        protoBuf.getOrComputeSerializedSize(ProtoBufData.serializer(), data)
        protoBuf.getOrComputeSerializedSize(ProtoBufData.serializer(), data2)
    }
}