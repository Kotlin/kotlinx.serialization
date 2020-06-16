/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("DeprecatedCallableAddReplaceWith")

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlin.reflect.*

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
@UnsafeSerializationApi
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
@UnsafeSerializationApi
public fun <T : Any> KClass<T>.serializerOrNull(): KSerializer<T>? =
    compiledSerializerImpl() ?: builtinSerializerOrNull()


internal expect fun <T : Any> KClass<T>.compiledSerializerImpl(): KSerializer<T>?

internal expect fun <T : Any, E : T?> ArrayList<E>.toNativeArrayImpl(eClass: KClass<T>): Array<E>

/**
 * Checks whether the receiver is an instance of a given kclass.
 *
 * This check is a replacement for [KClass.isInstance] because on JVM it requires kotlin-reflect.jar in classpath (see KT-14720).
 *
 * On JS and Native, this function delegates to aforementioned [KClass.isInstance] since it is supported there out-of-the-box;
 * on JVM, it falls back to `java.lang.Class.isInstance`, which causes difference when applied to function types with big arity.
 */
internal expect fun Any.isInstanceOf(kclass: KClass<*>): Boolean

/**
 * Returns a simple name (a last part of FQ name) of [this] kclass or null if class is local or anonymous.
 *
 * In contrary to [KClass.simpleName], does not require kotlin-reflect.jar on JVM (see KT-33646).
 * On JVM, it uses `java.lang.Class.getSimpleName()` (therefore does not work for local classes and other edge cases).
 */
internal expect fun <T : Any> KClass<T>.simpleName(): String?
