/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlin.test.*

class CborPolymorphismTest {
    @Serializable
    sealed class A {
        @Serializable
        data class B(val b: String) : A()
    }

    val cbor = Cbor { serializersModule = SimplePolymorphicModule }

    @Test
    fun testSealedWithOneSubclass() {
        val original = A.B("bbb")
        val hexResultToCheck =
            "9f78336b6f746c696e782e73657269616c697a6174696f6e2e63626f722e43626f72506f6c796d6f72706869736d546573742e412e42bf616263626262ffff"
        assertSerializedToBinaryAndRestored(
            original,
            A.serializer(),
            cbor,
            hexResultToCheck = hexResultToCheck
        )

        val struct = cbor.encodeToCbor(A.serializer(), original)
        assertEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hexResultToCheck))
        assertEquals(hexResultToCheck, cbor.encodeToHexString(struct))
        assertEquals(original, cbor.decodeFromCbor(A.serializer(), struct))
    }

    @Test
    fun testSealedWithMultipleSubclasses() {
        val obj = SealedBox(
            listOf(
                SimpleSealed.SubSealedB(33),
                SimpleSealed.SubSealedA("str")
            )
        )
        assertSerializedToBinaryAndRestored(obj, SealedBox.serializer(), cbor)

        val struct = cbor.encodeToCbor(SealedBox.serializer(), obj)
        assertEquals(obj, cbor.decodeFromCbor(SealedBox.serializer(), struct))
    }

    @Test
    fun testOpenPolymorphism() {
        val obj = PolyBox(
            SimpleStringInheritor(
                "str",
                133
            )
        )
        assertSerializedToBinaryAndRestored(obj, PolyBox.serializer(), cbor)


        val struct = cbor.encodeToCbor(PolyBox.serializer(), obj)
        assertEquals(obj, cbor.decodeFromCbor(PolyBox.serializer(), struct))
    }
}
