/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlin.reflect.KClass

public class SealedClassSerializer<T : Any>(
    serialName: String,
    override val baseClass: KClass<T>,
    subclasses: Array<KClass<out T>>,
    subSerializers: Array<KSerializer<out T>>
) : AbstractPolymorphicSerializer<T>() {

    private val backingMap: Map<KClass<out T>, KSerializer<out T>>

    private val inverseMap: Map<String, KSerializer<out T>>


    init {
        require(subclasses.size == subSerializers.size) { "Arrays of classes and serializers must have the same length" }
        backingMap = subclasses.zip(subSerializers).toMap()
        inverseMap = backingMap.values.associateBy { serializer -> serializer.descriptor.name }
    }

    @Suppress("UNCHECKED_CAST")
    override fun findPolymorphicSerializer(decoder: CompositeDecoder, klassName: String): KSerializer<out T> {
        return inverseMap[klassName]
                ?: super.findPolymorphicSerializer(decoder, klassName)
    }

    @Suppress("UNCHECKED_CAST")
    override fun findPolymorphicSerializer(encoder: Encoder, value: T): KSerializer<out T> {
        return backingMap[value::class]
                ?: super.findPolymorphicSerializer(encoder, value)
    }

    override val descriptor: SerialDescriptor = SealedClassDescriptor(serialName, subSerializers.map { it.descriptor })
}

public class SealedClassDescriptor(
    override val name: String,
    elementDescriptors: List<SerialDescriptor>
) : SerialClassDescImpl(name) {
    override val kind: SerialKind = PolymorphicKind.SEALED

    init {
        elementDescriptors.forEach {
            addElement(it.name)
            pushDescriptor(it)
        }
    }
}
