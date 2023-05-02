/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:UseContextualSerialization(SerializersModuleTest.FileContextualType::class)

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
import kotlin.reflect.*
import kotlin.test.*

class SerializersModuleTest {
    @Serializable
    object Object

    @Serializable
    sealed class SealedParent {
        @Serializable
        data class Child(val i: Int) : SealedParent()
    }

    @Serializable
    abstract class Abstract

    @Serializable
    enum class SerializableEnum { A, B }

    @Serializable(CustomSerializer::class)
    class WithCustomSerializer(val i: Int)

    @Serializer(forClass = WithCustomSerializer::class)
    object CustomSerializer

    @Serializable
    class Parametrized<T : Any>(val a: T)

    class ContextualType(val i: Int)

    @Serializer(forClass = ContextualType::class)
    object ContextualSerializer

    class FileContextualType(val i: Int)

    @Serializer(forClass = FileContextualType::class)
    object FileContextualSerializer

    @Serializable
    class ContextualHolder(@Contextual val contextual: ContextualType, val fileContextual: FileContextualType)

    @Test
    fun testCompiled() = noJsLegacy {
        val m = SerializersModule { }

        assertSame<KSerializer<*>>(Object.serializer(), m.serializer(Object::class, emptyList(), false))
        assertSame<KSerializer<*>>(SealedParent.serializer(), m.serializer(SealedParent::class, emptyList(), false))
        assertSame<KSerializer<*>>(
            SealedParent.Child.serializer(),
            m.serializer(SealedParent.Child::class, emptyList(), false)
        )

        assertSame<KSerializer<*>>(Abstract.serializer(), m.serializer(Abstract::class, emptyList(), false))
        assertSame<KSerializer<*>>(SerializableEnum.serializer(), m.serializer(SerializableEnum::class, emptyList(), false))
    }

    @Test
    fun testBuiltIn() {
        val m = SerializersModule { }
        assertSame<KSerializer<*>>(Int.serializer(), m.serializer(Int::class, emptyList(), false))
    }

    @Test
    fun testCustom() {
        val m = SerializersModule { }
        assertSame<KSerializer<*>>(CustomSerializer, m.serializer(WithCustomSerializer::class, emptyList(), false))
    }

    @Test
    fun testParametrized() {
        val m = SerializersModule { }

        val serializer = m.serializer(Parametrized::class, listOf(Int.serializer()), false)
        assertSame<KClass<*>>(Parametrized.serializer(Int.serializer())::class, serializer::class)
        assertEquals(PrimitiveKind.INT, serializer.descriptor.getElementDescriptor(0).kind)

        val mapSerializer = m.serializer(Map::class, listOf(String.serializer(), Int.serializer()), false)
        assertIs<MapLikeSerializer<*, *, *, *>>(mapSerializer)
        assertEquals(PrimitiveKind.STRING, mapSerializer.descriptor.getElementDescriptor(0).kind)
        assertEquals(PrimitiveKind.INT, mapSerializer.descriptor.getElementDescriptor(1).kind)
    }

    @Test
    fun testUnsupportedArray() {
        val m = SerializersModule { }

        assertFails {
            m.serializer(Array::class, listOf(Int.serializer()), false)
        }
    }

    @Test
    fun testContextual() {
        val m = SerializersModule {
            contextual<FileContextualType>(FileContextualSerializer)
            contextual<ContextualType>(ContextualSerializer)
        }

        val contextualSerializer = m.serializer(ContextualType::class, emptyList(), false)
        assertSame<KSerializer<*>>(ContextualSerializer, contextualSerializer)

        val fileContextualSerializer = m.serializer(FileContextualType::class, emptyList(), false)
        assertSame<KSerializer<*>>(FileContextualSerializer, fileContextualSerializer)

        val holderSerializer = m.serializer(ContextualHolder::class, emptyList(), false)
        assertSame<KSerializer<*>>(ContextualHolder.serializer(), holderSerializer)
    }

}

