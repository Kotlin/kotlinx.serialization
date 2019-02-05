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

import kotlinx.serialization.internal.*

val <T> KSerializer<T>.list: KSerializer<List<T>>
    get() = ArrayListSerializer(this)

val <T> KSerializer<T>.set: KSerializer<Set<T>>
    get() = LinkedHashSetSerializer(this)

val <K, V> Pair<KSerializer<K>, KSerializer<V>>.map: KSerializer<Map<K, V>>
    get() = LinkedHashMapSerializer(this.first, this.second)

fun String.Companion.serializer(): KSerializer<String> = StringSerializer
fun Char.Companion.serializer(): KSerializer<Char> = CharSerializer
fun Byte.Companion.serializer(): KSerializer<Byte> = ByteSerializer
fun Short.Companion.serializer(): KSerializer<Short> = ShortSerializer
fun Int.Companion.serializer(): KSerializer<Int> = IntSerializer
fun Long.Companion.serializer(): KSerializer<Long> = LongSerializer
fun Float.Companion.serializer(): KSerializer<Float> = FloatSerializer
fun Double.Companion.serializer(): KSerializer<Double> = DoubleSerializer
fun Boolean.Companion.serializer(): KSerializer<Boolean> = BooleanSerializer

/**
 * Creates a [List] out of a child descriptors retrieved via [SerialDescriptor.getElementDescriptor].
 *
 * Size of a sequence is equal to [SerialDescriptor.elementsCount].
 */
fun SerialDescriptor.elementDescriptors(): List<SerialDescriptor> {
    return (0 until elementsCount).map { getElementDescriptor(it) }
}

fun SerialDescriptor.getElementIndexOrThrow(name: String): Int {
    val i = getElementIndex(name)
    if (i == CompositeDecoder.UNKNOWN_NAME) throw SerializationException("Unknown name '$name'")
    return i
}

/**
 * Searches for annotation of type [A] in annotations, obtained via
 * [SerialDescriptor.getElementAnnotations] at given [elementIndex]
 *
 * Returns null if there are no annotations with such type.
 * Throws [IllegalStateException] if there are duplicated annotations for a given type.
 */
inline fun <reified A: Annotation> SerialDescriptor.findAnnotation(elementIndex: Int): A? {
    val candidates = getElementAnnotations(elementIndex).filterIsInstance<A>()
    return when (candidates.size) {
        0 -> null
        1 -> candidates[0]
        else -> throw IllegalStateException("There are duplicate annotations of type ${A::class} in the descriptor $this")
    }
}

@Deprecated(deprecationText, ReplaceWith("elementsCount"))
val SerialDescriptor.associatedFieldsCount: Int
    get() = elementsCount
