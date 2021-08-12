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
    @Serializable(with = ObjectExternalObjectSerializer::class)
    object ObjectExternalObject

    @Serializer(forClass = ObjectExternalObject::class)
    object ObjectExternalObjectSerializer {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("tmp", StructureKind.OBJECT)

        override fun serialize(encoder: Encoder, value: ObjectExternalObject) {
            TODO()
        }

        override fun deserialize(decoder: Decoder): ObjectExternalObject {
            TODO()
        }
    }

    @Serializable(with = ObjectExternalClassSerializer::class)
    object ObjectExternalClass

    @Serializer(forClass = ObjectExternalClass::class)
    class ObjectExternalClassSerializer {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("tmp", StructureKind.OBJECT)

        override fun serialize(encoder: Encoder, value: ObjectExternalClass) {
            TODO()
        }

        override fun deserialize(decoder: Decoder): ObjectExternalClass {
            TODO()
        }
    }

    @Polymorphic
    object ObjectPolymorphic

    @Serializable
    object PlainObject

    @Test
    fun testPlainObject() {
        if (!isJsLegacy()) {
            assertSame(PlainObject.serializer(), serializer<PlainObject>())
        }
    }


    @Test
    fun testObjectExternalObject() {
        assertSame(ObjectExternalObjectSerializer, ObjectExternalObject.serializer())
        if (!isJsLegacy()) {
            assertSame(ObjectExternalObjectSerializer, serializer<ObjectExternalObject>())
        }
    }

    @Test
    fun testObjectExternalClass() {
        assertIs<ObjectExternalClassSerializer>(ObjectExternalClass.serializer())

        if (!isJsLegacy()) {
            assertIs<ObjectExternalClassSerializer>(serializer<ObjectExternalClass>())
        }
    }

    @Test
    fun testEnumPolymorphic() {
        if (isJvm()) {
            assertEquals(
                PolymorphicSerializer(ObjectPolymorphic::class).descriptor,
                serializer<ObjectPolymorphic>().descriptor
            )
        } else {
            // FIXME serializer<PolymorphicObject> is broken for K/JS and K/Native. Remove `assertFails` after fix
            assertFails { serializer<ObjectPolymorphic>() }
        }

    }
}
