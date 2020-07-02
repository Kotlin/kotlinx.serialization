/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.modules.*
import kotlin.reflect.*

internal class ContextValidator(private val discriminator: String) : SerializersModuleCollector {
    override fun <T : Any> contextual(kClass: KClass<T>, serializer: KSerializer<T>) {
        // Nothing here
    }

    override fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>
    ) {
        val descriptor = actualSerializer.descriptor
        for (i in 0 until descriptor.elementsCount) {
            val name = descriptor.getElementName(i)
            if (name == discriminator) {
                throw IllegalArgumentException(
                    "Polymorphic serializer for $actualClass has property '$name' that conflicts " +
                            "with JSON class discriminator. You can either change class discriminator in JsonConfiguration, " +
                            "rename property with @SerialName annotation " +
                            "or fall back to array polymorphism"
                )
            }
        }
    }

    override fun <Base : Any> polymorphicDefault(
        baseClass: KClass<Base>,
        defaultSerializerProvider: (className: String) -> DeserializationStrategy<out Base>?
    ) {
        // Nothing here
    }
}
