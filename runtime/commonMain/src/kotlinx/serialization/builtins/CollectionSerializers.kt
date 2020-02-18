/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.builtins

import kotlinx.serialization.*
import kotlinx.serialization.internal.*

/**
 * Creates a serializer for [`List<T>`][List] for the given serializer of type [T].
 */
public val <T> KSerializer<T>.list: KSerializer<List<T>>
    get() = ArrayListSerializer(this)

/**
 * Creates a serializer for [`List<T>`][List] for the given serializer of type [T].
 */
public fun <T> ListSerializer(elementSerializer: KSerializer<T>): KSerializer<List<T>> =
    ArrayListSerializer(elementSerializer)

/**
 * Reified version of [ListSerializer]
 */
@ImplicitReflectionSerializer
public inline fun <reified T> ListSerializer(): KSerializer<List<T>> = ListSerializer(serializer())

/**
 * Creates a serializer for [`Set<T>`][Set] for the given serializer of type [T].
 */
public val <T> KSerializer<T>.set: KSerializer<Set<T>>
    get() = LinkedHashSetSerializer(this)

/**
 * Creates a serializer for [`Set<T>`][Set] for the given serializer of type [T].
 */
public fun <T> SetSerializer(elementSerializer: KSerializer<T>): KSerializer<Set<T>> =
    LinkedHashSetSerializer(elementSerializer)

/**
 * Reified version of [SetSerializer]
 */
@ImplicitReflectionSerializer
public inline fun <reified T> SetSerializer(): KSerializer<Set<T>> = SetSerializer(serializer())

/**
 * Creates a serializer for [`Map<K, V>`][Map] for the given serializers for
 * its ket type [K] and value type [V].
 */
public fun <K, V> MapSerializer(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>
): KSerializer<Map<K, V>> = LinkedHashMapSerializer(keySerializer, valueSerializer)

/**
 * Reified version of [MapSerializer]
 */
@ImplicitReflectionSerializer
public inline fun <reified K, reified V> MapSerializer(): KSerializer<Map<K, V>> =
    MapSerializer(serializer(), serializer())
