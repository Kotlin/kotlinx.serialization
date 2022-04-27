/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * Serializer for Kotlin's singletons (denoted by `object` keyword).
 * To preserve singleton identity after serialization and deserialization, object serializer
 * uses an [object instance][objectInstance].
 * By default, a singleton is serialized as an empty structure, e.g. `{}` in JSON.
 */
@PublishedApi
@OptIn(ExperimentalSerializationApi::class)
internal class ObjectSerializer<T : Any>(serialName: String, private val objectInstance: T) : KSerializer<T> {

    @PublishedApi // See comment in SealedClassSerializer
    internal constructor(
        serialName: String,
        objectInstance: T,
        classAnnotations: Array<Annotation>
    ) : this(serialName, objectInstance) {
        _annotations = classAnnotations.asList()
    }

    private var _annotations: List<Annotation> = emptyList()

    override val descriptor: SerialDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildSerialDescriptor(serialName, StructureKind.OBJECT) {
            annotations = _annotations
        }
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.beginStructure(descriptor).endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): T {
        decoder.decodeStructure(descriptor) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> {
                    return@decodeStructure
                }
                else -> throw SerializationException("Unexpected index $index")
            }
        }
        return objectInstance
    }
}
