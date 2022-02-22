/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UNCHECKED_CAST")

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.reflect.*
import kotlin.test.*

class ModuleBuildersTest {
    @Serializable
    class A(val i: Int)

    @Serializable
    class B(val b: String)

    @Serializer(forClass = A::class)
    object ASerializer : KSerializer<A>

    @Serializer(forClass = B::class)
    object BSerializer : KSerializer<B>

    private fun SerializersModule.assertModuleHas(aSerializer: Boolean = false, bSerializer: Boolean = false) {
        with(this) {
            assertSame(if (aSerializer) ASerializer else null, getContextual(A::class))
            assertSame(if (bSerializer) BSerializer else null, getContextual(B::class))
        }
    }

    @Test
    fun testSingletonModule() {
        val module = serializersModuleOf(A::class, ASerializer)
        module.assertModuleHas(
            aSerializer = true,
            bSerializer = false
        )
    }

    @Test
    fun testMapModule() {
        val module1 = serializersModuleOf(BSerializer)
        module1.assertModuleHas(
            aSerializer = false,
            bSerializer = true
        )

        SerializersModule {
            contextual(ASerializer)
            contextual(BSerializer)
        }.assertModuleHas(
            aSerializer = true,
            bSerializer = true
        )

        (module1 + serializersModuleOf(A::class, ASerializer)).assertModuleHas(
            aSerializer = true,
            bSerializer = true
        )
    }

    @Test
    fun testCompositeModule() {
        val moduleA = serializersModuleOf(ASerializer)
        val moduleB = serializersModuleOf(BSerializer)

        (moduleA + moduleB).assertModuleHas(
            aSerializer = true,
            bSerializer = true
        )

        var composite = SerializersModule { }
        composite.assertModuleHas(
            aSerializer = false,
            bSerializer = false
        )
        composite += moduleA
        composite.assertModuleHas(
            aSerializer = true,
            bSerializer = false
        )
        composite += moduleB
        composite.assertModuleHas(
            aSerializer = true,
            bSerializer = true
        )
    }

    @Test
    fun testDSL() {
        val module = SerializersModule {
            contextual(A::class, ASerializer)
        }
        module.assertModuleHas(aSerializer = true, bSerializer = false)
    }

    @Test
    fun testPolymorphicDSL() {
        val module1 = SerializersModule {
            polymorphic(PolyBase::class, PolyBase.serializer()) {
                subclass(PolyDerived.serializer())
            }
            polymorphic(Any::class, baseSerializer = null) {
                subclass(PolyBase.serializer())
                subclass(PolyDerived.serializer())
            }
        }

        val module2 = SerializersModule {
            polymorphic(Any::class) {
                subclass(PolyBase::class)
                subclass(PolyDerived.serializer())
            }

            polymorphic(PolyBase::class) {
                subclass(PolyBase.serializer())
                subclass(PolyDerived::class)
            }
        }

        val base = PolyBase(10)
        val derived = PolyDerived("foo")

        listOf(module1, module2).forEachIndexed { index, module ->
            fun <Base : Any, T : Base> assertPoly(serializer: KSerializer<T>, base: KClass<Base>, obj: T) =
                assertEquals(
                    serializer,
                    module.getPolymorphic(base, obj),
                    "No serializer for ${obj::class} with base $base in module ${index + 1}:"
                )

            assertPoly(PolyBase.serializer(), PolyBase::class, base)
            assertPoly(PolyDerived.serializer(), PolyBase::class, derived)
            assertPoly(PolyBase.serializer(), Any::class, base)
            assertPoly(PolyDerived.serializer(), Any::class, derived)
        }

    }

    @Test
    fun testOverwriteSerializer() {
        val moduleA = SerializersModule {
            contextual(A::class, ASerializer)
            assertFailsWith<IllegalArgumentException> {
                contextual(A::class, object : KSerializer<A> by A.serializer() {})
            }
        }
        moduleA.assertModuleHas(aSerializer = true, bSerializer = false)
    }

    @Test
    fun testOverwriteIsRightBiased() {
        val incorrect = serializersModuleOf(A::class as KClass<Any>, BSerializer as KSerializer<Any>)
        val correct = serializersModuleOf(ASerializer)
        correct.assertModuleHas(aSerializer = true, bSerializer = false)
        val sum = incorrect overwriteWith correct
        sum.assertModuleHas(aSerializer = true, bSerializer = false)
    }

    @Test
    fun testPlusThrowsExceptionOnDuplication() {
        val incorrect = serializersModuleOf(A::class as KClass<Any>, BSerializer as KSerializer<Any>)
        val correct = serializersModuleOf(ASerializer)
        correct.assertModuleHas(aSerializer = true, bSerializer = false)
        assertFailsWith<IllegalArgumentException> {
            incorrect + correct
        }
    }

    @Serializable
    @SerialName("C")
    class C

