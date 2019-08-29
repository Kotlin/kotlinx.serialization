/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.test.assertSerializedToBinaryAndRestored
import kotlin.test.Test

@Serializable
abstract class SimpleHierarchy

@Serializable
data class IntInheritor(val i: Int, val s: String) : SimpleHierarchy()

@Serializable
data class StringInheritor(val s: String, val i: Int) : SimpleHierarchy()

@Serializable
data class PolyBox(@Polymorphic val boxed: SimpleHierarchy)

@Serializable
data class SealedBox(val boxed: List<SimpleSealed>)

class ProtobufPolymorphismTest {
    @Test
    fun testAbstract() {
        val ctx = SerializersModule {
            polymorphic<SimpleHierarchy> {
                IntInheritor::class with IntInheritor.serializer()
                StringInheritor::class with StringInheritor.serializer()
            }
        }
        val obj = PolyBox(StringInheritor("str", 133))
        assertSerializedToBinaryAndRestored(obj, PolyBox.serializer(), ProtoBuf(ctx))
    }

    @Test
    fun testSealed() {
        val obj = SealedBox(listOf(SimpleSealed.SubSealedB(33), SimpleSealed.SubSealedA("str")))
        assertSerializedToBinaryAndRestored(obj, SealedBox.serializer(), ProtoBuf)
    }
}
