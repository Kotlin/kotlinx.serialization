/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.test.*
import kotlin.test.*

class SerializersLookupInterfaceTest {
    @Serializable(InterfaceExternalObjectSerializer::class)
    interface InterfaceExternalObject

    object InterfaceExternalObjectSerializer : KSerializer<InterfaceExternalObject> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("ignore", StructureKind.OBJECT)

        override fun serialize(encoder: Encoder, value: InterfaceExternalObject) =
            throw UnsupportedOperationException()

        override fun deserialize(decoder: Decoder): InterfaceExternalObject =
            throw UnsupportedOperationException()
    }

    @Serializable(InterfaceExternalClassSerializer::class)
    interface InterfaceExternalClass

    class InterfaceExternalClassSerializer : KSerializer<InterfaceExternalClass> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("ignore", StructureKind.OBJECT)

        override fun serialize(encoder: Encoder, value: InterfaceExternalClass) =
            throw UnsupportedOperationException()

        override fun deserialize(decoder: Decoder): InterfaceExternalClass =
            throw UnsupportedOperationException()
    }


    @Test
    fun testInterfaceExternalObject() = noJsLegacy {
        assertSame(InterfaceExternalObjectSerializer, serializer())
    }

    @Test
    fun testInterfaceExternalClass() = noJsLegacy {
        assertIs<InterfaceExternalClassSerializer>(serializer<InterfaceExternalClass>())
    }
}
