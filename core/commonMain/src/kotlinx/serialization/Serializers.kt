/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("DEPRECATION_ERROR", "UNCHECKED_CAST")
@file:JvmMultifileClass
@file:JvmName("SerializersKt")

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.builtins.MapEntrySerializer
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.builtins.TripleSerializer
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*
import kotlin.reflect.*

/**
 * Retrieves a serializer for the given type [T].
 * This overload is a reified version of `serializer(KType)`.
 *
 * This overload works with full type information, including type arguments and nullability,
 * and is a recommended way to retrieve a serializer.
 * For example, `serializer<List<String?>>()` returns [KSerializer] that is able
 * to serialize and deserialize list of nullable strings — i.e. `ListSerializer(String.serializer().nullable)`.
 *
 * Variance of [T]'s type arguments is not used by the serialization and is not taken into account.
 * Star projections in [T]'s type arguments are prohibited.
 *
 * @throws SerializationException if serializer cannot be created (provided [T] or its type argument is not serializable).
 * @throws IllegalArgumentException if any of [T]'s type arguments contains star projection
 */
public inline fun <reified T> serializer(): KSerializer<T> {
    return serializer(typeOf<T>()).cast()
}

/**
 * Retrieves default serializer for the given type [T] and,
 * if [T] is not serializable, fallbacks to [contextual][SerializersModule.getContextual] lookup.
 *
 * This overload works with full type information, including type arguments and nullability,
 * and is a recommended way to retrieve a serializer.
 * For example, `serializer<List<String?>>()` returns [KSerializer] that is able
 * to serialize and deserialize list of nullable strings — i.e. `ListSerializer(String.serializer().nullable)`.
 *
 * Variance of [T]'s type arguments is not used by the serialization and is not taken into account.
 * Star projections in [T]'s type arguments are prohibited.
 *
 * @throws SerializationException if serializer cannot be created (provided [T] or its type argument is not serializable).
 * @throws IllegalArgumentException if any of [T]'s type arguments contains star projection
 */
public inline fun <reified T> SerializersModule.serializer(): KSerializer<T> {
    return serializer(typeOf<T>()).cast()
}

/**
 * Creates a serializer for the given [type].
 * [type] argument is usually obtained with [typeOf] method.
 *
 * This overload works with full type information, including type arguments and nullability,
 * and is a recommended way to retrieve a serializer.
 * For example, `serializer<typeOf<List<String?>>>()` returns [KSerializer] that is able
 * to serialize and deserialize list of nullable strings — i.e. `ListSerializer(String.serializer().nullable)`.
 *
 * Variance of [type]'s type arguments is not used by the serialization and is not taken into account.
 * Star projections in [type]'s arguments are prohibited.
 *
 * @throws SerializationException if serializer cannot be created (provided [type] or its type argument is not serializable).
 * @throws IllegalArgumentException if any of [type]'s arguments contains star projection
 */
public fun serializer(type: KType): KSerializer<Any?> = EmptySerializersModule().serializer(type)


/**
 * Retrieves serializer for the given [kClass].
 * This method uses platform-specific reflection available.
 *
 * If [kClass] is a parametrized type then it is necessary to pass serializers for generic parameters in the [typeArgumentsSerializers].
 * The nullability of returned serializer is specified using the [isNullable].
 *
 * Note that it is impossible to create an array serializer with this method,
 * as array serializer needs additional information: type token for an element type.
 * To create array serializer, use overload with [KType] or [ArraySerializer] directly.
 *
 * Caching on JVM platform is disabled for this function, so it may work slower than an overload with [KType].
 *
 * @throws SerializationException if serializer cannot be created (provided [kClass] or its type argument is not serializable)
 * @throws SerializationException if [kClass] is a `kotlin.Array`
 * @throws SerializationException if size of [typeArgumentsSerializers] does not match the expected generic parameters count
 */
@ExperimentalSerializationApi
public fun serializer(
    kClass: KClass<*>,
    typeArgumentsSerializers: List<KSerializer<*>>,
    isNullable: Boolean
): KSerializer<Any?> = EmptySerializersModule().serializer(kClass, typeArgumentsSerializers, isNullable)

