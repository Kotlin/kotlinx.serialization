/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("DEPRECATION_ERROR")

package kotlinx.serialization.builtins

import kotlinx.serialization.*
import kotlinx.serialization.internal.*

/**
 * Returns a nullable serializer for the given serializer of non-null type.
 */
public val <T : Any> KSerializer<T>.nullable: KSerializer<T?>
    get() {
        @Suppress("UNCHECKED_CAST")
        return if (descriptor.isNullable) (this as KSerializer<T?>) else NullableSerializer(this)
    }

/**
 * Returns built-in serializer for Kotlin [Pair].
 * Resulting serializer represents pair as a structure of two key-value pairs.
 */
public fun <K, V> PairSerializer(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>
): KSerializer<Pair<K, V>> = kotlinx.serialization.internal.PairSerializer(keySerializer, valueSerializer)

/**
 * Returns built-in serializer for [Map.Entry].
 * Resulting serializer represents entry as a structure with a single key-value pair.
 * E.g. `Pair(1, 2)` and `Map.Entry(1, 2)` will be serialized to JSON as
 * `{"first": 1, "second": 2}` and {"1": 2} respectively.
 */
public fun <K, V> MapEntrySerializer(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>
): KSerializer<Map.Entry<K, V>> = kotlinx.serialization.internal.MapEntrySerializer(keySerializer, valueSerializer)

/**
 * Returns built-in serializer for Kotlin [Triple].
 * Resulting serializer represents triple as a structure of three key-value pairs.
 */
public fun <A, B, C> TripleSerializer(
    aSerializer: KSerializer<A>,
    bSerializer: KSerializer<B>,
    cSerializer: KSerializer<C>
): KSerializer<Triple<A, B, C>> = kotlinx.serialization.internal.TripleSerializer(aSerializer, bSerializer, cSerializer)

