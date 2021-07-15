/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.test.*
import kotlin.test.*

// This is unimplemented functionality that should be
@Suppress("RemoveExplicitTypeArguments") // This is exactly what's being tested
class SerializersLookupEnumTest {
    @Serializable(with = EnumExternalObjectSerializer::class)
    enum class EnumExternalObject

    @Serializer(forClass = EnumExternalObject::class)
    object EnumExternalObjectSerializer {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("tmp", SerialKind.ENUM)

        override fun serialize(encoder: Encoder, value: EnumExternalObject) {
            TODO()
        }

        override fun deserialize(decoder: Decoder): EnumExternalObject {
            TODO()
        }
    }

    @Serializable(with = EnumExternalClassSerializer::class)
    enum class EnumExternalClass

    @Serializer(forClass = EnumExternalClass::class)
    class EnumExternalClassSerializer {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("tmp", SerialKind.ENUM)

        override fun serialize(encoder: Encoder, value: EnumExternalClass) {
            TODO()
        }

        override fun deserialize(decoder: Decoder): EnumExternalClass {
            TODO()
        }
    }

    @Polymorphic
    enum class EnumPolymorphic

    @Serializable
    enum class PlainEnum

    @Test
    fun testPlainEnum() {
        assertEquals(PlainEnum.serializer(), serializer<PlainEnum>())
    }

    @Test
    fun testEnumExternalObject() {
        assertSame(EnumExternalObjectSerializer, EnumExternalObject.serializer())
        assertSame(EnumExternalObjectSerializer, serializer<EnumExternalObject>())
    }

    @Test
    fun testEnumExternalClass() {
        assertIs<EnumExternalClassSerializer>(EnumExternalClass.serializer())

        if (isJvm()) {
            assertIs<EnumExternalClassSerializer>(serializer<EnumExternalClass>())
        } else if (isJsIr() || isNative()) {
            // FIXME serializer<EnumWithClassSerializer> is broken for K/JS and K/Native. Remove `assertFails` after fix
            assertFails { serializer<EnumExternalClass>() }
        }
    }

    @Test
    fun testEnumPolymorphic() {
        if (isJvm()) {
            assertEquals(
                PolymorphicSerializer(EnumPolymorphic::class).descriptor,
                serializer<EnumPolymorphic>().descriptor
            )
        } else {
            // FIXME serializer<PolymorphicEnum> is broken for K/JS and K/Native. Remove `assertFails` after fix
            assertFails { serializer<EnumPolymorphic>() }
        }
    }
}
