/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.pinnedBugs

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

/**
 * PINNED BUG. Related issue: https://github.com/Kotlin/kotlinx.serialization/issues/2489
 * Pins the current behavior that surfaced in the specific issue.
 *
 * @Contextual on a TYPE position (`val inner: @Contextual Inner`) is ignored
 * when the target type already has a @Serializable(with = ...) annotation,
 * but works when on the PROPERTY position.
 */
class Gh2489PinnedBugTest {
    object OwnSerializer : KSerializer<Inner> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Inner", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: Inner) = encoder.encodeString("explicit:${value.x}")
        override fun deserialize(decoder: Decoder): Inner =
            Inner(decoder.decodeString().removePrefix("explicit:").toInt())
    }

    @Serializable(with = OwnSerializer::class)
    class Inner(val x: Int)

    object CustomSerializer : KSerializer<Inner> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CustomInner", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: Inner) = encoder.encodeString("custom:${value.x}")
        override fun deserialize(decoder: Decoder): Inner =
            Inner(decoder.decodeString().removePrefix("custom:").toInt())
    }

    @Serializable
    data class OuterTypeAnnotation(val inner: @Contextual Inner)

    @Serializable
    data class OuterPropertyAnnotation(@Contextual val inner: Inner)

    @Test
    fun contextualOnTypeUsageIsIgnoredForTypeWithOwnSerializable() {
        val json = Json {
            serializersModule = SerializersModule {
                contextual<Inner>(CustomSerializer)
            }
        }
        val value = Inner(42)
        // @Contextual on the type position does not pick up contextual serializer
        val typeAnnotationResult = json.encodeToString(OuterTypeAnnotation(value))
        assertEquals("{\"inner\":\"explicit:42\"}", typeAnnotationResult)
        // @Contextual on the property position does pick up contextual serializer
        val propertyAnnotationResult = json.encodeToString(OuterPropertyAnnotation(value))
        assertEquals("{\"inner\":\"custom:42\"}", propertyAnnotationResult)
    }
}
