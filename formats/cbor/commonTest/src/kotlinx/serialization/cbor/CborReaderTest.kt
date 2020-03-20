/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.io.*
import kotlinx.serialization.*
import kotlin.test.*

class CborReaderTest {
    private fun withDecoder(input: String, block: Cbor.CborDecoder.() -> Unit) {
        val bytes = HexConverter.parseHexBinary(input.toUpperCase())
        Cbor.CborDecoder(ByteArrayInput(bytes)).block()
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
        assertEquals(Simple("str"), Cbor.loads(Simple.serializer(), "bf616163737472ff"))
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
            listOf(Simple("kek"))
        )
        // with maps & lists of indefinite length
        assertEquals(test, Cbor.loads(TypesUmbrella.serializer(),
            "bf637374726d48656c6c6f2c20776f726c64216169182a686e756c6c61626c65f6646c6973749f61616162ff636d6170bf01f502f4ff65696e6e6572bf6161636c6f6cff6a696e6e6572734c6973749fbf6161636b656bffffff"
        ))
        // with maps & lists of definite length
        assertEquals(test, Cbor.loads(TypesUmbrella.serializer(),
            "a7646c6973748261616162686e756c6c61626c65f6636d6170a202f401f56169182a6a696e6e6572734c69737481a16161636b656b637374726d48656c6c6f2c20776f726c642165696e6e6572a16161636c6f6c"
        ))
    }
}
