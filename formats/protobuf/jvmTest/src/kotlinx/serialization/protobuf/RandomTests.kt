/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import com.google.protobuf.GeneratedMessageV3
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.specs.ShouldSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.TestData.*


class RandomTest : ShouldSpec() {

    companion object {
        fun Gen<String>.generateNotEmpty() = nextPrintableString(Gen.choose(1, 100).generate())
    }

    object KTestData {
        @Serializable
        data class KTestInt32(@ProtoNumber(1) val a: Int) : IMessage {
            override fun toProtobufMessage(): GeneratedMessageV3 = TestInt32.newBuilder().setA(a).build()

            companion object : Gen<KTestInt32> {
                override fun generate(): KTestInt32 = KTestInt32(Gen.int().generate())
            }
        }

        @Serializable
        data class KTestSignedInt(@ProtoNumber(1) @ProtoType(ProtoIntegerType.SIGNED) val a: Int) : IMessage {
            override fun toProtobufMessage(): GeneratedMessageV3 = TestSignedInt.newBuilder().setA(a).build()

            companion object : Gen<KTestSignedInt> {
                override fun generate(): KTestSignedInt = KTestSignedInt(Gen.int().generate())
            }
        }

        @Serializable
        data class KTestSignedLong(@ProtoNumber(1) @ProtoType(ProtoIntegerType.SIGNED) val a: Long) : IMessage {
            override fun toProtobufMessage(): GeneratedMessageV3 = TestSignedLong.newBuilder().setA(a).build()

            companion object : Gen<KTestSignedLong> {
                override fun generate(): KTestSignedLong = KTestSignedLong(Gen.long().generate())
            }
        }

        @Serializable
        data class KTestFixedInt(@ProtoNumber(1) @ProtoType(ProtoIntegerType.FIXED) val a: Int) : IMessage {
            override fun toProtobufMessage(): GeneratedMessageV3 = TestFixedInt.newBuilder().setA(a).build()

            companion object : Gen<KTestFixedInt> {
                override fun generate(): KTestFixedInt = KTestFixedInt(Gen.int().generate())
            }
        }

        @Serializable
        data class KTestDouble(@ProtoNumber(1) val a: Double) : IMessage {
            override fun toProtobufMessage(): GeneratedMessageV3 = TestDouble.newBuilder().setA(a).build()

            companion object : Gen<KTestDouble> {
                override fun generate(): KTestDouble = KTestDouble(Gen.double().generate())
            }
        }

        @Serializable
        data class KTestBoolean(@ProtoNumber(1) val a: Boolean) : IMessage {
            override fun toProtobufMessage(): GeneratedMessageV3 = TestBoolean.newBuilder().setA(a).build()

            companion object : Gen<KTestBoolean> {
                override fun generate(): KTestBoolean = KTestBoolean(Gen.bool().generate())
            }
        }

        @Serializable
        data class KTestAllTypes(
            @ProtoNumber(1) val i32: Int,
            @ProtoNumber(2) @ProtoType(ProtoIntegerType.SIGNED) val si32: Int,
            @ProtoNumber(3) @ProtoType(ProtoIntegerType.FIXED) val f32: Int,
            @ProtoNumber(10) val i64: Long,
            @ProtoNumber(11) @ProtoType(ProtoIntegerType.SIGNED) val si64: Long,
            @ProtoNumber(12) @ProtoType(ProtoIntegerType.FIXED) val f64: Long,
            @ProtoNumber(21) val f: Float,
            @ProtoNumber(22) val d: Double,
            @ProtoNumber(41) val b: Boolean = false,
            @ProtoNumber(51) val s: String
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
            @ProtoNumber(1) val a: Int,
            @ProtoNumber(2) val b: Double,
            @ProtoNumber(10) val inner: KTestAllTypes,
            @ProtoNumber(20) val s: String
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
            @ProtoNumber(1) val s: Int,
            @ProtoNumber(10) val l: List<Int>
        ) : IMessage {
            override fun toProtobufMessage(): GeneratedMessageV3 = TestRepeatedIntMessage.newBuilder().setS(s).addAllB(l).build()

            companion object : Gen<KTestIntListMessage> {
                override fun generate() = KTestIntListMessage(Gen.int().generate(), Gen.list(Gen.int()).generate())
            }
        }

        @Serializable
        data class KTestObjectListMessage(
            @ProtoNumber(1) val inner: List<KTestAllTypes>
        ) : IMessage {
            override fun toProtobufMessage(): GeneratedMessageV3 = TestRepeatedObjectMessage.newBuilder().addAllInner(inner.map { it.toProtobufMessage() }).build()

            companion object : Gen<KTestObjectListMessage> {
                override fun generate() = KTestObjectListMessage(Gen.list(KTestAllTypes.Companion).generate())
            }
        }

        enum class KCoffee { AMERICANO, LATTE, CAPPUCCINO }

        @Serializable
        data class KTestEnum(@ProtoNumber(1) val a: KCoffee): IMessage {
            override fun toProtobufMessage() = TestEnum.newBuilder().setA(TestEnum.Coffee.forNumber(a.ordinal)).build()

            companion object : Gen<KTestEnum> {
                override fun generate(): KTestEnum = KTestEnum(Gen.oneOf<KCoffee>().generate())
            }
        }

        @Serializable
        data class KTestMap(@ProtoNumber(1) val s: Map<String, String>, @ProtoNumber(2) val o: Map<Int, KTestAllTypes> = emptyMap()) :
            IMessage {
            override fun toProtobufMessage() = TestMap.newBuilder()
                .putAllStringMap(s)
                .putAllIntObjectMap(o.mapValues { it.value.toProtobufMessage() })
                .build()

            companion object : Gen<KTestMap> {
                override fun generate(): KTestMap =
                    KTestMap(Gen.map(Gen.string(), Gen.string()).generate(), Gen.map(Gen.int(), KTestAllTypes).generate())
            }
        }
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
    }
}
