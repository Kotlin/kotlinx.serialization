/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("DEPRECATION_ERROR")

package kotlinx.serialization.builtins

import kotlinx.serialization.*

/**
 * Returns built-in serializer for Kotlin [Pair].
 * Resulting serializer represents pair as a structure of two key-value pairs.
 */
public fun <K, V> PairSerializer(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>
): KSerializer<Pair<K, V>> = kotlinx.serialization.internal.PairSerializer(keySerializer, valueSerializer)

/**
 * Reified version of [`PairSerializer(keySerializer, valueSerializer)`][PairSerializer]
 */
@ImplicitReflectionSerializer
public inline fun <reified K, reified V> PairSerializer(): KSerializer<Pair<K, V>> =
    PairSerializer(serializer(), serializer())

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
 * Reified version of [`MapEntrySerializer(keySerializer, valueSerializer)`][MapEntrySerializer]
 */
@ImplicitReflectionSerializer
public inline fun <reified K, reified V> MapEntrySerializer(): KSerializer<Map.Entry<K, V>> =
    MapEntrySerializer(serializer(), serializer())

/**
 * Returns built-in serializer for Kotlin [Triple].
 * Resulting serializer represents triple as a structure of three key-value pairs.
 */
public fun <A, B, C> TripleSerializer(
    aSerializer: KSerializer<A>,
    bSerializer: KSerializer<B>,
    cSerializer: KSerializer<C>
): KSerializer<Triple<A, B, C>> = kotlinx.serialization.internal.TripleSerializer(aSerializer, bSerializer, cSerializer)

/**
 * Reified version of [`TripleSerializer(aSerializer: KSerializer<A>,
 *     bSerializer: KSerializer<B>, cSerializer: KSerializer<C>)`][TripleSerializer].
 */
@ImplicitReflectionSerializer
public inline fun <reified A, reified B, reified C> TripleSerializer(): KSerializer<Triple<A, B, C>> =
    TripleSerializer(serializer(), serializer(), serializer())
