/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("DEPRECATION_ERROR")

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
 * Creates a serializer for [`Map<K, V>`][Map] for the given serializers for
 * its ket type [K] and value type [V].
 */
public fun <K, V> MapSerializer(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>
): KSerializer<Map<K, V>> = LinkedHashMapSerializer(keySerializer, valueSerializer)
