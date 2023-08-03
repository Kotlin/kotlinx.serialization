/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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

    @Serializable
    class ParametrizedOfNullable<T>(val a: T)

    class ContextualType(val i: Int)

    @Serializer(forClass = ContextualType::class)
    object ContextualSerializer

    @Serializable
    class ContextualHolder(@Contextual val contextual: ContextualType)

    @Test
    fun testCompiled() {
        assertSame<KSerializer<*>>(Object.serializer(), serializer(Object::class, emptyList(), false))
        assertSame<KSerializer<*>>(SealedParent.serializer(), serializer(SealedParent::class, emptyList(), false))
        assertSame<KSerializer<*>>(
            SealedParent.Child.serializer(),
            serializer(SealedParent.Child::class, emptyList(), false)
        )

        assertSame<KSerializer<*>>(Abstract.serializer(), serializer(Abstract::class, emptyList(), false))
        assertSame<KSerializer<*>>(SerializableEnum.serializer(), serializer(SerializableEnum::class, emptyList(), false))
    }

    @Test
    fun testBuiltIn() {
        assertSame<KSerializer<*>>(Int.serializer(), serializer(Int::class, emptyList(), false))
    }

    @Test
    fun testCustom() {
        val m = SerializersModule { }
        assertSame<KSerializer<*>>(CustomSerializer, m.serializer(WithCustomSerializer::class, emptyList(), false))
    }

    @Test
    fun testParametrized() {
        val serializer = serializer(Parametrized::class, listOf(Int.serializer()), false)
        assertEquals<KClass<*>>(Parametrized.serializer(Int.serializer())::class, serializer::class)
        assertEquals(PrimitiveKind.INT, serializer.descriptor.getElementDescriptor(0).kind)

        val mapSerializer = serializer(Map::class, listOf(String.serializer(), Int.serializer()), false)
        assertIs<MapLikeSerializer<*, *, *, *>>(mapSerializer)
        assertEquals(PrimitiveKind.STRING, mapSerializer.descriptor.getElementDescriptor(0).kind)
        assertEquals(PrimitiveKind.INT, mapSerializer.descriptor.getElementDescriptor(1).kind)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testNothingAndParameterizedOfNothing() {
        assertEquals(NothingSerializer, Nothing::class.serializer())
        //assertEquals(NothingSerializer, serializer<Nothing>()) // prohibited by compiler
        assertEquals(NothingSerializer, serializer(Nothing::class, emptyList(), false) as KSerializer<Nothing>)
        //assertEquals(NullableSerializer(NothingSerializer), serializer<Nothing?>()) // prohibited by compiler
        assertEquals(
            NullableSerializer(NothingSerializer),
            serializer(Nothing::class, emptyList(), true) as KSerializer<Nothing?>
        )

        val parameterizedNothingSerializer = serializer<Parametrized<Nothing>>()
        val nothingDescriptor = parameterizedNothingSerializer.descriptor.getElementDescriptor(0)
        assertEquals(NothingSerialDescriptor, nothingDescriptor)

        val parameterizedNullableNothingSerializer = serializer<ParametrizedOfNullable<Nothing?>>()
        val nullableNothingDescriptor = parameterizedNullableNothingSerializer.descriptor.getElementDescriptor(0)
        assertEquals(SerialDescriptorForNullable(NothingSerialDescriptor), nullableNothingDescriptor)
    }

    @Test
    fun testUnsupportedArray() {
        assertFails {
            serializer(Array::class, listOf(Int.serializer()), false)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testContextual() {
        val m = SerializersModule {
            contextual<ContextualType>(ContextualSerializer)
            contextual(ContextualGenericsTest.ThirdPartyBox::class) { args -> ContextualGenericsTest.ThirdPartyBoxSerializer(args[0]) }
        }

        val contextualSerializer = m.serializer(ContextualType::class, emptyList(), false)
        assertSame<KSerializer<*>>(ContextualSerializer, contextualSerializer)

        val boxSerializer = m.serializer(ContextualGenericsTest.ThirdPartyBox::class, listOf(Int.serializer()), false)
        assertIs<ContextualGenericsTest.ThirdPartyBoxSerializer<Int>>(boxSerializer)
        assertEquals(PrimitiveKind.INT, boxSerializer.descriptor.getElementDescriptor(0).kind)

        val holderSerializer = m.serializer(ContextualHolder::class, emptyList(), false)
        assertSame<KSerializer<*>>(ContextualHolder.serializer(), holderSerializer)
    }

}

