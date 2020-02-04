/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*

/**
 * Serializer for Kotlin's singletons (denoted by `object` keyword).
 * To preserve singleton identity after serialization and deserialization, object serializer
 * uses an [object instance][objectInstance].
 * By default, a singleton is serialized as an empty structure, e.g. `{}` in JSON
 */
@Deprecated("", level = DeprecationLevel.HIDDEN)
public class ObjectSerializer<T : Any>(serialName: String, private val objectInstance: T) : KSerializer<T> {
    override val descriptor: SerialDescriptor = SerialDescriptor(serialName, StructureKind.OBJECT)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.beginStructure(descriptor).endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): T {
        decoder.beginStructure(descriptor).endStructure(descriptor)
        return objectInstance
    }
}
