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

object PolymorphicSerializer : KSerializer<Any> {

    override val descriptor: SerialDescriptor
        get() = PolymorphicClassDesc

    override fun serialize(output: Encoder, obj: Any) {
        val saver = serializerByValue(obj, output.context)
        @Suppress("NAME_SHADOWING")
        val output = output.beginStructure(descriptor)
        output.encodeStringElement(descriptor, 0, saver.descriptor.name)
        output.encodeSerializableElement(descriptor, 1, saver, obj)
        output.endStructure(descriptor)
    }

    override fun deserialize(input: Decoder): Any {
        @Suppress("NAME_SHADOWING")
        val input = input.beginStructure(descriptor)
        var klassName: String? = null
        var value: Any? = null
        mainLoop@ while (true) {
            when (input.decodeElementIndex(descriptor)) {
                READ_ALL -> {
                    klassName = input.decodeStringElement(descriptor, 0)
                    val loader = serializerBySerialDescClassname<Any>(klassName, input.context)
                    value = input.decodeSerializableElement(descriptor, 1, loader)
                    break@mainLoop
                }
                READ_DONE -> {
                    break@mainLoop
                }
                0 -> {
                    klassName = input.decodeStringElement(descriptor, 0)
                }
                1 -> {
                    klassName = requireNotNull(klassName) { "Cannot read polymorphic value before its type token" }
                    val loader = serializerBySerialDescClassname<Any>(klassName, input.context)
                    value = input.decodeSerializableElement(descriptor, 1, loader)
                }
                else -> throw SerializationException("Invalid index")
            }
        }

        input.endStructure(descriptor)
        return requireNotNull(value) { "Polymorphic value have not been read" }
    }
}
