/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.reflect.*

@OptIn(ExperimentalSerializationApi::class)
internal class JsonSerializersModuleValidator(
    configuration: JsonConfiguration,
) : SerializersModuleCollector {

    private val discriminator: String = configuration.classDiscriminator
    private val useArrayPolymorphism: Boolean = configuration.useArrayPolymorphism
    private val isDiscriminatorRequired = configuration.classDiscriminatorMode != ClassDiscriminatorMode.NONE

    override fun <T : Any> contextual(
        kClass: KClass<T>,
        provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>
    ) {
        // Nothing here
    }

    override fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>
    ) {
        val descriptor = actualSerializer.descriptor
        checkKind(descriptor, actualClass)
        if (!useArrayPolymorphism && isDiscriminatorRequired) {
            // Collisions with "type" can happen only for JSON polymorphism
            checkDiscriminatorCollisions(descriptor, actualClass)
        }
    }

    private fun checkKind(descriptor: SerialDescriptor, actualClass: KClass<*>) {
        val kind = descriptor.kind
        if (kind is PolymorphicKind || kind == SerialKind.CONTEXTUAL) {
            throw IllegalArgumentException("Serializer for ${actualClass.simpleName} can't be registered as a subclass for polymorphic serialization " +
                    "because its kind $kind is not concrete. To work with multiple hierarchies, register it as a base class.")
        }

        if (useArrayPolymorphism) return
        if (!isDiscriminatorRequired) return
        /*
         * For this kind we can't intercept the JSON object {} in order to add "type: ...".
         * Except for maps that just can clash and accidentally overwrite the type.
         */
        if (kind == StructureKind.LIST || kind == StructureKind.MAP
            || kind is PrimitiveKind
            || kind is SerialKind.ENUM
        ) {
            throw IllegalArgumentException(
                "Serializer for ${actualClass.simpleName} of kind $kind cannot be serialized polymorphically with class discriminator."
            )
        }
    }

    private fun checkDiscriminatorCollisions(
        descriptor: SerialDescriptor,
        actualClass: KClass<*>
    ) {
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

    override fun <Base : Any> polymorphicDefaultSerializer(
        baseClass: KClass<Base>,
        defaultSerializerProvider: (value: Base) -> SerializationStrategy<Base>?
    ) {
        // Nothing here
    }

    override fun <Base : Any> polymorphicDefaultDeserializer(
        baseClass: KClass<Base>,
        defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<Base>?
    ) {
        // Nothing here
    }
}
