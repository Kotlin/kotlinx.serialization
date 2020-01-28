/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*

val <T> KSerializer<T>.list: KSerializer<List<T>>
    get() = ArrayListSerializer(this)

val <T> KSerializer<T>.set: KSerializer<Set<T>>
    get() = LinkedHashSetSerializer(this)

val <K, V> Pair<KSerializer<K>, KSerializer<V>>.map: KSerializer<Map<K, V>>
    get() = LinkedHashMapSerializer(this.first, this.second)

/**
 * Returns a nullable serializer for the given serializer of non-null type.
 */
public val <T : Any> KSerializer<T>.nullable: KSerializer<T?>
    get() {
        @Suppress("UNCHECKED_CAST")
        return if (descriptor.isNullable) (this as KSerializer<T?>) else NullableSerializer(this)
    }

/**
 * Creates a [List] out of a child descriptors retrieved via [SerialDescriptor.getElementDescriptor].
 *
 * Size of a list is equal to [SerialDescriptor.elementsCount].
 * TODO revisit
 */
public fun SerialDescriptor.elementDescriptors(): List<SerialDescriptor> {
    return List(elementsCount) { getElementDescriptor(it) }
}

/**
 * Returns a [List] out of all serial names of serial descriptor [elements][SerialDescriptor.getElementDescriptor]
 */
public fun SerialDescriptor.elementNames(): List<String> {
    // TODO always allocates, also revisit
    return List(elementsCount) { getElementName(it) }
}

/**
 * Same as [SerialDescriptor.getElementIndex], but throws [SerializationException] if
 * given [name] is not associated with any element in the descriptor.
 */
public fun SerialDescriptor.getElementIndexOrThrow(name: String): Int {
    val i = getElementIndex(name)
    if (i == CompositeDecoder.UNKNOWN_NAME)
        throw SerializationException("${this.serialName} does not contain element with name '$name'")
    return i
}

/**
 * Searches for annotation of type [A] in annotations, obtained via
 * [SerialDescriptor.getElementAnnotations] at given [elementIndex]
 *
 * Returns null if there are no annotations with such type.
 * Throws [IllegalStateException] if there are duplicated annotations for a given type.
 */
public inline fun <reified A: Annotation> SerialDescriptor.findAnnotation(elementIndex: Int): A? {
    val candidates = getElementAnnotations(elementIndex).filterIsInstance<A>()
    return when (candidates.size) {
        0 -> null
        1 -> candidates[0]
        else -> throw IllegalStateException("There are duplicate annotations of type ${A::class} in the descriptor $this")
    }
}
