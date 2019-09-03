/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.defaultSerializer
import kotlin.reflect.KClass

@ImplicitReflectionSerializer
fun <T : Any> KClass<T>.serializer(): KSerializer<T> = compiledSerializer() ?: defaultSerializer()
    ?: throw SerializationException("Can't locate argument-less serializer for $this. For generic classes, such as lists, please provide serializer explicitly.")

@ImplicitReflectionSerializer
expect fun <T : Any> KClass<T>.compiledSerializer(): KSerializer<T>?

expect fun String.toUtf8Bytes(): ByteArray
expect fun stringFromUtf8Bytes(bytes: ByteArray): String

expect fun <E: Enum<E>> enumFromName(enumClass: KClass<E>, value: String): E
expect fun <E: Enum<E>> enumFromOrdinal(enumClass: KClass<E>, ordinal: Int): E

expect fun <E: Enum<E>> KClass<E>.enumClassName(): String
expect fun <E: Enum<E>> KClass<E>.enumMembers(): Array<E>

expect fun <T: Any, E: T?> ArrayList<E>.toNativeArray(eClass: KClass<T>): Array<E>

/**
 * Checks whether the receiver is an instance of a given kclass.
 *
 * This check is a replacement for [KClass.isInstance] because on JVM it requires kotlin-reflect.jar in classpath (see KT-14720).
 *
 * On JS and Native, this function delegates to aforementioned [KClass.isInstance] since it is supported there out-of-the-box;
 * on JVM, it falls back to java.lang.Class.isInstance which causes difference when applied to function types with big arity.
 */
internal expect fun Any.isInstanceOf(kclass: KClass<*>): Boolean

/**
 * Returns simple name (a last part of FQ name) of [this] kclass or null if class is local or anonymous.
 *
 * In contrary to [KClass.simpleName], does not require kotlin-reflect.jar on JVM (see KT-33646).
 * On JVM, it uses `java.lang.Class.getSimpleName()` (therefore does not work for local classes and other edge cases).
 */
internal expect fun <T : Any> KClass<T>.simpleName(): String?
