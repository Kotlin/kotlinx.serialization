/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import org.junit.Test
import kotlin.test.*

class SerializationMethodInvocationOrderTest {

    @Serializable
    @SerialName("kotlinx.serialization.Container")
    data class Container(val data: Data)

    @Test
    fun testRec() {
        val out = Out()
        out.encodeSerializableValue(serializer(), Container(Data("s1", 42)))
        out.done()

        val inp = Inp()
        inp.decodeSerializableValue(serializer<Container>())
        inp.done()
    }

    companion object {
        fun checkContainerDesc(desc: SerialDescriptor) {
            if (desc.serialName != "kotlinx.serialization.Container") fail("checkContainerDesc name $desc")
            if (desc.getElementName(0) != "data") fail("checkContainerDesc $desc")
        }

        fun checkDataDesc(desc: SerialDescriptor) {
            if (desc.serialName != "kotlinx.serialization.Data") fail("checkDataDesc name $desc")
            if (desc.getElementName(0) != "value1") fail("checkDataDesc.0 $desc")
            if (desc.getElementName(1) != "value2") fail("checkDataDesc.1 $desc")
        }
    }

    class Out : AbstractEncoder() {
        var step = 0

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
            when (step) {
                1 -> {
                    checkContainerDesc(descriptor); step++; return this
                }
                4 -> {
                    checkDataDesc(descriptor); step++; return this
                }
            }
            fail("@$step: beginStructure($descriptor)")
        }

        override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
            when (step) {
                2 -> {
                    checkContainerDesc(descriptor); if (index == 0) {
                        step++; return true
                    }
                }
                5 -> {
                    checkDataDesc(descriptor); if (index == 0) {
                        step++; return true
                    }
                }
                7 -> {
                    checkDataDesc(descriptor); if (index == 1) {
                        step++; return true
                    }
                }
            }
            fail("@$step: encodeElement($descriptor, $index)")
        }

        override fun <T : Any?> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
            when (step) {
                0, 3 -> { step++; serializer.serialize(this, value); return }
            }
            fail("@$step: encodeSerializableValue($value)")
        }

        override fun encodeString(value: String) {
            when (step) {
                6 -> if (value == "s1") { step++; return }
            }
            fail("@$step: encodeString($value)")
        }

        override fun encodeInt(value: Int) {
            when (step) {
                8 -> if (value == 42) { step++; return }
            }
            fail("@$step: decodeInt($value)")
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            when(step) {
                9 -> { checkDataDesc(descriptor); step++; return }
                10 -> { checkContainerDesc(descriptor); step++; return }
            }
            fail("@$step: endStructure($descriptor)")
        }

        fun done() {
            if (step != 11) fail("@$step: OUT FAIL")
        }
    }

    class Inp : AbstractDecoder() {
        var step = 0

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            when (step) {
                1 -> {
                    checkContainerDesc(descriptor); step++; return this
                }
                4 -> {
                    checkDataDesc(descriptor); step++; return this
                }
            }
            fail("@$step: beginStructure($descriptor)")
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            when (step) {
                2 -> {
                    checkContainerDesc(descriptor); step++; return 0
                }
                5 -> {
                    checkDataDesc(descriptor); step++; return 0
                }
                7 -> {
                    checkDataDesc(descriptor); step++; return 1
                }
                9 -> {
                    checkDataDesc(descriptor); step++; return -1
                }
                11 -> {
                    checkContainerDesc(descriptor); step++; return -1
                }
            }
            fail("@$step: decodeElementIndex($descriptor)")
        }

        override fun <T : Any?> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
            when (step) {
                0, 3 -> { step++; return deserializer.deserialize(this) }
            }
            fail("@$step: decodeSerializableValue()")
        }

        override fun decodeString(): String {
            when (step) {
                6 -> { step++; return "s1" }
            }
            fail("@$step: decodeString()")
        }

        override fun decodeInt(): Int {
            when (step) {
                8 -> { step++; return 42 }
            }
            fail("@$step: decodeInt()")
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            when(step) {
                10 -> { checkDataDesc(descriptor); step++; return }
                12 -> { checkContainerDesc(descriptor); step++; return }
            }
            fail("@$step: endStructure($descriptor)")
        }

        fun done() {
            if (step != 13) fail("@$step: INP FAIL")
        }
    }
}
