/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
import kotlin.test.*

class SerialPolymorphicNumberTest {
    @Serializable
    @UseSerialPolymorphicNumbers
    sealed class Sealed1 {
        @Serializable
        @SerialPolymorphicNumber(Sealed1::class, 1)
        class Case : Sealed1()
    }

    @Serializable
    sealed class Sealed2 {
        @Serializable
        @SerialPolymorphicNumber(Sealed2::class, 1)
        class Case : Sealed2()
    }

    @Serializable
    @UseSerialPolymorphicNumbers
    sealed class Sealed3 {
        @Serializable
        class Case : Sealed3()
    }

    @Serializable
    @UseSerialPolymorphicNumbers
    sealed class Sealed4 {
        @Serializable
        @UseSerialPolymorphicNumbers
        sealed class Sealed41 : Sealed4(){
            @Serializable
            @SerialPolymorphicNumber(Sealed4::class, 1)
            @SerialPolymorphicNumber(Sealed41::class, 2)
            class Case : Sealed41()
        }
    }

    @Test
    fun testSealed() {
        testConversion<Sealed1>(Sealed1.Case(), """{"type":1}""")
        testConversion<Sealed2>(Sealed2.Case(), """{"type":"kotlinx.serialization.SerialPolymorphicNumberTest.Case"}""")
        assertFailsWith(SerializationException::class) {
            Json.encodeToString<Sealed3>(Sealed3.Case())
        }
        assertFailsWith(SerializationException::class) {
            Json.decodeFromString<Sealed3>("{}")
        }
        testConversion<Sealed4>(Sealed4.Sealed41.Case(), """{"type":1}""")
        testConversion<Sealed4.Sealed41>(Sealed4.Sealed41.Case(), """{"type":2}""")
    }

    @Serializable
    @UseSerialPolymorphicNumbers
    sealed class Abstract {
        @Serializable
        @SerialPolymorphicNumber(Abstract::class, 1)
        class Case : Abstract()

        @Serializable
        class Default(val type: Int?):Abstract()
    }

    val json = Json {
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
        testConversion<Abstract>(json, Abstract.Case(), """{"type":1}""")
        testConversion<Abstract>(json, Abstract.Default(0), """{"type":0}""")
    }
}