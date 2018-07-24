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

import kotlinx.serialization.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlin.reflect.KClass


interface SerialDescriptor {
    val name: String
    val kind: SerialKind

    fun getElementName(index: Int): String
    fun getElementIndex(name: String): Int

    fun getEntityAnnotations(): List<Annotation> = emptyList()
    fun getElementAnnotations(index: Int): List<Annotation> = emptyList()

    val elementsCount: Int
        get() = 0

    fun getElementDescriptor(index: Int): SerialDescriptor = TODO()

    val isNullable: Boolean
        get() = false

    fun isElementOptional(index: Int): Boolean

    // --- deprecations and helpers

    fun getElementIndexOrThrow(name: String): Int {
        val i = getElementIndex(name)
        if (i == UNKNOWN_NAME) throw SerializationException("Unknown name '$name'")
        return i
    }

    @Deprecated("Obsolete name from preview version of library.", ReplaceWith("elementsCount"))
    val associatedFieldsCount: Int
        get() = elementsCount
}

interface SerializationStrategy<in T> {
    fun serialize(output: Encoder, obj : T)

    val descriptor: SerialDescriptor
}

interface DeserializationStrategy<T> {
    fun deserialize(input: Decoder): T
    fun patch(input: Decoder, old: T): T

    val descriptor: SerialDescriptor
}

enum class UpdateMode {
    BANNED, OVERWRITE, UPDATE
}

interface KSerializer<T>: SerializationStrategy<T>, DeserializationStrategy<T> {
    override val descriptor: SerialDescriptor

    override fun patch(input: Decoder, old: T): T = throw UpdateNotSupportedException(descriptor.name)
}


interface EnumCreator<E : Enum<E>> {
    fun createFromOrdinal(ordinal: Int): E
    fun createFromName(name: String): E
}

internal class LegacyEnumCreator<E : Enum<E>>(private val eClass: KClass<E>) : EnumCreator<E> {
    override fun createFromOrdinal(ordinal: Int): E {
        return enumFromOrdinal(eClass, ordinal)
    }

    override fun createFromName(name: String): E {
        return enumFromName(eClass, name)
    }
}


class SerializationConstructorMarker private constructor()


inline fun <reified T : Any> Encoder.encode(obj: T) { encode(T::class.serializer(), obj) }
fun <T : Any?> Encoder.encode(strategy: SerializationStrategy<T>, obj: T) { strategy.serialize(this, obj) }
fun <T : Any> Encoder.encodeNullable(strategy: SerializationStrategy<T>, obj: T?) {
    if (obj == null) {
        encodeNull()
    } else {
        encodeNotNullMark()
        strategy.serialize(this, obj)
    }
}


inline fun <reified T: Any> Decoder.decode(): T = this.decode(T::class.serializer())
fun <T : Any?> Decoder.decode(loader: DeserializationStrategy<T>): T = loader.deserialize(this)
fun <T : Any> Decoder.decodeNullable(loader: DeserializationStrategy<T>): T? = if (decodeNotNullMark()) decode(loader) else decodeNull()
