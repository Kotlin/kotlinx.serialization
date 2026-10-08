/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.pinnedBugs

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * PINNED BUG. Related issue: https://github.com/Kotlin/kotlinx.serialization/issues/3118
 * Pins the current behavior that surfaced in the specific issue.
 *
 * When a generic sealed class's type argument is a nullable type, generated
 * serializer for the generic subclass incorrectly uses NON-nullable serializer
 * which leads to the JsonEncodingException at runtime
 */
class Gh3118PinnedBugTest {
    @Serializable
    sealed class TypedSealedClass<out T> {
        @Serializable
        data class Child<out T>(val value: T) : TypedSealedClass<T>()
    }

    @Serializable
    data class Box(
        val value: TypedSealedClass<String?>
    )

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun serializingNullValueInGenericSealedSubclassWithNullableTypeArgThrows() {
        val box = Box(
            value = TypedSealedClass.Child(null)
        )

        val e = assertFailsWith<JsonEncodingException> {
            Json.encodeToString(box)
        }
        assertEquals(
            "Serialization of 'kotlin.String' failed because of 'Parameter specified as non-null is null: method kotlinx.serialization.internal.StringSerializer.serialize, parameter value' exception in the encoder",
            e.message
        )
    }
}
