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
        assertSerializedToBinaryAndRestored(obj, PolyBox.serializer(), ProtoBuf(context = SimplePolymorphicModule))
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
}
