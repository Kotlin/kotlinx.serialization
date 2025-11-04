/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.assertFailsWithMessage
import kotlin.test.*

private const val prefix = "kotlinx.serialization.modules.SerialNameCollisionTest"

class SerialNameCollisionTest {

    // Polymorphism
    interface IBase

    @Serializable
    abstract class Base : IBase

    @Serializable
    data class Derived(val type: String, val type2: String) : Base()

    @Serializable
    data class DerivedCustomized(
        @SerialName("type") val t: String, @SerialName("type2") val t2: String, val t3: String
    ) : Base()

    @Serializable
    @SerialName("$prefix.Derived")
    data class DerivedRenamed(val type: String, val type2: String) : Base()

    private fun Json(discriminator: String, context: SerializersModule, useArrayPolymorphism: Boolean = false) = Json {
        classDiscriminator = discriminator
        this.useArrayPolymorphism = useArrayPolymorphism
        serializersModule = context
    }

    @Test
    fun testCollisionWithDiscriminator() {
        val module = SerializersModule {
            polymorphic(Base::class) {
                subclass(Derived.serializer())
            }
        }

        assertFailsWithMessage<SerializationException>("Class 'kotlinx.serialization.modules.SerialNameCollisionTest.Derived' cannot be serialized as base class 'kotlinx.serialization.Polymorphic<Base>' because it has property name that conflicts with JSON class discriminator 'type'.") {
            Json("type", module).encodeToString<Base>(Derived("foo", "bar"))
        }
        assertFailsWithMessage<SerializationException>("Class 'kotlinx.serialization.modules.SerialNameCollisionTest.Derived' cannot be serialized as base class 'kotlinx.serialization.Polymorphic<Base>' because it has property name that conflicts with JSON class discriminator 'type2'.") {
            Json("type2", module).encodeToString<Base>(Derived("foo", "bar"))
        }
        assertEquals("{\"type3\":\"kotlinx.serialization.modules.SerialNameCollisionTest.Derived\",\"type\":\"foo\",\"type2\":\"bar\"}",
            Json("type3", module).encodeToString<Base>(Derived("foo", "bar"))
        )
    }

    @Test
    fun testNoCollisionWithArrayPolymorphism() {
        val module = SerializersModule {
            polymorphic(Base::class) {
                subclass(Derived.serializer())
            }
        }
        Json("type", module, true)
    }

    @Test
    fun testCollisionWithDiscriminatorViaSerialNames() {
        val module = SerializersModule {
            polymorphic(Base::class) {
                subclass(DerivedCustomized.serializer())
            }
        }

        assertFailsWithMessage<SerializationException>("Class 'kotlinx.serialization.modules.SerialNameCollisionTest.DerivedCustomized' cannot be serialized as base class 'kotlinx.serialization.Polymorphic<Base>' because it has property name that conflicts with JSON class discriminator 'type'.") {
            Json("type", module).encodeToString<Base>(DerivedCustomized("foo", "bar", "t3"))
        }
        assertFailsWithMessage<SerializationException>("Class 'kotlinx.serialization.modules.SerialNameCollisionTest.DerivedCustomized' cannot be serialized as base class 'kotlinx.serialization.Polymorphic<Base>' because it has property name that conflicts with JSON class discriminator 'type2'.") {
            Json("type2", module).encodeToString<Base>(DerivedCustomized("foo", "bar", "t3"))
        }
        assertFailsWithMessage<SerializationException>("Class 'kotlinx.serialization.modules.SerialNameCollisionTest.DerivedCustomized' cannot be serialized as base class 'kotlinx.serialization.Polymorphic<Base>' because it has property name that conflicts with JSON class discriminator 't3'.") {
            Json("t3", module).encodeToString<Base>(DerivedCustomized("foo", "bar", "t3"))
        }
        assertEquals("{\"t4\":\"kotlinx.serialization.modules.SerialNameCollisionTest.DerivedCustomized\",\"type\":\"foo\",\"type2\":\"bar\",\"t3\":\"t3\"}",
        Json("t4", module).encodeToString<Base>(DerivedCustomized("foo", "bar", "t3"))
        )

    }

    @Test
    fun testCollisionWithinHierarchy() {
        SerializersModule {
            assertFailsWith<IllegalArgumentException> {
                polymorphic(Base::class) {
                    subclass(Derived.serializer())
                    subclass(DerivedRenamed.serializer())
                }
            }
        }
    }

    @Test
    fun testCollisionWithinHierarchyViaConcatenation() {
        val module = SerializersModule {
            polymorphic(Base::class) {
                subclass(Derived.serializer())
            }
        }
        val module2 = SerializersModule {
            polymorphic(Base::class) {
                subclass(DerivedRenamed.serializer())
            }
        }

        assertFailsWith<IllegalArgumentException> { module + module2 }
    }

    @Test
    fun testNoCollisionWithinHierarchy() {
        val module = SerializersModule {
            polymorphic(Base::class) {
                subclass(Derived.serializer())
            }

            polymorphic(IBase::class) {
                subclass(DerivedRenamed.serializer())
            }
        }

        assertSame(Derived.serializer(), module.getPolymorphic(Base::class, "$prefix.Derived"))
        assertSame(
            DerivedRenamed.serializer(),
            module.getPolymorphic(IBase::class, "$prefix.Derived")
        )
    }
}
