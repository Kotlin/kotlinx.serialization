/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import java.lang.reflect.*
import kotlin.reflect.*

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun <T> Array<T>.getChecked(index: Int): T {
    return get(index)
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun BooleanArray.getChecked(index: Int): Boolean {
    return get(index)
}

@Suppress("UNCHECKED_CAST")
internal actual fun <T : Any> KClass<T>.compiledSerializerImpl(): KSerializer<T>? =
    this.constructSerializerForGivenTypeArgs()

@Suppress("UNCHECKED_CAST")
internal actual fun <T : Any, E : T?> ArrayList<E>.toNativeArrayImpl(eClass: KClass<T>): Array<E> =
    toArray(java.lang.reflect.Array.newInstance(eClass.java, size) as Array<E>)

@Suppress("UNCHECKED_CAST")
internal actual fun <T : Any> KClass<T>.constructSerializerForGivenTypeArgs(vararg args: KSerializer<Any?>): KSerializer<T>? {
    val jClass = this.java
    // Search for serializer defined on companion object.
   val serializer = invokeSerializerOnCompanion<T>(jClass, *args)
    if (serializer != null) return serializer
    // Check whether it's serializable object
    findObjectSerializer(jClass)?.let { return it }
    // Search for default serializer if no serializer is defined in companion object.
    val default = try {
        jClass.declaredClasses.singleOrNull { it.simpleName == ("\$serializer") }
            ?.getField("INSTANCE")?.get(null) as? KSerializer<T>
    } catch (e: NoSuchFieldException) {
        null
    }

    if (default != null) return default
    // Fallback: enum without @Serializable annotation as last resort
    return createEnumSerializer()
}

private fun <T : Any> invokeSerializerOnCompanion(jClass: Class<*>, vararg args: KSerializer<Any?>): KSerializer<T>? {
    val companion =
        jClass.declaredFields.singleOrNull { it.name == "Companion" }?.apply { isAccessible = true }?.get(null)
            ?: return null
    try {
        return companion.javaClass.methods
            .find { method ->
                method.name == "serializer" && method.parameterTypes.size == args.size && method.parameterTypes.all { it == KSerializer::class.java }
            }
            ?.invoke(companion, *args) as? KSerializer<T>
    } catch (e: InvocationTargetException) {
        val cause = e.cause ?: throw e
        throw InvocationTargetException(cause, cause.message ?: e.message)
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> KClass<T>.createEnumSerializer(): KSerializer<T>? {
    if (!Enum::class.java.isAssignableFrom(this.java)) return null
    val enum = java
    val constants = enum.enumConstants
    return EnumSerializer(enum.canonicalName, constants as Array<out Enum<*>>) as? KSerializer<T>
}

private fun <T : Any> findObjectSerializer(jClass: Class<T>): KSerializer<T>? {
    // Check it is an object without using kotlin-reflect
    val field =
        jClass.declaredFields.singleOrNull { it.name == "INSTANCE" && it.type == jClass && Modifier.isStatic(it.modifiers) }
            ?: return null
    // Retrieve its instance and call serializer()
    val instance = field.get(null)
    val method =
        jClass.methods.singleOrNull { it.name == "serializer" && it.parameters.isEmpty() && it.returnType == KSerializer::class.java }
            ?: return null
    val result = method.invoke(instance)
    @Suppress("UNCHECKED_CAST")
    return result as? KSerializer<T>
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
 * on JVM, it falls back to java.lang.Class.isInstance, which causes
 * difference when applied to function types with big arity.
 */
internal actual fun Any.isInstanceOf(kclass: KClass<*>): Boolean = kclass.javaObjectType.isInstance(this)

internal actual fun isReferenceArray(rootClass: KClass<Any>): Boolean = rootClass.java.isArray