/**
 * Creates a serializer for the given [type] if possible.
 * [type] argument is usually obtained with [typeOf] method.
 *
 * This overload works with full type information, including type arguments and nullability,
 * and is a recommended way to retrieve a serializer.
 * For example, `serializerOrNull<typeOf<List<String?>>>()` returns [KSerializer] that is able
 * to serialize and deserialize list of nullable strings — i.e. `ListSerializer(String.serializer().nullable)`.
 *
 * Variance of [type]'s arguments is not used by the serialization and is not taken into account.
 * Star projections in [type]'s arguments are prohibited.
 *
 * @return [KSerializer] for the given [type] or `null` if serializer cannot be created (given [type] or its type argument is not serializable).
 * @throws IllegalArgumentException if any of [type]'s arguments contains star projection
 */
public fun serializerOrNull(type: KType): KSerializer<Any?>? = EmptySerializersModule().serializerOrNull(type)

/**
 * Retrieves default serializer for the given [type] and,
 * if [type] is not serializable, fallbacks to [contextual][SerializersModule.getContextual] lookup.
 * [type] argument is usually obtained with [typeOf] method.
 *
 * This overload works with full type information, including type arguments and nullability,
 * and is a recommended way to retrieve a serializer.
 * For example, `serializer<typeOf<List<String?>>>()` returns [KSerializer] that is able
 * to serialize and deserialize list of nullable strings — i.e. `ListSerializer(String.serializer().nullable)`.
 *
 * Variance of [type]'s arguments is not used by the serialization and is not taken into account.
 * Star projections in [type]'s arguments are prohibited.
 *
 * @throws SerializationException if serializer cannot be created (provided [type] or its type argument is not serializable and is not registered in [this] module).
 * @throws IllegalArgumentException if any of [type]'s arguments contains star projection
 */
public fun SerializersModule.serializer(type: KType): KSerializer<Any?> =
    serializerByKTypeImpl(type, failOnMissingTypeArgSerializer = true) ?: type.kclass()
        .platformSpecificSerializerNotRegistered()


/**
 * Retrieves serializer for the given [kClass] and,
 * if [kClass] is not serializable, fallbacks to [contextual][SerializersModule.getContextual] lookup.
 * This method uses platform-specific reflection available.
 *
 * If [kClass] is a parametrized type then it is necessary to pass serializers for generic parameters in the [typeArgumentsSerializers].
 * The nullability of returned serializer is specified using the [isNullable].
 *
 * Note that it is impossible to create an array serializer with this method,
 * as array serializer needs additional information: type token for an element type.
 * To create array serializer, use overload with [KType] or [ArraySerializer] directly.
 *
 * Caching on JVM platform is disabled for this function, so it may work slower than an overload with [KType].
 *
 * @throws SerializationException if serializer cannot be created (provided [kClass] or its type argument is not serializable and is not registered in [this] module)
 * @throws SerializationException if [kClass] is a `kotlin.Array`
 * @throws SerializationException if size of [typeArgumentsSerializers] does not match the expected generic parameters count
 */
@ExperimentalSerializationApi
public fun SerializersModule.serializer(
    kClass: KClass<*>,
    typeArgumentsSerializers: List<KSerializer<*>>,
    isNullable: Boolean
): KSerializer<Any?> =
    serializerByKClassImpl(kClass as KClass<Any>, typeArgumentsSerializers as List<KSerializer<Any?>>, isNullable)
        ?: kClass.platformSpecificSerializerNotRegistered()

/**
 * Retrieves default serializer for the given [type] and,
 * if [type] is not serializable, fallbacks to [contextual][SerializersModule.getContextual] lookup.
 * [type] argument is usually obtained with [typeOf] method.
 *
 * This overload works with full type information, including type arguments and nullability,
 * and is a recommended way to retrieve a serializer.
 * For example, `serializerOrNull<typeOf<List<String?>>>()` returns [KSerializer] that is able
 * to serialize and deserialize list of nullable strings — i.e. `ListSerializer(String.serializer().nullable)`.
 *
 * Variance of [type]'s arguments is not used by the serialization and is not taken into account.
 * Star projections in [type]'s arguments are prohibited.
 *
 * @return [KSerializer] for the given [type] or `null` if serializer cannot be created (given [type] or its type argument is not serializable and is not registered in [this] module).
 * @throws IllegalArgumentException if any of [type]'s arguments contains star projection
 */
public fun SerializersModule.serializerOrNull(type: KType): KSerializer<Any?>? =
    serializerByKTypeImpl(type, failOnMissingTypeArgSerializer = false)

