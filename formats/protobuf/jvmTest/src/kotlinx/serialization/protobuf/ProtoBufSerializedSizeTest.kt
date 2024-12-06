package kotlinx.serialization.protobuf

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.TestData.*
import org.junit.Test
import kotlin.test.assertEquals

class ProtoBufSerializedSizeTest {

    private val protoBuf = ProtoBuf

    @Serializable
    data class DataInt32(val valueInt: Int)

    @Test
    fun shouldCalculateInt32Size() {
        val dataInt32 = DataInt32(1)
        val size = protoBuf.getOrComputeSerializedSize(DataInt32.serializer(), dataInt32)
        val javaType = TestInt32.newBuilder().apply { a = 1 }.build()
        assertEquals(javaType.serializedSize, size)
    }

    @Serializable
    data class DataSignedInt(@ProtoType(ProtoIntegerType.SIGNED) val value: Int)

    @Test
    fun shouldCalculateSingedIntSize() {
        val data = DataSignedInt(10)
        val size = protoBuf.getOrComputeSerializedSize(DataSignedInt.serializer(), data)
        val javaType = TestSignedInt.newBuilder().apply { a = 10 }.build()
        assertEquals(javaType.serializedSize, size)
    }

    @Serializable
    data class DataSignedLong(@ProtoType(ProtoIntegerType.SIGNED) val value: Long)

    @Test
    fun shouldCalculateSignedLongSize() {
        val data = DataSignedLong(10)
        val size = protoBuf.getOrComputeSerializedSize(DataSignedLong.serializer(), data)
        val javaType = TestSignedLong.newBuilder().apply { a = 10 }.build()
        assertEquals(javaType.serializedSize, size)
    }

    @Serializable
    data class DataFixedInt(@ProtoType(ProtoIntegerType.FIXED) val value: Int)

    @Test
    fun shouldCalculateFixedInt() {
        val data = DataFixedInt(10)
        val size = protoBuf.getOrComputeSerializedSize(DataFixedInt.serializer(), data)
        val javaType = TestFixedInt.newBuilder().apply { a = 10 }.build()
        assertEquals(javaType.serializedSize, size)
    }

    @Serializable
    data class DataDouble(val value: Double)

    @Test
    fun shouldCalculateDouble() {
        val data = DataDouble(10.0)
        val size = protoBuf.getOrComputeSerializedSize(DataDouble.serializer(), data)
        val javaType = TestDouble.newBuilder().apply { a = 10.0 }.build()
        assertEquals(javaType.serializedSize, size)
    }

    @Serializable
    data class DataBoolean(val value: Boolean)

    @Test
    fun shouldCalculateBoolean() {
        val data = DataBoolean(true)
        val size = protoBuf.getOrComputeSerializedSize(DataBoolean.serializer(), data)
        val javaType = TestBoolean.newBuilder().apply { a = true }.build()
        assertEquals(javaType.serializedSize, size)
    }

    @Serializable
    data class DataAllTypes(
        val int32: Int,
        @ProtoType(ProtoIntegerType.SIGNED)
        val sint32: Int,
        @ProtoType(ProtoIntegerType.FIXED)
        val fixed32: Int,
        @ProtoNumber(10)
        val int64: Long,
        @ProtoType(ProtoIntegerType.SIGNED)
        @ProtoNumber(11)
        val sint64: Long,
        @ProtoType(ProtoIntegerType.FIXED)
        @ProtoNumber(12)
        val fixed64: Long,
        @ProtoNumber(21)
        val float: Float,
        @ProtoNumber(22)
        val double: Double,
        @ProtoNumber(41)
        val bool: Boolean,
        @ProtoNumber(51)
        val string: String
    )

    @Test
    fun shouldCalculateAllTypes() {
        val data = DataAllTypes(
            1,
            2,
            3,
            4,
            5,
            6,
            7.0F,
            8.0,
            true,
            "hi"
        )
        val size = protoBuf.getOrComputeSerializedSize(DataAllTypes.serializer(), data)
        val javaType = TestAllTypes.newBuilder().apply {
            i32 = 1
            si32 = 2
            f32 = 3
            i64 = 4
            si64 = 5
            f64 = 6
            f = 7.0F
            d = 8.0
            b = true
            s = "hi"
        }.build()
        assertEquals(javaType.serializedSize, size)
    }

    @Serializable
    data class DataOuterMessage(
        val a: Int,
        val d: Double,
        @ProtoNumber(10)
        val inner: DataAllTypes,
        @ProtoNumber(20)
        val s: String
    )

