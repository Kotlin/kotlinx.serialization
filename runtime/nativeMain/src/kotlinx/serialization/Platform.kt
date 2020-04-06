/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlin.reflect.*


@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
@Deprecated("Inserted into generated code and should not be used directly", level = DeprecationLevel.HIDDEN)
public annotation class SerializableWith(public val serializer: KClass<out KSerializer<*>>)

@Suppress(
    "UNCHECKED_CAST",
    "DEPRECATION_ERROR"
)
@OptIn(ExperimentalAssociatedObjects::class)
internal actual fun <T : Any> KClass<T>.constructSerializerForGivenTypeArgs(vararg args: KSerializer<Any?>): KSerializer<T>? =
    when (val assocObject = findAssociatedObject<SerializableWith>()) {
        is KSerializer<*> -> assocObject as KSerializer<T>
        is kotlinx.serialization.internal.SerializerFactory -> assocObject.serializer(*args) as KSerializer<T>
        else -> null
    }


@Suppress(
    "UNCHECKED_CAST",
    "DEPRECATION_ERROR"
)
@OptIn(ExperimentalAssociatedObjects::class)
@ImplicitReflectionSerializer
internal actual fun <T : Any> KClass<T>.compiledSerializerImpl(): KSerializer<T>? =
    this.constructSerializerForGivenTypeArgs()

internal actual fun <T : Any, E : T?> ArrayList<E>.toNativeArrayImpl(eClass: KClass<T>): Array<E> {
    val result = arrayOfAnyNulls<E>(size)
    var index = 0
    for (element in this) result[index++] = element
    @Suppress("UNCHECKED_CAST", "USELESS_CAST")
    return result as Array<E>
}

@Suppress("UNCHECKED_CAST")
private fun <T> arrayOfAnyNulls(size: Int): Array<T> = arrayOfNulls<Any>(size) as Array<T>

internal actual fun Any.isInstanceOf(kclass: KClass<*>): Boolean = kclass.isInstance(this)

internal actual fun <T : Any> KClass<T>.simpleName(): String? = simpleName

internal actual fun isReferenceArray(type: KType, rootClass: KClass<Any>): Boolean {
    val typeParameters = type.arguments
    if (typeParameters.size != 1) return false
    val parameter = typeParameters.single()
    // Fun fact -- star projections pass this check
    val variance = parameter.variance ?: error("Star projections are forbidden: $type")
    if (parameter.type == null) error("Star projections are forbidden: $type")
    val prefix = if (variance == KVariance.IN || variance == KVariance.OUT)
        variance.toString().toLowerCase() + " " else ""
    val parameterName = prefix + parameter.type.toString()
    val expectedName = "kotlin.Array<$parameterName>"
    if (type.toString() != expectedName) {
        return false
    }
    return true
}
