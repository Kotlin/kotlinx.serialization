/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.descriptors

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*
import kotlin.reflect.*


/**
 * Retrieves [KClass] associated with serializer and its descriptor, if it was captured.
 *
 * For schema introspection purposes, [capturedKClass] can be used in [SerializersModule] as a key
 * to retrieve registered descriptor at runtime.
 * This property is intended to be used on [SerialKind.CONTEXTUAL] and [PolymorphicKind.OPEN] kinds of descriptors,
 * where actual serializer used for a property can be determined only at runtime.
 * Serializers which represent contextual serialization and open polymorphism (namely, [ContextualSerializer] and
 * [PolymorphicSerializer]) capture statically known KClass in a descriptor and can expose it via this property.
 *
 * This property is `null` for descriptors that are not of [SerialKind.CONTEXTUAL] or [PolymorphicKind.OPEN] kinds.
 * It _may_ be `null` for descriptors of these kinds, if captured class information is unavailable for various reasons.
 * It means that schema introspection should be performed in an application-specific manner.
 *
 * ### Example
 * Imagine we need to find all distinct properties names, which may occur in output after serializing a given class
 * with respect to [`@Contextual`][Contextual] annotation and all possible inheritors when the class is
 * serialized polymorphically.
 * Then we can write following function:
 * ```
 * fun allDistinctNames(descriptor: SerialDescriptor, module: SerialModule) = when (descriptor.kind) {
 *   is PolymorphicKind.OPEN -> module.getPolymorphicDescriptors(descriptor)
 *     .map { it.elementNames() }.flatten().toSet()
 *   is SerialKind.CONTEXTUAL -> module.getContextualDescriptor(descriptor)
 *     ?.elementNames().orEmpty().toSet()
 *   else -> descriptor.elementNames().toSet()
 * }
 * ```
 * @see SerializersModule.getContextualDescriptor
 * @see SerializersModule.getPolymorphicDescriptors
 */
@ExperimentalSerializationApi
public val SerialDescriptor.capturedKClass: KClass<*>?
    get() = when (this) {
        is ContextDescriptor -> kClass
        is SerialDescriptorForNullable -> original.capturedKClass
        else -> null
    }

/**
 * Looks up a descriptor of serializer registered for contextual serialization in [this],
 * using [SerialDescriptor.capturedKClass] as a key.
 *
 * @see SerializersModuleBuilder.contextual
 */
@ExperimentalSerializationApi
public fun SerializersModule.getContextualDescriptor(descriptor: SerialDescriptor): SerialDescriptor? =
    descriptor.capturedKClass?.let { klass -> getContextual(klass)?.descriptor }

/**
 * Retrieves a collection of descriptors which serializers are registered for polymorphic serialization in [this]
 * with base class equal to [descriptor]'s [SerialDescriptor.capturedKClass].
 * This method does not retrieve serializers registered with [PolymorphicModuleBuilder.default].
 *
 * @see SerializersModule.getPolymorphic
 * @see SerializersModuleBuilder.polymorphic
 */
@ExperimentalSerializationApi
public fun SerializersModule.getPolymorphicDescriptors(descriptor: SerialDescriptor): List<SerialDescriptor> {
    val kClass = descriptor.capturedKClass ?: return emptyList()
    // SerializersModule is sealed class with the only implementation
    return (this as SerialModuleImpl).polyBase2Serializers[kClass]?.values.orEmpty().map { it.descriptor }
}

/**
 * Wraps [this] in [ContextDescriptor].
 */
internal fun SerialDescriptor.withContext(context: KClass<*>): SerialDescriptor =
    ContextDescriptor(this, context)

/**
 * Descriptor that captures [kClass] and allows retrieving additional runtime information,
 * if proper [SerializersModule] is provided.
 */
@OptIn(ExperimentalSerializationApi::class)
private class ContextDescriptor(
    private val original: SerialDescriptor,
    @JvmField val kClass: KClass<*>
) : SerialDescriptor by original {
    override val serialName = "${original.serialName}<${kClass.simpleName}>"

    override fun equals(other: Any?): Boolean {
        val another = other as? ContextDescriptor ?: return false
        return original == another.original && another.kClass == this.kClass
    }

    override fun hashCode(): Int {
        var result = kClass.hashCode()
        result = 31 * result + serialName.hashCode()
        return result
    }

    override fun toString(): String {
        return "ContextDescriptor(kClass: $kClass, original: $original)"
    }
}
