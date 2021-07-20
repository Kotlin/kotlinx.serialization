/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlin.reflect.*

/**
 * This class provides support for multiplatform polymorphic serialization of sealed classes.
 *
 * In contrary to [PolymorphicSerializer], all known subclasses with serializers must be passed
 * in `subclasses` and `subSerializers` constructor parameters.
 * If a subclass is a sealed class itself, all its subclasses are registered as well.
 *
 * If a sealed hierarchy is marked with [@Serializable][Serializable], an instance of this class is provided automatically.
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
 * Json.encodeToString(SimpleSealed.serializer(), SubSealedA("foo"))
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
 *     polymorphic(ProtocolWithAbstractClass::class) {
 *         subclass(ProtocolWithAbstractClass.Message.IntMessage::class)
 *         subclass(ProtocolWithAbstractClass.Message.StringMessage::class)
 *         // no need to register ProtocolWithAbstractClass.ErrorMessage
 *     }
 * }
 * ```
 */
@InternalSerializationApi
@OptIn(ExperimentalSerializationApi::class)
public class SealedClassSerializer<T : Any>(
    serialName: String,
    override val baseClass: KClass<T>,
    subclasses: Array<KClass<out T>>,
    subclassSerializers: Array<KSerializer<out T>>
) : AbstractPolymorphicSerializer<T>() {

    @PublishedApi
    internal constructor(
        serialName: String,
        baseClass: KClass<T>,
        subclasses: Array<KClass<out T>>,
        subclassSerializers: Array<KSerializer<out T>>,
        classAnnotations: Array<Annotation>
    ) : this(serialName, baseClass, subclasses, subclassSerializers) {
        (this.descriptor as SerialDescriptorImpl).annotations = classAnnotations.asList()
    }

    override val descriptor: SerialDescriptor = buildSerialDescriptor(serialName, PolymorphicKind.SEALED) {
        element("type", String.serializer().descriptor)
        val elementDescriptor =
            buildSerialDescriptor("kotlinx.serialization.Sealed<${baseClass.simpleName}>", SerialKind.CONTEXTUAL) {
                subclassSerializers.forEach {
                    val d = it.descriptor
                    element(d.serialName, d)
                }
            }
        element("value", elementDescriptor)
    }

    private val class2Serializer: Map<KClass<out T>, KSerializer<out T>>
    private val serialName2Serializer: Map<String, KSerializer<out T>>

    init {
        if (subclasses.size != subclassSerializers.size) {
            throw IllegalArgumentException("All subclasses of sealed class ${baseClass.simpleName} should be marked @Serializable")
        }

        class2Serializer = subclasses.zip(subclassSerializers).toMap()
        serialName2Serializer = class2Serializer.entries.groupingBy { it.value.descriptor.serialName }
            .aggregate<Map.Entry<KClass<out T>, KSerializer<out T>>, String, Map.Entry<KClass<*>, KSerializer<out T>>>
            { key, accumulator, element, _ ->
                if (accumulator != null) {
                    error(
                        "Multiple sealed subclasses of '$baseClass' have the same serial name '$key':" +
                                " '${accumulator.key}', '${element.key}'"
                    )
                }
                element
            }.mapValues { it.value.value }
    }

    override fun findPolymorphicSerializerOrNull(decoder: CompositeDecoder, klassName: String?): DeserializationStrategy<out T>? {
        return serialName2Serializer[klassName] ?: super.findPolymorphicSerializerOrNull(decoder, klassName)
    }

    override fun findPolymorphicSerializerOrNull(encoder: Encoder, value: T): SerializationStrategy<T>? {
        return (class2Serializer[value::class] ?: super.findPolymorphicSerializerOrNull(encoder, value))?.cast()
    }
}