    @Test
    fun shouldCalculateOuterMessage() {
        val dataInner = DataAllTypes(
            1,
            2,
            3,
            4,
            5,
            6,
            7.0F,
            8.0,
            true,
            "hi"
        )
        val data = DataOuterMessage(10, 20.0, dataInner, "hi")
        val size = protoBuf.getOrComputeSerializedSize(DataOuterMessage.serializer(), data)
        val javaInner = TestAllTypes.newBuilder().apply {
            i32 = 1
            si32 = 2
            f32 = 3
            i64 = 4
            si64 = 5
            f64 = 6
            f = 7.0F
            d = 8.0
            b = true
            s = "hi"
        }.build()
        val javaOuter = TestOuterMessage.newBuilder().apply {
            a = 10
            b = 20.0
            inner = javaInner
            s = "hi"
        }.build()
        assertEquals(javaOuter.serializedSize, size)
    }

    @Serializable
    data class DataRepeatedIntMessage(
        val s: Int,
        @ProtoNumber(10)
        val b: List<Int>
    )

    @Test
    fun shouldCalculateRepeatedIntMessage() {
        val data = DataRepeatedIntMessage(1, listOf(10, 20, 10, 10, 10, 10))
        val size = protoBuf.getOrComputeSerializedSize(DataRepeatedIntMessage.serializer(), data)
        val javaType = TestRepeatedIntMessage.newBuilder().apply {
            s = 1
            addAllB(listOf(10, 20, 10, 10, 10, 10))
        }.build()
        assertEquals(javaType.serializedSize, size)
    }

    @Serializable
    data class DataRepeatedIntMessageWithRandomTags(
        @ProtoNumber(5)
        val s: Int,
        @ProtoNumber(20)
        val b: List<Int>
    )

    @Test
    fun shouldCalculateRepeatedIntMessageWithRandomTags() {
        val data = DataRepeatedIntMessageWithRandomTags(1, listOf(10, 20, 10, 10, 10, 10))
        val size = protoBuf.getOrComputeSerializedSize(DataRepeatedIntMessageWithRandomTags.serializer(), data)
        val javaType = TestRepeatedIntMessageWithRandomTags.newBuilder().apply {
            s = 1
            addAllB(listOf(10, 20, 10, 10, 10, 10))
        }.build()
        assertEquals(javaType.serializedSize, size)
    }

    @Serializable
    data class DataRepeatedObjectMessage(val inner: List<DataAllTypes>)

    @Test
    fun shouldCalculateRepeatedObjectMessage() {
        val dataInner = DataAllTypes(
            1,
            2,
            3,
            4,
            5,
            6,
            7.0F,
            8.0,
            true,
            "hi"
        )
        val data = DataRepeatedObjectMessage(listOf(dataInner, dataInner, dataInner))
        val size = protoBuf.getOrComputeSerializedSize(DataRepeatedObjectMessage.serializer(), data)
        val javaInner = TestAllTypes.newBuilder().apply {
            i32 = 1
            si32 = 2
            f32 = 3
            i64 = 4
            si64 = 5
            f64 = 6
            f = 7.0F
            d = 8.0
            b = true
            s = "hi"
        }.build()
        val javaType = TestRepeatedObjectMessage.newBuilder().apply {
            addAllInner(listOf(javaInner, javaInner, javaInner))
        }.build()
        assertEquals(javaType.serializedSize, size)
    }

    @Serializable
    data class DataRepeatedObjectMessageWithRandomTag(@ProtoNumber(20) val inner: List<DataAllTypes>)

    @Test
    fun shouldCalculateRepeatedObjectMessageWithRandomTags() {
        val dataInner = DataAllTypes(
            1,
            2,
            3,
            4,
            5,
            6,
            7.0F,
            8.0,
            true,
            "hi"
        )
        val data = DataRepeatedObjectMessageWithRandomTag(listOf(dataInner, dataInner, dataInner))
        val size = protoBuf.getOrComputeSerializedSize(DataRepeatedObjectMessageWithRandomTag.serializer(), data)
        val javaInner = TestAllTypes.newBuilder().apply {
            i32 = 1
            si32 = 2
            f32 = 3
            i64 = 4
            si64 = 5
            f64 = 6
            f = 7.0F
            d = 8.0
            b = true
            s = "hi"
        }.build()
        val javaType = TestRepeatedObjectMessageWithRandomTags.newBuilder().apply {
            addAllInner(listOf(javaInner, javaInner, javaInner))
        }.build()
        assertEquals(javaType.serializedSize, size)
    }

    @Serializable
    data class DataEnumMessage(val a: Coffee) {
        enum class Coffee {
            Americano,
            Latte,
            Capuccino
        }
    }

