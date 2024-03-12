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
        testConversion<Sealed1>(Sealed1.Case1(1), """{"type":1,"property":1}""")
        testConversion<Sealed1>(Sealed1.Case2, """{"type":2}""")
        testConversion<Sealed2>(Sealed2.Case, """{"type":"${Sealed2.Case.serializer().descriptor.serialName}"}""")
        assertFailsWith(SerializationException::class) {
            Json.encodeToString<Sealed3>(Sealed3.Case)
        }
        assertFailsWith(SerializationException::class) {
            Json.decodeFromString<Sealed3>("{}")
        }
        testConversion<Sealed4>(Sealed4.Sealed41.Case, """{"type":1}""")
        testConversion<Sealed4.Sealed41>(Sealed4.Sealed41.Case, """{"type":2}""")
    }

    @Serializable
    @UseSerialPolymorphicNumbers
    abstract class Abstract {
        @Serializable
        @SerialPolymorphicNumber(Abstract::class, 1)
        object Case : Abstract()

        @Serializable
        data class Default(val type: Int?) : Abstract()
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
        testConversion<Abstract>(json, Abstract.Case, """{"type":1}""")
        assertEquals(Abstract.Default(0), json.decodeFromString<Abstract>("""{"type":0}"""))
    }
}