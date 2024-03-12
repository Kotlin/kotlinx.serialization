/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.modules.*
import kotlin.test.*

/**
 * Copied and adapted from [kotlinx.serialization.SerialPolymorphicNumberTest].
 */
class SerialPolymorphicNumberTest {
    inline fun <reified T> testConversion(protoBuf: ProtoBuf, data: T, expectedHexString: String) {
        val string = protoBuf.encodeToHexString(data)
        assertEquals(expectedHexString, string)
        assertEquals(data, protoBuf.decodeFromHexString(string))
    }

    inline fun <reified T> testConversion(data: T, expectedHexString: String) =
        testConversion(ProtoBuf, data, expectedHexString)

    @Serializable
    @UseSerialPolymorphicNumbers
    sealed class Sealed1 {
        @Serializable
        @SerialPolymorphicNumber(Sealed1::class, 1)
        data class Case1(val property: Int) : Sealed1()

        @Serializable
        @SerialPolymorphicNumber(Sealed1::class, 2)
        object Case2 : Sealed1()
    }

    @Serializable
    sealed class Sealed2 {
        @Serializable
        @SerialPolymorphicNumber(Sealed2::class, 1)
        object Case : Sealed2()
    }

    @Serializable
    @UseSerialPolymorphicNumbers
    sealed class Sealed3 {
        @Serializable
        object Case : Sealed3()
    }

    @Serializable
    @UseSerialPolymorphicNumbers
    sealed class Sealed4 {
        @Serializable
        @UseSerialPolymorphicNumbers
        sealed class Sealed41 : Sealed4() {
            @Serializable
            @SerialPolymorphicNumber(Sealed4::class, 1)
            @SerialPolymorphicNumber(Sealed41::class, 2)
            object Case : Sealed41()
        }
    }

    @Test
    fun testSealed() {
        testConversion<Sealed1>(Sealed1.Case1(1), "080112020801")
        testConversion<Sealed1>(Sealed1.Case2, "08021200")
        run {
            val serialName = Sealed2.Case.serializer().descriptor.serialName
            testConversion<Sealed2>(
                Sealed2.Case,
                "0a" + serialName.length.toByte().toHexString() + serialName.encodeToByteArray().toHexString() + "1200"
            )
        }
        assertFailsWith(SerializationException::class) {
            ProtoBuf.encodeToHexString<Sealed3>(Sealed3.Case)
        }
        assertFailsWith(SerializationException::class) {
            ProtoBuf.decodeFromHexString<Sealed3>("08011200")
        }
        testConversion<Sealed4>(Sealed4.Sealed41.Case, "08011200")
        testConversion<Sealed4.Sealed41>(Sealed4.Sealed41.Case, "08021200")
    }

    @Serializable
    @UseSerialPolymorphicNumbers
    abstract class Abstract {
        @Serializable
        @SerialPolymorphicNumber(Abstract::class, 1)
        object Case : Abstract()

        @Serializable
        object Default : Abstract()
    }

    val protoBuf = ProtoBuf {
        serializersModule = SerializersModule {
            polymorphic(Abstract::class) {
                subclass(Abstract.Case::class)
                defaultDeserializerForNumber {
                    Abstract.Default.serializer()
                }
            }
        }
    }

    @Test
    fun testPolymorphicModule() {
        testConversion<Abstract>(protoBuf, Abstract.Case, "08011200")
        assertEquals(Abstract.Default, protoBuf.decodeFromHexString<Abstract>("08001200"))
    }
}