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
        assertSerializedToBinaryAndRestored(
            A.B("bbb"),
            A.serializer(),
            cbor,
            hexResultToCheck = "9f78336b6f746c696e782e73657269616c697a6174696f6e2e63626f722e43626f72506f6c796d6f72706869736d546573742e412e42bf616263626262ffff"
        )
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
    }
}
