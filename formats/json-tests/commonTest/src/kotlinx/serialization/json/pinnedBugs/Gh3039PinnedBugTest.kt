/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.pinnedBugs

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.jvm.*
import kotlin.test.*

/**
 * PINNED BUG. Related issue: https://github.com/Kotlin/kotlinx.serialization/issues/3039
 * Pins the current behavior that surfaced in the specific issue.
 *
 * encodeToJsonElement and JsonTransformingSerializer built on top of it
 * silently drops the type discriminator, when serializing a value class
 * that wraps a non-primitive to a sealed interface subtype.
 */
class Gh3039PinnedBugTest {

    @Serializable
    data class Data(val id: Int, val name: String)

    @Serializable
    sealed interface Parent

    @JvmInline
    @Serializable
    value class Child(val value: Data) : Parent

    object ParentSerializer : JsonTransformingSerializer<Parent>(Parent.serializer()) {
        override fun transformSerialize(element: JsonElement): JsonElement {
            return JsonObject(element.jsonObject + ("version" to JsonPrimitive("1.0")))
        }
    }

    @Test
    fun encodeToJsonElementDropsDiscriminatorForValueClassWrapper() {
        val message: Parent = Child(Data(1, "one"))
        // encodeToJsonElement does not produce type discriminator
        val element = Json.encodeToJsonElement(Parent.serializer(), message)
        assertEquals("{\"id\":1,\"name\":\"one\"}", element.toString())
        // JsonTransformingSerializer does not produce type discriminator
        val str = Json.encodeToString(ParentSerializer, message)
        assertEquals("{\"id\":1,\"name\":\"one\",\"version\":\"1.0\"}",str)
    }
}