    @Serializer(forClass = C::class)
    object CSerializer : KSerializer<C> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("AnotherName", StructureKind.OBJECT)
    }

    @Serializer(forClass = C::class)
    object CSerializer2 : KSerializer<C> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("C", StructureKind.OBJECT)
    }

    @Test
    fun testOverwriteWithDifferentSerialName() {
        val m1 = SerializersModule {
            polymorphic<Any>(Any::class) {
                subclass(C::class, CSerializer)
            }
        }
        val m2 = SerializersModule {
            polymorphic<Any>(Any::class) {
                subclass(C::class, C.serializer())
            }
        }
        assertEquals(CSerializer, m1.getPolymorphic(Any::class, serializedClassName = "AnotherName"))
        assertFailsWith<IllegalArgumentException> { m1 + m2 }
        val result = m1 overwriteWith m2
        assertEquals(C.serializer(), result.getPolymorphic(Any::class, C()))
        assertEquals(C.serializer(), result.getPolymorphic(Any::class, serializedClassName = "C"))
        assertNull(result.getPolymorphic(Any::class, serializedClassName = "AnotherName"))
    }

    @Test
    fun testOverwriteWithSameSerialName() {
        val m1 = SerializersModule {
            polymorphic<Any>(Any::class) {
                subclass(C::class, C.serializer())
            }
        }
        val m2 = SerializersModule {
            polymorphic<Any>(Any::class) {
                subclass(C::class, CSerializer2)
            }
        }
        assertEquals(C.serializer(), m1.getPolymorphic(Any::class, serializedClassName = "C"))
        assertEquals(CSerializer2, m2.getPolymorphic(Any::class, serializedClassName = "C"))
        assertFailsWith<IllegalArgumentException> { m1 + m2 }
        val result = m1 overwriteWith m2
        assertEquals(CSerializer2, result.getPolymorphic(Any::class, C()))
        assertEquals(CSerializer2, result.getPolymorphic(Any::class, serializedClassName = "C"))
    }

    @Test
    fun testDoesntThrowOnTheSameSerializer() {
        val m1 = serializersModuleOf(A::class, A.serializer())
        val m2 = serializersModuleOf(A::class, A.serializer())
        val aggregate = m1 + m2
        assertEquals(A.serializer(), aggregate.getContextual(A::class))
    }

    @Test
    fun testDoesntThrowOnTheEqualSerializers() {
        val delegate = object : KSerializer<Unit> by Unit.serializer() {
            override fun equals(other: Any?): Boolean = (other is KSerializer<*>) && other.descriptor == descriptor
        }

        val delegate2 = object : KSerializer<Unit> by Unit.serializer() {
            override fun equals(other: Any?): Boolean = (other is KSerializer<*>) && other.descriptor == descriptor
        }

        val m1 = serializersModuleOf(Unit::class, delegate)
        val m2 = serializersModuleOf(Unit::class, delegate2)
        val aggregate = m1 + m2
        assertEquals(delegate2, aggregate.getContextual(Unit::class))
        assertEquals(delegate, aggregate.getContextual(Unit::class))
    }

    @Test
    fun testThrowOnTheSamePolymorphicSerializer() {
        val m1 = SerializersModule { polymorphic(Any::class) { subclass(A.serializer()) } }
        val m2 = SerializersModule { polymorphic(Any::class) { subclass(ASerializer) } }
        assertFailsWith<IllegalArgumentException> { m1 + m2 }
    }

    @Test
    fun testDoesntThrowOnEqualPolymorphicSerializer() {
        val delegate = object : KSerializer<Unit> by Unit.serializer() {
            override fun equals(other: Any?): Boolean = (other is KSerializer<*>) && other.descriptor == descriptor
        }

        val delegate2 = object : KSerializer<Unit> by Unit.serializer() {
            override fun equals(other: Any?): Boolean = (other is KSerializer<*>) && other.descriptor == descriptor
        }

        assertEquals(delegate as Any, delegate2 as Any)
        val m1 = SerializersModule { polymorphic<Any>(Any::class) { subclass(delegate) } }
        val m2 = SerializersModule { polymorphic<Any>(Any::class) { subclass(delegate2) } }
        val aggregate = m1 + m2
        assertEquals(delegate2, aggregate.getPolymorphic(Any::class, Unit))
        assertEquals(delegate, aggregate.getPolymorphic(Any::class, Unit))
    }

    @Test
    fun testPolymorphicCollision() {
        val m1 = SerializersModule {
            polymorphic<Any>(Any::class) {
                defaultDeserializer { _ -> Unit.serializer() }
            }
        }

        val m2 = SerializersModule {
            polymorphic<Any>(Any::class) {
                defaultDeserializer { _ -> Unit.serializer() }
            }
        }

        assertFailsWith<IllegalArgumentException> { m1 + m2 }
    }

    @Test
    fun testNoPolymorphicCollision() {
        val defaultSerializerProvider = { _: String? -> Unit.serializer() }
        val m1 = SerializersModule {
            polymorphic(Any::class) {
                defaultDeserializer(defaultSerializerProvider)
            }
        }

        val m2 = m1 + m1
        assertEquals<Any?>(Unit.serializer(), m2.getPolymorphic(Any::class, serializedClassName = "foo"))
    }

    @Test
    fun testBothPolymorphicDefaults() {
        val anySerializer = object : KSerializer<Any> {
            override val descriptor: SerialDescriptor get() = error("descriptor")
            override fun serialize(encoder: Encoder, value: Any): Unit = error("serialize")
            override fun deserialize(decoder: Decoder): Any = error("deserialize")
        }
        val module = SerializersModule {
            polymorphicDefaultDeserializer(Any::class) { _ -> anySerializer }
            polymorphicDefaultSerializer(Any::class) { _ -> anySerializer }
        }
        assertEquals(anySerializer, module.getPolymorphic(Any::class, 42))
        assertEquals(anySerializer, module.getPolymorphic(Any::class, serializedClassName = "42"))
    }

    @Test
    fun testPolymorphicForStandardSubtypesOfAny() {
        val serializer = object : KSerializer<Int> by Int.serializer() {}

        val module = SerializersModule {
            polymorphic(Any::class) {
                subclass(serializer)
            }
        }

        assertSame(serializer, module.getPolymorphic(Any::class, 42))
        assertSame(serializer, module.getPolymorphic(Any::class, serializedClassName =  "kotlin.Int"))
    }
}
