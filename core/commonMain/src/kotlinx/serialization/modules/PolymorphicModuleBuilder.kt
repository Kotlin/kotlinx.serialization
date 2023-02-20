/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.reflect.*

/**
 * A builder which registers all its content for polymorphic serialization in the scope of the [base class][baseClass].
 * If [baseSerializer] is present, registers it as a serializer for [baseClass] (which will be used if base class is serializable).
 * Subclasses and its serializers can be added with [subclass] builder function.
 *
 * To obtain an instance of this builder, use [SerializersModuleBuilder.polymorphic] DSL function.
 */
public class PolymorphicModuleBuilder<in Base : Any> @PublishedApi internal constructor(
    private val baseClass: KClass<Base>,
    private val baseSerializer: KSerializer<Base>? = null
) {
    private val subclasses: MutableList<Pair<KClass<out Base>, KSerializer<out Base>>> = mutableListOf()
    private var defaultSerializerProvider: ((Base) -> SerializationStrategy<Base>?)? = null
    private var defaultDeserializerProvider: ((String?) -> DeserializationStrategy<Base>?)? = null

    /**
     * Registers the subclasses of the given class as subclasses of the outer class. Currently this requires `baseClass`
     * to be sealed.
     */
    public fun <T: Base> subclassesOf(baseClass: KClass<T>, serializer: KSerializer<T>) {
        require(serializer is SealedClassSerializer) {
            "subClassesOf only supports automatic adding of subclasses of sealed types."
        }
        for ((subsubclass, subserializer) in serializer.class2Serializer.entries) {
            @Suppress("UNCHECKED_CAST")
            // We don't know the type here, but it matches if correct in the sealed serializer.
            subclass(subsubclass as KClass<T>, subserializer as KSerializer<T>)
        }
    }

    /**
     * Registers a [subclass] [serializer] in the resulting module under the [base class][Base].
     */
    public fun <T : Base> subclass(subclass: KClass<T>, serializer: KSerializer<T>) {
        subclasses.add(subclass to serializer)
    }

    /**
     * Adds a default serializers provider associated with the given [baseClass] to the resulting module.
     * [defaultDeserializerProvider] is invoked when no polymorphic serializers associated with the `className`
     * were found. `className` could be `null` for formats that support nullable class discriminators
     * (currently only `Json` with `JsonBuilder.useArrayPolymorphism` set to `false`)
     *
     * Default deserializers provider affects only deserialization process. To affect serialization process, use
     * [SerializersModuleBuilder.polymorphicDefaultSerializer].
     *
     * [defaultDeserializerProvider] can be stateful and lookup a serializer for the missing type dynamically.
     *
     * Typically, if the class is not registered in advance, it is not possible to know the structure of the unknown
     * type and have a precise serializer, so the default serializer has limited capabilities.
     * If you're using `Json` format, you can get a structural access to the unknown data using `JsonContentPolymorphicSerializer`.
     *
     * @see SerializersModuleBuilder.polymorphicDefaultSerializer
     */
    public fun defaultDeserializer(defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<Base>?) {
        require(this.defaultDeserializerProvider == null) {
            "Default deserializer provider is already registered for class $baseClass: ${this.defaultDeserializerProvider}"
        }
        this.defaultDeserializerProvider = defaultDeserializerProvider
    }

    /**
     * Adds a default deserializers provider associated with the given [baseClass] to the resulting module.
     * This function affect only deserialization process. To avoid confusion, it was deprecated and replaced with [defaultDeserializer].
     * To affect serialization process, use [SerializersModuleBuilder.polymorphicDefaultSerializer].
     *
     * [defaultSerializerProvider] is invoked when no polymorphic serializers associated with the `className`
     * were found. `className` could be `null` for formats that support nullable class discriminators
     * (currently only `Json` with `JsonBuilder.useArrayPolymorphism` set to `false`)
     *
     * [defaultSerializerProvider] can be stateful and lookup a serializer for the missing type dynamically.
     *
     * Typically, if the class is not registered in advance, it is not possible to know the structure of the unknown
     * type and have a precise serializer, so the default serializer has limited capabilities.
     * If you're using `Json` format, you can get a structural access to the unknown data using `JsonContentPolymorphicSerializer`.
     *
     * @see defaultDeserializer
     * @see SerializersModuleBuilder.polymorphicDefaultSerializer
     */
    @Deprecated(
        "Deprecated in favor of function with more precise name: defaultDeserializer",
        ReplaceWith("defaultDeserializer(defaultSerializerProvider)"),
        DeprecationLevel.WARNING // Since 1.5.0. Raise to ERROR in 1.6.0, hide in 1.7.0
    )
    public fun default(defaultSerializerProvider: (className: String?) -> DeserializationStrategy<Base>?) {
        defaultDeserializer(defaultSerializerProvider)
    }

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun buildTo(builder: SerializersModuleBuilder) {
        if (baseSerializer != null) builder.registerPolymorphicSerializer(baseClass, baseClass, baseSerializer)
        subclasses.forEach { (kclass, serializer) ->
            builder.registerPolymorphicSerializer(
                baseClass,
                kclass as KClass<Base>,
                serializer.cast()
            )
        }

        val defaultSerializer = defaultSerializerProvider
        if (defaultSerializer != null) {
            builder.registerDefaultPolymorphicSerializer(baseClass, defaultSerializer, false)
        }

        val defaultDeserializer = defaultDeserializerProvider
        if (defaultDeserializer != null) {
            builder.registerDefaultPolymorphicDeserializer(baseClass, defaultDeserializer, false)
        }
    }
}

/**
 * Registers a [subclass] [serializer] in the resulting module under the [base class][Base].
 */
public inline fun <Base : Any, reified T : Base> PolymorphicModuleBuilder<Base>.subclass(serializer: KSerializer<T>): Unit =
    subclass(T::class, serializer)

/**
 * Registers a serializer for class [T] in the resulting module under the [base class][Base].
 */
public inline fun <Base : Any, reified T : Base> PolymorphicModuleBuilder<Base>.subclass(clazz: KClass<T>): Unit =
    subclass(clazz, serializer())

/**
 * Registers the child serializers for the sealed [subclass] [serializer] in the resulting module under the [base class][Base].
 */
public inline fun <Base : Any, reified T : Base> PolymorphicModuleBuilder<Base>.subclassesOf(serializer: KSerializer<T>): Unit =
    subclassesOf(T::class, serializer)

/**
 * Registers the child serializers for the sealed class [T] in the resulting module under the [base class][Base].
 */
public inline fun <Base : Any, reified T : Base> PolymorphicModuleBuilder<Base>.subclassesOf(clazz: KClass<T>): Unit =
    subclassesOf(clazz, serializer())
