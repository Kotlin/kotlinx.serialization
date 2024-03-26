/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlin.test.*


class CbrWriterTest {
    @Test
    fun writeSimpleClass() {
        assertEquals("bf616163737472ff", Cbor.encodeToHexString(Simple.serializer(), Simple("str")))
    }

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
            "bf637374726d48656c6c6f2c20776f726c64216169182a686e756c6c61626c65f6646c6973749f61616162ff636d6170bf01f502f4ff65696e6e6572bf6161636c6f6cff6a696e6e6572734c6973749fbf6161636b656bffff6a62797465537472696e6742cafe696279746541727261799f383521ffff",
            Cbor.encodeToHexString(TypesUmbrella.serializer(), test)
        )
    }

    @Test
    fun writeComplicatedClassDefLen() {
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
            Cbor { writeDefiniteLengths = true }.encodeToHexString(TypesUmbrella.serializer(), test)
        )
    }

    @Test
    fun writeManyNumbers() {
        val test = NumberTypesUmbrella(
            100500,
            Long.MAX_VALUE,
            42.0f,
            1235621356215.0,
            true,
            'a'
        )
        assertEquals(
            "bf63696e741a00018894646c6f6e671b7fffffffffffffff65666c6f6174fa4228000066646f75626c65fb4271fb0c5a2b700067626f6f6c65616ef564636861721861ff",
            Cbor.encodeToHexString(NumberTypesUmbrella.serializer(), test)
        )
    }

    @Test
    fun testWriteByteStringWhenNullable() {
        /* BF                         # map(*)
         *    6A                      # text(10)
         *       62797465537472696E67 # "byteString"
         *    44                      # bytes(4)
         *       01020304             # "\x01\x02\x03\x04"
         *    FF                      # primitive(*)
         */
        assertEquals(
            expected = "bf6a62797465537472696e674401020304ff",
            actual = Cbor.encodeToHexString(
                serializer = NullableByteString.serializer(),
                value = NullableByteString(byteString = byteArrayOf(1, 2, 3, 4))
            )
        )

        /* BF                         # map(*)
         *    6A                      # text(10)
         *       62797465537472696E67 # "byteString"
         *    40                      # bytes(0)
         *                            # ""
         *    FF                      # primitive(*)
         */
        assertEquals(
            expected = "bf6a62797465537472696e6740ff",
            actual = Cbor.encodeToHexString(
                serializer = NullableByteString.serializer(),
                value = NullableByteString(byteString = byteArrayOf())
            )
        )
    }

    @Test
    fun testWriteNullForNullableByteString() {
        /* BF                         # map(*)
         *    6A                      # text(10)
         *       62797465537472696E67 # "byteString"
         *    F6                      # primitive(22)
         *    FF                      # primitive(*)
         */
        assertEquals(
            expected = "bf6a62797465537472696e67f6ff",
            actual = Cbor.encodeToHexString(
                serializer = NullableByteString.serializer(),
                value = NullableByteString(byteString = null)
            )
        )
    }

    @Test
    fun testOmitNullForNullableByteString() {
        /* BF                         # map(*)
         *    FF                      # primitive(*)
         */
        assertEquals(
            expected = "bfff",
            actual = Cbor.encodeToHexString(
                serializer = NullableByteStringDefaultNull.serializer(),
                value = NullableByteStringDefaultNull(byteString = null)
            )
        )
    }

    @Test
    fun testOmitNullDefLenForNullableByteString() {
        /* A0                         # map(0)
         */
        assertEquals(
            expected = "a0",
            actual = Cbor { writeDefiniteLengths = true }.encodeToHexString(
                serializer = NullableByteStringDefaultNull.serializer(),
                value = NullableByteStringDefaultNull(byteString = null)
            )
        )
    }

    @Test
    fun testWriteCustomByteString() {
        assertEquals(
            expected = "bf617843112233ff",
            actual = Cbor.encodeToHexString(TypeWithCustomByteString(CustomByteString(0x11, 0x22, 0x33)))
        )
    }

    @Test
    fun testWriteNullableCustomByteString() {
        assertEquals(
            expected = "bf617843112233ff",
            actual = Cbor.encodeToHexString(TypeWithNullableCustomByteString(CustomByteString(0x11, 0x22, 0x33)))
        )
    }

    @Test
    fun testWriteNullCustomByteString() {
        assertEquals(
            expected = "bf6178f6ff",
            actual = Cbor.encodeToHexString(TypeWithNullableCustomByteString(null))
        )
    }

    @Test
    fun testWriteValueClassWithByteString() {
        assertEquals(
            expected = "43112233",
            actual = Cbor.encodeToHexString(ValueClassWithByteString(byteArrayOf(0x11, 0x22, 0x33)))
        )
    }

    @Test
    fun testWriteValueClassCustomByteString() {
        assertEquals(
            expected = "43112233",
            actual = Cbor.encodeToHexString(ValueClassWithCustomByteString(CustomByteString(0x11, 0x22, 0x33)))
        )
    }

    @Test
    fun testWriteValueClassWithUnlabeledByteString() {
        assertEquals(
            expected = "43112233",
            actual = Cbor.encodeToHexString(ValueClassWithUnlabeledByteString(ValueClassWithUnlabeledByteString.Inner(byteArrayOf(0x11, 0x22, 0x33))))
        )
    }
}
