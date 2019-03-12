/*
 * Copyright 2018 JetBrains s.r.o.
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

package kotlinx.serialization

import org.junit.Test

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
        override val name = "kotlinx.serialization.Custom"
        override val kind: SerialKind = StructureKind.CLASS
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

        override fun isElementOptional(index: Int): Boolean = false
    }

    override fun serialize(encoder: Encoder, obj : Custom) {
        val encoder = encoder.beginStructure(descriptor)
        encoder.encodeStringElement(descriptor, 0, obj._value1)
        encoder.encodeIntElement(descriptor, 1, obj._value2)
        encoder.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): Custom {
        val decoder = decoder.beginStructure(descriptor)
        if (decoder.decodeElementIndex(descriptor) != 0) throw java.lang.IllegalStateException()
        val value1 = decoder.decodeStringElement(descriptor, 0)
        if (decoder.decodeElementIndex(descriptor) != 1) throw java.lang.IllegalStateException()
        val value2 = decoder.decodeIntElement(descriptor, 1)
        if (decoder.decodeElementIndex(descriptor) != CompositeDecoder.READ_DONE) throw java.lang.IllegalStateException()
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
        out.encode(Data::class.serializer(), Data("s1", 42))
        out.done()

        val inp = Inp("Data")
        val data = inp.decode(Data::class.serializer())
        inp.done()
        assert(data.value1 == "s1" && data.value2 == 42)
    }

    @Test
    fun testDataExplicit() {
        val out = Out("DataExplicit")
        out.encode(DataExplicit::class.serializer(), DataExplicit("s1", 42))
        out.done()

        val inp = Inp("DataExplicit")
        val data = inp.decode(DataExplicit::class.serializer())
        inp.done()
        assert(data.value1 == "s1" && data.value2 == 42)
    }

    @Test
    fun testReg() {
        val out = Out("Reg")
        val reg = Reg();
        reg.value1 = "s1"
        reg.value2 = 42
        out.encode(Reg::class.serializer(), reg)
        out.done()

        val inp = Inp("Reg")
        val data = inp.decode(Reg::class.serializer())
        inp.done()
        assert(data.value1 == "s1" && data.value2 == 42)
    }

    @Test
    fun testNames() {
        val out = Out("Names")
        out.encode(Names::class.serializer(), Names("s1", 42))
        out.done()

        val inp = Inp("Names")
        val data = inp.decode(Names::class.serializer())
        inp.done()
        assert(data.custom1 == "s1" && data.custom2 == 42)
    }

    @Test
    fun testCustom() {
        val out = Out("Custom")
        out.encode(CustomSerializer, Custom("s1", 42))
        out.done()

        val inp = Inp("Custom")
        val data = inp.decode(CustomSerializer)
        inp.done()
        assert(data._value1 == "s1" && data._value2 == 42)
    }

    @Test
    fun testExternalData() {
        val out = Out("ExternalData")
        out.encode(ExternalSerializer, ExternalData("s1", 42))
        out.done()

        val inp = Inp("ExternalData")
        val data = inp.decode(ExternalSerializer)
        inp.done()
        assert(data.value1 == "s1" && data.value2 == 42)
    }

    companion object {
        fun fail(msg: String): Nothing = throw RuntimeException(msg)

        fun checkDesc(name: String, desc: SerialDescriptor) {
            if (desc.name != "kotlinx.serialization." + name) fail("checkDesc name $desc")
            if (desc.kind != StructureKind.CLASS) fail("checkDesc kind ${desc.kind}")
            if (desc.getElementName(0) != "value1") fail("checkDesc[0] $desc")
            if (desc.getElementName(1) != "value2") fail("checkDesc[1] $desc")
        }
    }

    class Out(private val name: String) : ElementValueEncoder() {
        var step = 0

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
            checkDesc(name, desc)
            if (step == 0) step++ else fail("@$step: beginStructure($desc)")
            return this
        }

        override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
            checkDesc(name, desc)
            when (step) {
                1 -> if (index == 0) { step++; return true }
                3 -> if (index == 1) { step++; return true }
            }
            fail("@$step: encodeElement($desc, $index)")
        }

        override fun encodeString(value: String) {
            when (step) {
                2 -> if (value == "s1") { step++; return }
            }
            fail("@$step: encodeString($value)")
        }

        override fun encodeInt(value: Int) {
            when (step) {
                4 -> if (value == 42) { step++; return }
            }
            fail("@$step: decodeInt($value)")
        }

        override fun endStructure(desc: SerialDescriptor) {
            checkDesc(name, desc)
            if (step == 5) step++ else fail("@$step: endStructure($desc)")
        }

        fun done() {
            if (step != 6) fail("@$step: OUT FAIL")
        }
    }

    class Inp(private val name: String) : ElementValueDecoder() {
        var step = 0

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            checkDesc(name, desc)
            if (step == 0) step++ else fail("@$step: beginStructure($desc)")
            return this
        }

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            checkDesc(name, desc)
            when (step) {
                1 -> { step++; return 0 }
                3 -> { step++; return 1 }
                5 -> { step++; return -1 }
            }
            fail("@$step: decodeElementIndex($desc)")
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

        override fun endStructure(desc: SerialDescriptor) {
            checkDesc(name, desc)
            if (step == 6) step++ else fail("@$step: endStructure($desc)")
        }

        fun done() {
            if (step != 7) fail("@$step: INP FAIL")
        }
    }
}
