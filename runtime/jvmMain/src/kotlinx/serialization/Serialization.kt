/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
@ImplicitReflectionSerializer
actual fun <T: Any> KClass<T>.compiledSerializer(): KSerializer<T>? = this.java.invokeSerializerGetter()

actual fun String.toUtf8Bytes() = this.toByteArray(Charsets.UTF_8)
actual fun stringFromUtf8Bytes(bytes: ByteArray) = String(bytes, Charsets.UTF_8)

actual fun <E: Enum<E>> enumFromName(enumClass: KClass<E>, value: String): E = java.lang.Enum.valueOf(enumClass.java, value)
actual fun <E: Enum<E>> enumFromOrdinal(enumClass: KClass<E>, ordinal: Int): E = enumClass.java.enumConstants[ordinal]

actual fun <E: Enum<E>> KClass<E>.enumClassName(): String = this.java.canonicalName ?: ""
actual fun <E : Enum<E>> KClass<E>.enumMembers(): Array<E> = this.java.enumConstants

@Suppress("UNCHECKED_CAST")
actual fun <T: Any, E: T?> ArrayList<E>.toNativeArray(eClass: KClass<T>): Array<E> = toArray(java.lang.reflect.Array.newInstance(eClass.java, size) as Array<E>)

@Suppress("UNCHECKED_CAST")
@ImplicitReflectionSerializer
internal fun <T> Class<T>.invokeSerializerGetter(vararg args: KSerializer<Any>): KSerializer<T>? {
    var serializer: KSerializer<T>? = null

    // Search for serializer defined on companion object.
    val companion = declaredFields.singleOrNull { it.name == "Companion" }?.apply { isAccessible = true }?.get(null)
    if (companion != null) {
        serializer = companion.javaClass.methods
            .find { method ->
                method.name == "serializer" && method.parameterTypes.size == args.size && method.parameterTypes.all { it == KSerializer::class.java }
            }
            ?.invoke(companion, *args) as? KSerializer<T>
    }

    // Search for default serializer in case no serializer is defined on companion object.
    if (serializer == null) {
        serializer =
            declaredClasses.singleOrNull { it.simpleName == ("\$serializer") }
            ?.getField("INSTANCE")?.get(null) as? KSerializer<T>
    }

    return serializer
}

/**
 * Checks if an [this@isInstanceOf] is an instance of a given [kclass].
 *
 * This check is a replacement for [KClass.isInstance] because
 * on JVM it requires kotlin-reflect.jar in classpath
 * (see https://youtrack.jetbrains.com/issue/KT-14720).
 *
 * On JS and Native, this function delegates to aforementioned
 * [KClass.isInstance] since it is supported there out-of-the box;
 * on JVM, it falls back to java.lang.Class.isInstance which causes
 * difference when applied to function types with big arity.
 */
internal actual fun Any.isInstanceOf(kclass: KClass<*>): Boolean = kclass.java.isInstance(this)

internal actual fun <T : Any> KClass<T>.simpleName(): String? = java.simpleName
