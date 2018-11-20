/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization

import kotlinx.serialization.CompositeDecoder.Companion.READ_ALL
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.internal.PolymorphicClassDesc

@ImplicitReflectionSerializer
object PolymorphicSerializer : KSerializer<Any> {

    override val descriptor: SerialDescriptor
        get() = PolymorphicClassDesc

    override fun serialize(encoder: Encoder, obj: Any) {
        val serializer = serializerByValue(obj, encoder.context)
        @Suppress("NAME_SHADOWING")
        val encoder = encoder.beginStructure(descriptor)
        encoder.encodeStringElement(descriptor, 0, serializer.descriptor.name)
        encoder.encodeSerializableElement(descriptor, 1, serializer, obj)
        encoder.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): Any {
        @Suppress("NAME_SHADOWING")
        val decoder = decoder.beginStructure(descriptor)
        var klassName: String? = null
        var value: Any? = null
        mainLoop@ while (true) {
            when (decoder.decodeElementIndex(descriptor)) {
                READ_ALL -> {
                    klassName = decoder.decodeStringElement(descriptor, 0)
                    val deserializer = serializerBySerialDescClassName<Any>(klassName, decoder.context)
                    value = decoder.decodeSerializableElement(descriptor, 1, deserializer)
                    break@mainLoop
                }
                READ_DONE -> {
                    break@mainLoop
                }
                0 -> {
                    klassName = decoder.decodeStringElement(descriptor, 0)
                }
                1 -> {
                    klassName = requireNotNull(klassName) { "Cannot read polymorphic value before its type token" }
                    val deserializer = serializerBySerialDescClassName<Any>(klassName, decoder.context)
                    value = decoder.decodeSerializableElement(descriptor, 1, deserializer)
                }
                else -> throw SerializationException("Invalid index")
            }
        }

        decoder.endStructure(descriptor)
        return requireNotNull(value) { "Polymorphic value have not been read" }
    }
}
