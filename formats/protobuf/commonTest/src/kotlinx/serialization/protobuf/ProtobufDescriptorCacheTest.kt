/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.protobuf.internal.*
import kotlin.test.*

class ProtobufDescriptorCacheTest {

    @Serializable
    data class Inner(@ProtoNumber(3) val name: String, @ProtoType(ProtoIntegerType.SIGNED) val delta: Int)

    @Serializable
    data class Message(
        val id: Long,
        @ProtoPacked val packed: List<Int>,
        val unpacked: List<Int>,
        val inners: List<Inner>,
        val map: Map<String, Inner>,
        @ProtoNumber(42) val nullable: Inner?,
    )

    private val message = Message(
        id = 7,
        packed = listOf(1, 2, 3),
        unpacked = listOf(4, 5),
        inners = listOf(Inner("a", -1), Inner("b", 2)),
        map = mapOf("x" to Inner("c", -3)),
        nullable = Inner("d", 4),
    )

    @Test
    fun testCacheIsReusedAcrossCalls() {
        val proto = ProtoBuf { }
        val bytes = proto.encodeToByteArray(message)
        assertEquals(message, proto.decodeFromByteArray<Message>(bytes))

        val decoding = proto.descriptorCache.decoding.size
        val encoding = proto.descriptorCache.encoding.size
        assertTrue(decoding > 0)
        assertTrue(encoding > 0)
        val info = proto.descriptorCache.decodingInfo(Message.serializer().descriptor, proto.serializersModule)

        repeat(3) {
            assertContentEquals(bytes, proto.encodeToByteArray(message))
            assertEquals(message, proto.decodeFromByteArray<Message>(bytes))
        }
        assertEquals(decoding, proto.descriptorCache.decoding.size)
        assertEquals(encoding, proto.descriptorCache.encoding.size)
        assertSame(info, proto.descriptorCache.decodingInfo(Message.serializer().descriptor, proto.serializersModule))
    }

    @Test
    fun testEqualDescriptorsShareEntry() {
        val proto = ProtoBuf { }
        val bytes = proto.encodeToByteArray(ListSerializer(Inner.serializer()), message.inners)
        assertEquals(message.inners, proto.decodeFromByteArray(ListSerializer(Inner.serializer()), bytes))
        val size = proto.descriptorCache.decoding.size
        assertEquals(message.inners, proto.decodeFromByteArray(ListSerializer(Inner.serializer()), bytes))
        assertEquals(size, proto.descriptorCache.decoding.size)
    }

    @Test
    fun testNullableAndNonNullDescriptors() {
        val proto = ProtoBuf { }
        val withNull = message.copy(nullable = null)
        assertEquals(withNull, proto.decodeFromByteArray<Message>(proto.encodeToByteArray(withNull)))
        assertEquals(message, proto.decodeFromByteArray<Message>(proto.encodeToByteArray(message)))
        assertEquals(withNull, proto.decodeFromByteArray<Message>(proto.encodeToByteArray(withNull)))
    }

    @Serializable
    @SerialName("Same")
    data class SameNameFirst(@ProtoNumber(1) val value: Int)

    @Serializable
    @SerialName("Same")
    data class SameNameSecond(@ProtoNumber(2) val value: Int)

    @Test
    fun testEqualDescriptorsWithDifferentAnnotations() {
        assertEquals(SameNameFirst.serializer().descriptor, SameNameSecond.serializer().descriptor)
        for (firstGoesFirst in listOf(true, false)) {
            val proto = ProtoBuf { }
            val first = {
                assertEquals("0801", proto.encodeToHexString(SameNameFirst(1)))
                assertEquals(SameNameFirst(1), proto.decodeFromHexString("0801"))
            }
            val second = {
                assertEquals("1001", proto.encodeToHexString(SameNameSecond(1)))
                assertEquals(SameNameSecond(1), proto.decodeFromHexString("1001"))
            }
            repeat(2) {
                if (firstGoesFirst) {
                    first()
                    second()
                } else {
                    second()
                    first()
                }
            }
        }
    }