    @Test
    fun shouldCalculateEnumMessage() {
        val data = DataEnumMessage(DataEnumMessage.Coffee.Americano)
        val size = protoBuf.getOrComputeSerializedSize(DataEnumMessage.serializer(), data)
        val java = TestEnum.newBuilder().apply {
            a = TestEnum.Coffee.Americano
        }.build()
        assertEquals(java.serializedSize, size)
    }

    @Serializable
    enum class DataEnumWithIds {
        @ProtoNumber(10)
        First,

        @ProtoNumber(20)
        Second
    }

    @Serializable
    data class DataEnumHolderMessage(@ProtoNumber(5) val s: DataEnumWithIds)

    @Test
    fun shouldCalculateEnumHolderMessage() {
        val data = DataEnumHolderMessage(DataEnumWithIds.Second)
        val size = protoBuf.getOrComputeSerializedSize(DataEnumHolderMessage.serializer(), data)
        val java = EnumHolder.newBuilder().apply { a = TestEnumWithIds.Second }.build()
        assertEquals(java.serializedSize, size)
    }

    @Serializable
    data class DataMap(val stringMap: Map<String, String>, val intObjectMap: Map<Int, DataAllTypes>)

    @Test
    fun shouldCalculateMapMessage() {
        val dataInner = DataAllTypes(
            1,
            2,
            3,
            4,
            5,
            6,
            7.0F,
            8.0,
            true,
            "hi"
        )
        val data = DataMap(
            mapOf("1" to "hello", "2" to "world"),
            mapOf(1 to dataInner)
        )
        val size = protoBuf.getOrComputeSerializedSize(DataMap.serializer(), data)

        val java = TestMap.newBuilder().apply {
            val javaInner = TestAllTypes.newBuilder().apply {
                i32 = 1
                si32 = 2
                f32 = 3
                i64 = 4
                si64 = 5
                f64 = 6
                f = 7.0F
                d = 8.0
                b = true
                s = "hi"
            }.build()
            putAllStringMap(mapOf("1" to "hello", "2" to "world"))
            putAllIntObjectMap(mapOf(1 to javaInner))
        }.build()
        assertEquals(java.serializedSize, size)
    }

    @Serializable
    data class DataWithOptionals(
        val a: Int? = null,
        val b: String? = null,
        val c: Position? = null,
        val d: Int = 99,
        val e: List<Int>
    ) {
        enum class Position {
            FIRST,
            SECOND
        }
    }

    @Test
    fun shouldCalculateWithOptionalsMessage() {
        val data = DataWithOptionals(
            c = DataWithOptionals.Position.SECOND,
            e = listOf(10, 10, 10)
        )
        val size = protoBuf.getOrComputeSerializedSize(DataWithOptionals.serializer(), data)
        val java = MessageWithOptionals.newBuilder().apply {
            c = MessageWithOptionals.Position.SECOND
            addAllE(listOf(10, 10, 10))
        }.build()
        assertEquals(java.serializedSize, size)
    }

    @Serializable
    data class DataWithPackedFields(
        @ProtoPacked
        val a: List<Int>,
    )

    @Test
    fun shouldCalculateMessageWithPackedFields() {
        val data = DataWithPackedFields(listOf(1, 2, 3))
        val size = protoBuf.getOrComputeSerializedSize(DataWithPackedFields.serializer(), data)
        val java = MessageWithPackedFields.newBuilder().apply { addAllA(listOf(1, 2, 3)) }.build()
        println(size)
        assertEquals(java.serializedSize, size)
    }

    @Test
    fun shouldCalculateEmptyMessageWithPackedFields() {
        val data = DataWithPackedFields(listOf())
        val size = protoBuf.getOrComputeSerializedSize(DataWithPackedFields.serializer(), data)
        val java = MessageWithPackedFields.newBuilder().apply { addAllA(listOf()) }.build()
        println(size)
        assertEquals(java.serializedSize, size)
    }

    @Serializable
    data class DataWithPackedFieldsAndRandomTags(
        @ProtoNumber(5)
        @ProtoPacked
        val a: List<Int>,
        @ProtoNumber(20)
        @ProtoPacked
        val b: List<Int>,
    )

    @Test
    fun shouldCalculateMessageWithPackedFieldsAndRandomTags() {
        val data = DataWithPackedFieldsAndRandomTags(listOf(1, 2, 3), listOf(4, 5))
        val size = protoBuf.getOrComputeSerializedSize(DataWithPackedFieldsAndRandomTags.serializer(), data)
        val java = MessageWithPackedFieldsAndRandomTags.newBuilder().apply {
            addAllA(listOf(1, 2, 3))
            addAllB(listOf(4, 5))
        }.build()
        println(size)
        assertEquals(java.serializedSize, size)
    }
}