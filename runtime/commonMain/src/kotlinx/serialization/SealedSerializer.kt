/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlin.reflect.*

/**
 * This class provides support for multiplatform polymorphic serialization of sealed classes.
 *
 * In contrary to [PolymorphicSerializer], all known subclasses with serializers must be passed
 * in `subclasses` and `subSerializers` constructor parameters.
 * If a subclass is a sealed class itself, all its subclasses are registered as well.
 * In most of the cases, you won't need to perform any manual setup:
 *
 * ```
 * @Serializable
 * sealed class SimpleSealed {
 *     @Serializable
 *     public data class SubSealedA(val s: String) : SimpleSealed()
 *
 *     @Serializable
 *     public data class SubSealedB(val i: Int) : SimpleSealed()
 * }
 *
 * // will perform correct polymorphic serialization and deserialization:
 * Json.stringify(SimpleSealed.serializer(), SubSealedA("foo"))
 * ```
 *
 * However, it is possible to register additional subclasses using regular [SerializersModule].
 * It is required when one of the subclasses is an abstract class itself:
 *
 * ```
 * @Serializable
 * sealed class ProtocolWithAbstractClass {
 *     @Serializable
 *     abstract class Message : ProtocolWithAbstractClass() {
 *         @Serializable
 *         data class StringMessage(val description: String, val message: String) : Message()
 *
 *         @Serializable
 *         data class IntMessage(val description: String, val message: Int) : Message()
 *     }
 *
 *     @Serializable
 *     data class ErrorMessage(val error: String) : ProtocolWithAbstractClass()
 * }
 * ```
 *
 * In this case, `ErrorMessage` would be registered automatically by the plugin,
 * but `StringMessage` and `IntMessage` require manual registration, as described in [PolymorphicSerializer] documentation:
 *
 * ```
 * val abstractContext = SerializersModule {
 *     polymorphic(ProtocolWithAbstractClass::class, ProtocolWithAbstractClass.Message::class) {
 *         ProtocolWithAbstractClass.Message.IntMessage::class with ProtocolWithAbstractClass.Message.IntMessage.serializer()
 *         ProtocolWithAbstractClass.Message.StringMessage::class with ProtocolWithAbstractClass.Message.StringMessage.serializer()
 *         // no need to register ProtocolWithAbstractClass.ErrorMessage
 *     }
 * }
 * ```
 */
@InternalSerializationApi
public class SealedClassSerializer<T : Any>(
    serialName: String,
    override val baseClass: KClass<T>,
    subclasses: Array<KClass<out T>>,
    subclassSerializers: Array<KSerializer<out T>>
) : AbstractPolymorphicSerializer<T>() {

    override val descriptor: SerialDescriptor =
        SealedClassDescriptor(serialName, subclassSerializers.map { it.descriptor })

    private val class2Serializer: Map<KClass<out T>, KSerializer<out T>>
    private val serialName2Serializer: Map<String, KSerializer<out T>>
    private val serializer2AllProperties: Map<KSerializer<*>, Set<String>>

    init {
        require(subclasses.size == subclassSerializers.size) {
            "Arrays of classes and serializers must have the same length," +
                    " got arrays: ${subclasses.contentToString()}, ${subclassSerializers.contentToString()}"
        }
        class2Serializer = subclasses.zip(subclassSerializers).toMap()
        serialName2Serializer = class2Serializer.entries.groupingBy { it.value.descriptor.serialName }
            .aggregate<Map.Entry<KClass<out T>, KSerializer<out T>>, String, Map.Entry<KClass<*>, KSerializer<out T>>>
            { key, accumulator, element, _ ->
                if (accumulator != null) {
                    error("Multiple sealed subclasses of $baseClass have the same serial name '$key: ${accumulator.key}, ${element.key}")
                }
                element
            }.mapValues { it.value.value }
        serializer2AllProperties = subclassSerializers.associate { it to it.descriptor.elementNames().toSet() }
    }

    override fun findPolymorphicSerializer(decoder: CompositeDecoder, klassName: String): KSerializer<out T> {
        return serialName2Serializer[klassName] ?: super.findPolymorphicSerializer(decoder, klassName)
    }

    override fun findPolymorphicSerializer(encoder: Encoder, value: T): KSerializer<out T> {
        return class2Serializer[value::class] ?: super.findPolymorphicSerializer(encoder, value)
    }

    internal fun validate(actualSerializer: KSerializer<Any>, classDiscriminator: String) {
        val properties = serializer2AllProperties[actualSerializer] ?: return
        if (classDiscriminator !in properties) return
        val clazz = class2Serializer.entries.first { it.value === actualSerializer }.key
        error(
            "Sealed $clazz cannot be serialized as base class $baseClass because it has property name that conflicts with JSON class discriminator. " +
                    "You can either change class discriminator in JsonConfiguration, " +
                    "rename property with @SerialName annotation " +
                    "or fallback to array polymorphism"
        )
    }
}

/**
 * Descriptor for sealed class contains descriptors for all its serializable inheritors
 * which can be obtained via [getElementDescriptor].
 */
internal class SealedClassDescriptor(
    name: String,
    elementDescriptors: List<SerialDescriptor>
) : SerialClassDescImpl(name, elementsCount = elementDescriptors.size) {
    override val kind: SerialKind = PolymorphicKind.SEALED

    init {
        elementDescriptors.forEach {
            addElement(it.serialName)
            pushDescriptor(it)
        }
    }
}
