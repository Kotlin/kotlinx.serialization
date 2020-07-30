/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("DEPRECATION_ERROR", "UNCHECKED_CAST")
@file:JvmMultifileClass
@file:JvmName("SerializersKt")

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*
import kotlin.reflect.*

/**
 * Retrieves a serializer for the given type [T].
 * This method is a reified version of `serializer(KType)`.
 */
public inline fun <reified T> serializer(): KSerializer<T> {
    return serializer(typeOf<T>()).cast()
}

/**
 * Retrieves serializer for the given type [T] from the current [SerializersModule] and,
 * if not found, fallbacks to plain [serializer] method.
 */
public inline fun <reified T> SerializersModule.serializer(): KSerializer<T> {
    return serializer(typeOf<T>()).cast()
}

/**
 * Creates a serializer for the given [type].
 * [type] argument can be obtained with experimental [typeOf] method.
 */
public fun serializer(type: KType): KSerializer<Any?> {
    val result = EmptySerializersModule.serializerByKTypeImpl(type)
    return result.nullable(type.isMarkedNullable)
}

/**
 * Retrieves serializer for the given [type] from the current [SerializersModule] and,
 * if not found, fallbacks to plain [serializer] method.
 * [type] argument can be obtained with experimental [typeOf] method.
 */
public fun SerializersModule.serializer(type: KType): KSerializer<Any?> {
    val kclass = type.kclass()
    val isNullable = type.isMarkedNullable
    getContextual(kclass)?.nullable(isNullable)?.let { return it.cast() }
    return serializerByKTypeImpl(type).nullable(isNullable).cast()
}

private fun SerializersModule.serializerByKTypeImpl(type: KType): KSerializer<Any> {
    val rootClass = type.kclass()
    val typeArguments = type.arguments
        .map { requireNotNull(it.type) { "Star projections in type arguments are not allowed, but had $type" } }
    return when {
        typeArguments.isEmpty() -> getContextual(rootClass) ?: rootClass.serializer()
        else -> builtinSerializer(typeArguments, rootClass)
    }.cast()
}

private fun SerializersModule.builtinSerializer(
    typeArguments: List<KType>,
    rootClass: KClass<Any>
): KSerializer<out Any> {
    val serializers = typeArguments
        .map(::serializer)
    // Array is not supported, see KT-32839
    return when (rootClass) {
        List::class, MutableList::class, ArrayList::class -> ArrayListSerializer(serializers[0])
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
            if (isReferenceArray(rootClass)) {
                return ArraySerializer<Any, Any?>(typeArguments[0].classifier as KClass<Any>, serializers[0]).cast()
            }
            requireNotNull(rootClass.constructSerializerForGivenTypeArgs(*serializers.toTypedArray())) {
                "Can't find a method to construct serializer for type ${rootClass.simpleName}. " +
                        "Make sure this class is marked as @Serializable or provide serializer explicitly."
            }
        }
    }
}

/**
 * Retrieves a [KSerializer] for the given [KClass].
 * The given class must be annotated with [Serializable] or be one of the built-in types.
 * It is not recommended to use this method for anything, but last-ditch resort, e.g.
 * when all type info is lost, your application has crashed and it is the final attempt to log or send some serializable data.
 *
 * The recommended way to retrieve the serializer is inline [serializer] function and [`serializer(KType)`][serializer]
 *
 * This API is not guaranteed to work consistent across different platforms or
 * to work in cases that slightly differ from "plain @Serializable class".
 *
 * @throws SerializationException if serializer can't be found.
 */
@InternalSerializationApi
public fun <T : Any> KClass<T>.serializer(): KSerializer<T> = serializerOrNull() ?: serializerNotRegistered()

/**
 * Retrieves a [KSerializer] for the given [KClass] or returns `null` if none is found.
 * The given class must be annotated with [Serializable] or be one of the built-in types.
 * It is not recommended to use this method for anything, but last-ditch resort, e.g.
 * when all type info is lost, your application has crashed and it is the final attempt to log or send some serializable data.
 *
 * This API is not guaranteed to work consistent across different platforms or
 * to work in cases that slightly differ from "plain @Serializable class".
 */
@InternalSerializationApi
public fun <T : Any> KClass<T>.serializerOrNull(): KSerializer<T>? =
    compiledSerializerImpl() ?: builtinSerializerOrNull()

private fun <T: Any> KSerializer<T>.nullable(shouldBeNullable: Boolean): KSerializer<T?> {
    if (shouldBeNullable) return nullable
    return this as KSerializer<T?>
}
