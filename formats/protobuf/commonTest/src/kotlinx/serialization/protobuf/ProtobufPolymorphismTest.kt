/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

class ProtobufPolymorphismTest {
    @Test
    fun testAbstract() {
        val obj = PolyBox(SimpleStringInheritor("str", 133))
        assertSerializedToBinaryAndRestored(obj, PolyBox.serializer(),
            ProtoBuf { encodeDefaults = true; serializersModule = SimplePolymorphicModule })
    }

    @Test
    fun testSealed() {
        val obj = SealedBox(
            listOf(
                SimpleSealed.SubSealedB(33),
                SimpleSealed.SubSealedA("str")
            )
        )
        assertSerializedToBinaryAndRestored(obj, SealedBox.serializer(), ProtoBuf)
    }

    @Serializable
    sealed class Single {
        @Serializable
        data class Impl(val i: Int) : Single()
    }

    @Test
    fun testSingleSealedClass() {
        val expected =
            "0a436b6f746c696e782e73657269616c697a6174696f6e2e70726f746f6275662e50726f746f627566506f6c796d6f72706869736d546573742e53696e676c652e496d706c1202082a"
        assertEquals(expected, ProtoBuf.encodeToHexString(Single.serializer(), Single.Impl(42)))
        assertEquals(Single.Impl(42), ProtoBuf.decodeFromHexString(Single.serializer(), expected))
    }
}