@OptIn(ExperimentalSerializationApi::class)
private fun SerializersModule.serializerByKTypeImpl(
    type: KType,
    failOnMissingTypeArgSerializer: Boolean
): KSerializer<Any?>? {
    val rootClass = type.kclass()
    val isNullable = type.isMarkedNullable
    val typeArguments = type.arguments.map(KTypeProjection::typeOrThrow)

    val cachedSerializer = if (typeArguments.isEmpty()) {
        findCachedSerializer(rootClass, isNullable)
    } else {
        findParametrizedCachedSerializer(rootClass, typeArguments, isNullable).getOrNull()
    }
    cachedSerializer?.let { return it }

    // slow path to find contextual serializers in serializers module
    val contextualSerializer: KSerializer<out Any?>? = if (typeArguments.isEmpty()) {
        getContextual(rootClass)
    } else {
        val serializers = serializersForParameters(typeArguments, failOnMissingTypeArgSerializer) ?: return null
        // first, we look among the built-in serializers, because the parameter could be contextual
        rootClass.parametrizedSerializerOrNull(serializers) { typeArguments[0].classifier }
            ?: getContextual(
                rootClass,
                serializers
            )
    }
    return contextualSerializer?.cast<Any>()?.nullable(isNullable)
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerializersModule.serializerByKClassImpl(
    rootClass: KClass<Any>,
    typeArgumentsSerializers: List<KSerializer<Any?>>,
    isNullable: Boolean
): KSerializer<Any?>? {
    val serializer = if (typeArgumentsSerializers.isEmpty()) {
        rootClass.serializerOrNull() ?: getContextual(rootClass)
    } else {
        try {
            rootClass.parametrizedSerializerOrNull(typeArgumentsSerializers) {
                throw SerializationException("It is not possible to retrieve an array serializer using KClass alone, use KType instead or ArraySerializer factory")
            } ?: getContextual(
                rootClass,
                typeArgumentsSerializers
            )
        } catch (e: IndexOutOfBoundsException) {
            throw SerializationException("Unable to retrieve a serializer, the number of passed type serializers differs from the actual number of generic parameters", e)
        }
    }

    return serializer?.cast<Any>()?.nullable(isNullable)
}

/**
 * Returns null only if `failOnMissingTypeArgSerializer == false` and at least one parameter serializer not found.
 */
internal fun SerializersModule.serializersForParameters(
    typeArguments: List<KType>,
    failOnMissingTypeArgSerializer: Boolean
): List<KSerializer<Any?>>? {
    val serializers = if (failOnMissingTypeArgSerializer) {
        typeArguments.map { serializer(it) }
    } else {
        typeArguments.map { serializerOrNull(it) ?: return null }
    }
    return serializers
}

/**
 * Retrieves a [KSerializer] for the given [KClass].
 * The given class must be annotated with [Serializable] or be one of the built-in types.
 *
 * This method uses platform-specific reflection available for the given erased `KClass`
 * and is not recommended to use this method for anything, but last-ditch resort, e.g.
 * when all type info is lost, your application has crashed and it is the final attempt to log or send some serializable data.
 *
 * The recommended way to retrieve the serializer is inline [serializer] function and [`serializer(KType)`][serializer]
 *
 * This API is not guaranteed to work consistently across different platforms or
 * to work in cases that slightly differ from "plain @Serializable class" and have platform and reflection specific limitations.
 *
 * ### Constraints
 * This paragraph explains known (but not all!) constraints of the `serializer()` implementation.
 * Please note that they are not bugs, but implementation restrictions that we cannot workaround.
 *
 * * This method may behave differently on JVM, JS and Native because of runtime reflection differences
 * * Serializers for classes with generic parameters are ignored by this method
 * * External serializers generated with `Serializer(forClass = )` are not lookuped consistently
 * * Serializers for classes with named companion objects  are not lookuped consistently
 *
 * @throws SerializationException if serializer can't be found.
 */
@InternalSerializationApi
public fun <T : Any> KClass<T>.serializer(): KSerializer<T> = serializerOrNull() ?: serializerNotRegistered()

/**
 * Retrieves a [KSerializer] for the given [KClass] or returns `null` if none is found.
 * The given class must be annotated with [Serializable] or be one of the built-in types.
 * This method uses platform-specific reflection available for the given erased `KClass`
 * and it is not recommended to use this method for anything, but last-ditch resort, e.g.
 * when all type info is lost, your application has crashed and it is the final attempt to log or send some serializable data.
 *
 * This API is not guaranteed to work consistently across different platforms or
 * to work in cases that slightly differ from "plain @Serializable class".
 *
 * ### Constraints
 * This paragraph explains known (but not all!) constraints of the `serializerOrNull()` implementation.
 * Please note that they are not bugs, but implementation restrictions that we cannot workaround.
 *
 * * This method may behave differently on JVM, JS and Native because of runtime reflection differences
 * * Serializers for classes with generic parameters are ignored by this method
 * * External serializers generated with `Serializer(forClass = )` are not lookuped consistently
 * * Serializers for classes with named companion objects  are not lookuped consistently
 */
@InternalSerializationApi
public fun <T : Any> KClass<T>.serializerOrNull(): KSerializer<T>? =
    compiledSerializerImpl() ?: builtinSerializerOrNull()

internal fun KClass<Any>.parametrizedSerializerOrNull(
    serializers: List<KSerializer<Any?>>,
    elementClassifierIfArray: () -> KClassifier?
): KSerializer<out Any>? {
    // builtin first because some standard parametrized interfaces (e.g. Map) must use builtin serializer but not polymorphic
    return builtinParametrizedSerializer(serializers, elementClassifierIfArray) ?: compiledParametrizedSerializer(serializers)
}


private fun KClass<Any>.compiledParametrizedSerializer(serializers: List<KSerializer<Any?>>): KSerializer<out Any>? {
    return constructSerializerForGivenTypeArgs(*serializers.toTypedArray())
}

@OptIn(ExperimentalSerializationApi::class)
private fun KClass<Any>.builtinParametrizedSerializer(
    serializers: List<KSerializer<Any?>>,
    elementClassifierIfArray: () -> KClassifier?
): KSerializer<out Any>? {
    return when (this) {
        Collection::class, List::class, MutableList::class, ArrayList::class -> ArrayListSerializer(serializers[0])
        HashSet::class -> HashSetSerializer(serializers[0])
        Set::class, MutableSet::class, LinkedHashSet::class -> LinkedHashSetSerializer(serializers[0])
        HashMap::class -> HashMapSerializer(serializers[0], serializers[1])
        Map::class, MutableMap::class, LinkedHashMap::class -> LinkedHashMapSerializer(
            serializers[0],
            serializers[1]
        )

        Map.Entry::class -> MapEntrySerializer(serializers[0], serializers[1])
        Pair::class -> PairSerializer(serializers[0], serializers[1])
        Triple::class -> TripleSerializer(serializers[0], serializers[1], serializers[2])
        else -> {
            if (isReferenceArray(this)) {
                ArraySerializer(elementClassifierIfArray() as KClass<Any>, serializers[0])
            } else {
                null
            }
        }
    }
}

private fun <T : Any> KSerializer<T>.nullable(shouldBeNullable: Boolean): KSerializer<T?> {
    if (shouldBeNullable) return nullable
    return this as KSerializer<T?>
}


/**
 * Overloads of [noCompiledSerializer] should never be called directly.
 * Instead, compiler inserts calls to them when intrinsifying [serializer] function.
 *
 * If no serializer has been found in compile time, call to [noCompiledSerializer] inserted instead.
 */
@Suppress("unused")
@PublishedApi
internal fun noCompiledSerializer(forClass: String): KSerializer<*> =
    throw SerializationException(notRegisteredMessage(forClass))

// Used when compiler intrinsic is inserted
@OptIn(ExperimentalSerializationApi::class)
@Suppress("unused")
@PublishedApi
internal fun noCompiledSerializer(module: SerializersModule, kClass: KClass<*>): KSerializer<*> {
    return module.getContextual(kClass) ?: kClass.serializerNotRegistered()
}

@OptIn(ExperimentalSerializationApi::class)
@Suppress("unused")
@PublishedApi
internal fun noCompiledSerializer(
    module: SerializersModule,
    kClass: KClass<*>,
    argSerializers: Array<KSerializer<*>>
): KSerializer<*> {
    return module.getContextual(kClass, argSerializers.asList()) ?: kClass.serializerNotRegistered()
}
