/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.test.*
import kotlin.test.*

@Suppress("RemoveExplicitTypeArguments") // This is exactly what's being tested
class SerializersLookupObjectTest {
    @Serializable(with = ObjectCustomObjectSerializer::class)
    object ObjectExternalObject

    object ObjectCustomObjectSerializer: KSerializer<ObjectExternalObject> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("tmp", StructureKind.OBJECT)

        override fun serialize(encoder: Encoder, value: ObjectExternalObject) {
            TODO()
        }

        override fun deserialize(decoder: Decoder): ObjectExternalObject {
            TODO()
        }
    }

    @Serializable(with = ObjectCustomClassSerializer::class)
    object ObjectExternalClass

    class ObjectCustomClassSerializer: KSerializer<ObjectExternalClass> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("tmp", StructureKind.OBJECT)

        override fun serialize(encoder: Encoder, value: ObjectExternalClass) {
            TODO()
        }

        override fun deserialize(decoder: Decoder): ObjectExternalClass {
            TODO()
        }
    }

    @Serializable
    object PlainObject

    @Test
    fun testPlainObject() {
        assertSame(PlainObject.serializer(), serializer<PlainObject>())
    }


    @Test
    fun testObjectExternalObject() {
        assertSame(ObjectCustomObjectSerializer, ObjectExternalObject.serializer())
        assertSame(ObjectCustomObjectSerializer, serializer<ObjectExternalObject>())
    }

    @Test
    fun testObjectExternalClass() {
        assertIs<ObjectCustomClassSerializer>(ObjectExternalClass.serializer())
        assertIs<ObjectCustomClassSerializer>(serializer<ObjectExternalClass>())
    }
}
