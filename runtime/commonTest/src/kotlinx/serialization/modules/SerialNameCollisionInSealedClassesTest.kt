/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
        assertFailsWith<IllegalStateException> { Json("type").encodeToString(Base.serializer(), Base.Child("a")) }
        assertFailsWith<IllegalStateException> { Json("type2").encodeToString(Base.serializer(), Base.Child("a")) }
        Json("f").encodeToString(Base.serializer(), Base.Child("a"))
    }

    @Test
    fun testNoCollisionWithArrayPolymorphism() {
        Json("type", true).encodeToString(Base.serializer(), Base.Child("a"))
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
        BaseCollision.Child()
        BaseCollision.ChildCollided()
        BaseCollision.ChildCollided.serializer().descriptor // Doesn't fail
        assertFailsWith<IllegalArgumentException> { BaseCollision.serializer().descriptor }
    }
}
