/*
 * Copyright 2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.formats

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.cbor.CBORFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.protobuf.GeneratedMessageV3
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.specs.ShouldSpec
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CBOR
import kotlinx.serialization.formats.proto.TestData.*
import kotlinx.serialization.internal.HexConverter
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumberType
import kotlinx.serialization.protobuf.ProtoType
import java.io.ByteArrayOutputStream

fun GeneratedMessageV3.toHex(): String {
    val b = ByteArrayOutputStream()
    this.writeTo(b)
    return (HexConverter.printHexBinary(b.toByteArray(), lowerCase = true))
}

fun Gen<String>.generateNotEmpty() = nextPrintableString(Gen.choose(1, 100).generate())

fun <K, V> Gen.Companion.map(genK: Gen<K>, genV: Gen<V>): Gen<Map<K,V>> = object : Gen<Map<K,V>>  {
    private val entryGen = object : Gen<Pair<K,V>> {
        override fun generate(): Pair<K, V> = genK.generate() to genV.generate()
    }

    override fun generate(): Map<K,V> = Gen.list(entryGen).generate().toMap()
}

interface IMessage {
    fun toProtobufMessage(): GeneratedMessageV3
}

object KTestData {
    @Serializable
    data class KTestInt32(@SerialId(1) val a: Int) : IMessage {
        override fun toProtobufMessage(): GeneratedMessageV3 = TestInt32.newBuilder().setA(a).build()

        companion object : Gen<KTestInt32> {
            override fun generate(): KTestInt32 = KTestInt32(Gen.int().generate())
        }
    }

    @Serializable
    data class KTestSignedInt(@SerialId(1) @ProtoType(ProtoNumberType.SIGNED) val a: Int) : IMessage {
        override fun toProtobufMessage(): GeneratedMessageV3 = TestSignedInt.newBuilder().setA(a).build()

        companion object : Gen<KTestSignedInt> {
            override fun generate(): KTestSignedInt = KTestSignedInt(Gen.int().generate())
        }
    }

    @Serializable
    data class KTestSignedLong(@SerialId(1) @ProtoType(ProtoNumberType.SIGNED) val a: Long) : IMessage {
        override fun toProtobufMessage(): GeneratedMessageV3 = TestSignedLong.newBuilder().setA(a).build()

        companion object : Gen<KTestSignedLong> {
            override fun generate(): KTestSignedLong = KTestSignedLong(Gen.long().generate())
        }
    }

    @Serializable
    data class KTestFixedInt(@SerialId(1) @ProtoType(ProtoNumberType.FIXED) val a: Int) : IMessage {
        override fun toProtobufMessage(): GeneratedMessageV3 = TestFixedInt.newBuilder().setA(a).build()

        companion object : Gen<KTestFixedInt> {
            override fun generate(): KTestFixedInt = KTestFixedInt(Gen.int().generate())
        }
    }

    @Serializable
    data class KTestDouble(@SerialId(1) val a: Double) : IMessage {
        override fun toProtobufMessage(): GeneratedMessageV3 = TestDouble.newBuilder().setA(a).build()

        companion object : Gen<KTestDouble> {
            override fun generate(): KTestDouble = KTestDouble(Gen.double().generate())
        }
    }

    @Serializable
    data class KTestBoolean(@SerialId(1) val a: Boolean) : IMessage {
        override fun toProtobufMessage(): GeneratedMessageV3 = TestBoolean.newBuilder().setA(a).build()

        companion object : Gen<KTestBoolean> {
            override fun generate(): KTestBoolean = KTestBoolean(Gen.bool().generate())
        }
    }

    @Serializable
    data class KTestAllTypes(
            @SerialId(1) val i32: Int,
            @SerialId(2) @ProtoType(ProtoNumberType.SIGNED) val si32: Int,
            @SerialId(3) @ProtoType(ProtoNumberType.FIXED) val f32: Int,
            @SerialId(10) val i64: Long,
            @SerialId(11) @ProtoType(ProtoNumberType.SIGNED) val si64: Long,
            @SerialId(12) @ProtoType(ProtoNumberType.FIXED) val f64: Long,
            @SerialId(21) val f: Float,
            @SerialId(22) val d: Double,
            @SerialId(41) @Optional val b: Boolean = false,
            @SerialId(51) val s: String
    ) : IMessage {
        override fun toProtobufMessage(): TestAllTypes = TestAllTypes.newBuilder()
                .setI32(i32)
                .setSi32(si32)
                .setF32(f32)
                .setI64(i64)
                .setSi64(si64)
                .setF64(f64)
                .setF(f)
                .setD(d)
                .setB(b)
                .setS(s)
                .build()

        companion object : Gen<KTestAllTypes> {
            override fun generate(): KTestAllTypes = KTestAllTypes(
                    Gen.int().generate(),
                    Gen.int().generate(),
                    Gen.int().generate(),
                    Gen.long().generate(),
                    Gen.long().generate(),
                    Gen.long().generate(),
                    Gen.float().generate(),
                    Gen.double().generate(),
                    Gen.bool().generate(),
                    Gen.string().generateNotEmpty()
            )
        }
    }

    @Serializable
    data class KTestOuterMessage(
            @SerialId(1) val a: Int,
            @SerialId(2) val b: Double,
            @SerialId(10) val inner: KTestAllTypes,
            @SerialId(20) val s: String
    ) : IMessage {
        override fun toProtobufMessage(): GeneratedMessageV3 = TestOuterMessage.newBuilder()
                .setA(a)
                .setB(b)
                .setInner(inner.toProtobufMessage())
                .setS(s)
                .build()

        companion object : Gen<KTestOuterMessage> {
            override fun generate(): KTestOuterMessage = KTestOuterMessage(
                    Gen.int().generate(),
                    Gen.double().generate(),
                    KTestAllTypes.generate(),
                    Gen.string().generateNotEmpty()
            )
        }
    }

    @Serializable
    data class KTestIntListMessage(
            @SerialId(1) val s: Int,
            @SerialId(10) val l: List<Int>
    ) : IMessage {
        override fun toProtobufMessage(): GeneratedMessageV3 = TestRepeatedIntMessage.newBuilder().setS(s).addAllB(l).build()

        companion object : Gen<KTestIntListMessage> {
            override fun generate() = KTestIntListMessage(Gen.int().generate(), Gen.list(Gen.int()).generate())
        }
    }

    @Serializable
    data class KTestObjectListMessage(
            @SerialId(1) val inner: List<KTestAllTypes>
    ) : IMessage {
        override fun toProtobufMessage(): GeneratedMessageV3 = TestRepeatedObjectMessage.newBuilder().addAllInner(inner.map { it.toProtobufMessage() }).build()

        companion object : Gen<KTestObjectListMessage> {
            override fun generate() = KTestObjectListMessage(Gen.list(KTestAllTypes.Companion).generate())
        }
    }

    enum class KCoffee { AMERICANO, LATTE, CAPPUCCINO }

    @Serializable
    data class KTestEnum(@SerialId(1) val a: KCoffee): IMessage {
        override fun toProtobufMessage() = TestEnum.newBuilder().setA(TestEnum.Coffee.forNumber(a.ordinal)).build()

        companion object : Gen<KTestEnum> {
            override fun generate(): KTestEnum = KTestEnum(Gen.oneOf<KCoffee>().generate())
        }
    }

    @Serializable
    data class KTestMap(@SerialId(1) val s: Map<String, String>, @SerialId(2) val o: Map<Int, KTestAllTypes>): IMessage {
        override fun toProtobufMessage() = TestMap.newBuilder()
                .putAllStringMap(s)
                .putAllIntObjectMap(o.mapValues { it.value.toProtobufMessage() })
                .build()

        companion object : Gen<KTestMap> {
            override fun generate(): KTestMap = KTestMap(Gen.map(Gen.string(), Gen.string()).generate(), Gen.map(Gen.int(), KTestAllTypes).generate())
        }
    }
}


class RandomTest : ShouldSpec() {
    val CBORJackson = ObjectMapper(CBORFactory()).apply { registerKotlinModule() }

    inline fun <reified T : IMessage> dumpCompare(it: T, alwaysPrint: Boolean = false): Boolean {
        val msg = it.toProtobufMessage()
        var parsed: GeneratedMessageV3?
        val c = try {
            val bytes = ProtoBuf.dump(it)
            parsed = msg.parserForType.parseFrom(bytes)
            msg == parsed
        } catch (e: Exception) {
            e.printStackTrace()
            parsed = null
            false
        }
        if (!c || alwaysPrint) println("Expected: $msg\nfound: $parsed")
        return c
    }

    inline fun <reified T : IMessage> dumpCBORCompare(it: T, alwaysPrint: Boolean = false): Boolean {
        var parsed: T?
        val c = try {
            val bytes = CBOR.dump(it)
            parsed = CBORJackson.readValue<T>(bytes)
            it == parsed
        } catch (e: Exception) {
            e.printStackTrace()
            parsed = null
            false
        }
        if (!c || alwaysPrint) println("Expected: $it\nfound: $parsed")
        return c
    }

    inline fun <reified T : IMessage> readCompare(it: T, alwaysPrint: Boolean = false): Boolean {
        var obj: T?
        val c = try {
            val msg = it.toProtobufMessage()
            val hex = msg.toHex()
            obj = ProtoBuf.loads<T>(hex)
            obj == it
        } catch (e: Exception) {
            obj = null
            e.printStackTrace()
            false
        }
        if (!c || alwaysPrint) println("Expected: $it\nfound: $obj")
        return c
    }

    inline fun <reified T : IMessage> readCBORCompare(it: T, alwaysPrint: Boolean = false): Boolean {
        var obj: T?
        val c = try {
            val hex = CBORJackson.writeValueAsBytes(it)
            obj = CBOR.load<T>(hex)
            obj == it
        } catch (e: Exception) {
            obj = null
            e.printStackTrace()
            false
        }
        if (!c || alwaysPrint) println("Expected: $it\nfound: $obj")
        return c
    }

    init {
        "Protobuf serialization" {
            should("serialize random int32") { forAll(KTestData.KTestInt32.Companion) { dumpCompare(it) } }
            should("serialize random signed int32") { forAll(KTestData.KTestSignedInt.Companion) { dumpCompare(it) } }
            should("serialize random signed int64") { forAll(KTestData.KTestSignedLong.Companion) { dumpCompare(it) } }
            should("serialize random fixed int32") { forAll(KTestData.KTestFixedInt.Companion) { dumpCompare(it) } }
            should("serialize random doubles") { forAll(KTestData.KTestDouble.Companion) { dumpCompare(it) } }
            should("serialize random booleans") { forAll(KTestData.KTestBoolean.Companion) { dumpCompare(it) } }
            should("serialize random enums") { forAll(KTestData.KTestEnum.Companion) { dumpCompare(it) } }
            should("serialize all base random types") { forAll(KTestData.KTestAllTypes.Companion) { dumpCompare(it) } }
            should("serialize random messages with embedded message") { forAll(KTestData.KTestOuterMessage.Companion) { dumpCompare(it) } }
            should("serialize random messages with primitive list fields as repeated") { forAll(KTestData.KTestIntListMessage.Companion) { dumpCompare(it) } }
            should("serialize messages with object list fields as repeated") { forAll(KTestData.KTestObjectListMessage.Companion) { dumpCompare(it) } }
            should("serialize messages with scalar-key maps") { forAll(KTestData.KTestMap.Companion) { dumpCompare(it) } }
        }

        "Protobuf deserialization" {
            should("read random int32") { forAll(KTestData.KTestInt32.Companion) { readCompare(it) } }
            should("read random signed int32") { forAll(KTestData.KTestSignedInt.Companion) { readCompare(it) } }
            should("read random signed int64") { forAll(KTestData.KTestSignedLong.Companion) { readCompare(it) } }
            should("read random fixed int32") { forAll(KTestData.KTestFixedInt.Companion) { readCompare(it) } }
            should("read random doubles") { forAll(KTestData.KTestDouble.Companion) { readCompare(it) } }
            should("read random enums") { forAll(KTestData.KTestEnum.Companion) { readCompare(it) } }
            should("read all base random types") { forAll(KTestData.KTestAllTypes.Companion) { readCompare(it) } }
            should("read random messages with embedded message") { forAll(KTestData.KTestOuterMessage.Companion) { readCompare(it) } }
            should("read random messages with primitive list fields as repeated") { forAll(KTestData.KTestIntListMessage.Companion) { readCompare(it) } }
            should("read random messages with object list fields as repeated") { forAll(KTestData.KTestObjectListMessage.Companion) { readCompare(it) } }
            should("read messages with scalar-key maps") { forAll(KTestData.KTestMap.Companion) { readCompare(it) } }
        }

        "CBOR Writer" {
            should("serialize random int32") { forAll(KTestData.KTestInt32.Companion) { dumpCBORCompare(it) } }
            should("serialize random signed int32") { forAll(KTestData.KTestSignedInt.Companion) { dumpCBORCompare(it) } }
            should("serialize random signed int64") { forAll(KTestData.KTestSignedLong.Companion) { dumpCBORCompare(it) } }
            should("serialize random fixed int32") { forAll(KTestData.KTestFixedInt.Companion) { dumpCBORCompare(it) } }
            should("serialize random doubles") { forAll(KTestData.KTestDouble.Companion) { dumpCBORCompare(it) } }
            should("serialize random booleans") { forAll(KTestData.KTestBoolean.Companion) { dumpCBORCompare(it) } }
            should("serialize random enums") { forAll(KTestData.KTestEnum.Companion) { dumpCBORCompare(it) } }
            should("serialize all base random types") { forAll(KTestData.KTestAllTypes.Companion) { dumpCBORCompare(it) } }
            should("serialize random messages with embedded message") { forAll(KTestData.KTestOuterMessage.Companion) { dumpCBORCompare(it) } }
            should("serialize random messages with primitive list fields") { forAll(KTestData.KTestIntListMessage.Companion) { dumpCBORCompare(it) } }
            should("serialize messages with object list fields") { forAll(KTestData.KTestObjectListMessage.Companion) { dumpCBORCompare(it) } }
            should("serialize messages with scalar-key maps") { forAll(KTestData.KTestMap.Companion) { dumpCBORCompare(it) } }
        }

        "CBOR Reader" {
            should("read random int32") { forAll(KTestData.KTestInt32.Companion) { readCBORCompare(it) } }
            should("read random signed int32") { forAll(KTestData.KTestSignedInt.Companion) { readCBORCompare(it) } }
            should("read random signed int64") { forAll(KTestData.KTestSignedLong.Companion) { readCBORCompare(it) } }
            should("read random fixed int32") { forAll(KTestData.KTestFixedInt.Companion) { readCBORCompare(it) } }
            should("read random doubles") { forAll(KTestData.KTestDouble.Companion) { readCBORCompare(it) } }
            should("read random enums") { forAll(KTestData.KTestEnum.Companion) { readCBORCompare(it) } }
            should("read all base random types") { forAll(KTestData.KTestAllTypes.Companion) { readCBORCompare(it) } }
            should("read random messages with embedded message") { forAll(KTestData.KTestOuterMessage.Companion) { readCBORCompare(it) } }
            should("read random messages with primitive list fields") { forAll(KTestData.KTestIntListMessage.Companion) { readCBORCompare(it) } }
            should("read random messages with object list fields") { forAll(KTestData.KTestObjectListMessage.Companion) { readCBORCompare(it) } }
        }
    }
}
