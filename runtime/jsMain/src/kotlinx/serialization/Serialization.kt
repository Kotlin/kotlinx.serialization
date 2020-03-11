/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlin.reflect.*

@Suppress("UNCHECKED_CAST")
@ImplicitReflectionSerializer
internal actual fun <T : Any> KClass<T>.compiledSerializerImpl(): KSerializer<T>? =
    this.js.asDynamic().Companion?.serializer() as? KSerializer<T>

internal actual fun <T : Any, E : T?> ArrayList<E>.toNativeArrayImpl(eClass: KClass<T>): Array<E> = toTypedArray()

internal actual fun Any.isInstanceOf(kclass: KClass<*>): Boolean = kclass.isInstance(this)

internal actual fun <T : Any> KClass<T>.simpleName(): String? = simpleName

internal actual fun <T : Any> KClass<T>.constructSerializerForGivenTypeArgs(vararg args: KSerializer<Any?>): KSerializer<T>? {
    throw NotImplementedError("This method is not supported for Kotlin/JS yet. Please provide serializer explicitly.")
}

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
    val expectedName = "Array<$parameterName>"
    if (type.toString() != expectedName) {
        return false
    }
    return true
}
