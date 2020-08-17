/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class JsonProhibitedPolymorphicKindsTest : JsonTestBase() {

    @Serializable
    sealed class Base {
        @Serializable
        class Impl(val data: Int) : Base()
    }

    @Serializable
    enum class MyEnum

    @Test
    fun testSealedSubclass() {
        assertFailsWith<IllegalArgumentException> {
            Json(true) {
                subclass(Base::class)
            }
        }
        assertFailsWith<IllegalArgumentException> {
            Json(false) {
                subclass(Base::class)
            }
        }
    }

    @Test
    fun testPrimitive() {
        assertFailsWith<IllegalArgumentException> {
            Json(false) {
                subclass(Int::class)
            }
        }

        // Doesn't fail
        Json(true) {
            subclass(Int::class)
        }
    }

    @Test
    fun testEnum() {
        assertFailsWith<IllegalArgumentException> {
            Json(false) {
                subclass(MyEnum::class)
            }
        }

        Json(true) {
            subclass(MyEnum::class)
        }
    }

    @Test
    fun testStructures() {
        assertFailsWith<IllegalArgumentException> {
            Json(false) {
                subclass(serializer<Map<Int, Int>>())
            }
        }

        assertFailsWith<IllegalArgumentException> {
            Json(false) {
                subclass(serializer<List<Int>>())
            }
        }

        Json(true) {
            subclass(serializer<List<Int>>())
        }


        Json(true) {
            subclass(serializer<Map<Int, Int>>())
        }
    }

    private fun Json(useArrayPolymorphism: Boolean, builderAction: PolymorphicModuleBuilder<Any>.() -> Unit) = Json {
        this.useArrayPolymorphism = useArrayPolymorphism
        serializersModule = SerializersModule {
            polymorphic(Any::class) {
                builderAction()
            }
        }
    }
}
