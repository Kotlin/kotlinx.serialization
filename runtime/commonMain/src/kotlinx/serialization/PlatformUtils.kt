/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("DeprecatedCallableAddReplaceWith")

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlin.reflect.*

/**
 * Retrieves a [KSerializer] for the given [KClass].
 * The class must be annotated as `@Serializable` or be one of the built-in types.
 *
 * This method has all the restrictions implied by [ImplicitReflectionSerializer], i.e.,
 * it will fail on an attempt to retrieve a serializer for generic class.
 * For classes with type parameters `serializer(KType)` should be used instead.
 */
@ImplicitReflectionSerializer
public fun <T : Any> KClass<T>.serializer(): KSerializer<T> = serializerOrNull()
        ?: throw SerializationException(
            "Can't locate argument-less serializer for class ${this.simpleName()}. " +
                    "For generic classes, such as lists, please provide serializer explicitly."
        )

/**
 * Retrieves a [KSerializer] for the given [KClass].
 * The class must be annotated as `@Serializable` or be one of the built-in types.
 *
 * This method has all the restrictions implied by [ImplicitReflectionSerializer].
 * It will return `null` in case serializer can't be obtained reflectively.
 */
@ImplicitReflectionSerializer
public fun <T : Any> KClass<T>.serializerOrNull(): KSerializer<T>? = compiledSerializerImpl()
        ?: builtinSerializerOrNull()


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
