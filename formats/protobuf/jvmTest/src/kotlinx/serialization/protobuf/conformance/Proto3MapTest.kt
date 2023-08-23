/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.conformance

import com.google.protobuf_test_messages.proto3.*
import io.kotlintest.properties.*
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import kotlin.test.*

@Serializable
data class KTestMessagesProto3Map(
    @ProtoNumber(56) val mapInt32Int32: Map<Int, Int> = emptyMap(),
    @ProtoNumber(57) val mapInt64Int64: Map<Long, Long> = emptyMap(),
    @ProtoNumber(58) val mapUint32Uint32: Map<UInt, UInt> = emptyMap(),
    @ProtoNumber(59) val mapUint64Uint64: Map<ULong, ULong> = emptyMap(),
    @ProtoNumber(60) val mapSint32Sint32: Map<Int, Int> = emptyMap(),
    @ProtoNumber(61) val mapSint64Sint64: Map<Long, Long> = emptyMap(),
    @ProtoNumber(62) val mapFixed32Fixed32: Map<Int, Int> = emptyMap(),
    @ProtoNumber(63) val mapFixed64Fixed64: Map<Long, Long> = emptyMap(),
    @ProtoNumber(64) val mapSfixed32Sfixed32: Map<Int, Int> = emptyMap(),
    @ProtoNumber(65) val mapSfixed64Sfixed64: Map<Long, Long> = emptyMap(),
    @ProtoNumber(66) val mapInt32Float: Map<Int, Float> = emptyMap(),
    @ProtoNumber(67) val mapInt32Double: Map<Int, Double> = emptyMap(),
    @ProtoNumber(68) val mapBoolBool: Map<Boolean, Boolean> = emptyMap(),
    @ProtoNumber(69) val mapStringString: Map<String, String> = emptyMap(),
    @ProtoNumber(70) val mapStringBytes: Map<String, ByteArray> = emptyMap(),
    @ProtoNumber(71) val mapStringNestedMessage: Map<String, KTestMessagesProto3Message.KNestedMessage> = emptyMap(),
    @ProtoNumber(72) val mapStringForeignMessage: Map<String, KForeignMessage> = emptyMap(),
    @ProtoNumber(73) val mapStringNestedEnum: Map<String, KTestMessagesProto3Enum.KNestedEnum> = emptyMap(),
    @ProtoNumber(74) val mapStringForeignEnum: Map<String, KForeignEnum> = emptyMap(),
)

