/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
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
    private var defaultSerializerProvider: ((String?) -> DeserializationStrategy<out Base>?)? = null

    /**
     * Registers a [subclass] [serializer] in the resulting module under the [base class][Base].
     */
    public fun <T : Base> subclass(subclass: KClass<T>, serializer: KSerializer<T>) {
        subclasses.add(subclass to serializer)
    }

    /**
     * Adds a default serializers provider associated with the given [baseClass] to the resulting module.
     * [defaultSerializerProvider] is invoked when no polymorphic serializers associated with the `className`
     * were found. `className` could be `null` for formats that support nullable class discriminators
     * (currently only [Json] with [useArrayPolymorphism][JsonBuilder.useArrayPolymorphism] set to `false`)
     *
     * [defaultSerializerProvider] can be stateful and lookup a serializer for the missing type dynamically.
     *
     * Typically, if the class is not registered in advance, it is not possible to know the structure of the unknown
     * type and have a precise serializer, so the default serializer has limited capabilities.
     * To have a structural access to the unknown data, it is recommended to use [JsonTransformingSerializer]
     * or [JsonContentPolymorphicSerializer] classes.
     */
    public fun default(defaultSerializerProvider: (className: String?) -> DeserializationStrategy<out Base>?) {
        require(this.defaultSerializerProvider == null) {
            "Default serializer provider is already registered for class $baseClass: ${this.defaultSerializerProvider}"
        }
        this.defaultSerializerProvider = defaultSerializerProvider
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

        val default = defaultSerializerProvider
        if (default != null) {
            builder.registerDefaultPolymorphicSerializer(baseClass, default, false)
        }
    }

    @Deprecated(
        message = "Use 'subclass' instead",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("subclass<T>()")
    )
    public inline fun <reified T : Base> addSubclass(): Unit =
        subclass(T::class, serializer())

    @Deprecated(
        message = "Use 'subclass(serializer)' instead",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("subclass(serializer)")
    )
    public inline fun <reified T : Base> addSubclass(serializer: KSerializer<T>): Unit =
        subclass(T::class, serializer)


    @Deprecated(
        message = "Use 'subclass(subclass, serializer)' instead",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("subclass(subclass, serializer)")
    )
    public fun <T: Base> addSubclass(subclass: KClass<T>, serializer: KSerializer<T>): Unit = subclass(subclass, serializer)

    @Deprecated(
        message = "This method was deprecated during serialization 1.0 API stabilization",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("subclass(serializer)")
    )
    public infix fun <T : Base> KClass<T>.with(serializer: KSerializer<T>): Unit {
        subclass(this, serializer)
    }

    @Deprecated(
        message = "This method was deprecated during serialization 1.0 API stabilization",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("subclass(serializer<T>())")
    )
    public fun <T : Any> subclass(): Unit = TODO()
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
