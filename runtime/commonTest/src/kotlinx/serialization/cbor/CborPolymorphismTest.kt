/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.test.assertSerializedToBinaryAndRestored
import kotlin.test.Test

@Serializable
sealed class A {
    @Serializable
    data class B(val b: String) : A()
}


class CborPolymorphismTest {
    val cbor = Cbor(context = SimplePolymorphicModule)

    @Test
    fun testSealedWithOneSubclass() {
        assertSerializedToBinaryAndRestored(
            A.B("bbb"),
            A.serializer(),
            cbor,
            hexResultToCheck = "9f781e6b6f746c696e782e73657269616c697a6174696f6e2e63626f722e412e42bf616263626262ffff"
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