class Proto3MapTest {
    @Test
    fun default() {
        val message = KTestMessagesProto3Map(
            mapInt32Int32 = Gen.map(Gen.int(), Gen.int()).generate(),
            mapInt64Int64 = Gen.map(Gen.long(), Gen.long()).generate(),
            mapUint32Uint32 = Gen.map(Gen.int().map { it.toUInt() }, Gen.int().map { it.toUInt() }).generate(),
            mapUint64Uint64 = Gen.map(Gen.int().map { it.toULong() }, Gen.int().map { it.toULong() }).generate(),
            mapInt32Float = Gen.map(Gen.int(), Gen.float()).generate(),
            mapInt32Double = Gen.map(Gen.int(), Gen.double()).generate(),
            mapBoolBool = Gen.map(Gen.bool(), Gen.bool()).generate(),
            mapStringString = Gen.map(Gen.string(), Gen.string()).generate(),
            mapStringBytes = Gen.map(Gen.string(), Gen.string().map { it.toByteArray() }).generate(),
            mapStringNestedMessage = mapOf(
                "asd_1" to KTestMessagesProto3Message.KNestedMessage(
                    1,
                    null
                ),
                "asi_#" to KTestMessagesProto3Message.KNestedMessage(
                    2,
                    KTestMessagesProto3Message(
                        KTestMessagesProto3Message.KNestedMessage(3, null),
                    )
                )
            ),
            mapStringForeignMessage = mapOf(
                "" to KForeignMessage(1),
                "-2" to KForeignMessage(-12),
            ),
            mapStringNestedEnum = Gen.map(
                Gen.string(), Gen.oneOf(
                    KTestMessagesProto3Enum.KNestedEnum.entries,
                )
            ).generate(),
            mapStringForeignEnum = Gen.map(
                Gen.string(), Gen.oneOf(
                    KForeignEnum.entries,
                )
            ).generate(),
        )

        val bytes = ProtoBuf.encodeToByteArray(message)
        val restored = TestMessagesProto3.TestAllTypesProto3.parseFrom(bytes)


        assertEquals(message.mapInt32Int32, restored.mapInt32Int32Map)
        assertEquals(message.mapInt64Int64, restored.mapInt64Int64Map)
        assertEquals(
            message.mapUint32Uint32,
            restored.mapUint32Uint32Map.map { it.key.toUInt() to it.value.toUInt() }.toMap()
        )
        assertEquals(
            message.mapUint64Uint64,
            restored.mapUint64Uint64Map.map { it.key.toULong() to it.value.toULong() }.toMap()
        )
        assertEquals(message.mapInt32Float, restored.mapInt32FloatMap)
        assertEquals(message.mapInt32Double, restored.mapInt32DoubleMap)
        assertEquals(message.mapBoolBool, restored.mapBoolBoolMap)
        assertEquals(message.mapStringString, restored.mapStringStringMap)
        assertContentEquals(
            message.mapStringBytes.mapValues { it.value.toString(Charsets.UTF_32) }.entries.toList(),
            restored.mapStringBytesMap.mapValues { it.value.toByteArray().toString(Charsets.UTF_32) }.entries.toList()
        )
        assertEquals(
            message.mapStringNestedMessage.mapValues { it.value.toProto() },
            restored.mapStringNestedMessageMap
        )
        assertEquals(
            message.mapStringForeignMessage.mapValues { it.value.toProto() },
            restored.mapStringForeignMessageMap
        )
        assertEquals(
            message.mapStringNestedEnum.mapValues { it.value.name },
            restored.mapStringNestedEnumMap.mapValues { it.value.name },
        )
        assertEquals(
            message.mapStringForeignEnum.mapValues { it.value.name },
            restored.mapStringForeignEnumMap.mapValues { it.value.name }
        )

        val restoredMessage = ProtoBuf.decodeFromByteArray<KTestMessagesProto3Map>(restored.toByteArray())
        assertEquals(message.copy(mapStringBytes = mapOf()), restoredMessage.copy(mapStringBytes = mapOf()))
    }

    @Test
    @Ignore
    // Issue: https://github.com/Kotlin/kotlinx.serialization/issues/2417
    fun signedAndFixed() {
        val message = KTestMessagesProto3Map(
            mapSint32Sint32 = Gen.map(Gen.int(), Gen.int()).generate(),
            mapSint64Sint64 = Gen.map(Gen.long(), Gen.long()).generate(),
            mapFixed32Fixed32 = Gen.map(Gen.int(), Gen.int()).generate(),
            mapFixed64Fixed64 = Gen.map(Gen.long(), Gen.long()).generate(),
            mapSfixed32Sfixed32 = Gen.map(Gen.int(), Gen.int()).generate(),
            mapSfixed64Sfixed64 = Gen.map(Gen.long(), Gen.long()).generate(),
        )

        val bytes = ProtoBuf.encodeToByteArray(message)
        val restored = TestMessagesProto3.TestAllTypesProto3.parseFrom(bytes)


        assertContentEquals(message.mapSint32Sint32.entries.toList(), restored.mapSint32Sint32Map.entries.toList())
        assertContentEquals(message.mapSint64Sint64.entries.toList(), restored.mapSint64Sint64Map.entries.toList())
        assertContentEquals(message.mapFixed32Fixed32.entries.toList(), restored.mapFixed32Fixed32Map.entries.toList())
        assertContentEquals(message.mapFixed64Fixed64.entries.toList(), restored.mapFixed64Fixed64Map.entries.toList())
        assertContentEquals(
            message.mapSfixed32Sfixed32.entries.toList(),
            restored.mapSfixed32Sfixed32Map.entries.toList()
        )
        assertContentEquals(
            message.mapSfixed64Sfixed64.entries.toList(),
            restored.mapSfixed64Sfixed64Map.entries.toList()
        )


        val restoredMessage = ProtoBuf.decodeFromByteArray<KTestMessagesProto3Map>(restored.toByteArray())
        assertEquals(message, restoredMessage)
    }
}
