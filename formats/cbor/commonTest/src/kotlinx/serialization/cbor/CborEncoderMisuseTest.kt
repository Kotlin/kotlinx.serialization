/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.cbor.internal.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.test.*

class CborEncoderMisuseTest {

    @Serializable(with = BadInStructureElementWrite.Serializer::class)
    private data class BadInStructureElementWrite(val x: Int) {
        object Serializer : KSerializer<BadInStructureElementWrite> {
            override val descriptor: SerialDescriptor =
                buildClassSerialDescriptor("BadInStructureElementWrite") {
                    element<Int>("x")
                }

            override fun serialize(encoder: Encoder, value: BadInStructureElementWrite) {
                val composite = encoder.beginStructure(descriptor)
                composite.encodeIntElement(descriptor, 0, value.x)

                // Illegal usage: inject an element without going through encodeElement(descriptor, index).
                (composite as CborEncoder).encodeCborElement(CborString("oops"))

                composite.endStructure(descriptor)
            }

            override fun deserialize(decoder: Decoder): BadInStructureElementWrite {
                val composite = decoder.beginStructure(descriptor)
                var x: Int? = null
                while (true) {
                    when (val index = composite.decodeElementIndex(descriptor)) {
                        CompositeDecoder.DECODE_DONE -> break
                        0 -> x = composite.decodeIntElement(descriptor, 0)
                        else -> throw SerializationException("Unexpected index $index")
                    }
                }
                composite.endStructure(descriptor)
                return BadInStructureElementWrite(x ?: throw SerializationException("Missing x"))
            }
        }
    }

    @Test
    fun testEncodeCborElementInsideStructureProducesMultipleRootItemsDefiniteLength() {
        val cbor = Cbor { useDefiniteLengthEncoding = true }

        val bytes = cbor.encodeToByteArray(BadInStructureElementWrite(1))
        assertEquals("a1617801646f6f7073", bytes.toHexString())

        val parser = CborParser(ByteArrayInput(bytes), verifyObjectTags = false)
        val reader = CborTreeReader(cbor.configuration, parser)

        val first = reader.read()
        assertEquals(CborMap(mapOf(CborString("x") to CborInt(1))), first)
        assertFalse(parser.isEof(), "Expected trailing bytes (second root item) after the first CBOR item")

        val second = reader.read()
        assertEquals(CborString("oops"), second)
        assertTrue(parser.isEof(), "Expected exactly two root CBOR items")
    }
}

