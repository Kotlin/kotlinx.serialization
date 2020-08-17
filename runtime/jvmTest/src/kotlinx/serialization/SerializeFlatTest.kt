/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import org.junit.Test
import kotlin.test.*

// Serializable data class

@Serializable
data class Data(
        val value1: String,
        val value2: Int
)

// Serializable data class with explicit companion object

@Serializable
data class DataExplicit(
        val value1: String,
        val value2: Int
) {
    companion object
}

// Regular (non-data) class with var properties

@Serializable
class Reg {
    var value1: String = ""
    var value2: Int = 0
}

// Specify serializable names

@Serializable
data class Names(
        @SerialName("value1")
        val custom1: String,
        @SerialName("value2")
        val custom2: Int
)

// Custom serializer

@Serializable(with= CustomSerializer::class)
data class Custom(
        val _value1: String,
        val _value2: Int
)

@Suppress("NAME_SHADOWING")
object CustomSerializer : KSerializer<Custom> {
    override val descriptor = object : SerialDescriptor {
        override val serialName = "kotlinx.serialization.Custom"
        override val kind: SerialKind = StructureKind.CLASS
        override val elementsCount: Int get() = 2
        override fun getElementName(index: Int) = when(index) {
            0 -> "value1"
            1 -> "value2"
            else -> ""
        }
        override fun getElementIndex(name: String) = when(name) {
            "value1" -> 0
            "value2" -> 1
            else -> -1
        }

        override fun getElementAnnotations(index: Int): List<Annotation> = emptyList()
        override fun getElementDescriptor(index: Int): SerialDescriptor = fail("Should not be called")
        override fun isElementOptional(index: Int): Boolean = false
    }

    override fun serialize(encoder: Encoder, value: Custom) {
        val encoder = encoder.beginStructure(descriptor)
        encoder.encodeStringElement(descriptor, 0, value._value1)
        encoder.encodeIntElement(descriptor, 1, value._value2)
        encoder.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): Custom {
        val decoder = decoder.beginStructure(descriptor)
        if (decoder.decodeElementIndex(descriptor) != 0) throw java.lang.IllegalStateException()
        val value1 = decoder.decodeStringElement(descriptor, 0)
        if (decoder.decodeElementIndex(descriptor) != 1) throw java.lang.IllegalStateException()
        val value2 = decoder.decodeIntElement(descriptor, 1)
        if (decoder.decodeElementIndex(descriptor) != CompositeDecoder.DECODE_DONE) throw java.lang.IllegalStateException()
        decoder.endStructure(descriptor)
        return Custom(value1, value2)
    }
}

// External serializer

// not Serializable !!!
data class ExternalData(
        val value1: String,
        val value2: Int
)

@Serializer(forClass= ExternalData::class)
object ExternalSerializer

// --------- tests and utils ---------

class SerializeFlatTest() {
    @Test
    fun testData() {
        val out = Out("Data")
        out.encodeSerializableValue(serializer(), Data("s1", 42))
        out.done()

        val inp = Inp("Data")
        val data = inp.decodeSerializableValue(serializer<Data>())
        inp.done()
        assert(data.value1 == "s1" && data.value2 == 42)
    }

    @Test
    fun testDataExplicit() {
        val out = Out("DataExplicit")
        out.encodeSerializableValue(serializer(), DataExplicit("s1", 42))
        out.done()

        val inp = Inp("DataExplicit")
        val data = inp.decodeSerializableValue(serializer<DataExplicit>())
        inp.done()
        assert(data.value1 == "s1" && data.value2 == 42)
    }

    @Test
    fun testReg() {
        val out = Out("Reg")
        val reg = Reg()
        reg.value1 = "s1"
        reg.value2 = 42
        out.encodeSerializableValue(serializer(), reg)
        out.done()

        val inp = Inp("Reg")
        val data = inp.decodeSerializableValue(serializer<Reg>())
        inp.done()
        assert(data.value1 == "s1" && data.value2 == 42)
    }

    @Test
    fun testNames() {
        val out = Out("Names")
        out.encodeSerializableValue(serializer(), Names("s1", 42))
        out.done()

        val inp = Inp("Names")
        val data = inp.decodeSerializableValue(serializer<Names>())
        inp.done()
        assert(data.custom1 == "s1" && data.custom2 == 42)
    }

    @Test
    fun testCustom() {
        val out = Out("Custom")
        out.encodeSerializableValue(CustomSerializer, Custom("s1", 42))
        out.done()

        val inp = Inp("Custom")
        val data = inp.decodeSerializableValue(CustomSerializer)
        inp.done()
        assert(data._value1 == "s1" && data._value2 == 42)
    }

    @Test
    fun testExternalData() {
        val out = Out("ExternalData")
        out.encodeSerializableValue(ExternalSerializer, ExternalData("s1", 42))
        out.done()

        val inp = Inp("ExternalData")
        val data = inp.decodeSerializableValue(ExternalSerializer)
        inp.done()
        assert(data.value1 == "s1" && data.value2 == 42)
    }

    companion object {
        fun fail(msg: String): Nothing = throw RuntimeException(msg)

        fun checkDesc(name: String, desc: SerialDescriptor) {
            if (desc.serialName != "kotlinx.serialization." + name) fail("checkDesc name $desc")
            if (desc.kind != StructureKind.CLASS) fail("checkDesc kind ${desc.kind}")
            if (desc.getElementName(0) != "value1") fail("checkDesc[0] $desc")
            if (desc.getElementName(1) != "value2") fail("checkDesc[1] $desc")
        }
    }

    class Out(private val name: String) : AbstractEncoder() {
        var step = 0

        override fun beginStructure(
            descriptor: SerialDescriptor
        ): CompositeEncoder {
            checkDesc(name, descriptor)
            if (step == 0) step++ else fail("@$step: beginStructure($descriptor)")
            return this
        }

        override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
            checkDesc(name, descriptor)
            when (step) {
                1 -> if (index == 0) {
                    step++; return true
                }
                3 -> if (index == 1) {
                    step++; return true
                }
            }
            fail("@$step: encodeElement($descriptor, $index)")
        }

        override fun encodeString(value: String) {
            when (step) {
                2 -> if (value == "s1") {
                    step++; return
                }
            }
            fail("@$step: encodeString($value)")
        }

        override fun encodeInt(value: Int) {
            when (step) {
                4 -> if (value == 42) { step++; return }
            }
            fail("@$step: decodeInt($value)")
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            checkDesc(name, descriptor)
            if (step == 5) step++ else fail("@$step: endStructure($descriptor)")
        }

        fun done() {
            if (step != 6) fail("@$step: OUT FAIL")
        }
    }

    class Inp(private val name: String) : AbstractDecoder() {
        var step = 0

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            checkDesc(name, descriptor)
            if (step == 0) step++ else fail("@$step: beginStructure($descriptor)")
            return this
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            checkDesc(name, descriptor)
            when (step) {
                1 -> {
                    step++; return 0
                }
                3 -> {
                    step++; return 1
                }
                5 -> {
                    step++; return -1
                }
            }
            fail("@$step: decodeElementIndex($descriptor)")
        }

        override fun decodeString(): String {
            when (step) {
                2 -> { step++; return "s1" }
            }
            fail("@$step: decodeString()")
        }

        override fun decodeInt(): Int {
            when (step) {
                4 -> { step++; return 42 }
            }
            fail("@$step: decodeInt()")
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            checkDesc(name, descriptor)
            if (step == 6) step++ else fail("@$step: endStructure($descriptor)")
        }

        fun done() {
            if (step != 7) fail("@$step: INP FAIL")
        }
    }
}
