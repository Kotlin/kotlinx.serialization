/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import io.kotlintest.properties.*
import io.kotlintest.specs.*
import kotlinx.serialization.*

class RandomTest : ShouldSpec() {

    companion object {
        fun Gen<String>.generateNotEmpty() = nextPrintableString(Gen.choose(1, 100).generate())
    }

    object KTestData {
        @Serializable
        data class KTestInt32(val a: Int) {
            companion object : Gen<KTestInt32> {
                override fun generate(): KTestInt32 = KTestInt32(Gen.int().generate())
            }
        }

        @Serializable
        data class KTestSignedInt(val a: Int) {
            companion object : Gen<KTestSignedInt> {
                override fun generate(): KTestSignedInt = KTestSignedInt(Gen.int().generate())
            }
        }

        @Serializable
        data class KTestSignedLong(val a: Long) {
            companion object : Gen<KTestSignedLong> {
                override fun generate(): KTestSignedLong = KTestSignedLong(Gen.long().generate())
            }
        }

        @Serializable
        data class KTestFixedInt(val a: Int) {
            companion object : Gen<KTestFixedInt> {
                override fun generate(): KTestFixedInt = KTestFixedInt(Gen.int().generate())
            }
        }

        @Serializable
        data class KTestDouble(val a: Double) {
            companion object : Gen<KTestDouble> {
                override fun generate(): KTestDouble = KTestDouble(Gen.double().generate())
            }
        }

        @Serializable
        data class KTestBoolean(val a: Boolean) {
            companion object : Gen<KTestBoolean> {
                override fun generate(): KTestBoolean = KTestBoolean(Gen.bool().generate())
            }
        }

        @Serializable
        data class KTestAllTypes(
            val i32: Int,
            val si32: Int,
            val f32: Int,
            val i64: Long,
            val si64: Long,
            val f64: Long,
            val f: Float,
            val d: Double,
            val b: Boolean = false,
            val s: String
        ) {

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
            val a: Int,
            val b: Double,
            val inner: KTestAllTypes,
            val s: String
        ) {
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
            val s: Int,
            val l: List<Int>
        ) {
            companion object : Gen<KTestIntListMessage> {
                override fun generate() = KTestIntListMessage(Gen.int().generate(), Gen.list(Gen.int()).generate())
            }
        }

        @Serializable
        data class KTestObjectListMessage(
            val inner: List<KTestAllTypes>
        ) {
            companion object : Gen<KTestObjectListMessage> {
                override fun generate() = KTestObjectListMessage(Gen.list(KTestAllTypes.Companion).generate())
            }
        }

        enum class KCoffee { AMERICANO, LATTE, CAPPUCCINO }

        @Serializable
        data class KTestEnum(val a: KCoffee) {
            companion object : Gen<KTestEnum> {
                override fun generate(): KTestEnum = KTestEnum(Gen.oneOf<KCoffee>().generate())
            }
        }

        @Serializable
        data class KTestMap(val s: Map<String, String>, val o: Map<Int, KTestAllTypes> = emptyMap()) {
            companion object : Gen<KTestMap> {
                override fun generate(): KTestMap =
                    KTestMap(Gen.map(Gen.string(), Gen.string()).generate(), Gen.map(Gen.int(), KTestAllTypes).generate())
            }
        }
    }

    init {
        "CBOR Writer" {
            should("serialize random int32") { forAll(KTestData.KTestInt32.Companion) { dumpCborCompare(it) } }
            should("serialize random signed int32") { forAll(KTestData.KTestSignedInt.Companion) { dumpCborCompare(it) } }
            should("serialize random signed int64") { forAll(KTestData.KTestSignedLong.Companion) { dumpCborCompare(it) } }
            should("serialize random fixed int32") { forAll(KTestData.KTestFixedInt.Companion) { dumpCborCompare(it) } }
            should("serialize random doubles") { forAll(KTestData.KTestDouble.Companion) { dumpCborCompare(it) } }
            should("serialize random booleans") { forAll(KTestData.KTestBoolean.Companion) { dumpCborCompare(it) } }
            should("serialize random enums") { forAll(KTestData.KTestEnum.Companion) { dumpCborCompare(it) } }
            should("serialize all base random types") { forAll(KTestData.KTestAllTypes.Companion) { dumpCborCompare(it) } }
            should("serialize random messages with embedded message") {
                forAll(KTestData.KTestOuterMessage.Companion) {
                    dumpCborCompare(
                        it
                    )
                }
            }
            should("serialize random messages with primitive list fields") {
                forAll(KTestData.KTestIntListMessage.Companion) {
                    dumpCborCompare(
                        it
                    )
                }
            }
            should("serialize messages with object list fields") {
                forAll(KTestData.KTestObjectListMessage.Companion) {
                    dumpCborCompare(
                        it
                    )
                }
            }
            should("serialize messages with scalar-key maps") {
                forAll(KTestData.KTestMap.Companion) {
                    dumpCborCompare(
                        it
                    )
                }
            }
        }

        "CBOR Reader" {
            should("read random int32") { forAll(KTestData.KTestInt32.Companion) { readCborCompare(it) } }
            should("read random signed int32") { forAll(KTestData.KTestSignedInt.Companion) { readCborCompare(it) } }
            should("read random signed int64") { forAll(KTestData.KTestSignedLong.Companion) { readCborCompare(it) } }
            should("read random fixed int32") { forAll(KTestData.KTestFixedInt.Companion) { readCborCompare(it) } }
            should("read random doubles") { forAll(KTestData.KTestDouble.Companion) { readCborCompare(it) } }
            should("read random enums") { forAll(KTestData.KTestEnum.Companion) { readCborCompare(it) } }
            should("read all base random types") { forAll(KTestData.KTestAllTypes.Companion) { readCborCompare(it) } }
            should("read random messages with embedded message") {
                forAll(KTestData.KTestOuterMessage.Companion) {
                    readCborCompare(
                        it
                    )
                }
            }
            should("read random messages with primitive list fields") {
                forAll(KTestData.KTestIntListMessage.Companion) {
                    readCborCompare(
                        it
                    )
                }
            }
            should("read random messages with object list fields") {
                forAll(KTestData.KTestObjectListMessage.Companion) {
                    readCborCompare(
                        it
                    )
                }
            }
        }
    }
}
