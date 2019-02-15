/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.features.PolyBase
import kotlinx.serialization.features.PolyDerived
import kotlin.reflect.KClass
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

    private fun SerialModule.assertModuleHas(aSerializer: Boolean = false, bSerializer: Boolean = false) {
        with(this) {
            assertSame(if (aSerializer) ASerializer else null, getContextual<A>())
            assertSame(if (bSerializer) BSerializer else null, getContextual<B>())
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
        val module1 = serializersModuleOf(mapOf(B::class to BSerializer))
        module1.assertModuleHas(
            aSerializer = false,
            bSerializer = true
        )

        serializersModuleOf(mapOf(A::class to ASerializer, B::class to BSerializer)).assertModuleHas(
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
        val moduleA = serializersModule(ASerializer)
        val moduleB = serializersModuleOf(mapOf(B::class to BSerializer))

        (moduleA + moduleB).assertModuleHas(
            aSerializer = true,
            bSerializer = true
        )

        var composite: SerialModule = SerializersModule { }
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
            contextual(BSerializer)
        }
        module.assertModuleHas(aSerializer = true, bSerializer = true)
    }

    @Test
    fun testPolymorphicDSL() {
        val module1 = SerializersModule {
            polymorphic(PolyBase.serializer()) {
                PolyDerived::class with PolyDerived.serializer()
            }
            polymorphic(Any::class, baseSerializer = null) {
                PolyBase::class with PolyBase.serializer()
                PolyDerived::class with PolyDerived.serializer()
            }
        }

        val module2 = SerializersModule {
            polymorphic(Any::class, PolyBase::class) {
                addSubclass(PolyBase.serializer())
                addSubclass(PolyDerived.serializer())
            }
        }

        val module3 = SerializersModule {
            polymorphic(PolyBase::class, Any::class) {
                addSubclass(PolyBase::class, PolyBase.serializer())
                addSubclass(PolyDerived::class, PolyDerived.serializer())
            }
        }

        val base = PolyBase(10)
        val derived = PolyDerived("foo")

        listOf(module1, module2, module3).forEachIndexed { index, module ->
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
            assertFailsWith<SerializerAlreadyRegisteredException> {
                contextual(A::class, ASerializer)
            }
        }
        moduleA.assertModuleHas(aSerializer = true, bSerializer = false)
    }

    @Test
    fun testOverwriteIsRightBiased() {
        val incorrect = serializersModuleOf(mapOf<KClass<*>, KSerializer<*>>(A::class to BSerializer))
        val correct = serializersModuleOf(mapOf<KClass<*>, KSerializer<*>>(A::class to ASerializer))
        correct.assertModuleHas(aSerializer = true, bSerializer = false)
        val sum = incorrect overwriteWith correct
        sum.assertModuleHas(aSerializer = true, bSerializer = false)
    }

    @Test
    fun testPlusThrowsExceptionOnDuplication() {
        val incorrect = serializersModuleOf(mapOf<KClass<*>, KSerializer<*>>(A::class to BSerializer))
        val correct = serializersModuleOf(mapOf<KClass<*>, KSerializer<*>>(A::class to ASerializer))
        correct.assertModuleHas(aSerializer = true, bSerializer = false)
        assertFailsWith<SerializerAlreadyRegisteredException> {
            incorrect + correct
        }
    }
}
