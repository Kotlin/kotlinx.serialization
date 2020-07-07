/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlin.reflect.*

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun <T> Array<T>.getChecked(index: Int): T {
    return get(index)
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun BooleanArray.getChecked(index: Int): Boolean {
    return get(index)
}

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

internal actual fun isReferenceArray(rootClass: KClass<Any>): Boolean = rootClass == Array::class
