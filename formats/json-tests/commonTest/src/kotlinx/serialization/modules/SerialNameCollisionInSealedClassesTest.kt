/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

private const val prefix = "kotlinx.serialization.modules.SerialNameCollisionInSealedClassesTest"

class SerialNameCollisionInSealedClassesTest {
    @Serializable
    sealed class Base {
        @Serializable
        data class Child(val type: String, @SerialName("type2") val f: String = "2") : Base()
    }

    private fun Json(discriminator: String, useArrayPolymorphism: Boolean = false) = Json {
        classDiscriminator = discriminator
        this.useArrayPolymorphism = useArrayPolymorphism
    }

    @Test
    fun testCollisionWithDiscriminator() {
        assertFailsWith<SerializationException> { Json("type").encodeToString(Base.serializer(), Base.Child("a")) }
        assertFailsWith<SerializationException> { Json("type2").encodeToString(Base.serializer(), Base.Child("a")) }
        val actual = Json("f").encodeToString(Base.serializer(), Base.Child("a"))
        val expected =
            """{"f":"kotlinx.serialization.modules.SerialNameCollisionInSealedClassesTest.Base.Child","type":"a"}"""
        assertEquals(expected, actual)
    }

    @Test
    fun testNoCollisionWithArrayPolymorphism() {
        val actual = Json("type", true).encodeToString(Base.serializer(), Base.Child("a"))
        val expected =
            """["kotlinx.serialization.modules.SerialNameCollisionInSealedClassesTest.Base.Child",{"type":"a"}]"""
        assertEquals(expected, actual)
    }

    @Serializable
    sealed class BaseCollision {
        @Serializable
        class Child() : BaseCollision()

        @Serializable
        @SerialName("$prefix.BaseCollision.Child")
        class ChildCollided() : BaseCollision()
    }

    @Test
    fun testDescriptorInitializerFailure() {
        val _ = BaseCollision.Child()
        val _ = BaseCollision.ChildCollided()
        val descriptor = BaseCollision.ChildCollided.serializer().descriptor // Doesn't fail
        assertEquals("$prefix.BaseCollision.Child", descriptor.serialName)
        assertFailsWith<IllegalStateException> { BaseCollision.serializer().descriptor }
    }
}
