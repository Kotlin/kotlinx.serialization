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
    if (jClass.isEnum && jClass.isNotAnnotated()) {
        return jClass.createEnumSerializer()
    }
    if (jClass.isInterface) {
        return interfaceSerializer()
    }
    // Search for serializer defined on companion object.
    val serializer = invokeSerializerOnCompanion<T>(jClass, *args)
    if (serializer != null) return serializer
    // Check whether it's serializable object
    findObjectSerializer(jClass)?.let { return it }
    // Search for default serializer if no serializer is defined in companion object.
    // It is required for named companions
    val fromNamedCompanion = try {
        jClass.declaredClasses.singleOrNull { it.simpleName == ("\$serializer") }
            ?.getField("INSTANCE")?.get(null) as? KSerializer<T>
    } catch (e: NoSuchFieldException) {
        null
    }
    if (fromNamedCompanion != null) return fromNamedCompanion
    // Check for polymorphic
    return polymorphicSerializer()
}

private fun <T: Any> Class<T>.isNotAnnotated(): Boolean {
    /*
     * For annotated enums search serializer directly (or do not search at all?)
     */
    return getAnnotation(Serializable::class.java) == null &&
            getAnnotation(Polymorphic::class.java) == null
}

private fun <T: Any> KClass<T>.polymorphicSerializer(): KSerializer<T>? {
    /*
     * Last resort: check for @Polymorphic or Serializable(with = PolymorphicSerializer::class)
     * annotations.
     */
    val jClass = java
    if (jClass.getAnnotation(Polymorphic::class.java) != null) {
        return PolymorphicSerializer(this)
    }
    val serializable = jClass.getAnnotation(Serializable::class.java)
    if (serializable != null && serializable.with == PolymorphicSerializer::class) {
        return PolymorphicSerializer(this)
    }
    return null
}

private fun <T: Any> KClass<T>.interfaceSerializer(): KSerializer<T>? {
    /*
     * Interfaces are @Polymorphic by default.
     * Check if it has no annotations or `@Serializable(with = PolymorphicSerializer::class)`,
     * otherwise bailout.
     */
    val serializable = java.getAnnotation(Serializable::class.java)
    if (serializable == null || serializable.with == PolymorphicSerializer::class) {
        return PolymorphicSerializer(this)
    }
    return null
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> invokeSerializerOnCompanion(jClass: Class<*>, vararg args: KSerializer<Any?>): KSerializer<T>? {
    val companion = jClass.companionOrNull() ?: return null
    return try {
        val types = if (args.isEmpty()) emptyArray() else Array(args.size) { KSerializer::class.java }
        companion.javaClass.getDeclaredMethod("serializer", *types)
            .invoke(companion, *args) as? KSerializer<T>
    } catch (e: NoSuchMethodException) {
        null
    } catch (e: InvocationTargetException) {
        val cause = e.cause ?: throw e
        throw InvocationTargetException(cause, cause.message ?: e.message)
    }
}

private fun Class<*>.companionOrNull() =
    try {
        val companion = getDeclaredField("Companion")
        companion.isAccessible = true
        companion.get(null)
    } catch (e: Throwable) {
        null
    }

@Suppress("UNCHECKED_CAST")
private fun <T : Any> Class<T>.createEnumSerializer(): KSerializer<T>? {
    val constants = enumConstants
    return EnumSerializer(canonicalName, constants as Array<out Enum<*>>) as? KSerializer<T>
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
