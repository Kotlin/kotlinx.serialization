package kotlinx.serialization.protobuf

import org.junit.Test

class ProtobufWriterTest {

    @Test
    fun writeSignedInteger() {
        ProtoBuf.dumps(t1).toLowerCase() shouldBe "08ab02"
    }

    @Test
    fun writeListOfVarintIntegers() {
        ProtoBuf.dumps(t2).toUpperCase() shouldBe "08960108E40108B90A"
    }

    @Test
    fun writeString() {
        ProtoBuf.dumps(t3).toUpperCase() shouldBe "120774657374696E67"
    }

    @Test
    fun writeInnerObject() {
        ProtoBuf.dumps(t4).toLowerCase() shouldBe "1a0308ab02"
    }

    @Test
    fun writeObjectWithUnorderedTags() {
        ProtoBuf.dumps(t5).toUpperCase() shouldBe "D0022A120774657374696E67"
    }

    @Test
    fun writeObjectsWithEmptyDefaultValues() {
        ProtoBuf.dumps(t1e) shouldBe "0800"
        ProtoBuf.dumps(t2e) shouldBe ""
        ProtoBuf.dumps(t3e) shouldBe "1200"
    }

    @Test
    fun writeNumbers() {
        ProtoBuf.dumps(t6).toLowerCase() shouldBe "0d9488010010ffffffffffffffff7f"
    }
}