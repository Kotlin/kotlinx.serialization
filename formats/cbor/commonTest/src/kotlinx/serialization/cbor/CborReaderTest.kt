/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.cbor.internal.*
import kotlin.test.*

class CborReaderTest {
    private fun withDecoder(input: String, block: CborDecoder.() -> Unit) {
        val bytes = HexConverter.parseHexBinary(input.toUpperCase())
        CborDecoder(ByteArrayInput(bytes)).block()
    }

    @Test
    fun testDecodeIntegers() {
        withDecoder("0C1903E8") {
            assertEquals(12L, nextNumber())
            assertEquals(1000L, nextNumber())
        }
        withDecoder("203903e7") {
            assertEquals(-1L, nextNumber())
            assertEquals(-1000L, nextNumber())
        }
    }

    @Test
    fun testDecodeStrings() {
        withDecoder("6568656C6C6F") {
            assertEquals("hello", nextString())
        }
        withDecoder("7828737472696E672074686174206973206C6F6E676572207468616E2032332063686172616374657273") {
            assertEquals("string that is longer than 23 characters", nextString())
        }
    }

    @Test
    fun testDecodeDoubles() {
        withDecoder("fb7e37e43c8800759c") {
            assertEquals(1e+300, nextDouble())
        }
        withDecoder("fa47c35000") {
            assertEquals(100000.0f, nextFloat())
        }
    }

    @Test
    fun testDecodeSimpleObject() {
        assertEquals(Simple("str"), Cbor.decodeFromHexString(Simple.serializer(), "bf616163737472ff"))
    }

    @Test
    fun testDecodeComplicatedObject() {
        val test = TypesUmbrella(
            "Hello, world!",
            42,
            null,
            listOf("a", "b"),
            mapOf(1 to true, 2 to false),
            Simple("lol"),
            listOf(Simple("kek")),
            HexConverter.parseHexBinary("cafe"),
            HexConverter.parseHexBinary("cafe")
        )
        // with maps, lists & strings of indefinite length
        assertEquals(test, Cbor.decodeFromHexString(
            TypesUmbrella.serializer(),
            "bf637374726d48656c6c6f2c20776f726c64216169182a686e756c6c61626c65f6646c6973749f61616162ff636d6170bf01f502f4ff65696e6e6572bf6161636c6f6cff6a696e6e6572734c6973749fbf6161636b656bffff6a62797465537472696e675f42cafeff696279746541727261799f383521ffff"
        )
        )
        // with maps, lists & strings of definite length
        assertEquals(test, Cbor.decodeFromHexString(
            TypesUmbrella.serializer(),
            "a9646c6973748261616162686e756c6c61626c65f6636d6170a202f401f56169182a6a696e6e6572734c69737481a16161636b656b637374726d48656c6c6f2c20776f726c642165696e6e6572a16161636c6f6c6a62797465537472696e6742cafe6962797465417272617982383521"
        )
        )
    }

    /**
     * Test using example shown on page 11 of [RFC 7049 2.2.2](https://tools.ietf.org/html/rfc7049#section-2.2.2):
     *
     * ```
     * 0b010_11111 0b010_00100 0xaabbccdd 0b010_00011 0xeeff99 0b111_11111
     *
     * 5F              -- Start indefinite-length byte string
     *    44           -- Byte string of length 4
     *       aabbccdd  -- Bytes content
     *    43           -- Byte string of length 3
     *       eeff99    -- Bytes content
     *    FF           -- "break"
     *
     * After decoding, this results in a single byte string with seven
     * bytes: 0xaabbccddeeff99.
     * ```
     */
    @Test
    fun testRfc7049IndefiniteByteStringExample() {
        withDecoder(input = "5F44aabbccdd43eeff99FF") {
            assertEquals(
                expected = "aabbccddeeff99",
                actual = HexConverter.printHexBinary(nextByteString(), lowerCase = true)
            )
        }
    }
}
