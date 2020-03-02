/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.RequiresOptIn.Level.*
import kotlin.reflect.typeOf

/**
 * Marks declaration that obtain serializer implicitly using experimental [typeOf] function and/or reflection.
 *
 * These declarations have the following limitations:
 * - [typeOf] API does not work properly on K/JS with user-defined serializers.
 * - Reflection can't infer correct serializers for generic classes, like collections.
 * - Performance of reflective calls is usually worse than direct access to `.serializer`.
 *
 * It is recommended to specify serializer explicitly, using generated `.serializer()`
 * function on serializable class' companion.
 */
@RequiresOptIn
public annotation class ImplicitReflectionSerializer

/**
 * This annotation marks declarations with default parameters that are subject to semantic change without a migration path.
 *
 * For example, [JsonConfiguration.Default] marked as unstable, thus indicating that it can change its default values
 * in the next releases (e.g. disable strict-mode by default), leading to a semantic (not source code level) change.
 */
@RequiresOptIn(level = WARNING)
public annotation class UnstableDefault

@ImplicitReflectionSerializer
public inline fun <reified T : Any> BinaryFormat.dump(value: T): ByteArray =
    dump(context.getContextualOrDefault(), value)

@ImplicitReflectionSerializer
public inline fun <reified T : Any> BinaryFormat.dumps(value: T): String =
    dumps(context.getContextualOrDefault(), value)

@ImplicitReflectionSerializer
public inline fun <reified T : Any> BinaryFormat.load(raw: ByteArray): T =
    load(context.getContextualOrDefault(), raw)

@ImplicitReflectionSerializer
public inline fun <reified T : Any> BinaryFormat.loads(hex: String): T =
    loads(context.getContextualOrDefault(), hex)


@ImplicitReflectionSerializer
public inline fun <reified T : Any> StringFormat.stringify(value: T): String =
    stringify(context.getContextualOrDefault(), value)

@ImplicitReflectionSerializer
public inline fun <reified T : Any> StringFormat.stringify(objects: List<T>): String =
    stringify(context.getContextualOrDefault<T>().list, objects)

@ImplicitReflectionSerializer
public inline fun <reified K : Any, reified V : Any> StringFormat.stringify(map: Map<K, V>): String
        = stringify(MapSerializer(context.getContextualOrDefault<K>(), context.getContextualOrDefault<V>()), map)

@ImplicitReflectionSerializer
public inline fun <reified T : Any> StringFormat.parse(str: String): T =
    parse(context.getContextualOrDefault(), str)

@ImplicitReflectionSerializer
public inline fun <reified T : Any> StringFormat.parseList(objects: String): List<T> =
    parse(context.getContextualOrDefault<T>().list, objects)

@ImplicitReflectionSerializer
public inline fun <reified K : Any, reified V : Any> StringFormat.parseMap(map: String)
        = parse(MapSerializer(context.getContextualOrDefault<K>(), context.getContextualOrDefault<V>()), map)
