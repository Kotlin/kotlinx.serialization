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


interface SerializationStrategy<in T> {
    val descriptor: SerialDescriptor

    fun serialize(encoder: Encoder, obj : T)
}

interface DeserializationStrategy<T> {
    val descriptor: SerialDescriptor
    fun deserialize(decoder: Decoder): T
    fun patch(decoder: Decoder, old: T): T
}

enum class UpdateMode {
    BANNED, OVERWRITE, UPDATE
}

interface KSerializer<T>: SerializationStrategy<T>, DeserializationStrategy<T> {
    override val descriptor: SerialDescriptor
    override fun patch(decoder: Decoder, old: T): T = throw UpdateNotSupportedException(descriptor.serialName)
}


@Suppress("UNUSED")
@Deprecated("Inserted into generated code and should not be used directly", level = DeprecationLevel.HIDDEN)
public class SerializationConstructorMarker private constructor()


@ImplicitReflectionSerializer
inline fun <reified T : Any> Encoder.encode(obj: T) { encode(T::class.serializer(), obj) }

fun <T : Any?> Encoder.encode(strategy: SerializationStrategy<T>, obj: T) {
    encodeSerializableValue(strategy, obj)
}

@ImplicitReflectionSerializer
inline fun <reified T: Any> Decoder.decode(): T = this.decode(T::class.serializer())

fun <T : Any?> Decoder.decode(deserializer: DeserializationStrategy<T>): T = decodeSerializableValue(deserializer)
