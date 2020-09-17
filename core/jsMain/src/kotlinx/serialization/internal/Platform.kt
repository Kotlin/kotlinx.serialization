/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlin.reflect.*

internal actual fun <T> Array<T>.getChecked(index: Int): T {
    if (index !in indices) throw IndexOutOfBoundsException("Index $index out of bounds $indices")
    return get(index)
}

internal actual fun BooleanArray.getChecked(index: Int): Boolean {
    if (index !in indices) throw IndexOutOfBoundsException("Index $index out of bounds $indices")
    return get(index)
}
@Suppress("UNCHECKED_CAST")
internal actual fun <T : Any> KClass<T>.compiledSerializerImpl(): KSerializer<T>? =
    this.constructSerializerForGivenTypeArgs() ?: this.js.asDynamic().Companion?.serializer() as? KSerializer<T>

internal actual fun <T : Any, E : T?> ArrayList<E>.toNativeArrayImpl(eClass: KClass<T>): Array<E> = toTypedArray()

internal actual fun Any.isInstanceOf(kclass: KClass<*>): Boolean = kclass.isInstance(this)

internal actual fun KClass<*>.platformSpecificSerializerNotRegistered(): Nothing {
    throw SerializationException(
        "Serializer for class '${simpleName}' is not found.\n" +
                "Mark the class as @Serializable or provide the serializer explicitly.\n" +
                "On Kotlin/JS explicitly declared serializer should be used for interfaces and enums without @Serializable annotation"
    )
}

@Suppress("UNCHECKED_CAST", "DEPRECATION_ERROR")
@OptIn(ExperimentalAssociatedObjects::class)
internal actual fun <T : Any> KClass<T>.constructSerializerForGivenTypeArgs(vararg args: KSerializer<Any?>): KSerializer<T>? {
    return try {
        when (val assocObject = findAssociatedObject<SerializableWith>()) {
            is KSerializer<*> -> assocObject as KSerializer<T>
            is SerializerFactory -> assocObject.serializer(*args) as KSerializer<T>
            else -> null
        }
    } catch (e: dynamic) {
        null
    }
}

internal actual fun isReferenceArray(rootClass: KClass<Any>): Boolean = rootClass == Array::class
