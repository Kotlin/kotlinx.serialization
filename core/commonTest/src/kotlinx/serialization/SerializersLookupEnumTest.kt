/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.EnumSerializer
import kotlinx.serialization.test.*
import kotlin.test.*

// This is unimplemented functionality that should be
@Suppress("RemoveExplicitTypeArguments") // This is exactly what's being tested
class SerializersLookupEnumTest {
    @Serializable(with = EnumExternalObjectSerializer::class)
    enum class EnumExternalObject

    object EnumExternalObjectSerializer: KSerializer<EnumExternalObject> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("tmp", SerialKind.ENUM)

        override fun serialize(encoder: Encoder, value: EnumExternalObject) {
            TODO()
        }

        override fun deserialize(decoder: Decoder): EnumExternalObject {
            TODO()
        }
    }

    @Serializable(with = EnumCustomClassSerializer::class)
    enum class EnumExternalClass

    class EnumCustomClassSerializer: KSerializer<EnumExternalClass> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("tmp", SerialKind.ENUM)

        override fun serialize(encoder: Encoder, value: EnumExternalClass) {
            TODO()
        }

        override fun deserialize(decoder: Decoder): EnumExternalClass {
            TODO()
        }
    }

    @Serializable
    enum class PlainEnum

    @Serializable
    enum class SerializableEnum { C, D }

    @Serializable
    enum class SerializableMarkedEnum { C, @SerialName("NotD") D }

    @Test
    fun testPlainEnum() {
        assertSame(PlainEnum.serializer(), serializer<PlainEnum>())

        if (!isJs()) {
            assertIs<EnumSerializer<PlainEnum>>(serializer<PlainEnum>())
        }
    }

    @Test
    fun testSerializableEnumSerializer() {
        assertIs<EnumSerializer<SerializableEnum>>(SerializableEnum.serializer())

        assertSame(SerializableEnum.serializer(), serializer<SerializableEnum>())
    }

    @Test
    fun testSerializableMarkedEnumSerializer() {
        assertIs<EnumSerializer<SerializableMarkedEnum>>(SerializableMarkedEnum.serializer())

        assertSame(SerializableMarkedEnum.serializer(), serializer<SerializableMarkedEnum>())
    }

    @Test
    fun testEnumExternalObject() {
        assertSame(EnumExternalObjectSerializer, EnumExternalObject.serializer())
        assertSame(EnumExternalObjectSerializer, serializer<EnumExternalObject>())
    }

    @Test
    fun testEnumExternalClass() {
        assertIs<EnumCustomClassSerializer>(EnumExternalClass.serializer())
        assertIs<EnumCustomClassSerializer>(serializer<EnumExternalClass>())
    }
}
