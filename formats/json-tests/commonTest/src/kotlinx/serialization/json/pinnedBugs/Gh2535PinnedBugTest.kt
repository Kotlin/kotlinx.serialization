/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.pinnedBugs

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

/**
 * PINNED BUG. Related issue: https://github.com/Kotlin/kotlinx.serialization/issues/2535
 * Pins the current behavior that surfaced in the specific issue.
 *
 * Json.encodeToJsonElement() crashes with an IndexOutOfBoundsException for a generic
 * ContextualSerializer, while Json.encodeToString() works without issue
 */
class Gh2535PinnedBugTest {
    @Serializable
    data class Box<T>(val contents: T)

    class BoxSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<Box<T>> {
        override val descriptor: SerialDescriptor = dataSerializer.descriptor
        override fun serialize(encoder: Encoder, value: Box<T>) =
            dataSerializer.serialize(encoder, value.contents)
        override fun deserialize(decoder: Decoder): Box<T> = Box(dataSerializer.deserialize(decoder))
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> createContextualSerializer(typeArgumentSerializer: KSerializer<T>): KSerializer<Box<T>> =
        ContextualSerializer(
            serializableClass = Box::class,
            fallbackSerializer = null,
            typeArgumentsSerializers = arrayOf(typeArgumentSerializer),
        ) as KSerializer<Box<T>>

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun encodeToJsonElementCrashesForGenericContextualSerializerButEncodeToStringWorks() {
        val json = Json {
            serializersModule = SerializersModule {
                contextual(Box::class) { args -> BoxSerializer(args[0]) }
            }
        }
        val serializer = createContextualSerializer(String.serializer())
        // The streaming encodeToString works
        assertEquals("\"test\"", json.encodeToString(serializer, Box("test")))
        // encodeToJsonElement crashes
        val e = assertFailsWith<JsonEncodingException> {
            json.encodeToJsonElement(serializer, Box("test"))
        }
        assertEquals("Serialization of 'kotlinx.serialization.ContextualSerializer<Box>' failed because of 'Empty list doesn't contain element at index 0.' exception in the encoder", e.message)
    }
}