    @Serializable
    data class WithUnknown(val a: Int, val unknown: ProtoUnknownFieldHolder)

    @Serializable
    data class Wider(val a: Int, val b: String, val c: List<Int>)

    @Test
    fun testUnknownFieldHolder() {
        val proto = ProtoBuf { }
        val bytes = proto.encodeToByteArray(Wider(1, "two", listOf(3, 4)))
        repeat(2) {
            val decoded = proto.decodeFromByteArray<WithUnknown>(bytes)
            assertEquals(1, decoded.a)
            assertEquals(Wider(1, "two", listOf(3, 4)), proto.decodeFromByteArray<Wider>(proto.encodeToByteArray(decoded)))
        }
    }

    @Serializable
    data class WithOneOf(@ProtoOneOf val value: OpenType, @ProtoNumber(1) val name: String)

    interface OpenType

    @Serializable
    data class IntCase(@ProtoNumber(11) val i: Int) : OpenType

    @Serializable
    data class StringCase(@ProtoNumber(12) val s: String) : OpenType

    @Test
    fun testInstancesWithDifferentModulesDoNotShareEntries() {
        val intOnly = ProtoBuf { serializersModule = SerializersModule { polymorphic(OpenType::class) { subclass(IntCase::class) } } }
        val stringOnly = ProtoBuf { serializersModule = SerializersModule { polymorphic(OpenType::class) { subclass(StringCase::class) } } }

        val intBytes = intOnly.encodeToByteArray(WithOneOf(IntCase(5), "n"))
        val stringBytes = stringOnly.encodeToByteArray(WithOneOf(StringCase("s"), "n"))

        repeat(2) {
            assertEquals(WithOneOf(IntCase(5), "n"), intOnly.decodeFromByteArray(intBytes))
            assertEquals(WithOneOf(StringCase("s"), "n"), stringOnly.decodeFromByteArray(stringBytes))
            assertFailsWith<SerializationException> { stringOnly.decodeFromByteArray<WithOneOf>(intBytes) }
            assertFailsWith<SerializationException> { intOnly.decodeFromByteArray<WithOneOf>(stringBytes) }
        }
    }

    @Serializable
    data class ZeroProtoNumber(@ProtoNumber(0) val value: Int)

    @Test
    fun testInvalidDescriptorsFailEveryTime() {
        val proto = ProtoBuf { }
        repeat(2) {
            assertFailsWith<SerializationException> { proto.encodeToByteArray(ZeroProtoNumber(42)) }
            assertFailsWith<SerializationException> { proto.decodeFromHexString<ZeroProtoNumber>("000f") }
        }
    }

    private class UniqueSerializer(name: String) : KSerializer<Int> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(name) {
            element<Int>("value", annotations = listOf(ProtoNumber(5)))
        }

        override fun serialize(encoder: Encoder, value: Int) =
            encoder.encodeStructure(descriptor) { encodeIntElement(descriptor, 0, value) }

        override fun deserialize(decoder: Decoder): Int = decoder.decodeStructure(descriptor) {
            var result = -1
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> result = decodeIntElement(descriptor, 0)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index $index")
                }
            }
            result
        }
    }

    @Test
    fun testCacheIsBounded() {
        val proto = ProtoBuf { }
        val bytes = proto.encodeToByteArray(UniqueSerializer("seed"), 9)
        for (i in 0 until MAX_CACHED_DESCRIPTORS + 16) {
            val serializer = UniqueSerializer("unique.$i")
            assertEquals(i, proto.decodeFromByteArray(serializer, proto.encodeToByteArray(serializer, i)))
        }
        assertEquals(MAX_CACHED_DESCRIPTORS, proto.descriptorCache.decoding.size)
        assertEquals(MAX_CACHED_DESCRIPTORS, proto.descriptorCache.encoding.size)
        assertEquals(9, proto.decodeFromByteArray(UniqueSerializer("not.cached"), bytes))
        assertContentEquals(bytes, proto.encodeToByteArray(UniqueSerializer("not.cached"), 9))
    }
}
