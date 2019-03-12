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
data class Container(
        val data: Data
)

class SerializeRecTest {
    @Test
    fun testRec() {
        val out = Out()
        out.encode(Container::class.serializer(), Container(Data("s1", 42)))
        out.done()

        val inp = Inp()
        inp.decode(Container::class.serializer())
        inp.done()
    }

    companion object {
        fun fail(msg: String): Nothing = throw RuntimeException(msg)

        fun checkContainerDesc(desc: SerialDescriptor) {
            if (desc.name != "kotlinx.serialization.Container") fail("checkContainerDesc name $desc")
            if (desc.getElementName(0) != "data") fail("checkContainerDesc $desc")
        }

        fun checkDataDesc(desc: SerialDescriptor) {
            if (desc.name != "kotlinx.serialization.Data") fail("checkDataDesc name $desc")
            if (desc.getElementName(0) != "value1") fail("checkDataDesc.0 $desc")
            if (desc.getElementName(1) != "value2") fail("checkDataDesc.1 $desc")
        }
    }

    class Out() : ElementValueEncoder() {
        var step = 0

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
            when(step) {
                0 -> { checkContainerDesc(desc); step++; return this }
                3 -> { checkDataDesc(desc); step++; return this }
            }
            fail("@$step: beginStructure($desc)")
        }

        override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
            when (step) {
                1 -> { checkContainerDesc(desc); if (index == 0) { step++; return true } }
                4 -> { checkDataDesc(desc); if (index == 0) { step++; return true } }
                6 -> { checkDataDesc(desc); if (index == 1) { step++; return true } }
            }
            fail("@$step: encodeElement($desc, $index)")
        }

        override fun <T : Any?> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
            when (step) {
                2 -> { step++; serializer.serialize(this, value); return }
            }
            fail("@$step: encodeSerializableValue($value)")
        }

        override fun encodeString(value: String) {
            when (step) {
                5 -> if (value == "s1") { step++; return }
            }
            fail("@$step: encodeString($value)")
        }

        override fun encodeInt(value: Int) {
            when (step) {
                7 -> if (value == 42) { step++; return }
            }
            fail("@$step: decodeInt($value)")
        }

        override fun endStructure(desc: SerialDescriptor) {
            when(step) {
                8 -> { checkDataDesc(desc); step++; return }
                9 -> { checkContainerDesc(desc); step++; return }
            }
            fail("@$step: endStructure($desc)")
        }

        fun done() {
            if (step != 10) fail("@$step: OUT FAIL")
        }
    }

    class Inp() : ElementValueDecoder() {
        var step = 0

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            when(step) {
                0 -> { checkContainerDesc(desc); step++; return this }
                3 -> { checkDataDesc(desc); step++; return this }
            }
            fail("@$step: beginStructure($desc)")
        }

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            when (step) {
                1 -> { checkContainerDesc(desc); step++; return 0 }
                4 -> { checkDataDesc(desc); step++; return 0 }
                6 -> { checkDataDesc(desc); step++; return 1 }
                8 -> { checkDataDesc(desc); step++; return -1 }
                10 -> { checkContainerDesc(desc); step++; return -1 }
            }
            fail("@$step: decodeElementIndex($desc)")
        }

        override fun <T : Any?> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
            when (step) {
                2 -> { step++; return deserializer.deserialize(this) }
            }
            fail("@$step: decodeSerializableValue()")
        }

        override fun decodeString(): String {
            when (step) {
                5 -> { step++; return "s1" }
            }
            fail("@$step: decodeString()")
        }

        override fun decodeInt(): Int {
            when (step) {
                7 -> { step++; return 42 }
            }
            fail("@$step: decodeInt()")
        }

        override fun endStructure(desc: SerialDescriptor) {
            when(step) {
                9 -> { checkDataDesc(desc); step++; return }
                11 -> { checkContainerDesc(desc); step++; return }
            }
            fail("@$step: endStructure($desc)")
        }

        fun done() {
            if (step != 12) fail("@$step: INP FAIL")
        }
    }
}
