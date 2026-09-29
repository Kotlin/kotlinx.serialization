/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlin.test.*


class CborDefiniteLengthTest {
    @Test
    fun writeComplicatedClass() {
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
        assertEquals(
            "a9637374726d48656c6c6f2c20776f726c64216169182a686e756c6c61626c65f6646c6973748261616162636d6170a201f502f465696e6e6572a16161636c6f6c6a696e6e6572734c69737481a16161636b656b6a62797465537472696e6742cafe6962797465417272617982383521",
            Cbor { useDefiniteLengthEncoding = true }.encodeToHexString(TypesUmbrella.serializer(), test)
        )
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun readCollections() {
        val cbor = Cbor { useDefiniteLengthEncoding = true }
        val arrayEncoded = cbor.encodeToByteArray(byteArrayOf(1, 2, 3, 4, 5))
        val fpArrayEncoded = cbor.encodeToByteArray(floatArrayOf(1.0f, 2.0f, 3.0f))
        val charArrayEncoded = cbor.encodeToByteArray(charArrayOf('a', 'b'))
        val listEncoded = cbor.encodeToByteArray(arrayListOf("a", "b", "c"))
        val mapEncoded = cbor.encodeToByteArray(mapOf("a" to 1, "b" to 2, "c" to 3))

        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5), cbor.decodeFromByteArray(arrayEncoded))
        assertContentEquals(shortArrayOf(1, 2, 3, 4, 5), cbor.decodeFromByteArray(arrayEncoded))
        assertContentEquals(intArrayOf(1, 2, 3, 4, 5), cbor.decodeFromByteArray(arrayEncoded))
        assertContentEquals(longArrayOf(1, 2, 3, 4, 5), cbor.decodeFromByteArray(arrayEncoded))
        assertContentEquals(ubyteArrayOf(1u, 2u, 3u, 4u, 5u), cbor.decodeFromByteArray(arrayEncoded))
        assertContentEquals(ushortArrayOf(1u, 2u, 3u, 4u, 5u), cbor.decodeFromByteArray(arrayEncoded))
        assertContentEquals(uintArrayOf(1u, 2u, 3u, 4u, 5u), cbor.decodeFromByteArray(arrayEncoded))
        assertContentEquals(ulongArrayOf(1u, 2u, 3u, 4u, 5u), cbor.decodeFromByteArray(arrayEncoded))

        assertContentEquals(arrayOf(1, 2, 3, 4, 5), cbor.decodeFromByteArray(arrayEncoded))

        assertContentEquals(floatArrayOf(1.0f, 2.0f, 3.0f), cbor.decodeFromByteArray(fpArrayEncoded))
        assertContentEquals(doubleArrayOf(1.0, 2.0, 3.0), cbor.decodeFromByteArray(fpArrayEncoded))

        assertContentEquals(charArrayOf('a', 'b'), cbor.decodeFromByteArray(charArrayEncoded))

        assertEquals(listOf("a", "b", "c"), cbor.decodeFromByteArray(listEncoded))
        assertEquals(setOf("a", "b", "c"), cbor.decodeFromByteArray(listEncoded))
        assertEquals(linkedSetOf("a", "b", "c"), cbor.decodeFromByteArray<LinkedHashSet<String>>(listEncoded))
        assertEquals(mapOf("a" to 1, "b" to 2, "c" to 3), cbor.decodeFromByteArray(mapEncoded))
        assertEquals(linkedMapOf("a" to 1, "b" to 2, "c" to 3), cbor.decodeFromByteArray(mapEncoded))
    }
}
